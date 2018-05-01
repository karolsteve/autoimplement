package com.steve.mvp.autoimplement;


import java.lang.reflect.Constructor;

/**
 * Created by Steve Tchatchouang
 */

public class AutoImplUtils {

    private static final String SUFFIX = "_Impl";

    private AutoImplUtils() {
    }

    public static <T> T build(Class<T> klass, Class[] constructorsType, Object[] constructorParams) {
        AutoImplBuilder<T> builder = new AutoImplBuilder<>(klass, constructorsType, constructorParams);
        return builder.build();
    }

    private static class AutoImplBuilder<T> {
        private final Class[]  constructorType;
        private final Object[] constructorParams;
        private final Class<T> klass;
        private final boolean  shouldUseDefaultConstructor;

        AutoImplBuilder(Class<T> klass, Class[] constructorsType, Object[] constructorParams) {
            this.klass = klass;
            this.constructorParams = constructorParams;
            this.constructorType = constructorsType;
            shouldUseDefaultConstructor = constructorsType == null || constructorsType.length == 0;
        }

        private T build() {
            try {
                if (shouldUseDefaultConstructor) {
                    Class<?> aClass = Class.forName(klass.getCanonicalName() + SUFFIX);
                    return (T) aClass.newInstance();
                } else {
                    Class<?> aClass = Class.forName(klass.getCanonicalName() + SUFFIX);
                    Constructor constructor = aClass.getConstructor(constructorType);
                    return (T) constructor.newInstance(constructorParams);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
