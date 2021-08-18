package com.sirionlabs.utils.executerserviceutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by manoj.upreti on 18-07-2017.
 */
public class TaskExecuter {
	private final static Logger logger = LoggerFactory.getLogger(TaskExecuter.class);

	public static List<TaskReturnObject> executeParallely(List<? extends TaskImpl> tasksForExec) {
		try {
			return TaskExecutionImpl.executeAnyTask(tasksForExec);
		} catch (InterruptedException e) {
			logger.error("Getting InterruptedException while executing task returning null, please debug", e.getStackTrace());
			e.printStackTrace();
			return null;
		} catch (ExecutionException e) {
			logger.error("Getting ExecutionException while executing task returning null, please debug", e.getStackTrace());
			e.printStackTrace();

			return null;
		}
	}
}
