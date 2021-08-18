package com.sirionlabs.utils.CLIUtils;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.SCPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIUtils {

    private static final Logger logger = LoggerFactory.getLogger(CLIUtils.class);

    private final static String configFilePath = "src/test/resources/Helper/CLIUtils";
    private final static String configFileName = "CLIRunnerDetails.cfg";
    private String sectionName= null;
    private SCPUtils appServerScpUtils;
    private SCPUtils searchServerScpUtils;

    public void closeConnections(){
        appServerScpUtils.closeSession();
        searchServerScpUtils.closeSession();
    }

    CLIUtils(String sectionName){
        this.sectionName=sectionName;

        String host = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"searchserverhost");
        String key = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"searchserverkey");
        String withKey = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"searchserverwithkey");
        String user = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"searchserveruser");

        assert host!=null && key!=null && withKey!=null && user!=null:"Some values from the config files are null for search server";

        if(withKey.equalsIgnoreCase("no"))
            searchServerScpUtils = new SCPUtils(host,user, key,22);
        else
            searchServerScpUtils = new SCPUtils(host,user,key,22,true);


        host = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"appserverhost");
        key = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"appserverkey");
        withKey = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"appserverwithkey");
        user = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"appserveruser");

        assert host!=null && key!=null && withKey!=null && user!=null:"Some values from the config files are null for app server";

        if(withKey.equalsIgnoreCase("no"))
            appServerScpUtils = new SCPUtils(host,user, key,22);
        else
            appServerScpUtils = new SCPUtils(host,user,key,22,true);


    }

    public boolean restartSchJob(){
        String schJobPath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"schjobpath");
        String schJobGrepKeyWord = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"grepkeywordsch");

        String showResponseProperty = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"showresponselogforsearchserver");

        boolean showResponseLog = showResponseProperty != null && (showResponseProperty.equalsIgnoreCase("yes"));


        String command = "ps -ef |grep "+schJobGrepKeyWord+"\nexit\n";
        String grepResultSchJob = searchServerScpUtils.runCommand(command,showResponseLog);
        grepResultSchJob=grepResultSchJob.replace("\u001B[m\u001B[K","");
        grepResultSchJob=grepResultSchJob.replace("\u001B[01;31m\u001B[K","");
        while (grepResultSchJob.contains(schJobPath)){
            logger.info("Sch Job is up");
            String user = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"searchserveruser");
            assert user!=null:"User is null";

            String taskId=null;

            String[] tasks = grepResultSchJob.split("\n"+user);

            for(String task : tasks){
                task = task.trim();
                if(task.contains(schJobPath)){
                    taskId=task.split(" ")[0].trim();
                    break;
                }
            }

            assert taskId!=null:"Task id is null";

            logger.info("Task id is : {} for : {}",taskId,schJobPath);


            logger.info("Killing task {}",taskId);
            command = "kill -9 "+taskId+"\nexit\n";
            searchServerScpUtils.runCommand(command,showResponseLog);

            command = "ps -ef |grep "+schJobGrepKeyWord+"\nexit\n";
            grepResultSchJob = searchServerScpUtils.runCommand(command,showResponseLog);

            if(grepResultSchJob.contains(taskId)){
                logger.info("The task {} found even after killing it",taskId);
                return false;
            }

            grepResultSchJob=grepResultSchJob.replace("\u001B[m\u001B[K","");
            grepResultSchJob=grepResultSchJob.replace("\u001B[01;31m\u001B[K","");
        }
            logger.info("Starting Sch Job");
            command = "cd "+schJobPath+"/bin\nnohup sh SchedulerMain &\nexit\n";
            searchServerScpUtils.runCommand(command,showResponseLog);


        command = "ps -ef |grep "+schJobGrepKeyWord+"\nexit\n";
        String startResponse = searchServerScpUtils.runCommand(command,showResponseLog);

        startResponse=startResponse.replace("\u001B[m\u001B[K","");
        startResponse=startResponse.replace("\u001B[01;31m\u001B[K","");

        return startResponse.contains(schJobPath);

    }

    public boolean restartScheduler(){
        String schedulerJobPath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"schedulerpath");
        String schedulerGrepKeyword = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"grepkeywordscheduler");

        String showResponseProperty = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"showresponselogforsearchserver");

        boolean showResponseLog = showResponseProperty != null && (showResponseProperty.equalsIgnoreCase("yes"));

        String command = "ps -ef |grep "+schedulerGrepKeyword+"\nexit\n";
        String grepResultSchJob = searchServerScpUtils.runCommand(command,showResponseLog);
        grepResultSchJob=grepResultSchJob.replace("\u001B[m\u001B[K","");
        grepResultSchJob=grepResultSchJob.replace("\u001B[01;31m\u001B[K","");
        while(grepResultSchJob.contains(schedulerJobPath)){
            logger.info("Scheduler is up");
            String user = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"searchserveruser");
            assert user!=null:"User is null";

            String taskId=null;

            String[] tasks = grepResultSchJob.split("\n"+user);

            for(String task : tasks){
                task = task.trim();
                if(task.contains(schedulerJobPath)){
                    taskId=task.split(" ")[0].trim();
                    break;
                }
            }

            assert taskId!=null:"Task id is null";

            logger.info("Task id is : {} for : {}",taskId,schedulerJobPath);


            logger.info("Killing task {}",taskId);
            command = "kill -9 "+taskId+"\nexit\n";
            searchServerScpUtils.runCommand(command,showResponseLog);

            command = "ps -ef |grep "+schedulerGrepKeyword+"\nexit\n";
            grepResultSchJob = searchServerScpUtils.runCommand(command,showResponseLog);

            if(grepResultSchJob.contains(taskId)){
                logger.info("The task {} found even after killing it",taskId);
                return false;
            }

            grepResultSchJob=grepResultSchJob.replace("\u001B[m\u001B[K","");
            grepResultSchJob=grepResultSchJob.replace("\u001B[01;31m\u001B[K","");
        }
            logger.info("Starting Scheduler");
            command = "cd "+schedulerJobPath+"/bin\n./startup.sh\nexit\n";
            searchServerScpUtils.runCommand(command,showResponseLog);


        command = "ps -ef |grep "+schedulerGrepKeyword+"\nexit\n";
        String startResponse = searchServerScpUtils.runCommand(command,showResponseLog);

        startResponse=startResponse.replace("\u001B[m\u001B[K","");
        startResponse=startResponse.replace("\u001B[01;31m\u001B[K","");

        return startResponse.contains(schedulerJobPath);
    }

    public boolean restartAppServer(){
        String appPath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"apppath");
        String appGrepKeyword = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"grepkeywordapp");

        String showResponseProperty = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"showresponselogforappserver");

        boolean showResponseLog = showResponseProperty != null && (showResponseProperty.equalsIgnoreCase("yes"));

        String command = "ps -ef |grep "+appGrepKeyword+"\nexit\n";
        String grepResultSchJob = appServerScpUtils.runCommand(command,showResponseLog);
        grepResultSchJob=grepResultSchJob.replace("\u001B[m\u001B[K","");
        grepResultSchJob=grepResultSchJob.replace("\u001B[01;31m\u001B[K","");
        while (grepResultSchJob.contains(appPath)){
            logger.info("App server is up");
            String user = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,sectionName,"appserveruser");
            assert user!=null:"User is null";

            String taskId=null;

            String[] tasks = grepResultSchJob.split("\n"+user);

            for(String task : tasks){
                task = task.trim();
                if(task.contains(appPath)){
                    taskId=task.split(" ")[0].trim();
                    break;
                }
            }

            assert taskId!=null:"Task id is null";

            logger.info("Task id is : {} for : {}",taskId,appPath);


            logger.info("Killing task {}",taskId);
            command = "kill -9 "+taskId+"\nexit\n";
            appServerScpUtils.runCommand(command,showResponseLog);

            command = "ps -ef |grep "+appGrepKeyword+"\nexit\n";
            grepResultSchJob = appServerScpUtils.runCommand(command,showResponseLog);

            if(grepResultSchJob.contains(taskId)){
                logger.info("The task {} found even after killing it",taskId);
                return false;
            }

            grepResultSchJob=grepResultSchJob.replace("\u001B[m\u001B[K","");
            grepResultSchJob=grepResultSchJob.replace("\u001B[01;31m\u001B[K","");

        }
            logger.info("Starting App server");
            command = "cd "+appPath+"/bin\n./startup.sh\nexit\n";
            appServerScpUtils.runCommand(command,showResponseLog);


        command = "ps -ef |grep "+appGrepKeyword+"\nexit\n";
        String startResponse = appServerScpUtils.runCommand(command,showResponseLog);

        startResponse=startResponse.replace("\u001B[m\u001B[K","");
        startResponse=startResponse.replace("\u001B[01;31m\u001B[K","");

        return startResponse.contains(appPath);
    }
}
