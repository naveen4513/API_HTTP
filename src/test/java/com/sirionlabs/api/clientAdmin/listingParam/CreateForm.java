package com.sirionlabs.api.clientAdmin.listingParam;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CreateForm extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(CreateForm.class);

	public static String getApiPath(int reportId) {
		return "/listingparam/createForm/" + reportId;
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();

		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
		headers.put("Accept-Encoding", "gzip, deflate");

		return headers;
	}

	public static String getCreateFormResponse(int reportId) {
		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		AdminHelper adminHelperObj = new AdminHelper();
		adminHelperObj.loginWithClientAdminUser();

		String reportListConfigureResponse = executor.get(getApiPath(reportId), getHeaders()).getResponse().getResponseBody();

		Check checkObj = new Check();
		checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

		return reportListConfigureResponse;
	}

	public static List<String> getAllSelectedPerformanceStatus(Integer reportId) {
		return getAllSelectedPerformanceStatus(getCreateFormResponse(reportId), reportId);
	}

	public static List<String> getAllSelectedPerformanceStatus(String createFormResponse, Integer reportId) {
		List<String> allSelectedPerformanceStatus = new ArrayList<>();

		try {
			logger.info("Getting All Selected Performance Status for Report Id {}", reportId);
			Document html = Jsoup.parse(createFormResponse);
			Element table = html.getElementsByClass("form-container").get(0).child(1);
			Elements allTableRows = table.children();

			for (Element row : allTableRows) {
				if (row.text().contains("Parameter : Performance Status To Include")) {
					Elements allPerformanceStatusOptions = row.child(4).child(0).children();

					for (Element option : allPerformanceStatusOptions) {
						if (option.hasAttr("selected")) {
							allSelectedPerformanceStatus.add(option.childNode(0).toString());
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Selected Performance Status for Report Id {}. {}", reportId, e.getStackTrace());
		}
		return allSelectedPerformanceStatus;
	}

	public static List<String> getAllSelectedStatus(Integer reportId) {
		return getAllSelectedPerformanceStatus(getCreateFormResponse(reportId), reportId);
	}

	public static List<String> getAllSelectedStatus(String createFormResponse, Integer reportId) {
		List<String> allSelectedPerformanceStatus = new ArrayList<>();

		try {
			logger.info("Getting All Selected Status for Report Id {}", reportId);
			Document html = Jsoup.parse(createFormResponse);
			Element table = html.getElementsByClass("form-container").get(0).child(1);
			Elements allTableRows = table.children();

			String parameterStr;

			switch(reportId) {
				case 444:
				case 385:
				case 386:
				case 355:
					parameterStr = "status";
					break;
				case 359:
					parameterStr = "Status";
					break;
				case 95:
					parameterStr="change request high priority";
					break;
				case 88:
					parameterStr="change request aging";
					break;
				case 89:
					parameterStr="Change Request Cycle Time";
					break;
				case 38:
					parameterStr="High Aging Status To Include";
					break;
				case 27:
					parameterStr="Status To Include For Rejection";
					break;
				case 324:
					parameterStr="Tracker";
					break;
				case 201:
					parameterStr="Status To Include";
					break;

				default:
					parameterStr = "status to include";
					break;
			}

			for (Element row : allTableRows) {
				if (row.text().toLowerCase().contains("parameter :")) {
					String[] paramsArr = row.text().toLowerCase().trim().split(":");
					paramsArr[1] = paramsArr[1].replace("value*", "").trim();

					if(paramsArr[1].equalsIgnoreCase(parameterStr)) {
						Elements allPerformanceStatusOptions = row.child(4).child(0).children();

						for (Element option : allPerformanceStatusOptions) {
							if (option.hasAttr("selected")) {
								allSelectedPerformanceStatus.add(option.childNode(0).toString());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Selected Status for Report Id {}. {}", reportId, e.getStackTrace());
		}
		return allSelectedPerformanceStatus;
	}
}
