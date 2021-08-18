package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sirionlabs.helper.api.TestAPIBase.executor;

public class TestPreSigMisc {

    private final static Logger logger = LoggerFactory.getLogger(TestPreSigMisc.class);

    private AdminHelper adminHelperObj = new AdminHelper();
    private ClientSetupHelper clientSetupHelperObj = new ClientSetupHelper();
    private Check checkObj = new Check();

    /*
    TC-C90054: Verify Functions & Services filters on Client Admin for CT Entity.
     */
    @Test
    public void testC90054() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90054: Verify Functions & Services filters on Client Admin for CT Entity");
            adminHelperObj.loginWithClientAdminUser();

            ListRendererConfigure configureObj = new ListRendererConfigure();
            configureObj.hitListRendererConfigure("273");
            String configureResponse = configureObj.getListRendererConfigureJsonStr();

            JSONObject jsonObj = new JSONObject(configureResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

            List<String> allFilterQueryNames = new ArrayList<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                allFilterQueryNames.add(jsonArr.getJSONObject(i).getString("queryName"));
            }

            if (!allFilterQueryNames.contains("functions")) {
                csAssert.assertFalse(true, "Functions Filter not present on Client Admin for CT Entity.");
            }

            if (!allFilterQueryNames.contains("services")) {
                csAssert.assertFalse(true, "Services Filter not present on Client Admin for CT Entity.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90054. " + e.getMessage());
        }

        checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

        csAssert.assertAll();
    }

    /*
    TC-C90067: Verify Functions & Services filters on Sirion Setup Admin for CT Entity.
     */
    @Test
    public void testC90067() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90067: Verify Functions & Services filters on Sirion Setup Admin for CT Entity");

            clientSetupHelperObj.loginWithClientSetupUser();

            String host = ConfigureEnvironment.getEnvironmentProperty("Host");
            host = host.replace(host.substring(0, host.indexOf(".")), "sirion");
            host = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + host
                    + ":" + ConfigureEnvironment.getEnvironmentProperty("Port");

            Map<String, String> headers = new HashMap<>();

            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

            String response = executor.post(host, "/listRenderer/list/273/listJson?clientId=1002", headers, null).getResponse().getResponseBody();

            JSONObject jsonObj = new JSONObject(response);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

            List<String> allFilterQueryNames = new ArrayList<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                allFilterQueryNames.add(jsonArr.getJSONObject(i).getString("queryName"));
            }

            if (!allFilterQueryNames.contains("functions")) {
                csAssert.assertFalse(true, "Functions Filter not present on Sirion Setup Admin for CT Entity.");
            }

            if (!allFilterQueryNames.contains("services")) {
                csAssert.assertFalse(true, "Services Filter not present on Sirion Setup Admin for CT Entity.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90067. " + e.getMessage());
        }

        checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

        csAssert.assertAll();
    }

    /*
    TC-C90068: Verify Agreement type filter on Sirion Setup Admin for Contract Pipeline Report New.
     */
    @Test
    public void testC90068() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90068: Verify Agreement Type Filter on Sirion Setup Admin for Contract Pipeline Report New");

            clientSetupHelperObj.loginWithClientSetupUser();

            String host = ConfigureEnvironment.getEnvironmentProperty("Host");
            host = host.replace(host.substring(0, host.indexOf(".")), "sirion");
            host = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + host
                    + ":" + ConfigureEnvironment.getEnvironmentProperty("Port");

            Map<String, String> headers = new HashMap<>();

            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

            String response = executor.post(host, "/reportRenderer/list/270/listJson?clientId=1002", headers, null).getResponse().getResponseBody();

            JSONObject jsonObj = new JSONObject(response);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

            List<String> allFilterQueryNames = new ArrayList<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                allFilterQueryNames.add(jsonArr.getJSONObject(i).getString("queryName"));
            }

            if (!allFilterQueryNames.contains("agreementType")) {
                csAssert.assertFalse(true, "Agreement Type Filter not present on Sirion Setup Admin for Contract Pipeline Report New.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90068. " + e.getMessage());
        }

        checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

        csAssert.assertAll();
    }

    /*
    TC-C90069: Verify Agreement Type Filter on Client Admin for Contract Pipeline Report - New
     */
    @Test
    public void testC90069() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90069: Verify Agreement Type Filter on Client Admin for Contract Pipeline Report - New");
            adminHelperObj.loginWithClientAdminUser();

            String configureResponse = executor.post("/reportRenderer/list/270/configure", ListRendererConfigure.getHeaders(),
                    null).getResponse().getResponseBody();
            JSONObject jsonObj = new JSONObject(configureResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

            List<String> allFilterQueryNames = new ArrayList<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                allFilterQueryNames.add(jsonArr.getJSONObject(i).getString("queryName"));
            }

            if (!allFilterQueryNames.contains("functions")) {
                csAssert.assertFalse(true, "Functions Filter not present on Client Admin for CT Entity.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90069. " + e.getMessage());
        }

        checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

        csAssert.assertAll();
    }
}