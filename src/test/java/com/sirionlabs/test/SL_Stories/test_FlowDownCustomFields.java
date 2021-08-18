package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class test_FlowDownCustomFields {

    private final static Logger logger = LoggerFactory.getLogger(test_FlowDownCustomFields.class);
    private static String configFilePath;
    private static String configFileName;
    private String entityIdConfigFilePath;
    private String entityIdMappingFileName;


    private int contractEntityTypeId;
    private int supplierEntityTypeId;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestFlowDownValuesConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestFlowDownValuesConfigFileName");
        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        contractEntityTypeId = 61;
        supplierEntityTypeId = 1;
    }

    @DataProvider(name = "childEntitiesToTestForContract", parallel = false)
    public Object[][] childEntitiesToTestForContract() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child_entities_to_test_for_contract").split(",");


        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "childEntitiesToTestForSupplier", parallel = false)
    public Object[][] childEntitiesToTestForSupplier() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child_entities_to_test_for_suppliers").split(",");


        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    //Passed
    @Test(enabled = true)
    public void TestUpdateParentFieldContract(){

        CustomAssert customAssert = new CustomAssert();
        Edit edit = new Edit();

        try{

            String parentContractId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_id_for_contract");

            if(parentContractId == null){
                throw new SkipException("Parent Contract Id is null thus skipping test");

            }else if(parentContractId.equalsIgnoreCase("")){
                throw new SkipException("Parent Contract Id is empty thus skipping test");
            }else {

                String payLoadFilePath = "src//test//resources//TestConfig//FlowDownValues";
                String payLoadFileName = "Edit_Contract_Payload.json";

                String payloadForEdit = new FileUtils().getDataInFile(payLoadFilePath + "//" + payLoadFileName);

                String[] fieldTypes = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_type_for_contract").split(",");
                 String[] fieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_id_for_contract").split(",");

                String textFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_textfield");
                List<String> singleSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_singleselect").split(","));
                List<String> multiSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_multiselect").split(","));
                List<String> singleSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_singleselect_suppliers").split(","));

                List<String> multiSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_multiselect_suppliers").split(","));

                String editPayload = editPayloadForSettingCustomFields(payloadForEdit,fieldTypes,
                        fieldIds,textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

                String editResponse = edit.hitEdit("contracts",editPayload);

                if(!editResponse.contains("success")){
                    customAssert.assertTrue(false,"Parent Field For Contract has been updated unsuccessfully");
                }

                sleepThread();
            }

        }catch (Exception e){
            logger.error("Exception while Updating Parent Field of Contract " + e.getMessage());
            customAssert.assertTrue(false,"Exception while Updating Parent Field of Contract " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //Passed
    @Test(dataProvider = "childEntitiesToTestForContract",dependsOnMethods = "TestUpdateParentFieldContract",enabled = true)
//    @Test(dataProvider = "childEntitiesToTestForContract",enabled = true)
    public void TestFlowDownValuesParentContract(String childEntity) {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();
        int entityId;
        String showResponse;

        try{

            logger.info("Testing Flow Down for Child of Parent Contract");
            String entityIdToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity id for parent contracts",childEntity);

            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, childEntity, "entity_type_id"));

            if(entityIdToTest == null){
                throw new SkipException("Child Entity Id is null thus skipping test");

            }else if(entityIdToTest.equalsIgnoreCase("")){
                throw new SkipException("Child Entity Id is empty thus skipping test");
            }else {

                logger.info("Testing Flow Down for Child " + entityIdToTest);
                entityId = Integer.parseInt(entityIdToTest);

                show.hitShowVersion2(entityTypeId,entityId);
                showResponse = show.getShowJsonStr();

                if(showResponse.contains("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore")){
                    throw new SkipException("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore");
                }

                String textFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_textfield");
                List<String> singleSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_singleselect").split(","));
                List<String> multiSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_multiselect").split(","));
                List<String> singleSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_singleselect_suppliers").split(","));
                List<String> multiSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit contract","parent_field_value_for_contract_multiselect_suppliers").split(","));
                String field_types_inorder = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child field id for parent contract","child_field_types_inorder");

                Boolean validationStatus =  validateFlowDownValuesEntity("contract",childEntity, entityId, field_types_inorder,
                        textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

                if(!validationStatus){
                    customAssert.assertTrue(false,"Flow Down validation unsuccessful");
                }

                String filterJson = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filter json for contract edit",childEntity);
                String selectedColumns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"selected columns for contract edit",childEntity);
//                String selectedColumns = "{\"columnId\":12142,\"columnQueryName\":\"id\"},{\"columnId\":110367,\"columnQueryName\":\"dyn102122\"}";

                String listingPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":" + filterJson + "}" + ",\"selectedColumns\": [" + selectedColumns + "]" + "}";

                int listId = ConfigureConstantFields.getListIdForEntity(childEntity.trim());

                String listingResponse = getListingData(listId,listingPayload,customAssert);

                Boolean listingValidationStatus =  validateFlowDownValuesEntityListingPage(listingResponse,"contract",childEntity,entityId,field_types_inorder,
                        textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

                if(!listingValidationStatus){
                    customAssert.assertTrue(false,"Flow Down validation unsuccessful for listing page");
                }

            }

        }catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e){
            logger.error("Exception while validating Flow Down Of Values From Parent Contract to child entities " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Flow Down Of Values From Parent Contract to child entities " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestUpdateParentFieldContractNullValues(){

        CustomAssert customAssert = new CustomAssert();
        Edit edit = new Edit();

        try{

            String parentContractId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_id_for_contract");

            if(parentContractId == null){
                throw new SkipException("Parent Contract Id is null thus skipping test");

            }else if(parentContractId.equalsIgnoreCase("")){
                throw new SkipException("Parent Contract Id is empty thus skipping test");
            }else {

                String payLoadFilePath = "src//test//resources//TestConfig//FlowDownValues";
                String payLoadFileName = "Edit_Contract_Payload_NullValues.json";

                String payloadForEdit = new FileUtils().getDataInFile(payLoadFilePath + "//" + payLoadFileName);

                String editResponse = edit.hitEdit("contracts",payloadForEdit);

                if(!editResponse.contains("success")){
                    customAssert.assertTrue(false,"Parent Field For Contract has been updated unsuccessfully");
                }

                sleepThread();
            }

        }catch (Exception e){
            logger.error("Exception while Updating Parent Field of Contract " + e.getMessage());
            customAssert.assertTrue(false,"Exception while Updating Parent Field of Contract " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //Passed
    @Test(dataProvider = "childEntitiesToTestForContract",dependsOnMethods = "TestUpdateParentFieldContractNullValues",enabled = false)
    public void TestFlowDownValuesParentContractNullValues(String childEntity) {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();
        int entityId;
        String showResponse;

        try{

            logger.info("Testing Flow Down for Child of Parent Contract");
            String entityIdToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity id for parent contracts",childEntity);

            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, childEntity, "entity_type_id"));

            if(entityIdToTest == null){
                throw new SkipException("Child Entity Id is null thus skipping test");

            }else if(entityIdToTest.equalsIgnoreCase("")){
                throw new SkipException("Child Entity Id is empty thus skipping test");
            }else {

                logger.info("Testing Flow Down for Child " + entityIdToTest);
                entityId = Integer.parseInt(entityIdToTest);

                show.hitShowVersion2(entityTypeId,entityId);
                showResponse = show.getShowJsonStr();

                if(showResponse.contains("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore")){
                    throw new SkipException("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore");
                }

                String textFieldValue = null;
                List<String> singleSelectValues = null;
                List<String> multiSelectValues = null;
                List<String> singleSelectValuesSupplier = null;
                List<String> multiSelectValuesSupplier = null;

                String field_types_inorder = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child field id for parent contract","child_field_types_inorder");

                Boolean validationStatus =  validateFlowDownValuesEntity("contract",childEntity, entityId, field_types_inorder,
                        textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

                if(!validationStatus){
                    customAssert.assertTrue(false,"Flow Down validation unsuccessful");
                }

            }

        }catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e){
            logger.error("Exception while validating Flow Down Of Values From Parent Contract to child entities " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Flow Down Of Values From Parent Contract to child entities " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestUpdateParentFieldSupplierNullValues(){

        CustomAssert customAssert = new CustomAssert();
        Edit edit = new Edit();

        try{

            String parentSupplierId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_id_for_suppliers");

            if(parentSupplierId == null){
                throw new SkipException("Parent Supplier Id is null thus skipping test");

            }else if(parentSupplierId.equalsIgnoreCase("")){
                throw new SkipException("Parent Supplier Id is empty thus skipping test");
            }else {

                String payLoadFilePath = "src//test//resources//TestConfig//FlowDownValues";
                String payLoadFileName = "Edit_Supplier_Payload_NullValues.json";

                String payloadForEdit = new FileUtils().getDataInFile(payLoadFilePath + "//" + payLoadFileName);

                String editResponse = edit.hitEdit("suppliers",payloadForEdit);

                if(!editResponse.contains("success")){
                    customAssert.assertTrue(false,"Parent Field For Supplier has been updated unsuccessfully");
                }

                sleepThread();
            }

        }catch (Exception e){
            logger.error("Exception while Updating Parent Field of Contract " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //Passed
    @Test(dataProvider = "childEntitiesToTestForSupplier",dependsOnMethods = "TestUpdateParentFieldSupplierNullValues",enabled = false)
    public void TestFlowDownValuesParentSupplierNullValues(String childEntity) {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();
        int entityId;
        String showResponse;

        try{

            logger.info("Testing Flow Down for Child of Parent Contract");
            String entityIdToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity id for parent contracts",childEntity);

            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, childEntity, "entity_type_id"));

            if(entityIdToTest == null){
                throw new SkipException("Child Entity Id is null thus skipping test");

            }else if(entityIdToTest.equalsIgnoreCase("")){
                throw new SkipException("Child Entity Id is empty thus skipping test");
            }else {

                logger.info("Testing Flow Down for Child " + entityIdToTest);
                entityId = Integer.parseInt(entityIdToTest);

                show.hitShowVersion2(entityTypeId,entityId);
                showResponse = show.getShowJsonStr();

                if(showResponse.contains("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore")){
                    throw new SkipException("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore");
                }

                String textFieldValue = null;
                List<String> singleSelectValues = null;
                List<String> multiSelectValues = null;
                List<String> singleSelectValuesSupplier = null;
                List<String> multiSelectValuesSupplier = null;

                String field_types_inorder = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child field id for parent contract","child_field_types_inorder");

                Boolean validationStatus =  validateFlowDownValuesEntity("contract",childEntity, entityId, field_types_inorder,
                        textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

                if(!validationStatus){
                    customAssert.assertTrue(false,"Flow Down validation unsuccessful");
                }

            }

        }catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e){
            logger.error("Exception while validating Flow Down Of Values From Parent Supplier to child entities " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Flow Down Of Values From Parent Supplier to child entities " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestUpdateParentFieldSupplier(){

        CustomAssert customAssert = new CustomAssert();
        Edit edit = new Edit();

        try{

            String parentSupplierId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_id_for_suppliers");

            if(parentSupplierId == null){
                throw new SkipException("Parent Supplier Id is null thus skipping test");

            }else if(parentSupplierId.equalsIgnoreCase("")){
                throw new SkipException("Parent Supplier Id is empty thus skipping test");
            }else {

                String[] fieldTypes = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_type_for_suppliers").split(",");
                String[] fieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_id_for_suppliers").split(",");

                String payLoadFilePath = "src//test//resources//TestConfig//FlowDownValues";
                String payLoadFileName = "Edit_Supplier_Payload.json";

                String payloadForEdit = new FileUtils().getDataInFile(payLoadFilePath + "//" + payLoadFileName);

                String textFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_textfield");
                List<String> singleSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_singleselect").split(","));
                List<String> multiSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_multiselect").split(","));

                List<String> multiSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_multiselect_suppliers").split(","));
                List<String> singleSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_singleselect_suppliers").split(","));

                String editPayload = editPayloadForSettingCustomFields(payloadForEdit,fieldTypes,
                        fieldIds,textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

                String editResponse = edit.hitEdit("suppliers",editPayload);

                if(!editResponse.contains("success")){
                    customAssert.assertTrue(false,"Parent Field For Supplier has been updated unsuccessfully");
                }

                sleepThread();
            }

        }catch (Exception e){
            logger.error("Exception while Updating Parent Field of Contract " + e.getMessage());
        }

        customAssert.assertAll();

    }

//                @Test(enabled = true,dataProvider = "childEntitiesToTestForSupplier")
    @Test(enabled = true,dataProvider = "childEntitiesToTestForSupplier",dependsOnMethods = "TestUpdateParentFieldSupplier")
    public void TestFlowDownValuesParentSupplier(String childEntity) {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();
        int entityId;
        String showResponse;

        try{

            logger.info("Testing Flow Down for Child of Parent Contract");
            String entityIdToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity id for parent suppliers",childEntity);

            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, childEntity, "entity_type_id"));

            if(entityIdToTest == null){
                throw new SkipException("Child Entity Id is null thus skipping test");

            }else if(entityIdToTest.equalsIgnoreCase("")){
                throw new SkipException("Child Entity Id is empty thus skipping test");
            }else {

                logger.info("Testing Flow Down for Child " + entityIdToTest);
                entityId = Integer.parseInt(entityIdToTest);

                show.hitShowVersion2(entityTypeId,entityId);
                showResponse = show.getShowJsonStr();

                if(showResponse.contains("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore")){
                    throw new SkipException("Either You Do Not Have The Required Permissions Or Requested Page Does Not Exist Anymore");
                }

                String textFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_textfield");
                List<String> singleSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_singleselect").split(","));
                List<String> multiSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_multiselect").split(","));
                List<String> singleSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_singleselect_suppliers").split(","));
                List<String> multiSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"normal edit suppliers","parent_field_value_for_suppliers_multiselect_suppliers").split(","));

                String field_types_inorder = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child field id for parent suppliers","child_field_types_inorder");

                Boolean validationStatus =  validateFlowDownValuesEntity("suppliers",childEntity, entityId, field_types_inorder,
                        textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

                if(!validationStatus){
                    customAssert.assertTrue(false,"Flow Down validation unsuccessful");
                }

            }

        }catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e){
            logger.error("Exception while validating Flow Down Of Values From Parent Contract to child entities " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Flow Down Of Values From Parent Contract to child entities " + e.getMessage());
        }

        customAssert.assertAll();

    }

    //Done
    @Test(enabled = false)
    public void TestFlowDownBulkEditParentContract(){

        CustomAssert customAssert = new CustomAssert();

        try {
            String payLoadFilePath = "src//test//resources//TestConfig//FlowDownValues";
            String payLoadFileName = "BulkEdit_Contract_Payload.json";
            String payloadForBulkEdit = new FileUtils().getDataInFile(payLoadFilePath + "//" + payLoadFileName);

            String parentContractIdsForBulkEdit = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entity id for bulk edit for parent contract", "contracts");

            if (parentContractIdsForBulkEdit == null || parentContractIdsForBulkEdit.equalsIgnoreCase("")) {
                throw new SkipException("Contract Ids for Bulk Edit is Null");
            }

            String[] recordsToEditArray = parentContractIdsForBulkEdit.split(",");

            BulkeditEdit bulkeditEdit = new BulkeditEdit();
//
            String[] fieldTypes = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_type_for_contract").split(",");
            String[] fieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_id_for_contract").split(",");
//
            String textFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit contract","parent_field_value_for_contract_textfield");
            List<String> singleSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit contract","parent_field_value_for_contract_singleselect").split(","));
            List<String> multiSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit contract","parent_field_value_for_contract_multiselect").split(","));
            List<String> singleSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit contract","parent_field_value_for_contract_singleselect_suppliers").split(","));
            List<String> multiSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit contract","parent_field_value_for_contract_multiselect_suppliers").split(","));
//

            String bulkEditCreatePayload = createBulkEditPayloadForSettingCustomFields(recordsToEditArray,payloadForBulkEdit,fieldTypes,
                    fieldIds,textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);
//Flow
            bulkeditEdit.hitBulkeditEdit(contractEntityTypeId, bulkEditCreatePayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {
//
                customAssert.assertTrue(false, "bulkEditResponse is response is not 200");
            }

            sleepThread();

            //Verify Changes in Show Page
            Boolean validationStatus = validateFlowDownValues("contract",textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

            if(!validationStatus){
                logger.error("Flow down validation unsuccessful");
                customAssert.assertTrue(false,"Flow down validation unsuccessful");
            }


        }catch (Exception e){
            logger.error("Exception while Bulk Edit Parent Contract");
            customAssert.assertTrue(false,"Exception while Bulk Edit Parent Contract");
        }

        customAssert.assertAll();
    }

    //Done
    @Test(enabled = false)
    public void TestFlowDownBulkEditParentSupplier(){

        CustomAssert customAssert = new CustomAssert();

        try {
            String payLoadFilePath = "src//test//resources//TestConfig//FlowDownValues";
            String payLoadFileName = "BulkEdit_Supplier_Payload.json";
            String payloadForBulkEdit = new FileUtils().getDataInFile(payLoadFilePath + "//" + payLoadFileName);

            String parentSupplierIdsForBulkEdit = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entity id for bulk edit for parent suppliers", "suppliers");

            if (parentSupplierIdsForBulkEdit == null || parentSupplierIdsForBulkEdit.equalsIgnoreCase("")) {
                throw new SkipException("Contract Ids for Bulk Edit is Null");
            }

            String[] recordsToEditArray = parentSupplierIdsForBulkEdit.split(",");

            BulkeditEdit bulkeditEdit = new BulkeditEdit();

            String[] fieldTypes = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_type_for_suppliers").split(",");
            String[] fieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent_field_id_for_suppliers").split(",");

            String textFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit suppliers","parent_field_value_for_suppliers_textfield");
            List<String> singleSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit suppliers","parent_field_value_for_suppliers_singleselect").split(","));
            List<String> multiSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit suppliers","parent_field_value_for_suppliers_multiselect").split(","));
            List<String> singleSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit suppliers","parent_field_value_for_suppliers_singleselect_suppliers").split(","));
            List<String> multiSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit suppliers","parent_field_value_for_suppliers_multiselect_suppliers").split(","));


            String bulkEditCreatePayload = createBulkEditPayloadForSettingCustomFields(recordsToEditArray,payloadForBulkEdit,fieldTypes,
                    fieldIds,textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

            bulkeditEdit.hitBulkeditEdit(supplierEntityTypeId, bulkEditCreatePayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {

                customAssert.assertTrue(false, "bulkEditResponse is response is not 200");
            }

            sleepThread();
            //Verify Changes in Show Page
            Boolean validationStatus = validateFlowDownValues("suppliers",textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

            if(!validationStatus){
                logger.error("Flow down validation unsuccessful");
                customAssert.assertTrue(false,"Flow down validation unsuccessful");
            }

        }catch (Exception e){
            logger.error("Exception while Bulk Edit Parent Supplier");
            customAssert.assertTrue(false,"Exception while Bulk Edit Parent Supplier");
        }

        customAssert.assertAll();
    }

    //Done
    @Test(enabled = false)
    public void TestFlowDownBulkUpdateParentContract(){

        CustomAssert customAssert = new CustomAssert();

        try{
            String parentContractForBulkUpdate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "parent_id_for_contract");

            if (parentContractForBulkUpdate == null || parentContractForBulkUpdate.equalsIgnoreCase("")) {
                throw new SkipException("Contract Ids for Bulk Update is Null");
            }

            String downloadFilePath = "src\\test\\resources\\TestConfig\\FlowDownValues";
            String downloadFileName = "BulkUpdateForContract.xlsm";
            int bulkUpdateTemplateId = 1025;

            Boolean downloadStatus = BulkTemplate.downloadBulkUpdateTemplate(downloadFilePath, downloadFileName, bulkUpdateTemplateId, contractEntityTypeId, parentContractForBulkUpdate);

            if(downloadStatus == false){
                customAssert.assertTrue(false,"Unable to download Bulk Update Template for contract id " + parentContractForBulkUpdate);
                customAssert.assertAll();
            }
            String sheetName = "Contract 10";

            String textFieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","parent_field_value_for_contract_textfield");
            String singleSelectValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","single_select_value");
            String multiSelectValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","multi_select_value");
            String singleSelectSupplierValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","single_select_suppliers_value");
            String multiSelectSupplierValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","multi_select_suppliers_value");

            try {
                XLSUtils.updateColumnValue(downloadFilePath, downloadFileName, sheetName, 6, 11, textFieldValue);
                XLSUtils.updateColumnValue(downloadFilePath, downloadFileName, sheetName, 6, 5, singleSelectValue);
                XLSUtils.updateColumnValue(downloadFilePath, downloadFileName, sheetName, 6, 4, multiSelectValue);
                XLSUtils.updateColumnValue(downloadFilePath, downloadFileName, sheetName, 6, 9, singleSelectSupplierValue);
                XLSUtils.updateColumnValue(downloadFilePath, downloadFileName, sheetName, 6, 7, multiSelectSupplierValue);

            }catch (Exception e){
                logger.error("Exception while updating excel file");
                customAssert.assertTrue(false,"Exception while updating excel file");
            }

            String bulkUpdateResponse = BulkTemplate.uploadBulkUpdateTemplate(downloadFilePath,downloadFileName,contractEntityTypeId,bulkUpdateTemplateId);

            if(!bulkUpdateResponse.contains("200")){

                customAssert.assertTrue(false,"Bulk Update unsuccessful");
                customAssert.assertAll();
            }

            sleepThread();

            List<String> singleSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","parent_field_value_for_contract_singleselect").split(","));
            List<String> multiSelectValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","parent_field_value_for_contract_multiselect").split(","));
            List<String> singleSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","parent_field_value_for_contract_singleselect_suppliers").split(","));
            List<String> multiSelectValuesSupplier = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update contract","parent_field_value_for_contract_multiselect_suppliers").split(","));

            Boolean validationStatus = validateFlowDownValues("contract",textFieldValue,singleSelectValues,multiSelectValues,singleSelectValuesSupplier,multiSelectValuesSupplier,customAssert);

            if(!validationStatus){
                logger.error("Flow down validation unsuccessful");
                customAssert.assertTrue(false,"Flow down validation unsuccessful");
            }

        }catch (Exception e){
            logger.error("Exception while Testing bulk upload for contract " + e.getMessage());
            customAssert.assertTrue(false,"Exception while Testing bulk upload for contract " + e.getMessage());
        }

        customAssert.assertAll();

    }


    private Boolean validateFlowDownValues(String parentEntity ,
                                           String textFieldValue,
                                           List<String> singleSelectValues,
                                           List<String> multiSelectValues,
                                           List<String> singleSelectValuesSupplier,
                                           List<String> multiSelectValuesSupplier,
                                           CustomAssert customAssert){

        logger.info("Validating flow down of values for Bulk Edit");
        Boolean validationStatus = true;

        try{
            Show show = new Show();
            JSONObject showResponseJson;
            String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child_entities_to_test_for_" + parentEntity).split(",");

            int entityId;
            int entityTypeId;

            String  entityIdString;
            String showResponse;

            String field_types_inorder = "";

            if(parentEntity.equals("suppliers")){

                field_types_inorder = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child field id for parent suppliers","child_field_types_inorder");
            }else if(parentEntity.equals("contract")){

                field_types_inorder = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child field id for parent contract","child_field_types_inorder");
            }

            String[] field_types_inorder_array = field_types_inorder.split(",");

            for(String entity : entitiesToTest) {

                try {
                    entityIdString = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entity id for bulk edit for parent " + parentEntity, entity);

                    if (entityIdString == null || entityIdString.equalsIgnoreCase("")) {
                        logger.warn("Child Entity Id is null for entity " + entity);
                    } else {

                        entityId = Integer.parseInt(entityIdString);
                        entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entity, "entity_type_id"));
                        show.hitShowVersion2(entityTypeId, entityId);
                        showResponse = show.getShowJsonStr();

                        showResponseJson = new JSONObject(showResponse);

                        String[] childFieldId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "child field id for parent " + parentEntity, entity).split(",");

                        if (childFieldId.length != field_types_inorder_array.length) {
                            logger.warn("Configuration Incorrect for Field Types Length and Child Field Id Length for child entity " + entity);
                            continue;
                        }
                        JSONObject dynamicFieldJson;
                        for (int i = 0; i < field_types_inorder_array.length; i++) {

                            try {
                                dynamicFieldJson = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(childFieldId[i]);

                                if (field_types_inorder_array[i].equals("textField")) {

                                    String actualTextValue = dynamicFieldJson.get("values").toString();

                                    if (!actualTextValue.equalsIgnoreCase(textFieldValue)) {

                                        logger.error("Expected and Actual value of Text Field does not match for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Expected and Actual value of Text Field does not match for childEntity " + entity + " entity Id " + entityId);
                                        validationStatus = false;
                                    }

                                } else if (field_types_inorder_array[i].equals("singleSelect")) {

                                    String actualSingleSelect;
                                    try {
                                        actualSingleSelect = dynamicFieldJson.getJSONObject("values").get("name").toString();

                                        if (!singleSelectValues.contains(actualSingleSelect)) {

                                            logger.error("Expected and Actual value of Single Select does not match for childEntity " + entity + " entity Id " + entityId);
                                            customAssert.assertTrue(false, "Expected and Actual value of Single Select does not match for childEntity " + entity + " entity Id " + entityId);
                                            validationStatus = false;
                                        }
                                    } catch (Exception e) {

                                        logger.error("Exception while fetching values Json Object for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Exception while fetching values Json Object for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Value is null or not present for the field");
                                        validationStatus = false;
                                    }
                                } else if (field_types_inorder_array[i].equals("multiSelect")) {

                                    JSONArray valuesJsonArray;
                                    try {
                                        valuesJsonArray = dynamicFieldJson.getJSONArray("values");
                                        String actualOptionValue;

                                        for (int j = 0; j < valuesJsonArray.length(); j++) {

                                            actualOptionValue = valuesJsonArray.getJSONObject(j).get("name").toString();
                                            if (!multiSelectValues.contains(actualOptionValue)) {
                                                logger.error("Expected and Actual value of Multi Select does not match for childEntity " + entity + " entity Id " + entityId);
                                                customAssert.assertTrue(false, "Expected and Actual value of Multi Select does not match for childEntity " + entity + " entity Id " + entityId);
                                                validationStatus = false;
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("Exception while fetching values Json Array for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Exception while fetching values Json Array for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Value is null or not present for the field");
                                        validationStatus = false;
                                    }

                                } else if (field_types_inorder_array[i].equals("multiSelectSupplier")) {

                                    JSONArray valuesJsonArray;
                                    try {
                                        valuesJsonArray = dynamicFieldJson.getJSONArray("values");
                                        String actualOptionValue;

                                        for (int j = 0; j < valuesJsonArray.length(); j++) {

                                            actualOptionValue = valuesJsonArray.getJSONObject(j).get("name").toString();
                                            if (!multiSelectValuesSupplier.contains(actualOptionValue)) {
                                                logger.error("Expected and Actual value of Multi Select Supplier Field does not match for childEntity " + entity + " entity Id " + entityId);
                                                customAssert.assertTrue(false, "Expected and Actual value of Multi Select Supplier does not match for childEntity " + entity + " entity Id " + entityId);
                                                validationStatus = false;
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("Exception while fetching values Json Array for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Exception while fetching values Json Array for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Value is null or not present for the field");
                                        validationStatus = false;
                                    }

                                } else if (field_types_inorder_array[i].equals("singleSelectSupplier")) {

                                    String actualSingleSelect;
                                    try {
                                        actualSingleSelect = dynamicFieldJson.getJSONObject("values").get("name").toString();

                                        if (!singleSelectValuesSupplier.contains(actualSingleSelect)) {

                                            logger.error("Expected and Actual value of Single Select Supplier does not match for childEntity " + entity + " entity Id " + entityId);
                                            customAssert.assertTrue(false, "Expected and Actual value of Single Select Supplier does not match for childEntity " + entity + " entity Id " + entityId);
                                            validationStatus = false;
                                        }
                                    } catch (Exception e) {

                                        logger.error("Exception while fetching values Json Object for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Exception while fetching values Json Object for childEntity " + entity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Value is null or not present for the field");
                                        validationStatus = false;
                                    }
                                }

                            } catch (Exception e) {
                                logger.warn("Exception while fetching field id " + childFieldId[i] + " from show page response of " + entity + " child entity id " + entityId + e.getMessage());
                                customAssert.assertTrue(false, "Exception while fetching field id " + childFieldId[i] + " from show page response of " + entity + " child entity id " + entityId + e.getMessage());
                                validationStatus = false;
                            }

                        }
                    }
                }catch (Exception e){
                    logger.error("Exception while validating flow for entity " + entity + " " + e.getStackTrace());
                    customAssert.assertTrue(false,"Exception while validating flow for entity " + entity + " " + e.getStackTrace());
                    validationStatus = false;
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating Flow Down For Values for Bulk Edit" + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Flow Down For Values for Bulk Edit" + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;

    }

    private Boolean validateFlowDownValuesEntity(String parentEntity,
                                                 String childEntity ,
                                                 int entityId,
                                                 String field_types_inorder,
                                                 String textFieldValue,
                                                 List<String> singleSelectValues,
                                                 List<String> multiSelectValues,
                                                 List<String> singleSelectValuesSupplier,
                                                 List<String> multiSelectValuesSupplier,
                                                 CustomAssert customAssert){

        logger.info("Validating flow down of values for parent entity " + parentEntity);
        Boolean validationStatus = true;

        try{
            Show show = new Show();
            JSONObject showResponseJson;

            int entityTypeId;

            String  entityIdString;
            String showResponse;

            String[] field_types_inorder_array = field_types_inorder.split(",");

            entityIdString = String.valueOf(entityId);

            if(entityIdString == null || entityIdString.equalsIgnoreCase("")){
                logger.warn("Child Entity Id is null for entity " + childEntity);
            }else {

                entityId = Integer.parseInt(entityIdString);
                entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, childEntity, "entity_type_id"));
                show.hitShowVersion2(entityTypeId, entityId);
                showResponse = show.getShowJsonStr();

                showResponseJson = new JSONObject(showResponse);

                String[] childFieldId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "child field id for parent " + parentEntity, childEntity).split(",");

                if (childFieldId.length != field_types_inorder_array.length) {
                    logger.warn("Configuration Incorrect for Field Types Length and Child Field Id Length for child entity " + childEntity);
                }
                JSONObject dynamicFieldJson;
                for (int i = 0; i < field_types_inorder_array.length; i++) {

                    try {
                        dynamicFieldJson = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(childFieldId[i]);

                        if (field_types_inorder_array[i].equals("textField")) {

                            String actualTextValue = dynamicFieldJson.get("values").toString();
                            if(textFieldValue == null){

                                if(!actualTextValue.equalsIgnoreCase("")) {
                                    logger.error("Expected value is null for text field but actual value is " + actualTextValue);
                                    customAssert.assertTrue(false, "Expected value is null for text field but actual value is " + actualTextValue);
                                    validationStatus = false;
                                }

                            } else if (!actualTextValue.equalsIgnoreCase(textFieldValue)) {

                                logger.error("Expected and Actual value of Text Field does not match for childEntity " + childEntity + " entity Id " + entityId);
                                customAssert.assertTrue(false, "Expected and Actual value of Text Field does not match for childEntity " + childEntity + " entity Id " + entityId);
                                validationStatus = false;
                            }else {
                                logger.info("textFieldValue validated successfully");
                            }

                        } else if (field_types_inorder_array[i].equals("singleSelect")) {

                            String actualSingleSelect;
                            try {
                                if(singleSelectValues == null){

                                    if(dynamicFieldJson.has("values")){
                                        actualSingleSelect = dynamicFieldJson.getJSONObject("values").get("name").toString();
                                        logger.error("Expected value is null for single select field but actual value is " + actualSingleSelect);
                                        customAssert.assertTrue(false, "Expected value is null for single select  field but actual value is " + actualSingleSelect);
                                        validationStatus = false;
                                    }
                                }else {
                                    actualSingleSelect = dynamicFieldJson.getJSONObject("values").get("name").toString();

                                    if (!singleSelectValues.contains(actualSingleSelect)) {

                                        logger.error("Expected and Actual value of Single Select does not match for childEntity " + childEntity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Expected and Actual value of Single Select does not match for childEntity " + childEntity + " entity Id " + entityId);
                                        validationStatus = false;
                                    } else {
                                        logger.info("singleSelect validated successfully");
                                    }
                                }
                            } catch (Exception e) {

                                logger.error("Exception while fetching values Json Object for childEntity " + childEntity + " entity Id " + entityId);
                                customAssert.assertTrue(false, "Exception while fetching values Json Object for childEntity " + childEntity + " entity Id " + entityId + " for the field singleSelect");
                                customAssert.assertTrue(false, "Value is null or not present for the field singleSelect");
                                validationStatus = false;
                            }
                        } else if (field_types_inorder_array[i].equals("multiSelect")) {

                            JSONArray valuesJsonArray;
                            try {
                                valuesJsonArray = dynamicFieldJson.getJSONArray("values");

                                if (multiSelectValues == null) {
                                    if(valuesJsonArray.length() !=0){
                                        logger.error("Expected multiselect values array should be null");
                                        customAssert.assertTrue(false,"Expected multiselect values array should be null");
                                        validationStatus = false;
                                    }
                                } else {
                                    if(valuesJsonArray.length()<=0 && multiSelectValues!=null){
                                        logger.error("Expected multi select values should not be null");
                                        customAssert.assertTrue(false,"Expected multiselect values array should not be null");
                                        validationStatus = false;
                                    }

                                    String actualOptionValue;

                                    for (int j = 0; j < valuesJsonArray.length(); j++) {

                                        actualOptionValue = valuesJsonArray.getJSONObject(j).get("name").toString();
                                        if (!multiSelectValues.contains(actualOptionValue)) {
                                            logger.error("Expected and Actual value of Multi Select does not match for childEntity " + childEntity + " entity Id " + entityId);
                                            customAssert.assertTrue(false, "Expected and Actual value of Multi Select does not match for childEntity " + childEntity + " entity Id " + entityId);
                                            validationStatus = false;
                                        } else {
                                            logger.info("Expected and Actual value of Multi Select does matched for childEntity " + childEntity + " entity Id " + entityId);
                                        }
                                    }

                                }
                            } catch (Exception e) {
                                logger.error("Exception while fetching values Json Array for childEntity " + childEntity + " entity Id " + entityId);
                                customAssert.assertTrue(false, "Exception while fetching values Json Array for childEntity " + childEntity + " entity Id " + entityId + " for the field multiSelect");
                                customAssert.assertTrue(false, "Value is null or not present for the field multiSelect");
                                validationStatus = false;
                            }

                        } else if (field_types_inorder_array[i].equals("multiSelectSupplier")) {

                            JSONArray valuesJsonArray;
                            try {
                                valuesJsonArray = dynamicFieldJson.getJSONArray("values");

                                if (multiSelectValues == null) {
                                    if(valuesJsonArray.length() !=0){
                                        logger.error("Expected multiselect values array should be null");
                                        customAssert.assertTrue(false,"Expected multiselect Supplier values array should be null");
                                        validationStatus = false;
                                    }
                                }else {
                                    String actualOptionValue;

                                    for (int j = 0; j < valuesJsonArray.length(); j++) {

                                        actualOptionValue = valuesJsonArray.getJSONObject(j).get("name").toString();
                                        if (!multiSelectValuesSupplier.contains(actualOptionValue)) {
                                            logger.error("Expected and Actual value of Multi Select Supplier Field does not match for childEntity " + childEntity + " entity Id " + entityId);
                                            customAssert.assertTrue(false, "Expected and Actual value of Multi Select Supplier does not match for childEntity " + childEntity + " entity Id " + entityId);
                                            validationStatus = false;
                                        } else {
                                            logger.info("Expected and Actual value of Multi Select Supplier Field matched for childEntity " + childEntity + " entity Id " + entityId);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Exception while fetching values Json Array for childEntity " + childEntity + " entity Id " + entityId);
                                customAssert.assertTrue(false, "Exception while fetching values Json Array for childEntity " + childEntity + " entity Id " + entityId);
                                customAssert.assertTrue(false, "Value is null or not present for the field multiSelectSupplier");
                                validationStatus = false;
                            }

                        } else if (field_types_inorder_array[i].equals("singleSelectSupplier")) {

                            String actualSingleSelectSupplier;
                            try {
                                if(singleSelectValuesSupplier == null){

                                    if(dynamicFieldJson.has("values")){
                                        actualSingleSelectSupplier = dynamicFieldJson.getJSONObject("values").get("name").toString();
                                        logger.error("Expected value is null for single select Supplier field but actual value is " + actualSingleSelectSupplier);
                                        customAssert.assertTrue(false, "Expected value is null for single select Supplier field but actual value is " + actualSingleSelectSupplier);
                                        validationStatus = false;
                                    }
                                }else {
                                    actualSingleSelectSupplier = dynamicFieldJson.getJSONObject("values").get("name").toString();

                                    if (!singleSelectValuesSupplier.contains(actualSingleSelectSupplier)) {

                                        logger.error("Expected and Actual value of Single Select Supplier does not match for childEntity " + childEntity + " entity Id " + entityId);
                                        customAssert.assertTrue(false, "Expected and Actual value of Single Select Supplier does not match for childEntity " + childEntity + " entity Id " + entityId);
                                        validationStatus = false;
                                    } else {
                                        logger.info("Expected and Actual value of Single Select Supplier matched for childEntity " + childEntity + " entity Id " + entityId);
                                    }
                                }
                            } catch (Exception e) {

                                logger.error("Exception while fetching values Json Object for childEntity singleSelectSupplier" + childEntity + " entity Id " + entityId);
                                customAssert.assertTrue(false, "Exception while fetching values Json Object for childEntity " + childEntity + " entity Id " + entityId);
                                customAssert.assertTrue(false, "Value is null or not present for the field singleSelectSupplier");
                                validationStatus = false;
                            }
                        }

                    } catch (Exception e) {
                        logger.warn("Exception while fetching field id " + childFieldId[i] + " from show page response of " + childEntity + " child entity id " + entityId + e.getMessage());
                        customAssert.assertTrue(false, "Exception while fetching field id " + childFieldId[i] + " from show page response of " + childEntity + " child entity id " + entityId + e.getMessage());
                        validationStatus = false;
                    }

                }
            }
        }catch (Exception e){
            logger.error("Exception while validating Flow Down For Values" + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Flow Down For Values" + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;

    }

    private Boolean validateFlowDownValuesEntityListingPage(String listingResponse,
                                                            String parentEntity,
                                                            String childEntity ,
                                                            int entityId,
                                                            String field_types_inorder,
                                                            String textFieldValue,
                                                            List<String> singleSelectValues,
                                                            List<String> multiSelectValues,
                                                            List<String> singleSelectValuesSupplier,
                                                            List<String> multiSelectValuesSupplier,
                                                            CustomAssert customAssert){

        logger.info("Validating flow down of values on Listing Page for parent entity " + childEntity);
        Boolean validationStatus = true;

        try{

            JSONObject listingResponseJson = new JSONObject(listingResponse);
            JSONArray dataArray = listingResponseJson.getJSONArray("data");
            JSONObject indRowJson;
            JSONArray indRowJsonArray = new JSONArray();
            String columnName = "";
            String columnValue = "";

            outerLoop:
            for(int i =0;i<dataArray.length();i++){
                indRowJson = dataArray.getJSONObject(i);

                indRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indRowJson);

                for(int j =0;j<indRowJsonArray.length();j++){

                    columnName = indRowJsonArray.getJSONObject(j).get("columnName").toString();
                    columnValue = indRowJsonArray.getJSONObject(j).get("value").toString();

                    if(columnName.equalsIgnoreCase("id") && columnValue.contains(String.valueOf(entityId))){
                        break outerLoop;
                    }
                }

            }
            HashMap<String,String> columnValueMap = new HashMap();

            for(int j =0;j<indRowJsonArray.length();j++){

                columnName = indRowJsonArray.getJSONObject(j).get("columnName").toString();
                columnValue = indRowJsonArray.getJSONObject(j).get("value").toString();

                columnValueMap.put(columnName,columnValue);
            }

            String[] childFieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "child field id for parent " + parentEntity, childEntity).split(",");
            String columnFieldValue = "";
            String[] field_types_inorder_array = field_types_inorder.split(",");

            for(int i =0;i<childFieldIds.length;i++){

                if(!columnValueMap.containsKey(childFieldIds[i])){
                    customAssert.assertTrue(false,"Child Field id not found in Listing page");
                }else {
                    columnFieldValue = columnValueMap.get(childFieldIds[i]);

                    if(field_types_inorder_array[i].equalsIgnoreCase("textField")){
                        if(!textFieldValue.equalsIgnoreCase(columnFieldValue)){
                            customAssert.assertTrue(false,"Expected and Actual value for text field didn't match for record ID " + entityId);
                        }

                    }else if(field_types_inorder_array[i].equalsIgnoreCase("singleSelect")){

                        if(!singleSelectValues.contains(columnFieldValue)){
                            customAssert.assertTrue(false,"Expected and Actual value for singleSelect didn't match for record ID " + entityId);
                        }

                    }else if(field_types_inorder_array[i].equalsIgnoreCase("multiSelect")){

                        String columnOptionsArray [] = columnFieldValue.split(",");

                        for(String columnOption : columnOptionsArray){
                            if(!multiSelectValues.contains(columnOption)){
                                customAssert.assertTrue(false,"Expected option value didn't found for  multi Select Field for record ID " + entityId);
                            }
                        }

                    }else if(field_types_inorder_array[i].equalsIgnoreCase("multiSelectSupplier")){

                        String columnOptionsArray [] = columnFieldValue.split(",");

                        for(String columnOption : columnOptionsArray){
                            if(!multiSelectValuesSupplier.contains(columnOption)){
                                customAssert.assertTrue(false,"Expected option value didn't found for  multi Select Supplier Field for record ID " + entityId);
                            }
                        }
                    }else if(field_types_inorder_array[i].equalsIgnoreCase("singleSelectSupplier")){

                        if(!singleSelectValuesSupplier.contains(columnFieldValue)){
                            customAssert.assertTrue(false,"Expected option value didn't found for  single Select Supplier Field for record ID " + entityId);
                        }
                    }
                }

            }

        }catch (Exception e){
            logger.error("Exception while validating Flow Down For Values" + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Flow Down For Values" + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;

    }

    private String createBulkEditPayloadForSettingCustomFields(String[]recordsToEdit,String bulkEditPayload,
                                                               String[] fieldTypes,String[] fieldIds,
                                                               String textFieldValue,
                                                               List<String> singleSelectValues,
                                                               List<String> multiSelectValues,
                                                               List<String> singleSelectValuesSupplier,
                                                               List<String> multiSelectValuesSupplier,
                                                               CustomAssert customAssert){

        String bulkEditCreatePayload = null;

        try{
            JSONObject bulkEditCreatePayloadJson = new JSONObject(bulkEditPayload);

            JSONObject globalDataJson = new JSONObject();

            JSONArray entityIds = new JSONArray();

            for(int i =0;i<recordsToEdit.length;i++){
                entityIds.put(Integer.parseInt(recordsToEdit[i]));
            }


            for(int i =0;i<fieldTypes.length;i++ ){

                if(fieldTypes[i].equalsIgnoreCase("textField")){

//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").put("dyn" + fieldIds[i],new JSONObject());
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").put("dyn" + fieldIds[i],new JSONObject());
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values", textFieldValue);
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values", textFieldValue);


                }else if(fieldTypes[i].equalsIgnoreCase("singleSelect")){

                    JSONObject valuesJson = new JSONObject();


                    for(int j=0;j<singleSelectValues.size();){

                        valuesJson.put("id",Integer.parseInt(singleSelectValues.get(j)));
                        valuesJson.put("name",singleSelectValues.get(j+1));
                        valuesJson.put("clientActive",true);
                        j = j +2;
                    }
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").put("dyn" + fieldIds[i],new JSONObject());
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").put("dyn" + fieldIds[i],new JSONObject());
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);

                }else if(fieldTypes[i].equalsIgnoreCase("multiSelect")){

                    JSONArray valuesJsonArray = new JSONArray();


                    int optionCount = 0;

                    for(int j=0;j<multiSelectValues.size();){

                        JSONObject valuesJson = new JSONObject();
                        valuesJson.put("id",Integer.parseInt(multiSelectValues.get(j)));
                        valuesJson.put("name",multiSelectValues.get(j+1));

                        valuesJsonArray.put(optionCount,valuesJson);

                        optionCount = optionCount + 1;

                        j = j +2;
                    }
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").put("dyn" + fieldIds[i],new JSONObject());
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").put("dyn" + fieldIds[i],new JSONObject());
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);

                }else if(fieldTypes[i].equalsIgnoreCase("singleSelectSupplier")){

                    JSONObject valuesJson = new JSONObject();


                    for(int j=0;j<singleSelectValuesSupplier.size();){

                        valuesJson.put("id",Integer.parseInt(singleSelectValuesSupplier.get(j)));
                        valuesJson.put("name",singleSelectValuesSupplier.get(j+1));
                        valuesJson.put("clientActive",true);
                        j = j +2;
                    }
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").put("dyn" + fieldIds[i],new JSONObject());
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").put("dyn" + fieldIds[i],new JSONObject());
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);

                }else if(fieldTypes[i].equalsIgnoreCase("multiSelectSupplier")){

                    JSONArray valuesJsonArray = new JSONArray();


                    int optionCount = 0;

                    for(int j=0;j<multiSelectValuesSupplier.size();){

                        JSONObject valuesJson = new JSONObject();
                        valuesJson.put("id",Integer.parseInt(multiSelectValuesSupplier.get(j)));
                        valuesJson.put("name",multiSelectValuesSupplier.get(j+1));
                        valuesJson.put("clientActive",true);

                        valuesJsonArray.put(optionCount,valuesJson);


                        optionCount = optionCount + 1;

                        j = j +2;
                    }
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").put("dyn" + fieldIds[i],new JSONObject());
//                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").put("dyn" + fieldIds[i],new JSONObject());
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);
                    bulkEditCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);

                }
            }

            Integer[] fieldIdsInteger = new Integer[fieldIds.length];
            for(int i =0;i<fieldIds.length;i++){

                fieldIdsInteger[i] = Integer.parseInt(fieldIds[i]);
            }

            globalDataJson.put("entityIds",entityIds);
            globalDataJson.put("fieldIds",fieldIdsInteger);
            globalDataJson.put("isGlobalBulk",true);

            bulkEditCreatePayloadJson.getJSONObject("body").put("globalData",globalDataJson);

            bulkEditCreatePayload = bulkEditCreatePayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while creating bulk edit Payload " + e.getMessage());
            customAssert.assertTrue(false,"Exception while creating bulk edit Payload " + e.getMessage());
            bulkEditCreatePayload = null;
        }

        return bulkEditCreatePayload;
    }

    private String editPayloadForSettingCustomFields(String editPayload,
                                                     String[] fieldTypes,String[] fieldIds,
                                                     String textFieldValue,
                                                     List<String> singleSelectValues,
                                                     List<String> multiSelectValues,
                                                     List<String> singleSelectValuesSupplier,
                                                     List<String> multiSelectValuesSupplier,
                                                     CustomAssert customAssert) {
        String editPayloadCreated;

        try{
            JSONObject editCreatePayloadJson = new JSONObject(editPayload);

            for(int i =0;i<fieldTypes.length;i++ ){

                if(fieldTypes[i].equalsIgnoreCase("textField")){

                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",textFieldValue);
                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",textFieldValue);

                }else if(fieldTypes[i].equalsIgnoreCase("singleSelect")){

                    JSONObject valuesJson = new JSONObject();

                    for(int j=0;j<singleSelectValues.size();){

                        valuesJson.put("id",Integer.parseInt(singleSelectValues.get(j)));
                        valuesJson.put("name",singleSelectValues.get(j+1));
                        valuesJson.put("clientActive",true);
                        j = j +2;
                    }

                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);
                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);

                }else if(fieldTypes[i].equalsIgnoreCase("multiSelect")){

                    JSONArray valuesJsonArray = new JSONArray();

                    int optionCount = 0;

                    for(int j=0;j<multiSelectValues.size();){

                        JSONObject valuesJson = new JSONObject();
                        valuesJson.put("id",Integer.parseInt(multiSelectValues.get(j)));
                        valuesJson.put("name",multiSelectValues.get(j+1));

                        valuesJsonArray.put(optionCount,valuesJson);

                        optionCount = optionCount + 1;

                        j = j +2;
                    }

                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);
                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);

                }else if(fieldTypes[i].equalsIgnoreCase("singleSelectSupplier")){

                    JSONObject valuesJson = new JSONObject();

                    for(int j=0;j<singleSelectValuesSupplier.size();){

                        valuesJson.put("id",Integer.parseInt(singleSelectValuesSupplier.get(j)));
                        valuesJson.put("name",singleSelectValuesSupplier.get(j+1));
                        valuesJson.put("clientActive",true);
                        j = j +2;
                    }

                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);
                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJson);

                }else if(fieldTypes[i].equalsIgnoreCase("multiSelectSupplier")){

                    JSONArray valuesJsonArray = new JSONArray();

                    int optionCount = 0;

                    for(int j=0;j<multiSelectValuesSupplier.size();){

                        JSONObject valuesJson = new JSONObject();
                        valuesJson.put("id",Integer.parseInt(multiSelectValuesSupplier.get(j)));
                        valuesJson.put("name",multiSelectValuesSupplier.get(j+1));
                        valuesJson.put("clientActive",true);

                        valuesJsonArray.put(optionCount,valuesJson);


                        optionCount = optionCount + 1;

                        j = j +2;
                    }
                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);
                    editCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + fieldIds[i]).put("values",valuesJsonArray);

                }
            }

            editPayloadCreated = editCreatePayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while creating bulk edit Payload " + e.getMessage());
            customAssert.assertTrue(false,"Exception while creating bulk edit Payload " + e.getMessage());
            editPayloadCreated = null;
        }

        return editPayloadCreated;
    }

    private String getListingData(int listId,String payload,CustomAssert customAssert){

        String listingData = null;
        try{

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2amTrue(listId,payload);

            listingData = listRendererListData.getListDataJsonStr();

            if(!APIUtils.validJsonResponse(listingData)){
                listingData = null;
            }


        }catch (Exception e){
            logger.error("Exception while getting listing data");
            listingData = null;
        }
        return listingData;
    }

    private void sleepThread() throws InterruptedException{

        logger.info("Thread Sleeping for 180000 ms");
        Thread.sleep(180000);
    }

//    select * from entity_field where id = 102123;
//    UPDATE entity_field SET date_created = '1990-01-01 00:00:00.000' WHERE Id = 102115;
//    update work_flow_task_configuration  set editable_fields_edit_page = array_append(editable_fields_edit_page, 102123) where id in (select work_flow_manual_task_id from contract where client_id = 1005);

}
