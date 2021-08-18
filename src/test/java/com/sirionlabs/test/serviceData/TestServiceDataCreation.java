package com.sirionlabs.test.serviceData;


import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;

import com.sirionlabs.helper.WorkflowActionsHelper;

import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class TestServiceDataCreation {

    private final Logger logger = LoggerFactory.getLogger(com.sirionlabs.test.purchaseOrder.TestPurchaseOrderCreation.class);
    private String configFilePath = null;
    private String configFileName = null;
    private String extraFieldsConfigFilePath = null;
    private String extraFieldsConfigFileName = null;
    private Integer serviceDataEntityTypeId;
    private Integer consumptionEntityTypeId = 176;

    private String listingPayloadConfigFilePath;
    private String listingPayloadConfigFileName;

    private List<String> allFieldGroupsToVerify;
    private Boolean deleteEntity = true;
    private String consumptions = "consumptions";
    private String serviceData = "service data";
    private String publishAction = "publish";

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataCreationTestConfigFileName");

        listingPayloadConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFilePath");
        listingPayloadConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFileName");


        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extrafieldsconfigfilepath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extrafieldsconfigfilename");
        serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;

    }

    @DataProvider()
    public Object[][] dataProviderForTestSDCreation() throws ConfigurationException {
        logger.info("Setting all Service Data Creation Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> flowsToTest = new ArrayList<>();

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
            logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
            flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
        } else {
            String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstovalidate").split(Pattern.quote(","));
            for (String flow : allFlows) {
                if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
                    flowsToTest.add(flow.trim());
                } else {
                    logger.info("Flow having name [{}] not found in Service Data Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "dataProviderForTestSDCreation")
    public void testServiceDataCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int serviceDataId = -1;

        String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath, extraFieldsConfigFileName, "dynamic filter name");
        String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath, extraFieldsConfigFileName, "dynamic filter id");
        String uniqueString = DateUtils.getCurrentTimeStamp();
        String min = "1";
        String max = "1000000";

        ListRendererListData listRendererListData = new ListRendererListData();

        try {


            uniqueString = uniqueString.replaceAll("_", "");
            uniqueString = uniqueString.replaceAll(" ", "");
            uniqueString = uniqueString.replaceAll("0", "1");
            uniqueString = uniqueString.substring(10);

            if (filter_name != null) {
                String dynamicField = "dyn" + filter_name;

                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest, "name", "unqString", uniqueString);

                serviceDataId = InvoiceHelper.getServiceDataId(extraFieldsConfigFilePath, configFileName, extraFieldsConfigFileName, flowToTest, 1, uniqueString);

                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString, "unqString");
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString, "unqString");
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest, "name", uniqueString, "unqString");

            } else {
                serviceDataId = InvoiceHelper.getServiceDataId(extraFieldsConfigFilePath, configFileName, extraFieldsConfigFileName, flowToTest, 1, uniqueString);
            }


            if (serviceDataId != -1) {
                //Validate Supplier Name and Contract Name are hyperlinked.

                if (filter_name != null) {
                    Thread.sleep(1000); //Sleeping for 1 sec
                    String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "service data", "payload");
                    String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "service data", "columnidstoignore");
                    payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);
                   // String payload = "";

                    min = new BigDecimal(uniqueString).subtract(new BigDecimal("5")).toString();
                    max = new BigDecimal(uniqueString).add(new BigDecimal("5")).toString();

                    Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(serviceDataEntityTypeId, filter_id,
                            filter_name, min, max, payload, csAssert);

                    String entityId = "";
                    try {
                        entityId = listColumnValuesMap.get("id").split(":;")[1];

                    } catch (Exception e) {

                    }

                    if (!entityId.equalsIgnoreCase(String.valueOf(serviceDataId))) {
                        csAssert.assertTrue(false, "On Listing page Service Data entity " + serviceDataId + " Not Found");
                    } else {
                        logger.info("On Listing page Service Data entity " + serviceDataId + "  Found");
                    }

                    String recordPresent = listRendererListData.chkPartRecIsPresentForDiffUser(serviceDataEntityTypeId, filter_id,
                            filter_name, min, max, payload, csAssert);

                    if(recordPresent.equalsIgnoreCase("Yes") ){
                        csAssert.assertTrue(false,"Record present for different user where is it is not supposed to present for different user");
                    }
                }

                logger.info("Hitting Show API for Service Data Id {}", serviceDataId);
                Show showObj = new Show();
                showObj.hitShow(serviceDataEntityTypeId, serviceDataId);
                String showResponse = showObj.getShowJsonStr();

                if (ParseJsonResponse.validJsonResponse(showResponse)) {

                    if(flowToTest.contains("arc")) {
                        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

                        boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);

                        if (!result) {
                            csAssert.assertTrue(false, "Error while publishing service data");
                        } else {
                            InvoiceHelper invoiceHelper = new InvoiceHelper();
                            ArrayList<Integer> consumptionIds = new ArrayList<>();

                            String status = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                            if (status.equalsIgnoreCase("pass")) {

                            } else {
                                csAssert.assertTrue(false, "Consumptions not generated for service data id " + serviceDataId);

                            }

                            listRendererListData = new ListRendererListData();
                            String sdFilterId = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"service data filter id");

                            if (sdFilterId != null) {

                                String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, consumptions, "payload");
                                String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, consumptions, "columnidstoignore");
                                payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);

                                String filterName = "serviceData";
                                Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(consumptionEntityTypeId, sdFilterId,filterName, serviceDataId, payload,csAssert);

                                if(listColumnValuesMap.size() == 0){
                                    csAssert.assertTrue(false, "On Listing page Consumption not found for Service Data entity " + serviceDataId + " ");
                                }

                            }
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Show API Response for Service Data Id " + serviceDataId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "Couldn't create Service Data for Flow [" + flowToTest + "]");
            }


        } catch (SkipException e) {

            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Service Data Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && serviceDataId != -1) {
                EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId);

                if (filter_name != null) {
                    listRendererListData = new ListRendererListData();
                    //String payload = "";
                    String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "service data", "payload");
                    String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "service data", "columnidstoignore");
                    payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);
                    Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(serviceDataEntityTypeId, filter_id,
                            filter_name, min, max, payload, csAssert);

                    String entityId = "";
                    try {
                        entityId = listColumnValuesMap.get("id").split(":;")[1];

                    } catch (Exception e) {

                    }

                    if (entityId.equalsIgnoreCase(String.valueOf(serviceDataId))) {
                        csAssert.assertTrue(false, "On Listing page Service Data entity " + serviceDataId + "  Found After Deletion");
                    } else {
                        logger.info("On Listing page Service Data entity " + serviceDataId + " Not Found After Deletion");
                    }
                }
            }
            csAssert.assertAll();
        }
    }

}
