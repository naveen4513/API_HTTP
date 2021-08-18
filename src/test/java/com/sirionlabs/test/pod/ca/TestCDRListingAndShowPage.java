package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestCDRListingAndShowPage {
    private final static Logger logger = LoggerFactory.getLogger(TestCDRListingAndShowPage.class);
    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
    }

    @DataProvider
    public Object[][] dataProviderForListingANdShowPage() {
        List<Object[]> listOfArrays = new ArrayList<>();
        List<String> inputTextList = new ArrayList<>();
        inputTextList.add("contract draft request");
        inputTextList.add("contracts");
        for (String entity : inputTextList) {
            listOfArrays.add(new Object[]{entity});
        }
        return listOfArrays.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForListingANdShowPage")
    public void testC153259(String entityName) {
        CustomAssert customAssert = new CustomAssert();
        String entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_url_id");
        String entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
        int entityId = -1;
        try {
            HashMap<String, String> listingData = getStatusOnListing(Integer.parseInt(entityTypeId), Integer.parseInt(entityURLId), customAssert);
            entityId = Integer.parseInt(listingData.get("EntityId"));
            String statusOnListing = listingData.get("Status");
            HashMap<String,List<String>> namesOnShowPage = getShowPageData(entityId, entityTypeId, customAssert);
            customAssert.assertTrue(namesOnShowPage.get("FieldNames").contains("templates"), "TC-C153259 is failed.");
            customAssert.assertTrue(namesOnShowPage.get("Status").contains(statusOnListing), "TC-C152731 is failed.");
        } catch (Exception e) {
            logger.error("Exception {} occurred while comparing listing data with the showPage.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while comparing listing data with the showPage.");
        }
        customAssert.assertAll();
    }

    private HashMap<String, String> getStatusOnListing(int entityTypeId, int entityURLId, CustomAssert customAssert) {
        ListRendererListData listRendererListData = new ListRendererListData();
        HashMap<String, String> listingData = new HashMap<>();
        String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":12259,\"columnQueryName\":\"id\"},{\"columnId\":12260,\"columnQueryName\":\"title\"},{\"columnId\":12277,\"columnQueryName\":\"status\"}]}";
        try {
            String listResponse = listRendererListData.hitListRendV2(entityURLId, payload);
            if (ParseJsonResponse.validJsonResponse(listResponse)) {
                JSONObject dataEntry = new JSONObject(listResponse).getJSONArray("data").getJSONObject(0);
                for (String key : dataEntry.keySet()) {
                    if (dataEntry.getJSONObject(key).getString("columnName").equalsIgnoreCase("id")) {
                        listingData.put("EntityId", dataEntry.getJSONObject(key).getString("value").split(":;")[1]);
                    } else if (dataEntry.getJSONObject(key).getString("columnName").equalsIgnoreCase("status")) {
                        listingData.put("Status", dataEntry.getJSONObject(key).getString("value").toLowerCase());
                    }
                }
            } else {
                logger.error("Listing Response is not a valid JSON");
                customAssert.assertTrue(false, "Listing response is not a valid JSON");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while getting the listing response data.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while getting the listing response data.");
        }
        return listingData;
    }

    private HashMap<String, List<String>> getShowPageData(int entityId, String entityTypeId, CustomAssert customAssert) {
        HashMap<String, List<String>> showPageData = new HashMap<>();
        List<String> fieldNames = new ArrayList<>();
        List<String> status = new ArrayList<>();
        Show show = new Show();
        try {
            show.hitShow(Integer.parseInt(entityTypeId), entityId, true);
            String showPageResponse = show.getShowJsonStr();
            if (ParseJsonResponse.validJsonResponse(showPageResponse)) {
                JSONObject showJSON = new JSONObject(showPageResponse);
                if (showJSON.getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                    status.add(showJSON.getJSONObject("body").getJSONObject("data").getJSONObject("status").getJSONObject("values").getString("name").toLowerCase());

                    JSONArray layOutArray = showJSON.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields");
                    for (int index = 0; index < layOutArray.length(); index++) {
                        if (layOutArray.getJSONObject(index).getString("label").equalsIgnoreCase("General")) {
                            JSONArray fieldsArray = layOutArray.getJSONObject(index).getJSONArray("fields");
                            for (int fIndex = 0; fIndex < fieldsArray.length(); fIndex++) {
                                if (fieldsArray.getJSONObject(fIndex).getString("label").equalsIgnoreCase("Basic Information")) {
                                    JSONArray fieldArray = fieldsArray.getJSONObject(fIndex).getJSONArray("fields");
                                    for (int i = 0; i < fieldArray.length(); i++) {
                                        try {
                                            fieldNames.add(fieldArray.getJSONObject(i).getString("name").toLowerCase());
                                        } catch (Exception e) {
                                            continue;
                                        }
                                    }

                                }
                            }
                        }
                    }
                } else {
                    logger.error("Show Page response is not a valid JSON");
                    customAssert.assertTrue(false, "Show Page response is not a valid JSON");
                }
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching Show page response", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching Show page response");
        }finally {
            showPageData.put("Status",status);
            showPageData.put("FieldNames",fieldNames);
        }
        return showPageData;
    }
}