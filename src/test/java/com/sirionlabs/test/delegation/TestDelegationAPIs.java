package com.sirionlabs.test.delegation;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.delegation.CreateDelegation;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;


public class TestDelegationAPIs extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestDelegationAPIs.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String clientAdminUserName = null;
    private static String clientAdminUserPassword = null;
    private static String delegatedByEndUserName = null;
    private static String delegatedByEndUserPassword = null;
    private static String delegatedToEndUserName = null;
    private static String delegatedToEndUserNPassword = null;
    private static String delegatedByEndUserDomainId = null;
    private static String delegatedToEndUserDomainId = null;
    private static String domainUserIdsFromAPI = null;
    private static List<String> domainAllUserIds = new LinkedList<>();
    private static List<String> domainAllDelegatedToUserName = new LinkedList<>();
    private static String size ="20";
    static List<Integer> domainUserIdIndexes = new LinkedList<>();
    static List<Integer> domainUserNameIndexes = new LinkedList<>();
    static List<String> domainAllDelegatedToEntityIds = new LinkedList<>();
    SoftAssert softAssert = new SoftAssert();
    CustomAssert customAssert = new CustomAssert();
    APIValidator validator;
    JSONObject jsonObject = null;

    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("delegationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("delegationConfigFileName");
        clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "client admin user details", "username");
        clientAdminUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "client admin user details", "password");
        delegatedByEndUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delegated by end user details", "username");
        delegatedByEndUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delegated by end user details", "password");
        delegatedToEndUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delegated to end user details", "username");
        delegatedToEndUserNPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delegated to end user details", "password");
        delegatedByEndUserDomainId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delegated by end user details", "domainuserid");
    }

//    @Test(dataProvider = "CreateDelegationData",priority = 1)
    public void TestCreateDelegationFromClientAdmin(String startDate,String endDate, String delegatedUserId,String domainUserIds){
        CreateDelegation createDelegation = new CreateDelegation();
        List<String> userIds=getDomainUserIds(domainUserIds).stream().map(m->m.split(":;")[0]).collect(Collectors.toList());
        String payload = getCreateDelegationPayload(startDate,endDate,delegatedUserId,userIds,delegatedByEndUserDomainId);
        domainUserIdIndexes = getIndexesForUsers(userIds);
        try {
            createDelegation.hitCreateDelegation(payload);
            softAssert.assertTrue(createDelegation.getcreateDelegationJsonStr().contains("\"success\":true"),"Delegation is not created successfully");
            String jsonResponse=null;
            if(APIUtils.validJsonResponse(createDelegation.getcreateDelegationJsonStr())){
               jsonResponse=getDomainUserIdsFromListRendererAPI(size);
                Integer delegatedUserColumnId=ListDataHelper.getColumnIdFromColumnName(jsonResponse,"delegateduser");
                domainAllDelegatedToUserName=getJSONObjectsDataforParticularKey(jsonResponse,String.valueOf(delegatedUserColumnId));
                domainUserNameIndexes = getIndexesForUserName(domainAllDelegatedToUserName,domainUserIdIndexes,"Akshay User1");
            }
            softAssert.assertTrue(domainUserIdIndexes.equals(domainUserNameIndexes),"Domain Id is not linked with Delegated End User");
            List<HashMap<String,String>> delegatedByMeResponseAllDataClientAdmin = delegatedToMeResponseHashMap(jsonResponse,"bulkcheckbox","entitytype","entitystatus",
                   "rolegroupname","delegateduser","startdate","enddate" );

            loginWithDelegatedByEndUser();
            String delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");
            softAssert.assertTrue(APIUtils.validJsonResponse(delegatedByMeResponse),
                    "Delegated By Me Response is not valid json");

            List<HashMap<String,String>> delegatedByMeResponseAllDataEndUser = delegatedToMeResponseHashMap(delegatedByMeResponse,"bulkcheckbox","entitytype","entitystatus",
                    "rolegroupname","domainuser","startdate","enddate");

            boolean delegationCreated = false;
            int matchedCounter =0;
            for(Integer userId: domainUserIdIndexes) {
            for(HashMap<String,String> endUserDelegateByMe : delegatedByMeResponseAllDataEndUser){
                    if (endUserDelegateByMe.values().stream().collect(Collectors.toList()).equals(delegatedByMeResponseAllDataClientAdmin.get(userId).values().stream().collect(Collectors.toList()))) {
                        delegationCreated = true;
                        matchedCounter++;
                    }
                }
            }

            softAssert.assertTrue(delegationCreated && matchedCounter ==domainUserIdIndexes.size(),"Delegation By Me is not created successfully from Client Admin as it is not getting reflected in end-user");
            loginWithDelegatedToEndUser();
            String delegatedToMeResponse =hitDelegatedToMeAPI(size, "{}");
            softAssert.assertTrue(APIUtils.validJsonResponse(delegatedToMeResponse),
                    "Delegated By Me Response is not valid json");

            List<HashMap<String,String>> delegatedToMeResponseAllDataEndUser = delegatedToMeResponseHashMap(delegatedToMeResponse,"entitytype","entitystatus",
                    "rolegroupname","domainuser","startdate","enddate");
            softAssert.assertAll();

        } catch (Exception e) {
            e.getMessage();
        }
    }

/*    @Test(dataProvider = "",priority = 2)
    public void TestDeleteDelegation(String startDate, String endDate, String delegatedUserId,String domainUserIds){
        SoftAssert softAssert = new SoftAssert();
        DeleteDelegation deleteDelegation = new DeleteDelegation();
        String payload = getDeleteDelegationPayload("",null);
        try {
            HttpResponse httpResponse = deleteDelegation.hitDeleteDelegation(payload);
            APIUtils.validJsonResponse(httpResponse.toString());
        } catch (Exception e) {
            e.getMessage();
        }
    }*/


// Create and Delete Delegation from End-User and verify whether delegation is reflecting in delegation by me tab
//C46190 , C46191
//@Test
public void TestDelegationCreationFromEndUser() throws ParseException, IOException {
    loginWithDelegatedByEndUser();
    String delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedByMeResponse),
            "Delegated By Me Response is not valid json");

    List<HashMap<String,String>> delegatedByMeResponseAllDataEndUserBeforeDelegation = delegatedToMeResponseHashMap(delegatedByMeResponse,"bulkcheckbox","entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus");

    String startDate = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),1,"MM-dd-yyyy HH:mm:ss");
    String endDate = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),10,"MM-dd-yyyy HH:mm:ss");

    HttpGet getRequest;
    String queryString = "/delegation/create-form";
    getRequest = new HttpGet(queryString);
    getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    getRequest.addHeader("Accept-Encoding", "gzip, deflate");

    HttpResponse response = APIUtils.getRequest(getRequest);
    String jsonString = EntityUtils.toString(response.getEntity());

    jsonObject = new JSONObject(jsonString);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").getJSONObject("values").put("name","Purchase Order");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").getJSONObject("values").put("id",181);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").getJSONObject("values").put("name","Akshay User1");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").getJSONObject("values").put("id",1193);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("name","Purchase Order Manager");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("id",2065);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("parentId",181);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("parentName","Purchase Order");

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values", startDate);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("endDate").put("values", endDate);
    jsonObject.getJSONObject("body").getJSONObject("data").put("domainUserIds", new JSONArray());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONArray("domainUserIds").put(653307);

    String payload = "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

    HttpPost postRequest;
    queryString = "/delegation/createV2";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

    response = APIUtils.postRequest(postRequest,payload);
    jsonString = EntityUtils.toString(response.getEntity());

    softAssert.assertTrue(jsonString.contains("Delegation Created Successfully"),
           "Delegation is not created Successfully");


    delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");

    jsonObject = new JSONObject(delegatedByMeResponse);

    List<HashMap<String,String>> delegatedByMeResponseAllDataEndUserAfterDelegation = delegatedToMeResponseHashMap(delegatedByMeResponse,"bulkcheckbox","entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus","delegationid");
    List<String> delegatedIds = new LinkedList<>();
    delegatedIds.add(delegatedByMeResponseAllDataEndUserAfterDelegation.stream().filter(m->m.get("bulkcheckbox").contains("653307")).map(m->m.get("delegationid")).collect(Collectors.joining()));

    queryString = "/delegation/delete";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

    response = APIUtils.postRequest(postRequest,"{\"delegationIds\":"+ delegatedIds +"}");
    jsonString = EntityUtils.toString(response.getEntity());

    softAssert.assertTrue(jsonString.contains("Delegation Deleted Successfully"),
            "Delegation is not deleted Successfully");

    softAssert.assertAll();
}

//C46387
//@Test
public void verifyExcelDownloadFromMeAndToTabs(){
    loginWithDelegatedByEndUser();
    String delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedByMeResponse),
            "Delegated By Me Response is not valid json");

    List<HashMap<String,String>> delegatedByMeResponseAllDataEndUser = delegatedToMeResponseHashMap(delegatedByMeResponse,"entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus","supplier","contract");

    String outputFilePath = downloadListDataForAllColumns("",null,"delegation",336,"DelegatedByMe",softAssert);

    List<List<String>> rowvalues = XLSUtils.getExcelDataOfMultipleRows(outputFilePath.replace("/DelegatedByMe.xlsx","").trim(),"DelegatedByMe.xlsx","Data",4, (XLSUtils.getNoOfRows(outputFilePath.replace("/DelegatedByMe.xlsx","").trim(),"DelegatedByMe.xlsx","Data").intValue()-2));

    softAssert.assertTrue(delegatedByMeResponseAllDataEndUser.size() == rowvalues.size()-2,
            "Number of records in UI is same as NUmber of records in Excel");

    String delegatedToMeResponse =hitDelegatedToMeAPI(size,"{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedToMeResponse),
            "Delegated By Me Response is not valid json");

    List<HashMap<String,String>> delegatedToMeResponseAllDataEndUser = delegatedToMeResponseHashMap(delegatedToMeResponse,"entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus","supplier","contract");

    outputFilePath = downloadListDataForAllColumns("",null,"delegation",335,"DelegatedToMe",softAssert);

    rowvalues = XLSUtils.getExcelDataOfMultipleRows(outputFilePath.replace("/DelegatedToMe.xlsx","").trim(),"DelegatedToMe.xlsx","Data",4, (XLSUtils.getNoOfRows(outputFilePath.replace("/DelegatedToMe.xlsx","").trim(),"DelegatedToMe.xlsx","Data").intValue()-2));

    softAssert.assertTrue(delegatedToMeResponseAllDataEndUser.size() == rowvalues.size()-2,
            "Number of records in UI is same as NUmber of records in Excel");

    softAssert.assertAll();
}

// C46195
//@Test
public void checkDelegationFilterColumns() throws IOException {
    loginWithDelegatedByEndUser();
    HttpPost postRequest;
    String queryString = "/listRenderer/list/336/defaultUserListMetaData/?newUX=true";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

    HttpResponse response = APIUtils.postRequest(postRequest,"{}");
    String jsonString = EntityUtils.toString(response.getEntity());

    List<String> allColumns = new LinkedList<>();
    jsonObject = new JSONObject(jsonString);
    JSONArray jsonArray = jsonObject.getJSONArray("columns");
    for(int i=0;i<jsonArray.length();i++){
        allColumns.add(jsonArray.getJSONObject(i).get("defaultName").toString());
    }

    softAssert.assertTrue(allColumns.contains("DELEGATION STATUS"),
            "DELEGATION STATUS column is not present in delegation by me");

    queryString = "/listRenderer/list/335/defaultUserListMetaData/?newUX=true";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

   response = APIUtils.postRequest(postRequest,"{}");
   jsonString = EntityUtils.toString(response.getEntity());

    allColumns = new LinkedList<>();
    jsonObject = new JSONObject(jsonString);
    jsonArray = jsonObject.getJSONArray("columns");
    for(int i=0;i<jsonArray.length();i++){
        allColumns.add(jsonArray.getJSONObject(i).get("defaultName").toString());
    }

    softAssert.assertTrue(allColumns.contains("DELEGATION STATUS"),
            "DELEGATION STATUS column is not present in delegation to me tab");

    softAssert.assertAll();
}


//@Test
public void checkInactivateUserFunctionality() throws IOException, ParseException, SQLException {
    loginWithDelegatedByEndUser();
    String delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedByMeResponse),
            "Delegated By Me Response is not valid json");

    String startDate = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),1,"MM-dd-yyyy HH:mm:ss");
    String endDate = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),10,"MM-dd-yyyy HH:mm:ss");

    HttpGet getRequest;
    String queryString = "/delegation/create-form";
    getRequest = new HttpGet(queryString);
    getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    getRequest.addHeader("Accept-Encoding", "gzip, deflate");

    HttpResponse response = APIUtils.getRequest(getRequest);
    String jsonString = EntityUtils.toString(response.getEntity());

    jsonObject = new JSONObject(jsonString);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").getJSONObject("values").put("name","Purchase Order");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").getJSONObject("values").put("id",181);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").getJSONObject("values").put("name","Akshay User1");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").getJSONObject("values").put("id",1193);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("name","Purchase Order Manager");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("id",2065);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("parentId",181);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("parentName","Purchase Order");

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values", startDate);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("endDate").put("values", endDate);
    jsonObject.getJSONObject("body").getJSONObject("data").put("domainUserIds", new JSONArray());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONArray("domainUserIds").put(653307);

    String payload = "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

    HttpPost postRequest;
    queryString = "/delegation/createV2";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

    response = APIUtils.postRequest(postRequest,payload);
    jsonString = EntityUtils.toString(response.getEntity());

    softAssert.assertTrue(jsonString.contains("Delegation Created Successfully"),
            "Delegation is not created Successfully");


    delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");

    jsonObject = new JSONObject(delegatedByMeResponse);

    List<HashMap<String,String>> delegatedByMeResponseAllDataEndUserAfterDelegation = delegatedToMeResponseHashMap(delegatedByMeResponse,"bulkcheckbox","entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus","delegationid");

    softAssert.assertTrue(delegatedByMeResponseAllDataEndUserAfterDelegation.stream().filter(m->m.get("bulkcheckbox").contains("653307")).map(m->m.get("delegationstatus")).findFirst().get().equals("Active"),
            "Delegation is not created successfully");

    loginWithDelegatedToEndUser();
    String delegatedToMeResponse =hitDelegatedToMeAPI(size, "{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedToMeResponse),
            "Delegated By Me Response is not valid json");

    List<HashMap<String,String>> delegatedToMeResponseAllDataEndUser = delegatedToMeResponseHashMap(delegatedToMeResponse,"entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus");

    softAssert.assertTrue(delegatedToMeResponseAllDataEndUser.stream().filter(m->m.get("entityid").equals( delegatedByMeResponseAllDataEndUserAfterDelegation.stream()
                    .filter(m1->m1.get("bulkcheckbox").contains("653307")).map(m1->m1.get("entityid")).findFirst().get()))
                    .map(m->m.get("delegationstatus")).findFirst().get().equals("Active"),
            "Delegation is not created successfully");

    loginWithClientAdminUser();

    response = hitInactivateUser(delegatedByEndUserDomainId);

    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200 ,
            "User has not been inactivated");

    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("192.168.2.193","5432","RC_1.22","postgres","5ow84=9I5F");
    String query = "SELECT active from app_user WHERE login_id='akshay_user' and client_id=1005";
    List<List<String>> activeStatus = postgreSQLJDBC.doSelect(query);

    String status = activeStatus.stream().findFirst().get().stream().findFirst().get();

    softAssert.assertTrue(status.equals("false"),"User is not inactivated successfully");

    loginWithDelegatedByEndUser();
    delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");

    jsonObject = new JSONObject(delegatedByMeResponse);

    List<HashMap<String,String >> delegatedByMeResponseAllDataEndUserAfterAccountInactive = delegatedToMeResponseHashMap(delegatedByMeResponse,"bulkcheckbox","entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus","delegationid");

    softAssert.assertTrue(delegatedByMeResponseAllDataEndUserAfterAccountInactive.stream().filter(m->m.get("bulkcheckbox").contains("653307")).map(m->m.get("delegationstatus")).findFirst().get().equals("Deleted"),
            "Delegation is not created successfully");

    loginWithDelegatedToEndUser();
    delegatedToMeResponse =hitDelegatedToMeAPI(size, "{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedToMeResponse),
            "Delegated By Me Response is not valid json");

    List<HashMap<String,String>> delegatedToMeResponseAllDataEndUserAfterAccountInactive = delegatedToMeResponseHashMap(delegatedToMeResponse,"entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus");

    softAssert.assertTrue(delegatedToMeResponseAllDataEndUserAfterAccountInactive.stream().filter(m->m.get("entityid").equals( delegatedByMeResponseAllDataEndUserAfterAccountInactive.stream()
                    .filter(m1->m1.get("bulkcheckbox").contains("653307")).map(m1->m1.get("entityid")).findFirst().get()))
                    .map(m->m.get("delegationstatus")).findFirst().get().equals("Deleted"),
            "Delegation is not created successfully");

    query = "update app_user  set temp_pwd_creation_time =null, first_login =false , last_login_time = now() , failed_login_count = 0 , password_never_expires = true , account_locked_time = null, \n" +
            "password= '84716ee1e851f731cdee4810258914d91e55340e0a75d8864aefa4ca330177ea' where id =1183;";

    boolean passwordUpdate = postgreSQLJDBC.updateDBEntry(query);

    softAssert.assertTrue(passwordUpdate == true,"Password Update is not successfully");
    softAssert.assertAll();
}

//@Test
public void verifyUserRemovalFromRoleGroup() throws IOException, ParseException {
    loginWithDelegatedByEndUser();
    String delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedByMeResponse),
            "Delegated By Me Response is not valid json");

    String startDate = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),1,"MM-dd-yyyy HH:mm:ss");
    String endDate = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),10,"MM-dd-yyyy HH:mm:ss");

    HttpGet getRequest;
    String queryString = "/delegation/create-form";
    getRequest = new HttpGet(queryString);
    getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    getRequest.addHeader("Accept-Encoding", "gzip, deflate");

    HttpResponse response = APIUtils.getRequest(getRequest);
    String jsonString = EntityUtils.toString(response.getEntity());

    jsonObject = new JSONObject(jsonString);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").getJSONObject("values").put("name","Change Requests");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityTypes").getJSONObject("values").put("id",63);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").getJSONObject("values").put("name","Akshay User1");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("users").getJSONObject("values").put("id",1193);

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").put("values",new JSONObject());
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("name","Change Requests Managers");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("id",2061);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("parentId",63);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("roleGroups").getJSONObject("values").put("parentName","Change Requests");

    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values", startDate);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("endDate").put("values", endDate);
    jsonObject.getJSONObject("body").getJSONObject("data").put("domainUserIds", new JSONArray());

    response = getAvailableUserDomainIds();
    String jsonStr = EntityUtils.toString(response.getEntity());
    JSONObject domainIdJsonObject = new JSONObject(jsonStr);
    int columnId = ListDataHelper.getColumnIdFromColumnName(jsonStr,"bulkcheckbox");

    List<String> allAvailableDomainUserId = new LinkedList<>();
    for(int i=0;i<domainIdJsonObject.getJSONArray("data").length();i++){
        allAvailableDomainUserId.add(domainIdJsonObject.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[0]);
    }

    String domainUserId = allAvailableDomainUserId.stream().findAny().get();
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONArray("domainUserIds").put(Integer.valueOf(domainUserId));

    String payload = "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

    HttpPost postRequest;
    queryString = "/delegation/createV2";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

    response = APIUtils.postRequest(postRequest,payload);
    jsonString = EntityUtils.toString(response.getEntity());

    softAssert.assertTrue(jsonString.contains("Delegation Created Successfully"),
            "Delegation is not created Successfully");


    delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");

    jsonObject = new JSONObject(delegatedByMeResponse);

    int initialFilteredRows = (Integer) jsonObject.get("filteredCount");

    List<HashMap<String,String>> delegatedByMeResponseAllDataEndUserAfterDelegation = delegatedToMeResponseHashMap(delegatedByMeResponse,"bulkcheckbox","entitytype","entityid","entitystatus",
            "rolegroupname","domainuser","startdate","enddate","delegationstatus","delegationid");

    softAssert.assertTrue(delegatedByMeResponseAllDataEndUserAfterDelegation.stream().filter(m->m.get("bulkcheckbox").contains(domainUserId)).map(m->m.get("delegationstatus")).findFirst().get().equals("Active"),
            "Delegation is not created successfully");

    String entityId = delegatedByMeResponseAllDataEndUserAfterDelegation.stream()
            .filter(m1->m1.get("bulkcheckbox").contains(domainUserId)).map(m1->m1.get("entityid")).findFirst().get();

    response =  getStackHoldersFromRoleGroup(entityId.split(":;")[1]);

    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Not able to fetch stakeholders details from entity show page");

    String stakeHoldersDetailsResponseStr = EntityUtils.toString(response.getEntity());

    jsonObject = new JSONObject(stakeHoldersDetailsResponseStr);
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_2061").getJSONArray("values").remove(0);


    payload =  "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

    response = editStakeHolderDetails(payload);

    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Stake holders are not updated");

    delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");

    jsonObject = new JSONObject(delegatedByMeResponse);

    int finalFilteredRows = (Integer) jsonObject.get("filteredCount");

    softAssert.assertTrue(initialFilteredRows - finalFilteredRows ==1,
            "Stake holder has not been edited");

    //Reverting stake - holders
    response =  getStackHoldersFromRoleGroup(entityId.split(":;")[1]);

    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Not able to fetch stakeholders details from entity show page");

    stakeHoldersDetailsResponseStr = EntityUtils.toString(response.getEntity());

    jsonObject = new JSONObject(stakeHoldersDetailsResponseStr);
    JSONObject stakeHolderJson = new JSONObject("{\"name\": \"Akshay User\",\"id\": 1183,\"type\": 2,\"email\": null}");
    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_2061").getJSONArray("values").put(stakeHolderJson);

    payload =  "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

    response = editStakeHolderDetails(payload);

    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Stake holders are not updated");
    softAssert.assertAll();
}


//@Test
public void globalPermissionsDelegation() throws IOException, ParseException {
    loginWithNoDelegationPermissionUser("access_check","admin123");
    HttpGet getRequest;
    String queryString = "/delegation/create-form";
    getRequest = new HttpGet(queryString);
    getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    getRequest.addHeader("Accept-Encoding", "gzip, deflate");

    HttpResponse response = APIUtils.getRequest(getRequest);
    String jsonString = EntityUtils.toString(response.getEntity());

    softAssert.assertTrue(!APIUtils.validJsonResponse(jsonString),
            "Its a valid JSON which means create form is being visible for user who didn't have" +
                    "permission to delegate");

    softAssert.assertAll();
}

public HttpResponse editStakeHolderDetails(String payload){
    HttpResponse response = null;
    try {
        HttpPost postRequest;
        String queryString = "/ccrs/edit";
        logger.debug("Query string url formed is {}", queryString);
        postRequest = new HttpPost(queryString);
        postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        postRequest.addHeader("Accept-Encoding", "gzip, deflate");

        APIUtils apiUtils = new APIUtils();
        response = apiUtils.postRequest(postRequest, payload);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse getAvailableUserDomainIds(){
    HttpResponse response = null;
    try {
        HttpPost postRequest;
        String queryString = "/listRenderer/list/431/listdata?version=2.0";
        logger.debug("Query string url formed is {}", queryString);
        postRequest = new HttpPost(queryString);
        postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        postRequest.addHeader("Accept-Encoding", "gzip, deflate");

        String payload = "{\"filterMap\":{\"entityTypeId\":63,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{},\"roleGroupId\":2061}}";
        APIUtils apiUtils = new APIUtils();
        response = apiUtils.postRequest(postRequest, payload);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

// C46196
@Test
public void testFilterInDelegationTabs() throws IOException {
    loginWithDelegatedByEndUser();
    HttpPost postRequest;
    String queryString = "/listRenderer/list/336/defaultUserListMetaData/?newUX=true";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

    HttpResponse response = APIUtils.postRequest(postRequest,"{}");
    String jsonString = EntityUtils.toString(response.getEntity());

    List<String> allColumns = new LinkedList<>();
    jsonObject = new JSONObject(jsonString);
    JSONArray jsonArray = jsonObject.getJSONArray("filterMetadatas");
    for(int i=0;i<jsonArray.length();i++){
        allColumns.add(jsonArray.getJSONObject(i).get("defaultName").toString());
    }

    softAssert.assertTrue(allColumns.contains("CONTRACT") && allColumns.contains("Role Group")
                    && allColumns.contains("SUPPLIER") && allColumns.contains("Entity Type"),
            "All Filters are not present in delegation by me");


    String delegatedByMeResponse = hitDelegatedByMeAPI(size,"{}");
    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedByMeResponse),
            "Delegated By Me Response is not valid json");

    jsonObject =new JSONObject(delegatedByMeResponse);
    int defaultRecordsCount = (Integer) jsonObject.get("filteredCount");

    delegatedByMeResponse = hitDelegatedByMeAPI(size,
            "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{\"363\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"181\",\"name\":\"Purchase Order\"}]},\"filterId\":363,\"filterName\":\"entityType\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}}");
    int filteredRecordsCount = (Integer) jsonObject.get("filteredCount");


    softAssert.assertTrue(filteredRecordsCount<defaultRecordsCount,
            "Filter is been applied successfully");

    softAssert.assertTrue(APIUtils.validJsonResponse(delegatedByMeResponse),
            "Delegated By Me Response is not valid json");

    queryString = "/listRenderer/list/335/defaultUserListMetaData/?newUX=true";
    postRequest = new HttpPost(queryString);
    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    postRequest.addHeader("Accept-Encoding", "gzip, deflate");

    response = APIUtils.postRequest(postRequest,"{}");
    jsonString = EntityUtils.toString(response.getEntity());

    allColumns = new LinkedList<>();
    jsonObject = new JSONObject(jsonString);
    jsonArray = jsonObject.getJSONArray("filterMetadatas");
    for(int i=0;i<jsonArray.length();i++){
        allColumns.add(jsonArray.getJSONObject(i).get("defaultName").toString());
    }

    softAssert.assertTrue(allColumns.contains("CONTRACT") && allColumns.contains("Role Group")
            && allColumns.contains("SUPPLIER") && allColumns.contains("Entity Type"),
            "All Filters are not present in delegation to me tab");

    softAssert.assertAll();
}

public HttpResponse getStackHoldersFromRoleGroup(String entityId){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/ccrs/show/" + entityId + "?version=2.0";
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate");
        getRequest.addHeader("Content-Type","application/json;charset=UTF-8");

        response = APIUtils.getRequest(getRequest);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

      /* @Test
    public void TestDelegationEntityTypes(){
        SoftAssert softAssert = new SoftAssert();
        DelegationEntityTypes delegationEntityTypes = new DelegationEntityTypes();
        HttpResponse httpResponse = delegationEntityTypes.getDelegationEntityTypes();
        softAssert.assertEquals(httpResponse.getStatusLine().getStatusCode(),200,
                "Error in Response Code Response Code is " + httpResponse.getStatusLine().getStatusCode()
        + " and Response error is " + httpResponse.getStatusLine().toString());
        softAssert.assertTrue(APIUtils.validJsonResponse(delegationEntityTypes.getDelegationEntityTypesJsonStr()),
                "Error in Delegation Entity Types Response not a valid json");
    }*/

   /* @Test
    public void TestRoleGroupEntityTypes(){
        SoftAssert softAssert = new SoftAssert();
        RoleGroupForEntityType roleGroupForEntityType = new RoleGroupForEntityType();
        HttpResponse httpResponse = roleGroupForEntityType.getRoleGroupsForEntityType("61");
        softAssert.assertEquals(httpResponse.getStatusLine().getStatusCode(),200,
                "Error in Response Code Response Code is " + httpResponse.getStatusLine().getStatusCode()
                        + " and Response error is " + httpResponse.getStatusLine().toString());
        softAssert.assertTrue(APIUtils.validJsonResponse(roleGroupForEntityType.getRoleGroupForEntityTypeJsonStr()),
                "Error in Delegation Role Group For Entity Types Response not a valid json");
    }*/

   public HttpResponse hitInactivateUser(String userId){
       HttpResponse response = null;
       HttpGet getRequest;
       try{
           String queryString = "/tblusers/inactivate/" + userId;
           logger.debug("Query string url formed is {}", queryString);
           getRequest = new HttpGet(queryString);
           getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
           getRequest.addHeader("Accept-Encoding", "gzip, deflate");

           response = APIUtils.getRequest(getRequest);
           logger.debug("Response status is {}", response.getStatusLine().toString());

           Header[] headers = response.getAllHeaders();
           for (Header oneHeader : headers) {
               logger.debug("Delete Delegation API header {}", oneHeader.toString());
           }
       } catch (Exception e) {
           logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
       }
       return response;
   }

    public List<HashMap<String ,String>> delegatedToMeResponseHashMap(String response, String... columnNames){
        List<HashMap<String,String>> jsonValues = new LinkedList<>();
        HashMap<String,String> addColumnValues = null;
        JSONObject jsonObj = new JSONObject(response);
        Integer columnId;

            for (int i = 0; i < jsonObj.getJSONArray("data").length(); i++) {
                addColumnValues = new LinkedHashMap<>();
                for (int j = 0; j < columnNames.length; j++) {
                    columnId=ListDataHelper.getColumnIdFromColumnName(response,columnNames[j]);
                    addColumnValues.put(columnNames[j], jsonObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString());
                }
                jsonValues.add(addColumnValues);
            }
        return jsonValues;
    }

    private String hitDelegatedByMeAPI(String size,String filteredJson){
        HttpResponse response = null;
        String delegateByMe = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/list/336/listdata?version=2.0";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":"+ size+",\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":"+ filteredJson +"}}";
            APIUtils apiUtils = new APIUtils();
            response = apiUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            delegateByMe= EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return delegateByMe;
    }

    private String hitDelegatedToMeAPI(String size,String filteredJson){
        HttpResponse response = null;
        String delegateByMe = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/list/335/listdata?version=2.0";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":"+ size+",\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":" + filteredJson +"}}";
            APIUtils apiUtils = new APIUtils();
            response = apiUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            delegateByMe= EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return delegateByMe;
    }

    private String getDomainUserIdsFromListRendererAPI(String size){
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/list/334/listdata?contractId=&relationId=&vendorId=&am=true&domainUserId=" + delegatedByEndUserDomainId;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":"+ size+",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}";
            APIUtils apiUtils = new APIUtils();
            response = apiUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.domainUserIdsFromAPI = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return domainUserIdsFromAPI;
    }

    public List<Integer> getIndexesForUsers(List<String> userIds){
        List<Integer> indexes = new LinkedList<>();
        for(int i=0;i<this.domainAllUserIds.size();i++){
            for(int j=0;j<userIds.size();j++) {
                if (this.domainAllUserIds.get(i).contains(userIds.get(j))){
                    indexes.add(i);
                }
            }
        }
        return indexes;
    }


    public List<Integer> getIndexesForUserName(List<String> usernames,List<Integer> index,String userName){
        List<Integer> indexes = new LinkedList<>();
        for(int i=0;i<usernames.size();i++){
            for(int j=0;j<index.size();j++) {
                if (index.get(j) == i) {
                    usernames.get(i).equals(userName);
                    indexes.add(i);
                }
            }
        }
        return indexes;
    }

    public List<String> hitGetDomainUserIdsFromListRendererAPI(String JsonString,String ColumnId){
        try {
                JSONObject jsonObj = new JSONObject(JsonString);
                for (int i = 0; i < jsonObj.getJSONArray("data").length(); i++) {
                    domainAllUserIds.add(jsonObj.getJSONArray("data").getJSONObject(i).getJSONObject(ColumnId).get("value").toString());
                }
        }
        catch (Exception e){
            logger.error("Exception while hitting List Renderer API. {}", e.getMessage());
        }
        return domainAllUserIds;
    }

    public List<String> getJSONObjectsDataforParticularKey(String jsonObject,String ColumnId){
        List<String> jsonValues = new LinkedList<>();
        try {
                JSONObject jsonObj = new JSONObject(jsonObject);
                for (int i = 0; i < jsonObj.getJSONArray("data").length(); i++) {
                    jsonValues.add(jsonObj.getJSONArray("data").getJSONObject(i).getJSONObject(ColumnId).get("value").toString());

            }
        }
        catch (Exception e){
            logger.error("Exception while hitting List Renderer API. {}", e.getMessage());
        }
        return jsonValues;
    }

    private Boolean loginWithClientAdminUser() {
        logger.info("Logging with Client Admin UserName [{}] and Password [{}]", clientAdminUserName, clientAdminUserPassword);
        Check checkObj = new Check();
        checkObj.hitCheck(clientAdminUserName, clientAdminUserPassword);

        return (Check.getAuthorization() != null);
    }

    private Boolean loginWithDelegatedByEndUser() {
        logger.info("Logging with UserName [{}] and Password [{}]", delegatedByEndUserName, delegatedByEndUserPassword);
        Check checkObj = new Check();
        checkObj.hitCheck(delegatedByEndUserName, delegatedByEndUserPassword);

        return (Check.getAuthorization() != null);
    }

    private Boolean loginWithNoDelegationPermissionUser(String uId, String password) {
        logger.info("Logging with UserName [{}] and Password [{}]", uId, password);
        Check checkObj = new Check();
        checkObj.hitCheck(uId, password);

        return (Check.getAuthorization() != null);
    }

    private Boolean loginWithDelegatedToEndUser() {
        logger.info("Logging with UserName [{}] and Password [{}]", delegatedToEndUserName, delegatedToEndUserNPassword);
        Check checkObj = new Check();
        checkObj.hitCheck(delegatedToEndUserName, delegatedToEndUserNPassword);

        return (Check.getAuthorization() != null);
    }

    public String getCreateDelegationPayload(String startDate, String endDate, String delegatedUserId, List<String> domainUserIds,String sourceDelegatedId){
        String payload = "{\"delegatedDomainUser\":{\"startDate\":\""+startDate+"\",\"endDate\":\""+endDate+"\",\"delegatedUser\":{\"id\":\""+ delegatedUserId+"\"}}," +
                "\"domainUserIds\":"+ domainUserIds + ",\"sourceUserId\":\"" + sourceDelegatedId + "\"}";
        return payload;
    }

    public String getDeleteDelegationPayload(String delegatedDomainUser,List<String> domainUserIds){
        String payload = "{\"delegatedDomainUser\":{"+ delegatedDomainUser+"},\"domainUserIds\":"+ domainUserIds+"}";
        return payload;
    }

    public List<String> getDomainUserIds(String domainUserIds){
        List<String> domainIds=new LinkedList<>();
        String[] domainUsers=domainUserIds.split(",");
        for(String s:domainUsers){
            domainIds.add(s);
        }
        return domainIds;
    }

    @DataProvider(name="CreateDelegationData")
    public Object[][] getCreateDelegationData() throws ParseException {
        loginWithClientAdminUser();
        delegatedToEndUserDomainId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delegated to end user details", "delegateduserdomainid");
        String jsonResponse=getDomainUserIdsFromListRendererAPI(size);
        Integer columnId=ListDataHelper.getColumnIdFromColumnName(jsonResponse,"bulkcheckbox");
        domainAllUserIds = hitGetDomainUserIdsFromListRendererAPI(jsonResponse,String.valueOf(columnId));
        return new Object[][] {

                {DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),30,"MM-dd-yyyy HH:mm:ss")
                        ,DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),50,"MM-dd-yyyy HH:mm:ss")
                        ,delegatedToEndUserDomainId, domainAllUserIds.get(RandomNumbers.getRandomNumberWithinRangeIndex(0,domainAllUserIds.size()))},

                {DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),-30,"MM-dd-yyyy HH:mm:ss")
                        ,DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),10,"MM-dd-yyyy HH:mm:ss")
                        ,delegatedToEndUserDomainId,domainAllUserIds.get(RandomNumbers.getRandomNumberWithinRangeIndex(0,domainAllUserIds.size())) +  "," +
                        domainAllUserIds.get(RandomNumbers.getRandomNumberWithinRangeIndex(0,domainAllUserIds.size()))},

                {DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),0,"MM-dd-yyyy HH:mm:ss")
                        ,DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),0,"MM-dd-yyyy HH:mm:ss")
                        ,delegatedToEndUserDomainId,"8400,8401"},

                {DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),20,"MM-dd-yyyy HH:mm:ss")
                        ,DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),5,"MM-dd-yyyy HH:mm:ss")
                        ,"","8400,8401"},

                {DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"),-10,"MM-dd-yyyy")
                        ,DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"),5,"MM-dd-yyyy")
                        ,delegatedToEndUserDomainId,"8400,8401"},

                {DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"),-10,"MM-dd-yyyy")
                        ,DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"),5,"MM-dd-yyyy")
                        ,"",""}
        };

    }


// Methods to be used for download purpose
    public  String downloadListDataForAllColumns(String filterJson,Integer entityTypeId, String entityName, Integer listId,String sheetName,SoftAssert softAssert){

        try{
            String outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
            Map<String, String> formParam = getFormParamDownloadList(entityTypeId,null,filterJson);

            logger.info("formParam is : [{}]", formParam);

            DownloadListWithData downloadListWithData = new DownloadListWithData();
            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
            HttpResponse response = hitDownloadListWithData(formParam, listId);

            if (response.getStatusLine().toString().contains("200")) {

                return dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "DelegationData", entityName, sheetName);

            } else {
                logger.error("Error while downloading list data for all columns for entity name +" + entityName);
                softAssert.assertTrue(false,"Error while downloading list data for all columns for entity name" + entityName);
            }
        }catch(Exception e){
            logger.error("Exception while downloading list data columns for entity name " + entityName);
            softAssert.assertTrue(false,"Exception while downloading list data columns for entity name " + entityName);
        }
        return null;
    }

    private  Map<String, String> getFormParamDownloadList(Integer entityTypeId, Map<Integer, String> selectedColumnMap,String filterJson) {

        int offset = 0;
        Integer size = 10;
        String jsonData;
        String orderByColumnName = "id";
        String orderDirection = "asc nulls first";
        String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");

        Map<String, String> formParam = new HashMap<String, String>();
        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + "\"\"" + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterJson +"}}}";
        } else {
            String selectedColumnArray = "\"selectedColumns\":[";
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
            }
            selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
            selectedColumnArray += "]";

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}," + selectedColumnArray + "}";
        }

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }
    private String dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {

        String outputFile = null;
        String outputFileFormatForDownloadListWithData = ".xlsx";
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status)
                logger.info("DownloadListWithData file generated at {}", outputFile);
        }
        return outputFile;
    }

    public HttpResponse hitDownloadListWithData(Map<String, String> formParam, Integer entityURLId) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/download/" + entityURLId + "/data?entityId=&entityTypeId=&tabList=true&clientEntitySeqId=";


            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            String params = UrlEncodedString.getUrlEncodedString(formParam);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.postRequest(postRequest, params);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                logger.debug("DownloadListWithData response header {}", headers[i].toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting DownloadListWithData Api. {}", e.getMessage());
        }
        return response;
    }
}
