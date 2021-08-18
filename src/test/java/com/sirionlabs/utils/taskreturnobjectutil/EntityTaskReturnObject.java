package com.sirionlabs.utils.taskreturnobjectutil;

import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;

/**
 * Created by manoj.upreti on 19-07-2017.
 */
public class EntityTaskReturnObject extends TaskReturnObject {
	//normal responce for single lavel thread
	public EntityTaskReturnObject(String entityName, String dbID, String taskType, String taskName, String taskJasonResponce, Boolean SUCCESS, Long TIMING) {
		super(entityName, dbID, taskType, taskName, taskJasonResponce, SUCCESS, TIMING);
	}

}
