package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.contractCreation.ContractCreationWithDocUpload;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.test.EntityCreation.TestEntityCreation;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.Map;

import static com.sirionlabs.helper.entityCreation.CreateEntity.getNewEntityId;

public class TestContractCreationAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestContractCreationAPI.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String entity = null;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");
        entity = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiytocreate");
    }

    public static int getNewlyCreatedContractId(String filePath,String fileName,String entityName,String uploadedFilePath,String uploadedFileName,String relationId,boolean viewer) {
        int contractId = 0;
        try {
            ContractCreationWithDocUpload createContractObj = new ContractCreationWithDocUpload();
            Map<String, String> properties = ParseConfigFile.getAllConstantProperties(filePath, fileName, entityName);
            logger.info("Creating Payload for Entity {}", entityName);
            String payloadForCreate = createContractObj.getCreatePayload(entityName, properties,uploadedFilePath,uploadedFileName,relationId,viewer);
            if (payloadForCreate != null) {
                logger.info("Hitting Create Api for Entity {}", entityName);
                Create createObj = new Create();
                createObj.hitCreate(entityName, payloadForCreate);

                String createResponse = createObj.getCreateJsonStr();

                JSONObject jsonObj = new JSONObject(createResponse);
                String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
                contractId = CreateEntity.getNewEntityId(createResponse,"contracts");
                if (status.equalsIgnoreCase("success")) {
                    logger.info("Entity {} created successfully.", entityName);
                    logger.info("Getting Id of the Newly Created Entity {}", entityName);

                } else {
                    logger.info("Entity Creation failed due to {}", status);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Entity {} using Old Approach. {}", entityName, e.getStackTrace());
        }
        return contractId;
    }
}
