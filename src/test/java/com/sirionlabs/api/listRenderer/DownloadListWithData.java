package com.sirionlabs.api.listRenderer;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shivashish on 20/9/17.
 */
public class DownloadListWithData extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(DownloadListWithData.class);
    static int offset = 0;
    static String orderByColumnName = "id";
    static String orderDirection = "desc";
    static int maxRandomOptions = 5;
    static Integer size = 20;
    static String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
    static String outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");
    String entitySectionSplitter = ",";

    public HttpResponse hitDownloadListWithData(Map<String, String> formParam, Integer entityURLId) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/download/" + entityURLId + "/data";


            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            String params = UrlEncodedString.getUrlEncodedString(formParam);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, params);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                logger.debug("DownloadListWithData response header {}", headers[i].toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting DownloadListWithData Api. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitListRendererDownload(Map<String, String> formParam, Map<String, String> urlStringMap, Integer entityURLId) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String appendString = "";

            if (urlStringMap != null && !urlStringMap.isEmpty()) {
                for (Map.Entry<String, String> entry : urlStringMap.entrySet()) {
                    appendString = appendString.concat(entry.getKey() + "=" + entry.getValue() + "&");
                }
            }

            if (!appendString.equalsIgnoreCase("")) {
                appendString = appendString.substring(0, appendString.length() - 1);
            }

            String queryString = "/listRenderer/download/" + entityURLId + "/data?" + appendString;

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            String params = UrlEncodedString.getUrlEncodedString(formParam);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, params);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                logger.debug("DownloadListWithData response header {}", headers[i].toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting DownloadListWithData Api. {}", e.getMessage());
        }
        return response;
    }

    public Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId) {

        Map<String, String> formParam = new HashMap<String, String>();

        String jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }


    //created by Rishabh on 24th july 2019


    public Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap, String payload) {
        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;

        if (selectedColumnMap == null) {
//			jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{\"111\":{\"filterId\":\"111\",\"filterName\":\"createdDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null,\"start\":\"07-31-2019\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}}}";
            jsonData = payload;

        } else {
            String selectedColumnArray = "\"selectedColumns\":[";
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
            }
            selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
            selectedColumnArray += "]";

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}," + selectedColumnArray + "}";
        }

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }


    /*
     * Below method is for dumping the DownloadListWithData response into file(default format : xlsx). Column status is appended in the file name to differentiate among :
     * 1. All Columns
     * 2. Default Selected Columns
     * 3. Randomized selected Columns
     *
     * featureName parameter is for creating new folder in the output directory. This will help to easily analyse the downloaded files separately for downloadList and downloadListWithData feature.
     * featureName = DownloadData
     * */
    public String dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status)
                logger.info("DownloadListWithData file generated at {}", outputFile);


        }
        return outputFile;
    }

    public Boolean hitDownloadSLPerformanceTab(String documentId, String outputFilePath, String outputFileName) {
        Boolean fileDownloaded = false;
        String queryString = "/download/v1/msaPerformanceData/" + documentId;
        try {

            HttpHost target = super.generateHttpTargetHost();
            logger.debug("Query string url formed is {}", queryString);
            String acceptHeader = "*/*";

            HttpGet getRequest = super.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
            fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting BulkUpload Download Api using QueryString [{}]. {}", queryString, e.getStackTrace());
        }
        return fileDownloaded;
    }

    public Boolean hitDownloadCSLPerformanceTab(String documentId, String outputFilePath, String outputFileName) {
        Boolean fileDownloaded = false;
        String queryString = "/download/v1/cslaPerformanceData/" + documentId;
        try {

            HttpHost target = super.generateHttpTargetHost();
            logger.debug("Query string url formed is {}", queryString);
            String acceptHeader = "*/*";

            HttpGet getRequest = super.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
            fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting BulkUpload Download Api using QueryString [{}]. {}", queryString, e.getStackTrace());
        }
        return fileDownloaded;
    }


    public boolean dumpDownloadListIntoFile(HttpResponse response, String outputFilePath, String outPutFileName) {
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();

        try {
            outputFile = outputFilePath + "//" + outPutFileName;

            return fileUtil.writeResponseIntoFile(response, outputFile);
        } catch (Exception e) {
            return false;
        }
    }

    public HttpResponse hitDownloadTabListData(int entityTypeId, int recordId, int tabId, String shortCodeId, Map<String, String> params) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/download/" + tabId + "/data?entityId=" + recordId + "&entityTypeId=" + entityTypeId +
                    "&tabList=true&clientEntitySeqId=" + shortCodeId;

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            String paramsStr = UrlEncodedString.getUrlEncodedString(params);

            response = APIUtils.postRequest(postRequest, paramsStr);
        } catch (Exception e) {
            logger.error("Exception while hitting DownloadTabListData Api. {}", e.getMessage());
        }
        return response;
    }

}
