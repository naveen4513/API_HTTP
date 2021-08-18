package com.sirionlabs.api.dashboard;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vijay.thakur on 7/25/2017.
 */
public class DashboardSLExecutiveData extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DashboardSLExecutiveData.class);
	String dashboardSLExecutiveDataJsonStr = null;

	public HttpResponse hitDashboardSLExecutiveData(String chartObjValue) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/dashboard/slExecutivedata/";
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
			this.dashboardSLExecutiveDataJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("dashboard SLExecutive api response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting dashboardSLExecutive Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDashboardSLExecutiveDataJsonStr() {
		return this.dashboardSLExecutiveDataJsonStr;
	}

	public List<String> getAllAttributeNames(String jsonStr) {
		List<String> allAttributes = new ArrayList<String>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			jsonObj = jsonObj.getJSONObject("attributes");
			String attributes[] = JSONObject.getNames(jsonObj);

			if (attributes == null) {
				logger.warn("No Attributes found in slExecutiveData api response.[empty attribute object]");
			} else {
				for (int i = 0; i < attributes.length; i++)
					allAttributes.add(attributes[i].trim());
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Attributes from slExecutiveData Response. {}", e.getMessage());
		}
		return allAttributes;
	}

	public List<String> getFilterData(String jsonStr, String filterName) {
		List<String> filterList = new ArrayList<String>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray filterArray = jsonObj.getJSONObject("attributes").getJSONObject(filterName).getJSONArray("values");
			for (int i = 0; i < filterArray.length(); i++)
				filterList.add(filterArray.get(i).toString());
		} catch (Exception e) {
			logger.error("Exception while getting local filters for chart : {}", e.getMessage());
		}
		return filterList;
	}

	public String[] getAllAttributeLabels(String jsonStr) {
		String attributeLabels[] = null;
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			jsonObj = jsonObj.getJSONObject("attributes");
			attributeLabels = JSONObject.getNames(jsonObj);
		} catch (Exception e) {
			logger.error("Exception while getting All Attribute Names. {}", e.getMessage());
		}
		return attributeLabels;
	}
}
