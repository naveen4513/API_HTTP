package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class TestESGovernanceBodyCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestGovernanceBodyCreation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer gbEntityTypeId;
    private static Boolean deleteEntity = true;
    private Map<String, String> expectedShowPageData;
    private Map<String, String> expectedListingData;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("GovernanceBodyCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("GovernanceBodyCreationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        gbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body");
        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
        expectedShowPageData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "gb_elastic_show_page");
        expectedListingData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "gb_elastic_listing_page");
    }

    @DataProvider
    public Object[][] dataProviderForTestGBCreation() throws ConfigurationException {
        logger.info("Setting all Governance Body Creation Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> flowsToTest = new ArrayList<>();

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
            logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
            flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
        } else {
            String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstovalidatees").split(",");
            for (String flow : allFlows) {
                if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
                    flowsToTest.add(flow.trim());
                } else {
                    logger.info("Flow having name [{}] not found in Governance Body Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTestGBCreation")
    public void testGBCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int gbId = -1;

        try {
            logger.info("Validating GB Creation Flow [{}]", flowToTest);

            //Validate GB Creation
            logger.info("Creating GB for Flow [{}]", flowToTest);
            String createResponse = GovernanceBody.createGB(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "gb_elastic",
                    true);

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);
                if (createStatus.equalsIgnoreCase("success"))
                    gbId = CreateEntity.getNewEntityId(createResponse, "governance body");
                String shortCodeId = CreateEntity.getShortId(createResponse, "GB");
                logger.info("Governance Body Created Successfully with shortCodeId {}", shortCodeId);
                expectedShowPageData.put("shortCodeId", shortCodeId);
                expectedListingData.put("id", String.valueOf(gbId));
                if (expectedResult.trim().equalsIgnoreCase("success")) {
                    if (createStatus.equalsIgnoreCase("success")) {
                        if (gbId != -1) {
                            logger.info("Governance Body Created Successfully with Id {}: ", gbId);
                            logger.info("Hitting Show API for Governance Body Id {}", gbId);
                            Show showObj = new Show();
                            showObj.hitShow(gbEntityTypeId, gbId);
                            String showResponse = showObj.getShowJsonStr();

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!showObj.isShowPageAccessible(showResponse)) {
                                    csAssert.assertTrue(false, "Show Page is Not Accessible for Governance Body Id " + gbId);
                                } else {
                                   verifyShowPageData(showResponse, csAssert);
                                    ListRendererListData listRendererListData = new ListRendererListData();
                                    listRendererListData.hitListRendererListData(211);
                                    String listDataJsonStr = listRendererListData.getListDataJsonStr();
                                    verifyListingPage(listDataJsonStr, csAssert);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Governance Body Id " + gbId + " is an Invalid JSON.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create Governance Body for Flow [" + flowToTest + "] due to " + createStatus);
                        if (createStatus.equals("validationError"))
                            logger.info(JSONUtility.parseJson(createResponse, "$.body.errors").toString());
                    }
                } else {
                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        csAssert.assertTrue(false, "Governance Body Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Governance Body Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Governance Body" + e.getMessage());
        } finally {
            if (deleteEntity && gbId != -1) {
                logger.info("Deleting Governance Body having Id {}", gbId);
                EntityOperationsHelper.deleteEntityRecord("governance body", gbId);
            }
        }

        csAssert.assertAll();
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
          expectedListingData.put("patterndate", calculateValueForDateField(expectedListingData.get("patterndate")));
          expectedListingData.put("effectivedate", calculateValueForDateField(expectedListingData.get("effectivedate")));
          expectedListingData.put("startdate", calculateValueForDateField(expectedListingData.get("startdate")));
          expectedListingData.put("datemodified", calculateValueForDateField(expectedListingData.get("datemodified")));
          expectedListingData.put("lastmodifiedby", "");
          expectedListingData.put("enddate", calculateValueForDateField(expectedListingData.get("enddate")));
          expectedListingData.put("datecreated",calculateValueForDateField(expectedListingData.get("datecreated")));

          for (Map.Entry<String, String> values : expectedListingData.entrySet()) {
              String expectedKey = values.getKey();
              String expectedValue = values.getValue();
              String actualValue = actualListingData.get(expectedKey);
              customAssert.assertTrue(actualValue.contains(expectedValue), "Expected and Actual Value Are Different on Listing page :: " + expectedValue + " :: " + actualValue);
          }
      }
      catch (Exception e) {
          customAssert.assertTrue(false,"Exception while verifying GB data on listing page"+e.getMessage());
      }
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
                        if (key.equalsIgnoreCase("startDate") || key.equalsIgnoreCase("expDate") || key.equalsIgnoreCase("effectiveDate") || key.equalsIgnoreCase("patternDate"))
                            expectedStringValue = calculateValueForDateField(expectedStringValue);
                        if (!jsonObject.getJSONObject(key).getString("values").equalsIgnoreCase(expectedStringValue)) {
                            logger.error("actual value of " + key + " is " + jsonObject.getJSONObject(key).getString("values") + "and Expected Value of " + key + " is " + expectedStringValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On GB Show Page ::" + jsonObject.getJSONObject(key).getString("values") + "::" + expectedStringValue);
                        }
                    } else if (actualValue instanceof JSONArray) {
                        List<String> expectedValueArray = new ArrayList<>(Arrays.asList(expectedShowPageData.get(key).split(",")));
                        JSONArray actualValueArray = jsonObject.getJSONObject(key).getJSONArray("values");
                        customAssert.assertTrue(expectedValueArray.size() == actualValueArray.length(), "actual size of " + key + " is " + actualValueArray.length() + "and Expected size of " + key + " is " + expectedValueArray.size() + " on show page ");
                        for (int i = 0; i < actualValueArray.length(); i++) {
                            if (!expectedValueArray.contains(actualValueArray.getJSONObject(i).getString("name"))) {
                                logger.error("actual value of " + key + " is " + actualValueArray.getJSONObject(i).getString("name") + "and Expected Value of " + key + " is " + expectedValueArray + " on show page ");
                                customAssert.assertTrue(false, "Actual And Expected Value are Different On GB Show Page");
                            }
                        }
                    } else if (actualValue instanceof JSONObject) {
                        String expectedValueObject = expectedShowPageData.get(key);
                        if (!expectedValueObject.equalsIgnoreCase(jsonObject.getJSONObject(key).getJSONObject("values").getString("name"))) {
                            logger.error("actual value of " + key + " is " + jsonObject.getJSONObject(key).getJSONObject("values").getString("name") + "and Expected Value of " + key + " is " + expectedValueObject + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On GB Show Page ::" + jsonObject.getJSONObject(key).getJSONObject("values").getString("name") + "::" + expectedValueObject);

                        }
                    } else if (actualValue instanceof Boolean) {
                        String expectedValueBoolean = expectedShowPageData.get(key);
                        if (expectedValueBoolean.equalsIgnoreCase("true")) {
                            customAssert.assertTrue(jsonObject.getJSONObject(key).getBoolean("values"), "Actual And Expected Value are Different On GB Show Page ::false :: true");
                        } else {
                            customAssert.assertTrue(!jsonObject.getJSONObject(key).getBoolean("values"), "Actual And Expected Value are Different On GB Show Page ::true :: false");
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
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On GB Show Page ::" + jsonObject.getJSONObject(dynamicKey).getString("values") + "::" + dynamicValue);
                        }
                    } else if (actualValue instanceof Integer) {
                        if (!dynamicValue.contains(String.valueOf(jsonObject.getJSONObject(dynamicKey).getInt("values")))) {
                            logger.error("actual value of " + dynamicKey + " is " + jsonObject.getJSONObject(dynamicKey).getInt("values") + "and Expected Value of " + dynamicKey + " is " + dynamicValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On GB Show Page ::" + jsonObject.getJSONObject(dynamicKey).getInt("values") + "::" + dynamicValue);
                        }
                    } else if (actualValue instanceof JSONArray) {
                        List<String> expectedValueArray = new ArrayList<>(Arrays.asList(dynamicValue.split(",")));
                        JSONArray actualValueArray = jsonObject.getJSONObject(dynamicKey).getJSONArray("values");
                        customAssert.assertTrue(expectedValueArray.size() == actualValueArray.length(), "actual size of " + dynamicKey + " is " + actualValueArray.length() + "and Expected size of " + dynamicKey + " is " + expectedValueArray.size() + " on show page ");
                        for (int j = 0; j < actualValueArray.length(); j++) {
                            if (!expectedValueArray.contains(actualValueArray.getJSONObject(j).getString("name"))) {
                                logger.error("actual value of " + dynamicKey + " is " + actualValueArray.getJSONObject(j).getString("name") + "and Expected Value of " + dynamicKey + " is " + expectedValueArray + " on show page ");
                                customAssert.assertTrue(false, "Actual And Expected Value are Different On GB Show Page");
                            }
                        }
                    } else if (actualValue instanceof JSONObject) {
                        if (!dynamicValue.equalsIgnoreCase(jsonObject.getJSONObject(dynamicKey).getJSONObject("values").getString("name"))) {
                            logger.error("actual value of " + dynamicKey + " is " + jsonObject.getJSONObject(dynamicKey).getJSONObject("values").getString("name") + "and Expected Value of " + dynamicKey + " is " + dynamicValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On GB Show Page ::" + jsonObject.getJSONObject(dynamicKey).getJSONObject("values").getString("name") + "::" + dynamicValue);

                        }
                    } else if (actualValue instanceof Boolean) {
                        if (dynamicValue.equalsIgnoreCase("true")) {
                            customAssert.assertTrue(jsonObject.getJSONObject(dynamicKey).getBoolean("values"), "Actual And Expected Value are Different On GB Show Page ::false :: true");
                        } else {
                            customAssert.assertTrue(!jsonObject.getJSONObject(dynamicKey).getBoolean("values"), "Actual And Expected Value are Different On GB Show Page ::true :: false");
                        }
                    }
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Governance Body Show Page Dynamic Filed" + e.getMessage());
        }
        customAssert.assertAll();
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
