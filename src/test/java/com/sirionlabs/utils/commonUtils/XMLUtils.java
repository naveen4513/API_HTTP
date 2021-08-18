package com.sirionlabs.utils.commonUtils;


import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class XMLUtils {

	private final static Logger logger = LoggerFactory.getLogger(XMLUtils.class);

	public static Boolean isValidXml(File inputFile) {
		Boolean flag = true;
		try {
			SAXReader saxReader = new SAXReader();
			Document document = saxReader.read(inputFile);
			document.getRootElement();
		} catch (Exception e) {
			flag = false;
			logger.error("Exception while parsing xml file : {}, error : {}", inputFile.getName(), e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	public static List<Node> getNodes(File inputFile, String xPath) {
		List<Node> nodeList = null;
		try {
			SAXReader saxReader = new SAXReader();
			Document document = saxReader.read(inputFile);
			nodeList = document.selectNodes(xPath);
		} catch (Exception e) {
			logger.error("Exception while getting nodes for xpath : {}, error :{}", xPath, e.getMessage());
			e.printStackTrace();
		}
		return nodeList;
	}

	public static Node getNode(File inputFile, String xPath) {
		Node node = null;
		try {
			SAXReader saxReader = new SAXReader();
			Document document = saxReader.read(inputFile);
			node = document.selectSingleNode(xPath);
		} catch (Exception e) {
			logger.error("Exception while getting nodes for xpath : {}, error :{}", xPath, e.getMessage());
			e.printStackTrace();
		}
		return node;
	}

	public static String getValue(Node node) {
		return node.getText();
	}

	public static Document modifyElement(File inputFile, String parentXpath, String elementName, String modifiedValue) {
		Document document = null;
		try {
			SAXReader saxReader = new SAXReader();
			document = saxReader.read(inputFile);
			Node node = document.selectSingleNode(parentXpath);

			Element element = (Element) node;
			Iterator<Element> iterator = element.elementIterator(elementName);

			while (iterator.hasNext()) {
				Element eccElement = iterator.next();
				eccElement.setText(modifiedValue);
			}
		} catch (Exception e) {
			logger.error("Exception while modifying the xml. element:{}, error :{}", elementName, e.getMessage());
			e.printStackTrace();
		}
		return document;
	}

	public static Document modifyElement(Document document, String parentXpath, String elementName, String modifiedValue) {
		try {
			Node node = document.selectSingleNode(parentXpath);

			Element element = (Element) node;
			Iterator<Element> iterator = element.elementIterator(elementName);

			while (iterator.hasNext()) {
				Element eccElement = iterator.next();
				eccElement.setText(modifiedValue);
			}

		} catch (Exception e) {
			logger.error("Exception while modifying the xml. element:{}, error :{}", elementName, e.getMessage());
			e.printStackTrace();
		}
		return document;
	}

	public static void dumpXmlDocumentIntoFile(Document document, File outputFile) {

		try {
			XMLWriter output = new XMLWriter(new FileWriter(outputFile));
			output.write(document);
			output.close();
		} catch (IOException e) {
			logger.error("Exception while dumping xml to file. error :{}", e.getMessage());
			e.printStackTrace();
		}

	}


}
