package com.sirionlabs.helper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SortingHelper {


	private final static Logger logger = LoggerFactory.getLogger(SortingHelper.class);

	// this function will verify whether records is sorted properly based of sortingType (ex : asc or desc)
	public static boolean isRecordsSortedProperly(List<String> allRecords, String type, String columnName, String sortingType) {


		try {
			logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
			boolean strictAscOrDescCheck = false;  // for verifying strict sorting for IDs (as of now )

			if (columnName.contentEquals("ID"))  // Since Duplicate Can't be allowed in this case
			{
				strictAscOrDescCheck = true;

			}

			String[] allRecordsArray = allRecords.toArray(new String[allRecords.size()]);
			int[] checkForConsecutivityOfNull = new int[allRecordsArray.length]; // since it's either null first or null last type to sorting this will return the index of comparable record
			int index = 0;
			int firstIndexToTest = 0;
			int lastIndexToTest = allRecordsArray.length;
			boolean allRecordsNotNull = false;

			for (String record : allRecordsArray) {
				if (record == null)
					checkForConsecutivityOfNull[index++] = 0;
				else {
					checkForConsecutivityOfNull[index++] = 1;
					allRecordsNotNull = true;
				}
			}

			if (!allRecordsNotNull) {

				logger.info("All Records Are Null Nothing to Verify ");
				return true;
			}

			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
				for (int i = 0; i < allRecordsArray.length; i++) {
					if (checkForConsecutivityOfNull[i] == 1) {
						firstIndexToTest = i;
						lastIndexToTest = allRecords.size();
						break;
					} else
						continue;
				}

				for (int j = firstIndexToTest; j < allRecordsArray.length; j++) {
					if (checkForConsecutivityOfNull[j] == 0) // again null should not occur since it's asc nulls first
					{
						logger.info("Error:Since It's Asc nulls first type of sorting So null value should occur at beginning only");
						return false;
					}

				}

			}

			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

				for (int i = 0; i < allRecordsArray.length; i++) {
					if (checkForConsecutivityOfNull[i] == 0) {
						firstIndexToTest = 0;
						lastIndexToTest = i;
						break;
					} else
						continue;
				}


				for (int j = lastIndexToTest; j < allRecordsArray.length; j++) {
					if (checkForConsecutivityOfNull[j] == 1) // again null should not occur since it's  desc nulls first
					{
						logger.info("Error:Since It's Desc nulls last type of sorting So null value should occur at last only");
						return false;
					}

				}

			}

			List<String> allRecordsExceptNullValues = allRecords.subList(firstIndexToTest, lastIndexToTest);

			if (type.contentEquals("TEXT"))  //if Records Type is Text
			{
				PostgreSQLJDBC jDBCUtils = new PostgreSQLJDBC();

				if (sortingType.toLowerCase().contentEquals("asc nulls first")) {

					boolean sorted = true;
					for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {
						if (!jDBCUtils.compareTwoRecordsForAscOrEqual(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
							logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
							logger.warn("Not Sorting Properly in asc null first where " + allRecordsExceptNullValues.get(j - 1) + " should come after " + allRecordsExceptNullValues.get(j));
							sorted = false;
						}

					}

					return sorted;
				}


				if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

					boolean sorted = true;
					for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {
						if (!jDBCUtils.compareTwoRecordsForDescOrEqual(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
							logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
							logger.warn("Not Sorting Properly in desc null first where " + allRecordsExceptNullValues.get(j) + " should come before " + allRecordsExceptNullValues.get(j - 1));
							sorted = false;
						}

					}

					return sorted;
				}


				logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
				return true;

			}

			if (type.contentEquals("NUMBER")) //if Records Type is number
			{

				if (sortingType.toLowerCase().contentEquals("asc nulls first")) {

					boolean sorted = true;
					for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {

						try {
							Float previousValue = Float.parseFloat(allRecordsExceptNullValues.get(j - 1));
							Float nextValue = Float.parseFloat(allRecordsExceptNullValues.get(j));
							if (strictAscOrDescCheck) {
								if (nextValue <= previousValue) {
									logger.info("Verifying Sorted Results for Column name : {} ", columnName);
									logger.error("Not Sorting Properly in asc null first where " + nextValue + " should come after " + previousValue);
									sorted = false;
									break;
								}
							} else {
								if (nextValue < previousValue) {
									logger.info("Verifying Sorted Results for Column name : {} ", columnName);
									logger.error("Not Sorting Properly in asc null first where " + nextValue + " should come after " + previousValue);
									sorted = false;
									break;
								}
							}
						} catch (Exception e) {
							logger.error("Fatal Error : Exception occured while converting the number formate data in Float : {}", e.getMessage());
							return false;
						}


					}

					return sorted;

				}


				if (sortingType.toLowerCase().contentEquals("desc nulls last")) {


					boolean sorted = true;
					for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {

						try {
							Float previousValue = Float.parseFloat(allRecordsExceptNullValues.get(j - 1));
							Float nextValue = Float.parseFloat(allRecordsExceptNullValues.get(j));

							if (strictAscOrDescCheck) {
								if (nextValue >= previousValue) {
									logger.info("Verifying Sorted Results for Column name : {} ", columnName);
									logger.error("Not Sorting Properly in desc null first where " + nextValue + " should come before " + previousValue);
									sorted = false;
									break;
								}
							} else {
								if (nextValue > previousValue) {
									logger.info("Verifying Sorted Results for Column name : {} ", columnName);
									logger.error("Not Sorting Properly in desc null first where " + nextValue + " should come before " + previousValue);
									sorted = false;
									break;
								}
							}
						} catch (Exception e) {
							logger.error("Fatal Error : Exception occured while converting the number formate data in Float : {}", e.getMessage());
							return false;
						}
					}

					return sorted;

				}


				logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
				return true;

			}

			if (type.contentEquals("DATE")) //todo laters since there is no standard formate
			{
				return true; // hack as of now

//			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {
//
//
//			}
//			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
//
//
//			}
//
//			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
//			return true;

			}

		} catch (Exception e) {
			logger.error("Exception while checking if records are sorted properly or not ");
			return false;
		}

		logger.info("Column DataType of Records is not matching with any of the above-mentioned condition");
		return true;

	}


	// this function will verify whether last value of prev page is less than or equal to the first Value of Current Page in case of asc sorting or vice versa
	public static boolean isPaginationCorrect(String lastRecordOfPrevPage, String firstRecordOfCurrentPage, String columnName, String type, String sortingType) {


		try {
			logger.debug("Verifying whether lastRecord of Prev Page is less than of equal to the first Value of Current Page in case of asc sorting or vice versa ");
			logger.debug("lastRecordOfPrevPage :{}", lastRecordOfPrevPage);
			logger.debug("firstRecordOfCurrentPage :{}", firstRecordOfCurrentPage);

			boolean strictAscOrDescCheck = false;  // for verifying strict sorting for IDs (as of now )

			if (columnName.contentEquals("ID"))  // Since Duplicate Can't be allowed in this case
			{
				strictAscOrDescCheck = true;

			}

			if (lastRecordOfPrevPage == null || firstRecordOfCurrentPage == null) {
				logger.info("either of lastRecordOfPrevPage or firstRecordOfCurrentPage is null , can't compare");
				return true;
			}


			if (type.contentEquals("TEXT"))  //if Records Type is Text
			{

				PostgreSQLJDBC jDBCUtils = new PostgreSQLJDBC();
				if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
					boolean sorted = true;
					if (!jDBCUtils.compareTwoRecordsForAscOrEqual(lastRecordOfPrevPage, firstRecordOfCurrentPage)) {
						sorted = false;
					}

					return sorted;
				}


				if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

					boolean sorted = true;
					if (!jDBCUtils.compareTwoRecordsForDescOrEqual(lastRecordOfPrevPage, firstRecordOfCurrentPage)) {
						sorted = false;
					}

					return sorted;
				}


				logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
				return true;

			}

			if (type.contentEquals("NUMBER")) //if Records Type is number
			{

				if (sortingType.toLowerCase().contentEquals("asc nulls first")) {

					boolean sorted = true;

					Float previousValue = Float.parseFloat(lastRecordOfPrevPage);
					Float nextValue = Float.parseFloat(firstRecordOfCurrentPage);
					if (strictAscOrDescCheck) {
						if (nextValue <= previousValue) {
							sorted = false;

						}
					} else {
						if (nextValue < previousValue) {
							sorted = false;

						}
					}


					return sorted;

				}


				if (sortingType.toLowerCase().contentEquals("desc nulls last")) {


					boolean sorted = true;

					Float previousValue = Float.parseFloat(lastRecordOfPrevPage);
					Float nextValue = Float.parseFloat(firstRecordOfCurrentPage);
					if (strictAscOrDescCheck) {
						if (nextValue >= previousValue) {
							sorted = false;

						}
					} else {
						if (nextValue > previousValue) {
							sorted = false;

						}
					}


					return sorted;

				}


				logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
				return true;

			}

			if (type.contentEquals("DATE")) //todo laters since there is no standard formate
			{
				return true; // hack as of now

//			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {
//
//
//			}
//			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
//
//
//			}
//
//			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
//			return true;

			}
		} catch (Exception e) {
			logger.error("Exception while checking if pagination is correct or not ");
			return false;
		}


		logger.info("Column DataType of Records is not matching with any of the above-mentioned condition");
		return true;

	}


	public static List<String> getAllRecordForParticularColumns(int columnID, String listDataJsonStr) {
		List<String> allRecords = new ArrayList<>();
		JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		for (int i = 0; i < noOfRecords; i++) {
			JSONObject recordObj = listDataResponseObj.getJSONArray("data").getJSONObject(i);
			if (recordObj.getJSONObject(Integer.toString(columnID)).get("value").equals(JSONObject.NULL)) {
				allRecords.add(null);
			} else {
				String record = (String) recordObj.getJSONObject(Integer.toString(columnID)).get("value");
				// if record is not text
				if (record.contains(":;")) {
					String[] splitDbId = record.split(":;");
					allRecords.add(splitDbId[0]);
				} else
					allRecords.add(record);
			}

		}

		logger.debug("All Records is : {}", allRecords);
		return allRecords;

	}


}
