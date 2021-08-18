package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.*;

public class Assignment
{
    private final static Logger logger = LoggerFactory.getLogger(Assignment.class);
    CustomAssert csAssert = new CustomAssert();

//Uploading a Document to assign it to a stakeholder
@DataProvider
public Object[][] dataProviderForDocumentUpload() {
    List<Object[]> referencesData = new ArrayList<>();

    String DocumentFormat = "ReferencesDocumentUploadSchedule2";
    String fileUploadName = "Schedule 2.docx";
    referencesData.add(new Object[]{DocumentFormat, fileUploadName});

    DocumentFormat="ReferencesDocumentUploadSchedule4";
    fileUploadName = "Schedule 4.docx";
    referencesData.add(new Object[]{DocumentFormat, fileUploadName});
    return referencesData.toArray(new Object[0][]);
}

    @Test(dataProvider = "dataProviderForDocumentUpload",enabled = true, priority=1)
    public void testReferenceFileUpload (String DocumentFormat, String fileUploadName)throws IOException
    {
        logger.info("Testing Global Upload API for " + DocumentFormat);
        CustomAssert csAssert = new CustomAssert();

        // File Upload API to get the key from the uploaded Files
        logger.info("File Upload API to get the key of file that has been uploaded");
        try {
            String templateFilePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
            String templateFileName = fileUploadName;
            Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);

            try {
                // Hit Global Upload API
                logger.info("Hit Global Upload API");
                String payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[1]}]";
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                logger.info("Checking whether Extraction Status is complete or not");
                boolean isExtractionCompletedForUploadedFile = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, " " + DocumentFormat + " is not getting completed ");

            } catch (Exception e) {
                csAssert.assertTrue(false, "Global Upload API is not working because of :" + e.getStackTrace());
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because of :" + e.getStackTrace());
        }
        csAssert.assertAll();
    }

    /*Assigning an AE entity to a user to trigger workflow for that entity--Test Case Id:WF-2037*/

    @Test(priority = 2)
    public void assignForReview() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try {
            //Getting the role group Ids to add a Stakeholder for Assignment
            logger.info("Checking the list of role groups that are editable should be present in assign for review popup:" + "TestCaseId:C152235");
            HttpResponse roleGroupResponse = AutoExtractionHelper.getRoleGroupsForAssignment();
            csAssert.assertTrue(roleGroupResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String roleGroupsStr = EntityUtils.toString(roleGroupResponse.getEntity());
            JSONArray jsonarray = new JSONArray(roleGroupsStr);
            int roleGroupCount = jsonarray.length();
            List<Integer> roleGroupIds = new LinkedList<>();
            for (int i = 0; i < roleGroupCount; i++) {
                roleGroupIds.add((Integer) jsonarray.getJSONObject(i).get("id"));
            }

            if (roleGroupIds.size() < 1) {
                throw new SkipException("No Role Groups are there to assign");

            }


            //Now Assigning the entity to the Stakeholders
            try{
                logger.info("Assigning the entities to the stakeholders", "TestCaseId:C152240");
                //Getting the Document Id from AutoExtraction Listing page to perform Assignment
                HttpResponse listingResponse = AutoExtractionHelper.unAssignedFilter();
                csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
                JSONObject listingResponseJson = new JSONObject(listingResponseStr);

                //Getting Document Id from the list of documents
                int columnId = ListDataHelper.getColumnIdFromColumnName(listingResponseStr,"documentname");
                int documentId= Integer.parseInt(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

                try {
                    //Getting the user Ids and names to add them for assigning it to a user
                    HttpResponse userOptionsResponse = AutoExtractionHelper.getUserId();
                    csAssert.assertTrue(userOptionsResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
                    String userOptionsStr = EntityUtils.toString(userOptionsResponse.getEntity());
                    JSONObject userOptionsResponseJson = new JSONObject(userOptionsStr);
                    int dataCount = userOptionsResponseJson.getJSONArray("data").length();

                    //Getting the UserId and name from the list of Users
                    int userId= (int) userOptionsResponseJson.getJSONArray("data").getJSONObject(0).get("id");
                    String userName = (String) userOptionsResponseJson.getJSONArray("data").getJSONObject(0).get("name");

                    try {
                        String payload = "{\"entityIds\":[" + documentId + "],\"entityTypeId\":316," +
                                "\"stakeHolders\":[{\"roleGroupId\":\"" + roleGroupIds.get(0) + "\",\"userIds\":["+ userId + "]," +
                                "\"userGroupIds\":[]},{\"roleGroupId\":\"" + roleGroupIds.get(1) + "\",\"userIds\":["+ userId +"],"+
                                "\"userGroupIds\":[]}]}";

                        HttpResponse assignDocumentResponse = AutoExtractionHelper.workflowAssignment(payload);
                        csAssert.assertTrue(assignDocumentResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                        String assignmentResponseStr = EntityUtils.toString(assignDocumentResponse.getEntity());
                        JSONObject assignmentResponseObj = new JSONObject(assignmentResponseStr);
                        int statusCode = (int) assignmentResponseObj.get("statusCode");
                        String status = (String) assignmentResponseObj.get("status");
                        csAssert.assertTrue(statusCode == 200 && status.contains("Success"), "Some Of The Documents Does Not Exist Or Already Moved To Workflow");

                        try
                        {
                            //Validating the selected users got updated in AE doc listing

                            logger.info("Applying filter for stakeholders that are being assigned to the document:"+ "Test Case Id:C152420");
                            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"53\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"6712\",\"name\":\"\",\"group\":[{\"name\":\"Karuna Kakkar\",\"id\":\""+userId+"\",\"idType\":2,\"$$hashKey\":\"object:17963\"," +
                                    "\"selected\":true}],\"type\":1},{\"name\":\"Karuna Kakkar\",\"id\":\""+userId+"\",\"idType\":2,\"$$hashKey\":\"object:17963\",\"selected\":true},{\"id\":\"6714\",\"name\":\"\",\"group\":[{\"name\":\"Karuna Kakkar\",\"id\":\""+userId+"\",\"idType\":2,\"$$hashKey\":\"object:18244\",\"selected\":true}],\"type\":1},{\"name\":\"Karuna Kakkar\"," +
                                    "\"id\":\""+userId+"\",\"idType\":2,\"$$hashKey\":\"object:18244\",\"selected\":true}]},\"filterId\":53,\"filterName\":\"stakeholder\",\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"uitype\":\"STAKEHOLDER\"},\"393\":{\"filterId\":\"393\",\"filterName\":\"metadatavalue\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}," +
                                    "\"421\":{\"filterId\":\"421\",\"filterName\":\"folder\",\"entityFieldId\":null,\"entityFieldHtmlType\":null},\"438\":{\"filterId\":\"438\",\"filterName\":\"duplicatedocs\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"AssignedShow\"}]}},\"448\":{\"filterId\":\"448\"," +
                                    "\"filterName\":\"entityidsfilter\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}}},\"selectedColumns\":[]}";
                            HttpResponse appliedFilterResponse = AutoExtractionHelper.stakeholderFilter(payload);
                            String appliedFilterStr = EntityUtils.toString(appliedFilterResponse.getEntity());
                            JSONObject appliedFilterJson = new JSONObject(appliedFilterStr);
                            int metadataRecord = appliedFilterJson.getJSONArray("data").length();
                            columnId = ListDataHelper.getColumnIdFromColumnName(appliedFilterStr, "documentname");
                            List<Integer> listOfDocuments = new LinkedList<>();
                            boolean isDocumentAssigned;

                            for (int i = 0; i < metadataRecord; i++)
                            {
                                JSONObject documentObj = appliedFilterJson.getJSONArray("data").getJSONObject(i);
                                String documentNameValue = documentObj.getJSONObject(Integer.toString(columnId)).getString("value");
                                String[] docId = documentNameValue.split(":;");
                                listOfDocuments.add(Integer.valueOf(docId[1]));
                                if(listOfDocuments.contains(documentId))
                                {
                                    isDocumentAssigned=true;
                                    break;
                                }
                                break;
                            }
                            csAssert.assertTrue(isDocumentAssigned=true,"Document that has been assigned is not present in the list");

                        }

                        catch (Exception e)
                        {
                            logger.info("Exception occured while hitting AE listing API");
                            csAssert.assertTrue(false,e.getMessage());
                        }

                    } catch (Exception e) {
                        logger.info("Exception occured while hitting assignment API ");
                        csAssert.assertTrue(false, e.getMessage());
                    }
                }
                catch (Exception e)
                {
                    logger.info("Exception occured while hitting user details API");
                    csAssert.assertTrue(false,e.getMessage());
                }
            }
            catch (Exception e)
            {
                logger.info("Exception occured while hitting Document Listing API ");
                csAssert.assertTrue(false,e.getMessage());
            }

        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting stakeholders API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();


    }

}
