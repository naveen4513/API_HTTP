package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestPurchaseOrderListing {

	private final static Logger logger = LoggerFactory.getLogger(TestPurchaseOrderListing.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static Integer poListId = -1;
	private static Integer paginationListDataSize = 20;
	private static Integer fieldsValidationListDataOffset = 0;
	private static Integer fieldsValidationListDataSize = 20;
	private static Integer maxNoOfRecordsToValidate = 5;
	private static Integer poEntityTypeId = -1;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderListingTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderListingTestConfigFileName");
		poListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "purchase orders", "entity_url_id"));

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "paginationListDataSize");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			paginationListDataSize = Integer.parseInt(temp);

		poEntityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldsValidationListDataOffset");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			fieldsValidationListDataOffset = Integer.parseInt(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldsValidationListDataSize");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			fieldsValidationListDataSize = Integer.parseInt(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxRecordstoValidate");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			maxNoOfRecordsToValidate = Integer.parseInt(temp.trim());
	}

	@Test(priority = 0,enabled = false)
	public void testPurchaseOrderListingPagination() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Purchase Order Listing Pagination");

			//Validate Listing Pagination
			ListDataHelper.verifyListingPagination(poEntityTypeId, poListId, paginationListDataSize, csAssert);
		} catch (Exception e) {
			logger.error("Exception while Validating Purchase Order Pagination. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Validating Purchase Order Pagination. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(priority = 1)
	public void testPurchaseOrderListingDataFields() {
		CustomAssert csAssert = new CustomAssert();

		try {
			//Validate Fields of List Data
			List<String> listDataFieldsToTest = getFieldsToTest();
			if (listDataFieldsToTest.size() > 0) {
				Map<String, String> fieldsShowPageObjectMap = getFieldShowPageObjectMap(listDataFieldsToTest);

				if (fieldsShowPageObjectMap.size() > 0) {
					String expectedDateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "showPageExpectedDateFormat");

					ListDataHelper.verifyListingRecordsData(poEntityTypeId, poListId, fieldsValidationListDataOffset, fieldsValidationListDataSize, maxNoOfRecordsToValidate,
							listDataFieldsToTest, fieldsShowPageObjectMap, expectedDateFormat, csAssert);
				} else {
					throw new SkipException("Couldn't get List Data Fields and Show Page Object Map. Hence skipping test.");
				}
			} else {
				throw new SkipException("Couldn't get List Data Fields to Test for Purchase Order. Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Validating Purchase Order Listing Data Fields. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Validating Purchase Order Listing Data Fields. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private List<String> getFieldsToTest() {
		List<String> fieldsToTest = new ArrayList<>();

		try {
			String allFields[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldsToTest").trim().split(Pattern.quote(","));

			for (String field : allFields) {
				fieldsToTest.add(field.trim());
			}
		} catch (Exception e) {
			logger.error("Exception while getting List Data Fields to test. {}", e.getMessage());
		}
		return fieldsToTest;
	}

	private Map<String, String> getFieldShowPageObjectMap(List<String> fieldsToTest) {
		Map<String, String> fieldShowPageObjectMap = new HashMap<>();

		try {
			List<String> allFieldsToTest = new ArrayList<>();
			allFieldsToTest.addAll(fieldsToTest);

			for (String field : allFieldsToTest) {
				if (ParseConfigFile.hasProperty(configFilePath, configFileName, "fieldShowPageObjectMapping", field)) {
					fieldShowPageObjectMap.put(field, ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldShowPageObjectMapping", field));
				} else {
					logger.error("Couldn't find Show Page Object Mapping for Field {}. Hence removing it.", field);
					fieldsToTest.remove(field);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting List Data Fields and Show Page Object Map. {}", e.getMessage());
		}
		return fieldShowPageObjectMap;
	}
}
