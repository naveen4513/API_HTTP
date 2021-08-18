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

public class MasterContractTypesShow extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(MasterContractTypesShow.class);

	public static String getAPIPath(String functionId) {
		return "/mastercontracttypes/show/" + functionId;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static List<Map<String, String>> getAllServicesOfFunction(String showResponse, String functionName, String functionId) {
		List<Map<String, String>> allServicesList = new ArrayList<>();

		try {
			logger.info("Getting All Services List for Function [{}] having Id.", functionName, functionId);
			Document html = Jsoup.parse(showResponse);
			Element div = html.getElementById("l_com_sirionlabs_model_MasterContractSubType");

			if (div == null) {
				return allServicesList;
			}

			div = div.child(1);
			Elements subDivs = div.getAllElements().get(0).children();

			for (Element subDiv : subDivs) {
				String tagName = subDiv.tagName();

				if (tagName.trim().equalsIgnoreCase("script")) {
					continue;
				}

				Element oneService = subDiv.child(0).child(0);
				String hrefValue = oneService.attr("href");
				String serviceName = oneService.child(0).childNode(0).toString();
				serviceName = serviceName.replace("\n", "");

				String[] temp = hrefValue.split(Pattern.quote(";"));
				String[] temp2 = temp[0].split(Pattern.quote("/"));
				String serviceId = temp2[temp2.length - 1];

				Map<String, String> serviceMap = new HashMap<>();
				serviceMap.put("id", serviceId);
				serviceMap.put("name", serviceName);

				allServicesList.add(serviceMap);
			}
		} catch (Exception e) {
			logger.info("Exception while Getting All Services List of Function [{}] having Id {}. {}", functionName, functionId, e.getStackTrace());
			return null;
		}

		return allServicesList;
	}

	public static String getMasterContractTypesShowResponseBody(String functionId) {
		return executor.get(getAPIPath(functionId), getHeaders()).getResponse().getResponseBody();
	}
}