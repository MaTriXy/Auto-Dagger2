package autodagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Lukasz Piliszczuk <lukasz.pili@gmail.com>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AutoComponent {

    Class<?>[] modules() default {};

    Class<?>[] dependencies() default {};

    Class<?>[] superinterfaces() default {};

    /**
     * Default is the class on which the @AutoComponent annotation is applied
     */
    Class<?> target() default void.class;
}