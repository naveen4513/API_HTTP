package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.List;
import java.util.Map;

public class FieldLabelsOnCreatePage extends TestDisputeInternationalization{
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnCreatePage.class);
    public void verifyFieldLabelsOnCreatePage(String entityName, int recordId, CustomAssert csAssert) {
        if (!entityName.toLowerCase().contains("suppliers") && !entityName.toLowerCase().contains("vendors") && !entityName.toLowerCase().contains("child obligations")
                && !entityName.toLowerCase().contains("child service levels") && !entityName.toLowerCase().contains("governance body meetings")
                && !entityName.toLowerCase().contains("consumptions") && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups")
                && !entityName.toLowerCase().contains("externalcontractingparty")) {
            try {
                logger.info("Validating Field Labels on Create Page.");
                Clone cl = new Clone();
                String cloneResponse = cl.hitClone(entityName, recordId);
                if (ParseJsonResponse.validJsonResponse(cloneResponse)) {
                    List<String> allFieldNames = ParseJsonResponse.getAllFieldNames(cloneResponse);
                    if (allFieldNames.isEmpty()) {
                        throw new SkipException("Couldn't get All Field Names from Edit Response.");
                    }
                    for ( String fieldName : allFieldNames ) {
                        Map<String, String> fieldProperties = ParseJsonResponse.getFieldByName(cloneResponse, fieldName);
                        if (fieldProperties.isEmpty()) {
                            csAssert.assertTrue(false, "Couldn't get Properties of Field Name " + fieldName + " from New Response.");
                            continue;
                        }
                        String fieldLabel = fieldProperties.get("label");
                        if(fieldLabel==null) {
                            csAssert.assertTrue(true, "Exception while Validating Field Label, field renaming not supported " + fieldName + " on Create Page.");
                        }else {
                            if (expectedPostFix == null) {
                                continue;
                            }
                            if (fieldLabel.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                                csAssert.assertTrue(false, "Field Label: [" + fieldLabel + "] contain: [" + expectedPostFix + "] under create page of " + entityName);
                            } else {
                                csAssert.assertTrue(true, "Field Label: [" + fieldLabel + "] does not contain: [" + expectedPostFix + "]c under create page of " + entityName);
                            }
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "New Get API Response for Record Id " + recordId + " of " + entityName + " is an Invalid JSON.");
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Field Labels on New Page of "+entityName+". " + e.getMessage());
            }
        }
        else {
            csAssert.assertTrue(true, "Clone feature is not available for " + recordId + " of Entity " + entityName + " Unable to clone");
        }
    }
}