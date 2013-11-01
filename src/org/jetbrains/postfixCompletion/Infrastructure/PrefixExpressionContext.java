package org.jetbrains.postfixCompletion.Infrastructure;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PrefixExpressionContext {
  @NotNull public final PostfixTemplateAcceptanceContext parentContext;
  @NotNull public final PsiExpression expression;
  @Nullable public final PsiType expressionType;
  public final boolean canBeStatement;

  public PrefixExpressionContext(@NotNull final PostfixTemplateAcceptanceContext parentContext,
                                 @NotNull final PsiExpression expression) {
    this.parentContext = parentContext;
    this.expression = expression;
    expressionType = expression.getType();
    canBeStatement = calculateCanBeStatement(expression);
  }

  private final boolean calculateCanBeStatement(@NotNull final PsiExpression expression) {
    // look for expression-statement parent
    final PsiExpressionStatement expressionStatement =
      PsiTreeUtil.getParentOfType(expression, PsiExpressionStatement.class);
    if (expressionStatement != null) return true;

    return false;
  }
}