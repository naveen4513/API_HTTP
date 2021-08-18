package com.sirionlabs.api.bulkupload;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UploadBulkData extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UploadBulkData.class);
	private String uploadBulkDataJsonStr = null;

	public void hitUploadBulkData(int entityTypeId, int templateId, String filePath, String fileName, Map<String, String> payloadMap) {
		try {
			String queryString = "/bulkupload/uploadBulkData/" + entityTypeId + "/" + templateId;
			logger.debug("Query string url formed is {}", queryString);

			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

			File fileToUpload = new File(filePath + "/" + fileName);
			HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
			postRequest.setEntity(entity);

			HttpHost target = generateHttpTargetHost();
			this.uploadBulkDataJsonStr = uploadFileToServer(target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting UploadBulkData Api. {}", e.getMessage());
		}
	}

	public void hitBulkUploadGenerate(String filePath, String fileName, Map<String, String> payloadMap) {
		try {
			String queryString = "/bulkupload/generate";
			logger.debug("Query string url formed is {}", queryString);

			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

			File fileToUpload = new File(filePath + "/" + fileName);
			HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
			postRequest.setEntity(entity);

			HttpHost target = generateHttpTargetHost();
			this.uploadBulkDataJsonStr = uploadFileToServer(target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting Bulk Upload Generate Api. {}", e.getMessage());
		}
	}

	public void hitBulkUploadUpload(String filePath, String fileName, Map<String, String> payloadMap) {
		try {
			String queryString = "/bulkupload/upload";
			logger.debug("Query string url formed is {}", queryString);

			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

			File fileToUpload = new File(filePath + "/" + fileName);
			HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
			postRequest.setEntity(entity);

			HttpHost target = generateHttpTargetHost();
			this.uploadBulkDataJsonStr = uploadFileToServer(target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting Bulk Upload Generate Api. {}", e.getMessage());
		}
	}

	public void hitUploadBulkData(String query, String filePath, String fileName,int parentEntityTypeId,int parentId) {
		try {
			logger.debug("Query string url formed is {}", query);

			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			HttpPost postRequest = generateHttpPostRequestWithQueryString(query, acceptHeader, "");

			File fileToUpload = new File(filePath + "/" + fileName);
			Map<String, String> payloadMap = new HashMap<>();
			payloadMap.put("parentEntityTypeId", Integer.toString(parentEntityTypeId));
			payloadMap.put("parentEntityId", Integer.toString(parentId));
			payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
			payloadMap.put("upload", "submit");
			HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
			postRequest.setEntity(entity);

			HttpHost target = generateHttpTargetHost();
			this.uploadBulkDataJsonStr = uploadFileToServer(target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting UploadBulkData Api. {}", e.getMessage());
		}
	}

	public String getUploadBulkDataJsonStr() {
		return uploadBulkDataJsonStr;
	}
}
