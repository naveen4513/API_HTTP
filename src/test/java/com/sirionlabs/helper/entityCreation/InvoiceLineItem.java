package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.config.ConfigureConstantFields;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceLineItem {
	private final static Logger logger = LoggerFactory.getLogger(InvoiceLineItem.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createInvoiceLineItem() {
		return createInvoiceLineItem("default", false);
	}

	public static String createInvoiceLineItem(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity Invoice Line Item. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemExtraFieldsFileName");
			return createInvoiceLineItem(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching Invoice Line Item Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createInvoiceLineItem(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createInvoiceLineItem(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", false);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createInvoiceLineItem(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                           String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Invoice Line Item using Section {}", sectionName);

			createResponse = createEntityObj.create("invoice line item", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating Invoice Line Item using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createInvoiceLineItem(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
											   String sectionName, String startDate,String endDate,String invoiceDate) {
		String createResponse = null;
		try {
			String invoiceLineItem = "invoice line item";
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Invoice Line Item using Section {}", sectionName);

			String createPayload = createEntityObj.getCreatePayload(invoiceLineItem,true,false);

			JSONObject createPayloadJson = new JSONObject(createPayload);
			createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceStartDate").put("values",startDate);
			createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceEndDate").put("values",endDate);
			createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceDate").put("values",invoiceDate);
			createPayload = createPayloadJson.toString();
			Create create = new Create();
			create.hitCreate(invoiceLineItem,createPayload);

			createResponse = create.getCreateJsonStr();

		} catch (Exception e) {
			logger.error("Exception while Creating Invoice Line Item using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}

}
