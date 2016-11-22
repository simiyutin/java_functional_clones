import com.intellij.psi.*;
import com.intellij.refactoring.util.RefactoringUtil;

import java.util.ArrayList;

/**
 * Created by boris on 22.11.16.
 */
public class CollectExpressionsToExtractVisitor extends PsiElementVisitor {

    private ArrayList<PsiExpression> expressions;

    public CollectExpressionsToExtractVisitor() {
        this.expressions = new ArrayList<>();
    }

    public ArrayList<PsiExpression> getExpressions() {
        return expressions;
    }

    @Override
    public void visitElement(PsiElement element) {
        if(element instanceof PsiLambdaExpression) {
            expressions.add(((PsiLambdaExpression) element));
        }
        element.acceptChildren(this);
    }
}
