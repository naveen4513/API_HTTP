package com.sirionlabs.test.common;

import com.sirionlabs.api.listRenderer.UserListMetaData;
import com.sirionlabs.api.userPreference.UserPreferenceData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestListingDefaultColumnsAndFilters extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestListingDefaultColumnsAndFilters.class);

    private UserListMetaData prefDataObj = new UserListMetaData();

    @DataProvider
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] allEntities = {"suppliers"};

        for (String entity : allEntities) {
            allTestData.add(new Object[]{entity.trim()});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProvider")
    public void testListingDefaultColumnsAndFilters(String entityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Listing Default Columns and Filters for Entity {}", entityName);
            int listId = ConfigureConstantFields.getListIdForEntity(entityName);

            logger.info("Hitting UserPreference List API for Entity {}", entityName);
            prefDataObj.hitUserPreferenceList(listId);
            String prefListResponse = prefDataObj.getListUserPreferenceAPIResponse();
            int defaultViewId = getDefaultViewId(prefListResponse);

            logger.info("DefaultView Id for Entity {} is: {}", entityName, defaultViewId);

            if (defaultViewId == -1) {
                //Todo: Get Expected Data from Admin
            } else {
                logger.info("Hitting UserListMetaData API for Entity {}", entityName);
                String apiPath = "/listRenderer/list/" + listId + "/userListMetaData?preferenceId=" + defaultViewId;
                String userListMetadataResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), null).getResponse().getResponseBody();



            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Listing Default Columns and Filters for Entity " + entityName + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private int getDefaultViewId(String response) {
        JSONArray jsonArr = new JSONArray(response);
        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getBoolean("isdefault")) {
                return jsonArr.getJSONObject(i).getInt("id");
            }
        }

        return -1;
    }
}