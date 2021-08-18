package com.sirionlabs.utils.taskreturnobjectutil;

import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;

/**
 * Created by manoj.upreti on 07-08-2017.
 */
public class ReportsTaskReturnObject extends TaskReturnObject {
	public ReportsTaskReturnObject(String entityName, String dbID, String taskType, String taskName, String taskJasonResponce, String filterID, String filterQueryName, String requestedPayload, Boolean SUCCESS, Long TIMING) {
		super(entityName, dbID, taskType, taskName, taskJasonResponce, filterID, filterQueryName, requestedPayload, SUCCESS, TIMING);
	}
}
