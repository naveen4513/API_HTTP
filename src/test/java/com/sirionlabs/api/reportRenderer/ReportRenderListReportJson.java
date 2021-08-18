package com.sirionlabs.api.reportRenderer;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import jdk.nashorn.api.scripting.JSObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author manoj.upreti
 */
public class ReportRenderListReportJson extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ReportRenderListReportJson.class);
	private String reportDataJsonStr = null;
	private HttpResponse reportRendererResponse = null;


	public HttpResponse hitReportRender() {
		try {
			HttpPost postRequest;
			String queryString = "/reportRenderer/listreportJson";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("X-Requested-With", "XMLHttpRequest");


			reportRendererResponse = APIUtils.postRequest(postRequest, "");
			this.reportDataJsonStr = EntityUtils.toString(reportRendererResponse.getEntity());

		} catch (Exception e) {
			logger.error("Exception while hitting ReportRenderListReportJson Api. {}", e.getMessage());
		}

		return reportRendererResponse;
	}

	public String getReportRendorJsonStr() {
		return this.reportDataJsonStr;
	}

	/**
	 * @param entityTypeId will take the entityTypeId
	 * @return the list of reportIds for the given entityTypeId in reportRendererResponse
	 */
	public List<Integer> getReportIdsforEntity(int entityTypeId) {
		hitReportRender();
		String reportRenderJsonStr = getReportRendorJsonStr();
		logger.info("The Report Response is : [ {} ]", reportRenderJsonStr);
		List<Integer> reportIds = new ArrayList<>();

		JSONArray jsonArray = new JSONArray(reportRenderJsonStr);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			if (jsonObject.has("entityTypeId") && jsonObject.has("listMetaDataJsons") && jsonObject.getInt("entityTypeId") == entityTypeId) {
				JSONArray listMetaDataJsons = jsonObject.getJSONArray("listMetaDataJsons");

				for (int j = 0; j < listMetaDataJsons.length(); j++) {

					reportIds.add(listMetaDataJsons.getJSONObject(j).getInt("id"));
				}
			}
		}

		return reportIds;
	}

	public void dumpReportMetadataInCsvFromJson(JSONArray reportJsonArrayForEntities, String csvFileName, String reportRenderCSVDelemeter) throws FileNotFoundException {
		int noOfEntitiesInReport = reportJsonArrayForEntities.length();
		logger.debug("Number of Records [ {} ]", noOfEntitiesInReport);

		File csvFile = new File(csvFileName);
		if (csvFile.exists()) {
			csvFile.delete();
		}

		PrintWriter pw = new PrintWriter(new File(csvFileName));
		StringBuilder sb = new StringBuilder();

		sb.append("Entity Name");
		sb.append(reportRenderCSVDelemeter);
		sb.append("Entity Type ID");
		sb.append(reportRenderCSVDelemeter);
		sb.append("Sub Report Name");
		sb.append(reportRenderCSVDelemeter);
		sb.append("Sub Report ID");
		sb.append(reportRenderCSVDelemeter);
		sb.append("isManualReport");
		sb.append('\n');

		for (int rc = 0; rc < noOfEntitiesInReport; rc++) {
			JSONObject obj = reportJsonArrayForEntities.getJSONObject(rc);
			JSONUtility jsonUtilObj = new JSONUtility(obj);
			String entityTypeId = jsonUtilObj.getStringJsonValue("entityTypeId");
			String entityName = jsonUtilObj.getStringJsonValue("name");
			logger.info("Currently dumping for entityName [ {} ] , and  entityTypeId [ {} ] ", entityName, entityTypeId);

			JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
			int sizeOfSubReports = subReportsJsonArray.length();
			for (int sc = 0; sc < sizeOfSubReports; sc++) {
				JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
				JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
				String entityTypeName = jsonUtilObjForSubReport.getStringJsonValue("entityTypeName");
				String entityId = jsonUtilObjForSubReport.getStringJsonValue("entityTypeId");
				String name = jsonUtilObjForSubReport.getStringJsonValue("name");
				String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
				String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");

				logger.debug("Entity Name : [ {} ] ", entityTypeName);
				logger.debug("Entity Type ID :  [ {} ] ", entityId);
				logger.debug("Sub Report Name :  [ {} ] ", name);
				logger.debug("Sub Report ID :  [ {} ] ", reportId);
				logger.debug("isManualReport :  [ {} ] ", isManualReport);


				sb.append(entityTypeName);
				sb.append(reportRenderCSVDelemeter);
				sb.append(entityId);
				sb.append(reportRenderCSVDelemeter);
				sb.append(name);
				sb.append(reportRenderCSVDelemeter);
				sb.append(reportId);
				sb.append(reportRenderCSVDelemeter);
				sb.append(isManualReport);
				sb.append('\n');

			}

		}

		pw.write(sb.toString());
		pw.close();
	}

	public Map<String, Map<String, String>> generateReportsMap(JSONArray reportJsonArrayForEntities) {
		int noOfEntitiesInReport = reportJsonArrayForEntities.length();
		logger.debug("Number of Records [ {} ]", noOfEntitiesInReport);
		Map<String, Map<String, String>> reportMap = new HashMap<>();

		for (int rc = 0; rc < noOfEntitiesInReport; rc++) {
			JSONObject obj = reportJsonArrayForEntities.getJSONObject(rc);
			JSONUtility jsonUtilObj = new JSONUtility(obj);
			String entityTypeId = jsonUtilObj.getStringJsonValue("entityTypeId");
			JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
			int sizeOfSubReports = subReportsJsonArray.length();
			Map<String, String> subReportsMap = new HashMap<>();
			for (int sc = 0; sc < sizeOfSubReports; sc++) {
				JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
				JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
				String reportName = jsonUtilObjForSubReport.getStringJsonValue("name");
				String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
				subReportsMap.put(reportId, reportName);
			}
			reportMap.put(entityTypeId, subReportsMap);
		}
		return reportMap;
	}

	public Map<Integer, String> getReportIdNameMapExcludingManualReports(String reportRenderListReportJson, Integer entityTypeId) {
		Map<Integer, String> reportIdNameMap = new HashMap<>();
		Integer reportId;
		String reportName;
		try {
			JSONArray reportJsonArray = new JSONArray(reportRenderListReportJson);
			for (int i = 0; i < reportJsonArray.length(); i++) {
				JSONObject jsonObj = reportJsonArray.getJSONObject(i);
				if (jsonObj.getInt("entityTypeId") == entityTypeId) {
					JSONArray reportArray = jsonObj.getJSONArray("listMetaDataJsons");
					for (int j = 0; j < reportArray.length(); j++) {
						if (reportArray.getJSONObject(j).getBoolean("isManualReport")) {
							continue;
						}
						reportId = reportArray.getJSONObject(j).getInt("id");
						reportName = reportArray.getJSONObject(j).getString("name");
						reportIdNameMap.put(reportId, reportName);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting report Id-Name Map. {}", e.getMessage());
		}
		return reportIdNameMap;
	}

}
