package com.sirionlabs.test.SL;

import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;

public class TestRawDataFilters {


    private String slConfigFilePath;
    private String slConfigFileName;

    @BeforeClass
    public void BeforeClass(){

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");


    }

//    C10722 C10724 C10726 C10664 C10674 C10752
    @Test
    public void TestRawDataFilter(){

        CustomAssert customAssert = new CustomAssert();

        try {
            String cslId = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "csl id");

            String incidentIdString = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "incident id string");
            String incidentIdValue = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "incident id");

            String serviceModuleNameString = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "service module name string");
            String serviceModuleNameValue = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "service module name value");
            String serviceModuleNameValueRawDataTab = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "service module name value raw data tab");

            String exceptionString = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "exception string");
            String exceptionValue = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "exception value");
            String exceptionValueRawDataTab = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "exception value raw data tab");

            String duplicateString = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "duplicate string");
            String duplicateValue = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "duplicate value");

            String sirionFunctionString = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "sirionfunction");
            String sirionFunctionValue = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "sirionfunctionvalue");

            String openTimeString = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "open time string");
            String openValueRawDataTab = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "open time value raw data tab");

            String activeValueRawDataTab = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "raw data tab filter scenarios", "active value raw data tab");

            String payload = "{\"offset\":0,\"size\":1,\"childSlaId\":" + cslId + ",\"" +
                    "filterQuery\":\"{\\\"query\\\": {\\\"bool\\\":{\\\"filter\\\":[{\\\"bool\\\": {\\\"must\\\": [" +
                    "{\\\"range\\\": {\\\"" + openTimeString + "\\\": {\\\"gte\\\": \\\"01-02-2018\\\",\\\"lte\\\": \\\"04-01-2020\\\"}}}," +
                    "{\\\"term\\\": {\\\"" + incidentIdString + "\\\": 11}}," +
                    "{\\\"query_string\\\": { \\\"default_field\\\": \\\"" + serviceModuleNameString + "\\\",\\\"query\\\": \\\"" + serviceModuleNameValue + "\\\"}}," +
                    "{\\\"term\\\": {\\\"" + duplicateString + "\\\": \\\"" + duplicateValue + "\\\"}}," +
                    "{\\\"term\\\\{\\\"" + exceptionString + "\\\": \\\"" + exceptionValue + "\\\"}}," +
                    "{\\\"term\\\":{\\\"useInComputation\\\": \\\"false\\\"}}," +
                    "{\\\"range\\\":{\\\"" + sirionFunctionString + "\\\":{\\\"gte\\\": " + sirionFunctionValue + "}}}]}}]}}}\"}";


            SLDetails sldetails = new SLDetails();

            sldetails.hitSLDetailsGlobalList(payload);

            String slDetailsResponse = sldetails.getSLDetailsResponseStr();

            JSONObject slDetailsResponseJson = new JSONObject(slDetailsResponse);
            JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");

            if (dataArray.length() == 0) {
                customAssert.assertTrue(false, "No Record found after Filtering Records on Raw Data Tab");
            }

            String columnName;
            String columnValue;
            for (int i = 0; i < dataArray.length(); i++) {

                JSONArray indRowArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));

                for (int j = 0; j < indRowArray.length(); j++) {

                    columnName = indRowArray.getJSONObject(j).get("columnName").toString();
                    columnValue = indRowArray.getJSONObject(j).get("columnValue").toString();

                    if (columnName.equals("Exception")) {

                        if (!exceptionValueRawDataTab.equalsIgnoreCase(columnValue)) {
                            customAssert.assertTrue(false, "Exception Values Expected and Actual didn't match");
                        }
                    }

                    if (columnName.equals("Active")) {

                        if (!activeValueRawDataTab.equalsIgnoreCase(columnValue)) {
                            customAssert.assertTrue(false, "Active Values Expected and Actual didn't match");
                        }
                    }

                    if (columnName.equals("Service Module Name")) {

                        if (!columnValue.contains(serviceModuleNameValueRawDataTab)) {
                            customAssert.assertTrue(false, "Service Module Name Expected and Actual didn't match");
                        }
                    }

                    if (columnName.equals("Incident ID")) {

                        if (!columnValue.equalsIgnoreCase(incidentIdValue)) {
                            customAssert.assertTrue(false, "Incident Id Expected and Actual didn't match");
                        }

                    }

                    if (columnName.equals("Open Time")) {

                        if (!columnValue.equalsIgnoreCase(openValueRawDataTab)) {
                            customAssert.assertTrue(false, "Open Time Expected and Actual didn't match");
                        }

                    }

                    if (columnName.equals("Time Taken (Seconds)")) {

                        if (!columnValue.equalsIgnoreCase(sirionFunctionValue)) {
                            customAssert.assertTrue(false, "Time Taken (Seconds) Expected and Actual didn't match");
                        }

                    }
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }
    }


}
