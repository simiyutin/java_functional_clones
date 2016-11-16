import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.QuickFix;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;

import java.util.List;

public class ForWithIfToForEachWithFilter extends AnAction {

    public void actionPerformed(AnActionEvent event) {

        PsiFile file = event.getData(LangDataKeys.PSI_FILE);
        Project project = event.getData(PlatformDataKeys.PROJECT);

        InspectionManager manager = new InspectionManagerEx(project);
        ProblemsHolder holder = new ProblemsHolder(manager ,file, true);
        PsiElementVisitor elementVisitor = new StreamApiMigrationInspection().buildVisitor(holder, true);



//        List<Divider.DividedElements> allDivided = new ArrayList<>();
//        Divider.divideInsideAndOutsideAllRoots(myFile, myRestrictRange, myPriorityRange, SHOULD_INSPECT_FILTER, new CommonProcessors.CollectProcessor<>(allDivided));
//        List<PsiElement> elements = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> d.inside));

        PsiVisitor collector = new PsiVisitor();
        file.accept(collector);
        List<PsiElement> elements = collector.getAllElements();

        for (int i = 0, elementsSize = elements.size(); i < elementsSize; i++) {
            PsiElement element = elements.get(i);
            element.accept(elementVisitor);
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (ProblemDescriptor descriptor : holder.getResults()) {
                        QuickFix[] fixes = descriptor.getFixes();
                        fixes[0].applyFix(project, descriptor);
                    }
        });





        //PsiFile file = event.getData(LangDataKeys.PSI_FILE);
        //PsiMethod main = (PsiMethod) file.findElementAt(6);

//        final DataContext dataContext = event.getDataContext();
//        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
//        Project project = event.getData(PlatformDataKeys.PROJECT);
//
//
//        PsiVisitor visitor = new PsiVisitor();
//
//        file.accept(visitor);
//
//        if(true)
//        return;
//
//        PsiElement[] test3 = file.getChildren();
//
//        PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
//
//
//        WriteCommandAction.runWriteCommandAction(project, () -> {
//            PsiComment comment = psiElementFactory.createCommentFromText("//asasasasas", file);
//            file.addBefore(comment, file.getFirstChild());
//
//            PsiElement[] classMainChildren = test3[3].getChildren();
//            PsiMethod method = (PsiMethod) classMainChildren[11];
//            method.setName("notMain");
//
//        });
//
//        String txt = Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
//        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());

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