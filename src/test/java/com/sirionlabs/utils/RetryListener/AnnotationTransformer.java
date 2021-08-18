package com.sirionlabs.utils.RetryListener;

import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(MyRetryAnalyzer.class);
        IRetryAnalyzer retry = annotation.getRetryAnalyzer();

        if(retry==null){
            annotation.setRetryAnalyzer(MyRetryAnalyzer.class);
        }
    }
}
