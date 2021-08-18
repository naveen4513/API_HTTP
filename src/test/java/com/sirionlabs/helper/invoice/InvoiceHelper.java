package com.sirionlabs.helper.invoice;

import com.jcraft.jsch.HASH;
import com.sirionlabs.api.clientAdmin.conversionMatrix.ConversionMatrixUpdate;
import com.sirionlabs.api.clientAdmin.invoiceCopy.Format;
import com.sirionlabs.api.clientAdmin.invoiceCopy.Template;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.entityWorkflowActions.Publish;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.invoice.InvoiceReValidationCheck;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

public class InvoiceHelper {

    private final static Logger logger = LoggerFactory.getLogger("InvoiceHelper.class");

    public ArrayList<Integer> consumptionIds = new ArrayList<>();
    private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private String contractEntity = "contracts";


    private Long pricingSchedulerTimeOut = 120000L;
    private Long forecastSchedulerTimeOut = 120000L;
    private Long consumptionToBeCreatedTimeOut = 120000L;

    private Long pollingTime = 5000L;
    private int serviceDataEntityTypeId = 64;
    private int consumptionsEntityTypeId = 176;
    private int invoiceEntityTypeId = 67;
    private int invoiceLineItemEntityTypeId = 165;

    int ARCRRCTabId = 311;
    int chargesTabId = 309;

    public static int getContractId(String contractConfigFilePath, String contractConfigFileName, String contractExtraFieldsConfigFileName, String contractSectionName) {
        int contractId = -1;
        try {
            //Create New Contract
            Boolean createLocalContract = true;

            String createResponse = Contract.createContract(contractConfigFilePath, contractConfigFileName, contractConfigFilePath,
                    contractExtraFieldsConfigFileName, contractSectionName, createLocalContract);

            contractId = CreateEntity.getNewEntityId(createResponse, "contracts");

        } catch (Exception e) {
            logger.error("Exception while getting Contract Id using Flow Section [{}]. {}", contractSectionName, e.getStackTrace());
        }
        return contractId;
    }

    public static int getMultiSupplierContract(String contractConfigFilePath, String contractConfigFileName, String contractExtraFieldsConfigFilePath,
                                               String contractExtraFieldsConfigFileName, String contractCreateSection) {
        try {
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(contractConfigFilePath, contractConfigFileName, contractCreateSection);

            String[] parentSupplierIdsArr = flowProperties.get("sourceid").split(",");
            String payload = "{\"documentTypeId\":5,\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}," +
                    "\"actualParentEntity\":{\"entityIds\":[" + parentSupplierIdsArr[0] + "],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" +
                    parentSupplierIdsArr[0] + "],\"entityTypeId\":1}}";

            String createResponse = multiSupplierCreateResponse(payload, contractCreateSection, contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFilePath,contractExtraFieldsConfigFileName);

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                String status = ParseJsonResponse.getStatusFromResponse(createResponse);

                if (status.equalsIgnoreCase("success")) {
                    return CreateEntity.getNewEntityId(createResponse, "contracts");
                } else {
                    logger.error("Multi Supplier Contract Creation failed due to " + status);
                }
            } else {
                logger.error("Contract Create API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Multi Supplier Contract. " + e.getMessage());
        }

        return -1;
    }

    private static String multiSupplierCreateResponse(String newPayload, String contractCreateSection, String contractConfigFilePath, String contractConfigFileName,
                                                      String contractExtraFieldsConfigFilePath, String contractExtraFieldsConfigFileName) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFilePath, contractExtraFieldsConfigFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, contractExtraFieldsConfigFilePath,
                        contractExtraFieldsConfigFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                    Create createObj = new Create();
                    createObj.hitCreate("contracts", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Contract Create Payload is null and hence cannot create Multi Supplier Contract.");
                }
            } else {
                logger.error("New V1 API Response is an Invalid JSON for Contracts.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }

    public static int getServiceDataId(String serviceDataConfigFilePath, String serviceDataConfigFileName, String serviceDataExtraFieldsConfigFileName, String serviceDataSectionName, int contractId) {
        int serviceDataId = -1;
        try {

            String serviceDataEntity = "service data";

            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "new client", "newClient" + contractId);
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "new supplier", "newSupplier" + contractId);

            Boolean createLocalServiceData = true;

            String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                    serviceDataExtraFieldsConfigFileName, serviceDataSectionName, createLocalServiceData);

            serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "newClient" + contractId, "new client");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "newSupplier" + contractId, "new supplier");

        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", serviceDataSectionName, e.getStackTrace());
        }
        return serviceDataId;
    }

    public static int getServiceDataId(String serviceDataConfigFilePath, String serviceDataConfigFileName, String serviceDataExtraFieldsConfigFileName, String serviceDataSectionName, String uniqueString) {
        int serviceDataId = -1;
        try {

            String serviceDataEntity = "service data";

            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "new client", "newClient" + uniqueString);
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "new supplier", "newSupplier" + uniqueString);

            Boolean createLocalServiceData = true;

            String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                    serviceDataExtraFieldsConfigFileName, serviceDataSectionName, createLocalServiceData);

            serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "newClient" + uniqueString, "new client");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "newSupplier" + uniqueString, "new supplier");

        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", serviceDataSectionName, e.getStackTrace());
        }
        return serviceDataId;
    }

    public static String getServiceDataCreateResponseForExistingContract(String serviceDataConfigFilePath, String serviceDataConfigFileName, String serviceDataExtraFieldsConfigFileName, String serviceDataSectionName) {
        int serviceDataId = -1;
        String createResponse = "";
        try {

            String serviceDataEntity = "service data";
            int randomId = RandomNumbers.getRandomNumberWithinRangeIndex(10000, 99999);
            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "new client", "newClient" + randomId);
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "new supplier", "newSupplier" + randomId);

            Boolean createLocalServiceData = true;

            createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                    serviceDataExtraFieldsConfigFileName, serviceDataSectionName, createLocalServiceData);

            serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "newClient" + randomId, "new client");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "newSupplier" + randomId, "new supplier");

        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", serviceDataSectionName, e.getStackTrace());
        }
        return createResponse;
    }


    //added by srijan for multiple service data in a contract
    public static int getServiceDataIdForDifferentClientAndSupplierId(String serviceDataConfigFilePath, String serviceDataConfigFileName, String serviceDataExtraFieldsConfigFileName, String serviceDataSectionName, int contractId) {
        int serviceDataId = -1;
        try {

            String serviceDataEntity = "service data";
            //Update Service Data Extra Fields.

            String clientId = "newClient" + contractId + RandomNumbers.getRandomNumberWithinRangeIndex(100, 999);
            String supplierId = "supplier" + contractId + RandomNumbers.getRandomNumberWithinRangeIndex(100, 999);

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "new client", clientId);
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "new supplier", supplierId);

            Boolean createLocalServiceData = true;

            String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                    serviceDataExtraFieldsConfigFileName, serviceDataSectionName, createLocalServiceData);

            serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    clientId, "new client");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    supplierId, "new supplier");

        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", serviceDataSectionName, e.getStackTrace());
        }
        return serviceDataId;
    }

    public static int getInvoiceId(String invoiceConfigFilePath, String invoiceConfigFileName, String invoiceExtraFieldsConfigFileName,
                                   String invoiceLineItemConfigFilePath, String invoiceLineItemConfigFileName, String flowToTest) {
        int invoiceId = -1;
        try {

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    flowToTest, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTest, invoiceId);
            int invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");

            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest, "sourcename",
                    invoiceName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest, "sourceid",
                    String.valueOf(invoiceId));
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return invoiceId;
    }

    public static int getInvoiceId(String invoiceConfigFilePath, String invoiceConfigFileName, String invoiceExtraFieldsConfigFileName,
                                   String invoiceLineItemConfigFilePath, String invoiceLineItemConfigFileName, String flowToTestInvoice, String flowToTestLineItem) {
        int invoiceId = -1;
        try {

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    flowToTestInvoice, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTestInvoice, invoiceId);
            int invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");

            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTestLineItem, "sourcename",
                    invoiceName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTestLineItem, "sourceid",
                    String.valueOf(invoiceId));
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTestInvoice, e.getStackTrace());
        }
        return invoiceId;
    }

    public static int   getInvoiceLineItemId(String invoiceLineItemConfigFilePath, String invoiceLineItemConfigFileName, String invoiceLineItemExtraFieldsConfigFileName, String invoiceLineItemSectionName, int serviceDataId) {

        int lineItemId = -1;
        try {
            String serviceDataEntity = "service data";
            String invoiceLineItemEntity = "invoice line item";

            logger.info("Updating Invoice Line Item Property Service Id Supplier in Extra Fields Config File for Flow [{}] and Service Data Id {}.",
                    invoiceLineItemSectionName, serviceDataId);
            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName(serviceDataEntity);
            String serviceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "title");
            String serviceIdSupplierName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "serviceIdSupplier");
            String serviceIdSupplierUpdatedName = serviceDataName + " (" + serviceIdSupplierName + ")";
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new name", serviceIdSupplierUpdatedName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new id", String.valueOf(serviceDataId));

            String createResponse = InvoiceLineItem.createInvoiceLineItem(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemConfigFilePath,
                    invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, true);
            lineItemId = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

            //Reverting Invoice Line Item Extra Fields changes.
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", serviceIdSupplierUpdatedName, "new name");
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", String.valueOf(serviceDataId), "new id");

        } catch (Exception e) {
            logger.error("Exception while getting Invoice Line Item Id using Flow Section [{}]. {}", invoiceLineItemSectionName, e.getStackTrace());
        }
        return lineItemId;
    }

    // this method will check whether consumption has been created or not
    // return "pass" if created
    // return "fail" if something went wrong in code or assumption
    // return "skip" if consumption would not created in specified time consumptionToBeCreatedTimeOut
    public String waitForConsumptionToBeCreated(String flowToTest, int serviceDataId) {

        int offset = 0; // default value for creating payload
        int size = 20; // default value for creating payload
        String result = "pass";

        Long pollingTime = 5000L;
        Long pricingSchedulerTimeOut = 600000L;
        Long consumptionToBeCreatedTimeOut = 3000000L;

        int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

        logger.info("Waiting for Consumption to be Created for Flow [{}].", flowToTest);
        try {
            Show show = new Show();
            show.hitShow(serviceDataEntityTypeId, serviceDataId);
            String showPageResponseStr = show.getShowJsonStr();
            List<String> dataUrl = show.getShowPageTabUrl(showPageResponseStr, Show.TabURL.dataURL);

            String consumptionDataURL = null;
            for (String Url : dataUrl) {
                if (Url.contains("listRenderer/list/376/tablistdata")) // for consumption
                {
                    consumptionDataURL = Url;
                    break;
                } else
                    continue;

            }


            if (consumptionDataURL != null) {

                logger.info("Time Out for Consumption to be created is {} milliseconds", consumptionToBeCreatedTimeOut);
                long timeSpent = 0;


                Boolean taskCompleted = false;
                logger.info("Checking if Consumption has been created or not.");

                String payload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":" +
                        offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc\",\"filterJson\":{}}}";

                while (timeSpent < consumptionToBeCreatedTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);
                    logger.info("Hitting tab data API for Consumption for service data Id : [{}]", serviceDataId);

                    String showPageDataUrlResponseStr = Show.hitshowPageTabUrl(consumptionDataURL, payload);
                    JSONObject showPageDataUrlResponseJson = new JSONObject(showPageDataUrlResponseStr);

                    int numberOfConsumption = showPageDataUrlResponseJson.getJSONArray("data").length();


                    if (numberOfConsumption > 0) {
                        taskCompleted = true;
                        logger.info("Consumptions have been created. ");

                        for (int i = 0; i < numberOfConsumption; i++) {

                            JSONObject data = showPageDataUrlResponseJson.getJSONArray("data").getJSONObject(i);
                            Set<String> keys = data.keySet();
                            for (String key : keys) {
                                String columnName = data.getJSONObject(key).getString("columnName");
                                if (columnName.contentEquals("id")) {
                                    consumptionIds.add(Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]));
                                    break;
                                } else
                                    continue;

                            }
                        }

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("time spent is : [{}]", timeSpent);
                        logger.info("Consumptions haven't been created yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }


            } else {
                logger.error("There is no consumption URL in the show page response of Service data : [{}] for flow : [{}]", serviceDataId, flowToTest);
                result = "skip";
                return result;
            }


        } catch (Exception e) {
            logger.error("Exception while Waiting for Consumption to get created to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }

        return result;
    }

    // this will create the row , <columnNumber,value> for editing the ARC/RRC Sheet
    public static Map<Integer, Map<Integer, Object>> getValuesMapForArcRrcSheet(String flowsConfigFilePath, String flowsConfigFileName, String flowToTest) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {

            int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "numberofcolumntoeditforeachrowforarc"));

            String[] arcRowNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "arcrownumber").trim().split(Pattern.quote(","));

            String[] arcColumnNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "arccolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "arcvalue").trim().split(Pattern.quote(","));

            for (int i = 0; i < arcRowNumber.length; i++) {

                Map<Integer, Object> innerValuesMap = new HashMap<>();
                for (int j = 0; j < numberOfColumnToEditForEachRow; j++) {
                    innerValuesMap.put(Integer.parseInt(arcColumnNumber[i * numberOfColumnToEditForEachRow + j]), values[i * numberOfColumnToEditForEachRow + j]);
                }
                valuesMap.put(Integer.parseInt(arcRowNumber[i]), innerValuesMap);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Pricing Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    // this will create the row , <columnNumber,value> for editing the Split Sheet
    public static Map<Integer, Map<Integer, Object>> getValuesMapForSplitSheet(String flowsConfigFilePath, String flowsConfigFileName, String flowToTest) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {

            int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "numberofcolumntoeditforeachrowforsplit"));

            String[] splitRowNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "splitrownumber").trim().split(Pattern.quote(","));

            String[] splitColumnNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "splitcolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "splitvalues").trim().split(Pattern.quote(","));

            for (int i = 0; i < splitRowNumber.length; i++) {

                Map<Integer, Object> innerValuesMap = new HashMap<>();
                for (int j = 0; j < numberOfColumnToEditForEachRow; j++) {
                    innerValuesMap.put(Integer.parseInt(splitColumnNumber[i * numberOfColumnToEditForEachRow + j]), values[i * numberOfColumnToEditForEachRow + j]);
                }
                valuesMap.put(Integer.parseInt(splitRowNumber[i]), innerValuesMap);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Split Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    /*
	This method will return the status of Pricing Scheduler as String.
	Possible Values are 'Pass', 'Fail', 'Skip'.
	'Pass' specifies that pricing scheduler completed and records processed successfully.
	'Fail' specifies that pricing scheduler failed
	'Skip' specifies that pricing scheduler didn't finish within time.
	 */
    public static String waitForPricingScheduler(String flowToTest, List<Integer> oldIds) {
        String result = "pass";
        Long pollingTime = 5000L;
        Long pricingSchedulerTimeOut = 1200000L;

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
                    if (newTaskStatus != null && (newTaskStatus.trim().equalsIgnoreCase("Completed") || newTaskStatus.trim().equalsIgnoreCase("Completed With Errors"))) { //edited by srijan
                        taskCompleted = true;
                        logger.info("Pricing Upload Task Completed. ");
                        logger.info("Checking if Pricing Upload Task failed or not.");
                        if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                            result = "fail";

                        break;
                    }
                    else {
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
                //Thread.sleep(pricingSchedulerTimeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Pricing Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }
        return result;
    }

    public static List<Integer> getAllTaskIds() {
        Fetch fetchObj = new Fetch();
        fetchObj.hitFetch();
        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
        return allTaskIds;
    }

    public static void updateServiceDataAndInvoiceConfig(String serviceDataConfigFilePath, String serviceDataConfigFileName,
                                                         String invoiceConfigFilePath, String invoiceConfigFileName, String flowToTest, int contractId) {
        try {
            if (contractId != -1) {
                logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", flowToTest, contractId);

                int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
                String contractName = ShowHelper.getValueOfField(contractEntityTypeId, contractId, "title");
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, flowToTest, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, flowToTest, "sourceid",
                        String.valueOf(contractId));


                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, flowToTest, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, flowToTest, "sourceid", String.valueOf(contractId));
            }
        } catch (Exception e) {
            logger.error("Exception while updating Service Data & Invoice Config File for Flow [{}] and Contract Id {}. {}", flowToTest, contractId, e.getStackTrace());
        }
    }

    public static void updateServiceDataAndInvoiceConfigDistinct(String serviceDataConfigFilePath, String serviceDataConfigFileName,
                                                                 String invoiceConfigFilePath, String invoiceConfigFileName, String invoiceSectionName, String serviceDataSectionName, int contractId) {
        try {
            if (contractId != -1) {
                logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", serviceDataSectionName, contractId);

                int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
                String contractName = ShowHelper.getValueOfField(contractEntityTypeId, contractId, "title");
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourceid",
                        String.valueOf(contractId));


                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourceid", String.valueOf(contractId));
            }
        } catch (Exception e) {
            logger.error("Exception while updating Service Data & Invoice Config File for Flow [{}] and Contract Id {}. {}", serviceDataSectionName, contractId, e.getStackTrace());
        }
    }

    public static void updateMultipleServiceDataAndInvoiceConfigDistinct(String serviceDataConfigFilePath, String serviceDataConfigFileName,
                                                                         String invoiceConfigFilePath, String invoiceConfigFileName, String invoiceSectionName, String serviceDataSectionNames, int contractId) {
        try {
            if (contractId != -1) {
                logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", serviceDataSectionNames, contractId);

                int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
                String contractName = ShowHelper.getValueOfField(contractEntityTypeId, contractId, "title");

                for (String serviceDataSectionName : serviceDataSectionNames.split(",")) {
                    UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourcename", contractName);
                    UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourceid",
                            String.valueOf(contractId));
                }


                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourceid", String.valueOf(contractId));
            }
        } catch (Exception e) {
            logger.error("Exception while updating Service Data & Invoice Config File for Flow [{}] and Contract Id {}. {}", serviceDataSectionNames, contractId, e.getStackTrace());
        }
    }


    public static void updateServiceDataAndInvoiceConfigForMultipleInvoice(String serviceDataConfigFilePath, String serviceDataConfigFileName,
                                                                           String invoiceConfigFilePath, String invoiceConfigFileName, String flowToTest, int contractId, int size) {
        try {
            if (contractId != -1) {
                logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", flowToTest, contractId);

                int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
                String contractName = ShowHelper.getValueOfField(contractEntityTypeId, contractId, "title");
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, flowToTest, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, flowToTest, "sourceid",
                        String.valueOf(contractId));

                int i = 0;
                String tempAppendInvoiceSectionName;
                while (i++ < size) {

                    tempAppendInvoiceSectionName = flowToTest + " " + i;

                    UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, tempAppendInvoiceSectionName, "sourcename", contractName);
                    UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, tempAppendInvoiceSectionName, "sourceid", String.valueOf(contractId));
                }
            }
        } catch (Exception e) {
            logger.error("Exception while updating Service Data & Invoice Config File for Flow [{}] and Contract Id {}. {}", flowToTest, contractId, e.getStackTrace());
        }
    }

    // this will update the final Consumption in Created Consumption
    public static boolean updateFinalConsumption(String flowToTest, int consumptionId, double finalConsumption) {
        boolean result = true;

        try {
            String consumptionEntity = "consumptions";
            Edit edit = new Edit();
            String editAPIResponse = edit.hitEdit(consumptionEntity, consumptionId);
            JSONObject editAPIResponseJson = new JSONObject(editAPIResponse);

            JSONObject editPostAPIPayload = editAPIResponseJson;
            editPostAPIPayload.getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").put("values", finalConsumption);

            logger.info("editPostAPIPayload is : {}", editPostAPIPayload);

            String editPostAPIResponse = edit.hitEdit(consumptionEntity, editPostAPIPayload.toString());
            JSONObject editPostAPIResponseJson = new JSONObject(editPostAPIResponse);

            if ((editPostAPIResponseJson.has("header") && editPostAPIResponseJson.getJSONObject("header").has("response")
                    && editPostAPIResponseJson.getJSONObject("header").getJSONObject("response").has("status")
                    && editPostAPIResponseJson.getJSONObject("header").getJSONObject("response").getString("status")
                    .trim().equalsIgnoreCase("success"))) {
                logger.info("Consumption has been updated successfully for flow [{}]", flowToTest);
            } else {
                logger.error("Error While Updating final Consumption in Created Consumption having id : [{}] for flow [{}]", consumptionId, flowToTest);
                result = false;
                return result;

            }


        } catch (Exception e) {
            logger.error("Error While Updating final Consumption in Created Consumption having id : [{}]", consumptionId);
            result = false;
            return result;

        }

        return result;
    }

    public static Boolean verifyInvoiceLineItem(String flowsConfigFilePath, String flowsConfigFileName, String flowToTest, int invoiceLineItemId, CustomAssert csAssert) {

        boolean lineItemValidationStatus = false;

        try {

            String invoiceLineItemEntity = "invoice line item";

            logger.info("Verifying Invoice Line Item for Flow [{}] having Id {}.", flowToTest, invoiceLineItemId);
            int invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity);
            long timeSpent = 0;
            Long lineItemValidationTimeOut = 1200000L;

            while (timeSpent <= lineItemValidationTimeOut && ShowHelper.isLineItemUnderOngoingValidation(invoiceLineItemEntityTypeId, invoiceLineItemId)) {
                logger.info("Invoice Line Item having Id {} is still Under Ongoing Validation. Waiting for it to finish validation.", invoiceLineItemId);
                logger.info("time spent is : [{}]", timeSpent);
                Thread.sleep(10000);
                timeSpent += 10000;
            }

            if (timeSpent < lineItemValidationTimeOut) {
                //Line Item Validation is Completed.
                String expectedResult = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "lineitemexpectedresult");
                String actualResult = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invoiceLineItemId, "validationStatus");
                logger.info("Expected Result is [{}] and Actual Result is [{}].", expectedResult, actualResult);
                if (actualResult.trim().toLowerCase().contains(expectedResult.trim().toLowerCase())) {
                    lineItemValidationStatus = true;
                } else {

                    csAssert.assertTrue(false,
                            "Invoice Line Item Validation failed as Expected Value is " + expectedResult + " and Actual Value is " + actualResult);
                }
            } else {
                //Line Item Validation is not yet Completed.
                logger.info("Invoice Line Item Validation couldn't be completed for Flow [{}] within TimeOut {} milliseconds", flowToTest, lineItemValidationTimeOut);

                logger.error("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [{}]", flowToTest);
                csAssert.assertTrue(false, "FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [" +
                        flowToTest + "]");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Invoice Line Item for Flow [{}] having Id {}. {}", flowToTest, invoiceLineItemId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while verifying Invoice Line Item for Flow [" + flowToTest + "] having Id " + invoiceLineItemId +
                    ". " + e.getMessage());
        }
        return lineItemValidationStatus;
    }

    public static Boolean verifyInvoiceLineItemValidationStatus(String expectedResult, int invoiceLineItemId, CustomAssert csAssert) {

        Boolean lineItemValidationStatus = false;

        try {

            String invoiceLineItemEntity = "invoice line item";

            logger.info("Verifying Invoice Line Item having Id {}.", invoiceLineItemId);
            int invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity);
            long timeSpent = 0;
            Long lineItemValidationTimeOut = 1200000L;

            while (timeSpent <= lineItemValidationTimeOut && ShowHelper.isLineItemUnderOngoingValidation(invoiceLineItemEntityTypeId, invoiceLineItemId)) {
                logger.info("Invoice Line Item having Id {} is still Under Ongoing Validation. Waiting for it to finish validation.", invoiceLineItemId);
                logger.info("time spent is : [{}]", timeSpent);
                Thread.sleep(10000);
                timeSpent += 10000;
            }

            if (timeSpent < lineItemValidationTimeOut) {
                //Line Item Validation is Completed.
                String actualResult = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invoiceLineItemId, "validationStatus");
                logger.info("Expected Result is [{}] and Actual Result is [{}].", expectedResult, actualResult);
                if (actualResult.trim().toLowerCase().equalsIgnoreCase(expectedResult.trim().toLowerCase())) {
                    lineItemValidationStatus = true;
                } else {

                    csAssert.assertTrue(false,
                            "Invoice Line Item Validation failed as Expected Value is " + expectedResult + " and Actual Value is " + actualResult);
                }
            } else {
                //Line Item Validation is not yet Completed.
                logger.info("Invoice Line Item Validation couldn't be completed for within TimeOut {} milliseconds", lineItemValidationTimeOut);

                logger.error("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow ");
                csAssert.assertTrue(false, "FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing ");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Invoice Line Item having Id {}. {}", invoiceLineItemId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while verifying Invoice Line Item having Id " + invoiceLineItemId +
                    ". " + e.getMessage());
        }
        return lineItemValidationStatus;
    }

    public static Boolean downloadAndEditPricingFile(String configFilePath, String configFileName, String pricingTemplateFilePath, String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj, CustomAssert csAssert) {
        boolean pricingFile = true;
        try {
            //Download Pricing Template
            logger.info("Downloading Pricing Template for Flow [{}]", flowToTest);
            if (!pricingObj.downloadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataId)) {
                logger.error("Pricing Template Download failed for Flow [{}].", flowToTest);
                return false;
            }
            if (flowToTest.contains("split")) {
                if (!(editPricingFileForSplit(configFilePath, configFileName, pricingTemplateFilePath, templateFileName, flowToTest, serviceDataId))) {
                    csAssert.assertTrue(false, "Split file edit failed for flow " + flowToTest);
                    return false;
                }
            }
            if (!editPricingFileForPricing(configFilePath, configFileName, pricingTemplateFilePath, templateFileName, flowToTest)) {
                csAssert.assertTrue(false, "Pricing file edit failed for flow " + flowToTest);
                return false;
            }
            String numberOfColumnsToEditForArc = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "numberofcolumntoeditforeachrowforarc");

            if (numberOfColumnsToEditForArc != null) {
                if (!(editPricingFileForARCRRC(configFilePath, configFileName, pricingTemplateFilePath, templateFileName, flowToTest, serviceDataId))) {
                    csAssert.assertTrue(false, "ARC/RRC file edit failed for flow " + flowToTest);
                    return false;
                }
            }

        } catch (Exception e) {
            logger.error("Exception while getting Pricing Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
            pricingFile = false;
        }
        return pricingFile;

    }

//    public static Boolean downloadAndEditPricingFile(String configFilePath,String configFileName,String pricingTemplateFilePath,String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj,String pricingSheetName, CustomAssert csAssert) {
//        Boolean pricingFile = true;
//        try {
//            //Download Pricing Template
//            logger.info("Downloading Pricing Template for Flow [{}]", flowToTest);
//            if (!pricingObj.downloadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataId)) {
//                logger.error("Pricing Template Download failed for Flow [{}].", flowToTest);
//                return false;
//            }
//            if(flowToTest.contains("split")) {
//                if (!(editPricingFileForSplit(configFilePath,configFileName,pricingTemplateFilePath,templateFileName, flowToTest, serviceDataId))) {
//                    csAssert.assertTrue(false, "Split file edit failed for flow " + flowToTest);
//                    return false;
//                }
//            }
//            if (!editPricingFileForPricing(configFilePath,configFileName,pricingTemplateFilePath,templateFileName, flowToTest)) {
//                csAssert.assertTrue(false, "Pricing file edit failed for flow " + flowToTest);
//                return false;
//            }
//
//            if (!(editPricingFileForARCRRC(configFilePath,configFileName,pricingTemplateFilePath,templateFileName, flowToTest, serviceDataId))) {
//                csAssert.assertTrue(false, "ARC/RRC file edit failed for flow " + flowToTest);
//                return false;
//            }
//
//            int numberOfRowsToEdit =  Integer.parseInt(XLSUtils.getNoOfRows(pricingTemplateFilePath,templateFileName,pricingSheetName).toString());
//
//            XLSUtils.updateColumnValue(pricingTemplateFilePath,templateFileName,pricingSheetName,);
//
//        }catch (Exception e) {
//            logger.error("Exception while getting Pricing Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
//            pricingFile = false;
//        }
//        return pricingFile;
//
//    }

    // this function will edit the split sheet based on the map created in getValuesMapForSplitSheet
    public static Boolean editPricingFileForSplit(String configFilePath, String configFileName, String pricingTemplateFilePath, String templateFileName, String flowToTest, Integer serviceDataId) {
        Boolean pricingFile;
        try {
            String splitSheetNameInXLSXFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "splitsheetname");
            int splitRowNumber = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "startingrownum"));
            XLSUtils.copyRowData(pricingTemplateFilePath, templateFileName, splitSheetNameInXLSXFile, splitRowNumber, splitRowNumber + 1);

            Map<Integer, Map<Integer, Object>> splitValuesMap = InvoiceHelper.getValuesMapForSplitSheet(configFilePath, configFileName, flowToTest);
            boolean editTemplate = XLSUtils.editMultipleRowsData(pricingTemplateFilePath, templateFileName, splitSheetNameInXLSXFile, splitValuesMap);

            if (editTemplate) {
                pricingFile = true;
                return pricingFile;
            } else {
                logger.error("Error While Updating the Split Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
                pricingFile = false;
                return pricingFile;
            }


        } catch (Exception e) {
            logger.error("Exception while getting Split Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
            pricingFile = false;
        }
        return pricingFile;
    }

    // this function will edit the ARC/RRC sheet based on the map created in getValuesMapForArcRrcSheet
    public static Boolean editPricingFileForARCRRC(String configFilePath, String configFileName, String pricingTemplateFilePath, String templateFileName, String flowToTest, Integer serviceDataId) {
        Boolean pricingFile = false;
        try {

            String ARCRRCSheetNameInXLSXFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "arcsheetname");
            Map<Integer, Map<Integer, Object>> arcValuesMap = InvoiceHelper.getValuesMapForArcRrcSheet(configFilePath, configFileName, flowToTest);
            boolean editTemplate = XLSUtils.editMultipleRowsData(pricingTemplateFilePath, templateFileName, ARCRRCSheetNameInXLSXFile, arcValuesMap);

            if (editTemplate == true) {
                return true;
            } else {
                logger.error("Error While Updating the ARC Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
                return false;
            }


        } catch (Exception e) {
            logger.error("Exception while getting ARC/RRC Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;
    }

    public static Boolean editPricingFileForPricing(String configFilePath, String configFileName, String pricingTemplateFilePath, String templateFileName, String flowToTest) {

        //Edit Pricing Template
        logger.info("Editing Pricing Template for Flow [{}]", flowToTest);

        String pricingTemplateSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "pricingstemplatesheetname");
        Integer totalRowsToEdit = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "numberofrowstoedit"));
        Integer startingRowNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "startingrownum"));
        Integer volumeColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "volumecolumnnum"));
        Integer rateColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "ratecolumnnum"));
        List<String> volumeColumnValues = getIdList(configFilePath, configFileName, flowToTest, "volumecolumnvalues");
        List<String> rateColumnValues = getIdList(configFilePath, configFileName, flowToTest, "ratecolumnvalues");

        int count = 0;
        for (int rowNum = startingRowNum; rowNum < (startingRowNum + totalRowsToEdit); rowNum++) {
            Map<Integer, Object> columnNumAndValueMap = new HashMap<>();
            columnNumAndValueMap.put(volumeColumnNum, volumeColumnValues.get(count));
            columnNumAndValueMap.put(rateColumnNum, rateColumnValues.get(count));

            Boolean isSuccess = XLSUtils.editRowData(pricingTemplateFilePath, templateFileName, pricingTemplateSheetName, rowNum, columnNumAndValueMap);

            count++;
            if (!isSuccess) {
                logger.error("Pricing Template Editing Failed for Flow [{}].", flowToTest);

                return false;
            }

        }
        return true;
    }

    private static List<String> getIdList(String configFilePath, String configFileName, String sectionName, String propertyName) {

        String value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName);
        List<String> idList = new ArrayList<>();

        if (!value.trim().equalsIgnoreCase("")) {
            String ids[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName).split(",");

            for (String id : ids)
                idList.add(id.trim());
        }
        return idList;
    }

    // this function will check whether any data has been created under Charges tab of Service Data or Not
    public boolean isChargesCreated(int serviceDataId) {

        logger.info("Checking whether data under Charges tab has/have been created and visible for serviceData" + serviceDataId);
        int serviceDataEntityTypeId = 64;
        int chargesTabId = 309;

        GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, chargesTabId, serviceDataId);
        getTabListData.hitGetTabListData();
        String chargesTabListDataResponse = getTabListData.getTabListDataResponse();


        boolean isListDataValidJson = APIUtils.validJsonResponse(chargesTabListDataResponse, "[Charges tab list data response]");


        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(chargesTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                return true;
            } else {
                logger.error("There is no data in Charges tab for Service Data Id : [{}]", serviceDataId);
                return false;
            }


        } else {
            logger.error("Charges tab List Data Response is not valid Json for Service Data  Id :[{}] ", serviceDataId);
            return false;

        }

    }

    // this function will check whether any data has been created under ARC/RRC tab of Service Data or Not
    public boolean isARCRRCCreated(int serviceDataId) {

        logger.info("Checking whether data under ARR/RRC tab has/have been created and visible for serviceData" + serviceDataId);
        int serviceDataEntityTypeId = 64;
        int ARCRRCTabId = 311;
        GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, ARCRRCTabId, serviceDataId);
        getTabListData.hitGetTabListData();
        String ARCRRCTabListDataResponse = getTabListData.getTabListDataResponse();


        boolean isListDataValidJson = APIUtils.validJsonResponse(ARCRRCTabListDataResponse, "[ARR/RRC tab list data response]");


        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(ARCRRCTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                return true;
            } else {
                logger.error("There is no data in ARR/RRC tab for Service Data Id : [{}]", serviceDataId);
                return false;
            }


        } else {
            logger.error("ARR/RRC tab List Data Response is not valid Json for Service Data  Id :[{}] ", serviceDataId);
            return false;

        }

    }

    public static int getInvoiceIdForMultipleLI(String invoiceConfigFilePath, String invoiceConfigFileName, String invoiceExtraFieldsConfigFileName,
                                                String invoiceLineItemConfigFilePath, String invoiceLineItemConfigFileName, String flowToTest,
                                                int numberOfRowsToEdit) {
        int invoiceId = -1;
        try {

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    flowToTest, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTest, invoiceId);
            int invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");
            int i = 0;
            String tempAppendSectionName;
            while (i++ < numberOfRowsToEdit) {
                tempAppendSectionName = flowToTest + i;
                UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, tempAppendSectionName, "sourcename",
                        invoiceName);
                UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, tempAppendSectionName, "sourceid",
                        String.valueOf(invoiceId));
            }

        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return invoiceId;
    }

    //added by srijan
    public static boolean verifyInvoiceValidationStatus(String flowsConfigFilePath, String flowsConfigFileName, String flowToTest, int invoiceId, CustomAssert customAssert) {

        int entityTypeId = 67;
        try {
            logger.info("invoiceId : {}", invoiceId);

            String label = "Show lineitems";
            HttpResponse response = InvoiceReValidationCheck.hitActionUrl(entityTypeId, invoiceId);
            String invoiceData = EntityUtils.toString(response.getEntity());
            JSONArray actionJsonObject = new JSONArray(invoiceData);
            String obj = "";
            boolean validationStatusFound = true;//To check whether the response contains any value with the below mentioned label
            String expectedResult = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicevalidationexpectedresult");
            if (expectedResult.equalsIgnoreCase("Revalidation Required"))
                label = "Revalidate";
            String actualResult = "";
            for (int jsonIndex = 0; jsonIndex < actionJsonObject.length(); jsonIndex++) {
                try {
                    obj = actionJsonObject.getJSONObject(jsonIndex).get("label").toString();
                } catch (Exception e) {
                    continue;
                }
                if (obj.equalsIgnoreCase(label)) {
                    if (expectedResult.equalsIgnoreCase("Completed")) {
                        customAssert.assertTrue(false, "Validation Status is completed and should not contain [Show lineitems], hence returning false");
                        logger.info("Validation Status is completed and should not contain [Show lineitems]");
                        return false;
                    }
                    try {
                        actualResult = actionJsonObject.getJSONObject(jsonIndex).getJSONObject("properties").get("validationStatusValue").toString();
                        if (actualResult.equalsIgnoreCase(expectedResult)) {
                            logger.info("Validation Status String matched, Test Successful");
                            return true;
                        } else {
                            customAssert.assertFalse(true, "Validation Status String not matched Expected [" + expectedResult + "] Found [" + actualResult + "]");
                            logger.info("Validation Status Line not found Expected [{}] Found [{}]", expectedResult, actualResult);
                            return false;
                        }
                    } catch (Exception e) {
                        logger.info("Exception caught while getting actual value of validation status value");
                        customAssert.assertFalse(true, "Exception caught while getting actual value of validation status value");
                    }

                }
            }

            if (expectedResult.equalsIgnoreCase("Completed"))
                return true;

        } catch (Exception e) {
            logger.error("Exception caught : " + e.toString());
            customAssert.assertFalse(true, "Exception caught : " + e.toString());
        }
        return false;
    }

    public boolean editAndUploadForecastSheet(String flowsConfigFilePath, String flowsConfigFileName, String forecastTemplateFilePath, String templateFileName, String flowToTest, String clientId, Integer contractId) {


        boolean result = true;
        String foreCastSheetName = "Forecast Data";
        boolean flag = editForecastSheet(flowsConfigFilePath, flowsConfigFileName, forecastTemplateFilePath, templateFileName, foreCastSheetName, flowToTest, clientId, contractId);

        if (flag) {

            //Kill All Scheduler Tasks if Flag is On.

            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
            killAllSchedulerTasks();


            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());


            String forecastUploadResponse = ForecastUploadHelper.uploadSheet(forecastTemplateFilePath, templateFileName, contractId);

            if (forecastUploadResponse != null && forecastUploadResponse.trim().contains("")) {

                //Wait for Forecast Scheduler to Complete
                String forecastSchedulerStatus = waitForForecastScheduler(flowToTest, allTaskIds);

                if (forecastSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                    logger.error("Forecast Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                    return false;
                } else if (forecastSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                    logger.info("Forecast Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                    if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                        logger.error("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                        return false;
                    } else {
                        logger.warn("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow and Skipping it[{}]", flowToTest);
                        throw new SkipException("Skipping this test");
                    }


                }

                boolean isForecastCreated = isForecastCreated(contractId);
                if (!isForecastCreated) {
                    logger.error("Forecast is not getting listed under forecast tab for Contract Id : [{}]", contractId);
                    return false;
                }


            } else {
                logger.error("Error While Uploading the Forecast sheet  : [{}]", flowToTest);
                return false;
            }
        } else {

            logger.error("Error in Editing the Forecast Sheet for this flow : [{}]", flowToTest);
            return false;
        }


        return result;
    }

    // this function will check whether any data has been created under forecast tab of Contract or Not
    boolean isForecastCreated(int contractId) {

        logger.info("Checking whether forecast has/have been created and visible under contract Forecast Tab");

        int forecastTabId = 313;
        int contractTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
        GetTabListData getTabListData = new GetTabListData(contractTypeId, forecastTabId, contractId);
        getTabListData.hitGetTabListData();
        String forecastTabListDataResponse = getTabListData.getTabListDataResponse();


        boolean isListDataValidJson = APIUtils.validJsonResponse(forecastTabListDataResponse, "[forecast tab list data response]");


        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(forecastTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                return true;
            } else {
                logger.error("There is no data in Forecast tab for Contract Id : [{}]", contractId);
                return false;
            }


        } else {
            logger.error("Forecast tab List Data Response is not valid Json for Contract Id :[{}] ", contractId);
            return false;

        }
    }

    public boolean editForecastSheet(String flowsConfigFilePath, String flowsConfigFileName, String forecastTemplateFilePath, String templateFileName, String forecastSheetNameInXLSXFile, String flowToTest, String clientId, Integer contractId) {

        Boolean pricingFile = false;
        Map<Integer, Map<Integer, Object>> forecastValuesMap = getValuesMapForForecastSheet(flowsConfigFilePath, flowsConfigFileName, flowToTest, clientId);


        try {
            InvoicePricingHelper pricingObj = new InvoicePricingHelper();
            boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(forecastTemplateFilePath, templateFileName, forecastSheetNameInXLSXFile,
                    forecastValuesMap);

            if (editTemplate == true) {
                return true;
            } else {
                logger.error("Error While Updating the Forecast Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, contractId);
                return false;
            }


        } catch (Exception e) {
            logger.error("Exception while getting Forecast Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;

    }

    // this will create the row , <columnNumber,value> for editing the Forecast Sheet
    public Map<Integer, Map<Integer, Object>> getValuesMapForForecastSheet(String flowsConfigFilePath, String flowsConfigFileName, String flowToTest, String clientId) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {


            String rowNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "forecastsheetrownumber");

            String[] columnNumbers = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "forecastsheetcolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "forecastsheetvalue").trim().split(Pattern.quote(","));


            Map<Integer, Object> innerValuesMap = new HashMap<>();
            for (int j = 0; j < columnNumbers.length; j++) {

                if (values[j].toLowerCase().contentEquals("clientid")) {
                    innerValuesMap.put(Integer.parseInt(columnNumbers[j]), clientId);
                    continue;

                }


                innerValuesMap.put(Integer.parseInt(columnNumbers[j]), values[j]);
            }
            valuesMap.put(Integer.parseInt(rowNumber), innerValuesMap);

        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Forecast Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    private void killAllSchedulerTasks() {
        UserTasksHelper.removeAllTasks();
    }

    /*
   This method will return the status of Forecast Scheduler as String.
   Possible Values are 'Pass', 'Fail', 'Skip'.
   'Pass' specifies that Forecast scheduler completed and records processed successfully.
   'Fail' specifies that Forecast scheduler failed
   'Skip' specifies that Forecast scheduler didn't finish within time.
 */
    private String waitForForecastScheduler(String flowToTest, List<Integer> oldIds) {
        String result = "pass";
        logger.info("Waiting for Forecast Scheduler to Complete for Flow [{}].", flowToTest);
        try {
            logger.info("Time Out for Forecast Scheduler is {} milliseconds", forecastSchedulerTimeOut);
            long timeSpent = 0;
            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Forecast Upload Job");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), oldIds);

            if (newTaskId != -1) {
                Boolean taskCompleted = false;
                logger.info("Checking if Forecast Upload Task has completed or not.");

                while (timeSpent < forecastSchedulerTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    fetchObj.hitFetch();
                    logger.info("Getting Status of Forcast Upload Task.");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        taskCompleted = true;
                        logger.info("Forecast Upload Task Completed. ");
                        logger.info("Checking if Forecast Upload Task failed or not.");
                        if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                            result = "fail";

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Forecast Upload Task is not finished yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= forecastSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }
            } else {
                logger.info("Couldn't get Forecast Upload Task Job Id. Hence waiting for Task Time Out i.e. {}", forecastSchedulerTimeOut);
                Thread.sleep(forecastSchedulerTimeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Forecast Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }
        return result;
    }

    // this method will check whether consumption has been created or not
    // return "pass" if created
    // return "fail" if something went wrong in code or assumption
    // return "skip" if consumption would not created in specified time consumptionToBeCreatedTimeOut
    public String waitForConsumptionToBeCreated(String flowToTest, int serviceDataId, ArrayList<Integer> consumptionIds) {
        int offset = 0; // default value for creating payload
        int size = 2000; // default value for creating payload

        Show show = new Show();
        String result = "pass";
        logger.info("Waiting for Consumption to be Created for Flow [{}].", flowToTest);
        try {
            show.hitShow(serviceDataEntityTypeId, serviceDataId);
            String showPageResponseStr = show.getShowJsonStr();
            List<String> dataUrl = show.getShowPageTabUrl(showPageResponseStr, Show.TabURL.dataURL);

            String consumptionDataURL = null;
            for (String Url : dataUrl) {
                if (Url.contains("listRenderer/list/376/tablistdata")) // for consumption
                {
                    consumptionDataURL = Url;
                    break;
                } else
                    continue;

            }


            if (consumptionDataURL != null) {

                logger.info("Time Out for Consumption to be created is {} milliseconds", consumptionToBeCreatedTimeOut);
                long timeSpent = 0;


                Boolean taskCompleted = false;
                logger.info("Checking if Consumption has been created or not.");

                String payload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":" +
                        offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc\",\"filterJson\":{}}}";

                while (timeSpent < consumptionToBeCreatedTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);
                    logger.info("Hitting tab data API for Consumption for service data Id : [{}]", serviceDataId);

                    String showPageDataUrlResponseStr = Show.hitshowPageTabUrl(consumptionDataURL, payload);
                    JSONObject showPageDataUrlResponseJson = new JSONObject(showPageDataUrlResponseStr);

                    int numberOfConsumption = showPageDataUrlResponseJson.getJSONArray("data").length();


                    if (numberOfConsumption > 0) {
                        taskCompleted = true;
                        logger.info("Consumptions have been created. ");

                        for (int i = 0; i < numberOfConsumption; i++) {

                            JSONObject data = showPageDataUrlResponseJson.getJSONArray("data").getJSONObject(i);
                            Set<String> keys = data.keySet();
                            for (String key : keys) {
                                String columnName = data.getJSONObject(key).getString("columnName");
                                if (columnName.contentEquals("id")) {
//									consumptionIds[i] = Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]);
                                    consumptionIds.add(Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]));
                                    break;
                                } else
                                    continue;

                            }
                        }

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("time spent is : [{}]", timeSpent);
                        logger.info("Consumptions haven't been created yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }


            } else {
                logger.error("There is no consumption URL in the show page response of Service data : [{}] for flow : [{}]", serviceDataId, flowToTest);
                result = "skip";
                return result;
            }


        } catch (Exception e) {
            logger.error("Exception while Waiting for Consumption to get created to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }


        return result;
    }


    public static boolean getInvoiceId(String invoiceConfigFilePath, String invoiceConfigFileName, String invoiceExtraFieldsConfigFileName,
                                       String invoiceLineItemConfigFilePath, String invoiceLineItemConfigFileName, String flowToTest, List<Integer> billingItems) {
        int invoiceId = -1;
        try {

            JSONArray billingIdsArray = new JSONArray(billingItems);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("values", billingIdsArray);
            jsonObject.put("name", "billingDataIds");

            UpdateFile.addPropertyToConfigFile(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, flowToTest, "billingDataIds", jsonObject.toString());

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    flowToTest, true);

            return createResponse.contains("Request submitted successfully");

        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return false;
    }
    public List<Integer> getBillingDataIds(String configFilePath, String configFileName, int contractId, int serviceDataId) {
        int selectBillingDataListId = 445;
        String columnIdForServiceDataInSelectLineItemList = "18694";
        String columnIdForIdInSelectLineItemList = "18715";

        List<Integer> billingIds = new ArrayList<>();

        try {
            String waitTimeSectionNameForBillingData = "waittimeforbillingdatageneration";
            int waitTimeForBillingData = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, waitTimeSectionNameForBillingData));


            ListRendererTabListData selectBillingIds = new ListRendererTabListData();
            JSONObject jsonObject = new JSONObject();

            int timeElapsed = 0;
            int length = 0;

            logger.info("Wait time logged for billing data generation is {}", waitTimeForBillingData);

            while (billingIds.size() == 0 && timeElapsed < waitTimeForBillingData) {

                try {
                    selectBillingIds.hitListRendererTabListData(selectBillingDataListId, ConfigureConstantFields.getEntityIdByName(contractEntity), contractId, selectBillingIds.getPayload(serviceDataEntityTypeId, 0, 20, "id", "asc", "{}"));
                    String response = selectBillingIds.getTabListDataJsonStr();
                    jsonObject = new JSONObject(response);
                    length = jsonObject.getJSONArray("data").length();

                    int index = -1;
                    while (++index < length) {

                        JSONArray arrayTemp = jsonObject.getJSONArray("data").getJSONObject(index).names();
                        for (int subIndex = 0; subIndex < arrayTemp.length(); subIndex++) {
                            String tempValue;
                            try {
                                tempValue = jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subIndex)).getString("value");
                                if (tempValue.contains(String.valueOf(serviceDataId)))
                                    for (int subSubIndex = 0; subSubIndex < arrayTemp.length(); subSubIndex++) {
                                        if (jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subSubIndex)).getString("columnName").contains("id")) {
                                            billingIds.add(Integer.parseInt(jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subSubIndex)).getString("value").split(":;")[0]));
                                            break;
                                        }
                                    }
                            } catch (Exception e) {
                                logger.error("Exception caught in parsing json {}", (Object) e.getStackTrace());
                            }

                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception caught {}", (Object) e.getStackTrace());
                }

                logger.info("Time elapsed while waiting for billing data {}", timeElapsed);
                logger.info("Billing data {}", billingIds.toString());
                timeElapsed += 5000;
                Thread.sleep(5000);
            }

            logger.info("length of billing data is {}", length);


            if (length == 0 && timeElapsed >= waitTimeForBillingData) {
                logger.info("wait time for billing data is over. Billing data not generated hence terminating the test case.");
                return null;
            }

            logger.info("Billing data {}", billingIds.toString());
        } catch (Exception e) {
            logger.info("Exception caught in getBillingDataIds()");
            return null;
        }

        return billingIds;
    }

    public List<Integer> getBillingDataIds(int contractId, int serviceDataId) {
        int selectBillingDataListId = 445;

        List<Integer> billingIds = new ArrayList<>();

        try {

            int waitTimeForBillingData = 600000;


            ListRendererTabListData selectBillingIds = new ListRendererTabListData();
            JSONObject jsonObject = new JSONObject();

            int timeElapsed = 0;
            int length = 0;

            logger.info("Wait time logged for billing data generation is {}", waitTimeForBillingData);

            while (billingIds.size() == 0 && timeElapsed < waitTimeForBillingData) {

                try {
                    selectBillingIds.hitListRendererTabListData(selectBillingDataListId, ConfigureConstantFields.getEntityIdByName(contractEntity), contractId, selectBillingIds.getPayload(serviceDataEntityTypeId, 0, 20, "id", "asc", "{}"));
                    String response = selectBillingIds.getTabListDataJsonStr();
                    jsonObject = new JSONObject(response);
                    length = jsonObject.getJSONArray("data").length();

                    int index = -1;
                    while (++index < length) {

                        JSONArray arrayTemp = jsonObject.getJSONArray("data").getJSONObject(index).names();
                        for (int subIndex = 0; subIndex < arrayTemp.length(); subIndex++) {
                            String tempValue;
                            try {
                                tempValue = jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subIndex)).getString("value");
                                if (tempValue.contains(String.valueOf(serviceDataId)))
                                    for (int subSubIndex = 0; subSubIndex < arrayTemp.length(); subSubIndex++) {
                                        if (jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subSubIndex)).getString("columnName").contains("id")) {
                                            billingIds.add(Integer.parseInt(jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subSubIndex)).getString("value").split(":;")[0]));
                                            break;
                                        }
                                    }
                            } catch (Exception e) {
                                logger.error("Exception caught in parsing json {}", (Object) e.getStackTrace());
                            }

                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception caught {}", (Object) e.getStackTrace());
                }

                logger.info("Time elapsed while waiting for billing data {}", timeElapsed);
                logger.info("Billing data {}", billingIds.toString());
                timeElapsed += 5000;
                Thread.sleep(5000);
            }

            logger.info("length of billing data is {}", length);


            if (length == 0 && timeElapsed >= waitTimeForBillingData) {
                logger.info("wait time for billing data is over. Billing data not generated hence terminating the test case.");
                return null;
            }

            logger.info("Billing data {}", billingIds.toString());
        } catch (Exception e) {
            logger.info("Exception caught in getBillingDataIds()");
            return null;
        }

        return billingIds;
    }

    public boolean verifyChargesCreated(int serviceDataId, double volume, double rate) {
        return verifyChargesCreated(serviceDataId, volume, rate, null, null,true, true,false,false);
    }

    public boolean verifyChargesCreated(int serviceDataId, double volume, double rate, String startDate, String endDate) { //date in format MMM-dd-yyyy
        return verifyChargesCreated(serviceDataId, volume, rate, startDate, endDate,true, true,true,true);
    }

    public boolean verifyChargesCreated(int serviceDataId, double volume, double rate, String startDate, String endDate, boolean checkVolume, boolean checkRate, boolean checkStartDate, boolean checkEndDate) {
//date in format MMM-dd-yyyy
        logger.info("Checking whether data under Charges tab has/have been created and visible for serviceData" + serviceDataId);
        int serviceDataEntityTypeId = 64;
        int chargesTabId = 309;

        GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, chargesTabId, serviceDataId);
        getTabListData.hitGetTabListData();
        String chargesTabListDataResponse = getTabListData.getTabListDataResponse();

        boolean isListDataValidJson = APIUtils.validJsonResponse(chargesTabListDataResponse, "[Charges tab list data response]");

        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(chargesTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                String rateString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12662").getString("value");
                String volString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12514").getString("value");
                String startDateString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12512").getString("value");
                String endDateString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12513").getString("value");

                boolean rateAssertion=true;
                if(checkRate)
                    rateAssertion=Double.valueOf(rateString).equals(rate);
                boolean VolumeAssertion=true;
                if(checkVolume)
                    VolumeAssertion=Double.valueOf(volString).equals(volume);
                boolean startAssertion=true;
                if(checkStartDate)
                    startAssertion=startDateString.equals(startDate);
                boolean endAssertion=true;
                if(checkEndDate)
                    endAssertion=endDateString.equals(endDate);

                return rateAssertion && VolumeAssertion && startAssertion && endAssertion;

            } else {
                logger.error("There is no data in Charges tab for Service Data Id : [{}]", serviceDataId);
                return false;
            }

        } else {
            logger.error("Charges tab List Data Response is not valid Json for Service Data  Id :[{}] ", serviceDataId);
            return false;
        }
    }

    public boolean verifyARCRRCCreated(int serviceDataId, double lower, double upper, double rate){
        return verifyARCRRCCreated(serviceDataId,lower,upper,rate,null,null,true,true,true,false,false);
    }
    public boolean verifyARCRRCCreated(int serviceDataId, double lower, double upper, double rate, String startDate, String endDate){//date in format MMM-dd-yyyy
        return verifyARCRRCCreated(serviceDataId,lower,upper,rate,startDate,endDate,true,true,true,true,true);
    }

    public boolean verifyARCRRCCreated(int serviceDataId, double lower, double upper, double rate, String startDate, String endDate, boolean checkLower
            , boolean checkUpper, boolean checkRate, boolean checkStartDate, boolean checkEndDate) {
//date in format MMM-dd-yyyy
        logger.info("Checking whether data under ARR/RRC tab has/have been created and visible for serviceData" + serviceDataId);
        int serviceDataEntityTypeId = 64;
        int ARCRRCTabId = 311;
        GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, ARCRRCTabId, serviceDataId);
        getTabListData.hitGetTabListData();
        String ARCRRCTabListDataResponse = getTabListData.getTabListDataResponse();

        boolean isListDataValidJson = APIUtils.validJsonResponse(ARCRRCTabListDataResponse, "[ARR/RRC tab list data response]");

        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(ARCRRCTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                String rateString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12522").getString("value");
                String lowerString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12520").getString("value");
                String upperString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12521").getString("value");
                String startDateString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12518").getString("value");
                String endDateString = jsonResponse.getJSONArray("data").getJSONObject(0).getJSONObject("12519").getString("value");

                boolean rateAssertion=true;
                if(checkRate)
                    rateAssertion=Double.valueOf(rateString).equals(rate);
                boolean lowerAssertion=true;
                if(checkLower)
                    lowerAssertion=Double.valueOf(lowerString).equals(lower);
                boolean upperAssertion=true;
                if(checkUpper)
                    upperAssertion=Double.valueOf(upperString).equals(upper);
                boolean startAssertion=true;
                if(checkStartDate)
                    startAssertion=startDateString.equals(startDate);
                boolean endAssertion=true;
                if(checkEndDate)
                    endAssertion=endDateString.equals(endDate);

                return rateAssertion && lowerAssertion && upperAssertion && startAssertion && endAssertion;
            } else {
                logger.error("There is no data in ARR/RRC tab for Service Data Id : [{}]", serviceDataId);
                return false;
            }


        } else {
            logger.error("ARR/RRC tab List Data Response is not valid Json for Service Data  Id :[{}] ", serviceDataId);
            return false;

        }

    }

    public List<String> getPricingVersionCharges(int serviceDataId) {
        return getPricingVersionCharges(String.valueOf(serviceDataId));
    }

    public List<String> getPricingVersionCharges(String serviceDataId) {
        List<String> returnList = new ArrayList<>();
        try {

            ListRendererFilterData listRendererFilterData = new ListRendererFilterData();
            listRendererFilterData.hitListRendererFilterData(309, "{\"entityId\":" + serviceDataId + "}");
            String response = listRendererFilterData.getListRendererFilterDataJsonStr();

            JSONObject jsonObject = new JSONObject(response);
            try {
                JSONArray jsonArray = jsonObject.getJSONObject("214").getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
                jsonArray.forEach(obj -> {
                    JSONObject object = (JSONObject) obj;
                    returnList.add(object.getString("id") + ":;" + object.getString("name"));
                });
            } catch (Exception e) {
                logger.info("Exception occurred in parsing json");
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception caught in getPricingVersion {}", (Object) e.getStackTrace());
            return null;
        }
        return returnList;
    }
    public List<String> getPricingVersionARCRRC(int serviceDataId) {
        return getPricingVersionARCRRC(String.valueOf(serviceDataId));
    }

    public List<String> getPricingVersionARCRRC(String serviceDataId) {
        List<String> returnList = new ArrayList<>();
        try {

            ListRendererFilterData listRendererFilterData = new ListRendererFilterData();
            listRendererFilterData.hitListRendererFilterData(311, "{\"entityId\":" + serviceDataId + "}");
            String response = listRendererFilterData.getListRendererFilterDataJsonStr();

            JSONObject jsonObject = new JSONObject(response);
            try {
                JSONArray jsonArray = jsonObject.getJSONObject("214").getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
                jsonArray.forEach(obj -> {
                    JSONObject object = (JSONObject) obj;
                    returnList.add(object.getString("id") + ":;" + object.getString("name"));
                });
            } catch (Exception e) {
                logger.info("Exception occurred in parsing json");
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception caught in getPricingVersion {}", (Object) e.getStackTrace());
            return null;
        }
        return returnList;
    }


    public static int getServiceDataIdForMultiSupplier(String serviceDataConfigFilePath, String serviceDataConfigFileName, String serviceDataExtraFieldsConfigFileName, String serviceDataSectionName, int contractId, int supplierId) {
        int serviceDataId = -1;
        try {
            Show supplierShow = new Show();
            supplierShow.hitShow(1,supplierId);

            String supplierName = ShowHelper.getSupplierNameFromShowResponse(supplierShow.getShowJsonStr(),1);

            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "supplier",
                    "sup_name", supplierName);
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "supplier",
                    "sup_id", String.valueOf(supplierId));

            serviceDataId = getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath,serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDataSectionName,contractId);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "supplier",
                    supplierName, "sup_name");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "supplier",
                    String.valueOf(supplierId), "sup_id");

        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", serviceDataSectionName, e.getStackTrace());
        }
        return serviceDataId;
    }

    public static int getServiceDataId(String serviceDataConfigFilePath, String serviceDataConfigFileName, String serviceDataExtraFieldsConfigFileName, String serviceDataSectionName, int contractId,String uniqueString) {

        int serviceDataId = -1;
        try {

            String serviceDataEntity = "service data";

            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "new client", "newClient" + uniqueString);
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "new supplier", "newSupplier" + uniqueString);

            Boolean createLocalServiceData = true;

            String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                    serviceDataExtraFieldsConfigFileName, serviceDataSectionName, createLocalServiceData);

            serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                    "newClient" + uniqueString, "new client");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                    "newSupplier" + uniqueString, "new supplier");

        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", serviceDataSectionName, e.getStackTrace());
        }
        return serviceDataId;
    }

    public static int getInvoiceIdNew(String invoiceConfigFilePath, String invoiceConfigFileName, String invoiceExtraFieldsConfigFileName, String flowToTest) {
        int invoiceId = -1;
        try {

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    flowToTest, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTest, invoiceId);

        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return invoiceId;
    }

    public static synchronized int getInvoiceLineItemIdNew(String invoiceLineItemConfigFilePath, String invoiceLineItemConfigFileName, String invoiceLineItemExtraFieldsConfigFileName, String invoiceLineItemSectionName, int serviceDataId,int invoiceId) {

        int lineItemId = -1;
        try {
            String serviceDataEntity = "service data";
            String invoiceLineItemEntity = "invoice line item";

            logger.info("Updating Invoice Line Item Property Service Id Supplier in Extra Fields Config File for Flow [{}] and Service Data Id {}.",
                    invoiceLineItemSectionName, serviceDataId);
            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName(serviceDataEntity);
            String serviceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "title");
            String serviceIdSupplierName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "serviceIdSupplier");
            String serviceIdSupplierUpdatedName = serviceDataName + " (" + serviceIdSupplierName + ")";

            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new name", serviceIdSupplierUpdatedName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new id", String.valueOf(serviceDataId));

            int invoiceEntityTypeId = 67;
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");

            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemSectionName, "sourcename",
                    invoiceName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemSectionName, "sourceid",
                    String.valueOf(invoiceId));

            String createResponse = InvoiceLineItem.createInvoiceLineItem(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemConfigFilePath,
                    invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, true);
            lineItemId = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

            //Reverting Invoice Line Item Extra Fields changes.
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", serviceIdSupplierUpdatedName, "new name");
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", String.valueOf(serviceDataId), "new id");

        } catch (Exception e) {
            logger.error("Exception while getting Invoice Line Item Id using Flow Section [{}]. {}", invoiceLineItemSectionName, e.getStackTrace());
        }
        return lineItemId;
    }

    public boolean updateConsumption(int consumptionId, Double finalConsumption, CustomAssert customAssert){

        Boolean consumptionUpdateStatus = true;
        Edit edit = new Edit();
        Show show = new Show();

        String editResponse = null;
        String showResponse = null;
        String consumptionsEntity = "consumptions";
        try{

            editResponse = edit.hitEdit(consumptionsEntity,consumptionId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").put("values",finalConsumption);
            editResponse = edit.hitEdit(consumptionsEntity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Consumtions updated unsuccessfully");
            }else {

                show.hitShowVersion2(consumptionsEntityTypeId, consumptionId);
                showResponse = show.getShowJsonStr();
                JSONObject showResponseJson = new JSONObject(showResponse);
                String finalConsumptionsValueShowPage = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").get("values").toString();

                if(!finalConsumptionsValueShowPage.equalsIgnoreCase(String.valueOf(finalConsumption))){
                    logger.error("Final Consumption value from show page and expected didn't matched");
                    customAssert.assertTrue(false,"Final Consumption value from show page and expected didn't matched");
                    consumptionUpdateStatus = false;
                }

            }

        }catch (Exception e){
            logger.error("Exception while updating consumption " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating consumption " + e.getStackTrace());
            consumptionUpdateStatus = false;
        }

        return consumptionUpdateStatus;
    }

    public int getLatestConsumptionCreated(int serviceDataId,int totalNumberOfConsumptionExpected,CustomAssert customAssert){

        TabListData tabListData = new TabListData();

        Long pollingTime =  5000L;
        Long waitTimeOut = 300000L;

        int tabId = 376;
        int latestConsumption = -1;

        String payload = "{\"filterMap\":{\"entityTypeId\":176,\"offset\":0,\"size\":20,\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}";
        try{

            tabListData.hitTabListData(tabId,serviceDataEntityTypeId,serviceDataId,payload);
            String tabListDataResponse = tabListData.getTabListDataResponseStr();

            JSONObject tabListDataResponseJson = new JSONObject(tabListDataResponse);

            JSONArray dataArray = tabListDataResponseJson.getJSONArray("data");

            JSONObject indRow;
            JSONArray indRowJSonArray;
            String columnName;
            String columnValue;

            while (pollingTime < waitTimeOut) {

                tabListData.hitTabListData(tabId,serviceDataEntityTypeId,serviceDataId,payload);
                dataArray = tabListDataResponseJson.getJSONArray("data");

                if (dataArray.length() == totalNumberOfConsumptionExpected) {

                    indRow = dataArray.getJSONObject(0);

                    indRowJSonArray = JSONUtility.convertJsonOnjectToJsonArray(indRow);

                    for (int i = 0; i < indRowJSonArray.length(); i++) {

                        columnName = indRowJSonArray.getJSONObject(i).get("columnName").toString();

                        if (columnName.equals("id")) {
                            columnValue = indRowJSonArray.getJSONObject(i).get("value").toString().split(":;")[1];
                            latestConsumption = Integer.parseInt(columnValue);
                            break;
                        }
                    }
                }
                pollingTime += 5000;
                Thread.sleep(5000);
            }

            if(latestConsumption == -1){
                customAssert.assertTrue(false,"Consumption not created in specific time of " + waitTimeOut);
            }
        }catch (Exception e){
            logger.error("Exception while fetching consumptions");
        }

        return latestConsumption;
    }

    public boolean revalidateScenario(int invoiceId,CustomAssert customAssert){

        String invoiceWorkFlowResponse = Actions.getActionsV3Response(invoiceEntityTypeId,invoiceId);

        Boolean revalidateScenario = true;
        Boolean reValidationButtonFound = true;

        String expectedButtonName = "Revalidate";
        String revalidateUrl = null;

        if(JSONUtility.validjson(invoiceWorkFlowResponse)){

            JSONObject invoiceWorkFlowResponseJson = new JSONObject(invoiceWorkFlowResponse);
            JSONArray layoutActionsArray = invoiceWorkFlowResponseJson.getJSONArray("layoutActions");

            for(int i = 0;i<layoutActionsArray.length();i++){

                if(layoutActionsArray.getJSONObject(i).get("name").toString().equals(expectedButtonName)){
                    revalidateUrl = layoutActionsArray.getJSONObject(i).get("api").toString();
                    logger.info(expectedButtonName +  " Button found on invoice show page");
                    reValidationButtonFound = true;
                    break;
                }
            }

            if(!reValidationButtonFound){
                customAssert.assertTrue(false,expectedButtonName +  " Button not found on invoice show page");
                revalidateScenario = false;
            }else {
                String payload = "{\"entityId\":" + invoiceId + "}";
                String response = Actions.hitActionApi(revalidateUrl,payload);

                if(!response.contains("success")){
                    customAssert.assertTrue(false,"Error while revalidating the invoice");
                    revalidateScenario = false;
                }
            }

        }else {
            logger.error("invoice WorkFlowResponse is an invalid json");
            customAssert.assertTrue(false,"invoice WorkFlowResponse is an invalid json");
            revalidateScenario = false;
        }

        return revalidateScenario;
    }

    public String waitForConsumptionToBeCreated(String flowToTest, int serviceDataId, int expectedNumberOfConsumptions,ArrayList<Integer> consumptionIds) {
        int offset = 0; // default value for creating payload
        int size = 30; // default value for creating payload

        Show show = new Show();
        String result = "pass";
        logger.info("Waiting for Consumption to be Created for Flow [{}].", flowToTest);
        try {
            show.hitShow(serviceDataEntityTypeId, serviceDataId);
            String showPageResponseStr = show.getShowJsonStr();
            List<String> dataUrl = show.getShowPageTabUrl(showPageResponseStr, Show.TabURL.dataURL);

            String consumptionDataURL = null;
            for (String Url : dataUrl) {
                if (Url.contains("listRenderer/list/376/tablistdata")) // for consumption
                {
                    consumptionDataURL = Url;
                    break;
                } else
                    continue;

            }


            if (consumptionDataURL != null) {

                logger.info("Time Out for Consumption to be created is {} milliseconds", consumptionToBeCreatedTimeOut);
                long timeSpent = 0;


                Boolean taskCompleted = false;
                logger.info("Checking if Consumption has been created or not.");

                String payload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":" +
                        offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc\",\"filterJson\":{}}}";

                while (timeSpent < consumptionToBeCreatedTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);
                    logger.info("Hitting tab data API for Consumption for service data Id : [{}]", serviceDataId);

                    String showPageDataUrlResponseStr = Show.hitshowPageTabUrl(consumptionDataURL, payload);
                    JSONObject showPageDataUrlResponseJson = new JSONObject(showPageDataUrlResponseStr);

                    int numberOfConsumption = showPageDataUrlResponseJson.getJSONArray("data").length();


                    if (numberOfConsumption == expectedNumberOfConsumptions) {
                        taskCompleted = true;
                        logger.info("Consumptions have been created. ");

                        for (int i = 0; i < numberOfConsumption; i++) {

                            JSONObject data = showPageDataUrlResponseJson.getJSONArray("data").getJSONObject(i);
                            Set<String> keys = data.keySet();
                            for (String key : keys) {
                                String columnName = data.getJSONObject(key).getString("columnName");
                                if (columnName.contentEquals("id")) {
//									consumptionIds[i] = Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]);
                                    consumptionIds.add(Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]));
                                    break;
                                } else
                                    continue;

                            }
                        }

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("time spent is : [{}]", timeSpent);
                        logger.info("Consumptions haven't been created yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }


            } else {
                logger.error("There is no consumption URL in the show page response of Service data : [{}] for flow : [{}]", serviceDataId, flowToTest);
                result = "skip";
                return result;
            }


        } catch (Exception e) {
            logger.error("Exception while Waiting for Consumption to get created to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }


        return result;
    }

    public boolean updateServiceStartAndEndDate(int serviceDataId, String startDate,String endDate, CustomAssert customAssert){

        Boolean updateServiceStartAndEndDate = true;

        Edit edit = new Edit();
        String editResponse;
        String serviceData = "service data";

        try{

            editResponse = edit.hitEdit(serviceData,serviceDataId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("endDate").put("values",endDate);

            editResponse = edit.hitEdit(serviceData,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating service start date and end date");
                updateServiceStartAndEndDate = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating service start date end date " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating service start date end date " + e.getStackTrace());
            updateServiceStartAndEndDate = false;
        }

        return updateServiceStartAndEndDate;
    }

    public boolean updateServiceStartAndEndDate(String entityName,int entityId, String startDate,String endDate, CustomAssert customAssert){

        Boolean updateServiceStartAndEndDate = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            editResponse = edit.hitEdit(entityName,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceStartDate").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceEndDate").put("values",endDate);

            editResponse = edit.hitEdit(entityName,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating service start date and end date");
                updateServiceStartAndEndDate = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating service start date end date " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating service start date end date " + e.getStackTrace());
            updateServiceStartAndEndDate = false;
        }

        return updateServiceStartAndEndDate;
    }


    public boolean updateConversionData(int entityId, int convDataId, CustomAssert customAssert){

        Boolean updateConversionData = true;

        Edit edit = new Edit();
        String editResponse;

        try{
            String entityName = "service data";
            editResponse = edit.hitEdit(entityName,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("conversionData").put("values",new JSONObject());
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("conversionData").getJSONObject("values").put("id",convDataId);

            editResponse = edit.hitEdit(entityName,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating conversion Data");
                updateConversionData = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating service start date end date " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating conversion Data " + e.getStackTrace());
            updateConversionData = false;
        }

        return updateConversionData;
    }

    public int getNumberOfBillingRecords(int serviceDataId,CustomAssert customAssert){

        int billingReportId = 444;
        int numberOfBillingRecords = 0;
        try{
            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"248\":{\"multiselectValues\":{\"SELECTEDDATA\":" +
                    "[{\"id\":\"" + serviceDataId + "\",\"name\":\"16 May 2020 (newSupplier2020_05_16 18_11_17_688)\"}]}," +
                    "\"filterId\":248,\"filterName\":\"serviceData\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                    "\"selectedColumns\":[{\"columnId\":18652,\"columnQueryName\":\"servicedataid\"},{\"columnId\":18641,\"columnQueryName\":\"startdate\"}]}";

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(billingReportId,payload);
            String listDataResponse = reportRendererListData.getListDataJsonStr();

            if(APIUtils.validJsonResponse(listDataResponse)){

                JSONObject listDataResponseJson = new JSONObject(listDataResponse);
                JSONArray dataArray = listDataResponseJson.getJSONArray("data");

                numberOfBillingRecords =  dataArray.length();

//                for(int i =0;i<dataArray.length();i++){
//
//                    names = dataArray.getJSONObject(i).names();
//
//                    for(int j=0;j<names.length();j++){
//
//                        names.getJSONObject(j).get("columnName");
//                    }
//
//                }

            }else{
                logger.error("Billing Report has invalid json Response Either Billing data not generated for service data id " + serviceDataId + " or there is bug");
                customAssert.assertTrue(false,"Billing Report has invalid json Response Either Billing data not generated for service data id " + serviceDataId + " or there is bug");
            }

        }catch (Exception e){
            logger.error("Exception while getting billing Data Ids");

        }

        return numberOfBillingRecords;
    }

    public HashMap<String,Map<String,String>> getBillingRecordAccToStartDate(int serviceDataId,CustomAssert customAssert){

        HashMap<String,Map<String,String>> billingRecordsAccToStartDate = new HashMap<>();
        int billingReportId = 444;

        try{
            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"248\":{\"multiselectValues\":{\"SELECTEDDATA\":" +
                    "[{\"id\":\"" + serviceDataId + "\",\"name\":\"\"}]}," +
                    "\"filterId\":248,\"filterName\":\"serviceData\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                    "\"selectedColumns\":[" +
                    "]}";

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(billingReportId,payload);
            String listDataResponse = reportRendererListData.getListDataJsonStr();

            if(APIUtils.validJsonResponse(listDataResponse)){

                JSONObject listDataResponseJson = new JSONObject(listDataResponse);
                int length = listDataResponseJson.getJSONArray("data").length();

                int index = -1;

                String columnName = "null";
                String columnValue;
                String startDate = "";
                HashMap<String,String> columnNameValues;
                int i =1;
                while (++index < length) {
                    columnNameValues = new HashMap<>();
                    JSONArray arrayTemp = listDataResponseJson.getJSONArray("data").getJSONObject(index).names();
                    for (int subIndex = 0; subIndex < arrayTemp.length(); subIndex++) {

                        try {
                            columnName = listDataResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subIndex)).getString("columnName");
                            columnValue = listDataResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subIndex)).getString("value");

                            if(columnName.equals("startdate")){
                                startDate = columnValue;
                            }else {
                                columnNameValues.put(columnName,columnValue);
                            }

                        } catch (Exception e) {
                            logger.error("Exception caught in parsing json {}", (Object) e.getStackTrace());
                            columnNameValues.put(columnName,"null");
                        }

                    }
                    if(billingRecordsAccToStartDate.containsKey(startDate)) {
                        billingRecordsAccToStartDate.put(startDate + "_" + i, columnNameValues);
                        i++;
                    }else {
                        billingRecordsAccToStartDate.put(startDate, columnNameValues);
                    }
                }

            }else{
                logger.error("Billing Report has invalid json Response Either Billing data not generated for service data id " + serviceDataId + " or there is bug");
                customAssert.assertTrue(false,"Billing Report has invalid json Response Either Billing data not generated for service data id " + serviceDataId + " or there is bug");
            }

        }catch (Exception e){
            logger.error("Exception while getting billing Data Ids");
            customAssert.assertTrue(false,"Exception while getting billing Data Ids");
        }

        return billingRecordsAccToStartDate;
    }

    public void updConvMatClientAdmin(String validTo,String validFrom,
                                      String convDataId,String convMatrixId,
                                      String filePath,String fileName,
                                      CustomAssert customAssert){

        Check check = new Check();
        try{

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

            check.hitCheck(adminUserName,adminPassword);

            ConversionMatrixUpdate conversionMatrixUpdate = new ConversionMatrixUpdate();

            Map<String,String> payloadMap = new HashMap<>();
            payloadMap.put("validTo",validTo);
            payloadMap.put("validFrom",validFrom);
            payloadMap.put("conversionDataId",convDataId);
            payloadMap.put("conversionMatrixId",convMatrixId);

            conversionMatrixUpdate.hitConversionMatrixUpdate(filePath,fileName,payloadMap);
            String updateResponse = conversionMatrixUpdate.updateResponse;

            if(!updateResponse.contains("200")){
                customAssert.assertTrue(false,"Error while conversion Matrix Upload");
            }

            System.out.println();
        }catch (Exception e){
            logger.error("Exception while updating ConversionMatrix on Client Admin");
            customAssert.assertTrue(false,"Exception while updating ConversionMatrix on Client Admin");
        }finally {

            String endUserUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
            String endUserPassword = ConfigureEnvironment.getEnvironmentProperty("password");

            check.hitCheck(endUserUserName,endUserPassword);

        }

    }

    public boolean updateConversionDate(int entityId, int convDateTypeId, CustomAssert customAssert){

        Boolean updateConversionData = true;

        Edit edit = new Edit();
        String editResponse;

        try{
            String entityName = "service data";
            editResponse = edit.hitEdit(entityName,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            if(editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("currencyConversionDateType").has("values")){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("currencyConversionDateType").getJSONObject("values").put("id",convDateTypeId);
            }else {

                JSONObject valuesJson = new JSONObject();
                valuesJson.put("id",convDateTypeId);
                valuesJson.put("name","");
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("currencyConversionDateType").put("values",valuesJson);

            }

            editResponse = edit.hitEdit(entityName,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating conversion Data");
                updateConversionData = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating service start date end date " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating conversion Data " + e.getStackTrace());
            updateConversionData = false;
        }

        return updateConversionData;
    }

    public boolean updateSplitAttributesServiceData(int entityId, String splitAttributeType, String splitRatioType, CustomAssert customAssert){

        Boolean updateConversionData = true;

        Edit edit = new Edit();
        String editResponse;

        try{
            String entityName = "service data";
            editResponse = edit.hitEdit(entityName,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitAttributeAvailable").put("values",true);

            if(editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitAttributeType").has("values")){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitAttributeType").getJSONObject("values").put("id",splitAttributeType);
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitAttributeType").getJSONObject("values").put("name","Department");

            }else {

                JSONObject valuesJson = new JSONObject();
                valuesJson.put("id",splitAttributeType);
                valuesJson.put("name","Department");
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitAttributeType").put("values",valuesJson);

            }

            if(editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitRatioType").has("values")){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitRatioType").getJSONObject("values").put("id",splitRatioType);
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitRatioType").getJSONObject("values").put("name","Percentage");
            }else {

                JSONObject valuesJson = new JSONObject();
                valuesJson.put("id",splitRatioType);
                valuesJson.put("name","Percentage");
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("splitRatioType").put("values",valuesJson);

            }

            editResponse = edit.hitEdit(entityName,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating split attributes on service data");
                updateConversionData = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating split attributes on service data" + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating split attributes on service data " + e.getStackTrace());
            updateConversionData = false;
        }

        return updateConversionData;
    }

    public boolean updateSDisBilAndPricingAvail(int serviceDataId, Boolean isBillable,Boolean pricingAvailable, CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;
        String serviceData = "service data";

        try{

            editResponse = edit.hitEdit(serviceData,serviceDataId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("billingAvailable").put("values",isBillable);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("pricingAvailable").put("values",pricingAvailable);

            editResponse = edit.hitEdit(serviceData,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating service start date and end date");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating service start date end date " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating service start date end date " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;
    }

    public boolean updateIsBillableAndPricingForReporting(int serviceDataId, Boolean pricingForReporting,Boolean billingAvailable, CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;
        String serviceData = "service data";

        try{

            editResponse = edit.hitEdit(serviceData,serviceDataId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("pricingAvailable").put("values",false);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("pricingForReporting").put("values",pricingForReporting);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("billingAvailable").put("values",billingAvailable);

            editResponse = edit.hitEdit(serviceData,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating IsBillable And PricingForReporting");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while while updating IsBillable And PricingForReporting " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating IsBillable And PricingForReporting " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;
    }

    //C89297
    public Boolean updateServiceDataMetaData(int serviceDataId,
                                             int serviceDataCategory,int serviceDataSubCategory,
                                             String serviceIdSupplier,String serviceIdClient,
                                             String name,
                                             CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;
        String serviceData = "service data";

        try{

            editResponse = edit.hitEdit(serviceData,serviceDataId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("pricingAvailable").put("values",false);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceDataServiceCategory").getJSONObject("values").put("id",serviceDataCategory);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceSubCategory").getJSONObject("values").put("id",serviceDataSubCategory);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceSubCategory").getJSONObject("values").put("parentId",serviceDataCategory);


            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdSupplier").put("values",serviceIdSupplier);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdClient").put("values",serviceIdClient);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values",name);

            editResponse = edit.hitEdit(serviceData,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating service data metadata fields");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while while updating service data metadata fields " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating service data metadata fields " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;

    }

    public boolean updateInvoiceDate(int invoiceId, String invoiceDate, CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;
        String invoice = "invoices";

        try{

            editResponse = edit.hitEdit(invoice,invoiceId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceDate").put("values",invoiceDate);

            editResponse = edit.hitEdit(invoice,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating invoice date on invoice");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating invoice date on invoice " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating invoice date on invoice " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;
    }

    public boolean updateInvoiceLineItemDate(int invoiceLineItemId, String startDate,String endDate, CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;
        String invoiceLieItem = "invoice line item";

        try{

            editResponse = edit.hitEdit(invoiceLieItem,invoiceLineItemId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceDate").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceStartDate").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceEndDate").put("values",endDate);

            editResponse = edit.hitEdit(invoiceLieItem,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating invoice date on invoice line Item");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating invoice date on invoice line Item" + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating invoice date on invoice line Item" + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;
    }

    public String getCreatePayloadFromClone(String entityName,int entityId){

        Clone clone = new Clone();

        String cloneResponse;
        String createPayload = null;
        cloneResponse = clone.hitCloneV2(entityName,entityId);
        try {
            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            cloneResponseJson.remove("header");
            cloneResponseJson.remove("session");
            cloneResponseJson.remove("actions");
            cloneResponseJson.remove("createLinks");
            cloneResponseJson.getJSONObject("body").remove("layoutInfo");
            cloneResponseJson.getJSONObject("body").remove("globalData");
            cloneResponseJson.getJSONObject("body").remove("errors");

            createPayload = cloneResponseJson.toString();

        }catch (Exception e){
            logger.error("Exception while creating payload for Create for  " + entityName  + " " + entityId);
        }

        return createPayload;
    }

    public String updatePayloadForDiffLineItemType(String createPayload,String valuesOptionName,String valuesOptionId){

        String updatedCreatePayload = null;
        try {
            JSONObject updatedCreatePayloadJson = new JSONObject(createPayload);
            JSONObject lineItemTypeJson = updatedCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("lineItemType");
            JSONObject valuesJson = new JSONObject();
            if(lineItemTypeJson.has("values")){

                valuesJson = lineItemTypeJson.getJSONObject("values");
                valuesJson.put("name",valuesOptionName);
                valuesJson.put("id",valuesOptionId);
                lineItemTypeJson.put("values",valuesJson);
            }else {
                valuesJson.put("name",valuesOptionName);
                valuesJson.put("id",valuesOptionId);
                lineItemTypeJson.append("values",valuesJson);
            }

            updatedCreatePayloadJson.getJSONObject("body").getJSONObject("data").put("lineItemType",lineItemTypeJson).toString();
            updatedCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("lineItemType").remove("options");

            updatedCreatePayload = updatedCreatePayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while creating uploaded payload for create " + e.getMessage());
        }

        return  updatedCreatePayload;
    }

    public Boolean validateNumberOfBillingDataGenerated(String payload,int numberOfExpectedBillingIds,CustomAssert customAssert){

        Boolean validationStatus = true;
        try {
            int reportId = 444;

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(reportId,payload);
            String reportListResponse = reportRendererListData.getListDataJsonStr();

            JSONObject reportListResponseJson = new JSONObject(reportListResponse);

            JSONArray dataArray = reportListResponseJson.getJSONArray("data");

            if(dataArray.length() != numberOfExpectedBillingIds){
                customAssert.assertTrue(false,"Expected Memo Billing Item is " + numberOfExpectedBillingIds + " actual Number of Memo Billing Item " + dataArray.length());
                validationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating C88814");
            validationStatus = false;
        }
        return validationStatus;

    }

    public Boolean validateExpectedMemoLinkedLineItem(int invoiceLineItem,int linkedLineItem,CustomAssert customAssert){

        Boolean validationStatus = true;
        TabListData tabListData = new TabListData();
        int expectedMemoTabId = 337;

        try {
            String payload = "{\"filterMap\":{\"entityTypeId\":188,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            tabListData.hitTabListDataV2(expectedMemoTabId,invoiceLineItemEntityTypeId,invoiceLineItem,payload);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            JSONObject tabListResponseJson = new JSONObject(tabListResponse);

            JSONObject indRowDataJson = tabListResponseJson.getJSONArray("data").getJSONObject(0);

            Iterator<String> keys = indRowDataJson.keys();
            String columnName;
            String linkedLineItemOnTab;
            while(keys.hasNext()) {
                String key = keys.next();
                columnName = indRowDataJson.getJSONObject(key).get("columnName").toString();

                if(columnName.equals("validated_by_line_item")){
                    linkedLineItemOnTab = indRowDataJson.getJSONObject(key).get("value").toString();
                    if(!linkedLineItemOnTab.equals("null")) linkedLineItemOnTab = linkedLineItemOnTab.split(":;")[1];
                    if(!String.valueOf(linkedLineItem).equals(linkedLineItemOnTab)){
                        customAssert.assertEquals("Expected Line Item " + linkedLineItem , "Actual Line Item " + linkedLineItemOnTab);
                    }
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Expected Memo LinkedLineItem Scenario " + e.getStackTrace());
        }
        return validationStatus;
    }

    public String getConsumptionCreated(String flowToTest, int serviceDataId, ArrayList<Integer> consumptionIds) {
        int offset = 0; // default value for creating payload
        int size = 2000; // default value for creating payload

        Show show = new Show();
        String result = "pass";
        logger.info("Waiting for Consumption to be Created for Flow [{}].", flowToTest);
        try {
            show.hitShow(serviceDataEntityTypeId, serviceDataId);
            String showPageResponseStr = show.getShowJsonStr();
            List<String> dataUrl = show.getShowPageTabUrl(showPageResponseStr, Show.TabURL.dataURL);

            String consumptionDataURL = null;
            for (String Url : dataUrl) {
                if (Url.contains("listRenderer/list/376/tablistdata")) // for consumption
                {
                    consumptionDataURL = Url;
                    break;
                } else
                    continue;

            }


            if (consumptionDataURL != null) {

                logger.info("Time Out for Consumption to be created is {} milliseconds", consumptionToBeCreatedTimeOut);
                logger.info("Checking if Consumption has been created or not.");

                String payload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":" +
                        offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc\",\"filterJson\":{}}}";


                logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);

                logger.info("Hitting tab data API for Consumption for service data Id : [{}]", serviceDataId);

                String showPageDataUrlResponseStr = Show.hitshowPageTabUrl(consumptionDataURL, payload);
                JSONObject showPageDataUrlResponseJson = new JSONObject(showPageDataUrlResponseStr);

                int numberOfConsumption = showPageDataUrlResponseJson.getJSONArray("data").length();

                if (numberOfConsumption > 0) {

                    logger.info("Consumptions have been created. ");

                    for (int i = 0; i < numberOfConsumption; i++) {

                        JSONObject data = showPageDataUrlResponseJson.getJSONArray("data").getJSONObject(i);
                        Set<String> keys = data.keySet();
                        for (String key : keys) {
                            String columnName = data.getJSONObject(key).getString("columnName");
                            if (columnName.contentEquals("id")) {
//									consumptionIds[i] = Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]);
                                consumptionIds.add(Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]));
                                break;
                            } else
                                continue;

                        }
                    }
                }
            } else {
                logger.error("There is no consumption URL in the show page response of Service data : [{}] for flow : [{}]", serviceDataId, flowToTest);
                result = "skip";
                return result;
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Consumption to get created to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }
        return result;
    }

    //Login with Client Admin necessary
    public boolean resetAllFormatAndTemplate(String clientId){

        Boolean resetStatus = true;

        try{

//            Format format = new Format();
//            String formatList = format.getFormatList(clientId);
//
//            String formatId;String defaultStatus;
//            JSONArray formatIdListJsonArray = new JSONArray(formatList);
//
//            for (int i = 0; i < formatIdListJsonArray.length(); i++) {
//                formatId = formatIdListJsonArray.getJSONObject(i).get("id").toString();
//                defaultStatus = formatIdListJsonArray.getJSONObject(i).get("default").toString();
//
//                if(defaultStatus.equals("false")) {
//                    format.deleteFormat(formatId);
//                }
//            }

            Template template = new Template();
            String templateIdList = template.getTemplateList("518","{\"filterMap\":{}}");

            if (!APIUtils.validJsonResponse(templateIdList)) {
                resetStatus = false;
            } else {

                JSONObject templateIdListJson = new JSONObject(templateIdList);

                JSONArray dataArray = templateIdListJson.getJSONArray("data");
                JSONObject indvJson;

                String columnName;
                String key;
                for(int i=0;i<dataArray.length();i++){

                    indvJson= dataArray.getJSONObject(i);

                    Iterator itr = indvJson.keys();
                    String templateId = "";
                    String defaultFlag = "";
                    while (itr.hasNext()) {
                        key = itr.next().toString();
                        columnName  = indvJson.getJSONObject(key).get("columnName").toString();

                        if(columnName.equals("id")){
                            templateId = indvJson.getJSONObject(key).get("value").toString();

                        }

                        if(columnName.equals("default")){
                            defaultFlag = indvJson.getJSONObject(key).get("value").toString();

                        }
                    }
                    if(defaultFlag.equals("false")) {
                        template.deleteTemplate(templateId);
                    }

                }
            }

        }catch (Exception e){
            resetStatus = false;
        }

        return resetStatus;
    }

    public boolean checkIfPartButtonPresentInV3Response(int invoiceId,String expectedButtonName,Boolean visible,CustomAssert customAssert) {

        Boolean expectedButtonFound = false;

        try {
            String invoiceWorkFlowResponse = Actions.getActionsV3Response(invoiceEntityTypeId, invoiceId);

            if (JSONUtility.validjson(invoiceWorkFlowResponse)) {

                JSONObject invoiceWorkFlowResponseJson = new JSONObject(invoiceWorkFlowResponse);
                JSONArray layoutActionsArray = invoiceWorkFlowResponseJson.getJSONArray("layoutActions");

                for (int i = 0; i < layoutActionsArray.length(); i++) {

                    if (layoutActionsArray.getJSONObject(i).get("name").toString().equals(expectedButtonName)) {

                        expectedButtonFound = true;
                        break;
                    }
                }

            }

            if(!expectedButtonFound){
                if(visible) {
                    customAssert.assertTrue(false, expectedButtonName + " Not present in the invoice show page");
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while checking " + expectedButtonName + " is present on Invoice Show Page ");
        }
        return expectedButtonFound;

    }

    public Boolean validateSchedulerForPassFailEntities(String expJobName,int expPassCount,int expFailCount,int expSubmissionCount,CustomAssert customAssert){

        Boolean validationStatus = true;
        Boolean jobFound = false;
        try{

            Thread.sleep(5000);
            Fetch fetch = new Fetch();

            int numOfTimesToCheck = 5;
            int initialCount = 0;
            JSONObject firsRowOfSchPageJson = new JSONObject();

            while (initialCount < numOfTimesToCheck) {
                fetch.hitFetch();
                String fetchResponse = fetch.getFetchJsonStr();

                JSONObject fetchResponseJson = new JSONObject(fetchResponse);

                String jobHeader =  fetchResponseJson.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks").getJSONObject(0).get("jobHeader").toString();
                firsRowOfSchPageJson = fetchResponseJson.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks").getJSONObject(0);

                if(jobHeader.equals(expJobName) ){jobFound = true;
                    break;
                }
                initialCount++;
                Thread.sleep(5000);
            }
            if(jobFound == true) {
                if (firsRowOfSchPageJson.getJSONObject("status").get("id").toString().equals("3")) {

                    int submittedRecordsCount = Integer.parseInt(firsRowOfSchPageJson.get("submittedRecordsCount").toString());

                    int failedRecordsCount = Integer.parseInt(firsRowOfSchPageJson.get("failedRecordsCount").toString());
                    int successRecordsCount = Integer.parseInt(firsRowOfSchPageJson.get("successfullyProcessedRecordsCount").toString());

                    if(submittedRecordsCount != expSubmissionCount){
                        customAssert.assertTrue(false,"Expected submitted records : " + expSubmissionCount + " for the job " + expJobName + " not matched");
                        validationStatus = false;
                    }

                    if(failedRecordsCount != expFailCount){
                        customAssert.assertTrue(false,"Expected Failed records : " + expSubmissionCount + " for the job " + expJobName + " not matched");
                        validationStatus = false;
                    }

                    if(successRecordsCount != expPassCount){
                        customAssert.assertTrue(false,"Expected Pass records : " + expSubmissionCount + " for the job " + expJobName + " not matched");
                        validationStatus = false;
                    }

                } else {

                    customAssert.assertTrue(false, "After 5 seconds the completion status id not equal to 3 Completion message " + firsRowOfSchPageJson.get("completionMessage").toString());
                    validationStatus = false;
                }

            }else {
                customAssert.assertTrue(false,"Job Not Found in the scheduler Page");
                validationStatus = false;
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Scheduler For Pass Fail Entities");
            validationStatus = false;
        }

        return validationStatus;
    }

    public List<HashMap<String,String>> getListingResponseInvoice(int listId,String startDate,String endDate,int filterId,int entityFieldId,String customFieldValue,CustomAssert customAssert){

        List<HashMap<String,String>> listValuesMapList = new ArrayList<>();

        ListRendererListData listRendererListData = new ListRendererListData();

        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":67," +
                    "\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"157\":{\"filterId\":\"157\",\"filterName\":" +
                    "\"invoicePeriodStartDate\",\"start\":\"" + startDate + "\",\"end\":\"" + startDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"158\":{\"filterId\":\"158\",\"filterName\":" +
                    "\"invoicePeriodEndDate\",\"start\":\"" + endDate + "\",\"end\":\"" + endDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"" + filterId + "\":{\"filterId\":\"" + filterId + "\",\"filterName\":\"103487\"," +
                    "\"entityFieldId\":" + entityFieldId + ",\"entityFieldHtmlType\":18,\"min\":\"" + customFieldValue + "\",\"max\":\"" + customFieldValue + "\"}}}," +
                    "\"selectedColumns\":[]}";

            listRendererListData.hitListRendererListDataV2(listId,payload);
            String lisResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(lisResponse)){
                customAssert.assertTrue(false,"List Response is not a valid json");
                return listValuesMapList;
            }
            JSONArray dataArray = new JSONObject(lisResponse).getJSONArray("data");

            for(int i =0;i<dataArray.length();i++) {
                JSONObject firstRowJson = dataArray.getJSONObject(i);

                HashMap<String, String> listValuesMap = new HashMap<>();

                Iterator<String> keys = firstRowJson.keys();
                String key;
                while (keys.hasNext()) {

                    key = keys.next();

                    listValuesMap.put(firstRowJson.getJSONObject(key).get("columnName").toString(), firstRowJson.getJSONObject(key).get("value").toString());

                }
                listValuesMapList.add(listValuesMap);
            }
        }catch (Exception e){
            logger.error("Exception while getting listing response");
        }

        return listValuesMapList;
    }

    public HashMap<String,String> getReportListResp(int listId,int entityTypeId,int filterId,int entityFieldId,String customFieldValue){

        HashMap<String,String> listValuesMap = new HashMap<>();

        ReportRendererListData reportRendererListData = new ReportRendererListData();
        String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                "\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId + "\",\"filterName\":\"105556\"," +
                "\"entityFieldId\":" + entityFieldId + ",\"entityFieldHtmlType\":19,\"min\":\"" + customFieldValue + "\",\"max\":\"" + customFieldValue + "\"," +
                "\"suffix\":null}}},\"selectedColumns\":[]}";
        try{
            reportRendererListData.hitReportRendererListData(listId,payload);
            String lisResponse = reportRendererListData.getListDataJsonStr();

            JSONObject firstRowJson = new JSONObject(lisResponse).getJSONArray("data").getJSONObject(0);

            Iterator<String> keys = firstRowJson.keys();
            String key;
            while (keys.hasNext()){

                key = keys.next();

                listValuesMap.put(firstRowJson.getJSONObject(key).get("columnName").toString(),firstRowJson.getJSONObject(key).get("value").toString());

            }

        }catch (Exception e){
            logger.error("Exception while getting listing response");
        }

        return listValuesMap;
    }

    public String getFilPayloadForSpecCustomFieldInvoice(int filterId,int entityFieldId,String customFieldValue) {

        String payload = "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                "\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId + "\",\"filterName\":\"105556\"," +
                "\"entityFieldId\":" + entityFieldId + ",\"entityFieldHtmlType\":19,\"min\":\"" + customFieldValue + "\",\"max\":\"" + customFieldValue + "\"," +
                "\"suffix\":null}}},\"selectedColumns\":[]}";


        return payload;
    }

    public String updateCustomField(String entityName,int entityId,int customFieldId,CustomAssert customAssert) {

        Edit edit = new Edit();
        String editPayload = edit.getEditPayload(entityName, entityId);
        String customFieldValue = "";

        try {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            JSONObject editPayloadJson = new JSONObject(editPayload);
            customFieldValue = DateUtils.getCurrentTimeStamp().replace("_", "");
            customFieldValue = customFieldValue.replace(" ", "");

            Double customFieldValueDouble = Double.parseDouble(customFieldValue);
            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customFieldId).put("values", customFieldValueDouble);

            String editResponse = edit.hitEdit(entityName, editPayloadJson.toString());

            if (!editResponse.contains("success")) {
                customAssert.assertTrue(false, "Error while editing custom field dyn" + customFieldId + " on invoice");

            }

            Show show = new Show();

            show.hitShowVersion2(entityTypeId, entityId);

            String showResponse = show.getShowJsonStr();
            JSONObject showResponseJson = new JSONObject(showResponse);

            try {
                customFieldValue = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customFieldId).get("values").toString();
            } catch (Exception e) {
                logger.error("Error while getting custom field value");
            }


        }catch (Exception e){

        }
        return customFieldValue;
    }

    public List<HashMap<String,String>> getListResp(int listId,int entityTypeId,int filterId,int entityFieldId,String customFieldValue,CustomAssert customAssert){

        List<HashMap<String,String>> listResp = new ArrayList<>();

        ListRendererListData listRendererListData = new ListRendererListData();

        String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                "\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId + "\",\"filterName\":\"105556\"," +
                "\"entityFieldId\":" + entityFieldId + ",\"entityFieldHtmlType\":19,\"min\":\"" + customFieldValue + "\",\"max\":\"" + customFieldValue + "\"," +
                "\"suffix\":null}}},\"selectedColumns\":[]}";
        try{
            listRendererListData.hitListRendererListDataV2(listId,payload);

            String lisResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(lisResponse)){
                customAssert.assertTrue(false,"List Response is not a valid json");
                return listResp;
            }

            JSONArray dataArray = new JSONObject(lisResponse).getJSONArray("data");

            for(int i=0;i<dataArray.length();i++) {
                JSONObject firstRowJson = dataArray.getJSONObject(i);

                HashMap<String, String> listValuesMap = new HashMap<>();
                Iterator<String> keys = firstRowJson.keys();
                String key;
                while (keys.hasNext()) {

                    key = keys.next();

                    listValuesMap.put(firstRowJson.getJSONObject(key).get("columnName").toString(), firstRowJson.getJSONObject(key).get("value").toString());

                }

                listResp.add(listValuesMap);
            }

        }catch (Exception e){
            logger.error("Exception while getting listing response");
        }

        return listResp;
    }

    public HashMap<String,String> checkIfEntityIsPresent(List<HashMap<String, String>> listResp,String recordId){

        HashMap<String,String> columnValuesMap = new HashMap<>();
        try {

            String id;

            for (int i = 0; i < listResp.size(); i++) {
                columnValuesMap = new HashMap<>();
                columnValuesMap = listResp.get(i);

                id = columnValuesMap.get("id").split(":;")[1];

                if (id.equals(recordId)) {
                    break;
                }
            }
        }catch (Exception e){

        }
        return columnValuesMap;
    }

    public void waitForScheduler(String flowToTest, int newTaskId, long schedulerPollingTime,long schedulerWaitTimeout,CustomAssert csAssert) {
        logger.info("Waiting for Scheduler to Complete for Flow [{}].", flowToTest);
        try {

            logger.info("Time Out for Scheduler is {} milliseconds", schedulerWaitTimeout);
            long timeSpent = 0;

            if (newTaskId != -1) {
                logger.info("Checking if Bulk Update Task has completed or not for Flow [{}]", flowToTest);

                while (timeSpent < schedulerWaitTimeout) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", schedulerPollingTime);
                    Thread.sleep(schedulerPollingTime);

                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();
                    String fetchResponse = fetchObj.getFetchJsonStr();
                    logger.info("Getting Status of Bulk Update Task for Flow [{}]", flowToTest);
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchResponse, newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {

                        logger.info("Bulk Update Task Completed for Flow [{}]", flowToTest);
                        break;
                    } else {
                        timeSpent += schedulerPollingTime;
                        logger.info("Bulk Update Task is not finished yet for Flow [{}]", flowToTest);
                    }
                }
            } else {
                logger.info("Couldn't get Bulk Update Task Job Id for Flow [{}]. Hence waiting for Task Time Out i.e. {}", flowToTest, schedulerWaitTimeout);
                Thread.sleep(schedulerWaitTimeout);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Waiting for Scheduler to Finish for Flow [" + flowToTest + "]. " + e.getMessage());
        }
    }

    public boolean validateLineItemValidationStatus(int lineItemId,String expectedStatus,CustomAssert customAssert){

        Boolean validationStatus = true;
        try{

            long timeSpent = 0;
            Long lineItemValidationTimeOut = 1200000L;

            while (timeSpent <= lineItemValidationTimeOut && ShowHelper.isLineItemUnderOngoingValidation(invoiceLineItemEntityTypeId, lineItemId)) {
                logger.info("Invoice Line Item having Id {} is still Under Ongoing Validation. Waiting for it to finish validation.", lineItemId);
                logger.info("time spent is : [{}]", timeSpent);
                Thread.sleep(10000);
                timeSpent += 10000;
            }

            if (timeSpent < lineItemValidationTimeOut) {
                //Line Item Validation is Completed.

                String actualValidationStatus = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, lineItemId, "validationstatus");

                if(!actualValidationStatus.equals(expectedStatus)){
                    logger.error("Expected and Actual Validation Status Didn't matched for line Item ");
                    logger.error("Expected Status : " + expectedStatus + " Actual Status : " + actualValidationStatus);

                    customAssert.assertTrue(false,"Expected and Actual Validation Status Didn't matched for line Item " + "Expected Status : " + expectedStatus + " Actual Status : " + actualValidationStatus);
                    validationStatus = false;
                }

            } else {
                //Line Item Validation is not yet Completed.
                logger.error("During Re validation with Expected Status" + expectedStatus + " Invoice Line Item Validation couldn't be completed within " + lineItemValidationTimeOut + " milliseconds");
                customAssert.assertTrue(false, "During Re validation with Expected Status" + expectedStatus + " Invoice Line Item Validation couldn't be completed within " + lineItemValidationTimeOut + " milliseconds");
            }

        }catch (Exception e){
            logger.error("Exception while validating Line Item Validation Status during re validating");
            customAssert.assertTrue(false,"Exception while validating Line Item Validation Status during re validating");
            validationStatus = false;
        }

        return validationStatus;
    }

    public static int getEffectiveRateCard(int contractId,String datePattern,String effectiveDate,CustomAssert customAssert){

        int effectiveRateCard =-1;

        try{
            Show show = new Show();
            show.hitShowVersion2(61,contractId);
            String showResponse = show.getShowJsonStr();

//            String effectiveDate = ShowHelper.getValueOfField("effectivedate",showResponse);

            JSONObject showResponseJson = new JSONObject(showResponse);

            JSONArray valuesArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("existingRateCards").getJSONArray("values");

            if(effectiveDate == null){
                if(valuesArray.length() > 1){
//                  In case when effective date is null and more than one rate card is defined
//                  then there will be no rate card to be applied
                    return 0;
                }
            }

            for(int i =0;i<valuesArray.length();i++){

                String rateCardId = valuesArray.getJSONObject(i).getJSONObject("rateCard").get("id").toString();
                String rateCardFromDate = valuesArray.getJSONObject(i).get("rateCardFromDate").toString();
                String rateCardToDate = valuesArray.getJSONObject(i).get("rateCardToDate").toString();

                rateCardFromDate = DateUtils.convertDateToAnyFormat(rateCardFromDate,datePattern);
                rateCardToDate = DateUtils.convertDateToAnyFormat(rateCardToDate,datePattern);

                int comparisonFactor = DateUtils.compareTwoDates(datePattern,rateCardFromDate,rateCardToDate,effectiveDate);
                if((comparisonFactor == 0 )|| (comparisonFactor == -1)){
                    effectiveRateCard = Integer.parseInt(rateCardId);
                    break;
                }else if(comparisonFactor == 1 && i == valuesArray.length() -1){
                    effectiveRateCard = Integer.parseInt(rateCardId);
                }
            }

        }catch (Exception e){
            logger.error("Exception while getting effective Rate Id form contract " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while getting effective Rate Id form contract " + e.getStackTrace());
        }
        return effectiveRateCard;
    }

    public static int getEffectiveRateCard(int contractId,String datePattern,String effectiveDate){

        int effectiveRateCard =-1;

        try{
            Show show = new Show();
            show.hitShowVersion2(61,contractId);
            String showResponse = show.getShowJsonStr();

//            String effectiveDate = ShowHelper.getValueOfField("effectivedate",showResponse);

            JSONObject showResponseJson = new JSONObject(showResponse);

            JSONArray valuesArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("existingRateCards").getJSONArray("values");

            if(effectiveDate == null){
                if(valuesArray.length() > 1){
//                  In case when effective date is null and more than one rate card is defined
//                  then there will be no rate card to be applied
                    return 0;
                }
            }

            for(int i =0;i<valuesArray.length();i++){

                String rateCardId = valuesArray.getJSONObject(i).getJSONObject("rateCard").get("id").toString();
                String rateCardFromDate = valuesArray.getJSONObject(i).get("rateCardFromDate").toString();
                String rateCardToDate = valuesArray.getJSONObject(i).get("rateCardToDate").toString();

                rateCardFromDate = DateUtils.convertDateToAnyFormat(rateCardFromDate,datePattern);
                rateCardToDate = DateUtils.convertDateToAnyFormat(rateCardToDate,datePattern);

                int comparisonFactor = DateUtils.compareTwoDates(datePattern,rateCardFromDate,rateCardToDate,effectiveDate);
                if((comparisonFactor == 0 )|| (comparisonFactor == -1)){
                    effectiveRateCard = Integer.parseInt(rateCardId);
                    break;
                }else if(comparisonFactor == 1 && i == valuesArray.length() -1){
                    effectiveRateCard = Integer.parseInt(rateCardId);
                }
            }

        }catch (Exception e){
            logger.error("Exception while getting effective Rate Id form contract " + e.getStackTrace());
        }
        return effectiveRateCard;
    }

    public static int getClientCurrencyId(int clientId,CustomAssert customAssert){

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        int clientCurrencyId = -1;
        try{

            List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select primary_currency_id from client where id=" + clientId);

            clientCurrencyId = Integer.parseInt(sqlOutput.get(0).get(0));

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting client currency Id");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        return clientCurrencyId;
    }

    public static int getClientCurrencyId(int clientId){

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        int clientCurrencyId = -1;
        try{

            List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select primary_currency_id from client where id=" + clientId);

            clientCurrencyId = Integer.parseInt(sqlOutput.get(0).get(0));

        }catch (Exception e){
            logger.error("Exception while getting client currency Id");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        return clientCurrencyId;
    }

    public static int getClientRateCard(int clientId,CustomAssert customAssert){

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        int clientRateCardId = -1;
        try{

            List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select rate_card_id from link_entity_rate_card where entity_id=" + clientId  + "and entity_type_id=2 and deleted = false");

            clientRateCardId = Integer.parseInt(sqlOutput.get(0).get(0));

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting client currency Id");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        return clientRateCardId;
    }

    public static int getClientRateCard(int clientId,String effectiveDate,String effectiveDateFormat,CustomAssert customAssert){

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        int clientRateCardId = -1;
        try{

            List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select rate_card_id,start_date,end_date from link_entity_rate_card where entity_id=" + clientId  + " and entity_type_id=2 and deleted = false order by start_date");

            clientRateCardId = Integer.parseInt(sqlOutput.get(0).get(0));

            if(effectiveDate.equals(null)){
                return clientRateCardId;
            }else if(effectiveDate.equals("null")){
                return clientRateCardId;
            }else if(effectiveDate.equals("")){
                return clientRateCardId;
            }
            effectiveDate = DateUtils.converDateToAnyFormat(effectiveDate,effectiveDateFormat,"yyyy-MM-dd");
            String startDate = sqlOutput.get(0).get(1);
            String endDate = sqlOutput.get(0).get(2);
            for(List<String> rowOutput:sqlOutput){
                String startDateRow = rowOutput.get(1);
                endDate = rowOutput.get(2);

                if(DateUtils.compareTwoDates("yyyy-MM-dd",startDateRow,endDate,effectiveDate) == 0){
                    clientRateCardId = Integer.parseInt(rowOutput.get(0));
                    return clientRateCardId;
                }

            }
            if(DateUtils.compareTwoDates("yyyy-MM-dd",startDate,endDate,effectiveDate) == -1){
                clientRateCardId = Integer.parseInt(sqlOutput.get(0).get(0));
                return clientRateCardId;
            }

            if(DateUtils.compareTwoDates("yyyy-MM-dd",startDate,endDate,effectiveDate) == 1){
                clientRateCardId = Integer.parseInt(sqlOutput.get(sqlOutput.size() -1).get(0));
                return clientRateCardId;
            }

        }catch (Exception e){
            customAssert.assertEquals("Exception while getting client rate card Id" ,"Exception should not occur");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        return clientRateCardId;
    }

    public static String getCurrency(int currencyId){

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        String clientCurrency = "";
        try{

            List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select short_name from currency where id=" + currencyId);

            clientCurrency = sqlOutput.get(0).get(0);

        }catch (Exception e){
            logger.error("Exception while getting Short Name for client currency ");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        return clientCurrency;
    }

    public static String getCurrencyName(int currencyId){

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        String clientCurrency = "";
        try{

            List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select name from currency where id=" + currencyId);

            clientCurrency = sqlOutput.get(0).get(0);

        }catch (Exception e){
            logger.error("Exception while getting Short Name for client currency ");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        return clientCurrency;
    }

    public static Double getConvFacWRTClient(int entityTypeId,int entityId,int contractId,int clientId,String datePattern,CustomAssert customAssert){

        Double conversionFactor = 0.0;
        try {
            String effectiveDate = "";

            //If entity is service data then consider service end date
            if(entityTypeId == 64){

                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "enddatevalue");
            }else if(entityTypeId == 165){//To update according to client admin
                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "invoicedatevalue");     //If from client admin invoice end date is chosen
            }else if(entityTypeId == 181){
                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "enddatevalue");     //
            }else if(entityTypeId == 61){
                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "effectivedatevalue");     //
            }
            else {
                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "invoicedatevalue");
            }
            int clientCurrencyId = InvoiceHelper.getClientCurrencyId(clientId,customAssert);

            int invoiceCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(entityTypeId, entityId, "currency id"));

            int rateCardId = InvoiceHelper.getEffectiveRateCard(contractId, datePattern, effectiveDate);

            if (rateCardId == 0 || rateCardId == -1) {
                rateCardId = getClientRateCard(clientId,effectiveDate,datePattern,customAssert);
            }

            if (rateCardId != 0) {
                conversionFactor = getConversionFactor(rateCardId, invoiceCurrencyId, clientCurrencyId, customAssert);
            } else {
                customAssert.assertTrue(false, "Rate card Id value is 0");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting conversion Factor ");
        }
        return conversionFactor;
    }

    //  Getting conversion factor by providing rate card Id currency from and currency To
    public static Double getConversionFactor(int rateCardId, int currFrom, int currTo, CustomAssert customAssert) {

        Double conversionFactor = 0.0;
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        try {
            conversionFactor = Double.parseDouble(postgreSQLJDBC.doSelect("select rate_value from rate_card_conversion where rate_card_id = " + rateCardId +
                    " and currency_from = " + currFrom + " and currency_to = " + currTo).get(0).get(0));
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while fetching conversion id from DB " + e.getStackTrace());
        } finally {
            postgreSQLJDBC.closeConnection();
        }
        return conversionFactor;

    }

    public boolean validateBillingDataListingPageData(int serviceDataId, int consumptionId, String invoiceId, String lineItemId,
                                                      String billingConfigFilePath,String billingConfigFileName,
                                                      Map<String,Map<String, String>> billingRecordAccToStartDate,
                                                      Boolean parentHeirarchyToBeChecked,String flowToTest,
                                                      CustomAssert customAssert) {

        Boolean validationBillingDataListingPage = true;
        try {

            logger.info("Validating Billing Data On Listing Page");
            Show show = new Show();
            show.hitShowVersion2(serviceDataEntityTypeId, serviceDataId);
            String showResponse = show.getShowJsonStr();

            Map<String, String> listColNameValue = new HashMap<>();
            String listColumnName;
            String listColumnValue;
            String showPageColumnName;
            String showPageColumnValue;
            String parentName = "null";
            int parentId = -1;
            String baseSpecific = "";
            baseSpecific = ShowHelper.getValueOfField("basespecific", showResponse);

            if (baseSpecific != null) {
                if (baseSpecific.contains("true")) {

                }
            }
            for (Map.Entry<String, Map<String, String>> entry : billingRecordAccToStartDate.entrySet()) {

                entry.getKey();
                listColNameValue = entry.getValue();

                innerLoop:
                for (Map.Entry<String, String> entry1 : listColNameValue.entrySet()) {

                    listColumnName = entry1.getKey();
                    listColumnValue = entry1.getValue();

                    if (parentHeirarchyToBeChecked && listColumnName.equals("parentid")) {
                        listColumnValue = entry1.getValue();

                        if (listColumnValue == null) {
                            customAssert.assertTrue(false, "For Billing Record Parent Service Data Id not found in child service data" + " for the flow" + flowToTest);
                        } else {
                            try {
                                parentId = Integer.parseInt(listColumnValue.split(":;")[1]);
                            } catch (Exception e) {
                                customAssert.assertTrue(false, "Exception while getting parent service data id from child service data in Billing Data Report" + " for the flow" + flowToTest);
                            }
                        }
                    }

                    if (parentHeirarchyToBeChecked && listColumnName.equals("parentname")) {
                        listColumnValue = entry1.getValue();

                        if (listColumnValue == null) {
                            customAssert.assertTrue(false, "Parent Service Data Name not found in child service data in billing Report" + " for the flow" + flowToTest);
                        } else {
                            try {
                                parentName = listColumnValue;
                            } catch (Exception e) {
                                customAssert.assertTrue(false, "Exception while getting parent service data name from child service data in Billing Data Report" + " for the flow" + flowToTest);
                            }
                        }
                    }
//                    if(listColumnName.equalsIgnoreCase("amountInServiceDataCurrency")){
//
//                        String expBaseAmount = getBaseAmount(serviceDataId,customAssert);
//                        if(consumptionId!=-1) {
//                            expBaseAmount = calcSysAmountARC(serviceDataId, consumptionId, customAssert).toString();
//                        }
//                        if(!expBaseAmount.contains(listColumnValue)){
//                            customAssert.assertTrue(false,"On Billing Report Expected Value for column amountInServiceDataCurrency didn't matched with Actual ");
//                        }
//                        continue;
//                    }

                    showPageColumnName = ParseConfigFile.getValueFromConfigFile(billingConfigFilePath, billingConfigFileName, "show page mapping listing page", listColumnName.toLowerCase());

                    if (showPageColumnName == null) {
                        logger.info("Show Page Field not defined for listing Column Name " + listColumnName);
                        continue;
                    }

                    if (baseSpecific.equals("true") && (listColumnName.equals("invoiced") ||
                            listColumnName.equalsIgnoreCase("invoiceid") ||
                            listColumnName.equalsIgnoreCase("invoicelineitemid")||
                            listColumnName.equalsIgnoreCase("invoicingCurrency")
                            )) {
                        if (listColNameValue.get("chargetype").equals("Base Charge")) {
                            if (listColumnName.equalsIgnoreCase("invoiced")) {

                                if (invoiceId.equalsIgnoreCase("")) {
                                    if (!listColumnValue.equalsIgnoreCase("No")) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoiced is " + listColumnValue + " Expected Value is No when invoice is not generated for billing data" + " for the flow" + flowToTest);
                                    }
                                    continue innerLoop;
                                }
                            }

                            if (listColumnName.equalsIgnoreCase("invoiceid")) {

                                if (invoiceId.equalsIgnoreCase("")) {
                                    if (!listColumnValue.equalsIgnoreCase("null")) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoiced Id is " + listColumnValue + " Expected Value is " + invoiceId + "when invoice is not generated for billing data" + " for the flow" + flowToTest);
                                    }
                                    continue innerLoop;
                                }
                            }

                            if (listColumnName.equalsIgnoreCase("invoicelineitemid")) {

                                if (lineItemId.equalsIgnoreCase("")) {
                                    if (!listColumnValue.equalsIgnoreCase("null")) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoicelineitemid is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is not generated for billing data" + " for the flow" + flowToTest);
                                    }

                                }
                                continue innerLoop;
                            }

                            if (listColumnName.equalsIgnoreCase("invoicingCurrency")) {

                                String serviceDataCurrency = ShowHelper.getValueOfField("invoicingcurrency",showResponse);
                                int InvLineLineItemId = - 1;
                                try{
                                    InvLineLineItemId = Integer.parseInt(lineItemId);
                                }catch (Exception e){

                                }
                                String lineItemCurrency = "";
                                if(InvLineLineItemId != -1) {
                                    lineItemCurrency = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, InvLineLineItemId, "currency");
                                }

                                String expInvoicingCurr = "";

                                if((serviceDataCurrency == null || serviceDataCurrency.equals("null")) && listColNameValue.get("invoiced").equals("Yes")){
                                    expInvoicingCurr = lineItemCurrency;
                                }else if((serviceDataCurrency == null) || serviceDataCurrency.equals("null")){
                                    expInvoicingCurr = "";
                                }else {
                                    expInvoicingCurr = serviceDataCurrency;
                                }

                                if(!listColumnValue.equals(expInvoicingCurr)){
                                    customAssert.assertTrue(false,"Expected and actual value of invoicing currency mismatched on billing record Expected Invoicing currency " + expInvoicingCurr + " Actual Invoicing Currency " + listColumnValue);
                                }

                                continue innerLoop;
                            }

                        }else if (listColNameValue.get("chargetype").equals("ARC/RRC")) {

                            if (listColumnName.equalsIgnoreCase("invoiced")) {

                                if (invoiceId.equalsIgnoreCase("") || invoiceId.equalsIgnoreCase("-1")) {
                                    if (!listColumnValue.equalsIgnoreCase("No")) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoiced is " + listColumnValue + " Expected Value is No when invoice is not generated for billing data" + " for the flow" + flowToTest);
                                    }
                                } else {
                                    if (!listColumnValue.equalsIgnoreCase("Yes")) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoiced is " + listColumnValue + " Expected Value is Yes when invoice is generated for billing data" + " for the flow" + flowToTest);
                                    }
                                }
                                continue innerLoop;
                            }


                            if (listColumnName.equalsIgnoreCase("invoiceid")) {

                                if (invoiceId.equalsIgnoreCase("")) {
                                    if (!listColumnValue.equalsIgnoreCase("null")) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoiced Id is " + listColumnValue + " Expected Value is " + invoiceId + "when invoice is not generated for billing data" + " for the flow" + flowToTest);
                                    }
                                    continue innerLoop;
                                } else {
                                    if (!listColumnValue.contains(invoiceId)) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoiced Id is " + listColumnValue + " Expected Value is " + invoiceId + "when invoice is generated for billing data" + " for the flow" + flowToTest);
                                    }
                                }
                            }

                            if (listColumnName.equalsIgnoreCase("invoicelineitemid")) {

                                if (lineItemId.equalsIgnoreCase("")) {
                                    if (!listColumnValue.equalsIgnoreCase("null")) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoicelineitemid is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is not generated for billing data" + " for the flow" + flowToTest);
                                    }

                                } else {
                                    if (!listColumnValue.contains(lineItemId)) {
                                        customAssert.assertTrue(false, "On Billing Report Value for column invoicelineitemid Id is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is generated for billing data" + " for the flow" + flowToTest);
                                    }
                                }
                                continue innerLoop;
                            }

                            if (listColumnName.equalsIgnoreCase("invoicingCurrency")) {

                                String serviceDataCurrency = ShowHelper.getValueOfField("invoicingcurrency",showResponse);
                                int InvLineLineItemId = - 1;
                                try{
                                    InvLineLineItemId = Integer.parseInt(lineItemId);
                                }catch (Exception e){

                                }
                                String lineItemCurrency = "";
                                if(InvLineLineItemId != -1) {
                                    lineItemCurrency = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, InvLineLineItemId, "currency");
                                }

                                String expInvoicingCurr = "";

                                if((serviceDataCurrency == null || serviceDataCurrency.equals("null")) && listColNameValue.get("invoiced").equals("Yes")){
                                    expInvoicingCurr = lineItemCurrency;
                                }else if((serviceDataCurrency == null) || serviceDataCurrency.equals("null")){
                                    expInvoicingCurr = "";
                                }else {
                                    expInvoicingCurr = serviceDataCurrency;
                                }

                                if(!listColumnValue.equals(expInvoicingCurr)){
                                    customAssert.assertTrue(false,"Expected and actual value of invoicing currency mismatched on billing record Expected Invoicing currency " + expInvoicingCurr + " Actual Invoicing Currency " + listColumnValue);
                                }

                                continue innerLoop;
                            }
                        }

                    } else {
                        if (listColumnName.equalsIgnoreCase("invoiced")) {

                            if (invoiceId.equalsIgnoreCase("") || invoiceId.equalsIgnoreCase("-1")) {
                                if (!listColumnValue.equalsIgnoreCase("No")) {
                                    customAssert.assertTrue(false, "On Billing Report Value for column invoiced is " + listColumnValue + " Expected Value is No when invoice is not generated for billing data" + " for the flow" + flowToTest);
                                }
                                continue;
                            } else {
                                if (!listColumnValue.equalsIgnoreCase("Yes")) {
                                    customAssert.assertTrue(false, "On Billing Report Value for column invoiced is " + listColumnValue + " Expected Value is Yes when invoice is generated for billing data" + " for the flow" + flowToTest);
                                }
                            }
                        }

                        if (listColumnName.equalsIgnoreCase("invoiceid")) {

                            if (invoiceId.equalsIgnoreCase("") || invoiceId.equalsIgnoreCase("-1")) {
                                if (!listColumnValue.equalsIgnoreCase("null")) {
                                    customAssert.assertTrue(false, "On Billing Report Value for column invoiced Id is " + listColumnValue + " Expected Value is " + invoiceId + "when invoice is not generated for billing data" + " for the flow" + flowToTest);
                                }
                                continue;
                            } else {
                                if (!listColumnValue.contains(invoiceId)) {
                                    customAssert.assertTrue(false, "On Billing Report Value for column invoiced Id is " + listColumnValue + " Expected Value is " + invoiceId + "when invoice is generated for billing data" + " for the flow" + flowToTest);
                                }
                            }
                        }

                        if (listColumnName.equalsIgnoreCase("invoicelineitemid")) {

                            if (lineItemId.equalsIgnoreCase("") || lineItemId.equalsIgnoreCase("-1")) {
                                if (!listColumnValue.equalsIgnoreCase("null")) {
                                    customAssert.assertTrue(false, "On Billing Report Value for column invoicelineitemid is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is not generated for billing data" + " for the flow" + flowToTest);
                                }

                            } else {
                                if (!listColumnValue.contains(lineItemId)) {
                                    customAssert.assertTrue(false, "On Billing Report Value for column invoicelineitemid Id is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is generated for billing data" + " for the flow" + flowToTest);
                                }
                            }
                            continue;
                        }

                        if (listColumnName.equalsIgnoreCase("invoicingCurrency")) {

                            String serviceDataCurrency = ShowHelper.getValueOfField("invoicingcurrency",showResponse);
                            String lineItemCurrency = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, Integer.parseInt(lineItemId), "currency");

                            String expInvoicingCurr = "";

                            if((serviceDataCurrency == null || serviceDataCurrency.equals("null")) && listColNameValue.get("invoiced").equals("Yes")){
                                expInvoicingCurr = lineItemCurrency;
                            }else {
                                expInvoicingCurr = serviceDataCurrency;
                            }
                            if((expInvoicingCurr == null && listColumnValue.equals(""))) {
//                                Do nothing this is the expected behavior
                            }else if(!listColumnValue.equals(expInvoicingCurr)){
                                customAssert.assertTrue(false,"Expected and actual value of invoicing currency mismatched on billing record Expected Invoicing currency " + expInvoicingCurr + " Actual Invoicing Currency " + listColumnValue);
                            }

                            continue innerLoop;
                        }

                        if (listColumnName.equalsIgnoreCase("lineitemdescription")) {

//                        if(lineItemId.equalsIgnoreCase("")){
//                            if(!listColumnValue.equalsIgnoreCase("")){
//                                customAssert.assertTrue(false,"On Billing Report Value for column lineitemdescription is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is not generated for billing data");
//                            }
//
//                        }else{
//                            String lineItemDescription =  ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, Integer.parseInt(lineItemId), "name");
//                            if(!listColumnValue.equalsIgnoreCase(lineItemDescription)){
//                                customAssert.assertTrue(false,"On Billing Report Value for column lineitemdescription is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is generated for billing data");
//                            }
//                        }
                            continue;
                        } else if (showPageColumnName.equals("")) {
                            logger.info("Show Page Field Value not defined for listing Column Name " + listColumnName);
                            continue;
                        } else {

                            try {

                                showPageColumnValue = ShowHelper.getValueOfField(showPageColumnName, showResponse);

                                if (listColumnName.equals("country") || listColumnName.equals("region")) {
                                    String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageColumnName, serviceDataEntityTypeId);
                                    List<String> fieldValuesList = ShowHelper.getAllSelectValuesOfField(showResponse, showPageColumnName, fieldHierarchy, serviceDataId, serviceDataEntityTypeId);
                                    listColumnValue = listColumnValue.toLowerCase();
                                    for (int i = 0; i < fieldValuesList.size(); i++) {

                                        if (!listColumnValue.contains(fieldValuesList.get(i).toLowerCase())) {
                                            customAssert.assertTrue(false, "For Billing Record listing column Name " + listColumnName + " listing page does not contain the value " + fieldValuesList.get(i) + " for the flow" + flowToTest);
                                        }
                                    }

                                } else if (listColumnName.toLowerCase().equals("service data mrole_group")) {
                                    String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageColumnName, serviceDataEntityTypeId);
                                    List<String> fieldValuesList = ShowHelper.getAllSelectedStakeholdersFromShowResponse(showResponse);
                                    listColumnValue = listColumnValue.toLowerCase();
                                    for (int i = 0; i < fieldValuesList.size(); i++) {

                                        if (!listColumnValue.contains(fieldValuesList.get(i).toLowerCase())) {
                                            customAssert.assertTrue(false, "For Billing Record listing column Name " + listColumnName + " listing page does not contain the value " + fieldValuesList.get(i) + " for the flow" + flowToTest);
                                        }
                                    }
                                } else if (listColumnName.equals("servicedataname")) {
                                    String showPageValue = ShowHelper.getValueOfField("name", showResponse) + " (" + ShowHelper.getValueOfField("serviceclient", showResponse) + ")";

                                    if (!listColumnValue.equals(showPageValue)) {
                                        customAssert.assertTrue(false, "For Billing Record Listing and Show Page value validated unsuccessfully for listing column " + listColumnName + " for the flow" + flowToTest);
                                    }
                                } else {
                                    if (listColumnValue.contains(":;")) {
                                        listColumnValue = listColumnValue.split(":;")[0];
                                    }
                                    if (listColumnValue.equals("") && showPageColumnValue == null) {
                                        continue;
                                    }
                                    if (!listColumnValue.equals(showPageColumnValue)) {
                                        customAssert.assertTrue(false, "For Billing Record Listing and Show Page value validated unsuccessfully for listing column " + listColumnName + " for the flow" + flowToTest);
                                    }
                                }

                            } catch (Exception e) {
                                customAssert.assertTrue(false, "For Billing Record Exception while validating Listing and Show Page value for listing column " + listColumnName + " for the flow" + flowToTest);
                            }
                        }
                    }

                    if (parentHeirarchyToBeChecked) {
                        show.hitShowVersion2(serviceDataEntityTypeId, parentId);
                        String showResponseParent = show.getShowJsonStr();

                        String showPageValue = ShowHelper.getValueOfField("name", showResponseParent) + " (" + ShowHelper.getValueOfField("serviceclient", showResponseParent) + ")";

                        if (!parentName.equals(showPageValue)) {
                            customAssert.assertTrue(false, "For Billing Record Listing and Show Page value validated unsuccessfully for listing column parent name" + " for the flow" + flowToTest);
                        }
                    }
//                    break;
                }

            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Billing Data Listing Page Data" + " for the flow " + flowToTest);
            validationBillingDataListingPage = false;
        }

        return validationBillingDataListingPage;

    }

    public String getBaseAmount(int serviceDataId,CustomAssert customAssert){

        TabListData tabListData = new TabListData();
        String baseAmount = "";
        int tabId = 309;
        try{
            logger.info("Getting base amount for service data");

            tabListData.hitTabListDataV2(tabId,serviceDataEntityTypeId,serviceDataId);
            String tabListResponse =tabListData.getTabListDataResponseStr();

            if(!JSONUtility.validjson(tabListResponse)){
                logger.error("Charges tab Response is an invalid json for service data " + serviceDataId);
                customAssert.assertTrue(false,"Charges tab Response is an invalid json for service data " + serviceDataId);
            }else {

                JSONObject tabListResponseJson = new JSONObject(tabListResponse);

                //Base amount column id is 12570
                baseAmount = tabListResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject("12570").get("value").toString();

            }
        }catch (Exception e){
            logger.error("Exception while getting base amount for service data");
            customAssert.assertTrue(false,"Exception while getting base amount for service data");
        }
        return baseAmount;
    }

    private BigDecimal calcSysAmountARC(int serviceDataId, int consumptionId, CustomAssert customAssert) {

        BigDecimal systemAmt = new BigDecimal(0.0);

        try {

            TabListData tabListData = new TabListData();
            String chargesTabResponse = tabListData.hitTabListDataV2(chargesTabId, serviceDataEntityTypeId, serviceDataId);

            ListRendererListData listRendererListData = new ListRendererListData();

            Map<String, String> chargesColMap = listRendererListData.getListColumnNameValueMap(chargesTabResponse);

            BigDecimal baseVolume = new BigDecimal(chargesColMap.get("volume"));
            BigDecimal rate = new BigDecimal(chargesColMap.get("unitrate"));

            String finalConsumption = ShowHelper.getValueOfField(consumptionsEntityTypeId, consumptionId, "finalconsumption");

            BigDecimal finalConsumptionBigDec;
            if (finalConsumption == null) {
                customAssert.assertTrue(false, "Final consumption is null while validating values for ARC ");
                return systemAmt;
            } else {
                finalConsumptionBigDec = new BigDecimal(finalConsumption);
            }

            String arcTabResponse = tabListData.hitTabListDataV2(ARCRRCTabId, serviceDataEntityTypeId, serviceDataId);

            TreeMap<BigDecimal, HashMap<String, String>> arcMap = new TreeMap<>();
            HashMap<String, String> columnValueMap;

            BigDecimal lowerLevel = new BigDecimal(0.0);
            BigDecimal upperLevel = new BigDecimal(0.0);
            if (JSONUtility.validjson(arcTabResponse)) {

                JSONObject arcTabRespJson = new JSONObject(arcTabResponse);

                JSONArray dataArray = arcTabRespJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {

                    columnValueMap = new HashMap<>();

                    JSONObject indvJson = dataArray.getJSONObject(i);
                    String[] columnIds = indvJson.getNames(indvJson);

                    for (String columnId : columnIds) {

                        String columnName = indvJson.getJSONObject(columnId).get("columnName").toString();
                        String columnValue = indvJson.getJSONObject(columnId).get("value").toString();

                        if (columnName.equals("lowerlevel")) {
                            try {
                                lowerLevel = new BigDecimal(columnValue);

                                lowerLevel = (baseVolume.multiply(lowerLevel).divide(new BigDecimal(100)));
                                columnValue = lowerLevel.toString();

                            } catch (Exception e) {
                                customAssert.assertTrue(false, "Exception while parsing double value for lower level");
                            }
                        }else if (columnName.equals("upperlevel")) {
                            try {
                                if(columnValue == "null"){

                                    columnValue = String.valueOf(Integer.MAX_VALUE);

                                }else {
                                    upperLevel = new BigDecimal(columnValue);

                                    upperLevel = (baseVolume.multiply(upperLevel).divide(new BigDecimal(100)));
                                    columnValue = upperLevel.toString();
                                }

                            } catch (Exception e) {
                                customAssert.assertTrue(false, "Exception while parsing double value for upper level");
                            }
                        }
                        columnValueMap.put(columnName, columnValue);

                    }
                    arcMap.put(lowerLevel, columnValueMap);
                }
            }
            HashMap<String, String> values;
            //1 means Greater than base volume || 0 means equal || -1 means less than
            if (finalConsumptionBigDec.compareTo(baseVolume) == 1) {

                BigDecimal remainingAmount = finalConsumptionBigDec;

                systemAmt = baseVolume.multiply(rate);

                remainingAmount = finalConsumptionBigDec.subtract(baseVolume);

                for (Map.Entry<BigDecimal, HashMap<String, String>> entry : arcMap.entrySet()) {

                    if (remainingAmount.equals(new BigDecimal(0.0))) {
                        break;
                    }
//                    BigDecimal band = entry.getKey();

                    values = entry.getValue();

                    String lowerlevel = values.get("lowerlevel");
                    String upperlevel = values.get("upperlevel");

                    if (upperlevel == null) {

                    } else {
                        BigDecimal interval = new BigDecimal(upperlevel).subtract(new BigDecimal(lowerlevel));

                        BigDecimal newRemainingAmount = remainingAmount.subtract(interval);

                        if (newRemainingAmount.equals(0)) {
                            systemAmt = systemAmt.add(new BigDecimal(values.get("rate")).multiply(newRemainingAmount));
                            remainingAmount = new BigDecimal(0.0);

                        } else if (newRemainingAmount.compareTo(new BigDecimal(0)) == 1) {

                            systemAmt = systemAmt.add(new BigDecimal(values.get("rate")).multiply(interval));
                            remainingAmount = newRemainingAmount;

                        }else if (newRemainingAmount.compareTo(new BigDecimal(0)) == -1) {

                            systemAmt = systemAmt.add(new BigDecimal(values.get("rate")).multiply(remainingAmount));
                            remainingAmount = new BigDecimal(0.0);

                        }
                    }
                }
            } else if (finalConsumptionBigDec.compareTo(baseVolume) == 0) {

                systemAmt = systemAmt.add(rate.multiply(baseVolume));

            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while calculating system amount ARC");
        }
        return systemAmt;
    }

    public boolean validateBillingDataListingPageData(int serviceDataId,
                                                      int consumptionId,
                                                      String invoiceId,
                                                      String lineItemId,
                                                      Map<String, String> listColNameValue,
                                                      Boolean parentHeirarchyToBeChecked, String flowToTest,
                                                      CustomAssert customAssert){

        Boolean validationBillingDataListingPage = true;
        try{

            String billingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFilePath");
            String billingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFileName");

            logger.info("Validating Billing Data On Listing Page");
            Show show = new Show();
            show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
            String showResponse = show.getShowJsonStr();

            String listColumnName;
            String listColumnValue;
            String showPageColumnName;
            String showPageColumnValue;

            int parentId;
            String parentName;

            for (Map.Entry<String,String> entry1 : listColNameValue.entrySet()) {

                listColumnName = entry1.getKey();
                listColumnValue = entry1.getValue();

                if (parentHeirarchyToBeChecked && listColumnName.equals("parentid")) {
                    listColumnValue = entry1.getValue();

                    if (listColumnValue == null) {
                        customAssert.assertTrue(false, "Parent Service Data Id not found in child service data " + flowToTest);
                    } else {
                        try {
                            parentId = Integer.parseInt(listColumnValue.split(":;")[1]);
                        } catch (Exception e) {
                            customAssert.assertTrue(false, "Exception while getting parent service data id from child service data in Billing Data Report" + flowToTest);
                        }
                    }
                }

                if (parentHeirarchyToBeChecked && listColumnName.equals("parentname")) {
                    listColumnValue = entry1.getValue();

                    if (listColumnValue == null) {
                        customAssert.assertTrue(false, "Parent Service Data Name not found in child service data in billing Report" + flowToTest);
                    } else {
                        try {
                            parentName = listColumnValue;
                        } catch (Exception e) {
                            customAssert.assertTrue(false, "Exception while getting parent service data name from child service data in Billing Data Report" + flowToTest);
                        }
                    }
                }
                if (listColumnName.equalsIgnoreCase("amountInServiceDataCurrency")) {

//                    String expBaseAmount = getBaseAmount(serviceDataId, customAssert);
//                    if (consumptionId != -1) {
//                        expBaseAmount = calcSysAmountARC(serviceDataId, consumptionId, customAssert).toString();
//                    }
//                    if (!expBaseAmount.contains(listColumnValue)) {
//                        customAssert.assertTrue(false, "On Billing Report Expected Value for column amountInServiceDataCurrency didn't matched with Actual ");
//                    }
                    continue;
                }

                showPageColumnName = ParseConfigFile.getValueFromConfigFile(billingConfigFilePath, billingConfigFileName, "show page mapping listing page", listColumnName.toLowerCase());

                if (showPageColumnName == null) {
                    logger.info("Show Page Field not defined for listing Column Name " + listColumnName);
                    continue;
                }

                if (listColumnName.equalsIgnoreCase("invoiced")) {

                    if (invoiceId.equalsIgnoreCase("") || invoiceId.equalsIgnoreCase("-1")) {
                        if (!listColumnValue.equalsIgnoreCase("No")) {
                            customAssert.assertTrue(false, "On Billing Report Value for column invoiced is " + listColumnValue + " Expected Value is No when invoice is not generated for billing data");
                        }
                        continue;
                    } else {
                        if (!listColumnValue.equalsIgnoreCase("Yes")) {
                            customAssert.assertTrue(false, "On Billing Report Value for column invoiced is " + listColumnValue + " Expected Value is Yes when invoice is generated for billing data for the flow" + flowToTest);
                        }
                    }
                }

                if (listColumnName.equalsIgnoreCase("invoiceid")) {

                    if (invoiceId.equalsIgnoreCase("")) {
                        if (!listColumnValue.equalsIgnoreCase("null")) {
                            customAssert.assertTrue(false, "On Billing Report Value for column invoiced Id is " + listColumnValue + " Expected Value is " + invoiceId + "when invoice is not generated for billing data for the flow" + flowToTest);
                        }
                        continue;
                    } else {
                        if (!listColumnValue.contains(invoiceId)) {
                            customAssert.assertTrue(false, "On Billing Report Value for column invoiced Id is " + listColumnValue + " Expected Value is " + invoiceId + "when invoice is generated for billing data for the flow" + flowToTest);
                        }
                    }
                }

                if (listColumnName.equalsIgnoreCase("invoicelineitemid")) {

                    if (lineItemId.equalsIgnoreCase("") || lineItemId.equalsIgnoreCase("-1")) {
                        if (!listColumnValue.equalsIgnoreCase("null")) {
                            customAssert.assertTrue(false, "On Billing Report Value for column invoicelineitemid is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is not generated for billing data for the flow" + flowToTest);
                        }

                    } else {
                        if (!listColumnValue.contains(lineItemId)) {
                            customAssert.assertTrue(false, "On Billing Report Value for column invoicelineitemid Id is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is generated for billing data for the flow" + flowToTest);
                        }
                    }
                    continue;
                }

                if (listColumnName.equalsIgnoreCase("lineitemdescription")) {
//
//                    if (lineItemId.equalsIgnoreCase("")) {
//                        if (!listColumnValue.equalsIgnoreCase("")) {
//                            customAssert.assertTrue(false, "On Billing Report Value for column lineitemdescription is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is not generated for billing data");
//                        }
//
//                    } else {
//                        String lineItemDescription = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, Integer.parseInt(lineItemId), "name");
//                        if (!listColumnValue.equalsIgnoreCase(lineItemDescription)) {
//                            customAssert.assertTrue(false, "On Billing Report Value for column lineitemdescription is " + listColumnValue + " Expected Value is " + lineItemId + "when lineItem is generated for billing data");
//                        }
//                    }
                    continue;
                }
                else if (showPageColumnName.equals("")) {
                    logger.info("Show Page Field Value not defined for listing Column Name " + listColumnName);
                    continue;
                } else {

                    try {

                        showPageColumnValue = ShowHelper.getValueOfField(showPageColumnName, showResponse);

                        if (listColumnName.equals("country") || listColumnName.equals("region")) {
                            String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageColumnName, serviceDataEntityTypeId);
                            List<String> fieldValuesList = ShowHelper.getAllSelectValuesOfField(showResponse, showPageColumnName, fieldHierarchy, serviceDataId, serviceDataEntityTypeId);
                            listColumnValue = listColumnValue.toLowerCase();
                            for (int i = 0; i < fieldValuesList.size(); i++) {

                                if (!listColumnValue.contains(fieldValuesList.get(i).toLowerCase())) {
                                    customAssert.assertTrue(false, "For Billing Record listing column Name " + listColumnName + " listing page does not contain the value " + fieldValuesList.get(i) + " for the flow" + flowToTest);
                                }
                            }

                        } else if (listColumnName.toLowerCase().equals("service data mrole_group")) {
                            String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageColumnName, serviceDataEntityTypeId);
                            List<String> fieldValuesList = ShowHelper.getAllSelectedStakeholdersFromShowResponse(showResponse);
                            listColumnValue = listColumnValue.toLowerCase();
                            for (int i = 0; i < fieldValuesList.size(); i++) {

                                if (!listColumnValue.contains(fieldValuesList.get(i).toLowerCase())) {
                                    customAssert.assertTrue(false, "For Billing Record listing column Name " + listColumnName + " listing page does not contain the value " + fieldValuesList.get(i) + " for the flow" + flowToTest);
                                }
                            }
                        } else if (listColumnName.equals("servicedataname")) {
                            String showPageValue = ShowHelper.getValueOfField("name", showResponse) + " (" + ShowHelper.getValueOfField("serviceclient", showResponse) + ")";

                            if (!listColumnValue.equals(showPageValue)) {
                                customAssert.assertTrue(false, "For Billing Record Listing and Show Page value validated unsuccessfully for listing column " + listColumnName + " for the flow" + flowToTest);
                            }
                        } else {
                            if (listColumnValue.contains(":;")) {
                                listColumnValue = listColumnValue.split(":;")[0];
                            }
                            if (!listColumnValue.equals(showPageColumnValue)) {
                                customAssert.assertTrue(false, "For Billing Record Listing and Show Page value validated unsuccessfully for listing column " + listColumnName+ " for the flow" + flowToTest);
                            }
                        }

                    } catch (Exception e) {
                        customAssert.assertTrue(false, "For Billing Record Exception while validating Listing and Show Page value for listing column " + listColumnName+ " for the flow" + flowToTest);
                    }
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Billing Data Listing Page Data "+ "for the flow" + flowToTest);
            validationBillingDataListingPage = false;
        }

        return validationBillingDataListingPage;

    }

    public boolean updateSDisBill_PriAvail_ConsAvail(int serviceDataId,
                                                     Boolean isBillable,Boolean pricingAvailable,
                                                     Boolean consAvail,CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;
        String serviceData = "service data";

        try{

            editResponse = edit.hitEdit(serviceData,serviceDataId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("billingAvailable").put("values",isBillable);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("pricingAvailable").put("values",pricingAvailable);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("consumptionAvailable").put("values",consAvail);

            editResponse = edit.hitEdit(serviceData,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating service data billingAvailable pricingAvailable consumptionAvailable flags");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating service start date end date " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating service start date end date " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;
    }

    public boolean updateServiceStartAndEndDate(int invoiceId, String invoiceDate, CustomAssert customAssert){

        Boolean updateInvoiceStartAndEndDate = true;

        Edit edit = new Edit();
        String editResponse;
        String invoice = "invoices";

        try{

            editResponse = edit.hitEdit(invoice,invoiceId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceDate").put("values",invoiceDate);
            editResponse = edit.hitEdit(invoice,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Error while updating invoice date ");
                updateInvoiceStartAndEndDate = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating invoice start date end date " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating invoice date " + e.getStackTrace());
            updateInvoiceStartAndEndDate = false;
        }

        return updateInvoiceStartAndEndDate;
    }

    public static int getInvoiceLineItemId(String invoiceLineItemConfigFilePath, String invoiceLineItemConfigFileName, String invoiceLineItemExtraFieldsConfigFileName, String invoiceLineItemSectionName,
                                           String startDate,String endDate,String invoiceDate,int serviceDataId) {

        int lineItemId = -1;
        try {
            String serviceDataEntity = "service data";
            String invoiceLineItemEntity = "invoice line item";

            logger.info("Updating Invoice Line Item Property Service Id Supplier in Extra Fields Config File for Flow [{}] and Service Data Id {}.",
                    invoiceLineItemSectionName, serviceDataId);
            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName(serviceDataEntity);
            String serviceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "title");
            String serviceIdSupplierName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "serviceIdSupplier");
            String serviceIdSupplierUpdatedName = serviceDataName + " (" + serviceIdSupplierName + ")";
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new name", serviceIdSupplierUpdatedName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new id", String.valueOf(serviceDataId));


            String createResponse = InvoiceLineItem.createInvoiceLineItem(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemConfigFilePath,
                    invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,startDate,endDate,invoiceDate);
            lineItemId = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

            //Reverting Invoice Line Item Extra Fields changes.
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", serviceIdSupplierUpdatedName, "new name");
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", String.valueOf(serviceDataId), "new id");

        } catch (Exception e) {
            logger.error("Exception while getting Invoice Line Item Id using Flow Section [{}]. {}", invoiceLineItemSectionName, e.getStackTrace());
        }
        return lineItemId;
    }

    public HashMap<String,Map<String,String>> getBillingRecordAccToStartDate(int serviceDataId,String payload,
                                                                             CustomAssert customAssert){

        HashMap<String,Map<String,String>> billingRecordsAccToStartDate = new HashMap<>();
        int billingReportId = 444;

        try{

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(billingReportId,payload);
            String listDataResponse = reportRendererListData.getListDataJsonStr();

            if(APIUtils.validJsonResponse(listDataResponse)){

                JSONObject listDataResponseJson = new JSONObject(listDataResponse);
                int length = listDataResponseJson.getJSONArray("data").length();

                int index = -1;

                String columnName = "null";
                String columnValue;
                String startDate = "";
                HashMap<String,String> columnNameValues;
                int i =1;
                while (++index < length) {
                    columnNameValues = new HashMap<>();
                    JSONArray arrayTemp = listDataResponseJson.getJSONArray("data").getJSONObject(index).names();
                    for (int subIndex = 0; subIndex < arrayTemp.length(); subIndex++) {

                        try {
                            columnName = listDataResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subIndex)).getString("columnName");
                            columnValue = listDataResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(arrayTemp.getString(subIndex)).getString("value");

                            if(columnName.equals("startdate")){
                                startDate = columnValue;
                            }else {
                                columnNameValues.put(columnName,columnValue);
                            }

                        } catch (Exception e) {
                            logger.error("Exception caught in parsing json {}", (Object) e.getStackTrace());
                            columnNameValues.put(columnName,"null");
                        }

                    }
                    if(billingRecordsAccToStartDate.containsKey(startDate)) {
                        billingRecordsAccToStartDate.put(startDate + "_" + i, columnNameValues);
                        i++;
                    }else {
                        billingRecordsAccToStartDate.put(startDate, columnNameValues);
                    }
                }

            }else{
                logger.error("Billing Report has invalid json Response Either Billing data not generated for service data id " + serviceDataId + " or there is bug");
                customAssert.assertTrue(false,"Billing Report has invalid json Response Either Billing data not generated for service data id " + serviceDataId + " or there is bug");
            }

        }catch (Exception e){
            logger.error("Exception while getting billing Data Ids");
            customAssert.assertTrue(false,"Exception while getting billing Data Ids");
        }

        return billingRecordsAccToStartDate;
    }

    //The key of the map expectedAmountValuesMap is such that
    // it should match audit log values in flows Config File Path
    public Boolean validateAmountValues(String flowToTest, int invoiceLineItemId, HashMap<String, String> expectedAmountValuesMap, CustomAssert csAssert) {

        Boolean validationStatus = true;
        String showResponse;

        try {
            Show show = new Show();

            show.hitShow(invoiceLineItemEntityTypeId, invoiceLineItemId);
            showResponse = show.getShowJsonStr();

            String supplierRateActual = ShowHelper.getValueOfField("rate", showResponse);
            if (!supplierRateActual.equals(expectedAmountValuesMap.get("Supplier Rate"))) {
                csAssert.assertEquals(supplierRateActual,expectedAmountValuesMap.get("Supplier Rate"), "Expected and Actual Value of supplierRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemRateActual = ShowHelper.getValueOfField("systemrate", showResponse);
            if (!systemRateActual.contains(expectedAmountValuesMap.get("System Rate"))) {
                csAssert.assertEquals(systemRateActual,expectedAmountValuesMap.get("System Rate"), "Expected and Actual Value of systemRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String discrepancyRateActual = ShowHelper.getValueOfField("discrepancyrate", showResponse);
            if (!discrepancyRateActual.contains(expectedAmountValuesMap.get("Discrepancy Rate"))) {
                csAssert.assertEquals(discrepancyRateActual,expectedAmountValuesMap.get("Discrepancy Rate"), "Expected and Actual Value of discrepancyRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String supplierConversionRateActual = ShowHelper.getValueOfField("conversionrate", showResponse);
            if (!supplierConversionRateActual.equals(expectedAmountValuesMap.get("Supplier conversionRate"))) {
                csAssert.assertEquals(supplierConversionRateActual,expectedAmountValuesMap.get("Supplier conversionRate"), "Expected and Actual Value of supplierConversionRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemConversionRateActual = ShowHelper.getValueOfField("systemconversionrate", showResponse);
            if (!systemConversionRateActual.equals(expectedAmountValuesMap.get("System Conversion Rate"))) {
                csAssert.assertEquals(systemConversionRateActual,expectedAmountValuesMap.get("System Conversion Rate"), "Expected and Actual Value of systemConversionRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }
            String discrepancyConversionRateActual = ShowHelper.getValueOfField("discrepancyconversionrate", showResponse);
            if (!discrepancyConversionRateActual.equals(expectedAmountValuesMap.get("Discrepancy Conversion Rate"))) {
                csAssert.assertEquals(discrepancyConversionRateActual,expectedAmountValuesMap.get("Discrepancy Conversion Rate"), "Expected and Actual Value of discrepancyConversionRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String supplierQuantityActual = ShowHelper.getValueOfField("quantity", showResponse);
            if (!supplierQuantityActual.equals(expectedAmountValuesMap.get("Supplier Quantity"))) {
                csAssert.assertEquals(supplierQuantityActual,expectedAmountValuesMap.get("Supplier Quantity"), "Expected and Actual Value of supplierQuantity doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }
            String systemQuantityActual = ShowHelper.getValueOfField("systemquantity", showResponse);
            if (!systemQuantityActual.equals(expectedAmountValuesMap.get("System Quantity"))) {
                csAssert.assertEquals(systemQuantityActual,expectedAmountValuesMap.get("System Quantity"), "Expected and Actual Value of systemQuantity doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String discrepancyQuantityActual = ShowHelper.getValueOfField("discrepancyquantity", showResponse);
            if (!discrepancyQuantityActual.equals(expectedAmountValuesMap.get("Discrepancy Quantity"))) {
                csAssert.assertEquals(discrepancyQuantityActual,expectedAmountValuesMap.get("Discrepancy Quantity"), "Expected and Actual Value of discrepancyQuantity doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String amountActual = ShowHelper.getValueOfField("amount", showResponse);
            if (!amountActual.equals(expectedAmountValuesMap.get("Supplier Amount"))) {
                csAssert.assertEquals(amountActual,expectedAmountValuesMap.get("Supplier Amount"), "Expected and Actual Value of amount doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemAmountActual = ShowHelper.getValueOfField("systemamount", showResponse);
            if (!systemAmountActual.equals(expectedAmountValuesMap.get("System Amount"))) {
                csAssert.assertEquals(systemAmountActual,expectedAmountValuesMap.get("System Amount"), "Expected and Actual Value of systemAmount doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }
            String discrepancyAmountActual = ShowHelper.getValueOfField("discrepancyamount", showResponse);
            if (!discrepancyAmountActual.equals(expectedAmountValuesMap.get("Discrepancy Amount"))) {
                csAssert.assertEquals(discrepancyAmountActual,expectedAmountValuesMap.get("Discrepancy Amount"), "Expected and Actual Value of discrepancyAmount doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String totalActual = ShowHelper.getValueOfField("total", showResponse);
            if (!totalActual.equals(expectedAmountValuesMap.get("Supplier Total"))) {
                csAssert.assertEquals(totalActual,expectedAmountValuesMap.get("Supplier Total"), "Expected and Actual Value of totalActual doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemTotalActual = ShowHelper.getValueOfField("systemtotal", showResponse);
            if (!systemTotalActual.equals(expectedAmountValuesMap.get("System Total"))) {
                csAssert.assertEquals(systemTotalActual,expectedAmountValuesMap.get("System Total"), "Expected and Actual Value of systemTotal doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String discrepancyTotalActual = ShowHelper.getValueOfField("discrepancytotal", showResponse);
            if (!discrepancyTotalActual.equals(expectedAmountValuesMap.get("Discrepancy Total"))) {
                csAssert.assertEquals(discrepancyTotalActual,expectedAmountValuesMap.get("Discrepancy Total"), "Expected and Actual Value of discrepancyTotal doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Amount values on invoice line item " + invoiceLineItemId + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;

    }

    public void updateAdditionalValues(String entityName,int recordId,BigDecimal additionalValue,CustomAssert customAssert){
        Edit edit = new Edit();

        try{
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            Show show = new Show();
            show.hitShowVersion2(entityTypeId,recordId);
            String showResponse = show.getShowJsonStr();

            String additionalAcv = ShowHelper.getValueOfField("additionalacv",showResponse);
            String additionalTcv = ShowHelper.getValueOfField("additionaltcv",showResponse);
            String additionalFacv = ShowHelper.getValueOfField("additionalfacv",showResponse);

            BigDecimal additionalAcvD;
            BigDecimal additionalTcvD;
            BigDecimal additionalFacvD;

            try{
                additionalAcvD = (new BigDecimal(additionalAcv).add(additionalValue).setScale(2, RoundingMode.HALF_UP));
            }catch (Exception e){
                additionalAcvD = additionalValue;
            }
            try{
                additionalTcvD = (new BigDecimal(additionalTcv).add(additionalValue).setScale(2, RoundingMode.HALF_UP));
            }catch (Exception e){
                additionalTcvD = additionalValue;
            }
            try{
                additionalFacvD = (new BigDecimal(additionalFacv).add(additionalValue).setScale(2, RoundingMode.HALF_UP));
            }catch (Exception e){
                additionalFacvD = additionalValue;
            }

            String editPayload = edit.getEditPayload(entityName, recordId);

            JSONObject editPayloadJson = new JSONObject(editPayload);

            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("additionalTCV").put("values", additionalTcvD);
            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("additionalACV").put("values", additionalAcvD);
            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("additionalFYCV").put("values", additionalFacvD);

            String editResponse = edit.hitEdit(entityName, editPayloadJson.toString());

            if (!editResponse.contains("success")) {
                customAssert.assertEquals("Additional values updated unsuccessfully on " + entityName + " for record Id " + recordId,"Additional values should update successfully on " + entityName + " for record Id " + recordId);
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while updating additional values on " + entityName + " for record Id " + recordId);
        }

    }
}
