package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.insights.EntityList;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestInsightOnListingPageWOR226 {

    private final static Logger logger = LoggerFactory.getLogger(TestInsightOnListingPageWOR226.class);

    private int insightId = -1;

    @BeforeClass
    public void beforeClass() {
        EntityList entityListObj = new EntityList();
        entityListObj.hitInsightsEntityList();
        String entityListResponse = entityListObj.getInsightsEntityListJsonStr();

        JSONArray jsonArr = new JSONArray(entityListResponse);

        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getString("name").equalsIgnoreCase("contracts")) {
                jsonArr = jsonArr.getJSONObject(i).getJSONArray("insights");

                int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, jsonArr.length() - 1);

                if (jsonArr.getJSONObject(randomNumber).getInt("listId") == 2) {
                    insightId = jsonArr.getJSONObject(randomNumber).getInt("insightComputationId");
                }

                break;
            }
        }
    }

    @Test
    public void testC89445() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating TC-C89445.");
            JSONObject jsonObj;

            DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();
            String[] entitiesToTest = {"contracts", "actions", "suppliers", "vendors", "obligations", "child obligations", "service levels", "child service levels", "issues"};

            for (String entityName : entitiesToTest) {
                logger.info("Hitting DefaultUserListMetadata API for Entity {}", entityName);
                String defaultUserListMetadataResponse = defaultUserListHelperObj.getDefaultUserListMetadataResponse(entityName);

                jsonObj = new JSONObject(defaultUserListMetadataResponse);
                boolean expectedFlagValue = entityName.equalsIgnoreCase("contracts");
                boolean actualFlagValue = jsonObj.getBoolean("insightViewEnabled");

                csAssert.assertTrue(actualFlagValue == expectedFlagValue, "Expected Value of Flag 'insightViewEnabled': " + expectedFlagValue +
                        " and Actual Value: " + actualFlagValue + " for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C89445. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    @Test
    public void testC89446() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating TC-C89446.");
            if (insightId == -1) {
                throw new SkipException("Couldn't get InsightComputation Id");
            }

            ListRendererFilterData filterDataObj = new ListRendererFilterData();

            logger.info("Hitting FilterData API for Contracts and InsightComputationId {}.", insightId);

            Map<String, String> params = new HashMap<>();
            params.put("insightComputationId", String.valueOf(insightId));

            filterDataObj.hitListRendererFilterData(2, params);
            String filterDataResponse = filterDataObj.getListRendererFilterDataJsonStr();

            JSONObject jsonObj = new JSONObject(filterDataResponse);
            String[] allFilterIds = JSONObject.getNames(jsonObj);

            for (String filterId : allFilterIds) {
                JSONObject filterJsonObj = jsonObj.getJSONObject(filterId);

                String filterName = filterJsonObj.getString("filterName");
                logger.info("Validating Flag Value for Filter {}", filterName);

                boolean actualFlagValue = filterJsonObj.getBoolean("filterDisabled");

                boolean expectedFlagValue = false;
                String filterUiType = filterJsonObj.isNull("uitype") ? null : filterJsonObj.getString("uitype");

                int selectedDataLength = filterJsonObj.isNull("multiselectValues") ? 0: filterJsonObj.getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").length();

                if (filterUiType != null && filterUiType.equalsIgnoreCase("Date")) {
                    if (!filterJsonObj.isNull("start") || !filterJsonObj.isNull("end")) {
                        expectedFlagValue = true;
                    }
                } else if (filterUiType != null && filterUiType.equalsIgnoreCase("Slider")) {
                    if (!filterJsonObj.isNull("min") || !filterJsonObj.isNull("max")) {
                        expectedFlagValue = true;
                    }
                } else if (selectedDataLength > 0) {
                    expectedFlagValue = true;
                }

                csAssert.assertTrue(actualFlagValue == expectedFlagValue, "Insight Computation Id:" + insightId + ". Filter " +
                        filterName + ". Expected Value of 'filterDisabled' Flag is: " + expectedFlagValue + " and Actual Value: " + actualFlagValue);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C89446. " + e.getMessage());
        }
        csAssert.assertAll();
    }
}