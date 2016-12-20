import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.controlFlow.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.introduceParameter.Util;
import com.intellij.refactoring.util.RefactoringUtil;
import one.util.streamex.StreamEx;

import java.util.ArrayList;
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
        if (element instanceof PsiExpression && !hasReferencedVariables(((PsiExpression) element))) {

            if(element instanceof PsiLambdaExpression) {
                expressions.add(((PsiLambdaExpression) element));
            }
            if(element instanceof PsiMethodReferenceExpression) {
                expressions.add(((PsiMethodReferenceExpression) element));
            }
        }
        element.acceptChildren(this);
    }

    boolean hasReferencedVariables(PsiExpression expr) {

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
}
