package com.sirionlabs.test.reports;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestRevertSupplierContract {

    private final static Logger logger = LoggerFactory.getLogger(TestRevertSupplierContract.class);
    private String dataFilePath = "src/test/resources/TestConfig/ScheduleReportFlowDownTestData";
    private String dataFileNameSupplier = "ScheduleReportFlowDownTestDataSupplier.json";
    private String dataFileNameContract = "ScheduleReportFlowDownTestDataContract.json";

    @DataProvider
    public Object[][] dataProviderJsonSupplier() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();
        if (FileUtils.fileExists(dataFilePath, dataFileNameSupplier)) {
            String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileNameSupplier);
            JSONArray jsonArr = new JSONObject(allJsonData).names();
            for (int i = 0; i < jsonArr.length(); i++) {
                String jsonObj = new JSONObject(allJsonData).getString(jsonArr.getString(i));
                allTestData.add(new Object[]{jsonObj, jsonArr.getString(i)});
            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderJsonSupplier")
    public void revertSupplier(String jsonData, String entityId) {

        logger.info("*************Hitting Edit API For entity Supplier ******************");
        logger.info("Entity Id {}", entityId);
        CustomAssert customAssert = new CustomAssert();
        try {
            Edit edit = new Edit();
            if (ParseJsonResponse.validJsonResponse(jsonData)) {
                edit.hitEdit("suppliers", jsonData);
                String editDataJsonStr = edit.getEditDataJsonStr();
                if (ParseJsonResponse.validJsonResponse(editDataJsonStr)) {
                    String result = new JSONObject(editDataJsonStr).getJSONObject("header").getJSONObject("response").getString("status");
                    if (!result.equalsIgnoreCase("success")) {
                        customAssert.assertTrue(false, "Supplier is not edit");
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception while Editing StackHolder for  Supplier " + e.getMessage());

        } finally {
            FileUtils.deleteFile(dataFilePath, dataFileNameSupplier);
        }
        customAssert.assertAll();
    }

    @DataProvider
    public Object[][] dataProviderJsonContract() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();
        if (FileUtils.fileExists(dataFilePath, dataFileNameContract)) {
            String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileNameContract);
            JSONArray jsonArr = new JSONObject(allJsonData).names();
            for (int i = 0; i < jsonArr.length(); i++) {
                String jsonObj = new JSONObject(allJsonData).getString(jsonArr.getString(i));
                allTestData.add(new Object[]{jsonObj, jsonArr.getString(i)});
            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderJsonContract")
    public void revertContract(String jsonData, String entityId) {
        logger.info("*************Hitting Edit API For entity Contract ******************");
        logger.info("Entity Id {}", entityId);
        CustomAssert customAssert = new CustomAssert();
        try {
            Edit edit = new Edit();
            if (ParseJsonResponse.validJsonResponse(jsonData)) {
                edit.hitEdit("contracts", jsonData);
                String editDataJsonStr = edit.getEditDataJsonStr();
                if (ParseJsonResponse.validJsonResponse(editDataJsonStr)) {
                    String result = new JSONObject(editDataJsonStr).getJSONObject("header").getJSONObject("response").getString("status");
                    if (!result.equalsIgnoreCase("success")) {
                        customAssert.assertTrue(false, "Contract is not edit");
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception while Editing StackHolder for  Supplier " + e.getMessage());

        } finally {
            FileUtils.deleteFile(dataFilePath, dataFileNameContract);
        }
        customAssert.assertAll();
    }
}
