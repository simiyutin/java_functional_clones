import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.refactoring.util.duplicates.Match;

public class MyUtil {

    public static boolean isMatchAloneInMethod(Match match) {
        return isAloneUpper(match.getMatchStart()) && isAloneLower(match.getMatchEnd());
    }

    private static boolean isAloneUpper(PsiElement element) {
        PsiElement prevSibling = element.getPrevSibling();
        if (prevSibling instanceof PsiWhiteSpace) {
            return isAloneUpper(prevSibling);
        } else if (prevSibling.getText().equals("{")) {
            return true;
        }
        return false;

    }

    private static boolean isAloneLower(PsiElement element) {

        PsiElement nextSibling = element.getNextSibling();
        if (nextSibling instanceof PsiWhiteSpace) {
            return isAloneLower(nextSibling);
        } else if (nextSibling.getText().equals("}")) {
            return true;
        } else if (nextSibling instanceof PsiReturnStatement) {
            return true; //todo не фига не так, переделать
        }
        return false;
    }
}
