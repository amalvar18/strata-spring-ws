package com.newco.strataws.siebel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.exception.NotFoundException;
import com.newco.strataws.model.ActivityHlpr;
import com.newco.strataws.model.ServiceRequestHlpr;
import com.newco.surya.base.exceptions.AncestorSiebelUtilityException;
import com.siebel.data.SiebelBusComp;
import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelException;
import com.siebel.data.SiebelPropertySet;

public class SvReqUtility extends SiebelUtility {

    private static final Logger logger = LoggerFactory.getLogger(SvReqUtility.class);

    public SvReqUtility(SiebelConfig siebelConfig) {
        super(siebelConfig);
    }

    public String createSR(ServiceRequestHlpr svreq) throws Exception {

        String createdSRNumber = "";

        try {
            if (!login()) {
                throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.error(
                    "SiebelUtilityException while attempting login in SvReqUtility.createSR()--> {}", ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

        logger.debug(svreq.toString());

        /* check mandatory fields */
        if (StringUtils.isBlank(svreq.getDomain()) || StringUtils.isBlank(svreq.getProcess())
                || StringUtils.isBlank(svreq.getSubProcess())
                || StringUtils.isBlank(svreq.getSblSrStatus())
                || StringUtils.isBlank(svreq.getShortDescription())
                || StringUtils.isBlank(svreq.getDescription())) {

            throw new AncestorSiebelUtilityException(StrataWSConstants.MANDATORY_FIELDS_EMPTY_MSG + " : "
                    + StrataWSConstants.SR_MANDATORY_FIELDS_MSG);
        }

        /* throw an Exception if Emp Id is null or blank */
        if (StringUtils.isBlank(svreq.getSrContactEmpId())) {
            throw new AncestorSiebelUtilityException(StrataWSConstants.EMP_ID_NULL_OR_BLANK_MSG);
        }

        SiebelBusObject srBusObject = null;
        SiebelBusComp srBusComp = null;
        /*SiebelBusComp lMvg = null;*/
        SiebelBusComp lPick = null;

        try {
            // ----Get the business Object
            srBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            srBusComp = srBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            srBusComp.newRecord(0);

            lPick = srBusComp.getPicklistBusComp(SiebelConstants.CONTACT_LASTNAME_FIELD);
            lPick.clearToQuery();

            lPick.activateField(SiebelConstants.CONTACT_PAY_1_FIELD);
            lPick.activateField(SiebelConstants.CONTACT_PAY_2_FIELD);
            lPick.activateField(SiebelConstants.CONTACT_PAY_3_FIELD);

            lPick.setSearchSpec(SiebelConstants.CONTACT_EMP_ID_FIELD, svreq.getSrContactEmpId());

            String clientAccount = svreq.getClientAccount();
            /*logger.info("clientAccount--> " + clientAccount);*/

            if (StringUtils.isNotBlank(clientAccount)) {
                logger.info("Searching for contact in Siebel with emp Id: {} and account: {}",
                        svreq.getSrContactEmpId(), clientAccount);
                lPick.setSearchSpec("Account", escSearchSpec(clientAccount));
            } else {
                logger.info("Searching for contact in Siebel with emp Id {}", svreq.getSrContactEmpId());
            }

            lPick.setViewMode(9);
            lPick.setSortSpec(SiebelConstants.CONTACT_SORT_SPEC);
            lPick.executeQuery(true);

            if (lPick.firstRecord()) {
                lPick.pick();
            } else {
                /* Should never happen because the Account from the SR must exist */
                throw new NotFoundException("Contact", true, StrataWSConstants.SBL_CONTACT_NOT_FOUND_MSG);
                /*throw new AncestorSiebelUtilityException(StrataWSConstants.SBL_CONTACT_NOT_FOUND_MSG);*/
            }

            setSimplePickWithError(srBusComp, svreq.getSblSrStatus(), SiebelConstants.STATUS_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);
            setSimplePickWithError(srBusComp, svreq.getDomain(), SiebelConstants.DOMAIN_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);
            setSimplePickWithError(srBusComp, svreq.getProcess(), SiebelConstants.PROCESS_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);
            setSimplePickWithError(srBusComp, svreq.getSubProcess(), SiebelConstants.SUBPROCESS_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);

            setSimplePickWithError(srBusComp, svreq.getType(), SiebelConstants.SRTYPE_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);
            setSimplePickWithError(srBusComp, svreq.getSblSrOwner(), SiebelConstants.SROWNER_FIELD,
                    SiebelConstants.CONTACT_PICKLIST_LOGIN_NAME_VAL);
            setSimplePickWithError(srBusComp, svreq.getSource(), SiebelConstants.SR_SOURCE_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);
            setSimplePickWithError(srBusComp, svreq.getPriority(), SiebelConstants.PRIORITY_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);
            setSimplePickWithError(srBusComp, svreq.getSeverity(), SiebelConstants.SEVERITY_FIELD,
                    SiebelConstants.PICKLIST_NAME_VAL);

            /* Truncate SR Short Description and Description fields before setting value in Siebel */
            String srShortDescr = svreq.getShortDescription();
            if (srShortDescr.length() > sblConfig.getMaxSrShortDescLength()) {
                logger.warn("SR Short Descr length is more than configured maximum. Will truncate before setting.");
                srShortDescr = StringUtils
                        .substring(srShortDescr, 0, sblConfig.getMaxSrShortDescLength());
            }
            srBusComp.setFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD, srShortDescr);

            String srDescr = svreq.getDescription();
            if (srDescr.length() > sblConfig.getMaxSrLongDescLength()) {
                logger.warn("SR Descr length is more than configured maximum. Will truncate before setting.");
                srDescr = StringUtils.substring(srDescr, 0, sblConfig.getMaxSrLongDescLength());
            }
            srBusComp.setFieldValue(SiebelConstants.SR_LONG_DESC_FIELD, srDescr);

            /*Insert SR Volume only if present in request*/
            if (svreq.getVolume() != null) {
                String srVolume = svreq.getVolume().toString();
                logger.debug("Volume: " + srVolume);
                if (StringUtils.isNotBlank(srVolume)) {
                    srBusComp.setFieldValue(SiebelConstants.SR_VOLUME_FIELD, srVolume);
                }
            }

            writeOrDeleteNewRecord(srBusComp);

            createdSRNumber = srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME);
            logger.info("SR Number [" + createdSRNumber + "] created");
            logger.info("**********************************************************");
            svreq.setSrNumber(createdSRNumber);
        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in class {}::createSR()", logger.getName());
            logSiebelException(sblEx);
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(srBusComp);
            releaseBusObject(srBusObject);
            logoff();
        }
        return createdSRNumber;
    }

    /**
     * Fetch SR details from Siebel based on SR number.
     * 
     * @param srNumber
     * @return ServiceRequestHlpr
     * @throws Exception
     * @author Amal Varghese
     */
    public ServiceRequestHlpr getSrDetails(String srNumber) throws Exception {

        ServiceRequestHlpr svReqHlpr = new ServiceRequestHlpr();

        try {
            if (!login()) {
                throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.error(
                    "SiebelUtilityException while attempting login in SvReqUtility.getSrDetails()--> {}",
                    ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

        if (StringUtils.isBlank(srNumber)) {
            logger.info("SR number is null or blank.");
            return svReqHlpr;
        }

        SiebelBusObject srBusObject = null;
        SiebelBusComp srBusComp = null;
        SiebelBusComp lPick = null;

        try {
            srBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            srBusComp = srBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

            /*SiebelPropertySet srPropSet = dataBean.newPropertySet();
            addSrFieldsToPropertySet(srPropSet);*/

            srBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
            srBusComp.activateField(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME);
            srBusComp.activateField(SiebelConstants.SR_VOLUME_FIELD);
            srBusComp.setViewMode(3);
            srBusComp.clearToQuery();
            srBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME, srNumber);

            srBusComp.executeQuery(true);

            if (srBusComp.firstRecord()) {

                svReqHlpr.setSrNumber(srNumber);
                svReqHlpr.setClientAccount(srBusComp.getFieldValue(SiebelConstants.ACCOUNT_FIELD));
                svReqHlpr.setSblSrStatus(srBusComp.getFieldValue(SiebelConstants.STATUS_FIELD));
                svReqHlpr.setSblSrOwner(srBusComp.getFieldValue(SiebelConstants.SROWNER_FIELD));
                svReqHlpr.setDomain(srBusComp.getFieldValue(SiebelConstants.DOMAIN_FIELD));
                svReqHlpr.setProcess(srBusComp.getFieldValue(SiebelConstants.PROCESS_FIELD));
                svReqHlpr.setSubProcess(srBusComp.getFieldValue(SiebelConstants.SUBPROCESS_FIELD));
                svReqHlpr.setType(srBusComp.getFieldValue(SiebelConstants.SRTYPE_FIELD));
                svReqHlpr.setSource(srBusComp.getFieldValue(SiebelConstants.SR_SOURCE_FIELD));
                svReqHlpr.setPriority(srBusComp.getFieldValue(SiebelConstants.PRIORITY_FIELD));
                svReqHlpr.setSeverity(srBusComp.getFieldValue(SiebelConstants.SEVERITY_FIELD));
                svReqHlpr.setShortDescription(srBusComp
                        .getFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD));
                svReqHlpr.setDescription(srBusComp.getFieldValue(SiebelConstants.SR_LONG_DESC_FIELD));
                svReqHlpr.setSrContactEmpId(srBusComp.getFieldValue(SiebelConstants.SR_CONTACT_ID_FIELD));
                svReqHlpr.setSblRowId(srBusComp.getFieldValue(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME));
                svReqHlpr.setSrDueDate(srBusComp
                        .getFormattedFieldValue(SiebelConstants.SR_DUE_DATE_FIELD));
                /*  svReqHlpr.setVolume(Integer.parseInt(srBusComp
                          .getFormattedFieldValue(SiebelConstants.SR_VOLUME_FIELD)));*/
                /*svReqHlpr.setVolume(Integer.parseInt(srBusComp
                        .getFieldValue(SiebelConstants.SR_VOLUME_FIELD)));*/
                if (StringUtils.isNotBlank(srBusComp.getFieldValue(SiebelConstants.SR_VOLUME_FIELD))) {
                    svReqHlpr.setVolume(Integer.parseInt(srBusComp
                            .getFieldValue(SiebelConstants.SR_VOLUME_FIELD)));
                }
                /*svReqHlpr.setVolume(NumberUtils.toInt(
                        srBusComp.getFieldValue(SiebelConstants.SR_VOLUME_FIELD), 0));*/
                svReqHlpr.setTargetContacts(fetchSRTargetContactIds(srNumber));
                svReqHlpr.setActivityIds(fetchSrActivityIds(srNumber));

                /* logger.info("*****************************************************");*/
                logger.info("SR Number [" + srNumber + "] details successfully fetched");
                logger.info("*****************************************************");

            } else {
                logger.error("SR# [{}] was not found in Siebel", srNumber);
                throw new NotFoundException(StrataWSConstants.SR_VALUE, true);
            }
        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in getSrDetails() " + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(lPick);
            releaseBusComp(srBusComp);
            releaseBusObject(srBusObject);
            logoff();
        }
        return svReqHlpr;
    }

    public String updateSR(String srNumber, ServiceRequestHlpr svreq) throws Exception {

        String updatedSRNum = "";
        /*Boolean updatedFlag = false;
        String pickErrMsg = "";*/

        try {
            if (!login()) {
                throw new AncestorSiebelUtilityException(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.error(
                    "SiebelUtilityException while attempting login in SvReqUtility.updateSR()--> {}", ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        if (!sblConnected) {
            throw new AncestorSiebelUtilityException("Not Connected");
        }

        logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

        logger.info("Attempting to update SR number: " + srNumber);
        logger.debug(svreq.toString());

        /* check mandatory fields */
        if (StringUtils.isBlank(svreq.getDomain()) || StringUtils.isBlank(svreq.getProcess())
                || StringUtils.isBlank(svreq.getSubProcess())
                || StringUtils.isBlank(svreq.getSblSrStatus())
                || StringUtils.isBlank(svreq.getShortDescription())
                || StringUtils.isBlank(svreq.getDescription())) {

            throw new AncestorSiebelUtilityException(StrataWSConstants.MANDATORY_FIELDS_EMPTY_MSG + " - "
                    + StrataWSConstants.SR_MANDATORY_FIELDS_MSG);
        }

        /* throw an Exception if Emp Id is null or blank */
        if (StringUtils.isBlank(svreq.getSrContactEmpId())) {
            throw new AncestorSiebelUtilityException(StrataWSConstants.EMP_ID_NULL_OR_BLANK_MSG);
        }

        SiebelBusObject srBusObject = null;
        SiebelBusComp srBusComp = null;
        /* SiebelBusComp lMvg = null;*/
        SiebelBusComp lPick = null;

        try {
            // ----Get the business Object
            srBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            srBusComp = srBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

            /* throw an Exception if Emp Id is null or blank */
            if (StringUtils.isBlank(svreq.getSrContactEmpId())) {
                throw new AncestorSiebelUtilityException(StrataWSConstants.EMP_ID_NULL_OR_BLANK_MSG);
            }

            /* SiebelPropertySet ps = dataBean.newPropertySet();
             ps.setProperty(SiebelConstants.SR_NUM_FIELD_NAME, "");
             ps.setProperty(SiebelConstants.ACCOUNT_FIELD, "");
             ps.setProperty(SiebelConstants.STATUS_FIELD, "");
             ps.setProperty(SiebelConstants.SROWNER_FIELD, "");
             ps.setProperty(SiebelConstants.DOMAIN_FIELD, "");
             ps.setProperty(SiebelConstants.PROCESS_FIELD, "");
             ps.setProperty(SiebelConstants.SUBPROCESS_FIELD, "");
             ps.setProperty(SiebelConstants.SRTYPE_FIELD, "");
             ps.setProperty(SiebelConstants.SR_SOURCE_FIELD, "");
             ps.setProperty(SiebelConstants.PRIORITY_FIELD, "");
             ps.setProperty(SiebelConstants.SEVERITY_FIELD, "");
             ps.setProperty(SiebelConstants.SR_SHORT_DESC_FIELD, "");
             ps.setProperty(SiebelConstants.SR_LONG_DESC_FIELD, "");*/

            SiebelPropertySet srPropSet = dataBean.newPropertySet();
            addSrFieldsToPropertySet(srPropSet);

            srBusComp.activateMultipleFields(srPropSet);
            srBusComp.setViewMode(3);
            srBusComp.clearToQuery();
            srBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME, srNumber);
            srBusComp.executeQuery(true);

            if (srBusComp.firstRecord()) {

                /* Check if SR is being reopened */

                String currentSRStatus = srBusComp.getFieldValue(SiebelConstants.STATUS_FIELD);
                String updatedSRStatus = svreq.getSblSrStatus();
                String closedSRStatus = SiebelConstants.SBL_CLOSED_SR_STATUS;
                logger.debug("Current SR status is {} | New status is {} ", currentSRStatus,
                        updatedSRStatus);

                if (currentSRStatus.equalsIgnoreCase(closedSRStatus)) {
                    throw new AncestorSiebelUtilityException(StrataWSConstants.SR_CLOSED_MSG);
                }

                logger.info("Updating SR: [{}]", srNumber);
                lPick = srBusComp.getPicklistBusComp(SiebelConstants.CONTACT_LASTNAME_FIELD);
                lPick.clearToQuery();

                /*lPick.activateField(SiebelConstants.CONTACT_EMP_ID_FIELD);*/
                lPick.activateField(SiebelConstants.CONTACT_PAY_1_FIELD);
                lPick.activateField(SiebelConstants.CONTACT_PAY_2_FIELD);
                lPick.activateField(SiebelConstants.CONTACT_PAY_3_FIELD);

                lPick.setSearchSpec(SiebelConstants.CONTACT_EMP_ID_FIELD, svreq.getSrContactEmpId());

                String clientAccount = svreq.getClientAccount();
                if (StringUtils.isNotBlank(clientAccount)) {
                    logger.info("Searching for contact in Siebel with emp Id: {} and account: {}",
                            svreq.getSrContactEmpId(), clientAccount);
                    lPick.setSearchSpec("Account", escSearchSpec(clientAccount));
                } else {
                    logger.info("Searching for contact in Siebel with emp Id {}",
                            svreq.getSrContactEmpId());
                }

                lPick.setViewMode(9);
                lPick.setSortSpec(SiebelConstants.CONTACT_SORT_SPEC);
                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                } else {
                    /* Should never happen because the Account from the SR must exist */
                    throw new AncestorSiebelUtilityException(StrataWSConstants.SBL_CONTACT_NOT_FOUND_MSG);
                }

                setSimplePickWithError(srBusComp, svreq.getSblSrStatus(), SiebelConstants.STATUS_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(srBusComp, svreq.getDomain(), SiebelConstants.DOMAIN_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(srBusComp, svreq.getProcess(), SiebelConstants.PROCESS_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(srBusComp, svreq.getSubProcess(),
                        SiebelConstants.SUBPROCESS_FIELD, SiebelConstants.PICKLIST_NAME_VAL);

                setSimplePickWithError(srBusComp, svreq.getType(), SiebelConstants.SRTYPE_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(srBusComp, svreq.getSblSrOwner(), SiebelConstants.SROWNER_FIELD,
                        "Login Name");
                setSimplePickWithError(srBusComp, svreq.getSource(), SiebelConstants.SR_SOURCE_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(srBusComp, svreq.getPriority(), SiebelConstants.PRIORITY_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(srBusComp, svreq.getSeverity(), SiebelConstants.SEVERITY_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);

                /* Truncate SR Short Description and Description fields before setting value in Siebel */
                String srShortDescr = svreq.getShortDescription();
                if (srShortDescr.length() > sblConfig.getMaxSrShortDescLength()) {
                    logger.warn("SR Short Descr length is more than configured maximum. Will truncate before setting.");
                    srShortDescr = StringUtils.substring(srShortDescr, 0,
                            sblConfig.getMaxSrShortDescLength());
                }

                srBusComp.setFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD, srShortDescr);

                String srDescr = svreq.getDescription();
                if (srDescr.length() > sblConfig.getMaxSrLongDescLength()) {
                    logger.warn("SR Descr length is more than configured maximum. Will truncate before setting.");
                    srDescr = StringUtils.substring(srDescr, 0, sblConfig.getMaxSrLongDescLength());
                }
                srBusComp.setFieldValue(SiebelConstants.SR_LONG_DESC_FIELD, srDescr);

                /*Update SR Volume only if present in request*/
                if (svreq.getVolume() != null) {
                    String srVolume = svreq.getVolume().toString();
                    logger.debug("Volume: " + srVolume);
                    if (StringUtils.isNotBlank(srVolume)) {
                        srBusComp.setFieldValue(SiebelConstants.SR_VOLUME_FIELD, srVolume);
                    }
                }

                srBusComp.writeRecord();

                updatedSRNum = srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME);
                logger.info("SR Number [" + updatedSRNum + "] updated");
                logger.info("**********************************************************");
                svreq.setSrNumber(updatedSRNum);

            } else {
                logger.info("Error in updateSR()--> " + StrataWSConstants.SR_VALUE
                        + StrataWSConstants.NOT_FOUND_IN_SBL_MSG);
            }

        } catch (SiebelException sblEx) {
            logger.error("Error in updateSR() " + sblEx.toString());
            sblEx.printStackTrace();
            srBusComp.undoRecord();
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(srBusComp);
            releaseBusObject(srBusObject);
            logoff();
        }
        return updatedSRNum;
    }

    /**
     * Checks whether a given SR exists in Siebel based on SR number.
     * 
     * @param srNumber
     * @return true/false
     * @throws Exception
     * @author Amal Varghese
     */
    public Boolean checkSrValidity(String srNumber) throws Exception {

        SiebelBusObject lBusObject = null;
        SiebelBusComp lBusComp = null;

        Boolean validSRFlag = false;

        if (!login()) {
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        } else {

            logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

            if (StringUtils.isBlank(srNumber)) {
                logger.info("SR number is null or blank.");
                return false;
            }

            try {
                lBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
                lBusComp = lBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

                lBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
                lBusComp.setViewMode(3);
                lBusComp.clearToQuery();
                lBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME, srNumber);

                lBusComp.executeQuery(true);

                if (lBusComp.firstRecord()) {

                    validSRFlag = true;
                    logger.info("SR Number [" + srNumber + "] present in Siebel");
                    logger.info("*****************************************************");
                } else {
                    logger.info("SR# {} was not found in Siebel", srNumber);
                    validSRFlag = false;
                }
            } catch (SiebelException sblEx) {
                logger.error("Error in checkSrValidity() " + sblEx.toString());
                sblEx.printStackTrace();
                throw new AncestorSiebelUtilityException(sblEx);
            } finally {
                releaseBusComp(lBusComp);
                releaseBusObject(lBusObject);
                logoff();
            }
        }
        return validSRFlag;
    }

    public List<ServiceRequestHlpr> querySRFields(Map<String, String> queryFieldsMap) throws Exception {

        List<ServiceRequestHlpr> srList = new ArrayList<ServiceRequestHlpr>();

        /*Check if map is empty. */
        // TODO: Use org.apache.commons.collections4.MapUtils
        if (queryFieldsMap.isEmpty() || queryFieldsMap.size() < 3) {
            throw new Exception(StrataWSConstants.QUERY_MAP_EMPTY_ERR);
        }

        try {
            if (!login()) {
                throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.error(
                    "SiebelUtilityException while attempting login in SvReqUtility.querySRFields()--> {}",
                    ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        SiebelBusObject srBusObject = null;
        SiebelBusComp srBusComp = null;
        /*SiebelBusComp lPick = null;*/

        try {
            srBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            srBusComp = srBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

            SiebelPropertySet srPropSet = dataBean.newPropertySet();
            /*addSrFieldsToPropertySet(srPropSet);*/

            /*Add all fields in query map for activation*/
            for (String fieldname : queryFieldsMap.keySet()) {
                srPropSet.setProperty(fieldname, "");
            }
            srBusComp.activateMultipleFields(srPropSet);
            srBusComp.setViewMode(3);
            srBusComp.clearToQuery();

            /*add each key value pair in query map to search spec using lambda expr*/
            /*queryFieldsMap.forEach((k, v) -> srBusComp.setSearchSpec(k, v));*/
            for (Entry<String, String> entry : queryFieldsMap.entrySet()) {

                String key = entry.getKey();
                String val = entry.getValue();
                logger.debug("Setting search spec: Field name = {}, Value = {}", key, val);
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(val)) {
                    srBusComp.setSearchSpec(key, val);
                }
            }
            srBusComp.setSortSpec(SiebelConstants.SR_DUE_DATE_FIELD);
            srBusComp.executeQuery(true);

            boolean srRecord = srBusComp.firstRecord();

            if (!srRecord) {
                logger.debug("SRs not found in Siebel");
                /*throw new NotFoundException(StrataWSConstants.SR_VALUE, true);*/
            }

            while (srRecord) {

                /* boolean srRecord = srBusComp.firstRecord();*/
                ServiceRequestHlpr svReqHlpr = new ServiceRequestHlpr();
                String srNumber = srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME);

                svReqHlpr.setSrNumber(srNumber);
                svReqHlpr.setClientAccount(srBusComp.getFieldValue(SiebelConstants.ACCOUNT_FIELD));
                svReqHlpr.setSblSrStatus(srBusComp.getFieldValue(SiebelConstants.STATUS_FIELD));
                svReqHlpr.setSblSrOwner(srBusComp.getFieldValue(SiebelConstants.SROWNER_FIELD));
                svReqHlpr.setDomain(srBusComp.getFieldValue(SiebelConstants.DOMAIN_FIELD));
                svReqHlpr.setProcess(srBusComp.getFieldValue(SiebelConstants.PROCESS_FIELD));
                svReqHlpr.setSubProcess(srBusComp.getFieldValue(SiebelConstants.SUBPROCESS_FIELD));
                svReqHlpr.setType(srBusComp.getFieldValue(SiebelConstants.SRTYPE_FIELD));
                svReqHlpr.setSource(srBusComp.getFieldValue(SiebelConstants.SR_SOURCE_FIELD));
                svReqHlpr.setPriority(srBusComp.getFieldValue(SiebelConstants.PRIORITY_FIELD));
                svReqHlpr.setSeverity(srBusComp.getFieldValue(SiebelConstants.SEVERITY_FIELD));
                svReqHlpr.setShortDescription(srBusComp
                        .getFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD));
                svReqHlpr.setDescription(srBusComp.getFieldValue(SiebelConstants.SR_LONG_DESC_FIELD));
                svReqHlpr.setSrContactEmpId(srBusComp.getFieldValue(SiebelConstants.SR_CONTACT_ID_FIELD));
                svReqHlpr.setSblRowId(srBusComp.getFieldValue(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME));
                svReqHlpr.setSrDueDate(srBusComp
                        .getFormattedFieldValue(SiebelConstants.SR_DUE_DATE_FIELD));
                svReqHlpr.setVolume(Integer.parseInt(srBusComp
                        .getFieldValue(SiebelConstants.SR_VOLUME_FIELD)));

                svReqHlpr.setTargetContacts(fetchSRTargetContactIds(srNumber));
                svReqHlpr.setActivityIds(fetchSrActivityIds(srNumber));

                srList.add(svReqHlpr);
                logger.info("SR Number [" + srNumber + "] details successfully fetched");
                try {
                    srRecord = srBusComp.nextRecord();
                } catch (SiebelException sblex) {
                    if (sblex.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE) {
                        srRecord = false;
                        logger.info("EOF - Iterated through all SR records successfully");
                    } else {
                        logException(sblex);
                    }
                }
            }
        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in querySrFields() " + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            /*Handle field not found in Siebel*/
            if (sblEx.getErrorCode() == SiebelConstants.FIELD_NOT_FOUND_SBL_ERR_CODE) {
                Pattern p = Pattern.compile("Field\\s'(.*?)'", Pattern.MULTILINE);
                Matcher m = p.matcher(sblEx.getErrorMessage());
                String text = "";
                while (m.find()) {
                    text = m.group(1);
                }
                throw new NotFoundException("Field '" + text + "'", true);
            } else {
                throw new AncestorSiebelUtilityException(sblEx);
            }
        } finally {
            /* releaseBusComp(lPick);*/
            releaseBusComp(srBusComp);
            releaseBusObject(srBusObject);
            logoff();
        }

        return srList;
    }

    public List<String> fetchSRTargetContactIds(String srNumber) throws Exception {

        try {
            if (!sblConnected) {
                if (!login()) {
                    throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
                }
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.error(
                    "SiebelUtilityException while attempting login in SvReqUtility.getSrDetails()--> {}",
                    ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        /*logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);*/

        List<String> targetContactIdList = new ArrayList<String>();
        SiebelBusObject srBusObject = null;
        SiebelBusComp srBusComp = null;
        SiebelBusComp mvgBusComp = null;

        try {
            srBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            srBusComp = srBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

            srBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
            srBusComp.activateField(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME);
            srBusComp.setViewMode(3);
            srBusComp.clearToQuery();
            srBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME, srNumber);

            srBusComp.executeQuery(true);

            if (srBusComp.firstRecord()) {
                mvgBusComp = srBusComp.getMVGBusComp(SiebelConstants.SR_TARGET_CONTACT_FIELD);
                mvgBusComp.clearToQuery();
                mvgBusComp.executeQuery(true);

                boolean targetEmp = mvgBusComp.firstRecord();
                if (!targetEmp) {
                    logger.info("No target contacts found for SR#[{}]", srNumber);
                }
                while (targetEmp) {

                    targetContactIdList.add(mvgBusComp
                            .getFieldValue(SiebelConstants.CONTACT_EMP_ID_FIELD));
                    try {
                        targetEmp = mvgBusComp.nextRecord();
                    } catch (SiebelException sblex) {
                        if (sblex.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE) {
                            targetEmp = false;
                        } else {
                            logException(sblex);
                        }
                    }
                }
            } else {
                logger.error("SR# [{}] was not found in Siebel", srNumber);
                throw new NotFoundException(StrataWSConstants.SR_VALUE, true);
            }
        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in getSrDetails() " + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(mvgBusComp);
            releaseBusComp(srBusComp);
            releaseBusObject(srBusObject);
        }
        return targetContactIdList;
    }

    /**
     * Returns list of activity Ids for an SR
     * 
     * @param srNumber
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public List<String> fetchSrActivityIds(String srNumber) throws Exception {

        List<String> activityIdList = new ArrayList<String>();

        try {
            if (!sblConnected) {
                if (!login()) {
                    throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
                }
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.error(
                    "SiebelUtilityException while attempting login in SvReqUtility.getSrDetails()--> {}",
                    ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        SiebelBusObject actBusObject = null;
        SiebelBusComp actBusComp = null;

        try {
            /* Get the business Object and business component */
            actBusObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
            actBusComp = actBusObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

            actBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
            actBusComp.setViewMode(3);
            actBusComp.clearToQuery();
            actBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME, srNumber);

            actBusComp.executeQuery(true);

            boolean actRecords = actBusComp.firstRecord();
            if (!actRecords) {
                logger.info("No activities found for SR#[{}]", srNumber);
            }
            while (actRecords) {
                activityIdList.add(actBusComp.getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD));
                try {
                    actRecords = actBusComp.nextRecord();
                } catch (SiebelException sblex) {
                    if (sblex.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE) {
                        actRecords = false;
                    } else {
                        logException(sblex);
                    }
                }
            }

        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in fetchSrActivityIds() " + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(actBusComp);
            releaseBusObject(actBusObject);
            /*logoff();*/
        }

        return activityIdList;
    }

    /**
     * @deprecated Use querySRFields instead
     * @param subProcess
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public List<ServiceRequestHlpr> getAllSr(String subProcess) throws Exception {
        List<ServiceRequestHlpr> allSRHlpr = new ArrayList();

        try {
            if (!login()) {
                throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
            }
        } catch (AncestorSiebelUtilityException ae) {
            logger.error(
                    "SiebelUtilityException while attempting login in SvReqUtility.getSrDetails()--> {}",
                    ae);
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        }

        logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

        /*
        * if (StringUtils.isBlank(srNumber)) {
        * logger.info("SR number is null or blank."); return allSRHlpr; }
        */
        SiebelBusObject srBusObject = null;
        SiebelBusComp srBusComp = null;
        SiebelBusComp lPick = null;

        try {
            srBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            srBusComp = srBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

            /*
            * SiebelPropertySet srPropSet = dataBean.newPropertySet();
            * addSrFieldsToPropertySet(srPropSet);
            */
            srBusComp.activateField(SiebelConstants.SR_TARGET_CONTACT_FIELD);
            srBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
            srBusComp.activateField(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME);
            srBusComp.activateField(SiebelConstants.SUBPROCESS_FIELD);
            srBusComp.activateField("Commit Time");
            srBusComp.setViewMode(3);
            srBusComp.clearToQuery();
            srBusComp.setSearchSpec(SiebelConstants.SUBPROCESS_FIELD, subProcess);

            srBusComp.setSearchSpec(SiebelConstants.STATUS_FIELD, "Open");
            srBusComp.setSortSpec("Commit Time");
            srBusComp.executeQuery(true);
            if (srBusComp.firstRecord()) {
                ServiceRequestHlpr svReqHlpr = new ServiceRequestHlpr();
                svReqHlpr.setSrNumber(srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));
                SiebelBusComp actBusComp = null;

                SiebelBusObject actBusObject = null;
                actBusObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
                actBusComp = srBusObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

                actBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
                actBusComp.setViewMode(3);
                actBusComp.clearToQuery();
                actBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME,
                        srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));

                actBusComp.executeQuery(true);
                List<ActivityHlpr> activityHlprs = new ArrayList();
                if (actBusComp.firstRecord()) {
                    ActivityHlpr activityHlpr = new ActivityHlpr();
                    activityHlpr
                            .setActivityId(actBusComp.getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD));
                    activityHlpr.setType(actBusComp.getFieldValue(SiebelConstants.ACT_TYPE_FIELD));
                    activityHlpr.setStatus(actBusComp.getFieldValue(SiebelConstants.ACT_STATUS_FIELD));
                    activityHlpr.setPrimaryOwner(actBusComp
                            .getFieldValue(SiebelConstants.ACT_OWNER_FIELD));
                    activityHlpr.setDescription(actBusComp.getFieldValue(SiebelConstants.ACT_DESC_FIELD));
                    activityHlpr
                            .setComments(actBusComp.getFieldValue(SiebelConstants.ACT_COMMENTS_FIELD));
                    activityHlpr.setParentSrNum(actBusComp
                            .getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));

                    String actDueDt = actBusComp
                            .getFormattedFieldValue(SiebelConstants.ACT_DUE_DATE_FIELD);

                    /*logger.debug("Act Due Date from Siebel: {}", actDueDt);

                    logger.debug("Parsing using format string: {}",
                                  SiebelConstants.SBL_DATE_TIME_PATTERN);*/
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                            SiebelConstants.SBL_DATE_TIME_PATTERN, Locale.ENGLISH);

                    LocalDateTime dateTime = LocalDateTime.parse(actDueDt, formatter);

                    /*logger.debug("Act Due Date from after formatting: {}",
                                  dateTime.toString());*/
                    activityHlpr.setDueDate(dateTime);
                    activityHlprs.add(activityHlpr);

                } else {
                    // logger.error("Act Id [{}] was not found in Siebel",
                    // actId);
                    throw new NotFoundException(StrataWSConstants.ACTIVITY_VALUE, true);
                }
                try {
                    while (actBusComp.nextRecord()) {
                        ActivityHlpr activityHlpr = new ActivityHlpr();
                        activityHlpr.setActivityId(actBusComp
                                .getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD));
                        activityHlpr.setType(actBusComp.getFieldValue(SiebelConstants.ACT_TYPE_FIELD));
                        activityHlpr
                                .setStatus(actBusComp.getFieldValue(SiebelConstants.ACT_STATUS_FIELD));
                        activityHlpr.setPrimaryOwner(actBusComp
                                .getFieldValue(SiebelConstants.ACT_OWNER_FIELD));
                        activityHlpr.setDescription(actBusComp
                                .getFieldValue(SiebelConstants.ACT_DESC_FIELD));
                        activityHlpr.setComments(actBusComp
                                .getFieldValue(SiebelConstants.ACT_COMMENTS_FIELD));
                        activityHlpr.setParentSrNum(actBusComp
                                .getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));
                        String actDueDt = actBusComp
                                .getFormattedFieldValue(SiebelConstants.ACT_DUE_DATE_FIELD);

                        /*logger.debug("Act Due Date from Siebel: {}", actDueDt);

                        logger.debug("Parsing using format string: {}",
                                      SiebelConstants.SBL_DATE_TIME_PATTERN);*/
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                                SiebelConstants.SBL_DATE_TIME_PATTERN, Locale.ENGLISH);

                        LocalDateTime dateTime = LocalDateTime.parse(actDueDt, formatter);

                        /*     logger.debug("Act Due Date from after formatting: {}",
                                             dateTime.toString());*/
                        activityHlpr.setDueDate(dateTime);
                        activityHlprs.add(activityHlpr);
                    }
                } catch (Exception e) {

                }

                svReqHlpr.setActivities(activityHlprs);
                srBusComp.getFieldValue("Exult Target Contact");
                // srBusComp.getFieldValue("TARGET_EMP_ID");
                // System.out.println(srBusComp.getFieldValue("Exult Target Contact"));
                svReqHlpr.setSrTargetContactLastName(srBusComp.getFieldValue("Exult Target Contact"));
                svReqHlpr.setSrDueDate(srBusComp.getFieldValue("Commit Time"));
                // srBusComp.getFieldValue("newco Effective Date");
                svReqHlpr.setClientAccount(srBusComp.getFieldValue(SiebelConstants.ACCOUNT_FIELD));
                svReqHlpr.setSblSrStatus(srBusComp.getFieldValue(SiebelConstants.STATUS_FIELD));
                svReqHlpr.setSblSrOwner(srBusComp.getFieldValue(SiebelConstants.SROWNER_FIELD));
                svReqHlpr.setDomain(srBusComp.getFieldValue(SiebelConstants.DOMAIN_FIELD));
                svReqHlpr.setProcess(srBusComp.getFieldValue(SiebelConstants.PROCESS_FIELD));
                svReqHlpr.setSubProcess(srBusComp.getFieldValue(SiebelConstants.SUBPROCESS_FIELD));
                svReqHlpr.setType(srBusComp.getFieldValue(SiebelConstants.SRTYPE_FIELD));
                svReqHlpr.setSource(srBusComp.getFieldValue(SiebelConstants.SR_SOURCE_FIELD));
                svReqHlpr.setPriority(srBusComp.getFieldValue(SiebelConstants.PRIORITY_FIELD));
                svReqHlpr.setSeverity(srBusComp.getFieldValue(SiebelConstants.SEVERITY_FIELD));
                svReqHlpr.setShortDescription(srBusComp
                        .getFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD));
                svReqHlpr.setDescription(srBusComp.getFieldValue(SiebelConstants.SR_LONG_DESC_FIELD));
                svReqHlpr.setSrContactEmpId(srBusComp.getFieldValue(SiebelConstants.SR_CONTACT_ID_FIELD));
                svReqHlpr.setSblRowId(srBusComp.getFieldValue(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME));
                svReqHlpr.setVolume(Integer.parseInt(srBusComp
                        .getFieldValue(SiebelConstants.SR_VOLUME_FIELD)));
                if (srBusComp.getFieldValue(SiebelConstants.STATUS_FIELD).equalsIgnoreCase("open")) {
                    allSRHlpr.add(svReqHlpr);
                }

                System.out.println("SR Number  =>> "
                        + srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));
            } else {
                logger.error("SR# [{}] was not found in Siebel for Sub Process Field :: ",
                        "Automation On-Demand");
                throw new NotFoundException(
                        "SR# [{}] was not found in Siebel for Sub Process Field :: Automation On-Demand",
                        true);
            }
            try {
                while (srBusComp.nextRecord()) {
                    ServiceRequestHlpr svReqHlpr = new ServiceRequestHlpr();
                    svReqHlpr.setSrNumber(srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));
                    svReqHlpr.setSrTargetContactLastName(srBusComp
                            .getFieldValue(SiebelConstants.SR_TARGET_CONTACT_FIELD));
                    svReqHlpr.setSrNumber(srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));
                    SiebelBusComp actBusComp = null;

                    SiebelBusObject actBusObject = null;
                    actBusObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
                    actBusComp = srBusObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

                    actBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
                    actBusComp.setViewMode(3);
                    actBusComp.clearToQuery();
                    actBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME,
                            srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));

                    actBusComp.executeQuery(true);
                    List<ActivityHlpr> activityHlprs = new ArrayList<ActivityHlpr>();
                    if (actBusComp.firstRecord()) {
                        ActivityHlpr activityHlpr = new ActivityHlpr();
                        activityHlpr.setActivityId(actBusComp
                                .getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD));
                        activityHlpr.setType(actBusComp.getFieldValue(SiebelConstants.ACT_TYPE_FIELD));
                        activityHlpr
                                .setStatus(actBusComp.getFieldValue(SiebelConstants.ACT_STATUS_FIELD));
                        activityHlpr.setPrimaryOwner(actBusComp
                                .getFieldValue(SiebelConstants.ACT_OWNER_FIELD));
                        activityHlpr.setDescription(actBusComp
                                .getFieldValue(SiebelConstants.ACT_DESC_FIELD));
                        activityHlpr.setComments(actBusComp
                                .getFieldValue(SiebelConstants.ACT_COMMENTS_FIELD));
                        activityHlpr.setParentSrNum(actBusComp
                                .getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));

                        String actDueDt = actBusComp
                                .getFormattedFieldValue(SiebelConstants.ACT_DUE_DATE_FIELD);

                        /*     logger.debug("Act Due Date from Siebel: {}", actDueDt);

                               logger.debug("Parsing using format string: {}",
                                             SiebelConstants.SBL_DATE_TIME_PATTERN);*/
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                                SiebelConstants.SBL_DATE_TIME_PATTERN, Locale.ENGLISH);

                        LocalDateTime dateTime = LocalDateTime.parse(actDueDt, formatter);

                        /*     logger.debug("Act Due Date from after formatting: {}",
                                             dateTime.toString());*/
                        activityHlpr.setDueDate(dateTime);
                        activityHlprs.add(activityHlpr);

                    } else {
                        // logger.error("Act Id [{}] was not found in Siebel",
                        // actId);
                        throw new NotFoundException(StrataWSConstants.ACTIVITY_VALUE, true);
                    }
                    try {
                        while (actBusComp.nextRecord()) {
                            ActivityHlpr activityHlpr = new ActivityHlpr();
                            activityHlpr.setActivityId(actBusComp
                                    .getFieldValue(SiebelConstants.SBL_ROW_ID_FIELD));
                            activityHlpr
                                    .setType(actBusComp.getFieldValue(SiebelConstants.ACT_TYPE_FIELD));
                            activityHlpr.setStatus(actBusComp
                                    .getFieldValue(SiebelConstants.ACT_STATUS_FIELD));
                            activityHlpr.setPrimaryOwner(actBusComp
                                    .getFieldValue(SiebelConstants.ACT_OWNER_FIELD));
                            activityHlpr.setDescription(actBusComp
                                    .getFieldValue(SiebelConstants.ACT_DESC_FIELD));
                            activityHlpr.setComments(actBusComp
                                    .getFieldValue(SiebelConstants.ACT_COMMENTS_FIELD));
                            activityHlpr.setParentSrNum(actBusComp
                                    .getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));
                            String actDueDt = actBusComp
                                    .getFormattedFieldValue(SiebelConstants.ACT_DUE_DATE_FIELD);

                            /*logger.debug("Act Due Date from Siebel: {}", actDueDt);

                            logger.debug("Parsing using format string: {}",
                                          SiebelConstants.SBL_DATE_TIME_PATTERN);*/
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                                    SiebelConstants.SBL_DATE_TIME_PATTERN, Locale.ENGLISH);

                            LocalDateTime dateTime = LocalDateTime.parse(actDueDt, formatter);

                            /*     logger.debug("Act Due Date from after formatting: {}",
                                                 dateTime.toString());*/
                            activityHlpr.setDueDate(dateTime);
                            activityHlprs.add(activityHlpr);
                        }
                    } catch (Exception e) {

                    }

                    // svReqHlpr.setSrTargetContact(srBusComp.getFieldValue("Exult Target Contact"));
                    svReqHlpr.setActivities(activityHlprs);
                    svReqHlpr.setSrDueDate(srBusComp.getFieldValue("Commit Time"));
                    svReqHlpr.setSrTargetContactLastName(srBusComp
                            .getFieldValue(SiebelConstants.SR_TARGET_CONTACT_FIELD));
                    svReqHlpr.setClientAccount(srBusComp.getFieldValue(SiebelConstants.ACCOUNT_FIELD));
                    svReqHlpr.setSblSrStatus(srBusComp.getFieldValue(SiebelConstants.STATUS_FIELD));
                    svReqHlpr.setSblSrOwner(srBusComp.getFieldValue(SiebelConstants.SROWNER_FIELD));
                    svReqHlpr.setDomain(srBusComp.getFieldValue(SiebelConstants.DOMAIN_FIELD));
                    svReqHlpr.setProcess(srBusComp.getFieldValue(SiebelConstants.PROCESS_FIELD));
                    svReqHlpr.setSubProcess(srBusComp.getFieldValue(SiebelConstants.SUBPROCESS_FIELD));
                    svReqHlpr.setType(srBusComp.getFieldValue(SiebelConstants.SRTYPE_FIELD));
                    svReqHlpr.setSource(srBusComp.getFieldValue(SiebelConstants.SR_SOURCE_FIELD));
                    svReqHlpr.setPriority(srBusComp.getFieldValue(SiebelConstants.PRIORITY_FIELD));
                    svReqHlpr.setSeverity(srBusComp.getFieldValue(SiebelConstants.SEVERITY_FIELD));
                    svReqHlpr.setShortDescription(srBusComp
                            .getFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD));
                    svReqHlpr.setDescription(srBusComp.getFieldValue(SiebelConstants.SR_LONG_DESC_FIELD));
                    svReqHlpr.setSrContactEmpId(srBusComp
                            .getFieldValue(SiebelConstants.SR_CONTACT_ID_FIELD));
                    svReqHlpr.setSblRowId(srBusComp
                            .getFieldValue(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME));

                    if (srBusComp.getFieldValue(SiebelConstants.STATUS_FIELD).equalsIgnoreCase("open")) {
                        allSRHlpr.add(svReqHlpr);
                    }
                    System.out.println("SR Number  =>> "
                            + srBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME));
                    // srBusComp.nextRecord();
                }
            } catch (Exception e) {

            }
        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in getSrDetails() " + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(lPick);
            releaseBusComp(srBusComp);
            releaseBusObject(srBusObject);
            logoff();
        }
        return allSRHlpr;
    }
}
