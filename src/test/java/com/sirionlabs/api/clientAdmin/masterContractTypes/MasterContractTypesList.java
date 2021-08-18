package com.sirionlabs.api.clientAdmin.masterContractTypes;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MasterContractTypesList extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(MasterContractTypesList.class);

	private static String apiPath = "/mastercontracttypes/list";

	public static String getAPIPath() {
		return apiPath;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static List<Map<String, String>> getAllFunctionsList(String listResponse) {
		List<Map<String, String>> allFunctionsList = new ArrayList<>();

		try {
			logger.info("Getting All Functions List.");
			Document html = Jsoup.parse(listResponse);
			Element div = html.getElementById("l_com_sirionlabs_model_MasterContractType").child(1);
			Elements subDivs = div.getAllElements().get(0).children();

			for (Element subDiv : subDivs) {
				String tagName = subDiv.tagName();

				if (tagName.trim().equalsIgnoreCase("script")) {
					continue;
				}

				Element oneFunction = subDiv.child(0).child(0);
				String hrefValue = oneFunction.attr("href");
				String functionName = oneFunction.child(0).childNode(0).toString();
				functionName = functionName.replace("\n", "");

				String[] temp = hrefValue.split(Pattern.quote(";"));
				String[] temp2 = temp[0].split(Pattern.quote("/"));
				String functionId = temp2[temp2.length - 1];

				Map<String, String> functionMap = new HashMap<>();
				functionMap.put("id", functionId);
				functionMap.put("name", functionName);

				allFunctionsList.add(functionMap);
			}
		} catch (Exception e) {
			logger.info("Exception while Getting All Functions List. {}", e.getMessage());
			return null;
		}

		return allFunctionsList;
	}

	public static String getMasterContractTypesListResponseBody() {
		return executor.get(getAPIPath(), getHeaders()).getResponse().getResponseBody();
	}
}