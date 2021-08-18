package com.sirionlabs.test.SL;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test_Feb2020Bugs {

    private final static Logger logger = LoggerFactory.getLogger(Test_Feb2020Bugs.class);

    //SIR-5609
    @Test(enabled = true)
    public void TestStakeholderOnSLTabListing(){

        CustomAssert customAssert = new CustomAssert();
        int cslListId = 265;

        try{
            Show show = new Show();

            ListRendererListData listRendererListData = new ListRendererListData();

            listRendererListData.hitListRendererListData(cslListId);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            JSONObject listDataResponseJson = new JSONObject(listDataResponse);

            JSONArray dataArray = listDataResponseJson.getJSONArray("data");

            JSONObject indRowData = dataArray.getJSONObject(0);

            JSONArray indRowDataJsonArray =  JSONUtility.convertJsonOnjectToJsonArray(indRowData);
            String columnName;
            String columnValue;
            String columnValueTabListing;

            int slIDToCheck = -1;
            int cslIDToCheck = -1;
            int cslTabId = 7;
            int slEntityTypeId = 14;
            int cslEntityTypeId = 15;

            for(int i=0;i<indRowDataJsonArray.length();i++){

                columnName = indRowDataJsonArray.getJSONObject(i).get("columnName").toString();

                if(columnName.equalsIgnoreCase("masterslid")){
                    columnValue = indRowDataJsonArray.getJSONObject(i).get("value").toString();
                    slIDToCheck = Integer.parseInt(columnValue.split(":;")[1]);

                }

                if(columnName.equalsIgnoreCase("id")){
                    columnValue = indRowDataJsonArray.getJSONObject(i).get("value").toString();
                    cslIDToCheck = Integer.parseInt(columnValue.split(":;")[1]);

                }

            }

            if(slIDToCheck !=-1) {
                TabListData tabListData = new TabListData();
                tabListData.hitTabListDataV2(cslTabId, slEntityTypeId, slIDToCheck);
                String tabListResponse = tabListData.getTabListDataResponseStr();

                JSONObject tabListResponseJson = new JSONObject(tabListResponse);
                JSONObject indJsonObject = tabListResponseJson.getJSONArray("data").getJSONObject(0);

                JSONArray indJsonObjectArray =  JSONUtility.convertJsonOnjectToJsonArray(indJsonObject);


                for(int i =0;i<indJsonObjectArray.length();i++){

                    columnName = indJsonObjectArray.getJSONObject(i).get("columnName").toString();

                    if(columnName.equalsIgnoreCase("Child Service Levels ManagerROLE_GROUP")) {
                        columnValueTabListing = indJsonObjectArray.getJSONObject(i).get("value").toString();

                        show.hitShowVersion2(cslEntityTypeId,cslIDToCheck);
                        String showResponse = show.getShowJsonStr();

                        List<String> stakeholderFromShowPage = getStakeHoldersValues(showResponse);


                        if(stakeholderFromShowPage == null && columnValueTabListing ==null){
                            logger.debug("Stakeholder from show page and listing page are null");

                        }else if(stakeholderFromShowPage == null && columnValueTabListing =="null"){
                            logger.debug("Stakeholder from show page and listing page are null");
                        } else if(columnValueTabListing == "null" && stakeholderFromShowPage !=null){
                            customAssert.assertTrue(false,"Stakeholder field from tab List Response is null where as it is not null from Show Page");

                        }else {

                            for(String stakeholder : stakeholderFromShowPage) {
                                if(!columnValueTabListing.contains(stakeholder)){
                                    customAssert.assertTrue(false, "Stakeholder field from tab List Response and Show Page are not equal");
                                }
                            }
                        }

                        break;
                    }

                }

            }else {
                customAssert.assertTrue(false,"Unable to find sl id from sl List Data");
            }

        }catch (Exception e){
            logger.error("Exception occurred while validating Stakeholder Field On SL Tab Listing " + e.getStackTrace() );
            customAssert.assertTrue(false,"Exception occurred while validating Stakeholder Field On SL Tab Listing " + e.getStackTrace() );
        }

        customAssert.assertAll();

    }

    //SIR-5505
    @Test(enabled = true)   //Columns to be brought into the report
    public void TestStakeholderOnStatusTransitionReport(){

        CustomAssert customAssert = new CustomAssert();
        int cslEntityTypeId = 15;
        int reportId = 406;

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        Show show = new Show();

        try{

            Map<String, String> formParam = getFormParamDownloadList(cslEntityTypeId);

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
            HttpResponse downloadResponse = downloadReportWithData.hitDownloadReportWithData(formParam,reportId);

            String excelFilePath = "src/test/output";

            String excelFileName = "ReportListData_Download.xlsx";

            boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, excelFilePath + "/" + excelFileName);

            if(fileDownloaded == false){
                customAssert.assertTrue(false,"StatusTransitionReport Downloaded unsuccessfully");
            }

            XLSUtils xlsUtils = new XLSUtils(excelFilePath,excelFileName);

            String sheetName = "Data";
            int columnCount = xlsUtils.getColumnCount(sheetName);
            int columnNamesRowNum = 4;
            int stakeHolderColumnNum = -1;
            int idColumnNum = -1;
            String columnName;

            Boolean columnFound = false;

            for(int columnNo =0;columnNo<columnCount;columnNo++){

                columnName = xlsUtils.getCellData(sheetName,columnNo,columnNamesRowNum);

                if(columnName.equalsIgnoreCase("CHILD SERVICE LEVELS MANAGER")){
                    columnFound = true;
                    stakeHolderColumnNum = columnNo;
                }

                if(columnName.equalsIgnoreCase("ID UPDATED ")){

                    idColumnNum = columnNo;
                }
            }

            String stakeHolderInReport;
            String cslId;
            String cslIdEntitySeqId;

            if(columnFound == true){

                stakeHolderInReport = xlsUtils.getCellData(sheetName,stakeHolderColumnNum,columnNamesRowNum + 1);

                cslId = xlsUtils.getCellData(sheetName,idColumnNum,columnNamesRowNum + 1);

                cslIdEntitySeqId = cslId.split("CSL")[1];

                String sqlQuery = "select id from child_sla where client_entity_seq_id = " + cslIdEntitySeqId;

                List<List<String>> sqlOutput = postgreSQLJDBC.doSelect(sqlQuery);

                int cslDbId;
                try{
                    cslDbId = Integer.parseInt(sqlOutput.get(0).get(0));
                    show.hitShowVersion2(cslEntityTypeId,cslDbId);
                    String showResponse = show.getShowJsonStr();

                    List<String> stakeHolderFromShowResponse = ShowHelper.getAllSelectedStakeholdersFromShowResponse(showResponse);

                    for(String stakeHolder : stakeHolderFromShowResponse){

                        if(!stakeHolderInReport.contains(stakeHolder)){
                            customAssert.assertTrue(false,"Stakeholder value not matched with showResponse and StatusTransition Report Downloaded ");
                        }
                    }

                }catch (Exception e){
                    logger.error("Exception while parsing entity seq id");
                    customAssert.assertTrue(false,"Exception while parsing entity seq id");

                }
            }else {
                customAssert.assertTrue(false,"Stake holder column CHILD SERVICE LEVELS MANAGER not found in the downloaded excel ");
            }

        }catch (Exception e){

        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }

    private Map<String, String> getFormParamDownloadList(Integer entityTypeId) {

        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData;

        jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);

        String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private List<String> getStakeHoldersValues(String showResponse){


        List<String> stakeHoldersValues = new ArrayList<>();
        String stakeHolder;
        try{
            JSONObject showResponseJson = new JSONObject(showResponse);


            JSONArray valuesJsonArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_2003").getJSONArray("values");

            for(int i=0;i<valuesJsonArray.length();i++){
                stakeHolder = valuesJsonArray.getJSONObject(i).get("name").toString();
                stakeHoldersValues.add(stakeHolder);
            }

        }catch (Exception e){

        }
        return stakeHoldersValues;
    }

}
