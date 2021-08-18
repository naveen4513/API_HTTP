package com.sirionlabs.api.WorkflowLibraryAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FetchWorkflowDetailsAPI {
    private static final String FetchWorkflowDetailsAPIFilePath;
    private static final String FetchWorkflowDetailsAPIFileName;
    private static final String CreateWorkflowDetailsAPIFilePath;
    private static final String CreateWorkflowDetailsAPIFileName;
    private static final String DeleteWorkflowDetailsAPIFilePath;
    private static final String DeleteWorkflowDetailsAPIFileName;
    private static final String UpdateWorkflowAPIFilePath;
    private static final String UpdateWorkflowAPIFileName;

    private static Map<String, String> map;

    static {
        FetchWorkflowDetailsAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("FetchWorkflowDetailsAPIFilePath");
        FetchWorkflowDetailsAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("FetchWorkflowDetailsAPIFileName");
        CreateWorkflowDetailsAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateWorkflowDetailsAPIFilePath");
        CreateWorkflowDetailsAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateWorkflowDetailsAPIFileName");
        DeleteWorkflowDetailsAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("DeleteWorkflowDetailsAPIFilePath");
        DeleteWorkflowDetailsAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("DeleteWorkflowDetailsAPIFileName");
        UpdateWorkflowAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("UpdateWorkflowAPIFilePath");
        UpdateWorkflowAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("UpdateWorkflowAPIFileName");
    }

    public APIValidator hitFetchWorkflowDataAPICall(APIExecutor executor, String domain, String workflowId) {
        HashMap<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-workflow/fetch/" + workflowId;
        return executor.get(domain, queryString, headers);
    }

    public APIValidator hitPostCreateWorkflowAPICall(APIExecutor executor, String domain, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-workflow/save-entity";
        return executor.post(domain, queryString, headers, payload, null);
    }

    public APIValidator hitDeleteWorkflowAPICall(APIExecutor executor, String domain, String workflowIdToBeDeleted) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-workflow/delete/" + workflowIdToBeDeleted;
        return executor.post(domain, queryString, headers, null, null);
    }

    public APIValidator hitUpdateWorkflowAPICall(APIExecutor executor, String domain, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-workflow/update-entity";
        return executor.post(domain, queryString, headers, payload, null);
    }

    public String createRequestBodyForCreateWorkflowAPI(int numberOfNodes, String workflowName) {
        String payload;
        String node = "";

        FetchWorkflowDetailsAPI fw = new FetchWorkflowDetailsAPI();
        boolean isEndOfArray = false;

        for (int i = 0; i < numberOfNodes; i++) {
            if (i == numberOfNodes - 1) {
                isEndOfArray = true;
            }
            node = node + fw.createWorkflowNodes(i + 1, isEndOfArray);
        }
        payload = fw.getPayloadCreateWorkflow(node, workflowName);
        return payload;
    }

    public String createWorkflowNodes(int nodeIndex, boolean isEndOfArray) {

        int nextNodeId;
        int nodeId = nodeIndex;
        if (isEndOfArray) {
            nextNodeId = 9;
        } else {
            nextNodeId = nodeIndex + 1;
        }

        String str =
                "{\n" +
                        "\"id\":" + nodeId + ",\n" +
                        "\"metadata\":{\n" +
                        "\"data\":{\n" +
                        "\"label\":\"Node " + nodeIndex + "\",\n" +
                        "\"nodeType\":{\n" +
                        "\"id\":1,\n" +
                        "\"name\":\"General Node\"\n" +
                        "},\n" +
                        "\"color\":{\n" +
                        "\"border\":\" #007ACC\",\n" +
                        "\"background\":\"#ffffff\",\n" +
                        "\"highlight\":\"#454545\"\n" +
                        "},\n" +
                        "\"shape\":\"box\",\n" +
                        "\"id\":" + nodeId + ",\n" +
                        "\"parentId\":" + nodeId + ",\n" +
                        "\"x\":120,\n" +
                        "\"y\":180\n" +
                        "},\n" +
                        "\"apiNodeId\":" + nodeId + "\n" +
                        "},\n" +
                        "\"name\":\"Node " + nodeIndex + "\",\n" +
                        "\"start\":false,\n" +
                        "\"end\":false,\n" +
                        "\"nodeLinks\":[\n" +
                        "{\n" +
                        "\"id\":null,\n" +
                        "\"apiNodeId\":" + nodeId + ",\n" +
                        "\"apiNextNodeId\":" + nextNodeId + ",\n" +
                        "\"rule\":null\n" +
                        "}\n" +
                        "],\n" +
                        "\"nodeType\":{\n" +
                        "\"id\":1,\n" +
                        "\"name\":\"General Node\"\n" +
                        "},\n" +
                        "\"generalChildTasks\":[\n" +
                        "],\n" +
                        "\"editChildTasks\":[\n" +
                        "],\n" +
                        "\"generalTask\":{\n" +
                        "\"id\":75,\n" +
                        "\"name\":\"General Task 1\",\n" +
                        "\"taskType\":null\n" +
                        "}\n" +
                        "},";

        return str;
    }

    public String getPayloadCreateWorkflow(String workflowNodes, String workflowName) {

        return "{\n" +
                "\"entityType\":{\n" +
                "\"name\":\"Vendor Hierarchy\",\n" +
                "\"id\":3\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + workflowName + "\",\n" +
                "\"description\":null,\n" +
                "\"wfNodes\":[\n" +
                "{\n" +
                "\"id\":800,\n" +
                "\"metadata\":{\n" +
                "\"data\":{\n" +
                "\"label\":\"Start Node\\n 1\",\n" +
                "\"nodeType\":{\n" +
                "\"id\":1,\n" +
                "\"name\":\"Start Node\"\n" +
                "},\n" +
                "\"color\":{\n" +
                "\"border\":\"#00C8A4\",\n" +
                "\"background\":\"#ffffff\"\n" +
                "},\n" +
                "\"shape\":\"circle\",\n" +
                "\"id\":800,\n" +
                "\"parentId\":800,\n" +
                "\"x\":120,\n" +
                "\"y\":180\n" +
                "},\n" +
                "\"apiNodeId\":800\n" +
                "},\n" +
                "\"name\":\"Start Node 1\",\n" +
                "\"start\":true,\n" +
                "\"end\":false,\n" +
                "\"nodeLinks\":[\n" +
                "{\n" +
                "\"id\":null,\n" +
                "\"apiNodeId\":800,\n" +
                "\"apiNextNodeId\":1,\n" +
                "\"rule\":null\n" +
                "}\n" +
                "],\n" +
                "\"nodeType\":{\n" +
                "\"id\":1,\n" +
                "\"name\":\"General Node\"\n" +
                "},\n" +
                "\"generalChildTasks\":[\n" +
                "],\n" +
                "\"editChildTasks\":[\n" +
                "],\n" +
                "\"generalTask\":{\n" +
                "\"id\":75,\n" +
                "\"name\":\"General Task 1\",\n" +
                "\"taskType\":null\n" +
                "}\n" +
                "},\n" +
                workflowNodes +
                "{\n" +
                "\"id\":9,\n" +
                "\"metadata\":{\n" +
                "\"data\":{\n" +
                "\"label\":\"End Node 3\",\n" +
                "\"nodeType\":{\n" +
                "\"id\":1,\n" +
                "\"name\":\"General Node\"\n" +
                "},\n" +
                "\"color\":{\n" +
                "\"border\":\" #007ACC\",\n" +
                "\"background\":\"#ffffff\",\n" +
                "\"highlight\":\"#454545\"\n" +
                "},\n" +
                "\"shape\":\"box\",\n" +
                "\"id\":9,\n" +
                "\"parentId\":9,\n" +
                "\"x\":120,\n" +
                "\"y\":180\n" +
                "},\n" +
                "\"apiNodeId\":9\n" +
                "},\n" +
                "\"name\":\"End Node 3\",\n" +
                "\"start\":false,\n" +
                "\"end\":true,\n" +
                "\"nodeLinks\":[\n" +
                "],\n" +
                "\"nodeType\":{\n" +
                "\"id\":1,\n" +
                "\"name\":\"General Node\"\n" +
                "},\n" +
                "\"generalChildTasks\":[\n" +
                "],\n" +
                "\"editChildTasks\":[\n" +
                "],\n" +
                "\"generalTask\":{\n" +
                "\"id\":75,\n" +
                "\"name\":\"General Task 1\",\n" +
                "\"taskType\":null\n" +
                "}\n" +
                "}\n" +
                "]\n" +
                "}";
    }

    public String createPayloadForUpdateWorkflow(String payload, String updatedName) {

        String keystoRemove = "},\"success\":true&\"errorMessages\":null,\"entity\":{";
        String[] keysToRemoveArray = keystoRemove.split("&");

        for (String str : keysToRemoveArray) {
            if (payload.contains(str)) {

                String tempWord = str;
                payload = payload.replace(tempWord, "");

            }
        }

        JSONObject jObject = new JSONObject(payload);
        jObject.put("name", updatedName);

        return jObject.toString();
    }

    public static Map<String, String> getAllConfigForGetWorkflowDataAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(FetchWorkflowDetailsAPIFilePath, FetchWorkflowDetailsAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForCreateWorkflowAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateWorkflowDetailsAPIFilePath, CreateWorkflowDetailsAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForDeleteWorkflowAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(DeleteWorkflowDetailsAPIFilePath, DeleteWorkflowDetailsAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForUpdateWorkflowAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(UpdateWorkflowAPIFilePath, UpdateWorkflowAPIFileName, section_name);
        return map;
    }

}
