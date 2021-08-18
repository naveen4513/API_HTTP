package com.sirionlabs.test;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class test_SIR133409 {

    private final static Logger logger = LoggerFactory.getLogger(test_SIR133409.class);
    private String configFilePath;
    private String configFileName;
    private int cslEntityTypeId = 15;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("testSIR133409ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("testSIR133409ConfigFileName");
    }

    @DataProvider
    public Object[][] bulkEditDataProvider(){

        logger.info("Creating flows for bulk edit numerator/denominator flow");

        List<Object[]> allTestData = new ArrayList<>();

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkeditflowstotest").split(",");
        HashMap<String,String> numDenominatorValues;
        String supplierNumerator;
        String supplierDenominator;
        String finalNumerator;
        String finalDenominator;
        String actualNumerator;
        String actualDenominator;

        Integer entityIdToTest = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkeditchildslid"));

        for(String flow : flowsToTest){
            numDenominatorValues = new HashMap<>();
            supplierNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"suppliernumerator");
            supplierDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"supplierdenominator");
            finalNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"finalnumerator");
            finalDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"finaldenominator");
            actualNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"actualnumerator");
            actualDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"actualdenominator");

            numDenominatorValues.put("supplierNumerator",supplierNumerator);
            numDenominatorValues.put("supplierDenominator",supplierDenominator);
            numDenominatorValues.put("finalNumerator",finalNumerator);
            numDenominatorValues.put("finalDenominator",finalDenominator);
            numDenominatorValues.put("actualNumerator",actualNumerator);
            numDenominatorValues.put("actualDenominator",actualDenominator);

            allTestData.add(new Object[]{flow,entityIdToTest,numDenominatorValues});
        }


        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] bulkUpdateDataProvider(){

        logger.info("Creating flows for bulk update numerator/denominator flow");

        List<Object[]> allTestData = new ArrayList<>();

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkupdateflowstotest").split(",");
        HashMap<String,String> numDenominatorValues;
        String supplierNumerator;
        String supplierDenominator;
        String finalNumerator;
        String finalDenominator;
        String actualNumerator;
        String actualDenominator;

        Integer entityIdToTest = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkupdatechildslid"));

        for(String flow : flowsToTest){
            numDenominatorValues = new HashMap<>();
            supplierNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"suppliernumerator");
            supplierDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"supplierdenominator");
            finalNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"finalnumerator");
            finalDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"finaldenominator");
            actualNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"actualnumerator");
            actualDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flow,"actualdenominator");

            numDenominatorValues.put("supplierNumerator",supplierNumerator);
            numDenominatorValues.put("supplierDenominator",supplierDenominator);
            numDenominatorValues.put("finalNumerator",finalNumerator);
            numDenominatorValues.put("finalDenominator",finalDenominator);
            numDenominatorValues.put("actualNumerator",actualNumerator);
            numDenominatorValues.put("actualDenominator",actualDenominator);

            allTestData.add(new Object[]{flow,entityIdToTest,numDenominatorValues});
        }


        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "bulkEditDataProvider",enabled = true)
    public void testValidatePerformanceOnCSLBulkEdit(String flowToTest,int entityIdToTest,HashMap<String,String> numDenominatorValues){

        logger.info("Validating Performance Fields On CSL through BulkEdit for the flow {}",flowToTest);
        CustomAssert csAssert = new CustomAssert();

        String supplierNumerator = numDenominatorValues.get("supplierNumerator");
        String supplierDenominator = numDenominatorValues.get("supplierDenominator");
        String finalNumerator = numDenominatorValues.get("finalNumerator");
        String finalDenominator = numDenominatorValues.get("finalDenominator");
        String actualNumerator = numDenominatorValues.get("actualNumerator");
        String actualDenominator = numDenominatorValues.get("actualDenominator");

        String bulkEditPayload = "{\"body\": {\"data\": {" +
                "\"supplierNumerator\": {\"name\": \"supplierNumerator\",\"id\": 1106,\"values\": \"" + supplierNumerator +"\"}," +
                "\"supplierDenominator\": {\"name\": \"supplierDenominator\",\"id\": 1107,\"values\": \"" + supplierDenominator +"\"}," +
                "\"finalNumerator\": {\"name\": \"finalNumerator\",\"id\": 1110,\"values\": \"" + finalNumerator +"\"}," +
                "\"finalDenominator\": {\"name\": \"finalDenominator\",\"id\": 1111,\"values\": \"" + finalDenominator +"\"}," +
                "\"actualNumerator\": {\"name\": \"actualNumerator\",\"id\": 1108,\"values\": \"" + actualNumerator + "\"}," +
                "\"actualDenominator\": {\"name\": \"actualDenominator\",\"id\": 1109,\"values\": \"" + actualDenominator +"\"}}," +
                "\"globalData\": {\"entityIds\": [" + entityIdToTest + "],\"fieldIds\": [1106,1107,1108,1109,1110,1111],\"isGlobalBulk\": true}}}";

        BulkeditEdit bulkeditEdit = new BulkeditEdit();
        bulkeditEdit.hitBulkeditEdit(cslEntityTypeId,bulkEditPayload);

        if(!(waitForScheduler().equalsIgnoreCase("pass"))){
            csAssert.assertTrue(false,"Scheduler didn't finish on time");
            csAssert.assertAll();
            return;
        }

        String supplierCalculationExpected = expectedResult(supplierNumerator,supplierDenominator);
        String actualPerformanceExpected = expectedResult(actualNumerator,actualDenominator);
        String finalPerformanceExpected = expectedResult(finalNumerator,finalDenominator);

        Show show = new Show();
        show.hitShow(cslEntityTypeId,entityIdToTest);
        String showResponse = show.getShowJsonStr();

        String supplierCalculationActual = ShowHelper.getValueOfField("suppliercalculation",showResponse);
        String actualPerformanceActual = ShowHelper.getValueOfField("actualperformance",showResponse);
        String finalPerformanceActual = ShowHelper.getValueOfField("finalperformance",showResponse);

        if(supplierCalculationExpected.equalsIgnoreCase("") && supplierCalculationActual == null){
            logger.info("supplierCalculation expected value of empty is present");
        }else if(supplierCalculationExpected.equalsIgnoreCase(supplierCalculationActual)){
            logger.info("supplierCalculation expected value equals supplierCalculation");
        }else {
            logger.error("supplierCalculationExpected and supplierCalculationActual didn't match");
            csAssert.assertTrue(false,"supplierCalculationExpected and supplierCalculationActual didn't match");
        }

        if(actualPerformanceExpected.equalsIgnoreCase("") && actualPerformanceActual == null){
            logger.info("actualPerformance expected value of empty is present");
        }else if(actualPerformanceExpected.equalsIgnoreCase(actualPerformanceExpected)){
            logger.info("actualPerformance expected value equals actualPerformance actual");
        }else {
            logger.error("actualPerformanceExpected and actualPerformanceActual didn't match");
            csAssert.assertTrue(false,"actualPerformanceExpected and actualPerformanceActual didn't match");
        }

        if(finalPerformanceExpected.equalsIgnoreCase("") && finalPerformanceActual == null){
            logger.info("finalPerformance expected value of empty is present");
        }else if(finalPerformanceExpected.equalsIgnoreCase(supplierCalculationActual)){
            logger.info("finalPerformance expected value equals finalPerformance actual");
        }else {
            logger.error("finalPerformanceExpected and finalPerformanceActual didn't match");
            csAssert.assertTrue(false,"finalPerformanceExpected and finalPerformanceActual didn't match");
        }
        csAssert.assertAll();
    }

    @Test(dataProvider = "bulkUpdateDataProvider",enabled = true)
    public void testValidatePerformanceOnCSLBulkUpdate(String flowToTest,int entityIdToTest,HashMap<String,String> numDenominatorValues){

        logger.info("Validating Performance Fields On CSL through BulkEdit for the flow {}",flowToTest);
        CustomAssert csAssert = new CustomAssert();
        Map<Integer,Object> columnDataMap = new HashMap<>();

        String supplierNumerator = numDenominatorValues.get("supplierNumerator");
        String supplierDenominator = numDenominatorValues.get("supplierDenominator");
        String finalNumerator = numDenominatorValues.get("finalNumerator");
        String finalDenominator = numDenominatorValues.get("finalDenominator");
        String actualNumerator = numDenominatorValues.get("actualNumerator");
        String actualDenominator = numDenominatorValues.get("actualDenominator");

        columnDataMap.put(20,supplierNumerator);
        columnDataMap.put(21,actualNumerator);
        columnDataMap.put(22,supplierDenominator);
        columnDataMap.put(23,actualDenominator);
        columnDataMap.put(28,finalNumerator);
        columnDataMap.put(29,finalDenominator);

        String templateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkupdatetemplatefilepath");
        String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkupdatetemplatefilename");
        String bulkUpdateTemplateId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkupdatetemplateid");

        XLSUtils.editRowData(templateFilePath,templateFileName,"Child SLA",6,columnDataMap);

        String bulkUploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templateFilePath,templateFileName,cslEntityTypeId,Integer.parseInt(bulkUpdateTemplateId));

        if(bulkUploadResponse.contains("Template is not correct") || bulkUploadResponse.contains("500:;basic:;Incorrect headers")){
            csAssert.assertTrue(false,"Bulk Update Template is not correct for CSL id" + entityIdToTest);
            csAssert.assertAll();
            return;
        }


        if(!bulkUploadResponse.contains("200")){
            csAssert.assertTrue(false,"bulk update done unsuccessfully");
            csAssert.assertAll();
            return;
        }
        if(!(waitForScheduler().equalsIgnoreCase("pass"))){
            csAssert.assertTrue(false,"Scheduler didn't finish on time or scheduler task failed");
            csAssert.assertAll();
            return;
        }

        String supplierCalculationExpected = expectedResult(supplierNumerator,supplierDenominator);
        String actualPerformanceExpected = expectedResult(actualNumerator,actualDenominator);
        String finalPerformanceExpected = expectedResult(finalNumerator,finalDenominator);

        Show show = new Show();
        show.hitShow(cslEntityTypeId,entityIdToTest);
        String showResponse = show.getShowJsonStr();

        String supplierCalculationActual = ShowHelper.getValueOfField("suppliercalculation",showResponse);
        String actualPerformanceActual = ShowHelper.getValueOfField("actualperformance",showResponse);
        String finalPerformanceActual = ShowHelper.getValueOfField("finalperformance",showResponse);

        if(supplierCalculationExpected.equalsIgnoreCase("") && supplierCalculationActual == null){
            logger.info("supplierCalculation expected value of empty is present");
        }else if(supplierCalculationExpected.equalsIgnoreCase(supplierCalculationActual)){
            logger.info("supplierCalculation expected value equals supplierCalculation");
        }else {
            logger.error("supplierCalculationExpected and supplierCalculationActual didn't match");
            csAssert.assertTrue(false,"supplierCalculationExpected and supplierCalculationActual didn't match");
        }

        if(actualPerformanceExpected.equalsIgnoreCase("") && actualPerformanceActual == null){
            logger.info("actualPerformance expected value of empty is present");
        }else if(actualPerformanceExpected.equalsIgnoreCase(actualPerformanceExpected)){
            logger.info("actualPerformance expected value equals actualPerformance actual");
        }else {
            logger.error("actualPerformanceExpected and actualPerformanceActual didn't match");
            csAssert.assertTrue(false,"actualPerformanceExpected and actualPerformanceActual didn't match");
        }

        if(finalPerformanceExpected.equalsIgnoreCase("") && finalPerformanceActual == null){
            logger.info("finalPerformance expected value of empty is present");
        }else if(finalPerformanceExpected.equalsIgnoreCase(supplierCalculationActual)){
            logger.info("finalPerformance expected value equals finalPerformance actual");
        }else {
            logger.error("finalPerformanceExpected and finalPerformanceActual didn't match");
            csAssert.assertTrue(false,"finalPerformanceExpected and finalPerformanceActual didn't match");
        }
        csAssert.assertAll();
    }

    private String expectedResult(String numerator,String denominator){

        logger.info("Calculating result based on numerator and denominator");
        String expectedResult;

        if(denominator.equalsIgnoreCase("0") || denominator.equalsIgnoreCase("") ){
            expectedResult = "";
        }else if(numerator.equalsIgnoreCase("")){
            expectedResult = "";
        }else{
            Double expectedValue = (Double.parseDouble(numerator) / Double.parseDouble(denominator)) * 100;
            expectedResult = String.valueOf(expectedValue);

        }
        return expectedResult;
    }

    private String waitForScheduler() {

        String result = "pass";
        Long schedulerTimeOut = 240000L;
        Long pollingTime = 5000L;
        try {
            logger.info("Time Out for Scheduler is {} milliseconds", schedulerTimeOut);
            long timeSpent = 0;
            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Current Job");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), null);

            if (newTaskId != -1) {
                Boolean taskCompleted = false;
                logger.info("Checking if Scheduler Task has completed or not.");

                while (timeSpent < schedulerTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    fetchObj.hitFetch();
                    logger.info("Getting Status of Scheduler Task.");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        taskCompleted = true;
                        result = "pass";

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Scheduler Task is not finished yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= schedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }
            } else {
                logger.info("Couldn't get Scheduler Task Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
                Thread.sleep(schedulerTimeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Scheduler to Finish [{}]", e.getStackTrace());
            result = "fail";
        }
        return result;
    }
}
