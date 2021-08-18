package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.List;

public class OptionsLabelsOnCreatePage extends TestDisputeInternationalization{
    private final static Logger logger = LoggerFactory.getLogger(OptionsLabelsOnCreatePage.class);
    public void verifyOptionsLabelsOnCreatePage(String entityName, int recordId, CustomAssert csAssert) {
        if (!entityName.toLowerCase().contains("suppliers") && !entityName.toLowerCase().contains("vendors") && !entityName.toLowerCase().contains("child obligations")
                && !entityName.toLowerCase().contains("child service levels") && !entityName.toLowerCase().contains("governance body meetings")
                && !entityName.toLowerCase().contains("consumptions") && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups")
                && !entityName.toLowerCase().contains("externalcontractingparty")) {
            try {
                logger.info("Validating Field Labels on Create Page.");
                Clone cl = new Clone();
                String cloneResponse = cl.hitClone(entityName, recordId);
                if (ParseJsonResponse.validJsonResponse(cloneResponse)) {
                    List<String> allFieldNames = ParseJsonResponse.getAllFieldNamesOfSelectType(cloneResponse);
                    if (allFieldNames.isEmpty()) {
                        throw new SkipException("Couldn't get All Field Names from Create Response.");
                    }
                    for ( String fieldName : allFieldNames ) {

                        if(!fieldName.toLowerCase().contains("timeZone") && !fieldName.toLowerCase().contains("supplier") && !fieldName.toLowerCase().contains("governanceBodyChild")
                                && !fieldName.toLowerCase().contains("startTime") && !fieldName.toLowerCase().contains("duration") && !fieldName.toLowerCase().contains("suppliers")) {


                            List<String> fieldOptions = ParseJsonResponse.getAllOptionsForFieldAsJsonString(cloneResponse, fieldName, false);

                            for ( String option : fieldOptions ) {

                                if (option.isEmpty()) {
                                    csAssert.assertTrue(false, "Couldn't get Properties of Field Name " + fieldName + " from New Response.");
                                    continue;
                                }
                                if (expectedPostFix == null) {
                                    continue;
                                }
                                if (option.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                                    csAssert.assertTrue(false, "[Option: + " + option + " +  contain " + expectedPostFix + " under $" + fieldName + "$ create page of " + entityName + "];");
                                } else {
                                    csAssert.assertTrue(true, "[Option: " + option + " does not contain " + expectedPostFix + " under $" + fieldName + "$ create page of " + entityName + "];");
                                }
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