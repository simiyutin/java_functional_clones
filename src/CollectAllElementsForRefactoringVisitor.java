import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTryStatement;
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf;

import java.util.ArrayList;

/**
 * Created by boris on 15.11.16.
 */
public class CollectAllElementsForRefactoringVisitor extends PsiElementVisitor {

    private ArrayList<PsiElement> allElements;

    public CollectAllElementsForRefactoringVisitor() {
        this.allElements = new ArrayList<>();
    }

    @Override
    public void visitElement(PsiElement element) {
        if (isOkToRefactor(element)) {
            allElements.add(element);
            element.acceptChildren(this);
        }
    }

    private boolean isOkToRefactor(PsiElement element) {

        if (element instanceof PsiMethod) {
            PsiMethod method = ((PsiMethod) element);
            boolean throwsAnything = method.getThrowsList().getReferenceElements().length > 0;
            if (throwsAnything) return false;
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
