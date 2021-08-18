package com.sirionlabs.test.SL_Stories.slif;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.servicedata.TblauditlogsFieldHistory;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sirionlabs.api.ServiceLevel.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Test_SLIF_SourceValidations extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(Test_SLIF_SourceValidations.class);

    String configFilePath;
    String configFileName;

    String adminUserName;
    String adminPassword;
    int sourceId;
    String name;
    String dbHostAddress;
    String dbName;
    String dbPortName;
    String dbUserName;
    String dbPassword;
    PostgreSQLJDBC postgreSQLJDBC;

    @BeforeClass
    public void BeforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFileName");
        adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
        dbHostAddress = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassword = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress,dbPortName,dbName,dbUserName,dbPassword);
    }

    // C89721
    @Test(enabled = true)
    public void TestMandatoryFieldsSource(){

        CustomAssert customAssert = new CustomAssert();
        Check check = new Check();
        check.hitCheck(adminUserName,adminPassword);
        int sourceId = -1;
        JSONObject getResponseJson;
        String message = "size of name must be between 1 and 200";

        try{
            APIResponse response = SLIF_Schemas.hitGetCreateSource(executor);
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Create Source API Response is not a valid Json");
                customAssert.assertTrue(false,"Create Source API Response is not a valid Json");
            }

            name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source mandatory details","name");
            String payload= createPayloadForSource(name);
            response = SLIF_Schemas.hitPostCreateSource(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create Source API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create Source API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            getResponseJson = new JSONObject(responseBody);

            String responseMessage = getResponseJson.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            if(message.equals(responseMessage)){
                customAssert.assertTrue(true,"Name is mandatory field");
                customAssert.assertAll();
            }

            sourceId =  getSourceId(responseBody,"source","source");

            if(sourceId == -1){
                customAssert.assertTrue(false,"Source ID not created successfully");
            }
            else {
                customAssert.assertTrue(true,"Source has been created successfully");
            }

        }catch (Exception e){
            logger.error("Exception while validating Create Source API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Create Source  API " + e.getMessage());
        }
    }

    // C89722
    @Test(enabled = true)
    public void TestEditSource(){

        CustomAssert customAssert = new CustomAssert();
        JSONObject updateResponseJson;
        String status = "success";
        String newName = "final Use Case 1 Automation";
        String[] emailId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","emailid").split(",");
        String[] emailName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","email").split(",");
        String data = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"audit details","data");
        String updateData = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"audit details","updatedata");
        String showName;

        try{
            APIResponse response = SLIF_Schemas.hitGetCreateSource(executor);
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Create Source API Response is not a valid Json");
                customAssert.assertTrue(false,"Create Source API Response is not a valid Json");
            }

            name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","name");
            String payload= createPayloadForSource(name);
            response = SLIF_Schemas.hitPostCreateSource(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create Source API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create Source API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            sourceId =  getSourceId(responseBody,"source","source");

            if(sourceId == -1){
                customAssert.assertTrue(false,"Source ID not created successfully");
            }
            else {
                customAssert.assertTrue(true,"Source has been created successfully");
            }
            APIResponse editResponse = SLIF_Schemas.hitGetEditSource(executor, sourceId);
            responseBody = editResponse.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Edit Source API Response is not a valid Json");
                customAssert.assertTrue(false,"Edit Source API Response is not a valid Json");
            }
            String newResponse = editSource(responseBody,customAssert);
            JSONObject editResponseJson = new JSONObject(newResponse);

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values",newName);
            JSONArray updateEmail = new JSONArray(createPayloadFromArray(emailId,emailName));
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("email").put("values",updateEmail);

            APIResponse updateResponse = SLIF_Schemas.hitPostEditSource(executor, editResponseJson.toString());
            String updateResponseBody = updateResponse.getResponseBody();
            updateResponseJson = new JSONObject(updateResponseBody);

            String statusMessage =  updateResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString();

            Assert.assertTrue(statusMessage.equalsIgnoreCase(status),"Edit is done unsuccessfully");

            APIResponse response1 = SLIF_Schemas.hitGetShowSource(executor, sourceId);
            String responseBody1 = response1.getResponseBody();
            JSONObject showResponse = new JSONObject(responseBody1);

            showName = showResponse.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();

            Assert.assertTrue(showName.equalsIgnoreCase(newName),"Edit is done unsuccessfully");

            int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","listid"));
            int entityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","entityid"));
            payload = "{\"filterMap\":{}}";
            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
            listRendererTabListData.hitListRendererTabListData(listId,entityId,sourceId,payload);
            String listReponse = listRendererTabListData.getTabListDataJsonStr();
            JSONObject jsonObject = new JSONObject(listReponse);
            String newData = jsonObject.getJSONArray("data").getJSONObject(1).getJSONObject("3967").get("value").toString();
            String newAction = jsonObject.getJSONArray("data").getJSONObject(0).getJSONObject("3967").get("value").toString();
            String auditApi = jsonObject.getJSONArray("data").getJSONObject(0).getJSONObject("5094").get("value").toString();
            Assert.assertTrue(newData.equalsIgnoreCase(data),"Audit log is not performing required action");
            Assert.assertTrue(newAction.equalsIgnoreCase(updateData),"Audit log is not performing required action");

            TblauditlogsFieldHistory tblauditlogsFieldHistory = new TblauditlogsFieldHistory();
            tblauditlogsFieldHistory.hitTblauditlogsFieldHistoryPage(auditApi);
            String auditResponse = tblauditlogsFieldHistory.getTblAuditLogsFieldHistoryResponseStr();

            String name = JsonPath.read(auditResponse,"value[0].newValue");
            Assert.assertTrue(name.equalsIgnoreCase(newName),"Audit logs is done unsuccessfully");

        }catch (Exception e){
            customAssert.assertTrue(false,"Edit is done unsuccessfully" + e.getMessage());
        }
    }

    // C153761
    @Test(dependsOnMethods = "TestEditSource",enabled = true)
    public void TestSourceValidateShowPageFields(){

        CustomAssert customAssert = new CustomAssert();
        JSONObject showResponseJson;
        String name = "final Use Case 1 Automation";
        String tool = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","toolname");
        String url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","url");
        String columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","columns");
        String NDC = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","ndc");
        String UDC = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
        String userName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","username");
        String password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","password");

        try{

            APIResponse showResponse = SLIF_Schemas.hitGetShowSource(executor, sourceId);
            String responseBody = showResponse.getResponseBody();
            String response = editSource(responseBody,customAssert);
            showResponseJson = new JSONObject(response);

            String showName = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
            String showTool = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("requestMetadata").getJSONObject("values").get("name").toString();
            String showUDC = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("uniqueDataCriteria").get("values").toString();
            String showUrl = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicData").getJSONObject("values").get("url").toString();
            String showColumns = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicData").getJSONObject("values").get("columns").toString();
            String showNDC = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicData").getJSONObject("values").get("newDataCriteria").toString();
            String showUserName = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicData").getJSONObject("values").get("username").toString();
            String showPassword = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicData").getJSONObject("values").get("password").toString();

            Assert.assertTrue(showName.equalsIgnoreCase(name),"Name is incorrect");
            Assert.assertTrue(showTool.equalsIgnoreCase(tool),"Tool is incorrect");
            Assert.assertTrue(showUDC.equalsIgnoreCase(UDC),"UDC is incorrect");
            Assert.assertTrue(showUrl.equalsIgnoreCase(url),"URL is incorrect");
            Assert.assertTrue(showColumns.equalsIgnoreCase(columns),"Columns are incorrect");
            Assert.assertTrue(showNDC.equalsIgnoreCase(NDC),"NDC is incorrect");
            Assert.assertTrue(showUserName.equalsIgnoreCase(userName),"User Name is incorrect");
            Assert.assertTrue(showPassword.equalsIgnoreCase(password),"Password Type is incorrect");

        }catch (Exception e){
            customAssert.assertTrue(false,"Show Page Field Validations is done unsuccessfully" + e.getMessage());
        }finally {
            deleteSource(sourceId);
        }
    }

    private String createPayloadForSource(String name){

        String description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","description");
        String active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","active");
        String[] email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","email").split(",");
        String url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","url");
        String columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","columns");
        String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","ndc");
        String uniqueDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
        String userName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","username");
        String password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source uc1 detail","password");

        String payload;
        payload = "{\"body\":{\"data\":{\"dynamicData\":{\"name\":\"dynamicData\",\"multiEntitySupport\":false," +
                "\"values\":{\"url\":\""+url+"\",\"newDataCriteria\":" +
                "\""+newDataCriteria+"\"," +
                "\"columns\":\""+columns+"\",\"username\":\""+userName+"\",\"password\":\""+password+"\"}}," +
                "\"authMetadata\":{\"name\":\"authMetadata\",\"id\":12635,\"multiEntitySupport\":false,\"values\":" +
                "{\"id\":1}},\"canSupplierBeParent\":true,\"requestMetadata\":{\"name\":\"requestMetadata\"," +
                "\"id\":12629,\"multiEntitySupport\":false,\"values\":{\"id\":1}},\"active\":{\"name\":\"active\"," +
                "\"id\":12625,\"values\":"+active+",\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12623," +
                "\"multiEntitySupport\":false,\"values\":\""+name+"\"},\"uniqueDataCriteria\":{\"name\":" +
                "\"uniqueDataCriteria\",\"id\":12665,\"multiEntitySupport\":false,\"values\":\""+uniqueDataCriteria+"\"}," +
                "\"description\":{\"name\":\"description\",\"id\":12624,\"multiEntitySupport\":false,\"values\":" +
                "\""+description+"\"},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false,\"values\":" +
                "["+createPayloadStringFromArray(email)+"]}}}}";

        return payload;

    }

    public static int getSourceId(String createJsonStr, String entityName,String notification) {
        int newEntityId = -1;
        try {
            if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
                JSONObject jsonObj = new JSONObject(createJsonStr);
                if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
                    if (jsonObj.getJSONObject("header").getJSONObject("response").has("entityId")) {
                        return jsonObj.getJSONObject("header").getJSONObject("response").getInt("entityId");
                    }

                    String notificationStr = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");

                    String temp[] = notificationStr.trim().split(Pattern.quote(notification + "/"));
                    if (temp.length > 1) {
                        String temp2 = temp[1];
                        String temp3[] = temp2.trim().split(Pattern.quote("\""));
                        if (temp3.length > 1) {
                            String temp4 = temp3[0];
                            String temp5[] = temp4.trim().split(Pattern.quote("/"));
                            if (temp5.length > 1)
                                newEntityId = Integer.parseInt(temp5[1]);
                        }
                    }
                } else {
                    logger.error("New Entity {} not created. Please fill mandatory fields. ", entityName);
                    // need to write custom assert aslo in all places.
                }
            } else {
                logger.error("Create Response for Entity {} is not valid JSON.", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Entity Id of Newly Created Entity {}. {}", entityName, e.getStackTrace());
        }
        return newEntityId;
    }

    private String editSource(String responseBody, CustomAssert customAssert){

        JSONObject editResponseJson = new JSONObject();

        try {
            editResponseJson = new JSONObject(responseBody);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");
            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

        }catch (Exception e){
            customAssert.assertTrue(false,"Source Edit API is not validated");
        }
        return editResponseJson.toString();
    }

    private String createPayloadStringFromArray(String[] list) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += "{\"id\": \"" + list[i] + "\"},";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
    }

    private String createPayloadFromArray(String[] list,String[] emailName) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += "{\"name\":\""+emailName[i]+"\",\"id\":"+list[i]+"},";

        }
        payload = "["+payload.substring(0,payload.length()-1)+"]";
        return payload;
    }

    private boolean deleteSource(int sourceId){
        Boolean updateDb=true;
        String dbName = "slif";

        try{
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            postgreSQLJDBC.deleteDBEntry("Delete from source_integration_config where id=" +sourceId+ ";");
            postgreSQLJDBC.closeConnection();

        }catch (Exception e){
            logger.error("Error while updating db");
            updateDb = false;
        }
        return updateDb;
    }
}
