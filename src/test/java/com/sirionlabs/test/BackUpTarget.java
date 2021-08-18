package com.sirionlabs.test;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author manoj.upreti
 */
public class BackUpTarget {
	private final static Logger logger = LoggerFactory.getLogger(BackUpTarget.class);

	@Test
	public void copyTargetTest() {
		logger.info("This Test is to copy the Tagget Folder into a time stamped folder");
		copyTargetFolder();
	}

	/**
	 * @return returns true if the backup is created successfully , otherwise returns false
	 * This method is for keeping the backup of the folder of target folder</br>
	 */
	public boolean copyTargetFolder() {
		logger.debug("Executing Copy Target folder method , and copying target folder to time stamped ");
		String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime());
		String currentDir = System.getProperty("user.dir");
		String projectParentDirName = "java-api-framework";

		File currentDirFile = new File(currentDir);

		while (!currentDirFile.getName().contentEquals(projectParentDirName)) {
			currentDirFile = currentDirFile.getParentFile();
		}

		logger.info("The Current Dir Path is : [ {} ]", currentDirFile.getAbsolutePath());
		logger.info("Copying target dir to taget_timestamp");
		String targetDirPath = currentDirFile.getAbsolutePath() + File.separator + "target";

		logger.info("Create OutputDirectory ");
		String outputDirFilePath = currentDirFile.getAbsolutePath() + File.separator + "Target_Backup";
		File outputDirFile = new File(outputDirFilePath);
		if (!outputDirFile.exists()) {
			logger.info("Creating Directory : [ {] ]", outputDirFilePath);
			boolean dirCreationResult = outputDirFile.mkdirs();
			if (!dirCreationResult) {
				logger.error("[ {} ] Directory creation failed.", outputDirFilePath);
				return false;
			}
		}
		String updatedTargetDirFilePath = outputDirFilePath + File.separator + "target_" + timeStamp;

		File targetDirPathFile = new File(targetDirPath);
		if (targetDirPathFile.exists()) {
			logger.info("The target dir is available [ {} ]  ,Copying to taget_timestamp : [ {} ] ", targetDirPath, updatedTargetDirFilePath);
			try {
				FileUtils.copyDirectory(new File(targetDirPath), new File(updatedTargetDirFilePath));
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}
}
