package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetEntityId extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(GetEntityId.class);

	public String hitGetEntityId(String entityName, String shortCodeId) throws Exception {
		String responseStr = null;
		try {
			HttpGet getRequest;
			String searchUrl = ConfigureConstantFields.getSearchUrlForEntity(entityName);
			String shortCode = ConfigureConstantFields.getShortCodeForEntity(entityName);

			if (shortCodeId.trim().toLowerCase().contains(shortCode.trim().toLowerCase()))
				shortCodeId = shortCodeId.replace(shortCode, "");

			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

			String queryString = "/" + searchUrl + "/getEntityId/" + shortCodeId + "?entityTypeId=" + entityTypeId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "text/html, */*; q=0.01");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = APIUtils.getRequest(getRequest, false);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Get Entity Id response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting GetEntityId Api. {}", e.getMessage());
		}
		return responseStr;
	}
}
