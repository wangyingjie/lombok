package de.plushnikov.intellij.plugin.processor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import de.plushnikov.intellij.plugin.lombokconfig.ConfigDiscovery;
import de.plushnikov.intellij.plugin.lombokconfig.ConfigKey;
import de.plushnikov.intellij.plugin.processor.field.AccessorsInfo;
import de.plushnikov.intellij.plugin.thirdparty.LombokUtils;
import de.plushnikov.intellij.plugin.util.LombokProcessorUtil;
import de.plushnikov.intellij.plugin.util.PsiAnnotationSearchUtil;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Base lombok processor class
 *
 * @author Plushnikov Michail
 */
public abstract class AbstractProcessor implements Processor {
  /**
   * Anntotation classes this processor supports
   */
  @NotNull
  private final Class<? extends Annotation>[] supportedAnnotationClasses;
  /**
   * Kind of output elements this processor supports
   */
  @NotNull
  private final Class<? extends PsiElement> supportedClass;

  /**
   * Constructor for all Lombok-Processors
   *
   * @param supportedClass              kind of output elements this processor supports
   * @param supportedAnnotationClass    annotation this processor supports
   * @param equivalentAnnotationClasses any other aquivalent annotations
   */
  protected AbstractProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                              @NotNull Class<? extends Annotation> supportedAnnotationClass,
                              @NotNull Class<? extends Annotation>... equivalentAnnotationClasses) {
    this.supportedClass = supportedClass;
    this.supportedAnnotationClasses = prepend(supportedAnnotationClass, equivalentAnnotationClasses);
  }

  @NotNull
  private static Class<? extends Annotation>[] prepend(@NotNull Class<? extends Annotation> element, @NotNull Class<? extends Annotation>... array) {
    int length = array.length;
    Class<? extends Annotation>[] result = new Class[length + 1];
    result[0] = element;
    System.arraycopy(array, 0, result, 1, length);
    return result;
  }

  @NotNull
  public final Class<? extends Annotation>[] getSupportedAnnotationClasses() {
    return supportedAnnotationClasses;
  }

  @NotNull
  @Override
  public final Class<? extends PsiElement> getSupportedClass() {
    return supportedClass;
  }

  @Override
  public boolean isEnabled(@NotNull PropertiesComponent propertiesComponent) {
    return true;
  }

  @Override
  public boolean isShouldGenerateFullBodyBlock() {
    return ShouldGenerateFullCodeBlock.getInstance().isStateActive();
  }

  @NotNull
  public List<? super PsiElement> process(@NotNull PsiClass psiClass) {
    return Collections.emptyList();
  }

  @NotNull
  public abstract Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass);

  protected String getGetterName(final @NotNull PsiField psiField) {
    final AccessorsInfo accessorsInfo = AccessorsInfo.build(psiField);

    final String psiFieldName = psiField.getName();
    final boolean isBoolean = PsiType.BOOLEAN.equals(psiField.getType());

    return LombokUtils.toGetterName(accessorsInfo, psiFieldName, isBoolean);
  }

  protected void filterToleratedElements(@NotNull Collection<? extends PsiModifierListOwner> definedMethods) {
    final Iterator<? extends PsiModifierListOwner> methodIterator = definedMethods.iterator();
    while (methodIterator.hasNext()) {
      PsiModifierListOwner definedMethod = methodIterator.next();
      if (PsiAnnotationSearchUtil.isAnnotatedWith(definedMethod, Tolerate.class)) {
        methodIterator.remove();
      }
    }
  }

  public static boolean readAnnotationOrConfigProperty(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass,
                                                       @NotNull String annotationParameter, @NotNull ConfigKey configKey) {
    final boolean result;
    final Boolean declaredAnnotationValue = PsiAnnotationUtil.getDeclaredBooleanAnnotationValue(psiAnnotation, annotationParameter);
    if (null == declaredAnnotationValue) {
      result = ConfigDiscovery.getInstance().getBooleanLombokConfigProperty(configKey, psiClass);
    } else {
      result = declaredAnnotationValue;
    }
    return result;
  }

  protected static void addOnXAnnotations(@Nullable PsiAnnotation processedAnnotation,
                                          @NotNull PsiModifierList modifierList,
                                          @NotNull String onXParameterName) {
    if (processedAnnotation == null) {
      return;
    }

    Collection<String> annotationsToAdd = LombokProcessorUtil.getOnX(processedAnnotation, onXParameterName);
    for (String annotation : annotationsToAdd) {
      modifierList.addAnnotation(annotation);
    }
  }

  public LombokPsiElementUsage checkFieldUsage(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation) {
    return LombokPsiElementUsage.NONE;
  }

}
