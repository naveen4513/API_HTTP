package com.sirionlabs.helper.Reports;

import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ReportsDefaultUserListMetadataHelper {

    private final static Logger logger = LoggerFactory.getLogger(ReportsDefaultUserListMetadataHelper.class);

    private ReportRendererDefaultUserListMetaData defaultUserListObj = new ReportRendererDefaultUserListMetaData();

    public String hitDefaultUserListMetadataAPIForReportId(int reportId) {
        try {
            logger.info("Hitting DefaultUserListMetadata API for ReportId {}", reportId);
            defaultUserListObj.hitReportRendererDefaultUserListMetadata(reportId);
            return defaultUserListObj.getReportRendererDefaultUserListMetaDataJsonStr();
        } catch (Exception e) {
            logger.error("Exception while Hitting DefaultUserListMetadata API for Report Id {}. {}", reportId, e.getMessage());
        }

        return null;
    }

    public Boolean isFieldPresentInDefaultUserListMetadataAPIResponse(String defaultUserListMetadataResponse, String queryName) {
        try {
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("columns");

                for (int i = 0; i < jsonArr.length(); i++) {
                    if (jsonArr.getJSONObject(i).getString("queryName").trim().equalsIgnoreCase(queryName.trim())) {
                        return true;
                    }
                }

                return false;
            } else {
                logger.error("DefaultUserListMetadata API Response is an Invalid JSON. Hence couldn't check Field having QueryName [{}]", queryName);
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Field having QueryName [{}] is Present in DefaultUserListMetadata API Response or not. {}", queryName,
                    e.getStackTrace());
        }
        return null;
    }

    public List<String> getAllListDataFieldsQueryName(String defaultUserListMetadataResponse) {
        try {
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                List<String> allFieldsQueryName = new ArrayList<>();

                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("columns");

                for (int i = 0; i < jsonArr.length(); i++) {
                    allFieldsQueryName.add(jsonArr.getJSONObject(i).getString("queryName"));
                }

                return allFieldsQueryName;
            } else {
                logger.error("DefaultUserListMetadata API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All List Data Fields from DefaultUserListMetadata API Response. {}", e.getMessage());
        }

        return null;
    }

    public String getFilterMetadataPropertyValueFromQueryName(String defaultUserListMetadataResponse, String queryName, String propertyName) {
        try {
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String actualQueryName = jsonArr.getJSONObject(i).getString("queryName");

                    if (queryName.equalsIgnoreCase(actualQueryName)) {
                        if (jsonArr.getJSONObject(i).has(propertyName)) {
                            return jsonArr.getJSONObject(i).get(propertyName).toString();
                        } else {
                            logger.error("FilterMetadata having QueryName {} doesn't contain Property {}", queryName, propertyName);
                        }

                        break;
                    }
                }
            } else {
                logger.error("DefaultUserListMetadata Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Value of Property {} of Query Name {}. {}", propertyName, queryName, e.getMessage());
        }

        return null;
    }

    public String getColumnPropertyValueFromQueryName(String defaultUserListMetadataResponse, String queryName, String propertyName) {
        try {
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("columns");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String actualQueryName = jsonArr.getJSONObject(i).getString("queryName");

                    if (queryName.equalsIgnoreCase(actualQueryName)) {
                        if (jsonArr.getJSONObject(i).has(propertyName)) {
                            return jsonArr.getJSONObject(i).get(propertyName).toString();
                        } else {
                            logger.error("FilterMetadata having QueryName {} doesn't contain Property {}", queryName, propertyName);
                        }

                        break;
                    }
                }
            } else {
                logger.error("DefaultUserListMetadata Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Value of Property {} of Query Name {}. {}", propertyName, queryName, e.getMessage());
        }

        return null;
    }

    public Boolean isReportListingAvailable(String reportName, int reportId) {
        return isReportListingAvailable(hitDefaultUserListMetadataAPIForReportId(reportId), reportName, reportId);
    }

	public Boolean isReportListingAvailable(String defaultUserListMetadataResponse, String reportName, int reportId) {
        try {
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse).getJSONObject("reportMetadataJson");

                if (jsonObj.has("isListing") && !jsonObj.isNull("isListing")) {
                    return jsonObj.getBoolean("isListing");
                }
            } else {
                logger.error("DefaultUserListMeta API Response for Report [{}] having Id {} is an Invalid JSON.", reportName, reportId);
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Report having Id {} has Listing Available or not. {}", reportId, e.getMessage());
        }

        return null;
    }

    public Boolean isReportDownloadAble(String defaultUserListMetadataResponse,String reportId, String reportName)
    {
        try {
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse).getJSONObject("reportMetadataJson");

                if (jsonObj.has("isDownload") && !jsonObj.isNull("isDownload")) {
                    return jsonObj.getBoolean("isDownload");
                }
            } else {
                logger.error("DefaultUserListMeta API Response for Report [{}] having Id {} is an Invalid JSON.", reportName, reportId);
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Report having Id {} has  DownloadAble  or not. {}", reportId, e.getMessage());
        }

        return null;
    }
}