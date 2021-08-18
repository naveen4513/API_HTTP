package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/*Implementation Of JIRA SIR-4280*/
public class Test_FilterSupplierListCDR {

    private final static Logger logger = LoggerFactory.getLogger(Test_FilterSupplierListCDR.class);

    @Test
    public void TestFilterSupplierListOnCDRByContractManagingUnitCustomField(){

        CustomAssert customAssert = new CustomAssert();

        try{

            int dropDownType = 2;
            String sourceFieldId = "102313";

            //A1,B1,C1,D1,E1
            //24366 24367 24368 24369 24370
            //24371,24372,24373,24374,24375

            String sourceFieldOptionId = "24374";
//            String sourceFieldOptionIdNeverUsed = "24371";
            String dynamicFieldId = "102312";
            String dynamicFieldOptionId = "24369";
            String dynamicFieldOptionName = "D1";

            Map<String,String> params = new HashMap<>();
            params.put("pageType","1");
            params.put("entityTpeId","1");
            params.put("parentEntityId","");
            params.put("pageEntityTypeId","160");
            params.put("fieldId","");
            params.put("query","");
            params.put("expandAutoComplete","true");
            params.put("offset","0");
            params.put("sourceFieldId",sourceFieldId);
            params.put("sourceFieldOptionId",sourceFieldOptionId);
            params.put("dynamicFieldId",dynamicFieldId);

            Options options = new Options();
            options.hitOptions(dropDownType,params);

            String optionsResponse = options.getOptionsJsonStr();

            JSONObject optionsResponseJson = new JSONObject(optionsResponse);

            JSONArray supplierJsonArray = optionsResponseJson.getJSONArray("data");
            ArrayList<String> supplierListFromOptionsResponse  = new ArrayList();

            for(int i=0;i<supplierJsonArray.length();i++){

                supplierListFromOptionsResponse.add(supplierJsonArray.getJSONObject(i).get("id").toString());

            }

            ListRendererListData listRendererListData = new ListRendererListData();
            int supplierListId = 3;
            int supplierEntityTypeId = 1;
            String payload = "{\"filterMap\":{\"entityTypeId\":" + supplierEntityTypeId + ",\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"1002565\":{\"multiselectValues\":" +
                    "{\"SELECTEDDATA\":[{\"id\":\"" + dynamicFieldOptionId + "\",\"name\":\"" + dynamicFieldOptionName + "\"}]}," +
                    "\"filterId\":1002565,\"filterName\":\"" + dynamicFieldId + "\"," +
                    "\"entityFieldHtmlType\":4,\"entityFieldId\":" + dynamicFieldId + "}}},\"selectedColumns\":[{\"columnId\":39,\"columnQueryName\":\"id\"}]}";

            listRendererListData.hitListRendererListDataV2(supplierListId,payload);
            String listingResponse =  listRendererListData.getListDataJsonStr();

            JSONObject listingResponseJson = new JSONObject(listingResponse);

            JSONArray listingArray =  listingResponseJson.getJSONArray("data");

            JSONObject listingJson;
            JSONArray listingJsonArray;
            ArrayList<String> supplierListFromListingResponse = new ArrayList<>();
            String supplierFromListingResponse;

            for(int i =0;i<listingArray.length();i++){

                listingJson = listingArray.getJSONObject(i);
                listingJsonArray = JSONUtility.convertJsonOnjectToJsonArray(listingJson);

                innerLoop:
                for(int j =0 ;j<listingJsonArray.length();j++){

                    if(listingJsonArray.getJSONObject(j).get("columnName").toString().equalsIgnoreCase("id")){

                        supplierFromListingResponse = listingJsonArray.getJSONObject(j).get("value").toString().split(":;")[1];
                        supplierListFromListingResponse.add(supplierFromListingResponse);
                        break innerLoop;
                    }
                }

            }

            if(supplierListFromOptionsResponse.size() != supplierListFromListingResponse.size()){
                customAssert.assertTrue(false,"Supplier list from Option Response and Listing Response are not equal");
            }else {
                for(int i =0;i<supplierListFromListingResponse.size();i++){

                    if(!supplierListFromOptionsResponse.contains(supplierListFromListingResponse.get(i))){
                        customAssert.assertTrue(false,"Supplier List From Options Response doesn't contain supplier " + supplierListFromListingResponse.get(i));
                    }
                }
            }

        }catch (Exception e){

            logger.error("Exception while validating Filter Supplier List On CDR By ContractManagingUnit CustomField " + e.getStackTrace());
        }

        customAssert.assertAll();

    }

}
