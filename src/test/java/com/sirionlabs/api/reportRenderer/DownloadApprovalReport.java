package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.util.HashMap;

public class DownloadApprovalReport extends APIUtils {

    public static String getApiPath(int cdrId) {
        return "/reportRenderer/approvalReport/download/426/160/" + cdrId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static boolean downloadApprovalReport(String filePath, String fileName, int cdrId) {
        HttpGet getRequest = new HttpGet(getApiPath(cdrId));
        HttpResponse response = APIUtils.getRequest(getRequest);
        return dumpResponseIntoFile(response, filePath, fileName);
    }

    private static boolean dumpResponseIntoFile(HttpResponse response, String outputFilePath, String outputFileName) {
        String outputFileExtension = ".xlsx";
        FileUtils fileUtil = new FileUtils();

        String outputFile = outputFilePath + "/" + outputFileName + outputFileExtension;
        return fileUtil.writeResponseIntoFile(response, outputFile);
    }
}