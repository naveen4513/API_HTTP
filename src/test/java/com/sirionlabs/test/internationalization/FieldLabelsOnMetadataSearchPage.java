package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class FieldLabelsOnMetadataSearchPage extends TestDisputeInternationalization {
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnMetadataSearchPage.class);


    public void verifyFieldLabelsOnMetadataSearchPage(String entityName,CustomAssert csAssert) {

        try {
            logger.info("Getting Entity Type Id for Entity {}", entityName);
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), entityName));

            logger.info("Hitting MetadataSearch Api for Entity : {}", entityName);
            MetadataSearch metadataSearchObj = new MetadataSearch();
            String metaSearchJsonStr = metadataSearchObj.hitMetadataSearch(entityTypeId);

            logger.info("Setting Fields to Test for Entity : {}", entityName);
            List<String> fieldsToTest = MetadataSearch.getAllFieldLabels(metaSearchJsonStr);
            dataLabelValidation(fieldsToTest,entityName,csAssert);
        }catch (Exception e){
            logger.error("Exception while Verifying Search Data for Entity {} of Type {}. {}", entityName, e.getStackTrace());
        }
    }

    private static void dataLabelValidation(List<String> data,String entityName,CustomAssert csAssert){
            for(String Label:data){
                if (Label.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                    csAssert.assertTrue(false, "Field Label: [" + Label.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] for " +entityName );
                } else {
                    csAssert.assertTrue(true, "Field Label: [" + Label.toLowerCase() + "] does not contain: [" + expectedPostFix.toLowerCase() + "] for "+entityName);
                }
            }
    }
}

