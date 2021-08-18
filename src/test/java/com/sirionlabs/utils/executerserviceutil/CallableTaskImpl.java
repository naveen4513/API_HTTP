package com.sirionlabs.utils.executerserviceutil;

import java.util.concurrent.Callable;

/**
 * @author manoj.upreti
 */
public class CallableTaskImpl implements Callable<TaskReturnObject> {
	private final TaskImpl task;

	CallableTaskImpl(TaskImpl taskToExecute) {
		task = taskToExecute;
	}

	@Override
	public TaskReturnObject call() throws Exception {
		return task.executeTask();
	}
}
