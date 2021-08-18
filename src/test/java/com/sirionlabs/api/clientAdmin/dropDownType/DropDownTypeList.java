package com.sirionlabs.api.clientAdmin.dropDownType;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class DropDownTypeList extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(DropDownTypeList.class);

	public static String getApiPath() {
		return "/dropdowntype/list";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static String getDropDownListResponse() {
		return executor.get(getApiPath(), getHeaders()).getResponse().getResponseBody();
	}

	public static String listIdFromResponse(String responseList, String fieldName) {
		String id = "";

		try {
			String ListIDValue = "";
			String ListIDHref = "";

			Document HTML = Jsoup.parse(responseList);
			int size = HTML.getElementById("l_com_sirionlabs_model_dropdownType").childNode(1).childNodeSize();

			for (int i = 0; i < size - 1; i++) {
				ListIDValue = HTML.getElementById("l_com_sirionlabs_model_dropdownType").child(1).child(i).child(0).child(0).child(0).childNode(0).toString()
						.replace("\n", "");
				if (ListIDValue.equalsIgnoreCase(fieldName)) {
					ListIDHref = HTML.getElementById("l_com_sirionlabs_model_dropdownType").child(1).child(i).child(0).child(0).attr("href");
					String[] temp = ListIDHref.split(";");
					String[] temp1 = temp[0].split("/");
					id = temp1[3];
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting List Id for Field {}. {}", fieldName, e.getStackTrace());
			return null;
		}

		return id;
	}

	public static Boolean isFieldOfAutoCompleteType(String listResponse, String fieldName) {
		try {
			logger.info("Checking if Field {} is of AutoComplete type or not.", fieldName);
			Document html = Jsoup.parse(listResponse);
			Elements allFieldOptions = html.getElementById("l_com_sirionlabs_model_dropdownType").child(1).children();

			for (Element fieldOption : allFieldOptions) {
				if (fieldOption.tagName().contains("script")) {
					continue;
				}

				String actualOption = fieldOption.child(0).child(0).child(0).childNode(0).toString().toLowerCase();

				if (actualOption.contains(fieldName.toLowerCase()) && !actualOption.contains("user admin")) {
					return fieldOption.child(1).child(0).childNode(0).toString().toLowerCase().contains("auto complete");
				}
			}

			logger.info("Couldn't find Field {} in DropDownList Response.", fieldName);
		} catch (Exception e) {
			logger.error("Exception while checking if Field {} is of AutoComplete Type or not. {}", fieldName, e.getStackTrace());
			return null;
		}

		return false;
	}
}