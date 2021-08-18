package com.sirionlabs.test.clientAdmin;


import com.sirionlabs.helper.APITesting.TestGetApi;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.testng.annotations.Test;


import java.util.HashMap;
import java.util.Map;

//SIR-2755
public class TestFieldProvisioningAdminApis extends TestGetApi {

    private Map<String,String> parameters = new HashMap<>();
    private String path="";

    TestFieldProvisioningAdminApis(){
        super("TestFieldProvisioningAdminApisConfig.cfg","src/test/resources/TestConfig/clientAdmin");
        System.out.println("In base constructor");
    }

//    @Test
//    public void validateHTTPStatus(){
//        System.out.println("In test method child class");
//    }

//    @Override
//    @Test
//    public void negativeFlow(CustomAssert customAssert,String sectionName){
//
//    }

}
