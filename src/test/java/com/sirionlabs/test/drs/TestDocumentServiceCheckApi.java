package com.sirionlabs.test.drs;

import com.sirionlabs.api.drs.DocumentServiceCheckApi;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestDocumentServiceCheckApi {
    private final static Logger logger = LoggerFactory.getLogger(TestDocumentServiceCheckApi.class);

    DocumentServiceCheckApi drsCheck = new DocumentServiceCheckApi();
    String hostName;
    Integer port;
    String scheme;
    String hostUrl;

    @BeforeClass
    public void before(){
        hostName = ConfigureEnvironment.getEnvironmentProperty("document_service_host");
        port = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("document_service_port"));
        scheme = ConfigureEnvironment.getEnvironmentProperty("document_service_scheme");
        hostUrl = scheme+"://"+hostName+":"+port;

    }

    @Test(description = "C152249")
    public void TestDRSCheckApiWithInvalidPath(){
        CustomAssert csAssert = new CustomAssert();
        APIResponse response = drsCheck.getDRSCheckApiWithInvalidPath(hostUrl);
        int statusCode = response.getResponseCode();
        String responseBody = response.getResponseBody();

        csAssert.assertEquals(statusCode,404,"expected status code -> 404 but actual ->"+ statusCode);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.error"),"Not Found",
                "Response body is "+ responseBody);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.path"),"/drs/healthtest",
                "Response body is "+ responseBody);

        csAssert.assertAll();
    }

    @Test(description = "C152248")
    public void TestDocumentServiceCheckApi(){
        CustomAssert csAssert = new CustomAssert();
        APIResponse response = drsCheck.getDocumentServiceCheckApi(hostUrl);
        int statusCode = response.getResponseCode();
        String responseBody = response.getResponseBody();

        csAssert.assertEquals(statusCode,200,"expected status code -> 200 but actual ->"+ statusCode);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.message"),"Service is up",
                "Response body is "+ responseBody);
        csAssert.assertAll();
    }



    @Test(description = "C152250")
    public void TestDRSCheckApiWithInvalidMethod(){
        CustomAssert csAssert = new CustomAssert();
        APIResponse response = drsCheck.getDRSCheckApiWithInvalidMethod(hostUrl);
        int statusCode = response.getResponseCode();
        String responseBody = response.getResponseBody();

        csAssert.assertEquals(statusCode,405,"expected status code -> 405 but actual ->"+ statusCode);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.error"),"Method Not Allowed",
                "Response body is "+ responseBody);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.path"),"/drs/health",
                "Response body is "+ responseBody);
        csAssert.assertAll();


    }







}
