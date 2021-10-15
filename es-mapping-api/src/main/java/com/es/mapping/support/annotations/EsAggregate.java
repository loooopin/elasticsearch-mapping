package com.es.mapping.support.annotations;

import com.es.mapping.support.enums.AggregateEnums;

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
