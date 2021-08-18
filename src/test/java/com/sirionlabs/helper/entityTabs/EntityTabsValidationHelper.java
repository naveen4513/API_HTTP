package com.sirionlabs.helper.entityTabs;

import com.sirionlabs.api.commonAPI.Comment;
import com.sirionlabs.api.download.DownloadCommunicationDocument;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.List;
import java.util.regex.Pattern;

public class EntityTabsValidationHelper {

	private final static Logger logger = LoggerFactory.getLogger(EntityTabsValidationHelper.class);

	private TabListData tabObj = new TabListData();

	public void validateTabsArePresentOnShowPage(String entityName, List<String> expectedTabs, CustomAssert csAssert) {
		try {
			logger.info("Validating Tabs on Show Page for Entity {}", entityName);
			String listDataResponse = ListDataHelper.getListDataResponse(entityName);

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

				String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
				int recordId = ListDataHelper.getRecordIdFromValue(idValue);

				String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

				if (ParseJsonResponse.validJsonResponse(showResponse)) {
					List<String> allTabLabels = ParseJsonResponse.getAllTabLabels(showResponse);
					allTabLabels.replaceAll(String::toLowerCase);

					if (allTabLabels == null || allTabLabels.isEmpty()) {
						throw new SkipException("Couldn't get All Tab Labels from Show Response of Entity " + entityName + " and Record Id: " + recordId);
					}

					for (String expectedTabLabel : expectedTabs) {
						csAssert.assertTrue(allTabLabels.contains(expectedTabLabel.toLowerCase()), expectedTabLabel + " Tab not found in Show Response of Entity " +
								entityName + " and Record Id: " + recordId);
					}
				} else {
					csAssert.assertTrue(false, "Show Response for Entity " + entityName + " and Record Id " + recordId + " is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for " + entityName + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Tabs on Show Page for Entity " + entityName);
		}
	}

	public void validateEntityCommunicationTab(String entityName, CustomAssert csAssert) {
		try {
			logger.info("Validating Communication Tab of Entity {}", entityName);
			String listDataResponse = ListDataHelper.getListDataResponse(entityName);

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
				int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

				for (int i = 0; i < jsonArr.length(); i++) {
					String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
					int recordIdHavingPermissionToComment = ListDataHelper.getRecordIdFromValue(idValue);

					boolean commentAllowedForRecord = ShowHelper.isActionCanBePerformed(entityTypeId, recordIdHavingPermissionToComment, "SaveComment/Attachment");

					if (commentAllowedForRecord) {
						String filePath = "src/test/resources/TestConfig";
						String originalFileName = "Test Move To Tree.txt";
						String copiedFileFullName = "TestDocUpload.txt";
						String copiedFileName = FileUtils.getFileNameWithoutExtension(copiedFileFullName);

						FileUtils.copyFile(filePath, originalFileName, filePath, copiedFileFullName);

						//Verify Document Upload
						String randomKeyForFile = RandomString.getRandomAlphaNumericString(18);
						String uploadResponse = DocumentHelper.uploadDocumentFile(filePath, copiedFileFullName, randomKeyForFile);

						if (uploadResponse != null && uploadResponse.contains(copiedFileFullName)) {
							String commentFieldPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null},\"shareWithSupplier\":{\"name\":" +
									"\"shareWithSupplier\",\"id\":12409},\"comments\":{\"name\":\"comments\",\"id\":86,\"values\":\"\"},\"draft\":{\"name\":\"draft\"}," +
									"\"actualDate\":{\"name\":\"actualDate\",\"id\":12243},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242," +
									"\"values\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null}," +
									"\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247},\"commentDocuments\":{\"values\":[{\"key\":\"" +
									randomKeyForFile + "\",\"performanceData\":false,\"searchable\":false,\"legal\":false,\"financial\":false,\"businessCase\":false}]}}";

							String showResponse = ShowHelper.getShowResponse(entityTypeId, recordIdHavingPermissionToComment);
							JSONObject showJsonObj = new JSONObject(showResponse);
							showJsonObj = showJsonObj.getJSONObject("body").getJSONObject("data");
							showJsonObj = showJsonObj.put("comment", new JSONObject(commentFieldPayload));
							String payloadForCommentAPI = "{\"body\":{\"data\":" + showJsonObj.toString() + "}}";

							Comment commentObj = new Comment();
							String commentResponse = commentObj.hitComment(entityName, payloadForCommentAPI);
							if (!ParseJsonResponse.successfulResponse(commentResponse)) {
								throw new SkipException("Comment API Response is not successful.");
							}

							String payloadForCommunication = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
									"\"orderDirection\":\"asc\",\"filterJson\":{}}}";

							String communicationTabResponse = tabObj.hitTabListData(65, entityTypeId, recordIdHavingPermissionToComment,
									payloadForCommunication);

							if (ParseJsonResponse.validJsonResponse(communicationTabResponse)) {
								JSONObject tabJsonObj = new JSONObject(communicationTabResponse);
								JSONArray tabJsonArr = tabJsonObj.getJSONArray("data");
								tabJsonObj = tabJsonArr.getJSONObject(0);

								String documentColumnNo = TabListDataHelper.getColumnIdFromColumnName(communicationTabResponse, "document");
								String actualValue = tabJsonObj.getJSONObject(documentColumnNo).getString("value");

								csAssert.assertTrue(actualValue.contains(copiedFileName), "Communication Tab Validation failed. Expected Document Value: [" +
										copiedFileName + "] and Actual Value: [" + actualValue + "]");

								//Verify Document Download from Communication Tab.
								String valuesArr[] = actualValue.trim().split(Pattern.quote(":;"));

								int id = Integer.parseInt(valuesArr[0]);
								int fileId = Integer.parseInt(valuesArr[valuesArr.length - 1]);

								DownloadCommunicationDocument downloadObj = new DownloadCommunicationDocument();

								if (!downloadObj.hitDownloadCommunicationDocument("src/test/output", copiedFileName, id,
										78, entityTypeId, fileId)) {
									csAssert.assertTrue(false, "Couldn't download Document File at Location [src/test/output/" + copiedFileName + "]");
								}

								//Verify Comment Field in Communication Tab.
								validateCommunicationTabCommentField(entityName, entityTypeId, recordIdHavingPermissionToComment, csAssert);

								//Verify Pagination.
								validateCommunicationTabPagination(entityName, entityTypeId, recordIdHavingPermissionToComment, csAssert);
							} else {
								csAssert.assertTrue(false, "TabListData API Response for Communication Tab of Entity " + entityName + " Record Id " +
										recordIdHavingPermissionToComment + " is an Invalid JSON.");
							}
						} else {
							throw new SkipException("Document Upload Failed.");
						}

						FileUtils.deleteFile(filePath + "/" + copiedFileFullName);
						break;
					}
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Communication Tab of Entity " + entityName + ". " + e.getMessage());
		}
	}

	private void validateCommunicationTabCommentField(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
		try {
			logger.info("Verifying Communication Tab Comment Field for Entity {} and Record Id {}", entityName, recordId);
			String commentTextValue = "Test @!@# Comment";

			String commentFieldPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null},\"shareWithSupplier\":" +
					"{\"name\":\"shareWithSupplier\",\"id\":12409},\"comments\":{\"name\":\"comments\",\"id\":86,\"values\":\"" + commentTextValue + "\"}," +
					"\"draft\":{\"name\":\"draft\"},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243},\"privateCommunication\":{\"name\":" +
					"\"privateCommunication\",\"id\":12242,\"values\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null}," +
					"\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247},\"commentDocuments\":{\"values\":[]}}";

			String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);
			JSONObject showJsonObj = new JSONObject(showResponse);
			showJsonObj = showJsonObj.getJSONObject("body").getJSONObject("data");
			showJsonObj = showJsonObj.put("comment", new JSONObject(commentFieldPayload));
			String payloadForCommentAPI = "{\"body\":{\"data\":" + showJsonObj.toString() + "}}";

			Comment commentObj = new Comment();
			String commentResponse = commentObj.hitComment(entityName, payloadForCommentAPI);
			if (!ParseJsonResponse.successfulResponse(commentResponse)) {
				throw new SkipException("Comment API Response is not successful for Entity " + entityName + " and Record Id " + recordId);
			}

			String payloadForCommunication = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"asc\",\"filterJson\":{}}}";

			String communicationTabResponse = tabObj.hitTabListData(65, entityTypeId, recordId, payloadForCommunication);

			if (ParseJsonResponse.validJsonResponse(communicationTabResponse)) {
				JSONObject tabJsonObj = new JSONObject(communicationTabResponse);
				JSONArray tabJsonArr = tabJsonObj.getJSONArray("data");
				tabJsonObj = tabJsonArr.getJSONObject(0);

				String communicationTabCommentColumnNo = TabListDataHelper.getColumnIdFromColumnName(communicationTabResponse, "comment");
				String actualCommentValue = tabJsonObj.getJSONObject(communicationTabCommentColumnNo).getString("value");
				csAssert.assertTrue(actualCommentValue.equalsIgnoreCase(commentTextValue),
						"Communication Tab Validation failed. Expected Comment Value: [" + commentTextValue + "] and Actual Comment Value: [" +
								actualCommentValue + "]");
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Communication Tab of Entity " + entityName + " Record Id " +
						recordId + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Communication Tab Comment Field for Entity " + entityName +
					" and Record Id " + recordId + ". " + e.getMessage());
		}
	}

	private void validateCommunicationTabPagination(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
		try {
			validateTabPagination(entityName, entityTypeId, recordId, "Communication", 65, csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		}
	}

	private void validateTabPagination(String entityName, int entityTypeId, int recordId, String tabName, int tabId, CustomAssert csAssert) {
		try {
			logger.info("Validating Pagination on Tab {} for Entity {} and Record Id {}", tabName, entityName, recordId);

			int[] limitArr = {10, 20, 25, 50, 100};

			String payloadForTab = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 20 + ",\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc\",\"filterJson\":{}}}";

			String tabResponse = tabObj.hitTabListData(tabId, entityTypeId, recordId, payloadForTab);

			int totalRecordsInTab = ListDataHelper.getTotalListDataCount(tabResponse);

			if (totalRecordsInTab == -1) {
				throw new SkipException("Couldn't get Total No of Records in Tab " + tabName + " of Entity " + entityName + " and Record Id " + recordId);
			}

			if (totalRecordsInTab == 0) {
				logger.info("No Record found in Tab {} of Entity {} and Record Id {}", tabName, entityName, recordId);
				return;
			}

			logger.info("Total No of Data present in Tab {} of Entity {} and Record Id {}", tabName, entityName, recordId);

			for (int size : limitArr) {
				int offset = 0;

				do {
					logger.info("Hitting TabListData API for Tab {} of Entity {} and Record Id {} with Size: {} and Offset: {}", tabName, entityName, recordId,
							size, offset);

					payloadForTab = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
							"\"orderDirection\":\"desc\",\"filterJson\":{}}}";

					tabResponse = tabObj.hitTabListData(tabId, entityTypeId, recordId, payloadForTab);

					if (ParseJsonResponse.validJsonResponse(tabResponse)) {
						JSONObject jsonObj = new JSONObject(tabResponse);
						JSONArray jsonArr = jsonObj.getJSONArray("data");

						int noOfData = jsonArr.length();
						int expectedNoOfData = (size + offset) > totalRecordsInTab ? (totalRecordsInTab % size) : size;

						csAssert.assertTrue(expectedNoOfData == noOfData, "Pagination failed for Tab " + tabName + " of Entity " + entityName +
								", Record Id " + recordId + ", size " + size + " and offset " + offset + ". Expected No of Records: " + size +
								" and Actual No of Records: " + noOfData);
					} else {
						csAssert.assertTrue(false, "TabListData API Response for Tab " + tabName + " of Entity " + entityName + ", Record Id " +
								recordId + ",size " + size + " and offset " + offset + " is an Invalid JSON.");
					}

					offset += size;
				} while (offset <= totalRecordsInTab);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Pagination for Tab " + tabName + " of Entity " + entityName +
					" and Record Id " + recordId);
		}
	}

	public void validateEntityAuditLogTab(String entityName, CustomAssert csAssert) {
		try {
			logger.info("Validating Audit Log Tab of Entity {}", entityName);
			String listDataResponse = ListDataHelper.getListDataResponse(entityName);

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

				for (int i = 0; i < jsonArr.length(); i++) {
					String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
					int recordIdHavingPermissionToComment = ListDataHelper.getRecordIdFromValue(idValue);

					boolean commentAllowedForRecord = ShowHelper.isActionCanBePerformed(entityTypeId, recordIdHavingPermissionToComment,
							"SaveComment/Attachment");

					if (commentAllowedForRecord) {
						//Verify Comments Field in Audit Log Tab.
						validateAuditLogCommentsField(entityName, entityTypeId, recordIdHavingPermissionToComment, csAssert);

						//Verify Document Field in Audit Log Tab.
						validateAuditLogDocumentField(entityName, entityTypeId, recordIdHavingPermissionToComment, csAssert);

						//Verify Requested By Field in Audit log Tab.
						validateAuditLogRequestedByField(entityName, entityTypeId, recordIdHavingPermissionToComment, csAssert);

						//Verify Pagination.
						validateAuditLogTabPagination(entityName, entityTypeId, recordIdHavingPermissionToComment, csAssert);
						break;
					}
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Audit Log Tab of Entity " + entityName + ". " + e.getMessage());
		}
	}

	private void validateAuditLogCommentsField(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
		try {
			logger.info("Validating Audit Log Comments Field for Entity {} and Record Id {}", entityName, recordId);
			String commentFieldPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null},\"shareWithSupplier\":" +
					"{\"name\":\"shareWithSupplier\",\"id\":12409},\"comments\":{\"name\":\"comments\",\"id\":86,\"values\":\"Test Comment\"}," +
					"\"draft\":{\"name\":\"draft\"},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243},\"privateCommunication\":{\"name\":" +
					"\"privateCommunication\",\"id\":12242,\"values\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null}," +
					"\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247},\"commentDocuments\":{\"values\":[]}}";

			String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);
			JSONObject showJsonObj = new JSONObject(showResponse);
			showJsonObj = showJsonObj.getJSONObject("body").getJSONObject("data");
			showJsonObj = showJsonObj.put("comment", new JSONObject(commentFieldPayload));
			String payloadForCommentAPI = "{\"body\":{\"data\":" + showJsonObj.toString() + "}}";

			Comment commentObj = new Comment();
			String commentResponse = commentObj.hitComment(entityName, payloadForCommentAPI);
			if (!ParseJsonResponse.successfulResponse(commentResponse)) {
				throw new SkipException("Comment API Response is not successful for Entity " + entityName + " and Record Id " + recordId);
			}

			String payloadForAuditLog = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc\",\"filterJson\":{}}}";

			String auditLogTabResponse = tabObj.hitTabListData(61, entityTypeId, recordId, payloadForAuditLog);

			if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
				JSONObject tabJsonObj = new JSONObject(auditLogTabResponse);
				JSONArray tabJsonArr = tabJsonObj.getJSONArray("data");

				String auditLogActionNameColumnNo = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "action_name");

				tabJsonObj = tabJsonArr.getJSONObject(0);

				String actualActionNameValue = tabJsonObj.getJSONObject(auditLogActionNameColumnNo).getString("value");
				csAssert.assertTrue(actualActionNameValue.equalsIgnoreCase("Comment/Attachment"),
						"Audit Log Validation failed. Expected Action Name: [Comment/Attachment] and Actual Action Name: [" +
								actualActionNameValue + "]");

				String auditLogCommentColumnNo = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "comment");

				String actualCommentValue = tabJsonObj.getJSONObject(auditLogCommentColumnNo).getString("value");
				csAssert.assertTrue(actualCommentValue.equalsIgnoreCase("Yes"),
						"Audit Log Validation failed. Expected Comment Value: [Yes] and Actual Comment Value: [" + actualCommentValue + "]");

				String auditLogDocumentColumnNo = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "document");

				String actualDocumentValue = tabJsonObj.getJSONObject(auditLogDocumentColumnNo).getString("value");
				csAssert.assertTrue(actualDocumentValue.equalsIgnoreCase("No"),
						"Audit Log Validation failed. Expected Document Value: [No] and Actual Document Value: [" + actualDocumentValue + "]");
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Audit Log Tab of Entity " + entityName + " and Record Id " +
						recordId + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Comments Field of Audit Log Tab for Entity " + entityName + " and Record Id " +
					recordId + ". " + e.getMessage());
		}
	}

	private void validateAuditLogDocumentField(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
		try {
			logger.info("Validating Audit Log Documents Field for Entity {} and Record Id {}", entityName, recordId);
			String filePath = "src/test/resources/TestConfig";
			String originalFileName = "Test Move To Tree.txt";
			String copiedFileFullName = "TestDocUpload.txt";

			FileUtils.copyFile(filePath, originalFileName, filePath, copiedFileFullName);

			String randomKeyForFile = RandomString.getRandomAlphaNumericString(18);
			String uploadResponse = DocumentHelper.uploadDocumentFile(filePath, copiedFileFullName, randomKeyForFile);

			if (uploadResponse != null && uploadResponse.contains(copiedFileFullName)) {
				String commentFieldPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null},\"shareWithSupplier\":{\"name\":" +
						"\"shareWithSupplier\",\"id\":12409},\"comments\":{\"name\":\"comments\",\"id\":86,\"values\":\"\"},\"draft\":{\"name\":\"draft\"}," +
						"\"actualDate\":{\"name\":\"actualDate\",\"id\":12243},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"values\":false}," +
						"\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247}," +
						"\"commentDocuments\":{\"values\":[{\"key\":\"" + randomKeyForFile + "\",\"performanceData\":false,\"searchable\":false,\"legal\":false," +
						"\"financial\":false,\"businessCase\":false}]}}";

				String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);
				JSONObject showJsonObj = new JSONObject(showResponse);
				showJsonObj = showJsonObj.getJSONObject("body").getJSONObject("data");
				showJsonObj = showJsonObj.put("comment", new JSONObject(commentFieldPayload));
				String payloadForCommentAPI = "{\"body\":{\"data\":" + showJsonObj.toString() + "}}";

				Comment commentObj = new Comment();
				String commentResponse = commentObj.hitComment(entityName, payloadForCommentAPI);
				if (!ParseJsonResponse.successfulResponse(commentResponse)) {
					throw new SkipException("Comment API Response is not successful for Entity " + entityName + " and Record Id " + recordId);
				}

				String payloadForAuditLog = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
						"\"orderDirection\":\"desc\",\"filterJson\":{}}}";

				String auditLogTabResponse = tabObj.hitTabListData(61, entityTypeId, recordId, payloadForAuditLog);

				if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
					JSONObject tabJsonObj = new JSONObject(auditLogTabResponse);
					JSONArray tabJsonArr = tabJsonObj.getJSONArray("data");

					tabJsonObj = tabJsonArr.getJSONObject(0);

					String auditLogCommentColumnNo = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "comment");

					String actualCommentValue = tabJsonObj.getJSONObject(auditLogCommentColumnNo).getString("value");
					csAssert.assertTrue(actualCommentValue.equalsIgnoreCase("No"),
							"Audit Log Validation failed. Expected Comment Value: [No] and Actual Comment Value: [" + actualCommentValue + "]");

					String auditLogDocumentColumnNo = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "document");

					String actualDocumentValue = tabJsonObj.getJSONObject(auditLogDocumentColumnNo).getString("value");
					csAssert.assertTrue(actualDocumentValue.equalsIgnoreCase("Yes"),
							"Audit Log Validation failed. Expected Document Value: [Yes] and Actual Document Value: [" + actualDocumentValue + "]");
				} else {
					csAssert.assertTrue(false, "TabListData API Response for Audit Log Tab of Entity " + entityName + " and Record Id " +
							recordId + " is an Invalid JSON.");
				}
			} else {
				throw new SkipException("Document Upload Failed.");
			}

			FileUtils.deleteFile(filePath + "/" + copiedFileFullName);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Audit Log Document Field for Entity " + entityName + " and Record Id " +
					recordId + ". " + e.getMessage());
		}
	}

	private void validateAuditLogRequestedByField(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
		try {
			logger.info("Validating Audit Log Requested By Field for Entity {} and Record Id {}", entityName, recordId);

			//Verify when Requested By Field is populated by some other user.
			String commentFieldPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null,\"values\":{\"name\":\"Akshay User\",\"id\":1183," +
					"\"idType\":2}},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409},\"comments\":{\"name\":\"comments\",\"id\":86," +
					"\"values\":\"Test Comment\"},\"draft\":{\"name\":\"draft\"},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243},\"privateCommunication\":" +
					"{\"name\":\"privateCommunication\",\"id\":12242,\"values\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null}," +
					"\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247},\"commentDocuments\":{\"values\":[]}}";

			String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);
			JSONObject showJsonObj = new JSONObject(showResponse);
			showJsonObj = showJsonObj.getJSONObject("body").getJSONObject("data");
			showJsonObj = showJsonObj.put("comment", new JSONObject(commentFieldPayload));
			String payloadForCommentAPI = "{\"body\":{\"data\":" + showJsonObj.toString() + "}}";

			Comment commentObj = new Comment();
			String commentResponse = commentObj.hitComment(entityName, payloadForCommentAPI);
			if (!ParseJsonResponse.successfulResponse(commentResponse)) {
				throw new SkipException("Comment API Response is not successful for Entity " + entityName + " and Record Id " + recordId);
			}

			String payloadForAuditLog = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc\",\"filterJson\":{}}}";

			String auditLogTabResponse = tabObj.hitTabListData(61, entityTypeId, recordId, payloadForAuditLog);

			String auditLogRequestedByColumnNo = null;

			if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
				JSONObject tabJsonObj = new JSONObject(auditLogTabResponse);
				JSONArray tabJsonArr = tabJsonObj.getJSONArray("data");

				tabJsonObj = tabJsonArr.getJSONObject(0);

				auditLogRequestedByColumnNo = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "requested_by");

				String actualRequestedByValue = tabJsonObj.getJSONObject(auditLogRequestedByColumnNo).getString("value");
				csAssert.assertTrue(actualRequestedByValue.equalsIgnoreCase("Akshay User"),
						"Audit Log Validation failed. Expected Requested By Value: [Akshay User] and Actual Requested By Value: [" + actualRequestedByValue + "]");
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Audit Log Tab of Entity " + entityName + " and Record Id " +
						recordId + " is an Invalid JSON.");
			}

			//Verify when Requested By Field is blank.
			commentFieldPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\"," +
					"\"id\":12409},\"comments\":{\"name\":\"comments\",\"id\":86,\"values\":\"Test Comment\"},\"draft\":{\"name\":\"draft\"}," +
					"\"actualDate\":{\"name\":\"actualDate\",\"id\":12243},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"values\":false}," +
					"\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247}," +
					"\"commentDocuments\":{\"values\":[]}}";

			showJsonObj = new JSONObject(showResponse);
			showJsonObj = showJsonObj.getJSONObject("body").getJSONObject("data");
			showJsonObj = showJsonObj.put("comment", new JSONObject(commentFieldPayload));
			payloadForCommentAPI = "{\"body\":{\"data\":" + showJsonObj.toString() + "}}";

			commentResponse = commentObj.hitComment(entityName, payloadForCommentAPI);
			if (!ParseJsonResponse.successfulResponse(commentResponse)) {
				throw new SkipException("Comment API Response is not successful for Entity " + entityName + " and Record Id " + recordId);
			}

			auditLogTabResponse = tabObj.hitTabListData(61, entityTypeId, recordId, payloadForAuditLog);

			if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
				JSONObject tabJsonObj = new JSONObject(auditLogTabResponse);
				JSONArray tabJsonArr = tabJsonObj.getJSONArray("data");

				tabJsonObj = tabJsonArr.getJSONObject(0);

				Object actualRequestedByValue = tabJsonObj.getJSONObject(auditLogRequestedByColumnNo).get("value");
				csAssert.assertTrue(actualRequestedByValue.toString().equalsIgnoreCase("chinmaya Nayak"),
						"Audit Log Validation failed. Expected Requested By Value: [Anay User] and Actual Requested By Value: [" + actualRequestedByValue + "]");
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Audit Log Tab of Entity " + entityName + " and Record Id " +
						recordId + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Audit Log Requested By Field for Entity " + entityName + " and Record Id " +
					recordId + ". " + e.getMessage());
		}
	}

	private void validateAuditLogTabPagination(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
		try {
			validateTabPagination(entityName, entityTypeId, recordId, "Audit Log", 61, csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		}
	}
}