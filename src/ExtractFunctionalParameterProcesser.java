import com.intellij.openapi.project.Project;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Created by boris on 22.11.16.
 */
public class ExtractFunctionalParameterProcesser extends BaseRefactoringProcessor {

    public ExtractFunctionalParameterProcesser(@NotNull Project project) {
        super(project);
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        return null;
    }

    @NotNull
    @Override
    protected UsageInfo[] findUsages() {
        return new UsageInfo[0];
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo[] usages) {

    }

    @Override
    protected String getCommandName() {
        return null;
    }


}
