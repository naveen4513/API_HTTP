package com.sirionlabs.utils.taskutil;

import com.sirionlabs.test.TestUserRoleGroupForParallelExecution;
import com.sirionlabs.utils.executerserviceutil.TaskImpl;
import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;
import com.sirionlabs.utils.taskreturnobjectutil.EntityTaskReturnObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author manoj.upreti
 */
public class EntityTaskImpl extends TaskImpl {
	private final static Logger logger = LoggerFactory.getLogger(EntityTaskImpl.class);
	TaskReturnObject resultOfTask = null;

	public EntityTaskImpl(String taskName, String entityName, String dbID, String taskType) {
		super(taskName, entityName, dbID, taskType);
	}

	@Override
	public synchronized TaskReturnObject executeTask() {
		if (getTaskName().equalsIgnoreCase("verifyEntityListing")) {
			return verifyEntityListing();
		} else {
			logger.error("Task Name [ {}  ] is not matching with existing task names in EntityTaskImpl.java class , please check .", getTaskName());
			return null;
		}
	}

	public TaskReturnObject verifyEntityListing() {
		TaskImpl task = this;
		TestUserRoleGroupForParallelExecution testUserRoleGroupObj = new TestUserRoleGroupForParallelExecution();

		String entityName = task.getEntityName();
		String dbID = task.getDbID();
		String taskType = task.getTaskType();
		String taskName = task.getTaskName();
		String taskJasonResponce = "";
		Boolean SUCCESS = testUserRoleGroupObj.verifyEntityListing(task.getEntityName());
		;
		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis();
		Long TIMING = end - start;
		//String entityName, String dbID, String taskType, String taskName, String taskJasonResponce, Boolean SUCCESS, Long TIMING, Map<String, String> showPageResponceMapForDbID
		return new EntityTaskReturnObject(entityName, dbID, taskType, taskName, taskJasonResponce, SUCCESS, TIMING);
	}

}
