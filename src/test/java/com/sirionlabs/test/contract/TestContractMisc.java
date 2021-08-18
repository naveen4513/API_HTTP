package com.sirionlabs.test.contract;

import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.file.FileUploadContractDocument;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;

@Listeners(value = MyTestListenerAdapter.class)
public class TestContractMisc {

	private final static Logger logger = LoggerFactory.getLogger(TestContractMisc.class);

	private int contractId;
	private String contractConfigFilePath;
	private String contractExtraFieldsConfigFileName;
	private String ContractFileName;
	private  String supplierId;

	@AfterClass
	public void afterClass() {
		//Delete Contract.
		EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
	}

	@BeforeClass
	public void beforeClass() {
		contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractFilePath");
		contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractExtraFieldsFileName");
		ContractFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractFileName");
		supplierId = ParseConfigFile.getValueFromConfigFile(contractConfigFilePath, ContractFileName, "contract misc test c8836", "sourceid");


	}

	/*
	TC-C8836: Verify Uploading Document without selecting options on Contract Create Page.
	 */
	@Test
	public void testC8836() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C8836: Verify Uploading Document without selecting options on Contract Create Page.");

			String filePath = "src/test/resources/TestConfig";
			String documentFileName = "C8836.txt";
			FileUtils.copyFile(filePath, "Test Move To Tree.txt", filePath, documentFileName);

			int parentSupplierId = Integer.valueOf(supplierId);

			//Uploading Document.
			FileUploadContractDocument documentObj = new FileUploadContractDocument();
			String randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);
			String documentUploadResponse = documentObj.hitUploadContractDocument(filePath, documentFileName, parentSupplierId, randomKeyForDocumentFile);

			if (!documentUploadResponse.contains("totalNumberOfPages=")) {
				throw new SkipException("Couldn't Upload Document to Contract.");
			}

			//Create Contract.
			 contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractFilePath");
			 contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractExtraFieldsFileName");
			 UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractExtraFieldsConfigFileName, "contract misc test c8836",
					"contractDocuments", "newKey", randomKeyForDocumentFile);

			String createResponse = Contract.createContract("contract misc test c8836", true);
			if (ParseJsonResponse.getStatusFromResponse(createResponse).equalsIgnoreCase("success")) {
				contractId = CreateEntity.getNewEntityId(createResponse, "contracts");

				//Validate Document in Contract Tree.
				HashMap<String, String> params = new HashMap<>();
				params.put("hierarchy", "false");
				params.put("offset", "0");

				ContractTreeData treeDataObj = new ContractTreeData();
				treeDataObj.hitContractTreeDataListAPI(61, contractId, params);
				String contractTreeResponse = treeDataObj.getResponseContractTreeData();

				if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
					JSONObject jsonObj = new JSONObject(contractTreeResponse);
					JSONArray jsonArr = jsonObj.getJSONObject("body").getJSONObject("data").getJSONArray("children");

					if (jsonArr.length() > 0) {
						String actualDocName = jsonArr.getJSONObject(0).getString("text");
						String actualDocExtension = jsonArr.getJSONObject(0).getString("extension");

						String expectedDocName = FileUtils.getFileNameWithoutExtension(documentFileName);
						String expectedDocExtension = FileUtils.getFileExtension(documentFileName);

						csAssert.assertTrue(expectedDocName.equalsIgnoreCase(actualDocName), "Expected Document File Name: [" + expectedDocName +
								"] and Actual Document File Name: [" + actualDocName + "].");
						csAssert.assertTrue(expectedDocExtension.equalsIgnoreCase(actualDocExtension), "Expected Document File Extension: [" + expectedDocExtension +
								"] and Actual Document File Extension: [" + actualDocExtension + "]");
					} else {
						csAssert.assertTrue(false, "No Document present in Contract Tree API Response for Contract Id " + contractId);
					}
				} else {
					csAssert.assertTrue(false, "Contract Tree API Response for Contract Id " + contractId + " is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "Couldn't create Contract.");
			}

			FileUtils.deleteFile(filePath + "/" + documentFileName);
			UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractExtraFieldsConfigFileName, "contract misc test c8836",
					"contractDocuments", randomKeyForDocumentFile, "newKey");
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating TC-C8836. " + e.getMessage());
		}

		csAssert.assertAll();
	}
}