package com.sirionlabs.api.moveToTree;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCRIdsForContract extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(GetCRIdsForContract.class);

	public String hitGetCRIdsForContract(int contractId) {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/moveToTree/getCRIdsForContract/" + contractId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);

			getRequest.addHeader("Accept", "application/json, text/plain, */*");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.getRequest(getRequest, false);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("MoveToTree GetCRIdsForContract response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting MoveToTree GetCRIdsForContract Api. {}", e.getMessage());
		}
		return response;
	}
}