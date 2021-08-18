package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.commonAPI.Comment;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.timeLine.ActionHistory;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

//Time Line Story
public class test_TimeLineFeatures {

    private final static Logger logger = LoggerFactory.getLogger(test_TimeLineFeatures.class);

    String entityIdMappingFileName;
    String entityIdConfigFilePath;

    @BeforeClass
    public void beforeClass(){

        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
    }

    @DataProvider(name = "entitiesToTest", parallel = false)
    public Object[][] entitiesToTest() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = {"obligations","child obligations","service levels","child service levels","interpretations","issues","actions","disputes","contracts","change requests","invoices","work order requests","governance body","governance body meetings","contract draft request","purchase orders","service data","clauses","contract templates","invoice line item","consumptions"};
//        String[] flowsToTest = {"suppliers"};

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "entitiesToTest",enabled = false)
    public void testActionHistory(String entityToTest){
//    public void testActionHistory(){

        logger.info("Testing Action History API");

        CustomAssert customAssert = new CustomAssert();

        ListRendererListData listRendererListData = new ListRendererListData();

        try {

            int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entityToTest, "entity_url_id"));
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entityToTest, "entity_type_id"));;

            String payLoad = "{\"filterMap\":{}}";

            listRendererListData.hitListRendererListDataV2isFirstCall(listId, payLoad);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            int showPageId = getShowPageID(listDataResponse);

            if(showPageId == -1){
                throw new SkipException("Show Page Id not found for entity " + entityToTest);
            }

            APIResponse response = ActionHistory.getActionHistoryShowResponse(entityTypeId,showPageId);

            if(response.getResponseCode() == 200){

                String responseBody = response.getResponseBody();

                JSONObject responseBodyJson;
                JSONArray statusArrayFromActionHistory;
                JSONArray statusArrayFromShowPageHistory;
                try {
                    responseBodyJson = new JSONObject(responseBody);
                    statusArrayFromActionHistory = responseBodyJson.getJSONObject("body").getJSONObject("history").getJSONArray("status");

                    if(statusArrayFromActionHistory.length() == 0){
                        logger.debug("Status Array size From Action History is zero");
                    }

                    try {
                        Show show = new Show();
                        show.hitShowVersion2(entityTypeId, showPageId);
                        String showPageResponse = show.getShowJsonStr();

                        JSONObject showPageResponseJson = new JSONObject(showPageResponse);
                        statusArrayFromShowPageHistory = showPageResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("history").getJSONArray("status");

                        if(statusArrayFromShowPageHistory.length() == 0){
                            logger.debug("Status Array size From Action History is zero");
                        }

                        Boolean validationStatus = validateStatusActionHistory(statusArrayFromActionHistory,statusArrayFromShowPageHistory,customAssert);

                        if(!validationStatus){
                            logger.error("Error while validating Status Action History");
                            customAssert.assertTrue(false,"Error while validating Status Action History");
                        }


                    }catch (Exception e){
                        logger.error("Exception while parsing history form show page Response " + e.getMessage());
                    }

                }catch (Exception e){
                    logger.error("Exception while parsing action history response " + e.getMessage());
                    customAssert.assertTrue(false,"Exception while parsing action history response " + e.getMessage());
                }

            }else {
                logger.error("API response code is " + response.getResponseCode());

                customAssert.assertTrue(false,"API response code is " + response.getResponseCode());
            }

        }catch (Exception e){
            logger.error("Exception while validating testActionHistory " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating testActionHistory " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(dataProvider = "entitiesToTest",enabled = true)
//    public void TestListRendererTabListData(String entityToTest,Integer entityTypeId){
    public void TestListRendererTabListData(String entityToTest){

        CustomAssert customAssert = new CustomAssert();

        try{

            int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entityToTest, "entity_url_id"));
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entityToTest, "entity_type_id"));;

            String payLoad = "{\"filterMap\":{}}";

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2isFirstCall(listId, payLoad);

            String listDataResponse = listRendererListData.getListDataJsonStr();

            int showPageId = getShowPageID(listDataResponse);

            if(showPageId == -1){
                throw new SkipException("Show Page Id not found for entity " + entityToTest);
            }

//            Show show = new Show();
//            show.hitShowVersion2(entityTypeId,showPageId);
//            String showResponse = show.getShowJsonStr();

//            JSONObject showResponseJson = new JSONObject(showResponse);

            String expectedComment = "test";

            try{

                String commentFieldPayload = "{\"comments\": {\n" +
                        "          \"name\": \"comments\",\n" +
                        "          \"id\": 86,\n" +
                        "          \"multiEntitySupport\": false,\n" +
                        "          \"values\": \""+ expectedComment +"\"\n" +
                        "        }}";

                String showResponse = ShowHelper.getShowResponse(entityTypeId, showPageId);

                JSONObject showJsonObj = new JSONObject(showResponse);
                showJsonObj = showJsonObj.getJSONObject("body").getJSONObject("data");
                showJsonObj = showJsonObj.put("comment", new JSONObject(commentFieldPayload));
                String payloadForCommentAPI = "{\"body\":{\"data\":" + showJsonObj.toString() + "}}";

                Comment commentObj = new Comment();

                String commentResponse = commentObj.hitComment(entityToTest, payloadForCommentAPI);

                if (!ParseJsonResponse.successfulResponse(commentResponse)) {
                    throw new SkipException("Comment API Response is not successful for Entity " + entityToTest + " and Record Id " + showPageId);
                }

            }catch (Exception e){
                logger.error("Exception while preparing payload for summary data API ");
            }

            int communicationTabID = 65;
            String currentDate = DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy");

            String payload = "{\n" +
                    " \"filterMap\": {\n" +
                    "\"last_timestamp\": \"" + currentDate + " 00:00:00\",\n" +
                    " \"first_timestamp\": \"" + currentDate + " 23:59:59\",\n" +
                    " \"offset\": 0,\n" +
                    " \"size\": 100,\n" +
                    " \"orderByColumnName\": \"id\",\n" +
                    " \"orderDirection\": \"asc\",\n" +
                    " \"filterJson\": {}\n" +
                    " }\n" +
                    "}";

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(communicationTabID,entityTypeId,showPageId,payload);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            JSONObject tabListResponseJson = new JSONObject(tabListResponse);
            JSONArray dataArray = tabListResponseJson.getJSONArray("data");

            if(dataArray.length() == 0){

                customAssert.assertTrue(false,"No Comments found in the communication tab");
                customAssert.assertAll();
            }

            JSONObject singleDataJson;
            JSONArray singleDataJsonArray;
            String columnName;
            String actualComment = "";

            try {
                OuterLoop:
                for (int i = 0; i < dataArray.length(); i++) {
                    singleDataJson = dataArray.getJSONObject(i);

                    singleDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(singleDataJson);

                    for (int j = 0; j < singleDataJsonArray.length(); j++) {

                        columnName = singleDataJsonArray.getJSONObject(j).get("columnName").toString();
                        if (columnName.equalsIgnoreCase("comment")) {
                            actualComment = singleDataJsonArray.getJSONObject(j).get("value").toString();
                            break OuterLoop;
                        }
                    }

                }
            }catch (Exception e){
                logger.error("Exception while getting actualComment " + e.getMessage());
            }

            if(!actualComment.equalsIgnoreCase(expectedComment)){
                customAssert.assertTrue(false,"actualComment and expectedComment does not match");
            }

        }catch (Exception e){
            logger.error("Exception while validating ListRendererTabListData " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating ListRendererTabListData " + e.getMessage());
        }

        customAssert.assertAll();
    }

    private int getShowPageID(String listDataResponse){

        JSONArray dataArray;
        JSONArray indDataArray;

        int showPageId = -1;
        try {
            dataArray = new JSONObject(listDataResponse).getJSONArray("data");

            JSONObject indDataJson = dataArray.getJSONObject(0);
            indDataArray = JSONUtility.convertJsonOnjectToJsonArray(indDataJson);
            JSONObject indData;

            for(int j = 0;j<indDataArray.length();j++){
                indData = indDataArray.getJSONObject(j);

                if(indData.get("columnName").toString().equals("id")){
                    showPageId = Integer.parseInt(indData.get("value").toString().split(":;")[1]);

                    break;
                }
            }
        }catch (Exception e){
            logger.debug("Exception while fetching data array " + e.getMessage());
        }

        return showPageId;
    }

    private Boolean validateStatusActionHistory(JSONArray statusArrayFromActionHistory,JSONArray statusArrayFromShowPageHistory,CustomAssert customAssert){

        Boolean validationStatus = true;

        try{

            if(statusArrayFromActionHistory.length() != statusArrayFromShowPageHistory.length()){
                customAssert.assertTrue(false,"Status Array Length From Action History is not equal to Status Array From Show Page History");
                validationStatus = false;
            }
            JSONObject statusArrayFromActionHistoryJson;
            JSONObject statusArrayFromShowPageHistoryJson;

            for(int i =0;i<statusArrayFromActionHistory.length();i++){

                statusArrayFromActionHistoryJson = statusArrayFromActionHistory.getJSONObject(i);
                statusArrayFromShowPageHistoryJson = statusArrayFromShowPageHistory.getJSONObject(i);

                try{
                    if(!(statusArrayFromActionHistoryJson.get("id").toString().equals(statusArrayFromShowPageHistoryJson.get("id").toString()))){
                        customAssert.assertTrue(false,"id value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }

                    if(!(statusArrayFromActionHistoryJson.get("taskType").toString().equals(statusArrayFromShowPageHistoryJson.get("taskType").toString()))){
                        customAssert.assertTrue(false,"taskType value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }

                    if(!(statusArrayFromActionHistoryJson.get("label").toString().equals(statusArrayFromShowPageHistoryJson.get("label").toString()))){
                        customAssert.assertTrue(false,"label value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }

                    if(!(statusArrayFromActionHistoryJson.get("requestedBy").toString().equals(statusArrayFromShowPageHistoryJson.get("requestedBy").toString()))){
                        customAssert.assertTrue(false,"requestedBy value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }

                    if(!(statusArrayFromActionHistoryJson.get("completedBy").toString().equals(statusArrayFromShowPageHistoryJson.get("completedBy").toString()))){
                        customAssert.assertTrue(false,"completedBy value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }
                    if(!(statusArrayFromActionHistoryJson.get("timeOfAction").toString().equals(statusArrayFromShowPageHistoryJson.get("timeOfAction").toString()))){
                        customAssert.assertTrue(false,"timeOfAction value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }
                    if(!(statusArrayFromActionHistoryJson.get("dateCreated").toString().equals(statusArrayFromShowPageHistoryJson.get("dateCreated").toString()))){
                        customAssert.assertTrue(false,"dateCreated value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }
                    if(!(statusArrayFromActionHistoryJson.get("status").toString().equals(statusArrayFromShowPageHistoryJson.get("status").toString()))){
                        customAssert.assertTrue(false,"status value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }
                    if(!(statusArrayFromActionHistoryJson.get("statuses").toString().equals(statusArrayFromShowPageHistoryJson.get("statuses").toString()))){
                        customAssert.assertTrue(false,"statuses value from status Array From Action History Json is not equal to status Array From ShowPage History Json");
                        validationStatus = false;
                    }
                }catch (Exception e){
                    logger.error("Exception while validating status Array From ActionHistory Json with status Array From ShowPage History Json " + e.getMessage());
                    customAssert.assertTrue(false,"Exception while validating status Array From ActionHistory Json with status Array From ShowPage History Json " + e.getMessage());
                    validationStatus = false;
                }

            }

        }catch (Exception e){
            logger.error("Exception while validating Status Action History " + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;
    }

}
