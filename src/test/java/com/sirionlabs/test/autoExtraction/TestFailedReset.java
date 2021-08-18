package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;

public class TestFailedReset {
    private final static Logger logger = LoggerFactory.getLogger(TestFailedReset.class);

    //TC: C153673: End User: Verify Failed document is getting reset
    @Test
    public void testFailedReset() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        logger.info("Start Test:Verify Reset Failed Only feature is working fine");
        try {
            String listDataResponseStr=listDataFailedFilter("FAILED",0);
            JSONObject litDataJsonObj = new JSONObject(listDataResponseStr);
            int count = litDataJsonObj.getJSONArray("data").length();
            int columnId= ListDataHelper.getColumnIdFromColumnName(listDataResponseStr,"id");
            if (count > 0) {
                String[] idValue=litDataJsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).get("value").toString().split(":;");
                String docId=idValue[1];
                String failedOnlyPayload = "{\"documentIds\":["+docId+"]}";
                logger.info("Hitting redo failed only API for document id "+docId);
                HttpResponse redoFailedOnlyResponse = AutoExtractionHelper.failedReset(failedOnlyPayload);
                csAssert.assertTrue(redoFailedOnlyResponse.getStatusLine().getStatusCode()==200,"redo Failed Only response code is invalid");
                String failedOnlyStr=EntityUtils.toString(redoFailedOnlyResponse.getEntity());
                JSONObject failedOnlyJson=new JSONObject(failedOnlyStr);
                String success=failedOnlyJson.get("success").toString();
                logger.info("Validating redo failed Only Response for document id "+docId);
                csAssert.assertTrue(success.equalsIgnoreCase("true"),"Reset failed only operation is unsuccessful for document Id "+docId);
                String updatedListDataResponseStr=listDataFailedFilter("FAILED",0);
                JSONObject newListDataJson=new JSONObject(updatedListDataResponseStr);
                String[] updatedIdValue=newListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).get("value").toString().split(":;");
                String newDocId=updatedIdValue[1];
                csAssert.assertTrue(!(newDocId.equalsIgnoreCase(docId)),"Failed document is not getting reset for document Id "+docId);

            } else {
                throw new SkipException("Could not validate this TC as No failed documents found on AE Listing Page");
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e)
        {
            logger.info("Getting Exception while validating reset Failed only Documents due to "+e.getMessage());
            csAssert.assertTrue(false,"Getting Exception while validating reset Failed only Documents");
        }
        csAssert.assertAll();

    }

    //TC:C153649:End User:Verify Reset Failed Only button is working fine.
    //TC:C153663: Error message should be displayed if user tries to reset other than Failed document
    @Test
    public void testFailedOnlyReset() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {
            String listDataResponseStr = listDataFailedFilter("FAILED", 0);
            JSONObject litDataFailedJsonObj = new JSONObject(listDataResponseStr);
            int failedCount = litDataFailedJsonObj.getJSONArray("data").length();
            String listDataResponseStrCompleted = listDataFailedFilter("COMPLETED", 4);
            JSONObject litDataCompletedJsonObj = new JSONObject(listDataResponseStrCompleted);
            int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            if (failedCount > 0) {
                String[] idValue = litDataFailedJsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).get("value").toString().split(":;");
                String failedDocId = idValue[1];
                String[] completedIdValue = litDataCompletedJsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).get("value").toString().split(":;");
                String completedDocId = completedIdValue[1];
                String failedOnlyPayload = "{\"documentIds\":[" + failedDocId +","+completedDocId+"]}";
                logger.info("Hitting redo failed only API for document ids " + failedDocId + " and " + completedDocId);
                HttpResponse redoFailedOnlyResponse = AutoExtractionHelper.failedReset(failedOnlyPayload);
                csAssert.assertTrue(redoFailedOnlyResponse.getStatusLine().getStatusCode() == 200, "redo Failed Only response code is invalid");
                String failedOnlyStr = EntityUtils.toString(redoFailedOnlyResponse.getEntity());
                JSONObject failedOnlyJson = new JSONObject(failedOnlyStr);
                String success = failedOnlyJson.get("success").toString();
                logger.info("Validating message on hitting redo failed only API for other than failed Document");
                String validationMsg=failedOnlyJson.getString("response");
                csAssert.assertEquals(validationMsg,"Out of submitted 2 documents, 1 failed documents submitted for reset.","Getting incorrect Validation message on hitting redo failed only API");
                logger.info("Validating redo failed Only Response for document id " + failedDocId);
                csAssert.assertTrue(success.equalsIgnoreCase("true"), "Reset failed only operation is unsuccessful for document Id " + failedDocId);
                String updatedListDataResponseStr = listDataFailedFilter("FAILED", 0);
                JSONObject newListDataJson = new JSONObject(updatedListDataResponseStr);
                String[] updatedIdValue = newListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).get("value").toString().split(":;");
                String newDocId = updatedIdValue[1];
                csAssert.assertTrue(!(newDocId.equalsIgnoreCase(failedDocId)), "Failed document is not getting reset for document Id " + failedDocId);
                logger.info("Validating other than failed document is not getting reset on hitting redo failed only API for document id " + completedDocId);
                String updatedListCompletedStr = listDataFailedFilter("COMPLETED", 4);
                JSONObject completedListDataJson = new JSONObject(updatedListCompletedStr);
                String[] newCompletedIdValue = completedListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).get("value").toString().split(":;");
                String newCompletedDocId = newCompletedIdValue[1];
                csAssert.assertEquals(completedDocId, newCompletedDocId, "Document with Completed Status is getting reset on hitting redo failed only API for document Id " + completedDocId);
            }
        }
        catch (Exception e)
        {
            logger.info("Getting exception while validating redo failed only API");
            csAssert.assertTrue(false,"Getting exception while validating redo failed only API");
        }
        csAssert.assertAll();
    }

    public static String listDataFailedFilter(String optionName, int optionId)
    {
        CustomAssert csAssert=new CustomAssert();
        String litDataResponseStr="";
        try {
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\""+optionId+"\",\"name\":\""+optionName+"\"}]},\"filterId\":368," +
                    "\"filterName\":\"statusId\"}}},\"selectedColumns\":[]}";
            HttpResponse listDataResponse = AutoExtractionHelper.automationListing(payload);
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "List data API Response code is invalid");
            litDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"List Data API response is invalid");
        }
        return litDataResponseStr;
    }
}
