package com.sirionlabs.test.serviceData;

import com.google.inject.internal.cglib.core.$Customizer;
import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.velocity.runtime.directive.Parse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestTabListingServiceData {

    private final static Logger logger = LoggerFactory.getLogger(TestTabListingServiceData.class);

    private String configFilePath;
    private String configFileName;

    private int serviceDataEntityTypeId = 64;
    private int consumptionsEntityTypeId = 176;

    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;

    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;

    private String invoiceFlowsConfigFilePath;
    private String invoiceFlowsConfigFileName;

    private String pricingTemplateFilePath;

    private String serviceDataEntity = "service data";

    private int contractId;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestServiceDataTabListingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestServiceDataTabListingFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        invoiceFlowsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFlowsConfigFilePath");
        invoiceFlowsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFlowsConfigFileName");
        pricingTemplateFilePath = ConfigureConstantFields.getConstantFieldsProperty("PricingTemplateFilePath");
        contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contractid"));

        //Invoice Config files
        invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
        invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");

    }

    @DataProvider
    public Object[][] flowToTestConsumptionsTab() {

        List<Object[]> allTestData = new ArrayList<>();

        String flowsToTest[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows to test consumptions tab").split(Pattern.quote(","));


        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForSubServiceDataTabValidation() {

        String[] flowsToTestForSubServiceDataTabValidation;
        List<Object[]> allTestData = new ArrayList<>();
        try {
            flowsToTestForSubServiceDataTabValidation = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsforsubservicedatatabvalidation").split(",");
            for (String flowToTest : flowsToTestForSubServiceDataTabValidation) {
                allTestData.add(new Object[]{flowToTest});
            }

        } catch (Exception e) {
            logger.error("Exception while preparing data provider for SubServiceDataTabValidation " + e.getStackTrace());
        }
        return allTestData.toArray(new Object[0][]);
    }

//    C141187
    @Test(dataProvider = "flowToTestConsumptionsTab",enabled = true)
    public void TestConsumptionTabListing(String flowToTest){

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();
        int serviceDataId;
        int tabId = 376;
        try{
            String payload = "{\"filterMap\":{\"entityTypeId\":176,\"offset\":0,\"size\":20,\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}";

            serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"service data id"));

            String tabListResponse = getTabListResponse(tabId,serviceDataId,payload);

            if(!JSONUtility.validjson(tabListResponse)){
                customAssert.assertTrue(false,"Consumption tab list response is not a valid json for Service Data " + serviceDataId);
            }else {

                Map<String,String> columnNameValueMap = createColumnNameValueMap(tabListResponse,customAssert);

                int consumptionId = Integer.parseInt(columnNameValueMap.get("id"));
                show.hitShowVersion2(consumptionsEntityTypeId,consumptionId);
                String consumptionShowPageResponse = show.getShowJsonStr();
                String expectedValueOnConsumptionShowPage;
                String actualValueOnConsumptionShowPage;
                String columnNameTabListResponse;
                String showPageColumnName;

                for(Map.Entry<String, String> entry : columnNameValueMap.entrySet()){
                    columnNameTabListResponse = entry.getKey();
                    showPageColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"tab column show page column mapping",columnNameTabListResponse);

                    if(columnNameTabListResponse.equals("serviceclient")){

                        showPageColumnName = "serviceidclientc";
                    }else if(columnNameTabListResponse.equals("servicesupplier")){
                        showPageColumnName = "serviceidsupplierc";
                    }
                    expectedValueOnConsumptionShowPage = entry.getValue();

                    if(showPageColumnName == null){
                        logger.info("Show page column hierarchy not get for column name " + columnNameTabListResponse);
                        logger.info("Skipping the column ");
                        continue;
                    }

                    actualValueOnConsumptionShowPage = ShowHelper.getValueOfField(showPageColumnName,consumptionShowPageResponse);

                    if(actualValueOnConsumptionShowPage == null){
                        List<String> actualValueOnConsumptionShowPageList = ShowHelper.getAllSelectValuesOfField(consumptionsEntityTypeId,consumptionId,showPageColumnName);

                        actualValueOnConsumptionShowPage = "";

                        if(actualValueOnConsumptionShowPageList != null) {
                            actualValueOnConsumptionShowPageList = actualValueOnConsumptionShowPageList.stream().sorted().collect(Collectors.toList());
                            for (String actualValue : actualValueOnConsumptionShowPageList) {
                                actualValueOnConsumptionShowPage += actualValue + ", ";
                            }
                        }
                        if(actualValueOnConsumptionShowPage.length() != 0 ){
                            actualValueOnConsumptionShowPage = actualValueOnConsumptionShowPage.substring(0, actualValueOnConsumptionShowPage.length() - 2);
                        }else {
                            actualValueOnConsumptionShowPage = null;
                        }
                    }
                    if(columnNameTabListResponse.equalsIgnoreCase("consumptionrole_group")){

                        List<String> actualValueOnConsumptionShowPageList  = ShowHelper.getAllSelectedStakeholdersFromShowResponse(consumptionShowPageResponse);
                        actualValueOnConsumptionShowPage = "";

                        if(actualValueOnConsumptionShowPageList != null) {
                            for (String actualValue : actualValueOnConsumptionShowPageList) {
                                actualValueOnConsumptionShowPage += actualValue + ", ";
                            }
                        }
                        if(actualValueOnConsumptionShowPage.length() != 0 ){
                            actualValueOnConsumptionShowPage = actualValueOnConsumptionShowPage.substring(0, actualValueOnConsumptionShowPage.length() - 2);
                        }else {
                            actualValueOnConsumptionShowPage = null;
                        }

                    }

                    if(actualValueOnConsumptionShowPage == null){
                        actualValueOnConsumptionShowPage = "null";
                    }
                    if(columnNameTabListResponse.equalsIgnoreCase("final_consumption")) {
                        if (NumberUtils.isDigits(actualValueOnConsumptionShowPage)) {
                            actualValueOnConsumptionShowPage = String.valueOf(Double.parseDouble(actualValueOnConsumptionShowPage));
                        }
                    }
                    if(!expectedValueOnConsumptionShowPage.equalsIgnoreCase(actualValueOnConsumptionShowPage)){

                        logger.error("Expected and Actual value didn't match for column " + columnNameTabListResponse);
                        customAssert.assertTrue(false,"Expected and Actual value didn't match for column " + columnNameTabListResponse);
                    }

                }

            }


        }catch (Exception e){
            logger.error("Exception in test Method");
            customAssert.assertTrue(false,"Exception in test Method");
        }

        customAssert.assertAll();
    }

//    C129174
    @Test(enabled = true)
    public void TestBillingRatioTab(){

        CustomAssert customAssert = new CustomAssert();
        int tabId = 376;
        try{
            String flowToTest = "billing ratio tab";
            int serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"service data id"));

            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            String tabListResponse = getTabListResponse(tabId,serviceDataId,payload);

            String attribute_type = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"billing ratio tab","attribute_type");
            String[] attribute_value =  ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"billing ratio tab","attribute_value").split(",");
            String ratio_type = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"billing ratio tab","ratio_type");
            String[] split_data  = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"billing ratio tab","split_data").split(",");

            if(!JSONUtility.validjson(tabListResponse)){
                customAssert.assertTrue(false,"Billing Ratio tab list response is not a valid json for Service Data " + serviceDataId);
            }else {
                JSONObject tabListJsonResponse = new JSONObject(tabListResponse);

                JSONArray dataArray = tabListJsonResponse.getJSONArray("data");
                JSONObject indvRow = new JSONObject();
                JSONArray indvRowArray = new JSONArray();

                String columnName;
                String columnValue;
                for(int i =0;i<dataArray.length();i++){

                    indvRow = dataArray.getJSONObject(i);
                    indvRowArray = JSONUtility.convertJsonOnjectToJsonArray(indvRow);

                    for(int j=0;j<indvRowArray.length();j++){
                        columnName = indvRowArray.getJSONObject(j).get("columnName").toString();
                        if(columnName.equalsIgnoreCase("split_data")){

                            columnValue = indvRowArray.getJSONObject(j).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("volume").toString();

                            if(!columnValue.equalsIgnoreCase(split_data[i])){
                                customAssert.assertTrue(false,"Expected and Actual Split data Value didn't match for row number " + i);
                            }
                        }else if(columnName.equalsIgnoreCase("attribute_type")) {
                            columnValue = indvRowArray.getJSONObject(j).get("value").toString();
                            if(!columnValue.equalsIgnoreCase(attribute_type)){
                                customAssert.assertTrue(false,"Expected and Actual Attribute Type didn't match for row number " + i);
                            }

                        }else if(columnName.equalsIgnoreCase("attribute_value")) {
                            columnValue = indvRowArray.getJSONObject(j).get("value").toString();

                            if(!columnValue.equalsIgnoreCase(attribute_value[i])){
                                customAssert.assertTrue(false,"Expected and Actual Attribute Value didn't match for row number " + i);
                            }

                        }else if(columnName.equalsIgnoreCase("ratio_type")) {

                            columnValue = indvRowArray.getJSONObject(j).get("value").toString();

                            if(!columnValue.equalsIgnoreCase(ratio_type)){
                                customAssert.assertTrue(false,"Expected and Actual ratio_type Value didn't match for row number " + i);
                            }
                        }

                    }

                }

            }

        }catch (Exception e){
            logger.error("Exception in test Method");
            customAssert.assertTrue(false,"Exception in test Method");
        }

        customAssert.assertAll();

    }

//    C129173
    @Test(dataProvider = "dataProviderForSubServiceDataTabValidation", priority = 1, enabled = true)
    public void TestSubServiceDataTab(String flowToTest) {

        logger.info("Validating Sub Service Data Tab for Service Data");

        CustomAssert csAssert = new CustomAssert();

        String parentClientId;

        Create create = new Create();
        Clone clone = new Clone();

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();

        JSONObject valuesJson = new JSONObject();
        int subServiceDataTabId = 63;

        int newServiceDataIdChild = -1;
        int serviceDataIdParent = -1;

        try {

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            serviceDataIdParent = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,uniqueDataString);

            if (serviceDataIdParent == -1) {
                throw new SkipException("Skipping the test as Service Data Id is null");
            }


            Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                    flowToTest, serviceDataIdParent,
                    pricingTemplateFilePath, false,
                    "", -1, null,
                    csAssert);

            if (!uploadPricing) {

                csAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                csAssert.assertAll();
            }

            String cloneResponse = clone.hitClone(serviceDataEntity, serviceDataIdParent);

            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");
            if (!dataJson.getJSONObject("parentService").has("values")) {
                parentClientId = dataJson.getJSONObject("serviceIdClient").getString("values");
                valuesJson.put("name", parentClientId);
                dataJson.getJSONObject("parentService").put("values", valuesJson);
            }
            String serviceIdClientChild = dataJson.getJSONObject("serviceIdClient").getString("values");
            serviceIdClientChild = serviceIdClientChild + "_Child12";
            dataJson.getJSONObject("serviceIdClient").put("values", serviceIdClientChild);

            String serviceIdSupplierChild = dataJson.getJSONObject("serviceIdSupplier").getString("values");
            serviceIdSupplierChild = serviceIdSupplierChild + "_Child12";
            dataJson.getJSONObject("serviceIdSupplier").put("values", serviceIdSupplierChild);
            dataJson.getJSONObject("billingAvailable").put("values", false);
            dataJson.getJSONObject("pricingAvailable").put("values", false);

            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();
            createEntityBodyJson.put("data", dataJson);
            createEntityJson.put("body", createEntityBodyJson);

            create.hitCreate(serviceDataEntity, createEntityJson.toString());
            String createResponse = create.getCreateJsonStr();
            newServiceDataIdChild = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            if (newServiceDataIdChild == -1) {
                logger.error("Unable to create child service data");
                csAssert.assertTrue(false, "Unable to create child service data");
                csAssert.assertAll();
                return;
            }

            String filterPayload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = getTabListResponse(subServiceDataTabId,serviceDataIdParent,filterPayload);

            Map<String,String> columnNameValueMap = createColumnNameValueMap(tabListResponse,csAssert);

            Show show = new Show();
            show.hitShowVersion2(serviceDataEntityTypeId,newServiceDataIdChild);

            String childServiceDataShowPageResponse = show.getShowJsonStr();

            String expectedValueOnConsumptionShowPage;
            String actualValueOnConsumptionShowPage;
            String columnNameTabListResponse;
            String showPageColumnName;

            for(Map.Entry<String, String> entry : columnNameValueMap.entrySet()){
                columnNameTabListResponse = entry.getKey();
                showPageColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"tab column show page column mapping",columnNameTabListResponse);
                expectedValueOnConsumptionShowPage = entry.getValue();

                if(showPageColumnName == null){
                    logger.info("Show page column hierarchy not get for column name " + columnNameTabListResponse);
                    logger.info("Skipping the column ");
                    continue;
                }

                actualValueOnConsumptionShowPage = ShowHelper.getValueOfField(showPageColumnName,childServiceDataShowPageResponse);

                if(actualValueOnConsumptionShowPage == null){
                    List<String> actualValueOnConsumptionShowPageList = ShowHelper.getAllSelectValuesOfField(serviceDataEntityTypeId,newServiceDataIdChild,showPageColumnName);

                    actualValueOnConsumptionShowPage = "";

                    if(actualValueOnConsumptionShowPageList != null) {
                        actualValueOnConsumptionShowPageList = actualValueOnConsumptionShowPageList.stream().sorted().collect(Collectors.toList());
                        for (String actualValue : actualValueOnConsumptionShowPageList) {
                            actualValueOnConsumptionShowPage += actualValue + ", ";
                        }
                    }
                    if(actualValueOnConsumptionShowPage.length() != 0 ){
                        actualValueOnConsumptionShowPage = actualValueOnConsumptionShowPage.substring(0, actualValueOnConsumptionShowPage.length() - 2);
                    }else {
                        actualValueOnConsumptionShowPage = null;
                    }
                }
                if(columnNameTabListResponse.equalsIgnoreCase("consumptionrole_group")){

                    List<String> actualValueOnConsumptionShowPageList  = ShowHelper.getAllSelectedStakeholdersFromShowResponse(childServiceDataShowPageResponse);
                    actualValueOnConsumptionShowPage = "";

                    if(actualValueOnConsumptionShowPageList != null) {
                        for (String actualValue : actualValueOnConsumptionShowPageList) {
                            actualValueOnConsumptionShowPage += actualValue + ", ";
                        }
                    }
                    if(actualValueOnConsumptionShowPage.length() != 0 ){
                        actualValueOnConsumptionShowPage = actualValueOnConsumptionShowPage.substring(0, actualValueOnConsumptionShowPage.length() - 2);
                    }else {
                        actualValueOnConsumptionShowPage = null;
                    }

                }

                if(actualValueOnConsumptionShowPage == null){
                    actualValueOnConsumptionShowPage = "null";
                }

                if(expectedValueOnConsumptionShowPage.equalsIgnoreCase("") && actualValueOnConsumptionShowPage.equalsIgnoreCase("null")){
                    continue;
                }
                if(!expectedValueOnConsumptionShowPage.equalsIgnoreCase(actualValueOnConsumptionShowPage)){

                    logger.error("Expected and Actual value didn't match for column " + columnNameTabListResponse);
                    csAssert.assertTrue(false,"Expected and Actual value didn't match for column " + columnNameTabListResponse);
                }

            }


        } catch (Exception e) {
            logger.error("Exception while validating tabs on service data entity");
            csAssert.assertTrue(false, "Exception while validating tabs on service data entity " + e.getStackTrace());
        }finally {
            EntityOperationsHelper.deleteEntityRecord(serviceDataEntity,newServiceDataIdChild);
            EntityOperationsHelper.deleteEntityRecord(serviceDataEntity,serviceDataIdParent);
        }

        csAssert.assertAll();
    }

//    C141188
    @Test(enabled = true)
    public void TestForecastTab(){

        CustomAssert customAssert = new CustomAssert();
        int tabId = 320;
        try{

            int serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"forecast tab","service data id"));

            String paylaod = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            String tabListResponse = getTabListResponse(tabId,serviceDataId,paylaod);

            JSONObject tabListResponseJson = new JSONObject(tabListResponse);

            JSONArray dataArray = tabListResponseJson.getJSONArray("data");

            JSONObject dataJson = dataArray.getJSONObject(0);

            JSONArray dataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataJson);
            String columnName;
            String columnValue;
            String volume = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"forecast tab","volume");
            String forecast_date = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"forecast tab","forecast_date");
            for(int i =0;i<dataJsonArray.length();i++){

                columnName = dataJsonArray.getJSONObject(i).get("columnName").toString();

                if(columnName.equalsIgnoreCase("forecast_date")){
                    columnValue = dataJsonArray.getJSONObject(i).get("value").toString();

                    if(!columnValue.equalsIgnoreCase(forecast_date)){
                        customAssert.assertTrue(false,"Actual and Expected value for forecast_date didn't match");
                    }

                }else if(columnName.equalsIgnoreCase("forecast_data")){

                    columnValue = dataJsonArray.getJSONObject(i).getJSONObject("value").getJSONArray("data").getJSONObject(0).get("volume").toString();

                    if(!columnValue.equalsIgnoreCase(volume)){
                        customAssert.assertTrue(false,"Actual and Expected value for Volume didn't match");
                        customAssert.assertTrue(false,"Expected " +   volume + " Actual " + columnValue );
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }

    public String getTabListResponse(int tabId,int serviceDataId,String payload){

        TabListData tabListData = new TabListData();

        tabListData.hitTabListData(tabId,serviceDataEntityTypeId,serviceDataId,payload);
        String tabListResponse = tabListData.getTabListDataResponseStr();

        return tabListResponse;

    }

    private Map<String,String> createColumnNameValueMap(String tabListResponse,CustomAssert customAssert){

        Map<String,String> columnNameValueMap = new HashMap<>();

        try {
            JSONObject tabListResponseJson = new JSONObject(tabListResponse);
            JSONObject indRow = tabListResponseJson.getJSONArray("data").getJSONObject(0);

            JSONArray indRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indRow);
            String columnName;
            String columnValue;

            for (int i = 0; i < indRowJsonArray.length(); i++) {

                columnName = indRowJsonArray.getJSONObject(i).get("columnName").toString().toLowerCase();
                columnValue = indRowJsonArray.getJSONObject(i).get("value").toString().toLowerCase();

                if (columnName.equalsIgnoreCase("id")) {
                    columnValue = columnValue.split(":;")[1];

                } else if (columnValue.contains(":;")) {
                    columnValue = columnValue.split(":;")[0];
                }else {
                    try{
                        columnValue = String.valueOf(Double.parseDouble(columnValue));

                    }catch (Exception e){

                    }
                }
                columnNameValueMap.put(columnName,columnValue);
            }
        }catch (Exception e){
            logger.error("Exception while creating ColumnNameValueMap");
            customAssert.assertTrue(false,"Exception while creating ColumnNameValueMap");
        }

        return columnNameValueMap;
    }

}
