package com.sirionlabs.test.invoiceList;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.QuickLinkViewConfiguration;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;

import org.apache.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;



import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestInvoiceDownload {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceDownload.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static Integer size = 5;
    private static int offset = 0;
    private static String csrfToken;
    private static String orderByColumnName = "id";
    private static String orderDirection = "desc";
    private static String outputFileFormatForDownloadListWithData;
    private static String outputFilePath;
    private static Integer entityTypeId;
    private static Integer entityListId;
    private static String entityName;
    private static String adminusername;
    private static String adminpassword;
    private static String downloadFilePath;
    private static String downloadFileName;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {

        csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceListTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceListTestConfigFileName");
        outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");
        outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
        entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicedownload","entitytypeid"));
        entityName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicedownload","entityname");
        entityListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicedownload","entitylistid"));
        adminusername = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminusername");
        adminpassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminpassword");
        downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoicedownload","downloadfilepath");
        downloadFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoicedownload","downloadfilename");
        setAdminConfigInvoiceDownload();
    }
    @AfterClass
    public void afterClass() {

        resetAdminConfigInvoiceDownload();
    }

    public void resetAdminConfigInvoiceDownload(){
        Check checkobj = new Check();
        int i =0;
        try {

            checkobj.hitCheck(adminusername,adminpassword,true);

            QuickLinkViewConfiguration config = new QuickLinkViewConfiguration();
            FieldRenaming fieldRenaming = new FieldRenaming();

            //Getting Quick Link View Response
            config.hitQuickLinkViewConfigure(entityListId);
            String configData = config.configureDataJsonStr;

            //Updating the quick link response for creating payload for updating
            String updatedconfigdata = configData.replace("\"id\":205,\"listId\":10,\"name\":\"Title\",\"defaultName\":\"TITLE\",\"order\":3","\"id\":205,\"listId\":10,\"name\":\"Title\",\"defaultName\":\"TITLE\",\"order\":2");
            updatedconfigdata = updatedconfigdata.replace("\"id\":204,\"listId\":10,\"name\":\"Invoice Number\",\"defaultName\":\"INVOICE NUMBER\",\"order\":2","\"id\":204,\"listId\":10,\"name\":\"Invoice Number\",\"defaultName\":\"INVOICE NUMBER\",\"order\":3");

            updatedconfigdata = configData.replace("\"id\":2,\"name\":\"Contract\",\"defaultName\":\"CONTRACT\",\"order\":3","\"id\":2,\"name\":\"Contract\",\"defaultName\":\"CONTRACT\",\"order\":2");
            updatedconfigdata = updatedconfigdata.replace("\"id\":59,\"name\":\"PAYMENT DUE DATE\",\"defaultName\":\"PAYMENT DUE DATE\",\"order\":2","\"id\":59,\"name\":\"PAYMENT DUE DATE\",\"defaultName\":\"PAYMENT DUE DATE\",\"order\":3");

            //Updating Quick Link View Configuration
            config.hitlistConfigureUpdate(entityListId,updatedconfigdata);

            int id1 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id1"));
            int id2 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id2"));

            String fieldrenamingresponse = fieldRenaming.hitFieldRenamingUpdate(id1,id2);
            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Test Automation Invoice Period Start\"","\"clientFieldName\":\"Invoice Period Start\"");
            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Test Automation Contracting Entity\"","\"clientFieldName\":\"Contracting Entity\"");

            fieldRenaming.hitFieldUpdate(fieldrenamingresponse);

            checkobj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));

        }catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }
    //Setting the config from admin after login from admin
    public void setAdminConfigInvoiceDownload(){
        Check checkobj = new Check();
        int i =0;
        try {
            checkobj.hitCheck(adminusername,adminpassword,true);

            QuickLinkViewConfiguration config = new QuickLinkViewConfiguration();
            FieldRenaming fieldRenaming = new FieldRenaming();

            config.hitQuickLinkViewConfigure(entityListId);
            String configData = config.configureDataJsonStr;

            String updatedconfigdata = configData.replace("\"id\":205,\"listId\":10,\"name\":\"Title\",\"defaultName\":\"TITLE\",\"order\":2","\"id\":205,\"listId\":10,\"name\":\"Title\",\"defaultName\":\"TITLE\",\"order\":3");
            updatedconfigdata = updatedconfigdata.replace("\"id\":204,\"listId\":10,\"name\":\"Invoice Number\",\"defaultName\":\"INVOICE NUMBER\",\"order\":3","\"id\":204,\"listId\":10,\"name\":\"Invoice Number\",\"defaultName\":\"INVOICE NUMBER\",\"order\":2");

            updatedconfigdata = configData.replace("\"id\":2,\"name\":\"Contract\",\"defaultName\":\"CONTRACT\",\"order\":2","\"id\":2,\"name\":\"Contract\",\"defaultName\":\"CONTRACT\",\"order\":3");
            updatedconfigdata = updatedconfigdata.replace("\"id\":59,\"name\":\"PAYMENT DUE DATE\",\"defaultName\":\"PAYMENT DUE DATE\",\"order\":3","\"id\":59,\"name\":\"PAYMENT DUE DATE\",\"defaultName\":\"PAYMENT DUE DATE\",\"order\":2");

            config.hitlistConfigureUpdate(entityListId,updatedconfigdata);
            int id1 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id1"));
            int id2 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldrename","id2"));

            String fieldrenamingresponse = fieldRenaming.hitFieldRenamingUpdate(id1,id2);

            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Invoice Period Start\"","\"clientFieldName\":\"Test Automation Invoice Period Start\"");
            fieldrenamingresponse = fieldrenamingresponse.replace("\"clientFieldName\":\"Contracting Entity\"","\"clientFieldName\":\"Test Automation Contracting Entity\"");

            fieldRenaming.hitFieldUpdate(fieldrenamingresponse);

            checkobj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));

        }catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }

    @Test()
    public void testInvoiceDownload(){

        logger.info("Validating Invoice List Download File");
        try {
            downloadListDataForAllColumns(entityTypeId, entityName, entityListId);
        }
        catch (Exception e){
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while downloading invoice list . {}", e.getMessage());
        }
    }

    @Test(enabled = true)
    public void testInvoiceDownloadListingPage(){

        logger.info("Validating Invoice List Download File");
        CustomAssert csAssert = new CustomAssert();

        try {

            int filterid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelistdownload","filterid"));
            int id = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelistdownload","contractid"));
            int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelistdownload","listid"));
            String filtername = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelistdownload","filtername");
            String contractname = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicelistdownload","contractname");
            String payload = createpayloadfilterlist(entityTypeId,filterid,filtername,id,contractname);
            Map<String, String> formParam = getDownloadListWithDataPayload(payload);

            logger.info("formParam is : [{}]", formParam);

            formParam = getDownloadListWithDataPayload(payload);
            DownloadListWithData downloadListWithData = new DownloadListWithData();
            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
            HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

            if (response.getStatusLine().toString().contains("200")) {
                /*
                 * dumping response into file
                 * */
                dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "AllColumn");

                XLSUtils xlutil = new XLSUtils(downloadFilePath,downloadFileName);
                int rowcount = xlutil.getRowCount("Data");
                int fieldvalidationspassed = 0;
                List<List<String>> rowdata = XLSUtils.getExcelDataOfMultipleRows(downloadFilePath,downloadFileName,"Data",4,rowcount - 1);String expecteddata[] = (ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicedownload","data")).split(">");

                int totalvalidationstopass = 0;

                for(List<String> rowdata1 : rowdata){

                    Object[] actualda =  rowdata1.toArray();
                    String actualdatainexcel[] = Arrays.asList(actualda).toArray((new String[0]));

                    fieldvalidationspassed = 0;
                    for(int i =0;i<expecteddata.length;i++){
                        String abc = actualdatainexcel[i];
                        String def = expecteddata[i];
                        if(actualdatainexcel[i].equals(expecteddata[i])){
                            fieldvalidationspassed = fieldvalidationspassed + 1;
                        }
                        else {
                            continue;
                        }

                    }
                    if(fieldvalidationspassed == expecteddata.length){
                        logger.info("Data in the downloaded excel on invoice listing page is correct");
                        break;
                    }
                }
                if(fieldvalidationspassed == expecteddata.length){
                    csAssert.assertTrue(true,"Data in the downloaded excel on invoice listing page is correct");
                    logger.info("Data in the downloaded excel on invoice listing page is correct");

                }else {
                    csAssert.assertTrue(false,"Data in the downloaded excel on invoice listing page is incorrect");
                    logger.info("Data in the downloaded excel on invoice listing page is incorrect");
                }
            }
        }
        catch (Exception e){
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while downloading invoice list . {}", e.getMessage());
            csAssert.assertTrue(false, "Exception occurred while downloading invoice list .");
        }
    }

    private void downloadListDataForAllColumns(Integer entityTypeId, String entityName, Integer listId) {

        CustomAssert csAssertion = new CustomAssert();
            Map<String, String> formParam = getDownloadListWithDataPayload(entityTypeId);

            logger.info("formParam is : [{}]", formParam);

            DownloadListWithData downloadListWithData = new DownloadListWithData();
            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
            HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

            if (response.getStatusLine().toString().contains("200")) {
                /*
                 * dumping response into file
                 * */
                dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "AllColumn");
                try {

                    logger.info("Validating Data tab of downloaded invoice list excel");

                    XLSUtils xlutil = new XLSUtils(downloadFilePath,downloadFileName);
                    if(xlutil.getCellData("Data",0,0).equals("INVOICES"))
                    {
                        csAssertion.assertTrue(true, "Valid data not present in the downloaded invoices xlsx for the row {} and column {}\",0,0");
                        logger.info("Valid data present in the downloaded invoices xlsx for the row {} and column {}",0,0);
                    }else{
                        csAssertion.assertTrue(false, "Valid data not present in the downloaded invoices xlsx for the row {} and column {}\",0,0");
                        logger.error("Valid data not present in the downloaded invoices xlsx for the row {} and column {}",0,0);
                    }

                    if(xlutil.getCellData("Data",0,1).equals("Date :"))
                    {
                        csAssertion.assertTrue(true, "Valid data not present in the downloaded invoices xlsx for the row {} and column {}\",1,0");
                        logger.info("Valid data present in the downloaded invoices xlsx for the row {} and column {}",1,0);
                    }else{
                        csAssertion.assertTrue(false, "Valid data not present in the downloaded invoices xlsx for the row {} and column {}\",1,0");
                        logger.error("Valid data not present in the downloaded invoices xlsx for the row {} and column {}",1,0);
                    }

                    if(xlutil.getCellData("Data",0,2).equals("Generated By : "))
                    {
                        csAssertion.assertTrue(true, "Valid data not present in the downloaded invoices xlsx for the row {} and column {}\",2,0");
                        logger.info("Valid data present in the downloaded invoices xlsx for the row {} and column {}",2,0);
                    }else{
                        csAssertion.assertTrue(false, "Valid data not present in the downloaded invoices xlsx for the row {} and column {}\",2,0");
                        logger.error("Valid data not present in the downloaded invoices xlsx for the row {} and column {}",2,0);
                    }

                    if(xlutil.getCellData("Data",1,1).equals(DateUtils.getCurrentDateInAnyFormat("dd-MM-yyyy")))
                    {
                        csAssertion.assertTrue(true, "Valid date present in the downloaded invoices xlsx for the row {} and column {}\",1,1");
                        logger.info("Valid date present in the downloaded invoices xlsx for the row {} and column {}",1,1);
                    }else{
                        csAssertion.assertTrue(false, "Valid date present in the downloaded invoices xlsx for the row {} and column {}\",1,1");
                        logger.error("Valid date not present in the downloaded invoices xlsx for the row {} and column {}",1,1);
                    }
                    String userdownloaded = ConfigureEnvironment.getEnvironmentProperty("j_username");
                    userdownloaded = userdownloaded.replace("_"," ");
                    if(xlutil.getCellData("Data",1,2).equals(userdownloaded))
                    {
                        csAssertion.assertTrue(true, "Valid user downloaded by present in the downloaded invoices xlsx for the row {} and column {}\",2,1");
                        logger.info("Valid user downloaded by present in the downloaded invoices xlsx for the row {} and column {}",2,1);
                    }else{
                        csAssertion.assertTrue(false, "Valid user downloaded by present in the downloaded invoices xlsx for the row {} and column {}\",2,1");
                        logger.error("Valid user downloaded by present in the downloaded invoices xlsx for the row {} and column {}",2,1);
                    }

                    List<String> rowheaderList = XLSUtils.getExcelDataOfOneRow(downloadFilePath,downloadFileName,"Data",4);
                    String expectedRowHeader = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicedownload","dataheader");

                    String[] expectedRowHeaderList = expectedRowHeader.split(",");

                    for(int i = 0; i < rowheaderList.size();i++){

                        if(rowheaderList.get(i).trim().contains(expectedRowHeaderList[i].trim())){
                            csAssertion.assertTrue(true, "Expected header present in the downloaded excel Data Sheet");
                            logger.info("Expected header present in the downloaded excel Data Sheet{}",expectedRowHeaderList[i]);
                        }
                        else{
                            csAssertion.assertTrue(false, "Expected header not present in the downloaded excel Data Sheet");
                            logger.error("Error validating row header of downloaded excel Data Sheet {}",expectedRowHeaderList[i]);
                        }
                    }

                    int rowCount = xlutil.getRowCount("Data");
                    String lastRowData = xlutil.getCellData("Data",0,rowCount-1);
                    if(lastRowData.equals("CONFIDENTIALITY AND DISCLAIMER\n" +
                            "The information in this document is proprietary and confidential and is provided upon the recipient's promise to keep such information confidential. In no event may this information be supplied to third parties without Berkshire Hathway's prior written consent.\n" +
                            "The following notice shall be reproduced on any copies permitted to be made:\n" +
                            "Berkshire Hathway Confidential & Proprietary. All rights reserved.")){

                        csAssertion.assertTrue(true, "Confidentiality and Disclaimer data valid in the downloaded excel");
                        logger.info("Confidentiality and Disclaimer data valid in the downloaded excel");
                    }
                    else{
                        csAssertion.assertTrue(false, "Confidentiality and Disclaimer data valid in the downloaded excel");
                        logger.error("Confidentiality and Disclaimer data invalid in the downloaded excel");
                    }

                    logger.info("Validating Filter tab of downloaded invoice list excel");

                    if(xlutil.getCellData("Filter",0,0).equals("INVOICES"))
                    {
                        csAssertion.assertTrue(true, "Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column 0 ,0");
                        logger.info("Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column {}",0,0);
                    }else{
                        csAssertion.assertTrue(false, "Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column 0 ,0");
                        logger.error("Valid data not present in the downloaded invoices xlsx for the sheet Filter row {} and column {}",0,0);
                    }

                    if(xlutil.getCellData("Filter",0,1).equals("Date :"))
                    {
                        csAssertion.assertTrue(true, "Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column 1 ,0");
                        logger.info("Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column {}",1,0);
                    }else{
                        csAssertion.assertTrue(false, "Valid data not present in the downloaded invoices xlsx for the sheet Filter row {} and column 1 ,0");
                        logger.error("Valid data not present in the downloaded invoices xlsx for the sheet Filter row {} and column {}",1,0);
                    }

                    if(xlutil.getCellData("Filter",0,2).equals("Generated By : "))
                    {
                        csAssertion.assertTrue(true, "Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column 2 ,0");
                        logger.info("Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column {}",2,0);
                    }else{
                        csAssertion.assertTrue(false, "Valid data not present in the downloaded invoices xlsx for the sheet Filter row {} and column 2 ,0");
                        logger.error("Valid data not present in the downloaded invoices xlsx for the sheet Filter row {} and column {}",1,0);
                    }

                    if(xlutil.getCellData("Filter",1,1).equals(DateUtils.getCurrentDateInAnyFormat("dd-MM-yyyy")))
                    {
                        csAssertion.assertTrue(true, "Valid data present in the downloaded invoices xlsx for the sheet Filter row {} and column 1 ,1");
                        logger.info("Valid date present in the downloaded invoices xlsx Filter sheet  for the row {} and column {}",1,1);
                    }else{
                        csAssertion.assertTrue(false, "Valid data not present in the downloaded invoices xlsx for the sheet Filter row {} and column 1 ,1");
                        logger.error("Valid date not present in the downloaded invoices xlsx Filter sheet for the row {} and column {}",1,1);
                    }
                    if(xlutil.getCellData("Filter",1,2).equals(userdownloaded))
                    {
                        csAssertion.assertTrue(true, "Valid user downloaded name present in the downloaded invoices xlsx Filter sheet for the row {} and column {}\",2,1");
                        logger.info("Valid user downloaded name present in the downloaded invoices xlsx Filter sheet for the row {} and column {}",2,1);
                    }else{
                        csAssertion.assertTrue(false, "Valid user downloaded name not present in the downloaded invoices xlsx Filter sheet for the row {} and column {}\",2,1");
                        logger.error("Valid user downloaded name not present in the downloaded invoices xlsx Filter sheet for the row {} and column {}",2,1);
                    }

                    List<String> rowheaderListFilter = XLSUtils.getExcelDataOfOneRow(downloadFilePath,downloadFileName,"Filter",4);
                    String expectedRowHeaderFilter = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicedownload","filterheader");

                    String[] expectedRowHeaderListFilter = expectedRowHeaderFilter.split(",");

                    for(int i = 0; i < rowheaderListFilter.size();i++){

                        if(expectedRowHeaderListFilter[i].trim().contains(rowheaderListFilter.get(i).trim())){
                            csAssertion.assertTrue(true, "Valid header present in the downloaded excel Filter sheet");
                            logger.info("Valid header present in the downloaded excel Filter sheet{}",expectedRowHeaderList[i]);
                        }
                        else{
                            csAssertion.assertTrue(false, "Valid header not present in the downloaded excel Filter sheet");
                            logger.error("Error validating row header of downloaded excel Filter sheet{}",expectedRowHeaderList[i]);
                        }
                    }

                    rowCount = xlutil.getRowCount("Filter");
                    String lastRowDataFilter = xlutil.getCellData("Filter",0,rowCount-1);
                    if(lastRowDataFilter.equals("Berkshire Hathway Confidential & Proprietary. All rights reserved. This report is for Berkshire Hathway internal administrative purpose only.")){

                        csAssertion.assertTrue(true, "Confidentiality and Disclaimer data valid in the downloaded excel for Filter Sheet");
                        logger.info("Confidentiality and Disclaimer data valid in the downloaded excel for Filter Sheet");
                    }
                    else{
                        csAssertion.assertTrue(false, "Confidentiality and Disclaimer data valid in the downloaded excel for Filter Sheet");
                        logger.error("Confidentiality and Disclaimer data invalid in the downloaded excel for Filter Sheet");
                    }

                }catch (Exception e){
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    logger.error("Exception occurred while downloading invoice list . {}", e.getMessage());
                    csAssertion.assertTrue(false, "Exception occurred while downloading invoice list .");
                }

            } else {
                csAssertion.assertTrue(false, "Unable to download invoice list data file");
                logger.error("Unable to download invoice list data file");
            }
        }


    private Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId) {
        return this.getDownloadListWithDataPayload(entityTypeId, null);
    }

    private Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap) {
        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;

        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";
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

    private void dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
        CustomAssert csAssertion = new CustomAssert();
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status) {
                csAssertion.assertTrue(true, "DownloadListWithData file generated at " + outputFile );
                logger.info("DownloadListWithData file generated at {}", outputFile);
            }else{
                csAssertion.assertTrue(false, "Unable to DownloadListWithData file at {}\"" + outputFile);
                logger.error("Unable to DownloadListWithData file at ",outputFile);
            }
        }
    }

    private Map<String, String> getDownloadListWithDataPayload(String jsonData) {

        Map<String, String> formParam = new HashMap<String, String>();

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private String createpayloadfilterlist(int entityTypeId,int filterid,String filtername,int id,String idname){

        String filterinitialString = "{\"filterMap\":{\"entityTypeId\":"+ entityTypeId+ ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":";
        String filterStringpayload =  filterinitialString + "{\"" + filterid + "\":{\"filterId\":\"" + filterid +"\",\"filterName\":\"" + filtername +"\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + id +"\",\"name\":\""+ idname +"\"}]}}";
        filterStringpayload = filterStringpayload + "}}}";
        return filterStringpayload;
    }
}
