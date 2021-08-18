package com.sirionlabs.test.drs;


import com.sirionlabs.api.drs.DocumentServiceInfoApi;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestDocumentServiceInfoApi {

    DocumentServiceInfoApi drsInfo = new DocumentServiceInfoApi();
    DRSUtils drsUtil = new DRSUtils();

    String hostName;
    Integer port;
    String scheme;
    String hostUrl;

    private static String configFilePath;
    private static String configFileName;
    private static String fileSystem_clientId ;
    private static String aws_clientId ;
    private static String validFileName ;
    private static String envDetail ;

    @BeforeClass
    public void before(){
        hostName = ConfigureEnvironment.getEnvironmentProperty("document_service_host");
        port = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("document_service_port"));
        scheme = ConfigureEnvironment.getEnvironmentProperty("document_service_scheme");
        hostUrl = scheme+"://"+hostName+":"+port;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DRSConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("DRSConfigFileName");
        fileSystem_clientId =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "filesystemclientid");
        aws_clientId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "awsclientid");
        envDetail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "envDetail");
        validFileName =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "validfilename");

    }


    @Test(description = "C153523")
    public void TestDRSInfoApiWithoutAuth(){
        String documentId = "test";
        CustomAssert csAssert = new CustomAssert();
        APIResponse response = drsInfo.getDocumentServiceInfoApiWithoutAuth(hostUrl,aws_clientId,documentId);
        int statusCode = response.getResponseCode();
        String responseBody = response.getResponseBody();

        csAssert.assertEquals( statusCode,401,
                "status code  is not correct in case of 401 UNAUTHORIZED "+ statusCode);
        csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),"UNAUTHORIZED",
                "status is not correct in case of 401 UNAUTHORIZED "+ response);
        csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.message"),"Not an authorize call",
                "resonse message is not correct in case of 401 UNAUTHORIZED "+ response );

        csAssert.assertAll();
    }



    @Test(description = "C153524")
    public void TestDRSInfoApiInvalidPath(){
        String documentId = "test";
        CustomAssert csAssert = new CustomAssert();
        APIResponse response = drsInfo.getDRSInfoApiWithInvalidPath(hostUrl,aws_clientId,documentId);
        int statusCode = response.getResponseCode();
        String responseBody = response.getResponseBody();

        csAssert.assertEquals(statusCode,404,"expected status code -> 404 but actual ->"+ statusCode);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.error"),"Not Found",
                "Response body is "+ responseBody);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.path"),"/drs/document/v1/clients/"+aws_clientId+"/documents/"+documentId+"/infotest",
                "Response body is "+ responseBody);

        csAssert.assertAll();
    }


    @Test(description = "C153525")
    public void TestDRSInfoApiWithAWS() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String uploadResponse = drsUtil.uploadFile(null,null,aws_clientId,envDetail,validFileName);
        if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
            Thread.sleep(2000);
           String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
            APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,aws_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,200,"expected status code -> 200 but actual ->"+ statusCode);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileName"),validFileName.split("\\.")[0],
                    "fileName is not correct "+ responseBody);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileExtension"),validFileName.split("\\.")[1],
                    "File Extension is not correct "+ responseBody);
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }

   @Test(description = "C153526")
    public void TestDRSInfoApiWithAWSNameExtension() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String fileName = "updatedName";
        String extension = "docx";
        String uploadResponse = drsUtil.uploadFile(fileName,extension,aws_clientId,envDetail,validFileName);
       if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
           Thread.sleep(2000);
           String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
           APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,aws_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,200,"expected status code -> 200 but actual ->"+ statusCode);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileName"),fileName,
                    "fileName is not correct "+ responseBody);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileExtension"),extension,
                    "File Extension is not correct "+ responseBody);
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }


   @Test(description = "C153527")
    public void TestDRSInfoApiWithAWSDifferentClientId() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String fileName = "updatedName";
        String extension = "docx";
        String uploadResponse = drsUtil.uploadFile(fileName,extension,aws_clientId,envDetail,validFileName);
       if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
           Thread.sleep(2000);
           String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
           APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,fileSystem_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,400,"expected status code -> 400 but actual ->"+ statusCode);
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),"BAD_REQUEST",
                    "status is not correct in case of 400 BAD_REQUEST "+ response);
            csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.message"),"Provided client id is not matching with the stored document's client id",
                    "resonse message is not correct in case of 400 BAD_REQUEST "+ response );
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }




    @Test(description = "C153528")
    public void TestDRSInfoApiWithAWSInvalidDocumentId() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String fileName = "updatedName";
        String extension = "docx";
        String uploadResponse = drsUtil.uploadFile(fileName,extension,aws_clientId,envDetail,validFileName);
        if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
            Thread.sleep(2000);
            String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
            documentId = documentId.substring(0,4);
            APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,aws_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,404,"expected status code -> 404 but actual ->"+ statusCode);
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),"NOT_FOUND",
                    "status is not correct in case of 404 NOT_FOUND "+ response);
            csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.message"),"Document id - "+documentId+" not exists",
                    "resonse message is not correct in case of 404 NOT_FOUND "+ response );
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }


     @Test(description = "C153529")
    public void TestDRSInfoApiWithFS() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String uploadResponse = drsUtil.uploadFile(null,null,fileSystem_clientId,envDetail,validFileName);
         if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
             Thread.sleep(2000);
             String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
             APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,fileSystem_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,200,"expected status code -> 200 but actual ->"+ statusCode);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileName"),validFileName.split("\\.")[0],
                    "fileName is not correct "+ responseBody);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileExtension"),validFileName.split("\\.")[1],
                    "File Extension is not correct "+ responseBody);
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }

      @Test(description = "C153530")
    public void TestDRSInfoApiWithFSNameExtension() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String fileName = "updatedName";
        String extension = "docx";
        String uploadResponse = drsUtil.uploadFile(fileName,extension,fileSystem_clientId,envDetail,validFileName);
          if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
              Thread.sleep(2000);
              String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
              APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,fileSystem_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,200,"expected status code -> 200 but actual ->"+ statusCode);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileName"),fileName,
                    "fileName is not correct "+ responseBody);
            csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.fileExtension"),extension,
                    "File Extension is not correct "+ responseBody);
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }


     @Test(description = "C153531")
    public void TestDRSInfoApiWithFSDifferentClientId() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String fileName = "updatedName";
        String extension = "docx";
        String uploadResponse = drsUtil.uploadFile(fileName,extension,fileSystem_clientId,envDetail,validFileName);
         if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
             Thread.sleep(2000);
             String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
             APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,aws_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,400,"expected status code -> 400 but actual ->"+ statusCode);
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),"BAD_REQUEST",
                    "status is not correct in case of 400 BAD_REQUEST "+ response);
            csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.message"),"Provided client id is not matching with the stored document's client id",
                    "resonse message is not correct in case of 400 BAD_REQUEST "+ response );
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }




    @Test(description = "C153532")
    public void TestDRSInfoApiWithFSInvalidDocumentId() throws InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String fileName = "updatedName";
        String extension = "docx";
        String uploadResponse = drsUtil.uploadFile(fileName,extension,fileSystem_clientId,envDetail,validFileName);
        if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
            Thread.sleep(2000);
            String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
            documentId = documentId.substring(0,4);
            APIResponse response = drsInfo.getDocumentServiceInfoApi(hostUrl,fileSystem_clientId,documentId);
            int statusCode = response.getResponseCode();
            String responseBody = response.getResponseBody();

            csAssert.assertEquals(statusCode,404,"expected status code -> 404 but actual ->"+ statusCode);
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),"NOT_FOUND",
                    "status is not correct in case of 404 NOT_FOUND "+ response);
            csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.message"),"Document id - "+documentId+" not exists",
                    "resonse message is not correct in case of 404 NOT_FOUND "+ response );
        }
        else{
            csAssert.assertFalse(true,"document is not uploaded. Response:-  "+uploadResponse);
        }


        csAssert.assertAll();
    }


    @Test(description = "C153533")
    public void TestDRSInfoApiInvalidMethod(){
        String documentId = "test";
        CustomAssert csAssert = new CustomAssert();
        APIResponse response = drsInfo.getDRSInfoApiWithInvalidMethod(hostUrl,aws_clientId,documentId);
        int statusCode = response.getResponseCode();
        String responseBody = response.getResponseBody();

        csAssert.assertEquals(statusCode,405,"expected status code -> 405 but actual ->"+ statusCode);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.error"),"Method Not Allowed",
                "Response body is "+ responseBody);
        csAssert.assertEquals(JSONUtility.parseJson(responseBody,"$.path"),"/drs/document/v1/clients/"+aws_clientId+"/documents/"+documentId+"/info",
                "Response body is "+ responseBody);

        csAssert.assertAll();
    }






}
