package com.sirionlabs.test.clientAdmin;

import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.AuditLogs;
import com.sirionlabs.api.clientAdmin.invoiceCopy.Copy;
import com.sirionlabs.api.clientAdmin.invoiceCopy.Format;
import com.sirionlabs.api.clientAdmin.invoiceCopy.Template;
import com.sirionlabs.api.clientAdmin.invoiceCopy.UploadInvoiceCopy;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.deleteComment.CommentAttachmentDelete;
import com.sirionlabs.api.documentViewer.DocumentViewerShow;
import com.sirionlabs.api.invoice.InvoiceCopyViewer;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.log4j.net.SyslogAppender;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Test_InvoiceCopyGeneration {

    private final static Logger logger = LoggerFactory.getLogger(Test_InvoiceCopyGeneration.class);

    int clientId;
    int supplierId;
    int contractId;

    private String configFilePath;
    private String configFileName;

    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;

    private String invoiceLineItemConfigFilePath;
    private String invoiceLineItemConfigFileName;
    private String invoiceLineItemExtraFieldsConfigFileName;

    int invoiceEntityTypeId = 67;
    int lineItemEntityTypeId = 165;
    int supplierEntityTypeId = 1;
    int contractEntityTypeId = 61;

    String userName = "Jyoti User";

    String invoices = "invoices";
    String lineItem = "invoice line item";

    List<Integer> invoicesToDelete = new ArrayList<>();
    List<Integer> invoicesLineItemToDelete = new ArrayList<>();

    String globalTemplateId;

    String regenerate = "Regenerate";

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestInvoiceGenerateFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestInvoiceGenerateFileName");

        String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

        Check check = new Check();
        check.hitCheck(adminUserName,adminPassword);

        invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
        invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
        invoiceExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");

        //Invoice Line Item Config files
        invoiceLineItemConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
        invoiceLineItemConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
        invoiceLineItemExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("invoiceLineItemExtraFieldsFileName");

        clientId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"client id"));
        supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier id"));
        contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contract id"));

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        invoiceHelper.resetAllFormatAndTemplate(String.valueOf(clientId));
    }

    @Test(enabled = false)
    public void Test_FormatCases(){

        CustomAssert customAssert = new CustomAssert();

        try {

            String formatId = validateCreateFormatId(customAssert);

            String updateNameOfFormatId = "Custom Template";

            validateUpdateFormatId(formatId, updateNameOfFormatId, customAssert);

            validateDelete(formatId, customAssert);

        }catch (Exception e){
            logger.error("Exception in Test Method " + e.getMessage());
            customAssert.assertTrue(false,"Exception in Test Method " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void Test_TemplateCases(){

        CustomAssert customAssert = new CustomAssert();

        try {
            globalTemplateId = validateCreateTemplateId(customAssert);

            String updateNameOfTemplateId = "Custom Template " + DateUtils.getCurrentTimeStamp();

            validateUpdateTemplateId(globalTemplateId,updateNameOfTemplateId,customAssert);

            String filePath = "src\\test\\resources\\TestConfig\\clientAdmin\\logo";
            String fileName = "pngfile.png";
            validateUploadTemplateLogo(globalTemplateId,filePath,fileName,customAssert);

            String fileNameDownload = "pngfile_download.png";

            validateDownloadTemplateLogo(globalTemplateId,filePath,fileNameDownload,customAssert);

            FileUtils.deleteFile(filePath,fileNameDownload);

        }catch (Exception e){
            logger.error("Exception in Test Method " + e.getMessage());
            customAssert.assertTrue(false,"Exception in Test Method " + e.getMessage());
        }

        customAssert.assertAll();
    }


    @Test(enabled = true,dependsOnMethods = "Test_TemplateCases")
//    @Test(enabled = true)
    public void Test_InvoiceCopyGeneration(){

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "arc flow 1";

        Check check = new Check();
        check.hitCheck();

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        String approve = "Approve";
        String approveInvoice = "ApproveInvoice";

        String outputFilePath = "src\\test\\resources\\TestConfig\\Invoice\\InvoiceCopyViewer";
        String outputFileName = "InvoiceCopy.pdf";
        String outputFile = outputFilePath + "\\" + outputFileName;

        String genInvCopyButton =  "Generate Invoice Copy";
        try{

            int serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service data id"));

            //Creating a new Invoice And Line Item
            int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName,invoiceExtraFieldsConfigFileName,
                    invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName,flowToTest);

            invoicesToDelete.add(invoiceId);
            int invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);

              invoicesLineItemToDelete.add(invoiceLineItemId);

//Before Approving line Item Generate Invoice should not be there
            invoiceHelper.checkIfPartButtonPresentInV3Response(invoiceId,genInvCopyButton,false,customAssert);

//            //Approving the invoice and line Item
            workflowActionsHelper.performWorkFlowStepV2(lineItemEntityTypeId,invoiceLineItemId,approve,customAssert);

//After Approving line Item But Not Approving invoice Generate Invoice should not be there
            invoiceHelper.checkIfPartButtonPresentInV3Response(invoiceId,genInvCopyButton,false,customAssert);
            workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId,invoiceId,approveInvoice,customAssert);

//            After Approving line Item And Approving invoice Generate Invoice should be there
            invoiceHelper.checkIfPartButtonPresentInV3Response(invoiceId,genInvCopyButton,true,customAssert);
//

//            generateInvoice(invoiceId,customAssert);
            String apiPath = "/invoice-copy/generate?invoiceId=" + invoiceId +"";
            Actions.hitActionApiGet(apiPath);

            String documentViewerLink =  getInvoiceCopyViewerLink(invoiceId,customAssert);

            if(documentViewerLink !=null) {

                String [] documentViewerLinkArray = documentViewerLink.split("/");
                String documentId =  documentViewerLinkArray[documentViewerLinkArray.length -1];

                StringBuilder fileId = new StringBuilder();
                validateCommunicationLink(invoiceId,documentId,fileId,customAssert);

                downloadInvoiceCopy(outputFilePath,outputFileName,documentId,fileId.toString(),customAssert);

                ArrayList<String> expectedPDFList = createExpectedPDFList(supplierId,contractId,invoiceId,invoiceLineItemId);

                validateLineItemListInGenPDF(outputFile,expectedPDFList,customAssert);

                validateRegenerateOption(invoices,invoiceId,invoiceLineItemId,outputFilePath,outputFileName,customAssert);

                int filterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filter id"));
                int customFieldId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom field id"));

                validateInvoiceCopyOnListingAndShowPage(invoiceId,filterId,customFieldId,customAssert);

                validateDeleteAttachment(invoiceId,customAssert);

            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validaitng the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();

    }

    @Test(enabled = true,dependsOnMethods = "Test_TemplateCases")
    public void bulkGenerateInvoiceCopy(){

        CustomAssert customAssert = new CustomAssert();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        try{
            Check check = new Check();
            check.hitCheck();

            String outputFilePath = "src\\test\\resources\\TestConfig\\Invoice\\InvoiceCopyViewer";
            String outputFileName = "InvoiceCopy.pdf";
            String outputFile = outputFilePath + "\\" + outputFileName;

            String jobName = "Bulk Generate Invoice Copy";
            int serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service data id"));

            String flowToTest = "arc flow 1";
            String approve = "Approve";String approveInvoice = "ApproveInvoice";

            //Creating 5 invoices and line Items and then perform bulk generate

            ArrayList<String> invoiceIdsList = new ArrayList<>();
            ArrayList<String> invoiceLineItemIdsList = new ArrayList<>();

            int totalInvToCreate = 5;
            for(int i=0;i<totalInvToCreate;i++) {
                //Creating a new Invoice And Line Item
                int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName,
                        invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest);

                        invoicesToDelete.add(invoiceId);
                invoiceIdsList.add(String.valueOf(invoiceId));
                int invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);
                invoiceLineItemIdsList.add(String.valueOf(invoiceLineItemId));
              invoicesLineItemToDelete.add(invoiceLineItemId);
//For third invoice none invoice or line item is approved
                if(i<3) {
//                Approving the invoice and line Item
                workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId, approveInvoice, customAssert);
                workflowActionsHelper.performWorkFlowStepV2(lineItemEntityTypeId, invoiceLineItemId, approve, customAssert);

                }else if(i ==4 ){
                    //Only invoice Line Item is approved not Invoice  C151350
                    workflowActionsHelper.performWorkFlowStepV2(lineItemEntityTypeId, invoiceLineItemId, approve, customAssert);
                }

            }

            String invoiceIds = "";
            for(int i=0;i<invoiceIdsList.size();i++){
                invoiceIds += invoiceIdsList.get(i) + ",";
            }
            invoiceIds = invoiceIds.substring(0,invoiceIds.length() -1);

            UserTasksHelper.removeAllTasks();

            Copy copy = new Copy();
            Map<String,String> parameters = new HashMap<>();

            parameters.put("invoiceIds",invoiceIds);
            parameters.put("_csrf_token","null");

            String bulkGenerateResponse = copy.bulkGenerateCopy(parameters);

            if(!bulkGenerateResponse.contains("Request Submitted Successfully")){
                customAssert.assertTrue(false,"Bulk Generate Copy Request Submitted unsuccessfully");
            }else {

                int expPassCount = 3;
                int expFailCount = 2;

//              C151353 C151350
                invoiceHelper.validateSchedulerForPassFailEntities(jobName,expPassCount,expFailCount,totalInvToCreate,customAssert);

                for(int i =0;i<expPassCount;i++) {

                    int invoiceId = Integer.parseInt(invoiceIdsList.get(i));

//                    C151354
                    if(!getAuditLogLatestAction(invoiceId,customAssert).equals("Invoice Copy Generated (Bulk)")){
                        customAssert.assertTrue(false,"After Bulk Copy Generation For Invoices Audit Log validated unsuccessfully");
                    }

                    int invoiceLineItemId = Integer.parseInt(invoiceLineItemIdsList.get(i));
                    String documentViewerLink = getInvoiceCopyViewerLink(invoiceId, customAssert);

                    if (documentViewerLink != null) {

                        String[] documentViewerLinkArray = documentViewerLink.split("/");
                        String documentId = documentViewerLinkArray[documentViewerLinkArray.length - 1];

                        StringBuilder fileId = new StringBuilder();
                        validateCommunicationLink(invoiceId, documentId, fileId, customAssert);

                        downloadInvoiceCopy(outputFilePath, outputFileName, documentId, fileId.toString(), customAssert);

                        ArrayList<String> expectedPDFList = createExpectedPDFList(supplierId, contractId, invoiceId, invoiceLineItemId);

                        validateLineItemListInGenPDF(outputFile, expectedPDFList, customAssert);


                    }
                }

            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario in main test method " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    @AfterClass
    public void afterClass(){

        Check check = new Check();

        CustomAssert customAssert = new CustomAssert();
        String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

        check.hitCheck(adminUserName,adminPassword);
        validateDeleteTemplate(globalTemplateId,customAssert);

        check.hitCheck();

        EntityOperationsHelper.deleteMultipleRecords("invoice line item",invoicesLineItemToDelete);

        EntityOperationsHelper.deleteMultipleRecords("invoices",invoicesToDelete);

        customAssert.assertAll();

    }

    private String createPayloadForFormatCreate(){

        String payload = "{\n" +
                "\t\"name\" : \"Custom Template Demo Automation\",\n" +
                "\t\"clientId\" : " + clientId + ",\n" +
                "\t\"bodyFtl\" : \"<!DOCTYPE html>\\n<html lang=\\\"en\\\">\\n<head>\\n    <meta content=\\\"text/html; charset=UTF-8\\\" http-equiv=\\\"Content-Type\\\"/>\\n    <meta content=\\\"IE=Edge\\\" http-equiv=\\\"X-UA-Compatible\\\"/>\\n    <meta name=\\\"robots\\\" content=\\\"noindex\\\" />\\n    <meta name=\\\"viewport\\\" content=\\\"width=device-width, initial-scale=1.0, user-scalable=no\\\"/>\\n    <title>Invoice Copy PDF</title>\\n    <link rel=\\\"stylesheet\\\" type=\\\"text/css\\\" href=\\\"invoice_copy.css\\\"/>\\n</head>\\n<body>\\n<div class=\\\"pdf-container\\\">\\n    <div class=\\\"invoice-header\\\">\\n        <div class=\\\"fl-left-header\\\">\\n            <h3 class=\\\"invoice-details\\\">INVOICE</h3>\\n            <div class=\\\"company-logo\\\">\\n                <img src=\\\"${logo}\\\" alt=\\\"Logo\\\"/>\\n            </div>\\n            <div class=\\\"invoice-details\\\">\\n                <h4 class=\\\"normal\\\">${Supplier}</h4>\\n                <div class=\\\"smallest\\\">\\n                    <p>Phone no : ${Phone_Number}</p>\\n                    <p class=\\\"cl-blue\\\">Email address : ${Email_Address}</p>\\n                </div>\\n                <h4 class=\\\"bold\\\">Bill to Address</h4>\\n                <p>Value</p>\\n            </div>\\n        </div>\\n        <div class=\\\"fl-right-header\\\">\\n            <div class=\\\"fl-left invoice-info-group\\\">\\n                <h4>Contract:</h4>\\n                <p>${Contract}</p>\\n                <h4>Invoice Date:</h4>\\n                <p>${Invoice_Date}</p>\\n                <h4>Payment Due Date:</h4>\\n                <p>${Payment_Due_Date}</p>\\n                <h4>Purchase Order:</h4>\\n                <p>${Purchase_Order}</p>\\n            </div>\\n            <div class=\\\"fl-left invoice-info-group\\\">\\n                <h4>ID:</h4>\\n                <p>${id}</p>\\n                <h4>Title:</h4>\\n                <p>${Title}</p>\\n                <h4>Invoice Currency:</h4>\\n                <p>${Currency}</p>\\n                <h4>Invoice Amount:</h4>\\n                <p>${Invoice_Amount}</p>\\n            </div>\\n        </div>\\n    </div>\\n    <hr style=\\\"color: lightgrey\\\"/>\\n    <br/>\\n    <div class=\\\"invoice-line-items\\\">\\n        <h6><b>Line Item Table</b></h6>\\n        <br/>\\n        <div class=\\\"invoice-totals-row\\\">\\n            <div class=\\\"fl-left-total\\\">Base Charges</div>\\n            <div class=\\\"fl-right-total\\\"><b>TOTAL ${Amount_Approved}</b></div>\\n        </div>\\n        <div class=\\\"invoice-item-list content-block\\\">\\n            <table class=\\\"table invoice-table\\\">\\n                <thead class=\\\"thead\\\">\\n                <tr>\\n                    <th>S.No.</th>\\n                    <th>Line Item ID</th>\\n                    <th>Service End Date</th>\\n                    <th>Conversion Rate</th>\\n                    <th>Rate</th>\\n                    <th>Unit</th>\\n                    <th>Qty</th>\\n                    <th>Amount</th>\\n                    <th>Tax</th>\\n                    <th>Total</th>\\n                </tr>\\n                </thead>\\n                <tbody class=\\\"invoice-items\\\">\\n                <#assign count = 1>\\n                <#list items as item>\\n                    <tr class=\\\"item-row\\\">\\n                        <td>${count}</td>\\n                        <td>${item.id}</td>\\n                        <td>${item.Service_End_Date}</td>\\n                        <td>${item.System_Conversion_Rate}</td>\\n                        <td>${item.Supplier_Rate}</td>\\n                        <td>${item.Supplier_Unit}</td>\\n                        <td>${item.Supplier_Quantity}</td>\\n                        <td>${item.Supplier_Amount}</td>\\n                        <td>${item.Supplier_Tax}</td>\\n                        <td>${item.Supplier_Total}</td>\\n                    </tr>\\n                    <#assign count++>\\n                </#list>\\n                </tbody>\\n            </table>\\n        </div>\\n    </div>\\n</div>\\n</body>\\n</html>\",\n" +
                "\t\"headerFtl\" : \"<div class=\\\"pdf-header\\\"></div>\",\n" +
                "\t\"footerFtl\" : \"<div class=\\\"pdf-footer\\\"></div>\",\n" +
                "\t\"css\" : \"*{\\n    box-sizing: border-box;\\n    margin: 0;\\n    padding: 0;\\n}\\nbody{\\n    max-width: 820px;\\n    margin: 0 auto;\\n    font: normal 13px/1.4em 'Open Sans', Sans-serif;\\n}\\nbody:after{\\n    content: \\\"\\\";\\n    clear: both;\\n    display: block;\\n}\\n\\n.pdf-header, .pdf-container, .pdf-footer{\\n    position: relative;\\n    float: left;\\n    width: 100%;\\n}\\n.pdf-header, .pdf-footer{\\n    background-color: #007acc;\\n    width: 100%;\\n}\\n.pdf-header{\\n    top: 0px;\\n    height: 50px;\\n}\\n.pdf-footer{\\n    bottom: 0px;\\n    height: 20px;\\n}\\n.fl-left{\\n    float: left;\\n}\\n.fl-right{\\n    float: right;\\n}\\n.clearfix{\\n    display: block;\\n    clear: both;\\n}\\n.pdf-container{\\n    height: calc(100% - 10px);\\n    width: 100%;\\n    max-width: 820px;\\n    margin: 0 auto;\\n    padding: 0 10px;\\n    margin: 0 auto;\\n}\\n.pdf-container >div{\\n    float: left;\\n    width: 100%;\\n}\\n.invoice-header {\\n    border-bottom: 1px solid;\\n    border-bottom-color: blue;\\n    height: 270px;\\n}\\n.fl-left-header{\\n    padding-left: 20px;\\n    padding-top: 20px;\\n    padding-right: 20px;\\n    width: 380px;\\n    float: left;\\n    height: inherit;\\n    background-color: aliceblue;\\n}\\n.fl-right-header{\\n    padding-left: 20px;\\n    padding-top: 20px;\\n    padding-right: 20px;\\n    width: 320px;\\n    float: right;\\n    height: inherit;\\n}\\n.invoice-details {\\n    padding-left: 20px;\\n}\\n.invoice-details > div {\\n    margin: 4px 0;\\n}\\n.invoice-info-group {\\n    width: 130px;\\n    text-align: right;\\n}\\n.invoice-info-group p {\\n    margin-bottom: 10px;\\n}\\n.company-logo {\\n    width: 140px;\\n    height: 75px;\\n    horiz-align: left;\\n}\\n.company-logo img {\\n    width: 140px;\\n    height: 75px;\\n    max-width: 100%;\\n    vertical-align: center;\\n    horiz-align: left;\\n    transform: translateY(-50%);\\n    position: relative;\\n    top: 50%;\\n}\\nh4{\\n    font-weight: bold;\\n}\\nh5{\\n    font-size: 13px;\\n}\\n.normal{\\n    font-weight: normal;\\n}\\n.bold{\\n    font-weight: bold;\\n}\\n.smallest{\\n    font-size: 12px;\\n    line-height: 1.2;\\n}\\n.cl-blue{\\n    color: #007acc;\\n}\\np{\\n    font-size: inherit;\\n    line-height: inherit;\\n}\\n.invoice-line-items {\\n    margin-top: 24px;\\n    width: inherit;\\n}\\n.invoice-totals-row {\\n    width: inherit;\\n    background-color: aliceblue;\\n    padding: 4px 4px;\\n    border-radius: 2px;\\n}\\n.fl-left-total {\\n    width: 300px;\\n    float: left;\\n    text-align: left;\\n}\\n.fl-right-total {\\n    width: 300px;\\n    float: right;\\n    text-align: right;\\n}\\ntable th, table td {\\n    vertical-align: top;\\n    font-size: 11px;\\n    text-align: center;\\n    min-width: 50px;\\n    padding: 8px 0;\\n}\\ntable{\\n    margin-bottom: 15px;\\n    border-collapse: collapse;\\n    width: 100%;\\n}\\nth {\\n    border-top: 1px solid #252525;\\n    border-bottom: 1px solid #252525;\\n    font-weight: normal;\\n}\\ntbody tr{\\n    padding: 4px 0;\\n}\",\n" +
                "\t\"headerRequired\" : true,\n" +
                "\t\"footerRequired\" : true,\n" +
                "\t\"pageNoRequired\" : true,\n" +
                "\t\"headerHeight\" : 40,\n" +
                "\t\"footerHeight\" : 20\n" +
                "}";

        return payload;
    }

    private String createPayloadForFormatUpdate(String formatId,String formatName){

        String payload = "{\n" +
                "\t\"id\" : \"" + formatId + "\",\n" +
                "\t\"name\" : \"" + formatName + "\",\n" +
                "\t\"clientId\" : " + clientId + ",\n" +
                "\t\"bodyFtl\" : \"<!DOCTYPE html>\\n<html lang=\\\"en\\\">\\n<head>\\n    <meta content=\\\"text/html; charset=UTF-8\\\" http-equiv=\\\"Content-Type\\\"/>\\n    <meta content=\\\"IE=Edge\\\" http-equiv=\\\"X-UA-Compatible\\\"/>\\n    <meta name=\\\"robots\\\" content=\\\"noindex\\\" />\\n    <meta name=\\\"viewport\\\" content=\\\"width=device-width, initial-scale=1.0, user-scalable=no\\\"/>\\n    <title>Invoice Copy PDF</title>\\n    <link rel=\\\"stylesheet\\\" type=\\\"text/css\\\" href=\\\"invoice_copy.css\\\"/>\\n</head>\\n<body>\\n<div class=\\\"pdf-container\\\">\\n    <div class=\\\"invoice-header\\\">\\n        <div class=\\\"fl-left-header\\\">\\n            <h3 class=\\\"invoice-details\\\">INVOICE</h3>\\n            <div class=\\\"company-logo\\\">\\n                <img src=\\\"${logo}\\\" alt=\\\"Logo\\\"/>\\n            </div>\\n            <div class=\\\"invoice-details\\\">\\n                <h4 class=\\\"normal\\\">${Supplier}</h4>\\n                <div class=\\\"smallest\\\">\\n                    <p>Phone no : ${Phone_Number}</p>\\n                    <p class=\\\"cl-blue\\\">Email address : ${Email_Address}</p>\\n                </div>\\n                <h4 class=\\\"bold\\\">Bill to Address</h4>\\n                <p>Value</p>\\n            </div>\\n        </div>\\n        <div class=\\\"fl-right-header\\\">\\n            <div class=\\\"fl-left invoice-info-group\\\">\\n                <h4>Contract:</h4>\\n                <p>${Contract}</p>\\n                <h4>Invoice Date:</h4>\\n                <p>${Invoice_Date}</p>\\n                <h4>Payment Due Date:</h4>\\n                <p>${Payment_Due_Date}</p>\\n                <h4>Purchase Order:</h4>\\n                <p>${Purchase_Order}</p>\\n            </div>\\n            <div class=\\\"fl-left invoice-info-group\\\">\\n                <h4>ID:</h4>\\n                <p>${id}</p>\\n                <h4>Title:</h4>\\n                <p>${Title}</p>\\n                <h4>Invoice Currency:</h4>\\n                <p>${Currency}</p>\\n                <h4>Invoice Amount:</h4>\\n                <p>${Invoice_Amount}</p>\\n            </div>\\n        </div>\\n    </div>\\n    <hr style=\\\"color: lightgrey\\\"/>\\n    <br/>\\n    <div class=\\\"invoice-line-items\\\">\\n        <h6><b>Line Item Table</b></h6>\\n        <br/>\\n        <div class=\\\"invoice-totals-row\\\">\\n            <div class=\\\"fl-left-total\\\">Base Charges</div>\\n            <div class=\\\"fl-right-total\\\"><b>TOTAL ${Amount_Approved}</b></div>\\n        </div>\\n        <div class=\\\"invoice-item-list content-block\\\">\\n            <table class=\\\"table invoice-table\\\">\\n                <thead class=\\\"thead\\\">\\n                <tr>\\n                    <th>S.No.</th>\\n                    <th>Line Item ID</th>\\n                    <th>Service End Date</th>\\n                    <th>Conversion Rate</th>\\n                    <th>Rate</th>\\n                    <th>Unit</th>\\n                    <th>Qty</th>\\n                    <th>Amount</th>\\n                    <th>Tax</th>\\n                    <th>Total</th>\\n                </tr>\\n                </thead>\\n                <tbody class=\\\"invoice-items\\\">\\n                <#assign count = 1>\\n                <#list items as item>\\n                    <tr class=\\\"item-row\\\">\\n                        <td>${count}</td>\\n                        <td>${item.id}</td>\\n                        <td>${item.Service_End_Date}</td>\\n                        <td>${item.System_Conversion_Rate}</td>\\n                        <td>${item.Supplier_Rate}</td>\\n                        <td>${item.Supplier_Unit}</td>\\n                        <td>${item.Supplier_Quantity}</td>\\n                        <td>${item.Supplier_Amount}</td>\\n                        <td>${item.Supplier_Tax}</td>\\n                        <td>${item.Supplier_Total}</td>\\n                    </tr>\\n                    <#assign count++>\\n                </#list>\\n                </tbody>\\n            </table>\\n        </div>\\n    </div>\\n</div>\\n</body>\\n</html>\",\n" +
                "\t\"headerFtl\" : \"<div class=\\\"pdf-header\\\"></div>\",\n" +
                "\t\"footerFtl\" : \"<div class=\\\"pdf-footer\\\"></div>\",\n" +
                "\t\"css\" : \"*{\\n    box-sizing: border-box;\\n    margin: 0;\\n    padding: 0;\\n}\\nbody{\\n    max-width: 820px;\\n    margin: 0 auto;\\n    font: normal 13px/1.4em 'Open Sans', Sans-serif;\\n}\\nbody:after{\\n    content: \\\"\\\";\\n    clear: both;\\n    display: block;\\n}\\n\\n.pdf-header, .pdf-container, .pdf-footer{\\n    position: relative;\\n    float: left;\\n    width: 100%;\\n}\\n.pdf-header, .pdf-footer{\\n    background-color: #007acc;\\n    width: 100%;\\n}\\n.pdf-header{\\n    top: 0px;\\n    height: 50px;\\n}\\n.pdf-footer{\\n    bottom: 0px;\\n    height: 20px;\\n}\\n.fl-left{\\n    float: left;\\n}\\n.fl-right{\\n    float: right;\\n}\\n.clearfix{\\n    display: block;\\n    clear: both;\\n}\\n.pdf-container{\\n    height: calc(100% - 10px);\\n    width: 100%;\\n    max-width: 820px;\\n    margin: 0 auto;\\n    padding: 0 10px;\\n    margin: 0 auto;\\n}\\n.pdf-container >div{\\n    float: left;\\n    width: 100%;\\n}\\n.invoice-header {\\n    border-bottom: 1px solid;\\n    border-bottom-color: blue;\\n    height: 270px;\\n}\\n.fl-left-header{\\n    padding-left: 20px;\\n    padding-top: 20px;\\n    padding-right: 20px;\\n    width: 380px;\\n    float: left;\\n    height: inherit;\\n    background-color: aliceblue;\\n}\\n.fl-right-header{\\n    padding-left: 20px;\\n    padding-top: 20px;\\n    padding-right: 20px;\\n    width: 320px;\\n    float: right;\\n    height: inherit;\\n}\\n.invoice-details {\\n    padding-left: 20px;\\n}\\n.invoice-details > div {\\n    margin: 4px 0;\\n}\\n.invoice-info-group {\\n    width: 130px;\\n    text-align: right;\\n}\\n.invoice-info-group p {\\n    margin-bottom: 10px;\\n}\\n.company-logo {\\n    width: 140px;\\n    height: 75px;\\n    horiz-align: left;\\n}\\n.company-logo img {\\n    width: 140px;\\n    height: 75px;\\n    max-width: 100%;\\n    vertical-align: center;\\n    horiz-align: left;\\n    transform: translateY(-50%);\\n    position: relative;\\n    top: 50%;\\n}\\nh4{\\n    font-weight: bold;\\n}\\nh5{\\n    font-size: 13px;\\n}\\n.normal{\\n    font-weight: normal;\\n}\\n.bold{\\n    font-weight: bold;\\n}\\n.smallest{\\n    font-size: 12px;\\n    line-height: 1.2;\\n}\\n.cl-blue{\\n    color: #007acc;\\n}\\np{\\n    font-size: inherit;\\n    line-height: inherit;\\n}\\n.invoice-line-items {\\n    margin-top: 24px;\\n    width: inherit;\\n}\\n.invoice-totals-row {\\n    width: inherit;\\n    background-color: aliceblue;\\n    padding: 4px 4px;\\n    border-radius: 2px;\\n}\\n.fl-left-total {\\n    width: 300px;\\n    float: left;\\n    text-align: left;\\n}\\n.fl-right-total {\\n    width: 300px;\\n    float: right;\\n    text-align: right;\\n}\\ntable th, table td {\\n    vertical-align: top;\\n    font-size: 11px;\\n    text-align: center;\\n    min-width: 50px;\\n    padding: 8px 0;\\n}\\ntable{\\n    margin-bottom: 15px;\\n    border-collapse: collapse;\\n    width: 100%;\\n}\\nth {\\n    border-top: 1px solid #252525;\\n    border-bottom: 1px solid #252525;\\n    font-weight: normal;\\n}\\ntbody tr{\\n    padding: 4px 0;\\n}\",\n" +
                "\t\"headerRequired\" : true,\n" +
                "\t\"footerRequired\" : true,\n" +
                "\t\"pageNoRequired\" : true\n" +
                "\t\"headerHeight\" : NumberLong(40)\n" +
                "\t\"footerHeight\" : NumberLong(20)\n" +
                "\t\"active\" : true\n" +
                "\t\"isDefault\" : true\n" +
                "}";

        return payload;
    }

    private Boolean validateDelete(String formatId,CustomAssert customAssert){

        Boolean validationStatusDelete = true;

        Format format = new Format();
        try {
            String deleteResponse = format.deleteFormat(formatId);

            if (!deleteResponse.contains("Template format deleted successfully")) {
                customAssert.assertTrue(false, "Delete Format not happened successfully or message has been changed");
                validationStatusDelete = false;
            }

            String formatIdList = format.getFormatList(String.valueOf(clientId));

            if (!APIUtils.validJsonResponse(formatIdList)) {
                customAssert.assertTrue(false, "Format Id list is not a valid json response");
            } else {
                String id;
                JSONArray formatIdListJsonArray = new JSONArray(formatIdList);

                for (int i = 0; i < formatIdListJsonArray.length(); i++) {
                    id = formatIdListJsonArray.getJSONObject(i).get("id").toString();

                    if (id.equals(formatId)) {
                        customAssert.assertTrue(false, "Format ID value found in the list even after deleting it");
                        validationStatusDelete = false;
                    }
                }

            }
        }catch (Exception e){
            logger.error("Exception while deleting Format ID");
            customAssert.assertTrue(false, "Exception while deleting Format ID");
        }
        return validationStatusDelete;
    }

    private Boolean validateUpdateFormatId(String formatId,String updateNameOfFormatId,CustomAssert customAssert){

        Boolean updationStatus = true;

        Format format = new Format();

        try {
            String updatePayload = createPayloadForFormatUpdate(formatId, updateNameOfFormatId);

            String updateResponse = format.updateFormat(updatePayload);

            if (!JSONUtility.validjson(updateResponse)) {
                customAssert.assertTrue(false, "Update Response is an invalid json");
                updationStatus = false;
            }

            String formatResponseOfPartFormatId = format.getFormat(formatId);
            if (!APIUtils.validJsonResponse(formatResponseOfPartFormatId)) {
                customAssert.assertTrue(false, "Format Response Of Particular Format Id is an invalid json");
                updationStatus = false;
            } else {
                JSONArray formatResponseArray = new JSONArray(formatResponseOfPartFormatId);

                String formatIdOFParticularFormatResponse = formatResponseArray.getJSONObject(0).get("id").toString();
                String formatNameOFParticularFormatResponse = formatResponseArray.getJSONObject(0).get("name").toString();

                if (!formatIdOFParticularFormatResponse.equals(formatId)) {
                    customAssert.assertTrue(false, "Expected Format id not found from format response");
                    updationStatus = false;

                }

                if (!formatNameOFParticularFormatResponse.equals(updateNameOfFormatId)) {
                    customAssert.assertTrue(false, "Expected Format name not found from format response");
                    updationStatus = false;
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false, "Exception while updating Format ID");
            logger.error("Exception while updating Format ID");
        }
        return updationStatus;
    }

    private Boolean validateUpdateTemplateId(String templateId,String updateNameOfTemplateId,CustomAssert customAssert){

        Boolean updationStatus = true;

//        Format format = new Format();
        Template template = new Template();
        try {

            String templateResponse = template.getTemplate(templateId);
            String formatId = null;

            if(!APIUtils.validJsonResponse(templateResponse)){
                customAssert.assertTrue(false,"Template Response is an invalid json for a particular template");
            }else {
                JSONArray templateResponseJsonArray = new JSONArray(templateResponse);
                formatId = templateResponseJsonArray.getJSONObject(0).get("templateFormatId").toString();


            }

            String updatePayload = createPayloadForTemplateUpdate(updateNameOfTemplateId,templateId,formatId);

            String updateResponse = template.updateTemplate(updatePayload);

            if (!JSONUtility.validjson(updateResponse)) {
                customAssert.assertTrue(false, "Update Response is an invalid json");
                updationStatus = false;
            }

            String templateResponseOfPartTemplateId = template.getTemplate(templateId);

            if (!APIUtils.validJsonResponse(templateResponseOfPartTemplateId)) {
                customAssert.assertTrue(false, "Format Response Of Particular Format Id is an invalid json");
                updationStatus = false;
            } else {
                JSONArray formatResponseArray = new JSONArray(templateResponseOfPartTemplateId);

                String templateIdOFParticularTemplateResponse = formatResponseArray.getJSONObject(0).get("id").toString();
                String templateNameOFParticularTemplateResponse = formatResponseArray.getJSONObject(0).get("templateName").toString();

                if (!templateIdOFParticularTemplateResponse.equals(templateId)) {
                    customAssert.assertTrue(false, "Expected Template id not found from template response");
                    updationStatus = false;

                }

                if (!templateNameOFParticularTemplateResponse.equals(updateNameOfTemplateId)) {
                    customAssert.assertTrue(false, "Expected Template name not found from template response");
                    updationStatus = false;
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false, "Exception while updating Template ID");
            logger.error("Exception while updating Template ID");
        }
        return updationStatus;
    }

    private Boolean validateDeleteTemplate(String templateId,CustomAssert customAssert){

        Boolean validationStatusDelete = true;


        Template template = new Template();
        try {
            String deleteResponse = template.deleteTemplate(templateId);

            if (!deleteResponse.contains("Template deleted successfully")) {
                customAssert.assertTrue(false, "Delete Format not happened successfully or message has been changed");
                validationStatusDelete = false;
            }

            String listId = "518";
            String payload = "{\"filterMap\":{}}";
            String templateIdList = template.getTemplateList(listId,payload);

            if (!APIUtils.validJsonResponse(templateIdList)) {
                customAssert.assertTrue(false, "Template Id list is not a valid json response");
            } else {

                JSONObject templateIdListJson = new JSONObject(templateIdList);

                JSONArray dataArray = templateIdListJson.getJSONArray("data");
                JSONObject indvJson;

                String columnName;

                String idColumnValue = "";
                String aciveColumnValue = "true";

                for(int i=0;i<dataArray.length();i++){

                    indvJson= dataArray.getJSONObject(i);

                    Iterator itr = indvJson.keys();

                    while (itr.hasNext()) {
                        columnName  = indvJson.getJSONObject(itr.next().toString()).get("columnName").toString();


                        if(columnName.equals("id")){
                            idColumnValue = indvJson.getJSONObject(itr.next().toString()).get("value").toString();
                        }

                        if(columnName.equals("active")){
                            aciveColumnValue = indvJson.getJSONObject(itr.next().toString()).get("value").toString();
                        }

                    }
                    if(idColumnValue.equals(templateId) && !aciveColumnValue.equals("false")){
                        customAssert.assertTrue(false,"Since template id is deleted it should have active value as false");
                        break;
                    }else if(idColumnValue.equals(templateId) && aciveColumnValue.equals("false")){
                        logger.info("Since template id is deleted it has active value as false");
                        break;
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception while deleting Format ID");
            customAssert.assertTrue(false, "Exception while deleting Format ID");
        }
        return validationStatusDelete;
    }

    private String validateCreateFormatId(CustomAssert customAssert){

        Format format = new Format();
        String formatId = null;

        String payload = createPayloadForFormatCreate();
        String createFormatResponse = format.createFormat(payload);

        try {
            if (!JSONUtility.validjson(createFormatResponse)) {

                customAssert.assertTrue(false, "Create Format Response is an invalid Json");
            } else {
                JSONObject createFormatResponseJson = new JSONObject(createFormatResponse);
                formatId = createFormatResponseJson.get("id").toString();
            }
        }catch (Exception e){
            logger.error("Exception while validating creation of format");
        }

        return formatId;
    }

    private String validateCreateTemplateId(CustomAssert customAssert){

        Template template = new Template();

        String templateId = null;

        String formatId = validateCreateFormatId(customAssert);

        String payload = createPayloadForTemplateCreate(formatId);

        String createTemplateResponse = template.createTemplate(payload);

        try {
            if (!JSONUtility.validjson(createTemplateResponse)) {

                customAssert.assertTrue(false, "Create Template Response is an invalid Json");
            } else {
                JSONObject createFormatResponseJson = new JSONObject(createTemplateResponse);
                templateId = createFormatResponseJson.get("id").toString();
            }
        }catch (Exception e){
            logger.error("Exception while validating creation of template");
        }

        return templateId;
    }

    private String createPayloadForTemplateCreate(String formatId){

        String createPayloadForTemplateCreate = "{\n" +
                "    \"templateName\": \"Custom Template 10 June\",\n" +
//                "    \"supplierId\": 1188,\n" +
                "    \"clientId\": "  + clientId + ",\n" +
                "    \"defaultTemplate\": false,\n" +
                "    \"active\": true,\n" +
                "    \"templateFormatId\" : \"" + formatId + "\"\n" +
                "}";

        return createPayloadForTemplateCreate;
    }

    private String createPayloadForTemplateUpdate(String templateName,String templateId,String formatId){

        String payloadForTemplateUpdate = "{\n" +
                "\t\"id\": \"" + templateId + "\",\n" +
                "    \"templateName\": \"" + templateName + "\",\n" +
                "    \"clientId\": " + clientId + ",\n" +
//                "    \"supplierId\": 1188,\n" +
                "    \"defaultTemplate\": false,\n" +
                "    \"active\": true,\n" +
                "    \"templateFormatId\" : \"" + formatId + "\"\n" +
                "}";

        return payloadForTemplateUpdate;
    }

    private Boolean validateUploadTemplateLogo(String templateId,String filePath,String fieName,CustomAssert customAssert){

        Boolean validationStatus = true;

        UploadInvoiceCopy uploadInvoiceCopy = new UploadInvoiceCopy();
        try{
            Map<String,File> payloadMap = new HashMap<String,File>();
            payloadMap.put("logo",new File(filePath,fieName));

            uploadInvoiceCopy.hitUploadData(templateId,filePath,fieName,payloadMap);
            String uploadFileJsonStr = uploadInvoiceCopy.uploadFileJsonStr;

            if(uploadFileJsonStr == null){
                customAssert.assertTrue(false,"Upload File Response is null");
            } else if(!uploadFileJsonStr.contains("File Uploaded Successfully")){
                customAssert.assertTrue(false,"Logo Uploaded UnSuccessfully on template");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Upload Template Logo " + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;
    }

    private Boolean validateDownloadTemplateLogo(String templateId,String filePath,String fieName,CustomAssert customAssert){

        Boolean validationStatus = false;
        UploadInvoiceCopy uploadInvoiceCopy = new UploadInvoiceCopy();
        try {

            validationStatus = uploadInvoiceCopy.hitDownload(filePath,fieName,templateId);

            if(!validationStatus){
                customAssert.assertTrue(false,"Logo downloaded unsuccessfully");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating download Template Logo " + e.getStackTrace());
            validationStatus = false;
        }
        return validationStatus;
    }

    public Boolean generateInvoice(int invoiceId,CustomAssert customAssert){

        Boolean invGenerationStatus = true;
        Copy copy = new Copy();
        try{

            int responseCode = copy.generateCopy(invoiceId).getResponseCode();

            if(responseCode != 200){
                customAssert.assertTrue(false,"Error while generating invoice copy");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while generating invoice");
        }

        return invGenerationStatus;
    }

    private String getInvoiceCopyViewerLink(int invoiceId,CustomAssert customAssert){

        InvoiceCopyViewer invoiceCopyViewer = new InvoiceCopyViewer();

        String documentUrl = null;
        try {
            String response = invoiceCopyViewer.getInvoiceCopyViewerLink(invoiceId).getResponse().getResponseBody();

            if(response == null){
                customAssert.assertTrue(false,"invoice Copy Viewer link response is null");
            }else {
                if(!JSONUtility.validjson(response)){
                    customAssert.assertTrue(false,"Invoice copy viewer link response is not a valid json");
                }else {

                    JSONObject responseJson = new JSONObject(response);

                    documentUrl = responseJson.getJSONObject("data").get("documentURL").toString();

                    int responseCode = DocumentViewerShow.getDocumentViewerResponse(documentUrl).getResponse().getResponseCode();

                    if(responseCode != 200){
                        customAssert.assertTrue(false,"Document viewer stream response code is not equal to 200");
                    }
                }
            }


        }catch (Exception e){
            logger.error("Exception while getting invoice copy viewer link");
        }

        return documentUrl;
    }

    private Boolean validateCommunicationLink(int invoiceId,String expDocumentId,
                                              StringBuilder fileId,CustomAssert customAssert){

        Boolean validationStatus = true;
        TabListData tabListData = new TabListData();
        Show show = new Show();

        int tabId = 65;
        try {
            show.hitShowVersion2(invoiceEntityTypeId,invoiceId);
            String showResponse = show.getShowJsonStr();

            String invShortCodeId = ShowHelper.getValueOfField("short code id",showResponse);

            tabListData.hitTabListData(tabId, invoiceEntityTypeId, invoiceId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (!JSONUtility.validjson(tabListResponse)) {
                customAssert.assertTrue(false, "Communication Link Response is not a valid json");
            }else {
                JSONObject tabListResponseJson = new JSONObject(tabListResponse);

                JSONObject firstComment = tabListResponseJson.getJSONArray("data").getJSONObject(0);

                Iterator<String> keys = firstComment.keys();
                String columnName;
                String columnValue;
                String key;
                while (keys.hasNext()){
                    key = keys.next();
                    columnName = firstComment.getJSONObject(key).get("columnName").toString();
                    columnValue = firstComment.getJSONObject(key).get("value").toString();

                    if(columnName.equals("document")){

                        String[] columnValuesArray =  columnValue.split(":;");
                        if(columnValuesArray.length != 5){
                            customAssert.assertTrue(false,"Communication Link Response should have 5 Document Details in column name document");
                            validationStatus = false;
                        }
                        for(int i =0;i<columnValuesArray.length;i++) {
                            if(i ==0) {
                                String actualDocumentId = columnValuesArray[i];

                                if(!expDocumentId.equals(actualDocumentId)){
                                    customAssert.assertTrue(false,"Expected and Actual Value Of Document Id Mismatched on Communication Link Tab ");
                                    validationStatus = false;
                                }

                            }else if(i == 1) {
                                String invShortCodeIdActual = columnValuesArray[1];
                                if(!invShortCodeIdActual.equals(invShortCodeId)){
                                    customAssert.assertTrue(false,"Expected and Actual Value OF Invoice Mismatched on Communication Link Tab ");
                                    validationStatus = false;
                                }

                            }else if(i == 2) {
                                String documentType = columnValuesArray[2];
                                if(!documentType.equals("pdf")){
                                    customAssert.assertTrue(false,"Document Type Expected as PDF on Communication Link Tab ");
                                    validationStatus = false;
                                }

                            }else if(i == 3) {
                                String entityTypeId = columnValuesArray[3];

                                if(!entityTypeId.equals(String.valueOf(invoiceEntityTypeId))){
                                    customAssert.assertTrue(false,"Expected and Actual Value OF Entity Type Id Mismatched on Communication Link Tab ");
                                    validationStatus = false;
                                }
                            }else if(i == 4) {
                                StringBuilder stringBuilder = new StringBuilder(columnValuesArray[4]);
                                fileId.append(stringBuilder.toString());

                            }

                        }
                    }else if(columnName.equals("comment")){
                        if(!columnValue.equals("Invoice Copy Generated")) {
                            if (!columnValue.equals("Invoice Copy Generated (Bulk)")) {
                                customAssert.assertTrue(false, "Expected comment on Communication Link Tab : Invoice Copy Generated, Actual :" + columnValue);
                                validationStatus = false;
                            }
                        }
                    }else if(columnName.equals("completed_by")){
                        if(!columnValue.equals(userName)){
                            customAssert.assertTrue(false,"Expected completed By on Communication Link Tab " + userName + "Actual :" + columnValue);
                            validationStatus = false;
                        }
                    }else if(columnName.equals("invoicecopy")){
                        if(!columnValue.equals("true")){
                            customAssert.assertTrue(false,"Expected Invoice Copy on Communication Link Tab " + "true" + "Actual :" + columnValue);
                            validationStatus = false;
                        }
                    }
                }

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating communication Link");
            validationStatus = false;
        }

        return validationStatus;
    }

    private Boolean downloadInvoiceCopy(String outputFilePath,String outputFileName,String documentId,String fileId,CustomAssert customAssert){

        Boolean downloadStatus = true;
        Download download = new Download();

        try{
            String queryString = "?id=" + documentId + "&entityTypeId=" + 78 + "&entityType.id=67&fileId=" + fileId + "";
            downloadStatus = download.downloadCommDocument(outputFilePath,outputFileName,queryString);

            if(!downloadStatus){
                customAssert.assertTrue(false,"Error while downloading invoice copy");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while downloading invoice copy");
        }

        return downloadStatus;
    }

    private Boolean deleteAttachment(int invoiceId,String auditLogId,CustomAssert customAssert){

        Boolean deleteStatus = true;
        CommentAttachmentDelete commentAttachmentDelete = new CommentAttachmentDelete();
        try{
            String queryString = "?isInvoiceCopy=%20true";
            String responseBody = commentAttachmentDelete.getResponse(String.valueOf(invoiceEntityTypeId),String.valueOf(invoiceId),auditLogId,queryString).getResponseBody();

            if(!responseBody.contains("Comment/Attachment deleted successfully")){
                customAssert.assertTrue(false,"Invoice Copy Attachment not deleted successfully");
                deleteStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while deleting invoice copy attachment");
            deleteStatus = false;
        }

        return deleteStatus;
    }

    private String getAuditLogId(int invoiceId,CustomAssert customAssert){

        String auditLogId = null;

        TabListData tabListData = new TabListData();
        try {
            tabListData.hitTabListData(61, invoiceEntityTypeId, invoiceId);
            String tabListDataResponseStrResponse = tabListData.getTabListDataResponseStr();
            int length = new JSONObject(tabListDataResponseStrResponse).getJSONArray("data").length();
            JSONObject jsonObject = new JSONObject(tabListDataResponseStrResponse).getJSONArray("data").getJSONObject(length - 1);
            JSONArray jsonObjectName = jsonObject.names();
            for (int i = 0; i < jsonObjectName.length(); i++) {
                if (jsonObject.getJSONObject(jsonObjectName.getString(i)).getString("columnName").equalsIgnoreCase("history")) {
                    String[] value = jsonObject.getJSONObject(jsonObjectName.getString(i)).getString("value").split("/");
                    auditLogId = value[3];
                    break;
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting audit Log Id");
        }
        return auditLogId;
    }

    private String getAuditLogLatestAction(int invoiceId,CustomAssert customAssert){

        String latestAction = "";

        AuditLog auditLog = new AuditLog();

        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String auditLogResponse = auditLog.hitAuditLogDataApi(String.valueOf(invoiceEntityTypeId), String.valueOf(invoiceId),payload);

            JSONObject auditLogResponseJson = new JSONObject(auditLogResponse);
            JSONObject auditLogFirstRowJson = auditLogResponseJson.getJSONArray("data").getJSONObject(0);
            Iterator<String> keys = auditLogFirstRowJson.keys();

            while (keys.hasNext()){
                String key = keys.next();

                String columnName = auditLogFirstRowJson.getJSONObject(key).get("columnName").toString();

                if(columnName.equals("action_name")){
                    latestAction = auditLogFirstRowJson.getJSONObject(key).get("value").toString();
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting latest action from audit log tab");
        }
        return latestAction;
    }

    private Boolean validateLineItemListInGenPDF(String outputFile,ArrayList<String> expectedContentList,CustomAssert customAssert) {

        Boolean validationStatus = true;
        FileUtils fileUtils = new FileUtils();

        ArrayList<String> contentNotFoundInPdf = new ArrayList<>();
        try {
            ArrayList<String> pdfFileContent = fileUtils.getPDFFileContent(outputFile);

            //Validating if LineItems are present in the PDF
            outerLoop:
            for (int i = 0; i < expectedContentList.size() - 1; i++) {
                if (i > expectedContentList.size()) {
                    break;
                }
//                if(!expectedContentList.get(i).trim().contains(pdfFileContent)){
                if (!pdfFileContent.contains(expectedContentList.get(i).trim()))

                    contentNotFoundInPdf.add(expectedContentList.get(i) + " at line number " + i);
            }
            if (contentNotFoundInPdf.size() > 0) {
                customAssert.assertTrue(false, "Few Content not found in pdf " + contentNotFoundInPdf);
                validationStatus = false;
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating line Item ");
            validationStatus = false;
        }
        return validationStatus;
    }


    private ArrayList<String> createExpectedPDFList(int supplierId,int contractId,int invoiceId,int invoiceLineItemId){

        ArrayList<String> expectedPDFList = new ArrayList<>();

        try{

            String invShortCodeId = ShowHelper.getValueOfField(invoiceEntityTypeId,invoiceId,"short code id");
            String lineItemShortCodeId = ShowHelper.getValueOfField(lineItemEntityTypeId,invoiceLineItemId,"short code id");

            String invName = ShowHelper.getValueOfField(invoiceEntityTypeId,invoiceId,"name");
            String supplierName = ShowHelper.getValueOfField(supplierEntityTypeId,supplierId,"name");
            String contractName = ShowHelper.getValueOfField(contractEntityTypeId,contractId,"name");
            String invCurr = ShowHelper.getValueOfField(invoiceEntityTypeId,invoiceId,"currency");

            String invDate= "01 Apr 2018";
            String paymentDueDate= "20 Jul 2017";//01 Jun 2018";

            expectedPDFList.add("INVOICE");
            expectedPDFList.add(supplierName);
            expectedPDFList.add("Phone no :");
            expectedPDFList.add("Email address :");
            expectedPDFList.add("Bill to Address");
            expectedPDFList.add("Value");
            expectedPDFList.add("Contract:");
            expectedPDFList.add(contractName);
            expectedPDFList.add("Invoice Date:");
            expectedPDFList.add(invDate);
            expectedPDFList.add("Payment Due Date:");
            expectedPDFList.add(paymentDueDate);
            expectedPDFList.add("Purchase Order:");
            expectedPDFList.add("ID:");
            expectedPDFList.add(invShortCodeId);
            expectedPDFList.add("Title:");
//            expectedPDFList.add(invName.substring(0,9));
//            expectedPDFList.add(invName.substring(9));
            expectedPDFList.add(invName);

            expectedPDFList.add("Invoice Currency:");
            expectedPDFList.add(invCurr.substring(0,18));
            expectedPDFList.add(invCurr.substring(18));
            expectedPDFList.add("Invoice Amount:");
            expectedPDFList.add("Line Item Table");
            expectedPDFList.add("Base Charges TOTAL");
            expectedPDFList.add("S.No. Line Item ID Service End Date Conversion Rate Rate Unit Qty Amount Tax Total");
            expectedPDFList.add("1 " + lineItemShortCodeId +" 31 Mar 2018 10.00 Desktop 13.00 136.00 136.00");
            expectedPDFList.add("1");

        }catch (Exception e){
            logger.error("Exception while creating Expected PDF List " + e.getStackTrace());
        }

        return expectedPDFList;
    }

    private Boolean validateDeleteAttachment(int invoiceId,CustomAssert customAssert){

        Boolean validationStatus= true;
        try {
            String auditLogId = getAuditLogId(invoiceId, customAssert);
            if (auditLogId != null) {
                deleteAttachment(invoiceId, auditLogId, customAssert);

                String latestAction = getAuditLogLatestAction(invoiceId, customAssert);

                if (!latestAction.equals("Invoice Copy Deleted")) {
                    customAssert.assertTrue(false, "After Deletion of Invoice Copy Latest Action Expected in Audit Log : Invoice Copy Deleted Actual : " + latestAction);
                    validationStatus = false;
                }
            }
        }catch (Exception e){
            customAssert.assertTrue( false,"Exception while validating Invoice Copy Deletion Scenario");
            validationStatus = false;
        }

        return validationStatus;
    }

    private Boolean validateRegenerateOption(String entity,int invoiceId,int invoiceLineItemId,String outputFilePath,String outputFileName,CustomAssert customAssert){

        Boolean validationStatus = true;

        try {

            Edit edit = new Edit();
            String editPayload = edit.getEditPayload(entity,invoiceId);

            JSONObject editPayloadJson = new JSONObject(editPayload);

            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values", DateUtils.getCurrentTimeStamp());
            String editResponse = edit.hitEdit(entity,editPayloadJson.toString());
            if(editResponse.contains("success")) {
                String actionResponse = Actions.getActionsV3Response(invoiceEntityTypeId, invoiceId);

                JSONObject actionResponseJson = new JSONObject(actionResponse);
                JSONArray layoutActions = actionResponseJson.getJSONArray("layoutActions");
                Boolean regenerateButtonFound = false;
                String apiPath = "";

                for(int i=0;i<layoutActions.length();i++){

                    if(layoutActions.getJSONObject(i).get("name").toString().equals(regenerate)){

                        apiPath = layoutActions.getJSONObject(i).get("api").toString();
                        regenerateButtonFound = true;break;
                    }
                }
                if(!regenerateButtonFound){
                    customAssert.assertTrue(false,"After updation of invoice regenerate button not found");
                }else {

                    Actions.hitActionApiGet(apiPath);

                    String documentViewerLink =  getInvoiceCopyViewerLink(invoiceId,customAssert);

                    if(documentViewerLink !=null) {

                        String [] documentViewerLinkArray = documentViewerLink.split("/");
                        String documentId =  documentViewerLinkArray[documentViewerLinkArray.length -1];

                        StringBuilder fileId = new StringBuilder();
                        validateCommunicationLink(invoiceId,documentId,fileId,customAssert);

                        downloadInvoiceCopy(outputFilePath,outputFileName,documentId,fileId.toString(),customAssert);

                        ArrayList<String> expectedPDFList = createExpectedPDFList(supplierId,contractId,invoiceId,invoiceLineItemId);

                        String outputFile = outputFilePath + "\\" + outputFileName;
                        validateLineItemListInGenPDF(outputFile,expectedPDFList,customAssert);

                    }
                }

            }else {
                customAssert.assertTrue(false,"Edit done unsuccessfully on invoice to check regenerate option");
                validationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Regenerate Option");
            validationStatus = false;
        }

        return validationStatus;
    }

    private String getFileId(int invoiceId,CustomAssert customAssert){


        TabListData tabListData = new TabListData();

        String fileId = "";

        int tabId = 65;
        try {

            tabListData.hitTabListData(tabId, invoiceEntityTypeId, invoiceId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (!JSONUtility.validjson(tabListResponse)) {
                customAssert.assertTrue(false, "Communication Link Response is not a valid json");
            }else {
                JSONObject tabListResponseJson = new JSONObject(tabListResponse);

                JSONObject firstComment = tabListResponseJson.getJSONArray("data").getJSONObject(0);

                Iterator<String> keys = firstComment.keys();
                String columnName;
                String columnValue;
                String key;
                while (keys.hasNext()){
                    key = keys.next();
                    columnName = firstComment.getJSONObject(key).get("columnName").toString();
                    columnValue = firstComment.getJSONObject(key).get("value").toString();

                    if(columnName.equals("document")){

                        String[] columnValuesArray =  columnValue.split(":;");
                        fileId = columnValuesArray[4];
                        break;
                    }
                }

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting file Id");
        }

        return fileId;
    }

    private Boolean validateInvoiceCopyOnListingAndShowPage(int invoiceId,int filterId,int customFieldId,CustomAssert customAssert){

        Boolean validationStatus = true;
        try{
            InvoiceHelper invoiceHelper = new InvoiceHelper();

            int listId = 10;
            Edit edit = new Edit();
            String editPayload = edit.getEditPayload(invoices,invoiceId);

            JSONObject editPayloadJson = new JSONObject(editPayload);
            String customFieldValue = DateUtils.getCurrentTimeStamp().replace("_","");
            customFieldValue = customFieldValue.replace(" ","");

            Double customFieldValueDouble = Double.parseDouble(customFieldValue);
            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customFieldId).put("values",customFieldValueDouble);

            String editResponse = edit.hitEdit(invoices,editPayloadJson.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Error while editing custom field dyn" + customFieldId +  " on invoice");
                return false;
            }

            Show show = new Show();
            show.hitShowVersion2(invoiceEntityTypeId,invoiceId);
            String showResponse = show.getShowJsonStr();
            JSONObject showResponseJson  = new JSONObject(showResponse);

            String shortCodeId = ShowHelper.getValueOfField("short code id",showResponse);

            String startDate = ShowHelper.getValueOfField("invoiceperiodfromdatevalues",showResponse);
            String endDate = ShowHelper.getValueOfField("invoiceperiodtodatevalues",showResponse);


            String customField = "";
            try {
                customField= showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn"+customFieldId).get("values").toString();
            }catch (Exception e){
                logger.error("Error while getting custom field value");
            }


            HashMap<String,String> listValuesMap = invoiceHelper.getListingResponseInvoice(listId,startDate,endDate,filterId,customFieldId,customField,customAssert).get(0);

            if(listValuesMap.size() == 0){
                customAssert.assertTrue(false,"Listing response contains no data");
            }else {
                if(!listValuesMap.get("id").contains(String.valueOf(invoiceId))){
                    customAssert.assertTrue(false,"Listing Response does not contain the expected invoice id " + invoiceId);
                    validationStatus = false;
                }else {
                    if(!listValuesMap.get("invoicecopy").contains(shortCodeId + ".pdf")){
                        customAssert.assertTrue(false,"Expected name of invoice copy not present in the listing page");
                        validationStatus = false;
                    }
                }
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Invoice Copy On Listing And ShowPage " + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;
    }



}
