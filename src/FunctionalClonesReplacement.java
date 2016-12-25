import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.introduceParameter.IntroduceParameterProcessor;
import com.intellij.refactoring.introduceParameter.Util;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler;
import com.intellij.util.IncorrectOperationException;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.function.Consumer;

public class FunctionalClonesReplacement extends AnAction {

    private Project project;

    // refactoring of this file leads to fail of FilterSetTest. Failure is bound to extraction of the parameter in
    // FilterSet.addConfiguredFilterSet
    private static Set<String> declinedFiles = new HashSet<>(Arrays.asList("FilterSet"));



    private void acceptAllPsiFiles(VirtualFile vfile, Consumer<PsiFile> consumer) {
        if ("java".equals(vfile.getExtension()) && ! declinedFiles.contains(vfile.getNameWithoutExtension())) {
            consumer.accept(PsiManager.getInstance(project).findFile(vfile));
        }
        for (VirtualFile vfile2 : vfile.getChildren()) {
            acceptAllPsiFiles(vfile2, consumer);
        }
    }


    public void actionPerformed(AnActionEvent event) {

        project = event.getData(PlatformDataKeys.PROJECT);

        acceptAllPsiFiles(project.getBaseDir(), this::doRefactor);

    }

    private void doRefactor(PsiFile psiFile) {
        Set<PsiMethod> affectedMethods = migrateToStreams(psiFile);
        Set<PsiMethod> refactoredMethods = extractFunctionalParameters(affectedMethods);
        removeDuplicatedFunctions(refactoredMethods, psiFile);
    }

    private Set<PsiMethod> migrateToStreams(PsiFile file) {

        InspectionManager manager = new InspectionManagerEx(project);
        ProblemsHolder holder = new ProblemsHolder(manager, file, true);
        StreamApiMigrationInspection inspection = new StreamApiMigrationInspection();
        inspection.SUGGEST_FOREACH = true;
        PsiElementVisitor elementVisitor = inspection.buildVisitor(holder, true);

        CollectMethodsForRefactoringVisitor collector = new CollectMethodsForRefactoringVisitor();
        file.accept(collector);
        List<PsiElement> elements = collector.getAllElements();
        Set<PsiMethod> affectedMethods = new HashSet<>();

        for (PsiElement element : elements) {
            element.accept(elementVisitor);
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (ProblemDescriptor descriptor : holder.getResults()) {
                PsiElement element = descriptor.getPsiElement();
                if (element != null) {
                    try {

                        PsiMethod method = Util.getContainingMethod(descriptor.getPsiElement());

                        QuickFix fix = descriptor.getFixes()[0];

                        // troubles with this refactoring in some cases
                        if ("Replace with findFirst()".equals(fix.getName())) continue;

                        affectedMethods.add(method);
                        fix.applyFix(project, descriptor);


                    } catch (IncorrectOperationException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        return affectedMethods;
    }

    private Set<PsiMethod> extractFunctionalParameters(Set<PsiMethod> prevMethods) {

        List<PsiExpression> expressions = getExpressionsToExtractAsParameters(prevMethods);
        Set<PsiMethod> affectedMethods = new HashSet<>();

        expressions.forEach(expr -> {
            PsiMethod method = Util.getContainingMethod(expr);
            if (method != null) {
                affectedMethods.add(method);
                String name = MyUtil.getNameForParameter(expr, project);
                if(name != null) {
                    if (expr instanceof PsiLambdaExpression) {
                        MyUtil.renameLocalVariables(((PsiLambdaExpression) expr), project);
                    }
                    performExtraction(name, expr, project, false);
                }
            }

        });

        return affectedMethods;
    }


    private void removeDuplicatedFunctions(Set<PsiMethod> methodsToRefactor, PsiFile file) {

        List<PsiMethod> deletedMethods = new ArrayList<>();
        for (PsiMethod psiMethod : methodsToRefactor) {
            if (!deletedMethods.contains(psiMethod)) {
                List<Match> matches = MethodDuplicatesHandler.hasDuplicates(file, psiMethod);
                if (!matches.isEmpty()) {

                    // first step - delete clone methods
                    for (Match match : matches) {
                        if (MyUtil.isMatchAloneInMethod(match)) {
                            PsiMethod matchedMethod = Util.getContainingMethod(match.getMatchStart());
                            // if found method, which does the same stuff as other method, rename it to other and delete definition
                            final RenameProcessor renameProcessor = new MyRenameProcessor(project, matchedMethod, psiMethod.getName(), false, false);
                            renameProcessor.run();

                            WriteCommandAction.runWriteCommandAction(project, matchedMethod::delete);
                            deletedMethods.add(matchedMethod);
                        }
                    }


                    String newName = Messages.showInputDialog(project, "Please choose more broad function name for function " + psiMethod.getName(), "Rename function " + psiMethod.getName(), Messages.getQuestionIcon());
                    final RenameProcessor renameProcessor = new RenameProcessor(project, psiMethod, newName, false, false);
                    renameProcessor.run();

                    // second step - replace other occurrences
                    if (deletedMethods.size() < methodsToRefactor.size()) {
                        MethodDuplicatesHandler.invokeOnScope(project, Collections.singleton(psiMethod), new AnalysisScope(file), true);
                    }
                }

            }

        }
    }

    private ArrayList<PsiExpression> getExpressionsToExtractAsParameters(Set<PsiMethod> methods) {

        CollectExpressionsToExtractVisitor visitor = new CollectExpressionsToExtractVisitor(project);

        if (methods != null) {
            methods.stream().filter(Objects::nonNull).forEach(method -> method.accept(visitor));
        }


        return visitor.getExpressions();
    }

    private boolean performExtraction(@NonNls String parameterName,
                                      PsiExpression expr,
                                      Project project,
                                      final boolean replaceDuplicates) {


        TIntArrayList parametersToRemove = new TIntArrayList();
        PsiMethod method = MyUtil.getContainingMethod(expr);
        if (method == null) return false;
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, method)) return false;

        PsiType forcedType = RefactoringUtil.getTypeByExpressionWithExpectedType(expr);

        IntroduceParameterProcessor processor = new IntroduceParameterProcessor(
                project,
                method,
                method,
                expr,
                expr,
                null,
                false,
                parameterName,
                true,
                1,
                false,
                false,
                forcedType,
                parametersToRemove
        ) {
            @Override
            protected boolean isReplaceDuplicates() {
                return replaceDuplicates;
            }
        };

        processor.run();

        return true;
    }

}