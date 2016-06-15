package de.jakobjarosch.rethinkdb.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Index {

    String[] fields();

    boolean geo() default false;
}

