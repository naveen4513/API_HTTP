package com.sirionlabs.api.snowIntegration;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 19/9/17.
 */
public class ChildServiceLevelPerformenceDataExcelDownload extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(ChildServiceLevelPerformenceDataExcelDownload.class);

	/**
	 * @param sOutputFile : Output File to store downloaded result
	 * @param excelId     : excel Id
	 * @return
	 * @throws Exception
	 */
	public HttpResponse downloadServiceDataTemplateFile(String sOutputFile, String excelId) throws Exception {
		logger.info("Started Download XLS Template for excel ID : [ {} ] and download File Location will be : [ {} ]", excelId, sOutputFile);

		//Generate HTTP Target Host
		HttpHost target = generateHttpTargetHost();

		String queryString = "/download/v1/cslaPerformanceData/" + excelId;
		logger.debug("Query string url formed is {}", queryString);

		//Generate HTTP get Request based on query String
		String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
		HttpGet getRequest = generateHttpGetRequestWithQueryString(queryString, acceptsHeader);

		return downloadAPIResponseFile(sOutputFile, target, getRequest);
	}

}
