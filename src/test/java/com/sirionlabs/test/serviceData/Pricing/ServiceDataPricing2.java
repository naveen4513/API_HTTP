package com.sirionlabs.test.serviceData.Pricing;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.invoice.InvoicePricingTemplateDownload;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.ChangeRequest;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.velocity.runtime.directive.Parse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.util.Strings;

import javax.print.attribute.PrintJobAttributeSet;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServiceDataPricing2 {
    private final Logger logger = LoggerFactory.getLogger(ServiceDataPricing2.class);
    private String configFilePath = "src/test/resources/TestConfig/ServiceData/Pricing";
    private String configFileName = "ServiceDataPricing2.cfg";
    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;
    private String changeRequestConfigFilePath;
    private String changeRequestConfigFileName;
    private String changeRequestExtraFieldsConfigFileName;
    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFileName;
    private int serviceDataTypeId = 64;
    private int crEntityTypeId = 63;
    private int cdrEntityTypeId = 160;
    private List<EntityPojo> toBeDeleted = new ArrayList<>();
    private int cdrListId = 279, columnIdForIDCDR = 12259;

    @BeforeClass
    public void beforeClass() throws Exception {

        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        changeRequestConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFilePath");
        changeRequestConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFileName");
        changeRequestExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestExtraFieldsFileName");

        //CDR Config files
        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRFileName");
        cdrExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRExtraFieldsFileName");

    }

    @AfterTest
    public void deleteEntities() {
        for (EntityPojo entityPojo : toBeDeleted)
            EntityOperationsHelper.deleteEntityRecord(entityPojo.name, entityPojo.entityId);
    }

    @DataProvider
    public Object[][] dataProviderForC10377() {
        String[] flows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c10377", "flows").split(",");

        Object[][] objects = new Object[flows.length][];
        int i = 0;
        for (String s : flows) {
            objects[i] = new Object[]{s};
            i++;
        }
        return objects;
    }

    @Test(enabled = true)
    public void c90840() {
        CustomAssert customAssert = new CustomAssert();
        try {

            int supplierId1 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90840", "s1"));
            int supplierId2 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90840", "s2"));
            int supplierId3 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90840", "s3"));

            String cdrFlowMulti = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "cdrmultisup");
            assert cdrFlowMulti != null : "Multi CDR flow name is null";

            CustomData cdr1 = getCDR(cdrFlowMulti, customAssert, false);
            String contractFlowMulti = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "contractmulti");
            assert contractFlowMulti != null : "multi sup Contract flow empty from config file";

            UpdateFile.addPropertyToConfigFile(contractConfigFilePath, contractExtraFieldsConfigFileName, contractFlowMulti, "sourceEntityId", "{\"values\":" + cdr1.id + "}");
            UpdateFile.addPropertyToConfigFile(contractConfigFilePath, contractExtraFieldsConfigFileName, contractFlowMulti, "sourceEntityTypeId", "{\"values\":160}");

            int c12 = InvoiceHelper.getMultiSupplierContract(contractConfigFilePath, contractConfigFileName, contractConfigFilePath, contractExtraFieldsConfigFileName, contractFlowMulti);
            assert c12 != -1 : "Cannot create contract";

            String contractFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");
            assert contractFlow != null : "single sup Contract flow empty from config file";
            int c1 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractFlow);
            assert c1 != -1 : "Cannot create contract";

            String serviceDateFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sd1");
            assert serviceDateFlow != null : "Service data multi sup flow is null";

            String serviceDateFlowMulti = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "servicedatamultisup");
            assert serviceDateFlowMulti != null : "Service data multi sup flow is null";

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDateFlowMulti, "sourceid", String.valueOf(c12));
            int sd1 = InvoiceHelper.getServiceDataIdForMultiSupplier(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlowMulti, c12, supplierId2);
            int sd2 = InvoiceHelper.getServiceDataIdForMultiSupplier(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlowMulti, c12, supplierId1);
            int sd4 = InvoiceHelper.getServiceDataIdForMultiSupplier(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlowMulti, c12, supplierId3);

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDateFlow, "sourceid", String.valueOf(c1));
            int sd3 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlow, c1);


            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, cdrFlowMulti, "sourceid", String.valueOf(c12));
            CustomData cdr2 = getCDR(cdrFlowMulti, customAssert);
            assert cdr2.id != -1 : "CDR not created";


            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");
            assert downloadFileName != null && downloadFilePath != null : "download file details not found";

            downloadPricingTemplate(sd1, customAssert);
            long rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            List<String> masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(masterDataSheet.contains(cdr2.name), "Master data doesn't contains CDR2 which was supposed to contain for sd1");
            customAssert.assertTrue(masterDataSheet.contains(cdr1.name), "Master data doesn't contains CDR1 which was supposed to contain for sd1");

            downloadPricingTemplate(sd2, customAssert);
            rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(masterDataSheet.contains(cdr1.name), "Master data doesn't contains CDR1 which was supposed to contain for sd2");
            customAssert.assertTrue(masterDataSheet.contains(cdr2.name), "Master data doesn't contains CDR2 which was supposed to contain for sd2");

            downloadPricingTemplate(sd4, customAssert);
            rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(masterDataSheet.contains(cdr1.name), "Master data doesn't contains CDR1 which was supposed to contain for sd4");
            customAssert.assertTrue(!masterDataSheet.contains(cdr2.name), "Master data contains CDR2 which was not supposed to contain for sd4");

            downloadPricingTemplate(sd3, customAssert);
            rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(masterDataSheet.contains(cdr2.name), "Master data doesn't contains CDR2 which was supposed to contain for sd3");
            customAssert.assertTrue(!masterDataSheet.contains(cdr1.name), "Master data contains CDR1 which was not supposed to contain for sd3");
        } catch (Exception e) {
            logger.error("Exception occurred : {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception occurred : " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void C90833() {
        CustomAssert customAssert = new CustomAssert();
        try {
            String contractFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "contractmulti");
            assert contractFlow != null : "multi sup Contract flow empty from config file";
            String contractFlow2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");
            assert contractFlow2 != null : "single sup Contract flow empty from config file";

            int c12 = InvoiceHelper.getMultiSupplierContract(contractConfigFilePath, contractConfigFileName, contractConfigFilePath, contractExtraFieldsConfigFileName, contractFlow);

            int c1 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractFlow2);

            String serviceDateFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sd1");
            assert serviceDateFlow != null : "Service data multi sup flow is null";

            String serviceDateFlowMulti = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "servicedatamultisup");
            assert serviceDateFlowMulti != null : "Service data multi sup flow is null";

            int supplierId1 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90833", "s1"));
            int supplierId2 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90833", "s2"));
            int supplierId3 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90833", "s3"));

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDateFlowMulti, "sourceid", String.valueOf(c12));
            int sd1 = InvoiceHelper.getServiceDataIdForMultiSupplier(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlowMulti, c12, supplierId2);
            int sd2 = InvoiceHelper.getServiceDataIdForMultiSupplier(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlowMulti, c12, supplierId1);
            int sd4 = InvoiceHelper.getServiceDataIdForMultiSupplier(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlowMulti, c12, supplierId3);

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDateFlow, "sourceid", String.valueOf(c1));
            int sd3 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDateFlow, c1);

            String ccrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "ccr1");
            assert ccrFlow != null : "CCR flow not found in config file";

            String ccrFlowMulti = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "ccr1");
            assert ccrFlowMulti != null : "Multi sup CCR flow not found in config file";

            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlow, "sourceid", String.valueOf(c1));
            CustomData ccr1 = getCCR(ccrFlow, customAssert, true);

            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlowMulti, "sourceid", String.valueOf(c12));
            CustomData ccr2 = getCCRMultiSup(ccrFlow, customAssert, true, supplierId2);
            CustomData ccr3 = getCCRMultiSup(ccrFlow, customAssert, true, supplierId1);

            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlowMulti, "sourceentity", "suppliers");
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlowMulti, "sourceid", String.valueOf(supplierId1));
            CustomData ccr5 = getCCR(ccrFlow, customAssert, true);

            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlowMulti, "sourceid", String.valueOf(supplierId2));
            CustomData ccr4 = getCCR(ccrFlow, customAssert, true);

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");
            assert downloadFileName != null && downloadFilePath != null : "download file details not found";

            downloadPricingTemplate(sd1, customAssert);
            long rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            List<String> masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(masterDataSheet.contains(ccr2.name), "Master data doesn't contains CCR2 which was supposed to contain");
            customAssert.assertTrue(masterDataSheet.contains(ccr4.name), "Master data doesn't contains CCR4 which was supposed to contain");

            downloadPricingTemplate(sd2, customAssert);
            rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(masterDataSheet.contains(ccr3.name), "Master data doesn't contains CCR3 which was supposed to contain");
            customAssert.assertTrue(masterDataSheet.contains(ccr5.name), "Master data doesn't contains CCR5 which was supposed to contain");

            downloadPricingTemplate(sd3, customAssert);
            rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(masterDataSheet.contains(ccr1.name), "Master data doesn't contains CCR1 which was supposed to contain");
            customAssert.assertTrue(masterDataSheet.contains(ccr5.name), "Master data doesn't contains CCR5 which was supposed to contain");

            downloadPricingTemplate(sd4, customAssert);
            rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
            masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
            logger.info("Master Data : {}", masterDataSheet);
            customAssert.assertTrue(!masterDataSheet.contains(ccr1.name), "Master data contains CCR1 which was not supposed to contain");
            customAssert.assertTrue(!masterDataSheet.contains(ccr2.name), "Master data contains CCR2 which was not supposed to contain");
            customAssert.assertTrue(!masterDataSheet.contains(ccr3.name), "Master data contains CCR3 which was not supposed to contain");
            customAssert.assertTrue(!masterDataSheet.contains(ccr4.name), "Master data contains CCR4 which was not supposed to contain");
            customAssert.assertTrue(!masterDataSheet.contains(ccr5.name), "Master data contains CCR5 which was not supposed to contain");

        } catch (Exception e) {
            logger.error("Exception caught : {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught : " + Arrays.toString(e.getStackTrace()));
        }
        customAssert.assertAll();
    }

    @Test(enabled = false, dataProvider = "dataProviderForC10377")
    public void c10377(String flow) { //also covers C3429
        CustomAssert customAssert = new CustomAssert();

        try {
            assert flow != null : "flow name is blank";

            int contract = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flow);
            assert contract != -1 : "Contract not created";

            toBeDeleted.add(new EntityPojo(contract, "contracts"));
            logger.info("contract id : [{}]", contract);
            //***************************************************** create service data sd1 **************

            int sd = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flow, contract);

            assert sd != -1 : "sd not created";

            toBeDeleted.add(new EntityPojo(sd, "service data"));
            logger.info("sd id : [{}]", sd);


            //*************************************************** Download pricing template ******************************

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

            assert downloadFileName != null && downloadFilePath != null : "download file details not found";

            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd)));
            assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";

            InvoiceHelper invoiceHelper = new InvoiceHelper();

            InvoiceHelper.editPricingFileForPricing(configFilePath, configFileName, downloadFilePath, downloadFileName, flow);
            if (!flow.contains("fixed"))
                InvoiceHelper.editPricingFileForARCRRC(configFilePath, configFileName, downloadFilePath, downloadFileName, flow, sd);

            customUploadPricing("pass", downloadFilePath, downloadFileName, flow, customAssert);


            String pricingVolume, pricingRate, pricingStart, pricingEnd, arcRate = null, arcLower = null, arcUpper = null, arcStart = null, arcEnd = null;
            String newPricingVolume, newPricingRate, newArcRate = null, newArcLower = null, newArcUpper = null;

            pricingVolume = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow, "volumecolumnvalues");
            newPricingVolume = pricingVolume + "1";
            UpdateFile.updateConfigFileProperty(configFilePath, configFileName, flow, "volumecolumnvalues", newPricingVolume);

            pricingRate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow, "ratecolumnvalues");
            newPricingRate = pricingRate + "1";
            UpdateFile.updateConfigFileProperty(configFilePath, configFileName, flow, "ratecolumnvalues", newPricingRate);

            pricingStart = XLSUtils.getOneCellValue(downloadFilePath, downloadFileName, "Pricing", 6, 5);
            pricingEnd = XLSUtils.getOneCellValue(downloadFilePath, downloadFileName, "Pricing", 6, 6);

            boolean arcRrcForecast = flow.contains("arc") || flow.contains("rrc") || flow.contains("vol");

            if (arcRrcForecast) {
                String[] temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow, "arcvalue").split(",");

                arcLower = temp[0].trim();
                arcUpper = temp[1].trim();
                arcRate = temp[2].trim();

                newArcLower = arcLower + "1";
                newArcUpper = arcUpper + "1";
                newArcRate = arcRate + "1";


                UpdateFile.updateConfigFileProperty(configFilePath, configFileName, flow, "arcvalue", newArcLower + "," + newArcUpper + "," + newArcRate + "," + temp[3]);

                arcStart = XLSUtils.getOneCellValue(downloadFilePath, downloadFileName, "ARCRRC", 6, 5);
                arcEnd = XLSUtils.getOneCellValue(downloadFilePath, downloadFileName, "ARCRRC", 6, 6);
            }

            customAssert.assertTrue(invoiceHelper.verifyChargesCreated(sd, Double.parseDouble(pricingVolume.trim()), Double.parseDouble(pricingRate.trim()),
                    new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(pricingStart))),
                    new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(pricingEnd)))), "Incorrect data in charges tab in first step for flow " + flow);
            if (arcRrcForecast)
                customAssert.assertTrue(invoiceHelper.verifyARCRRCCreated(sd, Double.parseDouble(arcLower.trim()), Double.parseDouble(arcUpper.trim()), Double.parseDouble(arcRate.trim()),
                        new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(arcStart))),
                        new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(arcEnd)))), "Incorrect data in charges tab in first step for flow " + flow);


            InvoiceHelper.editPricingFileForPricing(configFilePath, configFileName, downloadFilePath, downloadFileName, flow);
            if (arcRrcForecast)
                InvoiceHelper.editPricingFileForARCRRC(configFilePath, configFileName, downloadFilePath, downloadFileName, flow, sd);

            customUploadPricing("pass", downloadFilePath, downloadFileName, flow, customAssert);

            customAssert.assertTrue(invoiceHelper.verifyChargesCreated(sd, Double.parseDouble(newPricingVolume.trim()), Double.parseDouble(newPricingRate.trim()),
                    new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(pricingStart))),
                    new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(pricingEnd)))), "Incorrect data in charges tab in first step for flow " + flow);
            if (arcRrcForecast)
                customAssert.assertTrue(invoiceHelper.verifyARCRRCCreated(sd, Double.parseDouble(newArcLower.trim()), Double.parseDouble(newArcUpper.trim()), Double.parseDouble(newArcRate.trim()),
                        new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(arcStart))),
                        new SimpleDateFormat("MMM-dd-yyyy").format(DateUtil.getJavaDate(Float.parseFloat(arcEnd)))), "Incorrect data in charges tab in first step for flow " + flow);


        } catch (Exception e) {
            logger.error("Exception occurred {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception occurred " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void C3865() {

        CustomAssert customAssert = new CustomAssert();
        try {
            String contractFlowName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");

            assert contractFlowName != null : "Contract flows name are blank";

            int contract = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractFlowName);
            assert contract != -1 : "Contract not created";

            toBeDeleted.add(new EntityPojo(contract, "contracts"));
            logger.info("contract id : [{}]", contract);
            //***************************************************** create service data sd1 **************

            String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sd1");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract));
            assert sdFlow != null : "Service data flow is null";

            int sd1 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, sdFlow, contract);

            assert sd1 != -1 : "sd1 not created";

            toBeDeleted.add(new EntityPojo(sd1, "service data"));
            logger.info("sd1 id : [{}]", sd1);


            //*************************************************** Download pricing template ******************************

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

            assert downloadFileName != null && downloadFilePath != null : "download file details not found";

            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
            assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";

            //Setting up the sftp
            String environment = "env";//ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"environment");
            //assert environment!=null:"Environment name is null in config file";

            String host = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "host");
            String user = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "user");
            String key = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "key");
            String withKey = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "withkey");

            boolean credentialStatus = host != null && user != null && key != null && withKey != null;
            customAssert.assertTrue(credentialStatus, "Environment details are empty");

            SCPUtils scpUtils;

            if (withKey.equalsIgnoreCase("yes"))
                scpUtils = new SCPUtils(host, user, key, 22, withKey.equalsIgnoreCase("yes"));
            else
                scpUtils = new SCPUtils(host, user, key, 22);
            String statusFileName = downloadFileName.split("[.]")[0] + "-servercopy." + downloadFileName.split("[.]")[1];
            String statusFilePath = downloadFilePath.charAt(downloadFilePath.length() - 1) == '/' ? downloadFilePath.substring(0, downloadFilePath.length() - 1) : downloadFilePath;

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();

            final String newFileCopy = downloadFileName.split("[.]")[0] + "-copy." + downloadFileName.split("[.]")[1];


            FileUtils.deleteFile(downloadFilePath, newFileCopy);
            assert !FileUtils.fileExists(downloadFilePath, newFileCopy);
            assert FileUtils.copyFile(downloadFilePath, downloadFileName, downloadFilePath, newFileCopy) : "Cannot copy the existing pricing file";

            Map<String, Object> dataMap;

            //start date -> empty string
            dataMap = new HashMap<>();
            dataMap.put("11345", "");
            XLSUtils.editRowDataUsingColumnId(downloadFilePath, newFileCopy, "Pricing", 6, dataMap);
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
            customUploadPricing("fail", downloadFilePath, newFileCopy, sdFlow, customAssert);
            scpDownloadFile("Failure", "(Service Start Date,Cell Value Cannot Be Empty)", allTaskIds, customAssert, "Pricing", downloadFilePath, downloadFileName, statusFileName, statusFilePath, newFileCopy, scpUtils);

            //start date -> Random string value
            dataMap = new HashMap<>();
            dataMap.put("11345", "Random string");
            XLSUtils.editRowDataUsingColumnId(downloadFilePath, newFileCopy, "Pricing", 6, dataMap);
            fetchObj.hitFetch();
            allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
            customUploadPricing("fail", downloadFilePath, newFileCopy, sdFlow, customAssert);
            scpDownloadFile("Failure", "(Service Start Date,Only date values are allowed)", allTaskIds, customAssert, "Pricing", downloadFilePath, downloadFileName, statusFileName, statusFilePath, newFileCopy, scpUtils);

            //start date -> empty string, end date -> empty
            dataMap = new HashMap<>();
            dataMap.put("11345", "");
            dataMap.put("11346", "");
            XLSUtils.editRowDataUsingColumnId(downloadFilePath, newFileCopy, "Pricing", 6, dataMap);
            fetchObj.hitFetch();
            allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
            customUploadPricing("fail", downloadFilePath, newFileCopy, sdFlow, customAssert);
            scpDownloadFile("Failure", "(Service Start Date,Cell Value Cannot Be Empty) , (Service End Date,Cell Value Cannot Be Empty)",
                    allTaskIds, customAssert, "Pricing", downloadFilePath, downloadFileName, statusFileName, statusFilePath, newFileCopy, scpUtils);

            //start date > end date
            dataMap = new HashMap<>();
            XLSUtils xlsUtils = new XLSUtils(downloadFilePath, newFileCopy);
            String startDatePricing = xlsUtils.getCellData("Pricing", 5, 6);
            String endDatePricing = xlsUtils.getCellData("Pricing", 6, 6);
            dataMap.put("11346", new SimpleDateFormat("MM-dd-yy").parse(startDatePricing));
            dataMap.put("11345", new SimpleDateFormat("MM-dd-yy").parse(endDatePricing));
            XLSUtils.editRowDataUsingColumnId(downloadFilePath, newFileCopy, "Pricing", 6, dataMap);
            fetchObj.hitFetch();
            allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
            customUploadPricing("fail", downloadFilePath, newFileCopy, sdFlow, customAssert);
            scpDownloadFile("Failure", "(Service End Date,Start Date Cannot Be After End Date)", allTaskIds, customAssert, "Pricing", downloadFilePath, downloadFileName, statusFileName, statusFilePath, newFileCopy, scpUtils);

        } catch (Exception e) {
            logger.error("Exception occurred {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception occurred " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void C3430() {


        CustomAssert customAssert = new CustomAssert();
        try {
            String contractFlowName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");

            assert contractFlowName != null : "Contract flows name are blank";

            int contract = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractFlowName);
            assert contract != -1 : "Contract not created";

            toBeDeleted.add(new EntityPojo(contract, "contracts"));
            logger.info("contract id : [{}]", contract);
            //***************************************************** create service data sd1 **************

            String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sdarc");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract));
            assert sdFlow != null : "Service data flow is null";

            int sd1 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, sdFlow, contract);

            assert sd1 != -1 : "sd1 not created";

            toBeDeleted.add(new EntityPojo(sd1, "service data"));
            logger.info("sd1 id : [{}]", sd1);


            //*************************************************** Download pricing template ******************************

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

            assert downloadFileName != null && downloadFilePath != null : "download file details not found";

            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
            assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";

            for (int index = 6; index <= 8; index++)
                XLSUtils.copyRowData(downloadFilePath, downloadFileName, "ARCRRC", index, index + 1);

            logger.info("Row data copied from index 6 to below rows");

            Map<String, Map<String, Object>> parentMap = new HashMap<>();
            int[] lower = {20, 51, 101, 151};
            int[] upper = {50, 99, 150, 199};
            double[] rate = {67, 5.2, 234, 29};
            String[] columnId = {"100000001", "8061", "8062", "8063", "8064"};
            String[] type = {"RRC", "RRC", "ARC", "ARC"};

            int count = -1;
            while (++count <= 3) {
                Map<String, Object> childMap = new HashMap<>();
                childMap.put(columnId[0], count + 1);
                childMap.put(columnId[1], lower[count]);
                childMap.put(columnId[2], upper[count]);
                childMap.put(columnId[3], rate[count]);
                childMap.put(columnId[4], type[count]);

                //editing 4 arc rows
                XLSUtils.editRowDataUsingColumnId(downloadFilePath, downloadFileName, "ARCRRC", count + 6, childMap);

                parentMap.put(String.valueOf(count + 6), childMap);
            }

            //editing 1 pricing row
            Map<String, Object> childMap = new HashMap<>();
            childMap.put("8052", 23);
            childMap.put("11348", 87);

            XLSUtils.editRowDataUsingColumnId(downloadFilePath, downloadFileName, "Pricing", 6, childMap);

            logger.info("Editing the ARCRRC and Pricing sheet with data {}", parentMap.toString());
            //XLSUtils.edi(downloadFilePath,downloadFileName,"ARCRRC",parentMap);

            logger.info("Editing done for ARCRRC sheet");

            customUploadPricing("pass", downloadFilePath, downloadFileName, sdFlow, customAssert);

        } catch (Exception e) {
            logger.error("Exception occurred {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception occurred " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void C3507() throws Exception { //also covers C3898, C3498
//        Create a contract c1 from supplier s1
//        Create a contract c2 from supplier s1
//        Create ccr ccr1 and cdr cdr1 from c2
//        Create service data sd1 from c1
//        Download the pricing template of sd1 - ccr1 and cdr1 filled
//        The pricing should not get uploaded


        CustomAssert customAssert = new CustomAssert();


        String contractC1FlowName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");
        String contractC2FlowName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c2");

        assert contractC1FlowName != null && contractC2FlowName != null : "Contract flows name are blank";

        String ccrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "ccr1");
        assert ccrFlow != null : "ccr3 flow name null";
        String cdrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "cdr1");
        assert cdrFlow != null : "cdr3 flow name null";

        int contract1 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractC1FlowName);
        assert contract1 != -1 : "Contract1 not created";

        toBeDeleted.add(new EntityPojo(contract1, "contracts"));
        logger.info("contract1 id : [{}]", contract1);

        int contract2 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractC1FlowName);
        assert contract2 != -1 : "Contract1 not created";

        toBeDeleted.add(new EntityPojo(contract2, "contracts"));
        logger.info("contract2 id : [{}]", contract2);

        //updating config files for ccr and cdr
        UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlow, "sourceid", String.valueOf(contract2));
        UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, cdrFlow, "sourceid", String.valueOf(contract2));


        //*************************************************** create ccr 1 ****************************
        String createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow,
                true);
        int ccr1 = -1;
        String ccr1Name = null;
        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
            logger.info("Create Status for Flow [{}]: {}", ccrFlow, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                ccr1 = CreateEntity.getNewEntityId(createResponse, "change requests");

            if (createStatus.equalsIgnoreCase("success")) {
                if (ccr1 != -1) {
                    logger.info("CR Created Successfully with Id {}: ", ccr1);
                    logger.info("Hitting Show API for CR Id {}", ccr1);
                    Show showObj = new Show();
                    showObj.hitShow(crEntityTypeId, ccr1);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        if (!showObj.isShowPageAccessible(showResponse)) {
                            customAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + ccr1);
                        }

                        ccr1Name = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                    } else {
                        customAssert.assertTrue(false, "Show API Response for CR Id " + ccr1 + " is an Invalid JSON.");
                    }
                }
            } else {
                customAssert.assertTrue(false, "Couldn't create CR for Flow [" + ccrFlow + "] due to " + createStatus);
            }
        } else {
            customAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + ccrFlow + "] is an Invalid JSON.");
        }
        assert ccr1 != -1 : "ccr1 not created";

        toBeDeleted.add(new EntityPojo(ccr1, "change requests"));
        logger.info("ccr1 id : [{}] and name : [{}]", ccr1, ccr1Name);

        //*************************************************** create cdr 1 ****************************
        createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow,
                true);
        int cdr1 = -1;
        String cdr1Name = null;
        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
            logger.info("Create Status for Flow [{}]: {}", cdrFlow, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                cdr1 = CreateEntity.getNewEntityId(createResponse, "change requests");

            if (createStatus.equalsIgnoreCase("success")) {
                if (cdr1 != -1) {
                    logger.info("CDR Created Successfully with Id {}: ", cdr1);
                    logger.info("Hitting Show API for CDR Id {}", cdr1);
                    Show showObj = new Show();
                    showObj.hitShow(cdrEntityTypeId, cdr1);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        customAssert.assertTrue(showObj.isShowPageAccessible(showResponse), "Show Page is Not Accessible for CDR Id " + cdr1);


                        cdr1Name = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                    } else {
                        customAssert.assertTrue(false, "Show API Response for CDR Id " + cdr1 + " is an Invalid JSON.");
                    }
                }
            } else {
                customAssert.assertTrue(false, "Couldn't create CDR for Flow [" + cdrFlow + "] due to " + createStatus);
            }
        } else {
            customAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + cdrFlow + "] is an Invalid JSON.");
        }

        assert cdr1 != -1 : "cdr1 not created";

        toBeDeleted.add(new EntityPojo(cdr1, "contract draft request"));
        logger.info("cdr1 id : [{}] and name : [{}]", cdr1, cdr1Name);

        //***************************************************** create service data sd1 **************

        String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sd1");
        UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract1));
        assert sdFlow != null : "Service data flow is null";

        int sd1 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, sdFlow, contract1);

        assert sd1 != -1 : "sd1 not created";

        toBeDeleted.add(new EntityPojo(sd1, "service data"));
        logger.info("sd1 id : [{}]", sd1);


        //*************************************************** Download pricing template ******************************

        String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
        String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

        assert downloadFileName != null && downloadFilePath != null : "download file details not found";

        InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
        invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));

        customAssert.assertTrue(FileUtils.fileExists(downloadFilePath, downloadFileName), "Downloaded pricing file doesn't exist after download");


        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitynamepricingid");
        assert temp != null && temp.length() > 0 : "Entity Name (Pricing) column id not found in config file";
        String entityNamePricingColumnId = temp;

        Map<String, Object> data;
        Fetch fetchObj;
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();
        for (String pricingChangeValue : new String[]{"case1success", "case2fail", ccr1Name, cdr1Name}) {

            logger.info("************** For case : {}", pricingChangeValue);

            if (!pricingChangeValue.contains("case")) {
                data = new HashMap<>();
                data.put(entityNamePricingColumnId, pricingChangeValue);
                XLSUtils.editRowDataUsingColumnId(downloadFilePath, downloadFileName, "Data", 6, data);
            }

            logger.info("Hitting Fetch API.");
            fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            if (pricingChangeValue.equalsIgnoreCase("case1success")) { //for test case C3507
                UpdateFile.updateConfigFileProperty(configFilePath, configFileName, sdFlow, "volumecolumnvalues",
                        "-" + ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sdFlow, "volumecolumnvalues"));
                UpdateFile.updateConfigFileProperty(configFilePath, configFileName, sdFlow, "ratecolumnvalues",
                        ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sdFlow, "ratecolumnvalues").replace("-", ""));
            } else if (pricingChangeValue.equalsIgnoreCase("case2fail")) { //for test case C3507
                UpdateFile.updateConfigFileProperty(configFilePath, configFileName, sdFlow, "ratecolumnvalues",
                        "-" + ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sdFlow, "ratecolumnvalues"));
                UpdateFile.updateConfigFileProperty(configFilePath, configFileName, sdFlow, "volumecolumnvalues",
                        ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sdFlow, "volumecolumnvalues").replace("-", ""));
            }

            InvoiceHelper.editPricingFileForPricing(configFilePath, configFileName, downloadFilePath, downloadFileName, sdFlow);

            String pricingUploadResponse = pricingObj.uploadPricing(downloadFilePath, downloadFileName);

            if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                    //Wait for Pricing Scheduler to Complete
                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(sdFlow, allTaskIds);
                    String expectation = pricingChangeValue.equalsIgnoreCase("case1success") ? "pass" : "fail";
                    logger.info("********************************Expected {}, Actual {}********************", expectation, pricingSchedulerStatus);
                    if (!pricingSchedulerStatus.trim().equalsIgnoreCase(expectation)) {
                        logger.error("Pricing Upload Task {}ed which was not expected. Hence skipping further validation for Flow [{}]", expectation, sdFlow);
                        customAssert.assertTrue(false, "Pricing Upload Task " + expectation + "ed which was not expected. Hence skipping further validation for Flow [" +
                                sdFlow + "|" + pricingChangeValue + "]");
                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", sdFlow);

                        logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", sdFlow);
                        customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                "Hence failing Flow [" + sdFlow + "|" + pricingChangeValue + "]");
                    }
                }
            } else {
                logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                        downloadFilePath + "/" + downloadFileName, sdFlow);
                customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                        downloadFilePath + "/" + downloadFileName + "] and Flow [" + sdFlow + "|" + pricingChangeValue + "]. Hence skipping further validation");
            }

            if (pricingChangeValue.contains("case")) { //for testing test case C3898


                int checkCount = 5;

                logger.info("Hitting Fetch API.");
                fetchObj = new Fetch();
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Pricing Job Create Job");

                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                int newRequestId = UserTasksHelper.getRequestIdFromTaskId(newTaskId);
                customAssert.assertTrue(newRequestId != -1, "-#$[SKIP]$#- Cannot find request id from the new task id");
                if (newRequestId == -1)
                    continue;

                String environment = "env";//ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"environment");
                //assert environment!=null:"Environment name is null in config file";

                String host = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "host");
                String user = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "user");
                String key = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "key");
                String withKey = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "withkey");

                boolean credentialStatus = host != null && user != null && key != null && withKey != null;
                customAssert.assertTrue(credentialStatus, "Environment details are empty");
                if (!credentialStatus)
                    continue;

                SCPUtils scpUtils;

                if (withKey.equalsIgnoreCase("yes"))
                    scpUtils = new SCPUtils(host, user, key, 22, withKey.equalsIgnoreCase("yes"));
                else
                    scpUtils = new SCPUtils(host, user, key, 22);
                String statusFileName = downloadFileName.split("[.]")[0] + "-servercopy." + downloadFileName.split("[.]")[1];
                String statusFilePath = downloadFilePath.charAt(downloadFilePath.length() - 1) == '/' ? downloadFilePath.substring(0, downloadFilePath.length() - 1) : downloadFilePath;

                FileUtils.deleteFile(downloadFilePath, statusFileName);

                while (--checkCount > 0) {
                    logger.info("Downloading the failed excel");

                    try {
                        scpUtils.downloadExcelFile(String.valueOf(newRequestId), statusFileName, statusFilePath);

                        logger.info("Downloaded file status : " + FileUtils.fileExists(downloadFilePath, statusFileName));

                        if (FileUtils.fileExists(downloadFilePath, statusFileName)) {
                            logger.info("File found");
                            break;
                        } else {
                            logger.error("File not found now, rechecking");
                        }

                    } catch (Exception e) {
                        logger.info("Error occurred in extracting file from server");
                        logger.info("Check count remaining : {}", checkCount);
                    }
                }

                customAssert.assertTrue(FileUtils.fileExists(downloadFilePath, statusFileName), "Downloaded file not found for case " + pricingChangeValue);
                if (!FileUtils.fileExists(downloadFilePath, statusFileName))
                    continue;

                XLSUtils xlsUtils = new XLSUtils(downloadFilePath + statusFileName);
                String errorStatus = xlsUtils.getProcessingStatus(6, "Pricing");

                logger.info("Checking error value in excel downloaded from server. Status in Column : {} for case {}", errorStatus, pricingChangeValue);

                if (pricingChangeValue.contains("success"))
                    customAssert.assertTrue(errorStatus.contains("Success"), "Error status in excel file " + downloadFilePath + statusFileName + " is not success");

                if (pricingChangeValue.contains("fail"))
                    customAssert.assertTrue(errorStatus.contains("Failure"), "Error status in excel file " + downloadFilePath + statusFileName + " is not failure");

            }

        }

        if (customAssert.getAllAssertionMessages().contains("-#$[SKIP]$#-"))
            throw new SkipException(customAssert.getAllAssertionMessages());
        customAssert.assertAll();
    }

    @Test(enabled = false) //has step 1 of C90577(partial), partial C90591
    public void C90573() throws Exception {
//        Create a contract c1 from supplier s1
//        Create service data sd1 from c1
//        Create ccr ccr1 from c1 without eff date and without approve
//        Download the pricing template of sd1 - ccr1 should not be found
//        Update ccr1 add eff date
//        Download the pricing template of sd1 - ccr1 should not be found
//        Update ccr1 do approval step
//        Download the pricing template of sd1 - ccr1 should be found


        CustomAssert customAssert = new CustomAssert();


        String contractC1FlowName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");

        assert contractC1FlowName != null : "Contract flows name are blank";

        String ccrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "ccr1");
        assert ccrFlow != null : "ccr flow name null";

        int contract1 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractC1FlowName);
        assert contract1 != -1 : "Contract1 not created";

        toBeDeleted.add(new EntityPojo(contract1, "contracts"));
        logger.info("contract1 id : [{}]", contract1);

        //updating config files for ccr and cdr
        UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlow, "sourceid", String.valueOf(contract1));


        //*************************************************** create ccr 1 ****************************

        //removing eff date
        String effectiveDate = "effectiveDate";
        String effectiveDateValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, effectiveDate);
        UpdateFile.deletePropertyFromConfigFile(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, effectiveDate);

        CustomData customData = getCCR(ccrFlow, customAssert, false);
        String ccr1Name = customData.name;


        //***************************************************** create service data sd1 **************

        String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sd1");
        UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract1));
        assert sdFlow != null : "Service data flow is null";

        int sd1 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, sdFlow, contract1);

        assert sd1 != -1 : "sd1 not created";

        toBeDeleted.add(new EntityPojo(sd1, "service data"));
        logger.info("sd1 id : [{}]", sd1);


        //*************************************************** Download pricing template ******************************

        String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
        String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

        assert downloadFileName != null && downloadFilePath != null : "download file details not found";

        InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
        invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
        assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";
        long rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
        List<String> masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
        logger.info("Master Data : {}", masterDataSheet);
        assert !masterDataSheet.contains(ccr1Name) : "Master data contains CCR which was not supposed to contain";
        FileUtils.deleteFile(downloadFilePath, downloadFileName);

        Edit edit = new Edit();
        String editPayload = edit.hitEdit("change requests", customData.id);

        JSONObject jsonObject = new JSONObject(editPayload);
        jsonObject.remove("actions");
        jsonObject.getJSONObject("body").remove("errors");
        jsonObject.getJSONObject("body").remove("globalData");
        jsonObject.getJSONObject("body").remove("layoutInfo");
        jsonObject.getJSONObject("body").remove("createLinks");
        jsonObject.remove("header");
        jsonObject.remove("session");

        jsonObject.getJSONObject("body").getJSONObject("data").put(effectiveDate, new JSONObject(effectiveDateValue));
        assert edit.hitEdit("change requests", jsonObject.toString()).contains("success") : "Edit is not success";

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        assert workflowActionsHelper.performWorkFlowStepV2(63, customData.id, "Submit", customAssert) : "Cannot perform Submit action on ccr " + customData.id;
        assert workflowActionsHelper.performWorkFlowStepV2(63, customData.id, "Approve", customAssert) : "Cannot perform Submit action on ccr " + customData.id;
        invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
        invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
        assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";
        rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
        masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
        logger.info("Master Data : {}", masterDataSheet);
        assert masterDataSheet.contains(ccr1Name) : "Master data doesn't contains CCR which was supposed to contain";
        FileUtils.deleteFile(downloadFilePath, downloadFileName);


        UpdateFile.addPropertyToConfigFile(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, effectiveDate, effectiveDateValue);
        ccr1Name = getCCR(ccrFlow, customAssert, false).name;
        invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
        invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
        assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";
        rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
        masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
        logger.info("Master Data : {}", masterDataSheet);
        assert !masterDataSheet.contains(ccr1Name) : "Master data contains CCR which was not supposed to contain";
        FileUtils.deleteFile(downloadFilePath, downloadFileName);

        UpdateFile.addPropertyToConfigFile(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, effectiveDate, effectiveDateValue);
        ccr1Name = getCCR(ccrFlow, customAssert, true).name;
        invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
        invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
        assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";
        rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
        masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
        logger.info("Master Data : {}", masterDataSheet);
        assert masterDataSheet.contains(ccr1Name) : "Master data doesn't contains CCR which was supposed to contain";
        FileUtils.deleteFile(downloadFilePath, downloadFileName);

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void C90577() throws Exception {

        CustomAssert customAssert = new CustomAssert();


        String contractC1FlowName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");

        assert contractC1FlowName != null : "Contract flows name are blank";

        String cdrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "cdr1");
        assert cdrFlow != null : "cdr3 flow name null";

        int contract1 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractC1FlowName);
        assert contract1 != -1 : "Contract1 not created";

        toBeDeleted.add(new EntityPojo(contract1, "contracts"));
        logger.info("contract1 id : [{}]", contract1);

        //updating config files for ccr and cdr
        UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, cdrFlow, "sourceid", String.valueOf(contract1));


        //*************************************************** create cdr ****************************

        //removing eff date
        String effectiveDate = "effectiveDate";
        UpdateFile.deletePropertyFromConfigFile(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, cdrFlow, effectiveDate);
        String createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow,
                true);
        int cdr = -1;
        String cdrName = null;
        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
            logger.info("Create Status for Flow [{}]: {}", cdrFlow, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                cdr = CreateEntity.getNewEntityId(createResponse, "change requests");

            if (createStatus.equalsIgnoreCase("success")) {
                if (cdr != -1) {
                    logger.info("CDR Created Successfully with Id {}: ", cdr);
                    logger.info("Hitting Show API for CDR Id {}", cdr);
                    Show showObj = new Show();
                    showObj.hitShow(cdrEntityTypeId, cdr);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        if (!showObj.isShowPageAccessible(showResponse)) {
                            customAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdr);
                        }

                        cdrName = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                    } else {
                        customAssert.assertTrue(false, "Show API Response for CDR Id " + cdr + " is an Invalid JSON.");
                    }
                }
            } else {
                customAssert.assertTrue(false, "Couldn't create CDR for Flow [" + cdrFlow + "] due to " + createStatus);
            }
        } else {
            customAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + cdrFlow + "] is an Invalid JSON.");
        }

        assert cdr != -1 : "cdr not created";

        toBeDeleted.add(new EntityPojo(cdr, "contract draft request"));
        logger.info("cdr id : [{}] and name : [{}]", cdr, cdrName);


        //***************************************************** create service data sd1 **************

        String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sd1");
        UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract1));
        assert sdFlow != null : "Service data flow is null";

        int sd1 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, sdFlow, contract1);

        assert sd1 != -1 : "sd1 not created";

        toBeDeleted.add(new EntityPojo(sd1, "service data"));
        logger.info("sd1 id : [{}]", sd1);


        //*************************************************** Download pricing template ******************************

        String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
        String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

        assert downloadFileName != null && downloadFilePath != null : "download file details not found";

        InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
        invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
        assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist after download";
        long rowCount = XLSUtils.getNoOfRows(downloadFilePath, downloadFileName, "Master Data");
        List<String> masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath, downloadFileName, "Master Data", 0, 4, (int) rowCount);
        logger.info("Master Data : {}", masterDataSheet);
        assert !masterDataSheet.contains(cdrName) : "Master data contains CDR which was not supposed to contain";
        FileUtils.deleteFile(downloadFilePath, downloadFileName);

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void C90576() throws ConfigurationException {
        CustomAssert customAssert = new CustomAssert();

        String contractC1FlowName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "c1");

        assert contractC1FlowName != null : "Contract flows name are blank";

        String ccrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "ccr1");
        assert ccrFlow != null : "ccr flow name null";
        String cdrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "cdr1");
        assert cdrFlow != null : "cdr flow name null";

        int contract1 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractC1FlowName);
        assert contract1 != -1 : "Contract1 not created";

        toBeDeleted.add(new EntityPojo(contract1, "contracts"));
        logger.info("contract1 id : [{}]", contract1);

        //updating config files for ccr and cdr
        UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlow, "sourceid", String.valueOf(contract1));
        UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, cdrFlow, "sourceid", String.valueOf(contract1));

        CustomData ccr1 = getCCR(ccrFlow, customAssert, true);
        CustomData ccr2 = getCCR(ccrFlow, customAssert, true);
        CustomData cdr1 = getCDR(cdrFlow, customAssert);
        CustomData cdr2 = getCDR(cdrFlow, customAssert);


        //***************************************************** create service data sd1 **************

        String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows", "sd1");
        UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract1));
        assert sdFlow != null : "Service data flow is null";

        int sd1 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, sdFlow, contract1);

        assert sd1 != -1 : "sd1 not created";

        toBeDeleted.add(new EntityPojo(sd1, "service data"));
        logger.info("sd1 id : [{}]", sd1);

        //*********************************************** Validating **********************************

        uploadPricing("pass", sdFlow, ccr1.name, sd1, customAssert);
        uploadPricing("pass", sdFlow, ccr2.name, sd1, customAssert);
        uploadPricing("pass", sdFlow, cdr1.name, sd1, customAssert);
        uploadPricing("pass", sdFlow, cdr2.name, sd1, customAssert);

        uploadPricing("fail", sdFlow, ccr1.name, sd1, customAssert);
        uploadPricing("fail", sdFlow, ccr2.name, sd1, customAssert);
        uploadPricing("fail", sdFlow, cdr1.name, sd1, customAssert);
        uploadPricing("fail", sdFlow, cdr2.name, sd1, customAssert);


        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void C89541() {
        CustomAssert customAssert = new CustomAssert();
        String testCase = "c89541";

        try {
            int serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, testCase, "servicedataid"));

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
            final String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

            assert downloadFileName != null && downloadFilePath != null : "download file details not found";
            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(serviceDataId)));

            assert FileUtils.fileExists(downloadFilePath, downloadFileName) : "Downloaded pricing file doesn't exist for only pricing available entity";

            String valuesSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, testCase, "rowvaluessection");
            List<String> sheetDataToFill = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, valuesSectionName);

            assert !sheetDataToFill.isEmpty() : "Cannot find the data to be filled in the pricing template";

            final String newFileCopy = downloadFileName.split("[.]")[0] + "-copy." + downloadFileName.split("[.]")[1];


            customUploadPricing("pass", downloadFilePath, downloadFileName, "Happy Case", customAssert);
            String environment = "env";//ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"environment");
            //assert environment!=null:"Environment name is null in config file";

            String host = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "host");
            String user = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "user");
            String key = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "key");
            String withKey = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, environment, "withkey");

            boolean credentialStatus = host != null && user != null && key != null && withKey != null;
            customAssert.assertTrue(credentialStatus, "Environment details are empty");

            SCPUtils scpUtils;

            if (withKey.equalsIgnoreCase("yes"))
                scpUtils = new SCPUtils(host, user, key, 22, withKey.equalsIgnoreCase("yes"));
            else
                scpUtils = new SCPUtils(host, user, key, 22);
            String statusFileName = downloadFileName.split("[.]")[0] + "-servercopy." + downloadFileName.split("[.]")[1];
            String statusFilePath = downloadFilePath.charAt(downloadFilePath.length() - 1) == '/' ? downloadFilePath.substring(0, downloadFilePath.length() - 1) : downloadFilePath;


            for (String data : sheetDataToFill) {
                FileUtils.deleteFile(downloadFilePath, newFileCopy);
                assert !FileUtils.fileExists(downloadFilePath, newFileCopy);
                assert FileUtils.copyFile(downloadFilePath, downloadFileName, downloadFilePath, newFileCopy) : "Cannot copy the existing pricing file";


                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());


                String[] rowDataArray = data.split(",");
                for (String rowData : rowDataArray) {
                    rowData = rowData.replace("@@@", ".");

                    String[] cellData = rowData.split("&[$]%");
                    customAssert.assertTrue(cellData.length == 5, "Complete cell data not found " + rowData);
                    if (cellData.length != 5)
                        continue;
                    Map<Integer, Object> map = new HashMap<>();
                    map.put(Integer.parseInt(cellData[2]), NumberUtils.isParsable(cellData[3]) ? Double.parseDouble(cellData[3]) : cellData[3]);
                    assert XLSUtils.editRowData(downloadFilePath, newFileCopy, cellData[0], Integer.parseInt(cellData[1]), map) : "Pricing template edit failed for " + rowData;
                }

                customUploadPricing(rowDataArray[rowDataArray.length - 1].split("&[$]%")[4], downloadFilePath, newFileCopy, Arrays.toString(rowDataArray), customAssert);
                int checkCount = 5;

                logger.info("Hitting Fetch API.");
                fetchObj = new Fetch();
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Pricing Job Create Job");

                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                int newRequestId = UserTasksHelper.getRequestIdFromTaskId(newTaskId);
                customAssert.assertTrue(newRequestId != -1, "Cannot find request id from the new task id");
                if (newRequestId == -1)
                    continue;

                FileUtils.deleteFile(downloadFilePath, statusFileName);

                while (--checkCount > 0) {
                    logger.info("Downloading the failed excel");

                    try {
                        scpUtils.downloadExcelFile(String.valueOf(newRequestId), statusFileName, downloadFilePath);

                        logger.info("Downloaded file status : " + FileUtils.fileExists(downloadFilePath, statusFileName));

                        if (FileUtils.fileExists(statusFilePath, statusFileName)) {
                            logger.info("File found");
                            break;
                        } else {
                            logger.error("File not found now, rechecking");
                        }

                    } catch (Exception e) {
                        logger.info("Error occurred in extracting file from server {}", (Object) e.getStackTrace());
                        logger.info("Check count remaining : {}", checkCount);
                    }

                    Thread.sleep(5000);
                }

                customAssert.assertTrue(FileUtils.fileExists(statusFilePath, statusFileName), "Downloaded file not found for case " + Arrays.toString(rowDataArray));
                if (!FileUtils.fileExists(statusFilePath, statusFileName))
                    continue;

                XLSUtils xlsUtils = new XLSUtils(downloadFilePath + statusFileName);
                String errorStatus = xlsUtils.getProcessingStatus(6, rowDataArray[0].split("&[$]%")[0]);

                logger.info("**************Checking error value in excel downloaded from server. Status in Column : {} for case {}", errorStatus, Arrays.toString(rowDataArray));

//                    if (pricingChangeValue.contains("success"))
//                        customAssert.assertTrue(errorStatus.contains("Success"), "Error status in excel file " + downloadFilePath + statusFileName + " is not success");
//
//                    if (pricingChangeValue.contains("fail"))
//                        customAssert.assertTrue(errorStatus.contains("Failure"), "Error status in excel file " + downloadFilePath + statusFileName + " is not failure");

            }

        } catch (Exception e) {
            logger.error("Exception occurred {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception occurred" + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    private CustomData getCCR(String ccrFlow, CustomAssert customAssert, boolean approve) {
        String createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow,
                true);
        int ccr1 = -1;
        String ccr1Name = null;
        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
            logger.info("Create Status for Flow [{}]: {}", ccrFlow, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                ccr1 = CreateEntity.getNewEntityId(createResponse, "change requests");

            if (createStatus.equalsIgnoreCase("success")) {
                if (ccr1 != -1) {
                    logger.info("CR Created Successfully with Id {}: ", ccr1);
                    logger.info("Hitting Show API for CR Id {}", ccr1);
                    Show showObj = new Show();
                    showObj.hitShow(crEntityTypeId, ccr1);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        if (!showObj.isShowPageAccessible(showResponse)) {
                            customAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + ccr1);
                        }

                        ccr1Name = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                    } else {
                        customAssert.assertTrue(false, "Show API Response for CR Id " + ccr1 + " is an Invalid JSON.");
                    }
                }
            } else {
                customAssert.assertTrue(false, "Couldn't create CR for Flow [" + ccrFlow + "] due to " + createStatus);
            }
        } else {
            customAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + ccrFlow + "] is an Invalid JSON.");
        }
        assert ccr1 != -1 : "ccr1 not created";

        toBeDeleted.add(new EntityPojo(ccr1, "change requests"));
        logger.info("ccr1 id : [{}] and name : [{}]", ccr1, ccr1Name);

        if (approve) {
            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
            assert workflowActionsHelper.performWorkFlowStepV2(63, ccr1, "Submit", customAssert) : "Cannot perform Submit action on ccr " + ccr1;
            assert workflowActionsHelper.performWorkFlowStepV2(63, ccr1, "Approve", customAssert) : "Cannot perform Submit action on ccr " + ccr1;
        }

        return new CustomData(ccr1, ccr1Name);
    }

    private CustomData getCDR(String cdrFlow, CustomAssert customAssert) {
        return getCDR(cdrFlow, customAssert, true);
    }

    private CustomData getCDR(String cdrFlow, CustomAssert customAssert, boolean local) {
        String createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow,
                local);
        int cdr = -1;
        String cdrName = null;
        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
            logger.info("Create Status for Flow [{}]: {}", cdrFlow, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                cdr = CreateEntity.getNewEntityId(createResponse, "change requests");

            if (createStatus.equalsIgnoreCase("success")) {
                if (cdr != -1) {
                    logger.info("CDR Created Successfully with Id {}: ", cdr);
                    logger.info("Hitting Show API for CDR Id {}", cdr);
                    Show showObj = new Show();
                    showObj.hitShow(cdrEntityTypeId, cdr);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        if (!showObj.isShowPageAccessible(showResponse)) {
                            customAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdr);
                        }

                        cdrName = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                    } else {
                        customAssert.assertTrue(false, "Show API Response for CDR Id " + cdr + " is an Invalid JSON.");
                    }
                }
            } else {
                customAssert.assertTrue(false, "Couldn't create CDR for Flow [" + cdrFlow + "] due to " + createStatus);
            }
        } else {
            customAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + cdrFlow + "] is an Invalid JSON.");
        }

        assert cdr != -1 : "cdr not created";

        toBeDeleted.add(new EntityPojo(cdr, "contract draft request"));
        logger.info("cdr id : [{}] and name : [{}]", cdr, cdrName);

        return new CustomData(cdr, cdrName);
    }

    private void uploadPricing(String requiredCase, String sdFlow, String nameOfSubstitute, int sd, CustomAssert customAssert) throws ConfigurationException {

        try {

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

            assert downloadFileName != null && downloadFilePath != null : "download file details not found";
            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd)));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for only pricing available entity");
            }

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
            InvoicePricingHelper pricingObj = new InvoicePricingHelper();

            String entitynamepricingid = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitynamepricingid");
            assert entitynamepricingid != null && entitynamepricingid.length() > 0 : "Entity Name (Pricing) column id not found in config file";

            HashMap<String, Object> data = new HashMap<>();
            data.put(entitynamepricingid, nameOfSubstitute);
            XLSUtils.editRowDataUsingColumnId(downloadFilePath, downloadFileName, "Data", 6, data);

            InvoiceHelper.editPricingFileForPricing(configFilePath, configFileName, downloadFilePath, downloadFileName, sdFlow);

            String pricingUploadResponse = pricingObj.uploadPricing(downloadFilePath, downloadFileName);

            if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                    //Wait for Pricing Scheduler to Complete
                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(sdFlow, allTaskIds);
                    logger.info("********************************Expected {}, Actual {}********************", requiredCase, pricingSchedulerStatus);
                    if (!pricingSchedulerStatus.trim().equalsIgnoreCase(requiredCase)) {
                        logger.error("Pricing Upload Task {}ed which was not expected. Hence skipping further validation for Flow [{}]", pricingSchedulerStatus, sdFlow);
                        customAssert.assertTrue(false, "Pricing Upload Task " + requiredCase + "ed which was not expected. Hence skipping further validation for Flow [" +
                                sdFlow + "]");
                        return;
                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", sdFlow);

                        logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", sdFlow);
                        customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                "Hence failing Flow [" + sdFlow + "]");
                        return;
                    }
                }
            } else {
                logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                        downloadFilePath + "/" + downloadFileName, sdFlow);
                customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                        downloadFilePath + "/" + downloadFileName + "] and Flow [" + sdFlow + "]. Hence skipping further validation");
            }

        } catch (Exception e) {
            logger.error("Exception caught in uploadPricing() {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught in uploadPricing " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void customUploadPricing(String requiredCase, String downloadFilePath, String downloadFileName, String flow, CustomAssert customAssert) throws ConfigurationException {

        try {

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
            InvoicePricingHelper pricingObj = new InvoicePricingHelper();
            String pricingUploadResponse = pricingObj.uploadPricing(downloadFilePath, downloadFileName);

            if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                    //Wait for Pricing Scheduler to Complete
                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler("", allTaskIds);
                    logger.info("********************************Expected {}, Actual {}********************", requiredCase, pricingSchedulerStatus);
                    if (!pricingSchedulerStatus.trim().equalsIgnoreCase(requiredCase)) {
                        logger.error("Pricing Upload Task {}ed which was not expected. Flow {}", pricingSchedulerStatus, flow);
                        customAssert.assertTrue(false, "Pricing Upload Task " + pricingSchedulerStatus + "ed which was not expected. Flow " + flow);
                        return;
                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                        logger.info("Pricing Upload Task didn't complete in specified time.  Flow {}", flow);

                        logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on.");
                        customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Flow " + flow);
                        return;
                    }
                }
            } else {
                logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}]. Hence skipping further validation",
                        downloadFilePath + "/" + downloadFileName);
                customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                        downloadFilePath + "/" + downloadFileName + "]");
            }

        } catch (Exception e) {
            logger.error("Exception caught in uploadPricing() {}. Flow {}", (Object) e.getStackTrace(), flow);
            customAssert.assertTrue(false, "Exception caught in uploadPricing " + Arrays.toString(e.getStackTrace()) + " adn Flow {}" + flow);
        }
    }

    private void scpDownloadFile(String expectedResult, String expectedResultString, List<Integer> allTaskIds, CustomAssert customAssert, String sheetName, String downloadFilePath, String downloadFileName, String statusFileName, String statusFilePath, String newFileCopy, SCPUtils scpUtils) throws IOException, InterruptedException {
        int checkCount = 5;

        logger.info("Hitting Fetch API.");
        Fetch fetchObj = new Fetch();
        fetchObj.hitFetch();
        logger.info("Getting Task Id of Pricing Job Create Job");

        int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

        int newRequestId = UserTasksHelper.getRequestIdFromTaskId(newTaskId);
        customAssert.assertTrue(newRequestId != -1, "Cannot find request id from the new task id");
        if (newRequestId == -1)
            return;

        FileUtils.deleteFile(downloadFilePath, statusFileName);

        while (--checkCount > 0) {
            logger.info("Downloading the failed excel");

            try {
                scpUtils.downloadExcelFile(String.valueOf(newRequestId), statusFileName, downloadFilePath);

                logger.info("Downloaded file status : " + FileUtils.fileExists(downloadFilePath, statusFileName));

                if (FileUtils.fileExists(statusFilePath, statusFileName)) {
                    logger.info("File found");
                    break;
                } else {
                    logger.error("File not found now, rechecking");
                }

            } catch (Exception e) {
                logger.info("Error occurred in extracting file from server {}", (Object) e.getStackTrace());
                logger.info("Check count remaining : {}", checkCount);
            }

            Thread.sleep(5000);
        }

        customAssert.assertTrue(FileUtils.fileExists(statusFilePath, statusFileName), "Downloaded file not found");
        if (!FileUtils.fileExists(statusFilePath, statusFileName))
            return;

        XLSUtils xlsUtils = new XLSUtils(downloadFilePath + statusFileName);
        String errorStatus = xlsUtils.getProcessingStatus(6, sheetName);

        logger.info("**************Checking error value in excel downloaded from server. Status in Column : {}", errorStatus);

        logger.info("Expected [{}] and [{}] : Actual {}", expectedResult, expectedResultString, errorStatus);
        customAssert.assertTrue(errorStatus.contains(expectedResult), "The status value in sheet is : " + errorStatus + " | while we expected " + expectedResult);
        customAssert.assertTrue(errorStatus.contains(expectedResultString), "The status value in sheet is : " + errorStatus + " | while we expected String " + expectedResultString);

        FileUtils.deleteFile(downloadFilePath, newFileCopy);
        assert !FileUtils.fileExists(downloadFilePath, newFileCopy);
        assert FileUtils.copyFile(downloadFilePath, downloadFileName, downloadFilePath, newFileCopy) : "Cannot copy the existing pricing file";

    }

    private CustomData getCCRMultiSup(String ccrFlow, CustomAssert customAssert, boolean approve, int supplierId) {
        CustomData customData = new CustomData(-1, null);
        try {
            Show supplierShow = new Show();
            supplierShow.hitShow(1, supplierId);

            String supplierName = ShowHelper.getSupplierNameFromShowResponse(supplierShow.getShowJsonStr(), 1);

            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, "supplier",
                    "sup_name", supplierName);
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, "supplier",
                    "sup_id", String.valueOf(supplierId));

            customData = getCCR(ccrFlow, customAssert, true);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, "supplier",
                    supplierName, "sup_name");
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow, "supplier",
                    String.valueOf(supplierId), "sup_id");
        } catch (Exception e) {
            logger.error("Exception caught : {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught " + Arrays.toString(e.getStackTrace()));
        }

        return customData;
    }

    private CustomData getCDRMultiSup(String cdrFlow, CustomAssert customAssert, boolean approve, int supplierId) {
        return getCDRMultiSup(cdrFlow, customAssert, approve, supplierId, true);
    }

    private CustomData getCDRMultiSup(String cdrFlow, CustomAssert customAssert, boolean approve, int supplierId, boolean local) {
        CustomData customData = new CustomData(-1, null);
        try {
            Show supplierShow = new Show();
            supplierShow.hitShow(1, supplierId);

            String supplierName = ShowHelper.getSupplierNameFromShowResponse(supplierShow.getShowJsonStr(), 1);

            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow, "supplier",
                    "sup_name", supplierName);
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow, "supplier",
                    "sup_id", String.valueOf(supplierId));

            customData = getCDR(cdrFlow, customAssert);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow, "supplier",
                    supplierName, "sup_name");
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow, "supplier",
                    String.valueOf(supplierId), "sup_id");
        } catch (Exception e) {
            logger.error("Exception caught : {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught " + Arrays.toString(e.getStackTrace()));
        }

        return customData;
    }

    private void downloadPricingTemplate(int serviceData, CustomAssert customAssert) throws Exception {
        String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFilePath");
        String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "downloadFileName");

        assert downloadFileName != null && downloadFilePath != null : "download file details not found";

        FileUtils.deleteFile(downloadFilePath, downloadFileName);
        InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
        invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(serviceData)));
        customAssert.assertTrue(FileUtils.fileExists(downloadFilePath, downloadFileName), "Downloaded pricing file doesn't exist after download");
    }

    class CustomData {
        private String name;
        private int id;

        CustomData(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class EntityPojo {
        public EntityPojo(int id, String name) {
            this.entityId = id;
            this.name = name;
        }

        int entityId;
        String name;
    }
}
