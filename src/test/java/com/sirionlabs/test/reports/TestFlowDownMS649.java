package com.sirionlabs.test.reports;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.WeeklyReminderHelper.WeeklyReminderHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

public class TestFlowDownMS649 {
    private static final Logger logger = LoggerFactory.getLogger(TestFlowDownMS649.class);
    private String reportToTest;
    private String stakeHolder;
    private String winSCPConfigFilePath;
    private String winSCPConfigFileName;
    private String remoteHost;
    private String remotePort;
    private String remoteUserName;
    private String remotePassword;
    private String remoteFilePath;
    private String outputFilePath;
    private String columnName;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        String scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("FDScheduleReportConfigFilePath");
        String scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("FDScheduleReportConfigFileName");
        reportToTest = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "allreportname");
        stakeHolder = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "stakeholder");

        columnName = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "columnnames");
        winSCPConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestWeeklyReminderConfigFilePath");
        winSCPConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestWeeklyReminderConfigFileName");
        outputFilePath = ParseConfigFile.getValueFromConfigFile(winSCPConfigFilePath, winSCPConfigFileName, "outputfilepath");
        remoteHost = ParseConfigFile.getValueFromConfigFile(winSCPConfigFilePath, winSCPConfigFileName, "host");
        remotePort = ParseConfigFile.getValueFromConfigFile(winSCPConfigFilePath, winSCPConfigFileName, "port");
        remoteUserName = ParseConfigFile.getValueFromConfigFile(winSCPConfigFilePath, winSCPConfigFileName, "user");
        remotePassword = ParseConfigFile.getValueFromConfigFile(winSCPConfigFilePath, winSCPConfigFileName, "password");
        remoteFilePath = ParseConfigFile.getValueFromConfigFile(winSCPConfigFilePath, winSCPConfigFileName, "remotefilepath");

    }
    @DataProvider
    public Object[][] dataProviderForReportName() {
        List<Object[]> allTestData = new ArrayList<>();
        if (!reportToTest.isEmpty()) {
            String[] allReportListId = reportToTest.split(",");
            for (String reportListId : allReportListId)
                allTestData.add(new Object[]{reportListId.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForReportName")
    public void testMS649(String reportName) {
        CustomAssert customAssert=new CustomAssert();
        logger.info("Excel Report Name {}",reportName);
        try {
            WeeklyReminderHelper weeklyReminderHelper = new WeeklyReminderHelper(remoteHost,Integer.parseInt(remotePort), remoteUserName,remotePassword);
            Boolean fileDownloaded = weeklyReminderHelper.downloadFileFromRemoteServer(remoteFilePath + "/" + reportName+".xlsx", outputFilePath);
            if (!fileDownloaded) {
                customAssert.assertTrue(false, "Couldn't Download Weekly Reminder  excel report  successfully");
                throw new SkipException("Couldn't Download Weekly Reminder  excel report  successfully");
            }

            List<String> allHeadersInColumnSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath,reportName+".xlsx", "Data", 4);
            String[] columnNameToVerify =columnName.split(",");
            List<Integer> columnNumberAccordingToColumnName=new ArrayList<>();
            for (String s : columnNameToVerify) {
                if (allHeadersInColumnSheet.contains(s.trim())) {
                   columnNumberAccordingToColumnName.add(allHeadersInColumnSheet.indexOf(s));
                }
                else {
                    customAssert.assertTrue(false, "Column Name" + s + " Not Found ");
                }
            }
            Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, reportName+".xlsx", "Data");
            List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, reportName+".xlsx", "Data", 4,
                    noOfRows.intValue() - 6);

            for (Integer integer : columnNumberAccordingToColumnName) {
                for (List<String> allRecordsDatum : allRecordsData) {
                    if (!allRecordsDatum.get(integer).contains(stakeHolder)) {
                        customAssert.assertTrue(false, "StakeHolder Name " + stakeHolder + " Not Found In Excel Sheet");
                    }
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false,"Exception While verifying Data In Report "+reportName);
        }
        finally {
            FileUtils.deleteFile(outputFilePath,reportName+".xlsx");
        }
        customAssert.assertAll();
    }
}
