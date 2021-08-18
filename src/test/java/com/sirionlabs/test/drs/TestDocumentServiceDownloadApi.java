package com.sirionlabs.test.drs;

import com.sirionlabs.api.drs.DocumentServiceDownloadAPi;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class TestDocumentServiceDownloadApi {


    private final static Logger logger = LoggerFactory.getLogger(TestDocumentServiceUploadApi.class);
    String hostName;
    Integer port;
    String scheme;
    HttpHost hostUrl;
    private String downloadPath = "src\\test\\resources\\TestData\\DRS\\Download";
    private static String configFilePath;
    private static String configFileName;
    private static String fileSystem_clientId ;
    private static String aws_clientId ;
    private static String kmsNotConfigured_clientId ;
    private static String validFileName ;
    private static String envDetail ;


    DocumentServiceDownloadAPi drsDownload = new DocumentServiceDownloadAPi();
    DRSUtils drsUtil = new DRSUtils();


    @BeforeClass
    public void before(){
        hostName = ConfigureEnvironment.getEnvironmentProperty("document_service_host");
        port = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("document_service_port"));
        scheme = ConfigureEnvironment.getEnvironmentProperty("document_service_scheme");
        hostUrl = new HttpHost(hostName,port,scheme);
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DRSConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("DRSConfigFileName");
        fileSystem_clientId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "filesystemclientid");
        aws_clientId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "awsclientid");
        envDetail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "envDetail");
        validFileName =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "validfilename");
    }


     @Test(description = "C152291")
    public void TestDRSDownloadInvalidPath() throws InterruptedException, IOException {
        CustomAssert csAssert = new CustomAssert();
        String uploadResponse = drsUtil.uploadFile(null,null, aws_clientId,envDetail ,validFileName);
        if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
            Thread.sleep(2000);
            String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
            HttpResponse response = drsDownload.hitDownloadDRSWithInvalidPath(hostUrl, downloadPath, "sarthak.pdf",
                    aws_clientId, documentId);
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

            csAssert.assertEquals(response.getStatusLine().getStatusCode(),404,
                    "expected status is 404 but found "+response.getStatusLine().getStatusCode() );
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),404,
                    "staus is not correct in case of InvalidApiPath"+ response);
            csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.error"),"Not Found",
                    "resonse error is not correct in case of InvalidApiPath "+ response );
            csAssert.assertAll();
        }

    }


     @Test(description = "C152293")
    public void TestDRSDownloadDifferentClientId() throws InterruptedException, IOException {
         CustomAssert csAssert = new CustomAssert();
         String uploadResponse = drsUtil.uploadFile(null,null, aws_clientId,envDetail ,validFileName);
         if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
             Thread.sleep(15000);
             String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
             HttpResponse response = drsDownload.hitDownloadDRS(hostUrl, downloadPath, "sarthak.pdf",
                     fileSystem_clientId, documentId);
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

            csAssert.assertEquals(response.getStatusLine().getStatusCode(),400,
                    "expected status is 400 but found "+response.getStatusLine().getStatusCode() );
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),"BAD_REQUEST",
                    "staus is not correct in case of Different client Id"+ responseBody);
            csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.message"),
                    "Provided client id is not matching with the stored document's client id",
                    "resonse message is not correct in case of Different client Id "+ responseBody );

        }else{
             csAssert.assertFalse(true,"document is not uploaded");
         }

        csAssert.assertAll();
    }

     @Test(description = "C152295")
    public void TestDRSDownloadInvalidDocumentId() throws InterruptedException, IOException {
        CustomAssert csAssert = new CustomAssert();
            HttpResponse response = drsDownload.hitDownloadDRS(hostUrl, downloadPath, "sarthak.pdf",
                    aws_clientId, "test123");
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

            csAssert.assertEquals(response.getStatusLine().getStatusCode(),404,
                    "expected status is 404 but found "+response.getStatusLine().getStatusCode() );
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.status"),"NOT_FOUND",
                    "staus is not correct in case of Invalid DocumentId"+ responseBody);
            csAssert.assertEquals(  JSONUtility.parseJson(responseBody,"$.message"),
                    "Document id - test123 not exists",
                    "resonse message is not correct in case of Invalid DocumentId "+ responseBody );
            csAssert.assertAll();
    }


    @Test(description = "C152294")
    public void TestDRSDownloadClientIdnotPassed() throws InterruptedException, IOException {
         CustomAssert csAssert = new CustomAssert();
            HttpResponse response = drsDownload.hitDownloadDRS(hostUrl, downloadPath, "sarthak.pdf",
                    null, "test");
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

            csAssert.assertEquals(response.getStatusLine().getStatusCode(),400,
                    "expected status is 400 but found "+response.getStatusLine().getStatusCode() );
            csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.error"),"Bad Request",
                    "staus is not correct in case of client Id not passed"+ responseBody);


        csAssert.assertAll();
    }

    @Test(description = "C152294")
    public void TestDRSDownloadDocumentnotPassed() throws InterruptedException, IOException {
        CustomAssert csAssert = new CustomAssert();
        HttpResponse response = drsDownload.hitDownloadDRS(hostUrl, downloadPath, "sarthak.pdf",
                aws_clientId, null);
        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

        csAssert.assertEquals(response.getStatusLine().getStatusCode(),400,
                "expected status is 400 but found "+response.getStatusLine().getStatusCode() );
        csAssert.assertEquals( JSONUtility.parseJson(responseBody,"$.error"),"Bad Request",
                "staus is not correct in case of client Id not passed"+ responseBody);


        csAssert.assertAll();
    }

     @Test(description = "C152273")
    public void TestDocumentServiceDownloadApi() throws InterruptedException, IOException {
        CustomAssert csAssert = new CustomAssert();
        String uploadResponse = drsUtil.uploadFile(null,null, aws_clientId,envDetail ,validFileName);
        if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
            Thread.sleep(15000);
            String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
            HttpResponse response =  drsDownload.hitDownloadDRS(hostUrl,downloadPath,"TestDRS.pdf",
              aws_clientId,documentId);
            if(response.getStatusLine().getStatusCode()==200){
               boolean filePresent =  FileUtils.fileExists(downloadPath+ "/" + documentId,"TestDRS.pdf");
               if(!filePresent){
                   csAssert.assertFalse(true,"downloaded file not present at the downloaded path "+downloadPath+ "/" + documentId);
               }

            }else{
                csAssert.assertFalse(true,"document is not downloaded successfully");
            }


    }else{
            csAssert.assertFalse(true,"document is not uploaded");
        }

        csAssert.assertAll();
     }


    @Test(description = "C152273")
    public void TestDRSDownloadApiwithFS() throws InterruptedException, IOException {
        CustomAssert csAssert = new CustomAssert();
        String uploadResponse = drsUtil.uploadFile(null,null, fileSystem_clientId,envDetail ,validFileName);
        if(JSONUtility.parseJson(uploadResponse, "$.status").equals("CREATED")){
            Thread.sleep(15000);
            String documentId = (String) JSONUtility.parseJson(uploadResponse,"$.documentDetails.documentId");
            HttpResponse response =  drsDownload.hitDownloadDRS(hostUrl,downloadPath,"TestDRS.pdf",
                    fileSystem_clientId,documentId);
            if(response.getStatusLine().getStatusCode()==200){
                boolean filePresent =  FileUtils.fileExists(downloadPath+ "/" + documentId,"TestDRS.pdf");
                if(!filePresent){
                    csAssert.assertFalse(true,"downloaded file not present at the downloaded path "+downloadPath+ "/" + documentId);
                }

            }else{
                csAssert.assertFalse(true,"document is not downloaded successfully");
            }


        }else{
            csAssert.assertFalse(true,"document is not uploaded");
        }

        csAssert.assertAll();
    }


}
