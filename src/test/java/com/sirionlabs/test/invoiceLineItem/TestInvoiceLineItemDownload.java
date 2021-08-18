package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.TabListConfigUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.download.Downloadentitydata;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceLineItemDownload {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceLineItemDownload.class);
    private static String configFilePath;
    private static String configFileName;
    private static String outputFileFormatForDownloadListWithData;
    private static String outputExcelFilePath;
    private static String filterjson;
    private static String adminusername;
    private static String adminpassword;
    private static String invoiceUrlName;
    private static String csrfToken;
    private static String orderByColumnName = "id";
    private static String orderDirection = "desc";
    private static Integer invoicelineitemEntityTypeId;
    private static Integer size = 20;
    private static Integer invoicelineitemlistid;
    private static int offset = 0;
    private static int invoicedetailstabid;
    private static int showPageId;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {

        csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemUploadFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemUploadFileName");
        invoicelineitemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");

        invoiceUrlName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
                ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "invoices", "url_name");
        invoicedetailstabid = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"invoicelineitemupload","invoicedetailstabid"));
        showPageId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"invoicelineitemupload","showpageid"));
        outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");
        outputExcelFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
        filterjson = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"invoicelineitemdownload","filterjson");
        adminusername = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelineitemdownload","adminusername");
        adminpassword = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelineitemdownload","adminpassword");
        invoicelineitemlistid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelineitemdownload","invoicelineitemlstid"));
        resetAdminConfigInvoiceLineItemDownload();
        setAdminConfigInvoiceLineItemDownload();
    }

    @AfterClass
    public void AfterClass(){
        resetAdminConfigInvoiceLineItemDownload();
    }

    @Test
    public void invoiceLineItemDownload(){

        logger.info("Validating download data for invoice line item");
        String excelDownloadedPath = downloadListDataForAllColumns(invoicelineitemEntityTypeId,"invoice line item");
        validateDownloadedInvoiceLineItemExcelData(excelDownloadedPath);
//        validatedownloadedinvoicelineitemexcelfilter(excelDownloadedPath);
    }

    public void validateDownloadedInvoiceLineItemExcelData(String filename){
        CustomAssert csAssert = new CustomAssert();
        try {
            String columnNames = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"invoicelineitemdownload","columnnames");
            String[] columns = columnNames.split(",");
            XLSUtils xlsUtils = new XLSUtils(outputExcelFilePath,filename);

            if(xlsUtils.getCellData("Data",0,0).equals("Invoice Line Item - Details"))
            {
                csAssert.assertTrue(true, "Expected and Actual data [Invoice Line Item - Details] mismatch at row num 0 and column num 0");
                logger.info("Expected and Actual data [Invoice Line Item - Details] match at row num {} and column num {}", 0,0);
            }else{
                csAssert.assertTrue(false, "Expected and Actual data [Invoice Line Item - Details] mismatch at row num 0 and column num 0");
                logger.error("Expected and Actual data [Invoice Line Item - Details] mismatch at row num {} and column num {}", 0,0);
            }

            if(xlsUtils.getCellData("Data",0,1).equals("Date :"))
            {
                logger.info("Expected and Actual data [Date :] match at row num {} and column num {}", 1,0);
                csAssert.assertTrue(true, "Expected and Actual data [Date :] mismatch at row num 1 and column num 0");
            }else{
                logger.error("Expected and Actual data [Date :] mismatch at row num {} and column num {}", 1,0);
                csAssert.assertTrue(false, "Expected and Actual data [Date :]  mismatch at row num 1 and column num 0");
            }

            if(xlsUtils.getCellData("Data",0,2).equals("Generated By :"))
            {
                csAssert.assertTrue(true, "Expected and Actual data [Generated By : ] mismatch at row num 2 and column num 0");
                logger.info("Expected and Actual data [Generated By : ] match at row num {} and column num {}", 2,0);
            }else{
                csAssert.assertTrue(false, "Expected and Actual data [Generated By : ] mismatch at row num 2 and column num 0");
                logger.error("Expected and Actual data [Generated By : ] mismatch at row num {} and column num {}", 2,0);
            }

            if(xlsUtils.getCellData("Data",1,2).equals(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelineitemdownload","downloadedby")))
            {
                csAssert.assertTrue(true, "Expected and Actual data mismatch at row num 2 and column num 1");
                logger.info("Expected and Actual data match at row num {} and column num {}", 2,1);
            }else{
                csAssert.assertTrue(false, "Expected and Actual data mismatch at row num 2 and column num 1");
                logger.error("Expected and Actual data mismatch at row num {} and column num {}", 2,1);
            }

            logger.info("Validating Column names in the downloaded excel");
            List<String> excelData = XLSUtils.getExcelDataOfOneRow(outputExcelFilePath,filename,"Data",4);
            int i =0;
            for(String data  : excelData){
                //if(Arrays.toString(columns).contains(data)){
                if(columns[i].trim().equals(data)){
                    logger.info("Valid Column Name Sequence present in the invoice line item downloaded for column name {}",columns[i]);
                    csAssert.assertTrue(true, "Valid Column Name Sequence present in the invoice line item downloaded for column name " + data);
                }
                else {
                    logger.error("Invalid Column Name Sequence present in the invoice line item downloaded for column name {}",columns[i]);
                    csAssert.assertTrue(false, "Valid Column Name Sequence present in the invoice line item downloaded for column name " + data);
                }
                i++;
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception occurred in testDownloadListWithData. " + e.getMessage());
            logger.error("Exception occurred in validating excel downloaded for invoice line item {}", e.getMessage());
        }

        csAssert.assertAll();
    }

    public void setAdminConfigInvoiceLineItemDownload(){
        Check checkobj = new Check();
        int i =0;
        try {
            checkobj.hitCheck(adminusername,adminpassword,true);

            TabListConfigUpdate config = new TabListConfigUpdate();
            FieldRenaming fieldRenaming = new FieldRenaming();

            config.hitTabListViewConfigure(invoicelineitemlistid,"");
            String configData = config.configureDataJsonStr;

            String updatedconfigdata = configData.replace("\"id\":13809,\"listId\":357,\"name\":\"Line Item Description\",\"defaultName\":\"LINE ITEM DESCRIPTION\",\"order\":2","\"id\": 13809,\"listId\": 357,\"name\": \"Line Item Description\",\"defaultName\": \"LINE ITEM DESCRIPTION\",\"order\": 1");
            updatedconfigdata = updatedconfigdata.replace("\"id\":13815,\"listId\":357,\"name\":\"Status\",\"defaultName\":\"STATUS\",\"order\":1","\"id\":13815,\"listId\":357,\"name\":\"Status\",\"defaultName\":\"STATUS\",\"order\":2");

            updatedconfigdata = updatedconfigdata.replace("\"id\":156,\"name\":\"Invoice Id\",\"defaultName\":\"Invoice Id\",\"order\":1","\"id\":156,\"name\":\"Invoice Id\",\"defaultName\":\"Invoice Id\",\"order\":2");
            updatedconfigdata = updatedconfigdata.replace("\"id\":209,\"name\":\"Line Item Id\",\"defaultName\":\"Line Item Id\",\"order\":2","\"id\":209,\"name\":\"Line Item Id\",\"defaultName\":\"Line Item Id\",\"order\":1");


            config.hitTabListViewConfigureupdate(invoicelineitemlistid,updatedconfigdata);

            int id1 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id1"));
            int id2 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id2"));

            String fieldrenamingresponse = fieldRenaming.hitFieldRenamingUpdate(id1,id2);

            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Sirion Milestone Child ID\"","\"clientFieldName\":\"Sirion Milestone Child ID123\"");
            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Quantity\"","\"clientFieldName\":\"Quantity123\"");

            fieldRenaming.hitFieldUpdate(fieldrenamingresponse);

            checkobj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));

        }catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }

    public void resetAdminConfigInvoiceLineItemDownload(){
        Check checkobj = new Check();
        int i =0;
        try {
            checkobj.hitCheck(adminusername,adminpassword,true);

            TabListConfigUpdate config = new TabListConfigUpdate();
            FieldRenaming fieldRenaming = new FieldRenaming();

            config.hitTabListViewConfigure(invoicelineitemlistid,"");
            String configData = config.configureDataJsonStr;

            String updatedconfigdata = configData.replace("\"id\":13809,\"listId\":357,\"name\":\"Line Item Description\",\"defaultName\":\"LINE ITEM DESCRIPTION\",\"order\":1","\"id\": 13809,\"listId\": 357,\"name\": \"Line Item Description\",\"defaultName\": \"LINE ITEM DESCRIPTION\",\"order\": 2");
            updatedconfigdata = updatedconfigdata.replace("\"id\":13815,\"listId\":357,\"name\":\"Status\",\"defaultName\":\"STATUS\",\"order\":2","\"id\":13815,\"listId\":357,\"name\":\"Status\",\"defaultName\":\"STATUS\",\"order\":1");

            updatedconfigdata = updatedconfigdata.replace("\"id\":156,\"name\":\"Invoice Id\",\"defaultName\":\"Invoice Id\",\"order\":2","\"id\":156,\"name\":\"Invoice Id\",\"defaultName\":\"Invoice Id\",\"order\":1");
            updatedconfigdata = updatedconfigdata.replace("\"id\":209,\"name\":\"Line Item Id\",\"defaultName\":\"Line Item Id\",\"order\":1","\"id\":209,\"name\":\"Line Item Id\",\"defaultName\":\"Line Item Id\",\"order\":2");


            config.hitTabListViewConfigureupdate(invoicelineitemlistid,updatedconfigdata);

            int id1 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id1"));
            int id2 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id2"));

            String fieldrenamingresponse = fieldRenaming.hitFieldRenamingUpdate(id1,id2);

            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Sirion Milestone Child ID123\"","\"clientFieldName\":\"Sirion Milestone Child ID\"");
            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Quantity123\"","\"clientFieldName\":\"Quantity\"");

            fieldRenaming.hitFieldUpdate(fieldrenamingresponse);

            checkobj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));

        }catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }

    public void validatedownloadedinvoicelineitemexcelfilter(String filename){
        CustomAssert csAssert = new CustomAssert();
        try {
            String filtercolumnnames = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"invoicelineitemdownload","filtercolumnnames");
            String[] columns = filtercolumnnames.split(",");
            XLSUtils xlsUtils = new XLSUtils(outputExcelFilePath,filename);

            if(xlsUtils.getCellData("Filter",0,0).equals("Invoice Line Item - Details"))
            {
                logger.info("Expected and Actual data match at row num {} and column num {}"+ 0,0);
            }else{
                logger.error("Expected and Actual data mismatch at row num {} and column num {}"+ 0,0);
                csAssert.assertTrue(false,"Expected and Actual data mismatch at row num 0 and column num 0");
            }

            if(xlsUtils.getCellData("Filter",0,1).equals("Date :"))
            {
                logger.info("Expected and Actual data match at row num {} and column num {} in the filter sheet"+ 1,0);
            }else{
                logger.error("Expected and Actual data mismatch at row num {} and column num {} in the filter sheet"+ 1,0);
            }

            if(xlsUtils.getCellData("Filter",0,2).equals("Generated By : "))
            {
                logger.info("Expected and Actual data match at row num {} and column num {} in the filter sheet"+ 2,0);
            }else{
                logger.error("Expected and Actual data mismatch at row num {} and column num {} in the filter sheet"+ 2,0);
            }

            if(xlsUtils.getCellData("Filter",1,2).equals(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelineitemdownload","downloadedby")))
            {
                logger.info("Expected and Actual data match at row num {} and column num {} in the filter sheet"+ 2,1);
            }else{
                logger.error("Expected and Actual data mismatch at row num {} and column num {} in the filter sheet"+ 2,1);
            }

            logger.info("Validating Column names in the downloaded excel for invoice line item for the filter sheet");
            List<String> excelData = XLSUtils.getExcelDataOfOneRow(outputExcelFilePath,filename,"Filter",4);
            int i =0;
            for(String data  : excelData){
                if(data.trim().equals(columns[i])){
                    logger.info("Valid Column Name Sequence present in the invoice line item downloaded for column name {} in the filter sheet",columns[i]);
                }
                else {
                    logger.error("Valid Column Name Sequence present in the invoice line item downloaded for column name {} in the filter sheet",columns[i]);
                }
                i++;
            }
        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception occurred in testDownloadListWithData. " + e.getMessage());
            logger.error("Exception occurred in validating excel downloaded for invoice line item for filter sheet", e.getMessage());
        }
    }

    private Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap,String filterjson) {

        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;

        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterjson +"}}}";
        } else {
            String selectedColumnArray = "\"selectedColumns\":[";
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
            }
            selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
            selectedColumnArray += "]";

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}," + selectedColumnArray + "}";
        }

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private String downloadListDataForAllColumns(Integer entityTypeId, String entityName) {

        Map<String, String> formParam = getDownloadListWithDataPayload(entityTypeId,null,filterjson);
        Downloadentitydata downloadentitydata = new Downloadentitydata();

        logger.info("formParam is : [{}]", formParam);

        HttpResponse response = downloadentitydata.hitDownloadListWithData(formParam, invoiceUrlName,invoicedetailstabid,showPageId);

        if (response.getStatusLine().toString().contains("200")) {
            /*
             * dumping response into file
             * */
            return dumpDownloadListWithDataResponseIntoFile(response, outputExcelFilePath, "TestDownloadEntityData", entityName, "AllColumn");
        }
        else {
            return null;
        }

    }

    private String dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
        String outputFile = null;
        String outputFileName;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFilePath = outputFilePath + "/" + featureName + "/" + entityName;
            outputExcelFilePath = outputFilePath;
            outputFileName = columnStatus + outputFileFormatForDownloadListWithData;
            outputFile = outputFilePath  + "/" + outputFileName;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status){
                logger.info("DownloadListWithData file generated at {}", outputFile);
                return outputFileName;
                }
            else {
                logger.error("DownloadListWithData file not generated at {}", outputFile);
                return null;
            }
            }
            else {
                return null;
            }
    }
}
