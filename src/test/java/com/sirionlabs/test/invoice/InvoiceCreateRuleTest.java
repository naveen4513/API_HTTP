package com.sirionlabs.test.invoice;

import com.sirionlabs.api.clientAdmin.customField.CustomField;
import com.sirionlabs.api.clientAdmin.invoiceCreateRule.InvoiceCreateRule;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Invoice;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.velocity.runtime.directive.Parse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//C88376,C76644,C76646
public class InvoiceCreateRuleTest extends TestAPIBase {

    private static Logger logger = LoggerFactory.getLogger(InvoiceCreateRuleTest.class);
    private Map<Integer, String> deleteEntityMap = new HashMap<>();
    private String configFilePath;
    private String configFileName;
    private String flowsConfigFilePath;
    private String flowsConfigFileName;
    private boolean killAllSchedulerTasks = false;
    private String pricingTemplateFilePath;
    private boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;
    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;
    private String invoiceLineItemConfigFilePath;
    private String invoiceLineItemConfigFileName;
    private String invoiceLineItemExtraFieldsConfigFileName;
    private int serviceDataEntityTypeId = -1;
    private String serviceDataEntitySectionUrlName;

    //	private Show show;
    private String approveAction = "approve";
    //	private Edit edit;
    private String contractEntity = "contracts";
    private String serviceDataEntity = "service data";
    private String invoiceEntity = "invoices";
    private String invoiceLineItemEntity = "invoice line item";
    private String customFieldDYN = "";
    private boolean foundResultInShow = false;
    private boolean staticFieldFound = false;
    private JSONObject staticContent;
    private String flowToTest = "fixed fee flow for create rule";
    private List<String> rulesToBeDeactivated = new ArrayList<>();

    @AfterMethod
    public void afterMethodForDelete() {
        //callSetRuleInactive(new CustomAssert());
        for (Map.Entry<Integer, String> me : deleteEntityMap.entrySet()) {
            deleteNewEntity(me.getValue(), me.getKey());
        }
       //new InvoiceCreateRule().deactivateRules(rulesToBeDeactivated);
    }

    @BeforeClass
    public void beforeClass(ITestContext context) {
        String env = context.getCurrentXmlTest().getParameter("Environment");
        if(env.equalsIgnoreCase("Sandbox/US")||env.equalsIgnoreCase("vpcEnv"))
            flowToTest = "fixed fee flow for sanity";
        logger.info("Flow to test got is : {}",flowToTest);
    }

    @BeforeClass
    public void BeforeClass() {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceCreateRuleConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceCreateRuleConfigFileName");
        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");
        flowsConfigFilePath = configFilePath;
        flowsConfigFileName = configFileName;

        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        //Invoice Config files
        invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
        invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
        invoiceExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");

        //Invoice Line Item Config files
        invoiceLineItemConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
        invoiceLineItemConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
        invoiceLineItemExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("invoiceLineItemExtraFieldsFileName");

        String entityIdMappingFileName;
        String baseFilePath;

        // for publishing of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));

        testCasesMap = getTestCasesMapping();

        deleteEntityMap = new HashMap<>();

    }

    @DataProvider(parallel = false)
    public Object[][] dataProviderForCheckValidationInvoiceCreationRule() {
        logger.info("Setting all Invoice Create Cases to Run");
        List<Object[]> allTestData = new ArrayList<>();
        Vector formulasToTest = getFormulasToTest();

        try {
            List<List<String>> formula = (List<List<String>>) formulasToTest.get(0);
            List<List<String>> fieldsMap = (List<List<String>>) formulasToTest.get(1);
            List<String> results = (List<String>) formulasToTest.get(2);
            List<List<List<String>>> conditions = (List<List<List<String>>>) formulasToTest.get(3);

            for (int count = 0; count < results.size(); count++) {
                allTestData.add(new Object[]{formula.get(count), fieldsMap.get(count), results.get(count), conditions.get(count)});
            }
        } catch (Exception e) {
            logger.info("Exception caught in data provider {}", e.getStackTrace().toString());
        }
        return allTestData.toArray(new Object[0][]);
    }

    @SuppressWarnings("unchecked")
    private Vector getFormulasToTest() {
        try {
            //String[] cases = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "properties", "run").split(",");
            List<String> casesToTestS = Arrays.asList(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "properties", "scasestorun").split(","));
            List<String> casesToTestF = Arrays.asList(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "properties", "fcasestorun").split(","));
            String testAllCases = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "properties", "testallcases");
            List<String> cases ;

            String successPropertyName = "success cases";
            String failPropertyName = "fail cases";

            Vector vector = new Vector();
            List<List<String>> list = new ArrayList<>();
            List<List<String>> customFields = new ArrayList<>();
            List<String> results = new ArrayList<>();
            List<List<List<String>>> conditions = new ArrayList<>();

            cases = ParseConfigFile.getAllPropertiesOfSection(configFilePath,configFileName,successPropertyName); //get success test cases
            getValueFromConfigurationFile(casesToTestS,testAllCases,cases,"success",list,customFields,conditions,results);

            cases = ParseConfigFile.getAllPropertiesOfSection(configFilePath,configFileName,failPropertyName); //get fail test cases
            getValueFromConfigurationFile(casesToTestF,testAllCases,cases,"failure",list,customFields,conditions,results);

            vector.add(list);
            vector.add(customFields);
            vector.add(results);
            vector.add(conditions);
            return vector;
        } catch (Exception e) {
            logger.info("Exception Caught in retrieving values from config files hence cannot proceed");
            return null;
        }
    }

    private void getValueFromConfigurationFile(List<String> casesToTest,String testAllCases,List<String> cases,String result,List<List<String>> list,List<List<String>> customFields,List<List<List<String>>> conditions, List<String> results) throws ConfigurationException {

        List<String> list2 ;

        String[] formula, temp;

        for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
            list2 = new ArrayList<>();
            String index = String.valueOf(caseIndex);
            if (!casesToTest.contains(index) && testAllCases.equalsIgnoreCase("false"))
                continue;

            results.add(result);

            List<String> insideCondition = new ArrayList<>();
            List<List<String>> condition = new ArrayList<>();
            if(cases.get(caseIndex).split(";").length>1){
                String tempString = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conditions",cases.get(caseIndex).split(";")[1].split("-")[0]).replace('~','=');
                condition.add(Collections.singletonList(
                        tempString
                ));
                insideCondition.addAll(Arrays.asList(cases.get(caseIndex).split(";")[1].split("-")).subList(2, cases.get(caseIndex).split(";")[1].split("-").length));

                condition.add(insideCondition);
                condition.add(Collections.singletonList(
                        cases.get(caseIndex).split(";")[1].split("-")[1]
                ));
            }else{
                condition.add(new ArrayList<>());
                condition.add(insideCondition);
                condition.add(new ArrayList<>());
            }

            temp = cases.get(caseIndex).split(";")[0].split("&");
            formula = new String[temp.length];
            int length;
            for (int iTemp = 0; iTemp < temp.length; iTemp++) {
                length = temp[iTemp].split("-").length;
                formula[iTemp] = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "formula", temp[iTemp].split("-")[0]);
                list2.addAll(Arrays.asList(temp[iTemp].split("-")).subList(1, length));
            }
            customFields.add(list2);

            list.add(Arrays.asList(formula));
            conditions.add(condition);
        }
    }

    @Test(enabled = false, dataProvider = "dataProviderForCheckValidationInvoiceCreationRule") //C88376,C76646,C76644
    public void checkValidationInvoiceCreationRule(String fieldName1, String invoiceCustomFieldCreatePayloadSectionName1, String fieldName2, String invoiceCustomFieldCreatePayloadSectionName2, String result, String formula, String ruleCreatePayload) {

        CustomAssert customAssert = new CustomAssert();
        try {
            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());

            String customFieldName1 = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field name mapping", fieldName1);
            String customFieldName2 = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field name mapping", fieldName2);

            boolean checkField1Creation = checkCreateCustomField(customFieldName1, invoiceCustomFieldCreatePayloadSectionName1, customAssert);
            boolean checkField2Creation = checkCreateCustomField(customFieldName2, invoiceCustomFieldCreatePayloadSectionName2, customAssert);

            if (!checkField1Creation || !checkField2Creation) {
                logger.info("Custom Field not created successfully after checkCreateCustomField() method");
                customAssert.assertTrue(false, "Custom Field not created successfully after checkCreateCustomField() method");
            }

            InvoiceCreateRule invoiceCreateRule = new InvoiceCreateRule();
            String ruleName = "newRuleAutomation" + RandomNumbers.getRandomNumberWithinRangeIndex(10000, 99999);
            String rule = "${" + customFieldName1 + "}@{" + formula + customFieldName2 + "}";

            logger.info("Setting invoice create rule payload");
            invoiceCreateRule.setCreatePayload(ruleName, rule, ruleCreatePayload, customAssert);

            logger.info("creating invoice rule");
            invoiceCreateRule.createRule(customAssert); //creating invoice rule
            String createResponse = invoiceCreateRule.getCreateResponse();

            if (createResponse.contains("Created Successfully")) {
                logger.info("Invoice rule created successfully - {} {}", ruleName, rule);
                if (result.equalsIgnoreCase("success")) {
                    customAssert.assertTrue(true, "Invoice rule created - " + ruleName + " " + rule);
                }
                else
                    customAssert.assertTrue(false, "Invoice rule created - " + ruleName + " " + rule);
            } else {
                logger.info("Invoice rule not created - {} {}", ruleName, rule);
                if (result.equalsIgnoreCase("failure"))
                    customAssert.assertTrue(true, "Invoice rule not created - " + ruleName + " " + rule);
                else
                    customAssert.assertTrue(false, "Invoice rule not created - " + ruleName + " " + rule);
            }

        } catch (Exception e) {
            logger.info("Exception caught while creating rule");
            customAssert.assertTrue(false, "Exception caught while creating rule");
        }

        customAssert.assertAll();

    }

    @Test(enabled=false)
    public void C4169(){
        CustomAssert customAssert = new CustomAssert();
        try {
            new AdminHelper().loginWithClientAdminUser();
            String emailName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoicecustomemailtemplatename");
            InvoiceCreateRule invoiceCreateRule = new InvoiceCreateRule();
            invoiceCreateRule.hitNew(customAssert);

            assert invoiceCreateRule.getNewResponse()!=null:"New response is null";

            JSONObject jsonObject = new JSONObject(invoiceCreateRule.getNewResponse());
            String emailData = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("email").getJSONObject("options").getJSONArray("data").toString();

            assert emailData.contains(emailName):"Email name not found in the new response of create rules";
        }
        catch (Exception e){
            logger.info("Exception caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false,"Exception caught "+ Arrays.toString(e.getStackTrace()));
        }
        customAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForCheckValidationInvoiceCreationRule",enabled = true) //SIR-610 TODO see compatibility for C88376,C76646,C76644
    public void checkValidationInvoiceCreationRule(List<String> formulas, List<String> fieldsMap, String result, List<List<String>> conditions) {

        CustomAssert customAssert = new CustomAssert();
        //todo to be removed
        //testCall(customAssert);

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        //callSetRuleInactive(customAssert);

        String staticValue = "%%##&&";
        int fieldsCount = 0;
        String formula, fieldName;
        String queryRule = "";
        List<List<String>> fieldsDataParent = new ArrayList<>();
        List<String> fieldsData = new ArrayList<>();
        String LHSRecord = ""; //for rule type
        try {
            for (String s : formulas) {
                LHSRecord = "";
                fieldsData = new ArrayList<>();
                if (queryRule.length() != 0) {
                    queryRule = queryRule.concat(";");
                }
                formula = s;
                fieldsData.add(formula);
                boolean isLHSValue;
                while (true) {
                    if (formula.contains(staticValue)) {
                        if(LHSRecord.length()==0) {
                            LHSRecord = fieldsMap.get(fieldsCount);
                            isLHSValue = true;
                        }
                        else{
                            isLHSValue = false;
                        }

                        fieldName = getFullFieldNameFromSequence(fieldsMap.get(fieldsCount), formula, customAssert,LHSRecord);

                        //adding the custom field name their randomly generated values in a map for future assistance
                        fieldsData.add(fieldsMap.get(fieldsCount));

                        fieldsCount++;
                        if (Character.isDigit(fieldName.toCharArray()[0]))
                            formula = formula.contains("}#{")  ? formula.replaceFirst(staticValue, "'"+fieldName+"'") : formula.contains("}@{") ? formula.replaceFirst(staticValue, fieldName) :formula.replaceFirst("\\$" + "\\{" + staticValue + "}", fieldName);
                        else
                            formula = (formula.contains("}#{")&&!isLHSValue)  ? formula.replaceFirst(staticValue, "'"+fieldName+"'") : formula.replaceFirst(staticValue, fieldName);
                    } else {
                        break;
                    }
                }
                queryRule = queryRule.concat(formula);
                fieldsDataParent.add(fieldsData);

                //Calculating the result of the rules
                logger.info("Calculating the result of the rule [{}] and fields count {}", formula, fieldsCount);


            }

            List<JSONObject> dataForUpdatingPayload = computeResultForRules(customAssert, fieldsDataParent);
            logger.info("dataForUpdatingPayload = [{}]",dataForUpdatingPayload.toString());

            List<JSONObject> conditionValuesToSet = getConditionsValuesToSet(customAssert,conditions.get(1));

            logger.info("Rule to be created is {}", queryRule);
            InvoiceCreateRule invoiceCreateRule = new InvoiceCreateRule();
            String ruleName = "newRuleAutomation" + RandomNumbers.getRandomNumberWithinRangeIndex(10000, 99999);

            String createRulePayloadSectionName = getCreateRulePayloadSectionName(customAssert, dataForUpdatingPayload);

            logger.info("Setting invoice create rule payload");
            if(conditions.get(0).size()>0){
                logger.info("Using condition : {}",conditions.get(0).get(0));
                invoiceCreateRule.setCreatePayloadWithCondition(ruleName, queryRule,  createRulePayloadSectionName, conditions.get(0).get(0), customAssert);
            }
            else
                invoiceCreateRule.setCreatePayload(ruleName, queryRule, createRulePayloadSectionName, customAssert);

            logger.info("creating invoice rule");
            invoiceCreateRule.createRule(customAssert); //creating invoice rule
            String createResponse = invoiceCreateRule.getCreateResponse();


            if (createResponse.contains("Created Successfully")) {
                logger.info("Invoice rule created successfully - {} {}", ruleName, queryRule);
                if (result.equalsIgnoreCase("success")) {
                    customAssert.assertTrue(true, "Invoice rule created - " + ruleName + " " + queryRule);

                    rulesToBeDeactivated.add(String.valueOf(new JSONObject(createResponse).getJSONObject("header").getJSONObject("response").getInt("entityId")));
                    logger.info("Updating workflow creation date to now");
                    updateWorkflowDate(customAssert);


                    //Starting End user part i.e. executing the created rule
                    new AdminHelper().loginWithEndUser();

                    //Starting the flow for creation and execution
                    logger.info("Starting the flow for creation and execution");

                    InvoicePricingHelper pricingObj = new InvoicePricingHelper();

                    int contractId;
                    int serviceDataId = -1;
                    int invoiceId = -1;
                    int invoiceLineItemId = -1;

                    String contractSectionName = "default";
                    String serviceDataSectionName = "default";
                    String invoiceSectionName = "default";
                    String invoiceLineItemSectionName = "default";

                    try {
                        //Get Contract that will be used for Invoice Flow Validation
                        synchronized (this) {
                            String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
                            if (temp != null)
                                contractSectionName = temp.trim();


                            contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);
                            //contractId = getContractId();
                            if (contractId != -1) {
                                logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                                deleteEntityMap.put(contractId, contractEntity);

                                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                                if (temp != null)
                                    serviceDataSectionName = temp.trim();

                                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                                if (temp != null)
                                    invoiceSectionName = temp.trim();

                                InvoiceHelper.updateServiceDataAndInvoiceConfigDistinct(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, serviceDataSectionName, contractId);

                                //added newly
                                New newServiceData = new New();
                                newServiceData.hitNew(serviceDataEntity,contractEntity,contractId);
                                String newResponse = newServiceData.getNewJsonStr();
                                JSONObject jsonObjectParent = new JSONObject(newResponse);
                                JSONObject DYNToAdd = jsonObjectParent.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");

                                List<JSONObject> payloadGroup = Stream.concat(dataForUpdatingPayload.stream(),conditionValuesToSet.stream()).collect(Collectors.toList());

                                for(JSONObject jsonObject : payloadGroup){
                                    String name = jsonObject.getString("name");
                                    String type = jsonObject.getString("type");
                                    String entityType = jsonObject.getString("entityName");
                                    String resultType = jsonObject.getString("result");
                                    String fieldType = jsonObject.getString("fieldType");

                                    String value = jsonObject.getString("value");
                                    if(resultType.equalsIgnoreCase("false")&&entityType.equalsIgnoreCase("service data")) {
                                        if (fieldType.equalsIgnoreCase("custom")) {
                                            customFieldDYN = "";
                                            try {
                                                findCustomFieldNodeFromJson(jsonObjectParent.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent"), name);
                                            } catch (Exception e) {
                                                logger.info("Exception caught {}", Arrays.toString(e.getStackTrace()));
                                            }
                                            if (customFieldDYN.length() == 0) {

                                                logger.info("Cannot find custom field [{}] in the layout json object in new response", name);
                                                customAssert.assertTrue(false, "Cannot find custom field [" + name + "] in the layout json object in new response");
                                                customAssert.assertAll();
                                            }
                                            if (type.equalsIgnoreCase("number"))
                                                DYNToAdd.getJSONObject(customFieldDYN).put("values", Integer.parseInt(value));
                                            else
                                                DYNToAdd.getJSONObject(customFieldDYN).put("values", value);
                                        }

                                        else if(fieldType.equalsIgnoreCase("static")){

                                            try{
                                                String staticFieldsPayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, name);
                                                JSONObject staticFieldsPayloadJSON = new JSONObject(staticFieldsPayload);
                                                if (staticFieldsPayloadJSON.has("displayValues")) {
                                                    logger.info("found [displayValues] for {}", name);
                                                    staticFieldsPayloadJSON.put("displayValues", getDisplayValue(value, type, customAssert));
                                                }
                                                if (staticFieldsPayloadJSON.has("values")) {
                                                    logger.info("found [values] for {}", name);
                                                    if(type.equalsIgnoreCase("number"))
                                                        staticFieldsPayloadJSON.put("values", Integer.parseInt(value));
                                                    else
                                                        staticFieldsPayloadJSON.put("values", value);
                                                }
                                                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, name, staticFieldsPayloadJSON.toString());
                                            }
                                            catch (Exception e){
                                                logger.info("Exception occurred in updating payload for service data item static field {}", (Object) e.getStackTrace());
                                                customAssert.assertTrue(false,"Exception occurred in updating payload for service data item static field "+ Arrays.toString(e.getStackTrace()));
                                            }
                                        }
                                    }
                                }

                                UpdateFile.addPropertyToConfigFile(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "dynamicMetadata", DYNToAdd.toString());
                                //added newly

                                serviceDataId = InvoiceHelper.getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDataSectionName,contractId);

                                logger.info("Created Service Data Id : [{}]", serviceDataId);
                                if (serviceDataId != -1) {
                                    logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                                    deleteEntityMap.put(serviceDataId, serviceDataEntity);


                                    //Kill All Scheduler Tasks if Flag is On.
                                    if (killAllSchedulerTasks) {
                                        logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                                        UserTasksHelper.removeAllTasks();
                                    }

                                    logger.info("Hitting Fetch API.");
                                    Fetch fetchObj = new Fetch();
                                    fetchObj.hitFetch();
                                    List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                                    boolean uploadPricing = true;
                                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                                    if (temp != null && temp.trim().equalsIgnoreCase("false"))
                                        uploadPricing = false;


                                    if (uploadPricing) {
                                        String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                "pricingstemplatefilename");

                                        boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, customAssert);
                                        // changes for ARC RRC FLOW
                                        if (pricingFile) {

                                            if (pricingFile) {


                                                String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                                if (pricingUploadResponse != null && pricingUploadResponse.trim().toLowerCase().contains("200:;")) {
                                                    //Wait for Pricing Scheduler to Complete
                                                    // pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);
                                                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                                                    if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                                        logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                                                flowToTest + "]");
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                        customAssert.assertAll();
                                                        return;
                                                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                                                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                                                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                                                            customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                                                    "Hence failing Flow [" + flowToTest + "]");
                                                        } else {
                                                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow [{}]", flowToTest);
                                                        }
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                        customAssert.assertAll();
                                                        return;
                                                    } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                                                    {


                                                        boolean isDataCreatedUnderChargesTab = isChargesCreated(serviceDataId);

                                                        if (!isDataCreatedUnderChargesTab) {
                                                            customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                                    flowToTest + "]");
                                                            addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                            customAssert.assertAll();
                                                            return;
                                                        }

                                                    }

                                                } else {
                                                    logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                                            pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);

                                                    customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                                            pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");

                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                    customAssert.assertAll();
                                                    return;
                                                }
                                            } else {
                                                logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                                                customAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                                        "Hence skipping validation");

                                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                customAssert.assertAll();

                                                return;

                                            }

                                        } else {
                                            logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
                                            customAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                                    "Hence skipping validation");

                                            addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                            customAssert.assertAll();
                                            return;
                                        }
                                    } else {
                                        logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                                    }


                                    //added newly
                                    New newInvoice = new New();
                                    newInvoice.hitNew(invoiceEntity,contractEntity,contractId);
                                    newResponse = newInvoice.getNewJsonStr();
                                    jsonObjectParent = new JSONObject(newResponse);
                                    DYNToAdd = jsonObjectParent.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");

                                    for(JSONObject jsonObject : payloadGroup) {
                                        String name = jsonObject.getString("name");
                                        String type = jsonObject.getString("type");
                                        String entityType = jsonObject.getString("entityName");
                                        String resultType = jsonObject.getString("result");
                                        String fieldType = jsonObject.getString("fieldType");

                                        String value = jsonObject.getString("value");
                                        if (resultType.equalsIgnoreCase("false") && entityType.equalsIgnoreCase("invoice")) {
                                            if (fieldType.equalsIgnoreCase("custom")) {
                                                customFieldDYN = "";
                                                try {
                                                    findCustomFieldNodeFromJson(jsonObjectParent.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent"), name);
                                                } catch (Exception e) {
                                                    logger.info("Exception caught {}", Arrays.toString(e.getStackTrace()));
                                                }
                                                if (customFieldDYN.length() == 0) {

                                                    logger.info("Cannot find custom field [{}] in the layout json object in new response", name);
                                                    customAssert.assertTrue(false, "Cannot find custom field [" + name + "] in the layout json object in new response");
                                                    customAssert.assertAll();
                                                }
                                                if (type.equalsIgnoreCase("number"))
                                                    DYNToAdd.getJSONObject(customFieldDYN).put("values", Integer.parseInt(value));
                                                else
                                                    DYNToAdd.getJSONObject(customFieldDYN).put("values", value);
                                            }
                                            else if (fieldType.equalsIgnoreCase("static")) {

                                                try {
                                                    String staticFieldsPayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, invoiceSectionName, name);
                                                    JSONObject staticFieldsPayloadJSON = new JSONObject(staticFieldsPayload);
                                                    if (staticFieldsPayloadJSON.has("displayValues")) {
                                                        logger.info("found [displayValues] for {}", name);
                                                        staticFieldsPayloadJSON.put("displayValues", getDisplayValue(value, type, customAssert));
                                                    }
                                                    if (staticFieldsPayloadJSON.has("values")) {
                                                        logger.info("found [values] for {}", name);
                                                        if(type.equalsIgnoreCase("number"))
                                                            staticFieldsPayloadJSON.put("values", Integer.parseInt(value));
                                                        else
                                                            staticFieldsPayloadJSON.put("values", value);
                                                    }
                                                    UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, invoiceSectionName, name, staticFieldsPayloadJSON.toString());
                                                } catch (Exception e) {
                                                    logger.info("Exception occurred in updating payload for invoice static field {}", (Object) e.getStackTrace());
                                                    customAssert.assertTrue(false, "Exception occurred in updating payload for invoice static field " + Arrays.toString(e.getStackTrace()));
                                                }
                                            }
                                        }

                                    }

                                    UpdateFile.addPropertyToConfigFile(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, invoiceSectionName, "dynamicMetadata", DYNToAdd.toString());

                                    //added newly
                                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                                    if (temp != null)
                                        invoiceLineItemSectionName = temp.trim();

                                    invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceSectionName,invoiceLineItemSectionName);

                                    logger.info("Created Invoice Id is : [{}]", invoiceId);
                                    if (invoiceId != -1) {
                                        //Get Invoice Line Item Id

                                        //added newly
                                        New newLineItem = new New();
                                        String lineItemTypeId = ParseConfigFile.getValueFromConfigFile(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemSectionName, "lineitemtypeid");
                                        newLineItem.hitNew(invoiceLineItemEntity,invoiceEntity,invoiceId,null,lineItemTypeId);
                                        newResponse = newLineItem.getNewJsonStr();
                                        jsonObjectParent = new JSONObject(newResponse);
                                        DYNToAdd = jsonObjectParent.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");

                                        for(JSONObject jsonObject : payloadGroup){
                                            String name = jsonObject.getString("name");
                                            String type = jsonObject.getString("type");
                                            String entityType = jsonObject.getString("entityName");
                                            String resultType = jsonObject.getString("result");
                                            String fieldType = jsonObject.get("fieldType").toString();

                                            String value = jsonObject.getString("value");
                                            if(resultType.equalsIgnoreCase("false")&&entityType.equalsIgnoreCase("line item")) {
                                                if (fieldType.equalsIgnoreCase("custom")) {
                                                    customFieldDYN = "";
                                                    try {
                                                        findCustomFieldNodeFromJson(jsonObjectParent.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent"), name);
                                                    } catch (Exception e) {
                                                        logger.info("Exception caught {}", Arrays.toString(e.getStackTrace()));
                                                    }
                                                    if (customFieldDYN.length() == 0) {

                                                        logger.info("Cannot find custom field [{}] in the layout json object in new response", name);
                                                        customAssert.assertTrue(false, "Cannot find custom field [" + name + "] in the layout json object in new response");
                                                        customAssert.assertAll();
                                                    }
                                                    if (type.equalsIgnoreCase("number"))
                                                        DYNToAdd.getJSONObject(customFieldDYN).put("values", Integer.parseInt(value));
                                                    else
                                                        DYNToAdd.getJSONObject(customFieldDYN).put("values", value);



                                                }
                                                else if(fieldType.equalsIgnoreCase("static")){

                                                    try{
                                                        String staticFieldsPayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, name);
                                                        JSONObject staticFieldsPayloadJSON = new JSONObject(staticFieldsPayload);
                                                        if (staticFieldsPayloadJSON.has("displayValues")) {
                                                            logger.info("found [displayValues] for {}", name);
                                                            staticFieldsPayloadJSON.put("displayValues", getDisplayValue(value, type, customAssert));
                                                        }
                                                        if (staticFieldsPayloadJSON.has("values")) {
                                                            logger.info("found [values] for {}", name);
                                                            if(type.equalsIgnoreCase("number"))
                                                                staticFieldsPayloadJSON.put("values", Integer.parseInt(value));
                                                            else
                                                                staticFieldsPayloadJSON.put("values", value);
                                                        }
                                                        UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, name, staticFieldsPayloadJSON.toString());
                                                    }
                                                    catch (Exception e){
                                                        logger.info("Exception occurred in updating payload for invoice line item static field {}", (Object) e.getStackTrace());
                                                        customAssert.assertTrue(false,"Exception occurred in updating payload for invoice line item static field "+ Arrays.toString(e.getStackTrace()));
                                                    }
                                                }
                                            }
                                        }

                                        //UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, "dynamicMetadata", DYNToAdd.toString());
                                        UpdateFile.addPropertyToConfigFile(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, "dynamicMetadata", DYNToAdd.toString());
                                        //added newly

                                        invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, serviceDataId);
                                        logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                                        deleteEntityMap.put(invoiceId, invoiceEntity);
                                    } else {
                                        logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                        //customAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                        customAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                    }

                                } else {
                                    logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                    //customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                    customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                }
                            } else {
                                logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                //customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                            }
                        }
                        if (invoiceLineItemId != -1) {
                            deleteEntityMap.put(invoiceLineItemId, invoiceLineItemEntity);

                            Show show ;

                            show = new Show();
                            show.hitShow(ConfigureConstantFields.getEntityIdByName(invoiceEntity),invoiceId);
                            String invoiceResponse = show.getShowJsonStr();
                            JSONObject invoiceJson = new JSONObject(invoiceResponse);

                            show = new Show();
                            show.hitShow(ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity),invoiceLineItemId);
                            String lineItemResponse = show.getShowJsonStr();
                            JSONObject lineItemJson = new JSONObject(lineItemResponse);

                            for(JSONObject jsonObject : dataForUpdatingPayload){
                                foundResultInShow = false;
                                staticFieldFound = false;
                                try {

                                    String name = jsonObject.get("name").toString();
                                    String value = jsonObject.get("value").toString();
                                    String entityName = jsonObject.get("entityName").toString();
                                    String resultType = jsonObject.get("result").toString();
                                    String fieldType = jsonObject.get("fieldType").toString();
                                    if(resultType.equalsIgnoreCase("true")) {
                                        boolean check = conditions.get(2).size() < 1 || conditions.get(2).get(0).equalsIgnoreCase("y");
                                        if(fieldType.equalsIgnoreCase("custom")) {
                                            if (entityName.equalsIgnoreCase("invoice")) {
                                                customFieldDYN = "";
                                                findCustomFieldNodeFromJson(invoiceJson, name);
                                                if (customFieldDYN.length() == 0) {
                                                    logger.info("Cannot find custom field [{}] for final result checking in the json object in new response", name);
                                                    customAssert.assertTrue(false, "Cannot find custom field [" + name + "] for final result checking in the json object in new response");
                                                }
                                                verifyResult(invoiceJson, customFieldDYN, value);
                                            } else if (entityName.equalsIgnoreCase("line item")) {
                                                customFieldDYN = "";
                                                findCustomFieldNodeFromJson(lineItemJson, name);
                                                if (customFieldDYN.length() == 0) {
                                                    logger.info("Cannot find custom field [{}] for final result checking in the json object in new response", name);
                                                    customAssert.assertTrue(false, "Cannot find custom field [" + name + "] for final result checking in the json object in new response");
                                                }
                                                verifyResult(lineItemJson, customFieldDYN, value);
                                            }
                                            if (!foundResultInShow&&check) {
                                                logger.info("Custom field name or desired value not found in the invoice/line item response name [{}], value [{}]", name, value);
                                                customAssert.assertTrue(false, "Custom field name or desired value not found in the invoice/line item response name [" + name + "] ,value [" + value + "]");
                                            }
                                            else if (foundResultInShow&&!check) {
                                                logger.info("Custom field name or desired value found in the invoice/line item which was not expected response name [{}], value [{}]", name, value);
                                                customAssert.assertTrue(false, "Custom field name or desired value found in the invoice/line item which was not expected response name [" + name + "] ,value [" + value + "]");
                                            }
                                        }
                                        else if(fieldType.equalsIgnoreCase("static")){
                                            if (entityName.equalsIgnoreCase("invoice")) {
                                                findStaticFieldNodeFromJson(invoiceJson, name, value);
                                            } else if (entityName.equalsIgnoreCase("line item")) {
                                                findStaticFieldNodeFromJson(lineItemJson, name, value);
                                            }
                                            if (!staticFieldFound&&check) {
                                                logger.info("Static field name or desired value not found in the invoice/line item response name [{}], value [{}]", name, value);
                                                customAssert.assertTrue(false, "Static field name or desired value not found in the invoice/line item response name [" + name + "] ,value [" + value + "]");
                                            }
                                            else if (staticFieldFound&&!check) {
                                                logger.info("Static field name or desired value found in the invoice/line item which was not expected response name [{}], value [{}]", name, value);
                                                customAssert.assertTrue(false, "Static field name or desired value found in the invoice/line item which was not expected response name [" + name + "] ,value [" + value + "]");
                                            }
                                        }
                                    }
                                }
                                catch (Exception e){
                                    logger.info("Exception caught {}", (Object) e.getStackTrace());
                                    customAssert.assertTrue(false,"Exception caught in checking result in payload");
                                }
                            }

                        } else {
                            logger.error("Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                            //customAssert.assertTrue(false, "Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [" +
//									flowToTest + "]");
                            customAssert.assertTrue(false, "Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [" +
                                    flowToTest + "]");
                        }
                    } catch (Exception e) {
                        logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
                        //customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
                        customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
                    }
                } else
                    customAssert.assertTrue(false, "Invoice rule created - " + ruleName + " " + queryRule);
            } else {
                logger.info("Invoice rule not created - {} {}", ruleName, queryRule);
                if (result.equalsIgnoreCase("failure"))
                    customAssert.assertTrue(true, "Invoice rule not created - " + ruleName + " " + queryRule);
                else
                    customAssert.assertTrue(false, "Invoice rule not created - " + ruleName + " " + queryRule);
            }//end
        } catch (Exception e) {
            logger.info("Exception caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught " + Arrays.toString(e.getStackTrace()));
        }
        new InvoiceCreateRule().deactivateRules(rulesToBeDeactivated);
        rulesToBeDeactivated=new ArrayList<>(); //resetting the array

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void C3341(){
        CustomAssert customAssert = new CustomAssert();

        try {
            String flow= "fixed fee flow 1";

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,flow);
            assert contractId!=-1:"Got the contract id - "+contractId;

            UpdateFile.updateConfigFileProperty(invoiceConfigFilePath,invoiceConfigFileName,flow,"sourceid",String.valueOf(contractId));

            int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,flow,"",flow);

            Show invoiceShow = new Show();
            invoiceShow.hitShow(67,invoiceId);

            List<String> stakeHolders = ShowHelper.getAllSelectedStakeholdersFromShowResponse(invoiceShow.getShowJsonStr());
            logger.info("Stakeholders list for invoice id {} are {}",invoiceId,stakeHolders);
            assert stakeHolders!=null:"Stakeholder from invoice show page is null";

            String stakeholderPayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(contractConfigFilePath,contractExtraFieldsConfigFileName,"common extra fields","stakeHolders");
            JSONObject jsonObject = new JSONObject(stakeholderPayload);
            String stakeHolderName = jsonObject.getJSONObject("values").getJSONObject("rg_2001").getJSONArray("values").getJSONObject(0).getString("name");

            assert stakeHolderName!=null:"Stakeholder name from config file of contract is null";

            assert stakeHolders.contains(stakeHolderName):"stake holder "+stakeHolderName+" not found in the invoice - "+invoiceId;

        }
        catch (Exception e){
            logger.error("Exception caught in main body {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false,"Exception caught main body.");
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void C7661(){
        CustomAssert customAssert = new CustomAssert();
        try {
            boolean success = true;
            String[] rules = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"c7661","rules").split("&");

            new AdminHelper().loginWithClientAdminUser();
            int priority=2;
            for(String rule : rules){
                logger.info("Rule to be created is {}", rule);
                InvoiceCreateRule invoiceCreateRule = new InvoiceCreateRule();
                String ruleName = "newRuleAutomation" + RandomNumbers.getRandomNumberWithinRangeIndex(10000, 99999);
                logger.info("Using rule - {}", ruleName);

                String createRulePayloadSectionName = "rule invoice abc news";

                logger.info("Setting invoice create rule payload");
                invoiceCreateRule.setCreatePayload(ruleName, rule, createRulePayloadSectionName, priority, customAssert);
                priority--;

                logger.info("creating invoice rule");
                invoiceCreateRule.createRule(customAssert); //creating invoice rule
                String createResponse = invoiceCreateRule.getCreateResponse();
                if(!createResponse.contains("Created Successfully"))
                    success = false;
                else
                    rulesToBeDeactivated.add(String.valueOf(new JSONObject(createResponse).getJSONObject("header").getJSONObject("response").getInt("entityId")));

            }

            if (success) {
                logger.info("Invoice rule created successfully - {}", (Object) rules);
                    customAssert.assertTrue(true, "Invoice rule created - " + rules);

                    //Starting End user part i.e. executing the created rule
                    new AdminHelper().loginWithEndUser();

                    //Starting the flow for creation and execution
                    logger.info("Starting the flow for creation and execution");

                    InvoicePricingHelper pricingObj = new InvoicePricingHelper();

                    int contractId;
                    int serviceDataId = -1;
                    int invoiceId = -1;
                    int invoiceLineItemId = -1;

                    String contractSectionName = "default";
                    String serviceDataSectionName = "default";
                    String invoiceSectionName = "default";

                    try {
                        //Get Contract that will be used for Invoice Flow Validation
                        synchronized (this) {
                            String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
                            if (temp != null)
                                contractSectionName = temp.trim();


                            contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);
                            //contractId = getContractId();
                            if (contractId != -1) {
                                logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                                deleteEntityMap.put(contractId, contractEntity);

                                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                                if (temp != null)
                                    serviceDataSectionName = temp.trim();

                                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                                if (temp != null)
                                    invoiceSectionName = temp.trim();

                                InvoiceHelper.updateServiceDataAndInvoiceConfigDistinct(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, serviceDataSectionName, contractId);

                                serviceDataId = InvoiceHelper.getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDataSectionName,contractId);

                                logger.info("Created Service Data Id : [{}]", serviceDataId);
                                if (serviceDataId != -1) {
                                    logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                                    deleteEntityMap.put(serviceDataId, serviceDataEntity);


                                    //Kill All Scheduler Tasks if Flag is On.
                                    if (killAllSchedulerTasks) {
                                        logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                                        UserTasksHelper.removeAllTasks();
                                    }

                                    logger.info("Hitting Fetch API.");
                                    Fetch fetchObj = new Fetch();
                                    fetchObj.hitFetch();
                                    List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                                    boolean uploadPricing = true;
                                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                                    if (temp != null && temp.trim().equalsIgnoreCase("false"))
                                        uploadPricing = false;


                                    if (uploadPricing) {
                                        String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                "pricingstemplatefilename");

                                        boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, customAssert);
                                        // changes for ARC RRC FLOW
                                        if (pricingFile) {

                                            if (pricingFile) {


                                                String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                                if (pricingUploadResponse != null && pricingUploadResponse.trim().toLowerCase().contains("200:;")) {
                                                    //Wait for Pricing Scheduler to Complete
                                                    // pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);
                                                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                                                    if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                                        logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                                                flowToTest + "]");
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                        customAssert.assertAll();
                                                        return;
                                                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                                                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                                                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                                                            customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                                                    "Hence failing Flow [" + flowToTest + "]");
                                                        } else {
                                                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow [{}]", flowToTest);
                                                        }
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                        customAssert.assertAll();
                                                        return;
                                                    } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                                                    {


                                                        boolean isDataCreatedUnderChargesTab = isChargesCreated(serviceDataId);

                                                        if (!isDataCreatedUnderChargesTab) {
                                                            customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                                    flowToTest + "]");
                                                            addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                            customAssert.assertAll();
                                                            return;
                                                        }

                                                    }

                                                } else {
                                                    logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                                            pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);

                                                    customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                                            pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");

                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                    customAssert.assertAll();
                                                    return;
                                                }
                                            } else {
                                                logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                                                customAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                                        "Hence skipping validation");

                                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                                customAssert.assertAll();

                                                return;

                                            }

                                        } else {
                                            logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
                                            customAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                                    "Hence skipping validation");

                                            addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), customAssert);
                                            customAssert.assertAll();
                                            return;
                                        }
                                    } else {
                                        logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                                    }


                                    //added newly

                                    invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceSectionName,"");

                                    logger.info("Created Invoice Id is : [{}]", invoiceId);
                                    if (invoiceId != -1) {

                                        Show invoiceShow = new Show();
                                        invoiceShow.hitShow(67,invoiceId);

                                        JSONObject invoiceShowJson = new JSONObject(invoiceShow.getShowJsonStr());
                                        String fieldName = "invoice text field basic information 1";
                                        String value="12";
                                        customFieldDYN = "";
                                        findCustomFieldNodeFromJson(invoiceShowJson, fieldName);
                                        if (customFieldDYN.length() == 0) {
                                            logger.info("Cannot find custom field [{}] for final result checking in the json object in new response", fieldName);
                                            customAssert.assertTrue(false, "Cannot find custom field [" + fieldName + "] for final result checking in the json object in new response");
                                        }
                                        verifyResult(invoiceShowJson, customFieldDYN,value);
                                        if (!foundResultInShow) {
                                            logger.info("Custom field name or desired value not found in the invoice/line item response name [{}], value [{}]", fieldName, value);
                                            customAssert.assertTrue(false, "Custom field name or desired value not found in the invoice/line item response name [" + fieldName + "] ,value [" + value + "]");
                                        }
                                    } else {
                                        logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                        //customAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                        customAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                    }

                                } else {
                                    logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                    //customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                    customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                }
                            } else {
                                logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                //customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
                        //customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
                        customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
                    }
            } else {
                logger.info("Invoice rule not created - {}", (Object) rules);
                    customAssert.assertTrue(false, "Invoice rule not created - " + Arrays.toString(rules));
            }//end
        }
        catch (Exception e){
            logger.error("Exception {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false,"Exception caught "+ Arrays.toString(e.getStackTrace()));
        }
        new InvoiceCreateRule().deactivateRules(rulesToBeDeactivated);
        rulesToBeDeactivated=new ArrayList<>(); //resetting the array
        customAssert.assertAll();
    }

    private String getDisplayValue(String value, String type, CustomAssert customAssert){
        try{
            if(type.equalsIgnoreCase("date")||type.equalsIgnoreCase("datewithtimezone")){
                SimpleDateFormat format2 = new SimpleDateFormat("dd-MMM-yyyy");
                SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
                Date date = format1.parse(value);
                return format2.format(date);
            }
//            else if(type.equalsIgnoreCase("number")){
//                return getDisplayValue()
//            }
            else{
                return value;
            }
        }
        catch (Exception e){
            logger.error("Exception occurred in parsing the date {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false,"Exception occurred in parsing the date "+ Arrays.toString(e.getStackTrace()));
            customAssert.assertAll();
        }
        return value;
    }

    private void verifyResult(JSONObject jsonObject, String name, String value){

        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()){
            nextKey = iterator.next();
            if(nextKey.equalsIgnoreCase(name)){
                if(jsonObject.getJSONObject(name).has("values")) {
                    String val;
                    if(jsonObject.getJSONObject(name).get("values").toString().matches("^[0-9]+[.][0-9]+$")) //for finding out decimal numbers
                    {
                        logger.info("Double value found {} while checking",name);
                        val = String.valueOf((int) jsonObject.getJSONObject(name).getDouble("values"));
                    }
                    else if(jsonObject.getJSONObject(name).get("values").toString().matches("^[0-9][0-9][-][0-9][0-9][-]([0-9]){4}[ ]([0-9][0-9][:]){2}[0-9][0-9]$")){
                        logger.info("Date value found {} while checking",name);
                        val = jsonObject.getJSONObject(name).get("values").toString().split(" ")[0];
                    }
                    else
                        val = jsonObject.getJSONObject(name).get("values").toString();

                    if(value.matches("^[0-9][0-9][-][0-9][0-9][-]([0-9]){4}[ ]([0-9][0-9][:]){2}[0-9][0-9]$")) //removing the time stamp as it is ignored in dev code
                        value = value.split(" ")[0];
                    if (val.equalsIgnoreCase(value)) {
                        foundResultInShow = true;
                        logger.info("Found name [{}] with value [{}]", name, value);
                    } else {
                        logger.info("Not Matched, Found name [{}] but with value [{}]", name, jsonObject.getJSONObject(name).get("values").toString());
                    }
                }
                else{
                    logger.info("Found name {} but didn't find [values]",name);
                }
            }
            else{
                if(jsonObject.get(nextKey) instanceof JSONObject){
                    verifyResult((JSONObject) jsonObject.get(nextKey), name,value);
                }
                else if(jsonObject.get(nextKey) instanceof JSONArray)
                    verifyResult((JSONArray) jsonObject.get(nextKey), name,value);
            }
        }
    }

    private void verifyResult(JSONArray jsonArray, String name, String value){

        for(int index =0;index<jsonArray.length();index++){
            if(jsonArray.get(index) instanceof  JSONArray)
                verifyResult((JSONArray) jsonArray.get(index), name,value);
            else if(jsonArray.get(index) instanceof  JSONObject)
                verifyResult((JSONObject) jsonArray.get(index), name,value);
        }
    }

    private List<JSONObject> getConditionsValuesToSet(CustomAssert customAssert, List<String> data){
        List<JSONObject> dataForUpdatingPayload = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        try {
            for (String datum : data) {
                String name = getFieldName(datum.split(">")[0]);
            jsonObject.put("name",name);
            jsonObject.put("fieldType", datum.charAt(0)=='C' ? "custom" : "static");
            jsonObject.put("entityName", ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", datum.split(">")[0].split("[+]")[0].substring(1)));
            jsonObject.put("result", "false");
            jsonObject.put("type", ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", datum.split(">")[0].split("[+]")[1]));
            jsonObject.put("value", datum.split(">")[1]);
            dataForUpdatingPayload.add(jsonObject);
        }
        }catch (Exception e) {
            logger.info("Exception caught in computeResultForRules() {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught in computeResultForRules() " + Arrays.toString(e.getStackTrace()));
            customAssert.assertAll();
        }

        return dataForUpdatingPayload;
    }

    private List<JSONObject> computeResultForRules(CustomAssert customAssert, List<List<String>> data) {
        List<JSONObject> dataForUpdatingPayload = new ArrayList<>();
        JSONObject jsonObject ;
        try {
            String LHSField;
            for (List<String> datum : data) {
                String name;
                jsonObject = new JSONObject();
                LHSField = getRHSComputation(customAssert, datum.get(0), datum.subList(2, datum.size()), dataForUpdatingPayload);
                name = getFieldName(datum.get(1));
                if(checkContainsField(name,dataForUpdatingPayload,datum.get(1))){
                    for(int index=0;index<dataForUpdatingPayload.size();index++){
                        JSONObject jsonObject1 = dataForUpdatingPayload.get(index);
                        if(jsonObject1.get("name").toString().equalsIgnoreCase(name))
                            jsonObject1.put("value",LHSField);
                    }
                }
                else{
                    jsonObject.put("name",datum.get(1).contains("C")?name:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", name));
                    jsonObject.put("fieldType",datum.get(1).contains("C")?"custom":"static");
                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", datum.get(1).split("[+]")[0].substring(1)));
                    jsonObject.put("result","true");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", datum.get(1).split("[+]")[1]));
                    jsonObject.put("value",LHSField);
                    dataForUpdatingPayload.add(jsonObject);
                }


            }
        } catch (Exception e) {
            logger.info("Exception caught in computeResultForRules() {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught in computeResultForRules() " + Arrays.toString(e.getStackTrace()));
            customAssert.assertAll();
        }

        return dataForUpdatingPayload;
    }

    private String getRHSComputation(CustomAssert customAssert, String formula, List<String> list, List<JSONObject> dataForUpdatingPayload) {
        try {
            if (formula.contains("~add(")) {
                int sum = 0;
                for (String s : list) {
                    int value;
                    String fieldName = getFieldName(s);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                    jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                    String subString = s.substring(1);
                    if (checkContainsField(fieldName,dataForUpdatingPayload,s)) {
                        value = Integer.parseInt(getFieldValue(fieldName,dataForUpdatingPayload,s));
                    } else {
                        value = RandomNumbers.getRandomNumberWithinRange(100, 999);

                        jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                        jsonObject.put("result","false");
                        jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                        jsonObject.put("value",String.valueOf(value));
                        dataForUpdatingPayload.add(jsonObject);
                    }
                    sum += value;
                }
                return String.valueOf(sum);
            }
            else if(formula.contains("~multiply(")) {
                int prod = 1;
                for (String s : list) {
                    int value;
                    String fieldName = getFieldName(s);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                    jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                    String subString = s.substring(1);
                    if (checkContainsField(fieldName,dataForUpdatingPayload,s)) {
                        value = Integer.parseInt(getFieldValue(fieldName,dataForUpdatingPayload,s));
                    } else {
                        value = RandomNumbers.getRandomNumberWithinRange(100, 999);
                        jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                        jsonObject.put("result","false");
                        jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                        jsonObject.put("value",String.valueOf(value));
                        dataForUpdatingPayload.add(jsonObject);
                    }
                    prod *= value;
                }
                return String.valueOf(prod);
            }
            else if(formula.contains("~divide(")) {
                int prod = -1;
                for (String s : list) {
                    int value;
                    String fieldName = getFieldName(s);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                    jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                    String subString = s.substring(1);
                    if (checkContainsField(fieldName,dataForUpdatingPayload,s)) {
                        value = Integer.parseInt(getFieldValue(fieldName,dataForUpdatingPayload,s));
                    } else {
                        value = RandomNumbers.getRandomNumberWithinRange(100, 999);
                        jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                        jsonObject.put("result","false");
                        jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                        jsonObject.put("value",String.valueOf(value));
                        dataForUpdatingPayload.add(jsonObject);
                    }
                    if (prod == -1)
                        prod = value;
                    else
                    if(value==0)
                        prod = 0;
                    else
                        prod /= value;
                }
                return String.valueOf(prod);
            }
            else if(formula.contains("~subtract(")) {
                int sum = 0;
                for (String s : list) {
                    int value;
                    String fieldName = getFieldName(s);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                    jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                    String subString = s.substring(1);
                    if (checkContainsField(fieldName,dataForUpdatingPayload,s)) {
                        value = Integer.parseInt(getFieldValue(fieldName,dataForUpdatingPayload,s));
                    } else {
                        value = RandomNumbers.getRandomNumberWithinRange(100, 999);
                        jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                        jsonObject.put("result","false");
                        jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                        jsonObject.put("value",String.valueOf(value));
                        dataForUpdatingPayload.add(jsonObject);
                    }
                    if(sum==0)
                        sum = value;
                    else
                        sum-=value;
                }
                return String.valueOf(sum);
            }
            else if(formula.contains("~min(")) {
                int min = Integer.MAX_VALUE;
                for (String s : list) {
                    int value;
                    String fieldName = getFieldName(s);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                    jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                    String subString = s.substring(1);
                    if (checkContainsField(fieldName,dataForUpdatingPayload,s)) {
                        value = Integer.parseInt(getFieldValue(fieldName,dataForUpdatingPayload,s));
                    } else {
                        value = RandomNumbers.getRandomNumberWithinRange(100, 999);
                        jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                        jsonObject.put("result","false");
                        jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                        jsonObject.put("value",String.valueOf(value));
                        dataForUpdatingPayload.add(jsonObject);
                    }
                    if (value < min)
                        min = value;
                }
                return String.valueOf(min);
            }
            else if(formula.contains("~max(")) {
                int max = Integer.MIN_VALUE;
                for (String s : list) {
                    int value;
                    String fieldName = getFieldName(s);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                    jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                    String subString = s.substring(1);
                    if (checkContainsField(fieldName,dataForUpdatingPayload,s)) {
                        value = Integer.parseInt(getFieldValue(fieldName,dataForUpdatingPayload,s));
                    } else {
                        value = RandomNumbers.getRandomNumberWithinRange(100, 999);
                        jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                        jsonObject.put("result","false");
                        jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                        jsonObject.put("value",String.valueOf(value));
                        dataForUpdatingPayload.add(jsonObject);
                    }
                    if (value > max)
                        max = value;
                }
                return String.valueOf(max);
            }
            else if(formula.contains("~avg(")) {
                int sum = 0;
                for (String s : list) {
                    int value;
                    String fieldName = getFieldName(s);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                    jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                    String subString = s.substring(1);
                    if (checkContainsField(fieldName,dataForUpdatingPayload,s)) {
                        value = Integer.parseInt(getFieldValue(fieldName,dataForUpdatingPayload,s));
                    } else {
                        value = RandomNumbers.getRandomNumberWithinRange(100, 999);
                        jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                        jsonObject.put("result","false");
                        jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                        jsonObject.put("value",String.valueOf(value));
                        dataForUpdatingPayload.add(jsonObject);
                    }

                    sum +=value;
                }
                int avg = sum / list.size();
                return String.valueOf(avg);
            }
            else if(formula.contains("@")) {
                String s = list.get(0);
                String fieldName = getFieldName(s);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("fieldType",s.contains("C")?"custom":"static");
                jsonObject.put("name",s.contains("C")?fieldName:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldName));

                String subString = s.substring(1);
                if(checkContainsField(fieldName,dataForUpdatingPayload,s)){
                    return getFieldValue(fieldName,dataForUpdatingPayload,s);
                }
                else{

                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", subString.split("[+]")[0]));
                    jsonObject.put("result","false");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", subString.split("[+]")[1]));
                    String value;
                    if(jsonObject.getString("type").equalsIgnoreCase("date"))
                        value = "04-15-2018";
                    else if(jsonObject.getString("type").equalsIgnoreCase("datewithtimezone"))
                        value = "04-15-2018 00:00:00";
                    else if(jsonObject.getString("type").equalsIgnoreCase("number"))
                        value = String.valueOf(RandomNumbers.getRandomNumberWithinRange(100, 999));
                    else
                        value = "sample text data";
                    jsonObject.put("value",value);
                    dataForUpdatingPayload.add(jsonObject);
                    return value;
                }
            }
            else if(formula.contains("~addMonths(")) {
                String dateName = list.get(0);
                String numName = list.get(1);
                String fieldDate = getFieldName(dateName);
                String fieldNum = getFieldName(numName);
                int value;
                String dateValue;
                if(checkContainsField(fieldNum,dataForUpdatingPayload,numName)){
                    value = Integer.parseInt(getFieldValue(fieldNum,dataForUpdatingPayload,numName));
                }
                else{
                    value = RandomNumbers.getRandomNumberWithinRange(1, 9);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",numName.contains("C")?"custom":"static");
                    jsonObject.put("name",numName.contains("C")?fieldNum:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldNum));
                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", numName.substring(1).split("[+]")[0]));
                    jsonObject.put("result","false");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", numName.substring(1).split("[+]")[1]));
                    jsonObject.put("value",String.valueOf(value));
                    dataForUpdatingPayload.add(jsonObject);
                }
                if(checkContainsField(fieldDate,dataForUpdatingPayload,dateName)){
                    dateValue = getFieldValue(fieldDate,dataForUpdatingPayload,dateName);
                }
                else{
                    dateValue = "04-15-2018";
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",dateName.contains("C")?"custom":"static");
                    jsonObject.put("name",dateName.contains("C")?fieldDate:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldDate));
                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", dateName.substring(1).split("[+]")[0]));
                    jsonObject.put("result","false");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", dateName.substring(1).split("[+]")[1]));
                    jsonObject.put("value",dateValue);
                    dataForUpdatingPayload.add(jsonObject);
                }

                SimpleDateFormat format1 = new SimpleDateFormat("MM-dd-yyyy");
                Date date = format1.parse(dateValue);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.MONTH, value);
                Date modifiedDate = cal.getTime();

                return format1.format(modifiedDate);
            }
            else if(formula.contains("~addDays(")) {
                String dateName = list.get(0);
                String numName = list.get(1);
                String fieldDate = getFieldName(dateName);
                String fieldNum = getFieldName(numName);
                int value;
                String dateValue;
                if(checkContainsField(fieldNum,dataForUpdatingPayload,numName)){
                    value = Integer.parseInt(getFieldValue(fieldNum,dataForUpdatingPayload,numName));
                }
                else{
                    value = RandomNumbers.getRandomNumberWithinRange(10, 99);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",numName.contains("C")?"custom":"static");
                    jsonObject.put("name",numName.contains("C")?fieldNum:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldNum));
                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", numName.substring(1).split("[+]")[0]));
                    jsonObject.put("result","false");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", numName.substring(1).split("[+]")[1]));
                    jsonObject.put("value",String.valueOf(value));
                    dataForUpdatingPayload.add(jsonObject);
                }
                if(checkContainsField(fieldDate,dataForUpdatingPayload,dateName)){
                    dateValue = getFieldValue(fieldDate,dataForUpdatingPayload,dateName);
                }
                else{
                    dateValue = "04-15-2018";
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",dateName.contains("C")?"custom":"static");
                    jsonObject.put("name",dateName.contains("C")?fieldDate:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldDate));
                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", dateName.substring(1).split("[+]")[0]));
                    jsonObject.put("result","false");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", dateName.substring(1).split("[+]")[1]));
                    jsonObject.put("value",dateValue);
                    dataForUpdatingPayload.add(jsonObject);
                }

                SimpleDateFormat format1 = new SimpleDateFormat("MM-dd-yyyy");
                Date date = format1.parse(dateValue);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.DATE, value);
                Date modifiedDate = cal.getTime();

                return format1.format(modifiedDate);
            }
            else if(formula.contains("~daysDifference(")) {
                String dateName = list.get(0);
                String dateName2 = list.get(1);
                String fieldDate = getFieldName(dateName);
                String fieldDate2 = getFieldName(dateName2);
                String dateValue, dateValue2;
                if(checkContainsField(fieldDate2,dataForUpdatingPayload,dateName2)){
                    dateValue2 = getFieldValue(fieldDate2,dataForUpdatingPayload,dateName2);
                }
                else{
                    dateValue2 = "04-15-2018";
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",dateName2.contains("C")?"custom":"static");
                    jsonObject.put("name",dateName2.contains("C")?fieldDate2:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldDate2));
                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", dateName2.substring(1).split("[+]")[0]));
                    jsonObject.put("result","false");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", dateName2.substring(1).split("[+]")[1]));
                    jsonObject.put("value",String.valueOf(dateValue2));
                    dataForUpdatingPayload.add(jsonObject);
                }
                if(checkContainsField(fieldDate,dataForUpdatingPayload,dateName)){
                    dateValue = getFieldValue(fieldDate,dataForUpdatingPayload,dateName);
                }
                else{
                    dateValue = "04-23-2018";
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("fieldType",dateName.contains("C")?"custom":"static");
                    jsonObject.put("name",dateName.contains("C")?fieldDate:ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field label name map", fieldDate));
                    jsonObject.put("entityName",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", dateName.substring(1).split("[+]")[0]));
                    jsonObject.put("result","false");
                    jsonObject.put("type",ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "field type", dateName.substring(1).split("[+]")[1]));
                    jsonObject.put("value",dateValue);
                    dataForUpdatingPayload.add(jsonObject);
                }

                SimpleDateFormat format1 = new SimpleDateFormat("MM-dd-yyyy");
                Date date = format1.parse(dateValue);
                Date date2 = format1.parse(dateValue2);
                long diff = date.getTime() - date2.getTime();

                return String.valueOf((int)diff/(24 * 60 * 60 * 1000));
            }
            else if (formula.contains("#")) {
                return list.get(0);
            }
        } catch (Exception e) {
            logger.info("Exception caught in getRHSComputation() {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught in getRHSComputation() " + Arrays.toString(e.getStackTrace()));
            customAssert.assertAll();
        }

        return null;

    }

    private boolean checkContainsField(String toFindName,List<JSONObject> dataForUpdatingPayload,String seq){
        if(!seq.matches("^[CS][1-9+]+$")) //if value is constant mark it present to avoid adding constant field to json list
            return true;
        for(JSONObject jsonObject : dataForUpdatingPayload){
            if(jsonObject.get("name").toString().equalsIgnoreCase(toFindName)){
                return true;
            }
        }
        return false;
    }
    private String getFieldValue(String toFindName,List<JSONObject> dataForUpdatingPayload,String seq){
        if(!seq.matches("^[CS][1-9+]+$")) //if value is constant mark it present to avoid adding constant field to json list
            return toFindName;
        for(JSONObject jsonObject : dataForUpdatingPayload){
            if(jsonObject.get("name").toString().equalsIgnoreCase(toFindName)){
                return jsonObject.getString("value");
            }
        }
        return "";
    }

    private String getFullFieldNameFromSequence(String seq, String queryRule, CustomAssert customAssert, String LHSRecord) {
        String fieldName = "", sectionNameCustomFieldCreatePayload;
        String fieldEntityType, customFieldName;
        try {
            if (seq.contains("C")) {
                seq = seq.substring(1);
                fieldEntityType = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", seq.split("[+]")[0]);
                customFieldName = fieldEntityType + " " + ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "custom field type", seq.split("[+]")[1]);

                sectionNameCustomFieldCreatePayload = customFieldName;
                customFieldName = customFieldName.concat(" "+seq.split("[+]")[2]);
                //sectionNameCustomFieldCreatePayload = customFieldName.replaceAll("[^A-Za-z ]", "").trim();
                checkCreateCustomField(customFieldName, sectionNameCustomFieldCreatePayload, customAssert);

                fieldName = getEntityParentRelation(customAssert, seq, LHSRecord) + customFieldName;

            } else if (seq.contains("S")) {

                seq = seq.substring(1);
                fieldName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field", seq.split("[+]")[0]+"+"+seq.split("[+]")[1]).split(",")[Integer.parseInt(seq.split("[+]")[2])-1];
                fieldName = getEntityParentRelation(customAssert, seq, LHSRecord) + fieldName;
            } else {
                fieldName = seq;
            }
        } catch (Exception e) {
            logger.info("Exception caught in extracting field name from config file {}", Arrays.toString(e.getStackTrace()));
            customAssert.assertTrue(false, "Exception caught in extracting field name from config file " + Arrays.toString(e.getStackTrace()));
        }

        return fieldName;
    }

    private String getFieldName(String seq){
        String fieldName;
        if(seq.contains("C")) {
            seq = seq.substring(1);
            fieldName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entity type", seq.split("[+]")[0])
                    + " " + ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "custom field type", seq.split("[+]")[1])
                    + " " + seq.split("[+]")[2];
        }
        else if(seq.contains("S")){
            seq = seq.substring(1);
            fieldName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "static field", seq.split("[+]")[0]+"+"+seq.split("[+]")[1]).split(",")[Integer.parseInt(seq.split("[+]")[2])-1];
        }
        else{
            fieldName = seq;
        }

        return fieldName;
    }

    private String getEntityParentRelation(CustomAssert customAssert, String seq, String LHSRecord) {
        try {
            String relationTypeParent = "3", relationTypeEntity = "2";
            if (LHSRecord.contains("165+")) {
                if (seq.contains("67+")) {
                    return ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "rule config", relationTypeParent);
                } else if (seq.contains("64+"))
                    return ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "rule config", relationTypeEntity);
            }
        } catch (Exception e) {
            logger.info("Exception Caught in getEntityParentRelation() {}", Arrays.toString(e.getStackTrace()));
            customAssert.assertTrue(false, "Exception Caught in getEntityParentRelation() " + e.getStackTrace().toString());
        }
        return "";
    }

    private String getCreateRulePayloadSectionName(CustomAssert customAssert, List<JSONObject> fieldData) {
        String payloadForLineItem = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"rule payload","line item");
        String payloadForInvoice = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"rule payload","invoice");
        try {
            for(JSONObject jsonObject : fieldData){
                if(jsonObject.getString("result").equalsIgnoreCase("true")){
                    if(jsonObject.getString("entityName").equalsIgnoreCase("line item"))
                        return payloadForLineItem;
                    else if(jsonObject.getString("entityName").equalsIgnoreCase("invoice"))
                        return payloadForInvoice;
                }
            }
        } catch (Exception e) {
            logger.info("Exception Caught in getCreateRulePayloadSectionName() {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception Caught in getCreateRulePayloadSectionName() " + Arrays.toString(e.getStackTrace()));
        }
        return "";
    }

    @Test(enabled = false) //C76648 - todo priority p2
    public void checkTabListConfigurationOfSelectInvoiceLineItems() {
        CustomAssert customAssert = new CustomAssert();
        String urlId = "445";
        try {
            new AdminHelper().loginWithClientAdminUser();

            ListRendererConfigure listRendererConfigure = new ListRendererConfigure();
            listRendererConfigure.hitListRendererConfigure(urlId, customAssert);
            String response = listRendererConfigure.getListRendererConfigureJsonStr();
            JSONObject jsonObject = new JSONObject(response);

            int filterCount = jsonObject.getJSONArray("filterMetadatas").length();
            int columnCount = jsonObject.getJSONArray("columns").length();
            List<Integer> randomDataToManipulate = getRandomNumbersForList(filterCount);
            logger.info("Random numbers generated are {}", randomDataToManipulate);
            JSONArray jsonObjectForFilter = jsonObject.getJSONArray("filterMetadatas");

            //getting order id for changed order
            int order1, order2;

            //getting random text to be concatenated with name
            String randomTextToBeAdded = String.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 999));
            String changedFilterName1, changedFilterName2;

            //storing ids for future reference
            int idForFilterChanged1, idForFilterChanged2;

            //getting details for first filter field
            order1 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).getInt("order");
            idForFilterChanged1 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).getInt("id");
            changedFilterName1 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).getString("defaultName") + randomTextToBeAdded;
            logger.info("First filter item to be updated is {}", changedFilterName1);

            //getting details for second data field
            order2 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).getInt("order");
            idForFilterChanged2 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).getInt("id");
            changedFilterName2 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).getString("defaultName") + randomTextToBeAdded;
            logger.info("Second filter item to be updated is {}", changedFilterName2);

            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).put("order", order2);
            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).put("order", order1);
            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).put("name", changedFilterName1);
            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).put("name", changedFilterName2);

            jsonObject.put("filterMetadatas", jsonObjectForFilter);

            listRendererConfigure = new ListRendererConfigure();
            listRendererConfigure.updateReportListConfigureResponse(445, jsonObject.toString(), customAssert);

        } catch (Exception e) {
            logger.info("Exception Caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception Caught " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    private boolean checkCreateCustomField(String customFieldName, String invoiceCustomFieldCreatePayloadSectionName, CustomAssert customAssert) {
        try {

            logger.info("Checking custom filed name [{}], payload section name [{}]", customFieldName, invoiceCustomFieldCreatePayloadSectionName);
            logger.info("checking if the required custom field is present or not");
            boolean customFieldFound = checkCustomFieldForFieldType(customAssert, customFieldName); // checking if the required custom field is present or not

            if (!customFieldFound) {
                logger.info("creating the custom field if not present");
                new CustomField().createCustomField(customFieldName, customAssert, invoiceCustomFieldCreatePayloadSectionName); //creating the custom field if not present

                logger.info("checking the custom field after creation");
                customFieldFound = checkCustomFieldForFieldType(customAssert, customFieldName); //checking the custom field after creation

                if (!customFieldFound) {
                    logger.info("Custom field not found and not even created hence terminating [{}]", customFieldName);
                    customAssert.assertTrue(false, "Custom field not found and not even created hence terminating " + customFieldName);
                    customAssert.assertAll();
                } else {
                    logger.info("Custom field found successfully after creating");
                    customAssert.assertTrue(true, "Custom field found successfully after creating");
                    return true;
                }
            } else {
                logger.info("Custom field is already present");
                customAssert.assertTrue(true, "Custom field is already present");
                return true;
            }
        } catch (Exception e) {
            logger.info("Exception caught while creating rule [{}]", e.toString());
            customAssert.assertTrue(false, "Exception caught while creating custom field " + e.toString());
            return false;
        }
        return false;
    }

    private boolean checkCustomFieldForFieldType(CustomAssert customAssert, String customFieldName) {

        try {
            Document html = getHtmlFromURLClientAdmin(customAssert);
            if (html == null)
                return false;
            int htmlLength = html.getElementById("_title_pl_com_sirionlabs_model_MasterGroup_id").getElementsByTag("a").size();
            String temp;
            for (int index = 0; index < htmlLength; index++) {
                temp = html.getElementById("_title_pl_com_sirionlabs_model_MasterGroup_id").getElementsByTag("a").get(index).getElementsByTag("a").text();
                if (temp.equalsIgnoreCase(customFieldName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.info("Exception caught while parsing html document");
            customAssert.assertTrue(false, "Exception caught while parsing html document");
        }
        return false;
    }

    private Document getHtmlFromURLClientAdmin(CustomAssert customAssert) {

        try {
            return new CustomField().getHtmlFromListing(customAssert);
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception caught while fetching html");
            logger.info("Exception caught while fetching html");
        }

        return null;
    }

    private List<Integer> getRandomNumbersForList(int sizeofList) {
        int lowerLimit = RandomNumbers.getRandomNumberWithinRangeIndex(0, sizeofList - 1);
        int upperLimit = RandomNumbers.getRandomNumberWithinRangeIndex(0, sizeofList);
        if (lowerLimit == upperLimit)
            getRandomNumbersForList(sizeofList);
        return Arrays.asList(lowerLimit, upperLimit);
    }

    private void deleteNewEntity(String entityName, int entityId) {


        try {
            logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
            Show showObj = new Show();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            showObj.hitShow(entityTypeId, entityId);
            if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
                JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
                String prefix = "{\"body\":{\"data\":";
                String suffix = "}}";
                String showBodyStr = jsonObj.getJSONObject("body").getJSONObject("data").toString();
                String deletePayload = prefix + showBodyStr + suffix;

                logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
                Delete deleteObj = new Delete();
                deleteObj.hitDelete(entityName, deletePayload);
                String deleteJsonStr = deleteObj.getDeleteJsonStr();
                jsonObj = new JSONObject(deleteJsonStr);
                String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.trim().equalsIgnoreCase("success"))
                    logger.info("Entity having Id {} is deleted Successfully.", entityId);
            }
        } catch (Exception e) {
            logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
        }
    }

    private boolean isChargesCreated(int serviceDataId) {


        int chargesTabId = 309; // hardcoded value @todo

        logger.info("Checking whether data under Charges tab has/have been created and visible for serviceData" + serviceDataId);
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

    private void findCustomFieldNodeFromJson(JSONObject jsonObject, String customField){

        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()){
            nextKey = iterator.next();
            if(nextKey.equalsIgnoreCase("label")){
                if(jsonObject.get("label").toString().equalsIgnoreCase(customField)) {
                    customFieldDYN = jsonObject.get("name").toString();
                    jsonObject.get("name").toString();
                }
            }
            else{
                if(jsonObject.get(nextKey) instanceof JSONObject){
                    findCustomFieldNodeFromJson((JSONObject) jsonObject.get(nextKey), customField);
                }
                else if(jsonObject.get(nextKey) instanceof JSONArray)
                    findCustomFieldNodeFromJson((JSONArray) jsonObject.get(nextKey), customField);
            }
        }
    }

    private void findCustomFieldNodeFromJson(JSONArray jsonArray, String customField){

        for(int index =0;index<jsonArray.length();index++){
            if(jsonArray.get(index) instanceof  JSONArray)
                findCustomFieldNodeFromJson((JSONArray) jsonArray.get(index), customField);
            else if(jsonArray.get(index) instanceof  JSONObject)
                findCustomFieldNodeFromJson((JSONObject) jsonArray.get(index), customField);
        }
    }

    private void findStaticFieldNodeFromJson(JSONObject jsonObject, String customFieldName , String value ){

        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()){
            nextKey = iterator.next();
            if(nextKey.equalsIgnoreCase(customFieldName)){
                if(jsonObject.getJSONObject(customFieldName).get("displayValues").toString().equalsIgnoreCase(value)) {
                    staticFieldFound = true;
                }
            }
            else{
                if(jsonObject.get(nextKey) instanceof JSONObject){
                    findStaticFieldNodeFromJson((JSONObject) jsonObject.get(nextKey),customFieldName , value);
                }
                else if(jsonObject.get(nextKey) instanceof JSONArray)
                    findStaticFieldNodeFromJson((JSONArray) jsonObject.get(nextKey), customFieldName , value);
            }
        }
    }

    private void findStaticFieldNodeFromJson(JSONArray jsonArray, String customFieldName , String value){

        for(int index =0;index<jsonArray.length();index++){
            if(jsonArray.get(index) instanceof  JSONArray)
                findStaticFieldNodeFromJson((JSONArray) jsonArray.get(index), customFieldName,value);
            else if(jsonArray.get(index) instanceof  JSONObject)
                findStaticFieldNodeFromJson((JSONObject) jsonArray.get(index), customFieldName,value);
        }
    }

    private void findStaticJSONNodeFromJson(JSONObject jsonObject, String nodeName){

        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()){
            nextKey = iterator.next();
            if(nextKey.equalsIgnoreCase(nodeName)){
                staticContent = jsonObject.getJSONObject(nodeName);
            }
            else{
                if(jsonObject.get(nextKey) instanceof JSONObject){
                    findStaticJSONNodeFromJson((JSONObject) jsonObject.get(nextKey),nodeName);
                }
                else if(jsonObject.get(nextKey) instanceof JSONArray)
                    findStaticJSONNodeFromJson((JSONArray) jsonObject.get(nextKey), nodeName);
            }
        }
    }

    private void findStaticJSONNodeFromJson(JSONArray jsonArray, String nodeName){

        for(int index =0;index<jsonArray.length();index++){
            if(jsonArray.get(index) instanceof  JSONArray)
                findStaticJSONNodeFromJson((JSONArray) jsonArray.get(index), nodeName);
            else if(jsonArray.get(index) instanceof  JSONObject)
                findStaticJSONNodeFromJson((JSONObject) jsonArray.get(index), nodeName);
        }
    }

    private void updateWorkflowDate(CustomAssert customAssert){
        boolean jobDOne = false;

        String query = "update work_flow set date_created = now() where relation_id=1024 and client_id = 1002";
        try{
            jobDOne = new PostgreSQLJDBC().updateDBEntry(query);
        }
        catch (Exception e){
            logger.error("Exception occurred in updating workflow in DB");
            //customAssert.assertTrue(false,"Exception occurred in updating workflow in DB");
        }

        if(jobDOne){
            logger.info("Updating the workflow date completed");
            customAssert.assertTrue(true,"Updating the workflow date completed successfully");
        }
        else{
            logger.error("Updating the workflow date failed");
            //customAssert.assertTrue(false,"Updating the workflow date failed");
        }
    }

    private void callSetRuleInactive(CustomAssert customAssert){
        try{
            String invoiceCreateRuleSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"rule payload","line item");
            Map<String,String> payloadMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFilePath"),ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFileName"),invoiceCreateRuleSectionName);
            String supplierJsonStringFromPayloadFile = payloadMap.get("supplier");
            JSONArray jsonArray = new JSONArray(supplierJsonStringFromPayloadFile);
            String supplierName = jsonArray.getJSONObject(0).getString("name");
            new InvoiceCreateRule().setRuleInactiveForSupplier(supplierName,customAssert);
        }
        catch (Exception e){
            logger.info("Exception caught in callSetRuleInactive() {}", (Object) e.getStackTrace());
        }
    }
}
