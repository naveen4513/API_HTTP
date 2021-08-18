package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.test.filters.TestFilters;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.kafka.common.protocol.types.Field;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

//SIR-5580 SIR-6070
public class TestCurrencyFieldConversion {

    private final static Logger logger = LoggerFactory.getLogger(TestCurrencyFieldConversion.class);

    private String configFilePath;
    private String configFileName;

    String slEntity = "service levels";
    String cslEntity = "child service levels";

    int cslEntityTypeId = 15;
    int slEntityTypeId = 14;

    String outputFilePath;

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestCurrencyFieldConversionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestCurrencyFieldConversionConfigFileName");

        outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\CurrencyConScenarios";

    }

    @Test(enabled = false)
    public void TestCurrencyConWithRateCard(){

        CustomAssert customAssert = new CustomAssert();

        try{

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl with rate card"));

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","conv currency");

            Double calcEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","calculated earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double finalEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","final earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double earnbackAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","earnback amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);


            Double earnbackAmountBalance = finalEarnbackAmount - earnbackAmountPaid;

            Double calcCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","calculated credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double finalCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","final credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double creditAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","credit amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Double creditAmountBalance = finalCreditAmount - creditAmountPaid;

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field csl");
            Double customCurrencyFieldValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Boolean updationStatus = updateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,earnbackAmountBalance,calcCreditAmount,finalCreditAmount,creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue);

            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","with rate card 1"));

            if(updationStatus == false){
                customAssert.assertTrue(false,"Currency Fields updated unsuccessfully");
            }
            Boolean currencyValuesOnShowPage = validateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,
                    earnbackAmountBalance,calcCreditAmount,finalCreditAmount,
                    creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue,convFactor,baseCurrency,convCurrency,customAssert);

            if(!currencyValuesOnShowPage){
                logger.error("Currency Values On ShowPage validated unsuccessfully");
                customAssert.assertTrue(false,"Currency Values On ShowPage validated unsuccessfully");
            }




        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestCurrencyWithSecondRateCard(){

        CustomAssert customAssert = new CustomAssert();

        try{

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl with rate card 2"));

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","conv currency");

            Double calcEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","calculated earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double finalEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","final earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double earnbackAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","earnback amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);


            Double earnbackAmountBalance = finalEarnbackAmount - earnbackAmountPaid;

            Double calcCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","calculated credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double finalCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","final credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double creditAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","credit amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Double creditAmountBalance = finalCreditAmount - creditAmountPaid;

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field csl");
            Double customCurrencyFieldValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Boolean updationStatus = updateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,earnbackAmountBalance,calcCreditAmount,finalCreditAmount,creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue);

            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","with rate card 2"));

            if(updationStatus == false){
                customAssert.assertTrue(false,"Currency Fields updated unsuccessfully");
            }
            Boolean currencyValuesOnShowPage = validateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,
                    earnbackAmountBalance,calcCreditAmount,finalCreditAmount,
                    creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue,convFactor,baseCurrency,convCurrency,customAssert);

            if(!currencyValuesOnShowPage){
                logger.error("Currency Values On ShowPage validated unsuccessfully");
                customAssert.assertTrue(false,"Currency Values On ShowPage validated unsuccessfully");
            }




        }catch (Exception e){

        }

        customAssert.assertAll();

    }

    @Test(enabled = false) // CSL has deleted
    public void TestCurrencyConNoRateCard(){

        CustomAssert customAssert = new CustomAssert();

        try{

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl without rate card"));

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","conv currency");
            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","without rate card"));

            Double calcEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","calculated earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double finalEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","final earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double earnbackAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","earnback amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Double earnbackAmountBalance = finalEarnbackAmount - earnbackAmountPaid;

            Double calcCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","calculated credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double finalCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","final credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            Double creditAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values without rate card","credit amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Double creditAmountBalance = finalCreditAmount - creditAmountPaid;

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field csl");
            Double customCurrencyFieldValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Boolean updationStatus = updateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,earnbackAmountBalance,calcCreditAmount,finalCreditAmount,creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue);

            if(updationStatus == false){
                customAssert.assertTrue(false,"Currency Fields updated unsuccessfully");
            }

            Boolean currencyValuesOnShowPage = validateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,
                    earnbackAmountBalance,calcCreditAmount,finalCreditAmount,
                    creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue,convFactor,baseCurrency,convCurrency,customAssert);

            if(!currencyValuesOnShowPage){
                logger.error("Currency Values On ShowPage validated unsuccessfully");
                customAssert.assertTrue(false,"Currency Values On ShowPage validated unsuccessfully");
            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void TestListingPageAndDownloadedExcel(){

        CustomAssert customAssert = new CustomAssert();

        try{

            int cslId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"csl with rate card"));

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","conv currency");
            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","with rate card 1"));

            Double calcEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","calculated earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            String calcEarnbackAmountConverted = convertValue(calcEarnbackAmount,convFactor);
            Double finalEarnbackAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","final earnback amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            String finalEarnbackAmountConverted = convertValue(finalEarnbackAmount,convFactor);

            Double earnbackAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","earnback amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);


            Double earnbackAmountBalance = finalEarnbackAmount - earnbackAmountPaid;

            Double calcCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","calculated credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            String calcCreditAmountConverted = convertValue(calcCreditAmount,convFactor);

            Double finalCreditAmount = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","final credit amount")) * RandomNumbers.getRandomNumberWithinRange(1,5);
            String finalCreditAmountConverted = convertValue(finalCreditAmount,convFactor);

            Double creditAmountPaid = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","credit amount paid")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Double creditAmountBalance = finalCreditAmount - creditAmountPaid;

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field csl");
            Double customCurrencyFieldValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Boolean updationStatus = updateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,earnbackAmountBalance,calcCreditAmount,finalCreditAmount,creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue);



            if(updationStatus == false){
                customAssert.assertTrue(false,"Currency Fields updated unsuccessfully");
            }

            Boolean currencyValuesOnShowPage = validateCurrencyFields(cslId,calcEarnbackAmount,finalEarnbackAmount,earnbackAmountPaid,
                    earnbackAmountBalance,calcCreditAmount,finalCreditAmount,
                    creditAmountPaid,creditAmountBalance,customCurrencyField,customCurrencyFieldValue,convFactor,baseCurrency,convCurrency,customAssert);

            if(!currencyValuesOnShowPage){
                logger.error("Currency Values On ShowPage validated unsuccessfully");
                customAssert.assertTrue(false,"Currency Values On ShowPage validated unsuccessfully");
            }


            ListRendererListData listRendererListData = new ListRendererListData();
            int cslListId = 265;

            String filterNameCalculatedCreditAmount = "calculatedCreditAmount";
            String filter_id_CalculatedCreditAmount = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"lisitng values validation","filter_id_calculated_ credit_amount");

            String filterNameCalculatedEarnbackAmount = "calculatedEarnbackAmount";
            String filter_id_CalculatedEarnbackAmount = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"lisitng values validation","filter_id_calculated_earnback_amount");

            String filterNameFinalCreditAmount = "finalCreditAmount";
            String filter_id_FinalCreditAmount = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"lisitng values validation","filter_id_final_credit_amount");

            String filterNameFinalEarnbackAmount = "finalEarnbackAmount";
            String filter_id_FinalEarnbackAmount = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"lisitng values validation","filter_id_final_earnback_amount");

            String payload = "{\"filterMap\":{\"entityTypeId\":" + cslEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"" + filter_id_CalculatedCreditAmount + "\":{\"filterId\":\"" + filter_id_CalculatedCreditAmount + "\"," +
                    "\"filterName\":\"calculatedCreditAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + calcCreditAmountConverted + "\",\"max\":\"" + calcCreditAmountConverted + "\",\"suffix\":null}," +
                    "\"" + filter_id_CalculatedEarnbackAmount + "\":{\"filterId\":\"" + filter_id_CalculatedEarnbackAmount + "\"," +
                    "\"filterName\":\"calculatedEarnbackAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + calcEarnbackAmountConverted + "\",\"max\":\"" + calcEarnbackAmountConverted + "\",\"suffix\":null}," +
                    "\"" + filter_id_FinalCreditAmount + "\":{\"filterId\":\"" + filter_id_FinalCreditAmount + "\"," +
                    "\"filterName\":\"finalCreditAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + finalCreditAmountConverted + "\",\"max\":\"" + finalCreditAmountConverted + "\",\"suffix\":null}," +
                    "\"" + filter_id_FinalEarnbackAmount + "\":{\"filterId\":\"" + filter_id_FinalEarnbackAmount + "\"," +
                    "\"filterName\":\"finalEarnbackAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + finalEarnbackAmountConverted + "\",\"max\":\"" + finalEarnbackAmountConverted + "\",\"suffix\":null}}}," +
                    "\"selectedColumns\":[" +
                    "{\"columnId\":12148,\"columnQueryName\":\"bulkcheckbox\"}," +
                    "{\"columnId\":12142,\"columnQueryName\":\"id\"}," +
                    "{\"columnId\":15874,\"columnQueryName\":\"masterslid\"}," +
                    "{\"columnId\":12126,\"columnQueryName\":\"title\"}," +
                    "{\"columnId\":16919,\"columnQueryName\":\"credit_amount_calculated\"}," +
                    "{\"columnId\":16920,\"columnQueryName\":\"earnback_amount_calculated\"}," +
                    "{\"columnId\":16923,\"columnQueryName\":\"final_credit_amount\"}," +
                    "{\"columnId\":16924,\"columnQueryName\":\"final_earnback_amount\"}," +
                    "{\"columnId\":16926,\"columnQueryName\":\"credit_amount_paid\"}," +
                    "{\"columnId\":16927,\"columnQueryName\":\"earnback_amount_paid\"}," +
                    "{\"columnId\":16997,\"columnQueryName\":\"client_credit_amount_calculated\"}," +
                    "{\"columnId\":16998,\"columnQueryName\":\"client_earnback_amount_calculated\"}," +
                    "{\"columnId\":16999,\"columnQueryName\":\"client_final_credit_amount\"}," +
                    "{\"columnId\":17000,\"columnQueryName\":\"client_final_earnback_amount\"}," +
                    "{\"columnId\":17001,\"columnQueryName\":\"client_credit_amount_paid\"}," +
                    "{\"columnId\":17002,\"columnQueryName\":\"client_earnback_amount_paid\"}," +
                    "{\"columnId\":16928,\"columnQueryName\":\"credit_amount_balance\"}," +
                    "{\"columnId\":16929,\"columnQueryName\":\"earnback_amount_balance\"}," +
                    "{\"columnId\":17003,\"columnQueryName\":\"client_credit_amount_balance\"}," +
                    "{\"columnId\":17004,\"columnQueryName\":\"client_earnback_amount_balance\"}]}";


            listRendererListData.hitListRendererListDataV2(cslListId,payload);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            JSONObject listDataResponseJson = new JSONObject(listDataResponse);

            JSONArray dataArray = listDataResponseJson.getJSONArray("data");

            if(dataArray.length() == 0){
                customAssert.assertTrue(false,"No records fetched in listing response");
            }

            JSONArray indJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(0));
            String columnName;
            String columnValue;

            HashMap<String,String> expectedColumnNamesAndValues= new HashMap<>();

            expectedColumnNamesAndValues.put("credit_amount_calculated",String.valueOf(calcCreditAmount));
            expectedColumnNamesAndValues.put("earnback_amount_calculated",String.valueOf(calcEarnbackAmount));
            expectedColumnNamesAndValues.put("final_credit_amount",String.valueOf(finalCreditAmount));
            expectedColumnNamesAndValues.put("final_earnback_amount",String.valueOf(finalEarnbackAmount));
            expectedColumnNamesAndValues.put("credit_amount_paid",String.valueOf(creditAmountPaid));
            expectedColumnNamesAndValues.put("earnback_amount_paid",String.valueOf(earnbackAmountPaid));
            expectedColumnNamesAndValues.put("credit_amount_balance",String.valueOf(creditAmountBalance));

            expectedColumnNamesAndValues.put("earnback_amount_balance",String.valueOf(earnbackAmountBalance));

            expectedColumnNamesAndValues.put("client_credit_amount_calculated",convertValue(calcCreditAmount,convFactor));
            expectedColumnNamesAndValues.put("client_earnback_amount_calculated",convertValue(calcEarnbackAmount,convFactor));

            expectedColumnNamesAndValues.put("client_final_credit_amount",convertValue(finalCreditAmount,convFactor));
            expectedColumnNamesAndValues.put("client_final_earnback_amount",convertValue(finalEarnbackAmount,convFactor));
            expectedColumnNamesAndValues.put("client_credit_amount_paid",convertValue(creditAmountPaid,convFactor));
            expectedColumnNamesAndValues.put("client_earnback_amount_paid",convertValue(earnbackAmountPaid,convFactor));
            expectedColumnNamesAndValues.put("client_credit_amount_balance",convertValue(creditAmountBalance,convFactor));
            expectedColumnNamesAndValues.put("client_earnback_amount_balance",convertValue(earnbackAmountBalance,convFactor));

            String expectedValue;
            for(int i =0 ;i<indJsonArray.length();i++){

                columnName = indJsonArray.getJSONObject(i).get("columnName").toString();
                columnValue = indJsonArray.getJSONObject(i).get("value").toString();

                if(expectedColumnNamesAndValues.containsKey(columnName)){
                    expectedValue = expectedColumnNamesAndValues.get(columnName);

                    if(!columnValue.contains(expectedValue)){
                        customAssert.assertTrue(false,"Expected and Actual Did not match for column" + columnName);
                    }
                }

            }

            String payloadListDataDownload = "{\"filterMap\":{\"entityTypeId\":" + cslEntityTypeId + ",\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"" + filter_id_CalculatedCreditAmount + "\":{\"filterId\":\"300\",\"filterName\":\"calculatedCreditAmount\"," +
                    "\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + calcCreditAmountConverted + "\",\"max\":\"" + calcCreditAmount + "\",\"suffix\":null}," +
                    "\"301\":{\"filterId\":\"" + filter_id_CalculatedEarnbackAmount+ "\",\"filterName\":\"calculatedEarnbackAmount\",\"entityFieldId\":null," +
                    "\"entityFieldHtmlType\":null,\"min\":\"" + calcEarnbackAmountConverted + "\",\"max\":\"" + calcEarnbackAmountConverted + "\",\"suffix\":null}," +
                    "\"302\":{\"filterId\":\"" + filter_id_FinalCreditAmount + "\",\"filterName\":\"finalCreditAmount\",\"entityFieldId\":null," +
                    "\"entityFieldHtmlType\":null,\"min\":\"" + finalCreditAmountConverted + "\",\"max\":\"" + finalCreditAmountConverted + "\",\"suffix\":null}," +
                    "\"303\":{\"filterId\":\"" + filter_id_FinalEarnbackAmount + "\",\"filterName\":\"finalEarnbackAmount\"," +
                    "\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + finalEarnbackAmountConverted + "\",\"max\":\"" + finalEarnbackAmountConverted + "\",\"suffix\":null}}}}";

            int urlId = 265;
            String sheetName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"lisitng values validation","excel download sheet name");

            HashMap<String,String> headersToValidate = new HashMap<>();
            headersToValidate.put("CALCULATED CREDIT AMOUNT", String.valueOf(calcCreditAmount));
            headersToValidate.put("CALCULATED EARNBACK AMOUNT",String.valueOf(calcEarnbackAmount));
            headersToValidate.put("FINAL CREDIT AMOUNT",String.valueOf(finalCreditAmount));
            headersToValidate.put("FINAL EARNBACK AMOUNT",String.valueOf(finalEarnbackAmount));
            headersToValidate.put("CREDIT AMOUNT PAID",String.valueOf(creditAmountPaid));
            headersToValidate.put("EARNBACK AMOUNT PAID",String.valueOf(earnbackAmountPaid));
            headersToValidate.put("CALCULATED CREDIT AMOUNT (" + convCurrency + ")",convertValue(calcCreditAmount,convFactor));
            headersToValidate.put("CALCULATED EARNBACK AMOUNT (" + convCurrency + ")",convertValue(calcEarnbackAmount,convFactor));
            headersToValidate.put("FINAL CREDIT AMOUNT (" + convCurrency + ")",convertValue(finalCreditAmount,convFactor));
            headersToValidate.put("FINAL EARNBACK AMOUNT (" + convCurrency + ")",convertValue(finalEarnbackAmount,convFactor));
            headersToValidate.put("CREDIT AMOUNT PAID (" + convCurrency + ")",convertValue(creditAmountPaid,convFactor));
            headersToValidate.put("EARNBACK AMOUNT PAID (" + convCurrency + ")",convertValue(earnbackAmountPaid,convFactor));
            headersToValidate.put("CREDIT AMOUNT BALANCE",String.valueOf(creditAmountBalance));
            headersToValidate.put("EARNBACK AMOUNT BALANCE",String.valueOf(earnbackAmountBalance));
            String convertedValue = convertValue(creditAmountBalance,convFactor);
            if(!convertedValue.contains(".")){
                convertedValue = convertedValue + ".0";
            }
            headersToValidate.put("CREDIT AMOUNT BALANCE (" + convCurrency + ")",convertedValue);
            convertedValue = convertValue(earnbackAmountBalance,convFactor);
            if(!convertedValue.contains(".")){
                convertedValue = convertedValue + ".0";
            }
            headersToValidate.put("EARNBACK AMOUNT BALANCE (" + convCurrency + ")",convertedValue);

            Boolean excelDownloadValidationStatus =  validateExcelDownload(urlId,payloadListDataDownload,sheetName,headersToValidate,customAssert);

            if(!excelDownloadValidationStatus){
                customAssert.assertTrue(false,"Listing Excel validated unsuccessfully");
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

//    Dev Bug is there
    @Test(enabled = false)
    public void TestReportingCurConEditSL(){
//
        CustomAssert customAssert = new CustomAssert();

        try{
            String entityIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","slid");

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","conv currency");
            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","with rate card 1"));

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field sl");
            String customCurrencyFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value");
            Double customCurrencyFieldValueDouble = Double.parseDouble(customCurrencyFieldValue) * RandomNumbers.getRandomNumberWithinRange(1,5);

            JSONObject editPayload = getEditPayload(slEntity,Integer.parseInt(entityIds));

            JSONObject dynamicFieldJson = editPayload.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyField);

            dynamicFieldJson.put("values",customCurrencyFieldValue);

            editPayload.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyField).put("values",Integer.valueOf(customCurrencyFieldValue));

            editPayload.getJSONObject("body").getJSONObject("data").put("dyn" + customCurrencyField,dynamicFieldJson);

            Edit edit = new Edit();
            String editResponse = edit.hitEdit(slEntity,editPayload.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit Done unsuccessfully");
            }else {

                Show show = new Show();
                show.hitShowVersion2(slEntityTypeId, Integer.parseInt(entityIds));
                String showResponse = show.getShowJsonStr();

                JSONObject showResponseJSon = new JSONObject(showResponse);

                String customCurrencyFieldValueShowPage  = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyField).get("displayValues").toString();

                String convertedValue = convertValue(customCurrencyFieldValueDouble,convFactor);

                if(convertedValue.length() == 4){
                    convertedValue = convertedValue + "0";
                }

                String expectedCustomCurrencyFieldValue  =  Math.round(customCurrencyFieldValueDouble  * 100.0) / 100+ " " + baseCurrency + " (" + convertedValue + " " + convCurrency + ")";
                if(!expectedCustomCurrencyFieldValue.equalsIgnoreCase(customCurrencyFieldValueShowPage)){
                    customAssert.assertTrue(false,"Expected and Actual Value Of custom field didn't match");

                }
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while performing Edit");

        }
        customAssert.assertAll();
//
    }

//    Dev Bug is there
    @Test(enabled = false)
    public void TestReportingCurConBulkUpdateSL(){

        CustomAssert customAssert = new CustomAssert();

        try{

            Download download = new Download();
            String fileName = "BulkUpdateSL.xlsm";
            String outputFile = outputFilePath + "//" + fileName;
            int templateId = 1026;

            String entityIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","slidbulkupdate");

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","conv currency");
            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","with rate card 1"));

            download.hitDownload(outputFile,templateId,slEntityTypeId,entityIds);

            String statusCode = download.getApiStatusCode();

            String sheetName = "Sla";

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field sl");
            Double customCurrencyFieldValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            Boolean excelUpdateStatus = XLSUtils.updateColumnValue(outputFilePath,fileName,sheetName,6,9,customCurrencyFieldValue);

            String expectedCustomCurrencyFieldValue;

            if(!excelUpdateStatus){
                customAssert.assertTrue(false,"Excel updated unsuccessfully");
            }else {

                UploadBulkData uploadBulkData = new UploadBulkData();
                Map<String,String> payloadMap = new HashMap<>();

                payloadMap.put("entityTypeId",String.valueOf(slEntityTypeId));
                payloadMap.put("upload","Submit");
                payloadMap.put("_csrf_token","c32411cb-7120-4b80-b70b-c77d160de529");

                uploadBulkData.hitUploadBulkData(slEntityTypeId,templateId,outputFilePath,fileName,payloadMap);

                String bulkUpdateStatus = uploadBulkData.getUploadBulkDataJsonStr();

                if(!bulkUpdateStatus.contains("200")){
                    customAssert.assertTrue(false,"Error while bulk update");
                }else {
                    Thread.sleep(5000);

                    String convertedValue = convertValue(customCurrencyFieldValue, convFactor);
                    if(convertedValue.length() == 4){
                        convertedValue = convertedValue + "0";
                    }

                    expectedCustomCurrencyFieldValue  =  Math.round(customCurrencyFieldValue  * 100.0) / 100+ " " + baseCurrency + " (" + convertedValue + " " + convCurrency + ")";

                    Boolean showPageValidation = validateDynamicFieldsOnShowPage(slEntityTypeId,Integer.parseInt(entityIds),customCurrencyField,expectedCustomCurrencyFieldValue,customAssert);

                    if(!showPageValidation){
                        customAssert.assertTrue(false,"Show page validated unsuccessfully ");
                    }
                }

            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while Bulk update " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

//    Dev Bug is there
    @Test(enabled = false)
    public void TestReportingCurConBulkEditSL() {

        CustomAssert customAssert = new CustomAssert();

        try {

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","conv currency");
            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","with rate card 1"));

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field sl");
            Double customCurrencyFieldValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            String entityIdToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","slid");

            String bulkEditPayload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"" +
                    "entityTypeId\",\"values\":" + slEntityTypeId + ",\"multiEntitySupport\":false},\"" +
                    "dyn" + customCurrencyField + "\":{\"name\":\"dyn" + customCurrencyField + "\",\"values\":" + customCurrencyFieldValue + ",\"multiEntitySupport\":false,\"id\":" + customCurrencyField + "}," +
                    "\"dynamicMetadata\":{\"dyn" + customCurrencyField + "\":{\"name\":\"dyn" + customCurrencyField + "\",\"id\":" + customCurrencyField + "," +
                    "\"multiEntitySupport\":false,\"values\":" + customCurrencyFieldValue + "}}}," +
                    "\"globalData\":{\"entityIds\":[" + entityIdToTest + "],\"fieldIds\":[" + customCurrencyField + "],\"isGlobalBulk\":true}}}";

            BulkeditEdit bulkeditEdit = new BulkeditEdit();
            bulkeditEdit.hitBulkeditEdit(slEntityTypeId, bulkEditPayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {

                customAssert.assertTrue(false, "Bulk edit done unsuccessfully ");
            }else {

                Thread.sleep(6000);

                String convertedValue = convertValue(customCurrencyFieldValue, convFactor);
                if(convertedValue.length() == 4){
                    convertedValue = convertedValue + "0";
                }

                String expectedCustomCurrencyFieldValue = Math.round(customCurrencyFieldValue * 100.0) / 100 + " " + baseCurrency + " (" + convertedValue + " " + convCurrency + ")";

                Boolean showPageValidation = validateDynamicFieldsOnShowPage(slEntityTypeId, Integer.parseInt(entityIdToTest), customCurrencyField, expectedCustomCurrencyFieldValue, customAssert);

                if (!showPageValidation) {
                    customAssert.assertTrue(false, "Show page validated unsuccessfully ");
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Bulk Edit Scenario");
        }

        customAssert.assertAll();
    }
//
    @Test(enabled = false)
    public void TestReportingCurConBulkEditCSL(){

        CustomAssert customAssert = new CustomAssert();

        try {

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

            String baseCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","base currency");
            String convCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency values with rate card","conv currency");
            Double convFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conversion factor","with rate card 1"));

            String customCurrencyField = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field csl");
            Double customCurrencyFieldValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamic currency field value")) * RandomNumbers.getRandomNumberWithinRange(1,5);

            String childServiceLevelId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","cslid");

            int entityIdToTest = Integer.parseInt(childServiceLevelId);

            String bulkEditPayload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"" +
                    "entityTypeId\",\"values\":" + cslEntityTypeId + ",\"multiEntitySupport\":false},\"" +
                    "dyn" + customCurrencyField + "\":{\"name\":\"dyn" + customCurrencyField + "\",\"values\":" + customCurrencyFieldValue + ",\"multiEntitySupport\":false,\"id\":" + customCurrencyField + "}," +
                    "\"dynamicMetadata\":{\"dyn" + customCurrencyField + "\":{\"name\":\"dyn" + customCurrencyField + "\",\"id\":" + customCurrencyField + "," +
                    "\"multiEntitySupport\":false,\"values\":" + customCurrencyFieldValue + "}}}," +
                    "\"globalData\":{\"entityIds\":[" + entityIdToTest + "],\"fieldIds\":[" + customCurrencyField + "],\"isGlobalBulk\":true}}}";

            BulkeditEdit bulkeditEdit = new BulkeditEdit();
            bulkeditEdit.hitBulkeditEdit(cslEntityTypeId, bulkEditPayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {

                customAssert.assertTrue(false, "Bulk edit done unsuccessfully ");
            }else {

                Thread.sleep(6000);

                String convertedValue = convertValue(customCurrencyFieldValue, convFactor);
                if(convertedValue.length() == 4){
                    convertedValue = convertedValue + "0";
                }

                String expectedCustomCurrencyFieldValue = Math.round(customCurrencyFieldValue * 100.0) / 100 + " " + baseCurrency + " (" + convertedValue + " " + convCurrency + ")";


                Boolean showPageValidation = validateDynamicFieldsOnShowPage(cslEntityTypeId,entityIdToTest,customCurrencyField,expectedCustomCurrencyFieldValue,customAssert);

                if(!showPageValidation){
                    customAssert.assertTrue(false,"Show page validated unsuccessfully ");
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Bulk Edit Scenario");
        }

        customAssert.assertAll();

    }


    private Boolean updateCurrencyFields(int cslId,Double calcEarnbackAmount,
                                         Double finalEarnbackAmount,Double earnbackAmountPaid,
                                         Double earnbackAmountBalance,Double calcCreditAmount,
                                         Double finalCreditAmount,Double creditAmountPaid,
                                         Double creditAmountBalance,String customCurrencyField,Double customCurrencyFieldValue) {


        Boolean editStatus = true;
        Edit edit = new Edit();
        String editResponse;
        try {

            editResponse = edit.hitEdit(cslEntity, cslId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("calculatedEarnbackAmount").put("values",calcEarnbackAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalEarnbackAmount").put("values", finalEarnbackAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("earnbackAmountPaid").put("values", earnbackAmountPaid);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("earnbackAmountBalance").put("values", earnbackAmountBalance);

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("calculatedCreditAmount").put("values", calcCreditAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalCreditAmount").put("values", finalCreditAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("creditAmountPaid").put("values", creditAmountPaid);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("creditAmountBalance").put("values", creditAmountBalance);

            if(customCurrencyField !=null) {

                JSONObject dynamicValuesJson = new JSONObject();
                dynamicValuesJson.put("name","dyn" + customCurrencyField);
                dynamicValuesJson.put("id",customCurrencyField);
                dynamicValuesJson.put("values",customCurrencyFieldValue);

                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").put("dyn" + customCurrencyField,dynamicValuesJson);
                editResponseJson.getJSONObject("body").getJSONObject("data").put("dyn" + customCurrencyField, dynamicValuesJson);


            }
            editResponse = edit.hitEdit(cslEntity, editResponseJson.toString());

            if (!editResponse.contains("success")) {

                editStatus = false;
            }

        } catch (Exception e) {
            logger.error("Exception while Editing custom currency Fields  " + e.getMessage());
            editStatus = false;
        }


        return editStatus;
    }



    private Boolean validateCurrencyFields(int cslId,Double calcEarnbackAmount,
                                           Double finalEarnbackAmount,Double earnbackAmountPaid,
                                           Double earnbackAmountBalance,Double calcCreditAmount,
                                           Double finalCreditAmount,Double creditAmountPaid,
                                           Double creditAmountBalance,String customCurrencyField,
                                           Double customCurrencyFieldValue,Double conversionFactor,
                                         String baseCurrency,String convCurrency , CustomAssert customAssert) {


        Boolean validationStatus = true;
        Show show = new Show();

        String showResponse;
        try {

            show.hitShowVersion2(cslEntityTypeId,cslId);

            showResponse = show.getShowJsonStr();
            JSONObject showResponseJson = new JSONObject(showResponse);

            JSONObject dataJson = showResponseJson.getJSONObject("body").getJSONObject("data");

            String calculatedEarnbackAmountShowPage = dataJson.getJSONObject("calculatedEarnbackAmount").get("displayValues").toString().replace(",","");
            String finalEarnbackAmountShowPage = dataJson.getJSONObject("finalEarnbackAmount").get("displayValues").toString().replace(",","");
            String earnbackAmountPaidShowPage = dataJson.getJSONObject("earnbackAmountPaid").get("displayValues").toString().replace(",","");
            String earnbackAmountBalanceShowPage = dataJson.getJSONObject("earnbackAmountBalance").get("displayValues").toString().replace(",","");

            String calculatedCreditAmountShowPage = dataJson.getJSONObject("calculatedCreditAmount").get("displayValues").toString().replace(",","");
            String finalCreditAmountShowPage = dataJson.getJSONObject("finalCreditAmount").get("displayValues").toString().replace(",","");
            String creditAmountPaidShowPage = dataJson.getJSONObject("creditAmountPaid").get("displayValues").toString().replace(",","");
            String creditAmountBalanceShowPage = dataJson.getJSONObject("creditAmountBalance").get("displayValues").toString().replace(",","");

            String customCurrencyFieldValueShowPage = dataJson.getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrencyField).get("displayValues").toString().replace(",","");

            String expectedcalcEarnbackAmount  =  Math.round(calcEarnbackAmount * 100.0) / 100 + " " + baseCurrency + " (" + convertValue(calcEarnbackAmount,conversionFactor)+ " " + convCurrency + ")";
            String expectedfinalEarnbackAmount  =  Math.round(finalEarnbackAmount * 100.0) / 100 + " " + baseCurrency + " (" + convertValue(finalEarnbackAmount,conversionFactor) + " " + convCurrency + ")";
            String expectedearnbackAmountPaid  =  Math.round(earnbackAmountPaid * 100.0) / 100 + " " + baseCurrency + " (" + convertValue(earnbackAmountPaid,conversionFactor) + " " + convCurrency + ")";
            String expectedearnbackAmountBalance  =  Math.round(earnbackAmountBalance * 100) / 100 + " " + baseCurrency + " (" + convertValue(earnbackAmountBalance,conversionFactor) + " " + convCurrency + ")";

            String expectedcalcCreditAmount  =  Math.round(calcCreditAmount  * 100.0) / 100 + " " + baseCurrency + " (" + convertValue(calcCreditAmount,conversionFactor) + " " + convCurrency + ")";
            String expectedfinalCreditAmount  =  Math.round(finalCreditAmount  * 100.0) / 100+ " " + baseCurrency + " (" + convertValue(finalCreditAmount,conversionFactor) + " " + convCurrency + ")";
            String expectedcreditAmountPaid  =  Math.round(creditAmountPaid  * 100.0) / 100+ " " + baseCurrency + " (" + convertValue(creditAmountPaid,conversionFactor) + " " + convCurrency + ")";
            String expectedcreditAmountBalance  =  Math.round(creditAmountBalance  * 100.0) / 100+ " " + baseCurrency + " (" + convertValue(creditAmountBalance,conversionFactor) + " " + convCurrency + ")";

            if(!expectedcalcEarnbackAmount.equalsIgnoreCase(calculatedEarnbackAmountShowPage)){
                logger.error("expectedcalcEarnbackAmount and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedcalcEarnbackAmount and actual value from show page didn't match");
                validationStatus = false;
            }

            if(!expectedfinalEarnbackAmount.equalsIgnoreCase(finalEarnbackAmountShowPage)){
                logger.error("expectedfinalEarnbackAmount and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedfinalEarnbackAmount and actual value from show page didn't match");
                validationStatus = false;
            }

            if(!expectedearnbackAmountPaid.equalsIgnoreCase(earnbackAmountPaidShowPage)){

                logger.error("expectedearnbackAmountPaid and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedearnbackAmountPaid and actual value from show page didn't match");
                validationStatus = false;
            }

            if(!expectedearnbackAmountBalance.equalsIgnoreCase(earnbackAmountBalanceShowPage)){
                logger.error("expectedearnbackAmountBalance and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedearnbackAmountBalance and actual value from show page didn't match");
                validationStatus = false;
            }

            if(!expectedcalcCreditAmount.equalsIgnoreCase(calculatedCreditAmountShowPage)){
                logger.error("expectedcalcCreditAmount and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedcalcCreditAmount and actual value from show page didn't match");
                validationStatus = false;
            }

            if(!expectedfinalCreditAmount.equalsIgnoreCase(finalCreditAmountShowPage)){
                logger.error("expectedfinalCreditAmount and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedfinalCreditAmount and actual value from show page didn't match");
                validationStatus = false;
            }

            if(!expectedcreditAmountPaid.equalsIgnoreCase(creditAmountPaidShowPage)){
                logger.error("expectedcreditAmountPaid and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedcreditAmountPaid and actual value from show page didn't match");
                validationStatus = false;
            }

            if(!expectedcreditAmountBalance.equalsIgnoreCase(creditAmountBalanceShowPage)){
                logger.error("expectedcreditAmountBalance and actual value from show page didn't match");
                customAssert.assertTrue(false,"expectedcreditAmountBalance and actual value from show page didn't match");
                validationStatus = false;
            }

            //Special handling for dynamic field scenario
            String covertedValue = convertValue(customCurrencyFieldValue,conversionFactor);

            if(covertedValue.contains(".")) {
                if (covertedValue.split("\\.")[1].length() == 1){
                    covertedValue = covertedValue + "0";
                }
            }else {
                covertedValue = covertedValue + ".00";
            }

            String expectedCustomCurrencyFieldValue  =  Math.round(customCurrencyFieldValue  * 100.0) / 100+ " " + baseCurrency + " (" + covertedValue + " " + convCurrency + ")";

            if(!expectedCustomCurrencyFieldValue.equalsIgnoreCase(customCurrencyFieldValueShowPage)){
                logger.error("Expected Custom Currency FieldValue and actual value from show page didn't match " +expectedCustomCurrencyFieldValue);
                customAssert.assertTrue(false,"Expected Custom Currency FieldValue and actual value from show page didn't match "+expectedCustomCurrencyFieldValue);
                validationStatus = false;
            }


        } catch (Exception e) {

            logger.error("Exception while validating currency fields on show page  " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating custom currency Fields on show page " + e.getMessage());
            validationStatus = false;
        }


        return validationStatus;
    }

    private String convertValue(Double value,Double conversionFactor){

        String covertedValue = null;

        Double roundOfValue = Math.round(value * conversionFactor * 1000.0) / 1000.0;
        covertedValue = roundOfValue.toString();

        if(roundOfValue.toString().contains(".0")){
            Long roundOfValueLong = Math.round(value * conversionFactor * 100) / 100;
            covertedValue = roundOfValueLong.toString();

        }

        return covertedValue;

    }

    private boolean validateExcelDownload(int urlId, String payload, String sheetName, HashMap<String,String> headersToValidate,CustomAssert customAssert){

        Boolean excelValidationStatus = true;
        DownloadListWithData downloadListWithData = new DownloadListWithData();
        Map<String,String> formParam = new HashMap<>();

        try{
            String downloadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\CurrencyConScenarios";
            String downloadFileName = "Child Service Levels.xlsx";


            formParam.put("_csrf_token","null");
            formParam.put("jsonData",payload);

            HttpResponse downloadResponse =  downloadListWithData.hitDownloadListWithData(formParam,urlId);
            Boolean downloadStatus = downloadListWithData.dumpDownloadListIntoFile(downloadResponse,downloadFilePath,downloadFileName);

            if(!downloadStatus){
                logger.error("Listing Data File Downloaded unsuccessfully");
                customAssert.assertTrue(false,"Listing Data File Downloaded unsuccessfully");
            }

            List<String> cellDataHeaders = XLSUtils.getExcelDataOfOneRow(downloadFilePath,downloadFileName,sheetName,4);
            String headerName;
            HashMap<String,Integer> headerNameColNum = new HashMap<>();

            for(int i =0;i<cellDataHeaders.size();i++ ){
                headerName = cellDataHeaders.get(i);

                if(headersToValidate.containsKey(headerName)){
                    headerNameColNum.put(headerName,i);
                }
            }
            List<String> cellDataHeadersDataValues = XLSUtils.getExcelDataOfOneRow(downloadFilePath,downloadFileName,sheetName,5);
            int columnId;
            String headerValue;
            String expectedValue;
            for (Map.Entry<String,String> entry : headersToValidate.entrySet()){

                headerName = entry.getKey();

                if(headerNameColNum.containsKey(headerName)){
                    columnId = headerNameColNum.get(headerName);
                    headerValue = cellDataHeadersDataValues.get(columnId);
                    expectedValue = headersToValidate.get(headerName);
                    if(!headerValue.equals(expectedValue)){
                        logger.error("Expected value mismatched in downloaded excel for header name " + headerName + " Expected Value " + expectedValue + " Actual Value " + headerValue);
                        customAssert.assertTrue(false,"Expected value mismatched in downloaded excel for header name " + headerName + " Expected Value " + expectedValue + " Actual Value " + headerValue);
                        excelValidationStatus = false;
                    }
                }else {
                    logger.error("Header names not found in the excel "+headerName);
                    customAssert.assertTrue(false,"Header names not found in the excel "+headerName);
                    excelValidationStatus = false;
                }

            }

    }catch (Exception e){
            logger.error("Exception while downloading excel " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while downloading excel " + e.getStackTrace());
            excelValidationStatus = false;
        }

        return excelValidationStatus;
    }

    private synchronized Boolean validateDynamicFieldsOnShowPage(int entityTypeId,int entityId,String dynamicField,String customFieldValue,CustomAssert customAssert){

        Boolean validationStatus = true;
        try {
            Show show = new Show();
            show.hitShowVersion2(entityTypeId, entityId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            String customCurrencyFieldValue = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + dynamicField).get("displayValues").toString().replace(",","");

            if (!customCurrencyFieldValue.equalsIgnoreCase(customFieldValue)) {
                customAssert.assertTrue(false, "Expected and Actual Value Of custom field didn't match");
                validationStatus = false;

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating values on show page " + e.getStackTrace());
            validationStatus = false;
        }
        return validationStatus;
    }

    private JSONObject getEditPayload(String entityName,int entityId){

        Edit edit = new Edit();

        String editPayload = null;
        JSONObject editResponseJson;

        try {

            editPayload = edit.hitEdit(entityName, entityId);

            editResponseJson = new JSONObject(editPayload);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

        }catch (Exception e){
            editResponseJson = null;
        }

        return editResponseJson;
    }

}
