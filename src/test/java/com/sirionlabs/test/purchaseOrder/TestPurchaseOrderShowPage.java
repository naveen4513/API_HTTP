package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.download.DownloadCommunicationDocument;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestPurchaseOrderShowPage {

	private final static Logger logger = LoggerFactory.getLogger(TestPurchaseOrderShowPage.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static Integer poEntityTypeId = -1;
	private static Integer poListId = -1;
	private static Boolean deleteEntity = true;
	private static Integer purchaseOrderId = -1;
	private static Boolean preRequisitePass = false;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderShowPageTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderShowPageTestConfigFileName");

		poListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "purchase orders", "entity_url_id"));
		poEntityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");

		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity").trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	//This test covers TC-98648
	@Test
	public void testPurchaseOrderShowPageTabs() {
		CustomAssert csAssert = new CustomAssert();

		try {
			int maxRecordsToValidate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "show page sub tabs",
					"maxRecordsToValidate"));

			logger.info("Hitting Purchase Order Listing API.");
			ListRendererListData listDataObj = new ListRendererListData();
			listDataObj.hitListRendererListData(poListId);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				listDataObj.setListData(listDataResponse);
				List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();

				int idColumnNo = listDataObj.getColumnIdFromColumnName("name");
				int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, listData.size() - 1, maxRecordsToValidate);
				logger.info("Total Records to validate for Show Page Tabs: {}", randomNumbers.length);

				for (int number : randomNumbers) {
					int purchaseOrderId = Integer.parseInt(listData.get(number).get(idColumnNo).get("valueId"));

					logger.info("Hitting Show API for Purchase Order Id {}", purchaseOrderId);
					Show showObj = new Show();
					showObj.hitShow(poEntityTypeId, purchaseOrderId);
					String showResponse = showObj.getShowJsonStr();

					if (ParseJsonResponse.validJsonResponse(showResponse)) {
						logger.info("Validating if Show API Response contains Tabs General, Communication and Audit Log");

						JSONObject jsonObj = new JSONObject(showResponse);
						jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");

						if (!jsonObj.getString("type").trim().equalsIgnoreCase("tabs")) {
							throw new SkipException("Couldn't find data in LayoutComponent of type Tabs. Hence skipping test.");
						}

						JSONArray fieldsArr = jsonObj.getJSONArray("fields");
						Boolean generalTabFound, communicationTabFound, auditLogTabFound;
						generalTabFound = communicationTabFound = auditLogTabFound = false;


						for (int i = 0; i < fieldsArr.length(); i++) {
							String tabLabel = fieldsArr.getJSONObject(i).getString("label");

							if (Pattern.compile(Pattern.quote("GENERAL"), Pattern.CASE_INSENSITIVE).matcher(tabLabel.trim()).find())
								generalTabFound = true;
							else if (Pattern.compile(Pattern.quote("AUDIT LOG"), Pattern.CASE_INSENSITIVE).matcher(tabLabel.trim()).find())
								auditLogTabFound = true;
							else if (Pattern.compile(Pattern.quote("COMMUNICATION"), Pattern.CASE_INSENSITIVE).matcher(tabLabel.trim()).find())
								communicationTabFound = true;
						}

						if (!generalTabFound) {
							csAssert.assertTrue(false, "General Tab not found in Show API Response for Purchase Order Id " + purchaseOrderId);
						}

						if (!communicationTabFound) {
							csAssert.assertTrue(false, "Communication Tab not found in Show API Response for Purchase Order Id " + purchaseOrderId);
						}

						if (!auditLogTabFound) {
							csAssert.assertTrue(false, "Audit Log Tab not found in Show API Response for Purchase Order Id " + purchaseOrderId);
						}
					} else {
						csAssert.assertTrue(false, "Show API Response for Purchase Order Id " + purchaseOrderId + " is an Invalid JSON.");
					}
				}

			} else {
				csAssert.assertTrue(false, "Purchase Order List Data API Response is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Purchase Order Show Page Tabs. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while validating Purchase Order Show Page Tabs. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//This test covers TC-98655, TC-98656, TC-98657, TC-98658, TC-98659, TC-98661
	@Test(priority = 1)
	public void testPurchaseOrderCommunicationTab() {
		CustomAssert csAssert = new CustomAssert();

		try {
			setPreRequisite();

			if (preRequisitePass) {
				logger.info("Hitting TabListData API for Purchase Order Communication Tab.");
				Integer communicationTabId = TabListDataHelper.getIdForTab("purchase order communication");

				if (communicationTabId != -1) {
					TabListData tabListObj = new TabListData();
					String tabListDataResponse = tabListObj.hitTabListData(communicationTabId, poEntityTypeId, purchaseOrderId);

					if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
						List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

						if (listData.size() > 0) {
							String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "document file upload",
									"uploadFileName");

							//Verify TC-98655, TC-98657, TC-98658
							verifyUploadedDocumentsPresentInCommunicationTab(tabListDataResponse, listData, fileName, csAssert);

							//Verify TC-98656
							verifyCommentWhileCreatingPO(tabListDataResponse, listData, csAssert);

							//Verify TC-98659
							verifyDownloadingDocument(tabListDataResponse, listData, csAssert);

							//Verify TC-98661
							Integer tabListDataSize = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "communication tab",
									"tabListDataSize"));
							ListDataHelper.verifyTabListingPagination(poEntityTypeId, purchaseOrderId, communicationTabId, tabListDataSize, csAssert);
						} else {
							throw new SkipException("Couldn't get any list data in Communication Tab Response for Purchase Order Id " + purchaseOrderId +
									". Hence skipping test.");
						}
					} else {
						csAssert.assertTrue(false, "Tab List Data API Response for Purchase Order Communication Tab is an Invalid JSON.");
					}
				} else {
					throw new SkipException("Couldn't get Id for Purchase Order Communication Tab. Hence skipping test.");
				}
			} else {
				throw new SkipException("Pre-Requisite failed for Purchase Order Communication Tab. Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Purchase Order Communication Tab Cases. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(priority = 2)
	public void testPurchaseOrderAuditLogTab() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (preRequisitePass) {
				int auditLogTabId = TabListDataHelper.getIdForTab("audit log");

				if (auditLogTabId != -1) {
					//Verify TC-98675, TC-98677, TC-98678
					verifyAuditLogForNewlyCreatedRecord(auditLogTabId, csAssert);

					//Verify TC-98664, TC-98665, TC-98666
					logger.info("Hitting Edit Get API for Purchase Order Id {}.", purchaseOrderId);
					Edit editObj = new Edit();
					String editGetResponse = editObj.hitEdit("purchase orders", purchaseOrderId);

					if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
						//Verify Audit Log is Generated for only Requested By Field.
						verifyAuditLogsForRequestedBy(editGetResponse, auditLogTabId, csAssert);

						//Verify Audit Log is Generated for not changing anything and simply hitting Submit/Update button.
						verifyAuditLogsForSubmit(editGetResponse, auditLogTabId, csAssert);

						//Verify Audit Log Pagination
						verifyAuditLogTabPagination(auditLogTabId, csAssert);
					} else {
						csAssert.assertTrue(false, "Edit Get API Response for Purchase Order Id " + purchaseOrderId + " is an Invalid JSON.");
					}
				} else {
					throw new SkipException("Couldn't get Id for Audit Log Tab. Hence Skipping test.");
				}
			} else {
				throw new SkipException("Pre-Requisite failed for Purchase Order Audit Log Tab. Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Purchase Order Audit Log Tab Cases. " + e.getMessage());
		} finally {
			deleteRecords();
			csAssert.assertAll();
		}
	}

	private void setPreRequisite() {
		try {
			logger.info("*********************** Pre-Requisite Stage for Purchase Order Show Page Tests ***********************");
			//Upload Document File
			String filePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "document file upload", "uploadFilePath");
			String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "document file upload", "uploadFileName");
			String randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);
			String uploadResponse = DocumentHelper.uploadDocumentFile(filePath, fileName, randomKeyForDocumentFile);

			if (!(uploadResponse != null && uploadResponse.trim().startsWith("/data/") && uploadResponse.trim().contains(fileName))) {
				throw new SkipException("Couldn't upload Document File located at [" + filePath + "/" + fileName + "]. Hence skipping test.");
			}

			//Create Purchase Order
			String poCreateConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "po creation 1", "configFilePath");
			String poCreateConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "po creation 1", "configFileName");
			String poCreateExtraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "po creation 1",
					"extraFieldsConfigFileName");
			String sectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "po creation 1", "sectionName");
			updatePurchaseOrderConfigFile(poCreateConfigFilePath, poCreateExtraFieldsConfigFileName, sectionName, "random alpha string",
					randomKeyForDocumentFile);

			logger.info("Creating Purchase Order.");
			String createResponse = PurchaseOrder.createPurchaseOrder(poCreateConfigFilePath, poCreateConfigFileName, poCreateConfigFilePath,
					poCreateExtraFieldsConfigFileName, sectionName, true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

				if (!createStatus.trim().equalsIgnoreCase("success"))
					throw new SkipException("Couldn't create Purchase Order due to" + createStatus + ". Hence skipping test.");
			} else {
				throw new SkipException("Purchase Order Create API Response is an Invalid JSON. Hence skipping test.");
			}

			logger.info("Reverting Purchase Order Extra Fields Config changes.");
			updatePurchaseOrderConfigFile(poCreateConfigFilePath, poCreateExtraFieldsConfigFileName, sectionName, randomKeyForDocumentFile,
					"random alpha string");

			purchaseOrderId = CreateEntity.getNewEntityId(createResponse);

			if (purchaseOrderId != -1) {
				preRequisitePass = true;
			} else {
				logger.error("Couldn't get Id of Newly Created Purchase Order.");
			}

			logger.info("***********************************************************************************");
		} catch (Exception e) {
			logger.error("Exception while setting Pre-Requisites. {}", e.getMessage());
		}
	}

	//This method verifies TC-98655, TC-98657, TC-98658
	private void verifyUploadedDocumentsPresentInCommunicationTab(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData, String documentName,
	                                                              CustomAssert csAssert) {
		try {
			int documentColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "document");
			String documentValue = listData.get(0).get(documentColumnNo).get("value");

			String documentNameWithoutExtension = FileUtils.getFileNameWithoutExtension(documentName);
			String documentExtension = FileUtils.getFileExtension(documentName);

			if (documentValue == null || !documentValue.contains(documentNameWithoutExtension) || !documentValue.contains(documentExtension)) {
				csAssert.assertTrue(false, "Uploaded Document " + documentName + " is not present in Purchase Order Communication Tab Response.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating that uploaded documents are present in Communication tab. " + e.getMessage());
		}
	}

	//This method verifies TC-98656
	private void verifyCommentWhileCreatingPO(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData, CustomAssert csAssert) {
		try {
			String expectedCommentValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "communication tab",
					"expectedCommentValue");

			int commentColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "comment");
			String actualCommentValue = listData.get(0).get(commentColumnNo).get("value");
			actualCommentValue = actualCommentValue == null ? "null" : actualCommentValue;

			if (!actualCommentValue.trim().equalsIgnoreCase(expectedCommentValue.trim())) {
				csAssert.assertTrue(false, "Expected Comment Value: [" + expectedCommentValue.trim() + "] and Actual Comment Value: [" +
						actualCommentValue.trim() + "]");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Comment while Creating PO in Communication Tab. " + e.getMessage());
		}
	}

	//This method verifies TC-98659
	private void verifyDownloadingDocument(String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData, CustomAssert csAssert) {
		try {
			int documentColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "document");
			String documentValue = listData.get(0).get(documentColumnNo).get("value");

			if (documentValue != null) {
				String valuesArr[] = documentValue.trim().split(Pattern.quote(":;"));

				if (valuesArr.length > 1) {
					String downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "communication tab",
							"downloadFilePath");
					String downloadFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "communication tab",
							"downloadFileName");
					int id = Integer.parseInt(valuesArr[0]);
					int fileId = Integer.parseInt(valuesArr[valuesArr.length - 1]);

					DownloadCommunicationDocument downloadObj = new DownloadCommunicationDocument();

					if (!downloadObj.hitDownloadCommunicationDocument(downloadFilePath, downloadFileName, id, 78, poEntityTypeId, fileId)) {
						csAssert.assertTrue(false, "Couldn't download Document File at Location [" + downloadFilePath + "/" + downloadFileName + "]");
					}
				} else {
					csAssert.assertTrue(false, "Document details like Id and File Id are not present in Purchase Order Communication Tab API Response.");
				}
			} else {
				csAssert.assertTrue(false, "Document Value is Null in Purchase Order Communication Tab API Response.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Downloading Document from Purchase Order Communication Tab. " + e.getMessage());
		}
	}

	//This method verifies TC-98675, TC-98677, TC-98678
	private void verifyAuditLogForNewlyCreatedRecord(int auditLogTabId, CustomAssert csAssert) {
		try {
			logger.info("Hitting TabListData API for Purchase Order Id {} and Audit Log Tab.", purchaseOrderId);
			ListRendererTabListData tabListObj = new ListRendererTabListData();
			String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			tabListObj.hitListRendererTabListData(auditLogTabId, poEntityTypeId, purchaseOrderId, payload);
			String tabListDataResponse = tabListObj.getTabListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
				List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

				if (listData.size() > 0) {
					//Validate Comment
					int commentColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "comment");
					String actualCommentValue = listData.get(0).get(commentColumnNo).get("value");
					actualCommentValue = actualCommentValue == null ? "null" : actualCommentValue;

					if (!actualCommentValue.trim().toLowerCase().contains("yes")) {
						logger.error("Expected Comment Value: Yes and Actual Comment Value: {}", actualCommentValue);
						csAssert.assertTrue(false, "Purchase Order Audit Log Tab Validation failed for Comment");
					}

					int documentColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "document");
					String actualDocumentValue = listData.get(0).get(documentColumnNo).get("value");
					actualDocumentValue = actualDocumentValue == null ? "null" : actualDocumentValue;

					if (!actualDocumentValue.trim().toLowerCase().contains("yes")) {
						logger.error("Expected Document Value: Yes and Actual Document Value: {}", actualDocumentValue);
						csAssert.assertTrue(false, "Purchase Order Audit Log Tab Validation failed for Document");
					}
				} else {
					csAssert.assertTrue(false, "Couldn't get list data in TabListData API Response.");
				}
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Purchase Order Id " + purchaseOrderId + " and Audit Log Tab Id " +
						auditLogTabId + " is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Audit Log Tab for Newly Created Purchase Order. " + e.getMessage());
		}
	}

	//This method verifies TC-98664, TC-98665
	private void verifyAuditLogsForRequestedBy(String editGetResponse, int auditLogTabId, CustomAssert csAssert) {
		try {
			String commentPayload = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "audit log tab requested by", "comment");
			Map<String, String> fieldsPayloadMap = new HashMap<>();
			fieldsPayloadMap.put("comment", commentPayload);
			String editPostPayload = EntityOperationsHelper.createPayloadForEditPost(editGetResponse, fieldsPayloadMap);

			if (editPostPayload != null) {
				logger.info("Hitting TabListData API for Purchase Order Id {} and Audit Log Tab.", purchaseOrderId);
				ListRendererTabListData tabListObj = new ListRendererTabListData();
				String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
						"\"filterJson\":{}}}";
				tabListObj.hitListRendererTabListData(auditLogTabId, poEntityTypeId, purchaseOrderId, payload);
				String tabListDataResponse = tabListObj.getTabListDataJsonStr();

				if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
					List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);
					int listDataSizeBeforeRequestedBy = listData.size();

					logger.info("Hitting Edit Post API for Purchase Order Id {}", purchaseOrderId);
					Edit editObj = new Edit();
					String editPostJsonStr = editObj.hitEdit("purchase orders", editPostPayload);

					if (ParseJsonResponse.validJsonResponse(editPostJsonStr)) {
						JSONObject jsonObj = new JSONObject(editPostJsonStr);
						String editStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

						if (!editStatus.trim().equalsIgnoreCase("success")) {
							csAssert.assertTrue(false, "Edit of Purchase Order Id " + purchaseOrderId + "failed due to " + editStatus);
						} else {
							logger.info("Hitting TabListData API for Purchase Order Id {} and Audit Log Tab.", purchaseOrderId);
							tabListObj.hitListRendererTabListData(auditLogTabId, poEntityTypeId, purchaseOrderId, payload);
							tabListDataResponse = tabListObj.getTabListDataJsonStr();

							if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
								listData = ListDataHelper.getListData(tabListDataResponse);

								if (listData.size() != listDataSizeBeforeRequestedBy + 1)
									csAssert.assertTrue(false, "Expected Audit Log Size: " + (listDataSizeBeforeRequestedBy + 1) +
											" and Actual Audit Log Size: " + listData.size() + " for Purchase Order Id " + purchaseOrderId +
											" after Requested By Edit Action.");
							} else {
								csAssert.assertTrue(false, "TabListData API Response for Purchase Order Id " + purchaseOrderId + " and Audit Log Tab Id " +
										auditLogTabId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Edit Post API Response for Purchase Order Id " + purchaseOrderId + " is an Invalid JSON.");
					}

				} else {
					csAssert.assertTrue(false, "TabListData API Response for Purchase Order Id " + purchaseOrderId + " and Audit Log Tab Id " +
							auditLogTabId + " is an Invalid JSON.");
				}

			} else {
				csAssert.assertTrue(false, "Couldn't create Edit Post Payload for Purchase Order Id " + purchaseOrderId);
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Audit Logs for Requested By Cases. " + e.getMessage());
		}
	}

	//This method verifies TC-98666
	private void verifyAuditLogsForSubmit(String editGetResponse, int auditLogTabId, CustomAssert csAssert) {
		try {
			logger.info("Hitting TabListData API for Purchase Order Id {} and Audit Log Tab.", purchaseOrderId);
			ListRendererTabListData tabListObj = new ListRendererTabListData();
			String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
					"\"filterJson\":{}}}";
			tabListObj.hitListRendererTabListData(auditLogTabId, poEntityTypeId, purchaseOrderId, payload);
			String tabListDataResponse = tabListObj.getTabListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
				List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);
				int listDataSizeBeforeRequestedBy = listData.size();

				logger.info("Hitting Edit Post API for Purchase Order Id {}", purchaseOrderId);
				Edit editObj = new Edit();
				String editPostJsonStr = editObj.hitEdit("purchase orders", editGetResponse);

				if (ParseJsonResponse.validJsonResponse(editPostJsonStr)) {
					JSONObject jsonObj = new JSONObject(editPostJsonStr);
					String editStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

					if (!editStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Edit of Purchase Order Id " + purchaseOrderId + "failed due to " + editStatus);
					} else {
						logger.info("Hitting TabListData API for Purchase Order Id {} and Audit Log Tab.", purchaseOrderId);
						tabListObj.hitListRendererTabListData(auditLogTabId, poEntityTypeId, purchaseOrderId, payload);
						tabListDataResponse = tabListObj.getTabListDataJsonStr();

						if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
							listData = ListDataHelper.getListData(tabListDataResponse);

							if (listData.size() != listDataSizeBeforeRequestedBy + 1)
								csAssert.assertTrue(false, "Expected Audit Log Size: " + (listDataSizeBeforeRequestedBy + 1) +
										" and Actual Audit Log Size: " + listData.size() + " for Purchase Order Id " + purchaseOrderId +
										" after Submit Action.");
						} else {
							csAssert.assertTrue(false, "TabListData API Response for Purchase Order Id " + purchaseOrderId + " and Audit Log Tab Id " +
									auditLogTabId + " is an Invalid JSON.");
						}
					}
				} else {
					csAssert.assertTrue(false, "Edit Post API Response for Purchase Order Id " + purchaseOrderId + " is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Purchase Order Id " + purchaseOrderId + " and Audit Log Tab Id " +
						auditLogTabId + " is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Audit Logs for Submit case. " + e.getMessage());
		}
	}

	//This method verifies TC-98668
	private void verifyAuditLogTabPagination(int auditLogTabId, CustomAssert csAssert) {
		try {
			int listDataSize = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "audit log pagination",
					"listDataSize"));
			ListDataHelper.verifyTabListingPagination(poEntityTypeId, purchaseOrderId, auditLogTabId, listDataSize, csAssert);
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Audit Log Tab Pagination. " + e.getMessage());
		}
	}

	private void deleteRecords() {
		if (deleteEntity) {
			EntityOperationsHelper.deleteEntityRecord("purchase orders", purchaseOrderId);
		}
	}

	private void updatePurchaseOrderConfigFile(String poCreateConfigFilePath, String poCreateExtraFieldsConfigFileName, String sectionName, String oldValue, String newValue) {
		try {
			logger.info("Updating Purchase Order Comment Field.");
			UpdateFile.updateConfigFileProperty(poCreateConfigFilePath, poCreateExtraFieldsConfigFileName, sectionName, "comment", oldValue, newValue);
		} catch (Exception e) {
			logger.error("Exception while Updating Purchase Order Config File. {}", e.getMessage());
		}
	}
}
