package com.sirionlabs.test.autoExtraction;

import com.google.inject.internal.cglib.core.$Customizer;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

public class TestDownloadAE {
    private final static Logger logger = LoggerFactory.getLogger(TestDownloadAE.class);


    //C90090: Verify that the downloaded excel should show data of only those tabs and documents that has been selected by the user
    //C90073: Verify after selecting documents when user click download button then it should show an option to download Clause, Metadata, Obligation Data
    @Test
    public void downloadExcel() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        String outputFileName = "AutoExtraction_ExtractedData";
        String outputFilePath="src/test/output";
        String aeDataEntity = "auto extraction";
        try {

            logger.info("Hitting List data API to get List of document IDs for downloading Extracted data");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List data Api Response is invalid");
            String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
            JSONObject listDataJson = new JSONObject(listDataStr);
            int documentCount = listDataJson.getJSONArray("data").length();
            int columnId=ListDataHelper.getColumnIdFromColumnName(listDataStr, "id");

            if (documentCount > 0) {
                for (int i = 0; i < 10; i++)
                {
                    JSONObject listDataObj = listDataJson.getJSONArray("data").getJSONObject(i);
                    String[] idValue = listDataObj.getJSONObject(Integer.toString(columnId)).getString("value").toString().split(":;");
                    String documentId = idValue[1];
                    logger.info("Hitting Download extracted data API for document id "+documentId);
                    HttpResponse downloadResponse = AutoExtractionHelper.downloadExcel(documentId);
                    logger.info("Checking if Auto extraction listing is downloaded for document id "+documentId );
                    DownloadListWithData DownloadListWithDataObj = new DownloadListWithData();
                    String downloadStatus = DownloadListWithDataObj.dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath, "ListDataDownloadOutput", aeDataEntity, outputFileName);
                    if (downloadStatus == null) {
                    csAssert.assertTrue(false,"Auto Extraction List Download is unsuccessful for Document id "+documentId);
                    }
                    else {
                        logger.info("Extracted data has been downloaded successfully for document id "+documentId);
                    }
                        logger.info("Checking if downloaded contains Contracts and clause sheet");
                        String filePath=outputFilePath + "/"+"ListDataDownloadOutput/" +aeDataEntity;
                        String fileName=outputFileName+".xlsx";
                        XLSUtils xlsUtils=new XLSUtils(filePath,fileName);
                        List<String> allSheet=xlsUtils.getSheetNames();
                        csAssert.assertTrue(allSheet.size()==2,"There is more than 2 sheet in Downloaded Extracted data Excel sheet for Document id "+documentId);
                        logger.info("Checking if Contract sheet is present in downloaded extracted data sheet for document id "+documentId);
                        boolean isMetadataPresent=xlsUtils.isSheetExist("Contracts");
                        csAssert.assertTrue(isMetadataPresent,"Contract Sheet is not present in downloaded extracted data sheet for document id "+documentId);
                        logger.info("Checking if Clause sheet is present in downloaded extracted data sheet for document id "+documentId);
                        boolean isClausePresent=xlsUtils.isSheetExist("Clause");
                        csAssert.assertTrue(isClausePresent,"Clause Sheet is not present in downloaded extracted data sheet for document id "+documentId);
                        for(String sheet: allSheet)
                        {
                            VerifyTextId(filePath,fileName,sheet);
                            VerifyCategoryId(filePath,fileName,sheet);
                        }
                }

            } else {
                logger.warn("There is no record present to download extracted data (metadata and clause)");
            }
        }
        catch (Exception e)
        {
            logger.info("Exception while validating download AE Excel due to "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating download AE Excel due to "+e.getMessage());
        }
        finally {
            logger.info("Deleting AE Extracted data downloaded sheet from location "+outputFilePath);
            FileUtils.deleteFile(outputFilePath + "/"+"ListDataDownloadOutput/" +aeDataEntity+"/"+ outputFileName+".xlsx");
            logger.info(outputFileName+" AE Extracted data downloaded sheet Deleted successfully.");
        }
        csAssert.assertAll();
    }

    //TC:C153758:Verify Text ID and Category ID column in clause sheet of downloaded Extracted Data
    //TC:C153759:Verify Text ID and Category ID column in metadata sheet of downloaded Extracted Data
    public void VerifyTextId(String filePath,String fileName,String sheetName) throws IOException {
        CustomAssert customAssert=new CustomAssert();
        try {
            XLSUtils xlsUtils = new XLSUtils(filePath, fileName);
            List<String> allHeader = xlsUtils.getOffSetHeaders(filePath, fileName, sheetName);
            logger.info("Checking if Text ID column is present in downloaded Extracted Data sheet "+sheetName);
            customAssert.assertTrue(!(allHeader.contains("TEXT ID")),"Text ID column is present in Extracted data sheet");
        }
        catch (Exception e)
        {
           customAssert.assertTrue(false,"Getting exception while validating Text ID column due to "+e.getMessage());
        }
        customAssert.assertAll();
    }

    //TC:C153758:Verify Text ID and Category ID column in clause sheet of downloaded Extracted Data
    //TC:C153759:Verify Text ID and Category ID column in metadata sheet of downloaded Extracted Data
    public void VerifyCategoryId(String filePath,String fileName,String sheetName) throws IOException {
        CustomAssert customAssert=new CustomAssert();
        try {
            XLSUtils xlsUtils = new XLSUtils(filePath, fileName);
            List<String> allHeader = xlsUtils.getOffSetHeaders(filePath, fileName, sheetName);
            logger.info("Checking if Category ID column is present in downloaded Extracted Data sheet "+sheetName);
            customAssert.assertTrue(!(allHeader.contains("CATEGORY ID")),"Category ID column is present in Extracted data sheet");
        }
        catch (Exception e)
        {
            customAssert.assertTrue(false,"Getting exception while validating Category ID column due to "+e.getMessage());
        }
        customAssert.assertAll();
    }


}
