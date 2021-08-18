package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.UpdateConfigFiles;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.test.reportRenderer.FilterData;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.jaxen.util.SingletonList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Listeners(value = MyTestListenerAdapter.class)
public class ServiceDataCreationThroughCDR {
    private static Logger logger = LoggerFactory.getLogger(ServiceDataCreationThroughCDR.class);
    private int cdrEntityTypeId = 160;
    private int serviceDataEntityTypeId = 64;
    private int consumptionEntityTypeId = 176;

    private int cdrListId = 279;
    private int columnIdForIDCDR = 12259, templateId = 1028;

    private String templateDownloadFilePath, templateName;
    private String configFilePath = "src/test/resources/TestConfig/ServiceData";
    private String configFileName = "ServiceDataCreationThroughCDRCreationData.cfg";

    private String contractConfigFilePath = "src/test/resources/Helper/EntityCreation/Contract", contractExtraFieldsConfigFileName = "contractExtraFields.cfg", contractConfigFileName = "contract.cfg";
    private String cdrEntityName = "contract draft request";
    private List<String> cdrPermissions;
    private String failedFileName;

    private String serviceDataEntityName = "service data";
    private String serviceDataIdBillingData;

    int contractId;
    int cdrId;
    String serviceDataId = "";
    String supplierid = null;
    String arcRrcSheetName;
    String pricingSheetName;

    @BeforeClass
    public void setConfigurations() {
        cdrPermissions = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrpermissions").split(","));
        templateName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkcreatetemplatefilename");
        templateDownloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templatefilepath");
        failedFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkcreatetemplatefilenamefailed");

        supplierid = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplierid");
        arcRrcSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "arc rrc sheet name");
        pricingSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricing sheet name");

    }

    @Test(priority = 0, enabled = false)
    public void testPermission() {
        CustomAssert customAssert = new CustomAssert();
        int permissionsColumnId = 23;

        try {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            logger.info("Running query for permission extraction");
            List<List<String>> result = postgreSQLJDBC.doSelect("select * from app_user where login_id='" + Check.lastLoggedInUserName + "' and client_id=" + new AdminHelper().getClientId());

            assert !result.isEmpty() : "Result list of the query is empty";

            String permissionString = null;
            logger.info("Extracting permission column from the returned list");

            try {
                permissionString = result.get(0).get(permissionsColumnId);
            } catch (Exception e) {
                logger.error("Exception caught in extracting permission string out of the result list");
                customAssert.assertTrue(false, "Exception caught in extracting permission string out of the result list");
                customAssert.assertAll();
            }

            assert permissionString != null : "permission String is null";

            String newPermissionString = permissionString.substring(1, permissionString.length() - 1);

            if (!newPermissionString.contains(cdrPermissions.get(0))) {
                newPermissionString = newPermissionString.concat("," + cdrPermissions.get(0));
                logger.info("Permission {} was not found, so inserted.", cdrPermissions.get(0));
            }
            if (!newPermissionString.contains(cdrPermissions.get(1))) {
                newPermissionString = newPermissionString.concat("," + cdrPermissions.get(1));
                logger.info("Permission {} was not found, so inserted.", cdrPermissions.get(1));
            }

            newPermissionString = "{" + newPermissionString + "}";

            String updateQuery = "update app_user set permissions = '" + newPermissionString + "' where login_id='" + Check.lastLoggedInUserName + "' and client_id=" + new AdminHelper().getClientId();

            boolean update = postgreSQLJDBC.updateDBEntry(updateQuery);

            assert update : "Updating the permission failed.";

        } catch (Exception e) {
            logger.error("Error in running select statement for CDR permission");
            customAssert.assertTrue(false, "Error in running select statement for CDR permission");
            customAssert.assertAll();
        }
    }

    @DataProvider
    public Object[][] DataProviderForCreateServiceDataMainFrame() {

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstotest").split(",");
        Object[][] object = new Object[flowsToTest.length][];

        int index = 0;
        for (String s : flowsToTest) {
            object[index] = new String[]{s};
            index++;
        }

        return object;
    }

    @DataProvider
    public Object[][] DataProviderForC89728() {

        String[] flowsToTest = {"case mandatory", "case wrong template"};
        Object[][] object = new Object[flowsToTest.length][];

        int index = 0;
        for (String s : flowsToTest) {
            object[index] = new String[]{s};
            index++;
        }

        return object;
    }

    @Test(dataProvider = "DataProviderForC89728", priority = 3, enabled = false)
    public void C89728(String testCase) {
        CustomAssert customAssert = new CustomAssert();
        String flowsToTest = "failure mandatory";

        try {

            ListRendererListData cdrListRendererListData = new ListRendererListData();
            cdrListRendererListData.hitListRendererListData(cdrEntityTypeId, 10, 10, "id", "desc", cdrListId);
            String cdrListDataResponse = cdrListRendererListData.getListDataJsonStr(); //Calling dateColumnIds API for CDR

            JSONObject cdrListRenderResponseJson = new JSONObject(cdrListDataResponse);

            ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
            try {
                parseJsonResponse.getNodeFromJsonWithValue(cdrListRenderResponseJson, Collections.singletonList("columnIdStatus"), columnIdForIDCDR); //Extracting columnIdStatus for the first CDR in dateColumnIds
            } catch (Exception e) {
                logger.error("Exception in extracting value of the dateColumnIds data API");
                customAssert.assertTrue(false, "Cannot extract CDR from cdr listing, hence marking it failed");
                customAssert.assertAll();
            }

            int cdrId = -1;
            if (parseJsonResponse.getJsonNodeValue() instanceof String)
                cdrId = Integer.parseInt(((String) parseJsonResponse.getJsonNodeValue()).split(":;")[1]); //Extracting id from the column value of the CDR dateColumnIds
            else {
                logger.error("columnIdStatus could not be retrieved correctly");
//                customAssert.assertTrue(false, "columnIdStatus could not be retrieved correctly");
//                customAssert.assertAll();
            }

            cdrId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid") == null ? cdrId : Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid"));

            Download downloadServiceDataBulkCreateTemplate = new Download();
            if (!downloadServiceDataBulkCreateTemplate.hitDownload(templateDownloadFilePath, templateName, templateId, cdrEntityTypeId, cdrId)) {
                logger.error("Template download failed, hence returning failure");
                customAssert.assertTrue(false, "Template download failed, hence returning failure");
                customAssert.assertAll();
            }

            logger.info("**************Excel Downloaded**************");

            List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));

            String[] sheetNames = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sheetnames").split(",");

            Map<String, Object> dataMap;

            if (testCase.equalsIgnoreCase("case mandatory")) {
                for (String sheetName : sheetNames) {
                    dataMap = new HashMap<>();
                    Map<String, String> map = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowsToTest + " " + sheetName);
                    if (map.isEmpty())
                        continue;
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        if (NumberUtils.isParsable(entry.getValue())) {
                            if (dateColumnIds.contains(entry.getKey())) {
                                dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                            } else
                                dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                        } else {
                            dataMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                    XLSUtils.editRowDataUsingColumnId(templateDownloadFilePath, templateName, sheetName, 6, dataMap);
                }

                String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);

                Map<String, String> queryParameters = setPostParams(cdrId, randomKeyForFileUpload);

                if (queryParameters.isEmpty()) {
                    logger.error("cannot make query parameters map");
                    customAssert.assertTrue(false, "cannot make query parameters map");
                    customAssert.assertAll();
                }

                //Getting initial tasks in memory
                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                FileUploadDraft fileUploadDraft = new FileUploadDraft();
                String uploadResponse = fileUploadDraft.hitFileUpload(templateDownloadFilePath, templateName, queryParameters);
                String documentFileId = null, documentSize = null;
                try {
                    if (!new JSONObject(uploadResponse).get("documentFileId").equals(JSONObject.NULL))
                        documentFileId = String.valueOf(new JSONObject(uploadResponse).getInt("documentFileId"));
                    documentSize = String.valueOf(new JSONObject(uploadResponse).getInt("documentSize"));
                } catch (Exception e) {
                    logger.error("cannot extract documents details for draft submit");
                    customAssert.assertTrue(false, "cannot extract documents details for draft submit");
                    customAssert.assertAll();
                }

                // Submit Document Draft
                HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
                JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
                JSONArray contractDocumentChangeStatusJsonArray = new JSONArray("[ { \"templateTypeId\": 900, \"documentFileId\": " + documentFileId + ", \"documentSize\": " + documentSize + ", \"key\": " + randomKeyForFileUpload + ", \"documentStatusId\": 5, \"permissions\": { \"financial\": false, \"legal\": false, \"businessCase\": false }, \"performanceData\": false, \"searchable\": false, \"shareWithSupplierFlag\": false } ]");
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", contractDocumentChangeStatusJsonArray);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);
                String submitDraftPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
                HttpResponse submitDraftResponse = PreSignatureHelper.submitFileDraft(submitDraftPayload);
                assert submitDraftResponse.getStatusLine().getStatusCode() == 200 : "Submit draft API Response is not valid";
                String responseString = EntityUtils.toString(submitDraftResponse.getEntity());
                if (!responseString.contains("success")) {
                    logger.error("Submit data API response is not SUCCESS");
                    customAssert.assertTrue(false, "Submit data API response is not SUCCESS");
                    customAssert.assertAll();
                }

                logger.info("*************************** Excel submitted successfully ******************************");


                logger.info("Checking for validation of the uploaded file");
                int schedulerTimeOut = 600000;
                int pollingTime = 5000;
                String result = "pass";
                logger.info("Time Out for Service data Create Scheduler is {} milliseconds", schedulerTimeOut);
                long timeSpent = 0;
                logger.info("Hitting Fetch API.");
                fetchObj = new Fetch();
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Service data Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
                String newRequestId = null;

                fetchObj = new Fetch();
                fetchObj.hitFetch();
                if (newTaskId != -1) {

                    JSONObject jsonObject = new JSONObject(fetchObj.getFetchJsonStr());
                    JSONArray jsonArray = jsonObject.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
                    for (Object object : jsonArray) {
                        JSONObject jsonObject1 = (JSONObject) object;
                        if (jsonObject1.getInt("id") == newTaskId) {
                            newRequestId = String.valueOf(jsonObject1.getInt("requestId"));
                            break;
                        }
                    }


                    boolean taskCompleted = false;
                    logger.info("Checking if Service data Creation Task has completed or not.");

                    while (timeSpent < schedulerTimeOut) {
                        logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                        Thread.sleep(pollingTime);

                        logger.info("Hitting Fetch API.");
                        fetchObj.hitFetch();
                        logger.info("Getting Status of Service data Create Task.");
                        String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                        if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                            taskCompleted = true;
                            logger.info("Service data Create Task Completed. ");
                            logger.info("Checking if Service data Create Task failed or not.");
                            if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                                result = "fail";

                            break;
                        } else {
                            timeSpent += pollingTime;
                            logger.info("Service data Create Task is not finished yet.");
                        }
                    }
                    if (!taskCompleted && timeSpent >= schedulerTimeOut) {
                        //Task didn't complete within given time.
                        result = "skip";
                    }
                } else {
                    logger.info("Couldn't get Service data Create Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
                    //Thread.sleep(schedulerTimeOut);
                }

                customAssert.assertTrue(result.equalsIgnoreCase("fail"), "Result is not fail");


                logger.info("The Service data create task status is {}", result);

                int checkcount = 5;

                FileUtils.deleteFile(templateDownloadFilePath, failedFileName);

                while (result.equalsIgnoreCase("fail") && --checkcount > 0) {
                    logger.info("Downloading the failed excel");
                    try {

                        Thread.sleep(10000);
                        String environment = "env";//ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"environment");
                        //assert environment!=null:"Environment name is null in config file";

                        String host = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "host");
                        String user = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "user");
                        String key = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "key");
                        String withKey = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "withkey");

                        assert host != null && user != null && key != null && withKey != null : "Environment details are empty";

                        SCPUtils scpUtils;
                        if (withKey.equalsIgnoreCase("yes"))
                            scpUtils = new SCPUtils(host, user, key, 22, withKey.equalsIgnoreCase("yes"));
                        else
                            scpUtils = new SCPUtils(host, user, key, 22);

                        scpUtils.downloadExcelFile(newRequestId, failedFileName, templateDownloadFilePath);

                        logger.info("Downloaded file status : " + FileUtils.fileExists(templateDownloadFilePath, failedFileName));

                        if (FileUtils.fileExists(templateDownloadFilePath, failedFileName))
                            break;

                    } catch (Exception e) {
                        logger.info("Error occurred in extracting file from server");
                        logger.info("Check count remaining : {}", checkcount);
                    }

                }

                customAssert.assertTrue(FileUtils.fileExists(templateDownloadFilePath, failedFileName), "File not found in server after failure result");

            } else {

                Map<Integer, Object> map = new HashMap<>();
                map.put(2, "123");
                XLSUtils.editRowData(templateDownloadFilePath, templateName, "Service Data", 1, map);

                UploadBulkData uploadBulkData = new UploadBulkData();
                uploadBulkData.hitUploadBulkData(64, templateId, templateDownloadFilePath, templateName, new HashMap<>());
                String responseWrongTemplate = uploadBulkData.getUploadBulkDataJsonStr();

                logger.info("Upload response for wrong template is {}", responseWrongTemplate);
                customAssert.assertTrue(responseWrongTemplate.equalsIgnoreCase("500:;basic:;Incorrect headers"), "Expected response is [500:;basic:;Incorrect headers] while we got [" + responseWrongTemplate + "]");

            }


        } catch (Exception e) {

            logger.error("Exception caught in C89728");
            customAssert.assertTrue(false, "Exception caught in C89728");
        }

        customAssert.assertAll();
    }

    @Test(dataProvider = "DataProviderForCreateServiceDataMainFrame", priority = 1, enabled = true)//C89748 covered
    public void createServiceDataMainFrame(String flowToTest) {
        CustomAssert customAssert = new CustomAssert();
        try {

            String flowsToTest = "happy flow";

            ListRendererListData cdrListRendererListData = new ListRendererListData();
            cdrListRendererListData.hitListRendererListData(cdrEntityTypeId, 10, 10, "id", "desc", cdrListId);
            String cdrListDataResponse = cdrListRendererListData.getListDataJsonStr(); //Calling dateColumnIds API for CDR

            JSONObject cdrListRenderResponseJson = new JSONObject(cdrListDataResponse);
            
            ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
            try {
                parseJsonResponse.getNodeFromJsonWithValue(cdrListRenderResponseJson, Collections.singletonList("columnIdStatus"), columnIdForIDCDR); //Extracting columnIdStatus for the first CDR in dateColumnIds
            } catch (Exception e) {
                logger.error("Exception in extracting value of the dateColumnIds data API");
                customAssert.assertTrue(false, "Cannot extract CDR from cdr listing, hence marking it failed");
                customAssert.assertAll();
            }


            if (parseJsonResponse.getJsonNodeValue() instanceof String)
                cdrId = Integer.parseInt(((String) parseJsonResponse.getJsonNodeValue()).split(":;")[1]); //Extracting id from the column value of the CDR dateColumnIds
            else {
                logger.error("columnIdStatus could not be retrieved correctly");
//                customAssert.assertTrue(false, "columnIdStatus could not be retrieved correctly");
//                customAssert.assertAll();
            }

            if(flowToTest.equals("pricing avail no flow no effective date")){
                cdrId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid wth no effective date"));
            }else {
                cdrId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid") == null ? cdrId : Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid"));
            }
            cdrId = EntityOperationsHelper.cloneRecord("contract draft request",cdrId);

            Download downloadServiceDataBulkCreateTemplate = new Download();
            if (!downloadServiceDataBulkCreateTemplate.hitDownload(templateDownloadFilePath, templateName, templateId, cdrEntityTypeId, cdrId)) {
                logger.error("Template download failed, hence returning failure");
                customAssert.assertTrue(false, "Template download failed, hence returning failure");
                customAssert.assertAll();
            }

            logger.info("**************Excel Downloaded**************");

            List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));

            String[] sheetNames = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sheetnames").split(",");

            Map<String, Object> dataMap;

            for (String sheetName : sheetNames) {
                dataMap = new HashMap<>();
                Map<String, String> map = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, flowsToTest + " " + sheetName);
                if (map.isEmpty())
                    continue;

                if(flowToTest.equals("consumption avail no flow")){
                    String consumptionColumnId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"column field id","consumption available");
                    String invoicingTypeColumnId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"column field id","invoicing type");
                    map.put(consumptionColumnId,"No");
                    map.put(invoicingTypeColumnId,"Fixed Fee");

                    if(sheetName.equals(arcRrcSheetName)){
                        map.clear();
                    }

                }else if(flowToTest.equals("pricing avail no flow with effective date")){

//                    C90749
                    String pricingAvailColumnId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"column field id","pricing available");
                    map.put(pricingAvailColumnId,"No");
                }else if(flowToTest.equals("pricing avail no flow no effective date")){

//                    C90749
                    String pricingAvailColumnId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"column field id","pricing available");
                    map.put(pricingAvailColumnId,"No");

                    if(sheetName.equals(pricingSheetName)){
                        map.clear();
                    }
                }



                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (NumberUtils.isParsable(entry.getValue())) {
                        if (dateColumnIds.contains(entry.getKey())) {
                            dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                        } else
                            dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                    } else {
                        dataMap.put(entry.getKey(), entry.getValue());
                    }
                }
                XLSUtils.editRowDataUsingColumnId(templateDownloadFilePath, templateName, sheetName, 6, dataMap);
            }

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);

            Map<String, String> queryParameters = setPostParams(cdrId, randomKeyForFileUpload);

            if (queryParameters.isEmpty()) {
                logger.error("cannot make query parameters map");
                customAssert.assertTrue(false, "cannot make query parameters map");
                customAssert.assertAll();
            }

            //Getting initial tasks in memory
            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            FileUploadDraft fileUploadDraft = new FileUploadDraft();
            String uploadResponse = fileUploadDraft.hitFileUpload(templateDownloadFilePath, templateName, queryParameters);
            String documentFileId = null, documentSize = null;
            try {
                if (!new JSONObject(uploadResponse).get("documentFileId").equals(JSONObject.NULL))
                    documentFileId = String.valueOf(new JSONObject(uploadResponse).getInt("documentFileId"));
                documentSize = String.valueOf(new JSONObject(uploadResponse).getInt("documentSize"));
            } catch (Exception e) {
                logger.error("cannot extract documents details for draft submit");
                customAssert.assertTrue(false, "cannot extract documents details for draft submit");
                customAssert.assertAll();
            }

            // Submit Document Draft
            HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            JSONArray contractDocumentChangeStatusJsonArray = new JSONArray("[ { \"templateTypeId\": 900, \"documentFileId\": " + documentFileId + ", \"documentSize\": " + documentSize + ", \"key\": " + randomKeyForFileUpload + ", \"documentStatusId\": 5, \"permissions\": { \"financial\": false, \"legal\": false, \"businessCase\": false }, \"performanceData\": false, \"searchable\": false, \"shareWithSupplierFlag\": false } ]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", contractDocumentChangeStatusJsonArray);
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);
            String submitDraftPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            HttpResponse submitDraftResponse = PreSignatureHelper.submitFileDraft(submitDraftPayload);
            assert submitDraftResponse.getStatusLine().getStatusCode() == 200 : "Submit draft API Response is not valid";
            String responseString = EntityUtils.toString(submitDraftResponse.getEntity());
            if (!responseString.contains("success")) {
                logger.error("Submit data API response is not SUCCESS");
                customAssert.assertTrue(false, "Submit data API response is not SUCCESS");
                customAssert.assertAll();
            }

            logger.info("*************************** Excel submitted successfully ******************************");

            logger.info("Checking for validation of the uploaded file");
            int schedulerTimeOut = 180000;
            int pollingTime = 5000;
            String result = "pass";
            logger.info("Time Out for Service data Create Scheduler is {} milliseconds", schedulerTimeOut);
            long timeSpent = 0;
            logger.info("Hitting Fetch API.");
            fetchObj = new Fetch();
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Service data Create Job");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
            String newRequestId = null;

            fetchObj = new Fetch();
            fetchObj.hitFetch();
            if (newTaskId != -1) {

                JSONObject jsonObject = new JSONObject(fetchObj.getFetchJsonStr());
                JSONArray jsonArray = jsonObject.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
                for (Object object : jsonArray) {
                    JSONObject jsonObject1 = (JSONObject) object;
                    if (jsonObject1.getInt("id") == newTaskId) {
                        newRequestId = String.valueOf(jsonObject1.getInt("requestId"));
                        break;
                    }
                }


                boolean taskCompleted = false;
                logger.info("Checking if Service data Creation Task has completed or not.");

                while (timeSpent < schedulerTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    fetchObj.hitFetch();
                    logger.info("Getting Status of Service data Create Task.");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        taskCompleted = true;
                        logger.info("Service data Create Task Completed. ");
                        logger.info("Checking if Service data Create Task failed or not.");
                        if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                            result = "fail";

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Service data Create Task is not finished yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= schedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }
            } else {
                logger.info("Couldn't get Service data Create Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
                Thread.sleep(schedulerTimeOut);
            }

            logger.info("The Service data create task status is {}", result);

            int checkcount = 5;

            FileUtils.deleteFile(templateDownloadFilePath, failedFileName);

            while (result.equalsIgnoreCase("fail") && --checkcount > 0) {
                logger.info("Downloading the failed excel");
                try {
                    String environment = "env";//ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"environment");
                    //assert environment!=null:"Environment name is null in config file";

                    String host = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "host");
                    String user = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "user");
                    String key = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "key");
                    String withKey = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "withkey");

                    assert host != null && user != null && key != null && withKey != null : "Environment details are empty";

                    SCPUtils scpUtils;
                    if (withKey.equalsIgnoreCase("yes"))
                        scpUtils = new SCPUtils(host, user, key, 22, withKey.equalsIgnoreCase("yes"));
                    else
                        scpUtils = new SCPUtils(host, user, key, 22);

                    scpUtils.downloadExcelFile(newRequestId, failedFileName, templateDownloadFilePath);

                    logger.info("Downloaded file status : " + FileUtils.fileExists(templateDownloadFilePath, failedFileName));

                    if (FileUtils.fileExists(templateDownloadFilePath, failedFileName))
                        break;


                } catch (Exception e) {
                    logger.info("Error occurred in extracting file from server");
                    logger.info("Check count remaining : {}", checkcount);
                }

            }

            TabListData cdrTabListData = new TabListData();
            String tabListResponse;

            boolean found = false;
            int columnIdStatus = 15939, columnIdName = 14388;
            String validationSuccessId = "6";
            ParseJsonResponse parseJsonResponse1;
            int timeOutForValidation = 240000;
            int interval = 5000, timeElapsed = 0;
            String clusterId = null;

//            documentUFileId=String.valueOf(Integer.parseInt(documentFileId)+1);

            tabListResponse = cdrTabListData.hitTabListData(367, 160, cdrId);

            JSONObject tabListJson = new JSONObject(tabListResponse);
            JSONArray tabListJsonArray = tabListJson.getJSONArray("data");

            for (Object jsonObject : tabListJsonArray) {
                if (jsonObject instanceof JSONObject) {
                    parseJsonResponse1 = new ParseJsonResponse();
                    parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdStatus);
                    Object value = parseJsonResponse1.getJsonNodeValue();
                    if (value instanceof String) {
                        String stringValue = (String) value;
//                        if (stringValue.contains(documentFileId)) {
                            documentFileId = stringValue.split(":;")[1];
                            parseJsonResponse1 = new ParseJsonResponse();
                            parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdName);
                            value = parseJsonResponse1.getJsonNodeValue();

                            if (value instanceof String) {
                                clusterId = ((String) value).split(":;")[0];
                                break;
                            }
//                        }
                    }
                }
            }

            if (clusterId == null) {
                logger.error("Cannot find cluster id");
                customAssert.assertTrue(false, "Cannot find cluster id");
                customAssert.assertAll();return;
            }

            String documentIdNew = documentFileId;

            while (!found && timeElapsed < timeOutForValidation) {
                tabListResponse = cdrTabListData.hitTabListData(367, 160, cdrId);

                tabListJson = new JSONObject(tabListResponse);
                tabListJsonArray = tabListJson.getJSONArray("data");

                for (Object jsonObject : tabListJsonArray) {
                    if (jsonObject instanceof JSONObject) {

                        parseJsonResponse1 = new ParseJsonResponse();
                        parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdName);
                        Object value = parseJsonResponse1.getJsonNodeValue();


                        if (value instanceof String) {
                            String stringValue = (String) value;
                            if (stringValue.contains(clusterId)) {

                                if (Integer.parseInt(((String) value).split(":;")[4]) >= Integer.parseInt(documentIdNew)) {
                                    documentIdNew = ((String) value).split(":;")[4];

                                    parseJsonResponse1 = new ParseJsonResponse();
                                    parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdStatus);
                                    value = parseJsonResponse1.getJsonNodeValue();

                                    if (value instanceof String) {
                                        stringValue = (String) value;
                                        String[] statusAndId = stringValue.split(":;");
                                        if (statusAndId[0].equals(validationSuccessId)) {
                                            found = true;
                                        } else {
                                            logger.error("Successful validation value not found in document {}", stringValue);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Thread.sleep(interval);
                timeElapsed += interval;
            }

            if (!found) {
                customAssert.assertTrue(false, "Successful validation value not found in document id [" + documentIdNew + "]");
                customAssert.assertAll();
                return;

            }

            contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", new JSONArray().put(0, new JSONObject().put("documentFileId", documentIdNew)));
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("editable", true);
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("editableDocumentType", true);
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("shareWithSupplierFlag", false);
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("templateTypeId", 900);
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("documentStatus", new JSONObject().put("id", 2)); //todo
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).getJSONObject("documentStatus").put("name", "Final");

            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values", "Document property updated");

            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("privateCommunication").put("values", false);

            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("shareWithSupplier").put("values", false);

            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("documentEditable").put("values", true);

            //contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);

            String submitFinalPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

            Edit editCdr = new Edit();
            String editResponse = editCdr.hitEdit(cdrEntityName, submitFinalPayload);
            logger.info("Edit response is {}", editResponse);


            JSONObject documentsToBeSubmittedForContractCreationJson = new JSONObject();
            JSONObject documentPayloadForCdr = new JSONObject();
            documentPayloadForCdr.put("auditLogDocFileId", documentIdNew);
            documentsToBeSubmittedForContractCreationJson.put("values", new JSONArray().put(0, documentPayloadForCdr));
            documentsToBeSubmittedForContractCreationJson.put("name", "contractDocuments");
            documentsToBeSubmittedForContractCreationJson.put("multiEntitySupport", false);


            //UpdateFile.addPropertyToConfigFile(contractConfigFilePath, contractExtraFieldsConfigFileName, "fixed fee flow 1", "contractDocuments", documentsToBeSubmittedForContractCreationJson.toString());

            UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractExtraFieldsConfigFileName, "fixed fee flow 1", "contractDocuments", documentsToBeSubmittedForContractCreationJson.toString());
            UpdateFile.addPropertyToConfigFile(contractConfigFilePath, contractExtraFieldsConfigFileName, "fixed fee flow 1", "sourceEntityId", "{\"values\":" + cdrId + "}");
            UpdateFile.addPropertyToConfigFile(contractConfigFilePath, contractExtraFieldsConfigFileName, "fixed fee flow 1", "sourceEntityTypeId", "{\"values\":" + cdrEntityTypeId + "}");

            assert supplierid != null : "Supplier id is found null";

            UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, "fixed fee flow 1", "sourceid", supplierid);

            contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, "fixed fee flow 1");

            logger.info("Contract Id is : {}", contractId);

            ListRendererListData listRendererListData = new ListRendererListData();
            //HttpResponse listDataResponse = listRendererListData.hitListRendererListData(64, 0, 20, "id", "desc nulls last", 352, getFilterJSonForServiceDataListing(contractId, contractName));


            Map<String, String> params = new HashMap<>();
            params.put("contractId", String.valueOf(contractId));
            params.put("relationId", supplierid);
            params.put("vendorId", "");

            String listingServiceData = "";
            JSONObject jsonObjectForServiceDataListing = new JSONObject();

            int haltTime = 5000;
            int checkCount = 20;
            String listPayload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId + "\",\"name\":\"\"}]},\"filterId\":2,\"filterName\":\"contract\"}}},\"selectedColumns\":[{\"columnId\":14219,\"columnQueryName\":\"id\"}]}";
            while (checkCount > 0) {
                logger.info("Checking if service data is created for contract {}", contractId);
                listRendererListData.hitListRendererListDataV2(352, listPayload);
                listingServiceData = listRendererListData.getListDataJsonStr();
                jsonObjectForServiceDataListing = new JSONObject(listingServiceData);
                if (jsonObjectForServiceDataListing.getJSONArray("data").length() > 0)
                    break;
                checkCount--;
                logger.info("Halting 5 seconds to check if service data is created for contract {}, remaining count {}", contractId, checkCount);
                Thread.sleep(haltTime);
            }


            jsonObjectForServiceDataListing = new JSONObject(listingServiceData);
            if (jsonObjectForServiceDataListing.has("data")) {
                if (jsonObjectForServiceDataListing.getJSONArray("data").length() == 0) {

                    if(flowToTest.equals("pricing avail no flow with effective date") ){
                        //C90749
                        logger.info("Service not created when pricing avail no flow with effective date present at CDR");
                        customAssert.assertAll();
                        return;
                    }else {

                        logger.error("No service data found in the listing after filter applied for the created contract");
                        customAssert.assertTrue(false, "No service data found in the listing after filter applied for the created contract");
//                        customAssert.assertAll();
//                        return;
                    }
                }
            }

            String columnIdServiceData = "14219";

            try {
                serviceDataId = jsonObjectForServiceDataListing.getJSONArray("data").getJSONObject(0).getJSONObject(columnIdServiceData).getString("value").split(":;")[1];
                serviceDataIdBillingData = serviceDataId;
            }catch (Exception e){

            }

////            C89752
            if(flowToTest.equals("consumption avail no flow")){

                if(serviceDataId.equals("")){
                    customAssert.assertTrue(false,"Service Data not created for consumption avail no flow");
                }
                Boolean validationStatus = validateValueUpdateTask(Integer.parseInt(serviceDataId),customAssert);

                if(!validationStatus){
                    customAssert.assertTrue(false,"Test Case C89752 validated unsuccessfully");
                }

                customAssert.assertAll();
                return;
            }else if(flowToTest.equals("pricing avail no flow no effective date")){

                if(serviceDataId.equals("")){
                    customAssert.assertTrue(false,"Service Data not created for pricing avail no flow no effective date");
                }
                customAssert.assertAll();
                return;
            }else if(flowToTest.equals("pricing avail no flow with effective date")){

                if(!serviceDataId.equals("")){
                    customAssert.assertTrue(false,"Service Data created for pricing avail no and having effective date");
                }
                customAssert.assertAll();
                return;
            }

            double volume = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowsToTest+" " + pricingSheetName,"8052"));
            double rate = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowsToTest+" " + pricingSheetName,"11348"));
            double lower = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowsToTest+" " + arcRrcSheetName,"8061"));
            double upper = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowsToTest+" " + arcRrcSheetName,"8062"));
            double rate2 = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowsToTest+" " + arcRrcSheetName,"8063"));

            InvoiceHelper invoiceHelper = new InvoiceHelper();

            Show cdrShow = new Show();
            cdrShow.hitShow(160, cdrId);
            String cdrShowResponse = cdrShow.getShowJsonStr();
            String cdrName = new JSONObject(cdrShowResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");


            boolean isDataUnderArcTab =  invoiceHelper.verifyARCRRCCreated(Integer.parseInt(serviceDataId),lower,upper,rate2);
            if(!isDataUnderArcTab){
                customAssert.assertTrue(false,"Arc Tab Data not verified for newly created service data " + serviceDataId);
            }

            boolean isDataUnderChargesTab =  invoiceHelper.verifyChargesCreated(Integer.parseInt(serviceDataId),volume,rate);
            if(!isDataUnderChargesTab){
                customAssert.assertTrue(false,"Charges Tab Data not verified for newly created service data " + serviceDataId);
            }

            List<String> list = invoiceHelper.getPricingVersionARCRRC(serviceDataId);
            assert list.size()==1:"Expected one pricing version in ARCRRC tab but found "+list.size()+" for service data ["+serviceDataId+"]";
            assert list.get(0).contains("Change 0")&&list.get(0).contains(cdrName):"Name of the pricing version "+cdrName+" on ARCRRC tab not matched for service data ["+serviceDataId+"]";

            list = invoiceHelper.getPricingVersionCharges(serviceDataId);
            assert list.size()==1:"Expected one pricing version in Charges tab but found "+list.size()+" for service data ["+serviceDataId+"]";
            assert list.get(0).contains("Change 0")&&list.get(0).contains(cdrName):"Name of the pricing version "+cdrName+" on Charges tab not matched for service data ["+serviceDataId+"]";


            int unitType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"unit type"));
            updateMandatoryFieldsinSD(Integer.parseInt(serviceDataId),unitType,customAssert);

            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
            workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId,Integer.parseInt(serviceDataId),"Publish",customAssert);

            ArrayList<Integer> consumptionIds = new ArrayList<>();

            invoiceHelper.waitForConsumptionToBeCreated(flowToTest,Integer.parseInt(serviceDataId),consumptionIds);

            if(consumptionIds.size() != 0){

                invoiceHelper.updateConsumption(consumptionIds.get(0),20.0,customAssert);

                workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId,consumptionIds.get(0),"Approve",customAssert);



            }else {
                customAssert.assertTrue(false,"Consumptions not created in specified time period");
            }

            validateContractDocumentTabOnContract(cdrId,contractId,templateName,"Service Data Bulk Template",customAssert);


        } catch (Exception e) {
            logger.error("Exception caught in createServiceDataMainFrame()");
            customAssert.assertTrue(false, "Exception caught in createServiceDataMainFrame()");
        }

        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "createServiceDataMainFrame",alwaysRun = true)
    public void TestBillingDataGeneration(){

        CustomAssert customAssert = new CustomAssert();

//       C90750
        validateServiceDataBillingReport(serviceDataIdBillingData,customAssert);

        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "createServiceDataMainFrame",alwaysRun = true)
    public void Test_ServiceDataTracker(){

        CustomAssert customAssert = new CustomAssert();
        try {
            Boolean serviceDataTrackerValidationStatus = validateServiceDataTrackerReport(contractId, serviceDataId, customAssert);

            if (!serviceDataTrackerValidationStatus) {
                customAssert.assertTrue(false, "ServiceData Tracker Report validated unsuccessfully");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception in main test method");
        }
        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "createServiceDataMainFrame",alwaysRun = true)
    public void TestPricingBookScenario(){

        CustomAssert customAssert = new CustomAssert();

        try{
            String flowsToTest = "happy flow";

            double volume = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowsToTest+" " + pricingSheetName,"8052"));
            double rate = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowsToTest+" " + pricingSheetName,"11348"));

            String expectedVolume = String.valueOf(volume * rate);
            String expMonthDate = "01-01-2020";
            String expEndDate = "01-31-2020 23:59:59";
            String expStartDate = "01-01-2020 00:00:00";

            validatePriceBookTab(contractId,supplierid,serviceDataId,cdrId,expectedVolume,expMonthDate,expEndDate,expStartDate,customAssert);

            Map<String,String> mapForPriceBook = createMapForPriceBook(contractId,Integer.parseInt(serviceDataId),Integer.parseInt(supplierid),cdrId);

            Boolean priceBookReportValidationStatus =  validatePriceBookReport(contractId,mapForPriceBook,customAssert);

            if(!priceBookReportValidationStatus){
                customAssert.assertTrue(false,"Price Book Report validated unsuccessfully");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception in main test method");
        }

        customAssert.assertAll();
    }

    @Test(priority = 2,enabled = true)
    public void C89732() {
        CustomAssert customAssert = new CustomAssert();

        int cdrId = -1;
        cdrId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid") == null ? cdrId : Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid"));
        customAssert.assertTrue(cdrId != -1, "Cannot get valid CDR id to test the case");

        String columnIdStatus = "15939", columnIdName = "14388", columnIdVersion = "14387";
        class DocumentData {
            float version;
            int status;
            String name;

            DocumentData(float v, int s,String n) {
                version = v;
                status = s;
                name=n;
            }
        }
        try {

            TabListData cdrTabListData = new TabListData();
            String tabListResponse = cdrTabListData.hitTabListData(367, 160, cdrId);

            JSONObject tabListJson = new JSONObject(tabListResponse);
            JSONArray tabListJsonArray = tabListJson.getJSONArray("data");


            logger.info("Extracting data from tab list api");
            Map<String, DocumentData> map = new HashMap<>();
            int finalStatus = 2;

            for (Object object : tabListJsonArray) {
                JSONObject jsonObject = (JSONObject) object;
                String groupId = jsonObject.getJSONObject(columnIdName).getString("value").split(":;")[0];
                float version = Float.parseFloat(jsonObject.getJSONObject(columnIdVersion).getString("value"));
                int status = Integer.parseInt(jsonObject.getJSONObject(columnIdStatus).getString("value").split(":;")[0]);
                String name = jsonObject.getJSONObject(columnIdName).getString("value").split(":;")[1]+"."+jsonObject.getJSONObject(columnIdName).getString("value").split(":;")[2];
                if (map.containsKey(groupId)) {
                    if (version > map.get(groupId).version && map.get(groupId).status == finalStatus)
                        map.get(groupId).status = status;

                } else if (status == finalStatus)
                    map.put(groupId, new DocumentData(version, status, name));

            }

            //status = 2 is for Final


            cdrTabListData = new TabListData();
            tabListResponse = cdrTabListData.hitTabListData(409, 160, cdrId);

            tabListJson = new JSONObject(tabListResponse);
            tabListJsonArray = tabListJson.getJSONArray("data");
            List<String> names = new ArrayList<>();

            customAssert.assertTrue(tabListJsonArray.length()==map.size(),"Count of final documents to be shown doesn't match for cdr "+cdrId);
            for (Object object : tabListJsonArray) {
                JSONObject jsonObject = (JSONObject) object;
                ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
                parseJsonResponse.getNodeFromJsonWithValue(jsonObject,Collections.singletonList("columnName"),"documentname");
                assert parseJsonResponse.getJsonNodeValue() instanceof String:" Name from tab list data is not string format";

                names.add((String) parseJsonResponse.getJsonNodeValue());
            }

            for(Map.Entry<String,DocumentData> entry : map.entrySet()){
                names.remove(entry.getValue().name);
            }

            assert names.size()==0:"Size of name list is not zero, therefore the count of final document has mis match";

            //List resultName = map.entrySet().stream().filter(key->names.contains(map.get(key).name)).map(key->names.remove(map.get(key).name)).collect(Collectors.toList());




        } catch (Exception e) {
            logger.error("Exception caught in C89732");
            customAssert.assertTrue(false, "Exception caught in C89732");
        }


        customAssert.assertAll();


    }

    private Map<String, String> setPostParams(int entityId, String randomKeyForFileUpload) {

        Map<String, String> map = new HashMap<>();
        if (entityId == -1)
            return map;

        map.put("name", templateName.split("\\.")[0]);
        map.put("extension", "xlsm");
        map.put("entityTypeId", String.valueOf(cdrEntityTypeId));
        map.put("entityId", String.valueOf(entityId));
        map.put("serviceDataRequest", "true");
        map.put("key", randomKeyForFileUpload);

        return map;
    }

    private String getFilterJSonForServiceDataListing(int contractId, String contractName) {
        return "{ \"2\": { \"multiselectValues\": { \"SELECTEDDATA\": [ { \"id\": " + contractId + ", \"name\": " + contractName + " } ] }, \"filterId\": \"2\", \"filterName\": \"contract\", \"entityFieldHtmlType\": null, \"entityFieldId\": null } }";

    }

//    C89746
    private Boolean validatePriceBookTab(int contractId,String supplierId,String serviceDataId,int cdrId,
                                         String expVol,String expMonthDate,String expEndDate,String expStartDate,
                                         CustomAssert customAssert){

        Boolean priceBookValidationStatus = true;

//        "pricingDataViewType Base Charges Summary";
        int pricingDataViewType = 1;
        int serviceDataPricingBookPivot = 1;

        try{
            int priceBookTabId = 338;
            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"filterJson\":{" +
                    "\"211\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"" +
                    "filterId\":211,\"filterName\":\"serviceDataPricingBookPivot\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataPricingBookPivot + "\",\"name\":\"Service Category\"}]}}," +
                    "\"212\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"" +
                    "filterId\":212,\"filterName\":\"pricingDataViewType\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + pricingDataViewType + "\",\"name\":\"\"}]}}," +
                    "\"215\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"" +
                    "filterId\":215,\"filterName\":\"priceVersion\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + cdrId + "\",\"name\":\"\"}]}}}," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"}}";

            TabListData tabListData = new TabListData();
            tabListData.hitTabListDataV2(priceBookTabId,61,contractId,payload);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if(!APIUtils.validJsonResponse(tabListResponse)){
                customAssert.assertTrue(false,"Price book tab list response is not a valid json");
            }else {
                JSONObject tabListResponseJson = new JSONObject(tabListResponse);
                JSONObject priceBookDataJson = tabListResponseJson.getJSONArray("data").getJSONObject(0);

                Iterator<String> keys = priceBookDataJson.keys();
                String columnName;
                String columnValue;


                while (keys.hasNext()){

                    String key = keys.next();

                    columnName = priceBookDataJson.getJSONObject(key).get("columnName").toString();

                    if(columnName.equals("pivotalcolumn")){
                        columnValue = priceBookDataJson.getJSONObject(key).get("value").toString();

                        if(columnValue == null){
                            customAssert.assertTrue(false,"Service Data value is null");
                            priceBookValidationStatus = false;
                        } else if(!columnValue.contains(String.valueOf(serviceDataId))){
                            customAssert.assertTrue(false,"Service Data value is not matched on pricing book tab");
                            priceBookValidationStatus = false;
                        }
                    } else if(columnName.equals("pricingdata")){
                        try {
                            String volume = priceBookDataJson.getJSONObject(key).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("volume").toString();
                            String monthDate = priceBookDataJson.getJSONObject(key).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("monthDate").toString();
                            String endDate = priceBookDataJson.getJSONObject(key).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("endDate").toString();
                            String startDate = priceBookDataJson.getJSONObject(key).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("startDate").toString();

                            if (!volume.equals(expVol)){
                                customAssert.assertTrue(false,"Expected and Actual value of volume from pricing data from price book tab are not equal ");
                                priceBookValidationStatus = false;
                            }

                            if (!monthDate.equals(expMonthDate)){
                                customAssert.assertTrue(false,"Expected and Actual value of monthDate from pricing data from price book tab are not equal ");
                                priceBookValidationStatus = false;
                            }
                            if (!endDate.equals(expEndDate)){
                                customAssert.assertTrue(false,"Expected and Actual value of endDate from pricing data from price book tab are not equal ");
                                priceBookValidationStatus = false;
                            }
                            if (!startDate.equals(expStartDate)){
                                customAssert.assertTrue(false,"Expected and Actual value of endDate from pricing data from price book tab are not equal ");
                                priceBookValidationStatus = false;
                            }
                        }catch (Exception e){
                            customAssert.assertTrue(false,"Exception while getting pricingdata from price book tab");
                            priceBookValidationStatus = false;
                        }
                    }

                }

            }



        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Price Book Tab " + e.getStackTrace());
            priceBookValidationStatus = false;
        }

        return priceBookValidationStatus;
    }

//    C89745
    private Boolean validatePriceBookReport(int contractId,Map<String,String> expectedExcelDataColMap,CustomAssert customAssert){

        Boolean validationStatus = true;

        int reportId = 442;
        DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
        Map<String,String> formParam = new HashMap<>();

        formParam.put("_csrf_token","null");
        formParam.put("jsonData","{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                "\"filterJson\":{\"371\":{\"filterId\":\"371\",\"filterName\":\"contractInfo\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId + "\",\"name\":\"Test Contract Creation Delete\"}]}}}}}");

        try{

            HttpResponse downloadResponse = downloadReportWithData.hitDownloadReportWithData(formParam,reportId);

            if (downloadResponse.getStatusLine().toString().contains("200")) {

//                String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
                String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceData\\PriceBook";
                String outputFileName = "PriceBookReport.xlsx";

                Boolean downLoadStatus = downloadReportWithData.dumpDownloadListWithDataResponseIntoFile(downloadResponse,outputFilePath,outputFileName);

                if(!downLoadStatus){
                    customAssert.assertTrue(false,"Error while downloading Report");
                }else {
                    String excelDataValue;
                    String expectedExcelDataValue;
                    List<String> excelDataColNames = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);
                    List<String> excelData = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 5);

                    for (int i = 0; i < excelDataColNames.size(); i++) {

                        if (!expectedExcelDataColMap.containsKey(excelDataColNames.get(i))) {
                            continue;
                        }
                        expectedExcelDataValue = expectedExcelDataColMap.get(excelDataColNames.get(i));

                        excelDataValue = excelData.get(i);

                        if (excelDataColNames.get(i).equals("EFFECTIVE DATE (PRICING)") || excelDataColNames.get(i).equals("SERVICE START DATE")
                                || excelDataColNames.get(i).equals("SERVICE END DATE")) {

                            continue;

                        } else {

                            if (expectedExcelDataValue == null) {
                                expectedExcelDataValue = "";
                            }
                            if (expectedExcelDataValue == "null") {
                                expectedExcelDataValue = "";
                            }

                            if (!expectedExcelDataValue.toUpperCase().contains(excelDataValue.toUpperCase())) {
                                if (expectedExcelDataValue.equalsIgnoreCase("") && excelDataValue.equalsIgnoreCase("-")) {

                                } else {
                                    validationStatus = false;
                                    customAssert.assertEquals(excelDataValue, expectedExcelDataValue, "In Pricing Report Expected And Actual value not matched for column name " + excelDataColNames.get(i) + " Expected Value : " + expectedExcelDataValue + " Actual Value : " + excelDataValue);
                                }
                            } else {

                                //validationStatus = false;
                                if(!excelDataValue.equalsIgnoreCase(expectedExcelDataValue)) {
                                    customAssert.assertEquals(excelDataValue, expectedExcelDataValue, "In Pricing Report Expected And Actual value not matched for column name " + excelDataColNames.get(i) + " Expected Value : " + expectedExcelDataValue + " Actual Value : " + excelDataValue);
                                }
                            }
                        }
                    }

                }

            } else {
                validationStatus = false;
                logger.error("Error while downloading Report ");
                customAssert.assertTrue(false,"Error while downloading Report ");
            }


        }catch (Exception e){
            validationStatus = false;
            customAssert.assertTrue(false,"Exception while validating price book report " + e.getStackTrace());
        }
        return  validationStatus;
    }


    private Map<String,String> createMapForPriceBook(int contractId,int serviceDataId,int supplierId,int cdrId){

        Map<String,String> expectedExcelDataColMap = new HashMap<>();

        Show show = new Show();
        show.hitShowVersion2(61,contractId);
        String contractShowPageResponse = show.getShowJsonStr();
        show.hitShowVersion2(64,serviceDataId);
        String serviceDataShowPageResponse = show.getShowJsonStr();
        show.hitShowVersion2(1,supplierId);
        String supplierShowPageResponse = show.getShowJsonStr();
        show.hitShowVersion2(160,cdrId);
        String cdrShowPageResponse = show.getShowJsonStr();

        String serviceDataShortCodeId = ShowHelper.getValueOfField("short code id",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("SERVICE DATA ID",serviceDataShortCodeId);

        String serviceDataName = ShowHelper.getValueOfField("name",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("SERVICE DATA NAME",serviceDataName);

        String contractName = ShowHelper.getValueOfField("name",contractShowPageResponse);
        expectedExcelDataColMap.put("CONTRACT",contractName);

        String supplierName = ShowHelper.getValueOfField("name",supplierShowPageResponse);
        expectedExcelDataColMap.put("SUPPLIER",supplierName);

        String entityTypePricing = "Contract Draft Request";
        expectedExcelDataColMap.put("ENTITY TYPE (PRICING)",entityTypePricing);

        String entityIdPricing = ShowHelper.getValueOfField("short code id",cdrShowPageResponse);
        expectedExcelDataColMap.put("ENTITY ID (PRICING)",entityIdPricing);

        String entityNamePricing = ShowHelper.getValueOfField("name",cdrShowPageResponse);
        expectedExcelDataColMap.put("ENTITY NAME (PRICING)",entityNamePricing);

        String effectiveDatePricing = "08-01-2016  05:00:00";
        expectedExcelDataColMap.put("EFFECTIVE DATE (PRICING)",effectiveDatePricing);

        String serviceCategory = ShowHelper.getValueOfField("service category",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("SERVICE CATEGORY",serviceCategory);

        String serviceSubCategory = ShowHelper.getValueOfField("servicesubcategory",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("SERVICE SUB CATEGORY",serviceSubCategory);

        String unitType = ShowHelper.getValueOfField("unit",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("UNIT TYPE",unitType);

        String currency = ShowHelper.getValueOfField("currency",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("CURRENCY",currency);

        String invoicingType = ShowHelper.getValueOfField("invoicingtype",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("INVOICING TYPE",invoicingType);

        String regions = ShowHelper.getAllSelectValuesOfField(64,serviceDataId,"globalregions").toString().replace("[","").replace("]","").toUpperCase();
        expectedExcelDataColMap.put("REGIONS",regions);

        String countries = ShowHelper.getAllSelectValuesOfField(64,serviceDataId,"service data countries").toString().replace("[","").replace("]","").toUpperCase();
        expectedExcelDataColMap.put("COUNTRIES",countries);

        String startDate = "";
        String endDate = "";

        Map<String,String> chargesMap = getDataFromChargesTab(serviceDataId);

        String baseVolume = chargesMap.get("volume");
        expectedExcelDataColMap.put("BASE VOLUME",String.valueOf(Double.parseDouble(baseVolume)));

        String baseRate = chargesMap.get("unitrate");
        expectedExcelDataColMap.put("BASE RATE",String.valueOf(Double.parseDouble(baseRate)));

        StringBuilder baseAmount = new StringBuilder();
        StringBuilder startDateSD = new StringBuilder();
        StringBuilder endDateSD = new StringBuilder();

        getPriceBookTabDetails(contractId,cdrId,baseAmount,startDateSD,endDateSD);
        expectedExcelDataColMap.put("BASE AMOUNT",String.valueOf(Double.parseDouble(baseAmount.toString())));
        expectedExcelDataColMap.put("SERVICE START DATE",startDateSD.toString());
        expectedExcelDataColMap.put("SERVICE END DATE",endDateSD.toString());


        String baseAmtClient = chargesMap.get("clientbaseamount");
        expectedExcelDataColMap.put("BASE AMOUNT (CLIENT)",baseAmtClient);

        String invoicingCurrency = ShowHelper.getValueOfField("invoicingcurrency",serviceDataShowPageResponse);
        expectedExcelDataColMap.put("INVOICING CURRENCY",invoicingCurrency);

        return expectedExcelDataColMap;
    }

    private void getPriceBookTabDetails(int contractId,int cdrId,
                                        StringBuilder baseAmount,StringBuilder startDate,StringBuilder endDate
                                         ){

//        "pricingDataViewType Base Charges Summary";
        int pricingDataViewType = 1;
        int serviceDataPricingBookPivot = 1;

        try {
            int priceBookTabId = 338;
            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"filterJson\":{" +
                    "\"211\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"" +
                    "filterId\":211,\"filterName\":\"serviceDataPricingBookPivot\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataPricingBookPivot + "\",\"name\":\"Service Category\"}]}}," +
                    "\"212\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"" +
                    "filterId\":212,\"filterName\":\"pricingDataViewType\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + pricingDataViewType + "\",\"name\":\"\"}]}}," +
                    "\"215\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"" +
                    "filterId\":215,\"filterName\":\"priceVersion\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + cdrId + "\",\"name\":\"\"}]}}}," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"}}";

            TabListData tabListData = new TabListData();
            tabListData.hitTabListDataV2(priceBookTabId, 61, contractId, payload);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (!APIUtils.validJsonResponse(tabListResponse)) {

            } else {
                JSONObject tabListResponseJson = new JSONObject(tabListResponse);
                JSONObject priceBookDataJson = tabListResponseJson.getJSONArray("data").getJSONObject(0);

                Iterator<String> keys = priceBookDataJson.keys();
                String columnName;

                while (keys.hasNext()) {

                    String key = keys.next();

                    columnName = priceBookDataJson.getJSONObject(key).get("columnName").toString();

                    if (columnName.equals("pricingdata")) {
                        try {
                            baseAmount.append(priceBookDataJson.getJSONObject(key).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("volume").toString().split("\\.")[0]);
                            endDate.append(priceBookDataJson.getJSONObject(key).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("endDate").toString());
                            startDate.append(priceBookDataJson.getJSONObject(key).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("startDate").toString());
                            break;
                        } catch (Exception e) {

                        }
                    }
                }

            }
        }catch (Exception e){
            logger.error("Exception while getting data from price book " + e.getStackTrace());
        }
    }

    private Map<String,String> getDataFromChargesTab(int serviceDataId){

        Map<String,String> chargesTabData = new HashMap<>();

        int chargesTabId = 309;
        String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"filterJson\":{},\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\"}}";

        TabListData tabListData = new TabListData();

        tabListData.hitTabListDataV2(chargesTabId,64,serviceDataId,payload);
        try {

            String tabListResponse = tabListData.getTabListDataResponseStr();

            JSONObject tabListResponseJson = new JSONObject(tabListResponse);

            JSONObject chargesTabDataJson = tabListResponseJson.getJSONArray("data").getJSONObject(0);

            Iterator<String> keys = chargesTabDataJson.keys();
            String columnName;
            String columnValue;


            while (keys.hasNext()) {

                String key = keys.next();

                columnName = chargesTabDataJson.getJSONObject(key).get("columnName").toString();
                columnValue = chargesTabDataJson.getJSONObject(key).get("value").toString();

                if(columnValue == null){
                    columnValue = "";
                }
                if(columnName.equals("volume") || columnName.equals("unitrate") || columnName.equals("baseamount")){

                    if(columnValue == null){
                        columnValue = "";
                    }else if(columnValue == "null"){
                        columnValue = "";
                    }else{
                        columnValue = columnValue.split("\\.")[0];
                    }
                    chargesTabData.put(columnName,columnValue);

                }else {
                    chargesTabData.put(columnName,columnValue);
                }

            }

        }catch (Exception e){
            logger.error("Exception while getting data from charges tab");
        }
        return chargesTabData;
    }

//    C89730
    private boolean validateContractDocumentTabOnContract(int cdrId,int contractId,String expectedDocName,String documentType,CustomAssert customAssert){

        Boolean validationStatus = true;
        int tabId;
        try{
            TabListData tabListData = new TabListData();

            tabId = 366;
            String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            tabListData.hitTabListDataV2(tabId,61,contractId,payload);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            Map<String,String>listMapValues = getListResponseZeroIndex(tabListResponse);

            String documentId = listMapValues.get("id");

            String documentName = listMapValues.get("documentname");
            String fileId = "";
            try {
                fileId = documentName.split(":;")[4];
            }catch (Exception e){

            }

            Download download = new Download();
            String outputFilePath = configFilePath;
            String outputFileName = "ServiceDataUploadFile.xlsm";

            String queryString = "?id=" + documentId + "&entityTypeId=78&entityType.id=61&fileId=" + fileId;

            Boolean downloadStatus = download.downloadCommDocument(outputFilePath,outputFileName,queryString);

            if(!downloadStatus){
                customAssert.assertTrue(false,"Service Data File Download unsuccessful from contract document tab of contracts show page");
                validationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating ContractDocument Tab on contract " + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;
    }

//    C89729
    private Boolean validateServiceDataTrackerReport(int contractId,String serviceData,CustomAssert customAssert){

        Boolean validationStatus = true;

        int reportId = 355;
        try{
            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":" +
                    "\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":" +
                    "[{\"id\":\"" + contractId + "\",\"name\":\"Test Contract Creation Delete\"}]},\"filterId\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[]}";
            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(reportId,payload);
            String reportListDataResponse = reportRendererListData.getListDataJsonStr();

            if(!APIUtils.validJsonResponse(reportListDataResponse)){
                customAssert.assertTrue(false,"Service Data Tracker Report Response is an invalid json");
                validationStatus = false;
            }else {
                Map<String,String> listDataColumnValues= getListResponseZeroIndex(reportListDataResponse);

                String serviceDataIdActual = listDataColumnValues.get("id").split(":;")[1];

                if(!serviceDataIdActual.equals(serviceData)){
                    customAssert.assertTrue(false,"Expected Service Data Id not found in Service Data Tracker Report");
                    validationStatus = false;
                }

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating service data tracker report " + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;
    }


    private Map<String,String> getListResponseZeroIndex(String listResponse){

        Map<String,String> listColumnNameValues = new HashMap<>();
        try{

            JSONObject listResponseJson = new JSONObject(listResponse);
            JSONObject rowAtZeroIndex =  listResponseJson.getJSONArray("data").getJSONObject(0);

            Iterator<String> keys = rowAtZeroIndex.keys();
            String columnName;
            String columnValue;


            while (keys.hasNext()){

                String key = keys.next();

                columnName = rowAtZeroIndex.getJSONObject(key).get("columnName").toString();
                columnValue = rowAtZeroIndex.getJSONObject(key).get("value").toString();

                listColumnNameValues.put(columnName,columnValue);
            }


        }catch (Exception e){
            logger.error("Exception while preparing data from listing Response");
        }

        return listColumnNameValues;
    }

//    private Boolean updateStakeholderAndUnitType(int serviceDataId,int unitType,CustomAssert customAssert){
    private Boolean updateMandatoryFieldsinSD(int serviceDataId,int unitType,CustomAssert customAssert){

        Boolean validationStatus = true;
        try{

            String user_id = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"stakeholder details","user id");
            String name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"stakeholder details","name");
            String type = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"stakeholder details","type");
            String stakeholderId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"stakeholder details","stakeholder id");

            Edit edit = new Edit();

            String editResponse = edit.hitEdit(serviceDataEntityName,serviceDataId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            JSONObject stakeholderObject = new JSONObject();
            stakeholderObject.put("name",name);
            stakeholderObject.put("id",user_id);
            stakeholderObject.put("type",type);

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(stakeholderId).getJSONArray("values").put(stakeholderObject);

            JSONObject unitTypeJson = new JSONObject();
            unitTypeJson.put("id",unitType);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("unitType").put("values",unitTypeJson);
            String uniqueString = DateUtils.getCurrentTimeStamp();
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdSupplier").put("values","newSupplier" + uniqueString);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdClient").put("values","newClient" + uniqueString);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("description").put("values",uniqueString);

            edit.hitEdit(serviceDataEntityName,editResponseJson.toString());
            editResponse = edit.getEditDataJsonStr();

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Error while updating mandatory fields after service data is created from CDR");
                validationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while updating Stakeholder values on service data");
            validationStatus = false;
        }
        return validationStatus;
    }

    private Boolean validateServiceDataBillingReport(String expServiceData,CustomAssert customAssert){

        Boolean validationStatus = true;
        int reportId = 444;
        ReportRendererListData reportRendererListData = new ReportRendererListData();
        long pollingTime = 5000;
        long timeOut = 300000;
        long schedulerTime = 0;

        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"248\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + expServiceData + "\",\"name\":\"\"}]},\"filterId\":248,\"filterName\":\"serviceData\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[]}";

            String reportResponse = "";
            JSONArray dataArray = new JSONArray();

            while (schedulerTime < timeOut){

                reportRendererListData.hitReportRendererListData(reportId,payload);
                reportResponse = reportRendererListData.getListDataJsonStr();

                if(!APIUtils.validJsonResponse(reportResponse)){
                    customAssert.assertTrue(false,"Billing Report Response is an invalid json " );
                    return false;
                }else {

                    JSONObject reportResponseJson = new JSONObject(reportResponse);

                    dataArray = reportResponseJson.getJSONArray("data");
                    if(dataArray.length() > 0){
                        break;
                    }
                }
                Thread.sleep(pollingTime);
                schedulerTime = schedulerTime + pollingTime;
            }

            if(dataArray.length() == 0){

                customAssert.assertTrue(false,"After " + timeOut + " milliseconds billing records not generated");
            }else {

                Map<String,String> columnMap = getListResponseZeroIndex(reportResponse);

                String actualServiceData = columnMap.get("servicedataid").split(":;")[1];

                if(!actualServiceData.equals(expServiceData)){
                    customAssert.assertEquals(false,"Expected and Actual Service Data Mismatch");
                }

            }
        }catch (Exception e){
            validationStatus = false;
            customAssert.assertTrue(false,"Exception while validating Billing Data Report ");
        }
        return  validationStatus;

    }

//    C89752
    private boolean validateValueUpdateTask(int serviceDataId,CustomAssert customAssert){

        Boolean validationStatus = true;

        try{

            Show show = new Show();
            show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
            String showResponse = show.getShowJsonStr();

            String invoicingType = ShowHelper.getValueOfField("invoicingtype",showResponse);

            if(!invoicingType.equals("Fixed Fee")){
                customAssert.assertTrue(false,"Expected Invoicing Type is Fixed Fee Actual Invoicing Type " + invoicingType);
                validationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating value update task for the case C89752" );
            validationStatus = false;
        }

        return validationStatus;
    }

}
