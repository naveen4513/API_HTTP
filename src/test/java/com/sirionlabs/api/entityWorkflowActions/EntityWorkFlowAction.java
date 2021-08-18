package com.sirionlabs.api.entityWorkflowActions;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityWorkFlowAction extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(EntityWorkFlowAction.class);


	private String responseJsonStr = null;

	public String getResponseJsonStr() {
		return responseJsonStr;
	}


	public String hitWorkFlowAction(String entityName, String payload, String workflowAction) throws Exception {
		String entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
		String entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		String entityUriName = ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName, "url_name");

		return hitWorkFlowAction(entityName, entityUriName, payload, workflowAction);
	}

	public String hitWorkFlowAction(String entityName, String uriName, String payload, String workflowAction) throws Exception {
		HttpResponse response = null;
		//String workflowAction = "activate";
		try {
			String workFlowIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("WorkFlowIdConfigFilePath");
			String workFlowIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("WorkFlowIdMappingFile");
			String environmentName = ConfigureEnvironment.environment;
			//workFlowIdMappingConfigFileName += "_" + environmentName + ".cfg"; // Since worflowActionDepends on Individual environment we will have environment specific for this

			workFlowIdMappingConfigFileName += ".cfg"; // Commented Above Because of New Structure of Config File

			try {
				String workFlowId = ParseConfigFile.getValueFromConfigFile(workFlowIdMappingConfigFilePath, workFlowIdMappingConfigFileName, workflowAction, entityName);
				logger.info("Workflow Id is : [{}]",workFlowId);


				HttpPost postRequest;

				if (uriName != null) {
					String queryString = "/" + uriName + "/" + workFlowId;
					logger.debug("Query string url formed is {}", queryString);
					postRequest = new HttpPost(queryString);
					postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
					postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
					postRequest.addHeader("Accept-Encoding", "gzip, deflate");
					response = super.postRequest(postRequest, payload);
					logger.debug("Response status is {}", response.getStatusLine().toString());
					this.responseJsonStr = EntityUtils.toString(response.getEntity());

					Header[] headers = response.getAllHeaders();
					for (Header oneHeader : headers) {
						logger.debug("Workflow Actions response header {}", oneHeader.toString());
					}
				} else {
					logger.error("Couldn't get URI Name for Entity {}. Hence not hitting workflow action : {}", entityName, workflowAction);
				}
			} catch (Exception e) {
				logger.error("Exception while hitting work flow action : {}  for Entity {}. {}", workflowAction, entityName, e.getStackTrace());
			}
			return responseJsonStr;
		} catch (Exception e) {

			logger.error("Exception while hitting work flow action : {}  for Entity {}. {}", workflowAction, entityName, e.getStackTrace());
			return "{}";

		}
	}


}
