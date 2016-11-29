import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.introduceParameter.IntroduceParameterProcessor;
import com.intellij.refactoring.introduceParameter.Util;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FunctionalClonesReplacement extends AnAction {



    public void actionPerformed(AnActionEvent event) {

        final PsiFile file = event.getData(LangDataKeys.PSI_FILE);
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        final Editor editor = event.getData(CommonDataKeys.EDITOR);




        Set<PsiMethod> affectedMethods = migrateToStreams(file, project);
        Set<PsiMethod> refactoredMethods = extractFunctionalParameters(affectedMethods, project, editor);
        removeDuplicatedFunctions(refactoredMethods, file, project);



//
//        String txt = Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
//        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());

    }

    private Set<PsiMethod> migrateToStreams(PsiFile file, Project project) {
        InspectionManager manager = new InspectionManagerEx(project);
        ProblemsHolder holder = new ProblemsHolder(manager, file, true);
        PsiElementVisitor elementVisitor = new StreamApiMigrationInspection().buildVisitor(holder, true);

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
                if(element != null) {
                    PsiMethod method = Util.getContainingMethod(descriptor.getPsiElement());
                    affectedMethods.add(method);
                    QuickFix[] fixes = descriptor.getFixes();
                    fixes[0].applyFix(project, descriptor);
                }
            }
        });

        return affectedMethods;
    }

    private Set<PsiMethod> extractFunctionalParameters(Set<PsiMethod> prevMethods, Project project, Editor editor) {
        List<PsiExpression> expressions = getExpressionsToExtractAsParameters(prevMethods);
        Set<PsiMethod> affectedMethods = new HashSet<>();

        expressions.forEach(expr -> {
            PsiMethod method = Util.getContainingMethod(expr);
            affectedMethods.add(method);
            performExtraction("azaza", expr, project, editor, false);
        });

        return affectedMethods;

    }

    private void removeDuplicatedFunctions(Set<PsiMethod> methodsToRefactor, PsiFile file, Project project) {

        methodsToRefactor.forEach(psiMethod -> removeDuplicates(psiMethod, project, file));
    }

    private ArrayList<PsiExpression> getExpressionsToExtractAsParameters(Set<PsiMethod> methods){

        CollectExpressionsToExtractVisitor visitor = new CollectExpressionsToExtractVisitor();

        methods.forEach(method -> method.accept(visitor));

        return visitor.getExpressions();
    }

    private boolean performExtraction(@NonNls String parameterName,
                                      PsiExpression expr,
                                      Project project,
                                      Editor editor,
                                      final boolean replaceDuplicates) {



        TIntArrayList parametersToRemove = new TIntArrayList();
        PsiMethod method = getContainingMethod(expr);
        PsiType forcedType = RefactoringUtil.getTypeByExpressionWithExpectedType(expr);
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

        editor.getSelectionModel().removeSelection();
        return true;
    }

    private PsiMethod getContainingMethod(PsiExpression expr) {

        PsiMethod method = Util.getContainingMethod(expr);
        if (method == null) return null;

        final List<PsiMethod> methods = com.intellij.refactoring.introduceParameter.IntroduceParameterHandler.getEnclosingMethods(method);
        method = methods.get(0);

        return method;
    }

    private void removeDuplicates(PsiMethod psiMethod, Project project, PsiFile file) {

        MethodDuplicatesHandler.invokeOnScope(project, psiMethod, new AnalysisScope(file));

        //checkResultByFile(filePath + ".after");
    }
}
/*Document is locked by write PSI operations. Use PsiDocumentManager.doPostponedOperationsAndUnblockDocument() to commit PSI changes to the document.*/


//PsiFile file = event.getData(LangDataKeys.PSI_FILE);
//PsiMethod main = (PsiMethod) file.findElementAt(6);

//        final DataContext dataContext = event.getDataContext();
//        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
//        Project project = event.getData(PlatformDataKeys.PROJECT);
//
//
//        CollectAllElementsVisitor visitor = new CollectAllElementsVisitor();
//
//        file.accept(visitor);
//
//        if(true)
//        return;
//
//        PsiElement[] test3 = file.getChildren();
//
//        PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
//
//
//        WriteCommandAction.runWriteCommandAction(project, () -> {
//            PsiComment comment = psiElementFactory.createCommentFromText("//asasasasas", file);
//            file.addBefore(comment, file.getFirstChild());
//
//            PsiElement[] classMainChildren = test3[3].getChildren();
//            PsiMethod method = (PsiMethod) classMainChildren[11];
//            method.setName("notMain");
//
//        });


/*
*
*
*
*
* Пример исползования Visitor'а:
*   public static boolean variableIsAssigned(
    @NotNull PsiVariable variable, @Nullable PsiElement context) {
    if (context == null) {
      return false;
    }
    //Создаем визитора
    final VariableAssignedVisitor visitor =
      new VariableAssignedVisitor(variable, true);

    пихаем визитора в дерево, для прохода по всем элементам
    context.accept(visitor);
    // если на каком-то элементе переменную переприсвоили, то у него поменяется флаг isAssigned
    return visitor.isAssigned();
  }

  */