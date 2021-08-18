package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.ServiceData;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestServiceDataBulkUpdate {
    private static Logger logger = LoggerFactory.getLogger(TestServiceDataBulkUpdate.class);
    private String configFilePath, configFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;
    private List<String> serviceDataIds = new ArrayList<>();


    @BeforeClass
    public void BeforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestServiceDataBulkUpdateConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestServiceDataBulkUpdateConfigFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        createServiceData(2);
    }
    private void createServiceData(int number){
        while (number-->0){
            //String response = ServiceData.createServiceData(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,"bulk update",true);
            int serviceDataId  = InvoiceHelper.getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,"bulk update",Integer.parseInt(ParseConfigFile.getValueFromConfigFile(serviceDataConfigFilePath,serviceDataConfigFileName,"bulk update","sourceid")));
            serviceDataIds.add(String.valueOf(serviceDataId));
        }
    }

    @Test(enabled = false)
    public void serviceDataBUlkUpdate() throws ParseException {

        CustomAssert customAssert = new CustomAssert();

        String downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
        String downloadFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilename");
        int templateId = 1027;
        int entityTypeId = 64;

        int entityListRenderId = 352;
        ListRendererListData listRendererListData = new ListRendererListData();
        listRendererListData.hitListRendererListData(entityListRenderId);
        String serviceDataListResponse = listRendererListData.getListDataJsonStr();

        JSONObject serviceDataListJSONObject = new JSONObject(serviceDataListResponse);
        JSONArray serviceDataListJSONArray = serviceDataListJSONObject.getJSONArray("data");
        ParseJsonResponse parseJsonResponse = new ParseJsonResponse();

        List<String> serviceDataIdsList = new ArrayList<>();

        for (int index = 0; index < serviceDataListJSONArray.length(); index++) {
            parseJsonResponse.getNodeFromJsonWithValue(serviceDataListJSONArray.getJSONObject(index), Collections.singletonList("columnName"), "id");
            String value;

            if (parseJsonResponse.getJsonNodeValue() instanceof String)
                value = (String) parseJsonResponse.getJsonNodeValue();
            else
                continue;

            try {
                if (!value.isEmpty())
                    serviceDataIdsList.add(value.split(":;")[1]);
            } catch (IndexOutOfBoundsException e) {
                logger.error("Cannot found columnName id or id (in form of [28267:;29059]) value in service data list data ");
            }
        }
        logger.info("Service data ids length : {}", serviceDataIdsList.size());

        Download download = new Download();
        download.hitDownload(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath")
                , ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilename")
                , templateId, entityTypeId, Arrays.toString(serviceDataIdsList.toArray()).substring(1, Arrays.toString(serviceDataIdsList.toArray()).length() - 1));

        logger.info("Checking for downloaded file");

        int rowsToSkip = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rowstoskip"));
        Map<String, Map<String, Object>> dataToVerify = new HashMap<>();
        Map<String, Object> subDataToVerify;

        try {
            XLSUtils xlsUtils = new XLSUtils(downloadFilePath, downloadFileName);
            String serviceDataSheetName = "Service Data";
            int rowCount = xlsUtils.getRowCount(serviceDataSheetName);
            logger.info("Row count : {}", rowCount);

            Map<String, String> rowTypeMapping = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "rows to edit type");
            Map<Integer, Map<Integer, Object>> dataToFill = new HashMap<>();
            Map<Integer, Object> dataTemp;

            for (int index = rowsToSkip; index < rowCount; index++) {
                dataTemp = new HashMap<>();
                subDataToVerify = new HashMap<>();
                for (Map.Entry<String, String> entry : rowTypeMapping.entrySet()) {
                    List<String> listTemp = XLSUtils.getExcelDataOfOneRow(downloadFilePath
                            , downloadFileName
                            , serviceDataSheetName, 2);
                    int columnIndex;
                    for (columnIndex = 0; columnIndex < listTemp.size(); columnIndex++)
                        if (listTemp.get(columnIndex).equalsIgnoreCase(entry.getKey()))
                            break;

                    String typeTemp = entry.getValue();
                    if (typeTemp.equalsIgnoreCase("date")) {
                        String value = XLSUtils.getOneCellValue(downloadFilePath, downloadFileName, serviceDataSheetName, index, columnIndex);
                        Date updatedDate;
                        try {
                            updatedDate = getUpdatedDateValue(Double.parseDouble(value));
                        } catch (Exception e) {
                            updatedDate = getUpdatedDateValue(value);
                        }
                        dataTemp.put(columnIndex, updatedDate);
                        subDataToVerify.put(entry.getKey(), updatedDate);
                    }
                }
                dataToFill.put(index, dataTemp);
                dataToVerify.put(serviceDataIdsList.get(index - rowsToSkip), subDataToVerify);
            }

            boolean updateExcel = XLSUtils.editMultipleRowsData(downloadFilePath, downloadFileName, serviceDataSheetName, dataToFill);
            boolean allColumnsMatched;


            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            Map<String, Boolean> matchMap = new HashMap<>();
            if (updateExcel) {
                UploadBulkData uploadBulkData = new UploadBulkData();
                uploadBulkData.hitUploadBulkData(entityTypeId, templateId, downloadFilePath, downloadFileName, new HashMap<>()); //todo change the empty hashMap
                String uploadResponse = uploadBulkData.getUploadBulkDataJsonStr();
                if (uploadResponse.contains("200:;")) {

                    String result = waitForBulkUpdateTask(allTaskIds);
                    if (result.equalsIgnoreCase("pass")) {
                        for (Map.Entry<String, Map<String, Object>> entryServiceData : dataToVerify.entrySet()) {
                            Show show = new Show();
                            show.hitShowVersion2(entityTypeId, Integer.parseInt(entryServiceData.getKey()));
                            String showResponse = show.getShowJsonStr();
                            ParseJsonResponse parseJsonResponse2 = new ParseJsonResponse();
                            allColumnsMatched = false;
                            for (Map.Entry<String, Object> subEntryServiceData : entryServiceData.getValue().entrySet()) {
                                parseJsonResponse2.getNodeFromJsonWithValue(new JSONObject(showResponse).getJSONObject("body").getJSONObject("data"), Collections.singletonList("id"), Integer.parseInt(subEntryServiceData.getKey()));
                                String fieldValue = (String) parseJsonResponse2.getJsonNodeValue();
                                if (!fieldValue.equalsIgnoreCase(subEntryServiceData.getValue() instanceof String ? (String) subEntryServiceData.getValue() : (String) subEntryServiceData.getValue())) {
                                    allColumnsMatched = false;
                                    break;
                                } else {
                                    allColumnsMatched = true;
                                }
                            }
                            if (allColumnsMatched) {
                                matchMap.put(entryServiceData.getKey(), true);
                            }
                        }

                        for (Map.Entry<String, Map<String, Object>> entry : dataToVerify.entrySet()) {
                            customAssert.assertTrue(matchMap.get(entry.getKey()), "Service data id " + entry.getKey() + " not matched");
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Exception caught in editing the excel sheet");
        }


    }

    @DataProvider
    public Object[][] DataProviderFOrBulkUpdate() {

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstotest").split(",");
        Object[][] object = new Object[flowsToTest.length][];

        int index = 0;
        for (String s : flowsToTest) {
            object[index] = new String[]{s};
            index++;
        }

        return object;
    }

    @Test(dataProvider = "DataProviderFOrBulkUpdate", enabled = true)
    public void BulkUpdate(String flowsToTest) {
        CustomAssert customAssert = new CustomAssert();

        try {
            String downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilename");

            int templateId = -1, entityTypeId = -1, entityListRenderId = -1, excelStartIndex=-1;
            try {
                entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowsToTest, "entitytype"));
                excelStartIndex = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowsToTest, "excelstartindex"));
                templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateidmap", String.valueOf(entityTypeId)));
//                entityListRenderId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listrenderermap", String.valueOf(entityTypeId)));
            } catch (IllegalArgumentException e) {
                logger.error("Exception while extracting template id");
                throw new SkipException("Exception while extracting template id from config files");
            }

//            ListRendererListData listRendererListData = new ListRendererListData();
//            listRendererListData.hitListRendererListData(entityListRenderId);
//            String entityListResponse = listRendererListData.getListDataJsonStr();
//
//            JSONObject entityListJSONObject = new JSONObject(entityListResponse);
//            JSONArray entityListJSONArray = entityListJSONObject.getJSONArray("data");
//            ParseJsonResponse parseJsonResponse = new ParseJsonResponse();

            List<String> entityIdsList = new ArrayList<>();
            if(entityTypeId==64){
                entityIdsList = serviceDataIds;
            }

//            for (int index = 0; index < entityListJSONArray.length(); index++) {
//                parseJsonResponse.getNodeFromJsonWithValue(entityListJSONArray.getJSONObject(index), Collections.singletonList("columnName"), "id");
//                String value;
//
//                if (parseJsonResponse.getJsonNodeValue() instanceof String)
//                    value = (String) parseJsonResponse.getJsonNodeValue();
//                else
//                    continue;
//
//                try {
//                    if (!value.isEmpty())
//                        entityIdsList.add(value.split(":;")[1]);
//                } catch (IndexOutOfBoundsException e) {
//                    logger.error("Cannot found columnName id or id (in form of [28267:;29059]) value in service data list data ");
//                }
//            }
            logger.info("{} ids length : {}", ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowsToTest, "entitytype"), entityIdsList.size());

            Download download = new Download();
            download.hitDownload(downloadFilePath
                    , downloadFileName
                    , templateId, entityTypeId, Arrays.toString(entityIdsList.toArray()).substring(1, Arrays.toString(entityIdsList.toArray()).length() - 1));

            logger.info("Checking for downloaded file");

            String[] dateColumnIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datefields").split(",");
            List listDateColumnIds = Arrays.asList(dateColumnIds);

            Map<String, Object> dataMap = new HashMap<>();
            Map<String, String> map = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowsToTest);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (NumberUtils.isParsable(entry.getValue())) {
                    if (listDateColumnIds.contains(entry.getKey())) {
                        dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                    } else
                        dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                } else {
                    dataMap.put(entry.getKey(), entry.getValue());
                }
            }

            int rowIndex = -1;
            boolean editDone = true;
            String editAllRows = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowsToTest,"editallrows");
            if(editAllRows==null)
                throw new SkipException("Cannot find property [editallrows] in the config files");
            if(editAllRows.equalsIgnoreCase("false")) {
                List<Integer> rowsToEdit = calcExcelRowsToEdit(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowsToTest, "excelindexes"));
                for (int index : rowsToEdit){
                    editDone = XLSUtils.editRowDataUsingColumnId(downloadFilePath, downloadFileName, ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templatesheetmap", String.valueOf(entityTypeId)), index, dataMap);
                }
            }
            else {
                while (++rowIndex < entityIdsList.size()+excelStartIndex && editDone) {
                    editDone = XLSUtils.editRowDataUsingColumnId(downloadFilePath, downloadFileName, ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templatesheetmap", String.valueOf(entityTypeId)),rowIndex , dataMap);
                }
            }
            if (editDone)
                logger.debug("*****************Excel file edit completed*************");
            else
                throw new SkipException("Excel file edit failed");

            boolean allColumnsMatched;


            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            Map<String, Boolean> matchMap = new HashMap<>();
            UploadBulkData uploadBulkData = new UploadBulkData();
            Map<String,String> dataToVerify = new HashMap<>();

            dataToVerify = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,flowsToTest);
            List<String> readOnlyData = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "readonlyfields").split(","));

            Iterator<Map.Entry<String, String>> iterator = dataToVerify.entrySet().iterator();

            while (iterator.hasNext()){
                String currentKey = iterator.next().getKey();
                if(!NumberUtils.isParsable(currentKey)){
                    iterator.remove();
                }
                else if(listDateColumnIds.contains(currentKey)){
                    dataToVerify.put(iterator.next().getKey(),getDateValueInShowFormat(Double.parseDouble(iterator.next().getValue())));
                }
            }

            uploadBulkData.hitUploadBulkData(entityTypeId, templateId, downloadFilePath, downloadFileName, new HashMap<>()); //todo change the empty hashMap
            String actualResponse = uploadBulkData.getUploadBulkDataJsonStr();

            String expectedResponse = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowsToTest, "expectedresponse");

            if(!actualResponse.equalsIgnoreCase(expectedResponse)){
                customAssert.assertTrue(false,"Expected response is ["+expectedResponse+"], and actual result is ["+actualResponse+"]");
                customAssert.assertAll();
            }

            else {
                String successResponse = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"successresponse");
                if(!successResponse.equalsIgnoreCase(actualResponse)){
                    customAssert.assertAll();
                }
                else{
                    String result = waitForBulkUpdateTask(allTaskIds);
                    String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowsToTest,"schedulerjobstatus");
                    if(result.equalsIgnoreCase(expectedResult)){
                        logger.info("Expected scheduler task status [{}] and actual status [{}]",expectedResult,result);
                        if(result.equalsIgnoreCase("fail"))
                            customAssert.assertAll();
                    }
                    else {
                        logger.info("Expected scheduler task status [{}] and actual status [{}]",expectedResult,result);
                        customAssert.assertTrue(false,"Expected scheduler task status ["+expectedResponse+"] and actual status ["+result+"]");
                        customAssert.assertAll();
                    }
                    if (result.equalsIgnoreCase("pass")) {
                        for (String entityId : entityIdsList) {
                            Show show = new Show();
                            show.hitShowVersion2(entityTypeId, Integer.parseInt(entityId));
                            String showResponse = show.getShowJsonStr();
                            ParseJsonResponse parseJsonResponse2 = new ParseJsonResponse();
                            allColumnsMatched = false;
                            for (Map.Entry<String,String> entryData : dataToVerify.entrySet()) {
                                parseJsonResponse2.getNodeFromJsonForValueGeneric(new JSONObject(showResponse).getJSONObject("body").getJSONObject("data"), Collections.singletonList("id"), Integer.parseInt(entryData.getKey()));
                                String fieldValue = (String) parseJsonResponse2.getJsonNodeValue();
                                if(readOnlyData.contains(entryData.getKey())){
                                    if (fieldValue.equalsIgnoreCase(entryData.getValue())) {
                                        allColumnsMatched = false;
                                        break;
                                    } else {
                                        allColumnsMatched = true;
                                    }
                                } else{
                                    if (!fieldValue.equalsIgnoreCase(entryData.getValue())) {
                                        allColumnsMatched = false;
                                        break;
                                    } else {
                                        allColumnsMatched = true;
                                    }
                                }
                            }
                            if (allColumnsMatched) {
                                matchMap.put(entityId, true);
                            }
                        }

                        for (Map.Entry<String, Boolean > entry : matchMap.entrySet()) {
                            logger.info("Service data id [" + entry.getKey() + "] update "+ (entry.getValue()?"Successful":"Failed"));
                            customAssert.assertTrue(entry.getValue(), "Service data id " + entry.getKey() + " not matched");
                        }
                    }
                    else {
                        logger.error("Schedule task failed hence skipping");
                        customAssert.assertTrue(false,"Schedule task failed hence skipping");
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Exception in Listing and downloading the excel for bulk upload");
            customAssert.assertTrue(false,"Exception in Listing and downloading the excel for bulk upload");
        }
        customAssert.assertAll();

    }

    private Date getUpdatedDateValue(double dateString) {
        Date date = DateUtil.getJavaDate(dateString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, 1);
        return cal.getTime();
    }
    private String getDateValueInShowFormat(double dateString) {
        Date date = DateUtil.getJavaDate(dateString);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        return simpleDateFormat.format(date);
    }

    private Date getUpdatedDateValue(String dateString) throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MMM-dd").parse(dateString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, 1);
        return cal.getTime();
    }

    public String waitForBulkUpdateTask(List<Integer> oldIds) {
        String result = "pass";
        int updateSchedulerTimeOut = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "timeoutforupdatestatus"));
        int pollingTime = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "intervaltime"));


        logger.info("Waiting for Bulk Update Scheduler.");
        try {
            logger.info("Time Out for Bulk Update Scheduler is {} milliseconds", updateSchedulerTimeOut);
            long timeSpent = 0;
            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Bulk Update Job");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), oldIds);

            if (newTaskId != -1) {
                boolean taskCompleted = false;
                logger.info("Checking if Bulk Update Task has completed or not.");

                while (timeSpent < updateSchedulerTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    fetchObj.hitFetch();
                    logger.info("Getting Status of Bulk Update Task.");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        taskCompleted = true;
                        logger.info("Bulk Update Task Completed. ");
                        logger.info("Checking if Pricing Upload Task failed or not.");
                        if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                            result = "fail";

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Bulk Task is not finished yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= updateSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }
            } else {
                logger.info("Couldn't get Bulk Update Task Job Id. Hence waiting for Task Time Out i.e. {}", updateSchedulerTimeOut);
                Thread.sleep(updateSchedulerTimeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Bulk Update Scheduler to Finish. {}", (Object) e.getStackTrace());
            result = "fail";
        }
        return result;
    }

    public List<Integer> calcExcelRowsToEdit(String query){

        assert query != null:"ExcelRowsToEdit String is null";

        List<Integer> list = new ArrayList<>();

        String[] splits = query.split(",");
        for(String s : splits){
            try{
                if(s.contains("-")){
                    String[] temp = s.split("-");
                    for(int i = Integer.parseInt(temp[0]); i<Integer.parseInt(temp[1]);i++){
                        list.add(i);
                    }
                }
                else
                    list.add(Integer.parseInt(s));
            }
            catch (NullPointerException | IllegalArgumentException e){
                logger.error("Null or Illegal argument exception in [calcExcelRowsToEdit]", (Object) e.getStackTrace());
            }
        }

        return list;
    }
}
