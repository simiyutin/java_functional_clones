import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.QuickFix;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
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
import java.util.stream.Collectors;

public class FunctionalClonesReplacement extends AnAction {

    private Project project;

    // workaround for running on Apache Ant:
    // refactoring of this file leads to fail of FilterSetTest. Failure is bound to extraction of the parameter in
    // FilterSet.addConfiguredFilterSet
    // Possibly, the problem is with dynamic methods invocations via reflection api
    private static Set<String> declinedFiles = new HashSet<>(Arrays.asList("FilterSet"));

    private static boolean NAMES_SALTING = true;


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
        removeDuplicatedFunctions(refactoredMethods);
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
                    // Avoiding name collisions in outer usages of refactored method
                    if (NAMES_SALTING && expr instanceof PsiLambdaExpression) {
                        MyUtil.saltLocalVariables(((PsiLambdaExpression) expr), project);
                    }
                    performExtraction(name, expr);
                }
            }

        });

        return affectedMethods;
    }


    private void removeDuplicatedFunctions(Set<PsiMethod> methodsToRefactor) {

        Set<PsiMember> members = methodsToRefactor.stream().map(meth -> (PsiMember) meth).collect(Collectors.toSet());
        List<PsiMember> deletedMethods = new ArrayList<>();

        for (PsiMember member : members) {
            if(!deletedMethods.contains(member)) {

                // inefficient, see comment below
                Map<PsiMember, List<Match>> duplicates = MyUtil.invokeOnScope(project, Collections.singleton(member), new AnalysisScope(project));
                List<Match> matches = duplicates.get(member);
                if (matches != null && !matches.isEmpty()) {

                    deletedMethods.addAll(MyUtil.collapseFullDuplicates(matches, member, project));

                    String newName = Messages.showInputDialog(project, "Please choose more broad function name for function " + member.getName(), "Rename function " + member.getName(), Messages.getQuestionIcon());
                    final RenameProcessor renameProcessor = new RenameProcessor(project, member, newName, false, false);
                    renameProcessor.run();

                    // inefficient, because we perform whole project search two times, but it is the simpliest way to use private api
                    MethodDuplicatesHandler.invokeOnScope(project, Collections.singleton(member), new AnalysisScope(project), true);
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



    private void performExtraction(@NonNls String parameterName,
                                      PsiExpression expr) {

        TIntArrayList parametersToRemove = new TIntArrayList();
        PsiMethod method = MyUtil.getContainingMethod(expr);
        if (method == null) return;
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, method)) return;

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
                return false;
            }
        };

        processor.run();

    }

}