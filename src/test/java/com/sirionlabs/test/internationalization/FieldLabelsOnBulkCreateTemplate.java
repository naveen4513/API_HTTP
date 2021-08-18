package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FieldLabelsOnBulkCreateTemplate extends TestDisputeInternationalization {
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnCreatePage.class);
    private static Map<String, String> reportIdMap = new HashMap<>();

    public void verifyFieldLabelsOnBulkCreateTemplate(String contracts, int recordId, CustomAssert csAssert) {
        String baseFilePath = "src//test//resources//CommonConfigFiles";
        String reportIdFile = "BulkCreateExcels.cfg";
        reportIdMap = ParseConfigFile.getAllConstantProperties(baseFilePath, reportIdFile, "bulk excel ids");

        for ( Map.Entry ReportID : reportIdMap.entrySet() ) {
            try {

                logger.info("Validating Field Labels on Bulk Create Excel Template.");
                Download downloadObj = new Download();
                String fileName = String.valueOf(ReportID.getKey())+".xlsm";
                String templateId= String.valueOf(ReportID.getValue());
                int parentEntityTypeId= 61;
                int parentId=recordId;
                String outputFilePath="D:\\java-api-framework\\src\\test\\output\\";

                downloadObj.hitDownload(outputFilePath,fileName ,Integer.parseInt(templateId) , parentEntityTypeId, parentId);

                //List<List<String>> data = XLSUtils.getAllExcelDataColumnWiseWithoutSheetName(outputFilePath, String.valueOf(ReportID.getKey())+".xlsm");
                getExcelDataOfEntitySheet("D:\\java-api-framework\\src\\test\\output", fileName,"Invoice",csAssert);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Field Labels on Bulk Create Excel Template."+ e.getMessage());
            }
        }
    }

    private static void getExcelDataOfEntitySheet(String filePath, String fileName,String entityName,CustomAssert csAssert) throws IOException {
        FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        int sheetNumber = workbook.getNumberOfSheets();
        for(int i=0;i<sheetNumber;i++) {
            String sheet = workbook.getSheetAt(i).getSheetName();
            if (!sheet.toLowerCase().contains("Master Data".toLowerCase()) && !sheet.toLowerCase().contains("Macro Messages".toLowerCase()) && !sheet.toLowerCase().contains("Stakeholders".toLowerCase()) && !sheet.toLowerCase().contains("Instructions".toLowerCase())
                    && !sheet.toLowerCase().contains("Information".toLowerCase())) {
                List<List<String>> allData = new ArrayList<>();
                List<String> oneRowData = XLSUtils.getExcelDataOfOneRow(filePath, fileName, sheet, 1);
                allData.add(oneRowData);
                List<String> secondRowData = XLSUtils.getExcelDataOfOneRow(filePath, fileName, sheet, 5);
                allData.add(secondRowData);
                List<String> thirdRowData = XLSUtils.getExcelDataOfOneRow(filePath, fileName, sheet, 6);
                allData.add(thirdRowData);
                dataLabelValidations(allData, fileName, sheet.toString(), csAssert);
            }
            if (sheet.toLowerCase().contains("Instructions".toLowerCase())) {
                List<String> instructions = new ArrayList<>();
                Cell C1 = workbook.getSheetAt(0).getRow(0).getCell(5);
                instructions.add(C1.getStringCellValue());
                Cell C2 = workbook.getSheetAt(0).getRow(0).getCell(6);
                instructions.add(C2.getStringCellValue());
                Cell C3 = workbook.getSheetAt(0).getRow(1).getCell(5);
                instructions.add(C3.getStringCellValue());
                Cell C4 = workbook.getSheetAt(0).getRow(2).getCell(5);
                instructions.add(C4.getStringCellValue());
                dataLabelValidation(instructions, workbook.getSheetAt(0).getSheetName(), fileName, csAssert);
            }
            if(sheet.toLowerCase().contains("Information".toLowerCase())) {
                List<String> information = new ArrayList<>();
                Cell C1 = workbook.getSheetAt(1).getRow(0).getCell(0);
                information.add(C1.getStringCellValue());
                Cell C2 = workbook.getSheetAt(1).getRow(2).getCell(0);
                information.add(C2.getStringCellValue());
                Cell C3 = workbook.getSheetAt(1).getRow(3).getCell(0);
                information.add(C3.getStringCellValue());
                Cell C4 = workbook.getSheetAt(1).getRow(4).getCell(0);
                information.add(C4.getStringCellValue());
                dataLabelValidation(information, workbook.getSheetAt(1).getSheetName(), fileName, csAssert);
            }
        }
    }

    private static void dataLabelValidations(List<List<String>> data,String sheetName,String fileName,CustomAssert csAssert){
        for( List<String> cell:data){
            for(String Label:cell){
                if (Label.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                    csAssert.assertTrue(false, "Field Label: [" + Label.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under "+sheetName+" of download excel for "+fileName);
                } else {
                    csAssert.assertTrue(true, "Field Label: [" + Label.toLowerCase() + "] does not contain: [" + expectedPostFix.toLowerCase() + "] under "+sheetName+" of download excel for "+fileName);
                }
            }
        }
    }

    private static void dataLabelValidation(List<String> data,String sheetName,String fileName,CustomAssert csAssert){
            for(String Label:data){
                if (Label.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                    csAssert.assertTrue(false, "Field Label: [" + Label.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under "+sheetName+" of download excel for "+fileName);
                } else {
                    csAssert.assertTrue(true, "Field Label: [" + Label.toLowerCase() + "] does not contain: [" + expectedPostFix.toLowerCase() + "] under "+sheetName+" of download excel for "+fileName);
                }
            }
    }
}

