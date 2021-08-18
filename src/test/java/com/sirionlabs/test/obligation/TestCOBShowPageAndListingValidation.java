package com.sirionlabs.test.obligation;

import com.sirionlabs.api.CreateAdhocChildCOB.CreateAdhocChildCOB;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

public class TestCOBShowPageAndListingValidation {

    private final static Logger logger = LoggerFactory.getLogger(TestCOBShowPageAndListingValidation.class);

    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer obligationEntityTypeId;
    private static Integer obligationEntityId;
    private ListRendererTabListData tabData;
    private int childObligationEntityTypeId;
    private int childObligationId;
    private Map<String, String> expectedShowPageData;
    private Map<String, String> expectedListingData;
    private Show showObj = new Show();
    private int COBListId;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");
        tabData = new ListRendererTabListData();
        childObligationEntityTypeId= ConfigureConstantFields.getEntityIdByName("child obligations");
        COBListId = ConfigureConstantFields.getListIdForEntity("child obligations");

    }

    @Test()
    public void TestObligationChildCreation(){
        String flowToTest = "flow 11";

        CustomAssert csAssert = new CustomAssert();
        obligationEntityId = -1;

        try {
            logger.info("Validating Obligation Creation Flow [{}]", flowToTest);

            //Validate Obligation Creation
            logger.info("Creating Obligation for Flow [{}]", flowToTest);
            String createResponse = Obligations.createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                    true);

            expectedShowPageData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "flow 11 show page");
            expectedListingData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "flow 11 listing page");


            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    obligationEntityId = CreateEntity.getNewEntityId(createResponse, "obligations");
            }

            logger.info("Perform Entity Workflow Action For Created Obligation");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, obligationEntityId, actionLabel);
            }
            String payload="{\"masterId\":"+obligationEntityId+",\"dueDate\":\""+ DateUtils.getCurrentDateInMM_DD_YYYY() +" 00:00:00\"}";
            APIResponse createAdhocChildCOB=new CreateAdhocChildCOB().getCreateAdhocChild(payload);
          if (ParseJsonResponse.validJsonResponse(createAdhocChildCOB.getResponseBody()))
          {
              String responseBody=createAdhocChildCOB.getResponseBody();
               String notificationStr= new org.json.JSONArray(responseBody).getJSONObject(0).getString("errorMessage");
                          String []temp = notificationStr.trim().split(Pattern.quote("show/"));
                          if (temp.length > 1) {
                              String temp2 = temp[1];
                              String []temp3 = temp2.trim().split(Pattern.quote("\""));
                              if (temp3.length > 1) {
                                  String temp4 = temp3[0];
                                  String []temp5 = temp4.trim().split(Pattern.quote("/"));
                                  if (temp5.length > 1)
                                      childObligationId = Integer.parseInt(temp5[1]);
                              }

                          }
              if (childObligationId !=0) {
//                  String shortCodeId = CreateEntity.getShortId(createResponse, "GB");
//                  expectedShowPageData.put("shortCodeId", shortCodeId);
                  expectedListingData.put("id", String.valueOf(childObligationId));
                  logger.info("Contract Created Successfully with Id {}: ", childObligationId);
                  logger.info("Hitting Show API for Contract Id {}", childObligationId);
                  showObj.hitShowVersion2(childObligationEntityTypeId, childObligationId);
                  String showResponse = showObj.getShowJsonStr();
                  if (ParseJsonResponse.validJsonResponse(showResponse)) {
                      if (!showObj.isShowPageAccessible(showResponse)) {
                          csAssert.assertTrue(false, "Show Page is Not Accessible for COB Id " + childObligationId);
                      } else {
                          verifyShowPageData(showResponse, csAssert);
                          ListRendererListData listRendererListData1= new ListRendererListData();
                          listRendererListData1.hitListRendererListData(COBListId);
                          String listDataJsonStr1 = listRendererListData1.getListDataJsonStr();
                          verifyListingPage(listDataJsonStr1, csAssert);
                      }

                  } else {
                      csAssert.assertTrue(false, "Show API Response for COB Id " + childObligationId + " is an Invalid JSON.");
                  }
              }
                      } else {
                          logger.error("COB not created.");
                      }
            logger.info("Deleting Obligation having Id {}", childObligationId);
            EntityOperationsHelper.deleteEntityRecord("child obligations", childObligationId);

        }catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
                EntityOperationsHelper.deleteEntityRecord("obligations", obligationEntityId);
            csAssert.assertAll();
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
                        if (key.contains("Date"))
                            expectedStringValue = calculateValueForDateField(expectedStringValue);
                        if (!jsonObject.getJSONObject(key).getString("values").equalsIgnoreCase(expectedStringValue)) {
                            logger.error("actual value of " + key + " is " + jsonObject.getJSONObject(key).getString("values") + "and Expected Value of " + key + " is " + expectedStringValue + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page ::" + jsonObject.getJSONObject(key).getString("values") + "::" + expectedStringValue);
                        }
                    } else if (actualValue instanceof org.json.JSONArray) {
                        List<String> expectedValueArray = new ArrayList<>(Arrays.asList(expectedShowPageData.get(key).split(",")));
                        org.json.JSONArray actualValueArray = jsonObject.getJSONObject(key).getJSONArray("values");
                        customAssert.assertTrue(expectedValueArray.size() == actualValueArray.length(), "actual size of " + key + " is " + actualValueArray.length() + "and Expected size of " + key + " is " + expectedValueArray.size() + " on show page ");
                        for (int i = 0; i < actualValueArray.length(); i++) {
                            if (!expectedValueArray.contains(actualValueArray.getJSONObject(i).getString("name"))) {
                                logger.error("actual value of " + key + " is " + actualValueArray.getJSONObject(i).getString("name") + "and Expected Value of " + key + " is " + expectedValueArray + " on show page ");
                                customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page ::"+actualValueArray.getJSONObject(i).getString("name")+"::"+expectedValueArray);
                            }
                        }
                    } else if (actualValue instanceof JSONObject) {
                        String expectedValueObject = expectedShowPageData.get(key);
                        if (!expectedValueObject.equalsIgnoreCase(jsonObject.getJSONObject(key).getJSONObject("values").getString("name"))) {
                            logger.error("actual value of " + key + " is " + jsonObject.getJSONObject(key).getJSONObject("values").getString("name") + "and Expected Value of " + key + " is " + expectedValueObject + " on show page ");
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page ::" + jsonObject.getJSONObject(key).getJSONObject("values").getString("name") + "::" + expectedValueObject);

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
            customAssert.assertTrue(false, "Exception while validating COB Show Page" + e.getMessage());
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
                            customAssert.assertTrue(false, "Actual And Expected Value are Different On Show Page ::" + jsonObject.getJSONObject(dynamicKey).getInt("values") + "::" + dynamicValue);
                        }
                    } else if (actualValue instanceof org.json.JSONArray) {
                        List<String> expectedValueArray = new ArrayList<>(Arrays.asList(dynamicValue.split(",")));
                        org.json.JSONArray actualValueArray = jsonObject.getJSONObject(dynamicKey).getJSONArray("values");
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
            customAssert.assertTrue(false, "Exception while validating COB Show Page Dynamic Filed" + e.getMessage());
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
            expectedListingData.put("expdate", calculateValueForDateField(expectedListingData.get("expdate")));
            expectedListingData.put("datecreated", calculateValueForDateField(expectedListingData.get("datecreated")));


            for (Map.Entry<String, String> values : expectedListingData.entrySet()) {
                String expectedKey = values.getKey();
                String expectedValue = values.getValue();
                String actualValue = actualListingData.get(expectedKey);
                customAssert.assertTrue(actualValue.contains(expectedValue), "Expected and Actual Value Are Different on Listing page :: " + expectedValue + " :: " + actualValue);
            }
        }
        catch (Exception e) {
            customAssert.assertTrue(false,"Exception while verifying data on listing page"+e.getMessage());
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

