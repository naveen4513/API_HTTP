package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class CurrencyConversionHelper {

	private final static Logger logger = LoggerFactory.getLogger(CurrencyConversionHelper.class);
	private String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CurrencyConversionMatrixConfigFilePath");
	private String configFileName = ConfigureConstantFields.getConstantFieldsProperty("CurrencyConversionMatrixConfigFileName");
	private String clientBaseCurrency = null;
	private int clientId = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id"));
	private final String datePattern = "MM-dd-yyyy";

	public Double getCurrencyConversionFactor(String inputCurrency, String outputCurrency) {
		return getCurrencyConversionFactor("default", inputCurrency, outputCurrency);
	}

	public Double getCurrencyConversionFactor(String conversionMatrixName, String inputCurrency, String outputCurrency) {
		if (inputCurrency == null || outputCurrency == null) {
			logger.error("Invalid Input/Output Currency Parameters");
			return -1D;
		}

		if (inputCurrency.trim().equalsIgnoreCase(outputCurrency.trim())) {
			return 1D;
		}

		String key = inputCurrency.trim() + " to " + outputCurrency.trim();
		Double conversionFactor = -1D;

		try {
			logger.info("Getting Currency Conversion Factor for Conversion Matrix [{}] and Key [{}]", conversionMatrixName, key);
			String factorValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, conversionMatrixName.trim(), key.trim().toLowerCase());

			if (factorValue != null && !factorValue.trim().equalsIgnoreCase("")) {
				conversionFactor = Double.parseDouble(factorValue);
			} else {
				logger.error("Couldn't find Currency Conversion Factor for Conversion Matrix [{}] and Key [{}]", conversionMatrixName, key);
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Currency Conversion Factor for Conversion Matrix [{}] and Key [{}]. {}", conversionMatrixName, key, e.getStackTrace());
		}
		return conversionFactor;
	}

	public String getClientBaseCurrency() {
		if (clientBaseCurrency != null) {
			return clientBaseCurrency;
		}

		try {
			logger.info("Hitting DefaultUserListMetaData API for Invoice to get Client Base Currency.");
			ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
			int invoiceListId = ConfigureConstantFields.getListIdForEntity("invoices");
			defaultUserListObj.hitListRendererDefaultUserListMetadata(invoiceListId);
			String defaultUserListResponse = defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
				JSONObject jsonObj = new JSONObject(defaultUserListResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("columns");

				for (int i = 0; i < jsonArr.length(); i++) {
					String queryName = jsonArr.getJSONObject(i).getString("queryName");

					if (queryName != null && queryName.trim().equalsIgnoreCase("clientinvoiceamount")) {
						String defaultName = jsonArr.getJSONObject(i).getString("defaultName");
						clientBaseCurrency = defaultName.substring(defaultName.indexOf("(") + 1, defaultName.lastIndexOf(")"));
						break;
					}
				}
			} else {
				logger.error("DefaultUserListMetaData API Response for Invoice is an Invalid JSON. Couldn't get Client Base Currency.");
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Client Base Currency. {}", e.getMessage());
		}
		return clientBaseCurrency;
	}

	public Double convertCurrencyValue(String inputCurrency, String outputCurrency, Double inputCurrencyValue) {
		return convertCurrencyValue("default", inputCurrency, outputCurrency, inputCurrencyValue);
	}

	public Double convertCurrencyValue(String currencyConversionMatrix, String inputCurrency, String outputCurrency, Double inputCurrencyValue) {
		Double outputCurrencyValue = -1D;

		if (inputCurrency.trim().equalsIgnoreCase(outputCurrency.trim())) {
			return inputCurrencyValue;
		}

		try {
			Double conversionFactor = getCurrencyConversionFactor(currencyConversionMatrix, inputCurrency, outputCurrency);

			if (conversionFactor != -1D) {
				outputCurrencyValue = inputCurrencyValue * conversionFactor;
			} else {
				logger.error("Couldn't get Currency Conversion Factor. Hence couldn't convert Currency Value.");
			}
		} catch (Exception e) {
			logger.error("Exception while Converting Input Currency Value [{}] from Currency [{}] to Currency [{}]. {}", inputCurrencyValue, inputCurrency,
					outputCurrency, e.getStackTrace());
		}
		return outputCurrencyValue;
	}

	public Double convertCurrencyValueToClientBaseCurrency(String currencyConversionMatrix, String inputCurrency, Double inputCurrencyValue) {
		if (getClientBaseCurrency() == null) {
			logger.error("Couldn't get Client Base Currency. Hence couldn't convert Currency Value.");
			return -1D;
		}

		return convertCurrencyValue(currencyConversionMatrix, inputCurrency, getClientBaseCurrency(), inputCurrencyValue);
	}

	public int getEffectiveRateCard(int entityId, int entityTypeId, String effectiveDate, CustomAssert customAssert){

		int effectiveRateCard =-1;

		try{
			Show show = new Show();
			if(entityTypeId!=160)
				show.hitShowVersion2(61,entityId);
			else
				show.hitShowVersion2(entityTypeId,entityId);

			String showResponse = show.getShowJsonStr();

//            String effectiveDate = ShowHelper.getValueOfField("effectivedate",showResponse);

			JSONObject showResponseJson = new JSONObject(showResponse);

			JSONArray valuesArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("existingRateCards").getJSONArray("values");

			if(effectiveDate == null){
				if(valuesArray.length() > 1){
//                  In case when effective date is null and more than one rate card is defined
//                  then there will be no rate card to be applied
					return 0;
				}
			}

			for(int i =0;i<valuesArray.length();i++){

				String rateCardId = valuesArray.getJSONObject(i).getJSONObject("rateCard").get("id").toString();
				String rateCardFromDate = valuesArray.getJSONObject(i).get("rateCardFromDate").toString();
				String rateCardToDate = valuesArray.getJSONObject(i).get("rateCardToDate").toString();

				rateCardFromDate = DateUtils.convertDateToAnyFormat(rateCardFromDate,datePattern);
				rateCardToDate = DateUtils.convertDateToAnyFormat(rateCardToDate,datePattern);

				int comparisonFactor = DateUtils.compareTwoDates(datePattern,rateCardFromDate,rateCardToDate,effectiveDate);
				if((comparisonFactor == 0 )|| (comparisonFactor == -1)){
					effectiveRateCard = Integer.parseInt(rateCardId);
					break;
				}else if(comparisonFactor == 1 && i == valuesArray.length() -1){
					effectiveRateCard = Integer.parseInt(rateCardId);
				}
			}

		}catch (Exception e){
			logger.error("Exception while getting effective Rate Id form contract " + e.getStackTrace());
			customAssert.assertTrue(false,"Exception while getting effective Rate Id form contract " + e.getStackTrace());
		}
		return effectiveRateCard;
	}

	public Double getConversionFactor(int rateCardId, int currFrom, int currTo, CustomAssert customAssert) {

		Double conversionFactor = 0.0;
		PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

		try {
			conversionFactor = Double.parseDouble(postgreSQLJDBC.doSelect("select rate_value from rate_card_conversion where rate_card_id = " + rateCardId +
					" and currency_from = " + currFrom + " and currency_to = " + currTo).get(0).get(0));
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while fetching conversion id from DB " + e.getStackTrace());
		} finally {
			postgreSQLJDBC.closeConnection();
		}
		return conversionFactor;

	}

	public Double getConvFacWRTClient(int entityTypeId,int entityId,int contractId,CustomAssert customAssert){

		Double conversionFactor = 0.0;
		try {
			String effectiveDate = "";

			//If entity is service data then consider service end date
			if(entityTypeId == 64){

				effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "enddatevalue");
			}else if(entityTypeId == 165){//To update according to client admin
				effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "invoicedatevalue");     //If from client admin invoice end date is chosen
			}else if(entityTypeId == 181){
				effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "enddatevalue");     //
			}
			else {
				effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "effectivedatevalue");
			}
			int clientCurrencyId = InvoiceHelper.getClientCurrencyId(clientId,customAssert);

			int invoiceCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(entityTypeId, entityId, "currency id"));

			int rateCardId = getEffectiveRateCard(contractId,entityTypeId, effectiveDate, customAssert);

			if (rateCardId == -1) {
				rateCardId = InvoiceHelper.getClientRateCard(clientId, customAssert);
			}

			if (rateCardId != 0) {
				conversionFactor = getConversionFactor(rateCardId, invoiceCurrencyId, clientCurrencyId, customAssert);
			} else {
				customAssert.assertTrue(false, "Rate card Id value is 0");
			}
		}catch (Exception e){
			customAssert.assertTrue(false,"Exception while getting conversion Factor ");
		}
		return conversionFactor;
	}
}