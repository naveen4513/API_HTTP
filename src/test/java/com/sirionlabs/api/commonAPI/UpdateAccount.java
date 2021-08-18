package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UpdateAccount extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UpdateAccount.class);

	public Integer hitUpdateAccount(Map<String, String> params) {
		Integer statusCode = -1;
		try {
			HttpHost target = super.generateHttpTargetHost();
			String queryString = "/updateAccount";
			logger.debug("Query string url formed is {}", queryString);
			String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			String contentTypeHeader = "multipart/form-data; boundary=----WebKitFormBoundarykggQHAjpfi6idAX5";

			HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, contentTypeHeader);
			HttpEntity entity = super.createMultipartEntityBuilder(params);
			postRequest.setEntity(entity);

			HttpClient httpClient;

			if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
				APIUtils oAPIUtils = new APIUtils();
				httpClient = oAPIUtils.getHttpsClient();
			} else {
				if (ConfigureEnvironment.isProxyEnabled) {
					HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
					httpClient = HttpClients.custom().setProxy(proxy).build();
				} else {
					httpClient = HttpClientBuilder.create().build();
				}
			}
			HttpResponse response = httpClient.execute(target, postRequest);
			String[] statusLine = response.getStatusLine().toString().trim().split(Pattern.quote("HTTP/1.1"));

			if (statusLine.length > 1) {
				statusCode = Integer.parseInt(statusLine[1].trim());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting BulkUpload Download Api. {}", e.getMessage());
		}
		return statusCode;
	}

	public boolean updateUserLanguage(String userLoginId, int clientId, int newLanguageId) {
		try {
			PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

			String query = "update app_user set language_id = " + newLanguageId + " where client_id = " + clientId + " and login_id ='" + userLoginId + "'";
			return sqlObj.updateDBEntry(query);
		} catch (Exception e) {
			logger.error("Exception while Updating Language for User {} and Client Id {}. {}", userLoginId, clientId, e.getMessage());
		}

		return false;
	}

	public int getCurrentLanguageIdForUser(String userLoginId, int clientId) {
		try {
			PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

			String query = "select language_id from app_user where client_id = " + clientId + " and login_id ='" + userLoginId + "'";
			List<List<String>> results = sqlObj.doSelect(query);

			if (!results.isEmpty()) {
				sqlObj.closeConnection();
				return Integer.parseInt(results.get(0).get(0));
			} else {
				logger.error("Couldn't get Current Language Id for User {} and Client Id {}.", userLoginId, clientId);
			}

		} catch (Exception e) {
			logger.error("Exception while Getting Current Language Id for User {} and Client Id {}. {}", userLoginId, clientId, e.getMessage());
		}

		return -1;
	}
}
