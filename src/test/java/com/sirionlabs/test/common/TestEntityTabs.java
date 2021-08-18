package com.sirionlabs.test.common;

import com.sirionlabs.helper.entityTabs.EntityTabsValidationHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestEntityTabs {

	private final static Logger logger = LoggerFactory.getLogger(TestEntityTabs.class);

	private EntityTabsValidationHelper tabsValidationHelperObj = new EntityTabsValidationHelper();


	@DataProvider
	public Object[][] dataProviderForEntitiesTabValidation() {
		List<Object[]> allTestData = new ArrayList<>();

		String[] entitiesArr = {"governance body","obligations", "actions", "issues", "disputes", "work order requests", "interpretations", "change requests",
				"clauses", "definition", "contract template structure", "contract draft request", "suppliers", "service levels"};

		for (String entity : entitiesArr) {
			allTestData.add(new Object[]{entity.trim()});
		}

		return allTestData.toArray(new Object[0][]);
	}

	@DataProvider
	public Object[][] dataProviderForEntitiesTabOnShowPage() {
		List<Object[]> allTestData = new ArrayList<>();

		String[] entitiesArr = {"obligations", "actions", "issues", "disputes", "governance body", "work order requests", "interpretations", "change requests",
				"clauses", "definition"};
		String[] defaultTabsArr = {"REFERENCES", "COMMUNICATION", "AUDIT LOG"};
		String[] tabsArr1 = {"COMMUNICATION", "AUDIT LOG"};
		String[] tabsArr2={"FORWARD REFERENCE", "COMMUNICATION", "AUDIT LOG"};

		for (String entity : entitiesArr) {
			List<String> expectedTabsOnShowPage;

			switch (entity) {
				case "obligations":
				case "governance body":
					expectedTabsOnShowPage = Arrays.asList(tabsArr1);
					break;
				case "clauses":
				case "definition":
					expectedTabsOnShowPage = Arrays.asList(tabsArr2);
					break;
				default:
					expectedTabsOnShowPage = Arrays.asList(defaultTabsArr);
					break;
			}


			allTestData.add(new Object[]{entity.trim(), expectedTabsOnShowPage});
		}

		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForEntitiesTabOnShowPage")
	public void testEntitiesTabsPresentOnShowPage(String entityName, List<String> expectedTabsOnShowPage) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: Validate Tabs Present on Show Page for Entity {}", entityName);
			tabsValidationHelperObj.validateTabsArePresentOnShowPage(entityName, expectedTabsOnShowPage, csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "dataProviderForEntitiesTabValidation")
	public void testEntitiesCommunicationTab(String entityName) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: Validate Communication Tab for Entity " + entityName);
			tabsValidationHelperObj.validateEntityCommunicationTab(entityName, csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		}

		csAssert.assertAll();
	}


	@Test(dataProvider = "dataProviderForEntitiesTabValidation")
	public void testEntitiesAuditLogTab(String entityName) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: Validate Audit Log Tab for Entity " + entityName);
			tabsValidationHelperObj.validateEntityAuditLogTab(entityName, csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		}

		csAssert.assertAll();
	}
}