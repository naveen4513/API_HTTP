package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectCreateAndEditPermission
{
    private final static Logger logger = LoggerFactory.getLogger(ResetPermission.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;
    private static String configAutoExtractionFilePath;
    private static String configAutoExtractionFileName;
    static String clientId;


    @BeforeClass
    public void beforeClass()
    {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        clientId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"clientid");

    }



    /*Verify that user should be able to Create/Edit a project when he is having permission*/
    @Test(priority=2,enabled = true)
    public void projectEditPermissionOn() throws IOException {
        try {
            softAssert = new SoftAssert();
            Check check = new Check();

            // Login to Client Admin
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response is not valid");

            //Updating the user role group when user is having permission of Project Create and Edit
            String url = "/masteruserrolegroups/update/";
            String roleGroupId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "rolegroupid");
            HttpResponse masterUserRoleGroupResponse = AutoExtractionHelper.hitMasterUserRoleGroup(url, roleGroupId);
            softAssert.assertTrue(masterUserRoleGroupResponse.getStatusLine().getStatusCode() == 200, "Master Role Group API Response is not valid");
            String masterUserRoleGroupResponseStr = EntityUtils.toString(masterUserRoleGroupResponse.getEntity());
            org.jsoup.nodes.Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
            List<String> allSections = document.select("#permission h3").stream().map(m -> m.text()).collect(Collectors.toList());
            int autoExtractionPermissionIndex = 0;
            for (int i = 0; i < allSections.size(); i++)
            {
                if (allSections.get(i).equals("AUTOEXTRACTION:"))
                {
                    autoExtractionPermissionIndex = i;
                    break;
                }
            }
            List<String> allSectionsValue = document.select("#permission div").stream().map(m -> m.text()).collect(Collectors.toList());
            softAssert.assertTrue(allSectionsValue.get(autoExtractionPermissionIndex).contains(" Auto Extraction Doc Reset"), " Auto Extraction Doc Reset is not present under global permissions section");
            FileUtils fileUtils;
            Map<String, String> formData;
            Map<String, String> keyValuePair;
            String params;
            HttpResponse httpResponse;
            String endUserName = null;
            String endUserPassword = null;
            // Switch on the Project Edit/Update Permission
            try {
                fileUtils = new FileUtils();
                formData = new LinkedHashMap<>();
                keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/AutoExtractionProjectEditPermissionOn.txt", ":", "RoleGroup");
                for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                    formData.put(m.getKey().trim(), m.getValue().trim());
                }
                params = UrlEncodedString.getUrlEncodedString(formData);
                httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
                softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

                // Login to End User
                endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
                endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
                loginResponse = check.hitCheck(endUserName, endUserPassword);
                softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");

                /*When user is  having permission to create a Project then Project should  get created -->Test Case Id:C141302*/
                // Get All the Meta Data Fields in Project to extract
                logger.info("Creating a new project when user is having permission:"+"Test Case Id:C141302");
                String getAllFieldsUrl = "/metadataautoextraction/getAllFields";
                HttpResponse metadataFieldResponse = AutoExtractionHelper.getAllMetaDataFields(getAllFieldsUrl);
                softAssert.assertTrue(metadataFieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
                String metadataFieldResponseStr = EntityUtils.toString(metadataFieldResponse.getEntity());

                JSONObject metadataFieldResponseJsonStr = new JSONObject(metadataFieldResponseStr);
                int metadataFieldsLength = metadataFieldResponseJsonStr.getJSONArray("response").length();
                HashMap<Integer,String> metadataFields = new LinkedHashMap<>();
                for(int i=0;i<metadataFieldsLength;i++){
                    metadataFields.put(Integer.valueOf(metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("id").toString()),metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("name").toString());
                }

                if(metadataFields.size()<1){
                    throw new SkipException("No Meta Data Fields are there to select in project");
                }

                String createProjectUrl = "/metadataautoextraction/create";
                String projectName = "Test_Automation" + RandomString.getRandomAlphaNumericString(10);
                String createProjectPayload = "{\"name\":\""+ projectName +"\",\"description\":\"sgsgd\",\"projectLinkedFieldIds\":["+ metadataFields.entrySet().stream().findFirst().get().getKey().intValue() +"],\"clientId\":"+ clientId +"}";
                HttpResponse projectCreationResponse = AutoExtractionHelper.createProject(createProjectUrl,createProjectPayload);
                softAssert.assertTrue(projectCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
                String projectCreationResponseStr = EntityUtils.toString(projectCreationResponse.getEntity());
                softAssert.assertTrue(APIUtils.validJsonResponse(projectCreationResponseStr),"Not a valid Json");
                JSONObject projectCreationJson = new JSONObject(projectCreationResponseStr);

                JSONObject createProjectJson = new JSONObject(projectCreationResponseStr);

                softAssert.assertTrue(createProjectJson.get("success").toString().equals("true"),"Project is not created successfully");
                int newlyCreatedProjectId = Integer.valueOf(createProjectJson.getJSONObject("response").get("id").toString());

                logger.info("Newly created project is " + newlyCreatedProjectId);

                //Updating the project Name and Field Mapping that has been created-->Test Case Id:C141304
                logger.info("Updating a project when user is having permission:"+"Test Case Id:C141304");
                String projectUpdateUrl = "/metadataautoextraction/update";
                String updatedProjectName = "UpdatedName" + RandomString.getRandomAlphaNumericString(10);
                String projectUploadPayload="{\"name\":\""+ updatedProjectName + "\",\"description\":\"AutomationProject1\",\"projectLinkedFieldIds\":[12484,12485],\"id\":\""+ newlyCreatedProjectId +"\",\"clientId\":1007}";
                HttpResponse projectUpdateResponse = AutoExtractionHelper.updateProject(projectUpdateUrl,projectUploadPayload);
                softAssert.assertTrue(projectUpdateResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String projectUpdateStr= EntityUtils.toString(projectUpdateResponse.getEntity());

            } catch (Exception e)
            {
                logger.info("Exception occured while hitting User Role Group Update API"+e.getMessage());
            }
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting User Role Group API"+e.getMessage());
        }
        softAssert.assertAll();
    }
    /*Verify that when user is not having permission to create or edit a project then user should not be able
        to create a new project -->C141301*/
    @Test(priority=1,enabled = true)
    public void projectEditPermissionOff() throws IOException {
        try {
            softAssert = new SoftAssert();
            Check check = new Check();

            // Login to Client Admin
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response is not valid");

            String url = "/masteruserrolegroups/update/";
            String roleGroupId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "rolegroupid");
            HttpResponse masterUserRoleGroupResponse = AutoExtractionHelper.hitMasterUserRoleGroup(url, roleGroupId);
            softAssert.assertTrue(masterUserRoleGroupResponse.getStatusLine().getStatusCode() == 200, "Master Role Group API Response is not valid");
            String masterUserRoleGroupResponseStr = EntityUtils.toString(masterUserRoleGroupResponse.getEntity());
            org.jsoup.nodes.Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
            List<String> allSections = document.select("#permission h3").stream().map(m -> m.text()).collect(Collectors.toList());
            int autoExtractionPermissionIndex = 0;
            for (int i = 0; i < allSections.size(); i++)
            {
                if (allSections.get(i).equals("AUTOEXTRACTION:"))
                {
                    autoExtractionPermissionIndex = i;
                    break;
                }
            }
            List<String> allSectionsValue = document.select("#permission div").stream().map(m -> m.text()).collect(Collectors.toList());
            softAssert.assertTrue(allSectionsValue.get(autoExtractionPermissionIndex).contains(" Auto Extraction Doc Reset"), " Auto Extraction Doc Reset is not present under global permissions section");
            FileUtils fileUtils;
            Map<String, String> formData;
            Map<String, String> keyValuePair;
            String params;
            HttpResponse httpResponse;
            String endUserName = null;
            String endUserPassword = null;
            // Switch off ProjectEdit/Update permission
            logger.info("Disabling the project update permission");
            try {
                fileUtils = new FileUtils();
                formData = new LinkedHashMap<>();
                keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/AutoExtractionProjectEditPermissionOff.txt", ":", "RoleGroup");
                for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                    formData.put(m.getKey().trim(), m.getValue().trim());
                }
                params = UrlEncodedString.getUrlEncodedString(formData);
                httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
                softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");


                // Login to End User
                endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
                endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
                loginResponse = check.hitCheck(endUserName, endUserPassword);
                softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");

                /*When user is not having permission to create a Project then Project should not get created */
                // Get All the Meta Data Fields in Project to extract
                String getAllFieldsUrl = "/metadataautoextraction/getAllFields";
                HttpResponse metadataFieldResponse = AutoExtractionHelper.getAllMetaDataFields(getAllFieldsUrl);
                softAssert.assertTrue(metadataFieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
                String metadataFieldResponseStr = EntityUtils.toString(metadataFieldResponse.getEntity());

                JSONObject metadataFieldResponseJsonStr = new JSONObject(metadataFieldResponseStr);
                int metadataFieldsLength = metadataFieldResponseJsonStr.getJSONArray("response").length();
                HashMap<Integer,String> metadataFields = new LinkedHashMap<>();
                for(int i=0;i<metadataFieldsLength;i++){
                    metadataFields.put(Integer.valueOf(metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("id").toString()),metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("name").toString());
                }

                if(metadataFields.size()<1){
                    throw new SkipException("No Meta Data Fields are there to select in project");
                }

                //Create a Project when user is not having permission--->C141301
                logger.info("Creating a project when user is not having permission:" + "Test Case Id:C141301");
                String createProjectUrl = "/metadataautoextraction/create";
                String projectName = "Test_Automation" + RandomString.getRandomAlphaNumericString(10);
                String createProjectPayload = "{\"name\":\""+ projectName +"\",\"description\":\"sgsgd\",\"projectLinkedFieldIds\":["+ metadataFields.entrySet().stream().findFirst().get().getKey().intValue() +"],\"clientId\":"+ clientId +"}";
                HttpResponse projectCreationResponse = AutoExtractionHelper.createProject(createProjectUrl,createProjectPayload);
                softAssert.assertTrue(projectCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
                String projectCreationResponseStr = EntityUtils.toString(projectCreationResponse.getEntity());
                softAssert.assertTrue(APIUtils.validJsonResponse(projectCreationResponseStr),"Not a valid Json");
                JSONObject projectCreationJson = new JSONObject(projectCreationResponseStr);
                String errorMessage = (String) projectCreationJson.getJSONObject("header").getJSONObject("response").get("errorMessage");
                softAssert.assertTrue(errorMessage.contains("Either you do not have the required permissions or requested page does not exist anymore."),"User is able to create project even when he is not having access");

                //Navigating on Project listing page to pick one project Id to update the same
                String listDataQuery = "/listRenderer/list/441/listdata?version=2.0&isFirstCall=true";
                String payload = "{\"filterMap\":{}}";
                HttpResponse projectListDataResponse = AutoExtractionHelper.projectListDataAPI(listDataQuery,payload);
                softAssert.assertTrue(projectListDataResponse.getStatusLine().getStatusCode() == 200, "Project List Data Response Code is not valid");
                String projectListDataResponseStr = EntityUtils.toString(projectListDataResponse.getEntity());

                softAssert.assertTrue(APIUtils.validJsonResponse(projectListDataResponseStr),"Project List Data is not a valid Json");
                JSONObject projectListDataResponseJson = new JSONObject(projectListDataResponseStr);

                int columnId = ListDataHelper.getColumnIdFromColumnName(projectListDataResponseStr, "name");
                int projectId= Integer.parseInt(projectListDataResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

                //Updating a Project when user is not having permission-->Test Case Id: C141303
                logger.info("Updating a project when user is not having Edit Permission :" + "C141303");
                String projectUpdateUrl = "/metadataautoextraction/update";
                String updatedProjectName = "UpdatedName" + RandomString.getRandomAlphaNumericString(10);
                String projectUploadPayload="{\"name\":\""+ updatedProjectName + "\",\"description\":\"AutomationProject1\",\"projectLinkedFieldIds\":[12484,12485],\"id\":\""+ projectId +"\",\"clientId\":1007}";
                HttpResponse projectUpdateResponse = AutoExtractionHelper.updateProject(projectUpdateUrl,projectUploadPayload);
                softAssert.assertTrue(projectUpdateResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String projectUpdateStr= EntityUtils.toString(projectUpdateResponse.getEntity());
                JSONObject projectUpdateJson = new JSONObject(projectUpdateStr);
                String ValidationMessage = (String) projectUpdateJson.getJSONObject("header").getJSONObject("response").get("errorMessage");
                softAssert.assertTrue(ValidationMessage.contains("Either you do not have the required permissions or requested page does not exist anymore."),"User is able to update a project even when he is not having access");
            } catch (Exception e)
            {
                logger.info("Exception occured while hitting User Role Group Update API");
            }
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting User Role Group API");
        }

        softAssert.assertAll();
    }

}
