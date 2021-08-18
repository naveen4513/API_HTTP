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

public class DashboardLocalFilters extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DashboardLocalFilters.class);
	String dashboardLocalFiltersJsonStr = null;

	public static List<String> getAllAttributes(String dashboardLocalFiltersJsonStr) {
		List<String> allAttributes = new ArrayList<String>();
		try {
			JSONObject jsonObj = new JSONObject(dashboardLocalFiltersJsonStr);
			jsonObj = jsonObj.getJSONObject("attributes");
			String attributes[] = JSONObject.getNames(jsonObj);

			if (attributes == null) {
				logger.warn("No Attributes found in Dashboard LocalFilter api response.[empty attribute object]");
			} else {
				for (int i = 0; i < attributes.length; i++)
					allAttributes.add(attributes[i].trim());
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Attributes from DashBoard Analysis Response. {}", e.getMessage());
		}
		return allAttributes;
	}

	public static String[] getAllAttributeLabels(String jsonStr) {
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

	public static String getAttributeType(String jsonStr, String attributeName) {
		String attributeType = null;
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
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
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray filterArray = jsonObj.getJSONObject("attributes").getJSONObject(filterName).getJSONArray("values");
			for (int i = 0; i < filterArray.length(); i++)
				filterList.add(filterArray.get(i).toString());
		} catch (Exception e) {
			logger.error("Exception while getting local filters for chart : {}", e.getMessage());
		}
		return filterList;
	}

	public static List<Map<String, String>> getRoleGroupsForStakeholder(String jsonStr, String stakeholderObjectName) {
		List<Map<String, String>> roleGroupsList = new ArrayList<Map<String, String>>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray jsonArr = jsonObj.getJSONObject("attributes").getJSONObject(stakeholderObjectName).getJSONObject("values").getJSONObject("OPTIONS")
					.getJSONArray("data");

			for (int i = 0; i < jsonArr.length(); i++) {
				Map<String, String> roleGroupMap = new HashMap<String, String>();
				jsonObj = new JSONObject(jsonArr.get(i).toString());
				roleGroupMap.put("id", jsonObj.get("id").toString().trim());
				roleGroupMap.put("name", jsonObj.get("name").toString().trim());
				roleGroupsList.add(roleGroupMap);
			}
		} catch (Exception e) {
			logger.error("Exception while getting RoleGroup Ids for Filter Stakeholder {}", e.getMessage());
		}
		return roleGroupsList;
	}

	public static List<Map<String, String>> getOptionsForStakeholderRoleGroup(String jsonStr, int roleGroupId, String stakeholderObjectName) {
		List<Map<String, String>> allOptions = new ArrayList<Map<String, String>>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray jsonArr = jsonObj.getJSONObject("attributes").getJSONObject(stakeholderObjectName).getJSONObject("values").getJSONObject("OPTIONS")
					.getJSONArray("data");

			logger.info("getting users options for {}", roleGroupId);
			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = new JSONObject(jsonArr.get(i).toString());

				if (jsonObj.getInt("id") == roleGroupId) {
					jsonArr = jsonObj.getJSONArray("users");

					for (int j = 0; j < jsonArr.length(); j++) {
						Map<String, String> optionsMap = new HashMap<String, String>();
						jsonObj = new JSONObject(jsonArr.get(j).toString());
						optionsMap.put("id", jsonObj.get("id").toString().trim());
						optionsMap.put("name", jsonObj.get("name").toString().trim());
						allOptions.add(optionsMap);
					}
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Options for RoleGroup Id {}. {}", roleGroupId, e.getMessage());
		}
		return allOptions;
	}

	public static String getStakeholderObjectName(String jsonStr) {
		String stakeholderObjectName = null;
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			jsonObj = jsonObj.getJSONObject("attributes");

			String[] objectNames = JSONObject.getNames(jsonObj);
			if (objectNames != null) {
				for (int i = 0; i < objectNames.length; i++) {
					JSONObject tempJsonObj = new JSONObject(jsonObj.getJSONObject(objectNames[i]).toString());

					if (tempJsonObj.getString("name").toLowerCase().contains("stakeholder")) {
						stakeholderObjectName = objectNames[i];
						break;
					}
				}
			} else
				logger.warn("Empty Attribute object found in DashboardAnalysis response. Hence returning empty stakeholderObjectName");
		} catch (Exception e) {
			logger.error("Exception while getting Stakeholder Json Object Name. {}", e.getMessage());
		}
		return stakeholderObjectName;
	}

	public static List<String> getAllAttributeNames(String jsonStr) {
		List<String> allAttributeNames = new ArrayList<String>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			jsonObj = jsonObj.getJSONObject("attributes");
			String attributes[] = JSONObject.getNames(jsonObj);
			if (attributes == null) {
				logger.warn("No Attributes found in Dashboard Analysis api response.[empty attribute object]");
			} else {
				for (String attribute : attributes)
					allAttributeNames.add(jsonObj.getJSONObject(attribute).getString("name"));
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Attribute Names. {}", e.getMessage());
		}
		return allAttributeNames;
	}

	public static String getAttributeNameFromLabel(String jsonStr, String label) {
		String attributeName = null;
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			attributeName = jsonObj.getJSONObject("attributes").getJSONObject(label).getString("name");
		} catch (Exception e) {
			logger.error("Exception while getting Attribute Name for Label {}. {}", label, e.getMessage());
		}
		return attributeName;
	}

	public static Map<String, String> getRoleGroupDataFromLabel(String jsonStr, String stakeholderObjectName, String roleGroupLabel) {
		Map<String, String> roleGroupData = new HashMap<String, String>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray jsonArr = jsonObj.getJSONObject("attributes").getJSONObject(stakeholderObjectName).getJSONObject("values").getJSONObject("OPTIONS")
					.getJSONArray("data");

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = new JSONObject(jsonArr.get(i).toString());
				if (jsonObj.getString("displayValue").trim().equalsIgnoreCase(roleGroupLabel)) {
					roleGroupData.put("id", jsonObj.get("id").toString().trim());
					roleGroupData.put("name", jsonObj.get("name").toString().trim());
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Data for RoleGroup {}. {}", roleGroupLabel, e.getMessage());
		}
		return roleGroupData;
	}

	public static Map<String, String> getOwnerDetailsFromLabel(String jsonStr, int roleGroupId, String stakeholderObjectName, String ownerLabel) {
		Map<String, String> ownerData = new HashMap<String, String>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray jsonArr = jsonObj.getJSONObject("attributes").getJSONObject(stakeholderObjectName).getJSONObject("values").getJSONObject("OPTIONS")
					.getJSONArray("data");

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = new JSONObject(jsonArr.get(i).toString());

				if (jsonObj.getInt("id") == roleGroupId) {
					jsonArr = jsonObj.getJSONArray("users");

					for (int j = 0; j < jsonArr.length(); j++) {
						jsonObj = new JSONObject(jsonArr.get(j).toString());
						if (jsonObj.getString("name").trim().equalsIgnoreCase(ownerLabel)) {
							ownerData.put("id", jsonObj.get("id").toString().trim());
							ownerData.put("name", jsonObj.get("name").toString().trim());
							break;
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Data for Owner {}. {}", ownerLabel, e.getMessage());
		}
		return ownerData;
	}

	public HttpResponse hitDashboardLocalFilters(String chartObjValue) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/dashboard/localFilters";
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
			this.dashboardLocalFiltersJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("dashboard local filters header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting dashboardLocalFilters Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDashboardLocalFiltersJsonStr() {
		return this.dashboardLocalFiltersJsonStr;
	}


}
