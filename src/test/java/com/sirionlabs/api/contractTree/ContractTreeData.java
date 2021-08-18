package com.sirionlabs.api.contractTree;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by shivashish on 2/8/17.
 */
public class ContractTreeData extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(ContractTreeData.class);
	String responseContractTreeData;
	String responseDocumentViewerData;
	String responseDocumentDownloadData;
	String responseDocumentReplaceData;
	Set<Integer> hashSetofDocumentIds = null;
	String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
	String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

	public String getResponseDocumentDownloadData() {
		return responseDocumentDownloadData;
	}

	public String getResponseDocumentReplaceData() {
		return responseDocumentReplaceData;
	}

	public String getResponseDocumentViewerData() {
		return responseDocumentViewerData;
	}


	public String getResponseContractTreeData() {
		return responseContractTreeData;
	}

	public Set<Integer> getHashSetofDocumentIds() {
		return hashSetofDocumentIds;
	}

	// this will return Set of all the Document Id present in Contract Tree API Response
	public void setHashSetofDocumentIds() {

		hashSetofDocumentIds = new HashSet<Integer>();

		JSONObject dataJson = new JSONObject(responseContractTreeData).getJSONObject("body").getJSONObject("data");
		JSONArray childrenArray = dataJson.getJSONArray("children");

		if (childrenArray != null && childrenArray.length() > 0) {

			for (int i = 0; i < childrenArray.length(); i++) {

				JSONObject childNod = (JSONObject) childrenArray.get(i);

				if (childNod.has("extension") && childNod.get("extension") != null && !childNod.get("extension").toString().contentEquals("null")) {
					hashSetofDocumentIds.add(Integer.parseInt(childNod.get("entityId").toString()));
				}
			}
		}


	}

	public HttpResponse hitContractTreeDataListAPI(int entityURLId, int contractDbId, HashMap<String, String> queryStringParams) throws Exception {

		HttpResponse response;
		String queryString = "/contract-tree/" + entityURLId + "/" + contractDbId;

		if (queryStringParams != null) {
			String urlParams = UrlEncodedString.getUrlEncodedString(queryStringParams);
			queryString += "?" + urlParams;
		}
		logger.info("Query string url formed is {}", queryString);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");

		response = APIUtils.postRequest(postRequest, "{}");
		logger.debug("Response is : {}", response.getStatusLine().toString());
		responseContractTreeData = EntityUtils.toString(response.getEntity());

		logger.info("API Response is  [ {} ]", responseContractTreeData);

		return response;
	}

	public HttpResponse hitContractTreeDataListAPI(int entityURLId, int contractDbId) throws Exception {
		return hitContractTreeDataListAPI(entityURLId, contractDbId, null);
	}


	// this function will hit the Contract Tree API
	// Taking 2 Parameter
	// first one in entityURLId and Second One is contractDbId
	public HttpResponse hitDocumentViewerAPI(int contractDbId) throws Exception {

		String documentViewAPIUri = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, "document_view");
		String queryString = documentViewAPIUri + contractDbId;
		logger.info("Query string url formed is {}", queryString);

		HttpResponse response;
		HttpGet getRequest = new HttpGet(queryString);
		getRequest.addHeader("Accept", "*/*");

		response = super.getRequest(getRequest);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Document Viewer Data API: response header {}", headers[i].toString());
		}

		responseDocumentViewerData = EntityUtils.toString(response.getEntity());

		logger.info("API Response is  [ {} ]", responseDocumentViewerData);

		return response;
	}

	public HttpResponse hitDocumentDownloadAPI(int contractDbId) throws Exception {

		String documentDownloadAPIUri = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, "document_download");
		String queryString = documentDownloadAPIUri + contractDbId;
		logger.info("Query string url formed is {}", queryString);

		HttpResponse response;
		HttpGet getRequest = new HttpGet(queryString);
		getRequest.addHeader("Accept", "*/*");

		response = super.getRequest(getRequest);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Document Download Data API: response header {}", headers[i].toString());
		}
		responseDocumentDownloadData = EntityUtils.toString(response.getEntity());


		return response;
	}

	public HttpResponse hitDocumentReplaceAPI(String payload) throws Exception {

		String documentReplaceAPIUri = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, "document_rename");
		String queryString = documentReplaceAPIUri;
		logger.info("Query string url formed is {}", queryString);

		HttpResponse response;

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		postRequest.addHeader("Accept-Encoding", "gzip, deflate");

		response = super.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Contract Tree Data API: response header {}", headers[i].toString());
		}
		responseDocumentReplaceData = EntityUtils.toString(response.getEntity());

		logger.info("API Response is  [ {} ]", responseDocumentReplaceData);

		return response;
	}

	public String hitContractTreeListAPIV1(int entityTypeId, int recordId) {
		return hitContractTreeListAPIV1(entityTypeId, recordId, "{}");
	}

	public String hitContractTreeListAPIV1(int entityTypeId, int recordId, String payload) {
		return hitContractTreeListAPIV1(entityTypeId, recordId, payload, null);
	}

	public String hitContractTreeListAPIV1(int entityTypeId, int recordId, String payload, Map<String, String> queryStringParams) {
		String response = null;

		try {
			String queryString = "/contract-tree/v1/" + entityTypeId + "/" + recordId;

			if (queryStringParams == null) {
				queryStringParams = new HashMap<>();
				queryStringParams.put("hierarchy", "false");
				queryStringParams.put("offset", "0");
			}

			String urlParams = UrlEncodedString.getUrlEncodedString(queryStringParams);
			queryString += "?" + urlParams;

			logger.info("Query string url formed is {}", queryString);

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			response = EntityUtils.toString(httpResponse.getEntity());
		} catch (Exception e) {
			logger.error("Exception while Hitting Contract Tree V1 API. {}", e.getMessage());
		}

		return response;
	}

	public List<Map<String, String>> getAllChildrenMapIncludingParent(String contractTreeResponse) {
		List<Map<String, String>> allChildrenMap = new ArrayList<>();

		try {
			JSONObject jsonObj = new JSONObject(contractTreeResponse).getJSONObject("body").getJSONObject("data");
			Map<String, String> childrenMap = new HashMap<>();
			childrenMap.put("text", jsonObj.getString("text"));
			childrenMap.put("entityTypeId", String.valueOf(jsonObj.getInt("entityTypeId")));
			childrenMap.put("entityId", String.valueOf(jsonObj.getInt("entityId")));
			childrenMap.put("numberOfChild", String.valueOf(jsonObj.getInt("numberOfChild")));
			childrenMap.put("lightColor", String.valueOf(jsonObj.getBoolean("lightColor")));

			allChildrenMap.add(childrenMap);

			JSONArray jsonArr = jsonObj.getJSONArray("children");

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = jsonArr.getJSONObject(i);

				childrenMap = new HashMap<>();
				childrenMap.put("text", jsonObj.getString("text"));
				childrenMap.put("entityTypeId", String.valueOf(jsonObj.getInt("entityTypeId")));
				childrenMap.put("entityId", String.valueOf(jsonObj.getInt("entityId")));
				childrenMap.put("numberOfChild", String.valueOf(jsonObj.getInt("numberOfChild")));
				childrenMap.put("lightColor", String.valueOf(jsonObj.getBoolean("lightColor")));

				allChildrenMap.add(childrenMap);
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Children Map. {}" + e.getMessage());
			return null;
		}

		return allChildrenMap;
	}

	public List<Map<String, String>> getAllChildrenMap(String contractTreeResponse) {
		List<Map<String, String>> allChildrenMap = new ArrayList<>();

		try {
			JSONObject jsonObj = new JSONObject(contractTreeResponse).getJSONObject("body").getJSONObject("data");
			JSONArray jsonArr = jsonObj.getJSONArray("children");

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = jsonArr.getJSONObject(i);

				Map<String, String> childrenMap = new HashMap<>();
				childrenMap.put("text", jsonObj.getString("text"));
				childrenMap.put("entityTypeId", String.valueOf(jsonObj.getInt("entityTypeId")));
				childrenMap.put("entityId", String.valueOf(jsonObj.getInt("entityId")));
				childrenMap.put("numberOfChild", String.valueOf(jsonObj.getInt("numberOfChild")));
				childrenMap.put("lightColor", String.valueOf(jsonObj.getBoolean("lightColor")));

				allChildrenMap.add(childrenMap);
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Children Map. {}" + e.getMessage());
			return null;
		}

		return allChildrenMap;
	}

	public List<String> getAllContractShortCodeIdOfSupplier(int supplierId) {
		List<String> allContractShortCodeIds = new ArrayList<>();

		try {
			hitContractTreeDataListAPI(1, supplierId);
			String contractTreeResponse = getResponseContractTreeData();

			if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
				JSONObject jsonObj = new JSONObject(contractTreeResponse);
				jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
				JSONArray jsonArr = jsonObj.getJSONArray("children");

				for (int i = 0; i < jsonArr.length(); i++) {
					allContractShortCodeIds.add(String.valueOf(jsonArr.getJSONObject(i).getInt("clientEntitySeqId")));
				}
			} else {
				logger.error("Contract Tree API Response for Supplier Id {} is an Invalid JSON.", supplierId);
				return null;
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Contract Short Code Ids of Supplier Id {}. {}", supplierId, e.getStackTrace());
			return null;
		}

		return allContractShortCodeIds;
	}
}
