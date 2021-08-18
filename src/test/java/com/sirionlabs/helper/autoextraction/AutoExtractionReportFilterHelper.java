package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.userGroups.UserGroupsUsers;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.test.autoExtraction.AutoExtractionTestReportListFilters;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import java.io.IOException;
import java.util.*;

public class AutoExtractionReportFilterHelper {
    private final static Logger logger = LoggerFactory.getLogger(AutoExtractionReportFilterHelper.class);
    AutoExtractionHelper autoExtractionHelper = new AutoExtractionHelper();

    public void autoExtractionhitListDataAndVerifyResults(String reportName, Integer reportId, String filterName, String optionName, String payload, String showPageObjectName,
                                                          int entityTypeId, String entityName, CustomAssert csAssert, Boolean applyRandomization, int maxRecordsToValidate) throws IOException {

        logger.info("Hitting Report List Data API for Filter {} and Option {} of Report [{}] having Id {}", filterName, optionName, reportName, reportId);
        ReportRendererListData listDataObj = new ReportRendererListData();
        listDataObj.hitReportRendererListData(reportId, payload);
        AutoExtractionTestReportListFilters.testDownloadReportData(payload,filterName);
        String listDataResponse = listDataObj.getListDataJsonStr();
        try {
            //Verifying ProjectIds,Status and StatusIds filter Via Listing Page
            if (filterName.equalsIgnoreCase("projectids")) {
                logger.info("Validating filter result for filter name " + filterName + " and " + optionName);
                JSONObject listObj = new JSONObject(listDataResponse);
                int totalData = listObj.getJSONArray("data").length();
                int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "projects");
                if (totalData > 0) {
                    for (int i = 0; i < totalData; i++) {
                        String[] projectValue = listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split("::");
                        if (projectValue.length > 1) {
                            List<String> allProjects = new ArrayList<>();
                            for (int j = 0; j < projectValue.length; j++) {
                                String[] projectNames = projectValue[i].trim().split(":;");
                                for (String project : projectNames) {
                                    if (project.equalsIgnoreCase(optionName)) {
                                        allProjects.add(project);
                                        logger.info("Project name " + optionName + " found in Multi Projects Document.");
                                    }
                                }
                            }
                            csAssert.assertEquals(allProjects.get(0), optionName, "List Page validation failed for" +
                                    " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                        } else {
                            String[] Value = listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;");
                            String projectName = Value[0].trim();
                            csAssert.assertEquals(projectName, optionName, "List Page validation failed for" +
                                    " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                        }
                    }
                } else {
                    logger.info("No list data is available to validate for " + filterName + " and " + optionName);
                }

            } else if (filterName.equals("clusters")) {
                int clusterColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "clusters");
                JSONObject listObj = new JSONObject(listDataResponse);
                int totalDocs = listObj.getJSONArray("data").length();
                for (int i = 0; i < totalDocs; i++) {
                    String[] clusterValue = listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(clusterColumnId)).get("value").toString().split(",");
                    if (clusterValue.length > 1) {
                        boolean flag = false;
                        for (int j = 0; j < clusterValue.length; j++) {
                            if (clusterValue[j].trim().equalsIgnoreCase(optionName)) {
                                flag = true;
                                break;
                            }
                        }
                        csAssert.assertTrue(flag, "List Page validation failed for" +
                                " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);


                    } else {
                        csAssert.assertEquals(clusterValue[0].trim(), optionName, "List Page validation failed for" +
                                " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);

                    }
                }
            }     else if (filterName.equalsIgnoreCase("contract")) {
                logger.info("Started the verification for Contract Filter ");
                int contractNameColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "contractname");
                int contractIdColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "contractid");
                List<String> contractName = new LinkedList<>();
                List<String> contractID = new LinkedList<>();
                JSONObject listObj = new JSONObject(listDataResponse);
                int totalData = listObj.getJSONArray("data").length();
                if(totalData > 0) {
                    for (int x = 0; x < totalData; x++) {
                        contractName.add(listObj.getJSONArray("data").getJSONObject(x).getJSONObject(String.valueOf(contractNameColumnId)).get("value").toString().split(":;")[0]);
                        contractID.add(listObj.getJSONArray("data").getJSONObject(x).getJSONObject(String.valueOf(contractIdColumnId)).get("value").toString().split(":;")[1]);
                    }

                    String actualContractOptionValue = optionName;
                    csAssert.assertTrue(contractName.stream().allMatch(actualContractOptionValue::equalsIgnoreCase), "Contract Filter is not working" + "Actual Option Value: " + actualContractOptionValue);
                }
                else {
                    throw new SkipException("Couldn't find the data after applying Contract Filter for Option Name: " +optionName + "Hence skipping test.");
                }
            }


            else if (filterName.equals("doctag1ids") || filterName.equals("doctag2ids") ||
                    filterName.equals("doctagids") || filterName.equals("groupids") || filterName.equals("batchids")) {
                String columnNameToCheck = "";
                if (filterName.equals("doctag1ids")) {
                    columnNameToCheck = "doctag1";
                } else if (filterName.equals("doctag2ids")) {
                    columnNameToCheck = "doctag2";
                } else if (filterName.equals("doctagids")) {
                    columnNameToCheck = "doctags";
                } else if (filterName.equals("groupids")) {
                    columnNameToCheck = "groups";
                } else if (filterName.equals("batchids")) {
                    columnNameToCheck = "batch";
                }
                logger.info("Started Verification for FilterName:" + filterName);
                int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, columnNameToCheck);
                List<String> groupOfTags = new LinkedList<>();
                List<String> labelNamesWithSeparator = new LinkedList<>();
                JSONObject listObj = new JSONObject(listDataResponse);
                int totalDocs = listObj.getJSONArray("data").length();
                for (int i = 0; i < totalDocs; i++) {
                    groupOfTags.add(listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString());
                }
                for (int i = 0; i < groupOfTags.size(); i++) {
                    if (groupOfTags.get(i).contains(":;")) {
                        String[] firstIndexOfName = groupOfTags.get(i).split(":;");
                        int size = firstIndexOfName.length;
                        for (int j = 0; j < size; j++) {
                            if (firstIndexOfName[j].contains("::")) {
                                String[] finalOptionNames = firstIndexOfName[j].split("::");
                                if (finalOptionNames[1].equalsIgnoreCase(optionName)) {
                                    labelNamesWithSeparator.add(finalOptionNames[1]);
                                }
                            } else if (firstIndexOfName[j].equalsIgnoreCase(optionName)) {
                                labelNamesWithSeparator.add(firstIndexOfName[j]);
                            }
                        }
                    }
                }
                logger.info("Starting the verification of Filter" +filterName+" on AE listing Page");
                for (String recordsToValidate : labelNamesWithSeparator) {
                    csAssert.assertTrue(recordsToValidate.equalsIgnoreCase(optionName), "Value of" + optionName + " " + "option applied in filter is not equal to the values in column" + filterName);
                }
            } else if (filterName.equalsIgnoreCase("status")) {
                JSONObject listObj = new JSONObject(listDataResponse);
                JSONArray listObjArr = listObj.getJSONArray("data");
                int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "workflowstatus");
                for (int i = 0; i < listObjArr.length(); i++) {
                    String workFlowStatusValue = listObjArr.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString();
                    csAssert.assertEquals(workFlowStatusValue, optionName, "List Page validation failed for Record Id " +
                            " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                }

            } else if (filterName.equalsIgnoreCase("statusId")) {
                JSONObject listObj = new JSONObject(listDataResponse);
                JSONArray listObjArr = listObj.getJSONArray("data");
                int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "status");
                for (int i = 0; i < listObjArr.length(); i++) {
                    String[] extractionStatusValue = listObjArr.getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;");
                    String status = extractionStatusValue[0];
                    csAssert.assertEquals(status, optionName, "List Page validation failed for Record Id " +
                            " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                }
            } else if (filterName.equalsIgnoreCase("valueId") || filterName.equalsIgnoreCase("fieldId") || filterName.equalsIgnoreCase("categoryId") || filterName.equalsIgnoreCase("duplicatedocs") || filterName.equalsIgnoreCase("unassigneddocs")) {
                List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(reportName, reportId, listDataObj, listDataResponse, filterName,
                        optionName, payload, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
                logger.info("Total Results to Validate for Filter {} and Option {} of Report [{}] having Id {}: {}", filterName, optionName, reportName, reportId,
                        listDataToValidate.size());

                for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {
                    int recordEntityTypeId = entityTypeId;
                    int idColumnNo = getIdColumnNoForReport(listDataObj.getListDataJsonStr(), reportId);

                    if (idColumnNo == -1) {
                        logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                        throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                    }

                    int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                    logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                    if (filterName.equalsIgnoreCase("categoryId")) {
                        logger.info("Validating Record Id " + recordId + "for Filter name " + filterName + " and " + optionName);
                        String payloadForClause = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\"},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":" + recordId + "}";
                        HttpResponse clauseListResponse = autoExtractionHelper.getTabData(payloadForClause, 493);
                        String clauseListResponseStr = EntityUtils.toString(clauseListResponse.getEntity());
                        boolean result = false;
                        if (ParseJsonResponse.validJsonResponse(clauseListResponseStr)) {
                            int columnId = ListDataHelper.getColumnIdFromColumnName(clauseListResponseStr, "name");
                            JSONObject obj = new JSONObject(clauseListResponseStr);
                            JSONArray arrObj = obj.getJSONArray("data");
                            for (int i = 0; i < arrObj.length(); i++) {
                                String[] temp = arrObj.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;");
                                if ((temp[0].trim()).equalsIgnoreCase(optionName)) {
                                    result = true;
                                    logger.info("Category id Filter name  is " + optionName + " and Clause list value is " + temp[0]);
                                }
                            }
                            csAssert.assertTrue(result, "Show Page validation failed for Record Id " + recordId +
                                    " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                        } else {
                            logger.error("Clause Tab List API Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
                            csAssert.assertTrue(false, "Clause Tab List API Response for Record Id " + recordId + " of Filter " +
                                    filterName + " and Option " + optionName + " is an Invalid JSON.");
                        }
                    } else if (filterName.equalsIgnoreCase("fieldId")) {
                        logger.info("Validating filter name " + filterName + " for " + optionName);
                        String payloadForMetadata = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":" + recordId + "}";
                        HttpResponse metadataTabListResponse = autoExtractionHelper.getTabData(payloadForMetadata, 433);
                        String metadataTabListResponseStr = EntityUtils.toString(metadataTabListResponse.getEntity());
                        boolean result = false;
                        if (ParseJsonResponse.validJsonResponse(metadataTabListResponseStr)) {
                            int columnId = ListDataHelper.getColumnIdFromColumnName(metadataTabListResponseStr, "fieldname");
                            JSONObject obj = new JSONObject(metadataTabListResponseStr);
                            JSONArray arrObj = obj.getJSONArray("data");
                            for (int i = 0; i < arrObj.length(); i++) {
                                String[] temp = arrObj.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().trim().split(":;");
                                if ((temp[0].trim()).equalsIgnoreCase(optionName)) {
                                    result = true;
                                } else if (temp[0].trim().contains("(")) {
                                    String[] temp1 = temp[0].split("\\(");
                                    String value = temp1[0].trim() + "(" + temp1[1].trim();
                                    if (value.equalsIgnoreCase(optionName)) {
                                        result = true;
                                    }
                                }
                            }
                            csAssert.assertTrue(result, "Metadata tab list API validation failed for Record Id " + recordId +
                                    " of Filter " + filterName + " and Option " + optionName + " of Entity Name  " + entityName + " of Report [" + reportName + "] having Id " + reportId);
                        } else {
                            logger.error("Metadata tab list API  Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
                            csAssert.assertTrue(false, "Metadata tab list API Response for Record Id " + recordId + " of Filter " +
                                    filterName + " and Option " + optionName + " is an Invalid JSON.");
                        }
                    } else if (filterName.equalsIgnoreCase("duplicatedocs")) {
                        logger.info("Started the verification for duplicate Data Filter ");
                        int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "documentname");
                        JSONObject listObj = new JSONObject(listDataResponse);
                        int totalData = listObj.getJSONArray("data").length();
                        int actualCount = listDataToValidate.size();
                        Set<String> documentNameSet = new HashSet<String>();
                        for (int j = 0; j < totalData; j++) {
                            documentNameSet.add(listObj.getJSONArray("data").getJSONObject(j).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[0]);
                        }
                        logger.info("Set of document Names when there is no duplicate doc:" + documentNameSet);
                        int expectedCount = documentNameSet.size();

                        csAssert.assertTrue(actualCount > expectedCount, "After Applying duplicate data filter it is still not showing duplicate docs for "+filterName+ "and report Id"+recordId);

                    } else if (filterName.equalsIgnoreCase("unassigneddocs")) {
                        logger.info("Started Verification for Filter Name" + filterName);
                        if (optionName.equals("Assigned")) {
                            try {
                                HttpResponse showPageResponse = AutoExtractionHelper.docShowAPI(recordId);
                                csAssert.assertTrue(showPageResponse.getStatusLine().getStatusCode() == 200, "Response code of AE show API is invalid");
                                String showPageStr = EntityUtils.toString(showPageResponse.getEntity());
                                JSONObject showPageObj = new JSONObject(showPageStr);
                                List<String> layoutGroups = new ArrayList<>();
                                int layoutGroupCount = showPageObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(0).getJSONArray("fields").length();
                                JSONArray layoutGroupArr = showPageObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(0).getJSONArray("fields");
                                for (int i = 0; i < layoutGroupCount; i++) {
                                    layoutGroups.add(String.valueOf(layoutGroupArr.getJSONObject(i).get("label")));
                                }
                                logger.info("Validating Assigned for:" + recordId);

                                csAssert.assertTrue(layoutGroups.contains("Stakeholders"),"Show Page validation failed for Record Id " + recordId + " of Filter " +
                                        filterName + " and Option " + optionName );

                            } catch (Exception e) {
                                csAssert.assertTrue(false, "AE Show page API is not working because of :  :" + e.getMessage());
                            }
                        } else if (optionName.equalsIgnoreCase("Unassigned")) {
                            try {
                                logger.info("Validating UnAssigned for:" + recordId);
                                HttpResponse generalTabResponse = AutoExtractionHelper.hitGeneralTabAPI(recordId);
                                csAssert.assertTrue(generalTabResponse.getStatusLine().getStatusCode() == 200, "Response Code of General Tab API is Invalid");
                                String generalTabStr = EntityUtils.toString(generalTabResponse.getEntity());
                                JSONObject generalTabObj = new JSONObject(generalTabStr);
                                boolean response = (boolean) generalTabObj.get("success");
                                csAssert.assertTrue(response==true,"Show Page validation failed for Record Id " + recordId + " of Filter " +
                                        filterName + " and Option " + optionName );

                            } catch (Exception e) {
                                csAssert.assertTrue(false, "General Tab API is not working because of :" + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type because of " + e.getMessage());
        }
    }

    public void hitListDataAndVerifyResults(String reportName, Integer reportId, String filterName, String optionName, String payload, String showPageObjectName,
                                            int entityTypeId, String entityName, CustomAssert csAssert, String filterType, Boolean applyRandomization, int maxRecordsToValidate) throws IOException {

        logger.info("Hitting Report List Data API for Filter {} and Option {} of Report [{}] having Id {}", filterName, optionName, reportName, reportId);
        ReportRendererListData listDataObj = new ReportRendererListData();
        listDataObj.hitReportRendererListData(reportId, payload);
        AutoExtractionTestReportListFilters.testDownloadReportData(payload,filterName);
        String listDataResponse = listDataObj.getListDataJsonStr();

        try {
            List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(reportName, reportId, listDataObj, listDataResponse, filterName,
                    optionName, payload, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
            logger.info("Total Results to Validate for Filter {} and Option {} of Report [{}] having Id {}: {}", filterName, optionName, reportName, reportId,
                    listDataToValidate.size());

            for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {

                int idColumnNo = getIdColumnNoForReport(listDataObj.getListDataJsonStr(), reportId);
                if (idColumnNo == -1) {
                    logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                    throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                }
                if (filterType.equalsIgnoreCase("inputtext") && filterName.equalsIgnoreCase("folder")) {
                    int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                    logger.info("Hitting General Tab API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                    HttpResponse generalTabResponse = autoExtractionHelper.hitGeneralTabAPI(recordId);
                    String generalTabResponseStr = EntityUtils.toString(generalTabResponse.getEntity());
                    boolean result = false;
                    if (ParseJsonResponse.validJsonResponse(generalTabResponseStr)) {
                        JSONObject obj = new JSONObject(generalTabResponseStr);
                        if (obj.getJSONObject("response").get("relativePath").toString().equalsIgnoreCase(optionName)) {
                            result = true;

                        }
                        logger.info("folder Name to validate is " + optionName);
                        csAssert.assertTrue(result, "General Tab validation failed for Record Id " + recordId +
                                " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                    } else {
                        logger.error("General tab list API  Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
                        csAssert.assertTrue(false, "General tab list API Response for Record Id " + recordId + " of Filter " +
                                filterName + " and Option " + optionName + " is an Invalid JSON.");
                    }
                } else if (filterType.equalsIgnoreCase("inputtext") && filterName.equalsIgnoreCase("metadatavalue")) {
                    int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                    logger.info("Hitting metadata Tab API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                    String payloadforMetadata = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"0\"}}},\"entityId\":" + recordId + "}";
                    HttpResponse metadataTabListResponse = autoExtractionHelper.getTabData(payloadforMetadata, 433);
                    String metadataTabListResponseStr = EntityUtils.toString(metadataTabListResponse.getEntity());
                    boolean result = false;
                    if (ParseJsonResponse.validJsonResponse(metadataTabListResponseStr)) {
                        int columnId = ListDataHelper.getColumnIdFromColumnName(metadataTabListResponseStr, "extractedtext");
                        JSONObject obj = new JSONObject(metadataTabListResponseStr);
                        JSONArray arrObj = obj.getJSONArray("data");
                        for (int i = 0; i < arrObj.length(); i++) {
                            if (arrObj.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().equalsIgnoreCase(optionName)) {
                                result = true;
                            }

                        }
                        logger.info("metadata Filter value  is " + optionName);
                        csAssert.assertTrue(result, "Metadata tab list API validation failed for Record Id " + recordId +
                                " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                    } else {
                        logger.error("Metadata tab list API  Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
                        csAssert.assertTrue(false, "Metadata tab list API Response for Record Id " + recordId + " of Filter " +
                                filterName + " and Option " + optionName + " is an Invalid JSON.");
                    }
                } else {
                    throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Select. " + e.getMessage());
        }
    }


    public static void hitListDataAndVerifyResultsForStakeholdersForAE(String reportName, Integer reportId, String filterName, String optionName, String optionId, String payload,
                                                                       int entityTypeId, CustomAssert csAssert, String roleGroupId, Boolean applyRandomization,
                                                                       int maxRecordsToValidate) throws Exception {
        try {
            logger.info("Hitting List Data API for Filter {}, Role Group Id {} and Option {}", filterName, roleGroupId, optionName);
            ReportRendererListData listDataObj = new ReportRendererListData();
            listDataObj.hitReportRendererListData(reportId, payload);
            AutoExtractionTestReportListFilters.testDownloadReportData(payload,filterName);

            List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(reportName, reportId, listDataObj, filterName, optionName,
                    payload, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, optionName, listDataToValidate.size());

            for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {
                int idColumnNo = getIdColumnNoForReport(listDataObj.getListDataJsonStr(), reportId);

                if (idColumnNo == -1) {
                    logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                    throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                }

                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                HttpResponse showApiRespone = AutoExtractionHelper.docShowAPI(recordId);
                String showApiResponseStr = EntityUtils.toString(showApiRespone.getEntity());
                if (ParseJsonResponse.validJsonResponse(showApiResponseStr)) {
                    JSONObject obj = new JSONObject(showApiResponseStr);
                    String stakeHolderJsonStr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").toString();
                    if (stakeHolderJsonStr != null) {
                        JSONObject stakeHolderJson = new JSONObject(stakeHolderJsonStr);
                        stakeHolderJson = stakeHolderJson.getJSONObject("values");
                        if (stakeHolderJson.has("rg_" + roleGroupId)) {
                            JSONArray stakeHolderJsonArr = stakeHolderJson.getJSONObject("rg_" + roleGroupId).getJSONArray("values");
                            boolean result = false;

                            for (int i = 0; i < stakeHolderJsonArr.length(); i++) {
                                if (stakeHolderJsonArr.getJSONObject(i).has("idTypes")) {
                                    int userGroupId = stakeHolderJsonArr.getJSONObject(i).getInt("id");
                                    logger.info("Hitting UserGroups Users API for UserGroup  Id {}, Entity Type Id {} and Record Id {}", userGroupId, entityTypeId,
                                            recordId);
                                    UserGroupsUsers userObj = new UserGroupsUsers();
                                    String userResponse = userObj.hitUserGroupsUsers(userGroupId, entityTypeId, recordId);

                                    if (ParseJsonResponse.validJsonResponse(userResponse)) {
                                        JSONArray userJsonArr = new JSONArray(userResponse);

                                        for (int j = 0; j < userJsonArr.length(); j++) {
                                            if (userJsonArr.getJSONObject(j).get("id").toString().trim().equalsIgnoreCase(optionId.trim())) {
                                                result = true;
                                                break;
                                            }
                                        }
                                    } else {
                                        csAssert.assertTrue(false, "UserGroups Users API Response for UserGroup Id " + userGroupId +
                                                ", Entity Type Id " + entityTypeId + " and Record Id " + recordId + " is an Invalid JSON.");
                                    }
                                } else if (stakeHolderJsonArr.getJSONObject(i).get("id").toString().trim().equalsIgnoreCase(optionId.trim())) {
                                    result = true;
                                }
                            }
                            if (!result) {
                                logger.error("Show Page validation failed for Record Id {} of Filter {}, Role Group {} and Option {}", recordId, filterName, roleGroupId,
                                        optionName);
                                csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                                        " of Filter " + filterName + ", Role Group " + roleGroupId + " and Option " + optionName);
                            }

                        } else {
                            logger.error("Couldn't find Role Group [{}] in Show Response for Record Id {} of Filter {} and Option {}", "rg_" + roleGroupId, recordId,
                                    filterName, optionName);
                            csAssert.assertTrue(false, "Couldn't find Role Group [rg_" + roleGroupId + "] in Show Response for Record Id " +
                                    recordId + " of Filter " + filterName + " and Option " + optionName);
                        }

                    } else {
                        logger.error("Couldn't get Stakeholders Json from Show Response for Record Id {} of Filter {} and Option {}", recordId, filterName,
                                optionName);
                        csAssert.assertTrue(false, "Couldn't get Stakeholders Json from Show Response for Record Id " + recordId + " of Filter " +
                                filterName + " and Option " + optionName);
                    }

                }

            }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }


    private static List<Map<Integer, Map<String, String>>> getListDataResultsToValidate(String reportName, Integer reportId, ReportRendererListData listDataObj,
                                                                                        String listDataResponse, String filterName, String optionName, String payload,
                                                                                        int entityTypeId, CustomAssert csAssert, Boolean applyRandomization,
                                                                                        int maxRecordsToValidate) {
        List<Map<Integer, Map<String, String>>> listDataResults = new ArrayList<>();

        try {
            if (listDataResponse == null) {
                listDataObj.hitReportRendererListData(reportId, payload);
                listDataResponse = listDataObj.getListDataJsonStr();
            }

            filterName = filterName.trim();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                listDataObj.setListData(listDataResponse);
                List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
                logger.info("Total Results found for Filter {} and Option {} of Report [{}] having Id {}: {}", filterName, optionName, reportName, reportId, listData.size());

                if (listData.size() > 0) {
                    listDataResults = filterListDataResultsToValidate(listData, applyRandomization, maxRecordsToValidate);
                }
            } else {
                logger.error("List Data API Response for Filter {} and Option {} of Report [{}] having Id {} is an Invalid JSON.", filterName, optionName, reportName,
                        reportId);
                csAssert.assertTrue(false, "List Data API Response for Filter " + filterName + " and Option " + optionName + " of Report [" + reportName +
                        "] having Id " + reportId + " is an Invalid JSON.");
                FileUtils.saveResponseInFile("ListData Filter " + filterName + " Report Id " + reportId + ".txt", listDataResponse);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting List Data Results to Validate for Filter {} and Option {} of Entity Type Id {}. {}", filterName, optionName,
                    entityTypeId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Getting List Data Results to Validate for Filter " + filterName + ", Option " + optionName +
                    " of Entity Type Id " + entityTypeId + ". " + e.getMessage());
        }
        return listDataResults;
    }

    private static List<Map<Integer, Map<String, String>>> filterListDataResultsToValidate(List<Map<Integer, Map<String, String>>> allResults, Boolean applyRandomization,
                                                                                           int maxRecordsToValidate) {
        if (!applyRandomization)
            return allResults;

        List<Map<Integer, Map<String, String>>> listDataResultsToValidate = new ArrayList<>();

        try {
            int[] randomNumbersArr = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allResults.size() - 1, maxRecordsToValidate);

            for (int randomNumber : randomNumbersArr) {
                listDataResultsToValidate.add(allResults.get(randomNumber));
            }
        } catch (Exception e) {
            logger.error("Exception while filtering Results to Validate. {}", e.getMessage());
        }
        return listDataResultsToValidate;
    }

    private static int getIdColumnNoForReport(String listDataResponse, int reportId) {
        String columnName;

        switch (reportId) {
            case 222:
            case 223:
            case 224:
            case 270:
                columnName = "sirion_id";
                break;

            case 444:
                columnName = "servicedataid";
                break;

            default:
                columnName = "id";
                break;
        }

        return ListDataHelper.getColumnIdFromColumnName(listDataResponse, columnName);
    }


    private static List<Map<Integer, Map<String, String>>> getListDataResultsToValidate(String reportName, Integer reportId, ReportRendererListData listDataObj,
                                                                                        String filterName, String optionName, String payload, int entityTypeId,
                                                                                        CustomAssert csAssert, Boolean applyRandomization, int maxRecordsToValidate) {
        return getListDataResultsToValidate(reportName, reportId, listDataObj, null, filterName, optionName, payload, entityTypeId,
                csAssert, applyRandomization, maxRecordsToValidate);
    }


}