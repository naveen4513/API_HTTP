package com.sirionlabs.utils.executerserviceutil;

/**
 * @author manoj.upreti
 */
public abstract class TaskImpl implements Task {

	private String taskName = "";
	private String taskType = "";
	private String dbID = "";
	private String entityName = "";
	private String requestedPayload = "";
	private String filterID = "";
	private String filterQueryName = "";

	public TaskImpl(String taskName, String entityName, String dbID, String taskType) {
		this.taskName = taskName;
		this.entityName = entityName;
		this.dbID = dbID;
		this.taskType = taskType;
	}

	//Reports Spacific
	public TaskImpl(String taskName, String entityName, String dbID, String taskType, String filterID, String filterQueryName, String requestedPayload) {
		this.taskName = taskName;
		this.entityName = entityName;
		this.dbID = dbID;
		this.taskType = taskType;
		this.filterID = filterID;
		this.filterQueryName = filterQueryName;
		this.requestedPayload = requestedPayload;
	}

	@Override
	public abstract TaskReturnObject executeTask();

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getDbID() {
		return dbID;
	}

	public void setDbID(String dbID) {
		this.dbID = dbID;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getRequestedPayload() {
		return requestedPayload;
	}

	public String getFilterQueryName() {
		return filterQueryName;
	}

	public String getFilterID() {
		return filterID;
	}

}
	
