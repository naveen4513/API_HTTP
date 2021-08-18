package com.sirionlabs.utils.csvutils;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DumpResultsIntoCSV {
	private final static Logger logger = LoggerFactory.getLogger(DumpResultsIntoCSV.class);
	FileUtils fileUtils;
	private String filePath;
	private String fileName;
	private String CSVDelimiter = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVDelimiter");
	private List<String> allHeaders;

	public DumpResultsIntoCSV(String filePath, String fileName, List<String> allHeaders) {
		this.filePath = filePath;
		this.fileName = fileName;
		this.allHeaders = new ArrayList<>();
		this.allHeaders.addAll(allHeaders);
		this.fileUtils = new FileUtils();
		initializeResultFile();
	}

	private void initializeResultFile() {
		try {
			File resultFile = new File(filePath + "/" + fileName);
			if (resultFile.exists()) {
				resultFile.delete();
			}

			if (resultFile.getParentFile().exists() || fileUtils.createDirIfNotExist(resultFile.getParentFile().toString())) {
				resultFile.createNewFile();
			} else {
				logger.error("Failed to create directory " + resultFile.getParent());
			}

			if (resultFile.canWrite()) {
				BufferedWriter oWriter = new BufferedWriter(new FileWriter(filePath + "/" + fileName));
				boolean firstEntry = true;
				for (String header : allHeaders) {
					if (firstEntry) {
						oWriter.write(header);
						firstEntry = false;
					} else {
						oWriter.write(CSVDelimiter);
						oWriter.write(header);
					}
				}
				oWriter.write("\n");
				oWriter.close();
			}
		} catch (Exception e) {
			logger.error("Exception while Initializing File {}. {}", filePath + "/" + fileName, e.getStackTrace());
		}
	}

	private Map<String, String> getResultsMapInCorrectOrder(Map<String, String> resultsMap) {
		Map<String, String> finalMap = new LinkedHashMap<>();
		for (String header : allHeaders) {
			finalMap.put(header, resultsMap.getOrDefault(header, "null"));
		}
		return finalMap;
	}

	public Boolean dumpOneResultIntoCSVFile(Map<String, String> resultsMap) {
		return dumpOneResultIntoCSVFile(resultsMap,this.CSVDelimiter);
	}

	public Boolean dumpOneResultIntoCSVFile(Map<String, String> resultsMap, String CSVDelimiter) {
		Boolean flag = false;
		try {
			Map<String, String> finalMap = getResultsMapInCorrectOrder(resultsMap);
			File resultFile = new File(filePath + "/" + fileName);
			if (!resultFile.exists()) {
				logger.error("Result file Not Found \'{}\'", filePath + "/" + fileName);
				return false;
			}
			if (resultFile.canWrite()) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(filePath + "/" + fileName, true));
				boolean firstEntry = true;
				for (Map.Entry<String, String> data : finalMap.entrySet()) {
					if (firstEntry) {
						writer.write(data.getValue());
						firstEntry = false;
					} else {
						writer.write(CSVDelimiter);
						writer.write(data.getValue());
					}
				}
				writer.write("\n");
				writer.close();
				flag = true;
			}
		} catch (Exception e) {
			logger.error("Exception while Dumping Result into File {}. {}", filePath + "/" + fileName, e.getStackTrace());
		}
		return flag;
	}

	public void dumpAllResultsIntoCSVFile(List<Map<String, String>> allResultsMap) {
		try {
			File resultFile = new File(filePath + "/" + fileName);
			if (!resultFile.exists()) {
				logger.error("Result file Not Found \'{}\'", filePath + "/" + fileName);
				return;
			}
			if (resultFile.canWrite()) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(filePath + "/" + fileName, true));
				for (Map<String, String> oneResultMap : allResultsMap) {
					Map<String, String> finalMap = getResultsMapInCorrectOrder(oneResultMap);
					boolean firstEntry = true;
					for (Map.Entry<String, String> data : finalMap.entrySet()) {
						if (firstEntry) {
							writer.write(data.getValue());
							firstEntry = false;
						} else {
							writer.write(CSVDelimiter);
							writer.write(data.getValue());
						}
					}
					writer.write("\n");
				}
				writer.write("\n");
				writer.close();
			}
		} catch (Exception e) {
			logger.error("Exception while Dumping All Results into File {}. {}", filePath + "/" + fileName, e.getStackTrace());
		}
	}
}