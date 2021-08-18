package com.sirionlabs.test.drs;


import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.drs.DocumentServiceUploadApi;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class TestDocumentServiceUploadApi {
    private final static Logger logger = LoggerFactory.getLogger(TestDocumentServiceUploadApi.class);

    private static String configFilePath;
    private static String configFileName;
    private static String hostName;
    private static Integer port;
    private static String scheme;
    private static String uploadFilePath = "src\\test\\resources\\TestData\\DRS\\Upload";
    private static String fileSystem_clientId ;
    private static String validateInDB ;
    private static String aws_clientId ;
    private static String kmsNotConfigured_clientId ;
    private static String validFileName ;
    private static String maxSizeFileName ;
    private static String invalidExtensionFileName ;
    private static String dbIp ;
    private static String dbPort ;
    private static String dbName ;
    private static String dbUsername ;
    private static String dbPassword ;
    private static PostgreSQLJDBC db;
    private static String envDetail;


    DocumentServiceUploadApi drsUpload = new DocumentServiceUploadApi();
    Check check = new Check();
    AdminHelper admin = new AdminHelper();
    DRSUtils drsUtil = new DRSUtils();



    @BeforeClass
    public void before() {
        hostName = ConfigureEnvironment.getEnvironmentProperty("document_service_host");
        port = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("document_service_port"));
        scheme = ConfigureEnvironment.getEnvironmentProperty("document_service_scheme");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DRSConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("DRSConfigFileName");
        fileSystem_clientId =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "filesystemclientid");
        validateInDB =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "validateindb");
        aws_clientId =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "awsclientid");
        validFileName =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "validfilename");
        maxSizeFileName =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "sizemaxfilename");
       invalidExtensionFileName =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "invalidextensionfilename");
        envDetail =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "envdetail");
       dbIp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dbhostaddress");
       dbPort =ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dbportname");
       dbName= ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dbname");
       dbUsername = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dbusername");
       dbPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dbpassword");

       if(validateInDB.equalsIgnoreCase("true"))
        db = new PostgreSQLJDBC(dbIp,dbPort,dbName,dbUsername,dbPassword);

    }

    @AfterClass
    public void afterClass() throws SQLException {
        if(validateInDB.equalsIgnoreCase("true"))
            db.closeConnection();
    }



   @Test(description = "C152264")
    public void TestInvalidApiPath() throws UnsupportedEncodingException {
        CustomAssert csAssert = new CustomAssert();
        String invalidqueryPath="/drs/document/v1/uploadtest";
        HashMap<String, String> params = drsUpload.getParamas(null, null, fileSystem_clientId,envDetail);
        String response = DocumentHelper.uploadDRSFile(hostName, port, scheme, invalidqueryPath,
                uploadFilePath,validFileName, params);
        csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),404,
                "staus is not correct in case of InvalidApiPath"+ response);
        csAssert.assertEquals(  JSONUtility.parseJson(response,"$.error"),"Not Found",
                "resonse error is not correct in case of InvalidApiPath "+ response );
        csAssert.assertAll();


    }

    @Test(description = "C152267")
    public void TestclientIdnotPassed() {
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(null,null,null,envDetail,validFileName);
        csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"BAD_REQUEST",
                "status is not correct in case of clientId is not Passed"+ response);
        csAssert.assertEquals(  JSONUtility.parseJson(response,"$.message"),"Client id is missing",
                "resonse error is not correct in case of clientId is not Passed "+ response );
        csAssert.assertAll();


    }


   @Test(description = "C152269")
    public void TestInvalidClientId() throws UnsupportedEncodingException {
        CustomAssert csAssert = new CustomAssert();
       String response = drsUtil.uploadFile(null,null,"xyz",envDetail,validFileName);
       csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),400,
                "status code is not correct in case of  invalid clientId is  Passed"+ response);
        csAssert.assertEquals(  JSONUtility.parseJson(response,"$.error"),"Bad Request",
                "resonse error is not correct in case of invalid clientId is Passed "+ response );
        csAssert.assertAll();

    }

   @Test(description = "C152270")
    public void TestMaxFileSize()  {
        CustomAssert csAssert = new CustomAssert();
       String response = drsUtil.uploadFile(null,null,fileSystem_clientId,envDetail,maxSizeFileName);
       csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"BAD_REQUEST",
                "status is not correct in case max size file is uploaded"+ response);
        csAssert.assertEquals( JSONUtility.parseJson(response,"$.errorList[0].message"),"File cannot be greater than max size",
                "resonse error is not correct in case max size file is uploaded "+ response );
        csAssert.assertAll();

    }

    @Test(description = "C152271")
    public void TestInvalidExtension()  {
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(null,null,fileSystem_clientId,envDetail,invalidExtensionFileName);
        csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"BAD_REQUEST",
                "status is not correct in case file uploaded having invalid extension"+ response);
        csAssert.assertEquals( JSONUtility.parseJson(response,"$.errorList[0].message"),"Extension is not valid",
                "resonse error is not correct in case file uploaded having invalid extension "+ response );
        csAssert.assertAll();

    }

    @Test(description = "C152272,C152266,C153446")
    public void TestNameandExtensionNotProvided(){
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(null,null,aws_clientId,envDetail,validFileName);
        if(JSONUtility.parseJson(response,"$.status").equals("CREATED")){

            if(validateInDB.equalsIgnoreCase("true")){
            String query = "select * from document_metadata order by id desc limit 1;";
            List<String> dbArray = getValuefromDB(query);
            if(JSONUtility.parseJson(response,"$.documentDetails.documentId")
                    .equals(dbArray.get(1)) && dbArray.get(16).equalsIgnoreCase("document successfully loaded")){

                String expectedFileName = validFileName.split("\\.")[0];
                String actualFileName =  dbArray.get(17);
                String expectedExtension = validFileName.split("\\.")[1];
                String actualExtension = dbArray.get(18);
                Integer clientId = Integer.valueOf(dbArray.get(3));
                String alias =  dbArray.get(4);
                Integer vendorId = Integer.valueOf(dbArray.get(10));
                String region = dbArray.get(11);
                String bucketName = dbArray.get(12);
                String fs_documentPath = dbArray.get(13);
                String antivirusEnabled =dbArray.get(5);
                String encriptionEnabled = dbArray.get(6);
                String key_id =  dbArray.get(7);
                String kms_client_id =  dbArray.get(8);
                String kms_required =  dbArray.get(9);

                String expected_Alias =  admin.getClientAliasFromDB(Integer.valueOf(aws_clientId));

                csAssert.assertEquals( "t",antivirusEnabled,"antivirus  is not enabled" +
                        "expected "+ false +" and actual "+ antivirusEnabled);
                csAssert.assertEquals( "t",encriptionEnabled,"encription is not enabled" +
                        "expected "+ false +" and actual "+ encriptionEnabled);
                csAssert.assertEquals(clientId, Integer.valueOf(aws_clientId), "clientId is not correct " +
                        "expected " + aws_clientId + " and actual " + clientId);
                csAssert.assertEquals(alias, expected_Alias, "alias is not correct " +
                        "expected " + aws_clientId + " and actual " + alias);
                csAssert.assertEquals(vendorId, Integer.valueOf(1), "vendorId is not correct " +
                        "expected " + vendorId + " and actual " + 1);
                csAssert.assertTrue(region != null, "region  is  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(bucketName != null, "bucketName  is  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(fs_documentPath == null, "file_system_document is  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(key_id != null, "key_id  is  not  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(kms_client_id.equals(aws_clientId), "kms_client_id  is not  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(kms_required.equals("t"), "kms_required  is not null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertEquals( expectedFileName,actualFileName,"filename is not correct" +
                        "expected "+ expectedFileName +" and actual "+ actualFileName);
                csAssert.assertEquals( expectedExtension,actualExtension,"extension is not correct" +
                        "expected "+ expectedExtension +" and actual "+ actualExtension);
            }

            else {
                logger.error("row is not inserted in DB corresponding to documentId "+
                        JSONUtility.parseJson(response,"$.documentDetails.documentId") );
            }
            }
        }else{
            csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"CREATED",
                    "status is not correct in case valid file uploaded "+ response);
        }
        csAssert.assertAll();

    }

    @Test(description = "C152274")
    public void TestNameKeyProvided() {
        String nameKey = "sarthak";
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(nameKey,null,aws_clientId,envDetail,validFileName);

        if(JSONUtility.parseJson(response,"$.status").equals("CREATED")){

            if(validateInDB.equalsIgnoreCase("true")){
            String query = "select * from document_metadata order by id desc limit 1;";
            List<String> dbArray = getValuefromDB(query);

            if(JSONUtility.parseJson(response,"$.documentDetails.documentId")
                    .equals(dbArray.get(1)) && dbArray.get(16).equalsIgnoreCase("document successfully loaded")){

                String expectedFileName = nameKey;
                String actualFileName =  dbArray.get(17);
                String expectedExtension = validFileName.split("\\.")[1];
                String actualExtension =  dbArray.get(18);
                csAssert.assertEquals( expectedFileName,actualFileName,"filename is not correct" +
                        "expected "+ expectedFileName +" and actual "+ actualFileName);
                csAssert.assertEquals( expectedExtension,actualExtension,"extension is not correct" +
                        "expected "+ expectedExtension +" and actual "+ actualExtension);}

            else {
                logger.error("row is not inserted in DB corresponding to documentId "+
                        JSONUtility.parseJson(response,"$.documentDetails.documentId") );
            }
            }
        }else{
            csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"CREATED",
                    "status is not correct in case valid file uploaded "+ response);
        }
        csAssert.assertAll();

    }

    @Test(description = "C152276")
    public void TestNameandextensionKeyProvided() {
        String nameKey = "sarthak";
        String extensionKey = "txt";
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(nameKey,extensionKey,aws_clientId,envDetail,validFileName);

        if(JSONUtility.parseJson(response,"$.status").equals("CREATED")){

            if(validateInDB.equalsIgnoreCase("true")){
            String query = "select * from document_metadata order by id desc limit 1;";
            List<String> dbArray = getValuefromDB(query);

            if(JSONUtility.parseJson(response,"$.documentDetails.documentId")
                    .equals(dbArray.get(1))&& dbArray.get(16).equalsIgnoreCase("document successfully loaded")){

                String expectedFileName = nameKey;
                String actualFileName =  dbArray.get(17);
                String expectedExtension = extensionKey;
                String actualExtension = dbArray.get(18);
                csAssert.assertEquals( expectedFileName,actualFileName,"filename is not correct" +
                        "expected "+ expectedFileName +" and actual "+ actualFileName);
                csAssert.assertEquals( expectedExtension,actualExtension,"extension is not correct" +
                        "expected "+ expectedExtension +" and actual "+ actualExtension);}

            else {
                logger.error("row is not inserted in DB corresponding to documentId "+
                        JSONUtility.parseJson(response,"$.documentDetails.documentId") );
            }
            }
        }else{
            csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"CREATED",
                    "status is not correct in case valid file uploaded "+ response);
        }
        csAssert.assertAll();

    }

    @Test(description = "C152288,C152289")
    public void TestAntivirusandEncriptionEnabled()  {
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(null,null,aws_clientId,envDetail,validFileName);


        if(JSONUtility.parseJson(response,"$.status").equals("CREATED")){

            if(validateInDB.equalsIgnoreCase("true")){
            String query = "select * from document_metadata order by id desc limit 1;";
            List<String> dbArray = getValuefromDB(query);

            if(JSONUtility.parseJson(response,"$.documentDetails.documentId")
                    .equals(dbArray.get(1)) && dbArray.get(16).equalsIgnoreCase("document successfully loaded")){

                String antivirusEnabled =dbArray.get(5);
                String encriptionEnabled = dbArray.get(6);
                String keyId = dbArray.get(7);
                csAssert.assertEquals( "t",antivirusEnabled,"antivirus  is not enabled" +
                        "expected "+ true +" and actual "+ antivirusEnabled);
                csAssert.assertEquals( "t",encriptionEnabled,"encription is not enabled" +
                        "expected "+ true +" and actual "+ encriptionEnabled);
                csAssert.assertTrue( keyId != null,"keyId is null if encription is enabled");

            }

            else {
                logger.error("row is not inserted in DB corresponding to documentId "+
                        JSONUtility.parseJson(response,"$.documentDetails.documentId") );
            }
            }
        }else{
            csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"CREATED",
                    "status is not correct in case valid file uploaded "+ response);
        }
        csAssert.assertAll();

    }

     @Test(description = "C152261,C153445")
    public void TestfileuploadinFS(){
        CustomAssert csAssert = new CustomAssert();
         String response = drsUtil.uploadFile(null,null,fileSystem_clientId,envDetail,validFileName);


         if(JSONUtility.parseJson(response,"$.status").equals("CREATED")){
             if(validateInDB.equalsIgnoreCase("true")){
            String query = "select * from document_metadata order by id desc limit 1;";
            List<String> dbArray = getValuefromDB(query);

            if(JSONUtility.parseJson(response,"$.documentDetails.documentId")
                    .equals(dbArray.get(1)) && dbArray.get(16).equalsIgnoreCase("document successfully loaded")) {

                Integer clientId = Integer.valueOf(dbArray.get(3));
                String alias =  dbArray.get(4);
                Integer vendorId = Integer.valueOf(dbArray.get(10));
                String region = dbArray.get(11);
                String bucketName = dbArray.get(12);
                String fs_documentPath = dbArray.get(13);
                String antivirusEnabled =dbArray.get(5);
                String encriptionEnabled = dbArray.get(6);
                String key_id =  dbArray.get(7);
                String kms_client_id =  dbArray.get(8);
                String kms_required =  dbArray.get(9);

                String expected_Alias =  admin.getClientAliasFromDB(Integer.valueOf(fileSystem_clientId));

                csAssert.assertEquals( "f",antivirusEnabled,"antivirus  is not enabled" +
                        "expected "+ false +" and actual "+ antivirusEnabled);
                csAssert.assertEquals( "f",encriptionEnabled,"encription is not enabled" +
                        "expected "+ false +" and actual "+ encriptionEnabled);
                csAssert.assertEquals(clientId, Integer.valueOf(fileSystem_clientId), "clientId is not correct " +
                        "expected " + fileSystem_clientId + " and actual " + clientId);
                csAssert.assertEquals(alias, expected_Alias, "alias is not correct " +
                        "expected " + fileSystem_clientId + " and actual " + alias);
                csAssert.assertEquals(vendorId, Integer.valueOf(3), "vendorId is not correct " +
                        "expected " + vendorId + " and actual " + 3);
                csAssert.assertTrue(region == null, "region  is  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(bucketName == null, "bucketName  is  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(fs_documentPath.contains(envDetail), "file_system_document is  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(key_id == null, "key_id  is  not  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(kms_client_id == null, "kms_client_id  is not  null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
                csAssert.assertTrue(kms_required.equals("f"), "kms_required  is not null for " +
                        "documentId " + dbArray.get(1) + "and clientId " + clientId);
            }
            else {
                logger.error("row is not inserted in DB corresponding to documentId "+
                        JSONUtility.parseJson(response,"$.documentDetails.documentId") );
            }
        }
         }else{
            csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"CREATED",
                    "status is not correct in case valid file uploaded "+ response);
        }
        csAssert.assertAll();

    }



      @Test(description = "C152267")
    public void TestEnvDetailnotPassed() {
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(null,null,aws_clientId,null,validFileName);
        csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"BAD_REQUEST",
                "status is not correct in case of clientId is not Passed"+ response);
        csAssert.assertEquals(  JSONUtility.parseJson(response,"$.errorList[0].message"),"Environment details are empty",
                "resonse error is not correct in case of EnvDetails is not Passed "+ response );
        csAssert.assertAll();


    }




    @Test(description = "C153444")
    public void KMSNotConfigured(){
        kmsNotConfigured_clientId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "kmsnotclientid");
        if(kmsNotConfigured_clientId!=null && !kmsNotConfigured_clientId.equals("")){
        CustomAssert csAssert = new CustomAssert();
        String response = drsUtil.uploadFile(null,null,kmsNotConfigured_clientId,envDetail,validFileName);
        if(JSONUtility.parseJson(response,"$.status").equals("CREATED")){

            if(validateInDB.equalsIgnoreCase("true")){
                String query = "select * from document_metadata order by id desc limit 1;";
                List<String> dbArray = getValuefromDB(query);
                if(JSONUtility.parseJson(response,"$.documentDetails.documentId")
                        .equals(dbArray.get(1)) && dbArray.get(16).equalsIgnoreCase("document successfully loaded")){
                    String expectedFileName = validFileName.split("\\.")[0];
                    String actualFileName =  dbArray.get(17);
                    String expectedExtension = validFileName.split("\\.")[1];
                    String actualExtension = dbArray.get(18);
                    Integer clientId = Integer.valueOf(dbArray.get(3));
                    String alias =  dbArray.get(4);
                    Integer vendorId = Integer.valueOf(dbArray.get(10));
                    String region = dbArray.get(11);
                    String bucketName = dbArray.get(12);
                    String fs_documentPath = dbArray.get(13);
                    String antivirusEnabled =dbArray.get(5);
                    String encriptionEnabled = dbArray.get(6);
                    String key_id =  dbArray.get(7);
                    String kms_client_id =  dbArray.get(8);
                    String kms_required =  dbArray.get(9);

                    String expected_Alias =  admin.getClientAliasFromDB(Integer.valueOf(kmsNotConfigured_clientId));

                    csAssert.assertEquals( "f",antivirusEnabled,"antivirus  is not enabled" +
                            "expected "+ false +" and actual "+ antivirusEnabled);
                    csAssert.assertEquals( "f",encriptionEnabled,"encription is not enabled" +
                            "expected "+ false +" and actual "+ encriptionEnabled);
                    csAssert.assertEquals(clientId, Integer.valueOf(kmsNotConfigured_clientId), "clientId is not correct " +
                            "expected " + kmsNotConfigured_clientId + " and actual " + clientId);
                    csAssert.assertEquals(alias, expected_Alias, "alias is not correct " +
                            "expected " + kmsNotConfigured_clientId + " and actual " + alias);
                    csAssert.assertEquals(vendorId, Integer.valueOf(1), "vendorId is not correct " +
                            "expected " + vendorId + " and actual " + 1);
                    csAssert.assertTrue(region != null, "region  is  null for " +
                            "documentId " + dbArray.get(1) + "and clientId " + clientId);
                    csAssert.assertTrue(bucketName != null, "bucketName  is  null for " +
                            "documentId " + dbArray.get(1) + "and clientId " + clientId);
                    csAssert.assertTrue(fs_documentPath == null, "file_system_document is  null for " +
                            "documentId " + dbArray.get(1) + "and clientId " + clientId);
                    csAssert.assertTrue(key_id != null, "key_id  is  not  null for " +
                            "documentId " + dbArray.get(1) + "and clientId " + clientId);
                    csAssert.assertTrue(kms_client_id.equals("0"), "kms_client_id  is not  null for " +
                            "documentId " + dbArray.get(1) + "and clientId " + clientId);
                    csAssert.assertTrue(kms_required.equals("t"), "kms_required  is not null for " +
                            "documentId " + dbArray.get(1) + "and clientId " + clientId);
                    csAssert.assertEquals( expectedFileName,actualFileName,"filename is not correct" +
                            "expected "+ expectedFileName +" and actual "+ actualFileName);
                    csAssert.assertEquals( expectedExtension,actualExtension,"extension is not correct" +
                            "expected "+ expectedExtension +" and actual "+ actualExtension);
                }

                else {
                    logger.error("row is not inserted in DB corresponding to documentId "+
                            JSONUtility.parseJson(response,"$.documentDetails.documentId") );
                }
            }
        }else{
            csAssert.assertEquals( JSONUtility.parseJson(response,"$.status"),"CREATED",
                    "status is not correct in case valid file uploaded "+ response);
        }
        csAssert.assertAll();
        }else{
            throw new SkipException("client id is null as we don't have any client for which kms is not configured");
        }

    }


    private  List<String> getValuefromDB(String query) {
        try {
            List<List<String>> rs = db.doSelect(query);
            int time = 0;
            while (time< 300000) {
                if (rs.get(0).get(16)!=null)
                break;
                else
                    Thread.sleep(5000);
                    time = time + 5000;
                rs = db.doSelect(query);

            }
            System.out.println(rs.get(0).get(16));
            return  rs.get(0) ;
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }


    }
}
