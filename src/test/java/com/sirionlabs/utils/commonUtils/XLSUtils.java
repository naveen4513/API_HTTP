package com.sirionlabs.utils.commonUtils;

import com.monitorjbl.xlsx.StreamingReader;
import com.sirionlabs.config.ConfigureEnvironment;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;

import javax.mail.MessagingException;
import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.Math.abs;


/**
 * Created by shivashish on 28/6/17.
 */


public class XLSUtils {
    private final static Logger logger = LoggerFactory.getLogger(XLSUtils.class);
    public String filePath;
    public String fileName;
    private List<String> sheetNames;
    private Workbook workBook = null;
    private Sheet sheet = null;
    private Row row = null;
    private String xlsDateFormate;

    private Cell cell = null;        //Changes by sarthak
    private java.awt.color.ICC_ProfileRGB customColor;

    // Xls_Reader Constructor
    public XLSUtils(String filePathWithName) throws IOException {
        //Create an object of File class to open xlsx file
        File file = new File(filePathWithName);
        //Create an object of FileInputStream class to read excel file
        FileInputStream inputStream = new FileInputStream(file);
        //Find the file extension by splitting  file name in substring and getting only extension name
        String fileExtensionName = filePathWithName.substring(filePathWithName.indexOf("."));
        ZipSecureFile.setMinInflateRatio(-1.0d);
        //Check condition if the file is xlsx file
        if (fileExtensionName.equals(".xlsx")) {
            workBook = new XSSFWorkbook(inputStream);
        } else if (fileExtensionName.equals(".xlsm")) {
            workBook = new XSSFWorkbook(inputStream);
        }
        //Check condition if the file is xls file
        else if (fileExtensionName.equals(".xls")) {
            workBook = new HSSFWorkbook(inputStream);
        }
        sheetNames = readAllSheetsName(workBook);

    } //added constructor by srijan

    // Xls_Reader Constructor
    public XLSUtils(String filePath, String fileName) throws IOException {
        //logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        this.filePath = filePath;
        this.fileName = fileName;

        //Create an object of File class to open xlsx file
        File file = new File(filePath + "//" + fileName);
        //Create an object of FileInputStream class to read excel file
        FileInputStream inputStream = new FileInputStream(file);
        //Find the file extension by splitting  file name in substring and getting only extension name
        String fileExtensionName = fileName.substring(fileName.indexOf("."));
        //Check condition if the file is xlsx file
        if (fileExtensionName.equals(".xlsx")) {
            workBook = new XSSFWorkbook(inputStream);
        } else if (fileExtensionName.equals(".xlsm")) {
            workBook = new XSSFWorkbook(inputStream);
        }
        //Check condition if the file is xls file
        else if (fileExtensionName.equals(".xls")) {
            workBook = new HSSFWorkbook(inputStream);
        }
        sheetNames = readAllSheetsName(workBook);

    }

    public static void convertCSVToXLSX(String csvFilePath, String csvFileName, String excelFilePath, String excelFileName, String csvFileDelimiter) {
        try {
            XSSFWorkbook workBook = new XSSFWorkbook();
            XSSFSheet sheet = workBook.createSheet("sheet1");
            String currentLine;
            int RowNum = -1;
            BufferedReader br = new BufferedReader(new FileReader(csvFilePath + "/" + csvFileName));
            while ((currentLine = br.readLine()) != null) {
                String str[] = currentLine.split(Pattern.quote(csvFileDelimiter));
                RowNum++;
                XSSFRow currentRow = sheet.createRow(RowNum);
                for ( int i = 0; i < str.length; i++ ) {
                    currentRow.createCell(i).setCellValue(str[i]);
                }
            }
            br.close();

            if (excelFileName.contains("."))
                excelFileName = excelFileName.substring(0, excelFileName.lastIndexOf("."));
            FileOutputStream fileOutputStream = new FileOutputStream(excelFilePath + "/" + excelFileName + ".xlsx");
            workBook.write(fileOutputStream);
            fileOutputStream.close();
        } catch (Exception ex) {
            logger.error("Exception while Converting CSV File {} to Excel File. {}", csvFileName, ex.getStackTrace());
        }
    }

    public static List<List<String>> getAllExcelDataIncludingHeaders(String filePath, String fileName, String sheetName) {
        List<List<String>> allData = new ArrayList<>();
        List<String> oneRowData;
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.warn("Couldn't locate Sheet {} in File {}.", sheetName, fileName);
                return allData;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            Row row = rowIterator.next();
            int noOfColumns = row.getPhysicalNumberOfCells();

            do {
                oneRowData = new ArrayList<>();

                for ( int i = 0; i < noOfColumns; i++ ) {
                    Cell cell = row.getCell(i);
                    if (cell == null) {
                        oneRowData.add(null);
                        continue;
                    }

                    if (cell.getCellTypeEnum() == CellType.NUMERIC)
                        oneRowData.add(Double.toString(cell.getNumericCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.STRING)
                        oneRowData.add(cell.getStringCellValue());
                    else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                        oneRowData.add(Boolean.toString(cell.getBooleanCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.BLANK)
                        oneRowData.add("");
                }

                allData.add(oneRowData);

                row = rowIterator.hasNext() ? rowIterator.next() : null;
            } while (row != null);

            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting All Excel Data from File {} and Sheet {}. {}", fileName, sheetName, e.getStackTrace());
        }
        return allData;
    }

    //This method returns all excel data excluding first row i.e. headers.
    public static List<List<String>> getAllExcelData(String filePath, String fileName, String sheetName) {
        List<List<String>> allData = new ArrayList<>();
        List<String> oneRowData;
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.warn("Couldn't locate Sheet {} in File {}.", sheetName, fileName);
                return allData;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            Row row = rowIterator.next();
            int noOfColumns = row.getPhysicalNumberOfCells();

            while (rowIterator.hasNext()) {
                oneRowData = new ArrayList<>();
                row = rowIterator.next();

                for ( int i = 0; i < noOfColumns; i++ ) {
                    Cell cell = row.getCell(i);
                    if (cell == null) {
                        oneRowData.add(null);
                        continue;
                    }

                    if (cell.getCellTypeEnum() == CellType.NUMERIC)
                        oneRowData.add(Double.toString(cell.getNumericCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.STRING)
                        oneRowData.add(cell.getStringCellValue());
                    else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                        oneRowData.add(Boolean.toString(cell.getBooleanCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.BLANK)
                        oneRowData.add("");
                }
                allData.add(oneRowData);
            }
            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting All Excel Data from File {} and Sheet {}. {}", fileName, sheetName, e.getStackTrace());
        }
        return allData;
    }

    //Returns the data of Any One Row. Row No is
    public static List<String> getExcelDataOfOneRow(String filePath, String fileName, String sheetName, int rowNo) {
        List<String> oneRowData = new ArrayList<>();
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            ZipSecureFile.setMinInflateRatio(-1.0d);
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.warn("Couldn't locate Sheet {} in File {}", sheetName, fileName);
                return oneRowData;
            }
            if (sheet.getPhysicalNumberOfRows() < rowNo) {
                logger.warn("Row No. {} doesn't exist.", rowNo);
                return oneRowData;
            }
            if (rowNo < 1) {
                logger.warn("Row No. can't be less than 1.");
                return oneRowData;
            }
            Iterator<Row> rowIterator = sheet.iterator();
            Row row = null;
            for ( int i = 0; i < rowNo; i++ )
                row = rowIterator.next();

            int noOfColumns = row.getLastCellNum();
            for ( int j = 0; j < noOfColumns; j++ ) {
                Cell cell = row.getCell(j);
                if (cell == null) {
                    oneRowData.add(null);
                    continue;
                }

                if (cell.getCellTypeEnum() == CellType.NUMERIC)
                    oneRowData.add(Double.toString(cell.getNumericCellValue()));
                else if (cell.getCellTypeEnum() == CellType.STRING)
                    oneRowData.add(cell.getStringCellValue().trim());
                else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                    oneRowData.add(Boolean.toString(cell.getBooleanCellValue()));
                else if (cell.getCellTypeEnum() == CellType.BLANK)
                    oneRowData.add("");
            }
            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting Data of Row {} from File {} and Sheet {}. {}", rowNo, fileName, sheetName, e.getStackTrace());
        }
        return oneRowData;
    }

    public static String getOneCellValue(String filePath, String fileName, String sheetName, int rowNo, int columnNo) {
        return getOneCellValue(filePath, fileName, sheetName, rowNo, columnNo, null, null);
    }

    public static String getOneCellValue(String filePath, String fileName, String sheetName, int rowNo, int columnNo, String fieldType, String dateFormat) {
        String cellValue = null;

        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                logger.warn("Couldn't locate Sheet {} in File {}", sheetName, fileName);
                return null;
            }

            if (sheet.getPhysicalNumberOfRows() < rowNo) {
                logger.warn("Row No. {} doesn't exist.", rowNo);
                return null;
            }

            Row row = sheet.getRow(rowNo);
            int noOfColumns = row.getLastCellNum();

            if (noOfColumns < columnNo) {
                logger.warn("Column No. {} doesn't exist in Row {}", columnNo, rowNo);
            }

            Cell cell = row.getCell(columnNo);
            if (cell == null) {
                return null;
            }

            if (fieldType == null) {
                if (cell.getCellTypeEnum() == CellType.NUMERIC)
                    cellValue = String.valueOf(cell.getNumericCellValue());
                else if (cell.getCellTypeEnum() == CellType.STRING)
                    cellValue = cell.getStringCellValue();
                else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                    cellValue = String.valueOf(cell.getBooleanCellValue());
                else if (cell.getCellTypeEnum() == CellType.BLANK)
                    cellValue = "";
            } else {
                if (fieldType.equalsIgnoreCase("date")) {
                    if (dateFormat == null || dateFormat.equalsIgnoreCase("")) {
                        DataFormatter dataFormatter = new DataFormatter();
                        cellValue = dataFormatter.formatCellValue(cell);
                    } else {
                        SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                        Date date = new Date(cell.getDateCellValue().toString());
                        cellValue = dateFormatter.format(date);
                    }
                }
            }

            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting Cell Value of Row {} and Column {} from File {} and Sheet {}. {}", rowNo, columnNo, fileName, sheetName, e.getStackTrace());
        }

        return cellValue;
    }

    public static List<String> getHeaders(String filePath, String fileName, String sheetName) {
        return getExcelDataOfOneRow(filePath, fileName, sheetName, 1);
    }

    public static List<String> getOffSetHeaders(String filePath, String fileName, String sheetName) {
        return getExcelDataOfOneRow(filePath, fileName, sheetName, 4);
    }

    public static synchronized List<String> getOneColumnDataFromMultipleRows(String excelFilePath, String excelFileName, String sheetName, int columnNo, int startingRowNo,
                                                                             int noOfRows) {
        List<String> columnData = new ArrayList<>();
        try {
            FileInputStream file = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.info("Couldn't locate Sheet {} in File {}.", sheetName, excelFileName);
                return columnData;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            int rowNo = 0;
            while (rowNo < startingRowNo && rowIterator.hasNext()) {
                rowIterator.next();
                rowNo++;
            }

            int i = 0;
            Row row;
            while (i < noOfRows && rowIterator.hasNext()) {
                row = rowIterator.next();
                if (row.getCell(columnNo) == null) {
                    break;
                }

                columnData.add(row.getCell(columnNo).toString());
                i++;
            }

            file.close();
        } catch (Exception e) {
            logger.error("Exception while Getting Data for Column No {} in Sheet {} at File location [{}]. {}", columnNo, sheetName, excelFilePath + "/" + excelFileName,
                    e.getStackTrace());
        }
        return columnData;
    }

    public static synchronized List<String> getOneColumnDataFromMultipleRowsIncludingEmptyRows(String excelFilePath, String excelFileName, String sheetName, int columnNo,
                                                                                               int startingRowNo, int noOfRows) {
        List<String> columnData = new ArrayList<>();
        try {
            FileInputStream file = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.info("Couldn't locate Sheet {} in File {}.", sheetName, excelFileName);
                return columnData;
            }

            int i = 0;
            Row row = sheet.getRow(startingRowNo);
            while (i < noOfRows) {
                if (row != null) {
                    columnData.add(row.getCell(columnNo).toString());
                } else {
                    columnData.add("");
                }
                row = sheet.getRow(startingRowNo + (++i));
            }

            file.close();
        } catch (Exception e) {
            logger.error("Exception while Getting Data for Column No {} in Sheet {} at File location [{}]. {}", columnNo, sheetName, excelFilePath + "/" + excelFileName,
                    e.getStackTrace());
        }
        return columnData;
    }

    public static List<List<String>> getExcelDataOfMultipleRows(String filePath, String fileName, String sheetName, int startingRowNo, int noOfRows) {
        List<List<String>> allData = new ArrayList<>();
        List<String> oneRowData;
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.warn("Couldn't locate Sheet {} in File {}.", sheetName, fileName);
                return allData;
            }

            for ( int i = startingRowNo; i < (startingRowNo + noOfRows); i++ ) {
                Row row = sheet.getRow(i);
                oneRowData = new ArrayList<>();

                int noOfColumns = row.getPhysicalNumberOfCells();

                for ( int j = 0; j < noOfColumns; j++ ) {
                    Cell cell = row.getCell(j);
                    if (cell == null) {
                        oneRowData.add(null);
                        continue;
                    }

                    if (cell.getCellTypeEnum() == CellType.NUMERIC)
                        oneRowData.add(Double.toString(cell.getNumericCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.STRING)
                        oneRowData.add(cell.getStringCellValue());
                    else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                        oneRowData.add(Boolean.toString(cell.getBooleanCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.BLANK)
                        oneRowData.add("");
                }
                allData.add(oneRowData);
            }

            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting Excel Data of Multiple Rows (starting Row: {} and No of Rows: {}) from File {} and Sheet {}. {}", startingRowNo, noOfRows,
                    fileName, sheetName, e.getStackTrace());
        }
        return allData;
    }

    public static List<List<String>> getExcelDataOfMultipleRowsWithNullAsHyphenInAnyColumn(String filePath, String fileName, String sheetName, int startingRowNo, int noOfRows, int noOfColumns) {
        List<List<String>> allData = new ArrayList<>();
        List<String> oneRowData;
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.warn("Couldn't locate Sheet {} in File {}.", sheetName, fileName);
                return allData;
            }

            for ( int i = startingRowNo; i < (startingRowNo + noOfRows); i++ ) {
                Row row = sheet.getRow(i);
                oneRowData = new ArrayList<>();

                for ( int j = 0; j < noOfColumns; j++ ) {
                    Cell cell = row.getCell(j);
                    if (cell == null) {
                        oneRowData.add("-");
                        continue;
                    }

                    if (cell.getCellTypeEnum() == CellType.NUMERIC)
                        oneRowData.add(Double.toString(cell.getNumericCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.STRING)
                        oneRowData.add(cell.getStringCellValue());
                    else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                        oneRowData.add(Boolean.toString(cell.getBooleanCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.BLANK)
                        oneRowData.add("-");
                }
                allData.add(oneRowData);
            }

            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting Excel Data of Multiple Rows (starting Row: {} and No of Rows: {}) from File {} and Sheet {}. {}", startingRowNo, noOfRows,
                    fileName, sheetName, e.getStackTrace());
        }
        return allData;
    }

    public static Long getNoOfRows(String excelFilePath, String excelFileName, String sheetName) {
        Long noOfRows = -1L;
        try {
            ZipSecureFile.setMinInflateRatio(0);
            FileInputStream file = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);

            noOfRows = Integer.toUnsignedLong(sheet.getPhysicalNumberOfRows());
        } catch (Exception e) {
            logger.error("Exception while getting Total No of Rows in Excel Sheet {}. {}", sheetName, e.getStackTrace());
        }
        return noOfRows;
    }

    public static boolean updateColumnValueAsFormula(String filePath, String fileName, String sheetName, int rowNo, int colNo, String updatedFormulaValue) {
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workBook = new XSSFWorkbook(file);
            XSSFSheet sheet = workBook.getSheet(sheetName);
            Row rowToWrite = sheet.getRow(rowNo);

            if (rowToWrite == null) {
                sheet.createRow(rowNo);
                rowToWrite = sheet.getRow(rowNo);
            }

            Cell cellToWrite = rowToWrite.getCell(colNo);

            if (cellToWrite == null) {
                rowToWrite.createCell(colNo);
                cellToWrite = rowToWrite.getCell(colNo);
            }

            cellToWrite.setCellType(Cell.CELL_TYPE_FORMULA);
            cellToWrite.setCellFormula(updatedFormulaValue);

            FileOutputStream fileOut = new FileOutputStream(filePath + "/" + fileName);
            workBook.write(fileOut);
            fileOut.flush();
            fileOut.close();
            return true;
        } catch (Exception e) {
            logger.error("Exception while Updating Column Value as Formula. {}", e.getMessage());
        }

        return false;
    }

    public static boolean updateColumnValue(String filePath, String fileName, String sheetName, int rowNo, int colNo, String updatedValue) {
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workBook = new XSSFWorkbook(file);
            XSSFSheet sheet = workBook.getSheet(sheetName);
            Row rowToWrite = sheet.getRow(rowNo);

            if (rowToWrite == null) {
                sheet.createRow(rowNo);
                rowToWrite = sheet.getRow(rowNo);
            }

            Cell cellToWrite = rowToWrite.getCell(colNo);

            if (cellToWrite == null) {
                rowToWrite.createCell(colNo);
                cellToWrite = rowToWrite.getCell(colNo);
            }

            String originalDateFormat = null;
            boolean isFieldOfDateType = false;

            //For Cell of Date Type
            if (cellToWrite.getCellTypeEnum().name().equalsIgnoreCase("Numeric")) {
                String dateFormatString = cellToWrite.getCellStyle().getDataFormatString();
                originalDateFormat = dateFormatString;

                dateFormatString = dateFormatString.toLowerCase();

                if (dateFormatString.contains("dd") || dateFormatString.contains("-") || dateFormatString.contains("/") || dateFormatString.contains("yy")) {
                    //It is of Date Type
                    isFieldOfDateType = true;
                }
            }

            if (isFieldOfDateType) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(originalDateFormat);
                Date cellValue = dateFormat.parse(updatedValue);
                cellToWrite.setCellValue(cellValue);
            } else if(cellToWrite.getCellTypeEnum().name().equalsIgnoreCase("Numeric")){
                cellToWrite.setCellValue(Integer.parseInt(updatedValue));
            }else {
                cellToWrite.setCellValue(updatedValue);
            }

            FileOutputStream fileOut = new FileOutputStream(filePath + "/" + fileName);
            workBook.write(fileOut);
            fileOut.flush();
            fileOut.close();
            return true;
        } catch (Exception e) {
            logger.error("Exception while Updating Column Value. {}", e.getMessage());
        }

        return false;
    }

    public static boolean updateColumnValue(String filePath, String fileName, String sheetName, int rowNo, int colNo, int updatedValue) {
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workBook = new XSSFWorkbook(file);
            XSSFSheet sheet = workBook.getSheet(sheetName);
            Row rowToWrite = sheet.getRow(rowNo);

            if (rowToWrite == null) {
                sheet.createRow(rowNo);
                rowToWrite = sheet.getRow(rowNo);
            }

            Cell cellToWrite = rowToWrite.getCell(colNo);

            if (cellToWrite == null) {
                rowToWrite.createCell(colNo);
                cellToWrite = rowToWrite.getCell(colNo);
            }

            String originalDateFormat = null;
            boolean isFieldOfDateType = false;

            //For Cell of Date Type
            if (cellToWrite.getCellTypeEnum().name().equalsIgnoreCase("Numeric")) {
                String dateFormatString = cellToWrite.getCellStyle().getDataFormatString();
                originalDateFormat = dateFormatString;

                dateFormatString = dateFormatString.toLowerCase();

                if (dateFormatString.contains("dd") || dateFormatString.contains("-") || dateFormatString.contains("/") || dateFormatString.contains("yy")) {
                    //It is of Date Type
                    isFieldOfDateType = true;
                }
            }
            cellToWrite.setCellValue(updatedValue);


            FileOutputStream fileOut = new FileOutputStream(filePath + "/" + fileName);
            workBook.write(fileOut);
            fileOut.flush();
            fileOut.close();
            return true;
        } catch (Exception e) {
            logger.error("Exception while Updating Column Value. {}", e.getMessage());
        }

        return false;
    }

    public static boolean updateColumnValueDate(String filePath, String fileName, String sheetName, int rowNo, int colNo, String dateFormatOfCell, String updatedValue) {

        Workbook wb = new XSSFWorkbook();

        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workBook = new XSSFWorkbook(file);
            CreationHelper creationHelper = workBook.getCreationHelper();

            XSSFSheet sheet = workBook.getSheet(sheetName);
            Row rowToWrite = sheet.getRow(rowNo);

            if (rowToWrite == null) {
                sheet.createRow(rowNo);
                rowToWrite = sheet.getRow(rowNo);
            }

            Cell cellToWrite = rowToWrite.getCell(colNo);

            if (cellToWrite == null) {
                rowToWrite.createCell(colNo);
                cellToWrite = rowToWrite.getCell(colNo);
            }


            SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatOfCell);
            Date cellValue = dateFormat.parse(updatedValue);
            cellToWrite.setCellValue(cellValue);
            CellStyle style1 = workBook.createCellStyle();
            style1.setDataFormat(creationHelper.createDataFormat().getFormat(
                    dateFormatOfCell));
            cellToWrite.setCellStyle(style1);

            FileOutputStream fileOut = new FileOutputStream(filePath + "/" + fileName);
            workBook.write(fileOut);
            fileOut.flush();
            fileOut.close();
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    public static boolean updateColumnValue(String filePath, String fileName, String sheetName, int rowNo, int colNo, Double updatedValue) {
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workBook = new XSSFWorkbook(file);
            XSSFSheet sheet = workBook.getSheet(sheetName);
            Row rowToWrite = sheet.getRow(rowNo);

            if (rowToWrite == null) {
                sheet.createRow(rowNo);
                rowToWrite = sheet.getRow(rowNo);
            }

            Cell cellToWrite = rowToWrite.getCell(colNo);

            if (cellToWrite == null) {
                rowToWrite.createCell(colNo);
                cellToWrite = rowToWrite.getCell(colNo);
            }

            String originalDateFormat = null;
            boolean isFieldOfDateType = false;

            //For Cell of Date Type
            if (cellToWrite.getCellTypeEnum().name().equalsIgnoreCase("Numeric")) {
                String dateFormatString = cellToWrite.getCellStyle().getDataFormatString();
                originalDateFormat = dateFormatString;

                dateFormatString = dateFormatString.toLowerCase();

                if (dateFormatString.contains("dd") || dateFormatString.contains("-") || dateFormatString.contains("/") || dateFormatString.contains("yy")) {
                    //It is of Date Type
                    isFieldOfDateType = true;
                }
            }
            cellToWrite.setCellValue(updatedValue);


            FileOutputStream fileOut = new FileOutputStream(filePath + "/" + fileName);
            workBook.write(fileOut);
            fileOut.flush();
            fileOut.close();
            return true;
        } catch (Exception e) {
            logger.error("Exception while Updating Column Value. {}", e.getMessage());
        }

        return false;
    }

    public static synchronized Boolean editRowData(String excelFilePath, String excelFileName, String sheetName, int rowNumber, Map<Integer, Object> columnDataMap) {
        try {
            logger.info("Editing Excel File {}, Sheet {}, Row {}", excelFilePath + "/" + excelFileName, sheetName, rowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row row = sheet.getRow(rowNumber);

            if(row == null){
                row = sheet.createRow(rowNumber);
            }
            for (Map.Entry<Integer, Object> entry : columnDataMap.entrySet()) {

                if (row.getCell(entry.getKey()) == null) {
                    row.createCell(entry.getKey());
                }

                Cell column = row.getCell(entry.getKey());
//                String updatedValue = entry.getValue().toString();
                Object updatedValue = entry.getValue();
                if (updatedValue instanceof Double) {
                    column.setCellValue((Double) updatedValue);
                }else if (updatedValue instanceof Integer) {
                    column.setCellValue((Integer) updatedValue);
                } else {
                    column.setCellValue(updatedValue.toString());
                }
            }

            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static synchronized Boolean editMultipleRowsData(String excelFilePath, String excelFileName, String sheetName, Map<Integer, Map<Integer, Object>> columnDataMap) {
        try {
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);
            logger.info("Editing Excel File {}, Sheet {}", excelFilePath + "/" + excelFileName, sheetName);

            for ( Map.Entry<Integer, Map<Integer, Object>> oneRowData : columnDataMap.entrySet() ) {
                Integer rowNumber = oneRowData.getKey();
                Row row = sheet.getRow(rowNumber);
                if (row == null)
                    sheet.createRow(rowNumber);
                row = sheet.getRow(rowNumber);
                logger.info("Editing Row No. {}", rowNumber);

                for ( Map.Entry<Integer, Object> entry : columnDataMap.get(rowNumber).entrySet() ) {
                    if (row.getCell(entry.getKey()) == null) {
                        row.createCell(entry.getKey());
                    }

                    Cell column = row.getCell(entry.getKey());
                    String updatedValue = entry.getValue().toString();
                    if (entry.getValue() instanceof Date) {
                        column.setCellValue((Date) entry.getValue());
                        logger.debug("Found Date instance in editMultipleRowsData() hence casting the Object to Date");
                    }else if (entry.getValue() instanceof Integer) {
                        column.setCellValue((Date) entry.getValue());
                        logger.debug("Found Date instance in editMultipleRowsData() hence casting the Object to Date");
                    } else
                        column.setCellValue(updatedValue);
                }
            }

            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Multiple Rows Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static synchronized Boolean editMultRowsDataAccToUpdVal(String excelFilePath, String excelFileName, String sheetName, Map<Integer, Map<Integer, Object>> columnDataMap) {
        try {
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);
            logger.info("Editing Excel File {}, Sheet {}", excelFilePath + "/" + excelFileName, sheetName);

            for ( Map.Entry<Integer, Map<Integer, Object>> oneRowData : columnDataMap.entrySet() ) {
                Integer rowNumber = oneRowData.getKey();
                Row row = sheet.getRow(rowNumber);
                if (row == null)
                    sheet.createRow(rowNumber);
                row = sheet.getRow(rowNumber);
                logger.info("Editing Row No. {}", rowNumber);

                for ( Map.Entry<Integer, Object> entry : columnDataMap.get(rowNumber).entrySet() ) {
                    if (row.getCell(entry.getKey()) == null) {
                        row.createCell(entry.getKey());
                    }

                    Cell column = row.getCell(entry.getKey());
                    String updatedValue = entry.getValue().toString();
                    if (entry.getValue() instanceof Date) {
                        column.setCellValue((Date) entry.getValue());
                        logger.debug("Found Date instance in editMultipleRowsData() hence casting the Object to Date");
                    }else if (entry.getValue() instanceof Integer) {
                        column.setCellValue(((Integer) entry.getValue()).intValue());
                        logger.debug("Found Date instance in editMultipleRowsData() hence casting the Object to Date");
                    } else
                        column.setCellValue(updatedValue);
                }
            }

            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Multiple Rows Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static synchronized Boolean copyRowData(String excelFilePath, String excelFileName, String sheetName, int rowNumber, int newRowNumberToBeCopiedTo) {
        try {
            logger.info("Editing Excel File {}, Sheet {}, Row {}", excelFilePath + "/" + excelFileName, sheetName, rowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row row = sheet.getRow(rowNumber);
            sheet.createRow(rowNumber + 1);
            Row newRow = sheet.getRow(newRowNumberToBeCopiedTo);
            if (newRow == null) { //added by srijan for null exception
                sheet.createRow(newRowNumberToBeCopiedTo);
                newRow = sheet.getRow(newRowNumberToBeCopiedTo);
            }
            ((XSSFRow) newRow).copyRowFrom(row, new CellCopyPolicy());

            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static synchronized Boolean copyRowDataMultipleTimesWithIncrementalSlNo(String excelFilePath, String excelFileName, String sheetName, int rowNumber,
                                                                                   long noOfTimesToBeCopied) {
        try {
            logger.info("Editing Excel File {}, Sheet {}, Row {}", excelFilePath + "/" + excelFileName, sheetName, rowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row row = sheet.getRow(rowNumber);

            for ( int i = 1; i <= noOfTimesToBeCopied; i++ ) {
                sheet.createRow(rowNumber + i);
                Row newRow = sheet.getRow(rowNumber + i);
                ((XSSFRow) newRow).copyRowFrom(row, new CellCopyPolicy());

                newRow.getCell(0).setCellValue(i + 1);
            }

            excelFile.close();

            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static synchronized Boolean copySameRowDatainMultipleColumns(String excelFilePath, String excelFileName, String sheetName, int rowNumber, int numberofrowstobecopied) {
        try {
            logger.info("Editing Excel File {}, Sheet {}, Row {}", excelFilePath + "/" + excelFileName, sheetName, rowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row row = sheet.getRow(rowNumber);
            for ( int j = rowNumber; j < numberofrowstobecopied; j++ ) {
                sheet.createRow(rowNumber + 1);
                Row newRow = sheet.getRow(j);

                int totalColumns = row.getLastCellNum();
                String updatedValue = "";
                for ( int i = 0; i < totalColumns; i++ ) {

                    Cell column = row.getCell(i);
                    if (column != null) {

                        newRow.createCell(i);
                        Cell copiedcolumn = newRow.getCell(i);
                        String celltype = column.getCellTypeEnum().toString();
                        if (celltype.equals("NUMERIC")) {
                            try {
                                updatedValue = column.toString();
                            } catch (IllegalStateException e) {
                                try {
                                    Double updatedValue1 = column.getNumericCellValue();
                                    updatedValue = updatedValue1.toString();
                                } catch (Exception e1) {
                                    updatedValue = "";
                                }
                            } catch (Exception e) {
                                updatedValue = "";
                            }

                        } else if (celltype.equals("STRING")) {
                            updatedValue = column.getStringCellValue();
                        }
                        copiedcolumn.setCellValue(updatedValue);
                    }
                }
            }
            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static synchronized Boolean copyColumnvalues(String excelFilePath, String excelFileName, String sheetName, int startingRowNumber, int numberofRowsToCopy, HashMap<Integer, String> columnMap, int filecount) {
        try {
            logger.info("Editing Excel File {}, Sheet {}, Staring Row {}", excelFilePath + "/" + excelFileName, sheetName, startingRowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);


            for ( int i = startingRowNumber; i < numberofRowsToCopy; i++ ) {

                Row row = sheet.getRow(i);

                int totalColumns = row.getLastCellNum();
                String updatedValue = "";
                for ( int j = 0; j < totalColumns; j++ ) {

                    if (columnMap.containsKey(j)) {
                        Cell column = row.getCell(j);
                        if (column != null) {

                            String celltype = column.getCellTypeEnum().toString();
                            if (celltype.equals("NUMERIC")) {
                                try {
                                    updatedValue = columnMap.get(j);
                                    if (updatedValue.contains("_")) {
                                        updatedValue = updatedValue + (i - 5);
                                    }
                                } catch (IllegalStateException e) {
                                    try {
                                        Double updatedValue1 = column.getNumericCellValue();
                                        updatedValue = updatedValue1.toString();
                                        if (updatedValue.contains("_")) {
                                            updatedValue = updatedValue + (i - 5);
                                        }
                                    } catch (Exception e1) {
                                        updatedValue = "";
                                    }
                                } catch (Exception e) {
                                    updatedValue = "";
                                }

                            } else if (celltype.equals("STRING")) {
                                updatedValue = columnMap.get(j);
                                if (j == 6 || j == 7 || j == 15) {
                                    updatedValue = column.getStringCellValue();
                                    updatedValue = updatedValue.substring(2);
                                    updatedValue = filecount + updatedValue;
                                }
//								if(updatedValue.equals("same as 2 column")){
//									updatedValue = columnMap.get(2);
//								}
//								if(updatedValue.contains("_")){
//									updatedValue = updatedValue + (i + 5006 - 5);
//								}
                            }
                            column.setCellValue(updatedValue);
                        }
                    }
                }
            }
            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + filecount + "_" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static void delete_xls(String filePath, String fileName) {
        try {

            File filetodelete = new File(filePath + "//" + fileName);

            if (filetodelete.delete()) {
                System.out.println(filetodelete.getName() + " is deleted!");
            } else {
                System.out.println("Delete has failed.");

            }

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public static List<List<String>> getAllExcelDataColumnWise(String filePath, String fileName, String sheetName) {
        List<List<String>> allData = new ArrayList<>();

        try {
            logger.info("Getting All Excel Data Column Wise from File {} and Sheet {}", filePath + "/" + fileName, sheetName);
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.error("Couldn't locate Sheet {} in File {}.", sheetName, fileName);
                return null;
            }

            int noOfRows = sheet.getPhysicalNumberOfRows();
            int noOfColumns = sheet.getRow(0).getPhysicalNumberOfCells();

            for ( int columnNo = 0; columnNo < noOfColumns; columnNo++ ) {
                List<String> columnDataList = new ArrayList<>();

                for ( int rowNo = 0; rowNo < noOfRows; rowNo++ ) {
                    Cell cell = sheet.getRow(rowNo).getCell(columnNo);

                    if (cell == null) {
                        columnDataList.add("");
                        continue;
                    }

                    if (cell.getCellTypeEnum() == CellType.NUMERIC)
                        columnDataList.add(Double.toString(cell.getNumericCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.STRING)
                        columnDataList.add(cell.getStringCellValue());
                    else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                        columnDataList.add(Boolean.toString(cell.getBooleanCellValue()));
                    else if (cell.getCellTypeEnum() == CellType.BLANK)
                        columnDataList.add("");
                }

                allData.add(columnDataList);
            }

            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting All Excel Data Column Wise from File {} and Sheet {}. {}", fileName, sheetName, e.getStackTrace());
        }
        return allData;
    }

    public static boolean createNewSheet(String filePath, String fileName, String newSheetName) {
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            workbook.createSheet(newSheetName);

            FileOutputStream fileOut = new FileOutputStream(filePath + "/" + fileName);
            workbook.write(fileOut);
            fileOut.flush();
            fileOut.close();

            return new XLSUtils(filePath, fileName).getSheetNames().contains(newSheetName);
        } catch (Exception e) {
            logger.error("Exception while Creating New Sheet {} in File located at [{}]. {}", newSheetName, filePath + "/" + fileName, e.getStackTrace());
        }

        return false;
    }

    public List<String> getSheetNames() {
        return sheetNames;
    }

    // it will return all the sheetsName in WorkBook
    private List<String> readAllSheetsName(Workbook workbook) {

        List<String> sheetNames = new ArrayList<>();
        for ( int i = 0; i < workBook.getNumberOfSheets(); i++ ) {
            sheetNames.add(workBook.getSheetName(i));
        }
        return sheetNames;
    }

    // Xls_Reader - Returns Sheet Existence
    public boolean isSheetExist(String sheetName) {
        int index = workBook.getSheetIndex(sheetName);
        if (index == -1) {
            index = workBook.getSheetIndex(sheetName.toUpperCase());
            if (index == -1)
                return false;

            return true;
        } else
            return true;
    }

    // Xls_Reader - Returns Row Count
    public int getRowCount(String sheetName) {
        int index = workBook.getSheetIndex(sheetName);
        if (index == -1) {
            return 0;
        } else {
            sheet = workBook.getSheetAt(index);
            int number = sheet.getLastRowNum() + 1;
            logger.debug("Row Count is {} ", number);
            return number;
        }
    }

    // Xls_Reader - Returns Column Count
    public int getColumnCount(String sheetName) {
        if (!isSheetExist(sheetName)) {
            return -1;
        }
        sheet = workBook.getSheet(sheetName);
        row = sheet.getRow(0);
        if (row == null) {
            return -1;
        }
        logger.debug("Column Count is {} ", row.getLastCellNum());
        return row.getLastCellNum();
    }

    // Xls_Reader - Returns the data as particular colNum and rowNum
    public String getCellData(String sheetName, int colNum, int rowNum) {
        try {
            if (rowNum < 0)
                return "";

            int index = workBook.getSheetIndex(sheetName);

            if (index == -1)
                return "";

            sheet = workBook.getSheetAt(index);
            row = sheet.getRow(rowNum);

            if (row == null)
                return "";

            Cell cell = row.getCell(colNum);

            if (cell == null)
                return "";

            if (cell.getCellTypeEnum() == CellType.STRING) {
//                logger.debug("{}",cell.getHyperlink().getClass());
                if (cell.getHyperlink() != null) {
                    logger.debug("hyperLink is : {}", cell.getHyperlink().getAddress());
                    logger.debug("cell value is : {}", cell.getStringCellValue());
                    return cell.getStringCellValue() + ":::>" + "Link:" + cell.getHyperlink().getAddress();
                } else {
                    logger.debug("cell value is : {}", cell.getStringCellValue());
                    return cell.getStringCellValue();
                }
            } else if (cell.getCellTypeEnum() == CellType.NUMERIC || cell.getCellTypeEnum() == CellType.FORMULA) {
                String cellText = String.valueOf(cell.getNumericCellValue());
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    double d = cell.getNumericCellValue();

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(HSSFDateUtil.getJavaDate(d));
                    cellText = (String.valueOf(cal.get(Calendar.YEAR))).substring(2);
                    //cellText = cal.get(Calendar.MONTH) + 1 + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cellText;
                    cellText = cal.get(Calendar.MONTH) + 1 + "-" + cal.get(Calendar.DAY_OF_MONTH) + "-" + cellText;
                }
                return cellText;
            } else if (cell.getCellTypeEnum() == CellType.BLANK)
                return "";

            else
                return String.valueOf(cell.getBooleanCellValue());
        } catch (Exception e) {
            e.printStackTrace();
            return "row " + rowNum + " or column " + colNum + " does not exist  in xls";
        }
    }

    // Xls_Reader - Returns the data in list of list of Strings based on the index Map
    public List<List<String>> getAllExcelData(String sheetName, HashMap<String, String> index) {

        List<List<String>> allData = new ArrayList<>();


        int startRow = Integer.parseInt(index.get("startRow"));
        int lastDataRow = Integer.parseInt(index.get("lastDataRow"));
        int startCol = Integer.parseInt(index.get("startCol"));
        int lastDataCol = Integer.parseInt(index.get("lastDataCol"));

        int c = getColumnCount(sheetName);
        int r = getRowCount(sheetName);
        logger.debug("Number of Columns Count is " + getColumnCount(sheetName));
        logger.debug("Number of Rows Count is " + getRowCount(sheetName));

        for ( int i = startRow; i < r + lastDataRow; i++ ) {
            List<String> oneRowData = new ArrayList<>();
            for ( int j = startCol; j < c + lastDataCol; j++ ) {
                logger.debug(" row_number {} , column_number {}  ", i, j);
                logger.debug(" |  " + getCellData(sheetName, j, i) + " |  ");
                oneRowData.add(getCellData(sheetName, j, i));
            }

            allData.add(oneRowData);
            logger.debug("\n-----------------------------------------------------------------------------------------");
        }
        return allData;
    }

    // function to verify whether Environment is Correct in Excel Sheet
    public boolean verifytheEnvironment(String link) throws MessagingException, IOException {
        logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        logger.info("Environment Host  is : {} ", link);
        return link.contains(ConfigureEnvironment.getEnvironmentProperty("Host"));
    }

    // function to check if two days are not more than one day Apart
    public boolean matchDate(String showPageDate, String xlsDate, String showPageDateFormate, String xlsDateFormate) throws ParseException {
        logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        logger.debug("Inside  matchDate function with showPageDate: {} ,xlsDate: {}", showPageDate, xlsDate);
        Date ShowPagedate, XlsDate;
        xlsDate = xlsDate.replace("UTC", "");
        showPageDate = showPageDate.replace("UTC", "");
        DateFormat df1 = new SimpleDateFormat(showPageDateFormate);
        DateFormat df2 = new SimpleDateFormat(xlsDateFormate);

        ShowPagedate = df1.parse(showPageDate);
        XlsDate = df2.parse(xlsDate);

        int daysApart = (int) (ShowPagedate.getTime() - XlsDate.getTime()) / (1000 * 60 * 60 * 24);
        if (abs(daysApart) >= 1)
            return false;
        else
            return true;

    }

    // function to verify the response of show API with excel Data
    public boolean verifyTheResponseWithXLSData(String showResponse, List<String> XLSData, HashMap<String, HashMap<String, String>> sheetConfigurationHashMap, int columnMax, String[] columnInternalProperties) throws MessagingException, IOException, ParseException {
        logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        logger.info("validation Starts from Here");
        logger.info("************************************************************");
        xlsDateFormate = sheetConfigurationHashMap.get("rowColInfo").get("dateFormate");
        JSONObject jsonObjOfResponse = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");


        logger.debug("xlsDateFormate is :{}", xlsDateFormate);
        logger.debug("body.data of Show Page Response is  :{}", jsonObjOfResponse);

        for ( int i = 1; i <= columnMax; i++ ) {

            if (sheetConfigurationHashMap.get(Integer.toString(i)) != null) {
                HashMap<String, String> internalHashMap = sheetConfigurationHashMap.get(Integer.toString(i));
                logger.debug("internal :{}", internalHashMap);

                if (internalHashMap.containsKey("json_column")) {
                    if (jsonObjOfResponse.has(internalHashMap.get("json_column"))) {
                        if (!verifyExcelRowDataWithAPIResponse(jsonObjOfResponse.getJSONObject(internalHashMap.get("json_column")), internalHashMap, XLSData.get(i - 1))) {
                            logger.debug("Error : Show Page Response don't have any key for : {}", internalHashMap.get("json_column"));
                            return false;
                        }
                    } else {
                        logger.debug("Error : Show Page Response don't have any key for : {}", internalHashMap.get("json_column"));
                        return false;
                    }
                } else {
                    logger.debug("Config File Formate is Incorrect : Didn't have json_column field in internal Map of Rows");
                    return false;
                }

            }
        }

        return true;

    }

    boolean verifyExcelRowDataWithAPIResponse(JSONObject jsonObjRow, HashMap<String, String> internalHashMap, String xlsValue) throws ParseException {
        logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        logger.debug("jsonObjRow   A:{}", jsonObjRow);
        logger.debug("internalHashMap   B:{}", internalHashMap);
        logger.debug("xlsValue   C:{}", xlsValue);
        String type = internalHashMap.get("type");
        String formate = internalHashMap.get("formate");


        if (type == null) {
            return verifyExcelRowDataWithAPIResponseAfterTypeParsing(jsonObjRow, internalHashMap, xlsValue);
        } else {
            if (type.contentEquals("DATE")) {

                if (formate != null) {
                    return verifyExcelRowDataWithAPIResponseAfterTypeParsing(jsonObjRow, internalHashMap, xlsValue);
                } else {
                    logger.debug("if Content Type is Date then Formate should not be null");
                    return false;
                }
            } else if (type.contentEquals("STAKEHOLDER")) {
                JSONUtility json;
                jsonObjRow = jsonObjRow.getJSONObject("values");
                for ( String name : JSONObject.getNames(jsonObjRow) ) {

                    logger.debug("name is {}", name);
                    JSONObject temp = new JSONObject(jsonObjRow.get(name).toString());
                    json = new JSONUtility(temp);
                    JSONArray jsonArr = new JSONArray(json.getArrayJsonValue("values").toString());

                    for ( int j = 0; j < jsonArr.length(); j++ ) {
                        JSONObject temp2 = new JSONObject(jsonArr.get(j).toString());

                        if (temp2.get("name").toString().contains(xlsValue)) {
                            logger.debug("StackHolder Matches : ");
                            return true;
                        }
                    }
                }
                return false;
            } else {
                return verifyExcelRowDataWithAPIResponseAfterTypeParsing(jsonObjRow, internalHashMap, xlsValue);
            }


        }
    }

    boolean verifyExcelRowDataWithAPIResponseAfterTypeParsing(JSONObject jsonObjRow, HashMap<String, String> internalHashMap, String xlsValue) throws ParseException {

        logger.debug("jsonObjRow   A:{}", jsonObjRow);
        logger.debug("internalHashMap   B:{}", internalHashMap);
        logger.debug("xlsValue   C:{}", xlsValue);
        boolean isPathExist; // if the relative json path mentioned in internalHashMap exist or not
        String showPageValue; // this string value will store
        String values = internalHashMap.get("values");
        String type = internalHashMap.get("type");
        String formate = internalHashMap.get("formate");

        // if internalHashMap for that column don't has relative path to fetch cell data from show Page
        if (values == null) {
            showPageValue = jsonObjRow.get("values").toString();

            if (type != null && type.contentEquals("DATE")) {
                logger.debug("Matching XLS Date Value {} with Show Page Response Value {}", xlsValue, showPageValue);
                if (!matchDate(showPageValue, xlsValue, formate, xlsDateFormate)) { // split required because we are saving first cell with Link Name
                    logger.info("A:Show Page Value: {} is not matching with Excel Cell Value:{}", showPageValue, xlsValue);
                    return false;
                } else
                    return true;

            } else {
                if (!showPageValue.contains(xlsValue.split(":::>")[0])) { // split required because we are saving first cell with Link Name
                    logger.info("A:Show Page Value: {} is not matching with Excel Cell Value:{}", showPageValue, xlsValue);
                    return false;
                } else
                    return true;
            }

        }
        // if internalHashMap for that column has relative path to fetch cell data from show Page
        else {
            isPathExist = true;
            String[] relativePath = internalHashMap.get("values").split("\\.");
            logger.debug("relative Path is :{} , {}", internalHashMap.get("values"), relativePath);
            for ( int i = 0; i < relativePath.length - 1; i++ ) { // iteratively reaching the correct node for cell data
                if (jsonObjRow.has(relativePath[i])) {
                    jsonObjRow = jsonObjRow.getJSONObject(relativePath[i]);
                } else {
                    isPathExist = false;
                    break;
                }
            }
            if (isPathExist) {
                showPageValue = jsonObjRow.get(relativePath[relativePath.length - 1]).toString();
            } else
                showPageValue = "-";


            if (type != null && type.contentEquals("DATE")) {
                logger.debug("Matching XLS Date Value {} with Show Page Response Value {}", showPageValue, xlsValue);
                if (!matchDate(showPageValue, xlsValue, formate, xlsDateFormate)) { // split required because we are saving first cell with Link Name
                    logger.info("A:Show Page Value: {} is not matching with Excel Cell Value:{}", showPageValue, xlsValue);
                    return false;
                } else
                    return true;
            } else {
                if (!showPageValue.contains(xlsValue.split(":::>")[0])) { // split required because we are saving first cell with Link Name
                    logger.info("B:Show Page Value: {} is not matching with Excel Cell Value:{}", showPageValue, xlsValue);
                    return false;
                } else
                    return true;
            }

        }

    }

    //Added by sarthak
    public String getCellData(String sheetName, String colName, int rowNum) {
        try {
            if (rowNum <= 0) {
                return "";
            }
            int index = this.workBook.getSheetIndex(sheetName);
            int col_Num = -1;
            if (index == -1) {
                return "";
            }
            this.sheet = this.workBook.getSheetAt(index);
            this.row = this.sheet.getRow(0);
            for ( int i = 0; i < this.row.getLastCellNum(); ++i ) {
                if (this.row.getCell(i).getStringCellValue().trim().equals(colName.trim()))
                    col_Num = i;
            }
            if (col_Num == -1) {
                return "";
            }
            this.sheet = this.workBook.getSheetAt(index);
            this.row = this.sheet.getRow(rowNum - 1);
            if (this.row == null)
                return "";
            this.cell = this.row.getCell(col_Num);

            if (this.cell == null) {
                return "";
            }
            // System.out.println(this.cell.getCellType());
            if (this.cell.getCellType() == 1)
                return this.cell.getStringCellValue();
            if ((this.cell.getCellType() == 0) || (this.cell.getCellType() == 2)) {
                String cellText = String.valueOf(this.cell.getNumericCellValue());
                if (HSSFDateUtil.isCellDateFormatted(this.cell)) {
                    double d = this.cell.getNumericCellValue();

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(HSSFDateUtil.getJavaDate(d));

                    String.valueOf(cal.get(1)).substring(2);
                    cellText = cal.get(5) + "/" +
                            cal.get(2) + 1 + "/" +
                            cellText;
                }

                return cellText;
            }
            if (this.cell.getCellType() == 3) {
                return "";
            }
            return String.valueOf(this.cell.getBooleanCellValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "row " + rowNum + " or column " + colName + " does not exist in xls";
    }

    //Added by sarthak
    public String getCellDataByCustomcolumn(String sheetName, String colName, int rowNum, int columnHeaderIndex) {
        try {
            if (rowNum <= 0) {
                return "";
            }
            int index = this.workBook.getSheetIndex(sheetName);
            int col_Num = -1;
            if (index == -1) {
                return "";
            }
            this.sheet = this.workBook.getSheetAt(index);
            this.row = this.sheet.getRow(columnHeaderIndex);
            for ( int i = 0; i < this.row.getLastCellNum(); ++i ) {
                if (this.row.getCell(i).getStringCellValue().trim().equals(colName.trim()))
                    col_Num = i;
            }
            if (col_Num == -1) {
                return "";
            }
            this.sheet = this.workBook.getSheetAt(index);
            this.row = this.sheet.getRow(rowNum - 1);
            if (this.row == null)
                return "";
            this.cell = this.row.getCell(col_Num);

            if (this.cell == null) {
                return "";
            }
            // System.out.println(this.cell.getCellType());
            if (this.cell.getCellType() == 1)
                return this.cell.getStringCellValue();
            if ((this.cell.getCellType() == 0) || (this.cell.getCellType() == 2)) {
                String cellText = String.valueOf(this.cell.getNumericCellValue());
                if (HSSFDateUtil.isCellDateFormatted(this.cell)) {
                    double d = this.cell.getNumericCellValue();

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(HSSFDateUtil.getJavaDate(d));

                    String.valueOf(cal.get(1)).substring(2);
                    cellText = cal.get(5) + "/" +
                            cal.get(2) + 1 + "/" +
                            cellText;
                }

                return cellText;
            }
            if (this.cell.getCellType() == 3) {
                return "";
            }
            return String.valueOf(this.cell.getBooleanCellValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "row " + rowNum + " or column " + colName + " does not exist in xls";
    }

    public synchronized boolean copySheetFromOneExcelToAnother(String sheetNameTobeCopiedFrom, String sheetNameToBeCopiedTo,
                                                               String updatedFilePath, String updatedFileName) {

        Boolean updateStatus = true;
        try {

            int c = getColumnCount(sheetNameTobeCopiedFrom);
            int r = getRowCount(sheetNameTobeCopiedFrom);

            String columnDataToBeCopied;
            for ( int i = 0; i < r; i++ ) {

                for ( int j = 0; j < c; j++ ) {

                    columnDataToBeCopied = getCellData(sheetNameTobeCopiedFrom, j, i);
                    if (!updateColumnValue(updatedFilePath, updatedFileName, sheetNameToBeCopiedTo, i, j, columnDataToBeCopied)) {
                        updateStatus = false;

                    }
                }
            }
        } catch (Exception e) {

            updateStatus = false;
        }

        return updateStatus;
    }

    public Color getCellColor(String sheet, int rowNum, int colNum) {

        Sheet sh = workBook.getSheet(sheet);
        CellStyle cs = sh.getRow(rowNum).getCell(colNum).getCellStyle();

        Color color = cs.getFillForegroundColorColor();
        return color;
    }

    public static synchronized Boolean editRowDataUsingColumnId(String excelFilePath, String excelFileName, String sheetName, int rowNumber, Map<String, Object> columnDataMap) {
        try {
            logger.info("Editing Excel File {}, Sheet {}, Row {}", excelFilePath + "/" + excelFileName, sheetName, rowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);

            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row row = sheet.getRow(rowNumber);
            if (row == null) {
                sheet.createRow(rowNumber);
                row = sheet.getRow(rowNumber);
            }

            Row columnRow = sheet.getRow(1);

            int columnIndex = -1;
            int columnCount = columnRow.getLastCellNum();
            if (columnCount == 0)
                logger.info("Column count is zero");

            Map<String, String> rowData = new HashMap<>();

            while (++columnIndex < columnCount) {
                Cell cell = columnRow.getCell(columnIndex);
                String value = cell.getStringCellValue();
                if (columnDataMap.containsKey(value)) {
                    Cell cellData = row.getCell(columnIndex);

                    if (cellData == null) {
                        row.createCell(columnIndex);
                    }
                    cellData = row.getCell(columnIndex);

                    if (columnDataMap.get(value) instanceof String) {
                        cellData.setCellValue((String) columnDataMap.get(value));
                    }
                    if (columnDataMap.get(value) instanceof Date) {

                        CellStyle cellStyle = workBook.createCellStyle();
                        CreationHelper createHelper = workBook.getCreationHelper();
                        cellStyle.setDataFormat(
                                createHelper.createDataFormat().getFormat("dd-MMM-yy"));
                        cellData.setCellValue((Date) columnDataMap.get(value));
                        cellData.setCellStyle(cellStyle);
                    }
                    if (columnDataMap.get(value) instanceof Double) {
                        cellData.setCellValue((Double) columnDataMap.get(value));
                    }
                    if (columnDataMap.get(value) instanceof Integer) {
                        cellData.setCellValue((Integer) columnDataMap.get(value));
                    }
                    if (columnDataMap.get(value) instanceof Float) {
                        cellData.setCellValue((Float) columnDataMap.get(value));
                    }
                    if (columnDataMap.get(value) instanceof Boolean) {
                        cellData.setCellValue((Boolean) columnDataMap.get(value));
                    }
                }
            }
            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Editing Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return false;
        }
        return true;
    }

    public static synchronized Map<String, String> getRowDataUsingColumnId(String excelFilePath, String excelFileName, String sheetName, int rowNumber) {

        Map<String, String> rowData = new HashMap<>();
        try {
            logger.info("Extracting Excel File {}, Sheet {}, Row {}", excelFilePath + "/" + excelFileName, sheetName, rowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row row = sheet.getRow(rowNumber);
            Row columnRow = sheet.getRow(1);

            int columnIndex = -1;
            int columnCount = columnRow.getLastCellNum();
            if (columnCount == 0)
                logger.info("Column count is zero");

            while (++columnIndex < columnCount) {
                Cell cell = columnRow.getCell(columnIndex);
                String key = cell.getStringCellValue();

                Cell cellData = row.getCell(columnIndex);

                if (cellData == null)
                    continue;

                if (cellData.getCellTypeEnum() == CellType.NUMERIC)
                    rowData.put(key, String.valueOf(cellData.getNumericCellValue()));
                else if (cellData.getCellTypeEnum() == CellType.STRING)
                    rowData.put(key, cellData.getStringCellValue());
                else if (cellData.getCellTypeEnum() == CellType.BOOLEAN)
                    rowData.put(key, String.valueOf(cellData.getDateCellValue()));

            }

            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Extracting Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return rowData;
        }
        return rowData;
    }

    public static synchronized List<CellType> getAllCellTypeOfRow(String excelFilePath, String excelFileName, String sheetName, int rowNumber) {

        List<CellType> rowData = new ArrayList<>();
        try {
            logger.info("Extracting Excel File {}, Sheet {}, Row {}", excelFilePath + "/" + excelFileName, sheetName, rowNumber);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row row = sheet.getRow(rowNumber);
            Row columnRow = sheet.getRow(1);

            int columnIndex = -1;
            int columnCount = columnRow.getLastCellNum();
            if (columnCount == 0)
                logger.info("Column count is zero");

            while (++columnIndex < columnCount) {
                Cell cell = columnRow.getCell(columnIndex);
                String key = cell.getStringCellValue();

                Cell cellData = row.getCell(columnIndex);
                try {

                    rowData.add(cellData.getCellTypeEnum());

                }catch (Exception e){

                }

            }

            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Extracting Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return rowData;
        }
        return rowData;
    }

    public static synchronized Map<String, List<String>> getMasterSheetDataUsingColumnId(String excelFilePath, String excelFileName) {

        String sheetName = "Master Data";
        Map<String, List<String>> rowData = new HashMap<>();
        try {
            logger.info("Extracting Excel File {}, Sheet {}", excelFilePath + "/" + excelFileName, sheetName);
            FileInputStream excelFile = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            XSSFWorkbook workBook = new XSSFWorkbook(excelFile);
            XSSFSheet sheet = workBook.getSheet(sheetName);

            Row columnRow = sheet.getRow(1);
            Row stakeHolderRow = sheet.getRow(3);

            int columnIndex = -1;
            int columnCount = columnRow.getLastCellNum();
            if (columnCount == 0)
                logger.info("Column count is zero");
            int startingRowNo = 4;

            while (++columnIndex < columnCount) {
                List<String> columnData = new ArrayList<>();
                Cell cell = columnRow.getCell(columnIndex);
                String key = cell.getStringCellValue();

                Cell tempCell = stakeHolderRow.getCell(columnIndex);

                String stakeHolderColumns = "";
                if (tempCell != null)
                    stakeHolderColumns = tempCell.getStringCellValue();

                if (stakeHolderColumns.length() > 0) {
                    XSSFSheet sheetStakeHolder = workBook.getSheet("Stakeholders");

                    int rowNo = 2;
                    List<String> oneRowData = new ArrayList<>();

                    Iterator<Row> rowIterator = sheetStakeHolder.iterator();
                    Row row = null;
                    for ( int i = 0; i < rowNo; i++ )
                        row = rowIterator.next();

                    int noOfColumns = row.getLastCellNum();
                    for ( int j = 0; j < noOfColumns; j++ ) {
                        Cell cell1 = row.getCell(j);
                        if (cell1 == null) {
                            oneRowData.add(null);
                            continue;
                        }

                        if (cell1.getCellTypeEnum() == CellType.NUMERIC)
                            oneRowData.add(Double.toString(cell1.getNumericCellValue()));
                        else if (cell1.getCellTypeEnum() == CellType.STRING)
                            oneRowData.add(cell1.getStringCellValue().trim());
                        else if (cell1.getCellTypeEnum() == CellType.BOOLEAN)
                            oneRowData.add(Boolean.toString(cell1.getBooleanCellValue()));
                        else if (cell1.getCellTypeEnum() == CellType.BLANK)
                            oneRowData.add("");
                    }

                    for ( int i = 0; i < oneRowData.size(); i++ ) {
                        if (stakeHolderColumns.contains(oneRowData.get(i))) {
                            Iterator<Row> rowIterator1 = sheetStakeHolder.iterator();
                            rowNo = 0;
                            while (rowNo < startingRowNo && rowIterator1.hasNext()) {
                                rowIterator1.next();
                                rowNo++;
                            }
                            while (rowIterator1.hasNext()) {
                                row = rowIterator1.next();
                                if (row.getCell(i) == null) {
                                    break;
                                }

                                columnData.add(row.getCell(i).toString());
                            }
                        }
                    }

                } else {

                    Iterator<Row> rowIterator = sheet.iterator();
                    int rowNo = 0;
                    while (rowNo < startingRowNo && rowIterator.hasNext()) {
                        rowIterator.next();
                        rowNo++;
                    }

                    int i = 0;
                    Row row;
                    while (rowIterator.hasNext()) {
                        row = rowIterator.next();
                        if (row.getCell(columnIndex) == null) {
                            break;
                        }

                        columnData.add(row.getCell(columnIndex).toString());
                        i++;
                    }
                }

                rowData.put(key, columnData);

            }

            excelFile.close();
            FileOutputStream out = new FileOutputStream(new File(excelFilePath + "/" + excelFileName));
            workBook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("Exception while Extracting Row Data for Excel File {} and Sheet {}. {}", excelFilePath + "/" + excelFileName, sheetName, e.getStackTrace());
            return rowData;
        }
        return rowData;
    }

    public static List<List<String>> getAllExcelDataColumnWiseWithoutSheetName(String filePath, String fileName) {
        List<List<String>> allData = new ArrayList<>();

        try {
            logger.info("Getting All Excel Data Column Wise from File {} and Sheet {}", filePath + "/" + fileName);
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            int sheetNumber = workbook.getNumberOfSheets();
            for ( int i = 0; i <= sheetNumber; i++ ) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                if (sheet == null) {
                    logger.error("Couldn't locate Sheet {} in File {}.", sheet.getSheetName(), fileName);
                    return null;
                }
                int noOfRows = sheet.getPhysicalNumberOfRows();
                int noOfColumns = sheet.getRow(0).getPhysicalNumberOfCells();
                for ( int columnNo = 0; columnNo < noOfColumns; columnNo++ ) {
                    List<String> columnDataList = new ArrayList<>();
                    for ( int rowNo = 0; rowNo < noOfRows; rowNo++ ) {
                        Cell cell = sheet.getRow(rowNo).getCell(columnNo);
                        if (cell == null) {
                            columnDataList.add("");
                            continue;
                        }
                        if (cell.getCellTypeEnum() == CellType.NUMERIC)
                            columnDataList.add(Double.toString(cell.getNumericCellValue()));
                        else if (cell.getCellTypeEnum() == CellType.STRING)
                            columnDataList.add(cell.getStringCellValue());
                        else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                            columnDataList.add(Boolean.toString(cell.getBooleanCellValue()));
                        else if (cell.getCellTypeEnum() == CellType.BLANK)
                            columnDataList.add("");
                    }
                    allData.add(columnDataList);
                }
                file.close();
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Excel Data Column Wise from File {} and Sheet {}. {}", fileName, e.getStackTrace());
        }
        return allData;
    }

    public static List<List<String>> getExcelDataOfAllRowsAllSheets(String filePath, String fileName) {
        List<List<String>> allData = new ArrayList<>();
        List<String> oneRowData;
        try {
            FileInputStream file = new FileInputStream(new File(filePath + "//" + fileName));
            XSSFWorkbook workbook = new XSSFWorkbook(file);

            List<String> sheetNames = new ArrayList<String>();
            for ( int i = 0; i < workbook.getNumberOfSheets(); i++ ) {
                // if (workbook.getSheetName(i).toLowerCase().contains(("UX2.0_2.04_rc").toLowerCase())) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                if (sheet == null) {
                    logger.warn("Couldn't locate Sheet {} in File {}.", i, fileName);
                    return allData;
                }
                for ( int j = 0; j <= sheet.getLastRowNum(); j++ ) {

                    Row row = sheet.getRow(j);
                    oneRowData = new ArrayList<>();


                    if (row != null) {
                        for ( int k = 0; k <= row.getLastCellNum(); k++ ) {
                            Cell cell = row.getCell(k);
                            if (cell == null) {
                                continue;
                            }
                            if (cell.getCellTypeEnum() == CellType.NUMERIC)
                                oneRowData.add(Double.toString(cell.getNumericCellValue()));
                            else if (cell.getCellTypeEnum() == CellType.STRING)
                                oneRowData.add(cell.getStringCellValue());
                            else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                                oneRowData.add(Boolean.toString(cell.getBooleanCellValue()));
                            else if (cell.getCellTypeEnum() == CellType.BLANK)
                                continue;
                        }
                    }
                    allData.add(oneRowData);
                }
                //}
            }

            file.close();
        } catch (Exception e) {
            logger.error("Exception while getting Excel Data for", fileName, e.getStackTrace());
        }
        return allData;
    }

    public String getProcessingStatus ( int rowNumber, String sheetName){
        try {
            int columnCount = getColumnCount(sheetName);
            return getCellData(sheetName, columnCount - 1, rowNumber);

        } catch (Exception e) {
            logger.error("Exception while Extracting Row Data for Excel File {} and Sheet {}. {}", filePath + fileName, sheetName, e.getStackTrace());
        }
        return null;
    }

    public static int getExcelColNumber(String filePath,String fileName, String sheetName, int rowNo,String colValue){

        int colNumber = -1;
        try{

            List<String> excelData = getExcelDataOfOneRow(filePath,fileName,sheetName,rowNo);

            for(int i =0;i<excelData.size();i++){
                if(excelData.get(i).equals(colValue)){
                    colNumber = i;
                    break;
                }
            }

        }catch (Exception e){
            logger.error("Exception while getting columnNumber");
        }
        return colNumber;
    }

    public static int getNoOfRowsStream(String excelFilePath, String excelFileName, String sheetName) {
        int noOfRows = 0;
        try {
            ZipSecureFile.setMinInflateRatio(0);
            InputStream file = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
//            XSSFWorkbook workbook = new XSSFWorkbook(file);
            Workbook workbook = StreamingReader.builder()
                    .rowCacheSize(100)    // number of rows to keep in memory
//            (defaults to 10)
                        .bufferSize(8192)     // buffer size to use when reading
//            InputStream to file (defaults to 1024)
                    .open(file);
            Sheet sheet = workbook.getSheet(sheetName);

            Iterator<Row> row = sheet.rowIterator();

            while (row.hasNext()) {
                row.next();
                noOfRows++;
            }

        } catch (Exception e) {
            logger.error("Exception while getting Total No of Rows in Excel Sheet {}. {}", sheetName, e.getStackTrace());
        }
        return noOfRows;
    }

}