package com.sirionlabs.api.presignature;

import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClausePageData extends APIUtils {

	public String clausePageDataResponseStr;
	public String compareClauseResponseStr;
	private final static Logger logger = LoggerFactory.getLogger(ClausePageData.class);

	public HttpResponse hitClausePageData(int entityDbId) {

		HttpResponse response = null;
		try {
			HttpGet getRequest;

			String queryString = "/tblclause/clausePageData/"+entityDbId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/plain, */*");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.getRequest(getRequest, true);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Show response header {}", oneHeader.toString());
			}
			this.clausePageDataResponseStr = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting ClausePageData Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitCompareClause(int entityId) {

		HttpResponse response = null;
		try {
			HttpGet getRequest;

			String queryString = "/tblclause/getAllVersion/"+entityId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/plain, */*");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.getRequest(getRequest, true);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Show response header {}", oneHeader.toString());
			}
			this.compareClauseResponseStr = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting ClausePageData Api. {}", e.getMessage());
		}
		return response;
	}

	public String getClausePageDataResponseStr() {
		return this.clausePageDataResponseStr;
	}

	public String getCompareClauseResponseStr() {
		return this.compareClauseResponseStr;
	}
}