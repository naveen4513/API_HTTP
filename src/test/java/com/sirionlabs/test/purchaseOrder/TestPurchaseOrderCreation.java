package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.InvoiceLineItem;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

//@Listeners(value = MyTestListenerAdapter.class)
public class TestPurchaseOrderCreation {

	private final Logger logger = LoggerFactory.getLogger(TestPurchaseOrderCreation.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String extraFieldsConfigFilePath = null;
	private String extraFieldsConfigFileName = null;
	private Integer poEntityTypeId;

	private String listingPayloadConfigFilePath;
	private String listingPayloadConfigFileName;

	private List<String> allFieldGroupsToVerify;
	private Boolean deleteEntity = true;

	private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

	private String purchaseOrder = "purchase orders";
	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderCreationTestConfigFileName");

		listingPayloadConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFilePath");
		listingPayloadConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFileName");


		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		poEntityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		String[] groupsToVerifyArr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldGroupsToVerify").split(Pattern.quote(","));
		allFieldGroupsToVerify = new ArrayList<>(Arrays.asList(groupsToVerifyArr));
	}

	@DataProvider
	public Object[][] dataProviderForTestPurchaseOrderCreation() throws ConfigurationException {
		logger.info("Setting all Purchase Order Creation Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
		if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
			logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
			flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
		} else {
			String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
			for (String flow : allFlows) {
				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
					flowsToTest.add(flow.trim());
				} else {
					logger.info("Flow having name [{}] not found in Purchase Order Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestPurchaseOrderCreation")
	public void testPurchaseOrderCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		int purchaseOrderId = -1;

		String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter name");
		String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter id");
		String uniqueString = DateUtils.getCurrentTimeStamp();
		String min = "1";
		String max = "1000000";
		try {
			logger.info("Validating Purchase Order Creation Flow [{}]", flowToTest);
			verifyPOCreatePageFields(flowToTest, csAssert);

			//Validate PO Creation
			logger.info("Creating Purchase Order for Flow [{}]", flowToTest);

			uniqueString = uniqueString.replaceAll("_", "");
			uniqueString = uniqueString.replaceAll(" ", "");

			uniqueString = uniqueString.substring(10);

			String createResponse = "";

			if(filter_name != null) {
				String dynamicField = "dyn" + filter_name;

				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest, "name", "unqString", uniqueString);

				createResponse = PurchaseOrder.createPurchaseOrder(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);

				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString,"unqString");
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString,"unqString");
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest, "name", uniqueString,"unqString");

			}else {
				createResponse = PurchaseOrder.createPurchaseOrder(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);

			}

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

						if (purchaseOrderId != -1) {
							//Validate Supplier Name and Contract Name are hyperlinked.

							if(filter_name != null) {
								ListRendererListData listRendererListData = new ListRendererListData();
								String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath,listingPayloadConfigFileName,purchaseOrder,"payload");
								String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath,listingPayloadConfigFileName,purchaseOrder,"columnidstoignore");

								if(payload == null){
									payload = "";
								}
								payload = listRendererListData.createPayloadForColStr(payload,columnIdsToIgnore);

								min = new BigDecimal(uniqueString).subtract(new BigDecimal("5")).toString();
								max = new BigDecimal(uniqueString).add(new BigDecimal("5")).toString();

								Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(poEntityTypeId, filter_id,
										filter_name, min, max,payload, csAssert);

								String entityId = "";
								try {
									entityId = listColumnValuesMap.get("id").split(":;")[1];

								} catch (Exception e) {

								}

								if (!entityId.equalsIgnoreCase(String.valueOf(purchaseOrderId))) {
									csAssert.assertTrue(false, "On Listing page PO entity " + purchaseOrderId + " Not Found");
								} else {
									logger.info("On Listing page PO entity " + purchaseOrderId + "  Found");
								}

								String recordPresent = listRendererListData.chkPartRecIsPresentForDiffUser(poEntityTypeId, filter_id,
										filter_name, min, max, payload, csAssert);

								if(recordPresent.equalsIgnoreCase("Yes") ){
									csAssert.assertTrue(false,"Purchase Order Record present for different user where is it is not supposed to present for different user");
								}
							}
							logger.info("Hitting Show API for Purchase Order Id {}", purchaseOrderId);
							Show showObj = new Show();
							showObj.hitShow(poEntityTypeId, purchaseOrderId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
								String sourceEntity = flowProperties.get("sourceentity");

								if (sourceEntity.trim().equalsIgnoreCase("contracts")) {
									if (ShowHelper.getValueOfField("contract url", showResponse) == null) {
										csAssert.assertTrue(false, "Contract Name doesn't contain hyperlink for Purchase Order Id " + purchaseOrderId +
												" and Flow [" + flowToTest + "]");
									}
								}

								if (ShowHelper.getValueOfField("supplier url", showResponse) == null) {
									csAssert.assertTrue(false, "Supplier Name doesn't contain hyperlink for Purchase Order Id " + purchaseOrderId +
											" and Flow [" + flowToTest + "]");
								}

								int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntity);
								int parentRecordId = Integer.parseInt(flowProperties.get("sourceid"));

								//Validate Source Reference Tab.
								tabListDataHelperObj.validateSourceReferenceTab("purchase orders", 181, purchaseOrderId,
										parentEntityTypeId, parentRecordId, csAssert);

								//Validate Forward Reference Tab.
								tabListDataHelperObj.validateForwardReferenceTab(sourceEntity, parentEntityTypeId, parentRecordId, 181, purchaseOrderId,
										csAssert);

								if (flowProperties.containsKey("multisupplier") && flowProperties.get("multisupplier").trim().equalsIgnoreCase("true")) {
									//Validate Supplier on Show Page
									String expectedSupplierId = flowProperties.get("multiparentsupplierid");
									ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, 181, purchaseOrderId, csAssert);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Purchase Order Id " + purchaseOrderId + " is an Invalid JSON.");
							}
						} else {
							throw new SkipException("Couldn't get Id of Newly Created Purchase Order for Flow [" + flowToTest + "]");
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Purchase Order Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			if (deleteEntity && purchaseOrderId != -1) {
				EntityOperationsHelper.deleteEntityRecord("purchase orders", purchaseOrderId);
			}

			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Purchase Order Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && purchaseOrderId != -1) {
				EntityOperationsHelper.deleteEntityRecord("purchase orders", purchaseOrderId);

				if(filter_name != null) {
					ListRendererListData listRendererListData = new ListRendererListData();
					String payload = "";

					Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(poEntityTypeId, filter_id,
							filter_name, min, max,payload, csAssert);

					String entityId = "";
					try {
						entityId = listColumnValuesMap.get("id").split(":;")[1];

					} catch (Exception e) {

					}

					if (entityId.equalsIgnoreCase(String.valueOf(purchaseOrderId))) {
						csAssert.assertTrue(false, "On Listing page PO entity " + purchaseOrderId + "  Found After Deletion");
					} else {
						logger.info("On Listing page PO entity " + purchaseOrderId + " Not Found After Deletion");
					}
				}
			}
			csAssert.assertAll();
		}
	}

	private void verifyPOCreatePageFields(String flowToTest, CustomAssert csAssert) {
		try {
			logger.info("Validating PO Create Page Fields for Flow [{}]", flowToTest);

			//Verify Field Groups
			String sourceEntity = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceEntity");
			int sourceEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceId"));

			logger.info("Hitting New API for PO Flow [{}]", flowToTest);
			New newObj = new New();
			newObj.hitNew("purchase orders", sourceEntity, sourceEntityId);
			String newResponse = newObj.getNewJsonStr();

			if (ParseJsonResponse.validJsonResponse(newResponse)) {
				//Validate General Tab
				JSONObject jsonObj = new JSONObject(newResponse);
				if (!jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields")
						.getJSONObject(0).getString("label").trim().equalsIgnoreCase("General")) {
					csAssert.assertTrue(false, "General Tab not found in New API Response for Flow [" + flowToTest + "]");
				}


				List<String> allFieldGroups = ParseJsonResponse.getAllFieldGroupLabels(newResponse);
				List<String> allFieldGroupNames = new ArrayList<>();

				for(String fieldGroup: allFieldGroups) {
					allFieldGroupNames.add(fieldGroup.toUpperCase());
				}

				if (allFieldGroups.isEmpty()) {
					throw new SkipException("Couldn't get All Field Groups from New API Response for Flow [" + flowToTest + "]");
				}

				String[] allExpectedGroupsArr = {"BASIC INFORMATION", "ORGANIZATION INFORMATION", "IMPORTANT DATES", "GEOGRAPHY", "FUNCTION", "FINANCIAL INFORMATION",
						"STAKEHOLDERS"};
				List<String> allExpectedGroupsList = new ArrayList<>(Arrays.asList(allExpectedGroupsArr));

				for (String expectedGroup : allExpectedGroupsList) {
					if (!allFieldGroupNames.contains(expectedGroup.trim())) {
						csAssert.assertTrue(false, "New API Response doesn't contain Field Group [" + expectedGroup + "] for Flow [" + flowToTest + "]");
					}
				}

				Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, null);

				//Verify Fields for every Group
				for (String groupToVerify : allFieldGroupsToVerify) {
					groupToVerify = groupToVerify.trim();

					if (!properties.containsKey(groupToVerify.toLowerCase())) {
						throw new SkipException("Couldn't find Fields for Group [" + groupToVerify + "] in Config file.");
					}

					String[] fieldsArr = properties.get(groupToVerify.toLowerCase()).split(Pattern.quote(","));
					List<String> allExpectedFieldsInGroup = new ArrayList<>(Arrays.asList(fieldsArr));
					List<String> allActualFieldsInGroup = ParseJsonResponse.getAllFieldNamesOfAGroup(newResponse, groupToVerify);

					if (allActualFieldsInGroup.isEmpty()) {
						throw new SkipException("Couldn't get All Field Names of Group [" + groupToVerify + "] from New API Response for Flow [" + flowToTest + "]");
					}

					for (String expectedFieldInGroup : allExpectedFieldsInGroup) {
						if (!allActualFieldsInGroup.contains(expectedFieldInGroup.trim())) {
							csAssert.assertTrue(false, "Field Name " + expectedFieldInGroup + " not found in Field Group " + groupToVerify +
									" for Flow [" + flowToTest + "]");
						}
					}
				}
			} else {
				csAssert.assertTrue(false, "New API Response for PO Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Create Page Fields for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}
}
