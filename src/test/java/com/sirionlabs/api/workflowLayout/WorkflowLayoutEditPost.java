package com.sirionlabs.api.workflowLayout;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.EntityDbHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class WorkflowLayoutEditPost extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowLayoutEditPost.class);

    public static String getApiPath() {
        return "/workflowlayout/v1/edit";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    public static String getPayload(int workflowLayoutId, String workflowLayoutGroupName, String editableFieldsShowPage, String editableFieldsEditPage,
                                    String editPageTabs, String showPageTabs, int entityTypeId) {
        String editableFieldsShowPagePayload = (editableFieldsShowPage == null || editableFieldsShowPage.trim().equalsIgnoreCase(""))
                ? null : getSubPayloadForEditableFieldsTabs(editableFieldsShowPage);

        String editableFieldsEditPagePayload = (editableFieldsEditPage == null || editableFieldsEditPage.trim().equalsIgnoreCase(""))
                ? null : getSubPayloadForEditableFieldsTabs(editableFieldsEditPage);

        String editPageTabsPayload = (editPageTabs == null || editPageTabs.trim().equalsIgnoreCase(""))
                ? null : getSubPayloadForTabs(editPageTabs);

        String showPageTabsPayload = (showPageTabs == null || showPageTabs.trim().equalsIgnoreCase(""))
                ? null : getSubPayloadForTabs(showPageTabs);

        String workflowLayoutGroupNamePayload = (workflowLayoutGroupName == null) ? null : "\"" + workflowLayoutGroupName + "\"";

        return "{\"body\": {\"data\": {\"entityTypeReq\": {\"values\": {\"id\":" + entityTypeId + "}},\"editableFieldsShowPage\": {\"values\": " +
                editableFieldsShowPagePayload + "},\"editableFieldsEditPage\": {\"values\": " + editableFieldsEditPagePayload +
                "},\"editPageTabs\": {\"values\": " + editPageTabsPayload + "},\"showPageTabs\": {\"values\": " + showPageTabsPayload +
                "},\"workflowLayoutGroup\":{\"values\": " + workflowLayoutGroupNamePayload + "}, \"id\": { \"name\": \"id\", \"values\": " + workflowLayoutId +
                ", \"multiEntitySupport\": false }}}}";
    }

    public static APIResponse getEditPostResponse(String apiPath, HashMap<String, String> headers, String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(apiPath, headers, payload).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    private static String getSubPayloadForEditableFieldsTabs(String fieldsValue) {
        String subPayload = "[";

        try {
            String[] allValues = fieldsValue.split(",");

            for (String value : allValues) {
                int fieldId = Integer.parseInt(value.trim());
                String fieldAliasName = EntityDbHelper.getEntityAliasName(fieldId);

                if (fieldAliasName == null) {
                    logger.error("Couldn't get Alias Name for Field Id {} from DB.", fieldId);
                    return null;
                }

                subPayload = subPayload.concat("{\"id\": \"" + fieldId + "\", \"name\":\"" + fieldAliasName + "\"},");
            }

            subPayload = subPayload.substring(0, subPayload.length() - 1).concat("]");
        } catch (Exception e) {
            logger.error("Exception while Getting SubPayload from Editable Fields Value: [{}]. {}", fieldsValue, e.getMessage());
        }

        return subPayload;
    }

    private static String getSubPayloadForTabs(String tabsValue) {
        String subPayload = "[";

        try {
            String[] allValues = tabsValue.split(",");

            for (String value : allValues) {
                int tabId = Integer.parseInt(value.trim());
                String tabLabel = EntityDbHelper.getEntityTabLabel(tabId);

                if (tabLabel == null) {
                    logger.error("Couldn't get Tab Label for Tab Id {} from DB.", tabId);
                    return null;
                }

                subPayload = subPayload.concat("{\"id\": \"" + tabId + "\", \"name\":\"" + tabLabel + "\"},");
            }

            subPayload = subPayload.substring(0, subPayload.length() - 1).concat("]");
        } catch (Exception e) {
            logger.error("Exception while Getting SubPayload from Tabs Value: [{}]. {}", tabsValue, e.getMessage());
        }

        return subPayload;
    }
}