package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.test.autoExtraction.AEPorting;
import com.sirionlabs.test.autoExtraction.TestContractCreationAPI;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.sirionlabs.helper.autoextraction.DocumentShowPageHelper.configAutoExtractionFileName;
import static com.sirionlabs.helper.autoextraction.DocumentShowPageHelper.configAutoExtractionFilePath;

public class ContractCreationWithDocUploadHelper {
    private final static Logger logger = LoggerFactory.getLogger(ContractCreationWithDocUploadHelper.class);
    String templateFileName;
    static Integer docId, contractId;
    static String contractCreationConfigFilePath;
    static String contractCreationConfigFileName;
    static String entity;
    static String contractCreationConfigFileNameVfSandbox;
    static String entityForVFSandbox;
    private static String relationId;
    private static String relationIdForVFSandbox;


    public int TestAEDocumentUploadFromContracts(String environment) {
        CustomAssert csAssert = new CustomAssert();
        try {
            configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
            configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
            contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
            contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");
            contractCreationConfigFileNameVfSandbox = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileNameVFSandbox");
            entity = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
            entityForVFSandbox = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
            relationId = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "contracts", "sourceid");
            relationIdForVFSandbox = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, "contracts", "sourceid");
            String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadpath");
            templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadname");
            if (environment.equals("autoextraction")) {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileName, entity, templateFilePath, templateFileName, relationId, true);
            }
            else if(environment.equals("Sandbox/AEVF"))
            {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, entityForVFSandbox, templateFilePath, templateFileName, relationIdForVFSandbox, true);
            }

            if (contractId == -1) {
                throw new Exception("Couldn't Create Contract. Hence couldn't validate further.");
            }

        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Creating a Contract" + e.getMessage());
        }
        csAssert.assertAll();
        return contractId;
    }
}
