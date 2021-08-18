package com.sirionlabs.test.contract;

import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestContractDocumentDocUpload {

    private final static Logger logger = LoggerFactory.getLogger(TestContractDocumentDocUpload.class);

    @Test
    public void testDocUpload() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts");
            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

            String idValue = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0).getJSONObject(idColumn).getString("value");
            int contractId = ListDataHelper.getRecordIdFromValue(idValue);

            logger.info("Uploading Document on Contract Document tab of Contract Id {}", contractId);
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);

            String templatePath = "src/test/resources/TestConfig/Contract";
            String templateName = "Test.docx";

            String tabListPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, tabListPayload);

            int noOfDocsBeforeUpload = new JSONObject(tabListResponse).getJSONArray("data").length();

            Map<String, String> queryParameters = setPostParams(templateName, contractId, randomKeyForFileUpload);

            FileUploadDraft fileUploadDraft = new FileUploadDraft();
            String uploadResponse = fileUploadDraft.hitFileUpload(templatePath, templateName, queryParameters);

            if (ParseJsonResponse.validJsonResponse(uploadResponse)) {
                String payload = getPayloadForSubmitDraft(contractId, uploadResponse);
                String submitDraftResponse = SubmitDraft.hitSubmitDraft("contracts", payload);

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    tabListResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, tabListPayload);

                    int noOfDocsAfterUpload = new JSONObject(tabListResponse).getJSONArray("data").length();

                    if (noOfDocsAfterUpload <= noOfDocsBeforeUpload) {
                        csAssert.assertFalse(true, "Document not appearing in Contract Document Tab of Contract Id " + contractId);
                    }
                } else {
                    csAssert.assertFalse(true, "Document Submit Draft failed.");
                }
            } else {
                csAssert.assertFalse(true, "Doc Upload Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Contract Document Doc Upload. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private Map<String, String> setPostParams(String templateName, int entityId, String randomKeyForFileUpload) {

        Map<String, String> map = new HashMap<>();
        if (entityId == -1)
            return map;

        map.put("name", templateName.split("\\.")[0]);
        map.put("extension", "docx");
        map.put("entityTypeId", String.valueOf(61));
        map.put("entityId", String.valueOf(entityId));
        map.put("key", randomKeyForFileUpload);

        return map;
    }

    private String getPayloadForSubmitDraft(int contractId, String uploadResponse) {
        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(61, contractId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonObject = new JSONObject(showPageResponse);
                JSONObject uploadObj = new JSONObject(uploadResponse);

                JSONArray contractDocumentValuesArr = new JSONArray("[{\"templateTypeId\":" + uploadObj.getInt("templateTypeId") +
                        ",\"documentFileId\": null, \"documentSize\": " + uploadObj.getInt("documentSize") + ", \"key\": " + uploadObj.getString("key") +
                        ", \"documentStatusId\": 1, \"permissions\": { \"financial\": false, \"legal\": false, \"businessCase\": false }, " +
                        "\"performanceData\": false, \"searchable\": false, \"shareWithSupplierFlag\": false } ]");

                jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", contractDocumentValuesArr);
                jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);
                return "{\"body\":{\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";
            }
        } catch (Exception e) {
            logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
        }

        return null;
    }
}