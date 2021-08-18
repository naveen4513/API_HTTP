package com.sirionlabs.helper.APITesting;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.*;

public class TestGetApi {

    private final Logger logger = LoggerFactory.getLogger(TestGetApi.class);

    private final String configFileName;
    private final String configFilePath;

    private String host, scheme;
    private String path;

    private Map<String, String> parameters = new HashMap<>();

    public TestGetApi(String configFileName, String configFilePath) {
        this.configFileName = configFileName;
        this.configFilePath = configFilePath;

        System.out.println("In parent constructor");
    }

    private boolean isInformationSeriesCode(int code) {
        return code >= 100 && code < 200;
    }

    private boolean isSuccessSeriesCode(int code) {
        return code >= 200 && code < 300;
    }

    private boolean isRedirectionSeriesCode(int code) {
        return code >= 300 && code < 400;
    }

    private boolean isClientErrorSeriesCode(int code) {
        return code >= 400 && code < 500;
    }

    private boolean isServerErrorSeriesCode(int code) {
        return code >= 500 && code < 600;
    }

    @BeforeClass
    public void setVariables() {

        host = ConfigureEnvironment.getEnvironmentProperty("host");
        scheme = ConfigureEnvironment.getEnvironmentProperty("scheme");
    }

    @DataProvider
    public Object[][] dataProvider() throws ConfigurationException {

        List<String> sectionNames = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, "apis");

        Object[][] object = new Object[sectionNames.size()][];
        int i = 0;
        for (String sectionName : sectionNames) {
            object[i] = new Object[]{sectionName};
            i++;
        }

        return object;
    }

    @Test(dataProvider = "dataProvider")
    public void testMethod(String sectionName) {
        CustomAssert customAssert = new CustomAssert();


        parameters = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, sectionName);
        if (parameters.containsKey("path")) {
            path = parameters.get("path");
            parameters.remove("path");
        } else {
            logger.error("[path] variable not found in the config file. Exiting");
            throw new SkipException("[path] variable not found in the config file. Exiting");
        }
        parameters.remove("custom");
        parameters.remove("pathvariables");


        new AdminHelper().loginWithClientAdminUser(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());

        validateCustomFlows(customAssert, sectionName);
        validateDeleteForGetMethod(customAssert, sectionName);
        validatePostForGetMethod(customAssert, sectionName);
        validatePutForGetMethod(customAssert, sectionName);
        validateWrongParameters(customAssert, sectionName);
        validateHappyFlow(customAssert, sectionName);
        validateNoAuthentication(customAssert, sectionName);
        negativeFlow(customAssert, sectionName);

        customAssert.assertAll();

    }

    public void validateCustomFlows(CustomAssert customAssert, String sectionName) {

        logger.info("Enter validateCustomFlows");

        try {
            String customTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "custom");
            String[] flows = new String[0];
            if (customTemp != null && !customTemp.isEmpty()) {
                flows = customTemp.split(",");
            }

            for (String flow : flows) {
                String validateStatusCode = null, validateResponseBody = null, validateResponseMessage = null, statusCode = null, responseBody = null, responseMessage = null;

                String flowSectionName = sectionName + "-" + flow;

                Map<String, String> parametersMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, flowSectionName);

                if (!parametersMap.containsKey("validatestatuscode") || !parametersMap.containsKey("validateresponsebody") || !parametersMap.containsKey("validateresponsemessage")) {
                    logger.error("Cannot find [validatestatuscode], [validateresponsebody] or [validateresponsemessage] in configuration file");
                    return;
                }
                validateStatusCode = parametersMap.get("validatestatuscode");
                parametersMap.remove("validatestatuscode");
                validateResponseBody = parametersMap.get("validateresponsebody");
                parametersMap.remove("validateresponsebody");
                validateResponseMessage = parametersMap.get("validateresponsemessage");
                parametersMap.remove("validateresponsemessage");

                if (validateStatusCode != null && validateStatusCode.equalsIgnoreCase("true")) {
                    if (!parametersMap.containsKey("statuscode")) {
                        logger.error("cannot find [statuscode] in config file");
                        return;
                    }
                    statusCode = parametersMap.get("statuscode");
                }
                parametersMap.remove("statuscode");
                if (validateResponseBody != null && validateResponseBody.equalsIgnoreCase("true")) {
                    if (!parametersMap.containsKey("responsebody")) {
                        logger.error("cannot find [responsebody] in config file");
                        return;
                    }
                    responseBody = parametersMap.get("responsebody");
                }
                parametersMap.remove("responsebody");
                if (validateResponseMessage != null && validateResponseMessage.equalsIgnoreCase("true")) {
                    if (!parametersMap.containsKey("responsemessage")) {
                        logger.error("cannot find [responsemessage] in config file");
                        return;
                    }
                    responseMessage = parametersMap.get("responsemessage");
                }
                parametersMap.remove("responsemessage");


                List<String> pathVariables = new ArrayList<>();
                String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
                if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                    pathVariables = Arrays.asList(pathVariableTemp.split(","));

                URIBuilder builder = new URIBuilder();

                for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
                    if (pathVariables.contains(entry.getKey()))
                        path = path.replace(entry.getKey(), ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, flowSectionName, entry.getKey()));
                    else
                        builder.setParameter(entry.getKey(), entry.getValue());
                }
                builder.setScheme(scheme).setHost(host).setPath(path);
                URI uri = builder.build();

                HttpGet getRequest = new HttpGet(uri);

                APIResponse responseObj = new APIResponse();

                logger.info("Preparing Http client for get request for happy flow validation");

                HttpClient client = getClient();
                addMandatoryDefaultHeadersToRequest(getRequest, true, true, true);
                HttpResponse httpResponse = client.execute(getRequest);
                responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
                responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
                responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

                logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

                Header[] allHeaders = httpResponse.getAllHeaders();
                for (Header header : allHeaders) {
                    responseObj.setHeader(header.getName(), header.getValue());
                }

                if (validateStatusCode.equalsIgnoreCase("true"))
                    customAssert.assertTrue(statusCode.contains("-") ?
                            responseObj.getResponseCode() >= Integer.parseInt(statusCode.split("-")[0]) && responseObj.getResponseCode() <= Integer.parseInt(statusCode.split("-")[1])
                            : String.valueOf(responseObj.getResponseCode()).equalsIgnoreCase(statusCode), "---validateHappyFlow---Expected status code : [200 Series], but got : [" + responseObj.getResponseCode() + "]");

                if (validateResponseBody.equalsIgnoreCase("true"))
                    customAssert.assertTrue(responseBody.equalsIgnoreCase("[json]") ? ParseJsonResponse.validJsonResponse(responseObj.getResponseBody())
                            : responseBody.equalsIgnoreCase(responseObj.getResponseBody()), "---validateHappyFlow---Response body not valid json format");

                if (validateResponseMessage.equalsIgnoreCase("true"))
                    customAssert.assertTrue(responseMessage.equalsIgnoreCase(responseObj.getResponseMessage()), "---validateHappyFlow---Response body not valid json format");

                saveResponse(getRequest, responseObj);
            }

        } catch (Exception e) {
            e.getStackTrace();
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit validateCustomFlows");

    }

    public void validatePutForGetMethod(CustomAssert customAssert, String sectionName) {

        logger.info("Enter validatePutForGetMethod");

        try {
            List<String> pathVariables = new ArrayList<>();
            String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
            if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                pathVariables = Arrays.asList(pathVariableTemp.split(","));
            URIBuilder builder = new URIBuilder();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (pathVariables.contains(entry.getKey()))
                    path = path.replace(entry.getKey(), ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, entry.getKey()));
                else
                    builder.setParameter(entry.getKey(), entry.getValue());
            }

            builder.setScheme(scheme).setHost(host).setPath(path);
            URI uri = builder.build();

            HttpPut httpPut = new HttpPut(uri);

            APIResponse responseObj = new APIResponse();

            logger.info("Preparing Http client for get request for PUT method validation");

            HttpClient client = getClient();
            addMandatoryDefaultHeadersToRequest(httpPut, true, true, true);
            HttpResponse httpResponse = client.execute(httpPut);
            responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
            responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
            responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

            logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

            Header[] allHeaders = httpResponse.getAllHeaders();
            for (Header header : allHeaders) {
                responseObj.setHeader(header.getName(), header.getValue());
            }

            customAssert.assertTrue(responseObj.getResponseCode() == 405, "---validatePutForGetMethod---Expected status code : [405 method not allowed], but got : [" + responseObj.getResponseCode() + "]");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseObj.getResponseBody()) && !responseObj.getResponseBody().contains("applicationError"), "---validatePutForGetMethod---Response body not valid json format");

        } catch (Exception e) {
            e.getStackTrace();
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit validatePutForGetMethod");

    }

    public void validatePostForGetMethod(CustomAssert customAssert, String sectionName) {

        logger.info("Enter validatePostForGetMethod");

        try {
            List<String> pathVariables = new ArrayList<>();
            String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
            if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                pathVariables = Arrays.asList(pathVariableTemp.split(","));
            URIBuilder builder = new URIBuilder();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (pathVariables.contains(entry.getKey()))
                    path = path.replace(entry.getKey(), ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, entry.getKey()));
                else
                    builder.setParameter(entry.getKey(), entry.getValue());
            }

            builder.setScheme(scheme).setHost(host).setPath(path);
            URI uri = builder.build();

            HttpPost httpPost = new HttpPost(uri);

            APIResponse responseObj = new APIResponse();

            logger.info("Preparing Http client for get request for POST method validation");

            HttpClient client = getClient();
            addMandatoryDefaultHeadersToRequest(httpPost, true, true, true);
            HttpResponse httpResponse = client.execute(httpPost);
            responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
            responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
            responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

            logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

            Header[] allHeaders = httpResponse.getAllHeaders();
            for (Header header : allHeaders) {
                responseObj.setHeader(header.getName(), header.getValue());
            }

            customAssert.assertTrue(responseObj.getResponseCode() == 405, "---validatePostForGetMethod---Expected status code : [405 method not allowed], but got : [" + responseObj.getResponseCode() + "]");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseObj.getResponseBody()) && !responseObj.getResponseBody().contains("applicationError"), "---validatePostForGetMethod---Response body not valid json format");

        } catch (Exception e) {
            e.getStackTrace();
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit validatePostForGetMethod");

    }

    public void validateDeleteForGetMethod(CustomAssert customAssert, String sectionName) {

        logger.info("Enter validateDeleteForGetMethod");

        try {
            List<String> pathVariables = new ArrayList<>();
            String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
            if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                pathVariables = Arrays.asList(pathVariableTemp.split(","));
            URIBuilder builder = new URIBuilder();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (pathVariables.contains(entry.getKey()))
                    path = path.replace(entry.getKey(), ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, entry.getKey()));
                else
                    builder.setParameter(entry.getKey(), entry.getValue());
            }

            builder.setScheme(scheme).setHost(host).setPath(path);
            URI uri = builder.build();

            HttpDelete httpDelete = new HttpDelete(uri);

            APIResponse responseObj = new APIResponse();

            logger.info("Preparing Http client for get request for Delete method validation");

            HttpClient client = getClient();
            addMandatoryDefaultHeadersToRequest(httpDelete, true, true, true);
            HttpResponse httpResponse = client.execute(httpDelete);
            responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
            responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
            responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

            logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

            Header[] allHeaders = httpResponse.getAllHeaders();
            for (Header header : allHeaders) {
                responseObj.setHeader(header.getName(), header.getValue());
            }

            customAssert.assertTrue(responseObj.getResponseCode() == 405, "---validateDeleteForGetMethod---Expected status code : [405 method not allowed], but got : [" + responseObj.getResponseCode() + "]");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseObj.getResponseBody()) && !responseObj.getResponseBody().contains("applicationError"), "---validateDeleteForGetMethod---Response body not valid json format");

        } catch (Exception e) {
            e.getStackTrace();
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit validateDeleteForGetMethod");

    }

    public void validateWrongParameters(CustomAssert customAssert, String sectionName) {

        logger.info("Enter validateWrongParameters");

        try {
            List<String> pathVariables = new ArrayList<>();
            String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
            if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                pathVariables = Arrays.asList(pathVariableTemp.split(","));

            URIBuilder builder = new URIBuilder();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (pathVariables.contains(entry.getKey()))
                    path = path.replace(entry.getKey(), ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, entry.getKey()));
            }
            builder.setScheme(scheme).setHost(host).setPath(path);
//            for (Map.Entry<String, String> entry : parameters.entrySet()) {
//                builder.setParameter(entry.getKey(), entry.getValue());
//            }

            //adding wrong path variable
            builder.setParameter("testWrongVariable", "123");

            URI uri = builder.build();

            HttpGet getRequest = new HttpGet(uri);

            APIResponse responseObj = new APIResponse();

            logger.info("Preparing Http client for get request for wrong parameters validation");

            HttpClient client = getClient();
            addMandatoryDefaultHeadersToRequest(getRequest, true, true, true);
            HttpResponse httpResponse = client.execute(getRequest);
            responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
            responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
            responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

            logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

            Header[] allHeaders = httpResponse.getAllHeaders();
            for (Header header : allHeaders) {
                responseObj.setHeader(header.getName(), header.getValue());
            }

            customAssert.assertTrue(isClientErrorSeriesCode(responseObj.getResponseCode()), "---validateWrongParameters---Expected status code : [400 Series], but got : [" + responseObj.getResponseCode() + "]");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseObj.getResponseBody()) && !responseObj.getResponseBody().contains("applicationError"), "---validateWrongParameters---Response body not valid json format");

        } catch (Exception e) {
            e.getStackTrace();
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit validateWrongParameters");

    }

    public void negativeFlow(CustomAssert customAssert, String sectionName) {

        logger.info("Enter negativeFlow");
        try {
            List<String> pathVariables = new ArrayList<>();
            String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
            if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                pathVariables = Arrays.asList(pathVariableTemp.split(","));

            Map<Integer, List<String>> allValues = new HashMap<>();
            Map<Integer, String> paramNamesMap = new HashMap<>();
            int[] counterArray = new int[parameters.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                allValues.put(i, ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, sectionName + "-" + entry.getKey().toLowerCase()));
                allValues.get(i).add(null);
                paramNamesMap.put(i, entry.getKey());
                counterArray[i] = allValues.get(i).size() - 1;
                i++;
            }

            List<Map<String, String>> list = new ArrayList<>();

            int length = counterArray.length;
            int l = length - 1;
            while (l >= 0) {
                Map<String, String> map = new HashMap<>();
                l = length - 1;

                for (i = 0; i <= l; i++) {
                    map.put(paramNamesMap.get(i), allValues.get(i).get(counterArray[i]));
                }

                list.add(map);
                logger.info("list length {}", list.size());


                counterArray[l]--;
                while (counterArray[l] == -1) {
                    counterArray[l] = allValues.get(l).size() - 1;
                    l--;
                    if (l < 0)
                        break;
                    counterArray[l]--;
                }
            }


            for (Map<String, String> map : list) {

                URIBuilder builder = new URIBuilder();

                for (Map.Entry<String, String> entry : map.entrySet()) {

                    if (pathVariables.contains(entry.getKey()))
                        path = path.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
                    else
                        builder.setParameter(entry.getKey(), entry.getValue());
                }

                builder.setScheme(scheme).setHost(host).setPath(path);

                URI uri = builder.build();
                HttpGet getRequest = new HttpGet(uri);

                APIResponse responseObj = new APIResponse();

                logger.info("Preparing Http client for get request for negative flows");
                HttpClient client = getClient();

                addMandatoryDefaultHeadersToRequest(getRequest, true, true, true);
                HttpResponse httpResponse = client.execute(getRequest);
                responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
                responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
                responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

                logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

                Header[] allHeaders = httpResponse.getAllHeaders();
                for (Header header : allHeaders) {
                    responseObj.setHeader(header.getName(), header.getValue());
                }

//                if(isSuccessSeriesCode(responseObj.getResponseCode())){
//                    customAssert.assertTrue(!ParseJsonResponse.isEmptyJson(responseObj.getResponseBody())&&!ParseJsonResponse.containsApplicationError(responseObj.getResponseBody()), "---negativeFlow---Response body not valid json format or the response body is blank");
//                }

                customAssert.assertTrue(isSuccessSeriesCode(responseObj.getResponseCode()) || isClientErrorSeriesCode(responseObj.getResponseCode()), "---negativeFlow---Expected status code : [200 or 400 Series], but got : [" + responseObj.getResponseCode() + "] for [" + map.toString() + "]");
                customAssert.assertTrue(!ParseJsonResponse.isEmptyJson(responseObj.getResponseBody()) && !ParseJsonResponse.containsApplicationError(responseObj.getResponseBody()) && ParseJsonResponse.validJsonResponse(responseObj.getResponseBody()), "---negativeFlow---Response body [" + responseObj.getResponseBody().substring(0, 100) + "] not valid json format or the response body is blank for [" + map.toString() + "]");

            }
        } catch (Exception e) {
            logger.error("Exception caught in negativeFlow()");
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit negativeFlow");

    }

    public void validateNoAuthentication(CustomAssert customAssert, String sectionName) {

        logger.info("Enter validateNoAuthentication");
        String expiredAuthentication = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjoiTXNUTjFvbW1CWUJ6UG01OWsxYTgxR2JQdm40TVI1aWdFT1IyRHdFNDJ0bTlzOUdpQStJVVFjc0o4S05jMzBzUTVFcldHWTZlYkJSdUZjQ01TdFU5ZFJ3dDlpS2d6QThHc2ZnbFNMY0VjYXVOdzJxVWJnczZER1FMMDMrYWMxa3hRUURpVmVoU0RlSWRsSEp5bk5jVHVnPT0iLCJpc3MiOiJzaXJpb24iLCJleHAiOjE1Nzg1NzA0MTAsImxvZ2luTWVjaGFuaXNtIjoxfQ.mj93U3Z1MqiGVnZ7FoHuOcE012yjxV4ltw7WdEFqwHg";

        try {
            List<String> pathVariables = new ArrayList<>();
            String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
            if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                pathVariables = Arrays.asList(pathVariableTemp.split(","));

            URIBuilder builder = new URIBuilder();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (pathVariables.contains(entry.getKey()))
                    path = path.replace(entry.getKey(), ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, entry.getKey()));
                else
                    builder.setParameter(entry.getKey(), entry.getValue());
            }

            builder.setScheme(scheme).setHost(host).setPath(path);
            URI uri = builder.build();

            int caseCount = 4;
            while (--caseCount >= 0) {

                HttpGet getRequest = new HttpGet(uri);

                if (caseCount == 3)
                    addMandatoryDefaultHeadersToRequest(getRequest, false, true, true);
                else if (caseCount == 2)
                    addMandatoryDefaultHeadersToRequest(getRequest, false, false, false);
                else if (caseCount == 1)
                    getRequest.addHeader("Authorization", "");
                else
                    getRequest.addHeader("Authorization", expiredAuthentication);

                APIResponse responseObj = new APIResponse();

                logger.info("Preparing Http client for get request for happy flow validation");

                HttpClient client = getClient();
                HttpResponse httpResponse = client.execute(getRequest);
                responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
                responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
                responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

                logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

                Header[] allHeaders = httpResponse.getAllHeaders();
                for (Header header : allHeaders) {
                    responseObj.setHeader(header.getName(), header.getValue());
                }

                customAssert.assertTrue(responseObj.getResponseCode() == 401, "---validateNoAuthentication---CASE[" + caseCount + "] Expected status code : [401], but got : [" + responseObj.getResponseCode() + "]");
                customAssert.assertTrue(responseObj.getResponseBody().contains("Unauthorized"), "---validateNoAuthentication---CASE[" + caseCount + "] [Unauthorised] not found in Response body");

            }
        } catch (Exception e) {
            e.getStackTrace();
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit validateNoAuthentication");
    }

    public void validateHappyFlow(CustomAssert customAssert, String sectionName) {

        logger.info("Enter validateHappyFlow");

        try {
            List<String> pathVariables = new ArrayList<>();
            String pathVariableTemp = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, "pathvariables");
            if (pathVariableTemp != null && !pathVariableTemp.isEmpty())
                pathVariables = Arrays.asList(pathVariableTemp.split(","));

            URIBuilder builder = new URIBuilder();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (pathVariables.contains(entry.getKey()))
                    path = path.replace(entry.getKey(), ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, sectionName, entry.getKey()));
                else
                    builder.setParameter(entry.getKey(), entry.getValue());
            }

            builder.setScheme(scheme).setHost(host).setPath(path);
            URI uri = builder.build();

            HttpGet getRequest = new HttpGet(uri);

            APIResponse responseObj = new APIResponse();

            logger.info("Preparing Http client for get request for happy flow validation");

            HttpClient client = getClient();
            addMandatoryDefaultHeadersToRequest(getRequest, true, true, true);
            HttpResponse httpResponse = client.execute(getRequest);
            responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
            responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
            responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

            logger.info("Response code {}, Response body {}", responseObj.getResponseCode(), responseObj.getResponseBody());

            Header[] allHeaders = httpResponse.getAllHeaders();
            for (Header header : allHeaders) {
                responseObj.setHeader(header.getName(), header.getValue());
            }

            customAssert.assertTrue(isSuccessSeriesCode(responseObj.getResponseCode()), "---validateHappyFlow---Expected status code : [200 Series], but got : [" + responseObj.getResponseCode() + "]");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseObj.getResponseBody()) && !responseObj.getResponseBody().contains("applicationError"), "---validateHappyFlow---Response body not valid json format");

        } catch (Exception e) {
            e.getStackTrace();
            customAssert.assertTrue(false, "Exception occurred. Could complete the request");
        }

        logger.info("Exit validateHappyFlow");
    }

    private void addMandatoryDefaultHeadersToRequest(HttpRequest request, boolean withAuth, boolean withXML, boolean withCSRF) {
        if (withAuth)
            request.addHeader("Authorization", Check.getAuthorization());
        if (withXML)
            request.addHeader("X-Requested-With", "XMLHttpRequest");
        if (withCSRF)
            request.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
    }

    private void saveResponse(HttpRequestBase http, APIResponse apiResponse) {


    }

    private HttpClient getClient() {
        HttpClient client;
        if (scheme.equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            client = oAPIUtils.getHttpsClient();
        } else
            client = HttpClientBuilder.create().build();

        return client;
    }

}
