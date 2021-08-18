package com.sirionlabs.test.autoExtraction;

import com.google.inject.internal.cglib.core.$ObjectSwitchCallback;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BatchCreation {
    private final static Logger logger = LoggerFactory.getLogger(BatchCreation.class);
    CustomAssert csAssert = new CustomAssert();
    String batchName = "AutomationBatch" + RandomString.getRandomAlphaNumericString(5);

    //Test Case to Validate Batch Creation Functionality
    @Test
    public void batchCreation() throws IOException
    {
        try
        {
            //Fetching the project Id having maximum number of linked documents
            logger.info("Getting the project Id with highest number of Documents linked");
            HttpResponse projectListingResponse = AutoExtractionHelper.projectListing();
            csAssert.assertTrue(projectListingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String projectListingStr = EntityUtils.toString(projectListingResponse.getEntity());
            JSONObject projectListingJson = new JSONObject(projectListingStr);

            int columnId = ListDataHelper.getColumnIdFromColumnName(projectListingStr, "name");
            JSONObject projectObj = projectListingJson.getJSONArray("data").getJSONObject(0);
            String projectNameValue = projectObj.getJSONObject(Integer.toString(columnId)).getString("value");
            String[] projectName = projectNameValue.split(":;");
            int projectId = Integer.parseInt(projectName[1]);
            String projectUniqueName = projectName[0];

            //Applying Filter on automation listing for Batch Creation
            try {
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"385\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + projectId + "\",\"name\":\"" + projectUniqueName + "\"}]},\"filterId\":385,\"filterName\":\"projectids\"," +
                        "\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"393\":{\"filterId\":\"393\",\"filterName\":\"metadatavalue\",\"entityFieldId\":null," +
                        "\"entityFieldHtmlType\":null},\"421\":{\"filterId\":\"421\",\"filterName\":\"folder\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}," +
                        "\"448\":{\"filterId\":\"448\",\"filterName\":\"entityidsfilter\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}}},\"selectedColumns\":[]}";

                HttpResponse automationListingResponse = AutoExtractionHelper.automationListing(payload);
                csAssert.assertTrue(automationListingResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String automationListingStr = EntityUtils.toString(automationListingResponse.getEntity());
                JSONObject automationListingJson = new JSONObject(automationListingStr);
                columnId = ListDataHelper.getColumnIdFromColumnName(automationListingStr, "documentname");
                int totalDocument = automationListingJson.getJSONArray("data").length();
                List<Integer> documentIds = new LinkedList<>();
                for (int i = 0; i < totalDocument; i++) {
                    JSONObject docObj = automationListingJson.getJSONArray("data").getJSONObject(i);
                    String documentNameValue = docObj.getJSONObject(Integer.toString(columnId)).getString("value");
                    String[] documentNames = documentNameValue.split(":;");
                    documentIds.add(Integer.valueOf(documentNames[1]));
                }
                String documentIdsWithComma = (String) documentIds.stream().map(Object::toString).collect(Collectors.joining(","));
                logger.info(documentIdsWithComma);

                //Getting the role group Ids to add a Stakeholder for Batch Creation
                try {
                    logger.info("Checking the list of role groups that are editable should be present in assign for review popup:" + "TestCaseId:");
                    HttpResponse bulkEditResponse = AutoExtractionHelper.getBulkEditData();
                    csAssert.assertTrue(bulkEditResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                    String bulkEditStr = EntityUtils.toString(bulkEditResponse.getEntity());
                    JSONObject bulkEditJson = new JSONObject(bulkEditStr);
                    List<String> stakeholderNames = new LinkedList<>();
                    int stakeholderCount = bulkEditJson.getJSONArray("stakeholders").length();
                    List<Integer> stakeholderIds = new LinkedList<>();
                    for(int i =0;i<stakeholderCount;i++)
                    {
                        stakeholderIds.add((Integer) bulkEditJson.getJSONArray("stakeholders").getJSONObject(i).get("id"));
                        stakeholderNames.add((String) bulkEditJson.getJSONArray("stakeholders").getJSONObject(i).get("name"));
                    }

                    //Now creating the batch with the set of Documents
                    try {
                        logger.info("Creating Batch Set" + "Test Case Id:TC Id-C152120,C152127");
                        payload = "{\"stakeHolderDatas\":[{\"roleGroupId\":\""+stakeholderIds.get(0)+"\",\"roleGroupName\":\""+stakeholderNames.get(0)+"\",\"userIds\":[]}," +
                                "{\"roleGroupId\":\""+stakeholderIds.get(1)+"\",\"roleGroupName\":\""+stakeholderNames.get(0)+"\",\"userIds\":[]},{\"roleGroupId\":\""+stakeholderIds.get(2)+"\"," +
                                "\"roleGroupName\":\""+stakeholderNames.get(2)+"\",\"userIds\":[]}],\"name\":\""+batchName+"\",\"batchSize\":10," +
                                "\"documentIds\":[" + documentIdsWithComma + "],\"projectId\":\""+projectId+"\",\"orderBy\":\"\"}";
                        HttpResponse batchCreationResponse = AutoExtractionHelper.createBatchSet(payload);
                        csAssert.assertTrue(batchCreationResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                        String batchSetCreationStr = EntityUtils.toString(batchCreationResponse.getEntity());
                        JSONObject batchSetCreationJson = new JSONObject(batchSetCreationStr);
                        String message = (String) batchSetCreationJson.get("response");
                        csAssert.assertTrue(message.contains("Batch Set Created Successfully"),"Batch Set Creation is not working");

                    }
                    catch (Exception e)
                    {
                        logger.info("Error occured while Creating Batch Set");
                        csAssert.assertTrue(false,e.getMessage());
                    }
                }
                catch (Exception e)
                {
                    logger.info("Error occured while htting the role Group API");
                    csAssert.assertTrue(false,e.getMessage());
                }

            }
            catch (Exception e)
            {
                logger.info("Error occured while Applying Project Filter in AE listing");
                csAssert.assertTrue(false,e.getMessage());
            }

        }
        catch (Exception e)
        {
            logger.info("Error occured while hitting project Listing API");
            csAssert.assertTrue(false,e.getMessage());
        }
        csAssert.assertAll();

    }

    @Test
    public void batchCreationValidateListing() throws IOException
    {
        try
        {
            Thread.sleep(5000);
            logger.info("Validating on listing page whether the batch set has been created successfully or not"+"Test Case Id:C152125");
            HttpResponse batchFilterDataResponse = AutoExtractionHelper.getBatchFilterData(batchName);
            csAssert.assertTrue(batchFilterDataResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            logger.info("success batch Filter");
            String batchFilterStr = EntityUtils.toString(batchFilterDataResponse.getEntity());
            JSONObject batchFilterJson = new JSONObject(batchFilterStr);
            int totalBatches = batchFilterJson.getJSONArray("data").length();
            List<Integer> batchSetIds = new ArrayList<>();
            List<String> batchSetNames = new ArrayList<>();

            csAssert.assertTrue(totalBatches==2,"Batch Set count is not 2 for the set of 20 documents when batch Set Size is 10");
            logger.info("Is this correct");
            int totalBatchSets = batchFilterJson.getJSONArray("data").length();
            for(int i=0;i<totalBatchSets;i++)
            {
                batchSetIds.add((Integer) batchFilterJson.getJSONArray("data").getJSONObject(i).get("id"));
                batchSetNames.add((String) batchFilterJson.getJSONArray("data").getJSONObject(i).get("name"));
            }

            //Applying Filter for Batch Set in AE Listing
            try {
                logger.info("Applying Batch Set Filter in AE listing:" + "Test case Id:C152123");
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"393\":{\"filterId\":\"393\",\"filterName\":\"metadatavalue\"," +
                        "\"entityFieldId\":null,\"entityFieldHtmlType\":null},\"421\":{\"filterId\":\"421\",\"filterName\":\"folder\",\"entityFieldId\":null," +
                        "\"entityFieldHtmlType\":null},\"447\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + batchSetIds.get(0) + "\",\"name\":\"" + batchSetNames.get(0) + "\"}]}," +
                        "\"filterId\":447,\"filterName\":\"batchids\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"448\":{\"filterId\":\"448\"," +
                        "\"filterName\":\"entityidsfilter\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}}},\"selectedColumns\":[]}";

                HttpResponse listingResponse = AutoExtractionHelper.stakeholderFilter(payload);
                csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
                JSONObject listingResponseJson = new JSONObject(listingResponseStr);

                int columnId = ListDataHelper.getColumnIdFromColumnName(listingResponseStr, "batch");

                JSONObject batchObj = listingResponseJson.getJSONArray("data").getJSONObject(0);
                String batchValue = batchObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] batch = batchValue.split("::");
                String batchSetName = batch[batch.length-1];
                String[] expectedBatch = batchSetName.split(":;");
                String expectedData = expectedBatch[0];
                String actualData = batchSetNames.get(0);
                csAssert.assertTrue(expectedData.equalsIgnoreCase(actualData),"Batch Set Name is not present after applying filter");

                //Test Case to validate the number of documents in each bath should be 10

                int totalDocumentsInBatch1 = listingResponseJson.getJSONArray("data").length();
                csAssert.assertTrue(totalDocumentsInBatch1==10,"Batch size is not 10 for the selected documents");
            }
            catch (Exception e)
            {
                logger.info("Error occured while Applying Batch Filter on AE listing");
                csAssert.assertTrue(false,e.getMessage());
            }
        }
        catch (Exception e)
        {
            logger.info("Error occured while hitting automation Listing API");
            csAssert.assertTrue(false,e.getMessage());
        }
        csAssert.assertAll();

    }
    /*Verify that it should not show the same batch set twice for a single document*/
    @Test
    public void uniqueBatchPerDocument()throws IOException
    {
        logger.info("Verify that it should not show the same batch set twice for a single document "+ "Test Case Id: C152126");
        try{
            HttpResponse sortByBatchResponse = AutoExtractionHelper.batchSorting();
            csAssert.assertTrue(sortByBatchResponse.getStatusLine().getStatusCode()==200,"Response Code is invalid");
            String sortByBatchStr = EntityUtils.toString(sortByBatchResponse.getEntity());
            JSONObject sortByBatchJson = new JSONObject(sortByBatchStr);
            List<String> batchNames = new LinkedList<>();
            int columnId = ListDataHelper.getColumnIdFromColumnName(sortByBatchStr, "batch");
            int docColumnId = ListDataHelper.getColumnIdFromColumnName(sortByBatchStr, "documentname");
            JSONObject batchObj = sortByBatchJson.getJSONArray("data").getJSONObject(0);
            String batchValue = batchObj.getJSONObject(Integer.toString(columnId)).getString("value");
            int recordId = Integer.parseInt(batchObj.getJSONObject(Integer.toString(docColumnId)).getString("value").trim().split(":;")[1]);
            String[] batch = batchValue.split("::");
            for(int i=0;i<batch.length;i++)
            {
                String[] batches = batch[i].split(":;");
                batchNames.add(batches[0]);
            }
            Set<String> uniqueBatchNames = new HashSet<>(batchNames);
            csAssert.assertEquals(batchNames.size(),uniqueBatchNames.size(),"Duplicate Batch Set Names Found for recordId" +recordId);

        }
        catch (Exception e)
        {
            logger.info("Error occured while Sorting batch column");
            csAssert.assertTrue(false,e.getMessage());
        }
    }

}
