package com.sirionlabs.test.autoExtraction;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class autoExtractionSearch extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoExtractionSmoke.class);
    private AutoExtractionHelper autoExtractionHelperObj=new AutoExtractionHelper();


    //TC :C152332: To Verify the Searching by Document Name
    @Test
    public void testDocumentSearch()
    {
        CustomAssert customAssert=new CustomAssert();
        try {
            logger.info("Validating Document Search");
            String searchFileName="Doc File API Automation.doc";

            String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection" +
                    "\":\"desc nulls last\",\"filterJson\":{},\"searchName\":\"\\\"" + searchFileName + "\\\"\"},\"selectedColumns" +
                    "\":[{\"columnId\":16797,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":17296,\"columnQueryName\":\"id\"}," +
                    "{\"columnId\":16369,\"columnQueryName\":\"documentname\"},{\"columnId\":16454,\"columnQueryName\":\"projects\"}," +
                    "{\"columnId\":17345,\"columnQueryName\":\"batch\"},{\"columnId\":17289,\"columnQueryName\":\"doctags\"}," +
                    "{\"columnId\":17332,\"columnQueryName\":\"clusters\"},{\"columnId\":16371,\"columnQueryName\":\"status\"}]}";
            
            logger.info("Validate Search operation for Document");
            HttpResponse docNameSearchResponse = autoExtractionHelperObj.localSearch(payload);
            customAssert.assertTrue(docNameSearchResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            logger.info("Validate search results with searched document");
            String listDataResponse = EntityUtils.toString(docNameSearchResponse.getEntity());
            //Validate Document Name of the list to the searched Doc Name
            JSONObject searchResultJson = new JSONObject(listDataResponse);
            int totalRecord = searchResultJson.getJSONArray("data").length();
            int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "documentname");
            List<String> allFilesToValidate=new ArrayList<String>();

            for (int i = 0; i < totalRecord; i++)
            {
                JSONObject searchResultObj = searchResultJson.getJSONArray("data").getJSONObject(i);
                String[] docValue = searchResultObj.getJSONObject(Integer.toString(columnId)).getString("value").split(":;");
                allFilesToValidate.add(String.valueOf(docValue[0]).trim());
            }
            logger.info("Validating File name from the list for the searched file "+searchFileName);
            for(String file:allFilesToValidate)
            {
                customAssert.assertEquals(file, searchFileName, ""+file+" File name doesn't matches to searched fileName"+searchFileName);
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Validating Document Search TC :C152332 " +e.getMessage());
        }
        customAssert.assertAll();

    }
// TC: C152116: To Verify the search giving the results of Clauses
    @Test
    public void testClauseSearch() throws IOException {
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.info( "Starting Test C152116 :Validating Clause Text Search");
            String Text="If only a portion of any Information falls within one of the foregoing exceptions, the remainder shall continue to be subject to the prohibitions and restrictions set out in this Agreement.";
            String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{},\"searchName\":\"\\\""+Text+" \\\"\"},\"selectedColumns\":[]}";
            HttpResponse clauseTextSearchResponse = autoExtractionHelperObj.localSearch(payload);
            customAssert.assertTrue(clauseTextSearchResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String listDataResponse = EntityUtils.toString(clauseTextSearchResponse.getEntity());

            //Validate Text Search Result for top most record
            JSONObject searchResultJson = new JSONObject(listDataResponse);
            int totalRecord = searchResultJson.getJSONArray("data").length();
            logger.info("Total search results found: " + totalRecord);
            int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            logger.info("Validating search result for top most Record");
            JSONObject searchResultObj = searchResultJson.getJSONArray("data").getJSONObject(0);
            String[] idValue = searchResultObj.getJSONObject(Integer.toString(columnId)).getString("value").split(":;");

            int docId = Integer.parseInt(idValue[0]);
            logger.info("Hitting Document View API for AE" + docId);
            int recordId = Integer.parseInt(idValue[1]);
            HttpResponse docViewerResponse = AutoExtractionHelper.docShowPage(recordId);
            String viewPageResponse = EntityUtils.toString(docViewerResponse.getEntity());

            JSONObject textSearchResultJson = new JSONObject(viewPageResponse);

            String clauseCount = textSearchResultJson.getJSONObject("response").get("clauseCount").toString();
            customAssert.assertTrue(Integer.parseInt(clauseCount)>0,"Clause count is 0 where it was supposed to be Non Zero.");
            String clausePayload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                    "\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null," +
                    "\"entityFieldId\":null},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"0\"}}},\"entityId\":"+recordId+"}";
                HttpResponse clauseListDataResponse = AutoExtractionHelper.getTabData(clausePayload, 493);
                String allClauseListDataResponse = EntityUtils.toString(clauseListDataResponse.getEntity());
                JSONObject clauseSearchResultJson = new JSONObject(allClauseListDataResponse);
                JSONArray jsonArr = clauseSearchResultJson.getJSONArray("data");
                int totalClause = jsonArr.length();
                List<String> clauseListData = new ArrayList<>();
                for (int i = 0; i < totalClause; i++) {
                    clauseSearchResultJson = jsonArr.getJSONObject(i);
                    for (String columnID : JSONObject.getNames(clauseSearchResultJson)) {
                        if (clauseSearchResultJson.getJSONObject(columnID).getString("columnName").trim().equalsIgnoreCase("text")) {
                            String value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnID)).getString("value");
                            clauseListData.add(value.trim());
                        }
                    }
                }
                logger.info("Validating whether Search text is present in  clause Text list");
                if (!(clauseListData.contains(Text))) {
                    customAssert.assertTrue(false, "Searched Text do not match the any of the clause list for the searched Text " + Text + "and ID AE" + docId);
                }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Validating Clause Text Search TC: C152116 because of"+ e.getMessage());
        }
        customAssert.assertAll();

    }
    /* TC :C152433: Single Entity Search
     */
    @Test
    public void testGlobalSearchForSingleEntity()
    {
        CustomAssert csAssert=new CustomAssert();
        try{
            logger.info("Start Test:Validate Single Entity ID Search.");
            logger.info("Hitting listdata API for Entity type Automation");
            HttpResponse automationListResponse = AutoExtractionHelper.getListDataForEntities("432", "{\"filterMap\":{}}");
            csAssert.assertTrue(automationListResponse.getStatusLine().getStatusCode() == 200, "Automation List Data Response Code is not valid");
            String automationListStr = EntityUtils.toString(automationListResponse.getEntity());
            JSONObject automationJsonObj = new JSONObject(automationListStr);
            JSONArray jsonArr = automationJsonObj.getJSONArray("data");
            int Count = automationJsonObj.getJSONArray("data").length();
            logger.info("Checking if list data count is greater than 0");
            csAssert.assertTrue(Count>0,"There is no Data in Automation Listing");
            int columnId = ListDataHelper.getColumnIdFromColumnName(automationListStr, "id");
            JSONObject listdata= jsonArr.getJSONObject(0);
            String[] idValue = listdata.getJSONObject(Integer.toString(columnId)).getString("value").split(":;");
            int docId = Integer.parseInt(idValue[0]);
            int recordId=Integer.parseInt(idValue[1]);
            logger.info("Validating search operation for Entity ID AE"+docId);
            HttpResponse singleEntitySearchResponse=autoExtractionHelperObj.globalSingleEntitySearch(docId);
            String singleEntitySearchResult= EntityUtils.toString(singleEntitySearchResponse.getEntity()).trim();
            logger.info("validating search result resonse for  Entity ID AE"+docId);
            csAssert.assertEquals(recordId,Integer.parseInt(singleEntitySearchResult),"Search Result doesn't match to recordId of AE"+docId);
            HttpResponse metadataResponse=autoExtractionHelperObj.hitExtractDocViewerDocId(recordId);
            logger.info("Validating extract metadata API Response");
            csAssert.assertTrue(metadataResponse.getStatusLine().getStatusCode()==200,"Metadata Response code is not valid");
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Single Entity Search TC:C152433" +e.getMessage());
        }
        csAssert.assertAll();
    }

    /* TC :C152333: Multiple Entity Search
     */
    @Test
    public void testGlobalSearchForMultipleEntity()
    {
        CustomAssert csAssert=new CustomAssert();
        try{
            logger.info("Start Test: Validate Multiple Entity Id search.");
            logger.info("Hitting listdata API for Entity type Automation");
            HttpResponse automationListResponse = AutoExtractionHelper.getListDataForEntities("432", "{\"filterMap\":{}}");
            csAssert.assertTrue(automationListResponse.getStatusLine().getStatusCode() == 200, "Automation List Data Response Code is not valid");
            String automationListStr = EntityUtils.toString(automationListResponse.getEntity());
            JSONObject automationJsonObj = new JSONObject(automationListStr);
            JSONArray jsonArr = automationJsonObj.getJSONArray("data");
            ArrayList<Integer> entityIDtoValidate=new ArrayList<>();
            int count = jsonArr.length();
            logger.info("Checking if list data count is greater than 0");
            csAssert.assertTrue(count>0,"There is no Data in Automation Listing");
            for(int i=0;i<2;i++) {
                int columnId = ListDataHelper.getColumnIdFromColumnName(automationListStr, "id");
                JSONObject listdata= jsonArr.getJSONObject(i);
                String[] idValue = listdata.getJSONObject(Integer.toString(columnId)).getString("value").trim().split(":;");
                int docId = Integer.parseInt(idValue[0]);
                entityIDtoValidate.add(docId);
            }
            logger.info("Validating multiple search operation for AE"+entityIDtoValidate.get(0)+" and AE"+entityIDtoValidate.get(1));
            String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"448\":{\"filterId\":\"448\",\"filterName\":\"entityidsfilter\"," +
                    "\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\""+entityIDtoValidate.get(0)+","+entityIDtoValidate.get(1)+"\"}}},\"selectedColumns\":[{\"columnId\":16797," +
                    "\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":17296,\"columnQueryName\":\"id\"},{\"columnId\":16369,\"columnQueryName\":\"documentname\"}," +
                    "{\"columnId\":16454,\"columnQueryName\":\"projects\"},{\"columnId\":17345,\"columnQueryName\":\"batch\"},{\"columnId\":17289,\"columnQueryName\":\"doctags\"}," +
                    "{\"columnId\":17332,\"columnQueryName\":\"clusters\"},{\"columnId\":16371,\"columnQueryName\":\"status\"}]}";

            HttpResponse searchResultResponse=autoExtractionHelperObj.globalMultipleEntitySearch(payload,432);

            String searchResultResponseStr = EntityUtils.toString(searchResultResponse.getEntity());
            JSONObject searchJsonObj = new JSONObject(searchResultResponseStr);
            JSONArray searchJsonArr = searchJsonObj.getJSONArray("data");
            int searchResultCount=searchJsonArr.length();
            csAssert.assertEquals(entityIDtoValidate.size(),searchResultCount,"searched entity id count do not match to search result count");
            ArrayList<Integer> actualResult=new ArrayList<>();
            for(int j=0;j<searchResultCount;j++)
            {
                int columnId = ListDataHelper.getColumnIdFromColumnName(automationListStr, "id");
                JSONObject listdata= jsonArr.getJSONObject(j);
                String[] idValue = listdata.getJSONObject(Integer.toString(columnId)).getString("value").trim().split(":;");
                int docId = Integer.parseInt(idValue[0]);
                actualResult.add(docId);
            }
            logger.info("Validating Actual results with Expected Search result.");
            Collections.sort(actualResult);
            Collections.sort(entityIDtoValidate);
            csAssert.assertEquals(actualResult,entityIDtoValidate,"Search Result doesn't match to  of AE"+entityIDtoValidate.get(0)+","+entityIDtoValidate.get(1));

        }
       catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Multiple Entity Search TC:C152333 " +e.getMessage());
        }
        csAssert.assertAll();
    }
}
