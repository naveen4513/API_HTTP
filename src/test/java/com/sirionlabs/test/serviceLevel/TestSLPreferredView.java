package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class TestSLPreferredView {
    private final static Logger logger = LoggerFactory.getLogger(TestSLPreferredView.class);

    @Test(enabled = true)
    public void TestPreferredView(){
        CustomAssert customAssert = new CustomAssert();

        int listId = 6;
        String payload = "{\"filterMap\":{\"entityTypeId\":14,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1426\",\"name\":\"API Automation Supplier for Child Entities Creation Test\"}]},\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"219\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Local\"}]},\"filterId\":219,\"filterName\":\"regionType\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"226\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1008\",\"name\":\"Availability Management\"}]},\"filterId\":226,\"filterName\":\"slasubcategory\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":277,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":110,\"columnQueryName\":\"id\"},{\"columnId\":111,\"columnQueryName\":\"slaid\"},{\"columnId\":112,\"columnQueryName\":\"name\"},{\"columnId\":113,\"columnQueryName\":\"suppliername\"},{\"columnId\":114,\"columnQueryName\":\"expected\"}]}";
        String response;
        String supplier = "API Automation Supplier for Child Entities Creation Test";
        try{
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2amTrue(listId,payload);

            response = listRendererListData.getListDataJsonStr();
            JSONObject listDataResponseJSON = new JSONObject(response);

            JSONArray listData = listDataResponseJSON.getJSONArray("data");

            for (int i=0;i<listData.length();i++){
                JSONArray arrayData =  listData.getJSONObject(i).names();
                for(int j=0;j<arrayData.length();j++){
                    if(listData.getJSONObject(i).getJSONObject(arrayData.getString(j)).getString("columnName").equalsIgnoreCase("suppliername")){
                        String supplierName = listData.getJSONObject(i).getJSONObject(arrayData.getString(j)).getString("value").split(":;")[0];
                        if(!supplierName.equals(supplier)){
                            logger.error("Supplier is not as expected");
                            customAssert.assertTrue(false,"Supplier is not as expected");
                            break;
                        }
                    }
                }
            }
        }catch(Exception e){
            logger.error("Test Preferred View does not validated successfully");
            customAssert.assertFalse(false, "SL preferred view has not validated successfully");
        }
    }
}