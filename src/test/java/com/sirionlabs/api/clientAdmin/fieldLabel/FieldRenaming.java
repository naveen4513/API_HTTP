package com.sirionlabs.api.clientAdmin.fieldLabel;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.DateUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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

public class FieldRenaming extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(FieldRenaming.class);

    public String hitFieldRenamingUpdate(int languageId, int groupId) {
        String response = null;
        try {
            HttpGet getRequest;
            String queryString = "/fieldlabel/findLabelsByGroupIdAndLanguageId/" + languageId + "/" + groupId;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            logger.info("Hitting Field Renaming FindLabelsByGroupIdAndLanguageId for Language Id {} and Group Id {}", languageId, groupId);

            HttpResponse httpResponse = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
            response = EntityUtils.toString(httpResponse.getEntity());

            Header[] headers = httpResponse.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Field renaming response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting field renaming Api. {}", e.getMessage());
        }
        return response;
    }

    public String getFieldRenamingUpdateResponse(int languageId, int groupId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        AdminHelper adminHelperObj = new AdminHelper();
        adminHelperObj.loginWithClientAdminUser();

        String fieldRenamingUpdateResponse = hitFieldRenamingUpdate(languageId, groupId);

        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return fieldRenamingUpdateResponse;
    }

    public String hitFieldUpdateWithClientAdminLogin(String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        AdminHelper adminHelperObj = new AdminHelper();
        adminHelperObj.loginWithClientAdminUser();

        String fieldUpdateResponse = hitFieldUpdate(payload);

        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return fieldUpdateResponse;
    }

    public String hitFieldUpdate(String payload) {
        String response = null;
        try {
            HttpPost postRequest;
            String queryString = "/fieldlabel/update";

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
            response = EntityUtils.toString(httpResponse.getEntity());

            Header[] headers = httpResponse.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Configure Data response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting field update. {}", e.getMessage());
        }
        return response;
    }

    public String getClientFieldNameFromName(String fieldRenamingResponse, String fieldNme) {
        return getClientFieldNameFromName(fieldRenamingResponse, null, fieldNme);
    }

    public String getClientFieldNameFromName(String fieldRenamingResponse, String groupName, String fieldName) {
        try {
            JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
            JSONArray groupArr = jsonObj.getJSONArray("childGroups");

            if (groupName != null) {
                for (int i = 0; i < groupArr.length(); i++) {
                    String actualGroupName = groupArr.getJSONObject(i).getString("name");

                    if (actualGroupName.equalsIgnoreCase(groupName)) {
                        jsonObj = groupArr.getJSONObject(i);
                        break;
                    }
                }
            }

            return getClientFieldName(jsonObj.toString(), fieldName);
        } catch (Exception e) {
            logger.error("Exception while Getting Client Field Name for Name {}. {}", fieldName, e.getMessage());
        }

        return null;
    }

    public String getClientFieldNameFromId(String fieldRenamingResponse, String groupName, int fieldId) {
        try {
            JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
            JSONArray groupArr = jsonObj.getJSONArray("childGroups");

            if (groupName != null) {
                for (int i = 0; i < groupArr.length(); i++) {
                    String actualGroupName = groupArr.getJSONObject(i).getString("name");

                    if (actualGroupName.equalsIgnoreCase(groupName)) {
                        jsonObj = groupArr.getJSONObject(i);
                        break;
                    }
                }
            }

            return getClientFieldNameFromId(jsonObj.toString(), fieldId);
        } catch (Exception e) {
            logger.error("Exception while Getting Client Field Name for Field Id {}. {}", fieldId, e.getMessage());
        }

        return null;
    }

    public String getClientFieldName(String jsonStr, String fieldName) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);

            if (jsonObj.has("childGroups") && jsonObj.getJSONArray("childGroups").length() > 0) {
                JSONArray childGroupsArr = jsonObj.getJSONArray("childGroups");

                for (int i = 0; i < childGroupsArr.length(); i++) {
                    String clientFieldName = getClientFieldName(childGroupsArr.getJSONObject(i).toString(), fieldName);

                    if (clientFieldName != null) {
                        return clientFieldName;
                    }
                }
            }

            JSONArray fieldLabelsArr = jsonObj.getJSONArray("fieldLabels");

            for (int j = 0; j < fieldLabelsArr.length(); j++) {
                String actualFieldName = fieldLabelsArr.getJSONObject(j).getString("name");

                if (actualFieldName.equalsIgnoreCase(fieldName) && !(fieldLabelsArr.getJSONObject(j).getInt("id")==29069)) {
                    return fieldLabelsArr.getJSONObject(j).getString("clientFieldName");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Client Field Name for Field having Name: {}. {}", fieldName, e.getMessage());
        }

        return null;
    }

    public String getClientFieldNameFromId(String jsonStr, int fieldId) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);

            if (jsonObj.has("childGroups") && jsonObj.getJSONArray("childGroups").length() > 0) {
                JSONArray childGroupsArr = jsonObj.getJSONArray("childGroups");

                for (int i = 0; i < childGroupsArr.length(); i++) {
                    String clientFieldName = getClientFieldNameFromId(childGroupsArr.getJSONObject(i).toString(), fieldId);

                    if (clientFieldName != null) {
                        return clientFieldName;
                    }
                }
            }

            JSONArray fieldLabelsArr = jsonObj.getJSONArray("fieldLabels");

            for (int j = 0; j < fieldLabelsArr.length(); j++) {
                int actualFieldId = fieldLabelsArr.getJSONObject(j).getInt("id");

                if (actualFieldId == fieldId) {
                    return fieldLabelsArr.getJSONObject(j).getString("clientFieldName");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Client Field Name for Field having Id: {}. {}", fieldId, e.getMessage());
        }

        return null;
    }

    public String getExpectedFeatureInstructionValueForBulkCreate(String fieldRenamingResponse) {
        try {
            JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("childGroups");

            for (int i = 0; i < jsonArr.length(); i++) {
                String name = jsonArr.getJSONObject(i).getString("name");

                if (name.equalsIgnoreCase("Instructions")) {
                    jsonArr = jsonArr.getJSONObject(i).getJSONArray("fieldLabels");

                    for (int j = 0; j < jsonArr.length(); j++) {
                        int id = jsonArr.getJSONObject(j).getInt("id");

                        if (id == 28432) {
                            return jsonArr.getJSONObject(j).getString("clientFieldName");
                        }
                    }

                    break;
                }
            }

        } catch (Exception e) {
            logger.error("Exception while Getting Expected Feature Instruction Value for Bulk Create. {}", e.getMessage());
        }

        return null;
    }

    public String getMetadataClientFieldNameFromId(String fieldRenamingResponse, int id) {
        try {
            JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("childGroups");

            for (int i = 0; i < jsonArr.length(); i++) {
                String groupName = jsonArr.getJSONObject(i).getString("name");

                if (groupName.equalsIgnoreCase("Metadata")) {
                    jsonArr = jsonArr.getJSONObject(i).getJSONArray("fieldLabels");

                    for (int j = 0; j < jsonArr.length(); j++) {
                        int actualFieldId = jsonArr.getJSONObject(j).getInt("id");

                        if (actualFieldId == id) {
                            return jsonArr.getJSONObject(j).getString("clientFieldName");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Metadata Client Field Name for Field Id {}. {}", id, e.getMessage());
        }

        return null;
    }

    public int getFieldRenamingGroupIdForEntity(String entityName) {
        int groupId = -1;

        switch (entityName) {
            case "contracts":
                groupId = 11;
                break;

            case "service levels":
                groupId = 71;
                break;

            case "child service levels":
                groupId = 81;
                break;

            case "obligations":
                groupId = 51;
                break;

            case "child obligations":
                groupId = 61;
                break;

            case "disputes":
                groupId = 908;
                break;

            case "consumptions":
                groupId = 1374;
                break;
        }

        return groupId;
    }

    public String getFieldAttribute(String fieldRenamingResponse, String fieldName, String groupName, String fieldAttributeName) {
        try {
            JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
            JSONArray groupArr = jsonObj.getJSONArray("childGroups");

            if (groupName != null) {
                for (int i = 0; i < groupArr.length(); i++) {
                    String actualGroupName = groupArr.getJSONObject(i).getString("name");

                    if (actualGroupName.equalsIgnoreCase(groupName)) {
                        jsonObj = groupArr.getJSONObject(i);
                        break;
                    }
                }
            }

            return getAttribute(jsonObj.toString(), fieldName, fieldAttributeName);
        } catch (Exception e) {
            logger.error("Exception while Getting Client Field Name for Name {}. {}", fieldName, e.getMessage());
        }

        return null;
    }

    private String getAttribute(String jsonStr, String fieldName, String fieldAttributeName) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);

            if (jsonObj.has("childGroups") && jsonObj.getJSONArray("childGroups").length() > 0) {
                JSONArray childGroupsArr = jsonObj.getJSONArray("childGroups");

                for (int i = 0; i < childGroupsArr.length(); i++) {
                    String attribute = getAttribute(childGroupsArr.getJSONObject(i).toString(), fieldName, fieldAttributeName);

                    if (attribute != null) {
                        return attribute;
                    }
                }
            }

            JSONArray fieldLabelsArr = jsonObj.getJSONArray("fieldLabels");

            for (int j = 0; j < fieldLabelsArr.length(); j++) {
                String actualFieldName = fieldLabelsArr.getJSONObject(j).getString("name");

                if (actualFieldName.equalsIgnoreCase(fieldName)) {
                    if (fieldLabelsArr.getJSONObject(j).has(fieldAttributeName)) {
                        return fieldLabelsArr.getJSONObject(j).get(fieldAttributeName).toString();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Attribute for Field having Name: {}. {}", fieldAttributeName, e.getMessage());
        }

        return null;
    }


    /**
     *  Added By - Sarthak Garg
     * @param languageId
     * @param groupId
     * @return the string which get appended in the labels
     */
    public String updateallfieldslabel(int languageId,int groupId){

        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        // login from client admin
        AdminHelper adminHelperObj = new AdminHelper();
        adminHelperObj.loginWithClientAdminUser();

        //get fields for languageId and groupId
        String response = getFieldRenamingUpdateResponse(languageId,groupId);

        // get appender in labels having date and time
        DateUtils utils = new DateUtils();
        String date = utils.getCurrentDateInAnyFormat("ddMMyyHHmm");
        logger.info("String appended in labels "+date);

        //replace the label in the payload and hit update api
        String update = response.replaceAll("\"clientFieldName\":\"([a-zA-Z0-9,@.?:/\\-(){}\\[\\] ]+)\"","\"clientFieldName\":\"$1"+date+"\"");
        hitFieldUpdate(update);

        //login again form end-user
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return date;

    }


    public Map<String, String> getAllFieldsOfAGroup(String fieldRenamingResponse, String groupName) {
        JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
        JSONArray groupArr = jsonObj.getJSONArray("childGroups");

        if (groupName != null) {
            for (int i = 0; i < groupArr.length(); i++) {
                String actualGroupName = groupArr.getJSONObject(i).getString("name");

                if (actualGroupName.equalsIgnoreCase(groupName)) {
                    jsonObj = groupArr.getJSONObject(i);
                    break;
                }
            }
        }

        JSONArray fieldsArr = jsonObj.getJSONArray("fieldLabels");
        Map<String, String> allFieldsMap = new HashMap<>();

        for (int j = 0; j < fieldsArr.length(); j++) {
            allFieldsMap.put(String.valueOf(fieldsArr.getJSONObject(j).getInt("id")), fieldsArr.getJSONObject(j).getString("clientFieldName"));
        }

        return allFieldsMap;
    }
}
