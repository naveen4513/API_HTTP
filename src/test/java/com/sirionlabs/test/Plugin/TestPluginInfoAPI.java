package com.sirionlabs.test.Plugin;

import com.google.inject.internal.cglib.core.$Customizer;
import com.sirionlabs.api.plugin.PluginInfoAPI;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.test.reports.TestStatusTransitionReport;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestPluginInfoAPI {
    private final static Logger logger = LoggerFactory.getLogger(TestPluginInfoAPI.class);

    @DataProvider
    public Object[][] getPluginType() {
        return new Object[][]{{1}, {2}};
    }

    @Test
    public void positiveTestCaseForAPI(int pluginId) {
        CustomAssert customAssert = new CustomAssert();
        try {
            APIResponse apiResponse = new PluginInfoAPI().getPluginInfoAPIResponse(pluginId);
            int statusCode = apiResponse.getResponseCode();
            String responseBody = apiResponse.getResponseBody();
            if(ParseJsonResponse.validJsonResponse(responseBody)) {
                ArrayList<String> actualData = new ArrayList<>();
                JSONObject jsonObject = new JSONObject(responseBody).getJSONObject("response");
                actualData.add(jsonObject.getString("id"));
                actualData.add(jsonObject.getString("versionNumber"));
                actualData.add(jsonObject.getString("supportedOS"));
                actualData.add(jsonObject.getString("supportedMSOffice"));
                actualData.add(jsonObject.getString("checkSum"));
                actualData.add(jsonObject.getString("uploadedDate"));
                actualData.add(jsonObject.getString("uploadedDate"));
                actualData.add(jsonObject.getString("uploadDate"));
                List<List<String>> expectedData = new PostgreSQLJDBC().doSelect("SELECT id,version_no,supported_os,supported_ms_office,date_created FROM plugin_version_info WHERE latest_version = true and plugin_type=" + pluginId);
                for (List<String> result : expectedData) {
                    if (!actualData.contains(result.get(0)))
                        customAssert.assertTrue(false, "Actual Data " + actualData + "And Expected Data " + result.get(0) + "different");
                }
            }
        } catch (Exception e) {
            logger.error("Exception While verifying Plugin API Response {}", e.getMessage());
            customAssert.assertTrue(false, "Exception While verifying Plugin API Response");
        }
    }
}
