package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.calendar.CalendarData;
import com.sirionlabs.api.calendar.CalendarExportData;
import com.sirionlabs.api.calendar.CalendarFilterData;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.listingParam.CreateForm;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.metadataSearch.MetadataSearchDownload;
import com.sirionlabs.api.metadataSearch.Search;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.todo.TodoPendingApproval;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Supplier;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

public class TestMultiSupplierContract extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestMultiSupplierContract.class);

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFileName;
    private int contractId = -1;
    private int calendarContractId = -1;
    private String[] parentSupplierIdArr;
    private String[] calendarContractParentSupplierIdArr;
    private int calendarMonth = 4;
    private int calendarYear = 2018;
    private Map<String, String> parentSuppliersMap = new HashMap<>();
    private Map<String, String> calendarContractParentSuppliersMap = new HashMap<>();
    private List<Integer> newlyCreatedContractIds = new ArrayList<>();

    private int[] contractReportIds = {49, 50, 222, 270, 1000};

    private CalendarData calendarDataObj = new CalendarData();
    private ReportsDefaultUserListMetadataHelper metadataHelperObj = new ReportsDefaultUserListMetadataHelper();
    private ReportsListHelper reportsHelperObj = new ReportsListHelper();
    private ContractTreeData contractTreeDataObj = new ContractTreeData();

    @BeforeClass(groups = { "minor" })
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierContractConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierContractConfigFileName");

        extraFieldsConfigFileName = "ExtraFieldsForMultiSupplierContract.cfg";
    }

    @AfterClass(groups = { "minor" })
    public void afterClass() {
        //Delete All Newly Created Contracts
        for (Integer contractId : newlyCreatedContractIds) {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
    }

    /*
    TC-C89867: Create Multi Supplier Contract.
     */
    @Test(groups = { "minor" })
    public void testC89867() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String createSection = "contract creation flow 1";
            logger.info("Creating Multi Supplier Contract using Flow [{}]", createSection);

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, createSection);

            parentSupplierIdArr = flowProperties.get("sourceid").split(",");

            contractId = createMultiSupplierContract(createSection);

            if (contractId == -1) {
                throw new SkipException("Couldn't Create Multi Supplier Contract");
            }

            logger.info("Newly Created Multi Supplier Contract Id: {}", contractId);
            newlyCreatedContractIds.add(contractId);

            for (String supplierId : parentSupplierIdArr) {
                String showResponse = ShowHelper.getShowResponseVersion2(1, Integer.parseInt(supplierId));
                String supplierName = ShowHelper.getSupplierNameFromShowResponse(showResponse, 1);

                parentSuppliersMap.put(supplierId, supplierName);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Multi Supplier Contract Creation. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
   TC-C90089: Verify Multiple Suppliers visible on Month Calendar for Multiple Supplier Contract
*/
    @Test(enabled = true, groups = { "minor" })
    public void testC90089() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90089: Verify Multiple Suppliers visible on Month Calendar for Multiple Supplier Contract.");

            String createSection = "contract creation for calendar todo";
            logger.info("Creating Multi Supplier Contract using Flow [{}]", createSection);

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, createSection);

            calendarContractParentSupplierIdArr = flowProperties.get("sourceid").split(",");

            calendarContractId = createMultiSupplierContract(createSection);

            logger.info("Newly Created Multi Supplier Contract Id: {}", calendarContractId);
            newlyCreatedContractIds.add(calendarContractId);

            for (String supplierId : calendarContractParentSupplierIdArr) {
                String showResponse = ShowHelper.getShowResponseVersion2(1, Integer.parseInt(supplierId));
                String supplierName = ShowHelper.getSupplierNameFromShowResponse(showResponse, 1);

                calendarContractParentSuppliersMap.put(supplierId, supplierName);
            }

            logger.info("Hitting Calendar Data API for Month {} {}", calendarMonth, calendarYear);
            calendarDataObj.hitCalendarData(calendarMonth, calendarYear);
            String calendarDataResponse = calendarDataObj.getCalendarDataJsonStr();
            calendarDataObj.setRecords(calendarDataResponse);

            List<CalendarData> allCalendarRecords = calendarDataObj.getRecords();

            if (!allCalendarRecords.isEmpty()) {
                boolean contractFound = false;

                for (CalendarData record : allCalendarRecords) {
                    int recordEntityTypeId = record.getEntityTypeId();

                    if (recordEntityTypeId == 61) {
                        int recordId = record.getId();

                        if (recordId == calendarContractId) {
                            contractFound = true;

                            //Validate All Parent Suppliers
                            String allSuppliersValue = record.getSupplier();

                            for (String supplierId : calendarContractParentSupplierIdArr) {
                                String expectedSupplierName = calendarContractParentSuppliersMap.get(supplierId);

                                if (expectedSupplierName != null) {
                                    csAssert.assertTrue(allSuppliersValue.trim().toLowerCase().contains(expectedSupplierName.trim().toLowerCase()),
                                            "Expected Supplier Name " + expectedSupplierName + " not found in CalendarData Response.");
                                } else {
                                    csAssert.assertTrue(false, "Couldn't get Expected Supplier Name for Supplier Id " + supplierId);
                                }
                            }

                            break;
                        }
                    }
                }

                csAssert.assertTrue(contractFound, "Multi Supplier Contract not found in CalendarData API Response.");
            } else {
                csAssert.assertFalse(true, "No Record found in Calendar for Month " + calendarMonth + " " + calendarYear);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90089. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C89875: Verify Multiple Supplier on Contract Show Page
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC89875() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C89875: Verify Multiple Suppliers on Contract Show Page.");
        validateMultipleSuppliersOnContractShowPage(contractId, csAssert);

        csAssert.assertAll();
    }

    /*
    TC-C89876: Verify Multiple Suppliers on Contract Listing.
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC89876() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C89876: Verify Multiple Supplier on Contract Listing.");
        validateMultipleSuppliersOnContractListing(contractId, csAssert);

        csAssert.assertAll();
    }

    /*
    TC-C89877: Verify Filter for Multiple Parent Suppliers
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC98977() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89877: Verify Filter for Multiple Parent Suppliers");
            String supplierSubPayload = "";

            for (String supplierId : parentSupplierIdArr) {
                supplierSubPayload = supplierSubPayload.concat("{\"id\":" + supplierId + "},");
            }

            supplierSubPayload = supplierSubPayload.substring(0, supplierSubPayload.length() - 1);

            String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + supplierSubPayload + "]},\"filterId\":1,\"filterName\":\"supplier\"}}}," +
                    "\"selectedColumns\":[{\"columnId\":17,\"columnQueryName\":\"id\"},{\"columnId\":19,\"columnQueryName\":\"relationname\"}]}";

            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts", payload);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONArray dataArr = new JSONObject(listDataResponse).getJSONArray("data");

                if (dataArr.length() > 0) {
                    boolean recordFound = false;
                    String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                    for (int i = 0; i < dataArr.length(); i++) {
                        String idValue = dataArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
                        int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                        if (recordId == contractId) {
                            recordFound = true;
                        }
                    }

                    csAssert.assertTrue(recordFound, "Contract Id " + contractId + " not found in ListData API Response.");
                } else {
                    csAssert.assertFalse(true, "No Record returned in ListData API for Contracts and Filter Supplier");
                }
            } else {
                csAssert.assertFalse(true, "ListData API Response for Contracts is an Invalid JSON.");
                FileUtils.saveResponseInFile("ContractsListData C98977.txt", listDataResponse);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89877. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C89949: Verify that All Parent Suppliers should come when Contract is cloned
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC89949() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89949: Verify that All Parent Suppliers should come when Contract is cloned.");
            logger.info("Cloning Contract Id: {}", contractId);
            int clonedContractId = EntityOperationsHelper.cloneRecord("contracts", contractId, "2.0");

            if (clonedContractId != -1) {
                newlyCreatedContractIds.add(clonedContractId);

                //Validate All Parent Suppliers on Contract Show Page.
                validateMultipleSuppliersOnContractShowPage(clonedContractId, csAssert);

                //Validate All Parent Suppliers on Contract Listing.
                validateMultipleSuppliersOnContractListing(clonedContractId, csAssert);
            } else {
                csAssert.assertTrue(false, "Contract Clone failed for Parent Contract Id: " + contractId);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C89949. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C89967: Verify Page for Selecting Supplier should appear for Creating Secondary Entity from a multi supplier Contract.
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC89967() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89967: Verify Page for Selecting Supplier should appear for Creating Secondary Entity from a multi supplier Contract.");
            logger.info("Hitting CreateLinks API for Contract Id {}", contractId);
            String createLinksResponse = CreateLinks.getCreateLinksV2Response(61, contractId);

            if (ParseJsonResponse.validJsonResponse(createLinksResponse)) {
                logger.info("Getting All Create Links from Contract Id {}", contractId);
                Map<Integer, String> allCreateLinks = CreateLinks.getAllSingleCreateLinksMap(createLinksResponse);

                if (allCreateLinks != null) {
                    for (Map.Entry<Integer, String> createLinkMap : allCreateLinks.entrySet()) {
                        int entityTypeId = createLinkMap.getKey();
                        String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

                        logger.info("Checking Parent Suppliers on Create Page for Child Entity {}", entityName);

                        String createFreeFormResponse = CreateFreeForm.getCreateFreeFormResponse(contractId, "contracts");
                        if (ParseJsonResponse.validJsonResponse(createFreeFormResponse)) {
                            List<Map<String, String>> allSupplierOptions = CreateFreeForm.getAllMultiParentSuppliers(createFreeFormResponse);

                            if (!allSupplierOptions.isEmpty()) {
                                for (String expectedSupplierId : parentSupplierIdArr) {
                                    boolean supplierIdFound = false;

                                    for (Map<String, String> supplierOptionMap : allSupplierOptions) {
                                        String supplierId = supplierOptionMap.get("id");

                                        if (supplierId.equalsIgnoreCase(expectedSupplierId)) {
                                            supplierIdFound = true;
                                            break;
                                        }
                                    }

                                    csAssert.assertTrue(supplierIdFound, "Parent Supplier Id " + expectedSupplierId + " not found in CreateFreeForm API Response.");
                                }
                            } else {
                                csAssert.assertFalse(true, "Couldn't get All Supplier Options from CreateFreeForm API Response.");
                            }
                        } else {
                            csAssert.assertFalse(true, "CreateFreeForm API Response is an Invalid JSON.");
                        }
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't get All Create Links for Contract Id " + contractId);
                }
            } else {
                csAssert.assertFalse(true, "CreateLinks API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89967. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90094: Verify Supplier Filter in Calendar.
*/
    @Test(dependsOnMethods = "testC90089")
    public void testC90094() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90094: Validate Supplier Filter in Calendar.");

            //Validating Supplier filter only with limited options that were used in Multi Supplier Contract.
            for (String expectedParentSupplierId : calendarContractParentSupplierIdArr) {
                String payload = "{\"relationIds\":[\"" + expectedParentSupplierId + "\"]}";

                calendarDataObj.hitCalendarData(calendarMonth, calendarYear, "false", payload);
                String calendarDataResponse = calendarDataObj.getCalendarDataJsonStr();
                calendarDataObj.setRecords(calendarDataResponse);

                List<CalendarData> allCalendarRecords = calendarDataObj.getRecords();

                boolean contractFound = false;

                for (CalendarData record : allCalendarRecords) {
                    int recordEntityTypeId = record.getEntityTypeId();

                    if (recordEntityTypeId == 61) {
                        int recordId = record.getId();

                        if (recordId == calendarContractId) {
                            contractFound = true;
                            break;
                        }
                    }
                }

                csAssert.assertTrue(contractFound, "Multi Supplier Contract not found for Filter Supplier Id: " + expectedParentSupplierId);
            }

            //Validate Supplier Filter
            logger.info("Hitting CalendarFilterData API.");
            CalendarFilterData filterDataObj = new CalendarFilterData();
            filterDataObj.hitCalendarFilterData();
            String filterDataResponse = filterDataObj.getCalendarFilterDataJsonStr();

            boolean isSupplierFilterOfAutoCompleteType = CalendarFilterData.isFilterAutoComplete(filterDataResponse, "Supplier", "relations");

            List<Map<String, String>> allOptions;

            if (isSupplierFilterOfAutoCompleteType) {
                allOptions = CalendarFilterData.getAllOptionsOfSupplierAutoCompleteFilter();
            } else {
                allOptions = CalendarFilterData.getAllFilterOptionsFromQueryName(filterDataResponse, "relations");
            }

            int[] randomFilterOptionsIndex = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1, 10);

            for (int randomOption : randomFilterOptionsIndex) {
                Map<String, String> optionMap = allOptions.get(randomOption);
                String expectedSupplierId = optionMap.get("id");
                String payload = "{\"relationIds\":[\"" + expectedSupplierId + "\"]}";

                calendarDataObj.hitCalendarData(calendarMonth, calendarYear, "false", payload);
                String calendarDataResponse = calendarDataObj.getCalendarDataJsonStr();
                calendarDataObj.setRecords(calendarDataResponse);

                List<CalendarData> allCalendarRecords = calendarDataObj.getRecords();

                for (CalendarData record : allCalendarRecords) {
                    int recordEntityTypeId = record.getEntityTypeId();
                    int recordId = record.getId();

                    String showResponse = ShowHelper.getShowResponseVersion2(recordEntityTypeId, recordId);
                    ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, recordEntityTypeId, recordId, csAssert);
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90094. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90095: Verify Supplier in Downloaded Calendar Data.
*/
    @Test(dependsOnMethods = "testC90089")
    public void testC90095() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90095: Validate Supplier in Downloaded Calendar Data.");

            logger.info("Hitting Calendar Export Data for {} {}", calendarMonth, calendarYear);
            String filePath = "src/test";
            String fileName = "CalendarDownloadDataMultiSupplierContract.xlsx";
            String file = filePath + "/" + fileName;

            CalendarExportData downloadObj = new CalendarExportData();
            String data = "{\"startDate\":\"05/01/2018\",\"endDate\":\"05/31/2018\"}";
            HttpResponse downloadResponse = downloadObj.downloadCalendarDataFile(calendarMonth, calendarYear, data, "false", file);

            if (downloadResponse != null) {
                File downloadedFile = new File(file);

                if (downloadedFile.exists()) {
                    String contractShowResponse = ShowHelper.getShowResponseVersion2(61, calendarContractId);
                    String showHierarchy = ShowHelper.getShowFieldHierarchy("short code id", 61);
                    String expectedShortCodeId = ShowHelper.getActualValue(contractShowResponse, showHierarchy);

                    List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(filePath, fileName, "Calendar", 5);

                    List<String> allHeadersInLowerCase = new ArrayList<>();
                    for (String header : allHeaders) {
                        allHeadersInLowerCase.add(header.trim().toLowerCase());
                    }

                    if (allHeadersInLowerCase.contains("entity id")) {
                        int entityIdIndex = allHeadersInLowerCase.indexOf("entity id");

                        if (allHeadersInLowerCase.contains("supplier")) {
                            int supplierIndex = allHeadersInLowerCase.indexOf("supplier");

                            Long noOfRows = XLSUtils.getNoOfRows(filePath, fileName, "Calendar");
                            List<String> allEntityIds = XLSUtils.getOneColumnDataFromMultipleRows(filePath, fileName, "Calendar", entityIdIndex, 5,
                                    noOfRows.intValue() - 6);

                            if (allEntityIds.contains(expectedShortCodeId)) {
                                int rowNo = allEntityIds.indexOf(expectedShortCodeId) + 6;

                                String supplierValueInExcel = XLSUtils.getOneCellValue(filePath, fileName, "Calendar", rowNo, supplierIndex);

                                for (String supplierId : calendarContractParentSupplierIdArr) {
                                    String expectedSupplierName = calendarContractParentSuppliersMap.get(supplierId);

                                    if (expectedSupplierName != null) {
                                        csAssert.assertTrue(supplierValueInExcel.trim().toLowerCase().contains(expectedSupplierName.trim().toLowerCase()),
                                                "Expected Supplier Name " + expectedSupplierName + " not found in Downloaded Excel.");
                                    } else {
                                        csAssert.assertTrue(false, "Couldn't get Expected Supplier Name for Supplier Id " + supplierId);
                                    }
                                }
                            } else {
                                csAssert.assertFalse(true, "Couldn't find Contract ShortCodeId " + expectedShortCodeId + " in Downloaded Excel.");
                            }
                        } else {
                            csAssert.assertFalse(true, "Couldn't find Supplier Header in Downloaded Excel.");
                        }
                    } else {
                        csAssert.assertFalse(true, "Couldn't find Entity Id Header in Downloaded Excel.");
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't Download Calendar Data Excel.");
                }

                //Delete file
                FileUtils.deleteFile(file);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90095. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90076: Verify Multiple Supplier Contract on Todo Listing Page.
*/
    @Test(dependsOnMethods = "testC90089", groups = { "minor" })
    public void testC90076() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90076: Verify Multiple Supplier Contract on Todo Listing Page.");

            //Validating Multi Suppliers in To Do Listing.
            String payload = "{\"filterMap\":{\"customFilter\":{\"pendingAction\":{\"occurrence\":\"daily\",\"statusId\":1603}},\"offset\":0,\"size\":10,\"bypassPreferredView\":true}}";
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts", payload, true);
            boolean contractFound = false;
            JSONObject obj = new JSONObject(listDataResponse);
            JSONArray arr = obj.getJSONArray("data");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj2 = arr.getJSONObject(i);
                JSONObject obj3 = obj2.getJSONObject("17");
                String actualcalendarContractId = obj3.getString("value").split(":;")[1];
                if (Integer.parseInt(actualcalendarContractId)==calendarContractId) {
                    contractFound = true;
                    break;
                }
            }
            if (!contractFound) {
                csAssert.assertTrue(false, "Newly created MultiSupplier contract is not present in ToDo Newly created listing.");
            }

        } catch (Exception e){
            csAssert.fail("Something went wrong during execution of testC90076()");
        }
        csAssert.assertAll();
    }

    /*
    TC-C90080: Verify Multiple Supplier Contract in Downloaded Todo Data
*/
    @Test(dependsOnMethods = "testC90089")
    public void testC90080() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90080: Verify Multiple Supplier Contract in Downloaded Todo Data");

            String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":5,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{},\"ids\":[" + calendarContractId + "],\"bypassPreferredView\":true}}";
            validateListingDataDownloadExcel(calendarContractParentSupplierIdArr, calendarContractParentSuppliersMap, payload, csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90080. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90158: Verify Multiple Suppliers in Insights Listing
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC90158() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting TC-C90158: Validating Multiple Suppliers in Insights Listing.");

            String listDataPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"15\":{\"filterId\":\"15\",\"filterName\":\"expirationDate\",\"start\":\"01-01-2020\",\"end\":\"11-30-2039\"," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}},\"selectedColumns\":[{\"columnId\":11751," +
                    "\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":17,\"columnQueryName\":\"id\"},{\"columnId\":19,\"columnQueryName\":\"relationname\"}]}";

            Map<String, String> params = new HashMap<>();
            params.put("insightComputationId", "33");

            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts", listDataPayload, false, params);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONArray dataArr = new JSONObject(listDataResponse).getJSONArray("data");

                if (dataArr.length() > 0) {
                    String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                    String supplierColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "relationname");

                    boolean recordFound = false;

                    for (int i = 0; i < dataArr.length(); i++) {
                        String idValue = dataArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
                        int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                        if (recordId == contractId) {
                            recordFound = true;

                            String suppliersValue = dataArr.getJSONObject(i).getJSONObject(supplierColumnId).getString("value");
                            for (String expectedSupplierId : parentSupplierIdArr) {
                                csAssert.assertTrue(suppliersValue.contains(expectedSupplierId.trim()), "Expected Parent Supplier Id: " + expectedSupplierId +
                                        " not found in List Data API Response.");
                            }

                            break;
                        }
                    }

                    csAssert.assertTrue(recordFound, "Contract Id " + contractId + " not found in Insights ListData API Response.");
                } else {
                    csAssert.assertFalse(true, "No Record returned in ListData API for Contracts");
                }
            } else {
                csAssert.assertFalse(true, "ListData API Response for Contracts and Insight Id: 33 is an Invalid JSON.");
                FileUtils.saveResponseInFile("ContractsListData Insight 33.txt", listDataResponse);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90158. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    /*
    TC-C90160: Verify Multiple Supplier Contract in Downloaded Insight Listing
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC90160() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting TC-C90160: Validating Multiple Supplier Contract in Downloaded Insight Listing.");

            String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"15\":{\"filterId\":\"15\",\"filterName\":\"expirationDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"dayOffset\":null," +
                    "\"duration\":null,\"start\":\"04-01-2020\",\"end\":\"11-30-2023\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\"," +
                    "\"name\":\"Date\"}]}}}},\"selectedColumns\":[{\"columnId\":17,\"columnQueryName\":\"id\"},{\"columnId\":19,\"columnQueryName\":\"relationname\"}]}";

            DownloadListWithData downloadObj = new DownloadListWithData();
            Map<String, String> formParam = new HashMap<>();
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
            formParam.put("jsonData", payload);

            Map<String, String> insightMap = new HashMap<>();
            insightMap.put("insightComputationId", "33");

            HttpResponse downloadResponse = downloadObj.hitListRendererDownload(formParam, null, ConfigureConstantFields.getListIdForEntity("contracts"));
            String excelFilePath = "src/test";
            String excelFileName = "ListData_Excel_File_SelectedColumns.xlsx";
            boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, excelFilePath + "/" + excelFileName);

            if (fileDownloaded) {
                List<String> allColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, "Data", 4);

                if (allColumnNames.contains("SUPPLIER")) {
                    int supplierIndex = allColumnNames.indexOf("SUPPLIER");
                    String supplierValue = XLSUtils.getOneCellValue(excelFilePath, excelFileName, "Data", 4, supplierIndex);

                    for (String supplierId : parentSupplierIdArr) {
                        String expectedSupplierName = parentSuppliersMap.get(supplierId);

                        if (expectedSupplierName != null) {
                            csAssert.assertTrue(supplierValue.trim().toLowerCase().contains(expectedSupplierName.trim().toLowerCase()), "Expected Supplier Name " +
                                    expectedSupplierName + " not found in Downloaded Excel.");
                        } else {
                            csAssert.assertTrue(false, "Couldn't get Expected Supplier Name for Supplier Id " + supplierId);
                        }
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't find Supplier Column in Downloaded Excel.");
                }

                //Delete File
                FileUtils.deleteFile(excelFilePath, excelFileName);
            } else {
                csAssert.assertFalse(true, "Couldn't Download ListData Excel for Multi Supplier Contract.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90160. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90167: Verify Child contract creation from Multi Supplier Contract.
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC90167() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting TC-C90167: Validate Child Contract creation from Multi Supplier Contract");
            String createSection = "child contract creation flow 1";

            logger.info("Creating Multi Supplier Child Contract using Flow [{}]", createSection);
            UpdateFile.updateConfigFileProperty(configFilePath, configFileName, createSection, "sourceid", String.valueOf(contractId));

            String payload = "{\"entityIds\":[";

            for (String supplierId : parentSupplierIdArr) {
                payload = payload.concat(supplierId + ",");
            }

            payload = payload.substring(0, payload.length() - 1);
            payload = payload.concat("]}");

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, createSection);

            logger.info("Hitting New V1 API for Contracts");
            New newObj = new New();
            newObj.hitNewForMultiSupplierParent("contracts", flowProperties.get("parententitytype"), contractId, payload);
            String newResponse = newObj.getNewJsonStr();

            if (newResponse != null) {
                if (ParseJsonResponse.validJsonResponse(newResponse)) {
                    CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, createSection);

                    Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                    newObj.setAllRequiredFields(newResponse);
                    Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                    allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                    allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                    String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                            extraFieldsConfigFileName);

                    if (createPayload != null) {
                        logger.info("Hitting Create Api for Entity for Multi Supplier Child Contract");
                        Create createObj = new Create();
                        createObj.hitCreate("contracts", createPayload);
                        String createResponse = createObj.getCreateJsonStr();

                        if (ParseJsonResponse.validJsonResponse(createResponse)) {
                            String status = ParseJsonResponse.getStatusFromResponse(createResponse);

                            if (status.equalsIgnoreCase("success")) {
                                int childContractId = CreateEntity.getNewEntityId(createResponse, "contracts");

                                logger.info("Newly Created Multi Supplier Child Contract Id: {}", childContractId);
                                newlyCreatedContractIds.add(childContractId);

                                String showResponse = ShowHelper.getShowResponseVersion2(61, childContractId);
                                String showFieldHierarchy = ShowHelper.getShowFieldHierarchy("suppliers id", 61);

                                List<String> allSupplierIdsOnShowPage = ShowHelper.getAllSelectValuesOfField(showResponse, "supplier ids",
                                        showFieldHierarchy, childContractId, 61);

                                if (allSupplierIdsOnShowPage != null && !allSupplierIdsOnShowPage.isEmpty()) {
                                    for (String expectedSupplierId : parentSupplierIdArr) {
                                        csAssert.assertTrue(allSupplierIdsOnShowPage.contains(expectedSupplierId.trim()), "Expected Parent Supplier Id: " +
                                                expectedSupplierId + " not found in Child Contract Show API Response.");
                                    }
                                } else {
                                    csAssert.assertFalse(true, "Couldn't get All Supplier Ids from Show Response of Child Contract Id " + childContractId);
                                }
                            } else {
                                csAssert.assertFalse(true, "Multi Supplier Child Contract Creation failed due to " + status);
                            }
                        } else {
                            csAssert.assertFalse(true, "Create API Response is an Invalid JSON.");
                        }
                    } else {
                        csAssert.assertFalse(true, "Create Payload is null and hence cannot create Multi Supplier Child Contract.");
                    }
                } else {
                    csAssert.assertFalse(true, "New V1 API Response is an Invalid JSON for Contracts.");
                }
            } else {
                csAssert.assertFalse(true, "New API Response is null.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90167. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*TC-C90169: Verify Supplier should not be deleted if it has any multi-supplier contract in its hierarchy.
    TC-C90171: Verify Supplier should be deleted if it doesn't have any multi-supplier contract in its hierarchy.
    TC-C90263: Verify Supplier Archiving when child multi-supplier contract is present in its hierarchy.*/
    @Test()
    public void testC90169() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting TC-C90169: Validating Supplier should not be deleted if it has any multi-supplier contract in its hierarchy.");

            String supplierCreateSection = "testc90169 suppliers";
            logger.info("Creating Supplier using Section: {}", supplierCreateSection);

            String supplierCreateResponse = Supplier.createSupplier(supplierCreateSection, true);

            if (ParseJsonResponse.validJsonResponse(supplierCreateResponse)) {
                String status = ParseJsonResponse.getStatusFromResponse(supplierCreateResponse);

                if (status.equalsIgnoreCase("success")) {
                    int supplierId = CreateEntity.getNewEntityId(supplierCreateResponse);
                    String contractCreateSection = "testc90169 contracts";

                    logger.info("Creating Multi Supplier Contract using Flow [{}]", contractCreateSection);
                    UpdateFile.updateConfigFileProperty(configFilePath, configFileName, contractCreateSection, "sourceid", "newSupplierId",
                            String.valueOf(supplierId));

                    int newContractId = createMultiSupplierContract(contractCreateSection);

                    UpdateFile.updateConfigFileProperty(configFilePath, configFileName, contractCreateSection, "sourceid", String.valueOf(supplierId),
                            "newSupplierId");

                    newlyCreatedContractIds.add(newContractId);

                    //Verify Archiving Supplier
                    boolean archiveSupplier = performActionOnSupplier("Archive", supplierId);
                    csAssert.assertTrue(archiveSupplier, "Couldn't Archive Parent Supplier");

                    String supplierShowResponse = ShowHelper.getShowResponseVersion2(1, supplierId);
                    String supplierStatus = ShowHelper.getActualValue(supplierShowResponse, ShowHelper.getShowFieldHierarchy("status", 1));

                    if (supplierStatus == null || !supplierStatus.equalsIgnoreCase("Archived")) {
                        csAssert.assertTrue(false, "Supplier Id " + supplierId + " is not in Archived State.");
                    }

                    //Verify Child Contract is not in Archived State.
                    String childContractShowResponse = ShowHelper.getShowResponseVersion2(61, newContractId);
                    String childContractStatus = ShowHelper.getActualValue(childContractShowResponse, ShowHelper.getShowFieldHierarchy("status", 61));

                    if (childContractStatus == null || childContractStatus.equalsIgnoreCase("Archived")) {
                        csAssert.assertTrue(false, "Child Contract Id " + newContractId + " is in Archived State.");
                    }

                    //Restore Supplier
                    boolean restoreSupplier = performActionOnSupplier("Restore", supplierId);
                    csAssert.assertTrue(restoreSupplier, "Couldn't Restore Parent Supplier");

                    //Verify Delete is unsuccessful.
                    logger.info("Hitting Delete Supplier API when Child Contract is present.");
                    String deleteSupplierResponse = deleteSupplier(supplierId);
                    String deleteStatus = ParseJsonResponse.getStatusFromResponse(deleteSupplierResponse);

                    if (deleteStatus.equalsIgnoreCase("validationError")) {
                        //C90171: Verify able to delete supplier when no child contract is present.
                        logger.info("Starting Test TC-C90171: Validate that supplier should be deleted when no child contract is present.");

                        //Deleting Child Contract
                        logger.info("Deleting Child Contract having Id: {}", newContractId);
                        EntityOperationsHelper.deleteEntityRecord("contracts", newContractId);

                        if (newlyCreatedContractIds.contains(newContractId)) {
                            newlyCreatedContractIds.remove(newlyCreatedContractIds.indexOf(newContractId));
                        }

                        logger.info("Deleting Supplier when Child Contract is not present.");
                        deleteSupplierResponse = deleteSupplier(supplierId);
                        deleteStatus = ParseJsonResponse.getStatusFromResponse(deleteSupplierResponse);

                        csAssert.assertTrue(deleteStatus.equalsIgnoreCase("success"),
                                "Supplier not deleted whereas it was supposed to be deleted after Child Contract is deleted.");
                    } else if (deleteStatus.equalsIgnoreCase("success")) {
                        csAssert.assertFalse(true, "Supplier Deleted successfully whereas it was not supposed to when Child Contract is present.");
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't Create Supplier due to " + status);
                }
            } else {
                csAssert.assertFalse(true, "Supplier Create API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90169. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C89918: Verify all Multiple Supplier Names in columns of Reports Listing page.
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC89918() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89918: Verify all Multiple Supplier Names in Columns of Reports Listing Page.");

            for (int reportId : contractReportIds) {
                String reportName = reportsHelperObj.getReportName(61, reportId);
                validateMultipleSuppliersOnContractReportListing(contractId, reportName, reportId, csAssert);
            }
        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89918. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C89920: Verify Multi-Supplier Filter on Report Listing.
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC89920() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89920: Verify Filter for Multiple Parent Suppliers on Report Listing");
            String supplierSubPayload = "";

            for (String supplierId : parentSupplierIdArr) {
                supplierSubPayload = supplierSubPayload.concat("{\"id\":" + supplierId + "},");
            }

            supplierSubPayload = supplierSubPayload.substring(0, supplierSubPayload.length() - 1);

            for (int reportId : contractReportIds) {
                logger.info("Validating Multi Supplier Filter for Report Id {}", reportId);
                String defaultUserListMetadataResponse = metadataHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);

                String supplierColumnName, supplierColumnId, contractIdColumnName, contractIdColumnId;

                switch (reportId) {
                    case 222:
                    case 223:
                    case 224:
                    case 270:
                        supplierColumnName = "vendor_name";
                        contractIdColumnName = "sirion_id";
                        break;

                    default:
                        supplierColumnName = "supplier";
                        contractIdColumnName = "id";
                        break;
                }

                supplierColumnId = metadataHelperObj.getColumnPropertyValueFromQueryName(defaultUserListMetadataResponse, supplierColumnName, "id");
                contractIdColumnId = metadataHelperObj.getColumnPropertyValueFromQueryName(defaultUserListMetadataResponse, contractIdColumnName, "id");

                String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + supplierSubPayload + "]},\"filterId\":1,\"filterName\":\"supplier\"}}}," +
                        "\"selectedColumns\":[{\"columnId\":" + contractIdColumnId + ",\"columnQueryName\":\"" + contractIdColumnName +
                        "\"},{\"columnId\":" + supplierColumnId + ",\"columnQueryName\":\"" + supplierColumnName + "\"}]}";

                String reportListDataResponse = reportsHelperObj.hitListDataAPIForReportId(reportId, payload);

                if (ParseJsonResponse.validJsonResponse(reportListDataResponse)) {
                    JSONArray dataArr = new JSONObject(reportListDataResponse).getJSONArray("data");

                    if (dataArr.length() > 0) {
                        boolean recordFound = false;

                        for (int i = 0; i < dataArr.length(); i++) {
                            String idValue = dataArr.getJSONObject(i).getJSONObject(contractIdColumnId).getString("value");
                            int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                            if (recordId == contractId) {
                                recordFound = true;
                            }
                        }

                        csAssert.assertTrue(recordFound, "Contract Id " + contractId + " not found in ListData API Response for Report Id " + reportId);
                    } else {
                        csAssert.assertFalse(true, "No Record returned in ListData API for Report Id " + reportId + " and Filter Supplier");
                    }
                } else {
                    csAssert.assertFalse(true, "ListData API Response for Report Id " + reportId + " is an Invalid JSON.");
                    FileUtils.saveResponseInFile("C89920 ReportListData.txt", reportListDataResponse);
                }
            }
        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89920. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90141: To Verify MPC will be shown in all the supplier's tree.
*/
    @Test(dependsOnMethods = "testC89867")
    public void testC90141() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90141: Validating MPC in Supplier's tree.");

            //Validate Multi Supplier Contract's Tree.
            logger.info("Hitting Contract Tree Data V1 API for Contract Id {}", contractId);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("hierarchy", "true");
            queryParams.put("offset", "0");
            queryParams.put("rootSupplierId", "0");

            String contractTreeResponseForContract = contractTreeDataObj.hitContractTreeListAPIV1(61, contractId, "{}", queryParams);

            if (ParseJsonResponse.validJsonResponse(contractTreeResponseForContract)) {
                JSONObject jsonObj = new JSONObject(contractTreeResponseForContract).getJSONObject("body").getJSONObject("data");
                JSONArray jsonArr = jsonObj.getJSONArray("relations");

                boolean multiSupplierFlagValue = jsonObj.getBoolean("multiSupplier");
                csAssert.assertTrue(multiSupplierFlagValue, "MultiSupplier Flag is False in Contract Tree V1 API Response for Contract Id " + contractId);

                List<String> actualSupplierIdsArr = new ArrayList<>();

                for (int i = 0; i < jsonArr.length(); i++) {
                    String supplierId = String.valueOf(jsonArr.getJSONObject(i).getInt("id"));
                    actualSupplierIdsArr.add(supplierId);
                }

                for (String expectedSupplierId : parentSupplierIdArr) {
                    if (!actualSupplierIdsArr.contains(expectedSupplierId)) {
                        csAssert.assertFalse(true, "Parent Supplier Id " + expectedSupplierId +
                                " not found in Contract Tree V1 API Response of Contract Id " + contractId);
                    }
                }

                //Validate MPC in Supplier's Tree.
                for (String parentSupplierId : parentSupplierIdArr) {
                    queryParams.put("hierarchy", "false");
                    queryParams.put("rootSupplierId", parentSupplierId);

                    String supplierTreeResponse = contractTreeDataObj.hitContractTreeListAPIV1(1, Integer.parseInt(parentSupplierId),
                            "{}", queryParams);

                    if (ParseJsonResponse.validJsonResponse(supplierTreeResponse)) {
                        jsonObj = new JSONObject(supplierTreeResponse).getJSONObject("body").getJSONObject("data");
                        jsonArr = jsonObj.getJSONArray("children");

                        boolean contractFoundInTree = false;

                        for (int j = 0; j < jsonArr.length(); j++) {
                            jsonObj = jsonArr.getJSONObject(j);
                            int entityId = jsonObj.getInt("entityId");

                            if (entityId == contractId) {
                                contractFoundInTree = true;

                                multiSupplierFlagValue = jsonObj.getBoolean("multiSupplier");
                                csAssert.assertTrue(multiSupplierFlagValue, "MultiSupplier Flag is False for Contract Id " + contractId +
                                        " in Contract Tree V1 API Response of Supplier Id " + parentSupplierId);

                                break;
                            }
                        }

                        csAssert.assertTrue(contractFoundInTree, "MultiSupplier Contract not found in  Contract Tree V1 API Response of Supplier Id " +
                                parentSupplierId);
                    } else {
                        csAssert.assertFalse(true, "Contract Tree V1 API Response for Supplier Id " + parentSupplierId + " is an Invalid JSON.");
                    }
                }

            } else {
                csAssert.assertFalse(true, "Contract Tree Data V1 API Response for Contract Id " + contractId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Test TC-C90141. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90246: Verify Correct data in Contract Tree for Multi Supplier Contract and Parent Suppliers.
*/
    @Test(dependsOnMethods = "testC90141")
    public void testC90246() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90246: Validating Correct data in Contract Tree for Multi Supplier Contract and Parent Suppliers");

            for (String parentSupplierId : parentSupplierIdArr) {
                //Validate Relation Id in Contract Tree Response of Contract Entity.
                validateRelationIdInContractTreeResponse(61, contractId, parentSupplierId, csAssert);

                //Validate Relation Id in Contract Tree Response of Parent Supplier Entity.
                validateRelationIdInContractTreeResponse(1, Integer.parseInt(parentSupplierId), parentSupplierId, csAssert);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Test TC-C90246. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90262: Verify Multi-Supplier Drop down should not be visible for Single Supplier Contract in Contract Tree
*/
    @Test()
    public void testC90262() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90262: Validate that Multi-Supplier Drop down should not be visible for Single Supplier Contract in Contract Tree.");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts");
            JSONArray jsonArr = new JSONObject(listDataResponse).getJSONArray("data");

            String columnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

            boolean recordFound = false;
            int recordId = -1;

            for (int i = 0; i < jsonArr.length(); i++) {
                String idValue = jsonArr.getJSONObject(i).getJSONObject(columnId).getString("value");
                recordId = ListDataHelper.getRecordIdFromValue(idValue);

                String showResponse = ShowHelper.getShowResponseVersion2(61, recordId);
                if (ParseJsonResponse.validJsonResponse(showResponse) && ShowHelper.isShowPageAccessible(showResponse)) {
                    JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");
                    int parentEntityTypeId = jsonObj.getJSONObject("parentEntityTypeId").getInt("values");

                    if (parentEntityTypeId == 1) {
                        int noOfSuppliers = jsonObj.getJSONObject("relations").getJSONArray("values").length();

                        if (noOfSuppliers == 1) {
                            recordFound = true;
                            break;
                        }
                    }
                }
            }

            if (recordFound) {
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("hierarchy", "true");
                queryParams.put("offset", "0");
                queryParams.put("rootSupplierId", "0");

                String contractTreeResponse = contractTreeDataObj.hitContractTreeListAPIV1(61, recordId, "{}", queryParams);

                if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
                    JSONObject jsonObj = new JSONObject(contractTreeResponse).getJSONObject("body").getJSONObject("data");
                    boolean multiSupplierFlag = jsonObj.getBoolean("multiSupplier");

                    csAssert.assertFalse(multiSupplierFlag, "MultiSupplier Flag in True in Contract Tree V1 API Response for Contract Id " + recordId);
                } else {
                    csAssert.assertFalse(true, "Contract Tree V1 API Response for Contract Id " + recordId + " is an Invalid JSON.");
                }
            } else {
                throw new SkipException("Couldn't find Single Supplier Contract in List Data API Response.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Test TC-C90262. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90036: Verify Multi Supplier Contract in Search Results and Downloaded Excel.
     */
    @Test()
    public void testC90036() {
        CustomAssert csAssert = new CustomAssert();

        String supplierSubPayload = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90036", "supplierquery");
        int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90036", "entityid"));
        String expectedSuppliers = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90036", "expectedsuppliers");
        String contractSeqId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90036", "contractseqid");

        try {
            logger.info("Starting TC-C90036: Verify Multi Supplier Contract in Search Results and Downloaded Excel.");

                String searchPayload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":61}," +
                        "\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":0},\"size\":{\"name\":\"size\",\"values\":20}}," +
                        "\"relations\":{\"name\":\"relations\",\"id\":39,\"values\":[{\"name\":\"" + supplierSubPayload + "\"}]}}}}";

                logger.info("Hitting Search Api for Entity Contract and Field Supplier and Value {}", supplierSubPayload);
                Search searchObj = new Search();
                String searchResponse = searchObj.hitSearch(61, searchPayload);

                if (ParseJsonResponse.validJsonResponse(searchResponse)) {
                    JSONObject jsonObj = new JSONObject(searchResponse).getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values");
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    boolean contractFound = false;
                    String idColumn = TabListDataHelper.getColumnIdFromColumnName(jsonObj.toString(), "id");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        String idValue = jsonArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
                        int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                        if (recordId == contractId) {
                            contractFound = true;
                            String idcol = TabListDataHelper.getColumnIdFromColumnName(jsonObj.toString(), "relationname");
                            String suppliers = jsonArr.getJSONObject(i).getJSONObject(idcol).getString("value");
                            suppliers = suppliers.split("::")[0].split(":;")[0]+","+suppliers.split("::")[1].split(":;")[0];
                            csAssert.assertEquals(expectedSuppliers, suppliers, "Suppliers don't match in response");
                            break;
                        }
                    }

                    if (contractFound) {
                        //Validate Results in Search Downloaded Excel.
                        String downloadLimitExceeded = jsonObj.getString("limitExceeded");
                        if (downloadLimitExceeded.equalsIgnoreCase("none")) {

                            logger.info("Downloading Excel for Multi Supplier Contract.");
                            String filePath = "src/test";
                            String fileName = "SearchResults.xlsx";

                            HttpResponse downloadResponse = new MetadataSearchDownload().downloadMetadataSearchFile(61, searchPayload);
                            boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, filePath + "/" + fileName);

                            if (fileDownloaded) {

                                List<List<String>> xlList = XLSUtils.getExcelDataOfMultipleRows(filePath, fileName, "Data",4, new Long(XLSUtils.getNoOfRows(filePath, fileName, "Data")).intValue());

                                for(List<String> item: xlList) {
                                    String seqId = item.get(0);
                                    if(seqId.equals(contractSeqId)){
                                        String actualSuppliers = item.get(3);
                                        csAssert.assertEquals(actualSuppliers, expectedSuppliers, "Suppliers don't match in Excel");
                                        break;
                                    }
                                }

                                //Delete Excel
                                FileUtils.deleteFile(filePath, fileName);
                            } else {
                                csAssert.assertFalse(true, "Couldn't Download Search Excel for Multi Supplier Contract");
                            }
                        } else {
                            throw new SkipException("Search Results Download limit Exceeded. Hence skipping further validation.");
                        }
                    } else {
                        csAssert.assertFalse(true, "Multi Supplier Contract having Id " + contractId + " not found in Search Response.");
                    }
                } else {
                    csAssert.assertFalse(true, "Search Response for Contract and Field Supplier and Value [" + supplierSubPayload +
                            "] is an Invalid JSON.");
                }
        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90036. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90296: Verify error message while creating OLA Type Contract with Multi Supplier
     */
    @Test(groups = { "minor" })
    public void testC90296() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90296: Verify error message while Creating OLA Type Contract with Multi Supplier.");
            String createSection = "tc90296";

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, createSection);
            String[] allSupplierIds = flowProperties.get("supplierids").split(",");

            String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":" + Arrays.toString(allSupplierIds) + ",\"entityTypeId\":1}," +
                    "\"actualParentEntity\":{\"entityIds\":[" + allSupplierIds[0] + "],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" +
                    allSupplierIds[0] + "],\"entityTypeId\":1}}";

            //Validating Error Message with default language.
            String createResponse = multiSupplierCreateResponse(newPayload, createSection);

            FieldRenaming fieldRenamingHelperObj = new FieldRenaming();
            String fieldRenamingResponse = fieldRenamingHelperObj.getFieldRenamingUpdateResponse(1, 2095);
            String fieldName = "OLA contracts does not support multi suppliers, Please select one supplier and resubmit request";
            String expectedErrorMessage = fieldRenamingHelperObj.getClientFieldNameFromName(fieldRenamingResponse, "Workflow Error Messages", fieldName);

            validateCreateErrorMessage(createResponse, expectedErrorMessage, "English", csAssert);

            //Validating Error Message with Russian language.
            UpdateAccount updateAccountObj = new UpdateAccount();
            String userLoginId = ConfigureEnvironment.getEnvironmentProperty("j_username");
            updateAccountObj.updateUserLanguage(userLoginId, 1002, 1000);

            fieldRenamingResponse = fieldRenamingHelperObj.getFieldRenamingUpdateResponse(1000, 2095);
            expectedErrorMessage = fieldRenamingHelperObj.getClientFieldNameFromName(fieldRenamingResponse, "Workflow Error Messages", fieldName);
            createResponse = multiSupplierCreateResponse(newPayload, createSection);

            validateCreateErrorMessage(createResponse, expectedErrorMessage, "Russian", csAssert);

            updateAccountObj.updateUserLanguage(userLoginId, 1002, 1);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90296. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C90475: Verify Multi Supplier Filter for Contract Reports.
     */
    @Test ()
    public void testC90475() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90475: Validate Multi Supplier Filter for Contract Reports.");
            int[] allContractReportIds = {49, 50, 222, 223, 224, 270, 280, 1000};

            ReportRendererFilterData reportFilterObj = new ReportRendererFilterData();

            for (int reportId : allContractReportIds) {
                String reportName = reportsHelperObj.getReportName(61, reportId);

                logger.info("Validating MultiSupplierContract Filter present in FilterData API for Report [{}] having Id {}", reportName, reportId);
                reportFilterObj.hitReportRendererFilterData(reportId);
                String filterDataResponse = reportFilterObj.getReportRendererFilterDataJsonStr();

                if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                    JSONObject jsonObj = new JSONObject(filterDataResponse);
                    String[] allJsonObj = JSONObject.getNames(jsonObj);

                    boolean mpcFilterFound = false;

                    for (String objName : allJsonObj) {
                        String filterName = jsonObj.getJSONObject(objName).getString("filterName");

                        if (filterName.equalsIgnoreCase("multisuppliercontracts")) {
                            mpcFilterFound = true;
                            break;
                        }
                    }

                    csAssert.assertTrue(mpcFilterFound, "Multi Supplier Contract Filter not found in Filter Data API Response for Report [" + reportName +
                            "] having Id " + reportId);
                } else {
                    csAssert.assertFalse(true, "Filter Data API Response for Report [" + reportName + "] having Id " + reportId + " is an Invalid JSON.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90475. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private String multiSupplierCreateResponse(String newPayload, String contractCreateSection) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        extraFieldsConfigFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                    Create createObj = new Create();
                    createObj.hitCreate("contracts", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Contract Create Payload is null and hence cannot create Multi Supplier Contract.");
                }
            } else {
                logger.error("New V1 API Response is an Invalid JSON for Contracts.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }

    private void validateCreateErrorMessage(String createResponse, String expectedErrorMessage, String language, CustomAssert csAssert) {
        if (createResponse != null && ParseJsonResponse.validJsonResponse(createResponse)) {
            String status = ParseJsonResponse.getStatusFromResponse(createResponse);

            if (status.equalsIgnoreCase("validationError")) {
                JSONObject jsonObj = new JSONObject(createResponse).getJSONObject("body").getJSONObject("errors");
                JSONArray jsonArr = jsonObj.getJSONArray("genericErrors");

                boolean errorMatch = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String errorMessage = jsonArr.getJSONObject(0).getString("message");

                    if (errorMessage.equalsIgnoreCase(expectedErrorMessage)) {
                        errorMatch = true;
                        break;
                    }
                }

                csAssert.assertTrue(errorMatch, "Expected Error Message: [" + expectedErrorMessage + "] not found in Create API Response for Language " + language);
            } else {
                csAssert.assertFalse(true, "OLA Contract Create API Response. Expected Status: ValidationError and Actual Status: " + status);
            }
        }
    }

    private int createMultiSupplierContract(String contractCreateSection) {
        try {
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, contractCreateSection);

            String[] parentSupplierIdsArr = flowProperties.get("sourceid").split(",");
            String payload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}," +
                    "\"actualParentEntity\":{\"entityIds\":[" + parentSupplierIdsArr[0] + "],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" +
                    parentSupplierIdsArr[0] + "],\"entityTypeId\":1}}";

            String createResponse = multiSupplierCreateResponse(payload, contractCreateSection);

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                String status = ParseJsonResponse.getStatusFromResponse(createResponse);

                if (status.equalsIgnoreCase("success")) {
                    return CreateEntity.getNewEntityId(createResponse, "contracts");
                } else {
                    logger.error("Multi Supplier Contract Creation failed due to " + status);
                }
            } else {
                logger.error("Contract Create API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Multi Supplier Contract. " + e.getMessage());
        }

        return -1;
    }

    private void validateMultipleSuppliersOnContractShowPage(int contractId, CustomAssert csAssert) {
        try {
            String showResponse = ShowHelper.getShowResponseVersion2(61, contractId);

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                String showFieldHierarchy = ShowHelper.getShowFieldHierarchy("suppliers id", 61);

                List<String> allSupplierIdsOnShowPage = ShowHelper.getAllSelectValuesOfField(showResponse, "supplier ids", showFieldHierarchy,
                        contractId, 61);

                if (allSupplierIdsOnShowPage != null && !allSupplierIdsOnShowPage.isEmpty()) {
                    for (String expectedSupplierId : parentSupplierIdArr) {
                        csAssert.assertTrue(allSupplierIdsOnShowPage.contains(expectedSupplierId.trim()), "Expected Parent Supplier Id: " + expectedSupplierId +
                                " not found in Contract Show API Response.");
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't get All Supplier Ids from Show Response of Contract Id " + contractId);
                }
            } else {
                csAssert.assertFalse(true, "Show API Response for Multi Supplier Contract Id " + contractId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Multiple Suppliers on Contract Show Page. " + e.getMessage());
        }
    }

    private void validateMultipleSuppliersOnContractListing(int contractId, CustomAssert csAssert) {
        try {
            String listDataPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":17,\"columnQueryName\":\"id\"},{\"columnId\":19,\"columnQueryName\":\"relationname\"}]}";
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts", listDataPayload);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONArray dataArr = new JSONObject(listDataResponse).getJSONArray("data");

                if (dataArr.length() > 0) {
                    String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                    String supplierColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "relationname");

                    boolean recordFound = false;

                    for (int i = 0; i < dataArr.length(); i++) {
                        String idValue = dataArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
                        int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                        if (recordId == contractId) {
                            recordFound = true;

                            String suppliersValue = dataArr.getJSONObject(i).getJSONObject(supplierColumnId).getString("value");
                            for (String expectedSupplierId : parentSupplierIdArr) {
                                csAssert.assertTrue(suppliersValue.contains(expectedSupplierId.trim()), "Expected Parent Supplier Id: " + expectedSupplierId +
                                        " not found in List Data API Response.");
                            }

                            break;
                        }
                    }

                    csAssert.assertTrue(recordFound, "Contract Id " + contractId + " not found in ListData API Response.");

                    //Validate Download Excel
                    validateListingDataDownloadExcel(parentSupplierIdArr, null, csAssert);
                } else {
                    csAssert.assertFalse(true, "No Record returned in ListData API for Contracts");
                }
            } else {
                csAssert.assertFalse(true, "ListData API Response for Contracts is an Invalid JSON.");
                FileUtils.saveResponseInFile("Contract List Data.txt", listDataResponse);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Multiple Suppliers on Contract Listing. " + e.getMessage());
        }
    }

    /*
   TC-C89902: Verify Multiple Suppliers in Listing Data Downloaded Excel
    */
    private void validateListingDataDownloadExcel(String[] parentSupplierId, String payload, CustomAssert csAssert) {
        validateListingDataDownloadExcel(parentSupplierId, parentSuppliersMap, payload, csAssert);
    }

    private void validateListingDataDownloadExcel(String[] parentSupplierId, Map<String, String> parentSuppliersMap, String payload, CustomAssert csAssert) {
        try {
            DownloadListWithData downloadObj = new DownloadListWithData();
            Map<String, String> formParam = new HashMap<>();
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));

            if (payload == null) {
                String supplierSubPayload = "";

                for (String supplierId : parentSupplierId) {
                    supplierSubPayload = supplierSubPayload.concat("{\"id\":" + supplierId + "},");
                }

                supplierSubPayload = supplierSubPayload.substring(0, supplierSubPayload.length() - 1);

                payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + supplierSubPayload + "]},\"filterId\":1,\"filterName\":\"supplier\"}}}," +
                        "\"selectedColumns\":[{\"columnId\":17,\"columnQueryName\":\"id\"},{\"columnId\":19,\"columnQueryName\":\"relationname\"}]}";
            }

            formParam.put("jsonData", payload);

            HttpResponse downloadResponse = downloadObj.hitDownloadListWithData(formParam, ConfigureConstantFields.getListIdForEntity("contracts"));
            String excelFilePath = "src/test";
            String excelFileName = "ListData_Excel_File_SelectedColumns.xlsx";
            boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, excelFilePath + "/" + excelFileName);

            if (fileDownloaded) {
                List<String> allColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, "Data", 4);

                if (allColumnNames.contains("SUPPLIER")) {
                    int supplierIndex = allColumnNames.indexOf("SUPPLIER");
                    String supplierValue = XLSUtils.getOneCellValue(excelFilePath, excelFileName, "Data", 4, supplierIndex);

                    for (String supplierId : parentSupplierId) {
                        String expectedSupplierName = parentSuppliersMap.get(supplierId);

                        if (expectedSupplierName != null) {
                            csAssert.assertTrue(supplierValue.trim().toLowerCase().contains(expectedSupplierName.trim().toLowerCase()), "Expected Supplier Name " +
                                    expectedSupplierName + " not found in Downloaded Excel.");
                        } else {
                            csAssert.assertTrue(false, "Couldn't get Expected Supplier Name for Supplier Id " + supplierId);
                        }
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't find Supplier Column in Downloaded Excel.");
                }

                //Delete Excel
                FileUtils.deleteFile(excelFilePath, excelFileName);
            } else {
                csAssert.assertFalse(true, "Couldn't Download ListData Excel for Multi Supplier Contract.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89902. " + e.getMessage());
        }
    }

    private String deleteSupplier(int supplierId) {
        try {
            String supplierShowResponse = ShowHelper.getShowResponseVersion2(1, supplierId);

            if (ParseJsonResponse.validJsonResponse(supplierShowResponse)) {
                String deletePayload = EntityOperationsHelper.getPayloadForDeletingRecord(supplierShowResponse);

                Delete deleteObj = new Delete();
                deleteObj.hitDelete("suppliers", deletePayload, "relations");
                return deleteObj.getDeleteJsonStr();
            }
        } catch (Exception e) {
            logger.error("Exception while Deleting Supplier");
        }

        return null;
    }

    private boolean performActionOnSupplier(String actionName, int supplierId) {
        try {
            logger.info("Hitting Actions V3 API for Supplier Id {}", supplierId);
            String actionsResponse = Actions.getActionsV3Response(1, supplierId);

            if (ParseJsonResponse.validJsonResponse(actionsResponse)) {
                List<String> allActionNames = Actions.getAllActionNamesV3(actionsResponse);

                if (allActionNames != null && allActionNames.contains(actionName)) {
                    String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);

                    if (apiPath != null) {
                        String showResponse = ShowHelper.getShowResponseVersion2(1, supplierId);

                        logger.info("Performing Action {} on Supplier Id {}", actionName, supplierId);
                        String payload = "{\"body\":{\"data\":" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").toString() + "}}";
                        String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
                        String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

                        return status.equalsIgnoreCase("success");
                    } else {
                        logger.error("Couldn't get API Path for Performing Action {} on Supplier Id {}", actionName, supplierId);
                    }
                } else {
                    logger.error("Action {} not available for Supplier Id {}", actionName, supplierId);
                }
            } else {
                logger.error("Actions V3 API Response for Supplier Id {} is an Invalid JSON.", supplierId);
            }
        } catch (Exception e) {
            logger.error("Exception while Performing Action {} on Supplier Id {}. {}", actionName, supplierId, e.getMessage());
        }

        return false;
    }

    private void validateMultipleSuppliersOnContractReportListing(int contractId, String reportName, int reportId, CustomAssert csAssert) {
        String createFormResponse = CreateForm.getCreateFormResponse(reportId);

        if (reportId != 1000) {
            List<String> allSelectedStatus = CreateForm.getAllSelectedStatus(createFormResponse, reportId);

            if (!allSelectedStatus.contains("Newly Created")) {
                throw new SkipException("Report [" + reportName + "] having Id " + reportId +
                        " doesn't contain Newly Created Status. Hence it will not have New Contracts in it");
            }
        }

        logger.info("Validating Multiple Suppliers for Report [{}] having Id {}", reportName, reportId);
        String defaultUserListMetadataResponse = metadataHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);

        String supplierColumnName, supplierColumnId, contractIdColumnName, contractIdColumnId;

        switch (reportId) {
            case 222:
            case 223:
            case 224:
            case 270:
//                case 280:
                supplierColumnName = "vendor_name";
                contractIdColumnName = "sirion_id";
                break;

            default:
                supplierColumnName = "supplier";
                contractIdColumnName = "id";
                break;
        }

        supplierColumnId = metadataHelperObj.getColumnPropertyValueFromQueryName(defaultUserListMetadataResponse, supplierColumnName, "id");
        contractIdColumnId = metadataHelperObj.getColumnPropertyValueFromQueryName(defaultUserListMetadataResponse, contractIdColumnName, "id");

        String reportListDataPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":" + contractIdColumnId +
                ",\"columnQueryName\":\"" + contractIdColumnName + "\"}," + "{\"columnId\":" + supplierColumnId + ",\"columnQueryName\":\"" + supplierColumnName + "\"}]}";

        String reportListDataResponse = reportsHelperObj.hitListDataAPIForReportId(reportId, reportListDataPayload);

        if (ParseJsonResponse.validJsonResponse(reportListDataResponse)) {
            JSONArray dataArr = new JSONObject(reportListDataResponse).getJSONArray("data");

            if (dataArr.length() > 0) {
                boolean recordFound = false;

                for (int i = 0; i < dataArr.length(); i++) {
                    String idValue = dataArr.getJSONObject(i).getJSONObject(contractIdColumnId).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if (recordId == contractId) {
                        recordFound = true;

                        String suppliersValue = dataArr.getJSONObject(i).getJSONObject(String.valueOf(supplierColumnId)).getString("value");
                        for (String expectedSupplierId : parentSupplierIdArr) {
                            if (!suppliersValue.contains(expectedSupplierId.trim())) {
                                String expectedSupplierName = parentSuppliersMap.get(expectedSupplierId);

                                if (!suppliersValue.contains(expectedSupplierName)) {
                                    csAssert.assertFalse(true, "Expected Parent Supplier: " + expectedSupplierName + " having Id " + expectedSupplierId
                                            + " not found in List Data API Response of Report [" + reportName + "] having Id " + reportId);
                                }
                            }
                        }

                        break;
                    }
                }

                csAssert.assertTrue(recordFound, "Contract Id " + contractId + " not found in ListData API Response of Report [" + reportName +
                        "] having Id " + reportId);

                //Validate Download Excel
                validateReportListingDataDownloadExcel(parentSupplierIdArr, null, reportName, reportId, contractIdColumnId,
                        contractIdColumnName, supplierColumnId, supplierColumnName, csAssert);
            } else {
                csAssert.assertFalse(true, "No Record returned in ListData API for Report [" + reportName + "] having Id " + reportId);
            }
        } else {
            csAssert.assertFalse(true, "ListData API Response for Report [" + reportName + "] having Id " + reportId + " is an Invalid JSON.");
            FileUtils.saveResponseInFile("ReportListData.txt", reportListDataResponse);
        }
    }

    private void validateReportListingDataDownloadExcel(String[] parentSupplierId, String payload, String reportName, int reportId, String contractIdColumnId,
                                                        String contractIdColumnName, String supplierColumnId, String supplierColumnName, CustomAssert csAssert) {
        validateReportListingDataDownloadExcel(parentSupplierId, parentSuppliersMap, payload, reportName, reportId, contractIdColumnId,
                contractIdColumnName, supplierColumnId, supplierColumnName, csAssert);
    }

    private void validateReportListingDataDownloadExcel(String[] parentSupplierId, Map<String, String> parentSuppliersMap, String payload, String reportName, int reportId,
                                                        String contractIdColumnId, String contractIdColumnName, String supplierColumnId,
                                                        String supplierColumnName, CustomAssert csAssert) {
        logger.info("Validating Multi Supplier details in Downloaded Excel for Report [{}] having Id {}", reportName, reportId);

        DownloadReportWithData downloadObj = new DownloadReportWithData();
        Map<String, String> formParam = new HashMap<>();
        formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));

        if (payload == null) {
            String supplierSubPayload = "";

            for (String supplierId : parentSupplierId) {
                supplierSubPayload = supplierSubPayload.concat("{\"id\":" + supplierId + "},");
            }

            supplierSubPayload = supplierSubPayload.substring(0, supplierSubPayload.length() - 1);

            payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + supplierSubPayload + "]},\"filterId\":1,\"filterName\":\"supplier\"}}}," +
                    "\"selectedColumns\":[{\"columnId\":" + contractIdColumnId + ",\"columnQueryName\":\"" + contractIdColumnName +
                    "\"},{\"columnId\":" + supplierColumnId + ",\"columnQueryName\":\"" + supplierColumnName + "\"}]}";
        }

        formParam.put("jsonData", payload);

        HttpResponse downloadResponse = downloadObj.hitDownloadReportWithData(formParam, reportId);
        String excelFilePath = "src/test";
        String excelFileName = "ReportListData_Excel_File_SelectedColumns.xlsx";
        boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, excelFilePath + "/" + excelFileName);

        if (fileDownloaded) {
            List<String> allColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, "Data", 4);
            String supplierColumnNameInExcel = supplierColumnName.equalsIgnoreCase("supplier") ? "SUPPLIER" : "VENDOR NAME";

            if (reportId == 270) {
                supplierColumnNameInExcel = "SUPPLIER";
            }

            if (allColumnNames.contains(supplierColumnNameInExcel)) {
                int supplierIndex = allColumnNames.indexOf(supplierColumnNameInExcel);
                String supplierValueForFirstRecord = XLSUtils.getOneCellValue(excelFilePath, excelFileName, "Data", 4, supplierIndex);

                Long noOfRows = XLSUtils.getNoOfRows(excelFilePath, excelFileName, "Data");
                String supplierValueForLastRecord = XLSUtils.getOneCellValue(excelFilePath, excelFileName, "Data",
                        noOfRows.intValue() - 3, supplierIndex);

                for (String supplierId : parentSupplierId) {
                    String expectedSupplierName = parentSuppliersMap.get(supplierId);

                    boolean valueMatch = (supplierValueForFirstRecord.trim().toLowerCase().contains(expectedSupplierName.trim().toLowerCase()) ||
                            supplierValueForLastRecord.trim().toLowerCase().contains(expectedSupplierName.trim().toLowerCase()));

                    csAssert.assertTrue(valueMatch, "Expected Supplier Name " + expectedSupplierName + " not found in Downloaded Excel for Report [" +
                            reportName + "] having Id " + reportId);

                }
            } else {
                csAssert.assertFalse(true, "Couldn't find Column " + supplierColumnNameInExcel + " in Downloaded Excel for Report [" +
                        reportName + "] having Id " + reportId);
            }

            //Delete Excel
            FileUtils.deleteFile(excelFilePath, excelFileName);
        } else {
            csAssert.assertFalse(true, "Couldn't Download ListData Excel for Multi Supplier Contract Report [" + reportName +
                    "] having Id " + reportId);
        }
    }

    private void validateRelationIdInContractTreeResponse(int entityTypeId, int recordId, String parentSupplierId, CustomAssert csAssert) {
        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("offset", "0");

            String rootSupplierId;

            if (entityTypeId == 61) {
                queryParams.put("hierarchy", "true");
                rootSupplierId = parentSupplierId;
            } else {
                queryParams.put("hierarchy", "false");
                rootSupplierId = "0";
            }

            queryParams.put("rootSupplierId", rootSupplierId);

            logger.info("Validating Relation Id for All Children in Contract Tree V1 Response of EntityTypeId {} and Record Id {} and RootSupplierId {}.",
                    entityTypeId, recordId, rootSupplierId);
            logger.info("Hitting Contract Tree V1 API for EntityTypeId {}, Record Id {} and RootSupplierId {}", entityTypeId, recordId, rootSupplierId);
            String contractTreeResponse = contractTreeDataObj.hitContractTreeListAPIV1(entityTypeId, recordId, "{}", queryParams);

            if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
                JSONObject jsonObj = new JSONObject(contractTreeResponse).getJSONObject("body").getJSONObject("data");
                JSONArray jsonArr = jsonObj.getJSONArray("children");

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    int entityId = jsonObj.getInt("entityId");

                    JSONArray relationsArr = jsonObj.getJSONArray("relationIds");
                    boolean parentSupplierMatch = false;

                    for (int j = 0; j < relationsArr.length(); j++) {
                        String actualSupplierId = relationsArr.get(j).toString();

                        if (actualSupplierId.equalsIgnoreCase(parentSupplierId)) {
                            parentSupplierMatch = true;
                            break;
                        }
                    }

                    csAssert.assertTrue(parentSupplierMatch, "Contract Tree V1 API Response of EntityTypeId " + entityTypeId + " and Record Id " + recordId +
                            " and RootSupplierId " + rootSupplierId + ": Child Contract Record Id " + entityId +
                            " doesn't belong to Parent Supplier Id " + parentSupplierId);
                }
            } else {
                csAssert.assertFalse(true, "Contract Tree V1 API Response for EntityTypeId Id " + entityTypeId + "and Record Id " +
                        recordId + " and RootSupplierId " + rootSupplierId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Relation Id in Contract Tree Response of EntityTypeId " + entityTypeId +
                    " and Record Id " + recordId + " and Parent Supplier Id " + parentSupplierId);
        }
    }

    private boolean isContractIndexed(int contractId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select indexed from contract where id = " + contractId;
            List<List<String>> results = sqlObj.doSelect(query);

            boolean indexed = false;

            if (!results.isEmpty()) {
                String indexedValue = results.get(0).get(0);
                indexed = indexedValue.equalsIgnoreCase("true") || indexedValue.equalsIgnoreCase("t");
            }

            sqlObj.closeConnection();

            return indexed;
        } catch (Exception e) {
            logger.error("Exception while Checking if Contract having Id {} is indexed or not. {}", contractId, e.getMessage());
        }

        return false;
    }

}