package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.autoExtraction.ContractCreationViaAE;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GetDocumentIdHelper {
    private final static Logger logger = LoggerFactory.getLogger(ContractCreationViaAE.class);
    static int documentId;

    public static int getDocIdOfLatestDocument() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Hitting list data API");
        HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
        csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "ListData Response code is not valid");
        String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
        JSONObject listDataJson = new JSONObject(listDataStr);
        int documentCount = listDataJson.getJSONArray("data").length();
        int columnId = ListDataHelper.getColumnIdFromColumnName(listDataStr, "id");
        if (documentCount > 0) {
            {
                JSONObject listDataObj = listDataJson.getJSONArray("data").getJSONObject(0);
                documentId = Integer.parseInt(listDataObj.getJSONObject(Integer.toString(columnId)).getString("value").toString().split(":;")[1]);
            }
        }

        return documentId;
    }

    public static int metadataCountOnListPage(CustomAssert csAssert) throws IOException {
        String value="";
        try {
            logger.info("Hitting list data API");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "ListData Response code is not valid");
            String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
            int metadataCountCol = ListDataHelper.getColumnIdFromColumnName(listDataStr, "metadatacount");
            JSONObject listDataJson = new JSONObject(listDataStr);
            value = listDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(metadataCountCol)).getString("value");
        }
        catch ( Exception e)
        {
            csAssert.assertTrue(false,"Exception while getting metadata count from AE Listing page");
        }
        csAssert.assertAll();
        return Integer.parseInt(value);

    }
    public static int CountOnListPage(CustomAssert csAssert) throws IOException {
        String value="";
        try {
            logger.info("Hitting list data API");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "ListData Response code is not valid");
            String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
            int clauseCountCol = ListDataHelper.getColumnIdFromColumnName(listDataStr, "clausecount");
            JSONObject clauseListJson = new JSONObject(listDataStr);
            value = clauseListJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(clauseCountCol)).getString("value");
        }
        catch ( Exception e)
        {
            csAssert.assertTrue(false,"Exception while getting clause count from AE Listing page");
        }

        return Integer.parseInt(value);
    }
}
