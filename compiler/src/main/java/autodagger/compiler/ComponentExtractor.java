package autodagger.compiler;

import com.google.auto.common.MoreElements;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Scope;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import autodagger.AutoComponent;
import processorworkflow.AbstractExtractor;
import processorworkflow.Errors;
import processorworkflow.ExtractorUtils;

/**
 * @author Lukasz Piliszczuk - lukasz.pili@gmail.com
 */
public class ComponentExtractor extends AbstractExtractor {

    static final String ANNOTATION_DEPENDENCIES = "dependencies";
    static final String ANNOTATION_MODULES = "modules";
    static final String ANNOTATION_TARGET = "target";
    static final String ANNOTATION_SUPERINTERFACES = "superinterfaces";
    static final String ANNOTATION_FROM_TEMPLATE = "fromTemplate";

    /**
     * The component element represented by @AutoComponent
     * It's either the element itself, or the element of an annotation if the @AutoComponent
     * is applied on the annotation
     */
    private final Element componentElement;

    private TypeMirror targetTypeMirror;
    private TypeMirror fromTemplateTypeMirror;
    private List<TypeMirror> dependenciesTypeMirrors;
    private List<TypeMirror> modulesTypeMirrors;
    private List<TypeMirror> superinterfacesTypeMirrors;
    private AnnotationMirror scopeAnnotationTypeMirror;

    public ComponentExtractor(Element componentElement, Element element, Types types, Elements elements, Errors errors) {
        super(element, types, elements, errors);
        this.componentElement = componentElement;

        extract();
    }

    @Override
    public void extract() {
        targetTypeMirror = ExtractorUtils.getValueFromAnnotation(element, AutoComponent.class, ANNOTATION_TARGET);
        if (targetTypeMirror == null) {
            targetTypeMirror = componentElement.asType();
        }

        fromTemplateTypeMirror = ExtractorUtils.getValueFromAnnotation(element, AutoComponent.class, ANNOTATION_FROM_TEMPLATE);
        dependenciesTypeMirrors = findTypeMirrors(element, ANNOTATION_DEPENDENCIES);
        modulesTypeMirrors = findTypeMirrors(element, ANNOTATION_MODULES);
        superinterfacesTypeMirrors = findTypeMirrors(element, ANNOTATION_SUPERINTERFACES);
        scopeAnnotationTypeMirror = findScope();

        if (fromTemplateTypeMirror != null &&
                (!dependenciesTypeMirrors.isEmpty() || !modulesTypeMirrors.isEmpty() || !superinterfacesTypeMirrors.isEmpty())) {
            errors.addInvalid("Cannot have fromTemplate with dependencies/superinterfaces/modules at the same time");
        }
    }

    private List<TypeMirror> findTypeMirrors(Element element, String name) {
        List<TypeMirror> typeMirrors = new ArrayList<>();
        List<AnnotationValue> values = ExtractorUtils.getValueFromAnnotation(element, AutoComponent.class, name);
        if (values != null) {
            for (AnnotationValue value : values) {
                if (!validateAnnotationValue(value, name)) {
                    continue;
                }

                try {
                    TypeMirror tm = (TypeMirror) value.getValue();
                    typeMirrors.add(tm);
                } catch (Exception e) {
                    errors.addInvalid(e.getMessage());
                    break;
                }
            }
        }

        return typeMirrors;
    }

    /**
     * Find annotation that is itself annoted with @Scope
     * If there is one, it will be later applied on the generated component
     * Otherwise the component will be unscoped
     * Throw error if more than one scope annotation found
     */
    private AnnotationMirror findScope() {
        // first look on the @AutoComponent annotated element
        AnnotationMirror annotationMirror = findScope(element);
        if (annotationMirror == null && element != componentElement) {
            // look also on the real component element, if @AutoComponent is itself on
            // an another annotation
            annotationMirror = findScope(componentElement);
        }

        return annotationMirror;
    }

    private AnnotationMirror findScope(Element element) {
        AnnotationMirror annotationTypeMirror = null;

        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            if (MoreElements.isAnnotationPresent(annotationElement, Scope.class)) {
                // already found one scope
                if (annotationTypeMirror != null) {
                    errors.getParent().addInvalid(element, "Class annotated with @AutoComponent cannot have several scopes (@Scope).");
                    continue;
                }

                annotationTypeMirror = annotationMirror;
            }
        }

        return annotationTypeMirror;
    }

    private boolean validateAnnotationValue(AnnotationValue value, String member) {
        if (!(value.getValue() instanceof TypeMirror)) {
            errors.addInvalid(String.format("%s cannot reference generated class. Use the class that applies the @AutoComponent annotation.", member));
            return false;
        }

        return true;
    }

    public Element getComponentElement() {
        return componentElement;
    }

    public TypeMirror getTargetTypeMirror() {
        return targetTypeMirror;
    }

    public List<TypeMirror> getDependenciesTypeMirrors() {
        return dependenciesTypeMirrors;
    }

    public List<TypeMirror> getModulesTypeMirrors() {
        return modulesTypeMirrors;
    }

    public List<TypeMirror> getSuperinterfacesTypeMirrors() {
        return superinterfacesTypeMirrors;
    }

    public AnnotationMirror getScopeAnnotationTypeMirror() {
        return scopeAnnotationTypeMirror;
    }
}