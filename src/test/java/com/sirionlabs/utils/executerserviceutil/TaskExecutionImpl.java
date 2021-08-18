package com.sirionlabs.utils.executerserviceutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author manoj.upreti
 */
public class TaskExecutionImpl {
	static final int maximExecutionConcurency = 20;
	private final static Logger logger = LoggerFactory.getLogger(TaskExecutionImpl.class);

	public static List<TaskReturnObject> executeAnyTask(List<? extends TaskImpl> tasksForExec) throws InterruptedException, ExecutionException {
		List<TaskReturnObject> resultList = new ArrayList<>();
		List<Callable<TaskReturnObject>> tasks = new ArrayList<>();
		for (TaskImpl url : tasksForExec) {
			tasks.add(new CallableTaskImpl(url));
		}

		ExecutorService executor = Executors.newFixedThreadPool(maximExecutionConcurency);
		List<Future<TaskReturnObject>> results = executor.invokeAll(tasks);
		for (Future<TaskReturnObject> result : results) {
			TaskReturnObject taskReturnResult = result.get();
			resultList.add(taskReturnResult);
			//logger.debug("The result is [ {} ] ", taskReturnResult);
			//System.out.println(taskReturnResult);
		}

		executor.shutdown();

		return resultList;
	}

}
