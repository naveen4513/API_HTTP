package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Entities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidateCountOnListing
{
    private final static Logger logger = LoggerFactory.getLogger(ValidateCountOnListing.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;

    /*Test Case to validate count of metadata and clause on Doc Viewer is equal to count on AE doc listing page*/
    @Test
    public void validateExtractedCountOfDoc() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try {
            //Applying filter on AE doc listing to get documents having status : Completed
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
            HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            csAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
            JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

            //Getting Document Id out of filtered List of Documents
            int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
            int documentId= Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

            //Getting Count of Metadata for the DocumentId
            columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "metadatacount");
            int metadataCount= Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString());

            //Getting Count of Clauses for the DocumentId
            columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "clausecount");
            int clauseCount= Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString());

            //Validating the Metadata Count on Document Viewer
            query = "/listRenderer/list/433/listdata?isFirstCall=false";
            payload= "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":\""+documentId+"\"}";
            HttpResponse metadataTabResponse = AutoExtractionHelper.hitTabdataAPI(query,payload);
            csAssert.assertTrue(metadataTabResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String metadataTabStr = EntityUtils.toString(metadataTabResponse.getEntity());
            JSONObject metadataTabJson = new JSONObject(metadataTabStr);

            //Extracting Total Count of Metadata Fields in Contracts/Metadata Tab

            int metadataRecord = metadataTabJson.getJSONArray("data").length();
            columnId = ListDataHelper.getColumnIdFromColumnName(metadataTabStr, "fieldname");
            List<String> extractedMetadataFields = new LinkedList<>();

            for (int i = 0; i < metadataRecord; i++)
            {
                JSONObject metadataObj = metadataTabJson.getJSONArray("data").getJSONObject(i);
                String metadataFieldValue = metadataObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] metadataField = metadataFieldValue.split(":;");
                extractedMetadataFields.add(String.valueOf(metadataField[0]));
            }

            //Removing the duplicate metadataFields from the list
            List<String> uniqueMetadataFields = extractedMetadataFields.stream().distinct().collect(Collectors.toList());

            int extractedMetadataFieldsCount = uniqueMetadataFields.size();
            csAssert.assertTrue(metadataCount==extractedMetadataFieldsCount,"There is a count mismatch in Metadata Count on doc viewer and on AE doc listing");

            //Extracting Total Count of Categories in Clause Tab
            query = "/listRenderer/list/493/listdata?isFirstCall=false";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":\""+documentId+"\"}";
            HttpResponse clauseTabResponse = AutoExtractionHelper.hitTabdataAPI(query,payload);
            csAssert.assertTrue(clauseTabResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String clauseTabStr = EntityUtils.toString(clauseTabResponse.getEntity());
            JSONObject clauseTabJson = new JSONObject(clauseTabStr);

            //Extracting Total Count of Metadata Fields in Contracts/Metadata Tab

            int clauseRecord = clauseTabJson.getJSONArray("data").length();
            columnId = ListDataHelper.getColumnIdFromColumnName(clauseTabStr, "name");
            List<String> extractedClauses = new LinkedList<>();

            for (int i = 0; i < clauseRecord; i++)
            {
                JSONObject clauseObj = clauseTabJson.getJSONArray("data").getJSONObject(i);
                String clauseValue = clauseObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String[] clauses = clauseValue.split(":;");
                extractedClauses.add(String.valueOf(clauses[0]));
            }

            //Removing the duplicate clauses from the list
            List<String> uniqueClauses = extractedClauses.stream().distinct().collect(Collectors.toList());

            int extractedClauseCount = uniqueClauses.size();
            csAssert.assertTrue(clauseCount==extractedClauseCount,"There is a count mismatch in CLause Count on doc viewer and on AE doc listing");
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting FilterData API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();


    }
        @Test
        public void validateStatusOfDocument() throws IOException
        {
            CustomAssert csAssert = new CustomAssert();            try
            {
                String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
                HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
                csAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
                String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
                JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

                //Getting Document Id out of filtered List of Documents
                int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
                int documentId= Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

                query= "/" +"autoextraction"+ "/"+"document"+"/"+documentId + "/metadata";
                HttpResponse getStatusOfDocument = AutoExtractionHelper.getStatusOfDocumentAPI(query);
                csAssert.assertTrue(getStatusOfDocument.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String getStatusOfDocumentStr = EntityUtils.toString(getStatusOfDocument.getEntity());
                JSONObject getStatusOfDocumentJson = new JSONObject(getStatusOfDocumentStr);
                String statusOfDoc = String.valueOf(getStatusOfDocumentJson.getJSONObject("response").getJSONObject("autoExtractStatus").get("name"));
                boolean isStatusCompleted;
                if(statusOfDoc.contains("SUBMITTED")||statusOfDoc.contains("INPROGRESS")||statusOfDoc.contains("FAILED"))
                {
                    isStatusCompleted=false;
                }
                else {
                    isStatusCompleted=true;
                }
                csAssert.assertTrue(isStatusCompleted==true,"Status of Document is not completed");


            }
            catch (Exception e)
            {
                logger.info("Error occured while hitting metadata of doc API");
                csAssert.assertTrue(false,e.getMessage());

            }
            csAssert.assertAll();

        }

}
