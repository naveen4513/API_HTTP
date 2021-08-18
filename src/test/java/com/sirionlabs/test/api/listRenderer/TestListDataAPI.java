package com.sirionlabs.test.api.listRenderer;

import com.sirionlabs.api.listRenderer.ListDataAPI;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.dto.listRenderer.ListDataDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestListDataAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestListDataAPI.class);

    private String testingType;
    private List<String> allHeaders;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/ListRenderer";
        String dataFileName = "ListDataAPI.xlsx";
        String dataSheet = "Sheet1";

        List<List<String>> allExcelData = XLSUtils.getAllExcelDataIncludingHeaders(dataFilePath, dataFileName, dataSheet);

        allHeaders = allExcelData.get(0);

        int indexOfEnabled = allHeaders.indexOf("Enabled");
        int indexOfTestingType = allHeaders.indexOf("Testing Type");

        List<ListDataDTO> dtoObjectList = new ArrayList<>();

        for (int i = 1; i < allExcelData.size(); i++) {
            if (allExcelData.get(i).get(indexOfEnabled).trim().equalsIgnoreCase("yes")) {
                if (allExcelData.get(i).get(indexOfTestingType).trim().toLowerCase().contains(testingType.toLowerCase())) {
                    ListDataDTO dtoObject = getListDataDTOObject(allExcelData.get(i));

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (ListDataDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "dataProvider")
    public void testAPIFromExcel(ListDataDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String payload = ListDataAPI.getPayload(dtoObject.getEntityTypeId(), dtoObject.getOffset(), dtoObject.getSize(), dtoObject.getOrderByColumnName(),
                    dtoObject.getOrderDirection(), dtoObject.getFilterJson(), dtoObject.getSelectedColumns());

            APIResponse listDataResponse = ListDataAPI.getListDataResponse(ListDataAPI.getApiPath(dtoObject.getListId(), dtoObject.getContractId(), dtoObject.getRelationId(),
                    dtoObject.getVendorId(), dtoObject.getIsFirstCall(), dtoObject.getVersion()), ListDataAPI.getHeaders(), payload);

            Integer responseCode = listDataResponse.getResponseCode();
            String listDataBody = listDataResponse.getResponseBody();

            String expectedStatusCode = dtoObject.getExpectedStatusCode();

            csAssert.assertTrue(expectedStatusCode.equalsIgnoreCase(responseCode.toString()), "Expected Status Code: " + expectedStatusCode +
                    " and Actual Status Code: " + responseCode);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private ListDataDTO getListDataDTOObject(List<String> oneRowData) {
        ListDataDTO dtoObject = null;

        try {
            String testCaseId = oneRowData.get(allHeaders.indexOf("Test Case Id"));
            String description = oneRowData.get(allHeaders.indexOf("Description"));

            String entityName = oneRowData.get(allHeaders.indexOf("Entity Name"));
            String listId = String.valueOf(ConfigureConstantFields.getListIdForEntity(entityName));
            String entityTypeId = String.valueOf(ConfigureConstantFields.getEntityIdByName(entityName));

            String contractId = oneRowData.get(allHeaders.indexOf("Contract Id"));
            String relationId = oneRowData.get(allHeaders.indexOf("Relation Id"));
            String vendorId = oneRowData.get(allHeaders.indexOf("Vendor Id"));
            String version = oneRowData.get(allHeaders.indexOf("Version"));
            String offset = oneRowData.get(allHeaders.indexOf("Offset"));
            String size = oneRowData.get(allHeaders.indexOf("Size"));
            String orderByColumnName = oneRowData.get(allHeaders.indexOf("Order by Column Name"));
            String orderDirection = oneRowData.get(allHeaders.indexOf("Order Direction"));
            String isFirstCall = oneRowData.get(allHeaders.indexOf("isFirstCall"));
            String filterJson = oneRowData.get(allHeaders.indexOf("Filter Json"));
            String selectedColumns = oneRowData.get(allHeaders.indexOf("Selected Columns"));
            String expectedStatusCode = oneRowData.get(allHeaders.indexOf("Expected Status Code"));

            dtoObject = new ListDataDTO(testCaseId, description, listId, contractId, relationId, vendorId, version, offset, size, entityTypeId, orderByColumnName,
                    orderDirection, isFirstCall, filterJson, selectedColumns, expectedStatusCode);
        } catch (Exception e) {
            logger.error("Exception while Getting ListData DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/ListRenderer";
        String dataFileName = "ListDataAPIJson.json";

        List<ListDataDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("Enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("Testing Type").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    ListDataDTO dtoObject = getListDataDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (ListDataDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private ListDataDTO getListDataDTOObjectFromJson(JSONObject jsonObj) {
        ListDataDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("Test Case Id");
            String description = jsonObj.getString("Description");

            String entityName = jsonObj.getString("Entity Name");
            String listId = String.valueOf(ConfigureConstantFields.getListIdForEntity(entityName));
            String entityTypeId = String.valueOf(ConfigureConstantFields.getEntityIdByName(entityName));

            String contractId = jsonObj.getString("Contract Id");
            String relationId = jsonObj.getString("Relation Id");
            String vendorId = jsonObj.getString("Vendor Id");
            String version = jsonObj.getString("Version");
            String offset = jsonObj.getString("Offset");
            String size = jsonObj.getString("Size");
            String orderByColumnName = jsonObj.getString("Order by Column Name");
            String orderDirection = jsonObj.getString("Order Direction");
            String isFirstCall = jsonObj.getString("isFirstCall");
            String filterJson = jsonObj.getString("Filter Json");
            String selectedColumns = jsonObj.getString("Selected Columns");
            String expectedStatusCode = jsonObj.getString("Expected Status Code");

            dtoObject = new ListDataDTO(testCaseId, description, listId, contractId, relationId, vendorId, version, offset, size, entityTypeId, orderByColumnName,
                    orderDirection, isFirstCall, filterJson, selectedColumns, expectedStatusCode);
        } catch (Exception e) {
            logger.error("Exception while Getting ListData DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(ListDataDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String payload = ListDataAPI.getPayload(dtoObject.getEntityTypeId(), dtoObject.getOffset(), dtoObject.getSize(), dtoObject.getOrderByColumnName(),
                    dtoObject.getOrderDirection(), dtoObject.getFilterJson(), dtoObject.getSelectedColumns());

            APIResponse listDataResponse = ListDataAPI.getListDataResponse(ListDataAPI.getApiPath(dtoObject.getListId(), dtoObject.getContractId(), dtoObject.getRelationId(),
                    dtoObject.getVendorId(), dtoObject.getIsFirstCall(), dtoObject.getVersion()), ListDataAPI.getHeaders(), payload);

            Integer responseCode = listDataResponse.getResponseCode();
            String listDataBody = listDataResponse.getResponseBody();

            String expectedStatusCode = dtoObject.getExpectedStatusCode();

            csAssert.assertTrue(expectedStatusCode.equalsIgnoreCase(responseCode.toString()), "Expected Status Code: " + expectedStatusCode +
                    " and Actual Status Code: " + responseCode);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }
}