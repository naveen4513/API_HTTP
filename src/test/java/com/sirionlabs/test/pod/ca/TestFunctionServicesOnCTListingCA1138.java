package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFunctionServicesOnCTListingCA1138 {

    private final static Logger logger = LoggerFactory.getLogger(TestFunctionServicesOnCTListingCA1138.class);

    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();
    private Check checkObj = new Check();

    /*
    TC-C90838: Verify Function and Service Fields should appear on Contract Template Listing.
    TC-C90855: Verify Function and Service Fields should appear in column drop down on Contract Template Listing.
    TC-C90839: Verify Function and Service Fields should appear on Contract Template listing excel download.
    TC-C90915: Verify Function and Service Fields for both Client Type and Supplier Type user.
     */
    @Test
    public void testC90838() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90838: Verify Function and Service fields should appear on Contract Template Listing.");
            logger.info("Validating for Client Type User.");

            String additionalInfo = "Client Type User. ";
            validateFieldsInDefaultUserListMetadataResponse(csAssert, additionalInfo);
            validateFieldsInListDataResponse(csAssert, additionalInfo);
            validateFieldsInDownloadedExcel(csAssert, additionalInfo);

            logger.info("Validating for Supplier Type User.");

            additionalInfo = "Supplier Type User. ";
            checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserName"),
                    ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserPassword"));

            validateFieldsInDefaultUserListMetadataResponse(csAssert, additionalInfo);
            validateFieldsInListDataResponse(csAssert, additionalInfo);
            validateFieldsInDownloadedExcel(csAssert, additionalInfo);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90838: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }

    private void validateFieldsInDefaultUserListMetadataResponse(CustomAssert csAssert, String additionalInfo) {
        try {
            logger.info("Validating Functions and Services fields in DefaultUserListMetadata API Response. " + additionalInfo);
            String defaultUserListResponse = defaultUserListHelperObj.getDefaultUserListMetadataResponse("contract templates");

            Boolean functionColumnPresent = defaultUserListHelperObj.hasColumnQueryName(defaultUserListResponse, "functions");
            Boolean serviceColumnPresent = defaultUserListHelperObj.hasColumnQueryName(defaultUserListResponse, "services");

            if (functionColumnPresent == null || !functionColumnPresent) {
                csAssert.assertFalse(true, "Functions Column not found in DefaultUserListMetadata API Response for Contract Templates. " + additionalInfo);
            }

            if (serviceColumnPresent == null || !serviceColumnPresent) {
                csAssert.assertFalse(true, "Services Column not found in DefaultUserListMetadata API Response for Contract Templates. " + additionalInfo);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Functions & Services fields in DefaultUserListMetadata API Response. " +
                    additionalInfo + e.getMessage());
        }
    }

    private void validateFieldsInListDataResponse(CustomAssert csAssert, String additionalInfo) {
        try {
            logger.info("Validating Functions and Services fields in ListData API Response. " + additionalInfo);
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contract templates");

            String functionColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "functions");
            String serviceColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "services");

            if (functionColumnId == null) {
                csAssert.assertFalse(true, "Functions Column not found in ListData API Response for Contract Templates. " + additionalInfo);
            }

            if (serviceColumnId == null) {
                csAssert.assertFalse(true, "Services Column not found in ListData API Response for Contract Templates. " + additionalInfo);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Functions & Services fields in ListData API Response. " +
                    additionalInfo + e.getMessage());
        }
    }

    private void validateFieldsInDownloadedExcel(CustomAssert csAssert, String additionalInfo) {
        try {
            logger.info("Starting Test TC-C90839: Verify Function and Service Fields should appear on Contract Template Listing excel download. {}", additionalInfo);
            String defaultListDataPayload = "{\"filterMap\":{\"entityTypeId\":140,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"101\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1003\",\"name\":\"End User License Agreement\"}]},\"filterId\":101,\"filterName\":\"agreementType\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}}";
            DownloadListWithData downloadObj = new DownloadListWithData();
            Map<String, String> formParam = new HashMap<>();
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
            formParam.put("jsonData", defaultListDataPayload);

            HttpResponse downloadResponse = downloadObj.hitDownloadListWithData(formParam, ConfigureConstantFields.getListIdForEntity("contract templates"));
            String excelFilePath = "src/test";
            String excelFileName = "CTAllColumns.xlsx";
            boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, excelFilePath + "/" + excelFileName);

            if (fileDownloaded) {
                List<String> allColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, "Data", 4);
                List<String> allColumnNamesInLowerCase = new ArrayList<>();

                for(String columnName: allColumnNames) {
                    allColumnNamesInLowerCase.add(columnName.trim().toLowerCase());
                }

                if (!allColumnNamesInLowerCase.contains("functions")) {
                    csAssert.assertFalse(true, "Functions Column not found in Contract Template Downloaded Excel.");
                }

                if (!allColumnNamesInLowerCase.contains("services")) {
                    csAssert.assertFalse(true, "Services Column not found in Contract Template Downloaded Excel.");
                }

                //Delete Excel
                FileUtils.deleteFile(excelFilePath, excelFileName);
            } else {
                csAssert.assertFalse(true, "Couldn't Download ListData Excel for Contract Templates. " + additionalInfo);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90839. " + additionalInfo + ". " + e.getMessage());
        }
    }


    /*
    TC-C90914: Verify Function and Services field on listing page when MultiLingual On/Off
     */
    @Test(priority = 1)
    public void testC90914() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90914: Verify Function and Services field on listing page when MultiLingual On/Off");
            int clientId = new AdminHelper().getClientId();

            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            String originalValueOfMultiLingual = postgresObj.doSelect("select multilanguage_supported from client where id = " + clientId).get(0).get(0);
            originalValueOfMultiLingual = originalValueOfMultiLingual.equalsIgnoreCase("t") ? "true" : "false";

            String additionalInfo = "MultiLingual is On. ";
            postgresObj.updateDBEntry("update client set multilanguage_supported = true where id = " + clientId);

            validateFieldsInDefaultUserListMetadataResponse(csAssert, additionalInfo);
            validateFieldsInListDataResponse(csAssert, additionalInfo);
            validateFieldsInDownloadedExcel(csAssert, additionalInfo);

            additionalInfo = "MultiLingual is Off. ";
            postgresObj.updateDBEntry("update client set multilanguage_supported = false where id = " + clientId);

            validateFieldsInDefaultUserListMetadataResponse(csAssert, additionalInfo);
            validateFieldsInListDataResponse(csAssert, additionalInfo);
            validateFieldsInDownloadedExcel(csAssert, additionalInfo);

            postgresObj.updateDBEntry("update client set multilanguage_supported = " + originalValueOfMultiLingual + " where id = " + clientId);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90914: " + e.getMessage());
        }

        csAssert.assertAll();
    }
}