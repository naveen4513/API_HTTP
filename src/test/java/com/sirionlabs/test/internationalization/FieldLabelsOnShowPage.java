package com.sirionlabs.test.internationalization;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import java.util.List;
import java.util.Map;

public class FieldLabelsOnShowPage extends TestDisputeInternationalization {
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnShowPage.class);
    public void verifyFieldLabelsOnShowPage(String entityName, int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Field Labels on Show Page.");
            entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);
            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                List<String> allFieldNames = ParseJsonResponse.getAllFieldNames(showResponse);
                if (allFieldNames.isEmpty()) {
                    throw new SkipException("Couldn't get All Field Names from Show Response.");
                }
                for ( String fieldName : allFieldNames ) {
                    Map<String, String> fieldProperties = ParseJsonResponse.getFieldByName(showResponse, fieldName);
                    if (fieldProperties.isEmpty()) {
                        csAssert.assertTrue(false, "Couldn't get Properties of Field Name " + fieldName + " from Show Response.");
                        continue;
                    }
                    String fieldLabel = fieldProperties.get("label");



                        if (expectedPostFix == null) {
                            continue;
                        }
                        if (fieldLabel.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                            csAssert.assertTrue(false, "Field Label: [" + fieldLabel + "] contain: [" + expectedPostFix + "] under show page of " + entityName + " and show page id is " + recordId);
                        } else {
                            csAssert.assertTrue(true, "Field Label: [" + fieldLabel + "] does not contain: [" + expectedPostFix + "] under show page of " + entityName + " and show page id is " + recordId);
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

    public static String getApiPath() {
        return "/redis-cache/get/";
    }
    public static APIResponse getResponse(String fieldlabel,String ClientId)
    {
        String apiPath=getApiPath()+"?authToken="+fieldlabel;
        APIResponse response = executor.getAPIWithoutMandatoryDefaultHeaders(apiPath,null).getResponse();
        return response;
    }
}