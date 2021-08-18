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
public class TestCurrencyRollUp_Supplier {

    private final static Logger logger = LoggerFactory.getLogger(TestCurrencyRollUp_Supplier.class);

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

    @Test(dependsOnMethods = "Test_ContractCreation_DC",priority = 120,enabled = true)
    public void Test_CurrencyRollUpSupplier(){

        CustomAssert customAssert = new CustomAssert();

        try{

            List<Integer> contractList = contractListSupplier(supplierId,customAssert);
            if(contractList.size() == 0){customAssert.assertTrue(false,"After applying supplier filter ");}
            int clientCurrencyId = InvoiceHelper.getClientCurrencyId(clientId,customAssert);

            int clientRateCardId = InvoiceHelper.getClientRateCard(clientId,customAssert);
            BigDecimal expectedAdditionalAcvSupplier = new BigDecimal("0.0").setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedAdditionalTcvSupplier = new BigDecimal("0.0").setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedAdditionalFycvSupplier = new BigDecimal("0.0").setScale(2, RoundingMode.HALF_UP);

            for(int i =0;i<contractList.size();i++) {
                int childCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(contractEntityTypeId, contractList.get(i), "currency id"));

                String effectiveDate = ShowHelper.getValueOfField(contractEntityTypeId,contractList.get(i),"effectivedatevalue");
                clientRateCardId =  InvoiceHelper.getClientRateCard(clientId,effectiveDate,"MM-dd-yyyy",customAssert);
                BigDecimal conversionFactor = getConversionFactor(clientRateCardId,childCurrencyId,clientCurrencyId,customAssert);

                BigDecimal additionalAcvContract = new BigDecimal(ShowHelper.getValueOfField(contractEntityTypeId, contractList.get(i), "additionalacv"));

                expectedAdditionalAcvSupplier = expectedAdditionalAcvSupplier.add(additionalAcvContract.multiply(conversionFactor));

                BigDecimal additionalTcvContract = new BigDecimal(ShowHelper.getValueOfField(contractEntityTypeId, contractList.get(i), "additionaltcv"));

                expectedAdditionalTcvSupplier = expectedAdditionalTcvSupplier.add(additionalTcvContract.multiply(conversionFactor));

                BigDecimal additionalFycvContract = new BigDecimal(ShowHelper.getValueOfField(contractEntityTypeId, contractList.get(i), "additionalfacv"));

                expectedAdditionalFycvSupplier = expectedAdditionalFycvSupplier.add(additionalFycvContract.multiply(conversionFactor));
            }

            BigDecimal aggregateAcvSupplier = new BigDecimal(ShowHelper.getValueOfField(1,supplierId,"aggregateacv"));
            if(!expectedAdditionalAcvSupplier.setScale(1).equals(aggregateAcvSupplier)){
                customAssert.assertSame(aggregateAcvSupplier,expectedAdditionalAcvSupplier,"Expected and Actual Value for Aggregate ACV didn't matched for supplier ");
            }

            BigDecimal aggregateTcvSupplier = new BigDecimal(ShowHelper.getValueOfField(1,supplierId,"aggregatetcv"));
            if(!expectedAdditionalTcvSupplier.setScale(1).equals(aggregateTcvSupplier)){
                customAssert.assertSame(aggregateTcvSupplier,expectedAdditionalTcvSupplier,"Expected and Actual Value for Aggregate TCV didn't matched for supplier ");
            }

            BigDecimal aggregateFycvSupplier = new BigDecimal(ShowHelper.getValueOfField(1,supplierId,"aggregatefcv"));
            if(!expectedAdditionalFycvSupplier.setScale(1).equals(aggregateFycvSupplier)){
                customAssert.assertSame(aggregateFycvSupplier,expectedAdditionalFycvSupplier,"Expected and Actual Value for Aggregate Fycv didn't matched for supplier ");
            }

        }catch (Exception e){
            logger.error("Exception in main test method " + e.getStackTrace());
            customAssert.assertEquals("Exception in main test method ","Exception should not occur");
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



    private List<Integer> contractListSupplier(int supplierId,CustomAssert customAssert){

        List<Integer> contractList = new ArrayList<>();

        try{
            ListRendererListData listRendererListData = new ListRendererListData();
            String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplierId + "\",\"name\":\"Gaurav Supplier\"}]},\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":17,\"columnQueryName\":\"id\"}]}";
            listRendererListData.hitListRendererListDataV2(2,payload);
            String listResponse = listRendererListData.getListDataJsonStr();

//            17 is ID column
            contractList = listRendererListData.getAllRecordDbId(17,listResponse);


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting Contract List for a particular supplier " + e.getStackTrace());
        }
        return contractList;
    }

    @AfterClass
    public void afterClass(){

        for(Integer supplierId : supplierIdList){
            EntityOperationsHelper.deleteEntityRecord("suppliers",supplierId);
        }

    }

}
