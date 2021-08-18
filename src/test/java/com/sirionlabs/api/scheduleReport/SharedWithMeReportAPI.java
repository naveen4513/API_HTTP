package com.sirionlabs.api.scheduleReport;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 15/6/18.
 */
public class SharedWithMeReportAPI extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(SharedWithMeReportAPI.class);
	String apiStatusCode = null;
	String apiReponse;

	Integer entityTypeId=null;
	Integer reportId=0;

	String payload = null;

	String scheduleReportConfigFilePath;
	String scheduleReportConfigFileName;

	Integer sharedWithMeReportUrlId;


	// constructor which will take the entityTypeId and reportId
	public SharedWithMeReportAPI(Integer entityTypeId, Integer reportId) throws ConfigurationException {

		this.entityTypeId = entityTypeId;
		this.reportId = reportId;

		scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
		scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");

		sharedWithMeReportUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "sharedwithmereportapiurld"));
		payload = createPayloadForSharedWithMeReportAPI(); // to Get the Payload

	}


	// constructor for Generic API Call
	public SharedWithMeReportAPI() throws ConfigurationException {
		scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
		scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");

		sharedWithMeReportUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "sharedwithmereportapiurld"));
		payload = createPayloadForSharedWithMeReportAPI(); // to Get the Payload

	}



	// Making this function independent so that we can modify the payload in future according to need
	private String createPayloadForSharedWithMeReportAPI() {
		String payload;

		payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{},\"scheduleEntityTypeId\":" + entityTypeId + "}}";
		return payload;
	}

	public String getResponseSharedWithMeReportAPI() {
		return apiReponse;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}


	// this function will hit the Shared with Me Report API for Given Report Id
	public HttpResponse hitSharedWithMeReportAPI() throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + sharedWithMeReportUrlId + "/scheduledata/" + reportId;

		logger.info("Query string url formed is {}", queryString);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Shared with Me Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		apiReponse = EntityUtils.toString(response.getEntity());

		return response;

	}
	public HttpResponse hitSharedWithMeReportAPI(int listId,String reportId,String payload) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + listId+ "/scheduledata/" + reportId;

		logger.info("Query string url formed is {}", queryString);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Shared with Me Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		apiReponse = EntityUtils.toString(response.getEntity());

		return response;

	}

}
