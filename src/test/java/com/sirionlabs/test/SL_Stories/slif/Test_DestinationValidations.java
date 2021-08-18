package com.sirionlabs.test.SL_Stories.slif;

import com.jayway.jsonpath.JsonPath;
import com.mysql.cj.xdevapi.JsonArray;
import com.sirionlabs.api.ServiceLevel.SLIF_Schemas;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.servicedata.TblauditlogsFieldHistory;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.velocity.runtime.directive.Parse;
import org.joda.time.DateTimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

public class Test_DestinationValidations extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(Test_DestinationValidations.class);

    String configFilePath;
    String configFileName;

    String adminUserName;
    String adminPassword;
    int destinationId;
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

    @Test(enabled = true)
    public void TestCreateDestination() {

        CustomAssert customAssert = new CustomAssert();
        try {
            Check check = new Check();
            check.hitCheck(adminUserName,adminPassword);
            String destname = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"service now uc2", "name") + DateUtils.getCurrentDateInMM_DD_YYYY();
            String[] slaId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"destination config details", "slaitem").split(",");
            int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"service now uc2", "sourceid"));
            String query = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"destination config details", "query");
            int frequencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "frequency"));
            String[] slaSubCategory = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"destination config details", "slacategory").split(",");
            String[] computationFrequency = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "computation frequency").split(",");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "entity type id"));

            destinationId = createDestination(destname, sourceId, frequencyId, slaId, slaSubCategory, computationFrequency, entityTypeId, query, customAssert);

            if(destinationId == -1){
                customAssert.assertTrue(false,"Destination not created successfully");
                customAssert.assertAll();
            }else {
                customAssert.assertTrue(true, "Destination Created successfully");
                logger.info("Destination has been created successfully "+destinationId+"");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Error while validating create API for destination");
        }
        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "TestCreateDestination", enabled = true)
    public void TestEditDestination() {

        CustomAssert customAssert = new CustomAssert();
        JSONObject updateResponseJson;
        String status = "success";
        String newName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","name") +DateUtils.getCurrentDateInMM_DD_YYYY();
        String[] emailId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","emailid").split(",");
        String[] emailName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","email").split(",");
        String frequency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","frequency");
        String[] performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","performancestatus").split(",");
        String[] performanceId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","performanceid").split(",");
        String[] slaItem = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","slaitem").split(",");
        String[] slaCategory = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","slacategory").split(",");
        String[] slaId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","slaid").split(",");
        String newQuery = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","query");
        String newEmail = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","newemail");
        String newFrequency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","newfrequency");
        String newDate = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","newdate")+" ";
        String newPerformance = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","newperformance");
        String newSla = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","newsla");
        String newTimeZone = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","timezone");
        String timeZoneId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"edit dest detail","timezoneid");
        int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"audit details","listid"));
        String data = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"audit details","data");
        String updateData = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"audit details","updatedata");

        try{
            APIResponse editResponse = SLIF_Schemas.hitGetEditDestination(executor, destinationId);
            String responseBody = editResponse.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Edit Destination API Response is not a valid Json");
                customAssert.assertTrue(false,"Edit Destination API Response is not a valid Json");
                customAssert.assertAll();
            }
            String response = editDestRemoveJsonExtraFields(responseBody,customAssert);
            JSONObject editResponseJson = new JSONObject(response);

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values",newName);
            JSONArray updateEmail = new JSONArray(createPayloadFromArray(emailId,emailName));
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("email").put("values",updateEmail);
            JSONArray performance = new JSONArray(createPayloadSLAFromArray(performanceStatus,performanceId));
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceStatus").put("values",performance);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("frequency").getJSONObject("values").put("name",frequency);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("frequency").getJSONObject("values").put("id",3);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("datePattern").put("values","10-10-2020");
            JSONArray sla = new JSONArray(createPayloadSLAFromArray(slaItem,slaCategory,slaId));
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slaIds").put("values",sla);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("configTimeZone").getJSONObject("values").put("name",newTimeZone);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("configTimeZone").getJSONObject("values").put("id",timeZoneId);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicData").getJSONObject("values").put("snowQuery",newQuery);

            APIResponse updateResponse = SLIF_Schemas.hitPostEditDestination(executor, editResponseJson.toString());
            String updateResponseBody = updateResponse.getResponseBody();
            updateResponseJson = new JSONObject(updateResponseBody);

            String statusMessage =  updateResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString();

            Assert.assertTrue(statusMessage.equalsIgnoreCase(status),"Edit is done unsuccessfully");

            APIResponse response1 = SLIF_Schemas.hitGetShowDestination(executor, destinationId);
            String responseBody1 = response1.getResponseBody();

            String showName = JsonPath.read(responseBody1,"body.data.name.values").toString();
            String showEmail = JsonPath.read(responseBody1,"body.data.email.values[*].name").toString();
            String showFrequency = JsonPath.read(responseBody1,"body.data.frequency.values.name").toString();
            String showDate = JsonPath.read(responseBody1,"body.data.datePattern.displayValues").toString();
            String showPerformance = JsonPath.read(responseBody1,"body.data.performanceStatus.values[*].name").toString();
            String showSla = JsonPath.read(responseBody1,"body.data.slaIds.values[*].name").toString();
            String showTimeZone = JsonPath.read(responseBody1,"body.data.configTimeZone.values.name").toString();
            String showQuery = JsonPath.read(responseBody1,"body.data.dynamicData.values.snowQuery").toString();

            Assert.assertTrue(showName.equalsIgnoreCase(newName),"Name edit is done unsuccessfully");
            Assert.assertTrue(showEmail.equalsIgnoreCase(newEmail),"Email edit is done unsuccessfully");
            Assert.assertTrue(showFrequency.equalsIgnoreCase(newFrequency),"Frequency edit is done unsuccessfully");
            Assert.assertTrue(showDate.equalsIgnoreCase(newDate),"DatePattern edit is done unsuccessfully");
            Assert.assertTrue(showPerformance.equalsIgnoreCase(newPerformance),"Performance Status edit is done unsuccessfully");
            Assert.assertTrue(showSla.equalsIgnoreCase(newSla),"Sla edit is done unsuccessfully");
            Assert.assertTrue(showQuery.equalsIgnoreCase(newQuery),"Query edit is done unsuccessfully");
            Assert.assertTrue(showTimeZone.equalsIgnoreCase(newTimeZone),"Time Zone edit is done unsuccessfully");

            int entityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"audit details","destentityid"));;
            String payload = "{\"filterMap\":{}}";
            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
            listRendererTabListData.hitListRendererTabListData(listId,entityId,destinationId,payload);
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
            String freq = JsonPath.read(auditResponse,"value[4].newValue");
            String date = JsonPath.read(auditResponse,"value[5].newValue");
            String zone = JsonPath.read(auditResponse,"value[6].newValue");

            Assert.assertTrue(name.equalsIgnoreCase(newName),"Audit logs is done unsuccessfully");
            Assert.assertTrue(freq.equalsIgnoreCase(newFrequency),"Audit logs is done unsuccessfully");
            Assert.assertTrue(date.equalsIgnoreCase(newDate),"Audit logs is done unsuccessfully");
            Assert.assertTrue(zone.equalsIgnoreCase(newTimeZone),"Audit logs is done unsuccessfully");


        }catch (Exception e){
            customAssert.assertTrue(false,"Edit is done unsuccessfully" + e.getMessage());
        }finally {
            destinationDelete(destinationId);
        }
        customAssert.assertAll();
    }

    private String editDestRemoveJsonExtraFields(String responseBody, CustomAssert customAssert){

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

    public int createDestination(String name,int sourceId,int frequencyId,String[] slaId, String[] slaSubCategory,String[] computationFrequency ,int entityTypeId,String query,CustomAssert customAssert){

        int destinationId = -1;
        try{
            String payload = createPayloadForDestination(name,sourceId,frequencyId,slaId,slaSubCategory,computationFrequency,entityTypeId,query);

            APIResponse response = SLIF_Schemas.hitPostCreateDestination(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create destination API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create destination API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Create Destination API Response is not a valid Json");
                customAssert.assertTrue(false,"Create Destination API Response is not a valid Json");

            }else {
                destinationId = getDestinationId(responseBody, "destination", "destination");

                if (destinationId == -1) {
                    customAssert.assertTrue(false, "Destination ID not created successfully.");
                }
                else{
                    customAssert.assertTrue(true, "Destination ID created successfully.");
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating Destination Create API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Destination Create API " + e.getMessage());
        }

        return destinationId;

    }

    private String createPayloadForDestination(String name,int sourceId, int frequencyId,String[] slaId,String[] subCategory,String[] computationFrequency,int entityTypeId,String query){

        String description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","description");
        String active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","active");
        String[] email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","email").split(",");
        int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","supplier"));

        String timeZone = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","time zone");
        String datePattern = DateUtils.getCurrentDateInDD_MM_YYYY();
        String[] contractId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","contract").split(",");
        String[] performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance status").split(",");
        String[] performanceParentId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance parent id").split(",");
        int emailID = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","emailid"));;

        String payload = "{\"body\":{\"data\":{\"dynamicData\":{\"name\":\"dynamicData\",\"multiEntitySupport\"" +
                ":false,\"values\":{\"snowQuery\":\""+query+"\"}},\"frequency\":{\"name\":\"frequency\",\"id\":12649," +
                "\"multiEntitySupport\":false,\"values\":{\"id\":"+frequencyId+"}},\"canSupplierBeParent\":true," +
                "\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"multiEntitySupport\":false,\"values\":\""+datePattern+"\"," +
                "\"displayValues\":\"11-19-2020\"},\"sourceIntegrationConfig\":{\"name\":\"sourceIntegrationConfig\"," +
                "\"id\":12641,\"multiEntitySupport\":false,\"values\":{\"id\":"+sourceId+"}},\"contractIds\":" +
                "{\"name\":\"contractIds\",\"id\":12647,\"multiEntitySupport\":false,\"values\":["+createPayloadStringFromArray(contractId)+"]}," +
                "\"active\":{\"name\":\"active\",\"id\":12640,\"values\":"+active+",\"multiEntitySupport\":false}," +
                "\"slaIds\":{\"name\":\"slaIds\",\"id\":12648,\"multiEntitySupport\":false,\"values\":" +
                "["+createPayloadSLAStringFromArray(slaId,subCategory)+"]},\"name\":{\"name\":\"name\",\"id\":" +
                "12638,\"multiEntitySupport\":false,\"values\":\""+name+"\"},\"supplierId\":{\"name\":\"supplierId\"," +
                "\"id\":12646,\"multiEntitySupport\":false,\"values\":{\"id\":"+supplierId+"}},\"configTimeZone\":" +
                "{\"name\":\"configTimeZone\",\"id\":12652,\"multiEntitySupport\":false,\"values\":{\"id\":"+timeZone+"}}," +
                "\"description\":{\"name\":\"description\",\"id\":12639,\"multiEntitySupport\":false,\"values\":\""+description+"\"}," +
                "\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Destination Integration Config\",\"id\":346}," +
                "\"multiEntitySupport\":false},\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":12671," +
                "\"multiEntitySupport\":false,\"values\":["+createPayloadSLAStringFromArray(performanceStatus,performanceParentId)+"]}," +
                "\"email\":{\"name\":\"email\",\"id\":"+emailID+",\"multiEntitySupport\":false,\"values\":["+createPayloadStringFromArray(email)+"]}," +
                "\"computationFrequency\":{\"name\":\"computationFrequency\",\"id\":12661,\"multiEntitySupport\":false," +
                "\"values\":["+createPayloadStringFromArray(computationFrequency)+"]},\"entityTypeId\":{\"name\":" +
                "\"entityTypeId\",\"values\":"+entityTypeId+",\"multiEntitySupport\":false}}}}";

        return payload;
    }

    public static int getDestinationId(String createJsonStr, String entityName,String notification) {
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
                    logger.error("New Entity {} not created. ", entityName);
                }
            } else {
                logger.error("Create Response for Entity {} is not valid JSON.", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Entity Id of Newly Created Entity {}. {}", entityName, e.getStackTrace());
        }
        return newEntityId;
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

    private String createPayloadSLAStringFromArray(String[] slaItem, String[] slaCategory) {
        String payload = "";
        for (int i = 0; i < slaItem.length; i++) {
            payload += "{ \"id\": "+slaItem[i]+", \"parentId\": "+slaCategory[i]+" },";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
    }

    private String createPayloadSLAFromArray(String[] slaItem, String[] slaCategory) {
        String payload = "";
        for (int i = 0; i < slaItem.length; i++) {
            payload += "{ \"name\": "+slaItem[i]+", \"id\": "+slaCategory[i]+" },";
        }
        payload = "["+payload.substring(0,payload.length()-1)+"]";
        return payload;
    }

    private String createPayloadSLAFromArray(String[] slaItem, String[] slaCategory,String[] slaId) {
        String payload = "";
        for (int i = 0; i < slaItem.length; i++) {
            payload += "{\"name\":\""+slaItem[i]+"\",\"id\":"+slaId[i]+",\"parentId\":"+slaCategory[i]+"},";
        }
        payload = "["+payload.substring(0,payload.length()-1)+"]";
        return payload;
    }

    private boolean destinationDelete(int destinationId){

        Boolean destinationDelete=true;

        try{

            Boolean deleteQuartzDest = deleteDest(destinationId);
            if (deleteQuartzDest == true) {

                dbName = "slif";
                postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
                postgreSQLJDBC.deleteDBEntry("delete from destination_integration_config where id='" + destinationId + " ';");
            }

        }catch (Exception e){
            destinationDelete = false;
        }

        return destinationDelete;
    }

    private Boolean deleteDest(int destinationId){
        Boolean updateDb=true;
        dbName = "slif";

        try{
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            postgreSQLJDBC.deleteDBEntry("Update destination_integration_config set active='false' where id = "+destinationId+ "';");
            postgreSQLJDBC.closeConnection();

        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

}
