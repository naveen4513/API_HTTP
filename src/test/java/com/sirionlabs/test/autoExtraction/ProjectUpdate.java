package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.config.UpdateConfigFiles;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectUpdate
{

    private final static Logger logger = LoggerFactory.getLogger(ProjectUpdate.class);
    CustomAssert csAssert = new CustomAssert();
    private String configAutoExtractionFilePath;
    private String configAutoExtractionFileName;
    private String clientId;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
         configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
         configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
         clientId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "clientid");

    }

    /*Create a new Project and Update that project*/
    /*Test Case Id:C141207,C141208,C141209*/
    @Test
    public void projectUpdate()throws IOException
    {
        CustomAssert csAssert = new CustomAssert();

        try {
            //Create a new project
            //Get all the metadata fields to map it to a project
            logger.info("Getting all the active fields for Creating a new Project");
            String getAllMetadataFieldsUrl = "/metadataautoextraction/getAllFields";
            HttpResponse getMetadatafieldsResponse = AutoExtractionHelper.getAllMetaDataFields(getAllMetadataFieldsUrl);
            csAssert.assertTrue(getMetadatafieldsResponse.getStatusLine().getStatusCode() == 200, "Response code is not valid");
            String fieldsResponseStr = EntityUtils.toString(getMetadatafieldsResponse.getEntity());
            org.json.JSONObject fieldResponseJson = new org.json.JSONObject(fieldsResponseStr);
            int metadataFieldLength = fieldResponseJson.getJSONArray("response").length();
            HashMap<Integer, String> metadataFields = new LinkedHashMap<>();
            for (int i = 0; i < metadataFieldLength; i++) {
                metadataFields.put(Integer.valueOf(fieldResponseJson.getJSONArray("response").getJSONObject(i).get("id").toString()), fieldResponseJson.getJSONArray("response").getJSONObject(i).get("name").toString());

            }
            if (metadataFields.size()==0) {
                throw new SkipException("No Meta Data Fields are there to select in project");
            }

            //Creating a new Project
            logger.info("Creating a new project after picking one field from all metadataFields");
            String createProjectUrl = "/metadataautoextraction/create";
            String projectName = "ProjectUpdate" + RandomString.getRandomAlphaNumericString(10);
            String createProjectPayload = "{\"name\":\""+ projectName +"\",\"description\":\"AutomationProject\",\"projectLinkedFieldIds\":["+ metadataFields.entrySet().stream().findFirst().get().getKey().intValue() +"],\"clientId\":"+ clientId +"}";
            HttpResponse projectCreationResponse = AutoExtractionHelper.createProject(createProjectUrl,createProjectPayload);
            csAssert.assertTrue(projectCreationResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String projectCreationStr = EntityUtils.toString(projectCreationResponse.getEntity());
            JSONObject projectCreationJson = new JSONObject(projectCreationStr);
            csAssert.assertTrue(projectCreationJson.get("success").toString().equals("true"),"Project is not created successfully");
            int newlyCreatedProjectId = Integer.valueOf(projectCreationJson.getJSONObject("response").get("id").toString());
            logger.info("Newly created project is " + newlyCreatedProjectId);

            //Updating the project Name and Field Mapping that has been created
            logger.info("Updating the newly created project name and mapping:"+ "TestCaseId:C141208,C141209");
            String projectUpdateUrl = "/metadataautoextraction/update";
            String updatedProjectName = "UpdatedName" + RandomString.getRandomAlphaNumericString(10);
            String projectUploadPayload="{\"name\":\""+ updatedProjectName + "\",\"description\":\"AutomationProject1\",\"projectLinkedFieldIds\":[12484,12485],\"id\":\""+ newlyCreatedProjectId +"\",\"clientId\":1007}";
            HttpResponse projectUpdateResponse = AutoExtractionHelper.updateProject(projectUpdateUrl,projectUploadPayload);
            csAssert.assertTrue(projectUpdateResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String projectUpdateStr= EntityUtils.toString(projectUpdateResponse.getEntity());
            JSONObject projectUpdateJson = new JSONObject(projectUpdateStr);

            //Now Checking the updated values of a Project
            logger.info("Checking the updated values are getting saved for that project");
           String updatedName = (String) projectUpdateJson.getJSONObject("response").get("name");
            csAssert.assertTrue(updatedName.equals(updatedProjectName), "values extraction for Clause is not as expected");
            int linkedFieldsCount = projectUpdateJson.getJSONObject("response").getJSONArray("projectLinkedFieldIds").length();
           csAssert.assertTrue(linkedFieldsCount==2,"Count mismatch in updated linked fields");
        }

        catch (Exception e) {
            logger.info("Exception occured while hitting Project Creation API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();


    }
/*Updating a Default Project of every Client*/
/*Test Case Id:C141210*/
    @Test
    public void updateDefaultProject()throws IOException {
        try {
            logger.info("Updating the Default project of the client");
            String projectUpdateUrl = "/metadataautoextraction/update";
            String updatedProjectName = "UpdatedName" + RandomString.getRandomAlphaNumericString(10);
            String projectUploadPayload="{\"name\":\""+ updatedProjectName + "\",\"description\":\"AutomationProject1\",\"projectLinkedFieldIds\":[12484,12485],\"id\":\"229\",\"clientId\":1007}";
            HttpResponse projectUpdateResponse = AutoExtractionHelper.updateProject(projectUpdateUrl,projectUploadPayload);
            csAssert.assertTrue(projectUpdateResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String projectUpdateStr= EntityUtils.toString(projectUpdateResponse.getEntity());
            JSONObject projectUpdateJson = new JSONObject(projectUpdateStr);
            String errorMessage = String.valueOf(projectUpdateJson.get("response"));
            csAssert.assertTrue(errorMessage.contains("Default Project name cannot be changed."),"Default Project is also getting Updated");
            csAssert.assertAll();
        }
        catch (Exception e)
        {
            logger.info("Exception occurred while updating a default project");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();

    }

    /*Test Case to validate user should not be able to create a project without mapping a field*/
    @Test
    public void projectFieldMapping()throws IOException
    {
        CustomAssert csAssert = new CustomAssert();


        try
        {
            logger.info("Creating a new project after picking one field from all metadataFields");
            String createProjectUrl = "/metadataautoextraction/create";
            String projectName = "AutomationProject" + RandomString.getRandomAlphaNumericString(10);
            String createProjectPayload = "{\"name\":\""+ projectName +"\",\"description\":\"AutomationProject\",\"projectLinkedFieldIds\":[],\"clientId\":"+ clientId +"}";
            HttpResponse projectCreationResponse = AutoExtractionHelper.createProject(createProjectUrl,createProjectPayload);
            csAssert.assertTrue(projectCreationResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String projectCreationStr = EntityUtils.toString(projectCreationResponse.getEntity());
            JSONObject projectCreationJson = new JSONObject(projectCreationStr);
            csAssert.assertTrue(projectCreationJson.get("success").toString().equals("false"),"Project is getting created without field mapping");
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting Project Creation API"+ e.getStackTrace());
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();

    }
}
