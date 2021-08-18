package com.sirionlabs.helper.Integration;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class CsvDataCreation {

	private final static Logger logger = LoggerFactory.getLogger(CsvDataCreation.class);

	String integrationDataCreationFilePath;
	String integrationDataCreationFileName;
	String integrationTestDataCSVFilePath;
	String integrationTestDataCSVFileName;
	String integrationTestDataCSVDelimiter;
	Integer totalRecords;
	DumpResultsIntoCSV dumpResultsObj;
	List<String> csvHeaders;
	Boolean isEmptyValueAllowed;
	private static final AtomicLong LAST_TIME_MS = new AtomicLong();


	@BeforeClass
	public void setConfig() {
		try {
			integrationDataCreationFilePath = ConfigureConstantFields.getConstantFieldsProperty("IntegrationTestDataCreationFilePath");
			integrationDataCreationFileName = ConfigureConstantFields.getConstantFieldsProperty("IntegrationTestDataCreationFileName");
			integrationTestDataCSVFilePath = ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "csvfilepath");
			integrationTestDataCSVFileName = ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "csvfilename");
			integrationTestDataCSVDelimiter = ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "csvdelimiter");
			totalRecords = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "totalrecords"));
			isEmptyValueAllowed = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "isemptyvalueallowed"));

			csvHeaders = getCsvFileHeaders("csvfileheaders");
			dumpResultsObj = new DumpResultsIntoCSV(integrationTestDataCSVFilePath, integrationTestDataCSVFileName, csvHeaders);

		} catch (ConfigurationException e) {
			logger.error("Exception occurred while setting config properties for csv data creation. error : {}", e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void createSupplierTestDataForRBI() {
		try {


			List<String> headersWithUniqueValues = getCsvFileHeaders("headerswithuniquevalues");

			for (int rowCount = 0; rowCount < totalRecords; rowCount++) {
				Map<String, String> csvDataMap = new HashMap<>();

				for (int i = 0; i < csvHeaders.size(); i++) {
					String headerName = csvHeaders.get(i);

					if (headersWithUniqueValues.contains(headerName)) {
						String uniqueValue = getUniqueValue(headerName);
						csvDataMap.put(headerName, uniqueValue);
						continue;
					}
					if (ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "fixed values", headerName) != null) {
						String fixedValue = getFixedValue(headerName);
						csvDataMap.put(headerName, fixedValue);
						continue;
					}
					if (getDefaultValue(headerName) != null) {
						String defaultValue = getDefaultValue(headerName);
						csvDataMap.put(headerName, defaultValue + rowCount);
						continue;
					}
					if (!isEmptyValueAllowed)
						csvDataMap.put(headerName, headerName + rowCount + 1);
				}
				logger.info("writing row number : {}", rowCount + 1);
				dumpResultsIntoCSV(csvDataMap);
			}
			logger.info("csv file created successfully. file :{}", integrationTestDataCSVFilePath + "/" + integrationTestDataCSVFileName);
		} catch (ConfigurationException e) {
			logger.error("Exception while creating test data. error : {}", e.getMessage());
			e.printStackTrace();
		}
	}

	private void dumpResultsIntoCSV(Map<String, String> resultsMap) {
		try {
			String[] allColumns = new String[csvHeaders.size()];
			allColumns = csvHeaders.toArray(allColumns);

			for (String column : allColumns) {
				if (!resultsMap.containsKey(column))
					resultsMap.put(column, "");
			}
			dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap, integrationTestDataCSVDelimiter);
		} catch (Exception e) {
			logger.error("Exception occurred while dumping into csv. error : {}", e.getMessage());
			e.printStackTrace();
		}
	}

	private List<String> getCsvFileHeaders(String propertyName) throws ConfigurationException {
		String value = ParseConfigFile.getValueFromConfigFile(this.integrationDataCreationFilePath, this.integrationDataCreationFileName, propertyName);
		List<String> headerList = new ArrayList<String>();

		if (!value.trim().equalsIgnoreCase("")) {
			String headers[] = ParseConfigFile.getValueFromConfigFile(this.integrationDataCreationFilePath, this.integrationDataCreationFileName, propertyName).split(",");

			for (int i = 0; i < headers.length; i++)
				headerList.add(headers[i].trim());
		}
		return headerList;
	}

	private String getFixedValue(String headerName) {
		String value = null;
		try {
			String fixedValues[] = ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "fixed values", headerName).split(",");
			int randomValue[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, fixedValues.length, 1);
			value = fixedValues[randomValue[0]];

		} catch (Exception e) {
			logger.error("Exception while getting fixed value. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return value;
	}

	private String getDefaultValue(String headerName) {
		String value = null;
		try {
			value = ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "default values", headerName);
		} catch (Exception e) {
			logger.error("Exception while getting default value for header : {},error : {}", headerName, e.getCause());
			e.printStackTrace();
		}
		return value;
	}

	private String getUniqueValue(String headerName) {
		String uniqueValue = null;

		try {
			Long timeStamp = uniqueCurrentTimeMS();
			String prefix = ParseConfigFile.getValueFromConfigFile(integrationDataCreationFilePath, integrationDataCreationFileName, "prefix", headerName);
			if (prefix != null) {
				uniqueValue = prefix + timeStamp.toString();
			} else
				uniqueValue = timeStamp.toString();
		} catch (Exception e) {
			logger.error("Exception while setting unique value for header :{}. error : {}", headerName, e.getCause());
			e.printStackTrace();
		}
		return uniqueValue;
	}

	private long uniqueCurrentTimeMS() {
		long now = System.currentTimeMillis();
		while (true) {
			long lastTime = LAST_TIME_MS.get();
			if (lastTime >= now)
				now = lastTime + 1;
			if (LAST_TIME_MS.compareAndSet(lastTime, now))
				return now;
		}
	}
}