package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

public class WorkBenchHelper {
    private final static Logger logger = LoggerFactory.getLogger(WorkBenchHelper.class);

    public static HttpResponse validateWorkbench(String recordId)
    {
        String query="/documentviewerstream/check/316/"+recordId+"/316/"+recordId+"";
        HttpResponse response = null;
        HttpGet getRequest;
        try{
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept","application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get document viewer stream header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting document viewer stream  API. {}", e.getMessage());
        }
        return response;
    }

    public static int getWorkflowStatusFilteredDataCount() throws IOException {
        CustomAssert customAssert = new CustomAssert();
        String url="/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"11557\",\"name\":\"Newly Created\"}]},\"filterId\":6,\"filterName\":\"status\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"393\":{\"filterId\":\"393\",\"filterName\":\"metadatavalue\",\"entityFieldId\":null,\"entityFieldHtmlType\":null},\"421\":{\"filterId\":\"421\",\"filterName\":\"folder\",\"entityFieldId\":null,\"entityFieldHtmlType\":null},\"448\":{\"filterId\":\"448\",\"filterName\":\"entityidsfilter\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}}},\"selectedColumns\":[{\"columnId\":16797,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":17295,\"columnQueryName\":\"workflowstatus\"}]}";

        HttpResponse listingDataResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(url,payload);
        customAssert.assertTrue(listingDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
        String listingDataResponseStr = EntityUtils.toString(listingDataResponse.getEntity());
        JSONObject listDataResponseJson = new JSONObject(listingDataResponseStr);
        int count = Integer.parseInt(listDataResponseJson.get("filteredCount").toString());
        return count;

    }

    public static String getRecordId() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        HttpResponse automationListResponse = AutoExtractionHelper.getListDataForEntities("432", "{\"filterMap\":{}}");
        csAssert.assertTrue(automationListResponse.getStatusLine().getStatusCode() == 200, "Automation List Data Response Code is not valid");
        String automationListStr = EntityUtils.toString(automationListResponse.getEntity());
        JSONObject automationJsonObj = new JSONObject(automationListStr);
        JSONArray jsonArr = automationJsonObj.getJSONArray("data");
        int count = jsonArr.length();
        logger.info("Checking if list data count is greater than 0");
        csAssert.assertTrue(count>0,"There is no Data in Automation Listing");
        int columnId = ListDataHelper.getColumnIdFromColumnName(automationListStr, "id");
        JSONObject listdata= jsonArr.getJSONObject(0);
        String[] idValue = listdata.getJSONObject(Integer.toString(columnId)).getString("value").trim().split(":;");
        String docId = idValue[1];
        return docId;
    }


}
