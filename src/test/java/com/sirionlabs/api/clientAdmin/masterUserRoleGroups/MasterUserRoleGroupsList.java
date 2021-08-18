package com.sirionlabs.api.clientAdmin.masterUserRoleGroups;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class MasterUserRoleGroupsList extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(MasterUserRoleGroupsList.class);

    public static String getAPIPath() {
        return "/masteruserrolegroups/list";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static String getMasterUserRoleGroupsListResponse() {
        return executor.get(getAPIPath(), getHeaders()).getResponse().getResponseBody();
    }

    public static Integer getUserRoleGroupId(String roleGroupListResponse, String roleGroupName) {
        try {
            logger.info("Getting Id for Role Group {}", roleGroupName);
            Document html = Jsoup.parse(roleGroupListResponse);
            Element div = html.getElementById("l_com_sirionlabs_model_MasterUserRoleGroup").child(1);
            Elements allSubDivs = div.children();

            AdminHelper adminObj = new AdminHelper();

            for (Element subDiv : allSubDivs) {
                String groupName = subDiv.child(1).child(0).childNode(0).toString().replace("\n", "");

                if (groupName.equalsIgnoreCase(roleGroupName)) {
                    String id = subDiv.childNode(0).childNode(0).attributes().toString().split("/")[3].replace("\"","");
                    return Integer.valueOf(id);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Id for Role Group {}. {}", roleGroupName, e.getStackTrace());
        }

        return null;
    }
}