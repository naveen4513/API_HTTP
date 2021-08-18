package com.sirionlabs.test.todo;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.todo.TodoDaily;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestToDoRussianValidations {

    private final static Logger logger = LoggerFactory.getLogger(TestToDoRussianValidations.class);
    private static String configFilePath;
    private static String configFileName;
    private int bufferBeforeDate;
    private int bufferAfterDate;
    private String dateFormat;
    private boolean applyRandomization = false;
    private int maxRecordsToValidate = 3;
    private List<String> entityTypeIdstoTest;
    private String entitytypeids[];

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TodoConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TodoApprovalsConfigFileName");
        bufferBeforeDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferbefore"));
        bufferAfterDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferafter"));
        dateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateformat");
        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization");
        if (temp != null && temp.trim().equalsIgnoreCase("true"))
            applyRandomization = true;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordstovalidate");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            maxRecordsToValidate = Integer.parseInt(temp);

        entitytypeids = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"tododaily","entitytypeids").split(",");
        entityTypeIdstoTest =  Arrays.asList(entitytypeids);
    }

    @Test
    public void testDailyApprovalsAPIResponse() {
        CustomAssert csAssert = new CustomAssert();
        JSONArray pendingapprovalsarray;
        JSONArray pendingtasksarray;
        try {
            logger.info("Hitting Todo Daily Api to verify Response.");
            TodoDaily todoDailyObj = new TodoDaily();
            todoDailyObj.hitTodoDaily();
            String tododailyresponse = todoDailyObj.getTodoDailyJsonStr();

            if (!ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {
                logger.error("Todo Daily Approvals API Response is an Invalid JSON.");
                csAssert.assertTrue(false, "Todo Daily Approvals API Response is an Invalid JSON.");
            }

            pendingapprovalsarray = new JSONObject(tododailyresponse).getJSONArray("Pending Approvals");
            pendingtasksarray = new JSONObject(tododailyresponse).getJSONArray("Pending Tasks");

            logger.info("Validating pending approvals array");
            Boolean todovalidation = todovalidation(pendingapprovalsarray,csAssert);

            if(todovalidation == false){
                logger.error("To do validation failed for pending approval russian characters");
                csAssert.assertTrue(false,"To do validation failed for russian characters for pending approvals");
            }else {
                logger.info("To do validation passed for russian characters");
            }
            logger.info("Validating pending tasks array");
            todovalidation = todovalidation(pendingtasksarray,csAssert);

            if(todovalidation == false){
                logger.error("To do validation failed for pending task russian characters");
                csAssert.assertTrue(false,"To do validation failed for russian characters for pending task");
            }else {
                logger.info("To do validation passed for pending task russian characters");
            }

        } catch (Exception e) {
            logger.error("Exception while Verifying Daily Approvals API Response. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Verifying Daily Approvals API Response. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private Boolean todovalidation(JSONArray pendingjsonarray,CustomAssert csAssert){

        String functionName;
        String[] serviceNamearray;
        String serviceName;
        String statusName;
        String contractid;
        Boolean validaterussiancharacters;
        Boolean todovalidation = true;
        Boolean showpagevalidationstatus = true;

        JSONObject pendingjson;
        Show show = new Show();
        String showResponse;
        String entitytypeid = null;
        List<String> servicenamearrayList = new ArrayList<>();
        List<String> functionnamearrayList = new ArrayList<>();

        for(int i = 0;i<pendingjsonarray.length();i++){

            pendingjson = pendingjsonarray.getJSONObject(i);
            try{

                entitytypeid = pendingjson.get("entityTypeId").toString();
                if(!(entityTypeIdstoTest.contains(entitytypeid))){
                    continue;
                }
            }catch (Exception e){
                logger.error("Exception while fetching entity type id from pending approval array");
            }

            try{
                functionName = pendingjson.get("functionName").toString();
                functionnamearrayList = Arrays.asList(functionName);

                validaterussiancharacters = validaterussiancharacters(functionName,csAssert);
                if(validaterussiancharacters == true){
                    logger.info("Function string: {} validated successfully",functionName);

                }else {
                    todovalidation = false;
                    logger.error("Function string: {} validated unsuccessfully",functionName);
                }
            }catch (Exception e){
                logger.error("Exception while getting function name from the pending approval json array " + i + " " + e.getMessage());
            }

            try{
                serviceNamearray = pendingjson.get("serviceName").toString().split(",");
                servicenamearrayList = Arrays.asList(serviceNamearray);

                for(int j =0;j<serviceNamearray.length;j++){

                    serviceName = serviceNamearray[i];

                    validaterussiancharacters = validaterussiancharacters(serviceName,csAssert);
                    if(validaterussiancharacters == true){
                        logger.info("Service Name string: {} validated successfully",serviceName);

                    }else {
                        todovalidation = false;
                        logger.error("Service Name string: {} validated unsuccessfully",serviceName);
                    }
                }

            }catch (Exception e){

                logger.error("Exception while getting service name from the pending approval json array [ {} ]",i);
                logger.error(e.getMessage());
            }
            try{
                statusName = pendingjson.get("statusName").toString();
                validaterussiancharacters = validaterussiancharacters(statusName,csAssert);
                if(validaterussiancharacters == true){
                    logger.info("Status Name string: {} validated successfully",statusName);

                }else {
                    todovalidation = false;
                    logger.error("Status Name string: {} validated unsuccessfully",statusName);
                }

            }catch (Exception e){
                todovalidation = false;
                logger.error("Exception while Status name from the pending approval json array [ {} ]",i);
                logger.error(e.getMessage());
            }
            int entitytypeidinteger = Integer.parseInt(entitytypeid);
            try{
                contractid = pendingjson.get("contractId").toString();
                show.hitShow(entitytypeidinteger,Integer.parseInt(contractid));
                showResponse = show.getShowJsonStr();

                //String fieldname = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"showfieldmapping","servicename");
                //String expectedvalue = pendingjson.get("serviceName").toString();
                logger.info("Validating to do with show page for services");
                JSONObject showjobj = new JSONObject(showResponse);
                JSONArray values = showjobj.getJSONObject("body").getJSONObject("data").getJSONObject("services").getJSONArray("values");

                String serviceNameshowpage;
                for(int j=0;j<values.length();j++){

                    serviceNameshowpage = values.getJSONObject(j).get("name").toString();

                    if(servicenamearrayList.contains(serviceNameshowpage)){
                        logger.info("Show page contains the service name " + serviceNameshowpage);
                    }else {
                        showpagevalidationstatus = false;
                        logger.error("Show page does not contains the service name " + serviceNameshowpage);
                    }
                }
                if(showpagevalidationstatus == false){
                    logger.error("Show page validation failed for service name , contract ID " + contractid);
                    csAssert.assertTrue(false,"Show page validation failed for service name , contract ID " + contractid);
                }
                showpagevalidationstatus = true;

                logger.info("Validating to do with show page for functions");
                showjobj = new JSONObject(showResponse);
                values = showjobj.getJSONObject("body").getJSONObject("data").getJSONObject("functions").getJSONArray("values");

                String functionNameshowpage;
                for(int j=0;j<values.length();j++){

                    functionNameshowpage = values.getJSONObject(j).get("name").toString();

                    if(functionnamearrayList.contains(functionNameshowpage)){
                        logger.info("Show page contains the function name " + functionNameshowpage);
                    }else {
                        showpagevalidationstatus = false;
                        logger.error("Show page does not contains the function name " + functionNameshowpage);
                        //csAssert.assertTrue(false,"Show page validation failed for function name" + functionNameshowpage + "show page id " + contractid);
                    }
                }

                if(showpagevalidationstatus == false){
                    logger.error("Show page validation failed for function name");
                    csAssert.assertTrue(false,"Show page validation failed for function name for show page id : " + contractid);
                }

                String fieldname = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"showfieldmapping","name");
                String expectedvalue = pendingjson.get("name").toString();
                showpagevalidationstatus = show.verifyShowField(showResponse,fieldname,expectedvalue,entitytypeidinteger);

                if(showpagevalidationstatus == false){
                    logger.error("Show page validation failed for name");
                    csAssert.assertTrue(false,"Show page validation failed for name");
                }

                fieldname = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"showfieldmapping","contractname");
                expectedvalue = pendingjson.get("contractName").toString();
                showpagevalidationstatus = show.verifyShowField(showResponse,fieldname,expectedvalue,entitytypeidinteger);

                if(showpagevalidationstatus == false){
                    logger.error("Show page validation failed for contract name");
                    csAssert.assertTrue(false,"Show page validation failed for contract name");
                }
                showpagevalidationstatus = true;
                fieldname = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"showfieldmapping","relationname");
                expectedvalue = pendingjson.get("relationName").toString();
                showpagevalidationstatus = show.verifyShowField(showResponse,fieldname,expectedvalue,entitytypeidinteger);

                if(showpagevalidationstatus == false){
                    logger.error("Show page validation failed for relation name");
                    csAssert.assertTrue(false,"Show page validation failed for relation name");
                }
                showpagevalidationstatus = true;

                fieldname = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"showfieldmapping","statusname");
                expectedvalue = pendingjson.get("statusName").toString();
                showpagevalidationstatus = show.verifyShowField(showResponse,fieldname,expectedvalue,entitytypeidinteger);

                if(showpagevalidationstatus == false){
                    logger.error("Show page validation failed for status name");
                    csAssert.assertTrue(false,"Show page validation failed for status name");
                }
                showpagevalidationstatus = true;

            }catch (Exception e){
                todovalidation = false;
                logger.error("Exception while Status name from the pending approval json array [ {} ]",i);
                logger.error(e.getMessage());
            }
        }
        return todovalidation;
    }

    private boolean validaterussiancharacters(String englishstring,CustomAssert csAssert){

//        if(wordstoskip.contains(englishstring)){
//            logger.debug("Skipping the word " + englishstring);
//            return true;
//        }

        logger.info("Validating the string : " + englishstring);
        String specialChars = "-/*!@#$%^&*()\"{}_[]|\\?/<>,. ";

        Boolean flag = true;
        List<Character> nonRussianChar = new ArrayList<>();
        String nonRussianCharacter;
        List<String> nonRussianCharacters = new ArrayList<>();
        innerloop:
        for (int i = 0; i < englishstring.trim().length(); i++) {
            if (!Character.UnicodeBlock.of(englishstring.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {

                nonRussianChar.add(englishstring.charAt(i));

                //for (int j = 0; i < s.length(); j++) {
                if (specialChars.contains(englishstring.substring(i, i + 1))) {

                    logger.info("Special character present in the string field");
                    //flag = false;
                } else {
                    //logger.error("Russian or Special character not present in the string field " + englishstring.substring(i, i + 1));
                    nonRussianCharacter = englishstring.substring(i, i + 1);
                    nonRussianCharacters.add(nonRussianCharacter);
                    flag = false;
                    //break innerloop;
                }
            }
        }
        if (!flag) {
            logger.error("String : {} contains non-russian characters : {}", englishstring, nonRussianCharacters);
            csAssert.assertTrue(false, " String : " + englishstring + ", contains non-russian characters : " + nonRussianChar);
        } else
            logger.info("String : {} contains all russian characters.", englishstring);

        return flag;
    }

}
