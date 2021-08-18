package com.sirionlabs.test.CustomerDataCreation;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.helper.ShowHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class DocumentTreeDownLoad {

    int supplierEntityTypeId = 1;
    int contractEntityTypeId = 61;

    @Test
    public void TestDocumentDownload(){

        List<Integer> supplierList = getListOfSuppliers();
        List<Integer> contractList;
        List<Integer> downloadedContractList;

        String supplierShortCodeId;
        String contractShortCodeId;


        String filePath;
        String filePathContract;


        for(Integer supplier : supplierList){

            supplierShortCodeId = ShowHelper.getValueOfField(supplierEntityTypeId,supplier,"short code id");

            filePath = "src/test/output/nblData/" + supplierShortCodeId;

            contractList = getContractList(supplier);

            for(Integer contract : contractList){

                contractShortCodeId = ShowHelper.getValueOfField(contractEntityTypeId,contract,"short code id");

                filePathContract = filePath + "/" + contractShortCodeId;

                downloadData(contract,filePathContract);

            }


        }


    }

    private List<Integer> getListOfSuppliers(){

        ListRendererListData listRendererListData = new ListRendererListData();
        int supplierListId = 3;

        List<Integer> supplierList = new ArrayList<>();

        String payload = "{\"filterMap\":{\"entityTypeId\":1,\"offset\":0,\"size\":200,\"" +
                "orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}," +
                "\"selectedColumns\":[{\"columnId\":39,\"columnQueryName\":\"id\"}]}";

        listRendererListData.hitListRendererListData(supplierListId,payload);
        String listingResponse = listRendererListData.getListDataJsonStr();

        JSONObject listingResponseJson = new JSONObject(listingResponse);

        JSONArray dataArray = listingResponseJson.getJSONArray("data");
        Integer supplierId;
        String supplierIdString;
        for(int i =0;i<dataArray.length();i++){

            supplierIdString = dataArray.getJSONObject(i).getJSONObject("39").get("value").toString().split(":;")[1];

            supplierId = Integer.parseInt(supplierIdString);

            supplierList.add(supplierId);
        }

        return supplierList;
    }

    private List<Integer> getContractList(int supplier){

        int listId = 2;
        int contractId;

        List<Integer> contractList = new ArrayList<>();

        String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                "{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplier + "\"," +
                "\"name\":\"Optimized Energy Solutions, LLC\"}]},\"filterId\":1,\"filterName\":" +
                "\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                "\"selectedColumns\":[{\"columnId\":17,\"columnQueryName\":\"id\"}]}";

        ListRendererListData listRendererListData = new ListRendererListData();

        listRendererListData.hitListRendererListDataV2(listId,payload);
        String listResponse = listRendererListData.getListDataJsonStr();

        JSONObject listResponseJson = new JSONObject(listResponse);

        JSONArray dataArray = listResponseJson.getJSONArray("data");

        for(int i = 0;i<dataArray.length();i++){

            contractId = Integer.parseInt(dataArray.getJSONObject(i).getJSONObject("17").get("value").toString().split(":;")[1]);
            contractList.add(contractId);
        }

        return  contractList;
    }

    private void downloadData(int contract,String filePath){

        ContractTreeData contractTreeData = new ContractTreeData();

        String contractTreeResponse = contractTreeData.hitContractTreeListAPIV1(contractEntityTypeId,contract,"{}");

        JSONObject contractTreeResponseJson = new JSONObject(contractTreeResponse);
        JSONArray childArray =contractTreeResponseJson.getJSONObject("body").getJSONObject("data").getJSONArray("children");

        String entityTypeId;
        String shortCodeId;
        String fileName;
        String extension;

        int entityId;
        Download download = new Download();

        for(int i = 0;i<childArray.length();i++){

            entityTypeId = childArray.getJSONObject(i).get("entityTypeId").toString();
            entityId = Integer.parseInt(childArray.getJSONObject(i).get("entityId").toString());

            if(entityTypeId.equals("61")){

                shortCodeId = ShowHelper.getValueOfField(61,entityId,"short code id");

                downloadData(entityId,filePath + "/" + shortCodeId);

            }else if(entityTypeId.equals("23")){

                fileName = childArray.getJSONObject(i).get("text").toString();
                extension = childArray.getJSONObject(i).get("extension").toString();
                fileName = fileName + "." + extension;

                download.hitDocumentDownload(filePath,fileName,entityId);

            }
        }


    }


}
