package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.masterListOptions.CreateMasterListOptions;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class TestMasterListOptionCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestMasterListOptionCreation.class);
    @Test
    public void TestMasterOptionListCreation(){

        CustomAssert customAssert = new CustomAssert();

        try{

            CreateMasterListOptions createMasterListOptions  = new CreateMasterListOptions();

            String payload;
            String masterOptionResponse;

            int optionNumber = 1;
            int orderSeq = 56608;
            int masterListId = 1009;

            for(int i =0;i<1000;i++) {
                payload = "{\"masterListId\":" + masterListId + ",\"options\":{\"-1\":{\"name\":\"Option" + optionNumber + "\",\"orderSeq\":" + orderSeq + ",\"active\":true}}}";
                masterOptionResponse = createMasterListOptions.hitCreateMasterListOptions(payload);

                if(!masterOptionResponse.contains("Success")){

                    customAssert.assertTrue(false,"Error while creating master option");
                }
                orderSeq = orderSeq + 1;
                optionNumber = optionNumber + 1;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while creating ");
        }

        customAssert.assertAll();

    }

}
