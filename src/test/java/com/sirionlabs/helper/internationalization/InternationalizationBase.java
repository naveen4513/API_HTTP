package com.sirionlabs.helper.internationalization;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.fieldLabel.MessagesList;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.config.ConfigureEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(InternationalizationBase.class);

    private UpdateAccount updateAccountObj = new UpdateAccount();
    public FieldRenaming fieldRenamingObj = new FieldRenaming();
    public MessagesList messagesListObj = new MessagesList();
    private String endUserLoginId;

    @BeforeClass
    public void beforeClass() {
        endUserLoginId = ConfigureEnvironment.getEndUserLoginId();

        logger.info("Changing User Language to Russian.");
        updateAccountObj.updateUserLanguage(endUserLoginId, 1002, 1000);
    }

    @AfterClass
    public void afterClass() {
        logger.info("Changing User Language back to English.");
        updateAccountObj.updateUserLanguage(endUserLoginId, 1002, 1);
    }
}