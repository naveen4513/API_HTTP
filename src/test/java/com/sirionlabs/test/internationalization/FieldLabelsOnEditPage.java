package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import java.util.List;
import java.util.Map;

public class FieldLabelsOnEditPage extends TestDisputeInternationalization{
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnShowPage.class);
    void verifyFieldLabelsOnEditPage(String entityName, int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Field Labels on Edit Page.");
            Edit editObj = new Edit();
            String editResponse = editObj.hitEdit(entityName, recordId);
            if (ParseJsonResponse.validJsonResponse(editResponse)) {
                List<String> allFieldNames = ParseJsonResponse.getAllFieldNames(editResponse);
                if (allFieldNames.isEmpty()) {
                    throw new SkipException("Couldn't get All Field Names from Edit Response.");
                }
                for ( String fieldName : allFieldNames ) {
                    Map<String, String> fieldProperties = ParseJsonResponse.getFieldByName(editResponse, fieldName);
                    if (fieldProperties.isEmpty()) {
                        csAssert.assertTrue(false, "Couldn't get Properties of Field Name " + fieldName + " from Edit Response.");
                        continue;
                    }
                    String fieldLabel = fieldProperties.get("label");

                        if (expectedPostFix == null) {
                            continue;
                        }
                        if (fieldLabel!= null && fieldLabel.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                            csAssert.assertTrue(false, "Field Label: [" + fieldLabel + "] contain: [" + expectedPostFix + "] under update page of "+entityName + " and update page id is "+recordId);
                        } else {
                            csAssert.assertTrue(true, "Field Label: [" + fieldLabel + "] does not contain: [" + expectedPostFix + "] under update page of "+entityName + " and update page id is "+recordId);
                        }
                }
            } else {
                csAssert.assertTrue(false, "Edit Get API Response for Record Id " + recordId + " of " + entityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Field Labels on Edit Page for Record Id " + recordId + " of "+entityName + e.getMessage());
        }
    }
}
