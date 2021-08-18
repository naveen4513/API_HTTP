package com.sirionlabs.listeners;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.xml.XmlClass;
import java.util.ArrayList;
import java.util.List;

public class MethodInterceptor implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> list, ITestContext iTestContext) {
        List<IMethodInstance> result = new ArrayList<>();

        List<XmlClass> classes = iTestContext.getCurrentXmlTest().getXmlClasses();
        for(int i=0;i<classes.size();i++) {
            if (classes.get(i).getName().contains(ClassesToExclude.TestAutoExtractionAlgorithms.name())) {
                if (iTestContext.getCurrentXmlTest().getAllParameters().get("Environment").equals(EnvironmentToSkip.autoextraction_sandbox.name())) {
                    for (IMethodInstance m : list) {
                        m.getMethod().getConstructorOrMethod().setEnabled(false);
                    }
                } else {
                    for (IMethodInstance m : list) {
                        if(m.getMethod().getTestClass().getName().equals(classes.get(i).getName())) {
                            result.add(m);
                        }
                    }
                }
            } else {
                for (IMethodInstance m : list) {
                    if(m.getMethod().getTestClass().getName().equals(classes.get(i).getName())){
                        result.add(m);
                    }
                }
            }
        }
        return result;
    }
}
