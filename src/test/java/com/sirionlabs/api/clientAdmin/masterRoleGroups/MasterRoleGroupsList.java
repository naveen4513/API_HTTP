package com.sirionlabs.api.clientAdmin.masterRoleGroups;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MasterRoleGroupsList extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(MasterRoleGroupsList.class);

	public static String getAPIPath() {
		return "/masterrolegroups/list";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static String getRoleGroupsListResponse() {
		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		AdminHelper adminHelperObj = new AdminHelper();

		//Logging with Client Admin User
		if (!adminHelperObj.loginWithClientAdminUser()) {
			return null;
		}

		logger.info("Hitting Role Groups List API.");
		String roleGroupsListResponse = executor.get(getAPIPath(), getHeaders()).getResponse().getResponseBody();

		//Logging back with End User
		adminHelperObj.loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);

		return roleGroupsListResponse;
	}

	public static Integer getRoleGroupId(String roleGroupListResponse, String roleGroupName) {
		try {
			logger.info("Getting Id for Role Group {}", roleGroupName);
			Document html = Jsoup.parse(roleGroupListResponse);
			Element div = html.getElementById("l_com_sirionlabs_model_MasterUserRoleGroup").child(1);
			Elements allSubDivs = div.children();

			AdminHelper adminObj = new AdminHelper();

			for (Element subDiv : allSubDivs) {
				String groupName = subDiv.child(1).child(0).childNode(0).toString().replace("\n", "");

				if (groupName.equalsIgnoreCase(roleGroupName)) {
					String hrefValue = subDiv.child(0).childNode(0).attr("href");
					return adminObj.getIdFromHrefValue(hrefValue);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Id for Role Group {}. {}", roleGroupName, e.getStackTrace());
		}

		return null;
	}

	public static List<Map<String, String>> getAllRoleGroupsOfEntity(String roleGroupListsResponse, String entityName) {
		List<Map<String, String>> allRoleGroupsOfEntity = new ArrayList<>();

		try {
			logger.info("Getting All Role Groups of Entity {}", entityName);
			Document html = Jsoup.parse(roleGroupListsResponse);
			Elements allRoleGroups = html.getElementById("l_com_sirionlabs_model_MasterRoleGroup").child(1).children();

			for (Element roleGroup : allRoleGroups) {
				if (roleGroup.tagName().equalsIgnoreCase("script")) {
					continue;
				}

				if (roleGroup.child(1).child(0).childNode(0).toString().replaceAll("\n", "").equalsIgnoreCase(entityName)) {
					Map<String, String> roleGroupMap = new HashMap<>();

					String idValue = roleGroup.child(0).child(0).attr("href");
					String[] temp = idValue.split(Pattern.quote(";"));
					String[] temp2 = temp[0].split(Pattern.quote("/"));

					String roleGroupId = temp2[temp2.length - 1];
					roleGroupMap.put("roleGroupId", roleGroupId);

					String displayName = roleGroup.child(5).child(0).childNode(0).toString().replaceAll("\n", "");
					roleGroupMap.put("displayName", displayName);

					allRoleGroupsOfEntity.add(roleGroupMap);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Role Groups of Entity {}. {}", entityName, e.getMessage());
			return null;
		}

		return allRoleGroupsOfEntity;
	}
}