package com.sirionlabs.test.autoExtraction;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import net.bytebuddy.utility.RandomString;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.RandomUtils;
import org.json.JSONObject;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class ClauseDevaitionAE849
{
    private final static Logger logger = LoggerFactory.getLogger(ClauseDevaitionAE849.class);
    CustomAssert csAssert = new CustomAssert();

    /*Test Case to check clause deviation listing - Test Case Id: C91091,C141098*/

    @Test(enabled = true)
    public void clauseDeviationListing() throws IOException
    {

        CustomAssert csAssert = new CustomAssert();        try
        {
            String query = "/listRenderer/list/515/listdata?version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse clauseDevaitionListingResponse = AutoExtractionHelper.clauseDeviationListing(query,payload);
            csAssert.assertTrue(clauseDevaitionListingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String clauseDeviationListStr = EntityUtils.toString(clauseDevaitionListingResponse.getEntity());
            JSONObject clauseDeviationListingJson =  new JSONObject(clauseDeviationListStr);
            int dataInListing = clauseDeviationListingJson.getJSONArray("data").length();
            csAssert.assertTrue(!(dataInListing ==0),"There is no data in Deviation Listing");

            //Checking Default Columns in Clause Deviation Listing
            List<String> allDefaultColumns = new LinkedList<>();

            Map<String,Object> rowData = clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                    RandomUtils.nextInt(0,clauseDeviationListingJson.getJSONArray("data").length())).toMap();

            for(Map.Entry<String,Object> data  : rowData.entrySet()){
                allDefaultColumns.add(clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                        RandomUtils.nextInt(0,clauseDeviationListingJson.getJSONArray("data").length())).getJSONObject(data.getKey()).get("columnName").toString());
            }

            csAssert.assertTrue(allDefaultColumns.contains("text"),"TEXT column is not present");
            csAssert.assertTrue(allDefaultColumns.contains("document"),"Document column is not present");
            csAssert.assertTrue(allDefaultColumns.contains("projects"),"Project column is not present");
            csAssert.assertTrue(allDefaultColumns.contains("category"),"Category column is not present");
            csAssert.assertTrue(allDefaultColumns.contains("fields"),"Fields column is not present");


        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting Clause Deviation listing API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();
    }

    /*Verify that Column Filter is working fine* Test Case Id: C141099*/
    @Test
    public void columnUncheck() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();

        try
        {
            //Without applying any Filter, the list of default filters
            String query = "/listRenderer/list/515/listdata?version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse clauseDevaitionListingResponse = AutoExtractionHelper.clauseDeviationListing(query,payload);
            csAssert.assertTrue(clauseDevaitionListingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String clauseDeviationListStr = EntityUtils.toString(clauseDevaitionListingResponse.getEntity());
            JSONObject clauseDeviationListingJson =  new JSONObject(clauseDeviationListStr);
            int dataInListing = clauseDeviationListingJson.getJSONArray("data").length();
            csAssert.assertTrue(!(dataInListing ==0),"There is no data in Deviation Listing");

            //Checking Default Columns in Clause Deviation Listing
            List<String> allDefaultColumns = new LinkedList<>();

            Map<String,Object> rowData = clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                    RandomUtils.nextInt(0,clauseDeviationListingJson.getJSONArray("data").length())).toMap();

            for(Map.Entry<String,Object> data  : rowData.entrySet())
            {
                allDefaultColumns.add(clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                        RandomUtils.nextInt(0,clauseDeviationListingJson.getJSONArray("data").length())).getJSONObject(data.getKey()).get("columnName").toString());
            }

            //Deselect any column and check it should not show up in Deviation listing
            query="/listRenderer/list/515/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":19473,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":19475,\"columnQueryName\":\"document\"},{\"columnId\":19476,\"columnQueryName\":\"projects\"},{\"columnId\":19477,\"columnQueryName\":\"category\"},{\"columnId\":19478,\"columnQueryName\":\"fields\"}]}";
            clauseDevaitionListingResponse = AutoExtractionHelper.clauseDeviationListing(query,payload);
            csAssert.assertTrue(clauseDevaitionListingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            clauseDeviationListStr = EntityUtils.toString(clauseDevaitionListingResponse.getEntity());
            clauseDeviationListingJson =  new JSONObject(clauseDeviationListStr);

            List<String> columnWithoutText = new LinkedList<>();
            rowData = clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                    RandomUtils.nextInt(0,clauseDeviationListingJson.getJSONArray("data").length())).toMap();

            for(Map.Entry<String,Object> data  : rowData.entrySet())
            {
                columnWithoutText.add(clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                            RandomUtils.nextInt(0,clauseDeviationListingJson.getJSONArray("data").length())).getJSONObject(data.getKey()).get("columnName").toString());
                }

            csAssert.assertTrue(columnWithoutText.size()<allDefaultColumns.size(),
                    "After unchecking column it is still showing the same in AE doc listing");
            csAssert.assertTrue(!columnWithoutText.contains("TEXT"),"It should not contain TEXT column");

        }

        catch (Exception e)
        {
            logger.info("Exception occurred while hitting clause deviation API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();

    }

    /*Test Case to check Filters are working fine on Deviation Listing*/
    @Test
    public void filterDeviationList()throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try
        {
           //Applying Category filter on Clause Deviation listing
            String query ="/listRenderer/list/515/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1045\",\"name\":\"TERMINATION\"}]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19473,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":19474,\"columnQueryName\":\"text\"},{\"columnId\":19475,\"columnQueryName\":\"document\"},{\"columnId\":19476,\"columnQueryName\":\"projects\"},{\"columnId\":19477,\"columnQueryName\":\"category\"},{\"columnId\":19478,\"columnQueryName\":\"fields\"}]}";
            HttpResponse listingDataResponse = AutoExtractionHelper.clauseDeviationListing(query,payload);
            csAssert.assertTrue(listingDataResponse.getStatusLine().getStatusCode()==200,"Response code is invalid");
            String listingDataResponseStr = EntityUtils.toString(listingDataResponse.getEntity());
            JSONObject listingDataResponseStrJson = new JSONObject(listingDataResponseStr);

            int dataLength = listingDataResponseStrJson.getJSONArray("data").length();
            LinkedList<String> allCategories = new LinkedList<>();

            if(dataLength>=1)
            {
                int columnId = ListDataHelper.getColumnIdFromColumnName(listingDataResponseStr, "category");

                for (int i = 0; i < dataLength; i++) {
                    allCategories.add(listingDataResponseStrJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[0]);

                }
                csAssert.assertTrue(allCategories.contains("TERMINATION"),"Category filter is not working properly");
            }
            else {
                logger.info("There is no enough data to apply filter on listing");
            }

        }
        catch (Exception e)
        {
            logger.info("Exception while hitting Filter Data API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();

    }

    @Test
    public void runDeviationAPI()throws IOException
    {
        CustomAssert csAssert = new CustomAssert();

        try
        {
            String reportName = "ClauseDeviation" + com.sirionlabs.utils.commonUtils.RandomString.getRandomAlphaNumericString(3);
            String query = "/autoExtraction/clauseDeviation/runDeviation";
            /*String payload = "{\"baseText\":\"FRACTION IV-1 PASTE SUPPLY AGREEMENT\\n \\nThis Fraction IV-1 Paste Supply Agreement is entered into, and effective as of this 3rd day of December, 2012 (“Effective Date”), by and between Baxter Healthcare\\nS.A., a Swiss entity, having a place of business at Postfach, 8010 Zurich, Switzerland (“Baxter”) and Kamada Ltd., having a place of business at 7 Sapir St. Kiryat Weizmann, Ness-Ziona 74036, Israel (“Kamada”).\\n \\nRECITALS\\n \\nWHEREAS, Kamada wishes to purchase filter-aid derived Fraction IV-I Paste (“Product”) from Baxter for further manufacturing of Alpha 1 Antitrypsin and/or\\nhuman Transferrin derived from the Product for clinical and commercial purposes; and\\n \\nWHEREAS, Baxter desires to sell available Product to Kamada for further manufacturing of Alpha 1 Antitrypsin and/or human Transferrin derived from the Product\\nfor clinical and commercial purposes upon the following conditions;\\n \\nNOW, THEREFORE, in consideration of the foregoing and the mutual promises contained herein, the parties agree as follows:\",\"textIds\":[951679],\"reportName\":\""+reportName+"\"}";*/
            //HttpResponse runDeviationResponse = AutoExtractionHelper.ruleBased(query,payload);
            //csAssert.assertTrue(runDeviationResponse.getStatusLine().getStatusCode()==200,"Response code is not valid");
            //String runDeviationStr = EntityUtils.toString(runDeviationResponse.getEntity());
            //JSONObject runDeviationJson = new JSONObject(runDeviationStr);
            //String responseMessage = String.valueOf(runDeviationJson.get("response"));
            //csAssert.assertTrue(responseMessage.contains("Deviation Runned Successfully"),"Deviation Request got failed");
            csAssert.assertAll();
        }
        catch (Exception e)
        {
            logger.info("Exception while hitting rule Based API");
            csAssert.assertTrue(false,e.getMessage());
        }
        csAssert.assertAll();

    }
    /*Test case to check all the columns are present in Deviation history Listing*/
    @Test
    public void aeRunDeviationHistory() throws IOException
    {
        try {
            String query = "/listRenderer/list/517/listdata?version=2.0&isFirstCall=true";
            String payload ="{\"filterMap\":{}}";
            HttpResponse historyListingResponse = AutoExtractionHelper.clauseDeviationListing(query,payload);
            csAssert.assertTrue(historyListingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String historyListingStr = EntityUtils.toString(historyListingResponse.getEntity());
            JSONObject clauseDeviationListingJson =  new JSONObject(historyListingStr);
            int dataInListing = clauseDeviationListingJson.getJSONArray("data").length();
            csAssert.assertTrue(!(dataInListing ==0),"There is no data in Deviation Listing");

            //Checking number of columns
            List<String> columnNames = new LinkedList<>();
            Map<String, Object> dataInrow = clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                    RandomUtils.nextInt(0, clauseDeviationListingJson.getJSONArray("data").length())).toMap();

            for (Map.Entry<String, Object> data : dataInrow.entrySet())
            {
                columnNames.add(clauseDeviationListingJson.getJSONArray("data").getJSONObject(
                        RandomUtils.nextInt(0, clauseDeviationListingJson.getJSONArray("data").length())).getJSONObject(data.getKey()).get("columnName").toString());
            }

            csAssert.assertTrue(columnNames.contains("basetext"),"Base Text column is not present in Deviation history listing");
            csAssert.assertTrue(columnNames.contains("reportname"),"Report name is not present in Deviation history listing");
            csAssert.assertTrue(columnNames.contains("status"),"Status is not present in Deviation history listing");
            csAssert.assertTrue(columnNames.contains("datecreated"),"Date Create is not present in Devaition history listing");
            csAssert.assertTrue(columnNames.contains("createdby"),"Created By is not present in Devaition history listing");
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting Deviation history Listing");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();


    }



}
