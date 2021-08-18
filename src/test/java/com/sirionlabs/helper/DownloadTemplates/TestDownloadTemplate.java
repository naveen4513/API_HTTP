package com.sirionlabs.helper.DownloadTemplates;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shivashish on 20/3/18.
 */
public class TestDownloadTemplate {


	@Test(priority = 0)
	public void testdownloadServiceData() throws Exception {
		DownloadServiceDataTemplate downloadServiceDataTemplate = new DownloadServiceDataTemplate();
		downloadServiceDataTemplate.downloadServiceDataTemplate("fixed_fee", "61", "129386");


	}

	@Test(priority = 1)
	public void testdownloadPricingTemplate() throws Exception {
		DownloadPricingTemplate downloadPricingTemplate = new DownloadPricingTemplate();
		List<String> ids = new ArrayList<>();
		ids.add("9575");


		downloadPricingTemplate.downloadPricingDataTemplate("forecast", "129388", ids);


	}


	@Test(priority = 2)
	public void testdownloadConsumptionTemplate() throws Exception {
		DownloadConsumptionTemplate downloadConsumptionTemplate = new DownloadConsumptionTemplate();
		List<String> ids = new ArrayList<>();
		ids.add("1016");


		downloadConsumptionTemplate.downloadConsumptionDataTemplate(ids);


	}

	@Test(priority = 3)
	public void testdownloadInvoiceCreationTemplate() throws Exception {
		DownloadInvoiceCreationTemplate downloadInvoiceCreationTemplate = new DownloadInvoiceCreationTemplate();
		downloadInvoiceCreationTemplate.downloadInvoiceCreationTemplate("61", "129386");


	}

	@Test(priority = 4)
	public void testdownloadInvoiceLineItemsCreationTemplate() throws Exception {
		DownloadInvoiceLineItemsCreationTemplate downloadInvoiceLineItemsCreationTemplate = new DownloadInvoiceLineItemsCreationTemplate();
		downloadInvoiceLineItemsCreationTemplate.downloadInvoiceLineItemsCreationTemplate("67", "5368");


	}


}
