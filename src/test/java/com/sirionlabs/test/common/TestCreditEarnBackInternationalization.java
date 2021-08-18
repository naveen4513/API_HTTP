package com.sirionlabs.test.common;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestCreditEarnBackInternationalization extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCreditEarnBackInternationalization.class);

    public FieldRenaming fieldRenamingObj = new FieldRenaming();
    private UpdateAccount updateAccountObj = new UpdateAccount();
    private String endUserLoginId;

    @BeforeClass
    public void beforeClass() {
        endUserLoginId = ConfigureEnvironment.getEndUserLoginId();

        logger.info("Changing User Language to Russian.");
        updateAccountObj.updateUserLanguage(endUserLoginId, 1002, 1000);
    }

    @AfterClass
    public void afterClass() {
        logger.info("Changing User Language back to English.");
        updateAccountObj.updateUserLanguage(endUserLoginId, 1002, 1);
    }

    /*
    TC-C7873: Verify Label Headers in New API Response for Credit & EarnBack from Contract and SL
     */
    @Test
    public void testC7873() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7873: Verify Label Headers in New API Response for Credit & EarnBack from Contract");

            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int recordId = -1;
                String createLink = null;
                String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
                    int entityId = ListDataHelper.getRecordIdFromValue(idValue);

                    String createLinksResponse = CreateLinks.getCreateLinksV2Response(61, entityId);
                    createLink = CreateLinks.getCreateLinkForEntity(createLinksResponse, 79);

                    if (createLink != null) {
                        recordId = entityId;
                        break;
                    }
                }

                if (createLink != null && recordId != -1) {
                    String newResponse = executor.get(createLink, ApiHeaders.getDefaultLegacyHeaders()).getResponse().getResponseBody();
                    String fieldLabelsResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 411);

                    validateGeneralTabHeader("contracts", newResponse, fieldLabelsResponse, csAssert);
                } else {
                    csAssert.assertFalse(true, "Couldn't find Contract record having permission to create Credit/Earnback.");
                }
            } else {
                csAssert.assertFalse(true, "ListData API Response for Entity Contracts is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C7873: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateGeneralTabHeader(String parentEntityName, String newResponse, String fieldLabelsResponse, CustomAssert csAssert) {
        try {
            logger.info("Validating Tabs Header in New API Response of Credit/EarnBack from Entity {}", parentEntityName);
            String expectedLabel = fieldRenamingObj.getClientFieldNameFromName(fieldLabelsResponse, "Tab Labels", "General");
            String actualLabel = new JSONObject(newResponse).getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields")
                    .getJSONObject(0).getString("label");

            matchLabels(expectedLabel, actualLabel, csAssert, "General Tab Header Validation.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating General Tab of Entity " + parentEntityName + ". " + e.getMessage());
        }
    }

    private void matchLabels(String expectedFieldLabel, String actualFieldLabel, CustomAssert csAssert, String additionalInfo) {
        boolean matchLabels = StringUtils.matchRussianCharacters(expectedFieldLabel, actualFieldLabel);
        csAssert.assertTrue(matchLabels, "Expected " + expectedFieldLabel + " Label: " + expectedFieldLabel + " and Actual Label: " + actualFieldLabel + ". " +
                additionalInfo);
    }
}