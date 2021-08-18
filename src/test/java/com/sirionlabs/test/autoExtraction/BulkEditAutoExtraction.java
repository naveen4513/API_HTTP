package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BulkEditAutoExtraction extends TestAPIBase {
private final static Logger logger = LoggerFactory.getLogger(BulkEditAutoExtraction.class);
CustomAssert csAssert = new CustomAssert();
String roleGroupName;
int roleGroupId;


//Creating a method to call Listing API after every bulk Operation(Append,Remove and Override)

public  String listingResponse() throws IOException {

        HttpResponse assignedFilterResponse = AutoExtractionHelper.assignedFilter();
        csAssert.assertTrue(assignedFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
        String assignedFiltersStr = EntityUtils.toString(assignedFilterResponse.getEntity());
        return assignedFiltersStr;
}


/*Test Case to Get the Details of all the editable fields on Bulk Create - Create Form*/
@Test
public void bulkEditAutoExtraction() throws IOException {
CustomAssert csAssert = new CustomAssert();
BulkEditAutoExtraction obj = new BulkEditAutoExtraction();

try {
logger.info("API to get the list of editable stakeholders, and Static Fields ");
HttpResponse bulkEditResponse = AutoExtractionHelper.getBulkEditData();
csAssert.assertTrue(bulkEditResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
String bulkEditStr = EntityUtils.toString(bulkEditResponse.getEntity());
JSONObject bulkEditJson = new JSONObject(bulkEditStr);

//Getting the list of Static Fields Name & Ids along with Role Groups to perform bulk action
logger.info("Fetching the list of Ids and Data that is to be used while performing bulk");
HttpResponse userOptionsResponse = AutoExtractionHelper.getUserId();
csAssert.assertTrue(userOptionsResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
String userOptionsStr = EntityUtils.toString(userOptionsResponse.getEntity());
JSONObject userOptionsResponseJson = new JSONObject(userOptionsStr);
int dataCount = userOptionsResponseJson.getJSONArray("data").length();

//Getting the UserId and name from the list of Users
int userId= (int) userOptionsResponseJson.getJSONArray("data").getJSONObject(0).get("id");
String userName = (String) userOptionsResponseJson.getJSONArray("data").getJSONObject(0).get("name");

//Fetching the roleGroupId and Name from bulk Edit Request API
    int stakeholderCount = bulkEditJson.getJSONArray("stakeholders").length();
    for(int i=0;i<stakeholderCount;i++)
    {
        if(bulkEditJson.getJSONArray("stakeholders").getJSONObject(i).get("name").equals("Manager"))
        {
            roleGroupName = (String) bulkEditJson.getJSONArray("stakeholders").getJSONObject(i).get("name");
            roleGroupId = (int) bulkEditJson.getJSONArray("stakeholders").getJSONObject(i).get("id");
        }
    }

int countOfStaticFields = bulkEditJson.getJSONArray("staticFields").length();
List<Integer> listOfStaticFieldIds = new LinkedList<>();
for (int i = 0; i < countOfStaticFields; i++) {
listOfStaticFieldIds.add((Integer) bulkEditJson.getJSONArray("staticFields").getJSONObject(i).get("id"));
}

//Creating the new QC-Level1 and QC-level2 tags that are to be selected while performing bulk edit
try {
    int newlyCreatedQCL1FieldId;
    String newlyCreatedQCL1FieldName = "Automation QC Level1" + RandomString.getRandomAlphaNumericString(5);
    String payload = "{\"name\":\"" + newlyCreatedQCL1FieldName + "\"}";
    HttpResponse qcLevel1FieldResponse = AutoExtractionHelper.qcLevel1TagCreation(payload);
    csAssert.assertTrue(qcLevel1FieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
    String qcLevel1Str = EntityUtils.toString(qcLevel1FieldResponse.getEntity());
    JSONObject qcLevel1Json = new JSONObject(qcLevel1Str);
    if (qcLevel1Json.has("id")) {
        newlyCreatedQCL1FieldId = (int) qcLevel1Json.get("id");
    } else {
        throw new SkipException("Error in Creating new qcLevelField1 with name " + newlyCreatedQCL1FieldName);
    }

//Creating the new QC-level2 tags that are to be selected while performing bulk edit
    try {
        int newlyCreatedQCL2FieldId;
        String newlyCreatedQCL2FieldName = "Automation QC Level2" + RandomString.getRandomAlphaNumericString(5);
        payload = "{\"name\":\"" + newlyCreatedQCL2FieldName + "\"}";
        HttpResponse qcLevel2FieldResponse = AutoExtractionHelper.qcLevel2TagCreation(payload);
        csAssert.assertTrue(qcLevel2FieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
        String qcLevel2Str = EntityUtils.toString(qcLevel2FieldResponse.getEntity());
        JSONObject qcLevel2Json = new JSONObject(qcLevel2Str);
        if (qcLevel2Json.has("id")) {
            newlyCreatedQCL2FieldId = (int) qcLevel2Json.get("id");
        } else {
            throw new SkipException("Error in Creating new qcLevelField2 with name " + newlyCreatedQCL2FieldName);
        }

        //Creating the new Doc tag that is to be selected while performing bulk edit
        try {
            int newlyCreatedDocTagId;
            String newlyCreatedDocTagName = "Automation Doc Tag" + RandomString.getRandomAlphaNumericString(5);
            payload = "{\"name\":\"" + newlyCreatedDocTagName + "\"}";
            HttpResponse docTagCreationResponse = AutoExtractionHelper.docTagCreation(payload);
            csAssert.assertTrue(docTagCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String docTagCreationStr = EntityUtils.toString(docTagCreationResponse.getEntity());
            JSONObject docTagCreationJson = new JSONObject(docTagCreationStr);
            if (docTagCreationJson.has("id")) {
                newlyCreatedDocTagId = (int) docTagCreationJson.get("id");
            } else {
                throw new SkipException("Error in Creating new docTagCreation with name " + newlyCreatedDocTagName);
            }


            //Applying filter for only assigned documents to perform bulk Action

            try {
                String assignedFiltersStr = obj.listingResponse();
                JSONObject assignedFilterJson = new JSONObject(assignedFiltersStr);
                int totalData = assignedFilterJson.getJSONArray("data").length();
                List<Integer> documentIds = new LinkedList<>();
                int columnId = ListDataHelper.getColumnIdFromColumnName(assignedFiltersStr, "documentname");
                for (int i = 0; i < totalData; i++) {
                    JSONObject documentObj = assignedFilterJson.getJSONArray("data").getJSONObject(i);
                    String documentNameValue = documentObj.getJSONObject(Integer.toString(columnId)).getString("value");
                    String[] docId = documentNameValue.split(":;");
                    documentIds.add(Integer.valueOf(docId[1]));
                }
                Collections.sort(documentIds,Collections.reverseOrder());
                //Performing Bulk Edit Append Operation
                try {
                    logger.info("Performing Bulk Edit Append Operation for Auto-extraction");
                    payload = "{\"entityTypeId\":316,\"entityIds\":[" + documentIds.get(0) + "," + documentIds.get(1) + "],\"stakeholders\":[{\"id\":\"" + roleGroupId + "\"," +
                            "\"name\":\"" + roleGroupName + "\",\"type\":\"multiselect\",\"action\":\"Append\",\"values\":\"" + userId + "\"" +
                            ",\"dataMap\":{\"userGroups\":\"\"}}],\"staticFields\":[{\"id\":\"" + listOfStaticFieldIds.get(0) + "\",\"name\":\"Tags\"," +
                            "\"type\":\"multiselect\",\"action\":\"Append\",\"values\":\"" + newlyCreatedDocTagId + "\",\"dataMap\":{\"userGroups\":\"\"}}," +
                            "{\"id\":\"" + listOfStaticFieldIds.get(1) + "\",\"name\":\"QC-Level1\",\"type\":\"multiselect\",\"action\":\"Append\",\"values\":\"" + newlyCreatedQCL1FieldId + "\"," +
                            "\"dataMap\":{\"userGroups\":\"\"}},{\"id\":\"" + listOfStaticFieldIds.get(2) + "\",\"name\":\"QC-Level2\",\"type\":\"multiselect\"," +
                            "\"action\":\"Append\",\"values\":\"" + newlyCreatedQCL2FieldId + "\",\"dataMap\":{\"userGroups\":\"\"}}]}";

                    HttpResponse bulkEditAppendResponse = AutoExtractionHelper.performBulkEdit(payload);
                    csAssert.assertTrue(bulkEditAppendResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                    String bulkEditAppendStr = EntityUtils.toString(bulkEditAppendResponse.getEntity());
                    JSONObject bulkEditAppendJson = new JSONObject(bulkEditAppendStr);
                    String message = String.valueOf(bulkEditAppendJson.get("Message"));
                    csAssert.assertTrue(message.equals("Entities Updated Successfully"), "Bulk Edit Append Operation is not getting performed");

                    //Waiting for bulk Edit to get complete
                    Thread.sleep(35000);
                    //Hitting the Listing API again to get the updated changes
                    assignedFiltersStr = obj.listingResponse();
                    assignedFilterJson = new JSONObject(assignedFiltersStr);

                    //Now validating on listing page if the data has been updated successfully in AE doc listing

                    List<String> columns = assignedFilterJson.getJSONArray("data").getJSONObject(0).keySet().stream().collect(Collectors.toList());
                    int totalColumns = assignedFilterJson.getJSONArray("data").getJSONObject(0).length();
                    for (int i = 0; i < totalColumns; i++) {
                        if (assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("columnName").toString().equals("ManagerROLE_GROUP")) {
                            String stakeholder = assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("value").toString();
                            csAssert.assertTrue(stakeholder.contains(userName), "User that has been added via bulk edit is not present");

                        }
                        else if (assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("columnName").toString().equals("doctag1")) {
                            String docTag1Name= assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("value").toString();
                            List<String> allTag1 = new ArrayList<>();
                            if(docTag1Name.contains(":;"))
                            {
                                String[] tagNames = docTag1Name.split(":;");
                                allTag1.add(tagNames[0]);
                                int size = tagNames.length;
                                for(int j=0;j<size;j++)
                                {
                                    if(tagNames[j].contains("::"))
                                    {
                                        String[] finalNames = tagNames[j].split("::");
                                        if(finalNames[1].equalsIgnoreCase(newlyCreatedQCL1FieldName))
                                        {
                                            allTag1.add(finalNames[1]);
                                        }

                                    }
                                }
                            }
                            csAssert.assertTrue(allTag1.contains(newlyCreatedQCL1FieldName), "Doc Tag1 that has been added via Bulk Edit is not present");

                        } else if (assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("columnName").toString().equals("doctag1")) {
                            String docTag2Name = assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("value").toString().split(":;")[0];
                            List<String> allTag2 = new ArrayList<>();
                            if(docTag2Name.contains(":;"))
                            {
                                String[] tagNames = docTag2Name.split(":;");
                                allTag2.add(tagNames[0]);
                                int size = tagNames.length;
                                for(int j=0;j<size;j++)
                                {
                                    if(tagNames[j].contains("::"))
                                    {
                                        String[] finalNames = tagNames[j].split("::");
                                        if(finalNames[1].equalsIgnoreCase(newlyCreatedQCL1FieldName))
                                        {
                                            allTag2.add(finalNames[1]);
                                        }

                                    }
                                }
                            }


                            csAssert.assertTrue(docTag2Name.equals(newlyCreatedQCL2FieldName), "Doc Tag2 that has been added via Bulk Edit is not present");
                        }
                    }

                    try {

                        //Hitting Master Data API to get list of Tag and Ids that will be used for performing Bulk Edit Override operation
                        logger.info("Fetching a Tag Id and Name from MasterData API to perform override operation-Bulk Edit");
                        HttpResponse masterDataResponse = AutoExtractionHelper.getMasterData();
                        String masterDataStr = EntityUtils.toString(masterDataResponse.getEntity());
                        JSONObject masterDataJson = new JSONObject(masterDataStr);
                        int tagIdOverride = (int) masterDataJson.getJSONArray("tags").getJSONObject(1).get("id");
                        String tagNameOverride = (String) masterDataJson.getJSONArray("tags").getJSONObject(1).get("name");

                        try {
                            //Performing Bulk Edit Override Operation
                            logger.info("Performing Bulk Edit Override Operation");
                            payload = "{\"entityTypeId\":316,\"entityIds\":[" + documentIds.get(0) + "," + documentIds.get(1) + "],\"staticFields\":[{\"id\":\"" + listOfStaticFieldIds.get(0) + "\",\"name\":\"Tags\"," +
                                    "\"type\":\"multiselect\",\"action\":\"Override\",\"values\":\"" + tagIdOverride + "\",\"dataMap\":{\"userGroups\":\"\"}}]}";
                            HttpResponse bulkOverrideResponse = AutoExtractionHelper.performBulkEdit(payload);
                            csAssert.assertTrue(bulkOverrideResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                            String bulkOverrideStr = EntityUtils.toString(bulkOverrideResponse.getEntity());
                            JSONObject bulkOverrideJson = new JSONObject(bulkOverrideStr);
                            message = (String) bulkOverrideJson.get("Message");
                            csAssert.assertTrue(message.contains("Entities Updated Successfully"), "Bulk Edit Override is not getting Performed(Override Operation)");

                            Thread.sleep(500);
                            //Hitting Assigned Filter API
                            //Hitting the Listing API again to get the updated changes
                            assignedFiltersStr = obj.listingResponse();
                            assignedFilterJson = new JSONObject(assignedFiltersStr);

                            for (int i = 0; i < totalColumns; i++) {
                                if (assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("columnName").toString().equals("doctags")) {
                                    String docTagName = assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("value").toString().split(":;")[0];
                                    csAssert.assertEquals(docTagName,tagNameOverride, "Doc Tag that has been added via Bulk Edit Override Operation is not present");

                                }
                            }

                            try {
                                //Performing Bulk Edit Remove Operation
                                logger.info("Performing Bulk Edit Remove Operation");

                                payload = "{\"entityTypeId\":316,\"entityIds\":[" + documentIds.get(0) + "," + documentIds.get(1) + "],\"stakeholders\":[{\"id\":\"" + roleGroupId + "\",\"name\":\"" + roleGroupName + "\"," +
                                        "\"type\":\"multiselect\",\"action\":\"remove\",\"values\":\"" + userId + "\",\"dataMap\":{\"userGroups\":\"\"}}]," +
                                        "\"staticFields\":[{\"id\":\"" + listOfStaticFieldIds.get(0) + "\",\"name\":\"Tags\",\"type\":\"multiselect\",\"action\":\"remove\"," +
                                        "\"values\":\"" + tagIdOverride + "\",\"dataMap\":{\"userGroups\":\"\"}},{\"id\":\"" + listOfStaticFieldIds.get(1) + "\",\"name\":\"QC-Level1\",\"type\":\"multiselect\"," +
                                        "\"action\":\"remove\",\"values\":\"" + newlyCreatedQCL1FieldId + "\",\"dataMap\":{\"userGroups\":\"\"}},{\"id\":\"" + listOfStaticFieldIds.get(2) + "\",\"name\":\"QC-Level2\"," +
                                        "\"type\":\"multiselect\",\"action\":\"remove\",\"values\":\"" + newlyCreatedQCL2FieldId + "\",\"dataMap\":{\"userGroups\":\"\"}}]}";

                                HttpResponse bulkEditRemoveResponse = AutoExtractionHelper.performBulkEdit(payload);
                                csAssert.assertTrue(bulkEditRemoveResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                                String bulkEditRemoveStr = EntityUtils.toString(bulkEditRemoveResponse.getEntity());
                                JSONObject bulkEditRemoveJson = new JSONObject(bulkEditRemoveStr);
                                message = String.valueOf(bulkEditRemoveJson.get("Message"));
                                csAssert.assertTrue(message.contains("Entities Updated Successfully"), "Bulk Edit Remove Operation is not getting performed(Remove Operation)");

                                Thread.sleep(35000);
                                //Hitting Assigned Filter API
                                assignedFiltersStr = obj.listingResponse();
                                assignedFilterJson = new JSONObject(assignedFiltersStr);
                                String roleGroupActualName = roleGroupName + "ROLE_GROUP";
                                //Now checking the removed values in AE doc listing
                                for (int i = 0; i < totalColumns; i++) {
                                    if (assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("columnName").toString().equals(roleGroupActualName)) {
                                        String stakeholder = assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("value").toString();
                                        csAssert.assertTrue(!stakeholder.contains(userName), "User that has been removed via Bulk Edit has not been removed");

                                    }
                                     else if (assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("columnName").toString().equals("doctag1")) {
                                        String docTag1Name = assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("value").toString().split(":;")[0];
                                        csAssert.assertTrue(!docTag1Name.contains(newlyCreatedQCL1FieldName), "Doc Tag1 that has been removed via Bulk Edit has not been removed");

                                    } else if (assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("columnName").toString().equals("doctag2")) {
                                        String docTag2Name = assignedFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(columns.get(i)).get("value").toString().split(":;")[0];
                                        csAssert.assertTrue(!docTag2Name.contains(newlyCreatedQCL2FieldName), "Doc Tag2 that has been removed via Bulk Edit has not been removed");
                                    }
                                }
                            } catch (Exception e) {
                                logger.info("Exception occured while hitting Bulk Edit API for Remove Operation");
                                csAssert.assertTrue(false, e.getMessage());
                            }
                        } catch (Exception e) {
                            logger.info("Exception occured while hitting Bulk Edit API for Override Operation");
                            csAssert.assertTrue(false, e.getMessage());
                        }

                    } catch (Exception e) {
                        logger.info("Exception occured while hitting masterData API");
                        csAssert.assertTrue(false, e.getMessage());
                    }

                } catch (Exception e) {
                    logger.info("Exception occured while performing Bulk Edit Append Operation");
                    csAssert.assertTrue(false, e.getMessage());
                }
            } catch (Exception e) {
                logger.info("Exception occured while applying Filters on automation listing");
                csAssert.assertTrue(false, e.getMessage());
            }

        } catch (Exception e) {
            logger.info("Exception occured while hitting docTagCreation API");
            csAssert.assertTrue(false, e.getMessage());
        }

    } catch (Exception e) {
        logger.info("Exception occured while hitting QCLevelFieldCreation2 API");
        csAssert.assertTrue(false, e.getMessage());
    }
}
      catch (Exception e)
    {
        logger.info("Exception occured while hitting QCLevelFieldCreation1 API");
        csAssert.assertTrue(false, e.getMessage());
    }
    }   catch (Exception e) {
        logger.info("Exception occured while hitting bukCreate API");
        csAssert.assertTrue(false, e.getMessage());

    }
        csAssert.assertAll();

    }

}
