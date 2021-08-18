package com.sirionlabs.api.servicedata;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author manoj.upreti
 */

public class ServiceDataUploadBulkAction extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ServiceDataUploadBulkAction.class);

	public String executeMultiPartRequest(String sFileToUpload, String entityTypeId, String parentEntityTypeId, String parentEntityId) throws IOException {
		logger.debug("Executing Multipart Upload Request for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ],  File To Upload : [ {} ] ", entityTypeId, parentEntityTypeId, parentEntityId, sFileToUpload);
		File fileToUpload = new File(sFileToUpload);

		String queryString = "/bulkupload/uploadBulkData/" + entityTypeId + "/1001";
		logger.debug("Query string url formed is {}", queryString);

		//Getting HttpHost
		HttpHost target = generateHttpTargetHost();

		//Getting Post Request
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		String contentTypeHeader = "";
		HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(queryString, acceptsHeader, contentTypeHeader);

		Map<String, String> textBodyMap = new LinkedHashMap<>();
		textBodyMap.put("parentEntityId", parentEntityId);
		textBodyMap.put("parentEntityTypeId", parentEntityTypeId);
		textBodyMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

		HttpEntity entity = multipartEntityBuilder(fileToUpload, textBodyMap);
		httpPostRequest.setEntity(entity);

		//Upload the file to Server
		return uploadFileToServer(target, httpPostRequest);
	}
}
