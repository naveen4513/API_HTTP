package com.sirionlabs.api.userGroups;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserGroupsUsers extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UserGroupsUsers.class);

	public String hitUserGroupsUsers(int userGroupId, int entityTypeId, int recordId) throws Exception {
		String usersResponse = null;

		try {
			HttpGet getRequest;
			String queryString = "/usergroups/" + userGroupId + "/users/" + entityTypeId + "/" + recordId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = super.getRequest(getRequest, false);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			usersResponse = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("UserGroups Users response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting UserGroups Users Api. {}", e.getMessage());
		}
		return usersResponse;
	}
}