package com.sirionlabs.test.supplier;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.internationalization.InternationalizationBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

public class TestSupplierInternationalization extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(TestSupplierInternationalization.class);

    /*
    TC-C378: Verify Field Renaming for Alias field of Supplier.
     */
    @Test
    public void testC378() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C378: Verify Field Renaming for Alias field of Supplier.");
            String expectedAliasLabel = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1),
                    "Metadata", "Alias");

            String listDataResponse = ListDataHelper.getListDataResponseVersion2("suppliers");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                String idValue = jsonArr.getJSONObject(0).getJSONObject(idColumn).getString("value");

                int supplierId = ListDataHelper.getRecordIdFromValue(idValue);
                String showResponse = ShowHelper.getShowResponseVersion2(1, supplierId);

                if (ParseJsonResponse.validJsonResponse(showResponse)) {
                    Map<String, String> fieldMap = ParseJsonResponse.getFieldByName(showResponse, "alias");
                    String actualAliasLabel = fieldMap.get("label");

                    boolean matchLabels = StringUtils.matchRussianCharacters(expectedAliasLabel, actualAliasLabel);

                    csAssert.assertTrue(matchLabels, "Expected Alias Label: " + expectedAliasLabel + " and Actual Alias Label: " + actualAliasLabel);
                } else {
                    csAssert.assertFalse(true, "Show API Response for Supplier Id " + supplierId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertFalse(true, "ListData API Response for Suppliers is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C378. " + e.getMessage());
        }

        csAssert.assertAll();
    }
}