package com.demo.voucher.rest.dto;

import java.io.Serializable;
import java.util.Date;

public class BatchExecutionResponse implements Serializable {
    private long executionId;
    private String jobName;
    private String batchStatus;
    private String exitStatus;
    private Date startTime;
    private Date endTime;

    public BatchExecutionResponse() {
    }

    public BatchExecutionResponse(long executionId, String jobName, String batchStatus, String exitStatus, Date startTime, Date endTime) {
        this.executionId = executionId;
        this.jobName = jobName;
        this.batchStatus = batchStatus;
        this.exitStatus = exitStatus;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(long executionId) {
        this.executionId = executionId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
