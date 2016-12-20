import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.introduceParameter.IntroduceParameterProcessor;
import com.intellij.refactoring.introduceParameter.Util;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.function.Consumer;

public class FunctionalClonesReplacement extends AnAction {

    private Project project;
    private PsiFile file;

    public void acceptAllPsiFiles(VirtualFile vfile, Consumer<PsiFile> consumer) {
        if ("java".equals(vfile.getExtension())) {
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

    void doRefactor(PsiFile psiFile) {
        file = psiFile;
        Set<PsiMethod> affectedMethods = migrateToStreams();
        Set<PsiMethod> refactoredMethods = extractFunctionalParameters(affectedMethods);
        removeDuplicatedFunctions(refactoredMethods);
    }

    private Set<PsiMethod> migrateToStreams() {
        InspectionManager manager = new InspectionManagerEx(project);
        ProblemsHolder holder = new ProblemsHolder(manager, file, true);
        StreamApiMigrationInspection inspection = new StreamApiMigrationInspection();
        inspection.SUGGEST_FOREACH = true;
        PsiElementVisitor elementVisitor = inspection.buildVisitor(holder, true);

        CollectAllElementsVisitor collector = new CollectAllElementsVisitor();
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
                    PsiMethod method = Util.getContainingMethod(descriptor.getPsiElement());
                    affectedMethods.add(method);
                    QuickFix[] fixes = descriptor.getFixes();
                    fixes[0].applyFix(project, descriptor);
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
                performExtraction(name, expr, project, false);
            }

        });

        return affectedMethods;

    }


    private void removeDuplicatedFunctions(Set<PsiMethod> methodsToRefactor) {


        List<PsiMethod> alreadyReplacedMethods = new ArrayList<>();
        for (PsiMethod psiMethod : methodsToRefactor) {
            if (!alreadyReplacedMethods.contains(psiMethod)) {
                List<Match> matches = MethodDuplicatesHandler.hasDuplicates(file, psiMethod);
                if (!matches.isEmpty()) {

                    for (Match match : matches) {
                        if (MyUtil.isMatchAloneInMethod(match)) {
                            PsiMethod matchedMethod = Util.getContainingMethod(match.getMatchStart());
                            final RenameProcessor renameProcessor = new MyRenameProcessor(project, matchedMethod, psiMethod.getName(), false, false);
                            renameProcessor.run();

                            WriteCommandAction.runWriteCommandAction(project, matchedMethod::delete);
                            alreadyReplacedMethods.add(matchedMethod);
                        }
                    }


                    String newName = Messages.showInputDialog(project, "Please choose more broad function name for function" + psiMethod.getName(), "Rename function " + psiMethod.getName(), Messages.getQuestionIcon());
                    final RenameProcessor renameProcessor = new RenameProcessor(project, psiMethod, newName, false, false);
                    renameProcessor.run();


                    if (alreadyReplacedMethods.size() < methodsToRefactor.size()) {
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
        PsiMethod method = getContainingMethod(expr);
        PsiType forcedType = RefactoringUtil.getTypeByExpressionWithExpectedType(expr);
        if (method != null) {
            IntroduceParameterProcessor processor = new IntroduceParameterProcessor(
                    project,
                    method, // равны со следующим. Надо понять, как его находить
                    method, //
                    expr, // вроде как просто мой экспрешн, равен следующему
                    expr, //
                    null, // null
                    false,
                    parameterName, // имя параметра, запихиваемого в функцию
                    true, //false
                    1, // 1
                    false, // false
                    false, // false
                    forcedType, // а вот это важная фигня
                    parametersToRemove //пустой, видимо можно передать просто new ...
            ) {
                @Override
                protected boolean isReplaceDuplicates() {
                    return replaceDuplicates;
                }
            };

            processor.run();
        }

        return true;
    }

    private PsiMethod getContainingMethod(PsiExpression expr) {

        PsiMethod method = Util.getContainingMethod(expr);
        if (method == null) return null;

        final List<PsiMethod> methods = com.intellij.refactoring.introduceParameter.IntroduceParameterHandler.getEnclosingMethods(method);
        method = methods.get(0);

        return method;
    }

}