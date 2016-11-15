import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf;

/**
 * Created by boris on 15.11.16.
 */
public class PsiVisitor extends PsiElementVisitor {
    @Override
    public void visitElement(PsiElement element) {
        System.out.println(element.toString());
        element.acceptChildren(this);
    }
}
