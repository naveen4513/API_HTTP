package com.sirionlabs.test.autoExtraction;
import com.sirionlabs.api.autoExtraction.aggregateDataAPI;
import com.sirionlabs.api.autoExtraction.fetchAllIdsAPI;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.WorkBenchHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class testWorkBench {
    private final static Logger logger = LoggerFactory.getLogger(testWorkBench.class);
    private WorkBenchHelper workBenchHelperObj=new WorkBenchHelper();
    /*TC: C152150 Verify the show-page
     */
    @Test
    public void verifyShowPage() {
        CustomAssert customAssert = new CustomAssert();
        try {
            fetchAllIdsAPI fetchAllIdsAPIOBJ = new fetchAllIdsAPI();
            APIResponse fetchIdApiResponse = fetchAllIdsAPIOBJ.fetchAllIdResponse(fetchAllIdsAPI.getAPIPath(), fetchAllIdsAPI.getHeaders(), fetchAllIdsAPI.getPayload());
            Integer responseCode = fetchIdApiResponse.getResponseCode();
            String fetchIdApiResponseStr = fetchIdApiResponse.getResponseBody();
            customAssert.assertTrue(responseCode == 200, "Response code is not Valid");
            JSONObject fetchIdResponse = new JSONObject(fetchIdApiResponseStr);
            String[] allEntityIds = fetchIdResponse.get("entityIds").toString().split(",");
            String documentId = allEntityIds[0].substring(1,allEntityIds[0].length()).trim();
            logger.info("Validating workbench with document url");
            String docId=WorkBenchHelper.getRecordId();
            HttpResponse validateWorkbenchResponse = workBenchHelperObj.validateWorkbench(docId);
            String validateWorkbenchResponseStr = EntityUtils.toString(validateWorkbenchResponse.getEntity());
            logger.info("document viewer stream API Response is:" + validateWorkbenchResponseStr);
            JSONObject documentResponseJson = new JSONObject(validateWorkbenchResponseStr);
            logger.info("Getting Document Id from Document url");
            String[] documentUrl = documentResponseJson.getJSONObject("data").get("documentURL").toString().split("/");
            String actualDocumentId = documentUrl[4];
            logger.info("Validating document id from fetch all Ids to document url's response for document id "+docId);
            customAssert.assertEquals(actualDocumentId,documentId, "Workbench View page is not correct for document ID "+docId);

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating ShowPage" + e.getMessage());
        }
        customAssert.assertAll();

    }

    /*TC: C152150 Verify the status on WorkBench
     */
    @Test
    public void verifyWorkflowStatus() {
        CustomAssert customAssert = new CustomAssert();

        try {
            aggregateDataAPI aggregateDataAPIObj = new aggregateDataAPI();
            APIResponse aggregateDataAPIResponse = aggregateDataAPIObj.aggregateDataAPIResponse(aggregateDataAPI.getAPIPath(), aggregateDataAPI.getHeaders(), aggregateDataAPI.getPayload());
            Integer responseCode = aggregateDataAPIResponse.getResponseCode();
            String aggregateDataResponse = aggregateDataAPIResponse.getResponseBody();
            customAssert.assertTrue(responseCode == 200, "Response code is not Valid");
            JSONObject jsonObj = new JSONObject(aggregateDataResponse);
            JSONArray jsonArray = jsonObj.getJSONArray("response").getJSONObject(0).getJSONArray("data");
            int actualCount = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getJSONObject(i).get("label").toString().equalsIgnoreCase("Newly Created")) {
                    actualCount = Integer.parseInt(jsonArray.getJSONObject(i).get("count").toString());
                    logger.info("Count for Newly created status displayed on WorkBench is: " + actualCount);
                }
            }
            logger.info("getting count for status Newly created document after applying status filter ");
            int expectedCount = workBenchHelperObj.getWorkflowStatusFilteredDataCount();
            logger.info("Validating status count displayed on workbench with filtered data count");
            customAssert.assertEquals(actualCount, expectedCount, "count shown on workbench is not same as filtered count");
        }
        catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating workflow status count with filtered data count:" + e.getStackTrace());
        }
        customAssert.assertAll();
    }

    //C152153: Verify the Document in Workbench after apply filters
    @Test
    public void testC152153()
    {
        CustomAssert customAssert=new CustomAssert();
        try{
            logger.info("Starting Test: Applying filter for Auto Extraction status : FAILED ");
            String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"0\",\"name\":\"FAILED\"}]},\"filterId\":368," +
                    "\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105," +
                    "\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":20265,\"columnQueryName\":\"id\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"}," +
                    "{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"}," +
                    "{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse listDataResponse= AutoExtractionHelper.automationListing(payload);
            customAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List Data API response is not valid");
            String listDataResponseStr= EntityUtils.toString(listDataResponse.getEntity());
            int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            JSONObject listDataResponseJSON=new JSONObject(listDataResponseStr);
            int documentCount = listDataResponseJSON.getJSONArray("data").length();

            if(documentCount>0)
            {
                JSONObject listObj = listDataResponseJSON.getJSONArray("data").getJSONObject(0);
                String idValue=listObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] idDetails=idValue.split(":;");
                String documentId=idDetails[1].trim();
                logger.info("Validating Workbench after applying filter");
                String workBenchPayload="{\"listId\":432,\"filterMap\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"0\",\"name\":\"FAILED\"}]}," +
                        "\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}},\"contractId\":\"\",\"relationId\":\"\",\"vendorId\":\"\",\"size\":1000,\"entityIds\":[]}";
                HttpResponse fetchIdResponse=AutoExtractionHelper.fetchAllId(workBenchPayload);
                customAssert.assertTrue(fetchIdResponse.getStatusLine().getStatusCode()==200,"response code for fetch all Id is not valid");
                String fetchIdApiResponseStr=EntityUtils.toString(fetchIdResponse.getEntity());
                JSONObject fetchIdResponseJson = new JSONObject(fetchIdApiResponseStr);
                String[] allEntityIds = fetchIdResponseJson.get("entityIds").toString().split(",");
                String workBenchDocumentId = allEntityIds[0].substring(1,allEntityIds[0].length()).trim();
                customAssert.assertTrue(documentId.equals(workBenchDocumentId),"1st document of list data doesn't match to workbench document Id");
            }
            else
            {
                throw new SkipException("No list data found after applying filer for filter Name Auto Extraction Status, FAILED");
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e)
        {
            logger.info("Exception while validating workbench after applying filter due to "+e.getMessage());
        }
        customAssert.assertAll();

    }

}
