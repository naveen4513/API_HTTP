package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AEDownloadSelectedDocs extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(AEDownloadSelectedDocs.class);
    static String documentIds;
    static int documentCount;

    public static String documentIds() throws IOException {

        logger.info("Hitting List data API to get List of document IDs for downloading");
        HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
        String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
        JSONObject listDataJson = new JSONObject(listDataStr);
        documentCount = listDataJson.getJSONArray("data").length();
        int documentColumnId = ListDataHelper.getColumnIdFromColumnName(listDataStr, "documentname");
        List<Integer> AEDocumentIds = new LinkedList<>();
        for (int i = 0; i < documentCount; i++) {
            JSONObject listDataObj = listDataJson.getJSONArray("data").getJSONObject(i);
            String documentValue = listDataObj.getJSONObject(Integer.toString(documentColumnId)).getString("value");
            String[] documentNames = documentValue.split(":;");
            int documentId = Integer.valueOf(documentNames[1]);
            AEDocumentIds.add(documentId);
        }
        Collections.sort(AEDocumentIds);
        documentIds= AEDocumentIds.stream().map(Object::toString).collect(Collectors.joining(","));

        return documentIds;
    }

    @Test
    public void downloadSelected()
    {
        CustomAssert csAssert = new CustomAssert();
        String outputFileName = "Autoextraction Doc Listing.xlsx";
        String outputFilePath="src/test/output";
        String aeDataEntity = "auto extraction";
        int entityUrlId=432;
        try{
                logger.info("Downloading Auto Extraction Selected Listing Data");
                DownloadListWithData DownloadListWithDataObj = new DownloadListWithData();
                documentIds = AEDownloadSelectedDocs.documentIds();
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc nulls last\",\"filterJson\":{},\"customFilter\":{\"downloadSelectedIds\":{\"selectIds\":["+documentIds+"]}}}}";
                Map<String, String> formParam = new HashMap<>();
                formParam.put("_csrf_token", "null");
                formParam.put("jsonData", payload);

                HttpResponse downloadResponse = DownloadListWithDataObj.hitDownloadListWithData(formParam, entityUrlId);
                logger.info("Checking if Auto extraction listing is downloaded");
                String downloadStatus =DownloadListWithDataObj.dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath, "ListDataDownloadOutput",aeDataEntity,"Autoextraction Doc Listing");
                if (downloadStatus == null) {
                    csAssert.assertTrue(false, "Auto Extraction List Download is unsuccessful");
                }
                else {
                    logger.info("Excel has been downloaded successfully for FilterName:");
                }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occurred while validating AE listing due to: "+e.getMessage());
        }
        finally {
            logger.info("Deleting downloaded file from location from:"+outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            FileUtils.deleteFile(outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            logger.info(outputFileName+" Downloaded sheet Deleted successfully.");
        }
        csAssert.assertAll();

    }
}


