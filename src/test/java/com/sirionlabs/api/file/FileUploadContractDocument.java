package com.sirionlabs.api.file;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileUploadContractDocument extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(FileUploadContractDocument.class);

	public String hitUploadContractDocument(String filePath, String fileName, int supplierId, String randomKeyForDocumentFile) {
		String uploadResponse = null;

		try {
			String queryString = "/file/upload/contractDocument";
			logger.debug("Query string url formed is {}", queryString);

			String acceptHeader = "application/json, text/plain, */*";

			String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(fileName);
			String fileExtension = FileUtils.getFileExtension(fileName);

			if (randomKeyForDocumentFile == null) {
				randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);
			}

			Map<String, String> payloadMap = new HashMap<>();
			payloadMap.put("name", fileNameWithoutExtension);
			payloadMap.put("extension", fileExtension);
			payloadMap.put("key", randomKeyForDocumentFile);
			payloadMap.put("relationId", String.valueOf(supplierId));

			HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");
			File fileToUpload = new File(filePath + "/" + fileName);
			HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", fileToUpload, payloadMap);
			postRequest.setEntity(entity);

			HttpHost target = generateHttpTargetHost();
			uploadResponse = uploadFileToServer(target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting File Upload ContractDocument Api. {}", e.getMessage());
		}
		return uploadResponse;
	}
}