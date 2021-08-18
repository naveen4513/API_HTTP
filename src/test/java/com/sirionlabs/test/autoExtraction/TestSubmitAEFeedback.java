package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AEUXAdminHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;


public class TestSubmitAEFeedback {
    private final static Logger logger = LoggerFactory.getLogger(TestSubmitAEFeedback.class);

    //TC:C153556 Verify to Create a New Clause
    @Test(priority = 0)
    public void testCreateNewClause()
    {
        CustomAssert csAssert=new CustomAssert();
        try{
            logger.info("Hitting list data API");
            HttpResponse listDataResponse=AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"ListData Response code is not valid");
            String listDataStr=EntityUtils.toString(listDataResponse.getEntity());
            JSONObject listDataJson = new JSONObject(listDataStr);
            int documentCount = listDataJson.getJSONArray("data").length();
            int columnId= ListDataHelper.getColumnIdFromColumnName(listDataStr, "id");
            if(documentCount>0)
            {
                JSONObject listDataObj = listDataJson.getJSONArray("data").getJSONObject(0);
                String[] idValue = listDataObj.getJSONObject(Integer.toString(columnId)).getString("value").toString().split(":;");
                String documentId = idValue[1];
                logger.info("Creating new clause for documentId "+documentId);
                DocumentShowPageHelper feedbackHelper=new DocumentShowPageHelper(documentId);
                String text="References in this Agreement to schedules shall be deemed to be references to schedules, the terms of which shall be incorporated into and form part of this Agreement";
                String categoryId=feedbackHelper.getNewCategoryId();
                String payload="{\"text\":\""+text+"\",\"pageNumber\":1,\"textId\":null,\"documentId\":"+documentId+",\"categoryId\":"+categoryId+",\"operation\":1,\"aeCoordinate\":null}";
                logger.info("Hitting create New Clause API for document ID "+documentId);
                HttpResponse createNewClauseResponse=AutoExtractionHelper.updateClause(payload);
                csAssert.assertTrue(createNewClauseResponse.getStatusLine().getStatusCode()==200,"Update Clause response code is not valid");
                String createClauseStr=EntityUtils.toString(createNewClauseResponse.getEntity());
                JSONObject createClauseJson=new JSONObject(createClauseStr);
                String success=createClauseJson.get("success").toString();
                logger.info("Validating create new Clause response");
                csAssert.assertTrue(success.equalsIgnoreCase("true"),"Insert new clause operation is not successful on document id "+documentId);
            }
            else {
                logger.warn("There is not data to perform submit  feedback");
            }
        }
        catch (Exception e)
        {
            logger.info("Create New clause operation is not successful due to "+e.getMessage());
         csAssert.assertTrue(false,"Create New clause operation is not successful ");
        }
        csAssert.assertAll();
    }

    //C153566: Verify By Given the Clause Feedback
    //C153567: Verify that Given Clause Feedback is Showing on UXAdmin
    @Test(priority = 1)
    public void testClauseFeedback()
    {
        CustomAssert csAssert=new CustomAssert();
        try{
            logger.info("Hitting List data API to get List of document IDs");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List data Api Response is invalid");
            String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
            JSONObject listDataJson = new JSONObject(listDataStr);
            int documentCount = listDataJson.getJSONArray("data").length();
            int columnId= ListDataHelper.getColumnIdFromColumnName(listDataStr, "id");
            if(documentCount>0)
            {
                JSONObject listDataObj = listDataJson.getJSONArray("data").getJSONObject(0);
                String[] idValue = listDataObj.getJSONObject(Integer.toString(columnId)).getString("value").toString().split(":;");
                String documentId = idValue[1];
                DocumentShowPageHelper feedbackHelper=new DocumentShowPageHelper(documentId);
                logger.info("Getting Text Id from clause list tab of document id "+documentId);
                String textId=feedbackHelper.getCategoryTextId();
                logger.info("Getting Clause Text from clause list");
                String text=feedbackHelper.getCategoryText();
                logger.info("Getting category Id from clause list");
                String previousCategoryId=feedbackHelper.getCategoryId();
                logger.info("Getting new category Id and Category name from show category API Response");
                int random = 2 + RandomUtils.nextInt(2,11);
                Map<String,String> categoryIdMap=feedbackHelper.getNewCategoryIdListMap();
                String newCategoryId=categoryIdMap.keySet().toArray()[random].toString();
                String newCategoryName=categoryIdMap.get(newCategoryId);

                int oldCount= DocumentShowPageHelper.feedbackCount(newCategoryId);
                String feedbackPayload="[{\"documentId\":\""+documentId+"\",\"textId\":\""+textId+"\",\"text\":\""+text+"\",\"extractionTypeId\":1001,\"fieldsFeedback\":{\"taggedText\":\""+text+"\",\"dataJson\":null},\"clauseFeedback\":{\"previousCategoryId\":\""+previousCategoryId+"\",\"newCategoryId\":"+newCategoryId+"}}]";
                logger.info("Hitting Extraction feedback API for category id "+previousCategoryId+" with Category Name "+newCategoryName+" and category id "+newCategoryId+" of document id "+documentId);
                HttpResponse feedbackResponse=AutoExtractionHelper.extractionFeedback(feedbackPayload);
                csAssert.assertTrue(feedbackResponse.getStatusLine().getStatusCode()==200,"Response code is invalid");
                logger.info("Feedback submitted for category id "+previousCategoryId +" with new category Id "+newCategoryId);
                String feedbackResponseStr=EntityUtils.toString(feedbackResponse.getEntity());
                JSONObject feedbackJson=new JSONObject(feedbackResponseStr);
                String ExpectedResult=feedbackJson.getJSONObject("response").get("success").toString();
                csAssert.assertTrue(ExpectedResult.equalsIgnoreCase("true"),"Feedback is not saved successfully for document id "+documentId);
                int newCount= DocumentShowPageHelper.feedbackCount(newCategoryId);
                logger.info("Verifying feedback count in feedback data list on UX Admin");
                csAssert.assertEquals(oldCount+1,newCount,"Feedback is not saved successfully in feedback data list");
                }
            else {
                logger.warn("There is not data to perform submit  feedback");
            }
        }
        catch (Exception e)
        {
            logger.info(" Exception while validating submit feedback for clause due to "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating submit feedback for clause due to "+e.getMessage());
        }
        csAssert.assertAll();

    }

    //TC: C153570: Verify Metadata Feedback Submitting Successfully
    //TC: C153571: Verify that Given Metadata Feedback is showing on UXAdmin
    @Test
    public void testMetadataFeedback()
    {

        CustomAssert csAssert=new CustomAssert();
        try{
            logger.info("Hitting List data API to get List of document IDs for downloading Extracted data");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List data Api Response is invalid");
            String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
            JSONObject listDataJson = new JSONObject(listDataStr);
            int documentCount = listDataJson.getJSONArray("data").length();
            int columnId= ListDataHelper.getColumnIdFromColumnName(listDataStr, "id");
            if(documentCount>0)
            {
                JSONObject listDataObj = listDataJson.getJSONArray("data").getJSONObject(0);
                String[] idValue = listDataObj.getJSONObject(Integer.toString(columnId)).getString("value").toString().split(":;");
                String documentId = idValue[1];
                DocumentShowPageHelper feedbackHelper=new DocumentShowPageHelper(documentId);
                logger.info("Getting Text Id from metadata list tab of document id "+documentId);
                String textId=feedbackHelper.getMetadataTextId();
                logger.info("Getting metadata Text from metadata list");
                String text=feedbackHelper.getMetadataText();
                String[] textArray=text.split(" ");
                String remainingText="";
                for(int j=1;j<textArray.length;j++)
                {
                    remainingText=remainingText+" "+textArray[j];
                }
                logger.info("Getting New Field id for Feedback submission");
                String NewFieldId=feedbackHelper.getNewFieldId();
                int oldCount= DocumentShowPageHelper.metadataFeedbackCount(NewFieldId);
                logger.info("Feedback list data count for Field Id "+NewFieldId+" is "+oldCount);
                String feedbackPayload="[{\"documentId\":\""+documentId+"\",\"textId\":\""+textId+"\",\"text\":\""+text+"\",\"extractionTypeId\":1001,\"fieldsFeedback\":{\"taggedText\":\"<span class=\\\"tag \\\">"+textArray[0]+"</span>"+remainingText+";\",\"dataJson\":[{\"fieldId\":"+NewFieldId+",\"fieldValue\":\""+textArray[0]+"\"}]},\"clauseFeedback\":null}]";
                logger.info("Hitting Submit feedback API with new Field id "+NewFieldId);
                HttpResponse feedbackSubmitResponse= AutoExtractionHelper.extractionFeedback(feedbackPayload);
                csAssert.assertTrue(feedbackSubmitResponse.getStatusLine().getStatusCode()==200,"Extraction feedback response code is not valid");
                String feedbackSubmitResponseStr=EntityUtils.toString(feedbackSubmitResponse.getEntity());
                JSONObject submitResponseJson=new JSONObject(feedbackSubmitResponseStr);
                String success=submitResponseJson.get("success").toString();
                csAssert.assertTrue(success.equalsIgnoreCase("true")," Feedback submit operation is unsuccessful for document id "+documentId);
                int newCount= DocumentShowPageHelper.metadataFeedbackCount(NewFieldId);
                logger.info("Feedback list data count for Field Id "+NewFieldId+" is "+newCount);
                logger.info("Validating feedback list data count on UX Admin");
                csAssert.assertEquals(oldCount+1,newCount,"Metadata feedback for field id "+NewFieldId+" is not added in feedback list after feedback submission");
               }
            else {
                logger.warn("There is not data to perform submit metadata feedback");
            }
        }
        catch (Exception e)
        {
            logger.info(" Exception while validating submit feedback for metadata due to "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating submit feedback for metadata due to "+e.getMessage());
        }
        csAssert.assertAll();

    }

    //TC:C153614: Verify Field creation from UX Admin
    @Test
    public void createField() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {
            logger.info("login to UX Admin...");
            DocumentShowPageHelper documentShowPageHelper = new DocumentShowPageHelper();
            documentShowPageHelper.clientAdminUserLogin();
            String metadataName = "API Automation Field" + DateUtils.getCurrentTimeStamp();
            String payload = "{\"field\":{\"name\":\"Field Name\",\"type\":\"text\",\"value\":\"" + metadataName + "\"},\"label\":null,\"extractionType\":null,\"mappedCategories\":null,\"active\":null,\"clientId\":1007,\"trainingDataFileList\":null,\"errors\":null}";
            logger.info("Hitting field creation API with metadata name " + metadataName);
            HttpResponse createFieldResponse = AEUXAdminHelper.CreateAPI(payload,"field");
            String createFieldResponseStr = EntityUtils.toString(createFieldResponse.getEntity());
            JSONObject createFieldJson = new JSONObject(createFieldResponseStr);
            csAssert.assertTrue(createFieldResponse.getStatusLine().getStatusCode() == 200, "Create Field API response is Invalid");
            String success = createFieldJson.get("success").toString();
            logger.info("Validating Field creation response");
            csAssert.assertTrue(success.equalsIgnoreCase("true"), "metadata creation operation is unsuccessful");
            HttpResponse fieldListResponse = AEUXAdminHelper.uxAdminListing(529, "{\"filterMap\":{}}");
            csAssert.assertTrue(fieldListResponse.getStatusLine().getStatusCode() == 200, "Field List API response code in invalid");
            String fieldListResponseStr = EntityUtils.toString(fieldListResponse.getEntity());
            JSONObject fieldListResponseJson = new JSONObject(fieldListResponseStr);
            Integer columnId = ListDataHelper.getColumnIdFromColumnName(fieldListResponseStr, "field_name");
            String[] value = fieldListResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).getString("value").split(":;");
            logger.info("Validate newly created field name in metadata list");
            csAssert.assertEquals(value[0].trim(), metadataName, "Newly created field " + metadataName + "is not present in metadata list");
            logger.info("Login to End User...");
            DocumentShowPageHelper.endUserLogin();
        }
        catch (Exception e)
        {
            logger.info("metadata creation validation failed");
            csAssert.assertEquals(false,"metadata creation validation failed");
        }
        csAssert.assertAll();

    }

    @Test
    public void createClause() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {
            logger.info("login to UX Admin...");
            DocumentShowPageHelper documentShowPageHelper = new DocumentShowPageHelper();
            documentShowPageHelper.clientAdminUserLogin();
            String clauseName = "API Automation Category" + DateUtils.getCurrentTimeStamp();
            String payload = "{\"category\":{\"name\":\"Clause Name\",\"type\":\"text\",\"value\":\""+clauseName+"\"},\"label\":null,\"extractionType\":null,\"mappedFields\":null,\"active\":null,\"clientId\":1007,\"trainingDataFileList\":null,\"errors\":null,\"baseClause\":{\"name\":\"Base Clause Text\",\"type\":\"textArea\"}}";
            logger.info("Hitting category creation API with category name " + clauseName);
            HttpResponse createFieldResponse = AEUXAdminHelper.CreateAPI(payload,"category");
            String createFieldResponseStr = EntityUtils.toString(createFieldResponse.getEntity());
            JSONObject createFieldJson = new JSONObject(createFieldResponseStr);
            csAssert.assertTrue(createFieldResponse.getStatusLine().getStatusCode() == 200, "Create Category API response is Invalid");
            String success = createFieldJson.get("success").toString();
            logger.info("Validating Field creation response");
            csAssert.assertTrue(success.equalsIgnoreCase("true"), "Clause creation operation is unsuccessful");
            String listPayload="{\"filterMap\":{}}";
            HttpResponse clauseListResponse = AEUXAdminHelper.uxAdminListing(509, listPayload);
            csAssert.assertTrue(clauseListResponse.getStatusLine().getStatusCode() == 200, "Category List API response code in invalid");
            String categoryListResponseStr = EntityUtils.toString(clauseListResponse.getEntity());
            JSONObject categoryListResponseJson = new JSONObject(categoryListResponseStr);
            Integer columnId = ListDataHelper.getColumnIdFromColumnName(categoryListResponseStr, "category_name");
            ArrayList<String> clauseList=new ArrayList<>();
            int count=categoryListResponseJson.getJSONArray("data").length();
            for(int i=0;i<count;i++) {
                String[] value = categoryListResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(columnId)).getString("value").split(":;");
                clauseList.add(value[0].trim());
            }
            logger.info("Validate newly created category name in metadata list");
            csAssert.assertTrue(clauseList.contains(clauseName), "Newly created category " + clauseName + "is not present in clause list");
            logger.info("Login to End User...");
            DocumentShowPageHelper.endUserLogin();
        }
        catch (Exception e)
        {
            logger.info("metadata creation validation failed");
            csAssert.assertEquals(false,"metadata creation validation failed");
        }
        csAssert.assertAll();

    }


}
