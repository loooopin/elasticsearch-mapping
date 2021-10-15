package io.github.loooopin.elasticsearch.support.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:22
 * Description:
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EsField {
    String value();
    //是否仅用于比较，不用于字段映射
    boolean onlyCompare() default false;
}
