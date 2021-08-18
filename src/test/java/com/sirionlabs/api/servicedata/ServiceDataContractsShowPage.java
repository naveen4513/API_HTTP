package com.sirionlabs.api.servicedata;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author manoj.upreti
 */
public class ServiceDataContractsShowPage extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ServiceDataContractsShowPage.class);
	String serviceDataContractShowPageResponseStr = null;

	public HttpResponse hitServiceDataContractShowPage(String parentEntityId) throws IOException {
		HttpResponse response = null;
		String queryString = "/contracts/show/" + parentEntityId;
		logger.debug("Query string url formed is {}", queryString);

		HttpGet getRequest = new HttpGet(queryString);
		getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		getRequest.addHeader("Accept-Encoding", "gzip, deflate");
		response = super.getRequest(getRequest);

		serviceDataContractShowPageResponseStr = EntityUtils.toString(response.getEntity());
		logger.debug("The Response is : [ {} ]", serviceDataContractShowPageResponseStr);
		return response;
	}

	public String getServiceDataContractShowPageResponseStr() {
		return serviceDataContractShowPageResponseStr;
	}
}
