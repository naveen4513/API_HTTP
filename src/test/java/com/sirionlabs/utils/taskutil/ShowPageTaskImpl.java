package com.sirionlabs.utils.taskutil;

import com.sirionlabs.test.TestUserRoleGroupForParallelExecution;
import com.sirionlabs.utils.executerserviceutil.TaskImpl;
import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;
import com.sirionlabs.utils.taskreturnobjectutil.ShowPageTaskReturnObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author manoj.upreti
 */
public class ShowPageTaskImpl extends TaskImpl {
	private final static Logger logger = LoggerFactory.getLogger(ShowPageTaskImpl.class);
	TaskReturnObject resultOfTask = null;

	public ShowPageTaskImpl(String taskName, String entityName, String dbID, String taskType) {
		super(taskName, entityName, dbID, taskType);
	}

	@Override
	public synchronized TaskReturnObject executeTask() {
		if (getTaskName().equalsIgnoreCase("validateShowPermissionForId")) {
			return validateShowPermission();
		} else if (getTaskName().equalsIgnoreCase("validateShowPermissionMultipleEntitiesParallel")) {
			return validateShowPermissionMultipleEntitiesParallel();
		} else {
			logger.error("Task Name [ {}  ] is not matching with existing task names in ShowPageTaskImpl.java class , please check .", getTaskName());
			return null;
		}

	}

	public TaskReturnObject validateShowPermissionMultipleEntitiesParallel() {
		TaskImpl task = this;
		TestUserRoleGroupForParallelExecution testUserRoleGroupObj = new TestUserRoleGroupForParallelExecution();

		String entityName = task.getEntityName();
		List<TaskReturnObject> taskReturnObjectList = testUserRoleGroupObj.verifyShowListingForAllRecords(entityName);
		return new ShowPageTaskReturnObject(entityName, taskReturnObjectList);
	}

	public TaskReturnObject validateShowPermission() {
		TaskImpl task = this;
		TestUserRoleGroupForParallelExecution testUserRoleGroupObj = new TestUserRoleGroupForParallelExecution();

		String entityName = task.getEntityName();
		String dbID = task.getDbID();
		String taskType = task.getTaskType();
		String taskName = task.getTaskName();
		String taskJasonResponce = "";
		Boolean SUCCESS = testUserRoleGroupObj.validateShowPermissionForId(Integer.parseInt(task.getEntityName()), Integer.parseInt(task.getDbID()));
		;
		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis();
		Long TIMING = end - start;
		//String entityName, String dbID, String taskType, String taskName, String taskJasonResponce, Boolean SUCCESS, Long TIMING, Map<String, String> showPageResponceMapForDbID
		return new ShowPageTaskReturnObject(entityName, dbID, taskType, taskName, taskJasonResponce, SUCCESS, TIMING);
	}

}
