package com.sirionlabs.test.invoice.InvoiceCopyViewer;

import com.sirionlabs.helper.APITesting.TestGetApi;

import java.util.HashMap;
import java.util.Map;

//SIR-4016
public class InvoiceCopyViewerAPIs extends TestGetApi {

    private Map<String,String> parameters = new HashMap<>();
    private String path="";

    //Covered test cases C141137,C141139
    InvoiceCopyViewerAPIs(){
        super("TestInvoiceCopyViewerAPIs.cfg","src/test/resources/TestConfig/Invoice/InvoiceCopyViewer");
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