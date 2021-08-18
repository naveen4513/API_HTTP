package com.sirionlabs.helper;

import com.sirionlabs.api.file.FileUpload;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DocumentHelper {

	private final static Logger logger = LoggerFactory.getLogger(DocumentHelper.class);

	public static String uploadDocumentFile(String filePath, String fileName) {
		return uploadDocumentFile(filePath, fileName, null);
	}

	public static String uploadDocumentFile(String filePath, String fileName, String randomKeyForDocumentFile) {
		String uploadResponse;

		try {
			String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(fileName);
			String fileExtension = FileUtils.getFileExtension(fileName);

			if (randomKeyForDocumentFile == null)
				randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);

			Map<String, String> payloadMap = new HashMap<>();
			payloadMap.put("name", fileNameWithoutExtension);
			payloadMap.put("extension", fileExtension);
			payloadMap.put("key", randomKeyForDocumentFile);

			FileUpload uploadObj = new FileUpload();
			uploadResponse = uploadObj.hitFileUpload(filePath, fileName, payloadMap);
		} catch (Exception e) {
			logger.error("Exception while Uploading Document File [{}]. {}", filePath + "/" + fileName, e.getStackTrace());
			return null;
		}
		return uploadResponse;
	}

	public static String uploadDRSFile(String hostName, int port, String scheme, String queryPath,
									   String filePath, String fileName, 	Map<String, String> payloadMap) {
		String uploadResponse;

		try {
			FileUpload uploadObj = new FileUpload();
			uploadResponse = uploadObj.hitMultipartFileUpload(hostName,port,scheme,queryPath,filePath, fileName, payloadMap);
		} catch (Exception e) {
			logger.error("Exception while Uploading Document File [{}]. {}", filePath + "/" + fileName, e.getStackTrace());
			return null;
		}
		return uploadResponse;
	}
}
