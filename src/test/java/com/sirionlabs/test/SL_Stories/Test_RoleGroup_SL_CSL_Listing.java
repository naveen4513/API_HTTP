package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test_RoleGroup_SL_CSL_Listing {

    private final static Logger logger = LoggerFactory.getLogger(Test_RoleGroup_SL_CSL_Listing.class);

    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("RoleGroupFlowDownFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("RoleGroupFlowDownFileName");

    }

    @Test
    public void TestRoleGroupFlowDownSLCSLListingForContract(){

        CustomAssert customAssert = new CustomAssert();
        String editPayloadOld = "";
        Edit edit = new Edit();
        String contractEntityName = "contracts";

        try{

            String parentRoleGroupId = "2052";
            String contractRoleGroupId = "rg_" + parentRoleGroupId ;

            int contractIdOnWhichRoleGroupIsUpdated = 129878;

            String contractEntityTypeId = String .valueOf(ConfigureConstantFields.getEntityIdByName(contractEntityName));

            String testForAllEntities = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"testforallentities");
            List<String> childEntitiesToTest;

            if(testForAllEntities.equalsIgnoreCase("true")){

                childEntitiesToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"allentitiestotest").split(","));

            }else {
                childEntitiesToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entitiestotest").split(","));
            }

            String editResponse = edit.hitEdit(contractEntityName,contractIdOnWhichRoleGroupIsUpdated);

            editPayloadOld = createEditPayload(editResponse);

            JSONArray valuesJsonArray = new JSONArray();
            String expectedStakeholder = "Naveen User";
            int stakeHolderId = 1181;
            String stakeHolderString = "{\"name\":\"" + expectedStakeholder + "\",\"id\":" + stakeHolderId + ",\"type\":2,\"email\":null,\"properties\":{\"Contact Number\":\"\",\"Time Zone\":\"Asia/Kolkata (GMT +05:30)\",\"Designation\":\"\",\"Custominheritmulti\":\" - \",\"Alternate Email Address\":\" - \",\"Default Tier\":\"View All\",\"Email\":\"naveen@sirionqa.office\",\"First Name\":\"Naveen\",\"Unique Number\":\" - \",\"Legal Document\":\"Yes\",\"Official Email Id\":\" - \",\"Salutation\":\" - \",\"Business Case\":\"Yes\",\"Financial Document\":\"Yes\",\"Type\":\" - \",\"User Department\":\" - \",\"Last Name\":\"User\",\"Custominherit\":\" - \",\"Middle  Name\":\" - \"}}";

            JSONObject stakeHolderJson = new JSONObject(stakeHolderString);
            valuesJsonArray.put(0,stakeHolderJson);

            JSONObject editPayloadNew = new JSONObject(editPayloadOld);

            editPayloadNew.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(contractRoleGroupId).put("values",valuesJsonArray);

            editResponse = edit.hitEdit(contractEntityName,editPayloadNew.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit Operation performed unsuccessfully for the parent entity " + contractEntityName);
            }

            String[] parentEntityTypeIdArr = new String[childEntitiesToTest.size()];
            String[] childEntityTypeIdArr = new String[childEntitiesToTest.size()];
            String[] roleGroupIdArr = new String[childEntitiesToTest.size()];
            String[] clientIdArr = new String[childEntitiesToTest.size()];
            String[] deletedArr = new String[childEntitiesToTest.size()];

            int i =0;
            for(String childEntity : childEntitiesToTest){

                parentEntityTypeIdArr[i] = contractEntityTypeId;
                childEntityTypeIdArr[i] = String.valueOf(ConfigureConstantFields.getEntityIdByName(childEntity));
                deletedArr[i] = "false";
                clientIdArr[i] = "false";
                roleGroupIdArr[i] = parentRoleGroupId;
                i++;
            }

            String payload = WorkflowRoleGroupFlowDownCreate.getPayloadFlowDownRoleGroup(parentEntityTypeIdArr,childEntityTypeIdArr,roleGroupIdArr,clientIdArr,deletedArr);

            String apiResponse =  WorkflowRoleGroupFlowDownCreate.getCreateResponse(payload).getResponseBody();

            if(!apiResponse.contains("\"success\":true")){
                customAssert.assertTrue(false,"Flow Down Api response is not success");
            }
            int childEntityId;
            int childEntityTypeId;
            String childRoleGroupId;

            Show show = new Show();
            String showResponse;
            JSONObject showResponseJson;
            String childStakeholder;

            for(String childEntity : childEntitiesToTest) {

                childEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,childEntity,"entityidtotest"));
                String roleGroupId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,childEntity,"childrolegroupidparentcontract");
                childRoleGroupId = "rg_" + roleGroupId;

                childEntityTypeId = ConfigureConstantFields.getEntityIdByName(childEntity);

                show.hitShowVersion2(childEntityTypeId,childEntityId);
                showResponse = show.getShowJsonStr();

                showResponseJson = new JSONObject(showResponse);

                childStakeholder = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(childRoleGroupId).getJSONArray("values").getJSONObject(0).get("name").toString();

                if(!childStakeholder.equalsIgnoreCase(expectedStakeholder)){
                    logger.error("Expected value of Stakeholder and actual Value of stakeholder didn't match Expected Value : " + expectedStakeholder + " Actual Value : " + childStakeholder);
                    customAssert.assertTrue(false,"Expected value of Stakeholder and actual Value of stakeholder didn't match Expected Value : " + expectedStakeholder + " Actual Value : " + childStakeholder);
                }

                int listId = ConfigureConstantFields.getListIdForEntity(childEntity);
                Boolean listingFilterValidationStatus =  validateListingFilterResponse(listId,childEntityTypeId,roleGroupId,stakeHolderId,expectedStakeholder,customAssert);

                if(listingFilterValidationStatus == false){
                    customAssert.assertTrue(false,"For Role Group Id Flow Down Listing Filter Validation Unsuccessful");
                }

            }

        }catch (Exception e){

            logger.error("Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
        }finally {
            try {
                String editResponse = edit.hitEdit(contractEntityName, editPayloadOld);

                if(!editResponse.contains("success")){
                    logger.error("Edit reset unsuccessful for contract");
                    customAssert.assertTrue(false,"Edit reset unsuccessful for contract");

                }
            }catch (Exception e){
                logger.error("Exception while resetting contract previous values " + e.getStackTrace());
            }
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestRoleGroupFlowDownSLCSLListingForSupplier(){

        CustomAssert customAssert = new CustomAssert();
        String editPayloadOld = "";
        Edit edit = new Edit();
        String entityName = "suppliers";

        try{

            String parentRoleGroupId = "2159";
            String contractRoleGroupId = "rg_" + parentRoleGroupId ;

            int supplierIdOnWhichRoleGroupIsUpdated = 1344;

            String testForAllEntities = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"testforallentities");
            List<String> childEntitiesToTest;

            if(testForAllEntities.equalsIgnoreCase("true")){

                childEntitiesToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"allentitiestotest").split(","));

            }else {
                childEntitiesToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entitiestotest").split(","));
            }

            String editResponse = edit.hitEdit(entityName,supplierIdOnWhichRoleGroupIsUpdated);

            editPayloadOld = createEditPayload(editResponse);

            JSONArray valuesJsonArray = new JSONArray();
            String expectedStakeholder = "Naveen User";
            String stakeHolderString = "{\"name\":\"" + expectedStakeholder + "\",\"id\":1181,\"type\":2,\"email\":null,\"properties\":{\"Contact Number\":\"\",\"Time Zone\":\"Asia/Kolkata (GMT +05:30)\",\"Designation\":\"\",\"Custominheritmulti\":\" - \",\"Alternate Email Address\":\" - \",\"Default Tier\":\"View All\",\"Email\":\"naveen@sirionqa.office\",\"First Name\":\"Naveen\",\"Unique Number\":\" - \",\"Legal Document\":\"Yes\",\"Official Email Id\":\" - \",\"Salutation\":\" - \",\"Business Case\":\"Yes\",\"Financial Document\":\"Yes\",\"Type\":\" - \",\"User Department\":\" - \",\"Last Name\":\"User\",\"Custominherit\":\" - \",\"Middle  Name\":\" - \"}}";

            JSONObject stakeHolderJson = new JSONObject(stakeHolderString);
            valuesJsonArray.put(0,stakeHolderJson);

            JSONObject editPayloadNew = new JSONObject(editPayloadOld);

            editPayloadNew.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(contractRoleGroupId).put("values",valuesJsonArray);

            editResponse = edit.hitEdit(entityName,editPayloadNew.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit Operation performed unsuccessfully for the parent entity " + entityName);
            }

            int childEntityId;
            int childEntityTypeId;
            String childRoleGroupId;

            Show show = new Show();
            String showResponse;
            JSONObject showResponseJson;
            String childStakeholder;

            for(String childEntity : childEntitiesToTest) {

                childEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,childEntity,"entityidtotest"));
                childRoleGroupId = "rg_" + ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,childEntity,"childrolegroupidparentsupplier");

                childEntityTypeId = ConfigureConstantFields.getEntityIdByName(childEntity);

                show.hitShowVersion2(childEntityTypeId,childEntityId);
                showResponse = show.getShowJsonStr();

                showResponseJson = new JSONObject(showResponse);

                childStakeholder = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(childRoleGroupId).getJSONArray("values").getJSONObject(0).get("name").toString();

                if(!childStakeholder.equalsIgnoreCase(expectedStakeholder)){
                    logger.error("Expected value of Stakeholder and actual Value of stakeholder didn't match Expected Value : " + expectedStakeholder + " Actual Value : " + childStakeholder);
                    customAssert.assertTrue(false,"Expected value of Stakeholder and actual Value of stakeholder didn't match Expected Value : " + expectedStakeholder + " Actual Value : " + childStakeholder);
                }





            }
        }catch (Exception e){

            logger.error("Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
        }

        customAssert.assertAll();

    }

    private String createEditPayload(String editPayload){

        JSONObject editResponseJson = new JSONObject(editPayload);

        editResponseJson.remove("header");
        editResponseJson.remove("session");
        editResponseJson.remove("actions");
        editResponseJson.remove("createLinks");

        editResponseJson.getJSONObject("body").remove("layoutInfo");
        editResponseJson.getJSONObject("body").remove("globalData");
        editResponseJson.getJSONObject("body").remove("errors");

        return editResponseJson.toString();

    }

    private Boolean validateListingFilterResponse(int listId,int entityTypeId,String roleGroupId,int stakeHolderId,String stakeHolderName,CustomAssert customAssert){

        Boolean listingFilterResponseForStakeHolder = true;

        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"" +
                    "filterJson\":{\"53\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + roleGroupId + "\",\"name\":\"\",\"group\":" +
                    "[{\"name\":\"" + stakeHolderName + "\",\"id\":" + stakeHolderId + ",\"idType\":2,\"$$hashKey\":\"object:3017\",\"selected\":true}]," +
                    "\"type\":2},{\"name\":\"" + stakeHolderName + "\",\"id\":" + stakeHolderId +",\"idType\":2,\"$$hashKey\":\"object:3017\",\"selected\":true}]}," +
                    "\"filterId\":53,\"filterName\":\"stakeholder\",\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"uitype\":" +
                    "\"STAKEHOLDER\"}}},\"selectedColumns\":[{\"columnId\":110,\"columnQueryName\":\"id\"},{\"columnId\":111,\"columnQueryName\":\"slaid\"}]}";

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId,payload);
            String listingResponse = listRendererListData.getListDataJsonStr();

            JSONObject listingResponseJson = new JSONObject(listingResponse);

            JSONObject listResponseJson = listingResponseJson.getJSONArray("data").getJSONObject(0);

            JSONArray listingResponseJsonArray =  JSONUtility.convertJsonOnjectToJsonArray(listResponseJson);
            String columnName;
            int entityId = -1;
            for(int i =0;i < listingResponseJsonArray.length();i++){

                columnName = listingResponseJsonArray.getJSONObject(i).get("columnName").toString();

                if(columnName.equalsIgnoreCase("id")){
                    entityId = Integer.parseInt(listingResponseJsonArray.getJSONObject(i).get("value").toString().split(":;")[1]);
                    break;
                }

            }

            Show show = new Show();
            show.hitShowVersion2(entityTypeId,entityId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);

            JSONArray childStakeholderArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + roleGroupId).getJSONArray("values");
            ArrayList<String> childStakeholderList = new ArrayList<>();

            for(int i = 0;i <childStakeholderArray.length();i++ ){
                childStakeholderList.add(childStakeholderArray.getJSONObject(i).get("name").toString());
            }

            if(!childStakeholderList.contains(stakeHolderName)){
                logger.error("Expected value of Stakeholder and actual Value of stakeholder didn't match Expected Value : " + stakeHolderName + " Actual Value : " + childStakeholderList);
                customAssert.assertTrue(false,"Expected value of Stakeholder and actual Value of stakeholder didn't match Expected Value : " + stakeHolderName + " Actual Value : " + childStakeholderList);
                listingFilterResponseForStakeHolder = false;
            }


        }catch (Exception e){
            logger.error("Exception while validating listing Filter Response For StakeHolder " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating listing Filter Response For StakeHolder ");
            listingFilterResponseForStakeHolder = false;
        }


        return listingFilterResponseForStakeHolder;
    }


}
