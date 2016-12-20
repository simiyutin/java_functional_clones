import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.introduceParameter.AbstractJavaInplaceIntroducer;
import com.intellij.refactoring.ui.NameSuggestionsGenerator;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.util.ArrayUtil;
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
            //TODO DuplicatesFinder.matchReturnStatement
        }
        return false;
    }

    public static String getNameForParameter(PsiExpression expr, Project project) {

        String[] names = createNameSuggestionGenerator(expr, null, project, null).getSuggestedNameInfo(expr.getType()).names;

        return names[0];
    }

    private static NameSuggestionsGenerator createNameSuggestionGenerator(final PsiExpression expr,
                                                                          final String propName,
                                                                          final Project project, String enteredName) {
        return new NameSuggestionsGenerator() {
            public SuggestedNameInfo getSuggestedNameInfo(PsiType type) {
                final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
                SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, propName, expr != null && expr.isValid() ? expr : null, type);

                if (expr != null && expr.isValid()) {
                    info = codeStyleManager.suggestUniqueVariableName(info, expr, true);
                }
                final String[] strings = AbstractJavaInplaceIntroducer.appendUnresolvedExprName(JavaCompletionUtil
                        .completeVariableNameForRefactoring(codeStyleManager, type, VariableKind.LOCAL_VARIABLE, info), expr);
                return new SuggestedNameInfo.Delegate(enteredName != null ? ArrayUtil.mergeArrays(new String[]{enteredName}, strings) : strings, info);
            }

        };
    }
}
