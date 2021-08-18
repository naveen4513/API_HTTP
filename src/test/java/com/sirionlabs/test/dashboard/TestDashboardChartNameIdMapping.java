package com.sirionlabs.test.dashboard;

import com.sirionlabs.api.dashboard.DashboardCharts;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.csvutils.CSVResults;
import com.sirionlabs.utils.csvutils.DashboardChartIdMappingResultInfo;
import com.sirionlabs.utils.csvutils.ResultInfoClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class TestDashboardChartNameIdMapping {
	private final static Logger logger = LoggerFactory.getLogger(TestDashboardChartNameIdMapping.class);
	static String dashboardChartIdMappingFilePath;
	static String dashboardChartIdMappingFileName;
	CSVResults csvResultsObj = null;

	@BeforeClass
	public void setCSVGeneration() {
		logger.debug("In BeforeClass");
		//Setting dashboard CSV report generation
		dashboardChartIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("dashboardChartIdMappingFilePath");
		dashboardChartIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("dashboardChartIdMappingFileName");
		csvResultsObj = new CSVResults();
		ResultInfoClass resultInfoClass = new DashboardChartIdMappingResultInfo();
		csvResultsObj.initializeResultCsvFile(dashboardChartIdMappingFilePath + "/" + dashboardChartIdMappingFileName, resultInfoClass);
	}

	@Test
	public void generateChartIdMappingFile() {
		CustomAssert csAssert = new CustomAssert();
		logger.info("Hitting Dashboard Charts Api for All DashboardCharts");
		try {
			Map<Integer, String> allChartsIdNameMap = new HashMap<Integer, String>();
			DashboardCharts chartObj = new DashboardCharts();
			chartObj.hitDashboardCharts();
			String chartResponse = chartObj.getDashboardChartsJsonStr();
			logger.info("chart response {}", chartResponse);
			if (APIUtils.validJsonResponse(chartResponse, "[dashboard charts response]")) {
				allChartsIdNameMap = chartObj.getAllChartIdNameMapping(chartResponse);
				pushReportsResultToCSV(allChartsIdNameMap);
			} else {
				logger.error("Dashboard chart response is not valid json.");
				csAssert.assertTrue(false, "Dashboard chart response is not valid json");
			}
		} catch (Exception e) {
			logger.error("Exception occured in generateChartIdMappingFile {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while getting chartId mapping");
		}
		csAssert.assertAll();
	}

	private void pushReportsResultToCSV(Map<Integer, String> dashboardReportMap) {
		logger.info("Generating CSV File");
		for (Map.Entry<Integer, String> entryMap : dashboardReportMap.entrySet()) {
			ResultInfoClass dashboardResultInfoObj = new DashboardChartIdMappingResultInfo(entryMap.getKey().toString(), entryMap.getValue());
			csvResultsObj.writeReportsToCSVFile(dashboardChartIdMappingFilePath + "/" + dashboardChartIdMappingFileName, dashboardResultInfoObj);
		}
		logger.info("Chart Id Mapping file generated at {}", dashboardChartIdMappingFilePath + "/" + dashboardChartIdMappingFileName);
	}
}
