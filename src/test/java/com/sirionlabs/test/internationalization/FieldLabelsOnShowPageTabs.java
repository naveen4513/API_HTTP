package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.listRenderer.TabDefaultUserListMetaData;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.HashMap;
import java.util.Map;

public class FieldLabelsOnShowPageTabs extends TestDisputeInternationalization{
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelsOnShowPageTabs.class);
    private static Map<String, String> tabListIdMap = new HashMap<>();
    public void verifyFieldLabelsOnShowPageTabs(CustomAssert csAssert) {
        String baseFilePath = "src//test//resources//CommonConfigFiles";
        String reportIdFile = "TabListId.cfg";
        tabListIdMap = ParseConfigFile.getAllConstantProperties(baseFilePath, reportIdFile, "tab list ids");
        /*Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("entityTypeId", entitytypeid.toString());*/
        for ( String TabId : tabListIdMap.values() ) {
            try {
                logger.info("Validating Field Labels on List Page on Report of" + TabId);
                TabDefaultUserListMetaData defaultObj = new TabDefaultUserListMetaData();
                defaultObj.hitTabDefaultUserListMetadata(Integer.parseInt(TabId));
                String defaultUserListResponse = defaultObj.getTabDefaultUserListMetaDataJsonStr();
                if (!ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
                    logger.error("DefaultUserListMetaData API Response for List Id {} and EntityTypeId {} is an Invalid JSON.", TabId, entityTypeId);
                    return;
                }
                JSONObject defaultJsonObj = new JSONObject(defaultUserListResponse);
                JSONArray jsonArrColName = defaultJsonObj.getJSONArray("columns");
                for ( int i = 0; i < jsonArrColName.length(); i++ ) {
                    String colName = jsonArrColName.getJSONObject(i).getString("name").trim();
                    if (colName.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                        csAssert.assertTrue(false, "Field Label: [" + colName.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under listing columns" +TabId);
                    } else {
                        csAssert.assertTrue(true, "Field Label: [" + colName + "] does not contain: [" + expectedPostFix + "] under listing columns" +TabId);
                    }
                    //break;
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                logger.error("Exception while Validating Field Labels on Show Page for Record Id {}. {}", TabId, e.getStackTrace());
            }
        }
    }
}