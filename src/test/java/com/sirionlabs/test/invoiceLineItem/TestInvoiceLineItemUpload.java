package com.sirionlabs.test.invoiceLineItem;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import javax.swing.plaf.synth.SynthEditorPaneUI;
import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceLineItemUpload {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceLineItemUpload.class);
    private static String configFilePath;
    private static String configFileName;
    private static Integer invoiceLineItemEntityTypeId;
    private static Integer parentEntityTypeId;
    private static Integer parentId;
    private static Integer invoiceDetailsTabId;
    private Integer showPageIdInvoiceLineItem;
    private List<Integer>showPageIdInvoiceLineItemList = new ArrayList<>();
    private List<String> lineitemidList = new ArrayList<>();
    private String lineItemId;
    private static String showPageFieldMappingFilepath;
    private static String showPageFieldMappingFileName;
    private static String columnName;
    TabListData tabListData = new TabListData();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemUploadFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemUploadFileName");
        invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");
        parentEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
        parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "parentid"));
        invoiceDetailsTabId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemupload", "invoicedetailstabid"));
        showPageFieldMappingFilepath = ConfigureConstantFields.getConstantFieldsProperty("ShowFieldHierarchyConfigFilePath");
        showPageFieldMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("ShowFieldHierarchyConfigFileName");

        deleteInvoiceLineItemList();

    }

    public void deleteInvoiceLineItemList(){

        logger.info("Deleting line item with newly created status");

        String tabListResponse = tabListData.hitTabListData(invoiceDetailsTabId,parentEntityTypeId,parentId,invoiceLineItemEntityTypeId);
        JSONObject json = new JSONObject(tabListResponse);
        JSONObject json1;
        JSONObject json2;
        JSONArray jarray2;


        Object value;
        logger.info("Validating uploaded invoice line item on details tab of invoices show page");
        try{

            JSONArray jarray = json.getJSONArray("data");
            String[] invoicelineiemid;
            //loop:
//            while (i < jarray.length()){
            for (int i=0;i < jarray.length();i++){

                json1 = jarray.getJSONObject(i);
                jarray2 = JSONUtility.convertJsonOnjectToJsonArray(json1);

//                while (j < jarray2.length()) {
                for (int j=0;j < jarray2.length();j++) {
                    json2 = jarray2.getJSONObject(j);
                    columnName = json2.getString("columnName");
                    value = json2.get("value").toString();

                    if(columnName.equals("status") && value.equals("Newly Created")){

                        logger.info("Validating json for invoice line item uploaded for invoice");

                        for(int k=0;k <jarray2.length();k++ ){
                            json2 = jarray2.getJSONObject(k);
                            columnName = json2.getString("columnName");
                            value = json2.get("value").toString();

                            switch (columnName)
                            {
                                case "id":{
                                    lineItemId = value.toString();
                                    invoicelineiemid = lineItemId.split(":;");
                                    showPageIdInvoiceLineItem = Integer.parseInt(invoicelineiemid[1]);
                                    showPageIdInvoiceLineItemList.add(showPageIdInvoiceLineItem);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception while deleting invoice line item ids with newly created status");
        }

        for (int showpageidinvoicelineitem : showPageIdInvoiceLineItemList) {
            logger.info("Deleting invoiceline item id " + showpageidinvoicelineitem);
            EntityOperationsHelper.deleteEntityRecord("invoice line item",showpageidinvoicelineitem);
        }
        showPageIdInvoiceLineItemList.clear();
    }

    @Test(priority = 0)//priority 3
    public void testInvoiceLineItemUpload(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();
        String tabListResponse;
        try {
            String bulkTemplateUploadResponse = null;
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemupload", "InvoiceLineItemUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemupload", "InvoiceMultipleLineItemUploadFileName");

            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));
            String fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();
            List<Integer> oldTaskId = UserTasksHelper.getAllTaskIds(fetchJobSchStr);

            BulkTemplate.downloadBulkCreateTemplate(templateFilePath,templateFileName,templateId,parentEntityTypeId,parentId);

            Map<String,String> dataMapForExcelFile = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,"invoicelineitemuploadvaliddata");

            List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));

            Map<String, Object> dataMap= new HashMap<>();;
            for (Map.Entry<String, String> entry : dataMapForExcelFile.entrySet()) {
                if (NumberUtils.isParsable(entry.getValue())) {
                    if (dateColumnIds.contains(entry.getKey())) {
                        dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                    } else
                        dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                } else {
                    dataMap.put(entry.getKey(), entry.getValue());
                }
            }
            boolean editDone= XLSUtils.editRowDataUsingColumnId(templateFilePath,templateFileName,"Invoice Line Item",6,dataMap);

            if(!editDone){
                logger.error("Editing Excel file failed. Need to check");
                csAssert.assertEquals(editDone,"Editing Excel file failed. Need to check");
                csAssert.assertAll();
            }
            bulkTemplateUploadResponse = BulkTemplate.uploadBulkCreateTemplate(templateFilePath,templateFileName,parentEntityTypeId,parentId,invoiceLineItemEntityTypeId,templateId);

            if(bulkTemplateUploadResponse.contains("Template is not correct") ){

                logger.error("Incorrect Template while bulk upload");
                csAssert.assertTrue(false,"Incorrect Template while bulk update");
                csAssert.assertAll();
                return;
            }
            fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();

            int newtaskId =  UserTasksHelper.getNewTaskId(fetchJobSchStr,oldTaskId);

            Map<String,String> statusMap = UserTasksHelper.waitForScheduler(1200000L,20L,newtaskId);
            String jobStatus = statusMap.get("jobPassed");
            if(jobStatus.equals("true")){
                logger.info("Invoice Line Item data uploaded successfully");
            }
            else {
                logger.error("Error while uploading Invoice Line Item data");
                csAssert.assertTrue(false, "Error while uploading Invoice Line Item data");
                csAssert.assertAll();
                return;
            }

            String expectedMessageValidData = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemupload","validuploadmessage");
            if(bulkTemplateUploadResponse.contains(expectedMessageValidData)) {
                logger.info("Valid message while uploading a invoicelineitem file with valid data");
                csAssert.assertTrue(true, "invoicelineitem uploaded successfully");
            }else {
                logger.error("invoicelineitem Data uploaded unsuccessfully");
                csAssert.assertTrue(false, "invoicelineitem Data uploaded unsuccessfully");
            }

            logger.info("Validating the details tab of invoice show page for invoicelineitem created");

            tabListResponse = tabListData.hitTabListData(invoiceDetailsTabId,parentEntityTypeId,parentId,invoiceLineItemEntityTypeId);

            verifyDetailsTabForInvoiceLineItemUploaded(tabListResponse,csAssert);

        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading invoice line item file\n" + errors.toString());
        }
        csAssert.assertAll();
    }

    @Test(priority = 0,enabled = true) //todo done
    public void testInvoiceLineItemUploadInvalidData(){

        CustomAssert csAssert = new CustomAssert();

        try {
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemupload", "InvoiceLineItemUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemupload", "InvoiceInvalidDataFileName");

            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));

            String fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();
            List<Integer> oldTaskId = UserTasksHelper.getAllTaskIds(fetchJobSchStr);

            BulkTemplate.downloadBulkCreateTemplate(templateFilePath,templateFileName,templateId,parentEntityTypeId,parentId);

            Map<String,String> dataMapForExcelFile = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,"invoicelineitemuploadinvaliddata");

            List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));

            Map<String, Object> dataMap= new HashMap<>();;
            for (Map.Entry<String, String> entry : dataMapForExcelFile.entrySet()) {
                if (NumberUtils.isParsable(entry.getValue())) {
                    if (dateColumnIds.contains(entry.getKey())) {
                        dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                    } else
                        dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                } else {
                    dataMap.put(entry.getKey(), entry.getValue());
                }
            }
            boolean editDone= XLSUtils.editRowDataUsingColumnId(templateFilePath,templateFileName,"Invoice Line Item",6,dataMap);

            if(!editDone){
                logger.error("Editing Excel file failed. Need to check");
                csAssert.assertEquals(editDone,"Editing Excel file failed. Need to check");
                csAssert.assertAll();
            }
            BulkTemplate.uploadBulkCreateTemplate(templateFilePath,templateFileName,parentEntityTypeId,parentId,invoiceLineItemEntityTypeId,templateId);

            Thread.sleep(10000);
            fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();

            int newtaskId =  UserTasksHelper.getNewTaskId(fetchJobSchStr,oldTaskId);

            Map<String,String> statusMap = UserTasksHelper.waitForScheduler(1200000L,20L,newtaskId);
            String jobStatus = statusMap.get("jobPassed");
            if(jobStatus.equals("false")){
                logger.info("Upload of invoice line item unsuccessful for invalid data");
                csAssert.assertTrue(true, "Upload of invoice line item unsuccessful for invalid data");
            }
            else {
                logger.error("Upload of invoice line item successful for invalid data");
                csAssert.assertTrue(false, "Upload of invoice line item successful for invalid data");
            }

        }catch (Exception ex){
            logger.error("Exception while uploading invoice line item file {}", (Object) ex.getStackTrace());
            csAssert.assertTrue(false, "Exception while uploading invoice line item file\n" + Arrays.toString(ex.getStackTrace()));
        }
        csAssert.assertAll();
    }

    @Test(dependsOnMethods = "testInvoiceLineItemUpload",enabled = true)
    public void validateInvoiceLineItemCreated(){

        String showpageobject;
        String heirarchy;
        String actualfieldvalonshowpage;
        String expectedvalueonshowpage;
        String showResponse;
        List<String> showpagefieldstovalidate;
        logger.info("Validating show page of invoice line item created");
        CustomAssert csAssert = new CustomAssert();
        try {
            Show showObj = new Show();
            for (int showpageidinvoicelineitem : showPageIdInvoiceLineItemList) {
                showObj.hitShow(invoiceLineItemEntityTypeId, showpageidinvoicelineitem);
                showResponse = showObj.getShowJsonStr();
                showpagefieldstovalidate = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, "showpagevalidationsobjects");

                for (String lineitemfieldinvoice : showpagefieldstovalidate) {
                    showpageobject = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "showpagevalidationsobjects", lineitemfieldinvoice);
                    heirarchy = ParseConfigFile.getValueFromConfigFileCaseSensitive(showPageFieldMappingFilepath, showPageFieldMappingFileName, showpageobject, String.valueOf(invoiceLineItemEntityTypeId));

                    if(heirarchy == null){
                        heirarchy = ParseConfigFile.getValueFromConfigFileCaseSensitive(showPageFieldMappingFilepath, showPageFieldMappingFileName, showpageobject, "0");
                    }

                    actualfieldvalonshowpage = ShowHelper.getActualValue(showResponse, heirarchy);
                    expectedvalueonshowpage = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "invoicelineitemfieldvalidations", lineitemfieldinvoice);
                    if(expectedvalueonshowpage == null || actualfieldvalonshowpage == null){
                        logger.error("expectedvalueonshowpage is null or actualfieldvalonshowpage is null");
                        continue;
                    }
                    if (expectedvalueonshowpage.contains(actualfieldvalonshowpage)) {

                        logger.info("Expected and Actual value are equal for show page object" + lineitemfieldinvoice);
                        csAssert.assertTrue(true, "Expected and Actual value are equal for show page object " + lineitemfieldinvoice);
                    } else {
                        logger.error("Expected and Actual value are equal for show page object" + lineitemfieldinvoice);
                        csAssert.assertTrue(false, "Expected and Actual value are not equal for show page object " + lineitemfieldinvoice);
                    }
                }
            }
        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while verifying line item show page\n" + errors.toString());
        }
        finally {
            for (int showpageidinvoicelineitem : showPageIdInvoiceLineItemList) {
                logger.info("Deleting invoiceline item id " + showpageidinvoicelineitem);
                EntityOperationsHelper.deleteEntityRecord("invoice line item",showpageidinvoicelineitem);
            }
        }
        csAssert.assertAll();
    }

    // need to know the logic, had discussion with the team
    @Test(dependsOnMethods = "testInvoiceLineItemUpload",enabled = false)
    public void testAuditLogInvoiceLineItemUpload(){
        CustomAssert csAssert = new CustomAssert();
        logger.info("Validating the audit logs updated for uploaded consumptions");
        try {

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            tabListData.hitTabListData("61",false,"",String.valueOf(parentEntityTypeId),String.valueOf(parentId),payload);
            String auditlogtablistrespone = tabListData.getTabListDataResponseStr();
            JSONArray dataarray = new JSONObject(auditlogtablistrespone).getJSONArray("data");
            int i = 0;
            int j;
            int k =0;

            String currentDate = DateUtils.getCurrentDateInDDMMYYYY();
            currentDate = currentDate.replace("/","-");
            String prevDate = DateUtils.getPreviousDateInDDMMYYYY(currentDate);
            prevDate = prevDate.substring(3,5) + "-" + prevDate.substring(0,2) + "-" + prevDate.substring(6,10);
            //currentDate = DateUtils.getCurrentDateInDDMMYYYY();
            String nextDate = DateUtils.getNextDateInDDMMYYYY(currentDate);
            nextDate = nextDate.substring(3,5) + "-" + nextDate.substring(0,2) + "-" + nextDate.substring(6,10);
            currentDate = DateUtils.getCurrentDateInMM_DD_YYYY();

            String user = ConfigureEnvironment.getEnvironmentProperty("j_username");
            user = user.replace("_"," ");
            int passtest = 0;
            loop:
            {
                while (i < dataarray.length()) {
                    JSONObject jboj1 = (JSONObject) dataarray.get(i);
                    JSONArray jarray = JSONUtility.convertJsonOnjectToJsonArray(jboj1);
                    j = 0;
                    while (j < jarray.length()) {
                        JSONObject jobj2 = (JSONObject) jarray.get(j);
                        if (jobj2.get("columnName").toString().equals("audit_log_date_created")) {

                            String datefromResponse = jobj2.get("value").toString().substring(0, 10);
                            if (datefromResponse.equals(currentDate)) {
                                k = 0;
                                logger.info("Validating the audit log response for current invoice line item bulk upload");
                                while (k < jarray.length()) {
                                    jobj2 = (JSONObject) jarray.get(k);

                                    String col = jobj2.get("columnName").toString();
                                    if (col.equals("action_name")) {
                                        if (jobj2.get("value").toString().equals("Line Items Uploaded")) {

                                            csAssert.assertTrue(true, "Valid value for action_name after invoice line item data upload");
                                            passtest = passtest + 1;
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for action_name after invoice line item data upload");
                                        }
                                    }
                                    if (col.equals("requested_by")) {
                                        if (jobj2.get("value").toString().equalsIgnoreCase(user)) {
                                            csAssert.assertTrue(true, "Valid value for user after invoice line item data upload");
                                            passtest = passtest + 1;
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for user after invoice line item data upload");
                                        }
                                    }
                                    if (col.equals("completed_by")) {
                                        if (jobj2.get("value").toString().equalsIgnoreCase(user)) {
                                            passtest = passtest + 1;
                                            csAssert.assertTrue(true, "Valid value for completed by after invoice line item data upload");
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for completed by after invoice line item data upload");
                                        }
                                    }
                                    if (col.equals("audit_log_user_date")) {
                                        if (jobj2.get("value").toString().substring(0, 10).equals(prevDate) || jobj2.get("value").toString().substring(0, 10).equals(currentDate) || jobj2.get("value").toString().substring(0, 10).equals(nextDate)) {
                                            passtest = passtest + 1;
                                            csAssert.assertTrue(true, "Valid value for audit_log_user_date after invoice line item data upload");
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for audit_log_user_date after invoice line item data upload");
                                        }
                                    }
                                    if (col.equals("comment")) {
                                        if (jobj2.get("value").toString().equals("No")) {
                                            passtest = passtest + 1;
                                            csAssert.assertTrue(true, "Valid value for comment after invoice line item data upload");
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for comment after invoice line item data upload");
                                        }

                                    }
                                    if (col.equals("document")) {
                                        if (jobj2.get("value").toString().equals("No")) {
                                            csAssert.assertTrue(true, "Valid value for document after invoice line item data upload");
                                            passtest = passtest + 1;
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for document after invoice line item data upload");
                                        }
                                    }
                                    k++;
                                }
                                if(k > 0 && passtest != 6){
                                    logger.error("Error validating audit logs for invoice line item");
                                    csAssert.assertTrue(false,"Error validating audit logs for invoice line item");
                                    break loop;
                                }
                                else{
                                    logger.info("Audit logs validated for invoice line item successfully");
                                    csAssert.assertTrue(true,"Audit logs validated for invoice line item successfully");
                                    break loop;
                                }
                            }
                        }
                        j++;
                    }
                    i++;
                }
            }

        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while validating audit logs for invoice line item\n" + errors.toString());
        }
        csAssert.assertAll();

    }

    public void verifyDetailsTabForInvoiceLineItemUploaded(String responseJson,CustomAssert csAssert){

        JSONObject json = new JSONObject(responseJson);
        JSONObject json1;
        JSONObject json2;
        JSONArray jarray2;
        int i = 0;
        int j = 0;
        int k = 0;

        Object value;
        String expectedValue;
        logger.info("Validating uploaded invoice line item on details tab of invoices show page");
        try{
            int invoice_id = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"parentid"));

            logger.info("Getting show page response for invoice id " + invoice_id);

            Show show = new Show();
            show.hitShow(parentEntityTypeId,invoice_id);
            String showResponse = show.getShowJsonStr();

            if(!(APIUtils.validJsonResponse(showResponse))){
                logger.error("Invalid Json Response for showResponse " + showResponse);
                csAssert.assertTrue(false,"Invalid Json Response for showResponse ");
            }
            JSONArray jarray = json.getJSONArray("data");
            String[] invoicelineiemid;
            int testperformedcount = 0;
            //loop:
            while (i < jarray.length()){

                json1 = jarray.getJSONObject(i);
                jarray2 = JSONUtility.convertJsonOnjectToJsonArray(json1);
                j = 0;

                while (j < jarray2.length()) {
                    json2 = jarray2.getJSONObject(j);
                    columnName = json2.getString("columnName");
                    value = json2.get("value");
                    value = value.toString();
                    if(columnName.equals("status") && value.equals("Newly Created")){

                        logger.info("Validating json for invoice line item uploaded for invoice");
                        k = 0;
                        while(k <jarray2.length() ){
                            json2 = jarray2.getJSONObject(k);
                            columnName = json2.getString("columnName");
                            value = json2.get("value");
                            value = value.toString();
                            switch (columnName)
                            {
                                case "id":{
                                    lineItemId = value.toString();
                                    invoicelineiemid = lineItemId.split(":;");
                                    lineItemId = invoicelineiemid[0];
                                    lineItemId = "LI0" + lineItemId ;
                                    showPageIdInvoiceLineItem = Integer.parseInt(invoicelineiemid[1]);
                                    showPageIdInvoiceLineItemList.add(showPageIdInvoiceLineItem);
                                    lineitemidList.add(lineItemId);
                                    break;
                                }
                                case "sirionLineItemDescription":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","sirionLineItemDescription");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for sirionLineItemDescription");
                                    }else {
                                        logger.error("Expected and actual value didn't match for sirionLineItemDescription");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for sirionLineItemDescription");
                                    }
                                    break;
                                }

                                case "lineItemType":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","lineItemType");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for lineItemType");
                                    }else {
                                        logger.error("Expected and actual value didn't match for lineItemType");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for lineItemType");
                                    }
                                    break;
                                }

                                case "contract":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","contract");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for contract");
                                    }else {
                                        logger.error("Expected and actual value didn't match for contract");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for contract");
                                    }
                                    break;
                                }

                                case "supplier":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","supplier");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for supplier");
                                    }else {
                                        logger.error("Expected and actual value didn't match for supplier");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for supplier");
                                    }
                                    break;
                                }
                                //Changes done  as timezone is carried from invoice
                                case "timeZone":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","timeZone");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for timeZone");
                                    }else {
                                        logger.error("Expected and actual value didn't match for timeZone");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for timeZone");
                                    }
                                    break;
                                }

                                case "serviceData":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceData");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceData");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceData");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceData");
                                    }
                                    break;
                                }

                                case "lineItemNumber":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","lineItemNumber");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for lineItemNumber");
                                    }else {
                                        logger.error("Expected and actual value didn't match for lineItemNumber");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for lineItemNumber");
                                    }
                                    break;
                                }

                                case "serviceDataServiceCategory":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceDataServiceCategory");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceDataServiceCategory");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceDataServiceCategory");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceDataServiceCategory");
                                    }
                                    break;
                                }
                                case "serviceSubCategory":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceSubCategory");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceSubCategory");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceSubCategory");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceSubCategory");
                                    }
                                    break;
                                }

                                case "deliveryRegion":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","deliveryRegion");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for deliveryRegion");
                                    }else {
                                        logger.error("Expected and actual value didn't match for deliveryRegion");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for deliveryRegion");
                                    }
                                    break;
                                }

                                case "deliveryCountry":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","deliveryCountry");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for deliveryCountry");
                                    }else {
                                        logger.error("Expected and actual value didn't match for deliveryCountry");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for deliveryCountry");
                                    }
                                    break;
                                }
                                case "billingRegion":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","billingRegion");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for billingRegion");
                                    }else {
                                        logger.error("Expected and actual value didn't match for billingRegion");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for billingRegion");
                                    }
                                    break;
                                }

                                case "billingCountry":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","billingCountry");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for billingCountry");
                                    }else {
                                        logger.error("Expected and actual value didn't match for billingCountry");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for billingCountry");
                                    }
                                    break;
                                }

                                case "serviceIdClient":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceIdClient");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceIdClient");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceIdClient");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceIdClient");
                                    }
                                    break;
                                }

                                case "serviceIdSupplier":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceIdSupplier");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceIdSupplier");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceIdSupplier");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceIdSupplier");
                                    }
                                    break;
                                }

                                case "serviceStartDate":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceStartDate");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceStartDate");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceStartDate");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceStartDate");
                                    }
                                    break;
                                }

                                case "serviceEndDate":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceEndDate");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceEndDate");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceEndDate");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceEndDate");
                                    }
                                    break;
                                }

                                case "function":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","function");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for function");
                                    }else {
                                        logger.error("Expected and actual value didn't match for function");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for function");
                                    }
                                    break;
                                }

                                case "milestoneDescription":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","milestoneDescription");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for milestoneDescription");
                                    }else {
                                        logger.error("Expected and actual value didn't match for milestoneDescription");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for milestoneDescription");
                                    }
                                    break;
                                }
                                case "adjustmentType":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","adjustmentType");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for adjustmentType");
                                    }else {
                                        logger.error("Expected and actual value didn't match for adjustmentType");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for adjustmentType");
                                    }
                                    break;
                                }
                                case "service":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","service");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for service");
                                    }else {
                                        logger.error("Expected and actual value didn't match for service");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for service");
                                    }
                                    break;
                                }
                                case "invoiceDate":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","invoiceDate");
                                    try {
                                        JSONObject showObj = new JSONObject(showResponse);
                                        expectedValue = showObj.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceDate").get("displayValues").toString();
                                    }
                                    catch (Exception e){
                                        logger.error("Exception while parsing show page response   ");
                                        csAssert.assertTrue(false,"Exception while parsing show page response");
                                    }
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for invoiceDate");
                                    }else {
                                        logger.error("Expected and actual value didn't match for invoiceDate");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for invoiceDate");
                                    }
                                    break;
                                }
                                case "dueDate":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","dueDate");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for dueDate");
                                    }else {
                                        logger.error("Expected and actual value didn't match for dueDate");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for dueDate");
                                    }
                                    break;
                                }
                                case "discrepancyReason":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","discrepancyReason");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for discrepancyReason");
                                    }else {
                                        logger.error("Expected and actual value didn't match for discrepancyReason");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for discrepancyReason");
                                    }
                                    break;
                                }

                                case "serviceDataCurrency":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","serviceDataCurrency");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for serviceDataCurrency");
                                    }else {
                                        logger.error("Expected and actual value didn't match for serviceDataCurrency");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for serviceDataCurrency");
                                    }
                                    break;
                                }

                                case "currency":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","currency");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for currency");
                                    }else {
                                        logger.error("Expected and actual value didn't match for currency");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for currency");
                                    }
                                    break;
                                }

                                case "quantity":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","quantity");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for quantity");
                                    }else {
                                        logger.error("Expected and actual value didn't match for quantity");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for quantity");
                                    }
                                    break;
                                }

                                case "amount":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","amount");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for amount");
                                    }else {
                                        logger.error("Expected and actual value didn't match for amount");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for amount");
                                    }
                                    break;
                                }

                                case "parentId":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","parentId");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for parentId");
                                    }else {
                                        logger.error("Expected and actual value didn't match for parentId");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for parentId");
                                    }
                                    break;
                                }

                                case "unit":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","unit");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for unit");
                                    }else {
                                        logger.error("Expected and actual value didn't match for unit");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for unit");
                                    }
                                    break;
                                }

                                case "variance":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","variance");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for variance");
                                    }else {
                                        logger.error("Expected and actual value didn't match for variance");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for variance");
                                    }
                                    break;
                                }

                                case "currentPricingVersionId":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","currentPricingVersionId");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for currentPricingVersionId");
                                    }else {
                                        logger.error("Expected and actual value didn't match for currentPricingVersionId");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for currentPricingVersionId");
                                    }
                                    break;
                                }

                                case "invoice_number":{
                                    testperformedcount +=1;
                                    expectedValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"invoicelineitemfieldvalidations","invoice_number");
                                    if(value.equals(expectedValue)){
                                        logger.info("Valid value for invoice_number");
                                    }else {
                                        logger.error("Expected and actual value didn't match for invoice_number");
                                        csAssert.assertEquals(value,expectedValue,"Expected and actual value didn't match for invoice_number");
                                    }
                                    break;
                                }
                                default:{
                                    logger.info("No handling for the column " + columnName);
                                }
                            }
                            k++;
                        }

                    }
                    j++;
                }
                i++;
            }
        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while verifying line item uploading invoice line item file\n" + errors.toString());
        }

    }

}
