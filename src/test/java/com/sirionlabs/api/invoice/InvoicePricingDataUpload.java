package com.sirionlabs.api.invoice;

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
 * <h2>Invoice.xml Pricing Data Bulk Upload</h2>
 *
 * @author manoj.upreti
 * @implNote This class is to upload the pricing Data xls file
 */
public class InvoicePricingDataUpload extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(InvoicePricingDataUpload.class);

	/**
	 * @param parentEntityTypeId this is the ID of Contract entity
	 * @param parentEntityId     this is ID of the sub contract to be edited
	 * @param entityTypeId       this is the ID if Service Data Entity
	 * @param sFileToUpload      this is the location of file need to be uploaded
	 * @param serviceDataCCR     this is the change request ID , which need to be selected while uploading the file
	 * @return Upload Response String
	 * @throws IOException
	 * @apiNote <b>This method will upload the pricing data xls file based on the parameters provided</b>
	 */
	public String uploadPricingData(String parentEntityTypeId, String parentEntityId, String entityTypeId, String sFileToUpload, String serviceDataCCR) throws IOException {
		logger.info("Started Upload pricing Data for parentEntityTypeId : [ {} ] , parentEntityId : [ {} ] , entityTypeId : [ {} ], and File Location will be : [ {} ]", parentEntityTypeId, parentEntityId, entityTypeId, sFileToUpload);

		File fileToUpload = new File(sFileToUpload);

		//Generate HttpHost using API Utils
		HttpHost target = generateHttpTargetHost();

		String resourceUri = "bulkupload/uploadBulkData/" + entityTypeId + "/1010";
		logger.debug("Query string url formed is {}", resourceUri);

		//Generate Post Request
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		String contentTypeHeader = "";
		HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(resourceUri, acceptsHeader, contentTypeHeader);

		Map<String, String> textBodyMap = new LinkedHashMap<>();
		if (!parentEntityId.trim().equalsIgnoreCase(""))
			textBodyMap.put("parentEntityId", parentEntityId);
		if (!parentEntityTypeId.trim().equalsIgnoreCase(""))
			textBodyMap.put("parentEntityTypeId", parentEntityTypeId);
		if (!serviceDataCCR.trim().equalsIgnoreCase(""))
			textBodyMap.put("serviceDataCCR", serviceDataCCR);

		textBodyMap.put("entityTypeId", entityTypeId);
		textBodyMap.put("upload", "submit");
		textBodyMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

		HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, textBodyMap);
		httpPostRequest.setEntity(entity);

		return uploadFileToServer(target, httpPostRequest);
	}

	public String uploadPricingData(String entityTypeId, String sFileToUpload) throws IOException {
		return uploadPricingData("", "", entityTypeId, sFileToUpload, "");
	}
}
