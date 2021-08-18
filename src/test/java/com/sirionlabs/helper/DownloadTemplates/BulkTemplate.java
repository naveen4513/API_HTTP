package com.sirionlabs.helper.DownloadTemplates;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.GetEntityId;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkTemplate {

	private final static Logger logger = LoggerFactory.getLogger(BulkTemplate.class);

	public static Boolean downloadBulkCreateTemplate(String outputFilePath, String outputFileName, String queryString) {
		Boolean templateDownloaded = false;
		try {
			logger.info("Downloading Bulk Create Template. File location : {}", outputFilePath + "/" + outputFileName);
			Download downloadObj = new Download();
			templateDownloaded = downloadObj.hitDownload(outputFilePath, outputFileName, queryString);
		} catch (Exception e) {
			logger.error("Exception while downloading Bulk Create Template at Location [{}] using QueryString {}. {}",
					outputFilePath + "/" + outputFileName, queryString, e.getStackTrace());
		}
		return templateDownloaded;
	}

	public static Boolean downloadBulkCreateTemplate(String outputFilePath, String outputFileName, int templateId, int parentEntityTypeId, int parentId) {
		Boolean templateDownloaded = false;
		try {
			logger.info("Downloading Bulk Create Template. File location : {}", outputFilePath + "/" + outputFileName);
			Download downloadObj = new Download();
			templateDownloaded = downloadObj.hitDownload(outputFilePath, outputFileName, templateId, parentEntityTypeId, parentId);
		} catch (Exception e) {
			logger.error("Exception while downloading Bulk Create Template at Location [{}] using Template Id {}, ParentEntityTypeId {} and Parent Id {}. {}",
					outputFilePath + "/" + outputFileName, templateId, parentEntityTypeId, parentId, e.getStackTrace());
		}
		return templateDownloaded;
	}

	public static String uploadBulkCreateTemplate(String templateFilePath, String templateFileName, int parentEntityTypeId, int parentId, int entityTypeId, int templateId) {
		String response = null;
		try {
			logger.info("Uploading Bulk Create Template from Location [{}] using EntityTypeId {} and Template Id {}", templateFilePath + "/" + templateFileName,
					entityTypeId, templateId);

			Map<String, String> payloadMap = new HashMap<>();
			payloadMap.put("parentEntityTypeId", Integer.toString(parentEntityTypeId));
			payloadMap.put("parentEntityId", Integer.toString(parentId));
			payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
			payloadMap.put("upload", "submit");

			UploadBulkData uploadObj = new UploadBulkData();
			uploadObj.hitUploadBulkData(entityTypeId, templateId, templateFilePath, templateFileName, payloadMap);
			response = uploadObj.getUploadBulkDataJsonStr();
		} catch (Exception e) {
			logger.error("Exception while Uploading Bulk Create Template from Location [{}] using Template Id {} and EntityTypeId {}. {}",
					templateFilePath + "/" + templateFileName, templateId, entityTypeId, e.getStackTrace());
		}
		return response;
	}

	public static Boolean downloadBulkUpdateTemplate(String outputFilePath, String outputFileName, int templateId, int entityTypeId, String entityIds) {
		Boolean templateDownloaded = false;
		try {
			logger.info("Downloading Bulk Update Template. File location : {}", outputFilePath + "/" + outputFileName);
			Download downloadObj = new Download();
			templateDownloaded = downloadObj.hitDownload(outputFilePath, outputFileName, templateId, entityTypeId, entityIds);
		} catch (Exception e) {
			logger.error("Exception while downloading Bulk Update Template at Location [{}] using Template Id {}, EntityTypeId {} and EntityIds {}. {}",
					outputFilePath + "/" + outputFileName, templateId, entityTypeId, entityIds, e.getStackTrace());
		}
		return templateDownloaded;
	}

	public static String uploadBulkUpdateTemplate(String templateFilePath, String templateFileName, int entityTypeId, int templateId) {
		String response = null;
		try {
			logger.info("Uploading Bulk Update Template from Location [{}] using EntityTypeId {} and Template Id {}", templateFilePath + "/" + templateFileName,
					entityTypeId, templateId);

			Map<String, String> payloadMap = new HashMap<>();
			payloadMap.put("entityTypeId", Integer.toString(entityTypeId));
			payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
			payloadMap.put("upload", "submit");

			UploadBulkData uploadObj = new UploadBulkData();
			uploadObj.hitUploadBulkData(entityTypeId, templateId, templateFilePath, templateFileName, payloadMap);
			response = uploadObj.getUploadBulkDataJsonStr();
		} catch (Exception e) {
			logger.error("Exception while Uploading Bulk Update Template from Location [{}] using Template Id {} and EntityTypeId {}. {}",
					templateFilePath + "/" + templateFileName, templateId, entityTypeId, e.getStackTrace());
		}
		return response;
	}

	public static Boolean downloadPricingTemplate(String outputFilePath, String outputFileName, int templateId, int entityTypeId, String entityIds) {
		Boolean templateDownloaded = false;
		try {
			logger.info("Downloading Pricing Template. File location : {}", outputFilePath + "/" + outputFileName);
			Download downloadObj = new Download();
			templateDownloaded = downloadObj.hitDownload(outputFilePath, outputFileName, templateId, entityTypeId, entityIds);

		} catch (Exception e) {
			logger.error("Exception while downloading Pricing Template at Location [{}] using Template Id {}, EntityTypeId {} and EntityIds {}. {}",
					outputFilePath + "/" + outputFileName, templateId, entityTypeId, entityIds, e.getStackTrace());
		}
		return templateDownloaded;
	}

	public static Boolean downloadPricingTemplate(String outputFilePath, String outputFileName, int templateId, int entityTypeId, String entityIds,Map<String, String> formParam) {
		Boolean templateDownloaded = false;
		try {
			logger.info("Downloading Pricing Template. File location : {}", outputFilePath + "/" + outputFileName);
			Download downloadObj = new Download();
			templateDownloaded = downloadObj.hitDownload(outputFilePath, outputFileName, templateId, entityTypeId, entityIds);

		} catch (Exception e) {
			logger.error("Exception while downloading Pricing Template at Location [{}] using Template Id {}, EntityTypeId {} and EntityIds {}. {}",
					outputFilePath + "/" + outputFileName, templateId, entityTypeId, entityIds, e.getStackTrace());
		}
		return templateDownloaded;
	}

	public static String uploadPricingTemplate(String templateFilePath, String templateFileName, int entityTypeId, int templateId) {
		String response = null;
		try {
			logger.info("Uploading Pricing Template from Location [{}] using EntityTypeId {} and Template Id {}", templateFilePath + "/" + templateFileName,
					entityTypeId, templateId);

			Map<String, String> payloadMap = new HashMap<>();
			payloadMap.put("entityTypeId", Integer.toString(entityTypeId));
			payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
			payloadMap.put("upload", "submit");

			UploadBulkData uploadObj = new UploadBulkData();
			uploadObj.hitUploadBulkData(entityTypeId, templateId, templateFilePath, templateFileName, payloadMap);
			response = uploadObj.getUploadBulkDataJsonStr();
		} catch (Exception e) {
			logger.error("Exception while Uploading Pricing Template from Location [{}] using Template Id {} and EntityTypeId {}. {}",
					templateFilePath + "/" + templateFileName, templateId, entityTypeId, e.getStackTrace());
		}
		return response;
	}

	// here parentEntityTypeId belongs to contract Type Id and parentEntityId is Contract Id  entityTypeId is the type Id of Forecast Entity
	public static String uploadForecastTemplate(String templateFilePath, String templateFileName, int parentEntityTypeId, int parentEntityId, int entityTypeId, int templateId) {
		String response = null;
		try {
			logger.info("Uploading Forecast Template from Location [{}] using EntityTypeId {} and Template Id {}", templateFilePath + "/" + templateFileName,
					entityTypeId, templateId);

			Map<String, String> payloadMap = new HashMap<>();
			payloadMap.put("parentEntityTypeId", Integer.toString(parentEntityTypeId));
			payloadMap.put("parentEntityId", Integer.toString(parentEntityId));
			payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
			//payloadMap.put("upload", "submit");

			UploadBulkData uploadObj = new UploadBulkData();
			uploadObj.hitUploadBulkData(entityTypeId, templateId, templateFilePath, templateFileName, payloadMap);
			response = uploadObj.getUploadBulkDataJsonStr();
		} catch (Exception e) {
			logger.error("Exception while Uploading Forecast Template from Location [{}] using Template Id {} and EntityTypeId {}. {}",
					templateFilePath + "/" + templateFileName, templateId, entityTypeId, e.getStackTrace());
		}
		return response;
	}

	public static String getBulkCreateDataSheetForEntity(String entityName) {
		String dataSheetName = "";

		switch (entityName) {
			case "service data":
				dataSheetName = "Service Data";
				break;

			case "actions":
				dataSheetName = "Action";
				break;

			case "invoices":
				dataSheetName = "Invoice";
				break;

			case "invoice line item":
				dataSheetName = "Invoice Line Item";
				break;
		}

		return dataSheetName;
	}

	public static int getBulkUpdateTemplateIdForEntity(String entityName) {
		int templateId = -1;

		switch (entityName) {
			case "contracts":
				templateId = 1025;
				break;

			case "obligations":
				templateId = 1019;
				break;

			case "child obligations":
				templateId = 1022;
				break;

			case "service levels":
				templateId = 1026;
				break;

			case "child service levels":
				templateId = 1021;
				break;

			case "disputes":
				templateId = 1018;
				break;

			case "consumptions":
				templateId = 1014;
				break;
		}

		return templateId;
	}

	public static String getBulkUpdateDataSheetForEntity(String entityName) {
		String dataSheetName = "";

		switch (entityName) {
			case "contracts":
				dataSheetName = "Contract";
				break;

			case "obligations":
				dataSheetName = "Obligation";
				break;

			case "child obligations":
				dataSheetName = "Child Obligation";
				break;

			case "service levels":
				dataSheetName = "Sla";
				break;

			case "child service levels":
				dataSheetName = "Child Sla";
				break;

			case "disputes":
				dataSheetName = "Dispute";
				break;

			case "consumptions":
				dataSheetName = "Consumption";
				break;
		}

		return dataSheetName;
	}

	public static String getBulkUpdateIdColumnHeaderIdForEntity(String entityName) {
		String columnHeaderId = null;

		switch (entityName) {
			case "contracts":
				columnHeaderId = "98";
				break;

			case "child obligations":
				columnHeaderId = "1045";
				break;

			case "child service levels":
				columnHeaderId = "1124";
				break;

			case "disputes":
				columnHeaderId = "11179";
				break;

			case "obligations":
				columnHeaderId = "350";
				break;

			case "consumptions":
				columnHeaderId = "11878";
				break;

			case "service levels":
				columnHeaderId = "285";
				break;
		}

		return columnHeaderId;
	}

	public static Integer getRecordIdFromBulkUpdateTemplateForEntity(String templatePath, String templateName, String entityName, int rowNo) {
		try {
			String idColumnHeaderId = getBulkUpdateIdColumnHeaderIdForEntity(entityName);
			String dataSheetName = getBulkUpdateDataSheetForEntity(entityName);

			List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, templateName, dataSheetName, 2);

			if (allHeaderIds == null) {
				logger.error("Couldn't get all header ids from Template [{}] for Entity {}", templatePath + "/" + templateName, entityName);
				return null;
			}

			int idColumnNo = allHeaderIds.indexOf(idColumnHeaderId);

			if (idColumnNo == -1) {
				throw new SkipException("Couldn't get Column No of Id Field for Entity " + entityName);
			}

			String idValueInTemplate = XLSUtils.getExcelDataOfOneRow(templatePath, templateName, dataSheetName, rowNo).get(idColumnNo);

			GetEntityId getEntityIdObj = new GetEntityId();
			String recordId = getEntityIdObj.hitGetEntityId(entityName, idValueInTemplate);

			if (recordId == null || recordId.equalsIgnoreCase("")) {
				logger.error("Couldn't get Record Id from Short Code Id " + idValueInTemplate + " for Entity " + entityName);
				return null;
			}

			return Integer.parseInt(recordId);
		} catch (Exception e) {
			logger.error("Exception while Getting Record Id from Bulk Update Template for Entity {} and Row No {}. {}", entityName, rowNo, e.getMessage());
		}

		return null;
	}

	public static Long getBulkUpdateMaxRecordsLimitForEntity(String entityName) {
		Long maxRecordsLimit = null;

		switch (entityName) {
			case "contracts":
			case "obligations":
			case "child obligations":
			case "service levels":
			case "child service levels":
			case "disputes":
				maxRecordsLimit = 1000L;
				break;

			case "consumptions":
				maxRecordsLimit = 1000L;
				break;
		}

		return maxRecordsLimit;
	}

	public static int getBulkUpdateHeaderIdForEntityId(String entityName) {
		int headerId = -1;

		switch (entityName) {
			case "contracts":
				headerId = 98;
				break;

			case "obligations":
				headerId = 350;
				break;

			case "child obligations":
				headerId = 1045;
				break;

			case "service levels":
				headerId = 285;
				break;

			case "child service levels":
				headerId = 1124;
				break;

			case "disputes":
				headerId = 11179;
				break;

			case "consumptions":
				headerId = 11878;
				break;
		}

		return headerId;
	}
}
