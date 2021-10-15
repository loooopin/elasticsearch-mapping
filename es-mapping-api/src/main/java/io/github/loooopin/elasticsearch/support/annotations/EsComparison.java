package io.github.loooopin.elasticsearch.support.annotations;

import io.github.loooopin.elasticsearch.support.enums.ComparisonEnums;

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
public @interface EsComparison {
    ComparisonEnums value();
}
