package com.sirionlabs.utils.commonUtils;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DateUtils {
	private final static Logger logger = LoggerFactory.getLogger(DateUtils.class);

	private static final String[] formats = {
			"dd-MM-yyyy",
			"dd-MM-yyyy a z",
			"dd-MM-yyyy hh:mm",
			"dd-MM-yyyy HH:mm:ss",
			"dd-MM-yyyy HH:mm:ss a z",
			"dd-MMM-yyyy",
			"dd-MMM-yyyy a z",
			"dd-MM-yyyy HH:mm:ss",
			"dd-MM-yyyy HH:mm:ss a z",
			"MM-dd-yyyy",
			"MM-dd-yy",
			"MM-dd-yyyy a z",
			"MM-dd-yyyy HH:mm:ss",
			"MM-dd-yyyy HH:mm:ss a z",
			"MMM-dd-yyyy",
			"MMM-dd-yyyy a z",
			"MMM-dd-yyyy HH:mm:ss",
			"MMM-dd-yyyy HH:mm:ss a z",
			"yyyy:MM:dd HH:mm:ss",
			"MMMMM-dd-yyyy",
			"dd-yyyy-MMM HH:mm:ss X"
	};

	public static String getCurrentDateInDDMMYYYYHHMMSS() {
		DateFormat dateFormat = new SimpleDateFormat("MMddyyyyHHmmss");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String getCurrentDateInDDMMYYYY() {
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String getCurrentDateInMM_DD_YYYY() {
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String getCurrentDateInDD_MM_YYYY() {
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String getCurrentDateInMMM_DD_YYYY() {
		DateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String getPreviousMonth() {

		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date date = new Date();
		String dateInMMDDYYYY = dateFormat.format(date);
		String previousMonthInString = "";
		int previousMonthInInteger = -1;

		try {
			String month = dateInMMDDYYYY.split("-")[0];
			int monthInInteger = Integer.parseInt(month);
			if ( monthInInteger == 1) {
				previousMonthInInteger = 12;
			}else {
				previousMonthInInteger = monthInInteger -1;
			}

			previousMonthInString = getMonthinMMM(previousMonthInInteger);
		}catch (Exception e){

		}


		return previousMonthInString;

	}

	//Getting previous date in MMM_DD_YYYY format current date in MMM_DD_YYYY format
	public static String getPreviousDateInMMM_DD_YYYY(String currentDate) {
		String Tokens[] = currentDate.split("-");
		int date = Integer.parseInt(Tokens[1]);
		int month = Integer.parseInt(getMonthindigit(Tokens[0]));
		int year = Integer.parseInt(Tokens[2]);

		date--;

		if (date < 1) {
			month -= 1;

			if (month < 1) {
				month += 12;
				year--;
			}

			String tokens[] = DateUtils.getMonthEndDateInMMDDFormat(month - 1, year).split("/");
			date = Integer.parseInt(tokens[1]);
		}

		String previousDate = "";

		if (date < 10)
			previousDate += "0";

		previousDate += date + "-";

		previousDate = getMonthinMMM(month) + "-" + previousDate + year;

		return previousDate;
	}

	public static String getCurrentDateInDDMMMYYYY() {
		DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static int getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static String getDateFromEpoch(long epoch, String currentFormat) {
		DateFormat dateFormat = new SimpleDateFormat(currentFormat);
		Date date = new Date(epoch);
		return dateFormat.format(date);
	}

	public static String getUTCDateFromEpoch(long epoch, String currentFormat) {
		DateFormat dateFormat = new SimpleDateFormat(currentFormat);
		dateFormat.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		Date date = new Date(epoch);
		return dateFormat.format(date);
	}


	public static String getDateFromEpoch(long epoch) {
		return getDateFromEpoch(epoch, ConfigureConstantFields.getConstantFieldsProperty("DefaultDateFormat"));
	}

	public static String convertDateToDDMMYYYY(String origDate) throws ParseException {
		return convertDateToDDMMYYYY(origDate, ConfigureConstantFields.getConstantFieldsProperty("DefaultDateFormat"));
	}

	public static String convertDateToDDMMYYYY(String origDate, String currentFormat) throws ParseException {
		if (!currentFormat.equalsIgnoreCase("dd-MM-yyyy") && !currentFormat.equalsIgnoreCase("dd/MM/yyyy")
				&& !currentFormat.equalsIgnoreCase("dd:MM:yyyy")) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(currentFormat);
			Date date = simpleDateFormat.parse(origDate);
			SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd-MM-yyyy");
			return outputDateFormat.format(date);
		}
		return origDate;
	}

	public static String convertDateToMMDDYYYY(String origDate) throws ParseException {
		return convertDateToMMDDYYYY(origDate, ConfigureConstantFields.getConstantFieldsProperty("DefaultDateFormat"));
	}

	public static String convertDateToMMDDYYYY(String origDate, String currentFormat) throws ParseException {
		if (!currentFormat.equalsIgnoreCase("MM-dd-yyyy") && !currentFormat.equalsIgnoreCase("MM/dd/yyyy")
				&& !currentFormat.equalsIgnoreCase("MM:dd:yyyy")) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(currentFormat);
			Date date = simpleDateFormat.parse(origDate);
			SimpleDateFormat outputDateFormat = new SimpleDateFormat("MM-dd-yyyy");
			return outputDateFormat.format(date);
		}
		return origDate;
	}

	public static String convertDateToAnyFormat(String origDate, String requiredFormat) throws ParseException {
		return converDateToAnyFormat(origDate, null, requiredFormat);
	}

	public static String converDateToAnyFormat(String origDate, String currentFormat, String requiredFormat) throws ParseException {
		if (currentFormat == null)
			currentFormat = DateUtils.getDateFormat(origDate);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(currentFormat);
		Date date = simpleDateFormat.parse(origDate);
		SimpleDateFormat outputDateFormat = new SimpleDateFormat(requiredFormat);
		return outputDateFormat.format(date);
	}

	public static String getDateFormat(String date) {
		String format = null;

		for (String parse : formats) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(parse);
			try {
				dateFormat.parse(date);
				format = parse;
			} catch (ParseException e) {
			}
			if (format != null)
				break;
		}
		return format;
	}

	public static String getPreviousDateInDDMMYYYY(String currentDate) {
		String Tokens[] = currentDate.split("-");
		int date = Integer.parseInt(Tokens[0]);
		int month = Integer.parseInt(Tokens[1]);
		int year = Integer.parseInt(Tokens[2]);

		date--;

		if (date < 1) {
			month -= 1;

			if (month < 1) {
				month += 12;
				year--;
			}

			String tokens[] = DateUtils.getMonthEndDateInMMDDFormat(month - 1, year).split("/");
			date = Integer.parseInt(tokens[1]);
		}

		String previousDate = "";

		if (date < 10)
			previousDate += "0";

		previousDate += date + "-";

		if (month < 10)
			previousDate += "0";

		previousDate += month + "-" + year;

		return previousDate;
	}

	public static String getPreviousDateInMM_DD_YYYY(String currentDate) {
		String Tokens[] = currentDate.split("-");
		int date = Integer.parseInt(Tokens[1]);
		int month = Integer.parseInt(Tokens[0]);
		int year = Integer.parseInt(Tokens[2]);
		String monthString = "";
		date--;

		if (date < 1) {
			month -= 1;

			if (month < 1) {
				month += 12;
				year--;
			}

			String tokens[] = DateUtils.getMonthEndDateInMMDDFormat(month - 1, year).split("/");
			date = Integer.parseInt(tokens[1]);
		}

		String previousDate = "";

		if (date < 10)
			previousDate += "0";

		previousDate += date;

		if (month < 10)
			monthString = "0" + month;
		else {
			monthString = String.valueOf(month);
		}

		previousDate = monthString + "-" + previousDate + "-" + year;

		return previousDate;
	}


	//Date Format to be input as  MM-DD-YYYY
	public static String getNextDateInMM_DD_YYYY(String currentDate) {
		String Tokens[] = currentDate.split("-");
		int date = Integer.parseInt(Tokens[1]);
		int month = Integer.parseInt(Tokens[0]);

		int year = Integer.parseInt(Tokens[2]);

		date++;
		String endDateTokens[];

		endDateTokens = DateUtils.getMonthEndDateInMMDDFormat(month, year).split("/");

		if (date > Integer.parseInt(endDateTokens[1])) {
			month = Integer.parseInt(Tokens[0]) + 1;

			if (month > 12) {
				month -= 12;
				year++;
			}

			String tokens[] = DateUtils.getMonthStartDateInMMDDFormat(month - 1).split("/");
			date = Integer.parseInt(tokens[1]);
		}

		String nextDate = "";
//		nextDate += getMonthinMMM(month) + "-";

		if (month < 10) {
			nextDate += "0" + month + "-";
		}else {
			nextDate += month + "-";
		}

		if (date < 10)
		{
			nextDate += "0" + date + "-";
		}else{
			nextDate += date + "-";
		}

		nextDate += year;

		return nextDate;
	}

	//Date Format to be input as  MMM-DD-YYYY
	public static String getNextDateInMMM_DD_YYYY(String currentDate) {
		String Tokens[] = currentDate.split("-");
		int date = Integer.parseInt(Tokens[1]);
		int month = Integer.parseInt(getMonthindigit(Tokens[0]));

		int year = Integer.parseInt(Tokens[2]);

		date++;
		String endDateTokens[];

		endDateTokens = DateUtils.getMonthEndDateInMMDDFormat(month, year).split("/");

		if (date > Integer.parseInt(endDateTokens[1])) {
			month = month + 1;

			if (month > 12) {
				month -= 12;
				year++;
			}

			String tokens[] = DateUtils.getMonthStartDateInMMDDFormat(month - 1).split("/");
			date = Integer.parseInt(tokens[1]);
		}

		String nextDate = "";
		nextDate += getMonthinMMM(month) + "-";

		if (date < 10)
		{
			nextDate += "0" + date + "-";
		}else{
			nextDate += date + "-";
		}

		nextDate += year;

		return nextDate;
	}

	public static String getNextDateInDDMMYYYY(String currentDate) {
		String Tokens[] = currentDate.split("-");
		int date = Integer.parseInt(Tokens[0]);
		int month = Integer.parseInt(Tokens[1]);
		int year = Integer.parseInt(Tokens[2]);

		date++;
		String endDateTokens[];

		endDateTokens = DateUtils.getMonthEndDateInMMDDFormat(month, year).split("/");

		if (date > Integer.parseInt(endDateTokens[1])) {
			month = Integer.parseInt(Tokens[1]) + 1;

			if (month > 12) {
				month -= 12;
				year++;
			}

			String tokens[] = DateUtils.getMonthStartDateInMMDDFormat(month - 1).split("/");
			date = Integer.parseInt(tokens[1]);
		}

		String nextDate = "";

		if (date < 10)
			nextDate += "0";

		nextDate += date + "-";

		if (month < 10)
			nextDate += "0";

		nextDate += month + "-" + year;

		return nextDate;
	}

	public static String getDateOfXDaysFromYDate(String yDate, int xDays) throws ParseException {
		return getDateOfXDaysFromYDate(yDate, xDays, ConfigureConstantFields.getConstantFieldsProperty("DefaultDateFormat"));
	}

	public static String getDateOfXDaysFromYDate(String yDate, int xDays, String currentFormat) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(currentFormat);
		Date date = simpleDateFormat.parse(yDate);

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, xDays);
		String xDate = simpleDateFormat.format(cal.getTime());

		return xDate;
	}

	public static boolean isDateWithinRange(String currentDate, String lowerDate, String upperDate, String currentFormat) throws ParseException {
		boolean withinRange = false;

		//****************************************
		//Code added by gaurav bhadani
//		String dateformat = DateUtils.getDateFormat(lowerDate);
		//currentDate = DateUtils.convertDateToAnyFormat(currentDate,currentFormat);
		//****************************************
		//
		if (currentDate.equalsIgnoreCase(lowerDate) || currentDate.equalsIgnoreCase(upperDate))
			withinRange = true;

		else {
			if (currentFormat == null || currentFormat.equalsIgnoreCase(""))
				currentFormat = ConfigureConstantFields.getConstantFieldsProperty("DefaultDateFormat");

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(currentFormat);
			Date curDate = simpleDateFormat.parse(currentDate);
			Date fromDate = simpleDateFormat.parse(lowerDate);
			Date toDate = simpleDateFormat.parse(upperDate);

			if (!curDate.before(fromDate) && !curDate.after(toDate))
				withinRange = true;
		}
		return withinRange;
	}

	public static String getMonthStartDateInMMDDFormat(int month) {
		String monthStartDate[] = {
				"01/01",
				"02/01",
				"03/01",
				"04/01",
				"05/01",
				"06/01",
				"07/01",
				"08/01",
				"09/01",
				"10/01",
				"11/01",
				"12/01"
		};

		if(month == 0){
			return monthStartDate[month];
		}else {
			return monthStartDate[month - 1];
		}
	}

	public static String getMonthStartDateInMM_DDFormat(int month) {
		String monthStartDate[] = {
				"01-01",
				"02-01",
				"03-01",
				"04-01",
				"05-01",
				"06-01",
				"07-01",
				"08-01",
				"09-01",
				"10-01",
				"11-01",
				"12-01"
		};

		if(month == 0){
			return monthStartDate[month];
		}else {
			return monthStartDate[month - 1];
		}
	}

	public static String getMonthStartDateInMMM_DDFormat(int month) {
		String monthStartDate[] = {
				"Jan-01",
				"Feb-01",
				"Mar-01",
				"Apr-01",
				"May-01",
				"Jun-01",
				"Jul-01",
				"Aug-01",
				"Sep-01",
				"Oct-01",
				"Nov-01",
				"Dec-01"
		};

		if(month == 0){
			return monthStartDate[month];
		}else {
			return monthStartDate[month - 1];
		}
	}

	public static String getQuarterStartDateInMM_DDFormat(int month) {

		String quarterStartDate = null;

		String monthStartDate[] = {
				"01-01",
				"04-01",
				"07-01",
				"10-01",
		};

		if(month == 1 || month == 2 || month == 3 ){
			quarterStartDate =  monthStartDate[0];
		}else if(month == 4 || month == 5 || month == 6 ){
			quarterStartDate =  monthStartDate[1];
		}if(month == 7 || month == 8 || month == 9 ){
			quarterStartDate =  monthStartDate[2];
		}if(month == 10 || month == 11 || month == 12 ){
			quarterStartDate =  monthStartDate[3];
		}

		return quarterStartDate;
	}

	public static String getQuarterEndDateInMM_DDFormat(int month) {

		String quarterStartDate = null;

		String monthStartDate[] = {
				"03-31",
				"06-30",
				"09-30",
				"12-31",
		};

		if(month == 1 || month == 2 || month == 3 ){
			quarterStartDate =  monthStartDate[0];
		}else if(month == 4 || month == 5 || month == 6 ){
			quarterStartDate =  monthStartDate[1];
		}if(month == 7 || month == 8 || month == 9 ){
			quarterStartDate =  monthStartDate[2];
		}if(month == 10 || month == 11 || month == 12 ){
			quarterStartDate =  monthStartDate[3];
		}

		return quarterStartDate;
	}

	public static String getMonthEndDateInMMDDFormat(int month, int year) {

		String monthEndDate[] = {
				"01/31",
				"",
				"03/31",
				"04/30",
				"05/31",
				"06/30",
				"07/31",
				"08/31",
				"09/30",
				"10/31",
				"11/30",
				"12/31"
		};

		if(month == 2){
			if (year % 4 == 0)
				return "02/29";
			else {

				return "02/28";
			}
		}


		return monthEndDate[month - 1];


	}

	public static String getMonthEndDateInMM_DDFormat(int month, int year) {

		String monthEndDate[] = {
				"01-31",
				"",
				"03-31",
				"04-30",
				"05-31",
				"06-30",
				"07-31",
				"08-31",
				"09-30",
				"10-31",
				"11-30",
				"12-31"
		};

		if(month == 2){
			if (year % 4 == 0)
				return "02-29";
			else {

				return "02-28";
			}
		}


		return monthEndDate[month - 1];

	}

	public static String getMonthEndDateInMMM_DDFormat(int month, int year) {

		String monthEndDate[] = {
				"Jan-31",
				"",
				"Mar-31",
				"Apr-30",
				"May-31",
				"Jun-30",
				"Jul-31",
				"Aug-31",
				"Sep-30",
				"Oct-31",
				"Nov-30",
				"Dec-31"
		};

		if(month == 2){
			if (year % 4 == 0)
				return "Feb-29";
			else {

				return "Feb-28";
			}
		}


		return monthEndDate[month - 1];

	}

	public static String getCurrentDateInAnyFormat(String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String getCurrentDateInAnyFormat(String format, String timeZone) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String getMonthinMMM(int month){
		String monthString;
		switch (month) {
			case 1:  monthString = "Jan";
				break;
			case 2:  monthString = "Feb";
				break;
			case 3:  monthString = "Mar";
				break;
			case 4:  monthString = "Apr";
				break;
			case 5:  monthString = "May";
				break;
			case 6:  monthString = "Jun";
				break;
			case 7:  monthString = "Jul";
				break;
			case 8:  monthString = "Aug";
				break;
			case 9:  monthString = "Sep";
				break;
			case 10: monthString = "Oct";
				break;
			case 11: monthString = "Nov";
				break;
			case 12: monthString = "Dec";
				break;
			default: monthString = "Invalid month"; break;
		}
		return monthString;
	}

	public static String getMonthindigit(String month){
		String monthdigit;
		switch (month) {
			case "Jan":  monthdigit = "1";
				break;
			case "Feb":  monthdigit = "2";
				break;
			case "Mar":  monthdigit = "3";
				break;
			case "Apr":  monthdigit = "4";
				break;
			case "May":  monthdigit = "5";
				break;
			case "Jun":  monthdigit = "6";
				break;
			case "Jul":  monthdigit = "7";
				break;
			case "Aug":  monthdigit = "8";
				break;
			case "Sep":  monthdigit = "9";
				break;
			case "Oct": monthdigit = "10";
				break;
			case "Nov": monthdigit = "11";
				break;
			case "Dec": monthdigit = "12";
				break;
			default: monthdigit = "0"; break;
		}
		return monthdigit;
	}

	public static String getCurrentMonthStartDateinMMDDYYYY(){

		String requiredDate;
		String currentDate =  getCurrentDateInMM_DD_YYYY();
		String month = currentDate.split("-")[0];
		String date = "01";
		String year = currentDate.split("-")[2];

		requiredDate = month + "-" + date + "-" + year;
		return requiredDate;
	}

	public static String getCurrentMonthEndDateinMMDDYYYY(){

		String requiredDate;
		String date = "31";
		String currentDate =  getCurrentDateInMM_DD_YYYY();
		String month = currentDate.split("-")[0];
		Integer year = Integer.parseInt(currentDate.split("-")[2]);

		if(month.equals("02") && (year % 4 == 0)){
			date = "29";
		}else if(month.equals("02") && (year % 4 != 0)){
			date = "28";
		}else if(month.equals("01") || month.equals("03") || month.equals("05") || month.equals("07") || month.equals("07")
				|| month.equals("08") || month.equals("10") || month.equals("12")) {
			date = "31";
		}else if(month.equals("04") || month.equals("06") || month.equals("09") || month.equals("11")) {
			date = "30";
		}
		requiredDate = month + "-" + date + "-" +year;
		return requiredDate;
	}
	//This functions takes the input in MMM-YY format and returns true if the date in ascending order
	public static boolean checkIfDateInAscending(String prevDate,String nextDate){

		Boolean dateInAscendingOrder = true;
		try {
			String prevRecordMonth = prevDate.split("-")[0];
			Integer prevRecordMonthNum = Integer.parseInt(DateUtils.getMonthindigit(prevRecordMonth));
			Integer prevRecordYear = Integer.parseInt(prevDate.split("-")[1]);

			String nextRecordMonth = nextDate.split("-")[0];
			Integer nextRecordMonthNum = Integer.parseInt(DateUtils.getMonthindigit(nextRecordMonth));
			Integer nextRecordYear = Integer.parseInt(nextDate.split("-")[1]);

			if (prevRecordYear.equals(nextRecordYear)) {
				if (nextRecordMonthNum >= prevRecordMonthNum) {
					dateInAscendingOrder = true;
				} else {
					dateInAscendingOrder = false;
				}
			} else if (prevRecordYear < nextRecordYear) {
				dateInAscendingOrder = true;
			} else {
				dateInAscendingOrder = false;
			}
		}catch (Exception e){
			return false;
		}
		return dateInAscendingOrder;
	}

	//Returns the date in MMMM-DD-YYYY from MM-DD-YYYY Format or MM/DD/YYYY
	public static String convertDateTo_MMM_DD_YYYY_From_MM_DD_YYYY(String origDate,String delimiter){

		String monthString;
		String date;
		String year;
		String convertedDate;
		try {
			String[] dateArray = origDate.split(delimiter);
			int month = Integer.parseInt(dateArray[0]);
			monthString = getMonthinMMM(month);
			date = dateArray[1];
			year = dateArray[2];
			convertedDate = monthString + "-" + date + "-" + year;
		}catch (Exception e){
			convertedDate =  "";
		}

		return convertedDate;
	}
	//converting date from MM-DD-YYYY to YYYYMMMDD
	public static String convertDateTo_YYYYMMMDD_From_MM_DD_YYYY(String date){

		String[] dateArray = date.split("-");
		String year = dateArray[2];
		String month = getMonthinMMM(Integer.parseInt(dateArray[0]));
		String day = dateArray[1];

		return year + month + day;

	}

	public static String convDateInExcelFormatFromDD_MMM_YYYY(String date){

		String convertedDate;
		try{
			String[] dateArray = date.split("-");
			String day = dateArray[0];
			String month = getMonthindigit(dateArray[1]);
			String year = dateArray[2];
			convertedDate = month + "/" + day + "/" + year;
		}catch (Exception e){
			convertedDate = "";
		}
		return convertedDate;
	}

	public static String getCurrentTimeStamp(){

		String currentTime;
		try {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			currentTime = String.valueOf(timestamp);

			currentTime = currentTime.replace("-","_");
			currentTime = currentTime.replace(":","_");
			currentTime = currentTime.replace(".","_");


		}catch (Exception e){
			currentTime = "";
		}

		return currentTime;
	}

	public List<List<String>> getCurrentTimeStampDB() {

		String sqlString = "select current_timestamp";
		List<List<String>> currentTimeStamp = null;
		PostgreSQLJDBC postgreSQLJDBC;

		postgreSQLJDBC = new PostgreSQLJDBC();
		try {
			currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
		} catch (Exception e) {

		}finally {
			postgreSQLJDBC.closeConnection();
		}
		return currentTimeStamp;
	}

	public synchronized static int compareTwoDates(String datePattern,String date1,String date2,String effectiveDate){

		int comparison = -99;
		SimpleDateFormat sdformat = new SimpleDateFormat(datePattern);


		try {
			Date d1 = sdformat.parse(date1);
			Date d2 = sdformat.parse(date2);
			Date effDate = sdformat.parse(effectiveDate);

//			Effective Date occurs after Date 1 and occurs before Date 2");
			if (effDate.compareTo(d1) >= 0 && effDate.compareTo(d2) <= 0) {
				comparison = 0;
			}
//				"Effective Date occurs before Date 1");
			else if (effDate.compareTo(d1) < 0) {
				comparison = -1;
			}
//			"Effective Date occurs after Date 2");
			else if (effDate.compareTo(d2) > 0) {
				comparison = 1;
			}
		}catch (Exception e){

		}
		return comparison;
	}

	public static String getDBTimeStamp(){
		String timeStamp = "";
		PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

		try{
			List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select now();");
			timeStamp = sqlOutput.get(0).get(0);
		}catch (Exception e){
			logger.error("Exception while fetching date time stamp from Data Base");
		}
		return timeStamp;
	}

}