package com.sirionlabs.helper.invoice;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


public class ForecastUploadHelper {

	private final static Logger logger = LoggerFactory.getLogger(ForecastUploadHelper.class);
	private static Integer templateId = 1003; // hardcode @todo
	private static Integer forecastEntityTypeId = 180; // hardcoded  @todo
	private static Integer contractTypeId = 61;


	public static String uploadSheet(String filePath, String fileName, int contractId) {
		try {
			contractTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
			return BulkTemplate.uploadForecastTemplate(filePath, fileName, contractTypeId, contractId, forecastEntityTypeId, templateId);
		} catch (Exception e) {
			logger.error("Error while getting Contract Type id : " + e.getMessage());
			return "Error while Uploading ForeCast Sheet"; // dummy return

		}
	}


}
