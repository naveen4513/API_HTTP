package com.sirionlabs.test.distributionList;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;


public class TestDistributionLIst {

    private final static Logger logger = LoggerFactory.getLogger(TestDistributionLIst.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String newCreatedDistributionList;
    SoftAssert softAssert;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("distributionListConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("distributionListConfigFileName");
    }

   // @Test(priority = 1)
    public void createDistributionList() throws IOException, SQLException {
        softAssert = new SoftAssert();
        String query = "/dl/create";
        String createdDistributionListName = "DistributionList" + RandomStringUtils.randomAlphabetic(5);
        String subscribers = getRandomEmail();

        // Create Distribution List with valid subscribers
        String payload = "{\"subscribers\":"+ "\"" + subscribers + "\"" +",\"type\":\"API Test\",\"name\":"+ "\"" + createdDistributionListName + "\"" +",\"allSuppliers\":false,\"allUsers\":false,\"allRoles\":false,\"suppliers\":[],\"users\":[],\"roleGroups\":[],\"mssettings\":{\"displayProp\":\"name\",\"selectedToTop\":true,\"scrollableHeight\":\"200px\",\"scrollable\":true,\"externalIdProp\":\"\",\"enableSearch\":true,\"showParentCheckBoxes\":false,\"parentElmClass\":\"list-filter-content\"}}";
        HttpResponse response= hitCreateDistributionListAPI(query,payload);

        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Status Code is not valid");
        String distributionListStrResponse = EntityUtils.toString(response.getEntity());

        softAssert.assertTrue(distributionListStrResponse.contains("{\"success\":true}"),"Distribution List is not created successfully");

        newCreatedDistributionList = createdDistributionListName;

        // Create Distribution List with same list name and subscribes
        payload = "{\"subscribers\":"+ "\"" + subscribers + "\"" +",\"type\":\"API Test\",\"name\":"+ "\"" + createdDistributionListName + "\"" +",\"allSuppliers\":false,\"allUsers\":false,\"allRoles\":false,\"suppliers\":[],\"users\":[],\"roleGroups\":[],\"mssettings\":{\"displayProp\":\"name\",\"selectedToTop\":true,\"scrollableHeight\":\"200px\",\"scrollable\":true,\"externalIdProp\":\"\",\"enableSearch\":true,\"showParentCheckBoxes\":false,\"parentElmClass\":\"list-filter-content\"}}";

        response= hitCreateDistributionListAPI(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Status Code is not valid");
        distributionListStrResponse = EntityUtils.toString(response.getEntity());

        softAssert.assertTrue(distributionListStrResponse.contains("{\"success\":false, \"message\":\"This dl name already exists in the system.\"}"),"Distribution List is not created successfully");

        // Create Distribution List with same list name and no subscribers
        payload = "{\"subscribers\":\"\",\"type\":\"API Test\",\"name\":"+ "\"" + createdDistributionListName + "\"" +",\"allSuppliers\":false,\"allUsers\":false,\"allRoles\":false,\"suppliers\":[],\"users\":[],\"roleGroups\":[],\"mssettings\":{\"displayProp\":\"name\",\"selectedToTop\":true,\"scrollableHeight\":\"200px\",\"scrollable\":true,\"externalIdProp\":\"\",\"enableSearch\":true,\"showParentCheckBoxes\":false,\"parentElmClass\":\"list-filter-content\"}}";

        response= hitCreateDistributionListAPI(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Status Code is not valid");
        distributionListStrResponse = EntityUtils.toString(response.getEntity());

        softAssert.assertTrue(distributionListStrResponse.contains("{\"success\":false, \"message\":\"This dl name already exists in the system.\"}"),"Distribution List is not created successfully");

        // Create distribution list with different name and same subscribers
        createdDistributionListName = "DistributionList" + RandomStringUtils.randomAlphabetic(5);
        payload = "{\"subscribers\":"+ "\"" + subscribers + "\"" +",\"type\":\"API Test\",\"name\":"+ "\"" + createdDistributionListName + "\"" +",\"allSuppliers\":false,\"allUsers\":false,\"allRoles\":false,\"suppliers\":[],\"users\":[],\"roleGroups\":[],\"mssettings\":{\"displayProp\":\"name\",\"selectedToTop\":true,\"scrollableHeight\":\"200px\",\"scrollable\":true,\"externalIdProp\":\"\",\"enableSearch\":true,\"showParentCheckBoxes\":false,\"parentElmClass\":\"list-filter-content\"}}";

        response= hitCreateDistributionListAPI(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Status Code is not valid");
        distributionListStrResponse = EntityUtils.toString(response.getEntity());

        softAssert.assertTrue(distributionListStrResponse.contains("{\"success\":true}"),"Distribution List is not created successfully");

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("192.168.2.156","5432","AE_1_37","postgres","postgres");
        query = "SELECT id FROM mass_email_distribution_list where name ="+ "\'" + createdDistributionListName + "\'" + "order by id desc limit 1;";
        String createdDistributionListId = postgreSQLJDBC.doSelect(query).stream().findFirst().get().stream().findFirst().get();

        // Delete Created distribution List
        query = "/dl/delete/"+ createdDistributionListId +"?force=false";
        response = deleteDistributionList(query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response code is not valid");
        String distributionListDeleteStr = EntityUtils.toString(response.getEntity());
        softAssert.assertTrue(distributionListDeleteStr.contains("{\"message\":"+ "\""+ createdDistributionListName+" deleted successfully\",\"status\":true}"),"Distribution list is not deleted");

        // Show deleted distribution list
        response = showDistributionList("/dl/show/"+createdDistributionListId+"/api/v1");
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Response code is not valid");
        String deletedDistributionListResponse = EntityUtils.toString(response.getEntity());
        softAssert.assertTrue(deletedDistributionListResponse.equals("{}"),"Distribution list is not deleted");
        softAssert.assertAll();
    }

   // @Test(priority = 2)
    public void updateCreatedDistributionList() throws SQLException, IOException {
        softAssert =new SoftAssert();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("192.168.2.156","5432","AE_1_37","postgres","postgres");
        String query = "SELECT id FROM mass_email_distribution_list where name ="+ "\'" + newCreatedDistributionList + "\'" + "order by id desc limit 1;";
        String createdDistributionListId = postgreSQLJDBC.doSelect(query).stream().findFirst().get().stream().findFirst().get();
        query = "/dl/show/" + createdDistributionListId +"/api/v1";
        HttpResponse httpResponse = showDistributionList(query);

        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode()==200,"Status is not valid");
        String distributionListResponseStr = EntityUtils.toString(httpResponse.getEntity());

        JSONObject jsonObject = new JSONObject(distributionListResponseStr);

        String updatedType = "API Testing Update Distribution List";
        jsonObject.getJSONObject("selectedData").put("type",updatedType);
        String payload = jsonObject.getJSONObject("selectedData").toString();

        // Update Distribution List
        httpResponse =hitCreateDistributionListAPI("/dl/update/"+createdDistributionListId,payload);
        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
        String updatedDLSTrResponse = EntityUtils.toString(httpResponse.getEntity());
        softAssert.assertTrue(updatedDLSTrResponse.contains("{\"success\":true}"),"DL is not updated successfully");

        // Show updated DL
        httpResponse = showDistributionList("/dl/show/"+createdDistributionListId+"/api/v1");
        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode()==200,"Response code is not valid");
        String showDistributionListResponse = EntityUtils.toString(httpResponse.getEntity());
        softAssert.assertTrue(showDistributionListResponse.contains(updatedType),"Distribution List is not updated successfully");

        // Delete Created distribution List
        query = "/dl/delete/"+ createdDistributionListId +"?force=false";
        httpResponse = deleteDistributionList(query);
        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() ==200,"Response code is not valid");
        String distributionListDeleteStr = EntityUtils.toString(httpResponse.getEntity());
        softAssert.assertTrue(distributionListDeleteStr.contains("{\"message\":"+ "\""+ newCreatedDistributionList+" deleted successfully\",\"status\":true}"),"Distribution list is not deleted");

        softAssert.assertAll();
    }

   // @Test
    public void verifyDistributionListingRendering() throws IOException, SQLException {
        softAssert = new SoftAssert();
        String query = "/dl/create";
        String createdDistributionListName = "DistributionList" + RandomStringUtils.randomAlphabetic(5);
        String subscribers = getRandomEmail();

        // List Renderer Distribution List before distribution list creation
        String payload = "{\"filterMap\":{\"entityTypeId\":250,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        HttpResponse response = filterDistributionListRenderer("/listRenderer/list/218/listdata/?version=2.0",payload);

        String listRendererResponse = EntityUtils.toString(response.getEntity());
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Response Code is not valid");

        JSONObject jsonObject = new JSONObject(listRendererResponse);

        int initialFilteredCount = Integer.parseInt(jsonObject.get("filteredCount").toString());

        // Create Distribution List with valid subscribers
        payload = "{\"subscribers\":"+ "\"" + subscribers + "\"" +",\"type\":\"API Test\",\"name\":"+ "\"" + createdDistributionListName + "\"" +",\"allSuppliers\":true,\"allUsers\":false,\"allRoles\":false,\"suppliers\":[],\"users\":[],\"roleGroups\":[],\"mssettings\":{\"displayProp\":\"name\",\"selectedToTop\":true,\"scrollableHeight\":\"200px\",\"scrollable\":true,\"externalIdProp\":\"\",\"enableSearch\":true,\"showParentCheckBoxes\":false,\"parentElmClass\":\"list-filter-content\"}}";
        response= hitCreateDistributionListAPI(query,payload);

        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Status Code is not valid");
        String distributionListStrResponse = EntityUtils.toString(response.getEntity());

        softAssert.assertTrue(distributionListStrResponse.contains("{\"success\":true}"),"Distribution List is not created successfully");

        // List Renderer Distribution List after distribution list creation
        payload = "{\"filterMap\":{\"entityTypeId\":250,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        response = filterDistributionListRenderer("/listRenderer/list/218/listdata/?version=2.0",payload);

        listRendererResponse = EntityUtils.toString(response.getEntity());
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Response Code is not valid");

        jsonObject = new JSONObject(listRendererResponse);

        int finalFilteredCount = Integer.parseInt(jsonObject.get("filteredCount").toString());

        softAssert.assertTrue((initialFilteredCount +1) == finalFilteredCount,"Distribution List is not present distribution list renderer");

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("192.168.2.156","5432","AE_1_37","postgres","postgres");
        query = "SELECT id FROM mass_email_distribution_list where name ="+ "\'" + createdDistributionListName + "\'" + "order by id desc limit 1;";
        String createdDistributionListId = postgreSQLJDBC.doSelect(query).stream().findFirst().get().stream().findFirst().get();

        // Delete Created distribution List
        query = "/dl/delete/"+ createdDistributionListId +"?force=false";
        response = deleteDistributionList(query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response code is not valid");
        String distributionListDeleteStr = EntityUtils.toString(response.getEntity());
        softAssert.assertTrue(distributionListDeleteStr.contains("{\"message\":"+ "\""+ createdDistributionListName+" deleted successfully\",\"status\":true}"),"Distribution list is not deleted");

        softAssert.assertAll();
    }

    //@Test
    public void checkSubscribersList() throws IOException, SQLException {
        softAssert = new SoftAssert();
        String query = "/dl/create";
        String createdDistributionListName = "DistributionList" + RandomStringUtils.randomAlphabetic(5);
        String subscribers = getRandomEmail();
        //Check subscribers list
        String payload= "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        HttpResponse response = filterDistributionListRenderer("/listRenderer/list/220/listdata/?version=2.0",payload);

        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response");
        String subscriberListRendererStrResponse = EntityUtils.toString(response.getEntity());
        JSONObject jsonObject = new JSONObject(subscriberListRendererStrResponse);
        int intialFilteredCount = Integer.valueOf(jsonObject.get("filteredCount").toString());

        // Create Distribution List with valid subscribers
        payload = "{\"subscribers\":"+ "\"" + subscribers + "\"" +",\"type\":\"API Test\",\"name\":"+ "\"" + createdDistributionListName + "\"" +",\"allSuppliers\":false,\"allUsers\":false,\"allRoles\":false,\"suppliers\":[],\"users\":[],\"roleGroups\":[],\"mssettings\":{\"displayProp\":\"name\",\"selectedToTop\":true,\"scrollableHeight\":\"200px\",\"scrollable\":true,\"externalIdProp\":\"\",\"enableSearch\":true,\"showParentCheckBoxes\":false,\"parentElmClass\":\"list-filter-content\"}}";
        response= hitCreateDistributionListAPI(query,payload);

        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Status Code is not valid");
        String distributionListStrResponse = EntityUtils.toString(response.getEntity());

        softAssert.assertTrue(distributionListStrResponse.contains("{\"success\":true}"),"Distribution List is not created successfully");

        //Check subscribers list
        payload= "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        response = filterDistributionListRenderer("/listRenderer/list/220/listdata/?version=2.0",payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response");
        subscriberListRendererStrResponse = EntityUtils.toString(response.getEntity());
        jsonObject = new JSONObject(subscriberListRendererStrResponse);
        int finalFilteredCount = Integer.valueOf(jsonObject.get("filteredCount").toString());
        softAssert.assertTrue(intialFilteredCount+1 == finalFilteredCount,"Subscriber is not added into subscriber list");

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("192.168.2.156","5432","AE_1_37","postgres","postgres");
        query = "SELECT id FROM mass_email_distribution_list where name ="+ "\'" + createdDistributionListName + "\'" + "order by id desc limit 1;";
        String createdDistributionListId = postgreSQLJDBC.doSelect(query).stream().findFirst().get().stream().findFirst().get();

        query ="select subscriber_detail_id from mass_email_distribution_list_subscriber_detail_link where distribution_list_id="+ createdDistributionListId;
        String createdSubscriberId = postgreSQLJDBC.doSelect(query).stream().findFirst().get().stream().findFirst().get();

        // Show Subscriber List
        response = showDistributionList("/massEmail/updatesubscriber/"+createdSubscriberId+"/api/v1");
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Response Code is not valid");
        String subscriberListData = EntityUtils.toString(response.getEntity());
        jsonObject = new JSONObject(subscriberListData);

        softAssert.assertTrue(jsonObject.get("emailId").toString().trim().equals(subscribers.trim()),"Subscriber email is not same");
        softAssert.assertTrue(jsonObject.get("optedOut").equals(false),"Opt Out is not false");

        payload ="{\"id\":"+createdSubscriberId +",\"optedOut\":true}";

        // Update Subscriber list
        response = hitCreateDistributionListAPI("/massEmail/updatesubscriber/api/v1",payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Response Code is not valid");
        subscriberListData = EntityUtils.toString(response.getEntity());
        jsonObject = new JSONObject(subscriberListData);
        softAssert.assertTrue(jsonObject.get("emailId").toString().trim().equals(subscribers.trim()),"Subscriber email is not same");
        softAssert.assertTrue(jsonObject.get("optedOut").equals(true),"Opt Out is not false");

        // Delete Created distribution List
        query = "/dl/delete/"+ createdDistributionListId +"?force=false";
        response = deleteDistributionList(query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response code is not valid");
        String distributionListDeleteStr = EntityUtils.toString(response.getEntity());
        softAssert.assertTrue(distributionListDeleteStr.contains("{\"message\":"+ "\""+ createdDistributionListName+" deleted successfully\",\"status\":true}"),"Distribution list is not deleted");
        softAssert.assertAll();
    }

    //@Test
    public void distributionAndSubscriberListInternationalization() throws IOException {

        softAssert = new SoftAssert();
        // Login with client admin
        Check check = new Check();
        HttpResponse response = check.hitCheck("karuna_admin","admin123");

        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==302,"Response is not valid");

        //Show field label english
        String clientFieldColumnName = null;
        String fieldLabelName = null;
        String updatedClientFieldColumnName ="DLNAME";
        String updatedfieldLabelName = "DLSuppliers";
        response =showDistributionList("/fieldlabel/findLabelsByGroupIdAndLanguageId/1/1096");
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response is not valid");
        String distributionListInternationalizeStr = EntityUtils.toString(response.getEntity());

        JSONObject distributionListInternationalizeJsonStr = new JSONObject(distributionListInternationalizeStr);

        int columnLength = distributionListInternationalizeJsonStr.getJSONArray("childGroups").getJSONObject(0).getJSONArray("fieldLabels").length();
        for(int i=0;i<columnLength;i++){
            if(distributionListInternationalizeJsonStr.getJSONArray("childGroups").getJSONObject(0).getJSONArray("fieldLabels").getJSONObject(i).get("name").equals("NAME")){
               clientFieldColumnName =distributionListInternationalizeJsonStr.getJSONArray("childGroups").getJSONObject(0).getJSONArray("fieldLabels").getJSONObject(i).get("clientFieldName").toString();
               distributionListInternationalizeJsonStr.getJSONArray("childGroups").getJSONObject(0).getJSONArray("fieldLabels").getJSONObject(i).put("clientFieldName",updatedClientFieldColumnName);
            }
        }

        int fieldLabelsLength = distributionListInternationalizeJsonStr.getJSONArray("fieldLabels").length();
        for(int i=0;i<fieldLabelsLength;i++){
            if(distributionListInternationalizeJsonStr.getJSONArray("fieldLabels").getJSONObject(i).get("name").equals("Suppliers")){
                fieldLabelName=distributionListInternationalizeJsonStr.getJSONArray("fieldLabels").getJSONObject(i).get("clientFieldName").toString();
                distributionListInternationalizeJsonStr.getJSONArray("fieldLabels").getJSONObject(i).put("clientFieldName",updatedfieldLabelName);
            }
        }

        String payload = distributionListInternationalizeJsonStr.toString();
        response = hitfieldAndColumnUpdate("/fieldlabel/update",payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response is not valid");
        String updateJsonStr = EntityUtils.toString(response.getEntity());
        softAssert.assertTrue(updateJsonStr.equals("{\"isSuccess\":true,\"errorMessages\":[]}"),"Fields and Columns are not updated successfully");

        // Login with end-user
        response = check.hitCheck("karuna_user1","admin123");
        softAssert.assertTrue(response.getStatusLine().getStatusCode() ==302,"Response is not valid");


        softAssert.assertAll();
    }

    public HttpResponse hitCreateDistributionListAPI(String query,String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);

            postRequest.addHeader("Accept","application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
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

    public HttpResponse hitfieldAndColumnUpdate(String query,String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
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

    public HttpResponse filterDistributionListRenderer(String query,String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
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


    public HttpResponse showDistributionList(String query){
        HttpResponse response = null;
        HttpGet httpGet;
        try{
            logger.debug("Query string url formed is {}", query);
            httpGet = new HttpGet(query);

            httpGet.addHeader("Accept","text/html, */*; q=0.01");
            httpGet.addHeader("Accept-Encoding","gzip, deflate");
            httpGet.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.getRequest(httpGet);
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

    public HttpResponse deleteDistributionList(String query){
        HttpResponse response = null;
        HttpDelete httpDelete;
        try{
            logger.debug("Query string url formed is {}", query);
            httpDelete = new HttpDelete(query);

            httpDelete.addHeader("Accept","*/*");
            httpDelete.addHeader("Accept-Encoding","gzip, deflate");
            httpDelete.addHeader("Authorization",Check.getAuthorization());

            response = APIUtils.deleteRequestWithoutAuth(httpDelete);
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

    public String getRandomEmail(){
        String name = RandomString.getRandomAlphaNumericString(5);
        return "Automation" + name + "@testing.com";
    }
}
