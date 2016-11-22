import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiExpression;
import com.intellij.refactoring.introduceParameter.IntroduceParameterHandler;
public class MyIntroduceParameterHandler extends IntroduceParameterHandler {

    public boolean invoke(final Editor editor, final Project project, final PsiExpression expr){
        return invokeImpl(project, expr, editor);
    };
}
