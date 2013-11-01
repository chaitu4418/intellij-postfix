package org.jetbrains.postfixCompletion.Infrastructure;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PostfixTemplatesManager implements ApplicationComponent {
  @NotNull private final List<TemplateProviderInfo> myProviders;

  public PostfixTemplatesManager(@NotNull final PostfixTemplateProvider[] providers) {
    myProviders = new ArrayList<>();

    for (PostfixTemplateProvider provider : providers) {
      final TemplateProvider annotation =
        provider.getClass().getAnnotation(TemplateProvider.class);
      if (annotation != null)
        myProviders.add(new TemplateProviderInfo(provider, annotation));
    }
  }

  private static class TemplateProviderInfo {
    @NotNull public final PostfixTemplateProvider provider;
    @NotNull public final TemplateProvider annotation;

    public TemplateProviderInfo(@NotNull final PostfixTemplateProvider provider,
                                @NotNull final TemplateProvider annotation) {
      this.provider = provider;
      this.annotation = annotation;
    }
  }

  @NotNull public final List<LookupElement> getAvailableTemplates(
    @NotNull final PsiElement positionElement, boolean forceMode) {

    if (positionElement instanceof PsiIdentifier) {
      final PsiReferenceExpression referenceExpression =
        PsiTreeUtil.getParentOfType(positionElement, PsiReferenceExpression.class);
      if (referenceExpression == null) return Collections.emptyList();

      // easy case: 'expr.postfix'
      final PsiExpression qualifier = referenceExpression.getQualifierExpression();
      if (qualifier != null) {
        return collectPostfixTemplates(referenceExpression, qualifier, forceMode);
      }

      // hard case: 'x > 0.if' (two expression statements, broken literal)
      if (referenceExpression.getFirstChild() instanceof PsiReferenceParameterList &&
          referenceExpression.getLastChild() == referenceExpression) {
        final PsiExpressionStatement statement =
          PsiTreeUtil.getParentOfType(referenceExpression, PsiExpressionStatement.class);
        if (statement == null) return Collections.emptyList();

        // todo: will it handle 'a instanceof T.if' - ES;Error;ES;?

        final PsiStatement prevStatement =
          PsiTreeUtil.getPrevSiblingOfType(statement, PsiStatement.class);
        if (!(prevStatement instanceof PsiExpressionStatement))
          return Collections.emptyList();

        final PsiElement lastErrorChild = prevStatement.getLastChild();
        if (lastErrorChild instanceof PsiErrorElement) {
          PsiExpression expression = ((PsiExpressionStatement) prevStatement).getExpression();
          if (prevStatement.getFirstChild() == expression &&
              lastErrorChild.getPrevSibling() == expression) {

            PsiLiteralExpression brokenLiteral = null;
            do {
              // look for double literal broken by dot at end
              if (expression instanceof PsiLiteralExpression) {
                final PsiJavaToken token = PsiTreeUtil.getChildOfType(expression, PsiJavaToken.class);
                if (token != null
                    && token.getTokenType() == JavaTokenType.DOUBLE_LITERAL
                    && token.getText().endsWith(".")) {
                  brokenLiteral = (PsiLiteralExpression) expression;
                  break;
                }
              }

              // skip current expression and look its last inner expression
              final PsiElement last = expression.getLastChild();
              if (last instanceof PsiExpression) expression = (PsiExpression) last;
              else expression = PsiTreeUtil.getPrevSiblingOfType(last, PsiExpression.class);
            } while (expression != null);

            if (brokenLiteral != null) {
              return collectPostfixTemplates(referenceExpression, brokenLiteral, forceMode);
            }
          }
        }
      }
    }

    return Collections.emptyList();
  }

  @NotNull private List<LookupElement> collectPostfixTemplates(
    @NotNull final PsiReferenceExpression reference,
    @NotNull final PsiExpression expression, final boolean forceMode) {

    final PostfixTemplateAcceptanceContext context =
      new PostfixTemplateAcceptanceContext(reference, expression, forceMode);

    final List<LookupElement> elements = new ArrayList<>();

    for (final TemplateProviderInfo providerInfo : myProviders) {
      providerInfo.provider.createItems(context, elements);
    }

    return elements;
  }

  @Override
  public void initComponent() { }

  @Override
  public void disposeComponent() { }

  @NotNull
  @Override
  public String getComponentName() {
    return PostfixTemplatesManager.class.getTypeName();
  }
}