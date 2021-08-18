package com.sirionlabs.api.dashboard;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.PayloadUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vijay.thakur on 7/25/2017.
 */
public class DashboardData extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DashboardData.class);
	String dashboardDataJsonStr = null;

	public static List<Integer> getAllChartsId(String dashboardDataJsonStr) {
		List<Integer> chartIdList = new ArrayList<Integer>();
		try {
			JSONArray responseArray = new JSONArray(dashboardDataJsonStr);
			for (int i = 0; i < responseArray.length(); i++) {
				chartIdList.add(Integer.parseInt(responseArray.getJSONObject(i).get("chartId").toString()));
			}
		} catch (Exception e) {
			logger.error("Exception while fetching Dashboard Chard Id : " + e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
		}
		return chartIdList;
	}

	public static List<String> getAllAttributes(String jsonStr) {
		List<String> allAttributes = new ArrayList<String>();
		try {
			JSONArray jsonArr = new JSONArray(jsonStr);
			JSONObject jsonObj = new JSONObject(jsonArr.get(0).toString());
			jsonObj = jsonObj.getJSONObject("attributes");
			String attributes[] = JSONObject.getNames(jsonObj);

			for (int i = 0; i < attributes.length; i++)
				allAttributes.add(attributes[i].trim());
		} catch (Exception e) {
			logger.error("Exception while getting All Attributes from DashBoard Analysis Response. {}", e.getMessage());
		}
		return allAttributes;
	}

	public static Map<String, String> getAttributeDetails(String jsonStr, String attributeName) {
		Map<String, String> attributeDetails = new HashMap<String, String>();
		String details[] = {"name", "type"};
		try {
			JSONArray jsonArr = new JSONArray(jsonStr);
			JSONObject jsonObj = new JSONObject(jsonArr.get(0).toString());
			jsonObj = jsonObj.getJSONObject("attributes").getJSONObject(attributeName);

			for (int i = 0; i < details.length; i++) {
				if (jsonObj.has(details[i]))
					attributeDetails.put(details[i], jsonObj.get(details[i]).toString());
			}
		} catch (Exception e) {
			logger.error("Exception while fetching details of Attribute {}. {}", attributeName, e.getMessage());
		}
		return attributeDetails;
	}

	public static String getAttributeType(String jsonStr, String attributeName) {
		String attributeType = null;
		try {
			JSONArray jsonArr = new JSONArray(jsonStr);
			JSONObject jsonObj = new JSONObject(jsonArr.get(0).toString());
			jsonObj = jsonObj.getJSONObject("attributes").getJSONObject(attributeName);

			if (jsonObj.has("type"))
				return jsonObj.getString("type");
		} catch (Exception e) {
			logger.error("Exception while fetching Type of Attribute {}. {}", attributeName, e.getMessage());
		}

		return attributeType;
	}

	public static List<String> getFilterData(String jsonStr, String filterName) {
		List<String> filterList = new ArrayList<String>();
		try {
			JSONArray jsonArr = new JSONArray(jsonStr);
			JSONObject jsonObj = new JSONObject(jsonArr.get(0).toString());
			JSONArray filterArray = jsonObj.getJSONObject("attributes").getJSONObject(filterName).getJSONArray("values");
			for (int i = 0; i < filterArray.length(); i++)
				filterList.add(filterArray.get(i).toString());
		} catch (Exception e) {
			logger.error("Exception while getting local filters for chart : {}", e.getMessage());
		}
		return filterList;
	}

	public HttpResponse hitDashboardData() {
		logger.info("No FormData Provided. Proceeding with default data.");
		String defaultChartObjValue = PayloadUtils.getPayloadForDashboardData();
		return hitDashboardData(defaultChartObjValue);
	}

	public HttpResponse hitDashboardData(String chartObjValue) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/dashboard/data/";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("chartObj", chartObjValue);
			String params = UrlEncodedString.getUrlEncodedString(parameters);
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			postRequest.addHeader("Accept", "text/html, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, params);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.dashboardDataJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("dashboard response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting dashboardData Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDashboardDataJsonStr() {
		return this.dashboardDataJsonStr;
	}
}
