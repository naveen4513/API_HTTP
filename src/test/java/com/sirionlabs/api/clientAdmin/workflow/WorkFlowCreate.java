package com.sirionlabs.api.clientAdmin.workflow;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.test.invoice.TestForecastBulkUpload;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class WorkFlowCreate extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(WorkFlowCreate.class);

    public void hitWorkflowCreate(String workflowName,String relationId,String entityTypeId,String filePath, String fileName) {
        AdminHelper adminHelper=new AdminHelper();

        try {
            adminHelper.loginWithClientAdminUser();

            String queryString = "/workflow/create";

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("name",workflowName);
            payloadMap.put("entityTypeId",entityTypeId);
            payloadMap.put("relationId",relationId);
            payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";
            HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

            File fileToUpload = new File(filePath + "/" + fileName);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting workflow create API. {}", e.getMessage());
        }finally {
            adminHelper.loginWithUser(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        }
    }
}
