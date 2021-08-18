package com.sirionlabs.api.servicedata;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author manoj.upreti
 */
public class ServiceDataTemplateDownload extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ServiceDataTemplateDownload.class);

	private String serviceDataTemplateConstant = "1001";


	public HttpResponse downloadServiceDataTemplateFile(String sOutputFile, String parentEntityTypeId, String parentEntityId) throws Exception {
		logger.info("Started Download XLS Template for entity ID : [ {} ] , parentEntityTypeId : [ {} ] , and download File Location will be : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);

		//Generate HTTP Target Host
		HttpHost target = generateHttpTargetHost();

		String queryString = "/bulkupload/download/" + serviceDataTemplateConstant + "/" + parentEntityTypeId + "/" + parentEntityId;
		logger.debug("Query string url formed is {}", queryString);

		//Generate HTTP get Request based on query String
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		HttpGet getRequest = generateHttpGetRequestWithQueryString(queryString, acceptsHeader);

		return downloadAPIResponseFile(sOutputFile, target, getRequest);
	}

}
