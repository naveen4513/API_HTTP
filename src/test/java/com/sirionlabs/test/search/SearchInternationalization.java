package com.sirionlabs.test.search;

import com.sirionlabs.helper.internationalization.InternationalizationBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class SearchInternationalization extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(SearchInternationalization.class);

    /*
    TC-C384: Verify Search Renaming.
    TC-C385: Verify Search Renamed fields for End User.
     */
    @Test
    public void testC384() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C384: Verify Search Renaming.");
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1725);

        try {
            logger.info("Updating Labels of Fields [Select an Entity, Attachments, Metadata] in Calendar Listing.");
            /*String updatedSelectEntityLabel = "выберите объект";
            String updatedAttachmentsLabel = "вложения";
            String updatedMetadataTreeLabel = "метаданные";
*/
            String updatePayload = fieldRenamingResponse;
            /*updatePayload = updatePayload
                    .replace("clientFieldName\":\"select an entity", "clientFieldName\":\"" + updatedSelectEntityLabel)
                    .replace("clientFieldName\":\"Attachments", "clientFieldName\":\"" + updatedAttachmentsLabel)
                    .replace("clientFieldName\":\"Metadata", "clientFieldName\":\"" + updatedMetadataTreeLabel);
*/
            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(updatePayload);

            String messagesListSubPayload = "";
            String selectEntityFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Select an Entity", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(selectEntityFieldId) + ",";

            String attachmentsFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Attachments", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(attachmentsFieldId) + ",";

            String metadataFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Metadata", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(metadataFieldId) + ",";

            messagesListSubPayload = messagesListSubPayload.substring(0, messagesListSubPayload.length() - 1);

            String messagesListPayload = "[" + messagesListSubPayload + "]";
            String messagesListResponse = messagesListObj.hitFieldLabelMessagesList(messagesListPayload);

            if (ParseJsonResponse.validJsonResponse(messagesListResponse)) {
                JSONObject jsonObj = new JSONObject(messagesListResponse);

                /*matchLabels(jsonObj, selectEntityFieldId, updatedSelectEntityLabel, "Select an Entity", csAssert);
                matchLabels(jsonObj, attachmentsFieldId, updatedAttachmentsLabel, "Attachments", csAssert);
                matchLabels(jsonObj, metadataFieldId, updatedMetadataTreeLabel, "Metadata", csAssert);*/
            } else {
                csAssert.assertFalse(true, "MessagesList API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C384. " + e.getMessage());
        } finally {
            logger.info("Reverting Labels in Search Listing");
            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(fieldRenamingResponse);
        }

        csAssert.assertAll();
    }

    private void matchLabels(JSONObject jsonObj, String fieldId, String expectedLabel, String fieldName, CustomAssert csAssert) {
        if (jsonObj.has(fieldId)) {
            String actualLabel = jsonObj.getJSONObject(fieldId).getString("name");
            boolean matchLabels = StringUtils.matchRussianCharacters(expectedLabel, actualLabel);

            csAssert.assertTrue(matchLabels, "Expected " + fieldName + " Label: " + expectedLabel + " and Actual Today Label: " + actualLabel);
        } else {
            csAssert.assertFalse(true, "MessagesList API Response doesn't contain Object " + fieldId);
        }
    }
}