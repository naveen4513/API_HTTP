//estInvoiceCreationListingShowPageValidation
package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Invoice;
import com.sirionlabs.helper.entityCreation.InvoiceLineItem;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceCreationListingShowPageValidation {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceCreationListingShowPageValidation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer invoiceEntityTypeId;
    private String invoice = "invoice";
    private Map<String, String> expectedShowPageData;
    private Map<String, String> expectedListingData;
    private int invoiceListId;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceCreationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extrafieldsconfigfilepath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extrafieldsconfigfilename");
        invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
         invoiceListId = ConfigureConstantFields.getListIdForEntity("invoices");
    }




    @Test()
    public void testInvoiceCreation() {
        CustomAssert csAssert = new CustomAssert();
        Integer invoiceId = -1;
        String flowToTest = "flow 4";

        try {

            expectedShowPageData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "flow 4 show page");
            expectedListingData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "flow 4 listing page");

            String createResponse = "";
                createResponse = Invoice.createInvoice(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                        true);
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedresult");
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    invoiceId = CreateEntity.getNewEntityId(createResponse, "invoices");

                if (expectedResult.trim().equalsIgnoreCase("success")) {
                    if (createStatus.equalsIgnoreCase("success")) {
                        if (invoiceId != -1) {
                            logger.info("Invoice Created Successfully with Id {}: ", invoiceId);
                            String shortCodeId = CreateEntity.getShortId(createResponse, "GB");
                            expectedShowPageData.put("shortCodeId", shortCodeId);
                            expectedListingData.put("id", String.valueOf(invoiceId));
                            //Validate Show Page is Accessible for Newly Created Invoice Line Item
                            logger.info("Hitting Show API for Invoice Id {}", invoiceId);
                            Show showObj = new Show();
                            showObj.hitShow(invoiceEntityTypeId, invoiceId);
                            String showResponse = showObj.getShowJsonStr();

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!showObj.isShowPageAccessible(showResponse)) {
                                    csAssert.assertTrue(false, "Show Page is Not Accessible for Invoice Id " + invoiceId);
                                } else {
                                    verifyShowPageData(showResponse, csAssert);
                                    ListRendererListData listRendererListData1= new ListRendererListData();
                                    listRendererListData1.hitListRendererListData( invoiceListId);
                                    String listDataJsonStr1 = listRendererListData1.getListDataJsonStr();
                                    verifyListingPage(listDataJsonStr1, csAssert);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Invoice Id " + invoiceId + " is an Invalid JSON.");
                            }


                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create Invoice for Flow [" + flowToTest + "] due to " + createStatus);
                    }
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Invoice Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Invoice Creation Flow [" + flowToTest + "]. " + e.getStackTrace());
        }finally {
     EntityOperationsHelper.deleteEntityRecord("invoices", invoiceId);
                    }
        csAssert.assertAll();
    }



    public void verifyShowPageData(String showPageResponse, CustomAssert customAssert) {
        try {
            JSONObject jsonObject = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
            for (String key : expectedShowPageData.keySet()) {
                if (key.equalsIgnoreCase("dynamicMetadata")) {
                    verifyDynamicValue(jsonObject.getJSONObject(key), expectedShowPageData.get(key), customAssert);
                } else {
                    Object actualValue = jsonObject.getJSONObject(key).get("values");
                    if (actualValue instanceof String) {
                        String expectedStringValue = expectedShowPageData.get(key);
                        if (key.contains("Date"))
                            expectedStringValue = calculateValueForDateField(expectedStringValue);
                        if (!jsonObject.getJSONObject(key).getString("values").equalsIgnoreCase(expectedStringValue)) {
                            logger.error("actual value of " + key + " is " + jsonObject.getJSONObject(key).getString("values") + "and Expected Value of " + key + " is " + expectedStringValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On  Show Page ::" + jsonObject.getJSONObject(key).getString("values") + "::" + expectedStringValue);
                        }
                    } else if (actualValue instanceof JSONArray) {
                        List<String> expectedValueArray = new ArrayList<>(Arrays.asList(expectedShowPageData.get(key).split(",")));
                        JSONArray actualValueArray = jsonObject.getJSONObject(key).getJSONArray("values");
                        customAssert.assertTrue(expectedValueArray.size() == actualValueArray.length(), "actual size of " + key + " is " + actualValueArray.length() + "and Expected size of " + key + " is " + expectedValueArray.size() + " on show page ");
                        for (int i = 0; i < actualValueArray.length(); i++) {
                            if (!expectedValueArray.contains(actualValueArray.getJSONObject(i).getString("name"))) {
                                logger.error("actual value of " + key + " is " + actualValueArray.getJSONObject(i).getString("name") + "and Expected Value of " + key + " is " + expectedValueArray + " on show page ");
                                customAssert.assertTrue(false, "Actual And Expected Value are Different On  Show Page");
                            }
                        }
                    } else if (actualValue instanceof JSONObject) {
                        String expectedValueObject = expectedShowPageData.get(key);
                        if (!expectedValueObject.equalsIgnoreCase(jsonObject.getJSONObject(key).getJSONObject("values").getString("name"))) {
                            logger.error("actual value of " + key + " is " + jsonObject.getJSONObject(key).getJSONObject("values").getString("name") + "and Expected Value of " + key + " is " + expectedValueObject + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On  Show Page ::" + jsonObject.getJSONObject(key).getJSONObject("values").getString("name") + "::" + expectedValueObject);

                        }
                    } else if (actualValue instanceof Boolean) {
                        String expectedValueBoolean = expectedShowPageData.get(key);
                        if (expectedValueBoolean.equalsIgnoreCase("true")) {
                            customAssert.assertTrue(jsonObject.getJSONObject(key).getBoolean("values"), "Actual And Expected Value are Different On Show Page ::false :: true");
                        } else {
                            customAssert.assertTrue(!jsonObject.getJSONObject(key).getBoolean("values"), "Actual And Expected Value are Different On Show Page ::true :: false");
                        }
                    }
                    else if (actualValue instanceof Integer) {
                        String expectedValue = expectedShowPageData.get(key);
                        if (!expectedValue.equalsIgnoreCase(String.valueOf(jsonObject.getJSONObject(key).getInt("values")))) {
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page ::"+jsonObject.getJSONObject(key).getInt("values")+" ::"+expectedValue);

                        }

                    }
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Governance Body Show Page" + e.getMessage());
        }
        customAssert.assertAll();
    }

    private void verifyDynamicValue(JSONObject jsonObject, String actualData, CustomAssert customAssert) {

        try {
            actualData = actualData.substring(1, actualData.length() - 1);
            String[] actualArray = actualData.split(":;");
            // customAssert.assertTrue(actualData.length() == jsonObject.names().length(), "Actual and Expected dynamic value different");
            for (String s : actualArray) {
                String[] dynamic = s.split("::");
                String dynamicKey = dynamic[0];
                String dynamicValue = dynamic[1];
                if (dynamicKey.equalsIgnoreCase(jsonObject.getJSONObject(dynamicKey).getString("name"))) {
                    Object actualValue = jsonObject.getJSONObject(dynamicKey).get("values");
                    if (actualValue instanceof String) {
                        if (!dynamicValue.contains(jsonObject.getJSONObject(dynamicKey).getString("values"))) {
                            logger.error("actual value of " + dynamicKey + " is " + jsonObject.getJSONObject(dynamicKey).getString("values") + "and Expected Value of " + dynamicKey + " is " + dynamicValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page ::" + jsonObject.getJSONObject(dynamicKey).getString("values") + "::" + dynamicValue);
                        }
                    } else if (actualValue instanceof Integer) {
                        if (!dynamicValue.contains(String.valueOf(jsonObject.getJSONObject(dynamicKey).getInt("values")))) {
                            logger.error("actual value of " + dynamicKey + " is " + jsonObject.getJSONObject(dynamicKey).getInt("values") + "and Expected Value of " + dynamicKey + " is " + dynamicValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On  Show Page ::" + jsonObject.getJSONObject(dynamicKey).getInt("values") + "::" + dynamicValue);
                        }
                    } else if (actualValue instanceof JSONArray) {
                        List<String> expectedValueArray = new ArrayList<>(Arrays.asList(dynamicValue.split(",")));
                        JSONArray actualValueArray = jsonObject.getJSONObject(dynamicKey).getJSONArray("values");
                        customAssert.assertTrue(expectedValueArray.size() == actualValueArray.length(), "actual size of " + dynamicKey + " is " + actualValueArray.length() + "and Expected size of " + dynamicKey + " is " + expectedValueArray.size() + " on show page ");
                        for (int j = 0; j < actualValueArray.length(); j++) {
                            if (!expectedValueArray.contains(actualValueArray.getJSONObject(j).getString("name"))) {
                                logger.error("actual value of " + dynamicKey + " is " + actualValueArray.getJSONObject(j).getString("name") + "and Expected Value of " + dynamicKey + " is " + expectedValueArray + " on show page ");
                                customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page");
                            }
                        }
                    } else if (actualValue instanceof JSONObject) {
                        if (!dynamicValue.equalsIgnoreCase(jsonObject.getJSONObject(dynamicKey).getJSONObject("values").getString("name"))) {
                            logger.error("actual value of " + dynamicKey + " is " + jsonObject.getJSONObject(dynamicKey).getJSONObject("values").getString("name") + "and Expected Value of " + dynamicKey + " is " + dynamicValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page ::" + jsonObject.getJSONObject(dynamicKey).getJSONObject("values").getString("name") + "::" + dynamicValue);

                        }
                    } else if (actualValue instanceof Boolean) {
                        if (dynamicValue.equalsIgnoreCase("true")) {
                            customAssert.assertTrue(jsonObject.getJSONObject(dynamicKey).getBoolean("values"), "Actual And Expected Value are Different On Show Page ::false :: true");
                        } else {
                            customAssert.assertTrue(!jsonObject.getJSONObject(dynamicKey).getBoolean("values"), "Actual And Expected Value are Different On Show Page ::true :: false");
                        }
                    }
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Invoice Show Page Dynamic Filed" + e.getMessage());
        }
        customAssert.assertAll();
    }
    public void verifyListingPage(String listDataJsonStr, CustomAssert customAssert) {

        try {
            Map<String, String> actualListingData = new HashMap<>();
            JSONArray jsonArray = new JSONObject(listDataJsonStr).getJSONArray("data").getJSONObject(0).names();
            for (int i = 0; i < jsonArray.length(); i++) {
                String columnName = new JSONObject(listDataJsonStr).getJSONArray("data").getJSONObject(0).getJSONObject(jsonArray.getString(i)).getString("columnName");
                Object columnValue = new JSONObject(listDataJsonStr).getJSONArray("data").getJSONObject(0).getJSONObject(jsonArray.getString(i)).get("value");
                actualListingData.put(columnName, columnValue.toString());
            }


            expectedListingData.put("expectedreceiptdate", calculateValueForDateField(expectedListingData.get("expectedreceiptdate")));
            expectedListingData.put("datemodified", calculateValueForDateField(expectedListingData.get("datemodified")));
            expectedListingData.put("invoicedate", calculateValueForDateField(expectedListingData.get("invoicedate")));
            expectedListingData.put("paymentduedate", calculateValueForDateField(expectedListingData.get("paymentduedate")));
            expectedListingData.put("datecreated", calculateValueForDateField(expectedListingData.get("datecreated")));
            expectedListingData.put("systemreceiptdate", calculateValueForDateField(expectedListingData.get("systemreceiptdate")));
            expectedListingData.put("actualpaymentdate", calculateValueForDateField(expectedListingData.get("actualpaymentdate")));

            for (Map.Entry<String, String> values : expectedListingData.entrySet()) {
                String expectedKey = values.getKey();
                String expectedValue = values.getValue();
                String actualValue = actualListingData.get(expectedKey);
                customAssert.assertTrue(actualValue.contains(expectedValue), "Expected and Actual Value Are Different on Listing page :: " + expectedValue + " :: " + actualValue);
            }
        }
        catch (Exception e) {
            customAssert.assertTrue(false,"Exception while verifying invoice data on listing page"+e.getMessage());
        }
    }
    private String calculateValueForDateField(String value) {
        String outputDate = null;

        try {
            String[] dateFields = value.split("->");
            String dateFormat = dateFields[1].trim();
            int daysToAdd = Integer.parseInt(dateFields[2].trim());
            String currentDate = DateUtils.getCurrentDateInAnyFormat(dateFormat);

            outputDate = DateUtils.getDateOfXDaysFromYDate(currentDate, daysToAdd, dateFormat);
        } catch (Exception e) {
            logger.error("Exception while Calculating Default Value for Date field with Value {}. {}", value, e.getMessage());
        }
        return outputDate;
    }
}

