package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.http.HttpResponse;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class TestCDRStatusTransitionAndLeadTimeReports {
    private final static Logger logger = LoggerFactory.getLogger(TestCDRStatusTransitionAndLeadTimeReports.class);
    private String configFilePath;
    private String configFileName;
    private String outputFilePath;
    private String reportType;
    private String entityName;
    private String submissionDateFrom;
    private String submissionDateTo;
    private String excelPath;
    private String excelFileName;
    private String uniqueStr = "";
    static boolean supplierFlag = false;


    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("StatusTransitionReportFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("StatusTransitionReportFileName");
        outputFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "outputfilepath");
        entityName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entityname");
    }

    @Test
    public void C152923() {
        CustomAssert customAssert = new CustomAssert();
        reportType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "status_transition_report", "reporttype");
        submissionDateFrom = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "status_transition_report", "submissiondatefrom");
        submissionDateTo = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "status_transition_report", "submissiondateto");
        try {
            List<Date> excelDates = getDateFromExcel(customAssert);

            Date fromDate = getDate(submissionDateFrom, customAssert);
            Date toDate = getDate(submissionDateTo, customAssert);

            for (Date excelDate : excelDates) {
                if (excelDate.after(fromDate) && excelDate.before(toDate)) {
                    logger.info("Valid date is present in the excel sheet : {}", excelDate);
                    customAssert.assertTrue(true, "Valid date is present in the excel sheet : " + excelDate);
                } else {
                    logger.error("Invalid date is present in the excel sheet : {}", excelDate);
                    customAssert.assertTrue(false, "Invalid date is present in the excel sheet : " + excelDate);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while Validating data from Report listing {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while Validating data from Report listing " + e.getMessage());
        }
        customAssert.assertAll();
    }

    @Test
    public void C152924() {
        CustomAssert customAssert = new CustomAssert();
        reportType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "status_transition_report", "reporttype");
        submissionDateFrom = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "status_transition_report", "submissiondatefrom");
        submissionDateTo = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "status_transition_report", "submissiondateto");
        try {
            List<Date> excelDates = getAndCompareFileDataForMultiLingualOnOff(customAssert);
            Date fromDate = getDate(submissionDateFrom, customAssert);
            Date toDate = getDate(submissionDateTo, customAssert);

            for (Date excelDate : excelDates) {
                if (excelDate.after(fromDate) && excelDate.before(toDate)) {
                    logger.info("Valid date is present in the excel sheet : {}", excelDate);
                    customAssert.assertTrue(true, "Valid date is present in the excel sheet : " + excelDate);
                } else {
                    logger.error("Invalid date is present in the excel sheet : {}", excelDate);
                    customAssert.assertTrue(false, "Invalid date is present in the excel sheet : " + excelDate);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while Validating data from Report listing {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while Validating data from Report listing " + e.getMessage());
        }
        customAssert.assertAll();
    }

    @Test
    public void C152925() {
        CustomAssert customAssert = new CustomAssert();
        reportType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lead_time_report", "reporttype");
        submissionDateFrom = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lead_time_report", "submissiondatefrom");
        submissionDateTo = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lead_time_report", "submissiondateto");
        try {
            List<Date> excelDates = getAndCompareFileDataForMultiLingualOnOff(customAssert);
            Date fromDate = getDate(submissionDateFrom, customAssert);
            Date toDate = getDate(submissionDateTo, customAssert);

            for (Date excelDate : excelDates) {
                if (excelDate.after(fromDate) && excelDate.before(toDate)) {
                    logger.info("Valid date is present in the excel sheet : {}", excelDate);
                    customAssert.assertTrue(true, "Valid date is present in the excel sheet : " + excelDate);
                } else {
                    logger.error("Invalid date is present in the excel sheet : {}", excelDate);
                    customAssert.assertTrue(false, "Invalid date is present in the excel sheet : " + excelDate);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while Validating data from Report listing {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while Validating data from Report listing " + e.getMessage());
        }
        customAssert.assertAll();
    }

    @Test
    public void C152916() {
        CustomAssert customAssert = new CustomAssert();
        reportType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lead_time_report", "reporttype");
        try {
            getAndCompareFileDataForMultiLingualOnOff(customAssert);
            if (!supplierFlag) {
                logger.error("Supplier column does not exist in the report.");
                customAssert.assertTrue(false, "Supplier column does not exist in the report.");
            } else {
                logger.info("Supplier column exists in the report.");
            }
        } catch (Exception e) {
            logger.error("Exception occurred while Validating data from Report listing {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while Validating data from Report listing " + e.getMessage());
        }
        customAssert.assertAll();
    }

    @Test
    public void C152964() {
        CustomAssert customAssert = new CustomAssert();
        reportType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lead_time_report", "reporttype");
        int counter = 0;
        try {
            int reportId = getReportId(reportType, customAssert);
            boolean dataDownloaded = downloadData(reportId,false, outputFilePath, reportType, entityName, customAssert);
            if (dataDownloaded) {
                List<String> suppliers = readExcelFile(customAssert).get("Suppliers");
                if (suppliers.size() > 0) {
                    for (String supplier : suppliers) {
                        try {
                            String[] supplierArray = supplier.split(",");
                            if(supplierArray.length>1){
                                counter = counter + 1;
                            }else{
                                continue;
                            }
                            if(counter>0){
                                logger.info(counter + " entries have multiple suppliers.");
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                } else {
                    logger.error("Suppliers Data is not present in the excel sheet.");
                    customAssert.assertTrue(false, "Suppliers Data is not present in the excel sheet.");
                }
            } else {
                logger.error("Report could not be downloaded.");
                customAssert.assertTrue(false, "Report could not be downloaded.");
            }
        } catch (Exception e) {

        }
        customAssert.assertAll();
    }

    public int getReportId(String reportType, CustomAssert customAssert) {
        int reportId = 0;
        try {
            ReportRendererListData reportRendererListData = new ReportRendererListData();
            String listingResponse = reportRendererListData.getReportListReportJSON("");
            if (listingResponse != null) {
                JSONArray jsonArray = new JSONArray(listingResponse);
                String listMetaDataJson = null;
                for (int index = 0; index < jsonArray.length(); index++) {
                    JSONObject job = jsonArray.getJSONObject(index);
                    if (entityName.equalsIgnoreCase(job.getString("name"))) {
                        listMetaDataJson = job.get("listMetaDataJsons").toString();
                        break;
                    }
                }
                JSONArray jsonArrayMetaData = new JSONArray(listMetaDataJson);
                for (int index = 0; index < jsonArrayMetaData.length(); index++) {
                    JSONObject job = jsonArrayMetaData.getJSONObject(index);
                    String entityReportName = job.getString("name");
                    if (entityReportName != null) {
                        if (entityReportName.contains(reportType)) {
                            reportId = job.getInt("id");
                            logger.info("Report for {} is {}", entityReportName, reportId);
                            break;
                        }
                    } else {
                        logger.error("Report Name of the entity is null");
                        customAssert.assertTrue(false, "Report Name of the entity is null");
                    }
                }
            } else {
                logger.error("Listing Response on Report Listing is null");
                customAssert.assertTrue(false, "Listing Response on Report Listing is null");
            }
        } catch (Exception e) {
            logger.error("Exception occurred while fetching ReportId from Report listing {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while fetching ReportId from Report listing " + e.getMessage());
        }
        return reportId;
    }

    public boolean downloadData(int reportId, boolean submissionDate, String outputFilePath, String reportType, String entityName, CustomAssert customAssert) {
        DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
        HashMap<String, String> formDataPayload = new HashMap<>();
        Boolean status = false;
        String jsonData = "";
        try {
            formDataPayload.put("_csrf_token", "null");
            if (submissionDate) {
                if (reportType.equalsIgnoreCase("Status Transition Report"))
                    jsonData = "{\"filterMap\":{\"entityTypeId\":160,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"25\":{\"filterId\":\"25\",\"filterName\":\"submissionDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null,\"start\":\"" + submissionDateFrom + "\",\"end\":\"" + submissionDateTo + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}}}";
                else if (reportType.equalsIgnoreCase("Lead Time"))
                    jsonData = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"25\":{\"filterId\":\"25\",\"filterName\":\"submissionDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null,\"start\":\"" + submissionDateFrom + "\",\"end\":\"" + submissionDateTo + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}}}";
            }else{
                jsonData = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
            }

            formDataPayload.put("jsonData", jsonData);
            HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formDataPayload, reportId);
            Thread.sleep(5000);

            String outputFile = null;
            FileUtils fileUtil = new FileUtils();
            Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, reportType);
            Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + reportType + "/", entityName);
            if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
                outputFile = excelPath + excelFileName;
                status = fileUtil.writeResponseIntoFile(response, outputFile);
                if (status) {
                    logger.info("DownloadListWithData file generated at {}", outputFile);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while downloading Report {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while downloading Report " + e.getMessage());
        }
        return status;
    }

    public HashMap<String, List<String>> readExcelFile(CustomAssert customAssert) {
        HashMap<String, List<String>> excelData = new HashMap<>();
        List<String> submissionDates = new ArrayList<>();
        List<String> suppliers = new ArrayList<>();
        supplierFlag = false;
        try {
            File file = new File(excelPath + excelFileName);
            FileInputStream fis = new FileInputStream(file);
            ZipSecureFile.setMinInflateRatio(-1.0d);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();
            int totalCols = sheet.getRow(0).getLastCellNum();
            DataFormatter dataFormatter = new DataFormatter();

            Object[][] dataOfExcelSheet = new Object[totalRows][totalCols];
            int rowId = 0;

            for (int rowIndex = rowId; rowIndex < totalRows - 1; rowIndex++) {
                for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                    try {
                        Cell cell = sheet.getRow(rowIndex).getCell(colIndex);
                        dataOfExcelSheet[rowIndex][colIndex] = dataFormatter.formatCellValue(cell);
                    } catch (Exception ex) {
                        dataOfExcelSheet[rowIndex][colIndex] = null;
                    }
                }
            }

            for (int index = 0; index < totalRows - 1; index++) {
                try {
                    if (dataOfExcelSheet[index][0].toString().equalsIgnoreCase("id")) {
                        rowId = index;
                        break;
                    }
                } catch (Exception ex) {
                    continue;
                }
            }

            int rsdCol = 0;
            int supCol = 0;
            boolean flag = false;
            for (int rowIndex = rowId; rowIndex < totalRows - 1; rowIndex++) {
                for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                    String value = dataOfExcelSheet[rowIndex][colIndex].toString();
                    if (value.equalsIgnoreCase("REQUEST SUBMISSION DATE")) {
                        rsdCol = colIndex;
                        flag = true;
                    } else if (value.equalsIgnoreCase("SUPPLIERS")) {
                        supCol = colIndex;
                        supplierFlag = true;
                    }
                }
                if (flag)
                    break;
            }
            for (int rowIndex = rowId + 1; rowIndex < totalRows - 1; rowIndex++) {
                submissionDates.add(dataOfExcelSheet[rowIndex][rsdCol].toString());
                suppliers.add(dataOfExcelSheet[rowIndex][supCol].toString());
            }
        } catch (Exception e) {
            logger.error("Exception while reading the excel file {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while reading the excel file " + e.getMessage());
        }
        excelData.put("SubmissionDates", submissionDates);
        excelData.put("Suppliers", suppliers);
        return excelData;
    }

    public String convertExcelDate(String dateInExcel, CustomAssert customAssert) {
        String dateUpdated = "";
        String yyExcel = dateInExcel.replaceAll("[^\\d.]", "-").split("-")[0];
        String mmExcel = dateInExcel.replaceAll("[0-9]", "").toLowerCase();
        String ddExcel = dateInExcel.toLowerCase().replaceAll(yyExcel, "").replace(mmExcel, "");
        String month = "";

        try {
            if ("january".contains(mmExcel))
                month = "01";
            else if ("february".contains(mmExcel))
                month = "02";
            else if ("march".contains(mmExcel))
                month = "03";
            else if ("april".contains(mmExcel))
                month = "04";
            else if ("may".contains(mmExcel))
                month = "05";
            else if ("june".contains(mmExcel))
                month = "06";
            else if ("july".contains(mmExcel))
                month = "07";
            else if ("august".contains(mmExcel))
                month = "08";
            else if ("september".contains(mmExcel))
                month = "09";
            else if ("october".contains(mmExcel))
                month = "10";
            else if ("november".contains(mmExcel))
                month = "11";
            else if ("december".contains(mmExcel))
                month = "12";
            dateUpdated = month + "-" + ddExcel + "-" + yyExcel;
            logger.info("Date found in the report is {}", dateUpdated);
        } catch (Exception e) {
            logger.error("Exception while validating the dates {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while validating the dates " + e.getMessage());
        }
        return dateUpdated;
    }

    public Date getDate(String date, CustomAssert customAssert) {
        Date formattedDate = null;
        try {
            formattedDate = new SimpleDateFormat("MM/dd/yyyy").parse(date.replaceAll("-", "/"));
        } catch (Exception e) {
            logger.error("Exception while converting the string in Date format {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while converting the string in Date format " + e.getMessage());
        }
        return formattedDate;
    }

    public List<Date> getDateFromExcel(CustomAssert customAssert) {
//        uniqueStr = DateUtils.getCurrentTimeStamp().replaceAll(" ","");
        excelPath = outputFilePath + "/" + reportType + "/" + entityName + "/";
        excelFileName = entityName + " - " + reportType + uniqueStr + ".xlsx";
        List<Date> listOfDates = new ArrayList<>();

        try {
            int reportId = getReportId(reportType, customAssert);
            if (("" + reportId) != null) {
                customAssert.assertTrue(true, "Fetched the report id " + reportId);
                boolean status = downloadData(reportId,true, outputFilePath, reportType, entityName,customAssert);
                if (status) {
                    logger.info("Report is Downloaded");
                } else {
                    logger.error("Report did not download");
                    customAssert.assertTrue(false, "File not downloaded");
                }

                List<String> submissionDates = readExcelFile(customAssert).get("SubmissionDates");
                if (submissionDates.size() == 0) {
                    logger.info("Data does not exist for the given time period");
                    File excelFile = new File(excelPath + excelFileName);
                    if (excelFile.delete()) {
                        logger.info("Report {} is deleted.", excelFileName);
                    } else {
                        logger.error("Report {} is not deleted.", excelFileName);
                        customAssert.assertTrue(false, "Report " + excelFileName + " is not deleted.");
                    }
                } else {
                    boolean flag = false;
                    for (String submissionDate : submissionDates) {
                        if (!submissionDate.equalsIgnoreCase("")) {
                            Date excelDate = getDate(convertExcelDate(submissionDate, customAssert), customAssert);
                            listOfDates.add(excelDate);
                            flag = true;
                        } else {
                            continue;
                        }
                    }
                    if (flag) {
                        File excelFile = new File(excelPath + excelFileName);
                        if (excelFile.delete()) {
                            logger.info("Report {} is deleted.", excelFileName);
                        } else {
                            logger.error("Report {} is not deleted.", excelFileName);
                            customAssert.assertTrue(false, "Report " + excelFileName + " is not deleted.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while Validating data from Report listing {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while Validating data from Report listing " + e.getMessage());
        }
        return listOfDates;
    }

    public List<Date> getAndCompareFileDataForMultiLingualOnOff(CustomAssert customAssert) {
        List<Date> listOfDatesOnTrue = null;
        List<Date> listOfDatesOnFalse;
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        try {
            String clientId = ConfigureEnvironment.getEnvironmentProperty("client_id");
            String sqlQuery = "select multilanguage_supported from public.client WHERE id = " + clientId + ";";
            List<List<String>> resultList = postgreSQLJDBC.doSelect(sqlQuery);
            String result = "";
            if (!resultList.isEmpty())
                result = resultList.get(0).get(0).toLowerCase();

            if ("true".contains(result)) {
                listOfDatesOnTrue = getDateFromExcel(customAssert);
                String updateQuery = "UPDATE public.client SET multilanguage_supported = false WHERE id = " + clientId + ";";
                postgreSQLJDBC.doSelect(updateQuery);
                listOfDatesOnFalse = getDateFromExcel(customAssert);
                if (listOfDatesOnTrue.equals(listOfDatesOnFalse)) {
                    logger.info("Data is same for Multilingual On/Off");
                } else {
                    logger.error("Data is not the same for Multilingual On/Off.");
                    customAssert.assertTrue(false, "Data is not the same for Multilingual On/Off.");
                }
                String resetQuery = "UPDATE public.client SET multilanguage_supported = true WHERE id = " + clientId + ";";
                postgreSQLJDBC.doSelect(resetQuery);
            } else if ("false".contains(result)) {
                listOfDatesOnFalse = getDateFromExcel(customAssert);
                String updateQuery = "UPDATE public.client SET multilanguage_supported = true WHERE id = " + clientId + ";";
                postgreSQLJDBC.doSelect(updateQuery);
                listOfDatesOnTrue = getDateFromExcel(customAssert);
                if (listOfDatesOnTrue.equals(listOfDatesOnFalse)) {
                    logger.info("Data is same for Multilingual On/Off");
                } else {
                    logger.error("Data is not the same for Multilingual On/Off.");
                    customAssert.assertTrue(false, "Data is not the same for Multilingual On/Off.");
                }
                String resetQuery = "UPDATE public.client SET multilanguage_supported = false WHERE id = " + clientId + ";";
                postgreSQLJDBC.doSelect(resetQuery);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while executing the SQL Query {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while executing the SQL query " + e.getMessage());
        } finally {
            try {
                postgreSQLJDBC.closeConnection();
                logger.info("Connection {} is closed now.", postgreSQLJDBC);
            } catch (Exception e) {
                logger.error("Exception occurred while closing the connection {}", e.getMessage());
                customAssert.assertTrue(false, "Exception occurred while closing the connection " + e.getMessage());
            }
        }
        return listOfDatesOnTrue;
    }
}