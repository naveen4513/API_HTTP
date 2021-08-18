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
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class InProgressDocWithStages
{
    private final static Logger logger = LoggerFactory.getLogger(InProgressDocWithStages.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;

    /*Checking the Response of Icon that shows All the IN-progress documents in a list*/
    @Test
    public void IconOnListing()throws IOException
    {
        CustomAssert csAssert = new CustomAssert();

        try
        {
            logger.info("Checking when user clicks on i icon to get the details of only those documents that are In-progress");
            String query = "/listRenderer/list/432/listdata?version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"INPROGRESS\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":16797,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":16369,\"columnQueryName\":\"documentname\"},{\"columnId\":16370,\"columnQueryName\":\"contracttype\"},{\"columnId\":16371,\"columnQueryName\":\"status\"},{\"columnId\":16381,\"columnQueryName\":\"totalpages\"},{\"columnId\":16454,\"columnQueryName\":\"projects\"},{\"columnId\":16686,\"columnQueryName\":\"datecreated\"},{\"columnId\":16687,\"columnQueryName\":\"uploadedby\"}]}";
            HttpResponse inprogressResponse = AutoExtractionHelper.inprogressDocs(query,payload);
            csAssert.assertTrue(inprogressResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
            String inprogressResponseStr = EntityUtils.toString(inprogressResponse.getEntity());
            JSONObject inprogressDocJson = new JSONObject(inprogressResponseStr);
            int InProgressDocCount = inprogressDocJson.getJSONArray("data").length();

            //Now apply the filter in AE Doc listing to get total number of In-progress Doc Count
            query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"datecreated\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"INPROGRESS\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":16797,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":16369,\"columnQueryName\":\"documentname\"},{\"columnId\":16370,\"columnQueryName\":\"contracttype\"},{\"columnId\":16371,\"columnQueryName\":\"status\"},{\"columnId\":16381,\"columnQueryName\":\"totalpages\"},{\"columnId\":16454,\"columnQueryName\":\"projects\"},{\"columnId\":16686,\"columnQueryName\":\"datecreated\"},{\"columnId\":16687,\"columnQueryName\":\"uploadedby\"}]}";
            HttpResponse listDataResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
            JSONObject listDataJson = new JSONObject(listDataStr);
            int listDataCount = (int) listDataJson.get("filteredCount");
            csAssert.assertTrue(InProgressDocCount==listDataCount,"Document COunt mismatch");
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting Listing API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();

    }

    /*Test Case to validate whether the stages are coming*/
    @Test
    public void stagesOFDocument()throws IOException
    {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Checking whether stages are coming for each document or not");
            String query = "/listRenderer/list/432/listdata?version=2.0";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"INPROGRESS\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":16797,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":16369,\"columnQueryName\":\"documentname\"},{\"columnId\":16370,\"columnQueryName\":\"contracttype\"},{\"columnId\":16371,\"columnQueryName\":\"status\"},{\"columnId\":16381,\"columnQueryName\":\"totalpages\"},{\"columnId\":16454,\"columnQueryName\":\"projects\"},{\"columnId\":16686,\"columnQueryName\":\"datecreated\"},{\"columnId\":16687,\"columnQueryName\":\"uploadedby\"}]}";
            HttpResponse inprogressResponse = AutoExtractionHelper.inprogressDocs(query, payload);
            csAssert.assertTrue(inprogressResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String inprogressResponseStr = EntityUtils.toString(inprogressResponse.getEntity());
            JSONObject inprogressDocJson = new JSONObject(inprogressResponseStr);
            int dataLength = inprogressDocJson.getJSONArray("data").length();


            if (dataLength >= 1)
            {
                int columnId = ListDataHelper.getColumnIdFromColumnName(inprogressResponseStr, "status");
                String stage = inprogressDocJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[2];
                logger.info("Stage name ="+ stage);
                csAssert.assertTrue(stage.equalsIgnoreCase("Sent for Parsing")|| stage.equalsIgnoreCase("Parsing Started") ||
                        stage.equalsIgnoreCase("Parsing Inprogress") || stage.equalsIgnoreCase("Parsing completed") ||
                        stage.equalsIgnoreCase("Parsing Results Saved"),"It does not match any specified Stage");

            }

        }

        catch (Exception e)
        {
            logger.info("Error while hitting Stages of Docs API");
            csAssert.assertTrue(false,e.getMessage());

        }
        csAssert.assertAll();



    }
}
