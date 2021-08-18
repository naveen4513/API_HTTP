package com.sirionlabs.helper.auditlog;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidateCommunicationTab {

    private final static Logger logger = LoggerFactory.getLogger(ValidateCommunicationTab.class);

    public void verifyStatusInCommunicationTab(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData, CustomAssert csAssert, String expectedStatus ) {
        try {

            int statusColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "status_name");
            String actualstatusByValue = listData.get(0).get(statusColumnNo).get("value");
            actualstatusByValue = actualstatusByValue == null ? "null" : actualstatusByValue;

            if (!actualstatusByValue.trim().equalsIgnoreCase(expectedStatus.trim())) {
                csAssert.assertTrue(false, "Expected status Value: [" + expectedStatus.trim() + "] and Actual status Value: [" +
                        actualstatusByValue.trim() + "]");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating status in Communication Tab. " + e.getMessage());
        }
    }

    public void verifyCompletedByInCommunicationTab(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData, CustomAssert csAssert, String expectedCompletedByValue ) {
        try {

            int completedByColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "completed_by");
            String actualcompletedByValue = listData.get(0).get(completedByColumnNo).get("value");
            actualcompletedByValue = actualcompletedByValue == null ? "null" : actualcompletedByValue;

            if (!actualcompletedByValue.trim().equalsIgnoreCase(expectedCompletedByValue.trim())) {
                csAssert.assertTrue(false, "Expected CompletedBy Value: [" + expectedCompletedByValue.trim() + "] and Actual CompletedBy Value: [" +
                        actualcompletedByValue.trim() + "]");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Completed By  in Communication Tab. " + e.getMessage());
        }
    }




    public void verifyCommentincommunicationtab(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData, CustomAssert csAssert, String expectedCommentValue ) {
        try {

            int commentColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "comment");
            String actualCommentValue = listData.get(0).get(commentColumnNo).get("value");
            actualCommentValue = actualCommentValue == null ? "null" : actualCommentValue;

            if (!actualCommentValue.trim().equalsIgnoreCase(expectedCommentValue.trim())) {
                csAssert.assertTrue(false, "Expected Comment Value: [" + expectedCommentValue.trim() + "] and Actual Comment Value: [" +
                        actualCommentValue.trim() + "]");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Comment  in Communication Tab. " + e.getMessage());
        }
    }



    public void verifyUploadedDocumentsPresentInCommunicationTab(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData,CustomAssert csAssert, ArrayList<String> fileNameList , int noOfFile) {
        try {
            int documentColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "document");
            String documentsValue = listData.get(0).get(documentColumnNo).get("value");
            String[] documents = documentsValue.split("###");
            csAssert.assertEquals(documents.length,noOfFile,"no of file uploaded is incorrect");
            for (int i =0 ; i< documents.length;i++){
                String documentNameWithoutExtension = FileUtils.getFileNameWithoutExtension(fileNameList.get(i));
                String documentExtension = FileUtils.getFileExtension(fileNameList.get(i));
                if (documents[i] == null || !documents[i].contains(documentNameWithoutExtension) || !documents[i].contains(documentExtension)) {
                    csAssert.assertTrue(false, "Uploaded Document " + fileNameList.get(i) + " is not present in Communication Tab Response.");
                }
            }




        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating that uploaded documents are present in Communication tab. " + e.getMessage());
        }
    }



    public void verifyActionTimeInCommunicationTab(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData, CustomAssert csAssert, String expectedActionTime ) {
        try {

            int statusColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "user_date");
            String actualActionTimeValue = listData.get(0).get(statusColumnNo).get("value");
            actualActionTimeValue = actualActionTimeValue == null ? "null" : actualActionTimeValue;

            if (!actualActionTimeValue.trim().contains(expectedActionTime.trim())) {
                csAssert.assertTrue(false, "Expected Action Time Value: [" + expectedActionTime.trim() + "] and Actual Action Time Value: [" +
                        actualActionTimeValue.trim() + "]");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating time of action  in Communication Tab. " + e.getMessage());
        }
    }




}
