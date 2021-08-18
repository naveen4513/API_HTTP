package com.sirionlabs.api.download;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadCommunicationDocument extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(DownloadCommunicationDocument.class);

	public Boolean hitDownloadCommunicationDocument(String outputFilePath, String outputFileName, int id, int otherEntityTypeId, int entityTypeId, int fileId) {
		Boolean fileDownloaded = false;
		try {
			HttpHost target = super.generateHttpTargetHost();

			String queryString = "download/communicationdocument?id=" + id + "&entityTypeId=" + otherEntityTypeId + "&entityType.id=" + entityTypeId + "&fileId=" + fileId;
			logger.debug("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";

			HttpGet getRequest = super.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
			fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting Download Communication Document Api. {}", e.getMessage());
		}
		return fileDownloaded;
	}
}
