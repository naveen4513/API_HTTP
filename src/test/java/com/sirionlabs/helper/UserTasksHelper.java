package com.sirionlabs.helper;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.api.usertasks.Remove;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserTasksHelper {

	private final static Logger logger = LoggerFactory.getLogger(Fetch.class);

	private static Long schedulerJobTimeOut = 180000L;
	private static Long schedulerJobPollingTime = 5000L;

	public static int getNoOfTasksInQueue(String fetchJsonStr) {
		int noOfTasks = -1;
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				JSONObject jsonObj = new JSONObject(fetchJsonStr);
				noOfTasks = jsonObj.getJSONObject("queuedTasksBox").getInt("totalCount");
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting No Of Tasks In Queue. {}", e.getMessage());
		}
		return noOfTasks;
	}

	/*
	Returns List of All the Tasks' JSON Object as String.
    */
	public static String fetchJobScheduler() {
		try {
			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			return fetchObj.getFetchJsonStr();
		} catch (Exception e) {
			logger.error("Exception while Removing all Tasks. {}", e.getMessage());
			return null;
		}
	}

	/*
	Returns List of All Tasks in Queue's JSON Object as String.
	 */
	public static List<String> getAllTasksInQueue(String fetchJsonStr) {
		List<String> allTasksInQueue = new ArrayList<>();
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				JSONObject jsonObj = new JSONObject(fetchJsonStr);
				jsonObj = jsonObj.getJSONObject("queuedTasksBox");
				if (jsonObj.has("queuedUserTasks") && !jsonObj.isNull("queuedUserTasks")) {
					JSONArray jsonArr = jsonObj.getJSONArray("queuedUserTasks");
					if (jsonArr.length() > 0) {
						for (int i = 0; i < jsonArr.length(); i++) {
							allTasksInQueue.add(jsonArr.getJSONObject(i).toString());
						}
					}
				} else {
					logger.info("Either Fetch Response doesn't have queuedUserTasks Object or queuedUserTasks is Null.");
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Tasks in Queue. {}", e.getMessage());
		}
		return allTasksInQueue;
	}

	/*
	Returns List of all Picked Tasks' JSON Object as String.
	 */
	public static List<String> getAllPickedTasks(String fetchJsonStr) {
		List<String> allPickedTasks = new ArrayList<>();
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				JSONObject jsonObj = new JSONObject(fetchJsonStr);
				jsonObj = jsonObj.getJSONObject("pickedTasksBox");

				if (jsonObj.has("currentDayUserTasks") && !jsonObj.isNull("currentDayUserTasks")) {
					JSONArray dayJsonArr = jsonObj.getJSONArray("currentDayUserTasks");
					if (dayJsonArr.length() > 0) {
						for (int i = 0; i < dayJsonArr.length(); i++) {
							allPickedTasks.add(dayJsonArr.getJSONObject(i).toString());
						}
					}
				}

				if (jsonObj.has("currentWeekUserTasks") && !jsonObj.isNull("currentWeekUserTasks") && jsonObj.getJSONObject("currentWeekUserTasks").has("tasks")
						&& !jsonObj.getJSONObject("currentWeekUserTasks").isNull("tasks")) {
					jsonObj = jsonObj.getJSONObject("currentWeekUserTasks");
					JSONArray weekJsonArr = jsonObj.getJSONArray("tasks");
					if (weekJsonArr.length() > 0) {
						for (int i = 0; i < weekJsonArr.length(); i++) {
							allPickedTasks.add(weekJsonArr.getJSONObject(i).toString());
						}
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Picked Tasks. {}", e.getMessage());
		}
		return allPickedTasks;
	}

	public static List<String> getAllTasks(String fetchJsonStr) {
		List<String> allTasks = new ArrayList<>();
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasksInQueue = getAllTasksInQueue(fetchJsonStr);
				allTasks.addAll(allTasksInQueue);
				List<String> allPickedTasks = getAllPickedTasks(fetchJsonStr);
				allTasks.addAll(allPickedTasks);
			} else {
				logger.error("Fetch Response in an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Tasks. {}", e.getMessage());
		}
		return allTasks;
	}

	/*
	Returns List of all Non Completed Tasks' JSON Object as String.
	 */
	public static List<String> getAllNonCompletedTasks(String fetchJsonStr) {
		List<String> allNonCompletedTasks = new ArrayList<>();
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.has("status") && !jsonObj.isNull("status")) {
						if (jsonObj.getJSONObject("status").has("name")) {
							String statusName = jsonObj.getJSONObject("status").getString("name");
							if (!statusName.trim().equalsIgnoreCase("Completed")) {
								allNonCompletedTasks.add(task);
							}
						} else {
							allNonCompletedTasks.add(task);
						}
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Non Completed Tasks. {}", e.getMessage());
		}
		return allNonCompletedTasks;
	}

	public static boolean removeAllTasks() {
		try {
			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			String fetchResponse = fetchObj.getFetchJsonStr();
			return removeAllTasks(fetchResponse);
		} catch (Exception e) {
			logger.error("Exception while Removing all Tasks. {}", e.getMessage());
		}
		return false;
	}

	public static boolean removeAllTasks(String fetchJsonStr) {
		boolean allTasksRemoved = true;
		try {
			List<String> allTasks = getAllTasks(fetchJsonStr);
			logger.info("Total tasks found: {}", allTasks.size());
			for (String task : allTasks) {
				logger.info("Hitting Remove API.");
				Remove removeObj = new Remove();
				removeObj.hitRemove(task);
				String removeResponse = removeObj.getRemoveJsonStr();
				if (removeResponse == null || !removeResponse.trim().equalsIgnoreCase("true"))
					allTasksRemoved = false;
			}
		} catch (Exception e) {
			logger.error("Exception while Removing all Tasks. {}", e.getMessage());
			allTasksRemoved = false;
		}
		return allTasksRemoved;
	}

	public static boolean removeAllNonCompletedTasks() {
		try {
			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			String fetchResponse = fetchObj.getFetchJsonStr();
			return removeAllNonCompletedTasks(fetchResponse);
		} catch (Exception e) {
			logger.error("Exception while Removing all Non Completed Tasks. {}", e.getMessage());
		}
		return false;
	}

	public static boolean removeAllNonCompletedTasks(String fetchJsonStr) {
		boolean allTasksRemoved = true;
		try {
			List<String> allNonCompletedTasks = getAllNonCompletedTasks(fetchJsonStr);
			logger.info("Total Non Completed tasks found: {}", allNonCompletedTasks.size());
			for (String task : allNonCompletedTasks) {
				logger.info("Hitting Remove API.");
				Remove removeObj = new Remove();
				removeObj.hitRemove(task);
				String removeResponse = removeObj.getRemoveJsonStr();
				if (removeResponse == null || !removeResponse.trim().equalsIgnoreCase("true"))
					allTasksRemoved = false;
			}
		} catch (Exception e) {
			logger.error("Exception while Removing all Non Completed Tasks. {}", e.getMessage());
			allTasksRemoved = false;
		}
		return allTasksRemoved;
	}

	/*
	Returns List of All Tasks' Id.
	 */
	public static List<Integer> getAllTaskIds(String fetchJsonStr) {
		List<Integer> allTaskIds = new ArrayList<>();
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					allTaskIds.add(jsonObj.getInt("id"));
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Task Ids. {}", e.getMessage());
		}
		return allTaskIds;
	}

	/*
	Returns New Task Id. Takes List of all the Old Ids. Checks for the id which is not present in Old Ids List and considers it as the new task id.
	 */
	public static int getNewTaskId(String fetchJsonStr, List<Integer> oldIds) {
		int newTaskId = -1;
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<Integer> allTaskIds = getAllTaskIds(fetchJsonStr);

				if (oldIds != null) {
					for (Integer taskId : allTaskIds) {
						if (!oldIds.contains(taskId)) {
							logger.info("New Task id Found.");
							newTaskId = taskId;
							break;
						}
					}
				} else {
					newTaskId = allTaskIds.get(0);
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting New Task Id. {}", e.getMessage());
		}
		return newTaskId;
	}

	public static String getStatusFromTaskJobId(String fetchJsonStr, int jobId) {
		String status = null;
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.getInt("id") == jobId) {
						status = "no status";
						if (jsonObj.has("status") && !jsonObj.isNull("status") && jsonObj.getJSONObject("status").has("name"))
							status = jsonObj.getJSONObject("status").getString("name");
						break;
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting Status of Task Job Id {}. {}", jobId, e.getStackTrace());
		}
		return status;
	}

	public static Boolean anyRecordFailedInTask(String fetchJsonStr, int jobId) {
		Boolean recordFailed = false;
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.getInt("id") == jobId) {
						if (jsonObj.getInt("failedRecordsCount") != 0)
							recordFailed = true;
						break;
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Checking if any Record failed in Job Id {}. {}", jobId, e.getStackTrace());
		}
		return recordFailed;
	}

	public static Boolean anyRecordFailedInTask(String fetchJsonStr, String taskId) {
		Boolean recordFailed = false;
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {

					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.get("id").toString().equals(taskId)) {
						if(jsonObj.get("failedRecordsCount")!=null){
							if(!jsonObj.get("failedRecordsCount").equals(null)) {
								if (jsonObj.getInt("failedRecordsCount") != 0)
									recordFailed = true;
								break;
							}
						}

					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Checking if any Record failed in Task Id {}. {}", taskId, e.getStackTrace());
		}
		return recordFailed;
	}

	public static Boolean anyRecordProcessedInTask(String fetchJsonStr, int jobId) {
		Boolean recordProcessed = false;
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.getInt("id") == jobId) {
						if (jsonObj.getInt("successfullyProcessedRecordsCount") != 0)
							recordProcessed = true;
						break;
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Checking if any Record Processed in Job Id {}. {}", jobId, e.getStackTrace());
		}
		return recordProcessed;
	}

	public static Integer getNoOfSubmittedRecordsForTask(String fetchJsonStr, int jobId) {
		Integer noOfRecordsSubmitted = -1;
		try {
			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.getInt("id") == jobId) {
						noOfRecordsSubmitted = jsonObj.getInt("submittedRecordsCount");
						break;
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting No of Submitted Records");
		}
		return noOfRecordsSubmitted;
	}

	public static Boolean ifAllRecordsFailedInTask(int taskId) {
		return ifAllRecordsFailedInTask(null, taskId);
	}

	public static Boolean ifAllRecordsFailedInTask(String fetchJsonStr, int taskId) {
		Boolean allRecordsFailed = false;
		try {
			if (fetchJsonStr == null) {
				logger.info("Hitting Fetch API to check if All Records Failed in Task {} or not.", taskId);
				Fetch fetchObj = new Fetch();
				fetchObj.hitFetch();
				fetchJsonStr = fetchObj.getFetchJsonStr();
			}

			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.getInt("id") == taskId) {
						if (jsonObj.getInt("submittedRecordsCount") == jsonObj.getInt("failedRecordsCount") &&
								jsonObj.getInt("successfullyProcessedRecordsCount") == 0)
							allRecordsFailed = true;
						break;
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while checking if All Records Failed in Task {} or not. {}", taskId, e.getStackTrace());
		}
		return allRecordsFailed;
	}

	public static Boolean ifAllRecordsPassedInTask(int taskId) {
		return ifAllRecordsPassedInTask(null, taskId);
	}

	public static Boolean ifAllRecordsPassedInTask(String fetchJsonStr, int taskId) {
		Boolean allRecordsPassed = false;
		try {
			if (fetchJsonStr == null) {
				logger.info("Hitting Fetch API to check if All Records Passed in Task {} or not.", taskId);
				Fetch fetchObj = new Fetch();
				fetchObj.hitFetch();
				fetchJsonStr = fetchObj.getFetchJsonStr();
			}

			if (ParseJsonResponse.validJsonResponse(fetchJsonStr)) {
				List<String> allTasks = getAllTasks(fetchJsonStr);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.getInt("id") == taskId) {
						if (jsonObj.getInt("submittedRecordsCount") == jsonObj.getInt("successfullyProcessedRecordsCount") &&
								jsonObj.getInt("failedRecordsCount") == 0)
							allRecordsPassed = true;
						break;
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while checking if All Records Passed in Task {} or not. {}", taskId, e.getStackTrace());
		}
		return allRecordsPassed;
	}

	public static Map<String, String> waitForScheduler(Long schedulerWaitTimeOut, Long schedulerPollingTime, int newTaskId) {
		Map<String, String> schedulerJobStatusMap = new HashMap<>();
		String jobPassed = "false";
		String errorMessage = null;
		Boolean jobFinished = false;

		logger.info("Waiting for Scheduler to Complete for Job Id {}", newTaskId);
		try {
			logger.info("Time Out for Scheduler is {} milliseconds", schedulerWaitTimeOut);
			long timeSpent = 0;

			if (newTaskId != -1) {
				logger.info("Checking if Scheduler Task has completed or not for Job Id {}", newTaskId);

				while (timeSpent < schedulerWaitTimeOut) {
					logger.info("Putting Thread on Sleep for {} milliseconds.", schedulerPollingTime);
					Thread.sleep(schedulerPollingTime);

					logger.info("Hitting Fetch API.");
					Fetch fetchObj = new Fetch();
					fetchObj.hitFetch();

					logger.info("Getting Status of Scheduler Task for Job Id {}", newTaskId);
					String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
					int percentageCompleted = UserTasksHelper.getPercentageCompletedFromTaskId(fetchObj.getFetchJsonStr(), newTaskId);
					if (newTaskStatus != null && (newTaskStatus.trim().equalsIgnoreCase("Completed") || percentageCompleted == 100)) {
						jobFinished = true;
						logger.info("Scheduler Task Completed for Job Id {}", newTaskId);
						if (UserTasksHelper.getNoOfSubmittedRecordsForTask(fetchObj.getFetchJsonStr(), newTaskId) > 0) {
							if (UserTasksHelper.ifAllRecordsPassedInTask(newTaskId)) {
								jobPassed = "true";
							} else {
								errorMessage = "Scheduler Task Completed but failed for Job Id " + newTaskId;
							}
						} else {
							errorMessage = "No Record processed for Job Id " + newTaskId;
						}
						break;
					} else {
						timeSpent += schedulerPollingTime;
						logger.info("Scheduler Task is not finished yet for Job Id {}. Time Spent: [{}]. TimeOut: [{}]", newTaskId, timeSpent, schedulerWaitTimeOut);
					}

					if (newTaskStatus != null && !newTaskStatus.trim().equalsIgnoreCase("In Progress")) {
						logger.info("Scheduler Task for Job Id {} has not been picked by Scheduler yet.", newTaskId);
					}
				}

				if (!jobFinished) {
					errorMessage = "Scheduler Task didn't finish within Time Limit for Job Id " + newTaskId;
					jobPassed = "skip";
				}
			} else {
				errorMessage = "New Task Id not defined.";
				jobPassed = "false";
			}
		} catch (Exception e) {
			logger.error("Exception while Waiting for Scheduler to Finish for Job Id {}. {}", newTaskId, e.getStackTrace());
			jobPassed = "false";
		}

		schedulerJobStatusMap.put("jobPassed", jobPassed);
		schedulerJobStatusMap.put("errorMessage", errorMessage);
		return schedulerJobStatusMap;
	}

	public static Integer getPercentageCompletedFromTaskId(String fetchResponse, int taskId) {
		Integer percentageCompleted = 0;

		try {
			if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
				List<String> allTasks = getAllTasks(fetchResponse);
				for (String task : allTasks) {
					JSONObject jsonObj = new JSONObject(task);
					if (jsonObj.getInt("id") == taskId) {
						percentageCompleted = jsonObj.getInt("percentageCompleted");
						break;
					}
				}
			} else {
				logger.error("Fetch Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while getting Percentage Completed of Task Id {}. {}", taskId, e.getStackTrace());
		}
		return percentageCompleted;
	}

	public static Integer getTaskIdFromDescription(String fetchResponse, String jobDescription) {
		try {
			List<String> allPickedTasks = getAllPickedTasks(fetchResponse);

			for (String pickedTask : allPickedTasks) {
				JSONObject pickedTaskJsonObj = new JSONObject(pickedTask);
				String description = pickedTaskJsonObj.getJSONObject("job").getString("description");

				if (description.trim().equalsIgnoreCase(jobDescription)) {
					return pickedTaskJsonObj.getInt("id");
				}
			}

			List<String> allTasksInQueue = getAllTasksInQueue(fetchResponse);

			for (String taskInQueue : allTasksInQueue) {
				JSONObject taskInQueueJsonObj = new JSONObject(taskInQueue);
				String description = taskInQueueJsonObj.getJSONObject("job").getString("description");

				if (description.trim().equalsIgnoreCase(jobDescription)) {
					return taskInQueueJsonObj.getInt("id");
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Task Id for Job Description {}. {}", jobDescription, e.getStackTrace());
		}
		return null;
	}
	public static int getRequestIdFromTaskId(Integer taskId) {
		try {

			Long initialTime = 0L;
			Long totalTimeOut = 300000L;
			Fetch fetchObj = new Fetch();
			while (initialTime < totalTimeOut) {

				fetchObj.hitFetch();

				if (ParseJsonResponse.validJsonResponse(fetchObj.getFetchJsonStr())) {

					JSONObject jsonObject = new JSONObject(fetchObj.getFetchJsonStr());
					JSONArray jsonArray = jsonObject.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
					for (Object object : jsonArray) {
						JSONObject jsonObject1 = (JSONObject) object;
						if (jsonObject1.getInt("id") == taskId) {
							return jsonObject1.getInt("requestId");
						}
					}
				} else {
					logger.error("Fetch Response is an Invalid JSON.");
				}
				Thread.sleep(5000);
				initialTime = initialTime + 5000;
			}
		} catch (Exception e) {
			logger.error("Exception while getting New Task Id. {}", e.getMessage());
		}

		return -1;
	}

	public static boolean updateBulkUpdate(String uploadFilePath, String bulkUpdateFileName, int entityTypeId, int bulkUpdateTemplateId, CustomAssert customAssert){

		Boolean bulkUpdateStatus = true;
		try {
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
			int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

			String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(uploadFilePath, bulkUpdateFileName, entityTypeId, bulkUpdateTemplateId);

			if (!uploadResponse.contains("success")) {
				customAssert.assertTrue(false, "Error while uploading bulk template file");
				return false;
			}

			Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

			String jobStatus = schedulerJob.get("jobPassed");
			if (jobStatus.equals("false")) {
				logger.info("Expected result should be Bulk Edit unsuccessful");
				customAssert.assertTrue(false, "Bulk update job run unsuccessfully");
				bulkUpdateStatus = false;
			} else {
				logger.info("Bulk Update done successfully");
			}
		}catch (Exception e){
			logger.error("Exception while doing bulk update");
			bulkUpdateStatus = false;
		}

		return bulkUpdateStatus;
	}

	public static boolean updateBulkEdit(int entityTypeId, String bulkEditPayload, CustomAssert customAssert){

		Boolean bulkUpdateStatus = true;
		try {
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
			int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

			BulkeditEdit editObj = new BulkeditEdit();
			editObj.hitBulkeditEdit(entityTypeId,bulkEditPayload);
			String editJsonStr = editObj.getBulkeditEditJsonStr();

			if(!editJsonStr.contains("success")){
				customAssert.assertTrue(false,"Error during hitting bulk edit ");
			}

			Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

			String jobStatus = schedulerJob.get("jobPassed");
			if(jobStatus.equals("false")){
				logger.info("Expected result should be Bulk Edit unsuccessful");
				customAssert.assertTrue(false,"Bulk Edit job failed");
				bulkUpdateStatus = false;
			}
			else {
				logger.info("Bulk Edit done successfully");
			}
		}catch (Exception e){
			logger.error("Exception while doing bulk edit");
			bulkUpdateStatus = false;
		}

		return bulkUpdateStatus;
	}

}
