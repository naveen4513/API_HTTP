package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateFreeForm extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(CreateFreeForm.class);

    public static String getApiPath(int multiSupplierContractId, String entityName) {
        String searchUrl = ConfigureConstantFields.getSearchUrlForEntity(entityName);
        return "/" + searchUrl + "/create-free-form/rest?multiSupplierContractId=" + multiSupplierContractId + "&version=2.0";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getCreateFreeFormResponse(int multiSupplierContractId, String entityName) {
        logger.info("Hitting CreateFreeForm API for Entity {} and MultiSupplierContract Id {}", entityName, multiSupplierContractId);
        return executor.get(getApiPath(multiSupplierContractId, entityName), getHeaders()).getResponse().getResponseBody();
    }

    public static List<Map<String, String>> getAllMultiParentSuppliers(String createFreeFormResponse) {
        List<Map<String, String>> allParentSuppliers = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONObject(createFreeFormResponse).getJSONObject("body").getJSONObject("data")
                    .getJSONObject("multiParentSuppliers").getJSONObject("options").getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                Map<String, String> supplierMap = new HashMap<>();

                supplierMap.put("id", String.valueOf(jsonArr.getJSONObject(i).getInt("id")));
                supplierMap.put("name", jsonArr.getJSONObject(i).getString("name"));

                allParentSuppliers.add(supplierMap);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Multi Parent Suppliers from CreateFreeForm Response.");
        }
        return allParentSuppliers;
    }
}