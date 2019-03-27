package com.newco.strataws.siebel;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.model.ServiceRequestHlpr;
import com.newco.surya.base.exceptions.AncestorSiebelUtilityException;
import com.siebel.data.SiebelBusComp;
import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;
import com.siebel.data.SiebelPropertySet;

public class SiebelUtility {

    /*@Autowired
    private Environment env;*/

    private static final Logger logger = LoggerFactory.getLogger(SiebelUtility.class);

    protected SiebelDataBean dataBean;
    protected SiebelConfig sblConfig;

    protected boolean sblConnected = false;

    /*protected final int SBL8_EOF_ERRORCODE = 7668105;*/

    /*
     * @Autowired ServiceRequestHlpr svreq;
     */

    public SiebelUtility(SiebelConfig siebelConfig) {
        this.sblConfig = siebelConfig;
    }

    public boolean login() throws Exception {

        /* System.out.println("Attempting Siebel Login"); */
        logger.info("SiebelUtility --> Attempting Siebel Login");
        System.setProperty("file.encoding", "Cp1252");
        try {

            dataBean = new SiebelDataBean();
            String sblServerURL = sblConfig.getSblServerURL();
            String login = sblConfig.getLogin();
            /*String sealedFile = sblConfig.getSealedFile();
            String secretFile = sblConfig.getSecretFile();*/
            String passwrd = sblConfig.getSblPasswrd();

            /*logger.debug("Using sealed File: {} and secret file: {}", sealedFile, secretFile);
            String passwrd = decryptPassword(secretFile, sealedFile);*/

            logger.debug("Attempting to login to Siebel server @ {}", sblServerURL);
            logger.debug("Using Login: {}", login);
            /* logger.debug("Passwrd: {}", passwrd);*/

            dataBean.login(sblServerURL, login, passwrd, "enu");
            sblConnected = true;

            logger.info("SiebelUtility --> Siebel login successful");
            logger.info("Position name: {}", dataBean.positionName());
        } catch (SiebelException sblEx) {
            logger.error("Error--> SiebelException in login() " + sblEx.getErrorMessage());
            logSiebelException(sblEx);
            /*If login fails, return LoginException so that it can be handled in controller*/
            if (sblEx.getErrorCode() == SiebelConstants.AUTH_FAILURE_SBL_ERR_CODE) {
                throw new LoginException(StrataWSConstants.SBL_LOGIN_FAILED_MSG + " - "
                        + sblEx.getErrorMessage());
            }
            /* All other Siebel exceptions */
            else {
                throw new AncestorSiebelUtilityException(sblEx);
            }
        } catch (Exception e) {
            logSiebelException(e);
            /*System.out.println(e);*/
        }
        return sblConnected;
    }

    public void logoff() throws AncestorSiebelUtilityException {

        /*logger.info("*******************************************************");
        logger.info("logoff() --> Attempting Siebel logoff ... ");*/

        if (sblConnected) {
            try {
                if (dataBean != null) {
                    logger.info("logoff() --> connected and databean is not null - attempting logoff");
                    dataBean.logoff();
                    dataBean = null;
                }
                sblConnected = false;
            } catch (SiebelException ex) {
                logSiebelException(ex);
                throw new AncestorSiebelUtilityException(ex);
            } catch (Exception ex1) {
                logSiebelException(ex1);
            } finally {
                sblConnected = false;
            }
        }

        logger.info("logoff() --> Siebel logoff successful ");
        logger.info("************************************************");
    }

    /*
     * public void logException(SiebelException pSiebelExp) {
     * System.out.println("------------------------------------");
     * System.out.println("Detailed Message: " +
     * pSiebelExp.getDetailedMessage()); System.out.println("Error Message: " +
     * pSiebelExp.getErrorMessage()); System.out.println("Error Code: " +
     * pSiebelExp.getErrorCode()); System.out.println("Message: " +
     * pSiebelExp.getMessage()); System.out.println("Major Number: " +
     * pSiebelExp.getMajorNumber()); System.out.println("Minor Number: " +
     * pSiebelExp.getMinorNumber());
     * System.out.println("------------------------------------"); }
     */

    public void logException(SiebelException pSiebelExp) {
        logger.error("------------------------------------");
        logger.error("Detailed Message: " + pSiebelExp.getDetailedMessage());
        logger.error("Error Message: " + pSiebelExp.getErrorMessage());
        logger.error("Error Code: " + pSiebelExp.getErrorCode());
        logger.error("Message: " + pSiebelExp.getMessage());
        logger.error("Major Number: " + pSiebelExp.getMajorNumber());
        logger.error("Minor Number: " + pSiebelExp.getMinorNumber());
        logger.error("------------------------------------");
    }

    protected void logSiebelException(Exception pExp) {
        if (pExp instanceof SiebelException) {
            logException((SiebelException) pExp);
        } else {
            logger.error("SiebelUtilityException: ", pExp);
            /* System.out.println(pExp); */
        }
    }

    public static String escSearchSpec(String pStr) {
        String lRetVal = pStr;
        if (!StringUtils.isBlank(pStr)) {
            StringBuffer buf = new StringBuffer();
            buf.append("'");
            buf.append(pStr.replaceAll("'", "''"));
            buf.append("'");
            lRetVal = buf.toString();
        }
        return lRetVal;
    }

    /**
     * Support function for Siebel pick lists Given the BC and the field and the value, pick it from the PickList. This
     * is not forgiving. If the value is not on the list, it throws an error
     * 
     * @param pickBC
     *            Bus Comp, pValue Value, pFieldName Field Name, pPickField Pick Field
     * @throws SiebelException
     *             , AncestorSiebelUtilityException
     */
    protected void setSimplePickWithError(SiebelBusComp pickBC, String pValue, String pFieldName,
            String pPickField) throws SiebelException, AncestorSiebelUtilityException {
        SiebelBusComp lPick = null;
        try {
            if (pValue.trim().length() > 0) {
                lPick = pickBC.getPicklistBusComp(pFieldName);
                lPick.clearToQuery();
                // ----Use ~= so that it ignores the case
                lPick.setSearchSpec(pPickField, " ~= '" + pValue + "'");
                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                } else {
                    /* If it's not picked throw error */
                    String pickErrMsg = "Could not pick value [" + pValue + "] for field [" + pFieldName
                            + "]";
                    logger.error("Error in setSimplePickWithError()--> " + pickErrMsg);
                    throw new AncestorSiebelUtilityException(pickErrMsg);
                }
            }
        } finally {
            lPick.release();
        }
    }

    /**
     * Support function for Siebel pick lists. Given the BC and the field and the value, pick it from the PickList. This
     * is very forgiving if the value is not on the list it doesn't error
     * 
     * @param pBC
     *            Bus Comp, pValue Value, pFieldName Field Name, pPickField Pick Field
     * @throws SiebelException
     */
    protected void setSimplePick(SiebelBusComp pBC, String pValue, String pFieldName, String pPickField)
            throws SiebelException {
        SiebelBusComp lPick = null;
        try {
            if (pValue.trim().length() > 0) {
                lPick = pBC.getPicklistBusComp(pFieldName);
                lPick.clearToQuery();
                // ----Use ~= so that it ignores the case
                lPick.setSearchSpec(pPickField, " ~= '" + pValue + "'");
                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                } else {
                    // Debug
                    logger.info("-------------------------------");
                    logger.info(pFieldName + ": Could not pick value [" + pValue + "] for pick field ["
                            + pPickField + "]");

                    lPick.clearToQuery();
                    lPick.executeQuery(true);
                    boolean lRetrieval = lPick.firstRecord();
                    try {
                        logger.info("Displaying all values for field [" + pFieldName + "]");
                        while (lRetrieval) {
                            logger.debug(pPickField + " : " + lPick.getFieldValue(pPickField) + " ("
                                    + lPick.getFieldValue("Value") + ")");
                            // lRetrieval = lPick.nextRecord();
                            try {
                                lRetrieval = lPick.nextRecord();
                            } catch (SiebelException sble) {
                                if (sble.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE)
                                    lRetrieval = false;
                            }

                        }
                        logger.info("-------------------------------");

                    } catch (Exception e) {
                        logger.error("Error in setSimpleQuery debug routine");
                        throw e;
                    }
                }
            }
        } finally {
            if (lPick != null) {
                lPick.release();
            }
        }
    }

    /**
     * This is a utility function to emulate undo of record in case of exception while writing the record. Siebel does
     * not automatically revert the changes during writing if an exception arises.
     * 
     * @author Abhijit Suvarna on Oct 17, 2008
     * @param pComp
     * @throws SiebelException
     */
    public static void writeOrDeleteNewRecord(SiebelBusComp pComp) throws SiebelException {
        if (pComp != null) {
            try {
                pComp.writeRecord();
            } catch (SiebelException e) {
                try {
                    logger.info("SiebelUtility::writeOrDeleteNewRecord() --> Exception while writing record, hence trying to delete");
                    pComp.deleteRecord();
                    logger.info("SiebelUtility::writeOrDeleteNewRecord() --> Delete record successful");
                } catch (Exception e1) {
                    logger.error("Error in writeOrDeleteNewRecord()" + e1.toString());
                }
                throw e;
            }
        }
    }

    public Boolean checkActivityAttachFlag(String activityId) throws Exception {

        if (StringUtils.isBlank(activityId)) {
            logger.error("Activity Id is Null or Blank", new Exception("Activity Id is Null or Blank"));
            throw new Exception("Activity Id is Null or Blank");
        }

        SiebelBusObject busObject = null;
        SiebelBusComp actBusComp = null;
        boolean hasAttachFlag = false;

        /* login */
        if (!login()) {
            logger.error("SiebelUtilty Exception--> ", new Exception("Siebel Login Failure"));
            throw new Exception("Siebel Login Failure");
        }

        busObject = dataBean.getBusObject(SiebelConstants.INBOUND_BO_NAME);
        actBusComp = busObject.getBusComp(SiebelConstants.ACTIVITY_BC_NAME);

        if (busObject == null || actBusComp == null) {
            logger.warn("SiebelUtilty--> Siebel BusComp or BusObject is null");
        }
        try {
            actBusComp.activateField(SiebelConstants.EMAIL_ATTACH_FLAG_FIELD);
            actBusComp.setViewMode(3);
            actBusComp.clearToQuery();

            actBusComp.setSearchSpec("Id", activityId);
            actBusComp.executeQuery(true);
            if (actBusComp.firstRecord()) {
                String actAttachFlagValue = actBusComp
                        .getFieldValue(SiebelConstants.EMAIL_ATTACH_FLAG_FIELD);

                if (StringUtils.equalsIgnoreCase("Y", actAttachFlagValue)) {
                    hasAttachFlag = true;
                    logger.info("Attachment Flag is Yes for activity.");
                } else {
                    logger.info("Attachment Flag is No for activity.");
                }
            }
        } catch (SiebelException se) {
            logSiebelException(se);
        } finally {
            releaseBusComp(actBusComp);
            releaseBusObject(busObject);
        }
        return hasAttachFlag;
    }

    /**
     * Releases the Bus Component
     * 
     * @author Abhijit Suvarna on Aug 22, 2008
     * @param pBusComp
     */
    public static void releaseBusComp(SiebelBusComp pBusComp) {
        if (pBusComp != null) {
            pBusComp.release();
        }
    }

    /**
     * Releases Bus Object.
     * 
     * @author Abhijit Suvarna on Aug 22, 2008
     * @param pBusObj
     */
    public static void releaseBusObject(SiebelBusObject pBusObj) {
        if (pBusObj != null) {
            pBusObj.release();
        }
    }

    /**
     * Add all SR fields to Property Set so that they can be activated in a single statement
     * 
     * @param ps
     * @return
     */
    public void addSrFieldsToPropertySet(SiebelPropertySet ps) {

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
        ps.setProperty(SiebelConstants.SR_LONG_DESC_FIELD, "");
        ps.setProperty(SiebelConstants.SR_TARGET_CONTACT_FIELD, "");
        ps.setProperty(SiebelConstants.SR_VOLUME_FIELD, "");
        /*return ps;*/
    }

    /**
     * This method handles closing of connection from siebel.
     * 
     * @author sabhijit
     * @param siebel
     *            The object holding connection to siebel
     */
    public static void closeConnection(SiebelUtility pSiebel) {
        if (pSiebel == null) {
            return;
        }
        try {
            pSiebel.logoff();
        } catch (Exception e) {
            logger.info("SiebelUtility::closeConnection() --->  error = " + e.getMessage());
        }
    }

    protected String checkSimplePickError(SiebelBusComp pBC, String pValue, String pFieldName,
            String pPickField) throws AncestorSiebelUtilityException, SiebelException {
        SiebelBusComp lPick = null;
        StringBuffer lPickFld = new StringBuffer();
        String lPickName = "";

        try {
            // if (pValue.trim().length() > 0) {
            if (!StringUtils.isBlank(pValue)) {
                lPick = pBC.getPicklistBusComp(pFieldName);
                lPick.clearToQuery();
                // ----Use ~= so that it ignores the case
                lPick.setSearchSpec(pPickField, " ~= '" + pValue + "'");
                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                } else {
                    // Debug

                    logger.info(pFieldName + ": Could not pick value [" + pValue + "] for pick field ["
                            + pPickField + "]");
                    lPick.clearToQuery();
                    lPick.executeQuery(true);
                    boolean lRetrieval = lPick.firstRecord();
                    logger.info("Displaying all values for field [" + pFieldName + "]");
                    lPickFld.append("\nDisplaying all possible values for field [" + pFieldName
                            + "] :::\n");
                    logger.info("lRetrieval" + lRetrieval);
                    while (lRetrieval) {
                        lPickFld.append("[" + lPick.getFieldValue("Value") + "]");
                        lPickFld.append("/");
                        lPickName = lPick.name();
                        // lRetrieval = lPick.nextRecord();
                        try {
                            lRetrieval = lPick.nextRecord();
                        } catch (SiebelException sble) {
                            if (sble.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE)
                                lRetrieval = false;
                        }
                    }

                    throw new SiebelException();
                }
            }
        } catch (SiebelException pExe) {
            logger.error("Siebel Exception" + pExe);
            logger.error("Siebel pFieldName" + pFieldName);
            if ("Owner".equalsIgnoreCase(pFieldName)) {
                throw new AncestorSiebelUtilityException("Invalid Siebel ID");
            }
            throw new AncestorSiebelUtilityException(lPickFld.toString());
        } finally {
            if (lPick != null) {
                lPick.release();
            }
        }
        return lPickName;
    }

    public String truncMultiLineField(String str, int maxLength) {

        String truncatedStr = StringUtils.substring(str, 0, maxLength);
        return truncatedStr;
    }

    /**
     * @deprecated Use createSR of SvReqUtil instead
     * @param svreq
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public String createSR(ServiceRequestHlpr svreq) throws Exception {

        String createdSRNumber = null;

        if (!login()) {
            // context.close();
            throw new Exception(StrataWSConstants.SBL_LOGIN_FAILED_MSG);
        } else {
            logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

            logger.debug(svreq.toString());

            /* check mandatory fields */
            if (StringUtils.isBlank(svreq.getDomain()) || StringUtils.isBlank(svreq.getProcess())
                    || StringUtils.isBlank(svreq.getSubProcess())
                    || StringUtils.isBlank(svreq.getSblSrStatus())
                    || StringUtils.isBlank(svreq.getShortDescription())
                    || StringUtils.isBlank(svreq.getDescription())
            /*|| StringUtils.isBlank(svreq.getClientAccount())*/) {

                throw new AncestorSiebelUtilityException(StrataWSConstants.MANDATORY_FIELDS_EMPTY_MSG
                        + " : " + StrataWSConstants.SR_MANDATORY_FIELDS_MSG);
            }

            SiebelBusObject lBusObject = null;
            SiebelBusComp lBusComp = null;
            /*SiebelBusComp lMvg = null;*/
            SiebelBusComp lPick = null;

            try {
                // ----Get the business Object
                lBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
                lBusComp = lBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);
                lBusComp.newRecord(0);

                /* throw an Exception if Emp Id is null or blank */
                if (StringUtils.isBlank(svreq.getSrContactEmpId())) {
                    throw new AncestorSiebelUtilityException(
                            "Empid is Null or Blank This should never happen!");
                }

                lPick = lBusComp.getPicklistBusComp(SiebelConstants.CONTACT_LASTNAME_FIELD);
                lPick.clearToQuery();

                lPick.activateField(SiebelConstants.CONTACT_PAY_1_FIELD);
                lPick.activateField(SiebelConstants.CONTACT_PAY_2_FIELD);
                lPick.activateField(SiebelConstants.CONTACT_PAY_3_FIELD);

                lPick.setSearchSpec("Employee Number", svreq.getSrContactEmpId());
                logger.debug("Employee Number: " + svreq.getSrContactEmpId());

                /*String clientAccount = sblConfig.getClientAccount();*/

                String clientAccount = svreq.getClientAccount();
                logger.info("clientAccount--> " + clientAccount);

                if (StringUtils.isNotBlank(clientAccount)) {
                    lPick.setSearchSpec("Account", escSearchSpec(clientAccount));
                }

                lPick.setViewMode(9);

                lPick.setSortSpec(SiebelConstants.CONTACT_SORT_SPEC);

                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                } else {
                    /* Should never happen because the Account from the SR must exist */
                    throw new AncestorSiebelUtilityException(
                            "Contact from SR doesn't exist! This should never happen!");
                }

                setSimplePickWithError(lBusComp, svreq.getSblSrStatus(), SiebelConstants.STATUS_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(lBusComp, svreq.getDomain(), SiebelConstants.DOMAIN_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(lBusComp, svreq.getProcess(), SiebelConstants.PROCESS_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(lBusComp, svreq.getSubProcess(), SiebelConstants.SUBPROCESS_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);

                setSimplePickWithError(lBusComp, svreq.getType(), SiebelConstants.SRTYPE_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(lBusComp, svreq.getSblSrOwner(), SiebelConstants.SROWNER_FIELD,
                        "Login Name");
                setSimplePickWithError(lBusComp, svreq.getSource(), SiebelConstants.SR_SOURCE_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(lBusComp, svreq.getPriority(), SiebelConstants.PRIORITY_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);
                setSimplePickWithError(lBusComp, svreq.getSeverity(), SiebelConstants.SEVERITY_FIELD,
                        SiebelConstants.PICKLIST_NAME_VAL);

                lBusComp.setFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD, svreq.getShortDescription());
                lBusComp.setFieldValue(SiebelConstants.SR_LONG_DESC_FIELD, svreq.getDescription());

                writeOrDeleteNewRecord(lBusComp);

                createdSRNumber = lBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME);
                logger.info("SR Number [" + createdSRNumber + "] created");
                logger.info("**********************************************************");
                svreq.setSrNumber(createdSRNumber);
            } catch (SiebelException sblEx) {
                logger.error("Error in createSR() " + sblEx.toString());
                sblEx.printStackTrace();
                throw new AncestorSiebelUtilityException(sblEx);
            } finally {
                releaseBusComp(lBusComp);
                releaseBusObject(lBusObject);
                logoff();
            }
            /*logoff();*/
        }

        return createdSRNumber;
    }

    /**
     * @deprecated Use updateSR of SvReqUtility instead
     * @param srNumber
     * @param svreq
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public String updateSR(String srNumber, ServiceRequestHlpr svreq) throws Exception {

        String updatedSRNum = "";

        if (!login()) {
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
                || StringUtils.isBlank(svreq.getDescription())
        /*|| StringUtils.isBlank(svreq.getClientAccount())*/) {

            /* context.close(); */
            throw new AncestorSiebelUtilityException(StrataWSConstants.MANDATORY_FIELDS_EMPTY_MSG + " - "
                    + StrataWSConstants.SR_MANDATORY_FIELDS_MSG);
        }

        SiebelBusObject lBusObject = null;
        SiebelBusComp lBusComp = null;
        /*SiebelBusComp lMvg = null;*/
        SiebelBusComp lPick = null;

        try {
            // ----Get the business Object
            lBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
            lBusComp = lBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

            /* throw an Exception if Emp Id is null or blank */
            if (StringUtils.isBlank(svreq.getSrContactEmpId())) {
                throw new AncestorSiebelUtilityException(
                        "Emp Id is Null or Blank. This should never happen!");
            }

            SiebelPropertySet ps = dataBean.newPropertySet();
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
            ps.setProperty(SiebelConstants.SR_LONG_DESC_FIELD, "");

            lBusComp.activateMultipleFields(ps);
            lBusComp.setViewMode(3);
            lBusComp.clearToQuery();
            lBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME, srNumber);
            lBusComp.executeQuery(true);

            if (lBusComp.firstRecord()) {

                /* Check if SR is being reopened */

                String currentSRStatus = lBusComp.getFieldValue(SiebelConstants.STATUS_FIELD);
                String updatedSRStatus = svreq.getSblSrStatus();
                /* String openSRstatus = env.getProperty("sbl.sr.open.status", "Open");*/
                String closedSRStatus = SiebelConstants.SBL_CLOSED_SR_STATUS;
                logger.debug("Current SR status is {} | New status is {} | Closed Status is {}",
                        currentSRStatus, updatedSRStatus, closedSRStatus);
                /* if (!currentSRStatus.equalsIgnoreCase(closedSRStatus)
                         && updatedSRStatus.equalsIgnoreCase(closedSRStatus))*/
                if (!currentSRStatus.equalsIgnoreCase(closedSRStatus)) {

                    logger.info("Update SR");

                    /* throw an Exception if Emp Id is null or blank */
                    if (StringUtils.isBlank(svreq.getSrContactEmpId())) {
                        throw new AncestorSiebelUtilityException(
                                StrataWSConstants.EMP_ID_NULL_OR_BLANK_MSG);
                    }

                    lPick = lBusComp.getPicklistBusComp(SiebelConstants.CONTACT_LASTNAME_FIELD);
                    lPick.clearToQuery();

                    /*lPick.activateField(SiebelConstants.CONTACT_EMP_ID_FIELD);*/
                    lPick.activateField(SiebelConstants.CONTACT_PAY_1_FIELD);
                    lPick.activateField(SiebelConstants.CONTACT_PAY_2_FIELD);
                    lPick.activateField(SiebelConstants.CONTACT_PAY_3_FIELD);

                    lPick.setSearchSpec(SiebelConstants.CONTACT_EMP_ID_FIELD, svreq.getSrContactEmpId());
                    logger.debug("Employee Number: " + svreq.getSrContactEmpId());

                    String clientAccount = svreq.getClientAccount();
                    logger.info("clientAccount--> " + clientAccount);

                    if (StringUtils.isNotBlank(clientAccount)) {
                        lPick.setSearchSpec("Account", escSearchSpec(clientAccount));
                    }

                    lPick.setViewMode(9);

                    lPick.setSortSpec(SiebelConstants.CONTACT_SORT_SPEC);

                    lPick.executeQuery(true);

                    if (lPick.firstRecord()) {
                        lPick.pick();
                    } else {
                        /* Should never happen because the Account from the SR must exist */
                        throw new AncestorSiebelUtilityException(
                                "Contact from SR doesn't exist! This should never happen!");
                    }

                    setSimplePickWithError(lBusComp, svreq.getSblSrStatus(),
                            SiebelConstants.STATUS_FIELD, SiebelConstants.PICKLIST_NAME_VAL);
                    setSimplePickWithError(lBusComp, svreq.getDomain(), SiebelConstants.DOMAIN_FIELD,
                            SiebelConstants.PICKLIST_NAME_VAL);
                    setSimplePickWithError(lBusComp, svreq.getProcess(), SiebelConstants.PROCESS_FIELD,
                            SiebelConstants.PICKLIST_NAME_VAL);
                    setSimplePickWithError(lBusComp, svreq.getSubProcess(),
                            SiebelConstants.SUBPROCESS_FIELD, SiebelConstants.PICKLIST_NAME_VAL);

                    setSimplePickWithError(lBusComp, svreq.getType(), SiebelConstants.SRTYPE_FIELD,
                            SiebelConstants.PICKLIST_NAME_VAL);
                    setSimplePickWithError(lBusComp, svreq.getSblSrOwner(),
                            SiebelConstants.SROWNER_FIELD, "Login Name");
                    setSimplePickWithError(lBusComp, svreq.getSource(), SiebelConstants.SR_SOURCE_FIELD,
                            SiebelConstants.PICKLIST_NAME_VAL);
                    setSimplePickWithError(lBusComp, svreq.getPriority(), SiebelConstants.PRIORITY_FIELD,
                            SiebelConstants.PICKLIST_NAME_VAL);
                    setSimplePickWithError(lBusComp, svreq.getSeverity(), SiebelConstants.SEVERITY_FIELD,
                            SiebelConstants.PICKLIST_NAME_VAL);

                    lBusComp.setFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD,
                            svreq.getShortDescription());
                    lBusComp.setFieldValue(SiebelConstants.SR_LONG_DESC_FIELD, svreq.getDescription());

                    lBusComp.writeRecord();

                    updatedSRNum = lBusComp.getFieldValue(SiebelConstants.SR_NUM_FIELD_NAME);
                    logger.info("SR Number [" + updatedSRNum + "] updated");
                    logger.info("**********************************************************");
                    svreq.setSrNumber(updatedSRNum);

                } else {
                    throw new Exception("Cannot update closed SR");
                }
            }

        } catch (SiebelException sblEx) {
            logger.error("Error in updateSR() " + sblEx.toString());
            sblEx.printStackTrace();
            lBusComp.undoRecord();
            throw new AncestorSiebelUtilityException(sblEx);
        } finally {
            releaseBusComp(lBusComp);
            releaseBusObject(lBusObject);
            logoff();
        }
        return updatedSRNum;
    }

    /**
     * @deprecated
     * @param srNumber
     * @return
     * @throws Exception
     */
    public ServiceRequestHlpr getSrDetails(String srNumber) throws Exception {

        ServiceRequestHlpr svReqHlpr = new ServiceRequestHlpr();

        SiebelBusObject lBusObject = null;
        SiebelBusComp lBusComp = null;
        SiebelBusComp lPick = null;

        if (!login()) {
            throw new Exception("Siebel Login Failure");
        } else {

            logger.info(StrataWSConstants.SBL_LOGIN_SUCCESS_MSG);

            if (StringUtils.isBlank(srNumber)) {
                logger.info("SR number is null or blank.");
                return svReqHlpr;
            }

            try {
                lBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
                lBusComp = lBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);

                /*SiebelPropertySet srPropSet = dataBean.newPropertySet();
                addSrFieldsToPropertySet(srPropSet);*/

                lBusComp.activateField(SiebelConstants.SR_NUM_FIELD_NAME);
                lBusComp.activateField(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME);
                lBusComp.setViewMode(3);
                lBusComp.clearToQuery();
                lBusComp.setSearchSpec(SiebelConstants.SR_NUM_FIELD_NAME, srNumber);

                lBusComp.executeQuery(true);

                if (lBusComp.firstRecord()) {

                    svReqHlpr.setSrNumber(srNumber);
                    svReqHlpr.setClientAccount(lBusComp.getFieldValue(SiebelConstants.ACCOUNT_FIELD));
                    svReqHlpr.setSblSrStatus(lBusComp.getFieldValue(SiebelConstants.STATUS_FIELD));
                    svReqHlpr.setSblSrOwner(lBusComp.getFieldValue(SiebelConstants.SROWNER_FIELD));
                    svReqHlpr.setDomain(lBusComp.getFieldValue(SiebelConstants.DOMAIN_FIELD));
                    svReqHlpr.setProcess(lBusComp.getFieldValue(SiebelConstants.PROCESS_FIELD));
                    svReqHlpr.setSubProcess(lBusComp.getFieldValue(SiebelConstants.SUBPROCESS_FIELD));
                    svReqHlpr.setType(lBusComp.getFieldValue(SiebelConstants.SRTYPE_FIELD));
                    svReqHlpr.setSource(lBusComp.getFieldValue(SiebelConstants.SR_SOURCE_FIELD));
                    svReqHlpr.setPriority(lBusComp.getFieldValue(SiebelConstants.PRIORITY_FIELD));
                    svReqHlpr.setSeverity(lBusComp.getFieldValue(SiebelConstants.SEVERITY_FIELD));
                    svReqHlpr.setShortDescription(lBusComp
                            .getFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD));
                    svReqHlpr.setDescription(lBusComp.getFieldValue(SiebelConstants.SR_LONG_DESC_FIELD));
                    svReqHlpr.setSrContactEmpId(lBusComp
                            .getFieldValue(SiebelConstants.SR_CONTACT_ID_FIELD));
                    svReqHlpr.setSblRowId(lBusComp
                            .getFieldValue(SiebelConstants.SR_SBL_ROW_ID_FIELD_NAME));

                    /*String srContactId = lBusComp.getFieldValue(SiebelConstants.SR_CONTACT_ID_FIELD);
                    logger.debug("Contact Emp Id: {}", srContactId);
                    svReqHlpr.setSrContactEmpId(srContactId);*/

                    /* logger.info("*****************************************************");*/
                    logger.info("SR Number [" + srNumber + "] details successfully fetched");
                    logger.info("*****************************************************");

                } else {
                    logger.error("SR# {} was not found in Siebel", srNumber);
                    throw new Exception("SR# not found. Please check SR#.");
                }
            } catch (SiebelException sblEx) {
                logger.error("Error in getSrDetails() " + sblEx.toString());
                sblEx.printStackTrace();
                throw new AncestorSiebelUtilityException(sblEx);
            } finally {
                releaseBusComp(lPick);
                releaseBusComp(lBusComp);
                releaseBusObject(lBusObject);
                logoff();
            }
        }
        return svReqHlpr;
    }

    /*protected boolean setSimpleBooleanPick(SiebelBusComp pBC, String pValue, String pFieldName,
            String pPickField) throws SiebelException {
        SiebelBusComp lPick = null;
        boolean pickFlag = false;
        try {
            if (pValue.trim().length() > 0) {
                lPick = pBC.getPicklistBusComp(pFieldName);
                lPick.clearToQuery();
                // ----Use ~= so that it ignores the case
                lPick.setSearchSpec(pPickField, " ~= '" + pValue + "'");
                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                    pickFlag = true;
                } else {
                    // Debug
                    logger.info("-------------------------------");
                    logger.info(pFieldName + ": Could not pick value [" + pValue + "] for pick field ["
                            + pPickField + "]");

                    lPick.clearToQuery();
                    lPick.executeQuery(true);
                    boolean lRetrieval = lPick.firstRecord();
                    try {
                        logger.info("Displaying all values for field [" + pFieldName + "]");
                        while (lRetrieval) {
                            logger.debug(pPickField + " : " + lPick.getFieldValue(pPickField) + " ("
                                    + lPick.getFieldValue("Value") + ")");
                            // lRetrieval = lPick.nextRecord();
                            try {
                                lRetrieval = lPick.nextRecord();
                            } catch (SiebelException sble) {
                                if (sble.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE)
                                    lRetrieval = false;
                            }

                        }
                        logger.info("-------------------------------");

                    } catch (Exception e) {
                        logger.error("Error in setSimpleQuery debug routine");
                        throw e;
                    }
                }
            }
        } finally {
            if (lPick != null) {
                lPick.release();
            }
        }
        return pickFlag;
    }*/
}
