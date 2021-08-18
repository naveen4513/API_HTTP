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

public class DownloadDocViewerDocument
{
    private final static Logger logger = LoggerFactory.getLogger(DownloadDocViewerDocument.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;

    @Test
    public void DownloadDocument()
    {
        softAssert = new SoftAssert();

        try
        {
            //Applying filter on  AE listing to pick the first document in completed state
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"datecreated\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":16797,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":16369,\"columnQueryName\":\"documentname\"},{\"columnId\":16370,\"columnQueryName\":\"contracttype\"},{\"columnId\":16371,\"columnQueryName\":\"status\"}," +
                    "{\"columnId\":16381,\"columnQueryName\":\"totalpages\"},{\"columnId\":16454,\"columnQueryName\":\"projects\"}," +
                    "{\"columnId\":16686,\"columnQueryName\":\"datecreated\"},{\"columnId\":16687,\"columnQueryName\":\"uploadedby\"}]}";
            HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            softAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
            JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

            int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
            int documentId = Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);
            String documentName = filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[0];
            String documentViewerFiles = "\\src\\test\\resources\\TestConfig\\AutoExtraction\\AutoExtractionDownloadedFiles\\DocumentViewerDownload";

            // Download the Document from Contract Document tab
             Boolean isDocumentDownloaded = AutoExtractionHelper.getDocumentFromDocumentViewer(System.getProperty("user.dir") +
                    "\\src\\test\\resources\\TestConfig\\AutoExtraction\\AutoExtractionDownloadedFiles\\DocumentViewerDownload",documentName,documentId);
            softAssert.assertTrue(isDocumentDownloaded, "Document Download is not working");
            AutoExtractionHelper.deleteAllFilesFromDirectory(documentViewerFiles);
            softAssert.assertAll();
        }
        catch (Exception e)
        {
            logger.info("Error occured while hitting ");
        }
        }
}
