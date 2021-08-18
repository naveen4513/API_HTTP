package com.sirionlabs.test.internationalization;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.List;

public class OptionsLabelsOnShowPage extends TestDisputeInternationalization {
    private final static Logger logger = LoggerFactory.getLogger(OptionsLabelsOnShowPage.class);
    public void verifyOptionsLabelsOnShowPage(String entityName, int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Field Labels on Show Page.");
            entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);
            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                List<String> allFieldNames = ParseJsonResponse.getAllFieldNamesOfSelectType(showResponse);
                if (allFieldNames.isEmpty()) {
                    throw new SkipException("Couldn't get All Field Names from Create Response.");
                }
                for ( String fieldName : allFieldNames ) {
                    if(!fieldName.toLowerCase().contains("timeZone") && !fieldName.toLowerCase().contains("supplier") && !fieldName.toLowerCase().contains("governanceBodyChild")
                            && !fieldName.toLowerCase().contains("startTime") && !fieldName.toLowerCase().contains("duration") && !fieldName.toLowerCase().contains("suppliers")){

                        List<String> fieldOptions = ParseJsonResponse.getAllOptionsForFieldAsJsonString(showResponse, fieldName,false);

                        for(String option:fieldOptions){

                            if (option.isEmpty()) {
                                csAssert.assertTrue(false, "Couldn't get Properties of Field Name " + fieldName + " from New Response.");
                                continue;
                            }
                            if (expectedPostFix == null) {
                                continue;
                            }
                            if (option.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                                csAssert.assertTrue(false, "[Option: + " + option + " +  contain "+ expectedPostFix +  " under $" + fieldName + "$ show page of " + entityName+"];");
                            } else {
                                csAssert.assertTrue(true, "[Option: " + option + " does not contain " + expectedPostFix + " under $" + fieldName + "$ show page of " + entityName + "];");
                            }
                        }
                    }
                }
            } else {
                csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of \"+entityName+\" is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Validating Field Labels on Show Page for Record Id {}. {}", recordId, e.getStackTrace());
        }
    }
}