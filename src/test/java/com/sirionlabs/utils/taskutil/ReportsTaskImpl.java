package com.sirionlabs.utils.taskutil;

import com.sirionlabs.test.reportRenderer.TestReportRenderParallelExecution;
import com.sirionlabs.utils.executerserviceutil.TaskImpl;
import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;
import com.sirionlabs.utils.taskreturnobjectutil.ReportsTaskReturnObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author manoj.upreti
 */
public class ReportsTaskImpl extends TaskImpl {
	private final static Logger logger = LoggerFactory.getLogger(ReportsTaskImpl.class);

	public ReportsTaskImpl(String taskName, String entityName, String dbID, String taskType, String filterID, String filterQueryName, String requestedPayload) {
		super(taskName, entityName, dbID, taskType, filterID, filterQueryName, requestedPayload);
	}


	@Override
	public TaskReturnObject executeTask() {

		if (getTaskName().equalsIgnoreCase("validatereport")) {
			return validatePayloadSpecificReportResponce();
		} else {
			logger.error("Task Name [ {}  ] is not matching with existing task names in ReportTaskImpl.java class , please check .", getTaskName());
			return null;
		}
	}

	public TaskReturnObject validatePayloadSpecificReportResponce() {
		TaskImpl task = this;
		TestReportRenderParallelExecution objTestReportRender = new TestReportRenderParallelExecution();

		String entityName = task.getEntityName();
		String dbID = task.getDbID();
		String taskType = task.getTaskType();
		String taskName = task.getTaskName();
		String taskJasonResponce = "";
		String filterID = task.getFilterID();
		String filterQueryName = task.getFilterQueryName();
		String requestedPayload = task.getRequestedPayload();

		Boolean SUCCESS = objTestReportRender.validateResponceParallelly(task.getEntityName(), Integer.parseInt(task.getDbID()), Integer.parseInt(filterID), filterQueryName, requestedPayload);

		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis();
		Long TIMING = end - start;
		return new ReportsTaskReturnObject(entityName, dbID, taskType, taskName, taskJasonResponce, filterID, filterQueryName, requestedPayload, SUCCESS, TIMING);
	}
}
