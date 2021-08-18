package com.sirionlabs.test.autoExtraction;

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

public class TestAEReportColumns {
    private final static Logger logger = LoggerFactory.getLogger(TestAEReportColumns.class);

    //TC: C153242:End User: Verify parent reference column on AE Tracker report listing page
    //TC: C153696: Verify Duplicate Docs Column on AE Tracker listing page
    @Test
    public void testC153242()
     {

      logger.info("Start Test : Verify Parent Reference Column Name on AE Report listing page");
      CustomAssert customAssert = new CustomAssert();
      try{
          HttpResponse reportListMetaDataResponse = AutoExtractionHelper.checkAutoExtractionReportListingMetaData("{}");
          customAssert.assertTrue(reportListMetaDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not Valid");
          String listingDataResponseStr = EntityUtils.toString(reportListMetaDataResponse.getEntity());
          JSONObject jsonObject = new JSONObject(listingDataResponseStr);
          int columnsLength = jsonObject.getJSONArray("columns").length();
          logger.info("Getting all Columns from List Page");
          List<String> allDefaultColumnsFromResponse = new LinkedList<>();
          for (int i = 0; i < columnsLength; i++) {
              allDefaultColumnsFromResponse.add(jsonObject.getJSONArray("columns").getJSONObject(i).get("queryName").toString());
          }
          logger.info("Validating Parent reference Columns on AE Report listing page");
          customAssert.assertTrue(allDefaultColumnsFromResponse.contains("parentreference"), "Parent Reference Column Name is not Present");
          logger.info("Validating Duplicate Document column on AE Report Tracker Listing Page");
          customAssert.assertTrue(allDefaultColumnsFromResponse.contains("duplicatedocument"),"Duplicate Docs Column name not present on AE Tracker Report listing page");
          logger.info("Validating Contract Id column on AE Tracker Report listing page: " +"Test Case ID:C153712 ");
          customAssert.assertTrue(allDefaultColumnsFromResponse.contains("contractid"), " Column Name is not Present");
          logger.info("Validating Contract Name column on AE Tracker Report listing page" + "Test Case ID:C153712 ");
          customAssert.assertTrue(allDefaultColumnsFromResponse.contains("contractname"), " Column Name is not Present");

      }
      catch(Exception e){
          logger.info("Error occurred while while validating parent reference Column on AE Report Listing page");
          customAssert.assertTrue(false,"Error occurred while while validating parent reference Column on AE Report Listing page "+e.getMessage());
      }
      customAssert.assertAll();
  }


}
