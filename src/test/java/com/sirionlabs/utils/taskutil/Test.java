package com.sirionlabs.utils.taskutil;

import com.sirionlabs.utils.executerserviceutil.TaskExecutionImpl;
import com.sirionlabs.utils.executerserviceutil.TaskImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Test {

	public static void main(String[] args) {
		List<TaskImpl> taskList = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			TaskImpl taskCreationObj = new ShowPageTaskImpl("validateShowPermissionForId", 45 + "", "dbID_" + i, "Show_Page_Task");
			taskList.add(taskCreationObj);
		}
		
		/*for (int i=0; i<100; i++) {
			TaskImpl taskCreationObj = new EntityTaskImpl();			
			taskList.add(taskCreationObj.createTask("verifyentity", 45+"", "dbID"+i, "taskType"+i));
		}*/

		try {
			TaskExecutionImpl.executeAnyTask(taskList);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
