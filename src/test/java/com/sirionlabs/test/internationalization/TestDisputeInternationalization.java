
package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestDisputeInternationalization extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDisputeInternationalization.class);
    private Integer languageId = 1008;
    static String outputFilePath;
    static String outputFileFormatForDownloadListWithData;
    public static Integer entityTypeId;
    private Boolean userSettingsUpdated = false;
    private Map<String, String> userSettingsMap = new LinkedHashMap<>();
    private UpdateAccount updateObj = new UpdateAccount();
    public static String expectedPostFix = "    TEST RC36";


    @BeforeClass
    public void beforeClass() throws InterruptedException {
        fieldLabels fieldlabels = new fieldLabels();
        fieldlabels.FieldLabelsTest();

      try {
            //Change User Language
            outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
            outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");
            userSettingsMap.put("firstName", "Naveen");
            userSettingsMap.put("lastName", "User");
            userSettingsMap.put("contactNo", "8588832952");
            userSettingsMap.put("questionId", "1");
            userSettingsMap.put("answer", "");
            userSettingsMap.put("language.id", languageId.toString());
            userSettingsMap.put("timeZone.id", "8");
            userSettingsMap.put("tierId", "0");
            userSettingsMap.put("sessionTierId", "");
            userSettingsMap.put("_showContractDocumentOnShowpage", "on");
            userSettingsMap.put("_showContractDocumentOnEditpage", "on");
            userSettingsMap.put("fontSize", "1");
            userSettingsMap.put("defaultHomePage", "");
            userSettingsMap.put("dynamicMetadata", "");
            userSettingsMap.put("id", "1201");
            userSettingsMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));

            Integer updateStatusCode = updateObj.hitUpdateAccount(userSettingsMap);

            if (updateStatusCode == 302) {
                userSettingsUpdated = true;
                logger.info("User Account Settings updated for the user [" + userSettingsMap.get("firstName") + userSettingsMap.get("lastName") + "]");
            }
        } catch (Exception e) {
            logger.error("Exception while Setting Data for Internationalization.");
        }
    }

    @Test
    public void testC63039() throws ConfigurationException {
        CustomAssert csAssert = new CustomAssert();


       for (String entityName : ConfigureConstantFields.entityTypeIds() ) {

         if (!entityName.toLowerCase().contains("references") && !entityName.toLowerCase().contains("msa") && !entityName.toLowerCase().contains("psa")
                && !entityName.toLowerCase().contains("sow") && !entityName.toLowerCase().contains("other") && !entityName.toLowerCase().contains("ola")
                && !entityName.toLowerCase().contains("work order") && !entityName.toLowerCase().contains("services") && !entityName.toLowerCase().contains("client")
                 && !entityName.toLowerCase().contains("purchase orders") && !entityName.toLowerCase().contains("invoice line item")) {

                try {
                    logger.info("Starting Test TC-C63039: " + entityName + "  Internationalization: Verify all Fields on Show Page, Create Page and Edit Page.");

                    if (!userSettingsUpdated) {
                        throw new SkipException("Couldn't Update Language Settings for the End User.");
                    }

                    String listDataResponse = ListDataHelper.getListDataResponse(entityName);

                    if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                        int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                        JSONObject jsonObj = new JSONObject(listDataResponse);
                        JSONArray jsonArr = jsonObj.getJSONArray("data");

                        String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                        int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                        FieldLabelsOnShowPage showObj = new FieldLabelsOnShowPage();
                        showObj.verifyFieldLabelsOnShowPage(entityName, recordId, csAssert);

                        FieldLabelsOnCreatePage createObj = new FieldLabelsOnCreatePage();
                        createObj.verifyFieldLabelsOnCreatePage(entityName,recordId, csAssert);

                        OptionsLabelsOnCreatePage createPageOptionObj = new OptionsLabelsOnCreatePage();
                        createPageOptionObj.verifyOptionsLabelsOnCreatePage(entityName,recordId,csAssert);

                        OptionsLabelsOnEditPage editPageOptionObj = new OptionsLabelsOnEditPage();
                        editPageOptionObj.verifyOptionsLabelsOnEditPage(entityName,recordId,csAssert);

                        OptionsLabelsOnShowPage showPageOptionObj = new OptionsLabelsOnShowPage();
                        showPageOptionObj.verifyOptionsLabelsOnShowPage(entityName,recordId,csAssert);

                        FieldLabelsOnEditPage editObj = new FieldLabelsOnEditPage();
                        editObj.verifyFieldLabelsOnEditPage(entityName, recordId, csAssert);

                    }

                        FieldLabelsOnReports reportObj = new FieldLabelsOnReports();
                        reportObj.verifyFieldLabelsOnReports(entityName, csAssert);

                        FieldLabelsOnListPage listObj = new FieldLabelsOnListPage();
                        listObj.verifyFieldLabelsOnListPage(entityName, csAssert);

                        FieldLabelsOnMetadataSearchPage metadataSearchPageObj = new FieldLabelsOnMetadataSearchPage();
                        metadataSearchPageObj.verifyFieldLabelsOnMetadataSearchPage(entityName,csAssert);

                          FieldLabelSisenseDashboard sisenseDashboardObj = new FieldLabelSisenseDashboard();
                          sisenseDashboardObj.testfieldRenaming(entityName, csAssert);


                } catch (SkipException e) {
                    logger.error("skip exception: " + e.getMessage());
                    //throw new SkipException(e.getMessage());
                    continue;
                } catch (Exception e) {
                    logger.error("exception error: " + e.getMessage());
                    csAssert.assertTrue(false, "Exception while Validating Test TC-C63039. " + e.getMessage());
                    continue;
                }
            }

        }

        FieldLabelsOnShowPageTabs  showTabsObj = new FieldLabelsOnShowPageTabs();
        showTabsObj.verifyFieldLabelsOnShowPageTabs(csAssert);

        FieldLabelsOnBulkCreateTemplate bulkCreateObj = new FieldLabelsOnBulkCreateTemplate();
        bulkCreateObj.verifyFieldLabelsOnBulkCreateTemplate("contracts",133732,csAssert);

        FieldLabelsOnBulkUpdateTemplate bulkUpdateObj = new FieldLabelsOnBulkUpdateTemplate();
        bulkUpdateObj.verifyFieldLabelsOnBulkUpdateTemplate(1001,csAssert);

        FieldLabelDashboard dashboardObj = new FieldLabelDashboard();
        dashboardObj.testDashboardGlobalFilters();

        FieldLabelsMyProfilePage myProfileObj = new FieldLabelsMyProfilePage();
        myProfileObj.getMyAccountInfoFieldLabels(csAssert);
        myProfileObj.getMyAccountGBInfoFieldLabels(csAssert);

        csAssert.assertAll();
    }

    @AfterClass
    public void afterClass() {
        userSettingsMap.put("language.id", "1");

        Integer updateStatusCode = updateObj.hitUpdateAccount(userSettingsMap);

        if (updateStatusCode == 302) {
            logger.info("User Account Settings Reverted for the user [" + userSettingsMap.get("firstName") + userSettingsMap.get("lastName") + "]");
        }
    }

}