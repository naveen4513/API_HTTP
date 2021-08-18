package com.sirionlabs.test.invoice.internationalization;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.internationalization.InternationalizationBase;

import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.HashMap;

public class TestPOInternationalization extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(TestPOInternationalization.class);

    /*
    SIR-15091
     */
    @Test
    public void testPOFieldRenaming() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Verify Listing Renaming for PO Entity.");

        String fieldRenamingListingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1, 1005);
        int poListId = 318;
        try {
            logger.info("Updating Labels of Fields in Listing.");
            String updatedLabel = "Updatedname" + RandomNumbers.getRandomNumberWithinRangeIndex(1,1000);
            String[] fieldNamesToTest = {"PO Number","Service Sub Category"};

            String updatePayload = fieldRenamingListingResponse;

            for(String fieldName : fieldNamesToTest){
                updatePayload = updatePayload
                        .replace("clientFieldName\":\"" + fieldName + "", "clientFieldName\":\"" + updatedLabel);
            }

            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(updatePayload);

            HashMap<String,String> fieldNamesOnListing = ListDataHelper.getFieldNamesOnListing(poListId, csAssert);

            for(String fieldName : fieldNamesToTest){

                if(fieldNamesOnListing.containsKey(fieldName)){
                    String actualFieldName = fieldNamesOnListing.get(fieldName);
                    csAssert.assertEquals(actualFieldName,updatedLabel,"Expected and Actual Field Name not matched for field " + fieldName + " Expected " + updatedLabel + " Actual " + actualFieldName);
                }else {
                    csAssert.assertTrue(false,"Field Name " + fieldName + " not found in Listing Response");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Field Renaming " + e.getMessage());
        } finally {
            logger.info("Reverting Labels in Listing");
            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(fieldRenamingListingResponse);


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
