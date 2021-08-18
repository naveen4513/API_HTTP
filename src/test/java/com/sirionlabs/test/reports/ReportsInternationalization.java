package com.sirionlabs.test.reports;

import com.sirionlabs.helper.internationalization.InternationalizationBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class ReportsInternationalization extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(ReportsInternationalization.class);

    /*
    TC-C389: Verify Report Renaming.
    TC-C391: Verify Report Renamed fields for End User.
     */
    @Test
    public void testC389() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C389: Verify Reports Renaming.");
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1724);

        try {
            logger.info("Updating Labels of Fields [Select an Entity, Attachments, Metadata] in Calendar Listing.");
            String updatedReportsLabel = "отчеты";
            String updatedEntityTypeLabel = "тип объекта";

            String updatePayload = fieldRenamingResponse;
            updatePayload = updatePayload
                    .replace("clientFieldName\":\"Reports", "clientFieldName\":\"" + updatedReportsLabel)
                    .replace("clientFieldName\":\"Entity Type :", "clientFieldName\":\"" + updatedEntityTypeLabel);

            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(updatePayload);

            String messagesListSubPayload = "";
            String selectEntityFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Reports", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(selectEntityFieldId) + ",";

            String attachmentsFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Entity Type :", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(attachmentsFieldId) + ",";

            messagesListSubPayload = messagesListSubPayload.substring(0, messagesListSubPayload.length() - 1);

            String messagesListPayload = "[" + messagesListSubPayload + "]";
            String messagesListResponse = messagesListObj.hitFieldLabelMessagesList(messagesListPayload);

            if (ParseJsonResponse.validJsonResponse(messagesListResponse)) {
                JSONObject jsonObj = new JSONObject(messagesListResponse);

                matchLabels(jsonObj, selectEntityFieldId, updatedReportsLabel, "Reports", csAssert);
                matchLabels(jsonObj, attachmentsFieldId, updatedEntityTypeLabel, "Entity Type :", csAssert);
            } else {
                csAssert.assertFalse(true, "MessagesList API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C389. " + e.getMessage());
        } finally {
            logger.info("Reverting Labels in Reports Listing");
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