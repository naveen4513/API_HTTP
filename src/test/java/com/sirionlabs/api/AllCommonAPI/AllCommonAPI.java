package com.sirionlabs.api.AllCommonAPI;

public class AllCommonAPI {

    public String getScheduledLargeReportAPI(int  id)
    {
        return "/scheduleLargeReport/create?id="+id;
    }

    public String getListRendererListScheduleData(int listId)
    {
        return "/listRenderer/list/"+listId+"/scheduledata/0";
    }

    public String  getScheduleLargeReportDownloadReport(String taskId) {
        return "/scheduleLargeReport/downloadReport?id=" + taskId;
    }

    public String getTodoClusteringWorkFlowStatus()
    {
        return "/todoClustering/workflowStatus?";
    }


    public String getTodoClusteringSavedWFStatus()
    {
        return "/todoClustering/savedWFstatus?";
    }

    public String getTodoClusteringSaveStatus() {
        return "/todoClustering/saveStatus?";
    }

    public String getPendingActionsDaily(int entityTypeID)
    {
        return "/pending-actions/daily/"+entityTypeID+"/?";
    }
    public String getPendingActionsWeekly(int entityTypeID)
    {
        return "/pending-actions/weekly/"+entityTypeID+"/?";
    }
}
