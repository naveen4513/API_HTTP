package com.sirionlabs.api.clientAdmin.report;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReportRendererListConfigure extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(ReportRendererListConfigure.class);

	public static String getApiPath(int reportId) {
		return "/reportRenderer/list/" + reportId + "/configure";
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();

		headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
		headers.put("Accept-Encoding", "gzip, deflate");
		headers.put("Content-Type", "application/json;charset=UTF-8");

		return headers;
	}

	public static String getReportListConfigureResponse(int reportId) {
		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		AdminHelper adminHelperObj = new AdminHelper();
		adminHelperObj.loginWithClientAdminUser();

		String reportListConfigureResponse = executor.post(getApiPath(reportId), getHeaders(), null).getResponse().getResponseBody();

		Check checkObj = new Check();
		checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

		return reportListConfigureResponse;
	}

	public static List<String> getAllEnabledExcelColumnsList(String reportConfigureResponse) {
		List<String> allEnabledExcelColumnsList = new ArrayList<>();

		try {
			JSONObject jsonObj = new JSONObject(reportConfigureResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("ecxelColumns");

			for (int i = 0; i < jsonArr.length(); i++) {
				if (!jsonArr.getJSONObject(i).getBoolean("deleted")) {
					allEnabledExcelColumnsList.add(jsonArr.getJSONObject(i).getString("name").toUpperCase());
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Enabled Excel Columns List. {}", e.getMessage());
			return null;
		}

		return allEnabledExcelColumnsList;
	}

	public static List<String> getAllEnabledFiltersList(String reportConfigureResponse) {
		List<String> allEnabledFiltersList = new ArrayList<>();

		try {
			JSONObject jsonObj = new JSONObject(reportConfigureResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

			for (int i = 0; i < jsonArr.length(); i++) {
				if (!jsonArr.getJSONObject(i).getBoolean("deleted")) {
					allEnabledFiltersList.add(jsonArr.getJSONObject(i).getString("name"));
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Enabled Filters List. {}", e.getMessage());
			return null;
		}

		return allEnabledFiltersList;
	}

	public static String getExcelColumnPropertyValueFromQueryName(String reportConfigureResponse, String queryName, String propertyName) {
		try {
			JSONObject jsonObj = new JSONObject(reportConfigureResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("ecxelColumns");

			for (int i = 0; i < jsonArr.length(); i++) {
				if (jsonArr.getJSONObject(i).getString("queryName").equalsIgnoreCase(queryName)) {
					if (jsonArr.getJSONObject(i).has(propertyName) && !jsonArr.getJSONObject(i).isNull(propertyName)) {
						return jsonArr.getJSONObject(i).get(propertyName).toString();
					}

					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Value of Property {} for Query Name {}. {}", propertyName, queryName, e.getStackTrace());
		}

		return null;
	}

	public static String updateReportListConfigureResponse(int reportId,String payload) {
		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		AdminHelper adminHelperObj = new AdminHelper();
		adminHelperObj.loginWithClientAdminUser();

		String reportListConfigureResponse = executor.post("/reportRenderer/list/" + reportId + "/listConfigureUpdate?reportName=undefined", getHeaders(), payload).getResponse().getResponseBody();

		Check checkObj = new Check();
		checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

		return reportListConfigureResponse;
	}
}