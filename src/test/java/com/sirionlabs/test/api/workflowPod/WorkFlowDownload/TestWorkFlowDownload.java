package com.sirionlabs.test.api.workflowPod.WorkFlowDownload;

import com.sirionlabs.api.WorkFlowDownload.WorkflowDownload;
import com.sirionlabs.api.WorkFlowDownload.WorkflowIdAPI;
import com.sirionlabs.dto.WorkFlowDownload.WorkFlowDownloadDto;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.HttpResponse;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestWorkFlowDownload {
    private final static Logger logger = LoggerFactory.getLogger(TestWorkFlowDownload.class);
    private String testingType;
    private String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkFlowDownload";
    private String dataFileName = "WorkFlowDownloadData.json";
    private String outputFilePath;
    private String outputFileName;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();
        List<WorkFlowDownloadDto> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
        JSONArray jsonArr = new JSONArray(allJsonData);
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkFlowDownloadDto dtoObject = getWorkFlowDownloadDtoObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }

        }

        for (WorkFlowDownloadDto dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkFlowDownloadDto getWorkFlowDownloadDtoObjectFromJson(JSONObject jsonObj) {
        WorkFlowDownloadDto dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            String shortCodeId = jsonObj.getString("shortCodeId");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");
            String expectedResponseMessage = jsonObj.getString("expectedResponseMessage");
            String status = jsonObj.getString("status");
            dtoObject = new WorkFlowDownloadDto(testCaseId, description, shortCodeId, expectedStatusCode, expectedResponseMessage, status);
        } catch (Exception e) {
            logger.error("Exception while Getting Work Flow Download DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(WorkFlowDownloadDto dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();
        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
            String expectedErrorMessage = dtoObject.getExpectedResponseMessage();
            String shortCodeId = dtoObject.getShortCodeId();
            String expectedStatusCode = dtoObject.getExpectedStatusCode();
            String status = dtoObject.getStatus();
            APIResponse apiResponse = WorkflowIdAPI.getResponse(shortCodeId);
            int actualResponseCode = apiResponse.getResponseCode();
            if (expectedErrorMessage != null && !expectedErrorMessage.isEmpty()) {

                String actualResponseBody = apiResponse.getResponseBody();

                JSONObject jsonObject = new JSONObject(actualResponseBody);
                String actualStatus = jsonObject.getString("status");

                String actualResponseMessage = jsonObject.getString("errorMessage");
                csAssert.assertTrue(expectedErrorMessage.equalsIgnoreCase(actualResponseMessage), "Actual error message " + actualResponseMessage + "and expected error message" + expectedErrorMessage + " are different");

                csAssert.assertTrue(status.equalsIgnoreCase(actualStatus), "status { " + status + " } and actual status { " + actualStatus + " } are different");
                csAssert.assertTrue(actualResponseCode == Integer.parseInt(expectedStatusCode), "actualResponseCode { " + actualResponseCode + " } and expectedStatusCode { " + expectedStatusCode + " } are different");
            } else {

                csAssert.assertTrue(actualResponseCode == Integer.parseInt(expectedStatusCode), "actual response code " + actualResponseCode + " and  expected response code " + expectedStatusCode + "different");

                String actualResponseBody = apiResponse.getResponseBody();
                JSONObject jsonObject = new JSONObject(actualResponseBody);

                String actualStatus = jsonObject.getString("status");
                csAssert.assertTrue(status.equalsIgnoreCase(actualStatus), "status { " + status + " } and actual status { " + actualStatus + " } are different");

                String workFlowId = jsonObject.getString("workFlowId");
                String entityId = jsonObject.getString("entityId");

                csAssert.assertTrue(shortCodeId.equalsIgnoreCase(entityId), "Short Code Id {" + shortCodeId + "}And Entity Id " + entityId + "Are Different");
                HttpResponse httpResponse = WorkflowDownload.getResponse(workFlowId);

                outputFilePath = "src/test/output";
                outputFileName = "WorkFlow" + ".xlsx";

                FileUtils fileUtil = new FileUtils();
                boolean fileDownloaded = fileUtil.writeResponseIntoFile(httpResponse, outputFilePath + "/" + outputFileName);

                if (!fileDownloaded) {
                    csAssert.assertTrue(false, "Couldn't Download Workflow");
                    throw new SkipException("Couldn't Download Workflow  for entityId[" + entityId + "] and Work flow id " + workFlowId);
                }
                List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, this.getSheetName(outputFilePath, outputFileName), 1);
                if (allHeadersInExcelDataSheet.isEmpty()) {
                    csAssert.assertTrue(false, "Downloaded File is InValid workFlow");
                }
                Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, outputFileName, this.getSheetName(outputFilePath, outputFileName));
                List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, outputFileName, this.getSheetName(outputFilePath, outputFileName), 2,
                        noOfRows.intValue());
                if (allRecordsData.isEmpty()) {
                    throw new SkipException("Couldn't get All Records Data from Work Flow  Sheet.");
                }
            }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        } finally {
            FileUtils.deleteFile(outputFilePath, outputFileName);
        }
        csAssert.assertAll();
    }

    private String getSheetName(String filePath, String fileName) throws IOException {
        FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
        ZipSecureFile.setMinInflateRatio(-1.0d);
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        return workbook.getSheetName(0);
    }

}
