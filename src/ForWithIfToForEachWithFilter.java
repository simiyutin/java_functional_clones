import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.util.Consumer;
import com.intellij.util.FileContentUtil;
import com.intellij.util.containers.Predicate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ForWithIfToForEachWithFilter extends AnAction {

    public void actionPerformed(AnActionEvent event) {
        //PsiFile file = event.getData(LangDataKeys.PSI_FILE);
        //PsiMethod main = (PsiMethod) file.findElementAt(6);

        final DataContext dataContext = event.getDataContext();
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        Project project = event.getData(PlatformDataKeys.PROJECT);


        PsiVisitor visitor = new PsiVisitor();

        file.accept(visitor);

        if(true)
        return;

        PsiElement[] test3 = file.getChildren();

        PsiClass mainClass = (PsiClass) test3[3];
        PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();


        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiComment comment = psiElementFactory.createCommentFromText("//asasasasas", file);
            file.addBefore(comment, file.getFirstChild());

            PsiElement[] classMainChildren = test3[3].getChildren();
            PsiMethod method = (PsiMethod) classMainChildren[11];
            method.setName("notMain");

        });

        String txt = Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());

    }
}




/*
*
*
*
*
* Пример исползования Visitor'а:
*   public static boolean variableIsAssigned(
    @NotNull PsiVariable variable, @Nullable PsiElement context) {
    if (context == null) {
      return false;
    }
    //Создаем визитора
    final VariableAssignedVisitor visitor =
      new VariableAssignedVisitor(variable, true);

    пихаем визитора в дерево, для прохода по всем элементам
    context.accept(visitor);
    // если на каком-то элементе переменную переприсвоили, то у него поменяется флаг isAssigned
    return visitor.isAssigned();
  }

  */