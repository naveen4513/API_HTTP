package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by gaurav bhadani on 21/8/18.
 */
public class DownloadReportWithData extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(DownloadReportWithData.class);

    private String ReportRendererFilterDataJsonStr = null;

    public HttpResponse hitDownloadReportWithData(Map<String, String> formParam, Integer reportID) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/reportRenderer/download/" + reportID + "/data";

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            String params = UrlEncodedString.getUrlEncodedString(formParam);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = postRequest(postRequest, params);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("DownloadReportListWithData response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting DownloadReportListWithData Api. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitReportRendererFilterData(Integer reportID) {
        return hitReportRendererFilterData(reportID, "{}", null);
    }

    public HttpResponse hitReportRendererFilterData(Integer reportId, String payload, Map<String, String> params) {
        HttpResponse response = null;

        try {
            String queryString = "/reportRenderer/list/" + reportId + "/filterData";
            if (params != null) {
                String urlParams = UrlEncodedString.getUrlEncodedString(params);
                queryString += "?" + urlParams;
            }
            logger.debug("Query string url formed is {}", queryString);
            HttpPost postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.ReportRendererFilterDataJsonStr = EntityUtils.toString(response.getEntity());
            logger.debug("response json is: {}", ReportRendererFilterDataJsonStr);

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("ListRenderer response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting ListRendererFilterData Api. {}", e.getMessage());
        }
        return response;
    }

    public String getReportRendererFilterDataJsonStr() {
        return this.ReportRendererFilterDataJsonStr;
    }

    public Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String outputFileName) {

        Boolean status;
        String outputFile;
        FileUtils fileUtil = new FileUtils();

        try {
            outputFile = outputFilePath + "/" + outputFileName;
            status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status) {
                logger.info("DownloadListWithData file generated at {}", outputFile);
                status = true;
            }
        }catch (Exception e){
            status = false;
        }
        return status;
    }
}