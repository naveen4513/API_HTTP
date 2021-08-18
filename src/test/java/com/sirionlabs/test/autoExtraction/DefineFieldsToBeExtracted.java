package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.HTTP;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sirionlabs.test.autoExtraction.TestAutoExtractionAPIs.clientId;
import static com.sirionlabs.test.autoExtraction.TestAutoExtractionAPIs.entityId;

public class DefineFieldsToBeExtracted
{
    private final static Logger logger = LoggerFactory.getLogger(TableExtraction.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;

    @Test
    public void defineFieldsInAProject() throws IOException
    {
        softAssert = new SoftAssert();

        try
        {
         logger.info("Validate the default set of fields for all clients is present or not", "Test Case Id:C89553");
            // Get All the Meta Data Fields in Project to extract
            String getAllFieldsUrl = "/metadataautoextraction/getAllFields";
            HttpResponse metadataFieldResponse = AutoExtractionHelper.getAllMetaDataFields(getAllFieldsUrl);
            softAssert.assertTrue(metadataFieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String metadataFieldResponseStr = EntityUtils.toString(metadataFieldResponse.getEntity());

            JSONObject metadataFieldResponseJsonStr = new JSONObject(metadataFieldResponseStr);
            int metadataFieldsLength = metadataFieldResponseJsonStr.getJSONArray("response").length();
            HashMap<Integer,String> metadataFields = new LinkedHashMap<>();
            for(int i=0;i<metadataFieldsLength;i++){
                metadataFields.put(Integer.valueOf(metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("id").toString()),metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("name").toString());
            }

            if(metadataFields.size()<1){
                throw new SkipException("No Meta Data Fields are there to select in project");
            }
            softAssert.assertTrue(metadataFields.size()>=60,"All the default metadata fields are not present for this client");

            /*Test Case to map a set of fields with a Project*/
            logger.info("Creating a new Project"+"Test Case Id:C141359");
            String createProjectUrl = "/metadataautoextraction/create";
            String projectName = "Test_Automation" + RandomString.getRandomAlphaNumericString(10);
            String createProjectPayload = "{\"name\":\""+ projectName +"\",\"description\":\"sgsgd\",\"projectLinkedFieldIds\":[12485,12486],\"clientId\":"+ clientId +"}";
            HttpResponse projectCreationResponse = AutoExtractionHelper.createProject(createProjectUrl,createProjectPayload);
            softAssert.assertTrue(projectCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String projectCreationResponseStr = EntityUtils.toString(projectCreationResponse.getEntity());
            softAssert.assertTrue(APIUtils.validJsonResponse(projectCreationResponseStr),"Not a valid Json");

            JSONObject createProjectJson = new JSONObject(projectCreationResponseStr);

            softAssert.assertTrue(createProjectJson.get("success").toString().equals("true"),"Project is not created successfully");
            int newlyCreatedProjectId = Integer.valueOf(createProjectJson.getJSONObject("response").get("id").toString());

            logger.info("Newly created project is " + newlyCreatedProjectId);


            //Applying filter on  AE listing to pick the first document in completed state
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            softAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
            JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

            int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
            int documentId = Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);


            /*Now Linking this project with a document in AE doc listing- Test Case Id: C89555*/
            query = "/autoextraction/updateProperties";
            payload = "{\"contractDocumentId\":\""+documentId+"\",\"projectIds\":["+newlyCreatedProjectId+"],\"groupIds\":[],\"tagIds\":[]}";
            HttpResponse updateProjectResponse = AutoExtractionHelper.updateProperties(query,payload);
            softAssert.assertTrue(updateProjectResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");

                // Now Goto that document and check only these two or one out of two should be extracted (Test case Id:C141360)
                query = "/listRenderer/list/433/listdata?isFirstCall=false";
                payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"367\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":367,\"filterName\":\"fieldId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"385\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":385,\"filterName\":\"projectids\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":\""+documentId+"\"}";
                HttpResponse metadataResponse = AutoExtractionHelper.autoExtractionMetaDataApi(query,payload);
                softAssert.assertTrue(metadataFieldResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String metadataResponseStr = EntityUtils.toString(metadataResponse.getEntity());
                JSONObject metadataResponseJson = new JSONObject(metadataResponseStr);

            int metadataRecord = metadataResponseJson.getJSONArray("data").length();
            columnId = ListDataHelper.getColumnIdFromColumnName(metadataResponseStr, "fieldname");
            List<Integer> extractedMetadataFields = new LinkedList<>();

            for (int i = 0; i < metadataRecord; i++)
            {
                JSONObject metadataObj = metadataResponseJson.getJSONArray("data").getJSONObject(i);
                String metadataFieldValue = metadataObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] metadataFieldIds = metadataFieldValue.split(":;");
                extractedMetadataFields.add(Integer.valueOf(metadataFieldIds[1]));
            }
            //Removing the duplicate metadataFields Ids from the list
            List<Integer> uniqueMetadataFieldsIds = extractedMetadataFields.stream().distinct().collect(Collectors.toList());

            boolean mappedFieldsExtraction;
            for(int i=0;i<uniqueMetadataFieldsIds.size();i++) {
                if (uniqueMetadataFieldsIds.get(i) == 12485 || uniqueMetadataFieldsIds.get(i) == 12486)
                {
                    mappedFieldsExtraction = true;
                }
                else
                    {
                    mappedFieldsExtraction = false;
                    }


                softAssert.assertTrue(mappedFieldsExtraction == true, "Metadata Fields that are not mapped to a Project are getting extracted");
            }
                //Test Case to validate only these two metadata fields should be present in Metadata Field filter of Contracts Tab- Test Case Id: C89565

                query = "/listRenderer/list/433/filterData";
                payload = "{\"entityId\":11237,\"entityTypeId\":316}";
                HttpResponse metadataFieldsInFilterResponse = AutoExtractionHelper.metadataFilterAPI(query, payload);
                softAssert.assertTrue(metadataFieldsInFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String metadataFilterStr = EntityUtils.toString(metadataFieldsInFilterResponse.getEntity());
                JSONObject metadataFieldsJson = new JSONObject(metadataFilterStr);

                List<Integer> metadataFieldsId = new LinkedList<>();
                int metadataFieldsCount = metadataFieldsJson.getJSONObject("367").getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA").length();
                for (int j = 0; j < metadataFieldsCount; j++)
                {
                    String metadataFieldId= (String) metadataFieldsJson.getJSONObject("367").getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA").getJSONObject(j).get("id");
                    metadataFieldsId.add(Integer.valueOf(metadataFieldId));
                }

                boolean mappedFieldsInFilter;
                for (int k = 0; k < metadataFieldsId.size(); k++)
                {
                    if (metadataFieldsId.get(k) == 12485 || metadataFieldsId.get(k) == 12486) {
                        mappedFieldsInFilter = true;
                    } else {
                        mappedFieldsInFilter = false;
                    }

                    softAssert.assertTrue(mappedFieldsInFilter == true, "Metadata Fields that are not mapped to a Project are getting reflected in Filter");


                }
                //Category Filter working fine in Clause tab
                query = "/listRenderer/list/493/listdata?isFirstCall=false";
                payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1050\",\"name\":\"CONFIDENTIALITY\"}]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"0\"}}},\"entityId\":\""+documentId+"\"}";
                HttpResponse categoryFilterResponse = AutoExtractionHelper.metadataFilterAPI(query,payload);
                softAssert.assertTrue(categoryFilterResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String categoryFilterStr = EntityUtils.toString(categoryFilterResponse.getEntity());
                JSONObject categoryFilterJson = new JSONObject(categoryFilterStr);
                int categoryRecord = categoryFilterJson.getJSONArray("data").length();
                columnId = ListDataHelper.getColumnIdFromColumnName(categoryFilterStr, "name");

                List<String> categoryNames = new LinkedList<>();

                for (int m = 0; m < categoryRecord; m++)
                {
                    JSONObject categoryObj = categoryFilterJson.getJSONArray("data").getJSONObject(m);
                    String categoryValue = categoryObj.getJSONObject(Integer.toString(columnId)).getString("value");
                    String[] categoryFieldNames = categoryValue.split(":;");
                    categoryNames.add(String.valueOf(categoryFieldNames[0]));

                }
            //Removing the duplicate categoryNames from the list
            List<String> uniqueCategoryName = categoryNames.stream().distinct().collect(Collectors.toList());
                softAssert.assertTrue(uniqueCategoryName.contains("Confidentiality"),"Showing categories other than applied filter");


        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting Project Fields mapping API");
            softAssert.assertTrue(false,e.getMessage());

        }

        softAssert.assertAll();

    }


}
