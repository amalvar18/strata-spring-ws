package com.newco.strataws.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

public class ActivityHlpr {

    private String activityId;

    private String type;

    private String status;

    private String primaryOwner;

    private String description;

    private String comments;

    private String parentSrNum;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime dueDate;

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimaryOwner() {
        return primaryOwner;
    }

    public void setPrimaryOwner(String primaryOwner) {
        this.primaryOwner = primaryOwner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getParentSrNum() {
        return parentSrNum;
    }

    public void setParentSrNum(String parentSrNum) {
        this.parentSrNum = parentSrNum;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ActivityHlpr [activityId=").append(activityId).append(", type=").append(type)
                .append(", status=").append(status).append(", primaryOwner=").append(primaryOwner)
                .append(", description=").append(description).append(", comments=").append(comments)
                .append(", dueDate=").append(dueDate).append("]");
        return builder.toString();
    }

}
