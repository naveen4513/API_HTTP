package com.sirionlabs.api.dashboard;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vijay.thakur on 7/28/2017.
 */
public class DashboardCharts extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DashboardCharts.class);
	String dashboardChartsJsonStr = null;

	public HttpResponse hitDashboardCharts() {
		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/dashboard/charts/?homePageView=2&myView=false&staging=false";
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			getRequest.addHeader("Accept", "text/html, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.dashboardChartsJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("dashboard charts response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting dashboard/charts Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDashboardChartsJsonStr() {
		return this.dashboardChartsJsonStr;
	}

	public List<Integer> getAllChartIds(String dashboardChartsJsonStr) {
		List<Integer> allChartsId = new ArrayList<>();
		JSONArray chartArray = new JSONArray(dashboardChartsJsonStr);
		for (int i = 0; i < chartArray.length(); i++)
			allChartsId.add(chartArray.getJSONObject(i).getInt("id"));

		return allChartsId;
	}

	public List<Integer> getAllChartIdsExcludingManualCharts(String dashboardChartsJsonStr) {
		List<Integer> allChartsId = new ArrayList<>();
		JSONArray chartArray = new JSONArray(dashboardChartsJsonStr);
		for (int i = 0; i < chartArray.length(); i++)
			if (!chartArray.getJSONObject(i).getBoolean("manual"))
				allChartsId.add(chartArray.getJSONObject(i).getInt("id"));

		return allChartsId;
	}

	public Map<Integer, String> getAllChartIdNameMapping(String dashboardChartsJsonStr) {
		Map<Integer, String> allChartsIdNameMap = new HashMap<Integer, String>();
		JSONArray chartArray = new JSONArray(dashboardChartsJsonStr);
		for (int i = 0; i < chartArray.length(); i++)
			allChartsIdNameMap.put(chartArray.getJSONObject(i).getInt("id"), chartArray.getJSONObject(i).getString("name"));

		return allChartsIdNameMap;
	}
}
