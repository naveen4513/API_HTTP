package com.sirionlabs.test.drs;

import com.sirionlabs.api.drs.DocumentServiceUploadApi;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DocumentHelper;



import java.util.HashMap;

public class DRSUtils {


    private  static  String uploadQueryPath = "/drs/document/v1/upload";
    private static String uploadFilePath = "src\\test\\resources\\TestData\\DRS\\Upload";
    DocumentServiceUploadApi drsUpload = new DocumentServiceUploadApi();



    public String uploadFile(String fileName, String fileExtension,String clientId, String envDetail, String uploadfileName ) {
        String  hostName = ConfigureEnvironment.getEnvironmentProperty("document_service_host");
        Integer port = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("document_service_port"));
        String scheme = ConfigureEnvironment.getEnvironmentProperty("document_service_scheme");

        HashMap<String, String> params = drsUpload.getParamas(fileName, fileExtension, clientId, envDetail);
        String response = DocumentHelper.uploadDRSFile(hostName, port, scheme, uploadQueryPath, uploadFilePath, uploadfileName, params);
           return  response;


    }


}
