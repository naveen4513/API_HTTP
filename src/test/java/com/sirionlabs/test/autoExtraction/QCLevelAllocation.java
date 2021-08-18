package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.api.autoExtraction.QCLevel1TagCreation;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class QCLevelAllocation {
    private final static Logger logger = LoggerFactory.getLogger(QCLevelAllocation.class);
    int Tag1Id, Tag2Id, TagId;
    String Tag1Name, Tag2Name, TagName;

    public String qcLevel1tagCreation() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Hitting the API to update value of Tag1 and Tag2");
        APIResponse tag1CreationResponse = QCLevel1TagCreation.tag1CreateAPIResponse(QCLevel1TagCreation.getAPIPath(), QCLevel1TagCreation.getHeaders(), QCLevel1TagCreation.getPayload());
        Integer tag1ResponseCode = tag1CreationResponse.getResponseCode();
        String tag1CreateStr = tag1CreationResponse.getResponseBody();
        csAssert.assertTrue(tag1ResponseCode == 200, "Response Code is Invalid");
        return tag1CreateStr;
    }

    /*Test Case to validate QC level allocation is working fine or not*/
    @Test
    public void qcLevelAllocation() {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Creating tag1 for performing qcLevelAllocation");
            QCLevelAllocation obj = new QCLevelAllocation();
            String tag1CreateStr = obj.qcLevel1tagCreation();
            JSONObject tag1CreateJson = new JSONObject(tag1CreateStr);
            csAssert.assertTrue(tag1CreateJson.get("success").toString().equals("true"), "Tag1 is not created successfully");
            TagId = (int) tag1CreateJson.get("id");
            TagName = (String) tag1CreateJson.get("name");

            //Creating Tag2
            try {
                logger.info("Creating tag2 for performing qcLevelAllocation");
                String tag2Name = "Tag2" + RandomString.getRandomAlphaNumericString(5);
                HttpResponse tag2CreationResponse = AutoExtractionHelper.tag2Creation(tag2Name);
                csAssert.assertTrue(tag2CreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String tag2Str = EntityUtils.toString(tag2CreationResponse.getEntity());
                JSONObject tag2Json = new JSONObject(tag2Str);
                csAssert.assertTrue(tag2Json.get("success").toString().equals("true"), "Tag2 is not created successfully");
                Tag2Id = (int) tag2Json.get("id");
                Tag2Name = (String) tag2Json.get("name");

                //Getting the list of Document Ids from AE doc listing to perform qc level allocation
                logger.info("Getting the list of document Ids");
                HttpResponse listingResponse = AutoExtractionHelper.aeDocListing();
                csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
                JSONObject listingResponseJson = new JSONObject(listingResponseStr);
                int documentCount = listingResponseJson.getJSONArray("data").length();
                int documentColumnId = ListDataHelper.getColumnIdFromColumnName(listingResponseStr, "documentname");
                List<Integer> documentIds = new LinkedList<>();
                for (int i = 0; i < documentCount; i++) {
                    JSONObject listingObj = listingResponseJson.getJSONArray("data").getJSONObject(i);
                    String documentNameValue = listingObj.getJSONObject(Integer.toString(documentColumnId)).getString("value");
                    String[] documentName = documentNameValue.split(":;");
                    int documentId = Integer.valueOf(documentName[1]);
                    documentIds.add(documentId);
                }
                Collections.sort(documentIds, Collections.reverseOrder());
                String documentIdsWithComma = (String) documentIds.stream().map(Object::toString).collect(Collectors.joining(","));

                //Creating Tag1 to Add to all documents
                logger.info("Creating tag1 for performing qcLevelAllocation");
                tag1CreateStr = obj.qcLevel1tagCreation();
                tag1CreateJson = new JSONObject(tag1CreateStr);
                csAssert.assertTrue(tag1CreateJson.get("success").toString().equals("true"), "Tag1 is not created successfully");
                Tag1Id = (int) tag1CreateJson.get("id");
                Tag1Name = (String) tag1CreateJson.get("name");


                //Now Performing QC Level Allocation
                try {
                    logger.info("Performing QC level allocation");
                    HttpResponse qcLevelAllocationResponse = AutoExtractionHelper.qcLevelAllocation(TagId, Tag1Id, Tag2Id, documentIdsWithComma);
                    csAssert.assertTrue(qcLevelAllocationResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                    String qcLevelAllocationStr = EntityUtils.toString(qcLevelAllocationResponse.getEntity());
                    JSONObject qcLevelAllocationJson = new JSONObject(qcLevelAllocationStr);
                    csAssert.assertEquals(qcLevelAllocationJson.get("success"), true, "QC Level Allocation is not working");
                } catch (Exception e) {
                    csAssert.assertTrue(false, "Exception occured while performing QC level allocation:" + e.getMessage());

                }

            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception occured while creating Tag2:" + e.getMessage());

            }

        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception occured while creating Tag1:" + e.getMessage());

        }

    }

    @Test
    public void validateQCLevelChangesOnListing() throws IOException {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Now Validating whether QC Level Tags have been updated or not");
            logger.info("Getting the list of document Ids");
            HttpResponse listingResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson = new JSONObject(listingResponseStr);
            int documentCount = listingResponseJson.getJSONArray("data").length();
            int documentColumnId = ListDataHelper.getColumnIdFromColumnName(listingResponseStr, "documentname");
            List<Integer> documentIds = new LinkedList<>();
            for (int i = 0; i < documentCount; i++) {
                JSONObject listingObj = listingResponseJson.getJSONArray("data").getJSONObject(i);
                String documentNameValue = listingObj.getJSONObject(Integer.toString(documentColumnId)).getString("value");
                String[] documentName = documentNameValue.split(":;");
                int documentId = Integer.valueOf(documentName[1]);
                documentIds.add(documentId);
            }
            Collections.sort(documentIds, Collections.reverseOrder());

            for (int i = 0; i < documentIds.size(); i++) {
                //Now validating on listing page if the data has been updated successfully in AE doc listing

                List<String> columns = listingResponseJson.getJSONArray("data").getJSONObject(i).keySet().stream().collect(Collectors.toList());
                int totalColumns = listingResponseJson.getJSONArray("data").getJSONObject(i).length();
                for (int j = 0; j < totalColumns; j++) {
                    if (listingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("columnName").toString().equals("doctag1")) {
                        String docTag1 = listingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("value").toString();
                        csAssert.assertTrue(docTag1.contains(Tag1Name), "User that has been added via bulk edit is not present");
                    } else if (listingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("columnName").toString().equals("doctag2")) {
                        String docTag2 = listingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("value").toString();
                        csAssert.assertTrue(docTag2.contains(Tag2Name), "User that has been added via bulk edit is not present");
                    }
                }


                //Test Case to validate QC Level1 field value has been updated for the Allocation size of the documents (Special Case)

                logger.info("Test Case to validate QC Level1 field value has been updated for the Allocation size of the documents (Special Case)");
                try {
                    int counter = 0;
                    for (i = 0; i < documentIds.size(); i++) {
                        //Now validating on listing page if the data has been updated successfully in AE doc listing

                        for (int j = 0; j < totalColumns; j++) {
                            if (listingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("columnName").toString().equals("doctag1")) {
                                String docTag1 = listingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("value").toString();
                                List<String> allTag1 = new ArrayList<>();
                                if (docTag1.contains(":;")) {
                                    String[] tagNames = docTag1.split(":;");
                                    allTag1.add(tagNames[0]);
                                    int size = tagNames.length;
                                    for (int k = 0; k < size; k++) {
                                        if (tagNames[k].contains("::")) {
                                            String[] finalNames = tagNames[k].split("::");
                                            if (finalNames[1].equalsIgnoreCase(TagName)) {
                                                counter = counter +1;
                                            }

                                        }
                                        else if (tagNames[k].equalsIgnoreCase(TagName))
                                        {
                                            counter=counter+1;
                                        }
                                    }
                                }

                            }

                        }
                    }
                    logger.info("Counter value:" + counter);
                    csAssert.assertEquals(counter,5,"Value of QC level1 has been updated for only 5 documents but it has updated for:" +counter + " " + "Documents");

                } catch (Exception e) {
                    csAssert.assertTrue(false, "Exception occured while hitting List Data API for Special Case of QC Level:" + e.getMessage());
                }

                csAssert.assertAll();
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception occured while hitting List Data API:" + e.getMessage());

        }
    }
}