package com.sirionlabs.helper.invoice;

import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class InvoicePricingHelper {

	private final static Logger logger = LoggerFactory.getLogger(InvoicePricingHelper.class);
	private String configFilePath;
	private String configFileName;
	private static Integer templateId = -1;
	private static Integer serviceDataEntityTypeId = -1;
	private static Long pricingSchedulerTimeOut = 1200000L;
	private static Long pollingTime = 5000L;

	public InvoicePricingHelper() {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFileName");

		if (templateId == -1)
			setTemplateId();

		if (serviceDataEntityTypeId == -1)
			setEntityTypeId();
	}

	private void setTemplateId() {
		try {
			templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateId"));
		} catch (Exception e) {
			logger.error("Exception while setting Template Id for Invoice Pricing Download. {}", e.getMessage());
		}
	}

	private void setEntityTypeId() {
		try {
			serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
		} catch (Exception e) {
			logger.error("Exception while setting Service Data Entity Type Id. {}", e.getMessage());
		}
	}

	public Boolean downloadPricingTemplate(String outputFilePath, String outputFileName, Integer serviceDataId) {
		return BulkTemplate.downloadPricingTemplate(outputFilePath, outputFileName, templateId, serviceDataEntityTypeId, serviceDataId.toString());
	}

	public Boolean downloadPricingTemplate(String outputFilePath, String outputFileName, Integer serviceDataId,Map<String,String> formData) {
		return BulkTemplate.downloadPricingTemplate(outputFilePath, outputFileName, templateId, serviceDataEntityTypeId, serviceDataId.toString(),formData);
	}

	public Boolean downloadPricingTemplateForMultipleServiceDataRecords(String outputFilePath, String outputFileName, List<Integer> serviceDataIDList) {
		Boolean templateDownloaded = false;
		try {
			String entityIds = "";

			for (Integer id : serviceDataIDList) {
				entityIds += id + ",";
			}

			entityIds = entityIds.substring(0, entityIds.length() - 1);
			templateDownloaded = BulkTemplate.downloadPricingTemplate(outputFilePath, outputFileName, templateId, serviceDataEntityTypeId, entityIds);
		} catch (Exception e) {
			logger.error("Exception while downloading pricing template at Location {}. {}", outputFilePath + "/" + outputFileName, e.getStackTrace());
		}
		return templateDownloaded;
	}

	public String uploadPricing(String filePath, String fileName) {
		return BulkTemplate.uploadPricingTemplate(filePath, fileName, serviceDataEntityTypeId, templateId);
	}

	public Boolean editPricingTemplateMultipleRows(String filePath, String fileName, String sheetName, Map<Integer, Map<Integer, Object>> columnNumAndValueMap) {
//		return XLSUtils.editMultipleRowsData(filePath, fileName, sheetName, columnNumAndValueMap);
		return XLSUtils.editMultRowsDataAccToUpdVal(filePath, fileName, sheetName, columnNumAndValueMap);
	}

	public Boolean editPricingTemplate(String filePath, String fileName, String sectionName, Map<Integer, Object> columnNumAndValueMap) {
		try {
			String sheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "sheetName");
			Integer rowNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "startingRowNum"));

			return XLSUtils.editRowData(filePath, fileName, sheetName, rowNo, columnNumAndValueMap);
		} catch (Exception e) {
			logger.error("Exception while getting details for editing Pricing Template. {}", e.getMessage());
			return false;
		}
	}

	public Boolean editPricingTemplate(String filePath, String fileName, String sheetName, Integer rowNum, Map<Integer, Object> columnNumAndValueMap) {
		return XLSUtils.editRowData(filePath, fileName, sheetName, rowNum, columnNumAndValueMap);
	}

	public Boolean downloadAndEditPricingFile(String pricingTemplateFilePath,String templateFileName,String flowsConfigFilePath,String flowsConfigFileName,String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj) {
		Boolean pricingFile = false;
		try {
			//Download Pricing Template
			logger.info("Downloading Pricing Template for Flow [{}]", flowToTest);
			if (!pricingObj.downloadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataId)) {
				logger.error("Pricing Template Download failed for Flow [{}].", flowToTest);
				return false;
			}

			//Edit Pricing Templatei
			logger.info("Editing Pricing Template for Flow [{}]", flowToTest);

			String sheetName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "pricingstemplatesheetname");
			Integer totalRowsToEdit = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
					"numberofrowstoedit"));
			Integer startingRowNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
					"startingrownum"));
			Integer volumeColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
					"volumecolumnnum"));
			Integer rateColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
					"ratecolumnnum"));
			List<String> volumeColumnValues = this.getIdList(flowsConfigFilePath, flowsConfigFileName, flowToTest, "volumecolumnvalues");
			List<String> rateColumnValues = this.getIdList(flowsConfigFilePath, flowsConfigFileName, flowToTest, "ratecolumnvalues");

			int count = 0;
			for (int rowNum = startingRowNum; rowNum < (startingRowNum + totalRowsToEdit); rowNum++) {
				Map<Integer, Object> columnNumAndValueMap = new HashMap<>();
				columnNumAndValueMap.put(volumeColumnNum, volumeColumnValues.get(count));
				columnNumAndValueMap.put(rateColumnNum, rateColumnValues.get(count));

				Boolean isSuccess = pricingObj.editPricingTemplate(pricingTemplateFilePath, templateFileName, sheetName, rowNum, columnNumAndValueMap);

				count++;
				if (!isSuccess) {
					logger.error("Pricing Template Editing Failed for Flow [{}].", flowToTest);
					pricingFile = false;
					break;
				} else
					pricingFile = true;
			}
		} catch (Exception e) {
			logger.error("Exception while getting Pricing Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
		}
		return pricingFile;
	}

	private List<String> getIdList(String configFilePath, String configFileName, String sectionName, String propertyName) throws ConfigurationException {

		String value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName);
		List<String> idList = new ArrayList<>();

		if (!value.trim().equalsIgnoreCase("")) {
			String ids[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName).split(",");

			for (String id : ids)
				idList.add(id.trim());
		}
		return idList;
	}

	/*
	This method will return the status of Pricing Scheduler as String.
	Possible Values are 'Pass', 'Fail', 'Skip'.
	'Pass' specifies that pricing scheduler completed and records processed successfully.
	'Fail' specifies that pricing scheduler failed
	'Skip' specifies that pricing scheduler didn't finish within time.
	 */
	public String waitForPricingScheduler(String flowToTest, List<Integer> oldIds) {
		String result = "pass";
		logger.info("Waiting for Pricing Scheduler to Complete for Flow [{}].", flowToTest);
		try {
			logger.info("Time Out for Pricing Scheduler is {} milliseconds", pricingSchedulerTimeOut);
			long timeSpent = 0;
			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			logger.info("Getting Task Id of Pricing Upload Job");
			int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), oldIds);

			if (newTaskId != -1) {
				Boolean taskCompleted = false;
				logger.info("Checking if Pricing Upload Task has completed or not.");

				while (timeSpent < pricingSchedulerTimeOut) {
					logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
					Thread.sleep(pollingTime);

					logger.info("Hitting Fetch API.");
					fetchObj.hitFetch();
					logger.info("Getting Status of Pricing Upload Task.");
					String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
					if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
						taskCompleted = true;
						logger.info("Pricing Upload Task Completed. ");
						logger.info("Checking if Pricing Upload Task failed or not.");
						if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
							result = "fail";

						break;
					} else {
						timeSpent += pollingTime;
						logger.info("Pricing Upload Task is not finished yet.");
					}
				}
				if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
					//Task didn't complete within given time.
					result = "skip";
				}
			} else {
				logger.info("Couldn't get Pricing Upload Task Job Id. Hence waiting for Task Time Out i.e. {}", pricingSchedulerTimeOut);
				Thread.sleep(pricingSchedulerTimeOut);
			}
		} catch (Exception e) {
			logger.error("Exception while Waiting for Pricing Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
			result = "fail";
		}
		return result;
	}

	// this function will edit the ARC/RRC sheet based on the map created in getValuesMapForArcRrcSheet
	public Boolean editPricingFileForARCRRC(String pricingTemplateFilePath,String templateFileName, String flowToTest,String ARCRRCSheetNameInXLSXFile,Map<Integer, Map<Integer, Object>> arcValuesMap, Integer serviceDataId) {
		Boolean pricingFile = false;
		try {

			boolean editTemplate = editPricingTemplateMultipleRows(pricingTemplateFilePath, templateFileName, ARCRRCSheetNameInXLSXFile,
					arcValuesMap);

			if (editTemplate == true) {
				return true;
			} else {
				logger.error("Error While Updating the Pricing Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
				return false;
			}


		} catch (Exception e) {
			logger.error("Exception while getting ARC/RRC Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
		}
		return pricingFile;
	}

	public Boolean uploadPricingFile(String invoiceFlowsConfigFilePath, String invoiceFlowsConfigFileName,
									 String flowToTest, Integer serviceDataId,
									 String pricingTemplateFilePath,
									 Boolean dataSheetToBeUpdated,
									 String dataSheetName,
									 int dataSheetRowNumberToBeEdited,
									 HashMap<Integer,Object> dataSheetColumnDataMap,
									 CustomAssert csAssert) {

		Boolean uploadStatus = true;
		try {
			//Kill All Scheduler Tasks if Flag is On.
			UserTasksHelper.removeAllTasks();

			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

			InvoicePricingHelper pricingObj = new InvoicePricingHelper();

			String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
					"pricingstemplatefilename");

			Boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, csAssert);

			if (pricingFile) {

				if(dataSheetToBeUpdated){

					Boolean status = XLSUtils.editRowData(pricingTemplateFilePath,pricingTemplateFileName,dataSheetName,dataSheetRowNumberToBeEdited,dataSheetColumnDataMap);
					if(!status){
						logger.error("Error while updating data sheet in pricing file");
					}

				}

				String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

				if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {

					//Wait for Pricing Scheduler to Complete
					String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

					if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
						logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
								flowToTest + "]");

						uploadStatus = false;

					} else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
						logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);

						logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
								"Hence failing Flow [" + flowToTest + "]");

						uploadStatus = false;
					}
				} else {
					csAssert.assertTrue(false, "Error while pricing upload");
				}
			} else {
				csAssert.assertTrue(false, "Error while pricing file download and edit");
				uploadStatus = false;
			}

		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while doing pricing upload");
			uploadStatus = false;

		}
		return uploadStatus;
	}

	// this function will edit the Pricing sheet based on the map created in getValuesMapForArcRrcSheet
	public Boolean editPricingFileForPricing(String pricingTemplateFilePath,String templateFileName, String flowToTest,String sheetNameInXLSXFile,Map<Integer, Map<Integer, Object>> pricingValuesMap, Integer serviceDataId) {
		Boolean pricingFile = false;
		try {

			boolean editTemplate = editPricingTemplateMultipleRows(pricingTemplateFilePath, templateFileName, sheetNameInXLSXFile,
					pricingValuesMap);

			if (editTemplate == true) {
				return true;
			} else {
				logger.error("Error While Updating the Pricing Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
				return false;
			}


		} catch (Exception e) {
			logger.error("Exception while getting " + sheetNameInXLSXFile  + "using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
		}
		return pricingFile;
	}

	public Boolean editPricingFilePricingSheet(String pricingTemplateFilePath, String pricingTemplateFileName,
											   Integer serviceDataId,String sheetName,int dataSheetRowNumberToBeEdited,
											   HashMap<Integer,Integer> dataSheetColumnDataMap,
											   CustomAssert customAssert) {

		Boolean uploadStatus = true;
		InvoicePricingHelper pricingObj = new InvoicePricingHelper();
		try {

			logger.info("Downloading Pricing Template for Flow ");
			if (!pricingObj.downloadPricingTemplate(pricingTemplateFilePath, pricingTemplateFileName, serviceDataId)) {
				logger.error("Pricing Template Download failed");
				return false;
			}

			int columnNo;
			int columnValue;
			Boolean excelUpdateStatus;

			for(Map.Entry<Integer,Integer> entry : dataSheetColumnDataMap.entrySet()){

				columnNo = entry.getKey();
				columnValue = entry.getValue();

				excelUpdateStatus = XLSUtils.updateColumnValue(pricingTemplateFilePath,pricingTemplateFileName,sheetName,dataSheetRowNumberToBeEdited,columnNo,columnValue);

				if(!excelUpdateStatus){
					customAssert.assertTrue(false,"Error while updating excel for column "+ columnNo);
				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while editing pricing sheet");
			uploadStatus = false;

		}
		return uploadStatus;
	}

	public Boolean editPricingFilePricingSheet(String pricingTemplateFilePath, String pricingTemplateFileName,
											   String sheetName,
											   int startRow,
											   int endRow,
											   HashMap<Integer,Integer> dataSheetColumnDataMap,
											   CustomAssert customAssert) {

		Boolean uploadStatus = true;

		try {

			int columnNo;
			int columnValue;
			int dataSheetRowNumberToBeEdited;
			Boolean excelUpdateStatus;

			for(int i=startRow;i<endRow;i++) {

				dataSheetRowNumberToBeEdited = i;

				for (Map.Entry<Integer, Integer> entry : dataSheetColumnDataMap.entrySet()) {

					columnNo = entry.getKey();
					columnValue = entry.getValue();

					excelUpdateStatus = XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, dataSheetRowNumberToBeEdited, columnNo, columnValue);

					if (!excelUpdateStatus) {
						customAssert.assertTrue(false, "Error while updating excel for column " + columnNo);
					}
				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while editing pricing sheet");
			uploadStatus = false;

		}
		return uploadStatus;
	}

	public Boolean uploadPricingFileWithoutEdit(String pricingTemplateFilePath,
												String pricingTemplateFileName,
												String flowToTest,
												CustomAssert customAssert) {

		Boolean uploadStatus = true;
		try {
			//Kill All Scheduler Tasks if Flag is On.
//			UserTasksHelper.removeAllTasks();

			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

			InvoicePricingHelper pricingObj = new InvoicePricingHelper();


			String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

			if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {

				//Wait for Pricing Scheduler to Complete
				String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

				if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
					logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
					customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
							flowToTest + "]");

					uploadStatus = false;

				} else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
					logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);

					logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
					customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
							"Hence failing Flow [" + flowToTest + "]");

					uploadStatus = false;
				}
			}
			else {
				customAssert.assertTrue(false, "Error while pricing upload");
				uploadStatus = false;

			}


		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while doing pricing upload");
			uploadStatus = false;

		}
		return uploadStatus;
	}

	public Boolean updateARCSheetPricingTemplate(String pricingTemplateFilePath, String pricingTemplateFileName,String type,int startMonth,int year,int endRow,CustomAssert customAssert){

		Boolean updateStatus = true;
		try {
			String sheetName = "ARC RRC";

			List<String> excelDataFirstRowData = XLSUtils.getExcelDataOfOneRow(pricingTemplateFilePath, pricingTemplateFileName, sheetName, 7);
			List<String> excelDataColumnIds = XLSUtils.getExcelDataOfOneRow(pricingTemplateFilePath, pricingTemplateFileName, sheetName, 2);
			int lowerVolume = 100;
			int upperVolume = 1000;
			int rate = 10;
//			String type = "ARC";

			String columnId;
			int serviceDataIdColumnNum = 2;
			int serviceDataNameColumnNum = 3;
			int serviceIdClientColumnNum = 4;
			int lvColumnNum = 7;
			int uvColumnNum = 8;
			int rateColumnNum = 9;
			int typeColumnNum = 10;
			int serviceStartDateColumnNum = 5;
			int serviceEndDateColumnNum = 6;
//			int year = 2018;

			for (int i = 0; i < excelDataColumnIds.size(); i++) {
				columnId = excelDataColumnIds.get(i);

				if (columnId.equalsIgnoreCase("8061")) {
					lvColumnNum = i;
				} else if (columnId.equalsIgnoreCase("8062")) {
					uvColumnNum = i;
				} else if (columnId.equalsIgnoreCase("8063")) {
					rateColumnNum = i;
				} else if (columnId.equalsIgnoreCase("8064")) {
					typeColumnNum = i;
				} else if (columnId.equalsIgnoreCase("11858")) {
					serviceStartDateColumnNum = i;
				} else if (columnId.equalsIgnoreCase("11859")) {
					serviceEndDateColumnNum = i;
				}
			}

			int serialNumber = 1;
//			int startMonth = 3;
			String startDate;
			String endDate;

			int startRow = 6;
//        int endRow = 13;
			String dateFormat = "MM/dd/yyyy";
			for (int i = startRow; i < endRow; i++) {

				startDate = DateUtils.getMonthStartDateInMMDDFormat(startMonth);
				endDate = DateUtils.getMonthEndDateInMMDDFormat(startMonth, year);

				startDate = startDate + "/" + year;
				endDate = endDate + "/" + year;

				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, 0, serialNumber);
				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, 1, 1);
				XLSUtils.updateColumnValueDate(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceStartDateColumnNum,dateFormat, startDate);
				XLSUtils.updateColumnValueDate(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceEndDateColumnNum,dateFormat, endDate);

				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, lvColumnNum, lowerVolume);
				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, uvColumnNum, upperVolume);
				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, typeColumnNum, type);
				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, rateColumnNum, rate);

				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceDataIdColumnNum, excelDataFirstRowData.get(serviceDataIdColumnNum));
				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceDataNameColumnNum, excelDataFirstRowData.get(serviceDataNameColumnNum));
				XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceIdClientColumnNum, excelDataFirstRowData.get(serviceIdClientColumnNum));


				startMonth = startMonth + 1;
				serialNumber = serialNumber + 1;
			}

		}catch (Exception e){
			logger.error("Exception while updating ARC Sheet of Pricing Template");
			customAssert.assertTrue(false,"Exception while updating ARC Sheet of Pricing Template");
			updateStatus = false;
		}
		return updateStatus;
	}

	public void updatePricingSheet(String filePath,String fileName,int startRow,int endRow,Map<Integer,Object> colValueMap,CustomAssert customAssert){


		try{
			String sheetName = "Pricing";

			for(int i = startRow;i<endRow;i++){
				XLSUtils.editRowData(filePath,fileName,sheetName,i,colValueMap);
			}


		}catch (Exception e){
			logger.error("Exception while updating pricing Sheet");
			customAssert.assertTrue(false,"Exception while updating pricing Sheet");
		}
	}

	public void updateARCSheet(String filePath,String fileName,int startRow,int endRow,Map<Integer,Object> colValueMap,CustomAssert customAssert){


		try{
			String sheetName = "ARC RRC";

			for(int i = startRow;i<endRow;i++){
				XLSUtils.editRowData(filePath,fileName,sheetName,i,colValueMap);
			}


		}catch (Exception e){
			logger.error("Exception while updating pricing Sheet");
			customAssert.assertTrue(false,"Exception while updating pricing Sheet");
		}
	}


}
