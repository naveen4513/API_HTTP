//Created to test JIRA ID SIR-4661
package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.*;

public class TestDocumentViewerCSLListingSupport {

    private final static Logger logger = LoggerFactory.getLogger(TestDocumentViewerCSLListingSupport.class);
    private static String configFilePath;
    private static String configFileName;

    @Test
    public void TestCSLListingOnContractDocumentPage(){

        CustomAssert customAssert = new CustomAssert();

        try{

            ArrayList<String> columnsInOrderFromClientAdmin = getDocumentViewerColumnsInOrder();

            ListRendererListData listRendererListData = new ListRendererListData();
            int listId = 504;
            int contractDocument = 1081;
            int cslEntityTypeId = 15;
            String parameterString = "?contractId=1110&version=2.0";

            String payload = "{\"filterMap\":{\"entityTypeId\":" + cslEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":" +
                    "\"id\",\"orderDirection\":\"asc\",\"filterJson\":" +
                    "{\"21\":{\"filterId\":\"21\",\"filterName\":\"references\",\"multiselectValues\":" +
                    "{\"SELECTEDDATA\":[{\"id\":\"" + contractDocument + "\"}]}}}}}";

            HashMap<String,String> listShowPageMapping = getMapForDocListingValidation();

            ArrayList<String> columnNameListFromTab = new ArrayList<>();
            ArrayList<String> columnValueListFromTab = new ArrayList<>();

            listRendererListData.hitListRendererListData(listId,parameterString,payload);

            String listDataResponse = listRendererListData.getListDataJsonStr();
            JSONArray indDataJsonArray = new JSONArray();
            if(!APIUtils.validJsonResponse(listDataResponse)){
                customAssert.assertTrue(false,"Document Viewer Listing Response is an invalid Json");
            }else {

                try{
                    JSONObject listDataResponseJson = new JSONObject(listDataResponse);

                    JSONObject indDataJson = listDataResponseJson.getJSONArray("data").getJSONObject(0);

                    indDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indDataJson);

                    TreeMap<Integer, String> columnNameOrderTabListingMap = new TreeMap<>();
                    TreeMap<Integer, String> columnValueOrderTabListingMap = new TreeMap<>();

                    String columnName;
                    String columnValue;
                    Integer columnId;

                    for(int i =0;i<indDataJsonArray.length();i++){

                        columnName = indDataJsonArray.getJSONObject(i).get("columnName").toString();
                        columnValue = indDataJsonArray.getJSONObject(i).get("value").toString();
                        columnId = Integer.parseInt(indDataJsonArray.getJSONObject(i).get("columnId").toString());

                        columnNameOrderTabListingMap.put(columnId,columnName);
                        columnValueOrderTabListingMap.put(columnId,columnValue);
                    }

                    columnNameListFromTab = convertMapValuesList(columnNameOrderTabListingMap);
                    columnValueListFromTab = convertMapValuesList(columnValueOrderTabListingMap);


                    System.out.println("abc");
                }catch (Exception e){
                    logger.error("Exception while parsing listing data json");
                }

                logger.info("Validating columnOrder on Doc Viewer CSL Listing Page");

                if(columnNameListFromTab.size() != columnsInOrderFromClientAdmin.size()){
                    logger.error("Column Size from tab and client admin are not equal");
                    customAssert.assertTrue(false,"Column Size from tab and client admin are not equal");

                }

                for(int i= 0;i<columnNameListFromTab.size();i++){

                    if(!(columnNameListFromTab.get(i).equalsIgnoreCase(columnsInOrderFromClientAdmin.get(i)))){
                        logger.error("Column Name From List and Client Admin are not same");
                        customAssert.assertTrue(false,"Column Name From List and Client Admin are not same for order " + i);
                    }
                }


                if(indDataJsonArray.length() == 0 ){

                    customAssert.assertTrue(false,"Listing Data Json contains zero rows");
                }else {
                    String columnName;
                    String columnValueFromDocListing;
                    String childCSLID = null;
                    for (int i = 0; i < indDataJsonArray.length(); i++) {
                        columnName = indDataJsonArray.getJSONObject(i).get("columnName").toString();

                        if(columnName.equalsIgnoreCase("id")){
                            childCSLID = indDataJsonArray.getJSONObject(i).get("value").toString().split(":;")[1];
                            break;
                        }
                    }

                    Show show = new Show();
                    String showResponse = null;
                    String columnNameForShowPage = null;
                    String columnValueForShowPage = null;
                    JSONObject showResponseJson;

                    if(childCSLID !=null) {
                        show.hitShowVersion2(cslEntityTypeId, Integer.parseInt(childCSLID));
                        showResponse = show.getShowJsonStr();

                    }

                    if(!APIUtils.validJsonResponse(showResponse)){
                        customAssert.assertTrue(false,"Show response is invalid json for CSL ID " + childCSLID);
                        showResponse =  null;
                        customAssert.assertAll();
                    }

                    showResponseJson = new JSONObject(showResponse);

                    for (int i = 0; i < indDataJsonArray.length(); i++) {
                        columnName = indDataJsonArray.getJSONObject(i).get("columnName").toString();

                        if(columnName.equalsIgnoreCase("id")){
                            logger.info("Skipping the id column");
                            continue;
                        }
                        columnValueFromDocListing = indDataJsonArray.getJSONObject(i).get("value").toString();

                        if(columnName.contains("dyn")){
                            try{
                                try {
                                    columnValueForShowPage = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(columnName).get("values").toString();
                                }catch (Exception e){
                                    columnValueForShowPage = null;
                                }
                                if(columnValueForShowPage == null){

                                    if(columnValueFromDocListing.equalsIgnoreCase("null")){
                                        logger.info("Values are null for both show page and listing page");
                                    }else {
                                        logger.error("Column Value on show Page is null");
                                        customAssert.assertTrue(false, "Column Value on show Page is null for column Name " + columnName);
                                    }
                                    continue;
                                }else if(!columnValueFromDocListing.equalsIgnoreCase(columnValueForShowPage)){

                                    logger.error("Doc Listing Column Name " + columnName +
                                            " not matched with Show Page Column Name " + columnNameForShowPage);
                                    logger.error("Value on DocListing Column " + columnValueFromDocListing + " Value on Show PageColumn " + columnValueForShowPage);


                                    customAssert.assertTrue(false,"Doc Listing Column Name " + columnName +
                                            " not matched with Show Page Column Name " + columnNameForShowPage);
                                    customAssert.assertTrue(false,"Value on DocListing Column " + columnValueFromDocListing + " Value on Show PageColumn " + columnValueForShowPage);

                                }

                            }catch (Exception e){
                                customAssert.assertTrue(false,"Exception while validating custom field");
                            }
                        }
                        else if(listShowPageMapping.containsKey(columnName)){

                            columnNameForShowPage = listShowPageMapping.get(columnName);
                            columnValueForShowPage = ShowHelper.getValueOfField(cslEntityTypeId,columnNameForShowPage,showResponse);

                            if(columnValueForShowPage == null){

                                if(columnValueFromDocListing.equalsIgnoreCase("null")){
                                    logger.info("Values are null for both show page and listing page");
                                }else {
                                    logger.error("Column Value on show Page is null");
                                    customAssert.assertTrue(false, "Column Value on show Page is null for column Name " + columnName);
                                }
                                continue;
                            }


                            if(columnValueForShowPage.contains(".")) {
                                try {
                                    columnValueFromDocListing = String.valueOf(Double.parseDouble(columnValueForShowPage));
                                }catch (NumberFormatException nfe){
                                    if(!columnValueFromDocListing.contains(columnValueForShowPage)) {
                                        logger.error("Column Value from listing page does not contain column value from show page");
                                        customAssert.assertTrue(false,"Column Value from listing page does not contain column value from show page");
                                    }
                                    continue;
                                }

                            }

                            if(columnValueFromDocListing.contains(":;") && columnName.equalsIgnoreCase("masterslid")) {
                                columnValueFromDocListing = columnValueFromDocListing.split(":;")[1];

                            }else if(columnValueFromDocListing.contains(":;")){
                                columnValueFromDocListing = columnValueFromDocListing.split(":;")[0];
                            }


                            if(columnValueFromDocListing.equalsIgnoreCase("Met Min")){
                                columnValueFromDocListing = "Met Minimum";
                            }else if(columnValueFromDocListing.equalsIgnoreCase("Met Sig Min")){
                                columnValueFromDocListing = "Met Significantly Minimum";
                            }

                            if(!columnValueFromDocListing.equalsIgnoreCase(columnValueForShowPage)){

                                logger.error("Doc Listing Column Name " + columnName +
                                        " not matched with Show Page Column Name " + columnNameForShowPage);
                                logger.error("Value on DocListing Column " + columnValueFromDocListing + " Value on Show PageColumn " + columnValueForShowPage);


                                customAssert.assertTrue(false,"Doc Listing Column Name " + columnName +
                                        " not matched with Show Page Column Name " + columnNameForShowPage);
                                customAssert.assertTrue(false,"Value on DocListing Column " + columnValueFromDocListing + " Value on Show PageColumn " + columnValueForShowPage);

                            }
                        }else {
                            customAssert.assertTrue(false,"Column Name " + columnName + " not found in Doc list ShowPage Mapping");
                        }

                    }

                }

            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating CSLListing On Contract Document Page");
        }

        customAssert.assertAll();

    }

    private HashMap<String,String> getMapForDocListingValidation(){

        HashMap<String,String> listShowPageMapping = new HashMap<>();

        listShowPageMapping.put("suppliername","supplier");
        listShowPageMapping.put("contractname","contract");
        listShowPageMapping.put("sl_category","slaCategory");
        listShowPageMapping.put("slsubcategory","slaSubCategory");
        listShowPageMapping.put("sl_item","slaItem");
        listShowPageMapping.put("priority","priority");
        listShowPageMapping.put("duedate","dueDate");
        listShowPageMapping.put("reporting_date","reportingDate");
        listShowPageMapping.put("performancestatus","performanceStatus");
        listShowPageMapping.put("measurement_unit","measurementUnit");
        listShowPageMapping.put("ragapplicable","ragApplicable");
        listShowPageMapping.put("threshold","threshold");
        listShowPageMapping.put("expected","expected");
        listShowPageMapping.put("significantlyminimum","sigMinMax");
        listShowPageMapping.put("finalperformance","finalperformance");
        listShowPageMapping.put("slmet","slMet");

        listShowPageMapping.put("minimummaximum","minmax");
        listShowPageMapping.put("actualperformance","actualperformance");
        listShowPageMapping.put("supplierperformance","suppliercalculation");
        listShowPageMapping.put("pagerefrences","pagereference");

        listShowPageMapping.put("title","name");
        listShowPageMapping.put("masterslid","parentshortcodeiddbid");
        listShowPageMapping.put("id","");

        return listShowPageMapping;
    }

    //        To get the columns order which will appear in CSL Tab List
    private ArrayList<String> getDocumentViewerColumnsInOrder(){

        ArrayList<String> columnsInOrder = new ArrayList<>();

        try{
            String docViewerListId = "504";

            AdminHelper adminHelper = new AdminHelper();

            adminHelper.loginWithClientAdminUser();
            ListRendererConfigure listRendererConfigure = new ListRendererConfigure();
            listRendererConfigure.hitListRendererConfigure(docViewerListId);
            String docListingViewResponse =  listRendererConfigure.getListRendererConfigureJsonStr();

            JSONObject docListingViewResponseJson = new JSONObject(docListingViewResponse);
            JSONArray columnsArray = docListingViewResponseJson.getJSONArray("columns");
            String columnName;
            Integer orderSeq;

            TreeMap<Integer, String> orderSeqColumnMap = new TreeMap<>();

            for(int i =0;i<columnsArray.length();i++){

                columnName = columnsArray.getJSONObject(i).get("queryName").toString();
                orderSeq = Integer.parseInt(columnsArray.getJSONObject(i).get("order").toString());

                orderSeqColumnMap.put(orderSeq,columnName);
            }

            for(Map.Entry<Integer, String> column : orderSeqColumnMap.entrySet()){

                columnsInOrder.add(column.getValue());
            }

        }catch (Exception e){
            logger.error("Exception while creating column order sequence ");
        }finally {

            Check check = new Check();
            check.hitCheck();

        }

        return columnsInOrder;
    }

    private ArrayList<String> convertMapValuesList(Map<Integer, String> treeMap){

        ArrayList<String> arrayList = new ArrayList<>();
        for(Map.Entry<Integer,String> column : treeMap.entrySet()){

            arrayList.add(column.getValue());
        }
        return arrayList;
    }

//URL Used for testing purpose
//http://qa.ba1.office/ux/#/show/tblcontractdocuments//show/tblcontractdocuments/1081?pageno=5&parententity=15&t=1579591504681




}
