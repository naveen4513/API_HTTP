package com.sirionlabs.api.clientAdmin.fieldProvisioning;

import com.sirionlabs.api.clientAdmin.dropDownType.DropDownTypeList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePage extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(CreatePage.class);

	public static String getAPIPath() {
		return "/fieldprovisioning/createpage";
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/html, */*; q=0.01");

		return headers;
	}

	public static String getFieldProvisioningCreatePageResponse() {
		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		AdminHelper adminHelperObj = new AdminHelper();

		//Logging with Client Admin User
		if (!adminHelperObj.loginWithClientAdminUser()) {
			return null;
		}

		logger.info("Hitting Field Provisioning Create Page API.");
		String createPageResponse = executor.get(getAPIPath(), getHeaders()).getResponse().getResponseBody();

		//Logging back with End User
		adminHelperObj.loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);

		return createPageResponse;
	}

	public static List<Integer> getAllSupplierIdsFromCreatePageResponse(String createPageResponse) {
		List<Integer> allSupplierIds = new ArrayList<>();

		try {
			logger.info("Getting All Supplier Ids from Field Provisioning Create Page Response.");
			Document html = Jsoup.parse(createPageResponse);
			String dropDownListResponse = DropDownTypeList.getDropDownListResponse();

			Boolean isSupplierFieldAutoCompleteType = DropDownTypeList.isFieldOfAutoCompleteType(dropDownListResponse, "supplier");

			if (isSupplierFieldAutoCompleteType == null) {
				return null;
			}

			if (!isSupplierFieldAutoCompleteType) {
				Elements allSupplierOptions = html.getElementById("SupplierNameC").children();

				for (int i = 1; i < allSupplierOptions.size(); i++) {
					allSupplierIds.add(Integer.parseInt(allSupplierOptions.get(i).attr("value")));
				}
			} else {
				OptionsHelper optionObj = new OptionsHelper();
				List<Map<String, String>> allSupplierOptions = optionObj.getAllOptionsForAutoCompleteField("suppliers", 1,
						"clientadmin", "");

				for (Map<String, String> supplierOption : allSupplierOptions) {
					allSupplierIds.add(Integer.parseInt(supplierOption.get("id")));
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Supplier Ids from Create Page Response. " + e.getMessage());
			return null;
		}
		return allSupplierIds;
	}
}