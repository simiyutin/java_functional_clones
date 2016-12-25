import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.controlFlow.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.introduceParameter.Util;
import one.util.streamex.StreamEx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by boris on 22.11.16.
 */
public class CollectExpressionsToExtractVisitor extends PsiElementVisitor {

    private ArrayList<PsiExpression> expressions;
    private Project myProject;

    public CollectExpressionsToExtractVisitor(Project project) {
        expressions = new ArrayList<>();
        myProject = project;
    }

    public ArrayList<PsiExpression> getExpressions() {
        return expressions;
    }

    @Override
    public void visitElement(PsiElement element) {

        if (implementsInterfaceContracts(element)) return;

        if (element instanceof PsiExpression &&
                !hasReferencedVariables(((PsiExpression) element)) &&
                !callsSharedMethods(element)) {

            if(element instanceof PsiLambdaExpression) {
                expressions.add(((PsiLambdaExpression) element));
            }
            if(element instanceof PsiMethodReferenceExpression) {
                expressions.add(((PsiMethodReferenceExpression) element));
            }
        }

        element.acceptChildren(this);

    }

    private boolean hasReferencedVariables(PsiExpression expr) {

        final ControlFlow controlFlow;
        try {
            controlFlow = ControlFlowFactory.getInstance(myProject)
                    .getControlFlow(expr, LocalsOrMyInstanceFieldsControlFlowPolicy.getInstance());
        }
        catch (AnalysisCanceledException ignored) {
            return true;
        }

        int startOffset = controlFlow.getStartOffset(expr);
        int endOffset = controlFlow.getEndOffset(expr);
        final List<PsiVariable> outerVariables = StreamEx.of(ControlFlowUtil.getUsedVariables(controlFlow, startOffset, endOffset))
                .remove(variable -> PsiTreeUtil.getParentOfType(variable, expr.getClass(), PsiClass.class) == expr)
                .toList();

        return ! outerVariables.isEmpty();
    }

    private boolean callsSharedMethods(PsiElement element) {

        Collection<PsiMethodCallExpression> calls = PsiTreeUtil.findChildrenOfType(element, PsiMethodCallExpression.class);
        for (PsiMethodCallExpression expr : calls) {
            if (expr.getMethodExpression().getQualifierExpression() == null) {
                System.out.println("Expression rejected to extract: \n" + element.getText() + "\n ________________________________________");
                return true;
            }
        }

        return false;
    }

    private boolean implementsInterfaceContracts(PsiElement element) {

        PsiMethod method = Util.getContainingMethod(element);
        if (method != null) {
            PsiMethod[] superMethods = method.findSuperMethods();
            return superMethods.length > 0;
        }

        return false;
    }
}
