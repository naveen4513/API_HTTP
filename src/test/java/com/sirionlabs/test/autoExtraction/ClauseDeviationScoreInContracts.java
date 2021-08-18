package com.sirionlabs.test.autoExtraction;


import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.ContractCreationWithDocUploadHelper;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.helper.autoextraction.GetDocumentIdHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ClauseDeviationScoreInContracts {
    private final static Logger logger = LoggerFactory.getLogger(ClauseDeviationScoreInContracts.class);
    static Integer contractId,docId,projectID;
    private static String postgresHost;
    private static String postgresPort;
    private static String postgresDbName;
    private static String postgresDbUsername;
    private static String postgresDbPassword;
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    HashMap<Integer, Integer> clauseDeviationInDB = new HashMap<>();

    @BeforeClass
    public void beforeClass() {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        postgresHost = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "host");
        postgresPort = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "port");
        postgresDbName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "dbname");
        postgresDbUsername = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "username");
        postgresDbPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "password");
    }

    public static int getProjectLinkedWithDoc() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Get Project Id Linked with the Document");
        HttpResponse metadataAPIResponse = AutoExtractionHelper.hitExtractDocViewerDocId(docId);
        csAssert.assertTrue(metadataAPIResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
        String metadataStr = EntityUtils.toString(metadataAPIResponse.getEntity());
        JSONObject metadataJson = new JSONObject(metadataStr);
        projectID = (Integer) metadataJson.getJSONObject("response").getJSONArray("projects").getJSONObject(0).get("id");
        return projectID;
    }
    public static HashMap<String,Integer> getCategoriesLinkedWithProject(int projectId) throws IOException {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Getting Category Ids from Project Show page");
        HttpResponse projectShowResponse = AutoExtractionHelper.testProjectShowpage(projectId);
        csAssert.assertTrue(projectShowResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
        String projectShowStr = EntityUtils.toString(projectShowResponse.getEntity());
        JSONObject projectShowJson = new JSONObject(projectShowStr);
        JSONArray linkedCategoriesArr = projectShowJson.getJSONObject("response").getJSONArray("projectLinkedCategories");
        HashMap<String ,Integer> categoryIdNamePair = new HashMap<>();
        int linkedCategoriesCount = projectShowJson.getJSONObject("response").getJSONArray("projectLinkedCategories").length();
        for(int i=0;i<linkedCategoriesCount;i++)
        {
            categoryIdNamePair.put((String)linkedCategoriesArr.getJSONObject(i).get("name"),(Integer)linkedCategoriesArr.getJSONObject(i).get("id"));
        }
        return categoryIdNamePair;
    }

    /* Test Case Id: C154348 - End User: Verify that the maximum clause deviation score against each category should get saved in "category_deviation_score_mapping_table"*/
    @Parameters("Environment")
    @Test(priority = 0)
    public void checkDeviationScoreForContract(String environment) throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Creating a contract along with document upload to check deviation score for the document");
        ContractCreationWithDocUploadHelper contractObj = new ContractCreationWithDocUploadHelper();
        contractId = contractObj.TestAEDocumentUploadFromContracts(environment);
        logger.info("Get Document Id to check the extraction status");
        try {
            if (!(contractId == -1)) {
                boolean isExtractionCompletedForUploadedFile = AEPorting.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, "Extraction not Completed");
                docId = GetDocumentIdHelper.getDocIdOfLatestDocument();
                logger.info("Document Id:" + docId);
                projectID = ClauseDeviationScoreInContracts.getProjectLinkedWithDoc();
                logger.info("ProjectId: "+projectID);
                HashMap<String, Integer> map = new HashMap<>();
                map.putAll(getCategoriesLinkedWithProject(projectID));
                if (map.size() > 0) {
                    //Saving the deviation score per clause from Clause Tab
                    try{
                        DocumentShowPageHelper clauseObj = new DocumentShowPageHelper(String.valueOf(docId));
                        String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                                "\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1045\",\"name\":\"Termination\"},{\"id\":\"1048\",\"name\":\"Indemnification\"}]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"0\"}}},\"entityId\":" + docId + "}\n";
                        String clauseTabStr = clauseObj.getClauseTabResponse(String.valueOf(docId), payload);
                        JSONObject clauseTabJson = new JSONObject(clauseTabStr);
                        int extractedClauses = clauseTabJson.getJSONArray("data").length();
                        int clauseColumnId = ListDataHelper.getColumnIdFromColumnName(clauseTabStr, "name");
                        int deviationScoreColumnId = ListDataHelper.getColumnIdFromColumnName(clauseTabStr, "deviationscore");
                        double maxDeviationScore = 0, categoryDeviationScore = 0;
                        HashMap<Integer, Integer> clauseAndDeviationScore = new HashMap<>();
                        logger.info("Store max deviation score");
                        for (int i = 0; i < extractedClauses; i++) {
                            int categoryId = Integer.parseInt(clauseTabJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(clauseColumnId)).get("value").toString().split(":;")[1]);
                            double deviationScore = Double.parseDouble(clauseTabJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(deviationScoreColumnId)).get("value").toString().split(":;")[0]);
                            if (categoryId == map.get(map.keySet().toArray()[0])) {
                                if (deviationScore > maxDeviationScore) {
                                    maxDeviationScore = deviationScore;
                                    int deviationScoreOfClause = Integer.parseInt(String.valueOf(maxDeviationScore).split("[.]")[0]);
                                    clauseAndDeviationScore.put(categoryId, deviationScoreOfClause);
                                }
                            } else if (categoryId == map.get(map.keySet().toArray()[1])) {
                                if (deviationScore > categoryDeviationScore) {
                                    categoryDeviationScore = deviationScore;
                                    int deviationScoreOfClause = Integer.parseInt(String.valueOf(categoryDeviationScore).split("[.]")[0]);
                                    clauseAndDeviationScore.put(categoryId, deviationScoreOfClause);
                                }
                            }
                        }

                        //Now Running the Query to check deviation score is getting saved in "category_deviation_score_mapping_table"
                        try {
                            Thread.sleep(11000);
                            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
                            String query1 = "select category_id,max_deviation_score from category_deviation_score_mapping_table where adr_id=" + docId + " ";
                            logger.info("Query Formed:" +query1);
                            List<List<String>> documentData = postgreSQLJDBC.doSelect(query1);
                            logger.info(String.valueOf(documentData));
                            int categoryId;
                            double deviationScore;
                            for (int i = 0; i < documentData.size(); i++) {
                                int j = 0;
                                while (j < 1) {
                                    categoryId = Integer.parseInt(documentData.get(i).get(j));
                                    logger.info(String.valueOf(categoryId));
                                    j++;
                                    deviationScore = Double.parseDouble(documentData.get(i).get(j)) * 100;
                                    int finalDeviation = Integer.parseInt(String.valueOf(deviationScore).split("[.]")[0]);
                                    logger.info(String.valueOf(finalDeviation));
                                    clauseDeviationInDB.put(categoryId, finalDeviation);
                                }
                            }
                            boolean dataSavedAccuratelyInDB = clauseAndDeviationScore.equals(clauseDeviationInDB);
                            csAssert.assertTrue(dataSavedAccuratelyInDB, "Incorrect Data in DB and in clause tab for deviation score");

                        }
                        catch (Exception e)
                        {
                            csAssert.assertTrue(false,"Exception while fetching deviation score from category deviation score table" + e.getMessage());
                        }
                    }
                    catch (Exception e)
                    {
                        csAssert.assertTrue(false,"Clause Tab Data API is not working" + e.getMessage());
                    }
                } else {
                    csAssert.assertTrue(false, "No Category is mapped with this project " + projectID);
                }

            }

        }
        catch (Exception e){
            csAssert.assertTrue(false,"Exception occured while hitting list data API of AE entity");
        }
        csAssert.assertAll();

    }

    @Test(priority=1,dependsOnMethods = "checkDeviationScoreForContract")
    public void checkContractDeviationScore() {
        CustomAssert csAssert = new CustomAssert();
        //Checking the Maximum Deviation Score in Contract Deviation Score Table
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
        try {
            postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            String query2 = "select category_id,max_deviation_score from contract_deviation_score_mapping where contract_id=" + contractId + "";
            List<List<String>> contractData = postgreSQLJDBC.doSelect(query2);
            int category;
            double deviationScoreForContract;
            HashMap<Integer, Integer> contractLevelDeviation = new HashMap<>();
            for (int i = 0; i < contractData.size(); i++) {
                int j = 0;
                while (j < 1) {
                    category = Integer.parseInt(contractData.get(i).get(j));
                    j++;
                    deviationScoreForContract = Double.parseDouble(contractData.get(i).get(j)) * 100;
                    int finalDeviation = Integer.parseInt(String.valueOf(deviationScoreForContract).split("[.]")[0]);
                    contractLevelDeviation.put(category, finalDeviation);
                }
            }
            boolean dataSavedForContractDeviation = contractLevelDeviation.equals(clauseDeviationInDB);
            csAssert.assertTrue(dataSavedForContractDeviation, "Incorrect Data in both the tables(Clause deviation and contract) for deviation score");
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while fetching deviation score from contract deviation score mapping table" + e.getMessage());

        }
        finally {
            postgreSQLJDBC.closeConnection();
            logger.info("Deleting Newly Created Contract Entity");
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
        csAssert.assertAll();
    }

}
