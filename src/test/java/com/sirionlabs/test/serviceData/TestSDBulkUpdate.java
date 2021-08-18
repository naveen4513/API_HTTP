package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.List;

public class TestSDBulkUpdate {

    private final static Logger logger = LoggerFactory.getLogger(TestSDBulkUpdate.class);

    private String entityName = "service data";
    int templateId = 1027;
    int entityTypeId = 64;
    int size = 2000;

    @Test
    public void TestSDBulkUpdate(){

        CustomAssert customAssert = new CustomAssert();

        try{

            int listId = ConfigureConstantFields.getListIdForEntity(entityName);

            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}," +
                    "\"selectedColumns\":[{\"columnId\":14483,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":14219,\"columnQueryName\":\"id\"},{\"columnId\":14220,\"columnQueryName\":\"display_name\"},{\"columnId\":14221,\"columnQueryName\":\"contract\"},{\"columnId\":14222,\"columnQueryName\":\"supplier\"},{\"columnId\":14223,\"columnQueryName\":\"serviceclient\"},{\"columnId\":14224,\"columnQueryName\":\"servicesupplier\"},{\"columnId\":14225,\"columnQueryName\":\"startdate\"}]}";
            ListRendererListData listRendererListData =  new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId,payload);

            String listResponse = listRendererListData.getListDataJsonStr();

            List<String> listData =  listRendererListData.getAllRecordForParticularColumns(14219,listResponse);
            String entityIds = "";
            for(String id : listData){
                entityIds = entityIds + id + ",";
            }
            entityIds = entityIds.substring(0,entityIds.length() - 1);

            Download download = new Download();

            String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceData\\BulkUpdate";
            String outputFileName = "BulkUpdate.xlsm";

            Boolean downloadStatus = download.hitDownload(outputFilePath,outputFileName,templateId,entityTypeId,entityIds);

            XLSUtils xlsUtils = new XLSUtils(outputFilePath,outputFileName);


        }catch (NotOfficeXmlFileException nfe){
            customAssert.assertTrue(false,"Bulk update Template not downloaded successfully for entity " + entityName + " and size " + size);
        }
        catch (Exception e){
            logger.error("Exception while validating the scenario "+ e.getStackTrace());
        }

        customAssert.assertAll();
    }
}
