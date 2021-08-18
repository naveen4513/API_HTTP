package com.sirionlabs.test.WeeklyReminder;
import com.sirionlabs.api.clientAdmin.todoParams.TodoParamsCreateForm;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WeeklyReminderHelper.WeeklyReminderHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestWeeklyReminder {
    private final static Logger logger = LoggerFactory.getLogger(TestWeeklyReminder.class);
    private String weeklyReminderConfigFilePath;
    private String weeklyReminderConfigFileName;
    private String prefixFileName;
    private String postfixFileName;
    private String outputFilePath;
    private String remoteHost;
    private String remotePort;
    private String remoteUserName;
    private String remotePassword;
    private String remoteFilePath;
    private String toMail;
    private ShowHelper ShowHelperObj = new ShowHelper();
    private  WeeklyReminderHelper WeeklyReminderHelperObj=new WeeklyReminderHelper();
    @BeforeClass
    public void beforeClass() {
        weeklyReminderConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestWeeklyReminderConfigFilePath");
        weeklyReminderConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestWeeklyReminderConfigFileName");
        prefixFileName = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "prefix");
        postfixFileName = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "postfix");
        toMail = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "tomail");
        outputFilePath = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "outputFilePath");
        remoteHost = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "host");
        remotePort = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "port");
        remoteUserName = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "user");
        remotePassword = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "password");
        remoteFilePath = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, "remotefilepath");

    }

    //TC C10276:The count and data of entities should be equal to the data shown in reminder email tab for the user
    @Test(enabled = false)
    public void testWeeklyReminder() {
        CustomAssert customAssert = new CustomAssert();
        try {
            String currentDate = new SimpleDateFormat("yyyyMMMdd").format(new Date());
            String weeklyReportName = prefixFileName +currentDate+ postfixFileName;
            logger.info("Looking for weekly report Name: "+weeklyReportName);
            Boolean weeklyReminderSendSuccessfully = new WeeklyReminderHelper().isWeeklyReminderSentSuccessfully(weeklyReportName, toMail, new AdminHelper().getClientId());
            if (!weeklyReminderSendSuccessfully) {
                customAssert.assertTrue(false, "Weekly Reminder was not sent successfully");
            }

            WeeklyReminderHelper weeklyReminderHelper = new WeeklyReminderHelper(remoteHost,Integer.parseInt(remotePort), remoteUserName,remotePassword);
            Boolean fileDownloaded = weeklyReminderHelper.downloadFileFromRemoteServer(remoteFilePath + "/" + weeklyReportName, outputFilePath);
            if (!fileDownloaded) {

                customAssert.assertTrue(false, "Couldn't Download Weekly Reminder  excel report  successfully");
                throw new SkipException("Couldn't Download Weekly Reminder  excel report  successfully");
            }
            List<String> allSheetNames = getAllExcelFileFromWeeklyReminder(outputFilePath, weeklyReportName, customAssert);

            logger.info("All sheet names from downloaded sheet:" +allSheetNames);
            String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

            if (allSheetNames != null && !allSheetNames.isEmpty()) {
                for (String SheetName : allSheetNames) {

                    logger.info("Verifying excel count and total list count for  "+SheetName);
                    int entityTypeId=Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName,SheetName,"entity_type_id"));
                    verifyExcelCountAndTotalCount( outputFilePath, weeklyReportName, SheetName,entityTypeId, customAssert);

                }
            }
            //Delete Downloaded file
            FileUtils.deleteFile(outputFilePath + "/" + weeklyReportName);
        } catch (Exception e) {
            logger.error("Exception while Verifying Total Count And Excel Count");
            customAssert.assertTrue(false, "Exception while Verifying Total Count And Excel Count");
        }
        customAssert.assertAll();
    }

    //TC C90166: Check Supplier Column in Weekly reminder Email attachment
    @Test(enabled = false)
    public void testC90166()
    {
        CustomAssert customAssert = new CustomAssert();
        try {
            String currentDate = new SimpleDateFormat("yyyyMMMdd").format(new Date());
            String weeklyReportName = prefixFileName +currentDate+ postfixFileName;
            HashMap<String, String> dataToValidate = new HashMap<>();

           Boolean weeklyReminderSendSuccessfully = new WeeklyReminderHelper().isWeeklyReminderSentSuccessfully(weeklyReportName, toMail, new AdminHelper().getClientId());
            if (!weeklyReminderSendSuccessfully) {
                customAssert.assertTrue(false, "Weekly Reminder was not sent successfully");
            }
            else {
                WeeklyReminderHelper weeklyReminderHelper = new WeeklyReminderHelper(remoteHost, Integer.parseInt(remotePort), remoteUserName, remotePassword);
                Boolean fileDownloaded = weeklyReminderHelper.downloadFileFromRemoteServer(remoteFilePath + "/" + weeklyReportName, outputFilePath);

            if (!fileDownloaded) {

                customAssert.assertTrue(false, "Couldn't Download Weekly Reminder  excel report  successfully");
                throw new SkipException("Couldn't Download Weekly Reminder  excel report  successfully");
            }

            List<String> allSheetNames = getAllExcelFileFromWeeklyReminder(outputFilePath, weeklyReportName, customAssert);

            logger.info("All sheet names from downloaded sheet:" + allSheetNames);
            if (allSheetNames != null && !allSheetNames.isEmpty()) {
                for (String sheetName : allSheetNames) {
                    if (sheetName.equalsIgnoreCase("CONTRACTS")) {
                        Boolean isColumnPresent = isSupplierColumnPresentInAttachment(outputFilePath, weeklyReportName, sheetName);
                        if (!isColumnPresent) {
                            customAssert.assertTrue(false, "Supplier column is not present in email attachment");
                        } else {
                            int idColumn = getColumnIndex(outputFilePath, weeklyReportName, sheetName, "Id");
                            int supplierCol = getColumnIndex(outputFilePath, weeklyReportName, sheetName, "Supplier");

                            ArrayList<String> supplierList = WeeklyReminderHelperObj.getColumnData(outputFilePath, weeklyReportName, sheetName, supplierCol);
                            ArrayList<String> idList = WeeklyReminderHelperObj.getColumnData(outputFilePath, weeklyReportName, sheetName, idColumn);

                            for (int i = 0; i < supplierList.size() - 1; i++) {
                                dataToValidate.put(idList.get(i), supplierList.get(i));
                            }

                            for (String s : dataToValidate.keySet()) {
                                int entityId = Integer.parseInt(s.substring(2, s.length()));
                                List<String> actualSupplier = new ArrayList<>();
                                List<String> supplierFromSheet = new ArrayList<>();

                                int recordId = WeeklyReminderHelperObj.getRecordId(entityId);
                                String showResponse = ShowHelperObj.getShowResponseVersion2(61, recordId);
                                if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                    if (showResponse.contains("Either you do not have the required permissions or requested page does not exist anymore.")) {
                                        logger.info("Either you do not have the required permissions or requested page does not exist anymore for Entity Id " + s);
                                    } else {
                                        JSONObject jsonObj = new JSONObject(showResponse);

                                        JSONObject obj1 = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("relations");
                                        JSONArray objArr = obj1.getJSONArray("values");
                                        if (objArr.length() > 0)
                                            for (int j = 0; j < objArr.length(); j++) {
                                                actualSupplier.add(objArr.getJSONObject(j).get("name").toString());

                                            }

                                    }
                                }

                                if (actualSupplier.size() > 1) {
                                    String suppArr[] = dataToValidate.get(s).split(",");
                                    for (String sup : suppArr) {
                                        supplierFromSheet.add(sup);
                                    }
                                    Collections.sort(supplierFromSheet);
                                    Collections.sort(actualSupplier);
                                    customAssert.assertEquals(actualSupplier, supplierFromSheet, "Multi supplier name does't match for the record " + recordId + "and contract Id " + s);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }
                    }

                    }
                }
            }

            }

        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Validating C90166 " +e.getMessage());
        }

       customAssert.assertAll();

    }

    //TC C90165: Check Supplier Column for multi supplier contracts in Reminder email List from MyProfile
    @Test
    public void testC90165()
    {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test: Validating supplier column for multi supplier contracts");
        String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        String entityName = "Contracts";
        try
        {
            int entityTypeId=Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName,entityName,"entity_type_id"));
            String listId = (ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "entity_url_id"));
            logger.info("Getting list Id for the entity: " +listId);

            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}},\"reminderEmailRequest\":true}";
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2WithOutParams(listId, payload);
            String listDataJsonStr = listRendererListData.getListDataJsonStr();

            logger.info("list Data API Response is: "+listDataJsonStr);

            if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                logger.info("checking for whether multi supplier contract field is present or not");
                Boolean isMultisupplierContractPresent = isFieldPresentInListDataAPIResponse(listDataJsonStr, "multisuppliercontracts");

                if (isMultisupplierContractPresent == null) {
                    throw new SkipException("Couldn't find whether MultiSupplier Contract Field is present or not in ListData API Response");
                }
                if (!isMultisupplierContractPresent) {
                    logger.info("Multi supplier contract field is not present in reminder email for contract listing");
                    csAssert.assertTrue(false, "Multi supplier contract field is not present in reminder email for contract listing");
                }

            }
            else {
                csAssert.assertTrue(false, "List data API response is an Invalid JSON.");
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C90165 " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test
    public void testC140769() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C140769: Validating status for entities in weekly reminder listing page");
        String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        String[] entitiesArr = {"Actions","Change Requests","contract draft request", "Contracts", "Disputes","Interpretations","Work Order Requests"};

        for (String entityName : entitiesArr) {
            logger.info("Validating Status for entity Name : "+entityName);
            try {
                Map<Integer,String> statusToValidate=new HashMap<>();
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                String listId = (ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "entity_url_id"));
                logger.info("Getting list Id for the entity " + entityName + " :" + listId);

                String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":2,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}},\"reminderEmailRequest\":true}";
                ListRendererListData listRendererListData = new ListRendererListData();
                listRendererListData.hitListRendererListDataV2WithOutParams(listId, payload);
                String listDataJsonStr = listRendererListData.getListDataJsonStr();

                logger.info("list Data API Response for " + entityName + " is " + listDataJsonStr);

                if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                    JSONObject jsonObj = new JSONObject(listDataJsonStr);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");
                    int n = jsonArr.length();

                    if(n>0) {
                        for (int i = 0; i < n; i++) {
                            String actualStatusName = "";
                            int recordId = -1;
                            jsonObj = jsonArr.getJSONObject(i);
                            for (String columnId : JSONObject.getNames(jsonObj)) {

                                if (jsonObj.getJSONObject(columnId).getString("columnName").trim().equalsIgnoreCase("status")) {
                                    actualStatusName = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnId)).getString("value");

                                }
                                if (jsonObj.getJSONObject(columnId).getString("columnName").trim().equalsIgnoreCase("id")) {
                                    String[] value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnId)).getString("value").trim().split(":;");
                                    if (value.length > 1) {
                                        recordId = Integer.parseInt(value[1]);
                                    }
                                }
                                if(recordId!=-1 && !(actualStatusName.equalsIgnoreCase(""))) {
                                    statusToValidate.put(recordId, actualStatusName);
                                }
                            }
                        }
                        //Validate Status for Record
                        logger.info("Validating status for Entity Type "+entityName);
                        List<String> allSelectedStatus = getTodoParamsResponse(entityTypeId);
                        for (Integer recordId : statusToValidate.keySet()) {
                            logger.info("Validating status for Entity "+entityName +" RecordId "+recordId);
                            validateStatusField(entityName, allSelectedStatus, recordId, statusToValidate.get(recordId), csAssert);
                        }
                    }
                    else {
                        logger.info("No records found in Reminder Email List for Entity "+entityName);
                    }
                }
            } catch (Exception e) {
                logger.info("Exception occurred while validating TC- C140769" + e.getMessage());
            }
        }
        csAssert.assertAll();
    }

    /* TC-C152309 :Check Name Column in Weekly reminder Email attachment for Consumption.
    */
    @Test(enabled = false)
    public void testC152309()
    {
        CustomAssert customAssert = new CustomAssert();
        try{
            String weeklyReportName=" ";
            List<String> allSheetNames = getAllExcelFileFromWeeklyReminder(outputFilePath, weeklyReportName, customAssert);
            logger.info("All sheet names from downloaded sheet:" + allSheetNames);
            if (allSheetNames != null && !allSheetNames.isEmpty())
            {
                for(String sheetName:allSheetNames)
                {
                    if(sheetName.equalsIgnoreCase("consumption"))
                    {
                        Boolean isColumnPresent = isNameColumnPresentInAttachment(outputFilePath, weeklyReportName, sheetName);
                        if (!isColumnPresent) {
                            customAssert.assertTrue(false, "Name column is not present in sheet Consumption");
                        }
                        else {
                            Map<String,String> recordsToValidate=new HashMap<>();
                            int nameColumn = getColumnIndex(outputFilePath, weeklyReportName, sheetName, "Name");
                            int idColumn = getColumnIndex(outputFilePath, weeklyReportName, sheetName, "Id");

                            ArrayList<String> NameList = WeeklyReminderHelperObj.getColumnData(outputFilePath, weeklyReportName, sheetName, nameColumn);
                            ArrayList<String> idList = WeeklyReminderHelperObj.getColumnData(outputFilePath, weeklyReportName, sheetName, idColumn);
                            for (int i = 0; i < NameList.size() - 1; i++) {
                                recordsToValidate.put(idList.get(i), NameList.get(i));
                            }
                            logger.info("Validating Name from Consumption sheet");
                            for(String id:recordsToValidate.keySet())
                            {
                                logger.info("Validating Name from Consumption sheet for entity Id "+id);
                                String actualConsumptionName=recordsToValidate.get(id);
                                String expectedConsumptionName=" ";
                                int entityId = Integer.parseInt(id.substring(3, id.length()));

                                int recordId = WeeklyReminderHelperObj.getRecordIdForConsumption(entityId);
                                String showResponse = ShowHelperObj.getShowResponseVersion2(176, recordId);
                                if (ParseJsonResponse.validJsonResponse(showResponse)){
                                    JSONObject jsonObj = new JSONObject(showResponse);

                                    JSONObject obj1 = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("name");
                                    expectedConsumptionName=obj1.getString("values");
                                }
                                if (!(actualConsumptionName.equalsIgnoreCase(expectedConsumptionName)))
                                {
                                    customAssert.assertEquals(false,"Consumption Name does't match for the record " + recordId);

                                }
                                Thread.sleep(1000);

                            }

                        }
                    }
                }
            }

        }
        catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Validating TC-C152309 " +e.getMessage());
        }
        customAssert.assertAll();
    }

    public static List<String> getTodoParamsResponse(int entityTypeId) {
        String createFormResponse = TodoParamsCreateForm.getCreateFormResponse(TodoParamsCreateForm.getAPIPath(20), TodoParamsCreateForm.getHeaders());
        Map<String, List<String>> allSelectedStatusMap = WeeklyReminderHelper.getAllSelectedStatusForEmailReminder(createFormResponse,entityTypeId);
        List<String> allSelectedStatus = allSelectedStatusMap.get(getEntityLabelNameFromEntityId(entityTypeId));
        logger.info("all Selected status for the entityType Id " + entityTypeId + " are :" + allSelectedStatus);
        return allSelectedStatus;
    }

    private void validateStatusField(String entityName, List<String> allSelectedStatus, int recordId,String actualStatusName, CustomAssert csAssert) {
        try {
            if (allSelectedStatus == null) {
                csAssert.assertTrue(false, "Couldn't get All Selected Status for Entity " + entityName);
                return;
            }

            if (!allSelectedStatus.contains(actualStatusName)) {
                csAssert.assertTrue(false, "Exception while Validating Status field for Entity: " + entityName );

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Status field for Entity: " + entityName + " and Record Id: " +recordId+
                    ". " + e.getMessage());
        }
    }

    private static String getEntityLabelNameFromEntityId(int entityTypeId) {
        String entityLabel = null;

        switch (entityTypeId) {
            case 18:
                entityLabel = "Actions Manager";
                break;

            case 63:
                entityLabel="Change Requests Manager";
                break;
            case 160:
                entityLabel="Contract Draft Request Manager";
                break;
            case 16:
                entityLabel="Interpretations Manager";
                break;
            case 140:
                entityLabel="Contract Template Manager";
                break;
            case 138:
                entityLabel="Clause Manager";
                break;
            case 61:
                entityLabel="Contract Manager";
                break;
            case 80:
                entityLabel="Work Order Request Manager";
                break;

            case 28:
                entityLabel="Disputes Manager";
                break;
            case 67:
                entityLabel="Invoice Manager";
                break;
        }

        return entityLabel;
    }

    public Boolean isFieldPresentInListDataAPIResponse(String listDataResponse, String columnName) {
        try {
            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                int n=jsonArr.length();
                logger.info("Validating multisupplier Contracts column Name");
                if (n > 0) {
                    List<String> Values=new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        jsonObj = jsonArr.getJSONObject(i);
                        for (String columnId : JSONObject.getNames(jsonObj)) {

                            if (jsonObj.getJSONObject(columnId).getString("columnName").trim().equalsIgnoreCase(columnName)) {

                                String value=jsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnId)).getString("value");
                                if(value.equalsIgnoreCase("Yes")|| value.equalsIgnoreCase("No"))
                                {
                                    Values.add(value);
                                }
                            }
                        }
                    }

                    if(Values.size()==n)
                    {
                        logger.info("Multi supplier contract field is present on email reminder listing page for contract");
                        return true;
                    }
                    return false;
                }
                return null;

            } else {
                logger.error("listData API Response is an Invalid JSON. Hence couldn't check Field having ColumnName [{}] ", columnName);
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Field having columnName [{}] is Present in List data API Response or not. {}",
                    e.getStackTrace());
        }
        return null;
    }
    private void verifyExcelCountAndTotalCount(String outputFilePath, String weeklyReportName, String sheetName,int entityTypeId, CustomAssert customAssert) throws Exception {

        try {
            int totalCount = 0;
            Long noOfRows = getTotalNoOfRows(outputFilePath, weeklyReportName, sheetName);
            logger.info("number of rows for the entity "+ sheetName.toLowerCase() +" is " +noOfRows);
            //String listId = ParseConfigFile.getValueFromConfigFile(weeklyReminderConfigFilePath, weeklyReminderConfigFileName, sheetName.toLowerCase());
            String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
            String listId = (ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, sheetName, "entity_url_id"));

            logger.info("Getting list Id for the entity: " +listId);
            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}},\"reminderEmailRequest\":true}";
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2WithOutParams(listId, payload);
            String listDataJsonStr = listRendererListData.getListDataJsonStr();
            logger.info("list data response is: "+listDataJsonStr);
            if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                totalCount = new JSONObject(listDataJsonStr).getInt("totalCount");

            }
            if(totalCount<1000) {
                logger.info("total count is than 1000 i.e. " + totalCount);
                if (!((noOfRows - 3) == totalCount)) {
                    customAssert.assertTrue(false, "Total Count And Excel Count Are Different");
                }
            }
            else if(totalCount>1000)
            {
                logger.info("total count is more 1000 i.e. " + totalCount);
                totalCount=1000;
                if (!((noOfRows - 3) == totalCount)) {
                    customAssert.assertTrue(false, "Total Count And Excel Count Are Different");
                }
            }
            else
            {
                logger.info("total count is 0");
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Verifying Total Count And Excel Count");
            throw new Exception("Exception while Verifying Total Count And Excel Count");
        }
    }

    private List<String> getAllExcelFileFromWeeklyReminder(String outputFilePath, String weeklyReportName, CustomAssert customAssert) throws Exception {

        try {

            XLSUtils xlsUtils=new XLSUtils(outputFilePath,weeklyReportName);
            List<String> allSheetNames=xlsUtils.getSheetNames();

            return  allSheetNames;


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Getting All Sheet Name");
            throw new Exception("Exception while Getting All Sheet Name");
        }
    }
    public static Long getTotalNoOfRows(String excelFilePath, String excelFileName, String sheetName) {
        Long noOfRows = -1L;
        try {
            ZipSecureFile.setMinInflateRatio(0);
            FileInputStream file = new FileInputStream(new File(excelFilePath + "/" + excelFileName));
            HSSFWorkbook workbook = new HSSFWorkbook(file);
            HSSFSheet sheet = workbook.getSheet(sheetName);

            noOfRows = Integer.toUnsignedLong(sheet.getPhysicalNumberOfRows());
        } catch (Exception e) {
            logger.error("Exception while getting Total No of Rows in Excel Sheet {}. {}", sheetName, e.getStackTrace());
        }
        return noOfRows;
    }

    public Boolean isSupplierColumnPresentInAttachment(String outputFilePath, String weeklyReportName,String sheetName ) throws IOException {
        String columnName="";

        logger.info("looking for supplier column in "+ sheetName+" Sheet");
        XLSUtils xlsUtils=new XLSUtils(outputFilePath,weeklyReportName);

        for(int i=0;i<3;i++)
        {
            for(int j=0;j<10;j++) {
                columnName = xlsUtils.getCellData(sheetName, j, i);
                if(columnName.equalsIgnoreCase("Supplier"));
                {
                    logger.info("Supplier Column is present in Weekly reminder Email attachment ");
                    return true;

                }
            }
        }

        return false;
    }
    public Boolean isNameColumnPresentInAttachment(String outputFilePath, String weeklyReportName,String sheetName ) throws IOException {
        String columnName="";

        logger.info("looking for Name column in "+ sheetName+" Sheet");
        XLSUtils xlsUtils=new XLSUtils(outputFilePath,weeklyReportName);
        for(int i=0;i<3;i++) {
            for(int j=0;j<10;j++) {
                columnName = xlsUtils.getCellData(sheetName,j,i);
                if(columnName.equalsIgnoreCase("Name"))
                {
                    return true;
                }
            }
        }
        return false;

    }

    public int getColumnIndex(String outputFilePath, String weeklyReportName,String sheetName,String colName) throws IOException {
        String columnName="";
        XLSUtils xlsUtils=new XLSUtils(outputFilePath,weeklyReportName);

        for(int i=0;i<3;i++)
        {
            for(int j=0;j<10;j++) {
                columnName = xlsUtils.getCellData(sheetName, j, i);
                if(columnName.equalsIgnoreCase(colName))
                {
                    return j;

                }
                else if(columnName.equalsIgnoreCase(colName))
                {
                    return j;
                }
            }
        }

       return 0;
    }


}
