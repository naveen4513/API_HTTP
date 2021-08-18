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
 * Created by shivashish on 29/8/17.
 */
public class ScheduleByMeReportAPI extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(ScheduleByMeReportAPI.class);
	String apiStatusCode = null;
	String apiReponse;

	Integer entityTypeId=null;
	Integer reportId=0;

	String payload = null;

	String scheduleReportConfigFilePath;
	String scheduleReportConfigFileName;

	Integer scheduleByMeUrlId;


	// constructor which will take the entityTypeId and reportId
	public ScheduleByMeReportAPI(Integer entityTypeId, Integer reportId) throws ConfigurationException {

		this.entityTypeId = entityTypeId;
		this.reportId = reportId;

		scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
		scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");

		scheduleByMeUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "schedulebymereportapiurlid"));
		payload = createPayloadForScheduleByMeReportAPI(); // to Get the Payload

	}

	// constructor for Generic API Call
	public ScheduleByMeReportAPI() throws ConfigurationException {

		scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
		scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");

		scheduleByMeUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "schedulebymereportapiurlid"));
		payload = createPayloadForScheduleByMeReportAPI(); // to Get the Payload

	}



	// Making this function independent so that we can modify the payload in future according to need
	private String createPayloadForScheduleByMeReportAPI() {
		String payload;

		payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1000,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{},\"scheduleEntityTypeId\":" + entityTypeId + "}}";
		return payload;
	}

	public String getResponseScheduleByMeReportAPI() {
		return apiReponse;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}


	// this function will hit the Create Report For API for Given Report Id
	public HttpResponse hitScheduleByMeReportAPI() throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + scheduleByMeUrlId + "/scheduledata/" + reportId;

		logger.info("Query string url formed is {}", queryString);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("create Schedule By Me Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		apiReponse = EntityUtils.toString(response.getEntity());

		return response;

	}

	public HttpResponse hitScheduleByMeReportAPI(int listId,int reportId) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + listId + "/scheduledata/" + reportId;

		logger.info("Query string url formed is {}", queryString);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("create Schedule By Me Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		apiReponse = EntityUtils.toString(response.getEntity());

		return response;

	}
}
