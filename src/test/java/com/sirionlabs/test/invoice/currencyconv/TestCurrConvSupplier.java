package com.sirionlabs.test.invoice.currencyconv;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Supplier;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestCurrConvSupplier {

    private final static Logger logger = LoggerFactory.getLogger(TestCurrConvSupplier.class);

    int clientId;
    int supplierEntityTypeId;
    String supplierEntity = "suppliers";

    @BeforeClass
    public void beforeClass(){

        clientId = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("client_id"));
        supplierEntityTypeId = 1;
    }

    @Test
    public void Test_SupplierCurrencyConv(){

        CustomAssert customAssert = new CustomAssert();
        int supplierId = -1;
        try {
            String createResponse = Supplier.createSupplier("curr conv scenario", true);
            supplierId = CreateEntity.getNewEntityId(createResponse, supplierEntity);

            if (supplierId == -1) {
                customAssert.assertEquals("Unable to create supplier ", "Supplier should be created");
            } else {

                Show show = new Show();
                show.hitShowVersion2(supplierEntityTypeId,supplierId);
                String showResponse = show.getShowJsonStr();

                String effectiveDate = ShowHelper.getValueOfField("effectivedatevalue",showResponse);
                int currencyFrom = -1;
                String currFrom = ShowHelper.getValueOfField("currency id",showResponse);

                if(currFrom !=null){
                    currencyFrom = Integer.parseInt(currFrom);
                }else {
                    customAssert.assertEquals("Currency From is null","Reporting Currency should be present for conversion");
                    customAssert.assertAll();
                    return;
                }
                int currencyTo = InvoiceHelper.getClientCurrencyId(clientId,customAssert);
                String currToShortCode = InvoiceHelper.getCurrency(currencyTo);
                String currFromShortCode = InvoiceHelper.getCurrency(currencyFrom);

                int clientRateCardId =  InvoiceHelper.getClientRateCard(clientId,effectiveDate,"MM-dd-yyyy",customAssert);

                int convFactor = InvoiceHelper.getConversionFactor(clientRateCardId,currencyFrom,currencyTo,customAssert).intValue();

                String convTcv = ShowHelper.getValueOfField("converted tcv",showResponse);
                String convAcv = ShowHelper.getValueOfField("converted acv",showResponse);
                String convFycv =ShowHelper.getValueOfField("converted fycv",showResponse);

                String originalTcv = ShowHelper.getValueOfField("tcv",showResponse);
                originalTcv = originalTcv.replace(".0","");
                String originalAcv = ShowHelper.getValueOfField("acv",showResponse);
                originalAcv = originalAcv.replace(".0","");
                String originalFycv =ShowHelper.getValueOfField("fycv",showResponse);
                originalFycv = originalFycv.replace(".0","");

                if(convTcv ==null){
                    customAssert.assertEquals("Converted Tcv value is null","Converted Tcv value should not be null");
                }else {
                    String expConvTcv = originalTcv + " " + currFromShortCode + " (" + Integer.parseInt(originalTcv) * convFactor + " " + currToShortCode + ")";
                    convTcv = convTcv.replaceAll(",","");

                    if(currToShortCode.equalsIgnoreCase(currFromShortCode)){
                        expConvTcv = originalTcv + " " + InvoiceHelper.getCurrencyName(currencyFrom);
                    }
                    if(expConvTcv.equals(convTcv)){
                        logger.info("Tcv conversion Value validated successfully");
                    }else {
                        logger.error("Tcv conversion Value validated unsuccessfully");
                        customAssert.assertEquals(convTcv,expConvTcv,"Tcv conversion Value validated unsuccessfully Expected Value " + expConvTcv + " Actual Value " + convTcv);
                    }

                }
                if(convAcv ==null){
                    customAssert.assertEquals("Converted Acv value is null","Converted Acv value should not be null");
                }else {
                    String expConvAcv = originalAcv + " " + currFromShortCode + " (" + Integer.parseInt(originalAcv) * convFactor + " " + currToShortCode + ")";
                    convAcv = convAcv.replaceAll(",","");

                    if(currToShortCode.equalsIgnoreCase(currFromShortCode)){
                        expConvAcv = originalAcv + " " +InvoiceHelper.getCurrencyName(currencyFrom);
                    }

                    if(expConvAcv.equals(convAcv)){
                        logger.info("Acv conversion Value validated successfully");
                    }else {
                        logger.error("Acv conversion Value validated unsuccessfully");
                        customAssert.assertEquals(convAcv,expConvAcv,"Acv conversion Value validated unsuccessfully Expected Value " + expConvAcv + " Actual Value " + convAcv);
                    }
                }
                if(convFycv ==null){
                    customAssert.assertEquals("Converted Fycv value is null","Converted Fycv value should not be null");
                }else {
                    String expConvFycv = originalFycv + " " + currFromShortCode + " (" + Integer.parseInt(originalFycv) * convFactor + " " + currToShortCode + ")";
                    convFycv = convFycv.replaceAll(",","");

                    if(currToShortCode.equalsIgnoreCase(currFromShortCode)){
                        expConvFycv = originalFycv + " " +InvoiceHelper.getCurrencyName(currencyFrom);
                    }
                    if(expConvFycv.equals(convFycv)){
                        logger.info("Fycv conversion Value validated successfully");
                    }else {
                        logger.error("Fycv conversion Value validated unsuccessfully");
                        customAssert.assertEquals(convFycv,expConvFycv,"Fycv conversion Value validated unsuccessfully Expected Value " + expConvFycv + " Actual Value " + convFycv);
                    }
                }

            }
        }catch (Exception e){
            customAssert.assertEquals("Exception in main test method while validating","Exception should not occur");
        }finally {
            if(supplierId != -1) {
                EntityOperationsHelper.deleteEntityRecord(supplierEntity,supplierId);
            }
        }
        customAssert.assertAll();
    }

}
