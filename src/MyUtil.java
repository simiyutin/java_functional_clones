import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.controlFlow.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.introduceParameter.AbstractJavaInplaceIntroducer;
import com.intellij.refactoring.introduceParameter.Util;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.ui.NameSuggestionsGenerator;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.*;
import com.intellij.util.ArrayUtil;
import one.util.streamex.StreamEx;

import java.util.*;

public class MyUtil {


    public static String getNameForParameter(PsiExpression expr, Project project) {

        String[] names = createNameSuggestionGenerator(expr, null, project, null).getSuggestedNameInfo(expr.getType()).names;

        if (names.length == 0) return null;

        return names[0];
    }

    private static NameSuggestionsGenerator createNameSuggestionGenerator(final PsiExpression expr,
                                                                          final String propName,
                                                                          final Project project, String enteredName) {
        return type -> {
            final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
            SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, propName, expr != null && expr.isValid() ? expr : null, type);

            if (expr != null && expr.isValid()) {
                info = codeStyleManager.suggestUniqueVariableName(info, expr, true);
            }
            final String[] strings = AbstractJavaInplaceIntroducer.appendUnresolvedExprName(JavaCompletionUtil
                    .completeVariableNameForRefactoring(codeStyleManager, type, VariableKind.LOCAL_VARIABLE, info), expr);
            return new SuggestedNameInfo.Delegate(enteredName != null ? ArrayUtil.mergeArrays(new String[]{enteredName}, strings) : strings, info);
        };
    }

    static void saltLocalVariables(PsiLambdaExpression expr, Project myProject) {

        final ControlFlow controlFlow;
        try {
            controlFlow = ControlFlowFactory.getInstance(myProject)
                    .getControlFlow(expr, LocalsOrMyInstanceFieldsControlFlowPolicy.getInstance());
        }
        catch (AnalysisCanceledException ignored) {
            return;
        }

        int startOffset = controlFlow.getStartOffset(expr);
        int endOffset = controlFlow.getEndOffset(expr);
        final List<PsiVariable> innerVariables = StreamEx.of(ControlFlowUtil.getUsedVariables(controlFlow, startOffset, endOffset))
                .remove(variable -> PsiTreeUtil.getParentOfType(variable, expr.getClass(), PsiClass.class) != expr)
                .append(expr.getParameterList().getParameters())
                .toList();

        if (innerVariables.isEmpty()) return;

        innerVariables.forEach(var -> {
            WriteCommandAction.runWriteCommandAction(myProject, () -> {
                String salt = UUID.randomUUID().toString().replace("-","");
                RefactoringUtil.renameVariableReferences(var, var.getName() + salt, new LocalSearchScope(expr), true);
                var.setName(var.getName() + salt);
            });
        });

    }

    // copy-paste
    static PsiMethod getContainingMethod(PsiExpression expr) {

        PsiMethod method = Util.getContainingMethod(expr);
        if (method == null) return null;

        final List<PsiMethod> methods = com.intellij.refactoring.introduceParameter.IntroduceParameterHandler.getEnclosingMethods(method);
        method = methods.get(0);

        return method;
    }

    // copy-paste
    public static Map<PsiMember, List<Match>> invokeOnScope(final Project project, final Set<PsiMember> members, final AnalysisScope scope) {
        final Map<PsiMember, List<Match>> duplicates = new HashMap<>();
        final int fileCount = scope.getFileCount();
        final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (progressIndicator != null) {
            progressIndicator.setIndeterminate(false);
        }

        final Map<PsiMember, Set<Module>> memberWithModulesMap = new HashMap<>();
        for (final PsiMember member : members) {
            final Module module = ApplicationManager.getApplication().runReadAction(new Computable<Module>() {
                @Override
                public Module compute() {
                    return ModuleUtilCore.findModuleForPsiElement(member);
                }
            });
            if (module != null) {
                final HashSet<Module> dependencies = new HashSet<>();
                ApplicationManager.getApplication().runReadAction(() -> ModuleUtilCore.collectModulesDependsOn(module, dependencies));
                memberWithModulesMap.put(member, dependencies);
            }
        }

        scope.accept(new PsiRecursiveElementVisitor() {
            private int myFileCount;
            @Override public void visitFile(final PsiFile file) {
                if (progressIndicator != null){
                    if (progressIndicator.isCanceled()) return;
                    progressIndicator.setFraction(((double)myFileCount++)/fileCount);
                    final VirtualFile virtualFile = file.getVirtualFile();
                    if (virtualFile != null) {
                        progressIndicator.setText2(ProjectUtil.calcRelativeToProjectPath(virtualFile, project));
                    }
                }
                final Module targetModule = ModuleUtilCore.findModuleForPsiElement(file);
                if (targetModule == null) return;
                for (Map.Entry<PsiMember, Set<Module>> entry : memberWithModulesMap.entrySet()) {
                    final Set<Module> dependencies = entry.getValue();
                    if (dependencies == null || !dependencies.contains(targetModule)) continue;

                    final PsiMember method = entry.getKey();
                    final List<Match> matchList = MethodDuplicatesHandler.hasDuplicates(file, method);
                    for (Iterator<Match> iterator = matchList.iterator(); iterator.hasNext(); ) {
                        Match match = iterator.next();
                        final PsiElement matchStart = match.getMatchStart();
                        final PsiElement matchEnd = match.getMatchEnd();
                        for (PsiMember psiMember : members) {
                            if (PsiTreeUtil.isAncestor(psiMember, matchStart, false) ||
                                    PsiTreeUtil.isAncestor(psiMember, matchEnd, false)) {
                                iterator.remove();
                                break;
                            }
                        }
                    }
                    if (!matchList.isEmpty()) {
                        List<Match> matches = duplicates.get(method);
                        if (matches == null) {
                            matches = new ArrayList<>();
                            duplicates.put(method, matches);
                        }
                        matches.addAll(matchList);
                    }
                }
            }
        });

        return duplicates;
    }

    public static List<PsiMethod> collapseFullDuplicates(List<Match> matches, PsiMember method, Project project) {

        List<PsiMethod> deletedMethods = new ArrayList<>();

        for (Match match : matches) {

            /*
             * method is subset of matchedMethod
             * if matchedMethod is subset of method, then they are equal
             *
             * this hack is needed because by default matches exclude return values,
             * but we need to handle them to be able to delete full duplicates
             */
            PsiMethod matchedMethod = Util.getContainingMethod(match.getMatchStart());
            if(matchedMethod.getContainingFile() != method.getContainingFile()) continue;
            final DuplicatesFinder duplicatesFinder = MethodDuplicatesHandler.createDuplicatesFinder(matchedMethod);
            List<Match> reverse = duplicatesFinder.findDuplicates(method);


            if (!reverse.isEmpty()) {
                final RenameProcessor renameProcessor = new MyRenameProcessor(project, matchedMethod, method.getName(), false, false);
                renameProcessor.run();

                WriteCommandAction.runWriteCommandAction(project, matchedMethod::delete);
                deletedMethods.add(matchedMethod);
            }

        }

        return deletedMethods;
    }
}
