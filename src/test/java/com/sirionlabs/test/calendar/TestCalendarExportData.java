package com.sirionlabs.test.calendar;

import com.sirionlabs.api.calendar.CalendarData;
import com.sirionlabs.api.calendar.CalendarExportData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.collections.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by akshay.rohilla on 7/11/2017.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestCalendarExportData extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestCalendarExportData.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static int month;
	private static int year;
	private static String calendarA;

	@BeforeClass(groups = { "minor" })
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CalendarConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("CalendarExportDataConfigFileName");
		month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));
		calendarA = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendarA");

		testCasesMap = getTestCasesMapping();
	}

	@Test(groups = { "minor" })
	public void testCalendarExportData() {
		CustomAssert csAssert = new CustomAssert();
		boolean exportDataPass = false;
		CalendarExportData calExportObj = new CalendarExportData();
		CalendarData calData = new CalendarData();

		month = 3;
		year = 2020;

		try {
			logger.info("Hitting Calendar Export Data for Month {} and Year {}", month, year);
			HttpResponse response = calExportObj.hitCalendarExportData(month, year, calendarA);
			logger.debug(response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();

			logger.info("Verifying Download");
			for (Header oneHeader : headers) {
				logger.debug(oneHeader.toString());
				if (oneHeader.toString().contains("Content-Disposition"))
					exportDataPass = true;
			}

			if (!exportDataPass) {
				csAssert.assertTrue(false, "Content-Disposition not found in Headers for Month " + month + " and Year " + year);
			}

			boolean downloadPass = verifyCalendarDownload();

			if (!downloadPass) {
				csAssert.assertTrue(false, "Calendar Download Failed for Month " + month + "and Year " + year);
			}

			calData.hitCalendarData(month, year, calendarA);
			JSONArray arr = new JSONArray(calData.getCalendarDataJsonStr());
			validateCalendarDownload(arr,csAssert);

		} catch (Exception e) {
			logger.error("Exception while Verifying Calendar Export Data for Month {} and Year {}. {}", month, year, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying Calendar Export Data for Month " + month + " and Year " + year + ". " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testCalendarExportData"), csAssert);
		csAssert.assertAll();
	}

	private boolean validateCalendarDownload(JSONArray array, CustomAssert csAssert) {
		boolean downloadPass = false;
		try {
			String filePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
			String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilename");
			String file = fileName + ".xlsx";
			List<List<String>> xlList = XLSUtils.getExcelDataOfMultipleRows(filePath, file, "Calendar",6, new Long(XLSUtils.getNoOfRows(filePath, file, "Calendar")).intValue());

            for(List<String> item: xlList) {
                String seqId = item.get(0);
                for(int i = 0  ; i < seqId.length() ; i++){
                    if(seqId.charAt(i)>= 48 && seqId.charAt(i) <=57){
                        seqId = seqId.substring(i);
                        break;
                    }
                }
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    String jsonSeqId = jsonObject.getString("clientEntitySeqId");
                    if (seqId.equals(jsonSeqId)) {
                        String supplier = jsonObject.getString("supplier");
                        String entityStatus = jsonObject.getString("entityStatus");
                        String title = jsonObject.getString("title");
                        csAssert.assertEquals(supplier, item.get(5), "Data didn't match for Supplier field : Actual - " + supplier +" Expected - "+item.get(5)+"+"+ " for entity under test - " + item.get(0));
                        csAssert.assertEquals(entityStatus, item.get(4), "Data didn't match for entityStatus field : Actual - " + entityStatus + " Expected - "+item.get(4)+"+"+  "for entity under test - " + item.get(0));
                        csAssert.assertEquals(title, item.get(2), "Data didn't match for title field : Actual - " + title + " Expected - "+item.get(2)+"+"+  "for entity under test - " + item.get(0));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Calendar Export Data Download for Month {} and Year {}. {}", month, year, e.getMessage());
        }
        return downloadPass;
    }

	private boolean verifyCalendarDownload() {
		boolean downloadPass = false;
		try {
			String filePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
			String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilename");
			String file = filePath + "/" + fileName + ".xlsx";
			CalendarExportData downloadObj = new CalendarExportData();
			Boolean downloadResponse = downloadObj.downloadCalendarDataFile2(month, year, "false", file);
			if (downloadResponse != null) {
				File downloadedFile = new File(file);
				if (downloadedFile.exists())
					downloadPass = true;
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Calendar Export Data Download for Month {} and Year {}. {}", month, year, e.getStackTrace());
		}
		return downloadPass;
	}
}
