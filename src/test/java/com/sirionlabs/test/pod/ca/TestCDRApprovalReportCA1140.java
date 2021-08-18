package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.report.ReportRendererListConfigure;
import com.sirionlabs.api.reportRenderer.DownloadApprovalReport;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCDRApprovalReportCA1140 {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRApprovalReportCA1140.class);

    private String[] newFieldsQueryName = {"sapsourcingid", "vendorcontractingparty", "eccid", "parentsupplierid", "recipientcliententities", "contractingcliententities"};

    /*
    TC-C91016: Verify New Fields in CDR Approval Report
    TC-C91017
    TC-C91018
     */
    @Test
    public void testC91016() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C91016: Verify New Fields in CDR Approval Report.");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contract draft request");
            JSONObject jsonObj = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0);

            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            String idValue = jsonObj.getJSONObject(idColumn).getString("value");
            int cdrId = ListDataHelper.getRecordIdFromValue(idValue);

            String filePath = "src/test";
            String fileName = "CDR Approval Report";

            boolean reportDownloaded = DownloadApprovalReport.downloadApprovalReport(filePath, fileName, cdrId);

            if (reportDownloaded) {
                Long noOfRows = XLSUtils.getNoOfRows(filePath, fileName + ".xlsx", "Contract Requests - Approval Re");
                List<String> allColumns = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(filePath, fileName + ".xlsx",
                        "Contract Requests - Approval Re", 0, 4, noOfRows.intValue());

                String listConfigureResponse = ReportRendererListConfigure.getReportListConfigureResponse(426);

                jsonObj = new JSONObject(listConfigureResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("ecxelColumns");

                Map<String, Map<String, String>> allExcelColumnsMap = new HashMap<>();

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    Map<String, String> fieldMap = new HashMap<>();

                    String queryName = jsonObj.getString("queryName");
                    fieldMap.put("name", jsonObj.getString("name"));
                    fieldMap.put("deleted", String.valueOf(jsonObj.getBoolean("deleted")));

                    allExcelColumnsMap.put(queryName, fieldMap);
                }

                for (String fieldQueryName : newFieldsQueryName) {
                    if (allExcelColumnsMap.containsKey(fieldQueryName)) {
                        String fieldName = allExcelColumnsMap.get(fieldQueryName).get("name");
                        String deleted = allExcelColumnsMap.get(fieldQueryName).get("deleted");

                        if (deleted.equalsIgnoreCase("false")) {
                            if (!allColumns.contains(fieldName)) {
                                csAssert.assertFalse(true, "Field " + fieldName + " not found in CDR Approval Report of CDR Id " + cdrId);
                            }
                        }
                    } else {
                        csAssert.assertFalse(true, "Field having QueryName " + fieldQueryName +
                                " not found in Excel Columns of List Configure API Response.");
                    }
                }

                FileUtils.deleteFile(filePath, fileName + ".xlsx");
            } else {
                csAssert.assertFalse(true, "Couldn't Download CDR Approval Report for CDR Id " + cdrId);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C91016: " + e.getMessage());
        }

        csAssert.assertAll();
    }
}