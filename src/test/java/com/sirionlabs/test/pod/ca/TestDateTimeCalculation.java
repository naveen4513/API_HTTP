package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestDateTimeCalculation {
    private final static Logger logger = LoggerFactory.getLogger(TestDateTimeCalculation.class);

    private String configFilePath;
    private String configFileName;
    private String entityTypeId;
    private String entityName = "contract draft request";

    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFilePath;
    private String cdrExtraFieldsConfigFileName;
    private Map<String, String> defaultProperties;

    @BeforeClass
    public void beforeClass() throws Exception {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");

        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFileName");
        defaultProperties = ParseConfigFile.getAllProperties(cdrConfigFilePath, cdrConfigFileName);

        cdrExtraFieldsConfigFilePath = defaultProperties.get("extrafieldsconfigfilepath");
        cdrExtraFieldsConfigFileName = defaultProperties.get("extrafieldsconfigfilename");
    }

    @Test
    public void testC90212() {
        CustomAssert customAssert = new CustomAssert();
        int entityId = createCDR(customAssert);
        try {
            boolean addDates = addDates(entityId,customAssert);
            if(addDates){
                HashMap<String,String> showPageData = getShowPageData(entityId, customAssert);
                String effectiveDate = showPageData.get("EffectiveDate");
                String expirationDate = showPageData.get("ExpirationDate");
                long showPageDays = Long.parseLong(showPageData.get("Days"));
                long showPageMonths = Long.parseLong(showPageData.get("Months"));
                long showPageYears = Long.parseLong(showPageData.get("Years"));

                HashMap<String,Long> differenceBetweenDates = getDifferenceBetweenDates(effectiveDate,expirationDate,customAssert);
                long calculatedDays = differenceBetweenDates.get("Days");
                long calculatedMonths = differenceBetweenDates.get("Months");
                long calculatedYears = differenceBetweenDates.get("Years");

                if(showPageYears==calculatedYears){
                    if(showPageMonths==calculatedMonths){
                        if(showPageDays==calculatedDays){
                            logger.info("Calculation is correct.");
                        }else{
                            logger.error("Days are not the same.");
                            customAssert.assertTrue(false,"Days are not the same.");
                        }
                    }else{
                        logger.error("Months are not the same.");
                        customAssert.assertTrue(false,"Months are not the same.");
                    }
                }else{
                    logger.error("Years are not the same.");
                    customAssert.assertTrue(false,"Years are not the same.");
                }
            }else{
                logger.error("CDR could not be edited.");
                customAssert.assertTrue(false,"CDR could not be edited.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while verifying the functionality of computing difference between effective and expiration date.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while verifying the functionality of computing difference between effective and expiration date.");
        }finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, entityId);
            logger.info("CDR with entityId id {} is deleted.", entityId);
        }
        customAssert.assertAll();
    }

    private int createCDR(CustomAssert customAssert) {
        int entityId = -1;
        String createResponse;
        try {
            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrExtraFieldsConfigFilePath, cdrExtraFieldsConfigFileName, "cdr creation", true);
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObject = new JSONObject(createResponse);
                String createStatus = jsonObject.getJSONObject("header").getJSONObject("response").getString("status").trim();
                if (createStatus.equalsIgnoreCase("success")) {
                    entityId = CreateEntity.getNewEntityId(createResponse, "contract draft request");
                    logger.info("Id of the Entity Created is : {}", entityId);
                } else {
                    logger.error("CDR creation is unsuccessful.");
                    customAssert.assertTrue(false, "CDR creation is unsuccessful.");
                }
            } else {
                logger.error("Create response of CDR is not a valid JSON Response");
                customAssert.assertTrue(false, "Create response of CDR is not a valid JSON Response");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while creating the CDR", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while creating the CDR");
        }
        return entityId;
    }

    private boolean addDates(int entityId, CustomAssert customAssert) {
        String options = null;
        Edit edit = new Edit();
        String editGetResponse;
        boolean status = false;
        try {
            editGetResponse = edit.getEditPayload(entityName, entityId);

            if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
                JSONObject editGetJSON = new JSONObject(editGetResponse);
                for(String key : editGetJSON.getJSONObject("body").getJSONObject("data").keySet()){
                    try{
                        if(!editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).isNull("options")){
                            editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options",options);
                        }
                    }catch (Exception e){
                        continue;
                    }
                }
                //Add dates in here
                String timeStamp = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").put("values",timeStamp);

                String editPostPayload = editGetJSON.toString();
                try {
                    String editPostResponse = edit.hitEdit(entityName, editPostPayload);
                    if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
                        if (new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                            status = true;
                        } else {
                            logger.error("POST EDIT API is not successful");
                            customAssert.assertTrue(false, "POST EDIT API is not successful");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception {} occurred while hitting POST Edit API", e.getMessage());
                    customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while hitting POST Edit API");
                }
            } else {
                logger.error("Get response of Edit API is not a valid JSON");
                customAssert.assertTrue(false, "Get response of Edit API is not a valid JSON");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while editing the Entity.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while editing the Entity.");
        }
        return status;
    }

    private HashMap<String,String> getShowPageData(int entityId, CustomAssert customAssert) {
        HashMap<String,String> showPageData = new HashMap<>();
        Show show = new Show();
        try{
            show.hitShow(Integer.parseInt(entityTypeId), entityId);
            String showResponse = show.getShowJsonStr();
            if(ParseJsonResponse.validJsonResponse(showResponse)){
                JSONObject showJSON = new JSONObject(showResponse);
                String effectiveDate = showJSON.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").getString("values");
                String expirationDate = showJSON.getJSONObject("body").getJSONObject("data").getJSONObject("expirationDate").getString("values");
                String days = showJSON.getJSONObject("body").getJSONObject("data").getJSONObject("days").get("values").toString();
                String months = showJSON.getJSONObject("body").getJSONObject("data").getJSONObject("months").get("values").toString();
                String years = showJSON.getJSONObject("body").getJSONObject("data").getJSONObject("years").get("values").toString();
                showPageData.put("EffectiveDate",effectiveDate);
                showPageData.put("ExpirationDate",expirationDate);
                showPageData.put("Days",days);
                showPageData.put("Months",months);
                showPageData.put("Years",years);
            }else{
                logger.error("Show JSON response of CDR is not a valid JSON.");
                customAssert.assertTrue(false,"Show JSON response of CDR is not a valid JSON.");
            }
        }catch (Exception e){
            logger.error("Exception {} while fetching show page data of CDR.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" while fetching show page data of CDR.");
        }
        return showPageData;
    }

    private HashMap<String,Long> getDifferenceBetweenDates(String start_date, String end_date, CustomAssert customAssert){
        HashMap<String,Long> differenceBetweenTwoDates = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy");
        try{
            DateTime date1 = formatter.parseDateTime(start_date);
            DateTime date2 = formatter.parseDateTime(end_date);

            Period period = new Period(date1, date2);

            long difference_In_Years = period.getYears();
            long difference_In_Months = period.getMonths();
            long difference_In_Weeks = period.getWeeks();
            long difference_In_Days = period.getDays()+(difference_In_Weeks*7);

            if(difference_In_Years<0 & difference_In_Months<0 & difference_In_Days<0){
                difference_In_Years = difference_In_Years *(-1);
                difference_In_Months = difference_In_Months*(-1);
                difference_In_Days = difference_In_Days *(-1);
            }

            differenceBetweenTwoDates.put("Years",difference_In_Years);
            differenceBetweenTwoDates.put("Months",difference_In_Months);
            differenceBetweenTwoDates.put("Days",difference_In_Days+1);
        }catch (Exception e){
            logger.error("Exception {} occurred while fetching the difference between the dates.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while fetching the difference between the dates.");
        }
        return differenceBetweenTwoDates;
    }
}