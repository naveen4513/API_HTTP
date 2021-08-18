package com.sirionlabs.api.servicedata;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author manoj.upreti
 */
public class TblauditlogsFieldHistory extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(TblauditlogsFieldHistory.class);
	String tblAuditLogsFieldHistoryResponseStr = null;

	public HttpResponse hitTblauditlogsFieldHistoryPage(String fieldHistoryURL) throws IOException {
		logger.debug("Hitting Tbl Audit Logs Field History API");
		HttpResponse response = null;
		logger.debug("Query string url is {}", fieldHistoryURL);

		HttpGet getRequest = new HttpGet(fieldHistoryURL);
		getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		getRequest.addHeader("Accept-Encoding", "gzip, deflate");
		response = super.getRequest(getRequest);

		tblAuditLogsFieldHistoryResponseStr = EntityUtils.toString(response.getEntity());
		logger.debug("The Response for field History is : [ {} ]", tblAuditLogsFieldHistoryResponseStr);
		return response;
	}

	public String getTblAuditLogsFieldHistoryResponseStr() {
		return tblAuditLogsFieldHistoryResponseStr;
	}
}
