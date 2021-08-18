package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AutoExtractionInsights
{
    private final static Logger logger = LoggerFactory.getLogger(AutoExtractionInsights.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;

    /*Validate whether AE sides are present in the list of entity-list(Insights)----Test Case Id:C89798*/
    @Test
    public void aeInsights()throws IOException
    {
        softAssert = new SoftAssert();
        try
        {
            logger.info("Testing AutoExtraction Insights API");
            String getAllInsightsUrl = "/insights/entity-list";
            HttpResponse insightsDataResponse = AutoExtractionHelper.getDataOfallInsights(getAllInsightsUrl);
            softAssert.assertTrue(insightsDataResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String insightsDataResponseStr = EntityUtils.toString(insightsDataResponse.getEntity());
            JSONArray insightsJsonResponseArray = new JSONArray(insightsDataResponseStr);
            int insightDataCount=insightsJsonResponseArray.length();
            ArrayList<String> insightsName = new ArrayList<>();
            for(int i=0;i<insightDataCount;i++)
            {
                insightsName.add((String) insightsJsonResponseArray.getJSONObject(i).get("name"));

            }
            boolean isAeInsightPresent=false;
            if(insightsName.contains("Auto Extraction"))
            {
                isAeInsightPresent=true;
            }
            softAssert.assertTrue(isAeInsightPresent==true,"There is no Permission of AE Insights");

        }

        catch (Exception e)
        {
            logger.info("Exception while hiiting Insights API");
        }

        softAssert.assertAll();
    }

    /*Test Case to validate if user clicks on Status "Completes" Insights then it should show the data of documents having status
    "Completed--Test case Id:C89800"
     */
    @Test
    public void statusCompletedInsight() throws IOException
    {
        try
        {
            String query = "/listRenderer/list/432/filterData?relationId=&contractId=&am=true&insightComputationId=94&_t=1587125958495";
            String payload = "{}";
            HttpResponse statusCompleteResponse = AutoExtractionHelper.aeInsights(query,payload);
            softAssert.assertTrue(statusCompleteResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String statusCompleteInsightStr = EntityUtils.toString(statusCompleteResponse.getEntity());
            JSONObject statusCompleteInsightJson = new JSONObject(statusCompleteInsightStr);

            String selectedStatus = String.valueOf(statusCompleteInsightJson.getJSONObject("368").getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").getJSONObject(0).get("name"));

            //Now Validating after selecting "Completed" status Insight AE doc listing is also having documents with completed status

            query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&insightComputationId=93&version=2.0";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse insightFilterResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String insightsFilterStr = EntityUtils.toString(insightFilterResponse.getEntity());
            JSONObject insightsJson = new JSONObject(insightsFilterStr);

            int metadataRecord = insightsJson.getJSONArray("data").length();
            int columnId = ListDataHelper.getColumnIdFromColumnName(insightsFilterStr, "status");

            List<String> documentStatus = new LinkedList<>();

            for (int i = 0; i < metadataRecord; i++)
            {
                JSONObject metadataObj = insightsJson.getJSONArray("data").getJSONObject(i);
                 String statusValue = metadataObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] statusOfDocs = statusValue.split(":;");
                documentStatus.add(String.valueOf(statusOfDocs[0]));
            }

            softAssert.assertTrue(!documentStatus.contains("InProgress")|| !documentStatus.contains("Submitted") || !documentStatus.contains("Queued") || !documentStatus.contains("Failed"),"Completed status insight is not workinf for Entity : Autoextraction");
            softAssert.assertAll();


        }

        catch (Exception e)
        {
            logger.info("Exception occurred while hitting AE Insights API");
        }
    }

    /*Test Case to check when user clicks on IN-PROGRESS Insight then it should show data of only in-progress documents--Test Case Id:C141277"*/

    @Test
    public void statusInprogressInsight() throws IOException
    {
        softAssert = new SoftAssert();
        try
        {
            String query = "/listRenderer/list/432/filterData?relationId=&contractId=&am=true&insightComputationId=93&_t=1587125958495";
            String payload = "{}";
            HttpResponse statusInprogressResponse = AutoExtractionHelper.aeInsights(query,payload);
            softAssert.assertTrue(statusInprogressResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String statusInprogressInsightsStr = EntityUtils.toString(statusInprogressResponse.getEntity());
            JSONObject statusCompleteInsightJson = new JSONObject(statusInprogressInsightsStr);

            String selectedStatus = String.valueOf(statusCompleteInsightJson.getJSONObject("368").getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").getJSONObject(0).get("name"));

            //Now Validating after selecting "In-Progress" status Insight AE doc listing is also having documents with In-progress status

            query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&insightComputationId=93&version=2.0";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"INPROGRESS\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse insightFilterResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String insightsFilterStr = EntityUtils.toString(insightFilterResponse.getEntity());
            JSONObject insightsJson = new JSONObject(insightsFilterStr);

            int metadataRecord = insightsJson.getJSONArray("data").length();
            int columnId = ListDataHelper.getColumnIdFromColumnName(insightsFilterStr, "status");

            List<String> documentStatus = new LinkedList<>();

            for (int i = 0; i < metadataRecord; i++)
            {
                JSONObject metadataObj = insightsJson.getJSONArray("data").getJSONObject(i);
                String statusValue = metadataObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] statusOfDocs = statusValue.split(":;");
                documentStatus.add(String.valueOf(statusOfDocs[0]));
            }

            softAssert.assertTrue(!documentStatus.contains("Completed")|| !documentStatus.contains("Submitted") || !documentStatus.contains("Queued") || !documentStatus.contains("Failed"),"In-progress status insight is not working for Entity : Autoextraction");
            softAssert.assertAll();


        }

        catch (Exception e)
        {
            logger.info("Exception occurred while hitting AE Insights API");
        }
    }
    /*Test Case to check when user clicks on SUBMITTED Insight then it should show data of only submitted documents--Test Case Id:C141276"*/

    @Test
    public void statusSubmittedInsights() throws IOException
    {
        softAssert = new SoftAssert();
        try
        {
            String query = "/listRenderer/list/432/filterData?relationId=&contractId=&am=true&insightComputationId=91&_t=1587125958495";
            String payload = "{}";
            HttpResponse statusSubmittedResponse = AutoExtractionHelper.aeInsights(query,payload);
            softAssert.assertTrue(statusSubmittedResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String statusSubmittedStr = EntityUtils.toString(statusSubmittedResponse.getEntity());
            JSONObject statusSubmittedJson = new JSONObject(statusSubmittedStr);

            String selectedStatus = String.valueOf(statusSubmittedJson.getJSONObject("368").getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").getJSONObject(0).get("name"));

            //Now Validating after selecting "Submitted" status Insight AE doc listing is also having documents with submitted status

            query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&insightComputationId=91&version=2.0";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"SUBMITTED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse insightFilterResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String insightsFilterStr = EntityUtils.toString(insightFilterResponse.getEntity());
            JSONObject insightsJson = new JSONObject(insightsFilterStr);

            int metadataRecord = insightsJson.getJSONArray("data").length();
            int columnId = ListDataHelper.getColumnIdFromColumnName(insightsFilterStr, "status");

            List<String> documentStatus = new LinkedList<>();

            for (int i = 0; i < metadataRecord; i++)
            {
                JSONObject metadataObj = insightsJson.getJSONArray("data").getJSONObject(i);
                String statusValue = metadataObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] statusOfDocs = statusValue.split(":;");
                documentStatus.add(String.valueOf(statusOfDocs[0]));
            }

            softAssert.assertTrue(!documentStatus.contains("Completed")|| !documentStatus.contains("In-Progress") || !documentStatus.contains("Queued") || !documentStatus.contains("Failed"),"Submitted status insight is not workinf for Entity : Autoextraction");
            softAssert.assertAll();


        }

        catch (Exception e)
        {
            logger.info("Exception occurred while hitting AE Insights API");
        }
    }
    /*Test Case to check when user clicks on QUEUED Insight then it should show data of only queued documents--Test Case Id:C141278"*/

    @Test
    public void queuedStatusInsights() throws IOException
    {
        softAssert = new SoftAssert();
        try
        {
            String query = "/listRenderer/list/432/filterData?relationId=&contractId=&am=true&insightComputationId=92&_t=1587125958495";
            String payload = "{}";
            HttpResponse statusQueuedResponse = AutoExtractionHelper.aeInsights(query,payload);
            softAssert.assertTrue(statusQueuedResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String statusQueuedStr = EntityUtils.toString(statusQueuedResponse.getEntity());
            JSONObject statusSubmittedJson = new JSONObject(statusQueuedStr);

            String selectedStatus = String.valueOf(statusSubmittedJson.getJSONObject("368").getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").getJSONObject(0).get("name"));

            //Now Validating after selecting "Queued" status Insight AE doc listing is also having documents with queued status

            query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&insightComputationId=92&version=2.0";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"QUEUED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse insightFilterResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String insightsFilterStr = EntityUtils.toString(insightFilterResponse.getEntity());
            JSONObject insightsJson = new JSONObject(insightsFilterStr);

            int metadataRecord = insightsJson.getJSONArray("data").length();
            int columnId = ListDataHelper.getColumnIdFromColumnName(insightsFilterStr, "status");

            List<String> documentStatus = new LinkedList<>();

            for (int i = 0; i < metadataRecord; i++)
            {
                JSONObject metadataObj = insightsJson.getJSONArray("data").getJSONObject(i);
                String statusValue = metadataObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] statusOfDocs = statusValue.split(":;");
                documentStatus.add(String.valueOf(statusOfDocs[0]));
            }

            softAssert.assertTrue(!documentStatus.contains("Completed")|| !documentStatus.contains("Submitted") || !documentStatus.contains("In-progress") || !documentStatus.contains("Failed"),"Queued status insight is not working for Entity : Autoextraction");
            softAssert.assertAll();


        }

        catch (Exception e)
        {
            logger.info("Exception occurred while hitting AE Insights API");
        }
    }
}
