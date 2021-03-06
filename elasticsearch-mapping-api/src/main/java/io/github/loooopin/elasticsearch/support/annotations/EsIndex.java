package io.github.loooopin.elasticsearch.support.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:24
 * Description:
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EsIndex {
    String value();

    String refProps() default "application.properties";
}
