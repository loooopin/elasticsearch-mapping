package io.github.loooopin.elasticsearch.support.annotations;

import io.github.loooopin.elasticsearch.support.enums.AggregateEnums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Deprecated
public @interface EsAggregate {
    AggregateEnums value();
}
