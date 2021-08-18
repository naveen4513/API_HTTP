package com.sirionlabs.api.commonAPI;


import com.google.inject.Singleton;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Comment extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Comment.class);

	public String hitComment(String entityName, String payload) {
		String response = null;

		try {
			HttpPost postRequest;

			String urlName = ConfigureConstantFields.getUrlNameForEntity(entityName);
			String queryString = "/" + urlName + "/comment";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting Comment Api. {}", e.getMessage());
		}
		return response;
	}


	public Boolean checkSuccessCommentApi(String response) {
		boolean success = false;
		String status = (String) JSONUtility.parseJson(response, "$.header.response.status");
		if (status.equals("success")) {
			success = true;
		}
		return success;
	}

	public String createCommentPayload(int entityTypeId, int entityId, String requestedBy, String shareWithSupplier, String comments, String documentTags, String draft, String actualDate, String privateCommunication, String changeRequest, String workOrderRequest, String commentDocuments, ArrayList<String> randomList) {
		return createCommentPayload(entityTypeId,entityId,requestedBy,shareWithSupplier,comments,documentTags,draft,actualDate,privateCommunication,changeRequest,workOrderRequest,commentDocuments,randomList,false,null,null);
	}

	public String createCommentPayload(int entityTypeId, int entityId, String requestedBy, String shareWithSupplier, String comments, String documentTags, String draft, String actualDate, String privateCommunication, String changeRequest, String workOrderRequest, String commentDocuments, ArrayList<String> randomList,String fileName,String filePath) {
		return createCommentPayload(entityTypeId,entityId,requestedBy,shareWithSupplier,comments,documentTags,draft,actualDate,privateCommunication,changeRequest,workOrderRequest,commentDocuments,randomList,false,fileName,filePath);
	}

	public String createCommentPayload(int entityTypeId, int entityId, String requestedBy, String shareWithSupplier, String comments, String documentTags, String draft, String actualDate, String privateCommunication, String changeRequest, String workOrderRequest, String commentDocuments, ArrayList<String> randomList, boolean isInvoiceCopy,String fileName,String filePath) {
		String randomKeyForDocumentFile=null;

		if(fileName!=null&&filePath!=null){
			randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);

			String uploadResponse = DocumentHelper.uploadDocumentFile(filePath,fileName,randomKeyForDocumentFile);
			assert uploadResponse != null:"upload response null";
			if(!uploadResponse.contains(fileName)){
				return uploadResponse;
			}
		}

		Show showObj = new Show();
		showObj.hitShow(entityTypeId, entityId);
		String show_response = showObj.getShowJsonStr();
		JSONObject obj = new JSONObject(show_response);
		JSONObject body = obj.getJSONObject("body");
		JSONObject data = body.getJSONObject("data");
		JSONObject comment = data.getJSONObject("comment");

		if (requestedBy != null && !requestedBy.equals("")) {
			updateRequestBy(comment, requestedBy);
		}
		if (comments != null && !comments.equals("")) {
			updateComment(comment, comments);
		}
		if (actualDate != null && !actualDate.equals("")) {
			updateActualDate(comment, actualDate);
		}
		if (privateCommunication != null && !privateCommunication.equals("")) {
			updatePrivateCommunication(comment, privateCommunication);
		}
		if (changeRequest != null && !changeRequest.equals("")) {
			updateChangeRequest(comment, changeRequest);
		}
		if (commentDocuments != null && !commentDocuments.equals("")) {
			updateCommentDocuments(comment, commentDocuments, randomList);
		}
		if (isInvoiceCopy) {
			updateInvoiceCopy(comment);
		}
		if (fileName!=null&&filePath!=null&&randomKeyForDocumentFile!=null) {
			updateCommentDocuments(comment,getCommentDocumentsData(comment), new ArrayList<>(Collections.singletonList(randomKeyForDocumentFile)));
		}


		JSONObject payload = new JSONObject();
		JSONObject payload_body = new JSONObject();
		data.put("comment", comment);
		payload_body.put("data", data);
		payload.put("body", payload_body);


		return payload.toString();
	}

	private void updateRequestBy(JSONObject comment, String requestedBy) {
		JSONObject rqstBy = comment.getJSONObject("requestedBy");
		rqstBy.put("values", new JSONObject(requestedBy));
		rqstBy.put("options", JSONObject.NULL);
	}

	private void updateComment(JSONObject comment, String comments) {
		JSONObject cmnts = comment.getJSONObject("comments");
		cmnts.put("values", comments);
	}
	private void updateActualDate(JSONObject comment, String actualDate) {
		JSONObject date = comment.getJSONObject("actualDate");
		date.put("values", actualDate);
	}
	private void updatePrivateCommunication(JSONObject comment, String privateCommunication) {
		JSONObject comm = comment.getJSONObject("privateCommunication");
		comm.put("values",privateCommunication);
	}

	private void updateChangeRequest(JSONObject comment, String changeRequest) {
		JSONObject changerqst = comment.getJSONObject("changeRequest");
		changerqst.put("values", new JSONObject(changeRequest));
		changerqst.put("options", JSONObject.NULL);
	}

	private void updateCommentDocuments(JSONObject comment, String commentDocuments, ArrayList<String> randomList) {
		JSONObject documents = comment.getJSONObject("commentDocuments");
		documents.remove("name");
		documents.remove("multiEntitySupport");
		documents.put("values",new JSONArray(commentDocuments));
		for (int i =0 ; i< randomList.size();i++){
			JSONArray	documentArray = documents.getJSONArray("values");
			JSONObject document = (JSONObject) documentArray.get(i);
			document.put("key",randomList.get(i));
		}
	}

	private void updateInvoiceCopy(JSONObject comment) {
		JSONObject jsonObject = new JSONObject("{ \"name\": \"invoiceCopy\", \"id\": 12585, \"values\": true, \"multiEntitySupport\": false }");
		comment.put("invoiceCopy",jsonObject);
	}
	private String getCommentDocumentsData(JSONObject comment) {
		return "[ {  \"performanceData\": false, \"searchable\": false, \"legal\": false, \"financial\": false, \"businessCase\": false } ]";
	}
}
