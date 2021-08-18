package com.sirionlabs.test.todo;

import com.sirionlabs.helper.internationalization.InternationalizationBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class TodoInternationalization extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(TodoInternationalization.class);

    /*
    TC-C379: Verify Todo Renaming.
    TC-C381: Verify Supplier field Renaming for Todo
     */
    @Test
    public void testC379() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C379: Verify Todo Renaming.");
        logger.info("Updating Supplier Field Label in Todo Listing.");
        //String updatedSupplierLabel = "поставщик";
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 469);

        try {
          /*  String updatePayload = fieldRenamingResponse.replace("clientFieldName\":\"Supplier", "clientFieldName\":\"" + updatedSupplierLabel);*/
            /*fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(updatePayload);*/

            String supplierFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Supplier", null, "id");
            String messagesListPayload = "[" + supplierFieldId + "]";
            String messagesListResponse = messagesListObj.hitFieldLabelMessagesList(messagesListPayload);

            if (ParseJsonResponse.validJsonResponse(messagesListResponse)) {
                JSONObject jsonObj = new JSONObject(messagesListResponse);

                if (jsonObj.has(supplierFieldId)) {
                    String actualSupplierLabel = jsonObj.getJSONObject(supplierFieldId).getString("name");
              /*      boolean matchLabels = StringUtils.matchRussianCharacters(updatedSupplierLabel, actualSupplierLabel);*/

/*                    csAssert.assertTrue(matchLabels, "Expected Supplier Label: " + updatedSupplierLabel + " and Actual Supplier Label: " + actualSupplierLabel);*/
                } else {
                    csAssert.assertFalse(true, "MessagesList API Response doesn't contain Object " + supplierFieldId);
                }
            } else {
                csAssert.assertFalse(true, "MessagesList API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C379. " + e.getMessage());
        } finally {
            logger.info("Reverting Supplier Label in Todo Listing");
            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(fieldRenamingResponse);
        }

        csAssert.assertAll();
    }
}