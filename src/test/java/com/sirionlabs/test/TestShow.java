package com.sirionlabs.test;
//

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.todo.TodoDaily;
import com.sirionlabs.api.todo.TodoWeekly;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vijay.thakur on 7/12/2017.
 */
//@Listeners(value = MyTestListenerAdapter.class)
public class TestShow {

    private final static Logger logger = LoggerFactory.getLogger(TestShow.class);
    static String configListDataFilePath;
    static String configListDataFileName;
    static List<Integer> entitiesToSkip = new ArrayList<Integer>();
    static List<Integer> doucmentHistoryTabUrl = new ArrayList<Integer>();
    String entityIdMappingFileName;
    String baseFilePath;
    int size;
    int offset;
    List<String> allEntitySection;
    List<Integer> allDBid;
    ListRendererListData listDataObj = new ListRendererListData();
    List<Integer> allDBidForVendors = null;
    List<Integer> allDBidForSuppliers = null;
    List<Integer> allDBidForContracts = null;
    TodoDaily todoDaily;
    TodoWeekly todoWeekly;
    Boolean isTabUrlValidationRequired = false;
    int dbIdCountToTestForSmoke = 10;

    @BeforeClass
    public void beforeClass() throws Exception {
        logger.info("In Before Class method");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        //allEntitySection = ParseConfigFile.getAllSectionNames(baseFilePath, entityIdMappingFileName);

        configListDataFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListDataConfigFilePath");
        configListDataFileName = ConfigureConstantFields.getConstantFieldsProperty("ListDataConfigFileName");
        size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configListDataFilePath, configListDataFileName, "size"));
        offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configListDataFilePath, configListDataFileName, "offset"));
        isTabUrlValidationRequired = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configListDataFilePath, configListDataFileName, "istaburlvalidationrequired"));
        entitiesToSkip = this.getListOfId("entitiestoskip");
        doucmentHistoryTabUrl = this.getListOfId("doucmenthistorytaburl");
        allEntitySection = Arrays.asList((ParseConfigFile.getValueFromConfigFile(configListDataFilePath, configListDataFileName,
                "allentitytotest")).split(","));

        todoDaily = new TodoDaily();
        todoWeekly = new TodoWeekly();
    }


    /**
     * Here the DAtaProvider will provide Object array on the basis on ITestContext
     *
     * @return
     */
    @DataProvider(name = "TestShowPageAPIData", parallel = true)
    public Object[][] getTestShowPageAPIData(ITestContext c) throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];

        for (String entitySection : allEntitySection) {
            groupArray[i] = new Object[2];
            Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
            groupArray[i][0] = entitySection; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            i++;
        }

        return groupArray;
    }

    @Test(dataProvider = "TestShowPageAPIData")
    public void testShowPage(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();

        if (entitiesToSkip.contains(entityTypeId)) {
            logger.warn("Skipping the show page validation for entity = {}", entitySection);
            //throw new SkipException("Skipping the show page validation for entity = "+entitySection);
        } else {

            try {
                // Iterate over each entitySection
                logger.info("----------------------------------------------------------------------------------------------");
                logger.info("validating entity {} ", entitySection);
                logger.info("----------------------------------------------------------------------------------------------");
                String listDataResponse = listDataResponse(entityTypeId, entitySection);
                logger.debug("List Data API Response : entity={} , response={}", entitySection, listDataResponse);

                boolean isListDataValidJson = APIUtils.validJsonResponse(listDataResponse, "[listData response]");
                csAssertion.assertTrue(isListDataValidJson, "List Data Response is not a valid JSON.");
                if (isListDataValidJson) {
                    boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataResponse);
                    boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataResponse);

                    csAssertion.assertFalse(isListDataApplicationError, "Application error found while hitting API = listData  for entity = " + entitySection);
//					csAssertion.assertFalse(isListDataPermissionDenied, "Permission Denied error found while hitting listData API  for entity = " + entitySection);
                    if (!isListDataApplicationError && !isListDataPermissionDenied) {

                        JSONObject listDataResponseObj = new JSONObject(listDataResponse);
                        int noOfRecords = listDataResponseObj.getJSONArray("data").length();

                        if (noOfRecords > 0) {
                            listDataObj.setListData(listDataResponse);
                            int columnId = listDataObj.getColumnIdFromColumnName("id");

                            allDBid = listDataObj.getAllRecordDbId(columnId, listDataResponse);

                            if (entitySection.contentEquals("vendors")) {
                                allDBidForVendors = allDBid;
                            }
                            if (entitySection.contentEquals("suppliers")) {
                                allDBidForSuppliers = allDBid;
                            }
                            if (entitySection.contentEquals("contracts")) {
                                allDBidForContracts = allDBid;
                            }

                            // Dbids is for selected id on which validation would be perform
                            List<Integer> DBids = allDBid;
                            // we will pick random dbIdCountToTestForSmoke ids or less in case of smoke testing
                            if (ConfigureEnvironment.getTestingType().toLowerCase().contains("smoke") && dbIdCountToTestForSmoke < DBids.size()) {
                                DBids = new ArrayList<>();
                                int[] randomIndex = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allDBid.size(), dbIdCountToTestForSmoke);
                                for (int i = 0; i < randomIndex.length; i++) {
                                    DBids.add(allDBid.get(randomIndex[i]));
                                }

                                if (entitySection.contentEquals("vendors")) {
                                    allDBidForVendors = DBids;
                                }
                                if (entitySection.contentEquals("suppliers")) {
                                    allDBidForSuppliers = DBids;
                                }
                                if (entitySection.contentEquals("contracts")) {
                                    allDBidForContracts = DBids;
                                }

                            }

                            logger.info("Select Db Ids is : {}", DBids);

                            for (Integer dbId : DBids) {

                                String showPageResponseStr = getShowResponse(entityTypeId, dbId);
                                logger.debug("Show API Response : entity={} , DB_ID = {} , response={}", entitySection, dbId, showPageResponseStr);
                                //Assertion for valid JSON response

                                boolean isShowPageValidJson = APIUtils.validJsonResponse(showPageResponseStr, "[show api response]");
                                csAssertion.assertTrue(isShowPageValidJson, "Show page Response is not a valid JSON for entity = " + entitySection + " and DBid = " + dbId);

                                if (isShowPageValidJson) {
                                    boolean isShowPageApplicationError = APIUtils.isApplicationErrorInResponse(showPageResponseStr);
                                    boolean isShowPagePermissionDenied = APIUtils.isPermissionDeniedInResponse(showPageResponseStr);

                                    boolean failCase = isShowPageApplicationError && !isShowPagePermissionDenied;

                                    if(failCase) {
                                        csAssertion.assertFalse(true, "Application error found while hitting show API  for entity = " + entitySection + " and DBid = " + dbId);
                                        FileUtils.saveResponseInFile("Entity " + entityTypeId + " dbId " + dbId + " Show Response Application Error", showPageResponseStr);
                                    }

                                    if (isShowPageApplicationError)
                                        continue;

                                    //Validating show page tab URLs
                                    if (isTabUrlValidationRequired) {
                                        Show showObj = new Show();
                                        List<String> layoutUrl = showObj.getShowPageTabUrl(showPageResponseStr, Show.TabURL.layoutURL);
                                        List<String> dataUrl = showObj.getShowPageTabUrl(showPageResponseStr, Show.TabURL.dataURL);

                                        validateLayoutUrl(layoutUrl, csAssertion);
                                        validateDataUrl(dataUrl, entityTypeId, csAssertion);
                                    }
                                } else {
                                    FileUtils.saveResponseInFile("Entity " + entityTypeId + " dbId " + dbId, showPageResponseStr);
                                }
                            }
                        } else
                            logger.warn("no records found for entity : {} ", entitySection);
                    }
                } else
                    logger.error("ListData response is not valid json for entity ={}", entitySection);
            } catch (Exception e) {
                logger.error(e.getMessage());
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                csAssertion.assertTrue(false, "TestShow Exception\n" + errors.toString());
            }
        }
        csAssertion.assertAll();
    }

	/*@Test(priority = 1, dependsOnMethods = "testShowPage")
	public void testToDoAPIForVendors() throws IOException {
		CustomAssert csAssertion = new CustomAssert();
		if (allDBidForVendors != null && !allDBidForVendors.isEmpty())
			for (Integer id : allDBidForVendors) {
				todoDaily.hitTodoDailyEntitySpecific("vendorid", (int) id);
				todoWeekly.hitTodoWeeklyEntitySpecific("vendorid", (int) id);

				String toDoDailyAPIResponse = todoDaily.getTodoDailyJsonStr();
				String toDoWeeklyAPIReponse = todoWeekly.getTodoWeeklyJsonStr();

				boolean istoDoDailyAPIResponseValidJson = APIUtils.validJsonResponse(toDoDailyAPIResponse);
				boolean istoDoWeeklyAPIResponseValidJson = APIUtils.validJsonResponse(toDoWeeklyAPIReponse);

				csAssertion.assertTrue(istoDoDailyAPIResponseValidJson, "Response is not a valid JSON for toDoDaily Vendor Id->" + (int) id);
				csAssertion.assertTrue(istoDoWeeklyAPIResponseValidJson, "Response is not a valid JSON for toDoWeekly Vendor Id->" + (int) id);
			}
		csAssertion.assertAll();
	}

	@Test(priority = 2, dependsOnMethods = "testToDoAPIForVendors")
	public void testToDoAPIForSuppliers() throws IOException {
		CustomAssert csAssertion = new CustomAssert();
		if (allDBidForSuppliers != null && !allDBidForSuppliers.isEmpty())
			for (Integer id : allDBidForSuppliers) {
				todoDaily.hitTodoDailyEntitySpecific("relationid", (int) id);
				todoWeekly.hitTodoWeeklyEntitySpecific("relationid", (int) id);

				String toDoDailyAPIResponse = todoDaily.getTodoDailyJsonStr();
				String toDoWeeklyAPIReponse = todoWeekly.getTodoWeeklyJsonStr();

				boolean istoDoDailyAPIResponseValidJson = APIUtils.validJsonResponse(toDoDailyAPIResponse);
				boolean istoDoWeeklyAPIResponseValidJson = APIUtils.validJsonResponse(toDoWeeklyAPIReponse);

				csAssertion.assertTrue(istoDoDailyAPIResponseValidJson, "Response is not a valid JSON for toDoDaily Vendor Id->" + (int) id);
				csAssertion.assertTrue(istoDoWeeklyAPIResponseValidJson, "Response is not a valid JSON for toDoWeekly Vendor Id->" + (int) id);
			}
		csAssertion.assertAll();
	}

	@Test(priority = 3, dependsOnMethods = "testToDoAPIForSuppliers")
	public void testToDoAPIForContracts() throws IOException {
		CustomAssert csAssertion = new CustomAssert();
		if (allDBidForContracts != null && !allDBidForContracts.isEmpty())
			for (Integer id : allDBidForContracts) {
				todoDaily.hitTodoDailyEntitySpecific("contractid", (int) id);
				todoWeekly.hitTodoWeeklyEntitySpecific("contractid", (int) id);

				String toDoDailyAPIResponse = todoDaily.getTodoDailyJsonStr();
				String toDoWeeklyAPIReponse = todoWeekly.getTodoWeeklyJsonStr();

				boolean istoDoDailyAPIResponseValidJson = APIUtils.validJsonResponse(toDoDailyAPIResponse);
				boolean istoDoWeeklyAPIResponseValidJson = APIUtils.validJsonResponse(toDoWeeklyAPIReponse);

				csAssertion.assertTrue(istoDoDailyAPIResponseValidJson, "Response is not a valid JSON for toDoDaily Vendor Id->" + (int) id);
				csAssertion.assertTrue(istoDoWeeklyAPIResponseValidJson, "Response is not a valid JSON for toDoWeekly Vendor Id->" + (int) id);
			}
		csAssertion.assertAll();
	}*/


    public void validateDataUrl(List<String> dataUrl, int entityTypeId, CustomAssert csAssertion) {

        String defaultPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
                offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        String payload = null;
        try {

            for (int i = 0; i < dataUrl.size(); i++) {
                logger.info("Hitting dataUrl = {} ", dataUrl.get(i));
                /*skipping validation for document history tab.
                 * dataURL = /listRenderer/list/233/tablistdata/23/1550. In new implementation Document id is passed instead of entityDBId(1550) in the url.*/
                String data[] = dataUrl.get(i).split("/");
                if (doucmentHistoryTabUrl.contains(Integer.parseInt(data[3]))) {
                    logger.info("Skipping tabUrl : {}", dataUrl.get(i));
                    continue;
                }
                if (entityTypeId == 160 && data[3].equals("377")) {
                    //contract_id is passed under orderByColumnName for CDR and tabName = Related Contracts
                    payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
                            offset + ",\"size\":" + size + ",\"orderByColumnName\":\"contract_id\"," +
                            "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                } else {
                    payload = defaultPayload;
                }
                String showPageDataUrlResponseStr = Show.hitshowPageTabUrl(dataUrl.get(i), payload);
                logger.debug(" dataUrl/tabListDataResponse ={}", showPageDataUrlResponseStr);

                boolean isDataUrlResponseValidJson = APIUtils.validJsonResponse(showPageDataUrlResponseStr);
                csAssertion.assertTrue(isDataUrlResponseValidJson, "dataUrl Response is not a valid JSON for dataUrl = " + dataUrl.get(i));
                if (isDataUrlResponseValidJson) {
                    boolean isDataUrlResponseApplicationError = APIUtils.isApplicationErrorInResponse(showPageDataUrlResponseStr);
                    boolean isDataUrlResponsePermissionDenied = APIUtils.isPermissionDeniedInResponse(showPageDataUrlResponseStr);

                    csAssertion.assertFalse(isDataUrlResponseApplicationError, "Application error found while hitting dataUrl  for dataUrl = " + dataUrl.get(i));
                    csAssertion.assertFalse(isDataUrlResponsePermissionDenied, "Permission Denied error found while hitting dataUrl  for dataUrl = " + dataUrl.get(i));
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage());

            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "TestShow Exception\n" + errors.toString());
        }
    }

    public void validateLayoutUrl(List<String> layoutUrl, CustomAssert csAssertion) {

        String payload = "{}";
        try {

            for (int i = 0; i < layoutUrl.size(); i++) {
                logger.info("Hitting layoutUrl = {} ", layoutUrl.get(i));
                /*skipping validation for document history tab.
                 * dataURL = /listRenderer/list/233/tablistdata/23/1550. In new implementation Document id is passed instead of entityDBId(1550) in the url.*/
                String data[] = layoutUrl.get(i).split("/");
                if (layoutUrl.get(i).contains("templatePages")) {
                    logger.info("Skipping validation for contract template pages. Since the expected response of the api is html.");
                    continue;
                }
                if (doucmentHistoryTabUrl.contains(Integer.parseInt(data[3]))) {
                    logger.info("Skipping tabUrl : {}", layoutUrl.get(i));
                    continue;
                }
                String layoutUrlResponseStr = Show.hitshowPageTabUrl(layoutUrl.get(i), payload);
                logger.debug("layoutUrl response : {}", layoutUrlResponseStr);

                boolean isLayoutUrlResponseValidJson = APIUtils.validJsonResponse(layoutUrlResponseStr);
                boolean isLayoutUrlResponseApplicationError = APIUtils.isApplicationErrorInResponse(layoutUrlResponseStr);
                boolean isLayoutUrlResponsePermissionDenied = APIUtils.isPermissionDeniedInResponse(layoutUrlResponseStr);

                csAssertion.assertTrue(isLayoutUrlResponseValidJson, "layoutUrl Response is not a valid JSON for layoutUrl = " + layoutUrl.get(i));
                csAssertion.assertFalse(isLayoutUrlResponseApplicationError, "Application error found while hitting layoutUrl  for layoutUrl = " + layoutUrl.get(i));
                csAssertion.assertFalse(isLayoutUrlResponsePermissionDenied, "Permission Denied error found while hitting layoutUrl  for layoutUrl = " + layoutUrl.get(i));

            }

        } catch (Exception e) {
            logger.error("Exception while validating layout url. {}", e.getMessage());

            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "TestShow Exception\n" + errors.toString());
        }
    }

    public String listDataResponse(int entityTypeId, String entitySection) throws Exception {

        Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

        String listDataPayload = "";
        String columnId =ParseConfigFile.getValueFromConfigFile(configListDataFilePath,configListDataFileName,entitySection,"columnid");
        String columnName = ParseConfigFile.getValueFromConfigFile(configListDataFilePath,configListDataFileName,entitySection,"columnname");

        if(columnId == null){
            listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + "id" + "\"," +
                    "\"orderDirection\":\"" + "desc" + "\",\"filterJson\":{}},\"selectedColumns\":[]}";
        }else {
            String columnPayload = "{\"columnId\": " + columnId + ",\"columnQueryName\": \"" + columnName + "\"}";
            listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + "id" + "\"," +
                    "\"orderDirection\":\"" + "desc" + "\",\"filterJson\":{}},\"selectedColumns\":[" + columnPayload + "]}";
        }
        logger.info("Hitting ListRendererListData");

        listDataObj.hitListRendererListDataV2(urlId, listDataPayload);

        String listDataJsonStr = listDataObj.getListDataJsonStr();
        return listDataJsonStr;
    }

    public String getShowResponse(int entityTypeId, int dbId) {
        Show showObj = new Show();

        logger.info("Hitting show api version 2 for Entity Type Id {} and Record Id {}", entityTypeId, dbId);
        showObj.hitShowVersion2(entityTypeId, dbId);

        return showObj.getShowJsonStr();

    }

    private List<Integer> getListOfId(String propertyName) throws ConfigurationException {
        String value = ParseConfigFile.getValueFromConfigFile(TestShow.configListDataFilePath, TestShow.configListDataFileName, propertyName);
        List<Integer> idList = new ArrayList<Integer>();

        if (!value.trim().equalsIgnoreCase("")) {
            String entityIds[] = ParseConfigFile.getValueFromConfigFile(TestShow.configListDataFilePath, TestShow.configListDataFileName, propertyName).split(",");

            for (int i = 0; i < entityIds.length; i++)
                idList.add(Integer.parseInt(entityIds[i].trim()));
        }
        return idList;
    }

}