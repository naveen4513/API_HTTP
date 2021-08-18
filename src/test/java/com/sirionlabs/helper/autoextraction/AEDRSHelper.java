package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class AEDRSHelper {
    private final static Logger logger = LoggerFactory.getLogger(AEDRSHelper.class);
    static String drsFlag;

    public String checkDRSFlag() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        String drsFlagValue=null;
        try {
            HttpResponse sideLayoutResponse = AutoExtractionHelper.sideLayoutAPI();
            csAssert.assertTrue(sideLayoutResponse.getStatusLine().getStatusCode() == 200, "Response code is Invalid");
            String sideLayoutStr = EntityUtils.toString(sideLayoutResponse.getEntity());
            JSONObject sideLayoutJson = new JSONObject(sideLayoutStr);
            drsFlagValue = (String) sideLayoutJson.getJSONObject("data").getJSONObject("rightSideBar").getJSONObject("springProperties").get("drsEnabled");
        }
        catch (Exception e)
        {
            logger.info("Getting exception while fetching drs flag value");
            csAssert.assertTrue(false,"Getting exception while fetching drs flag value due to "+e.getMessage());
        }
        return drsFlagValue;
    }

    public String getPayloadforDRS(String templateFilePath, String templateFileName,int newlyCreatedProjectId)
    {
        String payload=null;
        String key=null;
        int pageNo=0;
        Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUploadForDRS(templateFilePath, templateFileName);

        String keyAndPages = uploadedFileProperty.get("filePathOnServer");
        JSONObject keyJson = new JSONObject(keyAndPages);
        key = (String) keyJson.get("documentId");
        pageNo = (int) keyJson.get("noOfPages");
        payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" +key + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"noOfPages\":\"" + pageNo + "\" ,\"projectIds\":[" + newlyCreatedProjectId + "]}]";

        return payload;

    }




}
