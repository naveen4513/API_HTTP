package com.sirionlabs.test.autoExtraction;

import com.flipkart.zjsonpatch.JsonDiff;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ServiceLevelExtraction
{
    private final static Logger logger = LoggerFactory.getLogger(ResetPermission.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;

 /*Test Case to validate whether extraction is working fine for SL */
    @Test
    public void ServiceLevelExtraction() throws IOException
    {
        softAssert = new SoftAssert();

        try {
            logger.info("Test Case to validate Service Level extraction working fine" + "Test Case Id:C91001");

            //Applying filter on  AE listing to pick the first document in completed state
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            softAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
            JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

            int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
            int documentId = Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

            //Check Data in Service Level Tab for the specified documentId (Test Case Id: C91003)
            logger.info("Hitting Service Level API to check extraction is working fine for Service Levels:" + "Test Case ID:C91003");
            query = "/listRenderer/list/510/listdata?isFirstCall=false";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":\"" + documentId + "\"}";
            HttpResponse serviceLevelResponse = AutoExtractionHelper.serviceLevelExtraction(query, payload);
            softAssert.assertTrue(serviceLevelResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String serviceLevelStr = EntityUtils.toString(serviceLevelResponse.getEntity());
            softAssert.assertTrue(JSONUtility.validjson(serviceLevelStr), "Not a valid Json means Service Level extraction is not Working");
            JSONObject serviceLevelJson = new JSONObject(serviceLevelStr);
            softAssert.assertTrue(serviceLevelJson.getJSONArray("data").length() > 1, "Extracted Data is not present");

            /*Test Case to check Category Filter is working fine in SL tab or not*/
            logger.info("Validating whether all the extracted categories are present in category filter or not"+"Test Case Id:C91004");
            query = "/listRenderer/list/510/listdata?isFirstCall=false";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"6048\",\"name\":\"Operational\"}]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":\""+ documentId +"\"}";
            HttpResponse categoryFilterResponse = AutoExtractionHelper.serviceLevelExtraction(query,payload);
            softAssert.assertTrue(categoryFilterResponse.getStatusLine().getStatusCode()==200,"Response code is invalid");
            String categoryFilterStr = EntityUtils.toString(categoryFilterResponse.getEntity());
            JSONObject categoryJson = new JSONObject(categoryFilterStr);
            columnId = ListDataHelper.getColumnIdFromColumnName(categoryFilterStr, "category");
            String CategoryName = categoryJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString();
            softAssert.assertTrue(CategoryName.contains("Operational"),"Category Filter is not woking for Oblgation Tab");

            /*Test Case to check Score Filter is working fine in SL tab: Test case Id:C141345*/
            logger.info("Validating the score filter in SL tab:" + "Test Case Id : C141345");
            query = "/listRenderer/list/510/listdata?isFirstCall=false";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"50\"}}},\"entityId\":\""+documentId+"\"}";
            HttpResponse scoreFilterResponse = AutoExtractionHelper.serviceLevelExtraction(query,payload);
            softAssert.assertTrue(scoreFilterResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String scoreFilterStr = EntityUtils.toString(scoreFilterResponse.getEntity());
            JSONObject scoreFilterJson = new JSONObject(scoreFilterStr);

            //Validating after apply score filter with min value 50, it should show the results>=50

            int metadataRecord = scoreFilterJson.getJSONArray("data").length();
            columnId = ListDataHelper.getColumnIdFromColumnName(scoreFilterStr, "score");
            List<Float> extractedScore = new LinkedList<>();

            for (int i = 0; i < metadataRecord; i++)
            {
                JSONObject metadataObj = scoreFilterJson.getJSONArray("data").getJSONObject(i);
                float score = (Float.parseFloat(metadataObj.getJSONObject(Integer.toString(columnId)).getString("value")));
                extractedScore.add(score);
            }

            boolean scoreFilterWorking;
            for(int i =0; i<extractedScore.size(); i++)
            {
                if(extractedScore.get(i)<=100 && extractedScore.get(i)>=50.00)
                {
                    scoreFilterWorking=true;
                }
                else
                {
                    scoreFilterWorking=false;
                }

                softAssert.assertTrue(scoreFilterWorking==true,"Score Filter is not working properly");
            }


        }

        catch (Exception e)
        {
            logger.info("Error occured while hitting SL tab API");
            softAssert.assertTrue(false,e.getMessage());

        }

        softAssert.assertAll();
    }

}
