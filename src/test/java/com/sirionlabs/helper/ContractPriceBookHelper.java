package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class ContractPriceBookHelper {

	private final static Logger logger = LoggerFactory.getLogger(ContractPriceBookHelper.class);
	private static String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractPriceBookHelperConfigFilePath");
	private static String configFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractPriceBookHelperConfigFileName");

	public static HashSet<Integer> getAllParentIdsOfGroupByCategoryForServiceData(String groupByCategoryName, Integer serviceDataId) {
		HashSet<Integer> allParentIdsOfGroupByCategory = new HashSet<>();

		try {
			String showPageObjectNameMapping = getShowPageObjectNameMappingForGroupByCategory(groupByCategoryName);

			if (showPageObjectNameMapping != null) {
				int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
				String showFieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageObjectNameMapping, serviceDataEntityTypeId);

				if (showFieldHierarchy != null) {
					showFieldHierarchy = showFieldHierarchy.trim().substring(0, showFieldHierarchy.lastIndexOf("->")).trim();
					String lastObjectName = showFieldHierarchy.substring(showFieldHierarchy.lastIndexOf(">") + 1, showFieldHierarchy.lastIndexOf("[")).trim();
					String lastObjectType = showFieldHierarchy.substring(showFieldHierarchy.lastIndexOf("[") + 1, showFieldHierarchy.lastIndexOf("]")).trim();

					logger.info("Hitting Show API for Service Data Id {}.", serviceDataId);
					Show showObj = new Show();
					showObj.hitShow(serviceDataEntityTypeId, serviceDataId);
					String showResponse = showObj.getShowJsonStr();

					if (ParseJsonResponse.validJsonResponse(showResponse)) {
						String actualValue = ShowHelper.getActualValue(showResponse, showFieldHierarchy);

						if (actualValue != null && ParseJsonResponse.validJsonResponse(actualValue)) {
							if (lastObjectType.equalsIgnoreCase("object")) {
								//Handle JSONObject
								JSONObject jsonObj = new JSONObject(actualValue);
								if (lastObjectName.equalsIgnoreCase("id")) {
									allParentIdsOfGroupByCategory.add(jsonObj.getInt("values"));
								} else {
									allParentIdsOfGroupByCategory.add(jsonObj.getInt("id"));
								}
							} else {
								//Handle JSONArray
								JSONArray jsonArr = new JSONArray(actualValue);

								for (int i = 0; i < jsonArr.length(); i++) {
									allParentIdsOfGroupByCategory.add(jsonArr.getJSONObject(i).getInt("id"));
								}
							}
						} else {
							logger.error("Couldn't get Actual Value at Hierarchy [{}]", showFieldHierarchy);
						}
					} else {
						logger.error("Show API Response for Service Data Id {} is an Invalid JSON.", serviceDataId);
					}
				} else {
					logger.error("Couldn't get Show Field Hierarchy for Field {}.", showPageObjectNameMapping);
				}
			} else {
				logger.error("Couldn't get Show Page Object Name Mapping for Group By Field {}.", groupByCategoryName);
			}
		} catch (Exception e) {
			logger.error("Exception while getting all Parent Ids of Group By Field {} for Service Data Id {}. {}", groupByCategoryName, serviceDataId, e.getStackTrace());
		}
		return allParentIdsOfGroupByCategory;
	}

	private static String getShowPageObjectNameMappingForGroupByCategory(String groupByCategoryName) {
		try {
			return ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "groupByCategoryNameShowPageObjectMapping",
					groupByCategoryName.trim());
		} catch (Exception e) {
			logger.error("Exception while getting Show Page Object Name Mapping for Group By Field {}. {}", groupByCategoryName, e.getStackTrace());
			return null;
		}
	}

	public static List<Integer> getAllServiceDataIdsOfContractFromPriceBookTab(int contractId) {
		return getAllServiceDataIdsOfContractFromPriceBookTab(null, contractId);
	}

	public static List<Integer> getAllServiceDataIdsOfContractFromPriceBookTab(String tabListResponse, int contractId) {
		List<Integer> allServiceDataIds = new ArrayList<>();

		try {
			if (tabListResponse == null) {
				int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
				int contractPriceBookTabId = TabListDataHelper.getIdForTab("contracts price book");
				ListRendererTabListData tabListObj = new ListRendererTabListData();
				tabListObj.hitListRendererTabListData(contractPriceBookTabId, contractEntityTypeId, contractId,
						"{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}");
				tabListResponse = tabListObj.getTabListDataJsonStr();
			}

			if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
				JSONObject jsonObj = new JSONObject(tabListResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				if (jsonArr.length() > 0) {
					String pivotalColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "pivotalcolumn");

					if (pivotalColumnId != null) {
						for (int i = 0; i < jsonArr.length(); i++) {
							jsonObj = jsonArr.getJSONObject(i);
							String[] valueArr = jsonObj.getJSONObject(pivotalColumnId).getString("value").trim().split(Pattern.quote(":;"));

							if (valueArr.length > 1) {
								allServiceDataIds.add(Integer.parseInt(valueArr[1]));
							}
						}
					} else {
						logger.error("Couldn't get Id of Column [pivotalColumn] from TabListData Response.");
					}
				} else {
					logger.info("No Data found in TabListData Response for Contract Id {}", contractId);
				}
			} else {
				logger.error("TabListData API Response for Contract Id {} and Price Book Tab is an Invalid JSON.", contractId);
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Service Data Ids of Contract Id {}. {}", contractId, e.getStackTrace());
		}
		return allServiceDataIds;
	}

	public static Integer getServiceDataIdFromSeqNoInContractPriceBookTab(String tabListDataResponse, int serviceDataSeqNo) {
		Integer serviceDataId = -1;

		try {
			JSONObject jsonObj = new JSONObject(tabListDataResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("data");

			if (jsonArr.length() > serviceDataSeqNo) {
				String pivotalColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "pivotalcolumn");

				if (pivotalColumnId != null) {
					String[] valueArr = jsonArr.getJSONObject(serviceDataSeqNo).getJSONObject(pivotalColumnId).getString("value").trim().split(Pattern.quote(":;"));

					if (valueArr.length > 1)
						serviceDataId = Integer.parseInt(valueArr[1].trim());
				} else {
					logger.error("Couldn't get Id for Pivotal Column.");
				}
			} else {
				logger.error("Couldn't find Service Data Seq No. {}", serviceDataSeqNo);
			}
		} catch (Exception e) {
			logger.error("Exception while getting Service Data Id for Seq No. {}. {}", serviceDataSeqNo, e.getStackTrace());
		}
		return serviceDataId;
	}
}
