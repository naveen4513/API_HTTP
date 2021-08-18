package com.sirionlabs.test.bePod;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Listeners(value = MyTestListenerAdapter.class)
public class TestNameTitleHyperLinkSIR4261 {

    private final static Logger logger = LoggerFactory.getLogger(TestNameTitleHyperLinkSIR4261.class);


    /*
    TC-C90349: Verify the Name Hyperlinks for Entities.
    TC-C90351: Verify navigation of HyperLinked Name.
     */
    @Test
    public void testNameHyperLinks() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test: Validate Name HyperLinks.");

        String[] allEntities = {"clauses", "definition", "consumptions", "sub contracts", "contract templates", "contract template structure",
                "purchase orders", "service data", "suppliers", "vendors", "work order requests"};

        ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
        List<FutureTask<Boolean>> taskList = new ArrayList<>();

        try {
            for (String entityName : allEntities) {
                try {
                    FutureTask<Boolean> result = new FutureTask<>(() -> {
                        logger.info("Validating Name HyperLink for Entity {}", entityName);
                        String listDataResponse = ListDataHelper.getListDataResponseVersion2(entityName);

                        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                            String expectedColumnName = getExpectedNameColumnName(entityName);

                            validateHyperLink(listDataResponse, expectedColumnName, entityName, "Name", csAssert);
                        } else {
                            csAssert.assertFalse(true, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
                        }
                        return true;
                    });
                    taskList.add(result);
                    executor.execute(result);
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while Validating Name HyperLink for Entity " + entityName + ". " + e.getMessage());
                }
            }

            for (FutureTask<Boolean> task : taskList)
                task.get();
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Executing Threads in parallel. " + e.getMessage());
            executor.shutdownNow();
        }

        csAssert.assertAll();
    }

    private String getExpectedNameColumnName(String entityName) {
        if (entityName.equalsIgnoreCase("service data")) {
            return "display_name";
        } else if (entityName.equalsIgnoreCase("work order requests")) {
            return "actualname";
        } else {
            return "name";
        }
    }

    /*
    TC-C90344: Verify Title HyperLinks for Entities.
    TC-C90350: Verify navigation of HyperLinked Title.
     */
    @Test
    public void testTitleHyperLinks() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test: Validate Title HyperLinks.");

        String[] allEntities = {"actions", "change requests", "child obligations", "child service levels", "contract draft request", "contracts", "sub contracts",
                "disputes", "governance body", "governance body meetings", "interpretations", "invoice line item", "invoices", "issues", "obligations", "service levels",
                "work order requests"};

        try {
            ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
            List<FutureTask<Boolean>> taskList = new ArrayList<>();

            for (String entityName : allEntities) {
                try {
                    FutureTask<Boolean> result = new FutureTask<>(() -> {
                        logger.info("Validating Title HyperLink for Entity {}", entityName);
                        String listDataResponse = ListDataHelper.getListDataResponseVersion2(entityName);

                        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                            String expectedColumnName = getExpectedTitleColumnName(entityName);

                            validateHyperLink(listDataResponse, expectedColumnName, entityName, "Title", csAssert);
                        } else {
                            csAssert.assertFalse(true, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
                        }
                        return true;
                    });
                    taskList.add(result);
                    executor.execute(result);
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while Validating Title HyperLink for Entity " + entityName + ". " + e.getMessage());
                }
            }

            for (FutureTask<Boolean> task : taskList)
                task.get();
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Executing threads in parallel. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private String getExpectedTitleColumnName(String entityName) {
        switch (entityName.toLowerCase()) {
            case "change requests":
            case "child service levels":
            case "contract draft request":
            case "interpretations":
                return "title";

            case "contracts":
            case "sub contracts":
                return "documenttitle";

            case "invoice line item":
                return "invoicetitle";

            default:
                return "name";
        }
    }

    private void validateHyperLink(String listDataResponse, String expectedColumnName, String entityName, String additionalInfo, CustomAssert csAssert) {
        String idColumnName = entityName.equalsIgnoreCase("invoice line item") ? "invoiceid" : "id";

        String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, idColumnName);
        String nameTitleColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, expectedColumnName);

        if (nameTitleColumnId == null) {
            csAssert.assertFalse(true, "Couldn't get Id of " + additionalInfo + " Column " + expectedColumnName +
                    " from ListData API Response for Entity " + entityName);

            return;
        }

        JSONObject jsonObj = new JSONObject(listDataResponse);
        JSONArray jsonArr = jsonObj.getJSONArray("data");

        int noOfRecords = Math.min(5, jsonArr.length());

        for (int i = 0; i < noOfRecords; i++) {
            String idValue = jsonArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
            int recordId = ListDataHelper.getRecordIdFromValue(idValue);
            String nameTitleValue = !jsonArr.getJSONObject(i).getJSONObject(nameTitleColumnId).isNull("value") ?
                    jsonArr.getJSONObject(i).getJSONObject(nameTitleColumnId).getString("value") : null;

            if (nameTitleValue == null) {
                if (!entityName.equalsIgnoreCase("change requests")) {
                    csAssert.assertFalse(true, additionalInfo + " Value is Null in ListData API Response for Entity " + entityName);
                }

                continue;
            }

            if (!nameTitleValue.contains(String.valueOf(recordId))) {
                csAssert.assertFalse(true, "Record Id " + recordId + " not present in " + additionalInfo +
                        " Value in ListData API for Entity " + entityName);
            }
        }
    }
}