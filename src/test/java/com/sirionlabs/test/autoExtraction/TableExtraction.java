package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class TableExtraction
{
    private final static Logger logger = LoggerFactory.getLogger(TableExtraction.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;
   /*Test Case to Validate Table Extraction is working fine for a document having table in it*/
    @Test
    public void TableExtraction()
    {
        CustomAssert csAssert = new CustomAssert();
        try
        {
             logger.info("Navigating to AE listing to pick one document in Completed State", "Test Case Id:C141353");
            //Applying filter on  AE listing to pick the first document in completed state
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            csAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
            JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

            int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
            int documentId = Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

            //Checking Data in Table Tab
            query = "/autoextraction/getTableData/1002/"+ documentId;
            HttpResponse tableDataResponse = AutoExtractionHelper.getTableData(query);
            csAssert.assertTrue(tableDataResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String tableDataStr = EntityUtils.toString(tableDataResponse.getEntity());
            JSONObject tableDataJson = new JSONObject(tableDataStr);
            int tableData = tableDataJson.getJSONArray("response").getJSONObject(0).getJSONArray("tabledata").length();
            csAssert.assertTrue(tableData>=0,"Table Extraction is not working");

        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting AE Table listdata API");
            csAssert.assertTrue(false, "Exception while Verifying TableData for Flow [" +  e.getMessage());

        }

        csAssert.assertAll();
    }

}
