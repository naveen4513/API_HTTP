package com.sirionlabs.test.bePod;

import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestContractCreatedForSIR202326 {

	private final static Logger logger = LoggerFactory.getLogger(TestContractCreatedForSIR202326.class);


	/*
	TC-C63577: Covered in Contract Entity Creation Suite.
	TC-C63579: Covered in Entity Edit Suite.
	TC-C63580: Covered in Listing Data Validation Suite.
	TC-C63581: Covered in Filter Data Validation Suite.
	TC-C63582: Covered in Metadata Search Suite.
	 */

	/*
	TC-C63521: Verify that Created For Column is multi select in Report Listing.
	 */
	@Test
	public void testC63521() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C63521: Verify that Created For Column is Multi-Select in Report Listing.");

			ReportsListHelper reportsHelperObj = new ReportsListHelper();
			ReportsDefaultUserListMetadataHelper reportsDefaultListHelperObj = new ReportsDefaultUserListMetadataHelper();

			List<Map<String, String>> allReportsOfContract = reportsHelperObj.getAllReportsOfEntity("contracts");

			if (allReportsOfContract.isEmpty()) {
				throw new SkipException("Couldn't get Reports of Contract.");
			}

			for (Map<String, String> reportMap : allReportsOfContract) {
				if (reportMap.get("isManualReport").equalsIgnoreCase("false")) {
					Integer reportId = Integer.parseInt(reportMap.get("id"));
					String reportName = reportMap.get("name");

					String reportDefaultUserListMetadataResponse = reportsDefaultListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);

					if (ParseJsonResponse.validJsonResponse(reportDefaultUserListMetadataResponse)) {
						Boolean isCreatedForFieldPresentInReport = reportsDefaultListHelperObj.isFieldPresentInDefaultUserListMetadataAPIResponse(
								reportDefaultUserListMetadataResponse, "createdFor");

						if (isCreatedForFieldPresentInReport == null) {
							throw new SkipException("Couldn't find whether Created For Field is present or not in DefaultUserListMetadata API Response for Report [" +
									reportName + "] having Id " + reportId);
						}

						if (isCreatedForFieldPresentInReport) {
							String uiTypeValue = reportsDefaultListHelperObj.getFilterMetadataPropertyValueFromQueryName(reportDefaultUserListMetadataResponse,
									"createdFor", "uiType");

							if (uiTypeValue == null || !uiTypeValue.equalsIgnoreCase("MULTISELECT")) {
								csAssert.assertTrue(false, "Created For Field is not of Type Multi-Select for Report [" + reportName + "] having Id " +
										reportId);
							}
						}
					} else {
						csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Report [" + reportName + "] having Id " + reportId +
								" is an Invalid JSON.");
					}
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating TC-C63521. " + e.getMessage());
		}
		csAssert.assertAll();
	}
}