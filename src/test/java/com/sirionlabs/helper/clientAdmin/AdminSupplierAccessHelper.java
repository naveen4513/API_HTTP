package com.sirionlabs.helper.clientAdmin;

import com.sirionlabs.helper.OptionsHelper;
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

public class AdminSupplierAccessHelper {

	private final static Logger logger = LoggerFactory.getLogger(AdminSupplierAccessHelper.class);

	public Boolean isEntityPresentInSupplierAccessConfigurationShowAPIResponse(String supplierAccessShowResponse, String entityName) {
		try {
			Document html = Jsoup.parse(supplierAccessShowResponse);
			Element div = html.getElementById("_title_fc_com_sirionlabs_model_supplier_access_id").getElementById("basicInfo").getElementById("tabs-inner-sec");

			Elements classes = div.getElementsByClass("tabs-inner-sec-content");
			Element table = classes.select("table").get(0);

			Elements rows = table.select("tr");

			for (int i = 1; i < rows.size(); i++) {
				Element row = rows.get(i);
				Elements columns = row.select("td");

				for (Element column : columns) {
					if (column.text().trim().toLowerCase().contains(entityName.trim().toLowerCase())) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Checking if Entity {} is Present in Supplier Access Configuration. {}", entityName, e.getStackTrace());
			return null;
		}
		return false;
	}

	public String getSupplierAccessConfigurationForEntityFromShowAPI(String supplierAccessShowResponse, String entityName) {
		try {
			Document html = Jsoup.parse(supplierAccessShowResponse);
			Element div = html.getElementById("_title_fc_com_sirionlabs_model_supplier_access_id").getElementById("basicInfo").getElementById("tabs-inner-sec");

			Elements classes = div.getElementsByClass("tabs-inner-sec-content");
			Element table = classes.select("table").get(0);

			Elements rows = table.select("tr");

			for (Element row : rows) {
				Elements columns = row.select("td");

				for (int j = 0; j < columns.size(); j++) {
					Element column = columns.get(j);

					if (column.text().trim().toLowerCase().contains(entityName.trim().toLowerCase())) {
						return columns.get(j + 1).text();
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Supplier Access Configuration Value from Show API for Entity {}. {}", entityName, e.getStackTrace());
		}
		return null;
	}

	public List<Map<String, String>> getAllVendorsPresentUnderSupplierAccessAtVendorLevel(String supplierAccessVendorLevelCreateResponse) {
		List<Map<String, String>> allVendors = new ArrayList<>();

		try {
			boolean autoCompleteType = false;

			Document html = Jsoup.parse(supplierAccessVendorLevelCreateResponse);
			Element div = html.getElementById("_title_fc_com_sirionlabs_model_supplier_access_vendor_level_id");
			Elements subDivs = div.getAllElements();

			for (Element subDiv : subDivs) {
				if (subDiv.className().trim().equalsIgnoreCase("inputholder")) {
					autoCompleteType = true;
					break;
				}
			}

			if (!autoCompleteType) {
				div = div.getElementById("basicInfo").getElementById("tabs-inner-sec");
				Elements classes = div.getElementsByClass("tabs-inner-sec-content");
				Element table = classes.select("table").get(0);

				Element row = table.select("tr").get(0);
				Element column = row.select("td").get(1);

				Elements allOptions = column.select("option");

				for (int i = 1; i < allOptions.size(); i++) {
					Map<String, String> vendorsMap = new HashMap<>();

					vendorsMap.put("id", allOptions.get(i).val());
					vendorsMap.put("name", allOptions.get(i).text());

					allVendors.add(vendorsMap);
				}
			} else {
				OptionsHelper optionObj = new OptionsHelper();
				allVendors = optionObj.getAllOptionsForAutoCompleteField("Vendor Hierarchy", 1, "clientAdmin", "");
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Vendors Present Under Supplier Access at Vendor Level. {}", e.getMessage());
			return null;
		}
		return allVendors;
	}

	public Boolean isEntityPresentInVendorSupplierAccessAPIResponse(String vendorSupplierAccessResponse, String entityName) {
		try {
			Document html = Jsoup.parse(vendorSupplierAccessResponse);
			Element div = html.getElementById("_title_fc_com_sirionlabs_model_supplier_access_vendor_level_id").getElementById("basicInfo").getElementById("tabs-inner-sec");

			Elements classes = div.getElementsByClass("tabs-inner-sec-content");
			Element table = classes.select("table").get(0);

			Elements rows = table.select("tr");

			for (int i = 1; i < rows.size(); i++) {
				Element row = rows.get(i);
				Elements columns = row.select("td");

				for (Element column : columns) {
					if (column.text().trim().toLowerCase().contains(entityName.trim().toLowerCase())) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Checking if Entity {} is Present under Vendor Supplier Access. {}", entityName, e.getStackTrace());
			return null;
		}
		return false;
	}

	public List<String> getAllSupplierAccessOptionsAtVendorHierarchyLevelForEntity(String vendorSupplierAccessResponse, String entityName) {
		List<String> allSupplierAccessOptions = new ArrayList<>();

		try {
			Document html = Jsoup.parse(vendorSupplierAccessResponse);
			Element div = html.getElementById("_title_fc_com_sirionlabs_model_supplier_access_vendor_level_id").getElementById("basicInfo").getElementById("tabs-inner-sec");

			Elements classes = div.getElementsByClass("tabs-inner-sec-content");
			Element table = classes.select("table").get(0);

			Elements rows = table.select("tr");

			for (int i = 1; i < rows.size(); i++) {
				Element row = rows.get(i);
				Elements columns = row.select("td");

				for (int j = 0; j < columns.size(); j++) {
					Element column = columns.get(j);

					if (column.text().trim().toLowerCase().contains(entityName.trim().toLowerCase())) {
						Elements allOptions = columns.get(j + 1).select("option");

						for (int k = 1; k < allOptions.size(); k++) {
							Element option = allOptions.get(k);
							allSupplierAccessOptions.add(option.text().trim());
						}

						return allSupplierAccessOptions;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Supplier Access Options at Vendor Hierarchy Level for Entity {}. {}", entityName, e.getStackTrace());
			return null;
		}
		return allSupplierAccessOptions;
	}
}