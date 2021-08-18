package com.sirionlabs.api.tabListData;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetTabListData extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(GetTabListData.class);
	private int entityTypeId;
	private int tabId;
	private int entityId;
	private String tabListDataResponse;

	public GetTabListData(int entityTypeId, int tabId, int entityId) {
		this.entityTypeId = entityTypeId;
		this.entityId = entityId;
		this.tabId = tabId;

	}

	public String getTabListDataResponse() {
		return tabListDataResponse;
	}

	public HttpResponse hitGetTabListData() {

		logger.info("Hitting Get Tab List Data for Entity Id : [{}] , EntityType Id :[{}] , tabId :[{}]", entityId, entityTypeId, tabId);

		HttpResponse response = null;
		try {

			// default payload
			String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
			String queryString = "/listRenderer/list/" + tabId + "/tablistdata" + "/" + entityTypeId + "/" + entityId;

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response is : {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Get Tab List Data API : response header {}", headers[i].toString());
			}

			tabListDataResponse = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting GetTabList Data Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitGetTabListData(String payload) {

		HttpResponse response = null;
		try {

			String queryString = "/listRenderer/list/" + tabId + "/tablistdata" + "/" + entityTypeId + "/" + entityId;

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "*/*");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response is : {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Unpin User Preference API : response header {}", headers[i].toString());
			}

			tabListDataResponse = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting GetTabList Data Api. {}", e.getMessage());
		}
		return response;
	}


}
