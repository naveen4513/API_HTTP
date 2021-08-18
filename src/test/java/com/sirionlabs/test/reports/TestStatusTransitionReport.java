package com.sirionlabs.test.reports;

import com.google.inject.internal.cglib.core.$KeyFactory;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.listingParam.CreateForm;
import com.sirionlabs.api.clientAdmin.report.ClientSetupAdminReportList;
import com.sirionlabs.api.clientAdmin.report.ReportRendererListConfigure;
import com.sirionlabs.api.clientAdmin.workflow.StatusUpdate;
import com.sirionlabs.api.clientSetup.reportRenderer.ReportRendererListJson;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.reportRenderer.DownloadGraphWithData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestStatusTransitionReport {

    private final static Logger logger = LoggerFactory.getLogger(TestStatusTransitionReport.class);
    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestReportStatusConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestReportStatusConfigFileName");
    }
    @DataProvider
    public Object[][] dataProviderForStatusTransitionReport() {
        List<Object[]> allTestData = new ArrayList<>();

        String entityName = "child obligations";
        String reportName = "Child Obligations - Status Transition Report";
        Integer reportId = 439;

        allTestData.add(new Object[]{entityName, reportName, reportId});

        entityName = "actions";
        reportName = "Actions - Status Transition Report";
        reportId = 440;

        allTestData.add(new Object[]{entityName, reportName, reportId});

        entityName = "invoices";
        reportName = "Invoice - Status Transition Report";
        reportId = 438;
        allTestData.add(new Object[]{entityName, reportName, reportId});

        entityName = "contract draft request";
        reportName = "Contract Draft Request- Status Transition Report";
        reportId = 446;
        allTestData.add(new Object[]{entityName, reportName, reportId});


        logger.info("Total Flows to Test : {}", allTestData.size());
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForTrackerReport() {
        List<Object[]> allDataToTest = new ArrayList<>();

       String entityName = "child obligations";
       String reportName = "Child Obligations - Tracker";
       int reportId = 13;
       allDataToTest.add(new Object[]{entityName, reportName, reportId});

        entityName = "Consumption";
        reportName = "Consumption - Tracker";
        reportId = 385;
        allDataToTest.add(new Object[]{entityName, reportName, reportId});

        entityName = "Work Order Requests";
        reportName = "Work Order Requests - Tracker";
        reportId = 275;
        allDataToTest.add(new Object[]{entityName, reportName, reportId});

        logger.info("Total Flows to Test : {}", allDataToTest.size());
        return allDataToTest.toArray(new Object[0][]);


    }

    @DataProvider(name = "filterData")
    public Object[][] filterDataDownload() {
        List<Object[]> allTestData = new ArrayList<>();
        Map<String, String> allConstantProperties= ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "filterdatadownload");
        String filterName = allConstantProperties.get("filtername");
        int filterId = Integer.parseInt(allConstantProperties.get("filterid"));
        String fieldName = allConstantProperties.get("fieldname");
        String entityName = allConstantProperties.get("filtername");
        String reportName = allConstantProperties.get("reportname");
        int reportId =Integer.parseInt(allConstantProperties.get("reportid"));
        String selected = allConstantProperties.get("selected");
        String excelFieldName = allConstantProperties.get("excelfieldname");  // always in capital later and same as filter name
        allTestData.add(new Object[]{filterName, filterId, fieldName, entityName, reportName, reportId, selected, excelFieldName});
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "reportMetaData")
    public Object[][] reportMetaData() {
        List<Object[]> allTestData = new ArrayList<>();
        Map<String, String> allConstantProperties= ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "reportmetadata");
        String reportName = allConstantProperties.get("reportname");
        String entityName = allConstantProperties.get("entityname");
        int reportId = Integer.parseInt(allConstantProperties.get("reportid"));
        int clientId = Integer.parseInt(allConstantProperties.get("clientid"));
        String workFlowStatus = allConstantProperties.get("workflowstatus");
        int workFlowStatusId = Integer.parseInt(allConstantProperties.get("workflowstatusid"));
        int languageId = Integer.parseInt(allConstantProperties.get("languageid"));
        int groupId =Integer.parseInt(allConstantProperties.get("groupid"));
        allTestData.add(new Object[]{reportName, entityName, reportId, clientId, workFlowStatus, workFlowStatusId, languageId, groupId});
        return allTestData.toArray(new Object[0][]);
    }

 /* TC-C46348 Default value of the the newly added filters should be enabled-Child Obligation
    TC-C46335 & TC-C46304 Default value of the the newly added filters should be enabled-WOR(Part-1)
    TC-C46336 & TC-C46338 Default value of the the newly added filters should be enabled-Consumption
 */
    @Test(dataProvider ="dataProviderForTrackerReport")
    public void testC46348(String entityName,String reportName,int reportId)
    {
        CustomAssert csAssert = new CustomAssert();

        int clientId= new AdminHelper().getClientId();
        try {
            logger.info("Starting Test: Start Test for Entity {}, Report Name [{}] having Report Id {}", entityName, reportName, reportId);
            logger.info("Hitting ReportRendererListJson API for Report Id {}", reportId);
            ReportRendererListJson ReportRenderListJsonObj = new ReportRendererListJson();
            String reportRenderListResponse = ReportRenderListJsonObj.getListJsonResponse(reportId, clientId);

            if (ParseJsonResponse.validJsonResponse(reportRenderListResponse)) {
                JSONObject jsonObj = new JSONObject(reportRenderListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("columns");
                Boolean flag=false;
                Boolean flag1=false;
                if (jsonArr.length() > 0) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject Obj1 = jsonArr.getJSONObject(i);
                        if (Obj1.getString("queryName").equalsIgnoreCase("datemodified")){
                            logger.info("Last Modified Date value is present in column List");
                            flag = true;
                        }
                      if (Obj1.getString("queryName").equalsIgnoreCase("lastmodifiedby")) {
                            logger.info("Last Modified By value is present in column List");
                            flag1 = true;
                        }

                    }
                    if (!(flag == true && flag1==true)) {
                        csAssert.assertTrue(false, "Either Last Modified Date/Last Modified By or both Values are not present in column List for " + reportName + " having report Id " + reportName);
                    }
                }
                jsonArr = jsonObj.getJSONArray("ecxelColumns");
                 flag=false;
                 flag1=false;
                if (jsonArr.length() > 0) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject Obj1 = jsonArr.getJSONObject(i);
                        if (Obj1.getString("queryName").equalsIgnoreCase("datemodified")) {
                            logger.info("Last Modified Date value is present in excelColumn List");
                            flag = true;
                        }
                        if (Obj1.getString("queryName").equalsIgnoreCase("lastmodifiedby")) {
                            logger.info("Last Modified By value is present in excelColumn List");
                            flag1 = true;
                        }

                    }
                    if (!(flag == true && flag1 == true)) {
                        csAssert.assertTrue(false, "Either Last Modified Date/Last Modified By Values or both Values are not present in Excel column List for " + reportName + " having report Id " + reportName);
                    }
                }
                jsonArr = jsonObj.getJSONArray("filterMetadatas");
                flag=false;
                flag1=false;
                if (jsonArr.length() > 0) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject Obj1 = jsonArr.getJSONObject(i);
                        if (Obj1.getString("queryName").equalsIgnoreCase("modifiedDate")) {
                            logger.info("Last Modified Date value is present in filterMetadatas List");
                            flag = true;
                        }
                        if (Obj1.getString("queryName").equalsIgnoreCase("lastmodifiedby")) {
                            logger.info("Last Modified By value is present in filterMetadatas List");
                            flag1 = true;
                        }
                    }
                    if (!(flag == true && flag1 == true)) {
                        csAssert.assertTrue(false, "Either Last Modified Date/Last Modified By Values or both Values are not present in filterMetadatas List for " + reportName + " having report Id " + reportName);
                    }
                }
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }

        catch(Exception e)
            {
                csAssert.assertTrue(false, "Exception while Validating TC- C46348. " + e.getMessage());
            }
        csAssert.assertAll();

    }

    /* TC- C46335 & TC-C46304-Default value of the the newly added filters should be enabled.
      Part-2 */
    @Test
    public void testC46335()
    {
        CustomAssert csAssert = new CustomAssert();
        String entityName = "Work Order Requests";
        String reportName = "Work Order Requests - Tracker";
        int reportId = 275;
        int clientId= new AdminHelper().getClientId();
        try {
            logger.info("Starting Test: Start Test for Entity {}, Report Name [{}] having Report Id {}", entityName, reportName, reportId);
            logger.info("Hitting ReportRendererListJson API for Report Id {}", reportId);
            ReportRendererListJson ReportRenderListJsonObj = new ReportRendererListJson();
            String reportRenderListResponse = ReportRenderListJsonObj.getListJsonResponse(reportId, clientId);
            logger.info("reportRenderListResponse for Entity {}, Report Name [{}] having report Id {}",entityName,reportName,reportId +"is " +reportRenderListResponse);

            if (ParseJsonResponse.validJsonResponse(reportRenderListResponse)) {
                JSONObject jsonObj = new JSONObject(reportRenderListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("columns");
                Boolean createdon=false;
                Boolean createdby=false;
                if (jsonArr.length() > 0) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject Obj1 = jsonArr.getJSONObject(i);
                        if (Obj1.getString("queryName").equalsIgnoreCase("datecreated")){
                            logger.info("CREATED ON value is present in column List");
                            createdon = true;
                        }
                        if (Obj1.getString("queryName").equalsIgnoreCase("createdby")) {
                            logger.info("CREATED By value is present in column List");
                            createdby = true;
                        }

                    }
                    if (!(createdon==true && createdby==true)) {
                        csAssert.assertTrue(false, "Either Created On/Created By or both values are not present in column List for " + reportName + " having report Id " + reportName);
                    }
                }
                jsonArr = jsonObj.getJSONArray("ecxelColumns");
                createdon=false;
                createdby=false;
                if (jsonArr.length() > 0) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject Obj1 = jsonArr.getJSONObject(i);
                        if (Obj1.getString("queryName").equalsIgnoreCase("datecreated")){
                            logger.info("CREATED ON value is present in excelColumn List");
                            createdon = true;
                        }
                        if (Obj1.getString("queryName").equalsIgnoreCase("createdby")) {
                            logger.info("CREATED By value is present in excelColumn List");
                            createdby = true;
                        }
                    }
                    if (!(createdon==true && createdby==true)) {
                        csAssert.assertTrue(false, "Either Created On/Created By value or both are not present in Excel Column List for " + reportName + " having report Id " + reportName);
                    }
                }
                jsonArr = jsonObj.getJSONArray("filterMetadatas");
                createdon=false;
                createdby=false;
                if (jsonArr.length() > 0) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject Obj1 = jsonArr.getJSONObject(i);
                        if (Obj1.getString("queryName").equalsIgnoreCase("createdDate")){
                            logger.info("CREATED ON value is present in filterMetadatas List");
                            createdon = true;
                        }
                        if (Obj1.getString("queryName").equalsIgnoreCase("createdby")) {
                            logger.info("CREATED By value is present in filterMetadatas List");
                            createdby = true;
                        }
                    }
                    if (!(createdon==true && createdby==true)) {
                        csAssert.assertTrue(false, "Either Created On/Created By  value or both are not present in filterMetadatas List for " + reportName + " having report Id " + reportName);
                    }
                }
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch(Exception e)
        {
            csAssert.assertTrue(false, "Exception while Validating TC- C46335. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(dataProvider = "reportMetaData")
    public void testCaseStatusTransitionReport(String reportName, String entityName, int reportId, int clientId, String workFlowStatus, int workFlowStatusId, int languageId, int groupId) {
        CustomAssert csAssert = new CustomAssert();

        // Verify the report "Status Transition" is present at client setup admin under CDR entity
        ClientSetupAdminReportList clientSetupAdminReportList = new ClientSetupAdminReportList();
        boolean results = ClientSetupAdminReportList.isReportPresentInClientSetupAdmin(entityName, reportName,csAssert);

        csAssert.assertTrue(results, " report" + reportName + "is present at client setup admin under CDR entity");

        // Verify that Report is available at Supplier Access, Supplier Access At VH level, and URG.
        supplierAccessAndVHLevelReportTest(clientId, reportName, csAssert);

        //Verify behavior of downloaded report when Status for which "Hide in Breadcrumb" flag is true form workflow and that status is configured from report content control
        //hideInBreadcrumbFlag(entityName, reportName, reportId, workFlowStatus, workFlowStatusId, csAssert);

        //Verify that report name, excel headers and disclaimer of the report has Internationalization support.
       // renamedFieldInternationalizationSupport(csAssert, languageId, groupId, clientId, entityName, reportId, reportName);

        csAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForStatusTransitionReport")
    public void testStatusTransitionReport(String entityName, String reportName, Integer reportId) {
        CustomAssert csAssert = new CustomAssert();


        try {
            logger.info("Starting Test: Validating Status Transition Report for Entity {}, Report Name [{}] having Id {}", entityName, reportName, reportId);
            logger.info("Hitting DefaultUserListMetadata API for Report Id {}", reportId);
            ReportsDefaultUserListMetadataHelper defaultUserListObj = new ReportsDefaultUserListMetadataHelper();
            String defaultUserListResponse = defaultUserListObj.hitDefaultUserListMetadataAPIForReportId(reportId);

            if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("columns");

//                if (jsonArr.length() != 0) {
//                    csAssert.assertTrue(false, "Expected Length of Columns: 0 and Actual Length of Columns Array: " + jsonArr.length() +
//                            " in DefaultUserListMetadata API Response for Report [" + reportName + "] having Id " + reportId);
//                }

                jsonArr = jsonObj.getJSONArray("ecxelColumns");

                    if (jsonArr.length() != 0) {
                        csAssert.assertTrue(false, "Expected Length of Excel Columns: 0 and Actual Length of Excel Columns Array: " + jsonArr.length() +
                            " in DefaultUserListMetadata API Response for Report [" + reportName + "] having Id " + reportId);
                }

                if (!jsonObj.getJSONObject("reportMetadataJson").getBoolean("isDownload")) {
                    csAssert.assertTrue(false, "Expected isDownload Property: True and Actual isDownload Property: False " +
                            " in DefaultUserListMetadata API Response for Report [" + reportName + "] having Id " + reportId);
                }

                if (!jsonObj.getJSONObject("properties").getString("maxEntitiesForStatusTransition").equalsIgnoreCase("2000")) {
                    csAssert.assertTrue(false, "Expected maxEntitiesForStatusTransition Property Value: 2000 and " +
                            "Actual maxEntitiesForStatusTransition Property Value: " + jsonObj.getJSONObject("properties").getString("maxEntitiesForStatusTransition") +
                            " in DefaultUserListMetadata API Response for Report [" + reportName + "] having Id " + reportId);
                }

                validateDownloadedExcel(entityName, reportName, reportId, csAssert);
            } else {
                csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Report [" + reportName + "] having Id " + reportId +
                        " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating End User Verification for Status Transition Report. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    @Test(dataProvider = "filterData")
    public void downloadExcelAppliedFilter(String filterName, int filterId, String fieldName, String entityName, String reportName, int reportId, String selected, String excelFieldName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            DownloadGraphWithData downloadObj = new DownloadGraphWithData();
            Map<String, String> formParam = new HashMap<>();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            String payload ="{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{\""+filterId+"\":{\"filterId\":\""+filterId+"\",\"filterName\":\""+filterName+"\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":"+selected+"}}}}}\n";
            formParam.put("jsonData", payload);
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            HttpResponse response = downloadObj.hitDownloadGraphWithData(formParam, reportId);

            String outputFilePath = "src/test/output";
            String outputFileName = reportName + ".xlsx";

            FileUtils fileUtil = new FileUtils();
            Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);

            if (!fileDownloaded) {
                throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
            }
            List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);
            Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, outputFileName, "Data");
            if ((noOfRows - 5) == 0) {
                throw new SkipException("No Data Found in Data Sheet");
            }
            List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, outputFileName, "Data", 5,
                    noOfRows.intValue() - 5);
            if (allRecordsData == null)
            {
                throw new SkipException("Couldn't get All Records Data from Data Sheet.");
            }

            int columnNoOfPerformanceStatus = allHeadersInExcelDataSheet.indexOf(excelFieldName);
            for (int i = 0; i < allRecordsData.size(); i++) {
                if (!allRecordsData.get(i).get(columnNoOfPerformanceStatus).equalsIgnoreCase(fieldName)) {
                    csAssert.assertTrue(false, "filter data[" + allRecordsData.get(i).get(columnNoOfPerformanceStatus) + "] not found ");
                }
            }

            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Verifying Filter Data In Download Report " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateDownloadedExcel(String entityName, String reportName, Integer reportId, CustomAssert csAssert) {
        try {
            //Download Report
            DownloadGraphWithData downloadObj = new DownloadGraphWithData();
            Map<String, String> formParam = new HashMap<>();

            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            formParam.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}");
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            HttpResponse response = downloadObj.hitDownloadGraphWithData(formParam, reportId);

            String outputFilePath = "src/test/output";
            String outputFileName = reportName + ".xlsx";

            FileUtils fileUtil = new FileUtils();
            Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);

            if (!fileDownloaded) {
                throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
            }

            String reportConfigureResponse = ReportRendererListConfigure.getReportListConfigureResponse(reportId);

            if (ParseJsonResponse.validJsonResponse(reportConfigureResponse)) {
                //Validate All Excel Columns
                List<String> allEnabledExcelColumnsList = ReportRendererListConfigure.getAllEnabledExcelColumnsList(reportConfigureResponse);
                if (allEnabledExcelColumnsList == null || allEnabledExcelColumnsList.isEmpty()) {
                    throw new SkipException("Couldn't get All Enabled Excel Columns List for Report [" + reportName + "] having Id " + reportId);
                }
//                if (entityName.equalsIgnoreCase("invoices")) {
//                    if (allEnabledExcelColumnsList.contains("REGIONS")) {
//                        int indexOfRegionColumn = allEnabledExcelColumnsList.indexOf("REGIONS");
//                        allEnabledExcelColumnsList.remove(indexOfRegionColumn);
//                        allEnabledExcelColumnsList.add("REGION");
//                    }
//                }

                List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);

                if (allHeadersInExcelDataSheet == null || allHeadersInExcelDataSheet.isEmpty()) {
                    throw new SkipException("Couldn't get All Headers in Data Sheet of Excel.");
                }

                if (allEnabledExcelColumnsList.size() != allHeadersInExcelDataSheet.size()) {
                    csAssert.assertTrue(false, "No of Columns do not match in Data Sheet and Configured in Admin. No of Columns in Client Admin: " + allEnabledExcelColumnsList.size() + " and No of Columns in Excel: " + allHeadersInExcelDataSheet.size());
                    for (String header : allEnabledExcelColumnsList) {
                        if (!allHeadersInExcelDataSheet.contains(header)) {
                            csAssert.assertTrue(false, "Column Name: " + header + " not found in Data Sheet.");
                        }
                    }
                } else {
                    for (String header : allEnabledExcelColumnsList) {
                        if (!allHeadersInExcelDataSheet.contains(header)) {
                            csAssert.assertTrue(false, "Column Name: " + header + " not found in Data Sheet.");
                        }
                    }
                }

                //Validate all Filters
                List<String> allEnabledFilterColumns = ReportRendererListConfigure.getAllEnabledFiltersList(reportConfigureResponse);
                if (allEnabledFilterColumns == null || allEnabledFilterColumns.isEmpty()) {
                    throw new SkipException("Couldn't get All Enabled Filters List for Report [" + reportName + "] having Id " + reportId);
                }

//                if (entityName.equalsIgnoreCase("invoices")) {
//                    if (allEnabledFilterColumns.contains("Regions")) {
//                        int indexOfRegionColumn = allEnabledFilterColumns.indexOf("Regions");
//                        allEnabledFilterColumns.remove(indexOfRegionColumn);
//                        allEnabledFilterColumns.add("Region");
//                    }
//                }

                List<String> allHeadersInFilterSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Filter", 4);
                List<String> allEnabledFilterColumnsInUpperCase = new ArrayList<>();
                for (String list : allEnabledFilterColumns) {
                    allEnabledFilterColumnsInUpperCase.add(list.toUpperCase());

                }
                if (allHeadersInFilterSheet == null || allHeadersInFilterSheet.isEmpty()) {
                    throw new SkipException("Couldn't get All Headers in Filter Sheet of Excel.");
                }

                if (allEnabledFilterColumns.size() != allHeadersInFilterSheet.size()) {
                    csAssert.assertTrue(false, "No of Filter Columns do not match in Filter Sheet and Configured in Admin. No of Columns in Client Admin: " +
                            allEnabledFilterColumns.size() + " and No of Filter Columns in Excel: " + allHeadersInFilterSheet.size());
                    for (String header : allHeadersInFilterSheet) {
                        if (!allEnabledFilterColumnsInUpperCase.contains(header.toUpperCase())) {
                            csAssert.assertTrue(false, "Filter Column Name: " + header + " not found in Filter Sheet.");
                        }
                    }
                } else {
                    for (String header : allHeadersInFilterSheet) {
                        if (!allEnabledFilterColumnsInUpperCase.contains(header.toUpperCase())) {
                            csAssert.assertTrue(false, "Filter Column Name: " + header + " not found in Filter Sheet.");
                        }
                    }
                }
                //Verify report availability in report view section at client admin under CDR entity
                if (entityName.equalsIgnoreCase("contract draft request")) {
                    listOfFilterAndExcelColumns(reportConfigureResponse, csAssert);
                }

                //Verify Data as per the Status selected in Report Content Control.
                String createFormResponse = CreateForm.getCreateFormResponse(reportId);

                Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, outputFileName, "Data");
                List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, outputFileName, "Data", 5,
                        noOfRows.intValue() - 5);

                if (allRecordsData == null) {
                    throw new SkipException("Couldn't get All Records Data from Data Sheet.");
                }

                if (entityName.equalsIgnoreCase("child obligations")) {
                    List<String> allSelectedPerformanceStatus = CreateForm.getAllSelectedPerformanceStatus(createFormResponse, reportId);

                    if (allSelectedPerformanceStatus == null) {
                        throw new SkipException("Couldn't get All Selected Performance Status for Report [" + reportName + "] having Id " + reportId);
                    }

                    int columnNoOfPerformanceStatus = allHeadersInExcelDataSheet.indexOf("PERFORMANCE STATUS");

                    for (int i = 0; i < allRecordsData.size(); i++) {
                        String actualPerformanceStatus = allRecordsData.get(i).get(columnNoOfPerformanceStatus);

                        if (!allSelectedPerformanceStatus.contains(actualPerformanceStatus)) {
                            csAssert.assertTrue(false, "Performance Status for Record #" + (i + 1) + ": [" + actualPerformanceStatus +
                                    "] not found in Selected Performance Status List.");
                        }
                    }
                }

                List<String> allSelectedStatus = CreateForm.getAllSelectedStatus(createFormResponse, reportId);

                if (allSelectedStatus == null) {
                    throw new SkipException("Couldn't get All Selected Status for Report [" + reportName + "] having Id " + reportId);
                }

                int columnNoOfStatus = allHeadersInExcelDataSheet.indexOf("CURRENT STATUS");

                for (int i = 0; i < allRecordsData.size(); i++) {
                    String actualStatus = allRecordsData.get(i).get(columnNoOfStatus);

                    if (!allSelectedStatus.contains(actualStatus)) {
                        csAssert.assertTrue(false, "Status for Record #" + (i + 1) + ": [" + actualStatus +
                                "] not found in Selected Status List.");
                    }
                }

                FileUtils.deleteFile(outputFilePath + "/" + outputFileName);

                validateMandatoryColumns(entityName, reportName, reportId, reportConfigureResponse, csAssert);
            } else {
                csAssert.assertTrue(false, "Report List Configure API Response for Report [" + reportName + "] having Id " + reportId +
                        " is an Invalid JSON");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Downloaded Excel of Report [" + reportName + "] having Id " + reportId);
        }
    }

    private void validateMandatoryColumns(String entityName, String reportName, Integer reportId, String reportConfigureResponse, CustomAssert csAssert) {
        try {
            logger.info("Validating Mandatory Columns for Entity {}, Report: [{}] having Id {}", entityName, reportName, reportId);
            String[] mandatoryColumnQueryNames = {"time_of_action", "status", "completed_by", "id"};

            for (String queryName : mandatoryColumnQueryNames) {
                String displayValue = ReportRendererListConfigure.getExcelColumnPropertyValueFromQueryName(reportConfigureResponse, queryName, "displayFormat");

                if (displayValue == null) {
                    csAssert.assertTrue(false, "Couldn't get DisplayValue for QueryName " + queryName);
                    continue;
                }

                if (!displayValue.contains("mandatoryForExcel\":true") && !displayValue.contains("isId\":true")) {
                    csAssert.assertTrue(false, "Column having Query Name: " + queryName + " is Expected Mandatory but it is not.");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Mandatory Columns for Report [" + reportName + "] having Id " + reportId);
        }
    }

    private void listOfFilterAndExcelColumns(String reportConfigureResponse, CustomAssert csAssert) {

        try {
            //create a list for mandatory field present in CDR Status Transition Report columns

            List<String> allExcelColumnsList = new ArrayList<>();
            List<String> allFilterColumns = new ArrayList<>();
            //excel columns list
            List<String> excelColumnsList = new ArrayList<>(Arrays.asList("ID", "STATUS", "STATE", "TITLE", "PRIORITY", "PAPER TYPE", "AGREEMENT TYPE", "REGIONS", "FUNCTIONS", "SERVICES", "START DATE", "COMPLETION DATE"));
            // filter list
            List<String> filterColumns = new ArrayList<>(Arrays.asList("STATUS", "AGREEMENT TYPE", "PAPER TYPE", "FUNCTIONS", "SERVICES", "START DATE", "COMPLETION DATE"));
            JSONArray jsonArray = new JSONObject(reportConfigureResponse).getJSONArray("ecxelColumns");
            for (int i = 0; i < jsonArray.length(); i++) {
                allExcelColumnsList.add(jsonArray.getJSONObject(i).get("name").toString().toUpperCase());
            }
            jsonArray = new JSONObject(reportConfigureResponse).getJSONArray("filterMetadatas");
            for (int i = 0; i < jsonArray.length(); i++) {
                allFilterColumns.add(jsonArray.getJSONObject(i).get("name").toString().toUpperCase());
            }
            for (String excelColumns : excelColumnsList) {
                if (!allExcelColumnsList.contains(excelColumns)) {
                    csAssert.assertTrue(false, " Columns List [" + excelColumns + "] is not present");
                }
            }

            for (int i = 0; i < filterColumns.size(); i++) {
                if (!allFilterColumns.contains(filterColumns.get(i))) {
                    csAssert.assertTrue(false, "filter List[" + filterColumns.get(i) + "] is not present");
                }
            }
            csAssert.assertAll();
        } catch (Exception e) {
            logger.info("Exception While Verifying List Of Filter And Excel Columns{}", e.toString());
        }
    }

    private void renamedFieldInternationalizationSupport(CustomAssert csAssert, int languageId, int groupId, int clientId, String entityName, int reportId, String reportName) {
        try {
            String lastLoggedInUserName = Check.lastLoggedInUserName;
            String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
            AdminHelper adminHelper = new AdminHelper();
            adminHelper.loginWithClientAdminUser();
            FieldRenaming fieldRenaming = new FieldRenaming();
            String filedResponse = fieldRenaming.hitFieldRenamingUpdate(languageId, groupId);//1000,1724
            String payloadForFieldRenamingUpdate = filedResponse;
            List<String> fieldNameUpdate = new ArrayList<>();
            List<String> fieldName = new ArrayList<>(Arrays.asList("Current Status", "Completed By", "Time Of Action", "Status"));
            for (int i = 0; i < fieldName.size(); i++) {
                String clientFieldName = fieldRenaming.getClientFieldNameFromName(payloadForFieldRenamingUpdate, fieldName.get(i));
                String newClientFieldName = clientFieldName + "Обновить";
                fieldNameUpdate.add(newClientFieldName);
                payloadForFieldRenamingUpdate = payloadForFieldRenamingUpdate.replace("\"clientFieldName\":\"" + clientFieldName + "\"",
                        "\"clientFieldName\":\"" + newClientFieldName + "\"");
            }
            FieldRenaming fieldRenamingObj = new FieldRenaming();
            String fieldUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);
            if (fieldUpdateResponse.contains("true")) {
                logger.info("update successfully");
            }
            Check check = new Check();
            check.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
            UpdateAccount updateAccountObj = new UpdateAccount();
            int currentLanguageId = updateAccountObj.getCurrentLanguageIdForUser(lastLoggedInUserName, clientId);
            if (currentLanguageId == -1) {
                throw new SkipException("Couldn't get Current Language Id for User " + lastLoggedInUserName);
            }
            updateAccountObj.updateUserLanguage(lastLoggedInUserName, clientId, languageId);
            //Download Report
            DownloadGraphWithData downloadObj = new DownloadGraphWithData();
            Map<String, String> formParam = new HashMap<>();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            formParam.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}");
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            HttpResponse response1 = downloadObj.hitDownloadGraphWithData(formParam, reportId);
            String outputFilePath = "src/test/output";
            String outputFileName = "contract draft request" + ".xlsx";
            FileUtils fileUtil = new FileUtils();
            Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response1, outputFilePath + "/" + outputFileName);

            if (!fileDownloaded) {
                throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
            }

            List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);

            for (int i = 0; i < fieldNameUpdate.size(); i++) {
                if (!allHeadersInExcelDataSheet.contains(fieldNameUpdate.get(i).toUpperCase())) {
                    csAssert.assertTrue(false, "filed name not found");
                }
            }
            updateAccountObj.updateUserLanguage(lastLoggedInUserName, clientId, currentLanguageId);
            adminHelper.loginWithClientAdminUser();
            fieldRenamingObj.hitFieldUpdate(filedResponse);
            check.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception While Verifying Rename Field Internationalization Support " + e.getMessage());
        }
    }

    private void supplierAccessAndVHLevelReportTest(int clientId, String reportName, CustomAssert csAssert) {
        AdminHelper adminHelper = new AdminHelper();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
        Check check = new Check();
        try {
            Set<String> reportGrantedListForUser = adminHelper.getReportsGrantedListForUser(ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserName"), clientId);
            int reportIdForReportName = adminHelper.getReportIdFromReportName(reportName);
            boolean updateResult = false;
            boolean results = reportGrantedListForUser.remove(String.valueOf(reportIdForReportName));
            if (results) {
                updateResult = adminHelper.updateReportGrantedListForUser(ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserName"), clientId, reportGrantedListForUser.toString().replaceAll("\\[", "{").replaceAll("\\]", "}"));
            }

            if (updateResult) {
                check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserName"), ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserPassword"));
                ReportsListHelper reportsListHelper = new ReportsListHelper();
                List<Map<String, String>> allReportListOfSelectedEntity = reportsListHelper.getAllReportsOfEntity("Contract Draft Request");
                if (!allReportListOfSelectedEntity.isEmpty()) {
                    for (Map<String, String> allReport : allReportListOfSelectedEntity) {
                        if (allReport.get("name").equalsIgnoreCase(reportName)) {
                            csAssert.assertTrue(false, "Report Is Found" + reportName);
                            break;
                        }
                    }
                }
            }
            check.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
            reportGrantedListForUser.add(String.valueOf(reportIdForReportName));
            adminHelper.updateReportGrantedListForUser(ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserName"), clientId, reportGrantedListForUser.toString().replaceAll("\\[", "{").replaceAll("\\]", "}"));
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Verifying Supplier Access Test " + e.getMessage());
        }
    }

    private void hideInBreadcrumbFlag(String entityName, String reportName, int reportId, String workFlowStatus, int workFlowStatusID, CustomAssert csAssert) {

        try {
            HashMap<String, String> formData = new HashMap<>();
            formData.put("name", workFlowStatus);
            formData.put("description", workFlowStatus);
            formData.put("id", String.valueOf(workFlowStatusID));
            formData.put("entityType.id", String.valueOf(ConfigureConstantFields.getEntityIdByName(entityName)));
            formData.put("entityType.name", entityName);
            formData.put("colorid", "4");
            formData.put("colorCode", "");
            formData.put("_excludeFromFilter", "on");
            formData.put("hideFromBreadcrumb", "true");
            formData.put("_hideFromBreadcrumb", "on");
            formData.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
            formData.put("workFlowStatusType.id", "118");

            boolean results = StatusUpdate.updateWorkflowStatus(formData);
            if (results) {
                //Download Report
                DownloadGraphWithData downloadObj = new DownloadGraphWithData();
                Map<String, String> formParam = new HashMap<>();

                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                formParam.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}");
                formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                HttpResponse response = downloadObj.hitDownloadGraphWithData(formParam, reportId);

                String outputFilePath = "src/test/output";
                String outputFileName = reportName + ".xlsx";

                FileUtils fileUtil = new FileUtils();
                Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);

                if (!fileDownloaded) {
                    throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
                }
                List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);
                Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, outputFileName, "Data");
                if ((noOfRows - 5) == 0) {
                    throw new SkipException("No Data Found in Data Sheet");
                }
                List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, outputFileName, "Data", 5,
                        noOfRows.intValue() - 5);
                if (allRecordsData == null) {
                    throw new SkipException("Couldn't get All Records Data from Data Sheet.");
                }
                int columnNoOfPerformanceStatus = allHeadersInExcelDataSheet.indexOf("STATUS");
                for (int i = 0; i < allRecordsData.size(); i++) {
                    if (allRecordsData.get(i).get(columnNoOfPerformanceStatus).equalsIgnoreCase(workFlowStatus)) {
                        csAssert.assertTrue(false, "Status [" + allRecordsData.get(i).get(columnNoOfPerformanceStatus) + "] found");
                    }
                }
                FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
                formData.remove("hideFromBreadcrumb", "true");
                StatusUpdate.updateWorkflowStatus(formData);
            } else {
                csAssert.assertTrue(false, "Work Flow Status Is Not Hide");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Verifying Hide In Breadcrumb Flag " + e.getMessage());
        }
    }
}
