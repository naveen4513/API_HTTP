package com.sirionlabs.utils.taskreturnobjectutil;

import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;

import java.util.List;

/**
 * Created by manoj.upreti on 19-07-2017.
 */
public class ShowPageTaskReturnObject extends TaskReturnObject {
	//Single Entity
	public ShowPageTaskReturnObject(String entityName, String dbID, String taskType, String taskName, String taskJasonResponce, Boolean SUCCESS, Long TIMING) {
		super(entityName, dbID, taskType, taskName, taskJasonResponce, SUCCESS, TIMING);
	}

	//multiple entities
	public ShowPageTaskReturnObject(String entityName, List<TaskReturnObject> taskReturnObjectList) {
		super(entityName, taskReturnObjectList);
	}
}
