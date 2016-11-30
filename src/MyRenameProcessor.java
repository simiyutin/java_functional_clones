import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
public class MyRenameProcessor extends RenameProcessor {

    public MyRenameProcessor(Project project,
                           @NotNull PsiElement element,
                           @NotNull @NonNls String newName,
                           boolean isSearchInComments,
                           boolean isSearchTextOccurrences) {
        super(project, element, newName, isSearchInComments, isSearchTextOccurrences);
    }

    @Override
    public boolean preprocessUsages(@NotNull final Ref<UsageInfo[]> refUsages) {

        prepareSuccessful();
        return PsiElementRenameHandler.canRename(myProject, null, null);
    }
}
