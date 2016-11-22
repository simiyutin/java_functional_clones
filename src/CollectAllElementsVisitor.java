import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf;

import java.util.ArrayList;

/**
 * Created by boris on 15.11.16.
 */
public class CollectAllElementsVisitor extends PsiElementVisitor {

    private ArrayList<PsiElement> allElements;

    public CollectAllElementsVisitor() {
        this.allElements = new ArrayList<>();
    }

    @Override
    public void visitElement(PsiElement element) {
        allElements.add(element);
        element.acceptChildren(this);
    }

    public ArrayList<PsiElement> getAllElements() {
        return allElements;
    }
}
