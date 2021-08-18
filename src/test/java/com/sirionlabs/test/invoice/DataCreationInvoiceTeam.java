package com.sirionlabs.test.invoice;

import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataCreationInvoiceTeam {

    private final static Logger logger = LoggerFactory.getLogger(DataCreationInvoiceTeam.class);
    private String configFilePath;
    private String configFileName;
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
    private String invoicePricingHelperConfigFilePath;
    private String invoicePricingHelperConfigFileName;
    private String contractSectionName;
    private String serviceDataSectionName;
    private String invoiceSectionName;
    private String invoiceLineItemSectionName;

    private int supplierId = 1416;
    String excelFilePath = "src\\test\\resources\\TestConfig\\ReferencesTab";
//    String excelFileName = "ServiceDataCreation4.xlsm";
    String excelFileName = "2000servicedata.xlsm";

    String dumpFilename = "src\\test\\output\\TestDataCreationInvTeam\\" + "ContractBasicInfo.txt";
    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DataCreationInvoiceTeamFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("DataCreationInvoiceTeamFileName");

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

        invoicePricingHelperConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFilePath");
        invoicePricingHelperConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFileName");
    }

    @Test(enabled = false)
    public void ContractCreation(){

        Edit edit = new Edit();
        JSONObject editAPIResponseJson;

        int contractIdCreated;
        int contractCount = 12;

        String editResponse;
        String editAPIResponse;
        String shortCodeId;
        String basicInfoContract;
        String flowToTest = "data creation arc";

        String contractName;

        HashMap<Integer,String> columnMap = new HashMap<Integer, String>();

        ArrayList<String> contractIds = getContractIds();
        ArrayList<String> basicInfoContractList = new ArrayList<String>();

        try {

            setEntitySectionsForFlow(flowToTest);
            for(int i =1;i<51;i++) {
                try {
                    contractIdCreated = getContractId(flowToTest);

                    editAPIResponse = edit.hitEdit("contracts", contractIdCreated);
                    if(!APIUtils.validJsonResponse(editAPIResponse)){
                        logger.error("Not valid json response for edit API for i : " + i);
                        continue;
                    }
                    editAPIResponseJson = new JSONObject(editAPIResponse);

                    editAPIResponseJson.remove("header");
                    editAPIResponseJson.remove("session");
                    editAPIResponseJson.getJSONObject("body").remove("layoutInfo");
                    editAPIResponseJson.getJSONObject("body").remove("globalData");
                    editAPIResponseJson.getJSONObject("body").remove("errors");
                    editAPIResponseJson.remove("actions");
                    editAPIResponseJson.remove("createLinks");
                    contractName = i + " Contract";
                    shortCodeId = editAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").get("values").toString();

                    editAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values", contractName);
                    editAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("title").put("values", contractName);

                    editResponse = editAPIResponseJson.toString();
                    edit.hitEdit("contracts", editResponse);

                    basicInfoContract = contractName + " ( " + shortCodeId + " )";
                    basicInfoContractList.add(basicInfoContract);

                }catch (Exception e ){
                    logger.error("Exception for i : " + i);
                }
            }

            Boolean fileDumpStatus = dumpBasicInfoContractListIntoFile(basicInfoContractList);
            if(!fileDumpStatus){
                logger.error("Error while dumping contracts into the file");
            }

        }catch (Exception e){
            logger.error("Exception while data creation");
        }
    }

    @Test(enabled = true)
    public void createServiceDataBulkUpload(){
        ArrayList<String> contractIdList;
        int contractNumber = 15;

        try{
            contractIdList = getContractBasicInfoFromDumpFile();

            for(String contractIdBasicInfo : contractIdList){
                try {
                    if (!createExcelForServiceDataCreation(contractIdBasicInfo, contractNumber)) {
                        logger.error("Error while creating excel for contract number " + contractNumber);
                        continue;
                    }
                    if (!(bulkUploadServiceData(contractNumber))) {
                        logger.error("Error while creating service data for contract number " + contractNumber);
                        continue;
                    }

                }catch (Exception e){
                    logger.error("Exception while creating Service Data for contract number " + contractNumber);
                }finally {
                    contractNumber = contractNumber + 1;
                }
            }
        }catch (Exception e){
            logger.error("Exception while creating Service Data " + e.getStackTrace());
        }

    }

    private String uploadBulkServiceDataOnSupplier(int contractNo,int supplierId,String excelFilePath,String excelFileName){

        UploadBulkData uploadObj = new UploadBulkData();

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("parentEntityTypeId", Integer.toString(1));
        payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        payloadMap.put("parentEntityId", Integer.toString(supplierId));
        String newexcelFileName;

        newexcelFileName = contractNo + "_" + excelFileName;

        logger.info("Uploading file " + newexcelFileName);
        uploadObj.hitUploadBulkData(64, 1001, excelFilePath, newexcelFileName, payloadMap);
        String uploadStatus = uploadObj.getUploadBulkDataJsonStr();

        return uploadStatus;
    }

    private void setEntitySectionsForFlow(String flowToTest) {
        logger.info("Setting Entity Sections for Flow [{}]", flowToTest);
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractsectionname");
            if (temp != null)
                contractSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedatasectionname");
            if (temp != null)
                serviceDataSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
            if (temp != null)
                invoiceSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();
        } catch (Exception e) {
            logger.error("Exception while Setting Entity Sections for Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
    }

    private int getContractId(String flowToTest) {
        int contractId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createcontract");
            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractid"));
            } else {
                //Create New Contract
                Boolean createLocalContract = true;
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createlocalcontract");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    createLocalContract = false;

                String createResponse = Contract.createContract(contractConfigFilePath, contractConfigFileName, contractConfigFilePath,
                        contractExtraFieldsConfigFileName, contractSectionName, createLocalContract);

                contractId = CreateEntity.getNewEntityId(createResponse, "contracts");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Contract Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return contractId;
    }

    private ArrayList<String> getContractIds(){
        ArrayList<String> contractIds = new ArrayList<>();
        contractIds.add("CO02266");
        contractIds.add("CO02267");
        contractIds.add("CO02268");
        contractIds.add("CO02269");
        contractIds.add("CO02270");
        contractIds.add("CO02271");
        contractIds.add("CO02272");
        contractIds.add("CO02273");
        contractIds.add("CO02274");
        contractIds.add("CO02275");
        contractIds.add("CO02276");
        contractIds.add("CO02277");
        contractIds.add("CO02278");
        contractIds.add("CO02279");
        contractIds.add("CO02280");
        contractIds.add("CO02281");
        contractIds.add("CO02282");
        contractIds.add("CO02283");
        contractIds.add("CO02284");
        contractIds.add("CO02285");
        contractIds.add("CO02286");
        contractIds.add("CO02287");
        contractIds.add("CO02288");
        contractIds.add("CO02289");
        contractIds.add("CO02290");
        contractIds.add("CO02291");
        contractIds.add("CO02292");
        contractIds.add("CO02293");
        contractIds.add("CO02294");
        contractIds.add("CO02295");
        contractIds.add("CO02296");
        contractIds.add("CO02297");

        return contractIds;
    }

    private Boolean dumpBasicInfoContractListIntoFile(ArrayList<String> basicInfoContractList){

        FileUtils fileUtils = new FileUtils();
        String output = "";
        Boolean fileUpdateStatus = true;

        for(String basicinfo : basicInfoContractList){
            output = output + basicinfo + "\n";
        }

        try {
            fileUtils.dumpResponseInFile(dumpFilename,output);
        }catch (Exception e){
            logger.error("Exception while dumping response in file " + dumpFilename);
            fileUpdateStatus = false;
        }
        return fileUpdateStatus;
    }

    private Boolean createExcelForServiceDataCreation(String basicInfoContract,int ContractNumber){

        HashMap<Integer,String> columnMap = new HashMap<>();

        String sheetName = "Service Data";
        Boolean excelCreationStatusServiceDataBulkUpload = true;

        try{
            columnMap.put(2, basicInfoContract);
            columnMap.put(6, "");
            columnMap.put(7, "");
            columnMap.put(15, "");
            //columnMap.put(5, ContractNumber + " ContractSup1SD_");
//            columnMap.put(6, ContractNumber + " ContractSup1SD_");
//            columnMap.put(7, ContractNumber + " ContractSup1SD_");

//              columnMap.put(2, basicInfoContract);
//              columnMap.put(15, "same as 2 column");

            XLSUtils.copyColumnvalues(excelFilePath, excelFileName, sheetName, 6, 2006, columnMap, ContractNumber);
        }catch (Exception e){
            logger.error("Exception while creating service data bulk upload excel");
            excelCreationStatusServiceDataBulkUpload = false;
        }

        return excelCreationStatusServiceDataBulkUpload;
    }

    private boolean bulkUploadServiceData(int contractNumber){

        Boolean bulkUploadServiceDataStatus = true;
        String uploadStatus = "";
        try {
            uploadStatus = uploadBulkServiceDataOnSupplier(contractNumber, supplierId, excelFilePath, excelFileName);

            if(uploadStatus.contains("200")){
                bulkUploadServiceDataStatus = true;
            }else {
                bulkUploadServiceDataStatus = false;
            }
        }catch (Exception e){
            logger.error("Exception while uploading Bulk ServiceData On Supplier ");
            bulkUploadServiceDataStatus = false;
        }
        return bulkUploadServiceDataStatus;
    }

    private ArrayList<String> getContractBasicInfoFromDumpFile(){

        ArrayList<String> contractBasicInfoList = new ArrayList<>();
        try{
            File file = new File(dumpFilename);

            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            line = br.readLine();

            do {
                line.trim();
                contractBasicInfoList.add(line);

            } while ((line = br.readLine()) != null);
        }catch (Exception e){
            logger.error("Exception while reading file");
        }

        return contractBasicInfoList;
    }
}