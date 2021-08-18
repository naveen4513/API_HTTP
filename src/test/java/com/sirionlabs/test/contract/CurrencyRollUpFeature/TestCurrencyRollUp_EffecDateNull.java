package com.sirionlabs.test.contract.CurrencyRollUpFeature;

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
public class TestCurrencyRollUp_EffecDateNull {

    private final static Logger logger = LoggerFactory.getLogger(TestCurrencyRollUp_EffecDateNull.class);

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
    //    int supplierId = -1;
    int supplierId = 95270;

    HashMap<Integer, List<Integer>> contractListMap = new HashMap<>();
    HashMap<Integer, List<Integer>> contractListMapWithLevel = new HashMap<>();
    HashMap<Integer, List<Integer>> immediateChildMap = new HashMap<>();

    HashMap<Integer, Integer> childParentMap = new HashMap<>();

    ArrayList<Integer> supplierIdList = new ArrayList<>();

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

    //Using 2nd and 3rd contract of level 3 for effecting date null setting
    @Test(dependsOnMethods = "Test_ContractCreation_DC",priority = 100,enabled = true)
    public void Test_CurrencyRoll_EffectiveDateNull() {

        CustomAssert customAssert = new CustomAssert();

        try {

//            Using 2nd contract of level 3 for Effective Date As Null And Single Rate Card
            int contractId = contractListMapWithLevel.get(3).get(1);

            String startDateForEffDateNull = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"rate card start date for eff date null");
            String endDateForEffDateNull = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"rate card end date for eff date null");

            updateEffDateAsNullAndSingleRateCard(contractId,rateCardIdList.get(0),startDateForEffDateNull,endDateForEffDateNull,customAssert);

//            Using 3rd contract of level 3 for Effective Date As Null And Multiple Rate Cards
            contractId = contractListMapWithLevel.get(3).get(2);
            updateEffDateAsNullAndMultipleRateCards(contractId,rateCardIdList,startDateList,endDateList,customAssert);

            validateAggValuesDC(contractListMapWithLevel, contractListMap, childParentMap, "After updating Effective Date as Null in one of child values ", customAssert);

            updateConversionTypeOnContracts(2,contractListMapWithLevel,customAssert);

            validateAggValuesIDC(contractListMapWithLevel, immediateChildMap, customAssert);

        } catch (Exception e) {
            logger.error("Exception in main test method " + e.getStackTrace());
            customAssert.assertEquals("Exception in Main Test Method " + e.getStackTrace(),"Exception should not occur");
        }

        customAssert.assertAll();
    }

    @Test(priority = 0,enabled = true)
    public void Test_SupplierCreation(){

        CustomAssert customAssert = new CustomAssert();

        try{

            String createResponse = Supplier.createSupplier("roll up scenario",true);
            supplierId = CreateEntity.getNewEntityId(createResponse, "suppliers");

            if(supplierId == -1){
                customAssert.assertEquals("Unable to create supplier ","Supplier should be created");
            }else {
                UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, "roll up scenario 1", "sourceid",
                        "new supplier", String.valueOf(supplierId));
                supplierIdList.add(supplierId);
            }

        }catch (Exception e){
            customAssert.assertEquals("Exception while supplier Creation " ,"Exception should not occur ");
        }
        customAssert.assertAll();
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

    private void updateSingleRateCard(int contractId, String rateCardId, String startDate, String endDate, CustomAssert customAssert) {

        try {

            String payload = "{\"body\":{\"data\":{\"rateCard\":{\"values\":{\"name\":\"Auto Conv 3\",\"id\":" + rateCardId + "}}," +
                    "\"rateCardFromDate\":{\"values\":\"" + startDate + "\"}," +
                    "\"rateCardToDate\":{\"values\":\"" + endDate + "\"}," +
                    "\"contractId\":{\"values\":" + contractId + "}}}}";

            UpdateRateCards updateRateCards = new UpdateRateCards();
            if (updateRateCards.hitUpdateRateCards(payload).getStatusLine().getStatusCode() != 200) {
                customAssert.assertEquals( "Rate Card updation not happened successfully for contract Id " + contractId,"Rate Card updation should happen successfully for contract Id " + contractId);
            }

        } catch (Exception e) {
            logger.error("Exception while Updating single rate card ");
            customAssert.assertEquals("Exception while Updating single rate card ","Exception should not occur");
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

    private void updateConversionTypeOnContracts(int convId, HashMap<Integer, List<Integer>> contractListMapWithLevel, CustomAssert customAssert) {
        Edit edit = new Edit();
        String entityName = "contracts";

        try {
            int contractId;

            for (Map.Entry<Integer, List<Integer>> entry : contractListMapWithLevel.entrySet()) {

                int level = entry.getKey();

                List<Integer> contractList = entry.getValue();

                for (int i = 0; i < contractList.size(); i++) {
                    contractId = contractList.get(i);
                    String editPayload = edit.getEditPayload(entityName, contractId);

                    JSONObject editPayloadJson = new JSONObject(editPayload);

                    editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("conversionType").getJSONObject("values").put("id", convId);
                    JSONObject userInfo = new JSONObject();
                    userInfo.put("id", 1979);
                    userInfo.put("idType", 2);
                    editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_2163").getJSONArray("values").put(userInfo);

                    String editResponse = edit.hitEdit(entityName, editPayloadJson.toString());

                    if (!editResponse.contains("success")) {
                        customAssert.assertEquals( "Conversion type updated unsuccessfully on contracts ","Conversion type should be updated successfully");
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Exception while updating Conversion Types On Contracts");
            customAssert.assertEquals( "Exception while updating Conversion Types On Contracts ","Exception should not occur");
        }

    }


    //    validate Aggregate Values In Direct Conversion
//    C40367 C152329
    public void validateAggValuesIDC(HashMap<Integer, List<Integer>> contractListMapWithLevel,
                                     HashMap<Integer, List<Integer>> immediateChildMap, CustomAssert customAssert) {

        try {

            int contractOnWhichValuesAreChecked;

            for (Map.Entry<Integer, List<Integer>> entry : contractListMapWithLevel.entrySet()) {

                int level = entry.getKey();

                List<Integer> contractList = entry.getValue();

                for (int i = 0; i < contractList.size(); i++) {
                    BigDecimal expectedAcvValue = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal expectedTcvValue = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal expectedFycvValue = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);

                    contractOnWhichValuesAreChecked = contractList.get(i);

                    List<Integer> childContractList = new ArrayList<>();
                    childContractList = immediateChildMap.get(contractOnWhichValuesAreChecked);

                    if (childContractList != null) {

                        for (int j = 0; j < childContractList.size(); j++) {

//                        int parentContractId = getParentContract(childContractList.get(j),contractListMap);
                            int childContractId = childContractList.get(j);
                            int parentContractId = contractOnWhichValuesAreChecked;
                            int parentCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(contractEntityTypeId, parentContractId, "currency id"));

                            int childCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(contractEntityTypeId, childContractList.get(j), "currency id"));
                            String effectiveDate = ShowHelper.getValueOfField(contractEntityTypeId, childContractId, "effectivedate");

                            int rateCardParent = InvoiceHelper.getEffectiveRateCard(parentContractId, datePattern, effectiveDate, customAssert);

//                            BigDecimal conversionFactor = getConversionFactor(rateCardParent, childCurrencyId, parentCurrencyId, customAssert);

                            BigDecimal conversionFactor = new BigDecimal(0.0).setScale(2, RoundingMode.HALF_UP);;
                            if(rateCardParent != 0) {
                                conversionFactor = getConversionFactor(rateCardParent, childCurrencyId, parentCurrencyId, customAssert);
                            }
//                      Aggregate ACV = Sum Of ACV's (of all immediate child Entities)
                            BigDecimal acvValueChild = new BigDecimal(ShowHelper.getValueOfField(contractEntityTypeId, childContractList.get(j), "acv")).setScale(2, RoundingMode.HALF_UP);

                            expectedAcvValue = expectedAcvValue.add(acvValueChild.multiply(conversionFactor));

                            BigDecimal tcvValueChild = new BigDecimal(ShowHelper.getValueOfField(contractEntityTypeId, childContractList.get(j), "acv")).setScale(2, RoundingMode.HALF_UP);

                            expectedTcvValue = expectedTcvValue.add(tcvValueChild.multiply(conversionFactor));

                            BigDecimal fycvValueChild = new BigDecimal(ShowHelper.getValueOfField(contractEntityTypeId, childContractList.get(j), "acv")).setScale(2, RoundingMode.HALF_UP);

                            expectedFycvValue = expectedFycvValue.add(fycvValueChild.multiply(conversionFactor));

                        }
                    }
                    String aggAcvValue = ShowHelper.getValueOfField(contractEntityTypeId, contractOnWhichValuesAreChecked, "aggregateacv");

                    if (aggAcvValue == null) {
                        if (!expectedAcvValue.setScale(2).equals(new BigDecimal("0.00"))) {
                            customAssert.assertEquals("0.00",expectedAcvValue, "Aggregate ACV Value is null when expected is not equal to zero");
                        }
                    } else {

                        BigDecimal aggregateAcvValue = new BigDecimal(aggAcvValue).setScale(2, RoundingMode.HALF_UP);
                        if (!aggregateAcvValue.equals(expectedAcvValue.setScale(2, RoundingMode.HALF_UP))) {
                            customAssert.assertEquals(aggregateAcvValue,expectedAcvValue, "Additional Acv Value not matched for contract Id " + contractOnWhichValuesAreChecked + " for level " + level + " For the InDirect Conversion Scenario");
                        }
                    }

                    String aggTcvValue = ShowHelper.getValueOfField(contractEntityTypeId, contractOnWhichValuesAreChecked, "aggregatetcv");

                    if (aggTcvValue == null) {
                        if (!expectedAcvValue.setScale(2).equals(new BigDecimal("0.00"))) {
                            customAssert.assertEquals("0.00",expectedTcvValue, "Aggregate TCV Value is null when expected is not equal to zero");
                        }
                    } else {

                        BigDecimal aggregateTcvValue = new BigDecimal(aggTcvValue).setScale(2, RoundingMode.HALF_UP);
                        if (!aggregateTcvValue.equals(expectedAcvValue.setScale(2, RoundingMode.HALF_UP))) {
                            customAssert.assertEquals(aggregateTcvValue,expectedTcvValue, "Additional Tcv Value not matched for contract Id " + contractOnWhichValuesAreChecked + " for level " + level+ " For the In Direct Conversion Scenario");
                        }
                    }
                    String aggFcvValue = ShowHelper.getValueOfField(contractEntityTypeId, contractOnWhichValuesAreChecked, "aggregatefcv");

                    if (aggFcvValue == null) {
                        if (!expectedFycvValue.setScale(2).equals(new BigDecimal("0.00"))) {
                            customAssert.assertEquals("0.00",expectedFycvValue, "Aggregate Fycv Value is null when expected is not equal to zero"+ " For the In Direct Conversion Scenario");
                        }
                    } else {

                        BigDecimal aggregateFycvValue = new BigDecimal(aggFcvValue).setScale(2, RoundingMode.HALF_UP);
                        if (!aggregateFycvValue.equals(expectedFycvValue.setScale(2, RoundingMode.HALF_UP))) {
                            customAssert.assertEquals(aggregateFycvValue,expectedFycvValue, "Additional Fycv Value not matched for contract Id " + contractOnWhichValuesAreChecked + " for level " + level+ " For the In Direct Conversion Scenario");
                        }
                    }

                }
            }

        } catch (Exception e) {
            logger.error("Exception while validating Agg Values In Direct Conversion");
            customAssert.assertEquals("Exception while validating Agg Values In InDirect Conversion " + e.getStackTrace(),"Exception should not occur");
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


    private void  updateEffDateAsNullAndSingleRateCard(int contractId,String rateCardId,String startDate,String endDate, CustomAssert customAssert){

        try{
            Edit edit = new Edit();
            String editPayload = edit.getEditPayload(contracts,contractId);

            JSONObject editPayloadJson = new JSONObject(editPayload);

            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").put("values",JSONObject.NULL);

            String editResponse = edit.hitEdit(contracts,editPayloadJson.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit done unsuccessfully for effective date null Single Rate Card");
            }

            updateSingleRateCard(contractId, rateCardId, startDate, endDate, customAssert);


        }catch (Exception e){
            customAssert.assertEquals("Exception while update Effective  Date As Null And Single Rate Card " + e.getStackTrace(),"");
        }
    }

    private void  updateEffDateAsNullAndMultipleRateCards(int contractId, List<String> rateCardIdList,List<String> startDateList,List<String> endDateList, CustomAssert customAssert){

        try{
            Edit edit = new Edit();
            String editPayload = edit.getEditPayload(contracts,contractId);

            JSONObject editPayloadJson = new JSONObject(editPayload);

            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").put("values",JSONObject.NULL);

            String editResponse = edit.hitEdit(contracts,editPayloadJson.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit done unsuccessfully for effective date null Single Rate Card");
            }

            updateRateCards(contractId,rateCardIdList,startDateList,endDateList,customAssert);


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while update Effective  Date As Null And Single Rate Card " + e.getStackTrace());
        }
    }

    @AfterClass
    public void afterClass(){

        for(Integer supplierId : supplierIdList){
            EntityOperationsHelper.deleteEntityRecord("suppliers",supplierId);
        }

    }

}
