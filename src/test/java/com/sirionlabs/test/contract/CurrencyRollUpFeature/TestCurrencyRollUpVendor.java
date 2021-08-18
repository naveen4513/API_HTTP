package com.sirionlabs.test.contract.CurrencyRollUpFeature;

import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.commonAPI.UpdateRateCards;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.novation.Novation;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Invoice;
import com.sirionlabs.helper.entityCreation.Supplier;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

//SIR-10073
public class TestCurrencyRollUpVendor {

    private final static Logger logger = LoggerFactory.getLogger(TestCurrencyRollUpVendor.class);

    private String configFilePath;
    private String configFileName;

    private String supplierConfigFilePath;
    private String supplierConfigFileName;

    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;

    private String contracts = "contracts";
    private int contractEntityTypeId = 61;
    private int supplierEntityTypeId = 1;
    private int vendorEntityTypeId = 3;

    private int clientId;
    List<String> rateCardIdList = new ArrayList<String>();
    List<String> startDateList = new ArrayList<String>();
    List<String> endDateList = new ArrayList<String>();

    List<String> newRateCardIdList1 = new ArrayList<>();
    List<String> newStartDateList1 = new ArrayList<String>();
    List<String> newEndDateList1 = new ArrayList<String>();

    List<String> newRateCardIdList2 = new ArrayList<>();
    List<String> newStartDateList2 = new ArrayList<String>();
    List<String> newEndDateList2 = new ArrayList<String>();

    String datePattern = "MM-dd-yyyy";
    int stakeholderId;
        int supplierId = -1;
        ArrayList<Integer> supplierIdList = new ArrayList<>();


    HashMap<Integer, List<Integer>> contractListMap = new HashMap<>();
    HashMap<Integer, List<Integer>> contractListMapWithLevel = new HashMap<>();
    HashMap<Integer, List<Integer>> immediateChildMap = new HashMap<>();

    HashMap<Integer, Integer> childParentMap = new HashMap<>();

    @BeforeClass
    public void beforeClass() {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestRollUpScenariosFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestRollUpScenariosFileName");

        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractExtraFieldsFileName");

        supplierConfigFilePath= ConfigureConstantFields.getConstantFieldsProperty("SupplierFilePath");
        supplierConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SupplierFileName");

        rateCardIdList = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rate card id list").split(","));
        startDateList = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "start date list").split(","));
        endDateList = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "end date list").split(","));

        newRateCardIdList1 = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "new rate card list 1").split(","));
        newStartDateList1 = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "new start date list 1").split(","));
        newEndDateList1 = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "new end date list 1").split(","));

        newRateCardIdList2 = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "new rate card list 2").split(","));
        newStartDateList2 = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "new start date list 2").split(","));
        newEndDateList2 = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "new end date list 2").split(","));

        stakeholderId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "stakeholder id"));

        clientId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "client id"));

    }

    @Test(dependsOnMethods = "Test_SupplierCreation",priority = 10,enabled = true)
    public void Test_ContractCreation_DC() {

        CustomAssert customAssert = new CustomAssert();
        try {

            contractCreation(1, 3, contractListMap, contractListMapWithLevel, childParentMap, immediateChildMap, customAssert);

            validateAggValuesDC(contractListMapWithLevel, contractListMap, childParentMap, "First Time Contract Creation", customAssert);

        } catch (Exception e) {
            logger.error("Exception while contract creation " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception while contract creation " + e.getStackTrace());
        }
        customAssert.assertAll();
    }

//    C153686
    @Test(dependsOnMethods = "Test_ContractCreation_DC",priority = 150,enabled = true)
    public void TestCurrencyRollUpVendor(){

        CustomAssert customAssert = new CustomAssert();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        String supplierName = "suppliers";
        BigDecimal additionalValue = new BigDecimal(10.0).setScale(2, RoundingMode.HALF_UP);
        try{

            int vendorId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"vendor id"));
            List<List<String>> supplierListString = postgreSQLJDBC.doSelect("select id from relation where deleted = false and vendor_id = " + vendorId);
            List<Integer> supplierList = new ArrayList<>();


            int clientCurrencyId = InvoiceHelper.getClientCurrencyId(clientId);
            if(clientCurrencyId == -1){
                customAssert.assertEquals("Client currency Id not found","Client currency Id should be fetched successfully");
                customAssert.assertAll();
                return;
            }

            for(int i =0;i<supplierListString.size();i++){

                //For one supplier updating tcv acv fycvvalues
                int supplierId = Integer.parseInt(supplierListString.get(i).get(0));
                if(i ==0){
//                    C153692
                    invoiceHelper.updateAdditionalValues(supplierName,supplierId,additionalValue,customAssert);
                }



//                If supplier currency is not the same as client currency and no effective date, then no roll up will be done.
//                if effective date is null and supplier currency is same as reporting currency then no roll up will be done.
                String supplierCurrId =  ShowHelper.getValueOfField(supplierEntityTypeId,supplierId, "currency id");
                String effectiveDate = ShowHelper.getValueOfField(supplierEntityTypeId,supplierId, "effectivedatevalue");

                if(supplierCurrId == null){
                    continue;
                }
                if(effectiveDate == null && (!supplierCurrId.equals(String.valueOf(clientCurrencyId)))){
//                If supplier currency is not the same as client currency and no effective date, then no roll up will be done
//                so skipping that supplier Id
                    continue;
                }

                supplierList.add(supplierId);
            }

            BigDecimal expectedAdditionalAcvSupplier = new BigDecimal("0.0").setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedAdditionalTcvSupplier = new BigDecimal("0.0").setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedAdditionalFycvSupplier = new BigDecimal("0.0").setScale(2, RoundingMode.HALF_UP);

            for(int i =0;i<supplierList.size();i++) {

                BigDecimal acvSupplier = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);
                BigDecimal tcvSupplier = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);
                BigDecimal fycvSupplier = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);

                String acvValue = ShowHelper.getValueOfField(supplierEntityTypeId, supplierList.get(i), "acv");
                if(acvValue !=null) {
                    acvSupplier = new BigDecimal(acvValue);
                }
                expectedAdditionalAcvSupplier = expectedAdditionalAcvSupplier.add(acvSupplier);

                String tcvValue = ShowHelper.getValueOfField(supplierEntityTypeId, supplierList.get(i), "tcv");
                if(tcvValue !=null) {
                    tcvSupplier = new BigDecimal(tcvValue);
                }
                expectedAdditionalTcvSupplier = expectedAdditionalTcvSupplier.add(tcvSupplier);

                String fycvValue = ShowHelper.getValueOfField(supplierEntityTypeId, supplierList.get(i), "fycv");
                if(fycvValue !=null) {
                    fycvSupplier = new BigDecimal(fycvValue);
                }
                expectedAdditionalFycvSupplier = expectedAdditionalFycvSupplier.add(fycvSupplier);
            }

            BigDecimal aggregateAcvVendor = new BigDecimal(ShowHelper.getValueOfField(3,vendorId,"aggregateacv"));
            if(!expectedAdditionalAcvSupplier.equals(aggregateAcvVendor.setScale(2, RoundingMode.HALF_UP))){
                customAssert.assertTrue(false,"Expected and Actual Value for Aggregate ACV didn't matched for supplier Expected : " + expectedAdditionalAcvSupplier + " Actual " + aggregateAcvVendor);
            }

            BigDecimal aggregateTcvVendor = new BigDecimal(ShowHelper.getValueOfField(3,vendorId,"aggregatetcv"));
            if(!expectedAdditionalTcvSupplier.equals(aggregateTcvVendor.setScale(2, RoundingMode.HALF_UP))){
                customAssert.assertTrue(false,"Expected and Actual Value for Aggregate TCV didn't matched for supplier Expected : " + expectedAdditionalTcvSupplier + " Actual " + aggregateTcvVendor);
            }

            BigDecimal aggregateFycvVendor = new BigDecimal(ShowHelper.getValueOfField(3,vendorId,"aggregatefcv"));
            if(!expectedAdditionalFycvSupplier.equals(aggregateFycvVendor.setScale(2, RoundingMode.HALF_UP))){
                customAssert.assertTrue(false,"Expected and Actual Value for Aggregate Fycv didn't matched for supplier Expected : " + expectedAdditionalFycvSupplier + " Actual " + aggregateFycvVendor);
            }


        }catch (Exception e){
            logger.error("Exception in main test method " + e.getStackTrace());
            customAssert.assertEquals("Exception in main test method ","Exception should not occur");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }


    @Test(priority = 0,enabled = true)
    public void Test_SupplierCreation(){

        CustomAssert customAssert = new CustomAssert();
        String sectionName = "";
        try{
            sectionName = "roll up scenario";
            String createResponse = Supplier.createSupplier(sectionName,true);
            supplierId = CreateEntity.getNewEntityId(createResponse, "suppliers");

            if(supplierId == -1){
                customAssert.assertEquals("Unable to create supplier for the flow " + sectionName,"Supplier should be created");
            }else {
                UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, "roll up scenario 1", "sourceid",
                        "new supplier", String.valueOf(supplierId));
            }

            sectionName="effective date null supplier currency same as reporting currency";
            createResponse = Supplier.createSupplier(sectionName,true);
            supplierId = CreateEntity.getNewEntityId(createResponse, "suppliers");

            if(supplierId == -1){
                customAssert.assertEquals("Unable to create supplier for the flow " + sectionName,"Supplier should be created");
            }
            sectionName = "effective date null supplier currency not same as client currency";
            createResponse = Supplier.createSupplier(sectionName,true);
            supplierId = CreateEntity.getNewEntityId(createResponse, "suppliers");

            if(supplierId == -1){
                customAssert.assertEquals("Unable to create supplier for the flow " + sectionName,"Supplier should be created");
            }
            supplierIdList.add(supplierId);

        }catch (Exception e){
            customAssert.assertEquals("Exception while supplier Creation " ,"Exception should not occur ");
        }
        customAssert.assertAll();
    }

    @AfterClass
    public void afterClass(){

        for(Integer supplierId : supplierIdList){
            EntityOperationsHelper.deleteEntityRecord("suppliers",supplierId);
        }

    }

    private void updateRateCards(int contractId, List<String> rateCardIdList, List<String> startDateList, List<String> endDateList, CustomAssert customAssert) {

        try {

            for (int i = 0; i < rateCardIdList.size(); i++) {
                String payload = "{\"body\":{\"data\":{\"rateCard\":{\"values\":{\"name\":\"Auto Conv 3\",\"id\":" + rateCardIdList.get(i) + "}}," +
                        "\"rateCardFromDate\":{\"values\":\"" + startDateList.get(i) + "\"}," +
                        "\"rateCardToDate\":{\"values\":\"" + endDateList.get(i) + "\"}," +
                        "\"contractId\":{\"values\":" + contractId + "}}}}";

                UpdateRateCards updateRateCards = new UpdateRateCards();
                if (updateRateCards.hitUpdateRateCards(payload).getStatusLine().getStatusCode() != 200) {
                    customAssert.assertEquals( "Rate Card updation not happened successfully for contract Id " + contractId,"Rate Card updation should happen successfully");
                }
            }
        } catch (Exception e) {
            logger.error("");
        }
    }

    //  validate Aggregate Values Direct Conversion
//    C40367
    public void validateAggValuesDC(HashMap<Integer, List<Integer>> contractListMapWithLevel, HashMap<Integer, List<Integer>> contractListMap,
                                    HashMap<Integer, Integer> childParentMap, String stepName, CustomAssert customAssert) {

        try {

            int contractOnWhichValuesAreChecked;

            for (Map.Entry<Integer, List<Integer>> entry : contractListMapWithLevel.entrySet()) {

                int level = entry.getKey();

                List<Integer> contractList = entry.getValue();

                for (int i = 0; i < contractList.size(); i++) {
                    BigDecimal expectedAcvValue = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);
                    contractOnWhichValuesAreChecked = contractList.get(i);

                    List<Integer> childContractList = new ArrayList<>();
                    childContractList = getChildContractList(contractOnWhichValuesAreChecked, contractListMap, childContractList,customAssert);


                    for (int j = 0; j < childContractList.size(); j++) {

//                        int parentContractId = getParentContract(childContractList.get(j),contractListMap);
                        int childContractId = childContractList.get(j);

                        int parentContractId = contractOnWhichValuesAreChecked;
                        int parentCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(contractEntityTypeId, parentContractId, "currency id"));

                        int childCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(contractEntityTypeId, childContractList.get(j), "currency id"));

                        String effectiveDate = ShowHelper.getValueOfField(contractEntityTypeId, childContractId, "effectivedate");

                        int rateCardChild = InvoiceHelper.getEffectiveRateCard(parentContractId, datePattern, effectiveDate, customAssert);

                        BigDecimal conversionFactor = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);
                        if (rateCardChild != 0) {
                            conversionFactor = getConversionFactor(rateCardChild, childCurrencyId, parentCurrencyId, customAssert);
                        }
                        String additionalAcv = ShowHelper.getValueOfField(contractEntityTypeId, childContractList.get(j), "additionalacv");

                        if (additionalAcv == null) {
                            customAssert.assertEquals(false, "Additional ACV Value is null ");
                            continue;
                        } else if (additionalAcv.equals("null")) {
                            customAssert.assertEquals( "Additional ACV Value is null string","Additional ACV Value is not a null string");
                            continue;
                        } else {

                            BigDecimal additionalAcvValueChild = new BigDecimal(additionalAcv).setScale(2, RoundingMode.HALF_UP);

                            additionalAcvValueChild = conversionFactor.multiply(additionalAcvValueChild);

                            expectedAcvValue = expectedAcvValue.add(additionalAcvValueChild);
                        }
                    }
                    String aggAcvValue = ShowHelper.getValueOfField(contractEntityTypeId, contractOnWhichValuesAreChecked, "aggregateacv");

                    if (aggAcvValue == null) {

                        if (!expectedAcvValue.setScale(2, RoundingMode.HALF_UP).equals(new BigDecimal("0.00"))) {
                            customAssert.assertEquals("Aggregate ACV Value is null when expected is not equal to zero","Aggregate ACV Value is null when expected is not equal to zero");
                        }
                    } else {
//                        Double aggregateAcvValueDouble = Double.parseDouble(aggAcvValue);
                        BigDecimal aggregateAcvValue = new BigDecimal(aggAcvValue).setScale(2, RoundingMode.HALF_UP);;

                        if (!aggregateAcvValue.equals(expectedAcvValue.setScale(2, RoundingMode.HALF_UP))) {
                            customAssert.assertEquals(aggregateAcvValue,expectedAcvValue, "Aggregate Acv Value not matched for contract Id " + contractOnWhichValuesAreChecked + " for level " + level + " For the Direct Conversion Scenario");
                        }
                    }
                }
            }
//            }
        } catch (Exception e) {
            logger.error("Exception while validating Agg Values Direct Conversion");
            customAssert.assertEquals("Exception while validating Agg Values Direct Conversion " + e.getStackTrace(),"Exception should not occur");
        }
    }


    private List<Integer> getChildContractList(Integer contractId, HashMap<Integer, List<Integer>> contractListMap, List<Integer> childContractListCreated,
                                               CustomAssert customAssert) {

        List<Integer> childContractList = new ArrayList<>();

        try {

            if (!contractListMap.containsKey(contractId)) {
                return childContractList;
            } else {
                childContractList = contractListMap.get(contractId);

                for (int i = 0; i < childContractList.size(); i++) {
                    if (!childContractListCreated.contains(childContractList.get(i))) {
                        childContractListCreated.add(childContractList.get(i));
                    }
                    getChildContractList(childContractList.get(i), contractListMap, childContractList,customAssert);
                }
            }


        } catch (Exception e) {
            logger.error("Exception while getting child contract");
            customAssert.assertEquals("Exception while getting child contract","Exception should not occur");
        }
        return childContractList;
    }

    //  Getting conversion factor by providing rate card Id currency from and currency To
    private BigDecimal getConversionFactor(int rateCardId, int currFrom, int currTo, CustomAssert customAssert) {

        BigDecimal conversionFactor = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        try {
            conversionFactor = new BigDecimal(postgreSQLJDBC.doSelect("select rate_value from rate_card_conversion where rate_card_id = " + rateCardId +
                    " and currency_from = " + currFrom + " and currency_to = " + currTo).get(0).get(0));
        } catch (Exception e) {
            customAssert.assertEquals( "Exception while fetching conversion id from DB " + e.getStackTrace(),"Exception should not occur");
        } finally {
            postgreSQLJDBC.closeConnection();
        }
        return conversionFactor;

    }

    private HashMap<Integer, List<Integer>> contractCreation(int currLevel, int totalLevels,
                                                             HashMap<Integer, List<Integer>> contractListMap,
                                                             HashMap<Integer, List<Integer>> contractListMapWithLevel,
                                                             HashMap<Integer, Integer> childParentMap,
                                                             HashMap<Integer, List<Integer>> immediateChildMap,
                                                             CustomAssert customAssert) {

        //HashMap<Integer,List<Integer>> contractListMap = new HashMap<>();
        List<Integer> contractIdList = new ArrayList<>();
        List<Integer> newContractList = new ArrayList<>();
        List<Integer> contractListForLevels = new ArrayList<>();
        List<Integer> immediateChildList = new ArrayList<>();

        if (contractListMap.size() != 0) {
            contractIdList = contractListMapWithLevel.get(currLevel - 1);
        }
        try {

            if (currLevel > totalLevels) {
                return contractListMap;
            }

            String contractSectionName = "roll up scenario " + currLevel;

            //Contract Creation C1 Reporting currency AUD additional tcv as 10
            //Conversion Type Direct Conversion
            if (contractIdList.size() != 0) {

                UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, contractSectionName, "sourceid",
                        "new supplier", String.valueOf(supplierId));

                for (int i = 0; i < contractIdList.size(); i++) {
                    newContractList = new ArrayList<>();
                    immediateChildList = new ArrayList<>();

                    for (int j = 0; j < 2; j++) {

                        UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, contractSectionName, "sourceid",
                                "new contract", String.valueOf(contractIdList.get(i)));
                        UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, contractSectionName, "parentsourceid",
                                "new contract", String.valueOf(contractIdList.get(i)));

                        int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);

                        //Updating 3 rate cards on Contract
                        updateRateCards(contractId, rateCardIdList, startDateList, endDateList, customAssert);

                        if (!newContractList.contains(contractId)) {
                            newContractList.add(contractId);
                        }
                        contractListForLevels.add(contractId);
                        immediateChildList.add(contractId);

                        contractListMap.put(contractIdList.get(i), newContractList);
                        contractListMapWithLevel.put(currLevel, contractListForLevels);

                        //child contract ID is Key and PArent Contract Id is value
                        childParentMap.put(contractId, contractIdList.get(i));


                        UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, contractSectionName, "sourceid",
                                String.valueOf(contractIdList.get(i)), "new contract");
                        UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, contractSectionName, "parentsourceid",
                                String.valueOf(contractIdList.get(i)), "new contract");

                        UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, contractSectionName, "parentsourceid",
                                String.valueOf(supplierId), "new supplier");
                    }
                    immediateChildMap.put(contractIdList.get(i), immediateChildList);
                }
            } else {

                int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);
                newContractList.add(contractId);
                updateRateCards(contractId, rateCardIdList, startDateList, endDateList, customAssert);
                contractListMap.put(currLevel, newContractList);
                contractListMapWithLevel.put(currLevel, newContractList);
            }

        } catch (Exception e) {
            logger.error("");
        }

        currLevel = currLevel + 1;
        contractCreation(currLevel, totalLevels, contractListMap, contractListMapWithLevel, childParentMap, immediateChildMap, customAssert);


        return contractListMap;
    }


}
