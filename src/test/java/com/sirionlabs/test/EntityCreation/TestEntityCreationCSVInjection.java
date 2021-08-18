package com.sirionlabs.test.EntityCreation;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Supplier;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestEntityCreationCSVInjection {
	private final static Logger logger = LoggerFactory.getLogger(TestEntityCreationCSVInjection.class);
	private static String configFilePath;
	private static String configFileName;

	private boolean deleteEntity = true;

	private String flowToTest = "csv injection scenario";

	@BeforeClass
	public void beforeClass() {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CsvInjectionTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("CsvInjectionTestConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

	}

	@DataProvider
	public Object[][] dataProviderForTestEntityCreation(){
		logger.info("Setting all Entity Creation Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();

		String entitiesToTest[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestotest").split(Pattern.quote(","));
		String[] splChars = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "splchars").split(Pattern.quote(","));
		String[] splCharsNames = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "splcharsname").split(Pattern.quote(","));

		for (String entityToTest : entitiesToTest) {
			String creationFileParseConfigFilePath_Alias = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityToTest,"filepath");
			String creationFileParseConfigName_Alias = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityToTest,"filename");
			String creationFileParseConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty(creationFileParseConfigFilePath_Alias);
			String creationFileParseConfigName = ConfigureConstantFields.getConstantFieldsProperty(creationFileParseConfigName_Alias);

			for(int i =0;i<splChars.length;i++){

				allTestData.add(new Object[]{entityToTest,splChars[i],splCharsNames[i],creationFileParseConfigFilePath,creationFileParseConfigName});
			}
		}

		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestEntityCreation")
	public void testEntityCreationCSVTest(String entityToTest,String splChar,String splCharsName,String creationFilePath,String creationFileName) {
		CustomAssert csAssert = new CustomAssert();

		String extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(creationFilePath, creationFileName, "extraFieldsConfigFilePath");
		String extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(creationFilePath, creationFileName, "extraFieldsConfigFileName");
		try {

			splChar = splChar + "_";
			setConfigFile(extraFieldsConfigFilePath, extraFieldsConfigFileName, entityToTest, splChar);

			CreateEntity createEntityObj = new CreateEntity(creationFilePath,creationFileName,extraFieldsConfigFilePath,extraFieldsConfigFileName,flowToTest);

			//Validate Supplier Creation
			logger.info("Creating Supplier for Flow [{}]", flowToTest);

			logger.info("Creating Payload for Entity {}", entityToTest);
			String payloadForCreate = createEntityObj.getCreatePayload(entityToTest,  true, false);

			if (payloadForCreate != null) {
				logger.info("Hitting Create Api for Entity {}", entityToTest);
				Create createObj = new Create();
				createObj.hitCreate(entityToTest, payloadForCreate);

				String createResponse = createObj.getCreateJsonStr();

				if (ParseJsonResponse.validJsonResponse(createResponse)) {
					JSONObject jsonObj = new JSONObject(createResponse);
					String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
					String expectedResult = ParseConfigFile.getValueFromConfigFile(creationFilePath, creationFileName, flowToTest, "expectedResult");
					logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

					if (expectedResult.trim().equalsIgnoreCase("success")) {

					} else {
						if (createStatus.trim().equalsIgnoreCase("success")) {
							csAssert.assertTrue(false, "Entity Created for Flow [" + flowToTest + "]" + " with special Character " + splCharsName + "whereas it was expected not to create.");
						}else {
							checkFieldErrors(entityToTest,createResponse,csAssert);
						}
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Supplier Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Supplier Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			resetConfigFile(extraFieldsConfigFilePath, extraFieldsConfigFileName, entityToTest, splChar);
		}
		csAssert.assertAll();
	}

	private void setConfigFile(String filePath,String fileName,String entityName,String splChar){

		try{
			switch (entityName){
				case "suppliers":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"alias","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"address","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"email","splChar",splChar);
					break;
				case "obligations":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					break;

				case "service levels":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					break;
				case "interpretations":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"title","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"areaOfDisagreement","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"background","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"questions","splChar",splChar);
					break;

				case "issues":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					break;

				case "actions":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					break;
				case "disputes":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					break;

				case "contracts":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"title","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"fieldComment","splChar",splChar);
					break;
				case "change requests":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"assumptions","splChar",splChar);
					break;
				case "service data":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"serviceIdClient","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"serviceIdSupplier","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,"common extra fields","dyn103535","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,"common extra fields","dynamicMetadata","splChar",splChar);
					break;

				case "invoices":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"supplierTaxId","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"poNumber","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"invoiceNumber","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"supplierBankAccount","splChar",splChar);
					break;
				case "work order requests":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"briefDescription","splChar",splChar);
					break;
				case "governance body":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"location","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"goal","splChar",splChar);
					break;

				case "clauses":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"headerLabel","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"subHeader","splChar",splChar);

					break;
				case "contract draft request":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"title","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"counterPartyAddress","splChar",splChar);
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"dealOverview","splChar",splChar);
					break;


				default:
					break;
			}

		}catch (Exception e){
			logger.error("Exception while setting Config File for entity name " + entityName);
		}

	}

	private void resetConfigFile(String filePath,String fileName,String entityName,String splChar){

		try{
			switch (entityName){
				case "suppliers":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"alias",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"address",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"email",splChar,"splChar");
					break;
				case "obligations":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					break;

				case "service levels":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					break;
				case "interpretations":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"title",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"areaOfDisagreement",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"background",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"questions",splChar,"splChar");
					break;

				case "issues":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					break;

				case "actions":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					break;
				case "disputes":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					break;

				case "contracts":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"title",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"fieldComment",splChar,"splChar");
					break;
				case "change requests":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"assumptions",splChar,"splChar");
					break;
				case "service data":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"serviceIdClient",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"serviceIdSupplier",splChar,"splChar");
					break;

				case "invoices":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"supplierTaxId",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"supplierAddress",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"poNumber",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"shiptoAddress",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"billToAddress",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"invoiceNumber",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"supplierBankAccount",splChar,"splChar");
					break;
				case "work order requests":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"briefDescription",splChar,"splChar");
					break;
				case "governance body":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"description",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"location",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"goal",splChar,"splChar");
					break;

				case "clauses":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"name",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"headerLabel",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"subHeader",splChar,"splChar");

					break;
				case "contract draft request":
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"title",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"counterPartyAddress",splChar,"splChar");
					UpdateFile.updateConfigFileProperty(filePath,fileName,flowToTest,"dealOverview",splChar,"splChar");
					break;


				default:
					break;
			}

		}catch (Exception e){
			logger.error("Exception while setting Config File for entity name " + entityName);
		}

	}

	private void checkFieldErrors(String entityName,String createResponse,CustomAssert customAssert){

		try{
			String expectedErrorString  = "The value cannot start with =, +, -, @";
			if(!JSONUtility.validjson(createResponse)){
				customAssert.assertTrue(false,"create Response is not a valid json");
				return;
			}
			JSONObject createRespJson = new JSONObject(createResponse);
			JSONObject fieldErrorJson = createRespJson.getJSONObject("body").getJSONObject("errors").getJSONObject("fieldErrors");
			switch (entityName){
				case "suppliers":
					try {
						String aliasMsg = fieldErrorJson.getJSONObject("alias").get("message").toString();
						String addressMsg = fieldErrorJson.getJSONObject("address").get("message").toString();
						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String emailMsg = fieldErrorJson.getJSONObject("email").get("message").toString();

						if(aliasMsg.equals(expectedErrorString) && addressMsg.equals(expectedErrorString)
						&& nameMsg.equals(expectedErrorString) && emailMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully");
						}else {
							customAssert.assertTrue(false,"Any of the field from alias address name email does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;
				case "obligations":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String descriptionMsg = fieldErrorJson.getJSONObject("description").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && descriptionMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;

				case "service levels":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String descriptionMsg = fieldErrorJson.getJSONObject("description").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && descriptionMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;

				case "interpretations":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("title").get("message").toString();
						String descriptionMsg = fieldErrorJson.getJSONObject("areaOfDisagreement").get("message").toString();
						String backgroundMsg = fieldErrorJson.getJSONObject("background").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && descriptionMsg.equals(expectedErrorString)
							&& backgroundMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;

				case "issues":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String descriptionMsg = fieldErrorJson.getJSONObject("description").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && descriptionMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;
				case "actions":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String descriptionMsg = fieldErrorJson.getJSONObject("description").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && descriptionMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;
				case "disputes":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String descriptionMsg = fieldErrorJson.getJSONObject("description").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && descriptionMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;

				case "contracts":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String titleMsg = fieldErrorJson.getJSONObject("title").get("message").toString();
						String fieldCommentMsg = fieldErrorJson.getJSONObject("fieldComment").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && titleMsg.equals(expectedErrorString)&&
								fieldCommentMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;
				case "change requests":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String descMsg = fieldErrorJson.getJSONObject("description").get("message").toString();


						if(nameMsg.equals(expectedErrorString) && descMsg.equals(expectedErrorString)){
							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}

					break;
				case "service data":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String descMsg = fieldErrorJson.getJSONObject("description").get("message").toString();
						String serviceIdClientMsg = fieldErrorJson.getJSONObject("serviceIdClient").get("message").toString();
						String serviceIdSupplierMsg = fieldErrorJson.getJSONObject("serviceIdSupplier").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && descMsg.equals(expectedErrorString)&&
								serviceIdClientMsg.equals(expectedErrorString) && serviceIdSupplierMsg.equals(expectedErrorString)){

							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}
					break;

				case "invoices":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("name").get("message").toString();
						String supplierTaxIdMsg = fieldErrorJson.getJSONObject("supplierTaxId").get("message").toString();
						String invoiceNumberMsg = fieldErrorJson.getJSONObject("invoiceNumber").get("message").toString();
						String supplierBankAccountMsg = fieldErrorJson.getJSONObject("supplierBankAccount").get("message").toString();
						String poNumberMsg = fieldErrorJson.getJSONObject("poNumber").get("message").toString();

						if(nameMsg.equals(expectedErrorString) && supplierTaxIdMsg.equals(expectedErrorString)&&
								 poNumberMsg.equals(expectedErrorString) && invoiceNumberMsg.equals(expectedErrorString)&& supplierBankAccountMsg.equals(expectedErrorString)){

							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}
					break;
				case "work order requests":
					try {

						String briefDescriptionMsg = fieldErrorJson.getJSONObject("briefDescription").get("message").toString();

						if(briefDescriptionMsg.equals(expectedErrorString)){

							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}
					break;
				case "clauses":
					try {

						String headerLabelMsg = fieldErrorJson.getJSONObject("headerLabel").get("message").toString();

						if(headerLabelMsg.equals(expectedErrorString)){

							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}
					break;
				case "contract draft request":
					try {

						String nameMsg = fieldErrorJson.getJSONObject("title").get("message").toString();
						String counterPartyAddressMsg = fieldErrorJson.getJSONObject("counterPartyAddress").get("message").toString();
						String dealOverviewMsg = fieldErrorJson.getJSONObject("dealOverview").get("message").toString();

						if(nameMsg.equals(expectedErrorString)&&counterPartyAddressMsg.equals(expectedErrorString)&&
								dealOverviewMsg.equals(expectedErrorString)){

							logger.info("All Field Name checks passed successfully for entity " + entityName);
						}else {
							logger.error("Any of the field does not contain expected error message " + expectedErrorString  + " for entity " + entityName);
							customAssert.assertTrue(false,"Any of the field does not contain expected error message " + expectedErrorString );
						}

					}catch (Exception e){
						customAssert.assertTrue(false,"Exception while parsing Field Errors Messages");
					}
					break;

				default:
					break;
			}

		}catch (Exception e){
			customAssert.assertTrue(false,"Exception while parsing Field Errors from create Response");
		}

	}


}