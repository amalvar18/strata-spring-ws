package com.newco.strataws.siebel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.exception.NotFoundException;
import com.newco.strataws.model.ActivityHlpr;
import com.newco.surya.base.exceptions.AncestorSiebelUtilityException;
import com.siebel.data.SiebelBusComp;
import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelException;

public class ActivityUtility extends SiebelUtility {

    private static final Logger logger = LoggerFactory.getLogger(ActivityUtility.class);

    public ActivityUtility(SiebelConfig siebelConfig) {
        super(siebelConfig);

    }

    /**
     * Creates an activity in Siebel. Will throw an exception if Siebel mandatory fields are null or blank in
     * activityHlpr
     * 
     * @param activityHlpr
     * @return createdActId
     * @throws Exception
     * @author Amal Varghese
     */
    public String createActivity(ActivityHlpr activityHlpr) throws Exception {

        String createdActId = "";

        try {
            if (!login()) {
                throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.info(
                    "SiebelUtilityException while attempting login in ActivityUtility.createActivity()--> {}",
                    ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);
        /*logger.debug(activityHlpr.toString());*/

        /* check mandatory fields */
        if (StringUtils.isBlank(activityHlpr.getType()) || StringUtils.isBlank(activityHlpr.getStatus())
                || StringUtils.isBlank(activityHlpr.getComments())) {
            logger.info("Error in createActivity() - One or more mandatory fields is null or blank");
            String errMsg = StrataWSConstants.MANDATORY_FIELDS_EMPTY_MSG + " : "
                    + StrataWSConstants.ACT_MANDATORY_FIELDS_MSG;
            logger.error(errMsg + " Type: {} | Status: {} | Comments: {}", activityHlpr.getType(),
                    activityHlpr.getStatus(), activityHlpr.getComments());
            throw new AncestorSiebelUtilityException(errMsg);
        }

        SiebelBusObject actBusObject = null;
        SiebelBusComp actBusComp = null;
        /*SiebelBusComp lPick = null;*/

        try {
            /* Get the business Object and business component */
            actBusObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
            actBusComp = actBusObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

            actBusComp.newRecord(0);

            /*Set the mandatory field values*/

            setSimplePickWithError(actBusComp, activityHlpr.getStatus(),
                    SiebelConstants.ACT_STATUS_FIELD, SiebelConstants.PICKLIST_NAME_VAL);
            setSimplePickWithError(actBusComp, activityHlpr.getType(), SiebelConstants.ACT_TYPE_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);

            /* Truncate Activity Comments field before setting value in Siebel */
            String actComments = activityHlpr.getComments();
            if (actComments.length() > sblConfig.getMaxActCommentLength()) {
                logger.warn("Act Comment length is more than configured maximum. Will truncate before setting.");
                actComments = StringUtils.substring(actComments, 0, sblConfig.getMaxActCommentLength());
            }
            actBusComp.setFieldValue(SiebelConstants.ACT_COMMENTS_FIELD, actComments);

            /*actBusComp.setFieldValue(SiebelConstants.ACT_STATUS_FIELD, activityHlpr.getStatus());*/

            /*Set additional field values if present*/
            String actDescr = activityHlpr.getDescription();
            if (StringUtils.isNotBlank(actDescr)) {
                /* Truncate Activity Descr field before setting value in Siebel */
                if (actDescr.length() > sblConfig.getMaxActDescLength()) {

                    logger.warn("Act Descr length is more than configured maximum. Will truncate before setting.");
                    actDescr = StringUtils.substring(actDescr, 0, sblConfig.getMaxActDescLength());
                }
                actBusComp.setFieldValue(SiebelConstants.ACT_DESC_FIELD, actDescr);
            }
            if (StringUtils.isNotBlank(activityHlpr.getPrimaryOwner())) {
                setSimplePickWithError(actBusComp, activityHlpr.getPrimaryOwner(),
                        SiebelConstants.ACT_OWNER_FIELD, SiebelConstants.CONTACT_PICKLIST_LOGIN_NAME_VAL);
                /*actBusComp.setFieldValue(SiebelConstants.ACT_OWNER_FIELD, activityHlpr.getPrimaryOwner());*/
            }

            if (StringUtils.isNotBlank(activityHlpr.getParentSrNum())) {
                logger.info("Creating activity for SR#: {}", activityHlpr.getParentSrNum());
                setSimplePickWithError(actBusComp, activityHlpr.getParentSrNum(),
                        SiebelConstants.SR_NUM_FIELD_NAME, SiebelConstants.SR_NUM_FIELD_NAME);
                /*actBusComp
                        .setFieldValue(SiebelConstants.SR_NUM_FIELD_NAME, activityHlpr.getParentSrNum());*/
            }

            writeOrDeleteNewRecord(actBusComp);

            createdActId = actBusComp.getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD);
            logger.info("Created activity with Id: {}", createdActId);
            logger.info("**********************************************************");
            activityHlpr.setActivityId(createdActId);
        } catch (SiebelException sblEx) {
            logger.info("Error--> SiebelException in createActivity()" + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            /*releaseBusComp(lPick);*/
            releaseBusComp(actBusComp);
            releaseBusObject(actBusObject);
            logoff();
        }
        return createdActId;
    }

    /**
     * Creates an activity in Siebel. Will throw an exception if Siebel mandatory fields are null or blank in
     * activityHlpr
     *
     * @param activityHlpr
     * @return actId
     * @throws Exception
     * @author Amal Varghese
     */
    public String updateActivity(ActivityHlpr activityHlpr) throws Exception {

        String actId = activityHlpr.getActivityId();
        String updatedActId = "";

        if (StringUtils.isBlank(actId)) {
            String errMsg = StrataWSConstants.ACTIVITY_VALUE + " " + StrataWSConstants.ID_VALUE + " "
                    + StrataWSConstants.NULL_OR_BLANK_MSG;
            logger.error(errMsg);
            throw new Exception(errMsg);
        }

        try {
            if (!login()) {
                throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        } catch (AncestorSiebelUtilityException ae) {
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);
        logger.debug(activityHlpr.toString());

        /* check mandatory fields */
        if (StringUtils.isBlank(activityHlpr.getType()) || StringUtils.isBlank(activityHlpr.getStatus())
                || StringUtils.isBlank(activityHlpr.getComments())) {
            logger.info("Error in createActivity() - One or more mandatory fields is null or blank");
            String errMsg = StrataWSConstants.MANDATORY_FIELDS_EMPTY_MSG + " : "
                    + StrataWSConstants.ACT_MANDATORY_FIELDS_MSG;
            logger.error(errMsg + " Type: {} | Status: {} | Comments: {}", activityHlpr.getType(),
                    activityHlpr.getStatus(), activityHlpr.getComments());
            throw new AncestorSiebelUtilityException(errMsg);
        }

        SiebelBusObject actBusObject = null;
        SiebelBusComp actBusComp = null;

        try {
            /* Get the business Object and business component */
            actBusObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
            actBusComp = actBusObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

            actBusComp.setViewMode(3);
            actBusComp.clearToQuery();
            actBusComp.setSearchSpec(SiebelConstants.SBL_ROW_ID_FIELD, actId);
            actBusComp.executeQuery(true);

            if (actBusComp.firstRecord()) {

                if (StringUtils.isNotBlank(activityHlpr.getStatus())) {
                    setSimplePickWithError(actBusComp, activityHlpr.getStatus(),
                            SiebelConstants.ACT_STATUS_FIELD, SiebelConstants.PICKLIST_NAME_VAL);
                    /*actBusComp.setFieldValue(SiebelConstants.ACT_STATUS_FIELD, activityHlpr.getStatus());*/
                }

                if (StringUtils.isNotBlank(activityHlpr.getType())) {
                    setSimplePickWithError(actBusComp, activityHlpr.getType(),
                            SiebelConstants.ACT_TYPE_FIELD, SiebelConstants.PICKLIST_NAME_VAL);
                    /*actBusComp.setFieldValue(SiebelConstants.ACT_TYPE_FIELD, activityHlpr.getType());*/
                }

                String comments = activityHlpr.getComments();
                if (StringUtils.isNotBlank(comments)) {

                    if (comments.length() > sblConfig.getMaxActCommentLength()) {

                        logger.warn("Act Comment length is more than configured maximum. Will truncate before setting.");
                        comments = StringUtils.substring(comments, 0, sblConfig.getMaxActCommentLength());
                    }
                    actBusComp.setFieldValue(SiebelConstants.ACT_COMMENTS_FIELD, comments);
                    /*actBusComp.setFieldValue(SiebelConstants.ACT_COMMENTS_FIELD,
                            activityHlpr.getComments());*/
                }

                String descr = activityHlpr.getDescription();
                if (StringUtils.isNotBlank(descr)) {

                    if (descr.length() > sblConfig.getMaxActDescLength()) {

                        logger.warn("Act Description length is more than configured maximum. Will truncate before setting.");
                        descr = StringUtils.substring(descr, 0, sblConfig.getMaxActDescLength());
                    }
                    actBusComp.setFieldValue(SiebelConstants.ACT_DESC_FIELD, descr);
                }
                if (StringUtils.isNotBlank(activityHlpr.getPrimaryOwner())) {
                    setSimplePickWithError(actBusComp, activityHlpr.getPrimaryOwner(),
                            SiebelConstants.ACT_OWNER_FIELD,
                            SiebelConstants.CONTACT_PICKLIST_LOGIN_NAME_VAL);
                    /*actBusComp.setFieldValue(SiebelConstants.ACT_OWNER_FIELD,
                            activityHlpr.getPrimaryOwner());*/
                }
                if (StringUtils.isNotBlank(activityHlpr.getParentSrNum())) {
                    setSimplePickWithError(actBusComp, activityHlpr.getParentSrNum(),
                            SiebelConstants.SR_NUM_FIELD_NAME, SiebelConstants.SR_NUM_FIELD_NAME);
                    /*actBusComp.setFieldValue(SiebelConstants.SR_NUM_FIELD_NAME,
                            activityHlpr.getParentSrNum());*/
                }
                actBusComp.writeRecord();

                updatedActId = actBusComp.getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD);
                logger.info("Activity Id [" + updatedActId + "] updated");
                logger.info("**********************************************************");

            } else {
                /*Activity with Id was not found in Siebel */
                logger.error("Act Id [{}] was not found in Siebel", actId);
                throw new NotFoundException(StrataWSConstants.ACTIVITY_VALUE, true);
            }

        } catch (SiebelException sblEx) {
            logger.info("Error--> SiebelException in updateActivity()" + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            actBusComp.undoRecord();
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(actBusComp);
            releaseBusObject(actBusObject);
            logoff();
        }

        return updatedActId;
    }

    public ActivityHlpr fetchActivity(String actId) throws Exception {

        ActivityHlpr activityHlpr = new ActivityHlpr();

        if (!sblConnected) {
            try {
                if (!login()) {
                    throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
                }
            } catch (AncestorSiebelUtilityException ae) {
                throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        }

        if (StringUtils.isBlank(actId)) {
            logger.info(StrataWSConstants.ACTIVITY_VALUE + StrataWSConstants.NULL_OR_BLANK_MSG);
            return activityHlpr;
        }
        SiebelBusObject actBusObject = null;
        SiebelBusComp actBusComp = null;

        try {
            /* Get the business Object and business component */
            actBusObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
            actBusComp = actBusObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

            actBusComp.activateField(SiebelConstants.SBL_ROW_ID_FIELD);
            actBusComp.setViewMode(3);
            actBusComp.clearToQuery();
            actBusComp.setSearchSpec(SiebelConstants.SBL_ROW_ID_FIELD, actId);

            actBusComp.executeQuery(true);

            if (actBusComp.firstRecord()) {

                activityHlpr.setActivityId(actBusComp.getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD));
                activityHlpr.setType(actBusComp.getFieldValue(SiebelConstants.ACT_TYPE_FIELD));
                activityHlpr.setStatus(actBusComp.getFieldValue(SiebelConstants.ACT_STATUS_FIELD));
                activityHlpr.setPrimaryOwner(actBusComp.getFieldValue(SiebelConstants.ACT_OWNER_FIELD));
                activityHlpr.setDescription(actBusComp.getFieldValue(SiebelConstants.ACT_DESC_FIELD));
                activityHlpr.setComments(actBusComp.getFieldValue(SiebelConstants.ACT_COMMENTS_FIELD));
                activityHlpr.setParentSrNum(actBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));

                String actDueDt = actBusComp.getFormattedFieldValue(SiebelConstants.ACT_DUE_DATE_FIELD);
                /*String actDueDt2 = actBusComp.getFieldValue(SiebelConstants.ACT_DUE_DATE_FIELD);*/

                /*String[] parsePatterns = { SiebelConstants.SBL_DATE_TIME_PATTERN };
                Date dueDateDt = DateUtils.parseDate(actDueDt, parsePatterns);
                 logger.debug(dueDateDt.toString());*/

                logger.debug("Act Due Date from Siebel: {}", actDueDt);

                logger.debug("Parsing using format string: {}", SiebelConstants.SBL_DATE_TIME_PATTERN);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                        SiebelConstants.SBL_DATE_TIME_PATTERN, Locale.ENGLISH);

                LocalDateTime dateTime = LocalDateTime.parse(actDueDt, formatter);
                /*DateTimeFormatter formatter2 = DateTimeFormatter
                .ofPattern(SiebelConstants.SBL_DATE_TIME_PATTERN2);*/
                /*LocalDateTime dateTime = LocalDateTime.parse(actDueDt2, formatter2);*/

                logger.debug("Act Due Date from after formatting: {}", dateTime.toString());
                activityHlpr.setDueDate(dateTime);
                /*activityHlpr.setDueDate(actBusComp
                        .getFormattedFieldValue(SiebelConstants.ACT_DUE_DATE_FIELD));*/
            } else {
                logger.error("Act Id [{}] was not found in Siebel", actId);
                throw new NotFoundException(StrataWSConstants.ACTIVITY_VALUE, true);
            }
        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in fetchActivity() " + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(actBusComp);
            releaseBusObject(actBusObject);
            logoff();
        }
        /*logger.debug(activityHlpr.toString());*/
        return activityHlpr;
    }

    /**
     * Checks whether a given activity exists in Siebel based on activity Id.
     * 
     * @param actId
     * @return true/false
     * @throws Exception
     * @author Amal Varghese
     */
    public Boolean checkActValidity(String actId) throws Exception {

        SiebelBusObject actBusObject = null;
        SiebelBusComp actBusComp = null;

        Boolean validActFlag = false;

        if (!login()) {
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        } else {

            logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

            if (StringUtils.isBlank(actId)) {
                logger.info("Activity is null or blank.");
                return false;
            }

            try {
                actBusObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
                actBusComp = actBusObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

                actBusComp.activateField(SiebelConstants.SBL_ROW_ID_FIELD);
                actBusComp.setViewMode(3);
                actBusComp.clearToQuery();
                actBusComp.setSearchSpec(SiebelConstants.SBL_ROW_ID_FIELD, actId);

                actBusComp.executeQuery(true);

                if (actBusComp.firstRecord()) {

                    validActFlag = true;
                    logger.info("Activity Id [" + actId + "] present in Siebel");
                    logger.info("*****************************************************");
                } else {
                    logger.info("Activity Id [{}] was not found in Siebel", actId);
                    validActFlag = false;
                }
            } catch (SiebelException sblEx) {
                logger.error("Error in checkActValidity() " + sblEx.getErrorMessage());
                logSiebelException(sblEx);
                throw new AncestorSiebelUtilityException(sblEx);
            } finally {
                releaseBusComp(actBusComp);
                releaseBusObject(actBusObject);
                logoff();
            }
        }
        return validActFlag;
    }

    /*public List<String> fetchAllSrActivityIds(String srNumber) {

        List<String> activityIdList = new ArrayList<String>();

        return activityIdList;
    }*/
}
