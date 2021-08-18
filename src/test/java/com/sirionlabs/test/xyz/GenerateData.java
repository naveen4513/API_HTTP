package com.sirionlabs.test.xyz;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.UpdateConfigFiles;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.poi.ss.usermodel.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.util.*;

import org.apache.commons.lang3.*;

import static com.sirionlabs.utils.commonUtils.XLSUtils.getRowDataUsingColumnId;

class TestThread1 extends Thread
{
    // reference to Line's Object.
    private CustomTest customTest = new CustomTest();

    @Override
    public void run()
    {
        try {
            customTest.testCR();
        } catch (FileNotFoundException | ConfigurationException e) {
            e.printStackTrace();
        }
    }
}
class TestThread2 extends Thread
{
    // reference to Line's Object.
    private CustomTest customTest = new CustomTest();

    @Override
    public void run()
    {
        try {
            customTest.testOBSL();
        } catch (FileNotFoundException | ConfigurationException e) {
            e.printStackTrace();
        }
    }
}

class CustomTest
{
        void testCR() throws FileNotFoundException, ConfigurationException {
            final Logger logger = LoggerFactory.getLogger(GenerateData.class);

            String createApiResponse;
            int sStart=1431,sEnd=2029; //2030
            List<Integer> supplierList = new ArrayList<>();

            for(int i=sStart;i<=sEnd;i++){
                supplierList.add(i);
            }

            for(Integer integer:supplierList){

                ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/changeRequest", "changeRequest.cfg", "docusign cr", "sourceid", String.valueOf(integer));

                int changeRequestNum = RandomNumbers.getRandomNumberWithinRangeIndex(1,3);
                while (changeRequestNum-->0){
                    createApiResponse = ChangeRequest.createChangeRequest("flow 1", true);
                    if (createApiResponse==null)
                        break;
                    logger.info("Response {}", createApiResponse);
                }
            }
        }

        void testOBSL() throws FileNotFoundException, ConfigurationException {
            final Logger logger = LoggerFactory.getLogger(GenerateData.class);

            String createApiResponse;
            int cStart = 135929,cEnd=136648;
            List<Integer> contractList = new ArrayList<>();

            for(int i=cStart;i<=cEnd;i++){
                contractList.add(i);
            }

            for (Integer integer : contractList) {
                ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/Obligation", "obligation.cfg", "flow 1", "sourceid", String.valueOf(integer));
                ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/serviceLevel", "serviceLevel.cfg", "flow 1", "sourceid", String.valueOf(integer));


                int serviceLevelNum = RandomNumbers.getRandomNumberWithinRangeIndex(0,10);
                while (serviceLevelNum-->0){
                    createApiResponse = ServiceLevel.createServiceLevel("flow 1", true);
                    if (createApiResponse==null)
                        break;
                    logger.info("Response {}", createApiResponse);
                }

                int obligationNum = RandomNumbers.getRandomNumberWithinRangeIndex(0,10);
                while (obligationNum-->0){
                    createApiResponse = Obligations.createObligation("flow 1", true);
                    if (createApiResponse==null)
                        break;
                    logger.info("Response {}", createApiResponse);
                }

            }
        }
}

public class GenerateData {

    @Test(enabled = false)
    public void Generate() throws FileNotFoundException, ConfigurationException {
        final Logger logger = LoggerFactory.getLogger(GenerateData.class);

        String createApiResponse;
        int cStart = 135953,cEnd=136648;
        int sStart=1431,sEnd=2029; //2030
        List<Integer> contractList = new ArrayList<>();
        List<Integer> supplierList = new ArrayList<>();

        for(int i=cStart;i<=cEnd;i++){
            contractList.add(i);
        }
        for(int i=sStart;i<=sEnd;i++){
            supplierList.add(i);
        }


//        for(Integer integer:supplierList){
//
//            ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/changeRequest", "changeRequest.cfg", "docusign cr", "sourceid", String.valueOf(integer));
//
//            int changeRequestNum = RandomNumbers.getRandomNumberWithinRangeIndex(1,3);
//            while (changeRequestNum-->0){
//                createApiResponse = ChangeRequest.createChangeRequest("flow 1", true);
//                if (createApiResponse==null)
//                    break;
//                logger.info("Response {}", createApiResponse);
//            }
//        }

        for (Integer integer : contractList) {
            ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/Obligation", "obligation.cfg", "flow 1", "sourceid", String.valueOf(integer));
            ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/serviceLevel", "serviceLevel.cfg", "flow 1", "sourceid", String.valueOf(integer));


            int serviceLevelNum = RandomNumbers.getRandomNumberWithinRangeIndex(0,10);
            while (serviceLevelNum-->0){
                createApiResponse = ServiceLevel.createServiceLevel("flow 1", true);
                if (createApiResponse==null)
                    break;
                logger.info("Response {}", createApiResponse);
            }

            int obligationNum = RandomNumbers.getRandomNumberWithinRangeIndex(0,10);
            while (obligationNum-->0){
                createApiResponse = Obligations.createObligation("flow 1", true);
                if (createApiResponse==null)
                    break;
                logger.info("Response {}", createApiResponse);
            }

        }




//        TestThread1 testThread1 = new TestThread1();
//        testThread1.start();
//
//        TestThread2 testThread2 = new TestThread2();
//        testThread2.start();

        int supplierCount =300;
        while(supplierCount-->0){

            createApiResponse = Supplier.createSupplier("flow 1", true);
            logger.info("Response {}",createApiResponse);

            int contractNum = RandomNumbers.getRandomNumberWithinRangeIndex(1,12);

            ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/Contract","contract.cfg","fixed fee flow 1 temp","sourceid",String.valueOf(CreateEntity.getNewEntityId(createApiResponse, "3")));

            while(contractNum-- > 0){
                createApiResponse = Contract.createContract("fixed fee flow 1 temp",true);
                logger.info("Response {}",createApiResponse);

                ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/Obligation", "obligation.cfg", "flow 1", "sourceid", String.valueOf(CreateEntity.getNewEntityId(createApiResponse, "61")));
                ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/serviceLevel", "serviceLevel.cfg", "flow 1", "sourceid", String.valueOf(CreateEntity.getNewEntityId(createApiResponse, "61")));
                ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/changeRequest", "changeRequest.cfg", "docusign cr", "sourceid", String.valueOf(CreateEntity.getNewEntityId(createApiResponse, "61")));


                int obligationNum = RandomNumbers.getRandomNumberWithinRangeIndex(0,5);

                while (obligationNum-->0){
                    createApiResponse = Obligations.createObligation("flow 1", true);
                    if (createApiResponse==null)
                        break;
                    logger.info("Response {}", createApiResponse);
                }

                int serviceLevelNum = RandomNumbers.getRandomNumberWithinRangeIndex(0,5);
                while (serviceLevelNum-->0){
                    createApiResponse = ServiceLevel.createServiceLevel("flow 1", true);
                    if (createApiResponse==null)
                        break;
                    logger.info("Response {}", createApiResponse);
                }
            }


//            ParseConfigFile.updateValueInConfigFile("src/test/resources/Helper/EntityCreation/changeRequest", "changeRequest.cfg", "docusign cr", "sourceid", String.valueOf(CreateEntity.getNewEntityId(createApiResponse, "3")));
//
//            int changeRequestNum = RandomNumbers.getRandomNumberWithinRangeIndex(2,10);
//            while (changeRequestNum-->0){
//                createApiResponse = ChangeRequest.createChangeRequest("flow 1", true);
//                if (createApiResponse==null)
//                    break;
//                logger.info("Response {}", createApiResponse);
//            }

        }



    }

    @Test(enabled = true)
    public void GetExcelData(){
        Map<String,String> dataMap = XLSUtils.getRowDataUsingColumnId("src/test/resources/TestConfig/InvoiceLineItem/BulkCreate/DataFiles","TC-98035_1.xlsm","Invoice Line Item",6);
        System.out.println(dataMap);
    }


}
