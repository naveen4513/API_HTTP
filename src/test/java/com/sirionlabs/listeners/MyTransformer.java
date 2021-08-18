package com.sirionlabs.listeners;

import org.testng.*;
import org.testng.annotations.ITestAnnotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MyTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation iTestAnnotation, Class aClass, Constructor constructor, Method method) {
        if (method.getDeclaringClass().getName().contains(ClassesToExclude.TestAutoExtractionAlgorithms.name())) {
                iTestAnnotation.setEnabled(false);
        }
    }
}
