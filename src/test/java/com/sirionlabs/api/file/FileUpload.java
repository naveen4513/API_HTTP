package com.sirionlabs.api.file;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class FileUpload extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(FileUpload.class);

	public String hitFileUpload(String filePath, String fileName, Map<String, String> payloadMap) {
		String uploadResponse = null;

		try {
			String queryString = "/file/upload";
			logger.debug("Query string url formed is {}", queryString);

			String acceptHeader = "application/json, text/plain, */*";

			HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");
			File fileToUpload = new File(filePath + "/" + fileName);
			HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", fileToUpload, payloadMap);
			postRequest.setEntity(entity);

			HttpHost target = generateHttpTargetHost();
			uploadResponse = uploadFileToServer(target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting File Upload Api. {}", e.getMessage());
		}
		return uploadResponse;
	}

	public String hitMultipartFileUpload(String hostname, int port, String scheme, String queryPath,
										 String filePath, String fileName, Map<String, String> payloadMap) {
		String uploadResponse = null;

		try {
			String queryString = queryPath;
			logger.debug("Query string url formed is {}", queryString);

			String acceptHeader = "application/json, text/plain, */*";

			HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");
			File fileToUpload = new File(filePath + "/" + fileName);
			HttpEntity entity = APIUtils.createMultipartEntityBuilder("multipartFile", fileToUpload, payloadMap);
			postRequest.setEntity(entity);

			HttpHost target = new HttpHost(hostname, port, scheme);
			uploadResponse = uploadFileToServer(hostname,port,target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting File Upload Api. {}", e.getMessage());
		}
		return uploadResponse;
	}


}
