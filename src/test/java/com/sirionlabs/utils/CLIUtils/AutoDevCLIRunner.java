package com.sirionlabs.utils.CLIUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoDevCLIRunner extends CLIUtils {

    private static final String sectionName = "autooffice";
    private static final Logger logger = LoggerFactory.getLogger(AutoDevCLIRunner.class);

    AutoDevCLIRunner(String sectionName) {
        super(sectionName);
    }

    public static void main(String[] args) {
        AutoDevCLIRunner autoDevCLIRunner = new AutoDevCLIRunner(sectionName);


        doStuffs(autoDevCLIRunner);


        autoDevCLIRunner.closeConnections();
        int i=0;
    }

    private static void doStuffs(AutoDevCLIRunner autoDevCLIRunner) {

        boolean restartDone;



        // Restart Sch Job
        restartDone = autoDevCLIRunner.restartSchJob();
        logger.info("Sch Job Restart : {}",restartDone);

        //Restart Scheduler
        restartDone = autoDevCLIRunner.restartScheduler();
        logger.info("Scheduler Restart : {}",restartDone);

        //Restart App server
//        restartDone = autoDevCLIRunner.restartAppServer();
//        logger.info("App Server Restart : {}",restartDone);


    }
}
