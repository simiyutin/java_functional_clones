import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.controlFlow.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.introduceParameter.AbstractJavaInplaceIntroducer;
import com.intellij.refactoring.introduceParameter.Util;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.ui.NameSuggestionsGenerator;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.DuplicatesFinder;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler;
import com.intellij.util.ArrayUtil;
import one.util.streamex.StreamEx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MyUtil {


    public static String getNameForParameter(PsiExpression expr, Project project) {

        String[] names = createNameSuggestionGenerator(expr, null, project, null).getSuggestedNameInfo(expr.getType()).names;

        if (names.length == 0) return null;

        return names[0];
    }

    private static NameSuggestionsGenerator createNameSuggestionGenerator(final PsiExpression expr,
                                                                          final String propName,
                                                                          final Project project, String enteredName) {
        return type -> {
            final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
            SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, propName, expr != null && expr.isValid() ? expr : null, type);

            if (expr != null && expr.isValid()) {
                info = codeStyleManager.suggestUniqueVariableName(info, expr, true);
            }
            final String[] strings = AbstractJavaInplaceIntroducer.appendUnresolvedExprName(JavaCompletionUtil
                    .completeVariableNameForRefactoring(codeStyleManager, type, VariableKind.LOCAL_VARIABLE, info), expr);
            return new SuggestedNameInfo.Delegate(enteredName != null ? ArrayUtil.mergeArrays(new String[]{enteredName}, strings) : strings, info);
        };
    }

    static void renameLocalVariables(PsiLambdaExpression expr, Project myProject) {

        final ControlFlow controlFlow;
        try {
            controlFlow = ControlFlowFactory.getInstance(myProject)
                    .getControlFlow(expr, LocalsOrMyInstanceFieldsControlFlowPolicy.getInstance());
        }
        catch (AnalysisCanceledException ignored) {
            return;
        }

        int startOffset = controlFlow.getStartOffset(expr);
        int endOffset = controlFlow.getEndOffset(expr);
        final List<PsiVariable> innerVariables = StreamEx.of(ControlFlowUtil.getUsedVariables(controlFlow, startOffset, endOffset))
                .remove(variable -> PsiTreeUtil.getParentOfType(variable, expr.getClass(), PsiClass.class) != expr)
                .append(expr.getParameterList().getParameters())
                .toList();

        if (innerVariables.isEmpty()) return;

        innerVariables.forEach(var -> {
            WriteCommandAction.runWriteCommandAction(myProject, () -> {
                String salt = UUID.randomUUID().toString().replace("-","");
                RefactoringUtil.renameVariableReferences(var, var.getName() + salt, new LocalSearchScope(expr), true);
                var.setName(var.getName() + salt);
            });
        });

    }

    static PsiMethod getContainingMethod(PsiExpression expr) {

        PsiMethod method = Util.getContainingMethod(expr);
        if (method == null) return null;

        final List<PsiMethod> methods = com.intellij.refactoring.introduceParameter.IntroduceParameterHandler.getEnclosingMethods(method);
        method = methods.get(0);

        return method;
    }
}
