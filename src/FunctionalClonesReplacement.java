import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.refactoring.introduceParameter.AbstractJavaInplaceIntroducer;
import com.intellij.refactoring.introduceParameter.IntroduceParameterProcessor;
import com.intellij.refactoring.introduceParameter.Util;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.ui.NameSuggestionsGenerator;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler;
import com.intellij.util.ArrayUtil;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NonNls;

import java.util.*;

public class FunctionalClonesReplacement extends AnAction {

    private Project project;
    private PsiFile file;
    private Editor editor;


    public void actionPerformed(AnActionEvent event) {

        file = event.getData(LangDataKeys.PSI_FILE);
        project = event.getData(PlatformDataKeys.PROJECT);
        editor = event.getData(CommonDataKeys.EDITOR);

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
            affectedMethods.add(method);
            //todo move to getName, throws java.lang.ArrayIndexOutOfBoundsException exceptions sometimes
            String name = createNameSuggestionGenerator(expr, null, project, null).getSuggestedNameInfo(expr.getType()).names[1];
            performExtraction(name, expr, project, editor, false);
        });

        return affectedMethods;

    }

    private static NameSuggestionsGenerator createNameSuggestionGenerator(final PsiExpression expr,
                                                                            final String propName,
                                                                            final Project project, String enteredName) {
        return new NameSuggestionsGenerator() {
            public SuggestedNameInfo getSuggestedNameInfo(PsiType type) {
                final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
                SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, propName, expr != null && expr.isValid() ? expr : null, type);

                if (expr != null && expr.isValid()) {
                    info = codeStyleManager.suggestUniqueVariableName(info, expr, true);
                }
                final String[] strings = AbstractJavaInplaceIntroducer.appendUnresolvedExprName(JavaCompletionUtil
                        .completeVariableNameForRefactoring(codeStyleManager, type, VariableKind.LOCAL_VARIABLE, info), expr);
                return new SuggestedNameInfo.Delegate(enteredName != null ? ArrayUtil.mergeArrays(new String[]{enteredName}, strings) : strings, info);
            }

        };
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


                    String newName = Messages.showInputDialog(project, "Please choose more broad function name", "Rename function " + psiMethod.getName(), Messages.getQuestionIcon());
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

}



//        String txt = Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
//        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());

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