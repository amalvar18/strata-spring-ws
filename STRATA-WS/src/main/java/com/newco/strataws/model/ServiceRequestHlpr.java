package com.newco.strataws.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Component
public class ServiceRequestHlpr {

    @JsonIgnore
    private String sblRowId;

    private String srNumber;

    private String clientAccount;

    @Value("${sbl.sr.open.status}")
    private String sblSrStatus;

    private String sblSrOwner;

    private String domain;

    private String process;

    private String subProcess;

    @Value("${default.sr.type}")
    private String type;

    @Value("${default.sr.source}")
    private String source;

    @Value("${default.sr.priority}")
    private String priority;

    @Value("${default.sr.severity}")
    private String severity;

    private String shortDescription;

    private String description;

    private String srContactEmpId;

    private String srTargetContactLastName;

    private String srDueDate;

    private Integer volume;

    private List<String> targetContacts;

    private List<String> activityIds;

    @Deprecated
    private List<ActivityHlpr> activities;

    public String getSblRowId() {
        return sblRowId;
    }

    public void setSblRowId(String sblRowId) {
        this.sblRowId = sblRowId;
    }

    public String getSrNumber() {
        return srNumber;
    }

    public void setSrNumber(String srNumber) {
        this.srNumber = srNumber;
    }

    public String getClientAccount() {
        return clientAccount;
    }

    public void setClientAccount(String clientAccount) {
        this.clientAccount = clientAccount;
    }

    public String getSblSrStatus() {
        return sblSrStatus;
    }

    public void setSblSrStatus(String sblSrStatus) {
        this.sblSrStatus = sblSrStatus;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getSubProcess() {
        return subProcess;
    }

    public void setSubProcess(String subProcess) {
        this.subProcess = subProcess;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getSrContactEmpId() {
        return srContactEmpId;
    }

    public void setSrContactEmpId(String srContactEmpId) {
        this.srContactEmpId = srContactEmpId;
    }

    public String getSblSrOwner() {
        return sblSrOwner;
    }

    public void setSblSrOwner(String sblSrOwner) {
        this.sblSrOwner = sblSrOwner;
    }

    public String getSrTargetContactLastName() {
        return srTargetContactLastName;
    }

    public void setSrTargetContactLastName(String srTargetContactLastName) {
        this.srTargetContactLastName = srTargetContactLastName;
    }

    public String getSrDueDate() {
        return srDueDate;
    }

    public void setSrDueDate(String srDueDate) {
        this.srDueDate = srDueDate;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public List<String> getTargetContacts() {
        return targetContacts;
    }

    public void setTargetContacts(List<String> targetContacts) {
        this.targetContacts = targetContacts;
    }

    public List<String> getActivityIds() {
        return activityIds;
    }

    public void setActivityIds(List<String> activityIds) {
        this.activityIds = activityIds;
    }

    @Deprecated
    public List<ActivityHlpr> getActivities() {
        return activities;
    }

    @Deprecated
    public void setActivities(List<ActivityHlpr> activities) {
        this.activities = activities;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ServiceRequestHlpr [sblRowId=").append(sblRowId).append(", srNumber=")
                .append(srNumber).append(", clientAccount=").append(clientAccount)
                .append(", sblSrStatus=").append(sblSrStatus).append(", sblSrOwner=").append(sblSrOwner)
                .append(", domain=").append(domain).append(", process=").append(process)
                .append(", subProcess=").append(subProcess).append(", type=").append(type)
                .append(", source=").append(source).append(", priority=").append(priority)
                .append(", severity=").append(severity).append(", shortDescription=")
                .append(shortDescription).append(", description=").append(description)
                .append(", srContactEmpId=").append(srContactEmpId).append(", srTargetContactLastName=")
                .append(srTargetContactLastName).append(", srDueDate=").append(srDueDate)
                .append(", volume=").append(volume).append(", targetContacts=").append(targetContacts)
                .append(", activityIds=").append(activityIds).append("]");
        return builder.toString();
    }

}
