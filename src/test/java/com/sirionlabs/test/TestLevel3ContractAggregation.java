package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class TestLevel3ContractAggregation {

    private final static Logger logger = LoggerFactory.getLogger(TestLevel3ContractAggregation.class);
    Show show = new Show();

    @Test
    public void TestLevel3ContractAggregation(){

        ListRendererListData listRendererListData = new ListRendererListData();

        String payload = "{\"filterMap\":{\"entityTypeId\":1,\"offset\":1000,\"size\":1000," +
                "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                "\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":17,\"columnQueryName\":\"id\"}]}";

        listRendererListData.hitListRendererListDataV2(3,payload);


        String listingResponse = listRendererListData.getListDataJsonStr();

        ArrayList<Integer> listingIds = getListingIds(listingResponse);

        HashMap<Integer,ArrayList<String>> listIdMap = new HashMap<>();

        try {

            for (Integer listingId : listingIds) {
                listIdMap = getContractsAfterGivenLevel(listingId,listIdMap);

            }

            System.out.println("");
            String excelFilePath = "src\\test\\output";
            String excelFileName = "Level3Cont.xlsx";
            String sheetName = "Sheet1";
            int row = 1;


            int parentEntity;
            String showResponse;

            JSONObject showResponseJson;
            String customerId;
            String shortCodeId;
            for (Map.Entry<Integer,ArrayList<String>> entry : listIdMap.entrySet()){

                parentEntity = entry.getKey();

                show.hitShowVersion2(61,parentEntity);
                showResponse = show.getShowJsonStr();

                showResponseJson = new JSONObject(showResponse);

                customerId = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn100369").getJSONObject("values").get("name").toString();

                shortCodeId = ShowHelper.getValueOfField("short code id",showResponse);

                XLSUtils.updateColumnValue(excelFilePath,excelFileName,sheetName,row,0,shortCodeId);
                XLSUtils.updateColumnValue(excelFilePath,excelFileName,sheetName,row,1,customerId);
                XLSUtils.updateColumnValue(excelFilePath,excelFileName,sheetName,row,2,entry.getValue().toString());

                row++;
            }

        }catch (Exception e){
            logger.error("Exception Occurred " + e.getStackTrace());
        }

        System.out.println("abc");
    }

    private ArrayList<Integer> getListingIds(String listingResponse){

        ArrayList<Integer> listingIds = new ArrayList<>();

        JSONObject listingResponseJson = new JSONObject(listingResponse);

        JSONArray dataArray = listingResponseJson.getJSONArray("data");
        JSONObject indJson = new JSONObject();
        JSONArray indJsonArray  = new JSONArray();

        String columnName;
        String columnValue;
        Integer listingId =-1;
        for(int i =0;i<dataArray.length();i++){

            indJson = dataArray.getJSONObject(i);

            indJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indJson);

            innerLoop:
            for(int j =0;j<indJsonArray.length();j++){

                columnName = indJsonArray.getJSONObject(j).get("columnName").toString();

                if(columnName.equalsIgnoreCase("id")){
                    listingId = Integer.parseInt(indJsonArray.getJSONObject(j).get("value").toString().split(":;")[1]);
                    break innerLoop;
                }
            }

            listingIds.add(listingId);
        }

        return listingIds;
    }

    private HashMap<Integer,ArrayList<String>> getContractsAfterGivenLevel(int parentSupplierId,HashMap<Integer,ArrayList<String>> listIdMap){

        ArrayList<String> childContractsAfterGivenLevel = new ArrayList<>();
//        HashMap<Integer,ArrayList<String>> listIdMap = new HashMap<>();

        try {

            ContractTreeData contractTreeData = new ContractTreeData();

//            contractTreeData.hitContractTreeDataListAPI(61, parentContractId);
            contractTreeData.hitContractTreeDataListAPI(1, parentSupplierId);

            String contractTreeResponse =  contractTreeData.getResponseContractTreeData();

            JSONObject contractTreeResponseJson = new JSONObject(contractTreeResponse);

            JSONArray firstLevelChildren = contractTreeResponseJson.getJSONObject("body").getJSONObject("data").getJSONArray("children");

            String entityTypeId;

            int entityId;
            String shortCodeId;

            for(int i =0;i<firstLevelChildren.length();i++) {

                entityTypeId = firstLevelChildren.getJSONObject(i).get("entityTypeId").toString();

                if(entityTypeId.equalsIgnoreCase("61")){
                    entityId = Integer.parseInt(firstLevelChildren.getJSONObject(i).get("entityId").toString());
                    childContractsAfterGivenLevel = getNLevelChild(1, entityId,childContractsAfterGivenLevel);

                    if(childContractsAfterGivenLevel.size() > 0) {


                        listIdMap.put(entityId, childContractsAfterGivenLevel);


                    }else {

                    }

                }

            }

        }catch (Exception e){
            System.out.println("Exception while getting contracts after given level " + e.getStackTrace());
        }

        return listIdMap;
    }


    private ArrayList<String> getNLevelChild(int level,int contractId,ArrayList<String> childArrayList){

        String entityTypeId;
        String entityId;

        try{

            if(contractId == 16585){
                System.out.println("Contract Id Found");
            }
            ContractTreeData contractTreeData = new ContractTreeData();

            contractTreeData.hitContractTreeDataListAPI(61, contractId);

            String contractTreeResponse =  contractTreeData.getResponseContractTreeData();

            JSONObject contractTreeResponseJson = new JSONObject(contractTreeResponse);

            JSONArray childrenArray;

            int entityIdInt =-1;

            String singleChildString;
            try {
                childrenArray = contractTreeResponseJson.getJSONObject("body").getJSONObject("data").getJSONArray("children");

                for(int j=0;j<childrenArray.length();j++){

                    if(level >= 2){
                        System.out.println("Level greater than 3");
                    }
                    entityTypeId = childrenArray.getJSONObject(j).get("entityTypeId").toString();

                    if(entityTypeId.equalsIgnoreCase("61")){

                        singleChildString = childrenArray.getJSONObject(j).get("entityId").toString();

                        if(!singleChildString.equalsIgnoreCase("null")){
                            entityIdInt = Integer.parseInt(singleChildString);

                            if(level >= 2){

                                System.out.println("level greater than 3 entity id " + entityIdInt);
                                show.hitShowVersion2(61,entityIdInt);
                                String showResponse= show.getShowJsonStr();

                                String shortCodeId = ShowHelper.getValueOfField("short code id",showResponse);
                                childArrayList.add(shortCodeId);
                            }

                        }
                        getNLevelChild(level + 1,entityIdInt,childArrayList);
                    }


                }

            }catch (JSONException jse){

                entityTypeId = contractTreeResponseJson.getJSONObject("body").getJSONObject("data").get("entityTypeId").toString();
                if(entityTypeId.equalsIgnoreCase("61")) {
                    singleChildString = contractTreeResponseJson.getJSONObject("body").getJSONObject("data").get("children").toString();

                    if(!singleChildString.equalsIgnoreCase("null")){

                        if(level >= 2){

                            System.out.println("level greater than 3 entity id " + entityIdInt);
                            show.hitShowVersion2(61,entityIdInt);
                            String showResponse= show.getShowJsonStr();

                            String shortCodeId = ShowHelper.getValueOfField("short code id",showResponse);
                            childArrayList.add(shortCodeId);

                        }
                        getNLevelChild(level + 1,entityIdInt,childArrayList);
                    }

                }

            }



        }catch (Exception e){
            logger.error("Exception occurred");
        }

        return childArrayList;
    }

}
