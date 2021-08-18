package com.sirionlabs.helper.clientSetup;

import com.sirionlabs.api.clientSetup.provisioning.ProvisioningList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSetupHelper {

	private final static Logger logger = LoggerFactory.getLogger(ClientSetupHelper.class);

	public String getClientNameFromId(int clientId) {
		try {
			logger.info("Getting Client Name from Id {}", clientId);

			if (!loginWithClientSetupUser()) {
				logger.error("Couldn't login with Client Setup User. Hence couldn't get Client Name");
				return null;
			}

			String provisioningListResponse = ProvisioningList.getProvisioningListResponseBody();

			Document html = Jsoup.parse(provisioningListResponse);
			Elements allClients = html.getElementsByClass("tabs-inner-sec-content").get(0).select("table")
					.get(0).select("tr").get(0).select("td").get(1).select("option");

			for (int i = 1; i < allClients.size(); i++) {
				String id = allClients.get(i).val();

				if (String.valueOf(clientId).equalsIgnoreCase(id)) {
					return allClients.get(i).text();
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Client Name from Id {} . {}", clientId, e.getStackTrace());
		}

		return null;
	}

	public boolean loginWithClientSetupUser() {
		String clientSetupUserName = ConfigureEnvironment.getClientSetupUserName();
		String clientSetupUserPassword = ConfigureEnvironment.getClientSetupUserPassword();

		Check checkObj = new Check();
		checkObj.hitCheckForClientSetup(clientSetupUserName, clientSetupUserPassword);

		return (Check.getAuthorization() != null);
	}

	public boolean loginWithSuperAdmin() {
		String superAdminUserName = ConfigureEnvironment.getSuperAdminUserName();
		String superAdminUserPassword = ConfigureEnvironment.getSuperAdminPassword();

		Check checkObj = new Check();
		checkObj.hitCheckForClientSetup(superAdminUserName, superAdminUserPassword);

		return (Check.getAuthorization() != null);
	}

	public boolean loginWithUserAdmin()
	{
		String userAdminName = ConfigureEnvironment.getUserAdminName();
		String userAdminPassword = ConfigureEnvironment.getUserAdminPassword();

		Check checkObj = new Check();
		checkObj.hitCheckForClientSetup(userAdminName, userAdminPassword);

		return (Check.getAuthorization() != null);
	}
}