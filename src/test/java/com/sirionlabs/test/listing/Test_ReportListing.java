package com.sirionlabs.test.listing;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.test.SL_Stories.slif.test_SLIF_EndUser;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class Test_ReportListing {

    private final static Logger logger = LoggerFactory.getLogger(Test_ReportListing.class);

    private String configFilePath;
    private String configFileName;

    int lineItemListId = 358;
    int sdListId = 352;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSIR7983FilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSIR7983FileName");

    }

    @Test(enabled = true)
    public void Test_C151240(){

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        int entityTypeIdToCheck = 165;
        try{

            int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplieridforlineitemlisting"));

            ListRendererListData listRendererListData = new ListRendererListData();
            String listingPayload = createPayloadListing(supplierId);

            listRendererListData.hitListRendererListData(lineItemListId,listingPayload);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listDataResponse)){
                customAssert.assertTrue(false,"List Response Data is an invalid Json");
            }else {

                JSONObject listDataResponseJson = new JSONObject(listDataResponse);

                JSONArray dataArray = listDataResponseJson.getJSONArray("data");

                if(dataArray.length() == 0){
                    customAssert.assertTrue(false,"Listing data response does not contain any data");
                }else {
                    JSONObject indvRowJson;
                    JSONArray indvRowJsonArray;

                    String columnName;
                    String columnValue;
                    HashMap<String,String> listingPageValuesMap = new HashMap<>();

                    HashMap<String,String> listAndShowPageMap = createListAndShowPageMapLineItem();

                    for(int i=0;i<dataArray.length();i++){

                        indvRowJson = dataArray.getJSONObject(i);

                        indvRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvRowJson);

                        for (int j=0;j<indvRowJsonArray.length();j++){

                            columnName = indvRowJsonArray.getJSONObject(j).get("columnName").toString();
                            columnValue = indvRowJsonArray.getJSONObject(j).get("value").toString();

                            listingPageValuesMap.put(columnName,columnValue);
                        }
                        int showPageId = Integer.parseInt(listingPageValuesMap.get("id").split(":;")[1]);

                        show.hitShowVersion2(entityTypeIdToCheck,showPageId);
                        String showPageResponse = show.getShowJsonStr();

                        for(Map.Entry<String,String> entry : listingPageValuesMap.entrySet()){

                            String listingColName = entry.getKey();
                            String listingColValue = entry.getValue();

                            if(listAndShowPageMap.containsKey(listingColName)) {
                                String showPageColName = listAndShowPageMap.get(listingColName);

                                String showPageValue = ShowHelper.getValueOfField(showPageColName, showPageResponse);
                                if(showPageValue == null){
                                    showPageValue = "null";
                                }

                                try{
                                    listingColValue = String.valueOf(Double.parseDouble(listingColValue));
                                }catch (NumberFormatException e){

                                }

                                try{
                                    showPageValue = String.valueOf(Double.parseDouble(showPageValue));
                                }catch (NumberFormatException e){

                                }

                                if(!showPageValue.equals(listingColValue)){
                                    customAssert.assertTrue(false,"Value from show page and listing page not equal for show page column name " + showPageColName);
                                }
                            }else{
                                logger.error("Show page Field not found from list data Map ");

                            }
                        }

                    }
                }

            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void Test_C151239(){

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        int entityTypeIdToCheck = 64;

        try{

            int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplieridforservicedatalisting"));

            ListRendererListData listRendererListData = new ListRendererListData();

            String listingPayload = createPayloadListingSD(supplierId);

            listRendererListData.hitListRendererListData(sdListId,listingPayload);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listDataResponse)){
                customAssert.assertTrue(false,"List Response Data is an invalid Json");
            }else {

                JSONObject listDataResponseJson = new JSONObject(listDataResponse);

                JSONArray dataArray = listDataResponseJson.getJSONArray("data");

                if(dataArray.length() == 0){
                    customAssert.assertTrue(false,"Listing data response does not contain any data");
                }else {
                    JSONObject indvRowJson;
                    JSONArray indvRowJsonArray;

                    String columnName;
                    String columnValue;
                    HashMap<String,String> listingPageValuesMap = new HashMap<>();

                    for(int i=0;i<dataArray.length();i++){

                        indvRowJson = dataArray.getJSONObject(i);

                        indvRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvRowJson);

                        for (int j=0;j<indvRowJsonArray.length();j++){

                            columnName = indvRowJsonArray.getJSONObject(j).get("columnName").toString();
                            columnValue = indvRowJsonArray.getJSONObject(j).get("value").toString();

                            listingPageValuesMap.put(columnName,columnValue);
                        }
                        int showPageIdChild = Integer.parseInt(listingPageValuesMap.get("id").split(":;")[1]);

                        show.hitShowVersion2(entityTypeIdToCheck,showPageIdChild);
                        String showResponse = show.getShowJsonStr();
                        String parentService = null;

                        try {
                            JSONObject showResponseJson = new JSONObject(showResponse);
                            parentService = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("parentService").getJSONObject("values").get("name").toString();

                        }catch (Exception e){

                        }

                        if(parentService != null) {
                            listingPageValuesMap.get("id");
                            if (listingPageValuesMap.get("parentid")!= null) {
                                if (!listingPageValuesMap.get("parentid").equals("null")) {

                                    int showPageIdParent = Integer.parseInt(listingPageValuesMap.get("parentid").split(":;")[1]);
                                    String parentName = listingPageValuesMap.get("parentname");

                                    show.hitShowVersion2(entityTypeIdToCheck, showPageIdParent);
                                    showResponse = show.getShowJsonStr();

                                    JSONObject showResponseJson = new JSONObject(showResponse);

                                    String name = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
                                    String serviceIdClient = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdClient").get("values").toString();

                                    if (!parentName.equals(name + " (" + serviceIdClient + ")")) {
                                        customAssert.assertTrue(false, "Expected parent name and actual parent name mismatched ");
                                    }
                                    customAssert.assertAll();
                                    return;

                                } else {
                                    customAssert.assertTrue(false, "On Listing page parent Service id is null");
                                }
                            }else {
                                customAssert.assertTrue(false, "On Listing page parent Service id is null or field not present");
                            }
                        }
                    }
                }

            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void Test_C150511(){

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        int entityTypeIdToCheck = 64;
        int reportId = 355; // Service Data Tracker Report
        try{
            String payload = createPayloadListingSDReport();

            ReportRendererListData reportRendererListData = new ReportRendererListData();

            reportRendererListData.hitReportRendererListData(reportId,payload);
            String listDataResponse = reportRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listDataResponse)){
                customAssert.assertTrue(false,"List Response Data is an invalid Json");
            }else {

                JSONObject listDataResponseJson = new JSONObject(listDataResponse);

                JSONArray dataArray = listDataResponseJson.getJSONArray("data");

                if(dataArray.length() == 0){
                    customAssert.assertTrue(false,"Listing data response does not contain any data");
                }else {
                    JSONObject indvRowJson;
                    JSONArray indvRowJsonArray;

                    String columnName;
                    String columnValue;
                    HashMap<String,String> listingPageValuesMap = new HashMap<>();

                    for(int i=0;i<dataArray.length();i++){

                        indvRowJson = dataArray.getJSONObject(i);

                        indvRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvRowJson);

                        for (int j=0;j<indvRowJsonArray.length();j++){

                            columnName = indvRowJsonArray.getJSONObject(j).get("columnName").toString();
                            columnValue = indvRowJsonArray.getJSONObject(j).get("value").toString();

                            listingPageValuesMap.put(columnName,columnValue);
                        }
                        int showPageIdChild = Integer.parseInt(listingPageValuesMap.get("id").split(":;")[1]);

                        show.hitShowVersion2(entityTypeIdToCheck,showPageIdChild);
                        String showResponse = show.getShowJsonStr();
                        String parentService = null;

                        try {
                            JSONObject showResponseJson = new JSONObject(showResponse);
                            parentService = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("parentService").getJSONObject("values").get("name").toString();

                        }catch (Exception e){

                        }

                        if(parentService != null) {
                            listingPageValuesMap.get("id");
                            if (!listingPageValuesMap.get("parentid").equals("null")) {

                                int showPageIdParent = Integer.parseInt(listingPageValuesMap.get("parentid").split(":;")[1]);
                                String parentName = listingPageValuesMap.get("parentname");

                                show.hitShowVersion2(entityTypeIdToCheck, showPageIdParent);
                                showResponse = show.getShowJsonStr();

                                JSONObject showResponseJson = new JSONObject(showResponse);

                                String name = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
                                String serviceIdClient = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdClient").get("values").toString();

                                if (!parentName.equals(name + " (" + serviceIdClient + ")")) {
                                    customAssert.assertTrue(false, "Expected parent name and actual parent name mismatched ");
                                }
                                customAssert.assertAll();
                                return;
                            }else {
                                customAssert.assertTrue(false,"On Listing page parent Service id is null");
                            }

                        }
                    }
                }

            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void Test_C151240_1(){

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        int entityTypeIdToCheck = 165;
        int reportId = 359;

        try{

            String payload = createPayloadListingLineReport();

            ReportRendererListData reportRendererListData = new ReportRendererListData();

            reportRendererListData.hitReportRendererListData(reportId,payload);
            String listDataResponse = reportRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listDataResponse)){
                customAssert.assertTrue(false,"List Response Data is an invalid Json");
            }else {

                JSONObject listDataResponseJson = new JSONObject(listDataResponse);

                JSONArray dataArray = listDataResponseJson.getJSONArray("data");

                if(dataArray.length() == 0){
                    customAssert.assertTrue(false,"Listing data response does not contain any data");
                }else {
                    JSONObject indvRowJson;
                    JSONArray indvRowJsonArray;

                    String columnName;
                    String columnValue;
                    HashMap<String,String> listingPageValuesMap = new HashMap<>();

                    HashMap<String,String> listAndShowPageMap = createListAndShowPageMapLineItem();

                    for(int i=0;i<dataArray.length();i++){

                        indvRowJson = dataArray.getJSONObject(i);

                        indvRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvRowJson);

                        for (int j=0;j<indvRowJsonArray.length();j++){

                            columnName = indvRowJsonArray.getJSONObject(j).get("columnName").toString();
                            columnValue = indvRowJsonArray.getJSONObject(j).get("value").toString();

                            listingPageValuesMap.put(columnName,columnValue);
                        }
                        int showPageId = Integer.parseInt(listingPageValuesMap.get("id").split(":;")[1]);

                        show.hitShowVersion2(entityTypeIdToCheck,showPageId);
                        String showPageResponse = show.getShowJsonStr();

                        for(Map.Entry<String,String> entry : listingPageValuesMap.entrySet()){

                            String listingColName = entry.getKey();
                            String listingColValue = entry.getValue();

                            if(listAndShowPageMap.containsKey(listingColName)) {
                                String showPageColName = listAndShowPageMap.get(listingColName);

                                String showPageValue = ShowHelper.getValueOfField(showPageColName, showPageResponse);
                                if(showPageValue == null){
                                    showPageValue = "null";
                                }

                                try{
                                    listingColValue = String.valueOf(Double.parseDouble(listingColValue));
                                }catch (NumberFormatException e){

                                }

                                try{
                                    showPageValue = String.valueOf(Double.parseDouble(showPageValue));
                                }catch (NumberFormatException e){

                                }

                                if(!showPageValue.equals(listingColValue)){
                                    customAssert.assertTrue(false,"Value from show page and listing page not equal for show page column name " + showPageColName);
                                }
                            }else{
                                logger.error("Show page Field not found from list data Map ");

                            }
                        }

                    }
                }

            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();

    }

    private String createPayloadListing(int supplierId){

        String payload = "{\"filterMap\":{\"entityTypeId\":165,\"offset\":0,\"size\":100,\"orderByColumnName\"" +
                ":\"id\",\"orderDirection\":\"desc nulls last\",\"" +
                "filterJson\":{\"239\":{\"multiselectValues\":{\"SELECTEDDATA\":" +
                "[{\"id\":\"" + supplierId + "\",\"name\":\"ACCESS SUPLIER\"}]}," +
                "\"filterId\":239,\"filterName\":\"suppliers\",\"entityFieldHtmlType\":null," +
                "\"entityFieldId\":null}}},\"selectedColumns\":" +
                "[{\"columnId\":13882,\"columnQueryName\":\"id\"}," +
                "{\"columnId\":19630,\"columnQueryName\":\"baseVolume\"}," +
                "{\"columnId\":19631,\"columnQueryName\":\"systemBaseVolume\"}," +
                "{\"columnId\":19632,\"columnQueryName\":\"discrepancyBaseVolume\"}," +
                "{\"columnId\":19633,\"columnQueryName\":\"conversionRate\"}," +
                "{\"columnId\":19634,\"columnQueryName\":\"systemConversionRate\"}," +
                "{\"columnId\":19635,\"columnQueryName\":\"discrepancyConversionRate\"}]}";

        return payload;
    }

    private String createPayloadListingSD(int supplierId){

        String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":100,\"orderByColumnName\":" +
                "\"id\",\"orderDirection\":\"desc nulls last\"," +
                "\"filterJson\":{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":" +
                "[{\"id\":\"1189\",\"name\":\"VMWare Inc\"}]}," +
                "\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":" +
                "[{\"columnId\":14483,\"columnQueryName\":\"bulkcheckbox\"}," +
                "{\"columnId\":14219,\"columnQueryName\":\"id\"}," +
                "{\"columnId\":19616,\"columnQueryName\":\"parentid\"}," +
                "{\"columnId\":19617,\"columnQueryName\":\"parentname\"}]}";

        return payload;
    }

    private String createPayloadListingSDReport(){

        String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
                "\"filterJson\":{}},\"selectedColumns\":[" +
                "{\"columnId\":14238,\"columnQueryName\":\"id\"}," +
                "{\"columnId\":19612,\"columnQueryName\":\"parentid\"}," +
                "{\"columnId\":19613,\"columnQueryName\":\"parentname\"}]}";

        return payload;
    }

    private String createPayloadListingLineReport(){

        String payload = "{\"filterMap\":{\"entityTypeId\":165,\"offset\":0,\"size\":20," +
                "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[" +
                "{\"columnId\":13927,\"columnQueryName\":\"id\"}," +
                "{\"columnId\":19618,\"columnQueryName\":\"baseVolume\"}," +
                "{\"columnId\":19619,\"columnQueryName\":\"systemBaseVolume\"}," +
                "{\"columnId\":19620,\"columnQueryName\":\"discrepancyBaseVolume\"}," +
                "{\"columnId\":19621,\"columnQueryName\":\"conversionRate\"}," +
                "{\"columnId\":19622,\"columnQueryName\":\"systemConversionRate\"}," +
                "{\"columnId\":19623,\"columnQueryName\":\"discrepancyConversionRate\"}]}";

        return payload;
    }

    private HashMap<String,String> createListAndShowPageMapLineItem(){

        HashMap<String,String> listAndShowPageMap = new HashMap<>();

        listAndShowPageMap.put("baseVolume","basevolume");
        listAndShowPageMap.put("systemBaseVolume","systembasevolume");
        listAndShowPageMap.put("discrepancyBaseVolume","discrepancybasevolume");
        listAndShowPageMap.put("conversionRate","conversionratedouble");
        listAndShowPageMap.put("systemConversionRate","systemconversionratedouble");
        listAndShowPageMap.put("discrepancyConversionRate","discrepancyconversionratedouble");

        return listAndShowPageMap;

    }


}
