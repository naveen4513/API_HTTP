package com.sirionlabs.helper.ListRenderer;

import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TabListDataHelper {

    private final static Logger logger = LoggerFactory.getLogger(TabListDataHelper.class);
    private static String tabListDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath");
    private static String tabListDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName");
    private static TabListData tabListDataObj = new TabListData();

    public static Boolean verifyServiceDataChargesStartDate(String tabListDataResponse, String expectedStartDate, int serviceDataSeqNo) {
        return verifyServiceDataChargesStartDate(tabListDataResponse, expectedStartDate, "MM-dd-yyyy", serviceDataSeqNo);
    }

    public static Boolean verifyServiceDataChargesStartDate(String tabListDataResponse, String expectedStartDate, String expectedDateFormat, int serviceDataSeqNo) {
        boolean startDateMatched = false;

        try {
            String columnQueryName = getServiceDataChargesTabListDataColumnQueryName("start date");
            String actualStartDate = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            Date actualDate = new SimpleDateFormat(expectedDateFormat).parse(actualStartDate);
            Date expectedDate = new SimpleDateFormat(expectedDateFormat).parse(expectedStartDate);

            logger.info("Actual Start Date: {} and Expected Start Date: {}", actualDate.toString(), expectedStartDate);
            startDateMatched = DateUtils.isSameDay(expectedDate, actualDate);
        } catch (Exception e) {
            logger.error("Exception while verifying Start Date. {}", e.getMessage());
        }
        return startDateMatched;
    }

    public static Boolean verifyServiceDataChargesEndDate(String tabListDataResponse, String expectedEndDate, int serviceDataSeqNo) {
        return verifyServiceDataChargesEndDate(tabListDataResponse, expectedEndDate, "MM-dd-yyyy", serviceDataSeqNo);
    }

    public static Boolean verifyServiceDataChargesEndDate(String tabListDataResponse, String expectedEndDate, String expectedDateFormat, int serviceDataSeqNo) {
        boolean endDateMatched = false;

        try {
            String columnQueryName = getServiceDataChargesTabListDataColumnQueryName("end date");
            String actualEndDate = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            Date actualDate = new SimpleDateFormat(expectedDateFormat).parse(actualEndDate);
            Date expectedDate = new SimpleDateFormat(expectedDateFormat).parse(expectedEndDate);

            logger.info("Actual End Date: {} and Expected End Date: {}", actualDate.toString(), expectedEndDate);
            endDateMatched = DateUtils.isSameDay(expectedDate, actualDate);
        } catch (Exception e) {
            logger.error("Exception while verifying End Date. {}", e.getMessage());
        }
        return endDateMatched;
    }

    public static Boolean verifyServiceDataChargesUnitType(String tabListDataResponse, String expectedValue, int serviceDataSeqNo) {
        boolean unitTypeMatched = false;

        try {
            String columnQueryName = getServiceDataChargesTabListDataColumnQueryName("unit");
            String actualUnitValue = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            actualUnitValue = actualUnitValue == null ? "null" : actualUnitValue;
            expectedValue = expectedValue == null ? "null" : expectedValue;

            logger.info("Actual Unit Type: {} and Expected Unit Type: {}", actualUnitValue, expectedValue);
            unitTypeMatched = actualUnitValue.trim().equalsIgnoreCase(expectedValue.trim());
        } catch (Exception e) {
            logger.error("Exception while verifying Unit Type. {}", e.getMessage());
        }
        return unitTypeMatched;
    }

    public static Boolean verifyServiceDataChargesCurrency(String tabListDataResponse, String expectedCurrencyShortCode, int serviceDataSeqNo) {
        boolean currencyMatched = false;

        try {
            String columnQueryName = getServiceDataChargesTabListDataColumnQueryName("currency");
            String actualCurrencyShortCode = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            actualCurrencyShortCode = actualCurrencyShortCode == null ? "null" : actualCurrencyShortCode;
            expectedCurrencyShortCode = expectedCurrencyShortCode == null ? "null" : expectedCurrencyShortCode;

            logger.info("Actual Currency Short Code: {} and Expected Currency Short Code: {}", actualCurrencyShortCode, expectedCurrencyShortCode);
//            currencyMatched = actualCurrencyShortCode.trim().equalsIgnoreCase(expectedCurrencyShortCode.trim());
            currencyMatched = actualCurrencyShortCode.trim().contains(expectedCurrencyShortCode.trim());
        } catch (Exception e) {
            logger.error("Exception while verifying Currency. {}", e.getMessage());
        }
        return currencyMatched;
    }

    public static Boolean verifyServiceDataChargesVolume(String tabListDataResponse, Double expectedVolume, int serviceDataSeqNo) {
        boolean volumeMatched = false;

        try {
            String columnQueryName = getServiceDataChargesTabListDataColumnQueryName("volume");
            String actualVolumeStr = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            if (actualVolumeStr != null) {
                Double actualVolume = Double.parseDouble(actualVolumeStr);

                logger.info("Actual Volume: {} and Expected Volume: {}", actualVolume, expectedVolume);
                volumeMatched = actualVolume.equals(expectedVolume);
            } else {
                logger.error("Volume in TabListData API Response is Null.");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Volume. {}", e.getMessage());
        }
        return volumeMatched;
    }

    public static Boolean verifyServiceDataChargesRate(String tabListDataResponse, Double expectedRate, int serviceDataSeqNo) {
        boolean rateMatched = false;

        try {
            String columnQueryName = getServiceDataChargesTabListDataColumnQueryName("rate");
            String actualRateStr = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            if (actualRateStr != null) {
                Double actualRate = Double.parseDouble(actualRateStr);

                logger.info("Actual Rate: {} and Expected Rate: {}", actualRate, expectedRate);
                rateMatched = actualRate.equals(expectedRate);
            } else {
                logger.error("Rate in TabListData API Response is Null.");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Rate. {}", e.getMessage());
        }
        return rateMatched;
    }

    public static Boolean verifyServiceDataChargesBaseAmount(String tabListDataResponse, Double expectedBaseAmount, int serviceDataSeqNo) {
        boolean baseAmountMatched = false;

        try {
            String columnQueryName = getServiceDataChargesTabListDataColumnQueryName("base amount");
            String actualBaseAmountStr = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            if (actualBaseAmountStr != null) {
                Double actualBaseAmount = Double.parseDouble(actualBaseAmountStr);

                logger.info("Actual Base Amount: {} and Expected Base Amount: {}", actualBaseAmount, expectedBaseAmount);
                baseAmountMatched = actualBaseAmount.equals(expectedBaseAmount);

                if (!baseAmountMatched) {
                    //Checking with converting Double into Long.
                    baseAmountMatched = (actualBaseAmount.longValue() == expectedBaseAmount.longValue());
                }
            } else {
                logger.error("Base Amount in TabListData API Response is Null.");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Base Amount. {}", e.getMessage());
        }
        return baseAmountMatched;
    }

    public static Boolean verifyContractPriceBookUnitType(String tabListDataResponse, String expectedValue, int serviceDataSeqNo) {
        boolean unitTypeMatched = false;

        try {
            String columnQueryName = getContractPriceBookTabListDataColumnQueryName("unit type");
            String actualUnitValue = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            actualUnitValue = actualUnitValue == null ? "null" : actualUnitValue;
            expectedValue = expectedValue == null ? "null" : expectedValue;

            logger.info("Actual Unit Type: {} and Expected Unit Type: {}", actualUnitValue, expectedValue);
            unitTypeMatched = actualUnitValue.trim().equalsIgnoreCase(expectedValue.trim());
        } catch (Exception e) {
            logger.error("Exception while verifying Unit Type. {}", e.getMessage());
        }
        return unitTypeMatched;
    }

    public static Boolean verifyContractPriceBookServiceData(String tabListDataResponse, String expectedServiceDataName, Integer expectedServiceDataId, int serviceDataSeqNo) {
        boolean serviceDataMatched = false;

        try {
            String columnQueryName = getContractPriceBookTabListDataColumnQueryName("service data");
            String actualValue = getActualValue(tabListDataResponse, columnQueryName, serviceDataSeqNo);

            actualValue = actualValue == null ? "null" : actualValue;

            logger.info("Actual Service Data Value: {}", actualValue);
            logger.info("Expected Service Data Name: {}", expectedServiceDataName);
            logger.info("Expected Service Data Id: {}", expectedServiceDataId);

            if (!actualValue.trim().toLowerCase().contains(expectedServiceDataName.trim().toLowerCase())) {
                logger.error("Service Data Field validation failed for Service Data Name.");
            } else if (!actualValue.trim().toLowerCase().contains(expectedServiceDataId.toString().trim())) {
                logger.error("Service Data Field validation failed for Service Data Id.");
            } else
                serviceDataMatched = true;
        } catch (Exception e) {
            logger.error("Exception while verifying Service Data. {}", e.getMessage());
        }
        return serviceDataMatched;
    }

    public static Boolean verifyContractPriceBookPricingData(String tabListDataResponse, List<Map<String, String>> expectedValues, int serviceDataSeqNo) {
        boolean pricingDataMatched = false;

        try {
            String columnQueryName = getContractPriceBookTabListDataColumnQueryName("pricing data");

            if (columnQueryName != null) {
                String pricingDataColumnId = getColumnIdFromColumnName(tabListDataResponse, columnQueryName);

                if (pricingDataColumnId != null) {
                    if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                        JSONObject jsonObj = new JSONObject(tabListDataResponse);
                        JSONArray dataArr = jsonObj.getJSONArray("data");

                        if (dataArr.length() > serviceDataSeqNo) {
                            jsonObj = dataArr.getJSONObject(serviceDataSeqNo);
                            jsonObj = jsonObj.getJSONObject(pricingDataColumnId).getJSONObject("value");

                            JSONArray pricingDataArr = jsonObj.getJSONArray("data");

                            for (int i = 0; i < expectedValues.size(); i++) {
                                JSONObject dataJsonObj = pricingDataArr.getJSONObject(i);

								/*
								It is expected that the Expected Values List will have Map of Fields like Volume and its Value, monthDate and its value and so on.
								Also it is expected that the List will have the Map in sequence as it should appear in API Response.
								As of now we are validating only Volume Column But are passing Map<String, String> so that in future can validate monthDate, endDate,
								startDate as well without changing the method signature.
								 */

                                if (expectedValues.get(i).get("volume") != null && !expectedValues.get(i).get("volume").trim().equalsIgnoreCase("")) {
                                    Long expectedVolume = new Double(Double.parseDouble(expectedValues.get(i).get("volume"))).longValue();
                                    Double dataVolume = dataJsonObj.getDouble("volume");
                                    Long actualVolume = dataVolume.longValue();

                                    logger.info("Expected Pricing Value: {} and Actual Pricing Value: {}", expectedVolume, actualVolume);
                                    pricingDataMatched = actualVolume.equals(expectedVolume);
                                }

                                if (!pricingDataMatched)
                                    return false;
                            }
                        } else {
                            logger.error("Couldn't find Service Data Sequence No. {} in TabListDataResponse", serviceDataSeqNo);
                            return false;
                        }
                    } else {
                        logger.error("TabListData API Response is an Invalid JSON.");
                        return false;
                    }
                } else {
                    logger.error("Couldn't find Id for Column {}.", columnQueryName);
                    return false;
                }
            } else {
                logger.error("ColumnQueryName value is Null.");
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Pricing Data. {}", e.getMessage());
        }
        return pricingDataMatched;
    }

    private static String getServiceDataChargesTabListDataColumnQueryName(String columnName) {
        String columnQueryName = null;

        try {
            columnQueryName = ParseConfigFile.getValueFromConfigFile(tabListDataConfigFilePath, tabListDataConfigFileName,
                    "service data charges tab column name mapping", columnName);
        } catch (Exception e) {
            logger.error("Exception while getting Query Name for Column {}. {}", columnName, e.getStackTrace());
        }
        return columnQueryName;
    }

    private static String getContractPriceBookTabListDataColumnQueryName(String columnName) {
        String columnQueryName = null;

        try {
            columnQueryName = ParseConfigFile.getValueFromConfigFile(tabListDataConfigFilePath, tabListDataConfigFileName,
                    "contracts price book tab column name mapping", columnName);
        } catch (Exception e) {
            logger.error("Exception while getting Query Name for Column {}. {}", columnName, e.getStackTrace());
        }
        return columnQueryName;
    }

    public static String getActualValue(String tabListDataResponse, String columnQueryName, int serviceDataNo) {
        try {
            if (columnQueryName != null) {
                if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                    JSONObject jsonObj = new JSONObject(tabListDataResponse);
                    JSONArray dataArr = jsonObj.getJSONArray("data");

                    if (dataArr.length() > serviceDataNo) {
                        jsonObj = dataArr.getJSONObject(serviceDataNo);
                        JSONArray jsonNamesArr = jsonObj.names();

                        for (Object objectName : jsonNamesArr) {
                            if (jsonObj.getJSONObject(objectName.toString().trim()).getString("columnName").trim().equalsIgnoreCase(columnQueryName.trim())) {
                                return jsonObj.getJSONObject(objectName.toString().trim()).getString("value");
                            }
                        }

                        logger.error("Couldn't find Value for ColumnQueryName {} in Service Data Sequence No. {}", columnQueryName, serviceDataNo);
                        return null;
                    } else {
                        logger.error("Couldn't find Service Data Sequence No. {} in TabListDataResponse", serviceDataNo);
                        return null;
                    }
                } else {
                    logger.error("TabListData API Response is an Invalid JSON.");
                    return null;
                }
            } else {
                logger.error("ColumnQueryName value is Null.");
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception while getting Actual Value of Column Query Name {}. {}", columnQueryName, e.getStackTrace());
            return null;
        }
    }

    public static int getServiceDataSeqNo(String tabListDataResponse, int serviceDataId) {
        try {
            String pivotColumnName = getContractPriceBookTabListDataColumnQueryName("service data");

            JSONObject jsonObj = new JSONObject(tabListDataResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            String pivotalColumnId = getColumnIdFromColumnName(tabListDataResponse, pivotColumnName);
            if (pivotalColumnId != null) {
                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject serviceDataObj = jsonArr.getJSONObject(i);

                    if (serviceDataObj.getJSONObject(pivotalColumnId).getString("value").contains(":;" + serviceDataId))
                        return i;
                }
            } else {
                logger.error("Couldn't find Id for Column {}. Hence couldn't get Seq No for Service Data {}", pivotColumnName, serviceDataId);
            }
            return -1;
        } catch (Exception e) {
            logger.error("Exception while getting Seq No for Service Data Id {}. {}", serviceDataId, e.getStackTrace());
            return -1;
        }
    }

    public static String getColumnIdFromColumnName(String tabListDataResponse, String columnName) {
        try {
            JSONObject jsonObj = new JSONObject(tabListDataResponse);

            if (jsonObj.getJSONArray("data").length() > 0) {
                jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);
                JSONArray jsonArr = jsonObj.names();

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject columnObj = jsonObj.getJSONObject(jsonArr.get(i).toString());
                    if (columnObj.getString("columnName").trim().equalsIgnoreCase(columnName.trim())) {
                        return jsonArr.get(i).toString();
                    }
                }
            } else {
                logger.error("Couldn't find any data in TabListData Response.");
            }
            return null;
        } catch (Exception e) {
            logger.error("Exception while getting Id for Column {}. {}", columnName, e.getStackTrace());
            return null;
        }
    }

    public static Integer getIdForTab(String tabName) {
        try {
            return Integer.parseInt(ParseConfigFile.getValueFromConfigFile(tabListDataConfigFilePath, tabListDataConfigFileName, "tabs mapping", tabName));
        } catch (Exception e) {
            logger.error("Exception while getting Id for Tab {}. {}", tabName, e.getStackTrace());
            return -1;
        }
    }

    public static Long getHistoryIdFromValue(String historyValue) {
        long historyId = -1L;

        try {
            String temp = historyValue.substring(0, historyValue.lastIndexOf("/"));
            temp = temp.substring(temp.lastIndexOf("/") + 1).trim();

            historyId = Long.parseLong(temp);
        } catch (Exception e) {
            logger.error("Exception while getting Id from History Value. {}", e.getMessage());
        }
        return historyId;
    }

    // this function will return the total count of records in tab list Page
    public static int getFilteredCount(String response) {

        int noOfRecords = -1;
        JSONObject listDataResponseObj = new JSONObject(response);
        if (listDataResponseObj.has("filteredCount"))
            noOfRecords = listDataResponseObj.getInt("filteredCount");
        else
            logger.error("response don't have filteredCount key in it");

        return noOfRecords;
    }

    public static Long getCommunicationIdFromValue(String value) {
        try {
            String[] temp = value.split(Pattern.quote(":;"));
            return Long.parseLong(temp[2]);
        } catch (Exception e) {
            logger.error("Couldn't get Communication Id from Value: {}", value);
            return null;
        }
    }

    public static String getDefaultTabListDataPayload(int entityTypeId) {
        return getDefaultTabListDataPayload(entityTypeId, 20, 0);
    }

    public static String getDefaultTabListDataPayload(int entityTypeId, int size, int offset) {
        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size +
                ",\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
    }

    public static String hitTabListDataAPIForCDRContractDocumentTab(Integer recordId) {
        return hitTabListDataAPIForCDRContractDocumentTab(recordId, null);
    }

    public static String hitTabListDataAPIForCDRContractDocumentTab(Integer recordId, String payload) {
        if (payload == null)
            payload = getDefaultTabListDataPayload(160);

        TabListData tabListObj = new TabListData();
        return tabListObj.hitTabListData(367, 160, recordId, payload);
    }

    public static Long getDocumentFileIdFromDocumentNameValue(String documentNameValue, String fileName) {
        try {
            String[] levelOne = documentNameValue.split(Pattern.quote("###"));

            if (fileName.contains(" (Version")) {
                String[] fileNameArr = fileName.split(Pattern.quote("("));
                fileName = fileNameArr[0].trim();
            }

            for (String oneFileStr : levelOne) {
                if (oneFileStr.contains(fileName)) {
                    String[] levelTwo = oneFileStr.trim().split(Pattern.quote(":;"));
                    return Long.parseLong(levelTwo[levelTwo.length - 1]);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Document File Id from Document Name Value: {}", documentNameValue);
        }

        return null;
    }

    public static String hitTabListDataAPIForCDRCommunicationTab(Integer recordId) {
        return hitTabListDataAPIForCDRCommunicationTab(recordId, null);
    }

    public static String hitTabListDataAPIForCDRCommunicationTab(Integer recordId, String payload) {
        if (payload == null)
            payload = getDefaultTabListDataPayload(160);

        TabListData tabListObj = new TabListData();
        return tabListObj.hitTabListData(65, 160, recordId, payload);
    }

    public static String hitTabListDataAPIForClauseForwardReferenceTab(int entityTypeId, Integer recordId, String payload) {
        if (payload == null)
            payload = getDefaultTabListDataPayload(entityTypeId);

        TabListData tabListObj = new TabListData();
        return tabListObj.hitTabListData(306, entityTypeId, recordId, payload);
    }

    public static String hitTabListDataAPIForAuditLogTab(Integer entityTypeId, Integer recordId, String payload) {
        Integer tabId = getIdForTab("audit log");

        return getTabListDataResponse(entityTypeId, recordId, tabId, payload);
    }

    public static String getTabListDataResponse(Integer entityTypeId, Integer recordId, Integer tabId) {
        return getTabListDataResponse(entityTypeId, recordId, tabId, null);
    }

    public static String getTabListDataResponse(Integer entityTypeId, Integer recordId, Integer tabId, String payload) {
        if (payload == null) {
            payload = getDefaultTabListDataPayload(entityTypeId);
        }

        return tabListDataObj.hitTabListData(tabId, entityTypeId, recordId, payload);
    }

    public static String getExpectedAuditLogActionName(String actionId, int entityTypeId) {
        String expectedActionName = null;

        switch (entityTypeId) {
            case 328:
            case 329:
                expectedActionName = (actionId.equalsIgnoreCase("1")) ? "Saved" : "Updated";
                break;
        }

        return expectedActionName;
    }

    public void validateSourceReferenceTab(String entityName, int entityTypeId, int recordId, int parentEntityTypeId, int parentRecordId, CustomAssert csAssert) {
        try {
            if (tabListDataConfigFilePath == null) {
                tabListDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath");
                tabListDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName");
            }

            int sourceReferenceTabId = TabListDataHelper.getIdForTab("source reference");

            if (sourceReferenceTabId == -1) {
                csAssert.assertTrue(false, "Couldn't get Id of Source Reference Tab.");
                return;
            }

            String tabListResponse = TabListDataHelper.getTabListDataResponse(entityTypeId, recordId, sourceReferenceTabId);

            String parentShowResponse = null;

            //Special Handling for GB Meeting
            if (parentEntityTypeId == 87) {
                parentEntityTypeId = 1;

                String entityShowResponse = ShowHelper.getShowResponseVersion2(entityTypeId, recordId);
                String supplierRecordId = ShowHelper.getActualValue(entityShowResponse, ShowHelper.getShowFieldHierarchy("supplierid", entityTypeId));

                if (supplierRecordId == null) {
                    csAssert.assertTrue(false, "Couldn't get Supplier Record Id from Show Response of Entity " + entityName);
                    return;
                }

                parentRecordId = Integer.parseInt(supplierRecordId);
                parentShowResponse = ShowHelper.getShowResponseVersion2(1, parentRecordId);
            }

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject jsonObj = new JSONObject(tabListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                boolean recordFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    //Validate Source Id
                    String idColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "id");
                    String[] idValueArr = jsonObj.getJSONObject(idColumn).getString("value").split(":;");

                    if (idValueArr[2].equalsIgnoreCase(String.valueOf(parentEntityTypeId)) && idValueArr[1].equalsIgnoreCase(String.valueOf(parentRecordId))) {
                        recordFound = true;

                        //Validate Source Name
                        if (parentShowResponse == null) {
                            parentShowResponse = ShowHelper.getShowResponseVersion2(parentEntityTypeId, parentRecordId);
                        }

                        String showFieldName = "title";
                        if (parentEntityTypeId == 61) {
                            showFieldName = "name";
                        }

                        String expectedNameValue = ShowHelper.getActualValue(parentShowResponse, ShowHelper.getShowFieldHierarchy(showFieldName, parentEntityTypeId));

                        if (expectedNameValue == null) {
                            expectedNameValue = ShowHelper.getActualValue(parentShowResponse, ShowHelper.getShowFieldHierarchy("name", parentEntityTypeId));
                        }

                        String nameColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "name");
                        String actualNameValue = null;

                        if (!jsonObj.getJSONObject(nameColumn).isNull("value")) {
                            actualNameValue = jsonObj.getJSONObject(nameColumn).getString("value");
                        }

                        if (expectedNameValue == null) {
                            csAssert.assertTrue(actualNameValue == null, "Expected Name: null and Actual Name: " + actualNameValue +
                                    ". Source Reference Tab of Entity " + entityName + " and Record Id " + recordId);
                        } else if (actualNameValue != null) {
                            csAssert.assertTrue(expectedNameValue.equalsIgnoreCase(actualNameValue), "Expected Name: " + expectedNameValue + " and Actual Name: " +
                                    actualNameValue);
                        } else {
                            csAssert.assertTrue(false, "Couldn't Validate Name Value in Source Reference Tab of Entity " + entityName + " and Record Id " +
                                    recordId);
                        }

                        //Validate Status
                        String statusColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "status");
                        String actualStatusValue = jsonObj.getJSONObject(statusColumn).getString("value");

                        String statusShowPageObjectName;

                        //Special handling for CSL
                        if (parentEntityTypeId == 15) {
                            statusShowPageObjectName = "performancestatus";
                        } else {
                            statusShowPageObjectName = "status";
                        }

                        String expectedStatusValue = ShowHelper.getActualValue(parentShowResponse, ShowHelper.getShowFieldHierarchy(statusShowPageObjectName,
                                parentEntityTypeId));

                        if (actualStatusValue != null && expectedStatusValue != null) {
                            csAssert.assertTrue(expectedStatusValue.equalsIgnoreCase(actualStatusValue), "Expected Status: " + expectedStatusValue +
                                    " and Actual Status: " + actualStatusValue + ". Source Reference Tab of Entity " + entityName + " and Record Id " + recordId);
                        } else {
                            csAssert.assertTrue(false, "Couldn't Validate Status Value in Source Reference Tab of Entity " + entityName +
                                    " and Record Id " + recordId);
                        }

                        break;
                    }
                }

                if (!recordFound) {
                    csAssert.assertFalse(true, "Expected Parent Entity Type Id " + parentEntityTypeId + " and Record Id " + parentRecordId +
                            " not found in Source Reference Tab of Entity " + entityName + " and Record Id " + recordId);
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Source Reference Tab of Entity " + entityName + " and Record Id " +
                        recordId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Source Reference Tab for Entity " + entityName + " and Record Id " + recordId);
        }
    }

    public void validateForwardReferenceTab(String entityName, int entityTypeId, int recordId, int childEntityTypeId, int childRecordId, CustomAssert csAssert) {
        try {
            //Forward Reference Tab is not present for following entities.
            if (entityTypeId == 1 || entityTypeId == 61 || entityTypeId == 87) {
                return;
            }

            if (tabListDataConfigFilePath == null) {
                tabListDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath");
                tabListDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName");
            }

            int forwardReferenceTabId = TabListDataHelper.getIdForTab("forward reference");

            if (forwardReferenceTabId == -1) {
                csAssert.assertTrue(false, "Couldn't get Id of Forward Reference Tab.");
                return;
            }

            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(entityTypeId, recordId, forwardReferenceTabId, payload);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                JSONObject jsonObj = new JSONObject(tabListResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                boolean recordFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    //Validate Child Id
                    String idColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "id");
                    String[] idValueArr = jsonObj.getJSONObject(idColumn).getString("value").split(":;");

                    if (idValueArr[2].equalsIgnoreCase(String.valueOf(childEntityTypeId)) && idValueArr[1].equalsIgnoreCase(String.valueOf(childRecordId))) {
                        recordFound = true;

                        //Validate Source Name
                        String showResponse = ShowHelper.getShowResponseVersion2(childEntityTypeId, childRecordId);
                        String expectedNameValue = ShowHelper.getActualValue(showResponse, ShowHelper.getShowFieldHierarchy("name", childEntityTypeId));

                        String nameColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "name");
                        String actualNameValue = jsonObj.getJSONObject(nameColumn).getString("value");

                        if (actualNameValue != null && expectedNameValue != null) {
                            csAssert.assertTrue(expectedNameValue.equalsIgnoreCase(actualNameValue), "Expected Name: " + expectedNameValue + " and Actual Name: " +
                                    actualNameValue + ". Forward Reference Tab of Entity " + entityName + " and Record Id " + recordId);
                        } else {
                            csAssert.assertTrue(false, "Couldn't Validate Name Value in Forward Reference Tab of Entity " + entityName + " and Record Id " +
                                    recordId);
                        }

                        //Validate Status
                        String statusColumn = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "status");
                        String actualStatusValue = jsonObj.getJSONObject(statusColumn).getString("value");
                        String expectedStatusValue = ShowHelper.getActualValue(showResponse, ShowHelper.getShowFieldHierarchy("status", childEntityTypeId));

                        if (actualStatusValue != null && expectedStatusValue != null) {
                            csAssert.assertTrue(expectedStatusValue.equalsIgnoreCase(actualStatusValue), "Expected Status: " + expectedStatusValue +
                                    " and Actual Status: " + actualStatusValue + ". Forward Reference Tab of Entity " + entityName + " and Record Id " + recordId);
                        } else {
                            csAssert.assertTrue(false, "Couldn't Validate Status Value in Forward Reference Tab of Entity " + entityName +
                                    " and Record Id " + recordId);
                        }

                        break;
                    }
                }

                if (!recordFound) {
                    csAssert.assertFalse(true, "Expected Child Entity Type Id " + childEntityTypeId + " and Child Record Id " + childRecordId +
                            " not found in Forward Reference Tab of Parent Entity " + entityName + " and Record Id " + recordId);
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Forward Reference Tab of Entity " + entityName + " and Record Id " +
                        recordId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Forward Reference Tab for Entity " + entityName + " and Record Id " + recordId);
        }
    }

    public static String hitTabListDataAPIForContractCommunicationTab(Integer recordId, String payload) {
        if (payload == null)
            payload = getDefaultTabListDataPayload(61);
            TabListData tabListObj = new TabListData();
            return tabListObj.hitTabListData(65, 61, recordId, payload);
    }
}
