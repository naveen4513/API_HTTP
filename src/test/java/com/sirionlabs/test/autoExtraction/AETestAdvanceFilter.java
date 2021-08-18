package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

public class AETestAdvanceFilter {
    private final static Logger logger = LoggerFactory.getLogger(AETestAdvanceFilter.class);

    //C153204: End User: Advance Filter- Clause Text Filter
    @Test
    public void testClauseTextFilter()
    {
        CustomAssert customAssert=new CustomAssert();
        try{
            logger.info("Start Test: Validating Clause Text Filter");
            String clauseText="Assignment. This Agreement shall not be assignable by either party, except for an assignment accompanying a transfer of the business to which this Agreement pertains or to a parent corporation or affiliate under common ownership with the transferring party, without the written consent of the other party, which consent shall not be unreasonably withheld.";
            String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"456\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1062\",\"name\":\"Assignment\",\"value\":\""+clauseText+"\"}]}," +
                    "\"filterId\":456,\"filterName\":\"clauseText\",\"uitype\":\"KEYVALUE\"}}},\"selectedColumns\":[]}";
            logger.info("Hitting list data API after applying Clause Text Filter for Category Name Assignment");
            HttpResponse listDataResponse= AutoExtractionHelper.automationListing(payload);
            customAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List data API Response code is not valid");
            String listDataResponseStr= EntityUtils.toString(listDataResponse.getEntity());
            JSONObject listDataResponseJson=new JSONObject(listDataResponseStr);
            int idColumn= ListDataHelper.getColumnIdFromColumnName(listDataResponseStr,"id");
            int documentCount=listDataResponseJson.getJSONArray("data").length();
            customAssert.assertTrue(documentCount>0,"No filter results found for Clause Text filter for category Name Assignment");

                logger.info("Validating top most record from filtered List data");
                String[] IdValue=listDataResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(idColumn)).get("value").toString().split(":;");
                String documentId=IdValue[1];
                logger.info("Hitting Clause Tab list data API");
                HttpResponse clauseListData=AutoExtractionHelper.clauseTabListing(Integer.parseInt(documentId));
                customAssert.assertTrue(clauseListData.getStatusLine().getStatusCode()==200,"Clause tab List data API is not Valid");
                String clauseListDataStr=EntityUtils.toString(clauseListData.getEntity());
                JSONObject clauseListDataJson=new JSONObject(clauseListDataStr);
                int textColumnId=ListDataHelper.getColumnIdFromColumnName(clauseListDataStr,"text");
                int count=clauseListDataJson.getJSONArray("data").length();
                customAssert.assertTrue(count>0,"Clause count is 0 on show page of document Id "+documentId);
                List<String> allClauseListData=new ArrayList<>();
                for(int i=0;i<count;i++)
                {
                    String textValue=clauseListDataJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(textColumnId)).get("value").toString();
                    allClauseListData.add(textValue);
                }

                logger.info("Validating clause Text in clause list tab for filter result for document id "+documentId);
                customAssert.assertTrue(allClauseListData.contains(clauseText),"Filtered Clause Text does not match to Clause List data for record Id "+documentId);
        }
        catch (Exception e)
        {
            logger.info("Exception while validating Advance Filter-clause Text filter");
            customAssert.assertTrue(false,"Exception while validating Advance Filter");
        }
        customAssert.assertAll();
    }

    //C153205: End User: Advance Filter-Metadata Text Filter.
    @Test
    public void testMetadataTextFilter()
    {
        CustomAssert customAssert=new CustomAssert();
        try{
            {
                logger.info("Start Test: Validating Metadata Text Filter");
                String metadataText="Clinical Research Organization Master Services Agreement";
                String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"457\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"12484\",\"name\":\"Agreement Type\",\"value\":\""+metadataText+"\"}]},\"filterId\":457,\"filterName\":\"extractedText\",\"uitype\":\"KEYVALUE\"}}},\"selectedColumns\":[]}";
                logger.info("Hitting list data API after applying metadata Text Filter for field  Name " +metadataText);
                HttpResponse listDataResponse= AutoExtractionHelper.automationListing(payload);
                customAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List data API Response code is not valid");
                String listDataResponseStr= EntityUtils.toString(listDataResponse.getEntity());
                JSONObject listDataResponseJson=new JSONObject(listDataResponseStr);
                int idColumn= ListDataHelper.getColumnIdFromColumnName(listDataResponseStr,"id");
                int documentCount=listDataResponseJson.getJSONArray("data").length();
                customAssert.assertTrue(documentCount>0,"No filter results found for metadata Text filter for field name "+metadataText);

                logger.info("Validating top most record from filtered List data");
                String[] IdValue=listDataResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(idColumn)).get("value").toString().split(":;");
                String documentId=IdValue[1];
                logger.info("Hitting metadata Tab list data API");
                String metadataPayload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"367\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":367,\"filterName\":\"fieldId\"},\"385\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":385,\"filterName\":\"projectids\"},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":"+documentId+"}";
                HttpResponse metadataListData=AutoExtractionHelper.getTabData(metadataPayload,433);
                customAssert.assertTrue(metadataListData.getStatusLine().getStatusCode()==200,"Clause tab List data API is not Valid");
                String metadataListDataStr=EntityUtils.toString(metadataListData.getEntity());
                JSONObject metadataListDataJson=new JSONObject(metadataListDataStr);
                int textColumnId=ListDataHelper.getColumnIdFromColumnName(metadataListDataStr,"extractedtext");
                int count=metadataListDataJson.getJSONArray("data").length();
                customAssert.assertTrue(count>0,"metadata Count is 0 on show page for the document Id "+documentId);
                List<String> allMetadataListData=new ArrayList<>();
                for(int i=0;i<count;i++)
                {
                    String textValue=metadataListDataJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(textColumnId)).get("value").toString();
                    allMetadataListData.add(textValue);
                }

                logger.info("Validating metadata Text in metadata list tab for filter result for document id "+documentId);
                //customAssert.assertTrue(allMetadataListData.contains(metadataText),"Filtered metadata Text does not match to Metadata List data for record Id "+documentId);
                customAssert.assertTrue(allMetadataListData.stream().anyMatch(metadataText::equalsIgnoreCase),"Filtered metadata Text does not match to Metadata List data for record Id "+documentId);


            }

        }
        catch (Exception e)
        {
            logger.info("Exception while validating Advance Filter-metadata Text filter");
            customAssert.assertTrue(false,"Exception while validating Advance Filter-metadata Text filter");
        }
        customAssert.assertAll();
    }

}
