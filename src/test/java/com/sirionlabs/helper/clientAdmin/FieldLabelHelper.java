package com.sirionlabs.helper.clientAdmin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldLabelHelper {

	private final static Logger logger = LoggerFactory.getLogger(FieldLabelHelper.class);

	public Integer getFieldLabelGroupValueFromCreateFormAPI(String fieldLabelCreateFormAPIResponse, String groupName) {
		return getFieldLabelGroupValueFromCreateFormAPI(fieldLabelCreateFormAPIResponse, null, groupName);
	}

	public Integer getFieldLabelGroupValueFromCreateFormAPI(String fieldLabelCreateFormAPIResponse, String parentGroupLabel, String groupName) {
		try {
			logger.info("Getting FieldLabel Group Value from CreateForm API Response for ParentGroupLabel [{}] and GroupName [{}]", parentGroupLabel, groupName);
			Document html = Jsoup.parse(fieldLabelCreateFormAPIResponse);
			Element div = html.getElementById("tabs").getElementById("generalInfo");

			Element supplierList = div.getElementsByClass("top-heading").get(0).getElementById("supplier");
			Elements allGroups = new Elements();

			if (parentGroupLabel != null) {
				Elements allParentGroups = supplierList.select("optgroup");

				for (Element parentGroup : allParentGroups) {
					if (parentGroup.attributes().get("label").trim().equalsIgnoreCase(parentGroupLabel)) {
						allGroups = parentGroup.children();
						break;
					}
				}
			} else {
				allGroups = supplierList.children();
			}

			for (Element group : allGroups) {
				if (group.text().trim().equalsIgnoreCase(groupName.trim())) {
					return Integer.parseInt(group.attributes().get("value").trim());
				}
			}

			logger.error("Couldn't find Entry for ParentGroupLabel [{}] and GroupName [{}] in CreateForm API Response.", parentGroupLabel, groupName);
		} catch (Exception e) {
			logger.error("Exception while Getting Id of FieldLabel Group from CreateForm API Response using ParentGroupLabel [{}], GroupName [{}]. {}", parentGroupLabel,
					groupName, e.getStackTrace());
		}
		return null;
	}

	public Integer getFieldLabelLanguageIdFromCreateFormAPI(String fieldLabelCreateFormAPIResponse, String languageName) {
		try {
			logger.info("Getting FieldLabel Language Id from CreateForm API Response for Language Name {}", languageName);
			Document html = Jsoup.parse(fieldLabelCreateFormAPIResponse);
			Element div = html.getElementById("tabs").getElementById("generalInfo");

			Element languageList = div.getElementsByClass("top-heading").get(0).getElementById("language");
			Elements allOptions = languageList.children();

			for (Element language : allOptions) {
				if (language.text().trim().equalsIgnoreCase(languageName.trim())) {
					return Integer.parseInt(language.attributes().get("value").trim());
				}
			}

			logger.error("Couldn't find Entry for Language [{}] in CreateForm API Response.", languageName);
		} catch (Exception e) {
			logger.error("Exception while Getting Id of FieldLabel Language from CreateForm API Response using LanguageName [{}]. {}", languageName, e.getStackTrace());
		}
		return null;
	}

	public String getEntityGroupName(String entityName) {
		String groupName;

		switch (entityName) {
			case "vendors":
				groupName = "Vendor Hierarchy";
				break;

			case "suppliers":
				groupName = "Supplier";
				break;

			case "contracts":
				groupName = "Contract";
				break;

			case "actions":
				groupName = "Action";
				break;

			case "issues":
				groupName = "Issue";
				break;

			case "change requests":
				groupName = "Change Request";
				break;

			case "obligations":
				groupName = "Master Obligation";
				break;

			case "child obligations":
				groupName = "Child Obligation";
				break;

			case "service levels":
				groupName = "Master Service Level";
				break;

			case "invoices":
				groupName = "Invoice";
				break;

			case "interpretations":
				groupName = "Interpretation";
				break;

			case "work order requests":
				groupName = "Work Order Request";
				break;

			case "clauses":
				groupName = "Clause";
				break;

			case "contract templates":
				groupName = "Contract Template";
				break;

			case "purchase orders":
				groupName = "Purchase Order";
				break;

			case "disputes":
				groupName = "Dispute";
				break;

			case "consumptions":
				groupName = "Consumption";
				break;

			default:
				groupName = entityName;
				break;
		}

		return groupName;
	}
}