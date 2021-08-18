package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.CommunicationAPI;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;

public class TestCommunication {

    private final static Logger logger = LoggerFactory.getLogger(TestCommunication.class);

    //TC:C152279: Verify that user should be able to add comment within the communication section.
    @Test(enabled = true)
    public void addComment() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {

            int oldCount=auditLogTab();
            logger.info("Audit log count before adding comment is "+oldCount);
            logger.info("Adding comment in Communication tab");
            CommunicationAPI communicationAPIObj = new CommunicationAPI();
            logger.info("Starting Test C152279:Hitting Add Comment API.");
            APIResponse communicationApiResponse = communicationAPIObj.communicationAPIResponse(CommunicationAPI.getAPIPath(), CommunicationAPI.getHeaders(), CommunicationAPI.getPayload());
            csAssert.assertTrue(communicationApiResponse.getResponseCode() == 200, "Add comment API Response code is not valid");
            String communicationApiResponseStr=communicationApiResponse.getResponseBody();
            JSONObject jsonObj = new JSONObject(communicationApiResponseStr);
            String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
            csAssert.assertEquals(status,"success","Comment is not updated successfully in communication tab");
            int newCount=auditLogTab();
            logger.info("Audit log count before adding comment is "+newCount);
            logger.info("Checking if Audit log is updated after adding comment in Communication Section");
            csAssert.assertTrue(oldCount<newCount,"Audit log tab is not updated");
        }
        catch (Exception e)
        {
            logger.info("Exception while adding comment in Communication Tab due to "+e.getMessage());
        }
        csAssert.assertAll();
    }

   //TC:C152285: Verify that after adding comment it should get logged in audit log tab

    public int  auditLogTab() throws IOException {
        logger.info("Hitting audit log tab on Document show page.");
        logger.info("Getting list data after applying Assigned document filter");
        HttpResponse listDataResponse = AutoExtractionHelper.assignedFilter();
        String listDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
        JSONObject jsonObject = new JSONObject(listDataResponseStr);
        JSONArray jsonArr = jsonObject.getJSONArray("data");
        JSONObject listdata = jsonArr.getJSONObject(0);
        int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
        String[] idValue = listdata.getJSONObject(Integer.toString(columnId)).getString("value").trim().split(":;");
        String docId = idValue[1];
        String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        HttpResponse auditLogTabData= AutoExtractionHelper.getAuditLog(payload,docId);
        String auditLogResponseStr = EntityUtils.toString(auditLogTabData.getEntity());
        JSONObject jsonObj=new JSONObject(auditLogResponseStr);
        int count=Integer.parseInt(jsonObj.get("filteredCount").toString());
        return count;
    }
}
