package com.steve.mvp.autoimplement.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Steve Tchatchouang on 24/03/2018
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AutoImplement {
}
