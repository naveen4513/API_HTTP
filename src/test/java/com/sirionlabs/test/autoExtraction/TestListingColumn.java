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

import java.util.LinkedList;
import java.util.List;

public class TestListingColumn {
    private final static Logger logger = LoggerFactory.getLogger(TestListingColumn.class);

    //TC: C153132:Verify Reference Documents Column on AE listing
    @Test
    public void testC153132()
    {
        logger.info("Start Test C153132: Verify Reference Docs Column Name");
        CustomAssert customAssert = new CustomAssert();
        try{
            HttpResponse docListMetaDataResponse = AutoExtractionHelper.checkAutoExtractionDocListingMetaData("/listRenderer/list/432/defaultUserListMetaData", "{}");
            customAssert.assertTrue(docListMetaDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not Valid");
            String listingDataResponseStr = EntityUtils.toString(docListMetaDataResponse.getEntity());
            JSONObject jsonObject = new JSONObject(listingDataResponseStr);
            int columnsLength = jsonObject.getJSONArray("columns").length();
            logger.info("Getting all Columns from List Page");
            List<String> allDefaultColumnsFromResponse = new LinkedList<>();
            for (int i = 0; i < columnsLength; i++) {
                allDefaultColumnsFromResponse.add(jsonObject.getJSONArray("columns").getJSONObject(i).get("queryName").toString());
            }
            logger.info("Validating reference documents Columns on listing page");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("referencedocs"), "Reference Documents Column Name is not Present");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("referencedocs"), "Reference Documents Column Name is not Present");

        }
        catch(Exception e){
            logger.info("Error occurred while while validating column data");
            customAssert.assertTrue(false,"Error occurred while while validating Reference Documents Column TC:C153132"+e.getMessage());
        }
        customAssert.assertAll();
    }

    //TC:C153252 :End user: Verify parent reference column on AE Listing page
    //TC:C153691:End user: Verify Duplicate Docs Column on listing page
    @Test
    public void testC153252()
    {
        logger.info("Start Test C153252 : Verify Parent Reference Column Name on listing page");
        CustomAssert customAssert = new CustomAssert();
        try{
            HttpResponse docListMetaDataResponse = AutoExtractionHelper.checkAutoExtractionDocListingMetaData("/listRenderer/list/432/defaultUserListMetaData", "{}");
            customAssert.assertTrue(docListMetaDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not Valid");
            String listingDataResponseStr = EntityUtils.toString(docListMetaDataResponse.getEntity());
            JSONObject jsonObject = new JSONObject(listingDataResponseStr);
            int columnsLength = jsonObject.getJSONArray("columns").length();
            logger.info("Getting all Columns from List Page");
            List<String> allDefaultColumnsFromResponse = new LinkedList<>();
            for (int i = 0; i < columnsLength; i++) {
                allDefaultColumnsFromResponse.add(jsonObject.getJSONArray("columns").getJSONObject(i).get("queryName").toString());
            }
            logger.info("Validating Parent reference Columns on listing page");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("parentreference"), "Parent Reference Column Name is not Present");
            logger.info("Validating Contract Id column on AE listing page: " + "Test Case Id: C153711 ");
            logger.info("Validating duplicate Docs Column on AE Listing page.");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("duplicatedocument"),"Duplicate Docs Column Name is not present on AE Listing Page");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("contractid"), " Column Name is not Present");
            logger.info("Validating Contract Name column on AE listing page "+ "Test Case Id: C153711 ");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("contractname"), " Column Name is not Present");

            logger.info("Validating Page Similarity Score column on AE listing page "+ "Test Case Id: C154597 ");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("maxpagesimilarityscore"), " Page Similarity Score Column Name is not Present");

            logger.info("Validating  Maximum Similarity Score column on AE listing page "+ "Test Case Id: C154596 ");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("maxsimilarityscore"), "  Maximum Similarity Score Column Name is not Present");

        }
        catch(Exception e){
            logger.info("Error occurred while while validating column data");
            customAssert.assertTrue(false,"Error occurred while while validating parent reference Column TC C153252 "+e.getMessage());

        }
        customAssert.assertAll();
    }

    //TC: C153947: End User: Verify page count value is not blank
    @Test
    public void testPageCount()
    {
        CustomAssert customAssert = new CustomAssert();
        try
        {
            logger.info("Start test TC: C153947");
            logger.info("Hitting Automation listing API ");
            HttpResponse listDataResponse=AutoExtractionHelper.aeDocListing();
            customAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List data API Response code is invalid");
            String listDataResponseStr=EntityUtils.toString(listDataResponse.getEntity());
            int pageCountColumnId= ListDataHelper.getColumnIdFromColumnName(listDataResponseStr,"totalpages");
            int recordColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            JSONObject jsonObj=new JSONObject(listDataResponseStr);
            int count = jsonObj.getJSONArray("data").length();
            for(int i=0;i<count;i++)
            {
                String[] idValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
                String docId = idValue[1];
                String value=jsonObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(pageCountColumnId)).getString("value");
                logger.info("Validating page count column value for document Id "+docId);
                customAssert.assertFalse(value.equalsIgnoreCase("null"),"Page count value is null for document id "+docId);
                customAssert.assertTrue(Integer.parseInt(value)>=-1,"Page count value is other than integer value for document id "+docId);
            }
        }
        catch (Exception e)
        {
            logger.info("Error occurred while while validating PAGE COUNT Value due to "+e.getMessage());
            customAssert.assertTrue(false,"Error occurred while while validating page Count value TC C153947 "+e.getMessage());
        }
        customAssert.assertAll();

    }

}
