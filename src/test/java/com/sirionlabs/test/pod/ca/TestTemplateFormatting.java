package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.entityWorkflowActions.Publish;
import com.sirionlabs.api.file.FileUpload;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.ContractTemplate;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class TestTemplateFormatting {
    private final static Logger logger = LoggerFactory.getLogger(TestTemplateFormatting.class);

    private String configFilePath;
    private String configFileName;

    private String entityIdPath;
    private String entityIdName;
    private String extraFieldsConfigFileName;
    private Map<String, String> defaultProperties;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestTemplateFormattingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestTemplateFormattingFileName");
        defaultProperties = ParseConfigFile.getAllProperties(configFilePath, configFileName);

        entityIdPath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        entityIdName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        extraFieldsConfigFileName = defaultProperties.get("extrafieldsfilename");
    }

    @Test
    public void testC152938() {
        CustomAssert customAssert = new CustomAssert();
        String entityName = "contract templates";
        List<Integer> entityIds = new ArrayList<>();
        int entityIdToFormat = -1;
        int entityIdToUseForFormatting = -1;
        try {
            entityIdToFormat = createTemplate("template to format", entityName, customAssert);
            entityIdToUseForFormatting = createTemplate("template used for formatting", entityName, customAssert);
            if (entityIdToFormat != -1 & entityIdToUseForFormatting != -1) {
                entityIds.add(entityIdToFormat);
                entityIds.add(entityIdToUseForFormatting);

                //Download Template Used for Formatting.
                HashMap<String, String> templateDetails = downloadEntity(entityIdToUseForFormatting, entityName, customAssert);
                if (templateDetails.size() > 0) {
                    logger.info("Template has been downloaded.");
                    String path = templateDetails.get("Download Path");
                    String documentName = templateDetails.get("File Name");
                    String extension = templateDetails.get("File Extension");
                    //Upload it in the template to be formatted.
                    boolean uploadFlag = uploadTemplate(entityIdToFormat, entityName, path, documentName, extension, customAssert);
                    if (uploadFlag) {
                        logger.info("Template has been changed by uploading the formatted document.");
                        //Download Template after updating so that it can be compared with the other template.
                        String templateName = downloadEntity(entityIdToFormat, entityName, customAssert).get("File Name");
                        File documentFile = new File(path + "/" + documentName + "." + extension);
                        File templateFile = new File(path + "/" + templateName + "." + extension);
                        if (compareFiles(documentFile, templateFile, true, customAssert)) {
                            logger.info("Uploaded Document is visible on the Doc Viewer of the template");
                        } else {
                            logger.error("Other Document is visible on the Doc Viewer of the template");
                            customAssert.assertTrue(false, "Other Document is visible on the Doc Viewer of the template");
                        }
                    } else {
                        logger.error("Template could not be uploaded");
                        customAssert.assertTrue(false, "Template could not be uploaded");
                    }
                } else {
                    logger.error("Template could not be downloaded. Hence can't proceed.");
                    customAssert.assertTrue(false, "Template could not be downloaded. Hence can't proceed.");
                }
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while validating TC-C152938.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while validating TC-C152938.");
        } finally {
            for (int entityId : entityIds) {
                EntityOperationsHelper.deleteEntityRecord(entityName, entityId);
                logger.info("CT with entityId id {} is deleted.", entityId);
            }
        }
        customAssert.assertAll();
    }

    @Test
    public void testC152214() {
        CustomAssert customAssert = new CustomAssert();
        int templateId = -1;
        int docComparisonStatus = -1;
        String clauseText = ParseConfigFile.getValueFromConfigFile(configFilePath, extraFieldsConfigFileName, "template doc viewer", "clauses");
        int clauseId = Integer.parseInt(new JSONObject(clauseText).getJSONArray("values").getJSONObject(0).getJSONObject("clause").get("id").toString());
        //int clauseId = 11493;
        try {
            HashMap<String, Integer> docViewerDetails = verifyDocViewer(templateId, clauseId, customAssert);
            templateId = docViewerDetails.get("TemplateID");
            docComparisonStatus = docViewerDetails.get("TemplateDocViewer");
            if (templateId != -1 & docComparisonStatus == 1) {
                String originalClauseText = getClauseText(clauseId, customAssert);
                String timeStamp = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
                String replacementClauseText = originalClauseText + "\n" + timeStamp;
                if (editClause(clauseId, originalClauseText, replacementClauseText, customAssert)) {
                    //Verify Template Doc with edited clause
                    if (editTemplate(templateId, customAssert)) {
                        docViewerDetails = verifyDocViewer(templateId, clauseId, customAssert);
                        docComparisonStatus = docViewerDetails.get("TemplateDocViewer");
                        if (docComparisonStatus == 1) {
                            logger.info("Template Document matches with clause Document.");
                        } else {
                            logger.error("Template Document does not match with clause Document.");
                            customAssert.assertTrue(false, "Template Document does not match with clause Document.");
                        }
                    } else {
                        logger.error("Template could not be edited.");
                        customAssert.assertTrue(false, "Template could not be edited.");
                    }
                } else {
                    logger.error("Clause could not be edited.");
                    customAssert.assertTrue(false, "Clause could not be edited.");
                }
            } else {
                logger.error("Template Document could not be verified with the Clause before editing the clause.");
                customAssert.assertTrue(false, "Template Document could not be verified with the Clause before editing the clause.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while verifying TC-C152214.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while verifying TC-C152214.");
        } finally {
            EntityOperationsHelper.deleteEntityRecord("contract templates", templateId);
            logger.info("CT with entityId id {} is deleted.", templateId);
        }
        customAssert.assertAll();
    }

    @Test
    public void testC152726() {
        CustomAssert customAssert = new CustomAssert();
        String entityName = "contract templates";
        int templateId = -1;
        String clauseText = ParseConfigFile.getValueFromConfigFile(configFilePath, extraFieldsConfigFileName, "template with image in doc viewer", "clauses");
        int clauseId = Integer.parseInt(new JSONObject(clauseText).getJSONArray("values").getJSONObject(0).getJSONObject("clause").get("id").toString());
        //int clauseId = 11494;
        String sectionName = "template with image in doc viewer";
        try {
            String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, false);
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                if (new JSONObject(createResponse).getJSONObject("header").getJSONObject("response").getString("status").equals("success")) {
                    logger.info("Template creation is successful.");
                    templateId = CreateEntity.getNewEntityId(createResponse, entityName);
                    if (templateId != -1) {
                        HashMap<String, String> templateDetails = downloadEntity(templateId, entityName, customAssert);
                        String templatePath = templateDetails.get("Download Path");
                        String templateName = templateDetails.get("File Name");
                        String templateExtension = templateDetails.get("File Extension");
                        //Download clause
                        HashMap<String, String> clauseDetails = downloadEntity(clauseId, "clauses", customAssert);
                        String clausePath = clauseDetails.get("Download Path");
                        String clauseName = clauseDetails.get("File Name");
                        String clauseExtension = clauseDetails.get("File Extension");

                        //Compare clause with Template
                        File templateFile = new File(templatePath + "/" + templateName + "." + templateExtension);
                        File clauseFile = new File(clausePath + "/" + clauseName + "." + clauseExtension);

                        if (compareTemplateAndClauseWithImage(templateFile, clauseFile, customAssert)) {
                            logger.info("Template contains the picture of the clause.");
                        } else {
                            logger.error("Template does not contain the picture of the clause.");
                            customAssert.assertTrue(false, "Template does not contain the picture of the clause.");
                        }

                    } else {
                        logger.error("Template Id could not be fetched.");
                        customAssert.assertTrue(false, "Template Id could not be fetched.");
                    }
                } else {
                    logger.error("Template creation is unsuccessful.");
                    customAssert.assertTrue(false, "Template creation is unsuccessful.");
                }
            } else {
                logger.error("Template Create Response is not a valid JSON.");
                customAssert.assertTrue(false, "Template Create Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while verifying TC-C152726.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while verifying TC-C152726.");
        } finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, templateId);
            logger.info("CT with entityId id {} is deleted.", templateId);
        }
        customAssert.assertAll();
    }

    private int createTemplate(String sectionName, String entityName, CustomAssert customAssert) {
        int entityId = -1;
        try {
            String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, false);
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                if (createStatus.equalsIgnoreCase("success")) {
                    entityId = CreateEntity.getNewEntityId(createResponse, entityName);
                    if (entityId != -1) {
                        logger.info("Contract Template Created Successfully.");
                    }
                } else {
                    logger.error("Template creation is unsuccessful.");
                    customAssert.assertTrue(false, "Template creation is unsuccessful.");
                }
            } else {
                logger.error("Template Create Response is not a valid JSON.");
                customAssert.assertTrue(false, "Template Create Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while creating the template.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while creating the template.");
        }
        return entityId;
    }

    private HashMap<String, String> downloadEntity(int entityId, String entityName, CustomAssert customAssert) {
        HashMap<String, String> templateDetails = new HashMap<>();
        String folderName = null;
        Download download = new Download();
        Show show = new Show();
        String outputFilePath = System.getProperty("user.dir") + "/src/test/resources";
        int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdPath, entityIdName, entityName, "entity_type_id"));
        String urlName = ParseConfigFile.getValueFromConfigFile(entityIdPath, entityIdName, entityName, "url_name");
        show.hitShow(entityTypeId, entityId, true);
        String showResponse = show.getShowJsonStr();
        if (ParseJsonResponse.validJsonResponse(showResponse)) {
            String fileName = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
            String extension = "docx";
            String queryString = "/" + urlName + "/download/" + entityId + "?";
            folderName = urlName + "ForAutomation";
            String downloadPath = null;
            try {
                FileUtils fileUtil = new FileUtils();
                Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, folderName);
                if (isFolderSuccessfullyCreated || new File(outputFilePath + "/" + folderName).exists()) {
                    downloadPath = outputFilePath + "/" + folderName;
                    if (download.hitDownload(downloadPath, fileName + "." + extension, queryString)) {
                        logger.info("Entity is downloaded.");
                        templateDetails.put("Download Path", downloadPath);
                        templateDetails.put("File Name", fileName);
                        templateDetails.put("File Extension", extension);
                    } else {
                        logger.error("{} is not downloaded.", entityName.toUpperCase());
                        customAssert.assertTrue(false, entityName.toUpperCase() + " is not downloaded.");
                    }
                } else {
                    logger.error("Either download - target folder is not created or not found.");
                    customAssert.assertTrue(false, "Either download - target folder is not created or not found.");
                }
            } catch (Exception e) {
                logger.error("Exception {} occurred while downloading {}.", e.getMessage(), entityName);
                customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while downloading " + entityName + ".");
            }
        } else {
            logger.error("Show Response of the {} is not a valid JSON.", entityName);
            customAssert.assertTrue(false, "Show Response of " + entityName + " is not a valid JSON.");
        }

        return templateDetails;
    }

    private boolean uploadTemplate(int entityId, String entityName, String path, String fileName, String extension, CustomAssert customAssert) {
        String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(18);
        String options = null;
        boolean flag = false;
        String docName = fileName + "." + extension;

        HashMap<String, String> forms = new HashMap<>();
        forms.put("name", fileName);
        forms.put("extension", extension);
        forms.put("key", randomKeyForFileUpload);

        try {
            Edit edit = new Edit();
            String editGetResponse = edit.getEditPayload(entityName, entityId);
            if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
                String uploadResponse = new FileUpload().hitFileUpload(path, docName, forms);
                if (!uploadResponse.equalsIgnoreCase(null)) {
                    JSONObject editGetJSON = new JSONObject(editGetResponse);
                    Set<String> keys = editGetJSON.getJSONObject("body").getJSONObject("data").keySet();
                    for (String key : keys) {
                        try {
                            if (!editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).isNull("options")) {
                                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options", options);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }

                    String uploadDocumentValue = "{\"key\":\"" + randomKeyForFileUpload + "\"}";
                    editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("uploadDocument").put("values", new JSONObject(uploadDocumentValue));

                    editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("templateStyleId").remove("id");
                    editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("templateStyleId").remove("multiEntitySupport");

                    editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("privateCommunication").put("values", false);

                    String commentDocumentsValue = "{\"values\":[]}";
                    editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").remove("commentDocuments");
                    editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").put("commentDocuments", new JSONObject(commentDocumentsValue));

                    String editPostPayload = editGetJSON.toString();
                    String editPostResponse = edit.hitEdit(entityName, editPostPayload);
                    if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
                        if (new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                            flag = true;
                        }
                    } else {
                        logger.error("Edit Post Response is not a valid JSON");
                        customAssert.assertTrue(false, "Edit Post Response is not a valid JSON");
                    }
                }
            } else {
                logger.error("Edit Get Response is not a valid JSON.");
                customAssert.assertTrue(false, "Edit Get Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while uploading the new document.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while uploading the new document.");
        }
        return flag;
    }

    private boolean compareFiles(File documentFile, File templateFile, boolean templateComparison, CustomAssert customAssert) {
        boolean flag = false;
        try {
            FileInputStream docFis = new FileInputStream(documentFile);
            FileInputStream tempFis = new FileInputStream(templateFile);
            XWPFDocument document = new XWPFDocument(docFis);
            XWPFDocument template = new XWPFDocument(tempFis);
            XWPFWordExtractor docWordEx = new XWPFWordExtractor(document);
            XWPFWordExtractor tempWordEx = new XWPFWordExtractor(template);
            try {
                String documentData = docWordEx.getText().trim();
                String templateData = tempWordEx.getText().trim();
                if (!templateComparison & templateData.contains(documentData)) {
                    flag = true;
                } else if (templateComparison & templateData.equals(documentData)) {
                    flag = true;
                }
            } catch (Exception e) {
                logger.error("Exception {} occurred while reading the documents.", e.getMessage());
                customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while reading the documents.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while comparing the uploaded document with the downloaded template.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while comparing the uploaded document with the downloaded template.");
        }
        return flag;
    }

    private boolean compareTemplateAndClauseWithImage(File templateFile, File clauseFile, CustomAssert customAssert) {
        boolean flag = false;
        try {
            FileInputStream tempFis = new FileInputStream(templateFile);
            XWPFDocument template = new XWPFDocument(tempFis);
            List<XWPFPictureData> templatePictures = template.getAllPictures();
            List<BufferedImage> templatePics = new ArrayList<>();
            for (XWPFPictureData picture : templatePictures) {
                System.out.println(ImageIO.read(new ByteArrayInputStream(picture.getData())));
                templatePics.add(ImageIO.read(new ByteArrayInputStream(picture.getData())));
            }

            FileInputStream clauseFis = new FileInputStream(clauseFile);
            XWPFDocument clause = new XWPFDocument(clauseFis);
            List<XWPFPictureData> clausePictures = clause.getAllPictures();
            List<BufferedImage> clausePics = new ArrayList<>();
            for (XWPFPictureData picture : clausePictures) {
                System.out.println(ImageIO.read(new ByteArrayInputStream(picture.getData())));
                clausePics.add(ImageIO.read(new ByteArrayInputStream(picture.getData())));
            }

            double difference = -1;
            List<Double> diff = new ArrayList<>();
            for (BufferedImage imageInTemplate : templatePics) {
                for (BufferedImage imageInClause : clausePics) {
                    difference = compareImages(imageInTemplate, imageInClause, customAssert);
                    if (difference == 0.0) {
                        diff.add(difference);
                        break;
                    }
                }
            }

            if (diff.size() == clausePics.size()) {
                for (Double differ : diff) {
                    if (differ == 0.0) {
                        flag = true;
                    } else {
                        flag = false;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Exception {} occurred while comparing the image in template and clause.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while comparing the image in template and clause.");
        }
        return flag;
    }

    private double compareImages(BufferedImage imageInTemplate, BufferedImage imageInClause, CustomAssert customAssert) {
        double percentage = -1;
        try {
            int templateWidth = imageInTemplate.getWidth();
            int clauseWidth = imageInClause.getWidth();
            int templateHeight = imageInTemplate.getHeight();
            int clauseHeight = imageInClause.getHeight();
            if ((templateWidth != clauseWidth) || (templateHeight != clauseHeight)) {
                System.out.println("Both images should have same dimwnsions");
            } else {
                long diff = 0;
                for (int thIndex = 0; thIndex < templateHeight; thIndex++) {
                    for (int twIndex = 0; twIndex < templateWidth; twIndex++) {
                        //Getting the RGB values of a pixel
                        int templatePixel = imageInTemplate.getRGB(twIndex, thIndex);
                        Color templateColors = new Color(templatePixel, true);
                        int tempRed = templateColors.getRed();
                        int tempGreen = templateColors.getGreen();
                        int tempBlue = templateColors.getBlue();
                        int pixel2 = imageInClause.getRGB(twIndex, thIndex);

                        Color clauseColrs = new Color(pixel2, true);
                        int clauseRed = clauseColrs.getRed();
                        int clauseGreen = clauseColrs.getGreen();
                        int clauseBlue = clauseColrs.getBlue();
                        //sum of differences of RGB values of the two images
                        long data = Math.abs(tempRed - clauseRed) + Math.abs(tempGreen - clauseGreen) + Math.abs(tempBlue - clauseBlue);
                        diff = diff + data;
                    }
                }
                double avg = diff / (templateWidth * templateHeight * 3);
                percentage = (avg / 255) * 100;
                System.out.println("Difference: " + percentage);
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while comparing images.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while comparing images.");
        }
        return percentage;
    }

    private HashMap<String, Integer> verifyDocViewer(int templateId, int clauseId, CustomAssert customAssert) {
        HashMap<String, Integer> docViewerDetails = new HashMap<>();
        try {
            if (templateId == -1) {
                templateId = createTemplate("template doc viewer", "contract templates", customAssert);
            }
            docViewerDetails.put("TemplateID", templateId);
            if (templateId != -1) {
                logger.info("Contract Template has been Created.");
                //Download template
                HashMap<String, String> templateDetails = downloadEntity(templateId, "contract templates", customAssert);
                String templatePath = templateDetails.get("Download Path");
                String templateName = templateDetails.get("File Name");
                String templateExtension = templateDetails.get("File Extension");
                //Download clause
                HashMap<String, String> clauseDetails = downloadEntity(clauseId, "clauses", customAssert);
                String clausePath = clauseDetails.get("Download Path");
                String clauseName = clauseDetails.get("File Name");
                String clauseExtension = clauseDetails.get("File Extension");

                //Compare clause with Template
                File templateFile = new File(templatePath + "/" + templateName + "." + templateExtension);
                File clauseFile = new File(clausePath + "/" + clauseName + "." + clauseExtension);
                boolean templateDocViewerFlag = compareFiles(clauseFile, templateFile, false, customAssert);
                if (templateDocViewerFlag) {
                    logger.info("Template Doc Viewer has the correct clause content.");
                    docViewerDetails.put("TemplateDocViewer", 1);
                } else {
                    logger.error("Template Doc Viewer has some other clause content than the selected clause.");
                    customAssert.assertTrue(false, "Template Doc Viewer has some other clause content than the selected clause.");
                    docViewerDetails.put("TemplateDocViewer", 0);
                }
            } else {
                logger.error("Contract Template is not created.");
                customAssert.assertTrue(false, "Contract Template is not created.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while testing TC-C152214", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while testing TC-C152214");
        }
        return docViewerDetails;
    }

    private String getClauseText(int clauseId, CustomAssert customAssert) {
        Show show = new Show();
        String clauseText = "";
        try {
            show.hitShowGetAPI(138, clauseId);
            String showResponse = show.getShowJsonStr();
            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                clauseText = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").getString("text");
            } else {
                logger.error("Show page response of Clause is not a valid JSON.");
                customAssert.assertTrue(false, "Show page response of Clause is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching clause text.", e.getMessage());
        }
        return clauseText;
    }

    private boolean editClause(int clauseId, String originalText, String replacementText, CustomAssert customAssert) {
        boolean editClauseFlag = false;
        if (performActionOnClause(clauseId,"Inactivate", customAssert)) {
            if (editClauseText(clauseId, originalText, replacementText, customAssert)) {
                logger.info("Data of clause has been changed.");
            } else {
                logger.error("Data of clause could not be changed.");
                customAssert.assertTrue(false, "Data of clause could not be changed.");
            }
            if (performActionOnClause(clauseId,"Publish", customAssert)) {
                editClauseFlag = true;
            } else {
                logger.error("Clause could not be Published.");
                customAssert.assertTrue(false, "Clause could not be Published.");
            }
        } else {
            logger.error("Clause could not be inActivated.");
            customAssert.assertTrue(false, "Clause could not be inActivated.");
        }
        return editClauseFlag;
    }

    private boolean performActionOnClause(int clauseId, String actionName, CustomAssert customAssert) {
        int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdPath, entityIdName, "clauses", "entity_type_id"));
        try {
            boolean actionFlag = new WorkflowActionsHelper().performWorkflowAction(entityTypeId, clauseId, actionName);
            return actionFlag;
        } catch (Exception e) {
            logger.error("Exception {} occurred while clicking the button Inactivate.");
            customAssert.assertTrue(false,"Exception {} occurred while clicking the button Inactivate.");
            return false;
        }
    }

    private boolean editClauseText(int clauseId, String originalText, String replacementText, CustomAssert customAssert) {
        Edit edit = new Edit();
        boolean changeTextFlag = false;
        String options = null;
        try {
            String editPayload = edit.getEditPayload("clauses", clauseId);
            if (ParseJsonResponse.validJsonResponse(editPayload)) {
                JSONObject editJSON = new JSONObject(editPayload);
                Set<String> keys = editJSON.getJSONObject("body").getJSONObject("data").keySet();
                for (String key : keys) {
                    try {
                        if (!editJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).isNull("options")) {
                            editJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options", options);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }

                editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("text", replacementText);
                String htmlText = editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").getString("htmlText").replace(originalText, replacementText.replaceAll("\n","<br>"));
                editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", htmlText);

                keys = editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").keySet();
                for (String key : keys) {
                    try {
                        if (!editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key).isNull("options")) {
                            editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key).put("options", options);
                        }
                    } finally {
                        editJSON.getJSONObject("body").put(key, editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key));
                    }
                }

                String editPostPayload = editJSON.toString();
                String editPostResponse = edit.hitEdit("clauses", editPostPayload);
                if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
                    if (new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                        changeTextFlag = true;
                    } else {
                        logger.error("Edit Post Request is unsuccessful.");
                        customAssert.assertTrue(false, "Edit Post Request is unsuccessful.");
                    }
                } else {
                    logger.error("Edit Post Response is not a valid JSON.");
                    customAssert.assertTrue(false, "Edit Post Response is not a valid JSON.");
                }
            } else {
                logger.error("EDIT GET Payload is not a valid JSON.");
                customAssert.assertTrue(false, "EDIT GET Payload is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while Editing the clause text.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while Editing the clause text.");
        }
        return changeTextFlag;
    }

    private boolean editTemplate(int templateId, CustomAssert customAssert) {
        Edit edit = new Edit();
        boolean editFlag = false;
        String options = null;
        try {
            String editGetResponse = edit.getEditPayload("contract templates", templateId);
            if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
                JSONObject editGetJSON = new JSONObject(editGetResponse);
                Set<String> keys = editGetJSON.getJSONObject("body").getJSONObject("data").keySet();
                for (String key : keys) {
                    try {
                        if (!editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).isNull("options")) {
                            editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options", options);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }

                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("coverPageText").put("values", "");

                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("uploadDocument").put("values", options);

                String templateStyleIdData = "{\"values\":\"2100\",\"name\":\"templateStyleId\"}";
                editGetJSON.getJSONObject("body").getJSONObject("data").remove("templateStyleIdData");
                editGetJSON.getJSONObject("body").getJSONObject("data").put("templateStyleIdData", new JSONObject(templateStyleIdData));

                String editPostPayload = editGetJSON.toString();
                String editPostResponse = edit.hitEdit("contract templates", editPostPayload);
                if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
                    if (new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                        logger.info("Template editing is successful.");
                        editFlag = true;
                    } else {
                        logger.error("Template editing is unsuccessful.");
                        customAssert.assertTrue(false, "Template editing is unsuccessful.");
                    }
                } else {
                    logger.error("Edit Post Response is not a valid JSON.");
                    customAssert.assertTrue(false, "Edit Post Response is not a valid JSON.");
                }
            } else {
                logger.error("Edit GET Response is not a valid JSON.");
                customAssert.assertTrue(false, "Edit GET Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while editing the Template.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while editing the Template.");
        }
        return editFlag;
    }
}