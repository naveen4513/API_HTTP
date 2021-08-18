package com.sirionlabs.api.invoice;

import com.sirionlabs.helper.APITesting.TestGetApi;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceCopyViewer extends TestAPIBase {


    private final Logger logger = LoggerFactory.getLogger(InvoiceCopyViewer.class);

    private String getInvoiceCopyViewerLinkPath(int invoiceId){
        return "/baseInvoice/getInvoiceCopyViewerLink/"+invoiceId;
    }

    public APIValidator getInvoiceCopyViewerLink(int invoiceId){
        return executor.get(getInvoiceCopyViewerLinkPath(invoiceId),null);
    }
}
