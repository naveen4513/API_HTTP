package com.sirionlabs.test.clause;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.TreeMap;

public class TestClauseMisc extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestClauseMisc.class);

    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();
    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();

    /*
    TC-C583: To Verify Ordering/Position of Clause Fields.
    TC-C592: Verify fields on Clause listing after changing its position from Client Admin.
     */
    @Test
    public void testC583() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating TC-C583: Verify Ordering/Position of Clause Fields.");
            adminHelperObj.loginWithClientAdminUser();

            String configureResponse = executor.post("/listRenderer/list/271/configure", ListRendererConfigure.getHeaders(),
                    null).getResponse().getResponseBody();

            TreeMap<Integer, String> allExpectedColumnsMap = ListRendererConfigure.getAllColumnsMapSortedByOrderId(configureResponse);

            String additionalInfo = "Default Order of Clause Columns.";

            if (allExpectedColumnsMap != null) {
                validateColumnsOrder(allExpectedColumnsMap, csAssert, additionalInfo);

                //TC-C592
                int firstOrderId = allExpectedColumnsMap.firstKey();
                int lastOrderId = allExpectedColumnsMap.lastKey();

                //Swap these two order ids.
                String updatePayload = configureResponse.replace("order\":" + firstOrderId + ",", "order\":tempKey,");
                updatePayload = updatePayload.replace("order\":" + lastOrderId + ",", "order\":" + firstOrderId + ",");
                updatePayload = updatePayload.replace("order\":tempKey,", "order\":" + lastOrderId + ",");

                adminHelperObj.loginWithClientAdminUser();

                int updateResponse = ListRendererConfigure.updateListConfigure(271, updatePayload);

                if (updateResponse == 200) {
                    additionalInfo = "Order of Clause Columns after re-arranging them.";
                    configureResponse = executor.post("/listRenderer/list/271/configure", ListRendererConfigure.getHeaders(),
                            null).getResponse().getResponseBody();

                    allExpectedColumnsMap = ListRendererConfigure.getAllColumnsMapSortedByOrderId(configureResponse);
                    validateColumnsOrder(allExpectedColumnsMap, csAssert, additionalInfo);
                } else {
                    csAssert.assertFalse(true, "List Configure Update failed for Clause");
                }
            } else {
                csAssert.assertFalse(true, "Couldn't get All Expected Columns Map in Sorted Order. " + additionalInfo);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C583: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }

    private void validateColumnsOrder(TreeMap<Integer, String> allExpectedColumnsMap, CustomAssert csAssert, String additionalInfo) {
        if (allExpectedColumnsMap != null) {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
            String defaultUserListResponse = defaultUserListHelperObj.getDefaultUserListMetadataResponse("clauses");
            TreeMap<Integer, String> allActualColumnsMap = ListRendererConfigure.getAllColumnsMapSortedByOrderId(defaultUserListResponse);

            if (allActualColumnsMap == null) {
                csAssert.assertFalse(true, "Couldn't get All Columns Map from DefaultUserListMetaData Response. " + additionalInfo);
            } else {
                if (allActualColumnsMap.containsKey(0) && allActualColumnsMap.get(0).equalsIgnoreCase("bulkcheckbox")) {
                    allActualColumnsMap.remove(0);
                }

                if (allActualColumnsMap.size() == allExpectedColumnsMap.size()) {
                    csAssert.assertEquals(allActualColumnsMap.values(), allExpectedColumnsMap.values(), "Clause Columns Ordering/Position failed. " + additionalInfo);
                } else {
                    csAssert.assertFalse(true, "Expected Columns Size: " + allExpectedColumnsMap.size() + " and Actual Columns Size: " +
                            allActualColumnsMap.size() + ". " + additionalInfo);
                }
            }
        } else {
            csAssert.assertFalse(true, "Couldn't get All Expected Columns Map in Sorted Order. " + additionalInfo);
        }
    }
}