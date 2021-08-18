package com.sirionlabs.api.invoice;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 2/4/18.
 */
public class InvoiceLineItemsCreationTemplateDownload extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(InvoiceLineItemsCreationTemplateDownload.class);

	private String invoiceLineItemsCreationTemplateConstant = "1004";


	public HttpResponse downloadInvoiceLineItemsCreationTemplateFile(String sOutputFile, String parentEntityTypeId, String parentEntityId) throws Exception {
		logger.info("Started Download XLS Template for invoice line items creation entity ID : [ {} ] , parentEntityTypeId : [ {} ] , and download File Location will be : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);

		//Generate HTTP Target Host
		HttpHost target = generateHttpTargetHost();

		String queryString = "/bulkupload/download/" + invoiceLineItemsCreationTemplateConstant + "/" + parentEntityTypeId + "/" + parentEntityId;
		logger.debug("Query string url formed is {}", queryString);

		//Generate HTTP get Request based on query String
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		HttpGet getRequest = generateHttpGetRequestWithQueryString(queryString, acceptsHeader);

		return downloadAPIResponseFile(sOutputFile, target, getRequest);
	}
}
