package com.sirionlabs.test.invoice;

import com.sirionlabs.helper.entityCreation.TestCreation;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ServiceDataCreationFromCDR {

    @BeforeClass
    void BeforeClass(){

    }

    @Test
    public void ServiceDataCreationFromCDRTest(){

        InvoiceHelper.getContractId("configFiles/Regression/AutoOffice/Helper/EntityCreation/Contract","contract","contractExtraFields","fixed fee flow 1");

    }
}
