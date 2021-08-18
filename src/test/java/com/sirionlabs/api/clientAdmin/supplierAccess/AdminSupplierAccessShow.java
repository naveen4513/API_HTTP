package com.sirionlabs.api.clientAdmin.supplierAccess;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminSupplierAccessShow extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(AdminSupplierAccessShow.class);

	public String hitSupplierAccessShow() {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/tblsupplieraccess/show";
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = super.getRequest(getRequest);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Admin Supplier Access Show Response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Admin Supplier Access Show Api. {}", e.getMessage());
		}
		return response;
	}
}