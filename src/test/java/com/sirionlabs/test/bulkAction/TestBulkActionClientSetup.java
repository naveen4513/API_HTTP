package com.sirionlabs.test.bulkAction;

import com.sirionlabs.api.clientSetup.provisioning.ProvisioningEdit;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;

public class TestBulkActionClientSetup extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(TestBulkActionClientSetup.class);
	private ClientSetupHelper setupHelperObj = new ClientSetupHelper();

	private String clientName = null;
	private Integer clientId = null;

	@BeforeClass
	public void beforeClass() {
		AdminHelper adminObj = new AdminHelper();
		clientId = adminObj.getClientId();
		clientName = setupHelperObj.getClientNameFromId(clientId);
	}

	@AfterClass
	public void afterClass() {
		logger.info("Logging back with End User.");
		String endUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
		String endUserPassword = ConfigureEnvironment.getEnvironmentProperty("password");

		Check checkObj = new Check();
		checkObj.hitCheck(endUserName, endUserPassword);
	}

	/*
	TC-C9075: Verify Bulk Action Checkbox Permission is Available for all the Entities in Client Setup.
	 */
	@Test
	public void testBulkActionCheckboxInClientSetup() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C9075: Verify Bulk Action Checkbox in Client Setup.");

			String[] entitiesToTest = {
					"Obligation",
					"Child Obligation",
					"Service Levels",
					"Child Service Level",
					"Consumption",
					"Invoice",
					"Contract Service Data",
					"Clause",
					"Governance Body"
			};

			if (clientId == null || clientName == null) {
				throw new SkipException("Couldn't get Client Id/Name.");
			}

			logger.info("Hitting Provisioning Edit API for Client Id {} and Name {}", clientId, clientName);
			String apiPath = ProvisioningEdit.getApiPath(clientId, clientName);
			HashMap<String, String> headers = ProvisioningEdit.getHeaders();
			String provisioningEditResponse = executor.get(apiPath, headers).getResponse().getResponseBody();

			Document html = Jsoup.parse(provisioningEditResponse);
			Element div = html.getElementById("permission").getElementsByClass("accordion").get(0).child(1).child(0);
			Elements allSubDivs = div.children();

			for (String entity : entitiesToTest) {
				logger.info("Validating Bulk Action Permission for Entity {}", entity);
				boolean bulkActionFound = false;

				for (int i = 0; i < allSubDivs.size(); i = i + 2) {
					Element subDiv = allSubDivs.get(i);
					String sectionName = subDiv.child(0).child(0).childNode(0).toString().trim();

					if (sectionName.equalsIgnoreCase(entity + ":")) {
						Elements allChildDivsOfEntity = allSubDivs.get(i + 1).child(0).child(0).child(0).child(0).children();

						for (Element childDiv : allChildDivsOfEntity) {
							String propertyName = childDiv.childNode(3).toString().trim();

							if (propertyName.equalsIgnoreCase("Bulk Action")) {
								bulkActionFound = true;
								break;
							}
						}
						break;
					}
				}

				csAssert.assertTrue(bulkActionFound, "Bulk Action CheckBox not found in Provisioning Edit API Response for Entity " + entity);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Bulk Action Permission at Client Setup TC-C9075. " + e.getMessage());
		}
		csAssert.assertAll();
	}
}
