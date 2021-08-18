package com.sirionlabs.api.invoice;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author manoj.upreti
 * @implNote <b>This class is to hit tablist Data API for Invoice.xml service Data</b>
 * @apiNote "/listRenderer/list/309/tablistdata/64"
 */
public class InvoiceTabListData extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(InvoiceTabListData.class);
	String tablistDataResponseJsonStr;


	/**
	 * <b>This method is to hit the Invoice.xml Tablist Data API based of the provided service data ID</b>
	 *
	 * @param entityTypeId  this is the ID if Service Data Entity
	 * @param serviceDataId is the ID of Invoice.xml Service Data created
	 * @return HttpResponse
	 * @throws Exception
	 * @apiNote "/listRenderer/list/309/tablistdata/64/"+serviceDataId
	 */
	public HttpResponse hitInvoiceTablistDataAPI(String entityTypeId, String serviceDataId) throws Exception {
		logger.debug("Hitting Invoice.xml TablistData for entityTypeId : [ {} ], and serviceDataId : [ {} ]", entityTypeId, serviceDataId);

		String queryString = "/listRenderer/list/309/tablistdata/" + entityTypeId + "/" + serviceDataId;
		logger.debug("Query string url formed is {}", queryString);

		//Generate Http Post Request
		String acceptsHeader = "application/json, text/javascript, */*; q=0.01";
		String contentTypeHeader = "application/json;charset=UTF-8";
		HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(queryString, acceptsHeader, contentTypeHeader);

		String tablistDataPayload = generatePayloadForChargesTab();

		//Call the post Request from API Util
		HttpResponse httpResponse = super.postRequest(httpPostRequest, tablistDataPayload);
		logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
		this.tablistDataResponseJsonStr = EntityUtils.toString(httpResponse.getEntity());
		logger.debug("response json is: {}", tablistDataResponseJsonStr);

		Header[] headers = httpResponse.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Invoice.xml Tab List Data response header {}", headers[i].toString());
		}
		logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
		return httpResponse;
	}

	/**
	 * @return Invoice.xml Tab list Data API Response as String
	 */
	public String getTablistDataResponseJsonStr() {
		return tablistDataResponseJsonStr;
	}

	/**
	 * <b>This method is to generate the payload used by invoice tab list data API</b>
	 *
	 * @return String payload Json
	 */
	private String generatePayloadForChargesTab() {
		logger.debug("Generating the Payload for Invoice.xml TabList data API");
		int offset = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("offset"));
		int size = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("pagesize"));
		String orderByColumnName = ConfigureConstantFields.getConstantFieldsProperty("orderby");
		String orderDirection = ConfigureConstantFields.getConstantFieldsProperty("orderdirection");

		JSONObject payload = new JSONObject();

		JSONObject filterMap = new JSONObject();
		filterMap.put("entityTypeId", JSONObject.NULL);
		filterMap.put("offset", offset);
		filterMap.put("size", size);
		filterMap.put("orderByColumnName", orderByColumnName);
		filterMap.put("orderDirection", orderDirection);

		JSONObject filterJson = new JSONObject();
		filterMap.put("filterJson", filterJson);

		payload.put("filterMap", filterMap);

		logger.debug("The Payload is generated for tabListData API, returning : [ {} ]", payload);
		return payload.toString();
	}
}
