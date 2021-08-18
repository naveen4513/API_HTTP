package com.sirionlabs.api.calendar;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class CalendarExportData extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(CalendarExportData.class);

    public HttpResponse hitCalendarExportData(int month, int year) throws Exception {
        return hitCalendarExportData(month, year, "false");
    }

    public HttpResponse hitCalendarExportData(int month, int year, String calendarA) throws Exception {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/calendar/exportData?month=" + month + "&year=" + year + "&calendarA=" + calendarA;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            String data = "{\"startDate\":\"04/13/2020\",\"endDate\":\"04/13/2020\"}";
            Map<String, String> parameters = new HashMap<>();
            parameters.put("data", data);
            parameters.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            String params = UrlEncodedString.getUrlEncodedString(parameters);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, params);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Calendar Export Data response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Calendar Export Data Api. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse downloadCalendarDataFile(int month, int year, String file) {
        return downloadCalendarDataFile(month, year, "false", file);
    }

    public HttpResponse downloadCalendarDataFile(int month, int year, String calendarA, String file) {
        return downloadCalendarDataFile(month, year, null, calendarA, file);
    }

    public HttpResponse downloadCalendarDataFile(int month, int year, String dateData, String calendarA, String file) {
        try {
            HttpHost target = generateHttpTargetHost();
            String queryString = "/calendar/exportData?month=" + month + "&year=" + year + "&calendarA=" + calendarA;
            String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
            String contentTypeHeader = "application/x-www-form-urlencoded";

            if (dateData == null) {
                dateData = "{\"startDate\":\"" + DateUtils.getMonthStartDateInMMDDFormat(month) + "/" + year + "\",\"endDate\":\"" +
                        DateUtils.getMonthEndDateInMMDDFormat(month, year) + "\"}";
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put("data", dateData);
            parameters.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            String params = UrlEncodedString.getUrlEncodedString(parameters);
            HttpPost httpPostRequest = generateHttpPostRequestWithQueryStringAndPayload(queryString, acceptsHeader, contentTypeHeader, params);
            return downloadAPIResponseFile(file, target, httpPostRequest);
        } catch (Exception e) {
            logger.error("Exception while Downloading Calendar Export Data File {}. {}", file, e.getStackTrace());
            return null;
        }
    }

    public boolean downloadCalendarDataFile2(int month, int year,  String calendarA, String file) {
        HttpResponse response = null;
        Boolean status = false;
        try {
            HttpPost postRequest;

            String queryString = "/calendar/exportData?month=" + month + "&year=" + year + "&calendarA=" + calendarA;
            String dateData = "{\"startDate\":\"04/13/2020\",\"endDate\":\"04/13/2020\"}";

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            Map<String, String> parameters = new HashMap<>();
            parameters.put("data", dateData);
            parameters.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            String params = UrlEncodedString.getUrlEncodedString(parameters);

            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, params);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                logger.debug("DownloadListWithData response header {}", headers[i].toString());
            }

            if (response.getStatusLine().toString().contains("200")) {
                FileUtils fileUtil = new FileUtils();
                status = fileUtil.writeResponseIntoFile(response, file);
                if (status)
                    logger.info("Calendar Data file generated at {}", file);
                else{
                    logger.error("Error while downloading Calendar data.");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Downloading Calendar Export Data File .",  e.getStackTrace());
            return false;
        }
        return status;
    }
}
