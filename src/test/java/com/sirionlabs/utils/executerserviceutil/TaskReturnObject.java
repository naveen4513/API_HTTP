package com.sirionlabs.utils.executerserviceutil;

import java.util.List;
import java.util.Map;

/**
 * @author manoj.upreti
 */
public abstract class TaskReturnObject {
	public String entityName;
	public String dbID;
	public String taskType;
	public String taskName;
	public String taskJasonResponce;
	public Boolean SUCCESS;
	public Long TIMING;
	public Map<String, List<TaskReturnObject>> taskReturnObjectMapForMultipleEntities;
	public List<TaskReturnObject> taskReturnObjectList;
	public String requestedPayload = "";
	public String filterQueryName = "";
	public String filterID = "";

	public TaskReturnObject(String entityName, List<TaskReturnObject> taskReturnObjectList) {
		this.entityName = entityName;
		this.taskReturnObjectList = taskReturnObjectList;
	}

	public TaskReturnObject(String entityName, String dbID, String taskType, String taskName, String taskJasonResponce, Boolean SUCCESS, Long TIMING) {
		this.entityName = entityName;
		this.dbID = dbID;
		this.taskType = taskType;
		this.taskName = taskName;
		this.SUCCESS = SUCCESS;
		this.TIMING = TIMING;

	}

	//Reports Spacific Method
	public TaskReturnObject(String entityName, String dbID, String taskType, String taskName, String taskJasonResponce, String filterID, String filterQueryName, String requestedPayload, Boolean SUCCESS, Long TIMING) {
		this.entityName = entityName;
		this.dbID = dbID;
		this.taskType = taskType;
		this.taskName = taskName;
		this.filterID = filterID;
		this.filterQueryName = filterQueryName;
		this.requestedPayload = requestedPayload;
		this.SUCCESS = SUCCESS;
		this.TIMING = TIMING;

	}

	@Override
	public String toString() {
		return "Result :" + SUCCESS + " EntityName : " + entityName + " dbID : " + dbID + " taskName : " + taskName + " Time Taken for Execution " + TIMING;
	}

}
