import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTryStatement;

import java.util.ArrayList;

public class CollectMethodsForRefactoringVisitor extends PsiElementVisitor {

    private ArrayList<PsiElement> allElements;

    public CollectMethodsForRefactoringVisitor() {
        this.allElements = new ArrayList<>();
    }

    @Override
    public void visitElement(PsiElement element) {
        if (doesNotThrowAnything(element)) {
            allElements.add(element);
            element.acceptChildren(this);
        }
    }

    private boolean doesNotThrowAnything(PsiElement element) {

        if (element instanceof PsiMethod) {
            PsiMethod method = ((PsiMethod) element);
            boolean throwsAnything = method.getThrowsList().getReferenceElements().length > 0;
            if (throwsAnything) return false;
            return true;
        }

        if (element instanceof PsiTryStatement) {
            return false;
        }

        return true;
    }

    public ArrayList<PsiElement> getAllElements() {
        return allElements;
    }
}
