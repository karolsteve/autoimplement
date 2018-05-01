package com.steve.mvp.autoimplement.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Steve Tchatchouang on 27/03/2018
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ImplementationOf {
    Class value();
}
