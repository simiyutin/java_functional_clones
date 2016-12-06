import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.util.duplicates.Match;
import org.jetbrains.annotations.Nullable;

public class MyUtil {

//    @Nullable //todo useful method
//    static PsiReturnStatement getNextReturnStatement(PsiStatement statement) {
//        PsiElement nextStatement = /*todo useful method*/PsiTreeUtil.skipSiblingsForward(statement, PsiWhiteSpace.class, PsiComment.class); /*todo*/
//        if(nextStatement instanceof PsiReturnStatement) return (PsiReturnStatement)nextStatement;
//        PsiElement parent = statement.getParent();
//        if(parent instanceof PsiCodeBlock) {
//            PsiStatement[] statements = ((PsiCodeBlock)parent).getStatements();
//            if(statements.length == 0 || statements[statements.length-1] != statement) return null;
//            parent = parent.getParent();
//            if(!(parent instanceof PsiBlockStatement)) return null;
//            parent = parent.getParent();
//        }
//        if(parent instanceof PsiIfStatement) return getNextReturnStatement((PsiStatement)parent);
//        return null;
//    }

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
