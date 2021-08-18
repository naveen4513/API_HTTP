package com.sirionlabs.utils.commonUtils;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by pradeep on 30/6/17.
 */
public class APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(APIUtils.class);
    public static String accessToken = null;
    public static int apiCounter = 0;
    static String apiResponseTime;
    String apiStatusCode;

    public static Boolean validJsonResponse(String strResponse) {
        return validJsonResponse(strResponse, "");
    }

    public static Boolean validJsonResponse(String strResponse, String additionalInfo) {
        // To check if the response is valid JSON or HTML?
        Boolean isValidJson = false;
        String responseAsString = strResponse;
        try {
            Object response = new JSONTokener(responseAsString).nextValue();

            if ((response instanceof JSONObject) || (response instanceof JSONArray)) {
                isValidJson = true;
            }
        } catch (Exception e) {
            logger.error("Not a valid JSON response {}", additionalInfo);
        }
        return isValidJson;
    }

    public static Boolean isApplicationErrorInResponse(String strResponse) {

        Boolean isApplicationError = false;
        try {
            JSONObject jObj = new JSONObject(strResponse);
            if (jObj.has("header")) {
                JSONObject jsonResponseData = jObj.getJSONObject("header").getJSONObject("response");
                if (jsonResponseData.get("status").toString().toLowerCase().equals("applicationerror")) {
                    isApplicationError = true;
                    logger.info("Error Message in response {}", jsonResponseData.get("status").toString());
                }

            }

        } catch (Exception e) {
            logger.error("Exception while Checking ApplicationError in response : {}", strResponse);
        }
        return isApplicationError;
    }

    public static boolean isPermissionDeniedInResponse(String jsonResponseStr) {
        boolean isPermissionDenied = false;
        JSONObject responseObj = new JSONObject(jsonResponseStr);

        if (responseObj.has("errorMessage") && responseObj.getString("errorMessage").contains("do not have access")) {
            isPermissionDenied = true;
        }
        if (responseObj.has("header")) {
            JSONObject jsonResponseData = responseObj.getJSONObject("header").getJSONObject("response");
            if (jsonResponseData.get("status").toString().toLowerCase().equals("applicationerror")
                    &&
                    (jsonResponseData.has("errorMessage") && jsonResponseData.getString("errorMessage").contains("do not have access"))
                    || jsonResponseData.has("errorMessage") && jsonResponseData.getString("errorMessage").toLowerCase().trim().
                    contains("either you do not have the required permissions or requested page does not exist anymore")
                    || jsonResponseData.has("errorMessage") && jsonResponseData.getString("errorMessage").contains("deleted")) {
                isPermissionDenied = true;
                logger.error("Error Message in response {}", jsonResponseData.get("status").toString());
            }

        }
        return isPermissionDenied;
    }

    public static HttpResponse postRequest(HttpPost httpPostRequest, String requestPayload) {
        return postRequest(httpPostRequest, requestPayload, false);
    }

    public static HttpResponse postRequest(HttpPost httpPostRequest, String requestPayload, boolean useSessionId) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
            Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");

            logger.debug("--------------------------------------------------------------------------\n");
            logger.debug("Making HttpPost Request with hostName: {} , portNumber:  {} , protocolScheme: {}", hostName, portNumber, protocolScheme);
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            if (ConfigureEnvironment.useCookies) {
                logger.debug("--------------------Cookies Flag is True------------------------------");
                httpPostRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
            } else {
                logger.debug("--------------------Cookies Flag is False------------------------------");
                logger.debug("Authorization is : {}", Check.getAuthorization());
                logger.debug("JSession Id is : {}", Check.getJSessionId());
                if (useSessionId) {
                    String cookie = "Authorization=\"" + Check.getAuthorization() + "\"; JSESSIONID=" + Check.getJSessionId();
                    httpPostRequest.addHeader("Cookie", cookie);
                } else
                    httpPostRequest.addHeader("Authorization", Check.getAuthorization());
            }
            if (ConfigureEnvironment.useCSRFToken) {
                logger.debug("--------------------useCSRFToken Flag is true------------------------------");
                logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                httpPostRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            }
            httpPostRequest.addHeader("X-Requested-With", "XMLHttpRequest");

            if (requestPayload != null)
                httpPostRequest.setEntity(new StringEntity(requestPayload, "UTF-8"));

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpPostRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();
            }
            //APIUtils utilObj = new APIUtils();
            String time = watch.toString();
            apiResponseTime = changeTimeIntoSec(time);
            logger.debug("API Response Time is : [{}]", apiResponseTime);
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpPostRequest.getURI());
        }
        return response;
    }

    public static HttpResponse postRequestWithoutAuth(HttpPost httpPostRequest) {
        return postRequestWithoutAuth(httpPostRequest, null);
    }

    public static HttpResponse postRequestWithoutAuth(HttpPost httpPostRequest, String requestPayload) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
            Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");

            logger.debug("--------------------------------------------------------------------------\n");
            logger.debug("Making HttpPost Request with hostName: {} , portNumber:  {} , protocolScheme: {}", hostName, portNumber, protocolScheme);
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            httpPostRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            httpPostRequest.setEntity(new StringEntity(requestPayload, "UTF-8"));

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpPostRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();
            }
            //APIUtils utilObj = new APIUtils();
            String time = watch.toString();
            apiResponseTime = changeTimeIntoSec(time);
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpPostRequest.getURI());
        }
        return response;
    }

    public static HttpResponse deleteRequestWithoutAuth(HttpDelete httpDeleteRequest) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
            Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");

            logger.debug("--------------------------------------------------------------------------\n");
            logger.debug("Making HttpDelete Request with hostName: {} , portNumber:  {} , protocolScheme: {}", hostName, portNumber, protocolScheme);
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpDeleteRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();
            }
            //APIUtils utilObj = new APIUtils();
            String time = watch.toString();
            apiResponseTime = changeTimeIntoSec(time);
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpDeleteRequest.getURI());
        }
        return response;
    }

    public static String changeTimeIntoSec(String time) {

        String responseTime[] = time.toString().trim().split(":");
        Integer hourToSec = Integer.parseInt(responseTime[0]) * 3600;
        Integer minToSec = Integer.parseInt(responseTime[1]) * 60;
        String splitSecAndMilliSec[] = responseTime[2].split("\\.");
        String milliSec = splitSecAndMilliSec[1];
        Integer totalSec = Integer.parseInt(splitSecAndMilliSec[0]) + minToSec + hourToSec;
        String finalResponseTimeInSec = totalSec.toString() + "." + milliSec;

        return finalResponseTimeInSec;
    }

    public static HttpEntity createMultipartEntityBuilder(File fileToUpload, Map<String, String> textBodyMap) {
        return createMultipartEntityBuilder("multipartFile", fileToUpload, textBodyMap);
    }

    public static HttpEntity createMultipartEntityBuilder(String binaryBodyName, File fileToUpload, Map<String, String> textBodyMap) {
        HttpEntity entity = null;
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
            }
            builder.addBinaryBody(binaryBodyName, fileToUpload, ContentType.parse("application/vnd.ms-excel.sheet.macroEnabled.12"), fileToUpload.getName());
            entity = builder.build();
        } catch (Exception e) {
            logger.error("Exception while Creating Multiple Part Entity Builder. {}", e.getMessage());
        }
        return entity;
    }

    public static HttpEntity createMultipartEntityBuilder(String binaryBodyName, File fileToUpload, String contentType, Map<String, String> textBodyMap) {
        HttpEntity entity = null;
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.setBoundary("----WebKitFormBoundarykZn8DRIyNRLBLQrV");
            for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
            }
            builder.addBinaryBody(binaryBodyName, fileToUpload, ContentType.parse(contentType), fileToUpload.getName());
            entity = builder.build();
        } catch (Exception e) {
            logger.error("Exception while Creating Multiple Part Entity Builder. {}", e.getMessage());
        }
        return entity;
    }

    public static HttpEntity createMultipartEntityBuilder(String name, byte[] isBody, String fileName, Map<String, String> textBodyMap) {
        HttpEntity entity = null;
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
            }
            builder.addBinaryBody(name, isBody, ContentType.DEFAULT_BINARY, fileName);
            entity = builder.build();
        } catch (Exception e) {
            logger.error("Exception while Creating Multiple Part Entity Builder. {}", e.getMessage());
        }
        return entity;
    }

    // for putting latency in the API Call
    private static void addLatency() throws InterruptedException {
        Thread.sleep(ConfigureEnvironment.latencyTime);
    }

    public static HttpResponse getRequest(HttpGet httpGetRequest) {
        return getRequest(httpGetRequest, false);
    }

    public static HttpResponse getRequestWithoutAuthorization(HttpGet httpGetRequest) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
            Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpGetRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();
            }
            apiResponseTime = changeTimeIntoSec(watch.toString());
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpGetRequest.getURI());
        }
        return response;
    }

    public static HttpResponse getRequestForDownloadFile(HttpGet httpGetRequest) {
        try {
            CookieStore httpCookieStore;
            HttpClientBuilder builder;
            HttpClient httpClient;
            HttpHost target = new APIUtils().generateHttpTargetHost();
            httpCookieStore = new BasicCookieStore();
            builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);

            // Read Proxy config from File and control HttpClient
            if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
                APIUtils oAPIUtils = new APIUtils();
                httpClient = oAPIUtils.getHttpsClient();
            } else {
                if (ConfigureEnvironment.isProxyEnabled) {
                    HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                    httpClient = builder.setProxy(proxy).build();
                } else {
                    httpClient = builder.build();
                }
            }

            logger.info("API Counter: {}", ++apiCounter);
            return httpClient.execute(target, httpGetRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting GetRequest for Download File. " + e.getMessage());
        }

        return null;
    }

    public static HttpResponse getRequest(HttpGet httpGetRequest, boolean useSessionId) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
            Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            if (ConfigureEnvironment.useCookies) {
                httpGetRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
            } else {
                logger.debug("Authorization is : {}", Check.getAuthorization());
                logger.debug("JSession Id is : {}", Check.getJSessionId());
                if (useSessionId) {
                    String cookie = "Authorization=\"" + Check.getAuthorization() + "\";JSESSIONID=" + Check.getJSessionId();
                    httpGetRequest.addHeader("Cookie", cookie);
                } else
                    httpGetRequest.addHeader("Authorization", Check.getAuthorization());
            }

            if (ConfigureEnvironment.useCSRFToken) {
                logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                httpGetRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            }
            httpGetRequest.addHeader("X-Requested-With", "XMLHttpRequest");

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpGetRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();

            }
            apiResponseTime = changeTimeIntoSec(watch.toString());
            logger.debug("API Response Time is : [{}]", apiResponseTime);
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpGetRequest.getURI());
        }
        return response;
    }

    public static HttpResponse getRequest(HttpGet httpGetRequest, HttpHost target, boolean useSessionId) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }
        }

        HttpResponse response = null;

        try {
            if (ConfigureEnvironment.useCookies) {
                httpGetRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
            } else {
                logger.debug("Authorization is : {}", Check.getAuthorization());
                logger.debug("JSession Id is : {}", Check.getJSessionId());
                if (useSessionId) {
                    String cookie = "Authorization=\"" + Check.getAuthorization() + "\";JSESSIONID=" + Check.getJSessionId();
                    httpGetRequest.addHeader("Cookie", cookie);
                } else
                    httpGetRequest.addHeader("Authorization", Check.getAuthorization());
            }

            if (ConfigureEnvironment.useCSRFToken) {
                logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                httpGetRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            }
            httpGetRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpGetRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();

            }
            apiResponseTime = changeTimeIntoSec(watch.toString());
            logger.debug("API Response Time is : [{}]", apiResponseTime);
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpGetRequest.getURI());
        }
        return response;
    }

    public static HttpResponse postRequestSisense(HttpPost httpPostRequest, String requestPayload, boolean useSessionId) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {

            httpClient = HttpClientBuilder.create().build();
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("SisenseHost");
            Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("SisensePort"));
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("SisenseScheme");

            logger.debug("--------------------------------------------------------------------------\n");
            logger.debug("Making HttpPost Request with hostName: {} , portNumber:  {} , protocolScheme: {}", hostName, portNumber, protocolScheme);
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            httpPostRequest.setEntity(new StringEntity(requestPayload, "UTF-8"));

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpPostRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();
            }
            //APIUtils utilObj = new APIUtils();
            String time = watch.toString();
            apiResponseTime = changeTimeIntoSec(time);
            logger.debug("API Response Time is : [{}]", apiResponseTime);
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpPostRequest.getURI());
        }
        return response;
    }

    protected HttpEntity createMultipartEntityBuilder(Map<String, String> textBodyMap) {
        HttpEntity entity = null;
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.setBoundary("----WebKitFormBoundarykggQHAjpfi6idAX5");
            for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
            }
            entity = builder.build();
        } catch (Exception e) {
            logger.error("Exception while Creating Multiple Part Entity Builder. {}", e.getMessage());
        }
        return entity;
    }

    protected HttpEntity createMultipartEntityBuilder(Map<String, String> textBodyMap, String textboundary) {
        HttpEntity entity = null;
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.setBoundary(textboundary);
            for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
            }
            entity = builder.build();
        } catch (Exception e) {
            logger.error("Exception while Creating Multiple Part Entity Builder. {}", e.getMessage());
        }
        return entity;
    }

    public String getApiStatusCode() {
        return apiStatusCode;
    }

    public String getApiResponseTime() {
        return apiResponseTime;
    }

    public String getStatusCodeFrom(HttpResponse httpResponse) {
        return String.valueOf(httpResponse.getStatusLine().getStatusCode());
    }

    /**
     * @param fileToUpload
     * @param textBodyMap
     * @return HttpEntity object
     * @apiNote This Method will return the Multi Part Data to upload the XLS sheet , It uses the Map to create the multipart Entity
     */
    public HttpEntity multipartEntityBuilder(File fileToUpload, Map<String, String> textBodyMap) {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
            builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
        }
        builder.addBinaryBody("multipartFile", fileToUpload, ContentType.DEFAULT_BINARY, fileToUpload.getName());
        final HttpEntity entity = builder.build();

        return entity;
    }

    public HttpEntity multiPartFormData(Map<String, String> textBodyMap, Map<String, File> fileUpload) throws FileNotFoundException {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
            builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
        }
        if (fileUpload != null) {
            // This attaches the file to the POST:
            for (Map.Entry<String, File> entry : fileUpload.entrySet()) {
                builder.addBinaryBody(
                        entry.getKey(),
                        new FileInputStream(entry.getValue()),
                        ContentType.APPLICATION_OCTET_STREAM,
                        entry.getValue().getName()
                );
            }
        }

        final HttpEntity entity = builder.build();

        return entity;
    }

    public HttpEntity multiPartFormData(Map<String, String> textBodyMap, Map<String, File> fileUpload, String contentType) throws FileNotFoundException {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, String> entry : textBodyMap.entrySet()) {
            builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN);
        }
        if (fileUpload != null) {
            // This attaches the file to the POST:
            for (Map.Entry<String, File> entry : fileUpload.entrySet()) {
                builder.addBinaryBody(
                        entry.getKey(),
                        new FileInputStream(entry.getValue()),
                        ContentType.create(contentType),
                        entry.getValue().getName()
                );
            }
        }

        final HttpEntity entity = builder.build();

        return entity;
    }

    public HttpEntity multiPartFormData(Map<String, String> textBodyMap) throws FileNotFoundException {
        return multiPartFormData(textBodyMap, null);
    }

    /**
     * @param nameValueMap
     * @return UrlEncodedFormEntity object
     * @throws UnsupportedEncodingException
     * @apiNote This Method will return the entity for Name Value Pair Data to download the XLS sheet
     * @apiNote It uses the Map to create the List of BasicNameValuePair
     */
    public HttpEntity generateNameValuePairFormDataEntity(Map<String, String> nameValueMap) throws UnsupportedEncodingException {
        List<BasicNameValuePair> data = new ArrayList<>();
        for (Map.Entry<String, String> entry : nameValueMap.entrySet()) {
            data.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return new UrlEncodedFormEntity(data, "UTF-8");
    }

    /**
     * @param resourceUri
     * @param acceptsHeader
     * @param contentTypeHeader
     * @return HttpPost object
     * @apiNote This method is for creating HTTP post request using the resource URI String
     */
    public HttpPost generateHttpPostRequestWithQueryString(String resourceUri, String acceptsHeader, String contentTypeHeader) {
        logger.debug("Generating Http post Request for resourceUri : [ {} ]", resourceUri);
        HttpPost httpPostRequest = new HttpPost(resourceUri);

        httpPostRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        if (!acceptsHeader.equalsIgnoreCase("")) {
            httpPostRequest.addHeader("Accept", acceptsHeader);
        }
        if (!contentTypeHeader.equalsIgnoreCase("")) {
            httpPostRequest.addHeader("Content-Type", contentTypeHeader);
        }
        httpPostRequest.addHeader("Accept-Encoding", "gzip, deflate");

        if (ConfigureEnvironment.useCookies) {
            logger.debug("--------------------Cookies Flag is True------------------------------");
            httpPostRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
        } else {
            logger.debug("--------------------Cookies Flag is False------------------------------");
            logger.debug("Authorization is : {}", Check.getAuthorization());
            logger.debug("JSession Id is : {}", Check.getJSessionId());

            httpPostRequest.addHeader("Authorization", Check.getAuthorization());
        }
        if (ConfigureEnvironment.useCSRFToken) {
            logger.debug("--------------------useCSRFToken Flag is true------------------------------");
            logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            httpPostRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        }
        httpPostRequest.addHeader("X-Requested-With", "XMLHttpRequest");

        return httpPostRequest;
    }

    public HttpPost generateHttpPostRequestWithQueryString(String resourceUri, String acceptsHeader, String contentTypeHeader, String acceptEncodingHeader) {
        logger.debug("Generating Http post Request for resourceUri : [ {} ]", resourceUri);
        HttpPost httpPostRequest = new HttpPost(resourceUri);

        httpPostRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        if (!acceptsHeader.equalsIgnoreCase("")) {
            httpPostRequest.addHeader("Accept", acceptsHeader);
        }
        if (!contentTypeHeader.equalsIgnoreCase("")) {
            httpPostRequest.addHeader("Content-Type", contentTypeHeader);
        }
		/*if (!contentLengthHeader.equalsIgnoreCase("")) {
			httpPostRequest.addHeader("Content-Length", contentLengthHeader);
		}*/
        httpPostRequest.addHeader("Accept-Encoding", acceptEncodingHeader);

        if (ConfigureEnvironment.useCookies) {
            logger.debug("--------------------Cookies Flag is True------------------------------");
            httpPostRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
        } else {
            logger.debug("--------------------Cookies Flag is False------------------------------");
            logger.debug("Authorization is : {}", Check.getAuthorization());
            logger.debug("JSession Id is : {}", Check.getJSessionId());

            httpPostRequest.addHeader("Authorization", Check.getAuthorization());
        }
        if (ConfigureEnvironment.useCSRFToken) {
            logger.debug("--------------------useCSRFToken Flag is true------------------------------");
            logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            httpPostRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        }
        httpPostRequest.addHeader("X-Requested-With", "XMLHttpRequest");

        return httpPostRequest;
    }

    public HttpPost generateHttpPostRequestWithQueryStringAndPayload(String resourceUri, String acceptsHeader, String contentTypeHeader, String payload) {
        logger.debug("Generating Http post Request for resourceUri : [ {} ]", resourceUri);
        HttpPost httpPostRequest = new HttpPost(resourceUri);

        httpPostRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        if (!acceptsHeader.equalsIgnoreCase("")) {
            httpPostRequest.addHeader("Accept", acceptsHeader);
        }
        if (!contentTypeHeader.equalsIgnoreCase("")) {
            httpPostRequest.addHeader("Content-Type", contentTypeHeader);
        }
        httpPostRequest.addHeader("Accept-Encoding", "gzip, deflate");

        if (ConfigureEnvironment.useCookies) {
            logger.debug("--------------------Cookies Flag is True------------------------------");
            httpPostRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
        } else {
            logger.debug("--------------------Cookies Flag is False------------------------------");
            logger.debug("Authorization is : {}", Check.getAuthorization());
            logger.debug("JSession Id is : {}", Check.getJSessionId());

            httpPostRequest.addHeader("Authorization", Check.getAuthorization());
        }
        if (ConfigureEnvironment.useCSRFToken) {
            logger.debug("--------------------useCSRFToken Flag is true------------------------------");
            logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            httpPostRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        }
        httpPostRequest.addHeader("X-Requested-With", "XMLHttpRequest");
        httpPostRequest.setEntity(new StringEntity(payload, "UTF-8"));
        return httpPostRequest;
    }

    /**
     * @return HttpHost
     * @apiNote This method is to return the HttpHost or http target host , it will read all information from config files
     */
    public HttpHost generateHttpTargetHost() {
        String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
        Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
        String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");

        logger.debug("--------------------------------------------------------------------------\n");
        logger.debug("Making HttpPost Request with hostName: {} , portNumber:  {} , protocolScheme: {}", hostName, portNumber, protocolScheme);
        return new HttpHost(hostName, portNumber, protocolScheme);
    }

    /**
     * <h2>This Methid will download the File from API Response in the specified location in parameter { sOutputFile }</h2>
     *
     * @param sOutputFile
     * @param target
     * @param httpRequest
     * @return HttpResponse object
     * @throws Exception
     * @apiNote This Methodd will download the File from API Response
     */
    public HttpResponse downloadAPIResponseFile(String sOutputFile, HttpHost target, HttpRequestBase httpRequest) throws Exception {
        logger.debug("Started Download File from API Response for HttpHost : [ {} ] , HttpPost request : [ {} ] and download File Location will be : [ {} ]", target, httpRequest, sOutputFile);

        //Create File to be downloaded
        File outputFile = createDownloadFile(sOutputFile);

        HttpResponse response = null;
        try {
            CookieStore httpCookieStore;
            HttpClientBuilder builder;
            HttpClient httpClient;

            httpCookieStore = new BasicCookieStore();
            builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);

            // Read Proxy config from File and control HttpClient
            if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
                APIUtils oAPIUtils = new APIUtils();
                httpClient = oAPIUtils.getHttpsClient();
            } else {
                if (ConfigureEnvironment.isProxyEnabled) {
                    HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                    httpClient = builder.setProxy(proxy).build();
                } else {
                    httpClient = builder.build();
                }
            }

            logger.info("API Counter: {}", ++apiCounter);
            response = httpClient.execute(target, httpRequest);
            logger.debug("The Response is {} ", response.getEntity());

            logger.debug("API Status Code is : {}", response.getStatusLine().toString());
            apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BufferedInputStream bis = new BufferedInputStream(entity.getContent());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
                int inByte;
                while ((inByte = bis.read()) != -1)
                    bos.write(inByte);
                bis.close();
                bos.close();
            }
            logger.info("The Download is completed , returning the response : [ {} ]", response);

            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while Downloading File {}. {}", sOutputFile, e.getStackTrace());
        }
        return response;
    }

    public HttpResponse postRequestWithRequestBase(HttpHost target, HttpRequestBase httpRequest) throws Exception {
        HttpResponse response = null;
        try {
            CookieStore httpCookieStore;
            HttpClientBuilder builder;
            HttpClient httpClient;

            httpCookieStore = new BasicCookieStore();
            builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);

            // Read Proxy config from File and control HttpClient
            if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
                APIUtils oAPIUtils = new APIUtils();
                httpClient = oAPIUtils.getHttpsClient();
            } else {
                if (ConfigureEnvironment.isProxyEnabled) {
                    HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                    httpClient = builder.setProxy(proxy).build();
                } else {
                    httpClient = builder.build();
                }
            }

            logger.info("API Counter: {}", ++apiCounter);
            response = httpClient.execute(target, httpRequest);
            logger.debug("The Response is {} ", response.getEntity());

            logger.debug("API Status Code is : {}", response.getStatusLine().toString());
            apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception Occurred");
        }
        return response;
    }

    public void unzip(String zipFileDir, String zipFileName, String unzipDir) {
        String zipFilePath = zipFileDir + "/" + zipFileName;
        try {
            System.out.println("zipFilePath = " + zipFilePath);
            ZipFile zipFile = new ZipFile(zipFilePath);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    System.out.print("dir  : " + entry.getName());
                    String destPath = unzipDir + File.separator + entry.getName();
                    System.out.println(" => " + destPath);
                    File file = new File(destPath);
                    file.mkdirs();
                } else {
                    String destPath = unzipDir + File.separator + entry.getName();

                    try (InputStream inputStream = zipFile.getInputStream(entry);
                         FileOutputStream outputStream = new FileOutputStream(destPath);
                    ) {
                        int data = inputStream.read();
                        while (data != -1) {
                            outputStream.write(data);
                            data = inputStream.read();
                        }
                    }
                    System.out.println("file : " + entry.getName() + " => " + destPath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error unzipping file " + zipFilePath, e);
        }
    }

    public Boolean downloadAPIResponseFile(String sOutputFilePath, String sOutputFileName, HttpHost target, HttpRequestBase httpRequest) {
        logger.info("Downloading File from API Response for HttpHost : [ {} ] , HttpPost request : [ {} ] and download File Location : [ {} ]", target, httpRequest, sOutputFilePath + sOutputFileName);

        HttpResponse response = null;
        try {
            //Create File to be downloaded
            File outputFile = createDownloadFile(sOutputFilePath + "/" + sOutputFileName);

            CookieStore httpCookieStore;
            HttpClientBuilder builder;
            HttpClient httpClient;

            httpCookieStore = new BasicCookieStore();
            builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);

            // Read Proxy config from File and control HttpClient
            if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
                APIUtils oAPIUtils = new APIUtils();
                httpClient = oAPIUtils.getHttpsClient();
            } else {
                if (ConfigureEnvironment.isProxyEnabled) {
                    HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                    httpClient = builder.setProxy(proxy).build();
                } else {
                    httpClient = builder.build();
                }
            }

            logger.info("API Counter: {}", ++apiCounter);
            response = httpClient.execute(target, httpRequest);
            logger.debug("The Response is {} ", response.getEntity());

            logger.debug("API Status Code is : {}", response.getStatusLine().toString());
            apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BufferedInputStream bis = new BufferedInputStream(entity.getContent());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
                int inByte;
                while ((inByte = bis.read()) != -1)
                    bos.write(inByte);
                bos.flush();
                bis.close();
                bos.close();
            }
            logger.info("Download completed");
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
            return true;
        } catch (Exception e) {
            logger.error("Exception while Downloading File {}. {}", sOutputFilePath + "/" + sOutputFileName, e.getStackTrace());
            return false;
        }
    }

    /**
     * @param sOutputFile
     * @return File object
     * @throws Exception
     * @apiNote This method is to create the File which need to be downloaded , it delete older file if exists and then creates new
     */
    private File createDownloadFile(String sOutputFile) throws Exception {
        File outputFile = new File(sOutputFile);
        if (outputFile.exists()) {
            logger.debug("The file : [ {} ] already exist , so deleting and recreating the file.", sOutputFile);
            outputFile.delete();
            outputFile.createNewFile();
        } else {
            logger.debug("The file : [ {} ] does not exist , so creating new file.", sOutputFile);
            outputFile.createNewFile();
        }
        return outputFile;
    }

    /**
     * @param resourceUri
     * @param acceptsHeader
     * @return HttpGet object
     * @apiNote This method is for creating HTTP get request using the query String
     */
    public HttpGet generateHttpGetRequestWithQueryString(String resourceUri, String acceptsHeader) {
        logger.debug("Generating Http Get Request for queryString : [ {} ]", resourceUri);
        HttpGet httpGetRequest = new HttpGet(resourceUri);

        httpGetRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        if (!acceptsHeader.equalsIgnoreCase("")) {
            httpGetRequest.addHeader("Accept", acceptsHeader);
        }
        httpGetRequest.addHeader("Accept-Encoding", "gzip, deflate");

        if (ConfigureEnvironment.useCookies) {
            logger.debug("--------------------Cookies Flag is True------------------------------");
            httpGetRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
        } else {
            logger.debug("--------------------Cookies Flag is False------------------------------");
            logger.debug("Authorization is : {}", Check.getAuthorization());
            logger.debug("JSession Id is : {}", Check.getJSessionId());

            httpGetRequest.addHeader("Authorization", Check.getAuthorization());
        }
        if (ConfigureEnvironment.useCSRFToken) {
            logger.debug("--------------------useCSRFToken Flag is true------------------------------");
            logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            httpGetRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        }
        httpGetRequest.addHeader("X-Requested-With", "XMLHttpRequest");

        return httpGetRequest;
    }

    /**
     * @param target
     * @param requestBase
     * @return Response String
     * @throws UnsupportedEncodingException
     * @throws ClientProtocolException
     * @throws IllegalStateException
     * @throws IOException
     * @apiNote <b>This method is to upload a file to Server , it uses the Http Request which includes multi part entity </b>
     */
    public String uploadFileToServer(HttpHost target, HttpRequestBase requestBase) throws UnsupportedEncodingException, ClientProtocolException, IllegalStateException, IOException {
        String responseString = null;

        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"),
                        Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")));
                httpClient = HttpClients.custom().setProxy(proxy).build();
            }
        }

        try {

            logger.info("API Counter: {}", ++apiCounter);
            HttpResponse response = httpClient.execute(target, requestBase);
            if (response != null) {
                responseString = EntityUtils.toString(response.getEntity());
            }
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while Uploading File to Server. {}", e.getMessage());
        }
        return responseString;
    }



    /**
     * @param target
     * @param requestBase
     * @return Response String
     * @throws UnsupportedEncodingException
     * @throws ClientProtocolException
     * @throws IllegalStateException
     * @throws IOException
     * @apiNote <b>This method is to upload a file to Server , it uses the Http Request which includes multi part entity </b>
     */
    public String uploadFileToServer(String hostName, int port, HttpHost target, HttpRequestBase requestBase) throws UnsupportedEncodingException, ClientProtocolException, IllegalStateException, IOException {
        String responseString = null;

        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                HttpHost proxy = new HttpHost(hostName,port);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            }
        }

        try {

            logger.info("API Counter: {}", ++apiCounter);
            HttpResponse response = httpClient.execute(target, requestBase);
            if (response != null) {
                responseString = EntityUtils.toString(response.getEntity());
            }
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while Uploading File to Server. {}", e.getMessage());
        }
        return responseString;
    }

    public String uploadFileToHttpsServer(HttpHost target, HttpRequestBase requestBase) throws UnsupportedEncodingException, ClientProtocolException, IllegalStateException, IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String responseString = null;

        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHTTPSClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"),
                        Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")));
                httpClient = HttpClients.custom().setProxy(proxy).build();
            }
        }

        try {
            logger.info("API Counter: {}", ++apiCounter);
            HttpResponse response = httpClient.execute(target, requestBase);
            if (response != null) {
                responseString = EntityUtils.toString(response.getEntity());
            }
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while Uploading File to Server. {}", e.getMessage());
        }
        return responseString;
    }

    public HttpClient getHttpsClient() {
        HttpClient httpClient = null;

        try {
            SSLContext sslcontext = SSLContexts.custom().useSSL().build();
            sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            httpClient = HttpClients.custom().setSSLSocketFactory(factory).build();

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Got exception while building https http Client : cause : [ {} ] , stackTrace : [ {} ]", ex.getMessage(), ex.getStackTrace());
        }
        return httpClient;
    }

    public HttpClient getHTTPSClient() {
        HttpClient httpClient = null;

        try {
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            httpClient = HttpClients.custom().setSSLSocketFactory(factory).build();

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Got exception while building https http Client : cause : [ {} ] , stackTrace : [ {} ]", ex.getMessage(), ex.getStackTrace());
        }
        return httpClient;
    }

    /**
     * this function is for verifying when any given uri will give 200 status code with json response (the request is always going to be of get Type)
     *
     * @param uri
     * @return boolean value
     */
    public Boolean isLinkValid(String uri) throws IOException {

        HttpResponse response;
        String queryString = uri;

        logger.info("Query string url formed is {}", queryString);

        HttpGet getRequest = new HttpGet(queryString);
        getRequest.addHeader("Accept", "*/*");

        response = getRequest(getRequest);
        logger.debug("Response is : {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Create Report form API: response header {}", headers[i].toString());
        }

        logger.debug("API Status Code is : {}", response.getStatusLine().toString());
        String apiStatusCode = response.getStatusLine().toString();
        String responseOfAPI = EntityUtils.toString(response.getEntity());


        if (apiStatusCode.contains("200") && validJsonResponse(responseOfAPI)) {
            return true;

        } else {
            return false;
        }
    }

    public HttpResponse getRequestSisense(HttpGet httpGetRequest, boolean useSessionId) {

        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("SisenseScheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("SisenseHost");
            Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("SisensePort"));
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("SisenseScheme");
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpGetRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();
            }
            apiResponseTime = changeTimeIntoSec(watch.toString());
            logger.debug("API Response Time is : [{}]", apiResponseTime);
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpGetRequest.getURI());
        }
        return response;
    }

    //Added by gaurav bhadani on 31 Dec 2018 as port number is dynamically reuired for some requests
    public HttpResponse getRequest(HttpGet httpGetRequest, boolean useSessionId, Integer portNumber) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            APIUtils oAPIUtils = new APIUtils();
            httpClient = oAPIUtils.getHttpsClient();
        } else {
            if (ConfigureEnvironment.isProxyEnabled) {
                HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                httpClient = HttpClients.custom().setProxy(proxy).build();
            } else {
                httpClient = HttpClientBuilder.create().build();
            }
        }

        HttpResponse response = null;

        try {
            String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
            String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");
            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            if (ConfigureEnvironment.useCookies) {
                httpGetRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
            } else {
                logger.debug("Authorization is : {}", Check.getAuthorization());
                logger.debug("JSession Id is : {}", Check.getJSessionId());
//				if (useSessionId) {
//					String cookie = "Authorization=" + Check.getAuthorization() + ";JSESSIONID=" + Check.getJsessionId();
//					httpGetRequest.addHeader("Cookie", cookie);
//				} else
                //httpGetRequest.addHeader("Authorization", Check.getAuthorization());
//					String abc = "Authorization=2406:a6e4496b8981e02bed6b8ea0b4d8ec54266766448b7b8ed2cf726729c63828a2";
                httpGetRequest.addHeader("Cookie", "Authorization=" + Check.getAuthorization());
            }

//			if (ConfigureEnvironment.useCSRFToken) {
//				logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
//				httpGetRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
//			}
//			httpGetRequest.addHeader("X-Requested-With", "XMLHttpRequest");

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                logger.info("API Counter: {}", ++apiCounter);
                response = httpClient.execute(target, httpGetRequest);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                watch.stop();

            }
            apiResponseTime = changeTimeIntoSec(watch.toString());
            logger.debug("API Response Time is : [{}]", apiResponseTime);
            // if we need to put some time gap between two API Call
            if (ConfigureEnvironment.addLatency) {
                logger.debug("Putting some gap between two API Call");
                addLatency();
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Url {}", httpGetRequest.getURI());
        }
        return response;
    }

    public Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String outputFileName) {

        Boolean status;

        FileUtils fileUtil = new FileUtils();

        String  outputFile = outputFilePath + "/" + outputFileName;
        status = fileUtil.writeResponseIntoFile(response, outputFile);
        if (status)
            logger.info("DownloadListWithData file generated at {}", outputFile);

        return status;
    }

}