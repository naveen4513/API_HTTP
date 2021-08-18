package com.sirionlabs.api.invoice;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @author manoj.upreti
 * @implNote This class is to implement the Invoice.xml Pricing Template Download API
 */
public class InvoicePricingTemplateDownload extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(InvoicePricingTemplateDownload.class);


	/**
	 * <h1><i>Download Invoice.xml Pricing Template File </i></h1>
	 *
	 * @param sOutputFile        is location where the file to be downloaded
	 * @param parentEntityTypeId this is the ID of Contract entity
	 * @param parentEntityId     this is ID of the sub contract to be edited
	 * @param entityTypeId       this is the ID if Service Data Entity
	 * @param entityIDList       this list will be containing the service Data entry IDs
	 * @return HttpResponse
	 * @throws Exception
	 * @apiNote <b>This method is to download the invoice pricing template according to the paramaters provided , And the API call will be for "/bulkupload/download/1010/" </b>
	 */
	public HttpResponse downloadInvoicePricingTemplateFile(String sOutputFile, String parentEntityTypeId, String parentEntityId, String entityTypeId, List<String> entityIDList) throws Exception {
		logger.info("Started Download XLS Template for entity ID : [ {} ] , parentEntityTypeId : [ {} ] , entityTypeId : [ {} ], and download File Location will be : [ {} ]", parentEntityTypeId, parentEntityId, entityTypeId, sOutputFile);

		//Generate HttpHost using API Utils
		HttpHost target = generateHttpTargetHost();

		String queryString = "/bulkupload/download/1010/";
		logger.debug("Query string url formed is {}", queryString);

		//Generate Http Post Request
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		String contentTypeHeader = "application/x-www-form-urlencoded";
		HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(queryString, acceptsHeader, contentTypeHeader);

		//Generate Form Data Map for Creating Name Value Form Data
		Map<String, String> formDataMap = generateFormDataMap(parentEntityTypeId, parentEntityId, entityTypeId, entityIDList);

		//Generate Name Value Pair Form Data Entity
		HttpEntity nameValuePairFormDataEntity = generateNameValuePairFormDataEntity(formDataMap);

		//Add the nave value pair form data entity with post request
		httpPostRequest.setEntity(nameValuePairFormDataEntity);

		//Call the DownloadFile method
		return downloadAPIResponseFile(sOutputFile, target, httpPostRequest);
	}

	public HttpResponse downloadInvoicePricingTemplateFile(String sOutputFile, List<String> entityIDList) throws Exception {
		return downloadInvoicePricingTemplateFile(sOutputFile, "", "", "", entityIDList);
	}

	/**
	 * @param parentEntityTypeId this is the ID of Contract entity
	 * @param parentEntityId     this is ID of the sub contract to be edited
	 * @param entityTypeId       this is the ID if Service Data Entity
	 * @param entityIDList       this list will be containing the service Data entry IDs
	 * @return Map<String       ,       String>
	 * @throws Exception
	 * @apiNote This method is to create the form Data Map , which is used in pricing template download API
	 */
	private Map<String, String> generateFormDataMap(String parentEntityTypeId, String parentEntityId, String entityTypeId, List<String> entityIDList) {
		Map<String, String> formDataMap = new LinkedHashMap<>();
		formDataMap.put("parentEntityId", parentEntityId);
		formDataMap.put("parentEntityTypeId", parentEntityTypeId);
		formDataMap.put("entityTypeId", entityTypeId);
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
}
