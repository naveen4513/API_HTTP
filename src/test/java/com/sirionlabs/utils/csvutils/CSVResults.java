package com.sirionlabs.utils.csvutils;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author manoj.upreti
 */
public class CSVResults {
	private final static Logger logger = LoggerFactory.getLogger(CSVResults.class);
	static String CSVDelemeter = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVDelimiter");
	//static String reportRenderCSVDelemeter = ";";

	public void initializeResultCsvFile(String sFileName, ResultInfoClass resultInfoClassObj) {
		try {
			File oFile = new File(sFileName);

			if (oFile.exists()) {
				oFile.delete();
			}
			if (!oFile.exists()) {
				oFile.createNewFile();
			}
			if (oFile.canWrite()) {
				BufferedWriter oWriter = new BufferedWriter(new FileWriter(sFileName));
				boolean firstEntry = true;
				for (Map.Entry<String, String> entryOfMap : resultInfoClassObj.resultDataMap.entrySet()) {
					if (firstEntry) {
						oWriter.write(entryOfMap.getValue());
						firstEntry = false;
					} else {
						oWriter.write(CSVDelemeter);
						oWriter.write(entryOfMap.getValue());
					}
				}
				oWriter.write("\n");
				oWriter.close();
			}
		} catch (IOException oException) {
			throw new IllegalArgumentException("Invalid folder path/File cannot be written: \n" + sFileName);
		}
	}

	public synchronized void writeReportsToCSVFile(String sFileName, ResultInfoClass resultInfoClassObj) {
		try {

			File oFile = new File(sFileName);
			if (!oFile.exists()) {
				logger.error("No CSV File is present for writing the report Result , please check [ {} ]", sFileName);
			}
			if (oFile.canWrite()) {
				BufferedWriter oWriter = new BufferedWriter(new FileWriter(sFileName, true));
				StringBuilder sb = new StringBuilder();
				//oWriter.write (sContent);
				boolean firstEntry = true;
				for (Map.Entry<String, String> entryOfMap : resultInfoClassObj.resultDataMap.entrySet()) {
					if (firstEntry) {
						oWriter.write(entryOfMap.getValue());
						firstEntry = false;
					} else {
						oWriter.write(CSVDelemeter);
						oWriter.write(entryOfMap.getValue());
					}
				}
				oWriter.write("\n");
				oWriter.close();
			}

		} catch (Exception oException) {
			logger.error("Error appending File [{}] cannot be written , got Exception [{}] \n" + sFileName, oException.getStackTrace());
		}
	}

}
