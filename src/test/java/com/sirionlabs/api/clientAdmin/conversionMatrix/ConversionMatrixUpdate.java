package com.sirionlabs.api.clientAdmin.conversionMatrix;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;

import java.io.File;
import java.util.Map;

public class ConversionMatrixUpdate extends APIUtils{

    public String updateResponse;

    public String hitConversionMatrixUpdate(String filePath,String fileName, Map<String, String> payloadMap) {
        try {
            String queryString = "/conversionMatrix/update";


            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
            HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

            File fileToUpload = new File(filePath + "/" + fileName);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            this.updateResponse = uploadFileToServer(target, postRequest);

        } catch (Exception e) {

        }

        return updateResponse;
    }

}
