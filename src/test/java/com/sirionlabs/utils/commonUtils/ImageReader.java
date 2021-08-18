package com.sirionlabs.utils.commonUtils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by shivashish on 4/10/17.
 */
public class ImageReader {
	private final static Logger logger = LoggerFactory.getLogger(ImageReader.class);

	public static String getTextFromImage(String filePath) {
		//System.out.println(System.getProperty("user.dir"));
		File imageFile = new File(filePath);

		ITesseract instance = new Tesseract();

		try {

			String result = instance.doOCR(imageFile);

			return result;

		} catch (TesseractException e) {

			logger.error(e.getMessage());

			return "Error while reading image";

		}
	}


	// this function will verify is image has this text "No Data to Display"
	public static Boolean isImageHasNotDataToDisplay(String filePath) {

		File imageFile = new File(filePath);

		ITesseract instance = new Tesseract();

		try {

			String result = instance.doOCR(imageFile);

			return result.contains("Mn um m mm");

		} catch (TesseractException e) {

			logger.error(e.getMessage());

			return false;

		}
	}

	// this function will verify is image has this text "Chart type not supported"
	public static Boolean isImageHasChartTypeNotSupported(String filePath) {

		File imageFile = new File(filePath);

		ITesseract instance = new Tesseract();

		try {

			String result = instance.doOCR(imageFile);

			return result.contains("c m type we suwrma");

		} catch (TesseractException e) {

			logger.error(e.getMessage());

			return false;

		}
	}


}
