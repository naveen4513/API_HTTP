package com.sirionlabs.api.bulkupload;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Download extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Download.class);

	public Boolean hitDownload(String outputFilePath, String outputFileName, int templateId, int parentEntityTypeId, int parentId) {
		String queryString = "/bulkupload/download/" + templateId + "/" + parentEntityTypeId + "/" + parentId;
		return hitDownload(outputFilePath, outputFileName, queryString);
	}

	public HttpResponse hitDownload(int templateId, int parentEntityTypeId, int parentId) {
		String queryString = "/bulkupload/download/" + templateId + "/" + parentEntityTypeId + "/" + parentId;
		return hitDownload(queryString);
	}

	public Boolean hitDownload(String outputFilePath, String outputFileName, String queryString) {
		Boolean fileDownloaded = false;
		try {
			HttpHost target = super.generateHttpTargetHost();
			logger.debug("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";

			HttpGet getRequest = super.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
			fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting BulkUpload Download Api using QueryString [{}]. {}", queryString, e.getStackTrace());
		}
		return fileDownloaded;
	}

	public HttpResponse hitDownload(String queryString) {
		HttpResponse response = null;

		try {
			logger.info("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";

			HttpGet getRequest = super.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
			response = APIUtils.getRequestForDownloadFile(getRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting BulkUpload Download Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitDownload(String outputFile, int templateId, int entityTypeId, String entityIds) {
		HttpResponse response = null;

		try {
			HttpHost target = super.generateHttpTargetHost();
			String queryString = "/bulkupload/download/" + templateId + "?entityTypeId=" + entityTypeId;
			logger.info("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			String contentTypeHeader = "application/x-www-form-urlencoded";
			String payload = "entityIds=" + entityIds;

			HttpPost postRequest = super.generateHttpPostRequestWithQueryStringAndPayload(queryString, acceptHeader, contentTypeHeader, payload);
			response = super.downloadAPIResponseFile(outputFile, target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting BulkUpload Download Api. {}", e.getMessage());
		}
		return response;
	}

	public Boolean hitDownload(String outputFilePath, String outputFileName, int templateId, int entityTypeId, String entityIds) {
		Boolean fileDownloaded = false;
		try {
			HttpHost target = super.generateHttpTargetHost();
			String queryString = "/bulkupload/download/" + templateId + "?entityTypeId=" + entityTypeId;
			logger.debug("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			String contentTypeHeader = "application/x-www-form-urlencoded";
			String payload = "entityIds=" + entityIds;

			HttpPost postRequest = super.generateHttpPostRequestWithQueryStringAndPayload(queryString, acceptHeader, contentTypeHeader, payload);
			fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting BulkUpload Download Api. {}", e.getMessage());
		}
		return fileDownloaded;
	}

	public Boolean hitDownload(String outputFilePath, String outputFileName, int templateId, int entityTypeId,Map<String, String> formParam) {

		Boolean fileDownloaded = false;
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/bulkupload/download/" + templateId + "?entityTypeId=" + entityTypeId;

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			String params = UrlEncodedString.getUrlEncodedString(formParam);
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
			postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = postRequest(postRequest, params);
			logger.debug("Response status is {}", response.getStatusLine().toString());

			fileDownloaded = super.dumpDownloadListWithDataResponseIntoFile(response, outputFilePath,outputFileName);

		} catch (Exception e) {
			logger.error("Exception while hitting DownloadReportListWithData Api. {}", e.getMessage());
		}
		return fileDownloaded;
	}

	public Boolean hitDownload(String outputFilePath, String outputFileName, int templateId, int entityTypeId,int provId, String entityIds) {
		Boolean fileDownloaded = false;
		try {
			HttpHost target = super.generateHttpTargetHost();
			String queryString = "/bulkupload/download/" + templateId + "?entityTypeId=" + entityTypeId + "&provisioningId=" + provId;
			logger.debug("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			String contentTypeHeader = "application/x-www-form-urlencoded";
			String payload = "entityIds=" + entityIds;

			HttpPost postRequest = super.generateHttpPostRequestWithQueryStringAndPayload(queryString, acceptHeader, contentTypeHeader, payload);
			fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, postRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting BulkUpload Download Api. {}", e.getMessage());
		}
		return fileDownloaded;
	}

	public HttpResponse downloadBulkTemplateRawData(String sOutputFile, List<String> entityIDList) throws Exception {

		//Generate HttpHost using API Utils
		HttpHost target = generateHttpTargetHost();

		String queryString = "/slRawData/generateBulkTemplate";
		logger.debug("Query string url formed is {}", queryString);

		//Generate Http Post Request
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		String contentTypeHeader = "application/x-www-form-urlencoded";
		HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(queryString, acceptsHeader, contentTypeHeader);

		//Generate Form Data Map for Creating Name Value Form Data
		Map<String, String> formDataMap = generateFormDataMapRawDataBulkUpload(entityIDList);

		//Generate Name Value Pair Form Data Entity
		HttpEntity nameValuePairFormDataEntity = generateNameValuePairFormDataEntity(formDataMap);

		//Add the nave value pair form data entity with post request
		httpPostRequest.setEntity(nameValuePairFormDataEntity);

		//Call the DownloadFile method
		return downloadAPIResponseFile(sOutputFile, target, httpPostRequest);
	}


	public HttpResponse downloadBulkPerformanceData(String sOutputFile, Map<String,String> payloadMap) throws Exception {

		//Generate HttpHost using API Utils
		HttpHost target = generateHttpTargetHost();

		String queryString = "/bulkupload/generate";
		logger.debug("Query string url formed is {}", queryString);

		//Generate Http Post Request
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		String contentTypeHeader = "application/x-www-form-urlencoded";
		HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(queryString, acceptsHeader, contentTypeHeader);

		//Generate Name Value Pair Form Data Entity
		HttpEntity nameValuePairFormDataEntity = generateNameValuePairFormDataEntity(payloadMap);

		//Add the nave value pair form data entity with post request
		httpPostRequest.setEntity(nameValuePairFormDataEntity);

		//Call the DownloadFile method
		return downloadAPIResponseFile(sOutputFile, target, httpPostRequest);
	}

	private Map<String, String> generateFormDataMapRawDataBulkUpload(List<String> entityIDList) {
		Map<String, String> formDataMap = new LinkedHashMap<>();
		StringBuilder stringBuilder = new StringBuilder();
		boolean firstValue = true;
		for (String entityId : entityIDList) {
			if (firstValue) {
				stringBuilder.append(entityId);
				firstValue = false;
			} else {
				stringBuilder.append(",");
				stringBuilder.append(entityId);
			}
		}

		formDataMap.put("entityIds", stringBuilder.toString());
		formDataMap.put("_csrf_token", "");
		return formDataMap;
	}

	public Boolean hitDocumentDownload(String outputFilePath, String outputFileName, int documentId) {
		Boolean fileDownloaded = false;
		try {
			HttpHost target = super.generateHttpTargetHost();
			String queryString = "/download/contractdocument/" + documentId ;
			logger.debug("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";

			HttpGet getRequest = super.generateHttpGetRequestWithQueryString(queryString, acceptHeader);

			File directory = new File(outputFilePath);
			if (!directory.exists()) {
				directory.mkdirs();

			}
			fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);
		} catch (Exception e) {
			logger.error("Exception while hitting Document Download Api. {}", e.getMessage());
		}
		return fileDownloaded;
	}

	public boolean downloadCommDocument(String outputFilePath,String outputFileName,String queryString){

		Boolean downloadStatus = true;

		try{

			HttpHost target = super.generateHttpTargetHost();
			String apiUrl = "/download/communicationdocument" + queryString ;
			logger.debug("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";

			HttpGet getRequest = super.generateHttpGetRequestWithQueryString(apiUrl, acceptHeader);

			File directory = new File(outputFilePath);
			if (!directory.exists()) {
				directory.mkdirs();

			}
			downloadStatus = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);

		}catch (Exception e){
			logger.error("Exception while downloading contract Document");
		}

		return downloadStatus;

	}

	public Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String outputFileName) {

		Boolean status;

		FileUtils fileUtil = new FileUtils();

		String  outputFile = outputFilePath + "/" + outputFileName;
		status = fileUtil.writeResponseIntoFile(response, outputFile);
		if (status)
			logger.info("DownloadListWithData file generated at {}", outputFile);

		return status;
	}

}