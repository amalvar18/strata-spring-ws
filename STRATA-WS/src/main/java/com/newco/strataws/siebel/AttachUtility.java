package com.newco.strataws.siebel;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.file.AttachConfig;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.siebel.data.SiebelBusComp;
import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelException;

public class AttachUtility extends SiebelUtility {

    @Autowired
    AttachConfig attachConfig;

    private static final Logger logger = LoggerFactory.getLogger(AttachUtility.class);

    public AttachUtility(SiebelConfig siebelConfig) {
        super(siebelConfig);
    }

    public String insertSiebelAttachRecord(String parentRowId, String filePath, AttachConfig attachConfig)
            throws Exception {

        logger.info("AttachUtilty --> Uploading attachment for activity Id: {}", parentRowId);

        String createFileReturns = "";
        String createFileReturnStr = "";
        String srvrFilePath = null;
        String parentType = attachConfig.getSblAttachType();

        if (StringUtils.isBlank(parentRowId)) {
            logger.info("Parent Id param is null - using value from attachConfig");
            parentRowId = attachConfig.getSblParentRowId();

            if (StringUtils.isBlank(parentRowId)) {
                String errorMsg = StrataWSConstants.ACTIVITY_VALUE + " or " + StrataWSConstants.SR_VALUE
                        + StrataWSConstants.NULL_OR_BLANK_MSG;
                logger.error("Error in AttachUtility.insertSiebelAttachRecord() " + errorMsg,
                        new Exception(errorMsg));
                throw new Exception(errorMsg);
            }
        }

        if (StringUtils.isNotBlank(filePath)) {
            logger.debug("AttachUtilty --> File path param exists: {}", filePath);
            srvrFilePath = filePath;
        } else {
            String errorMsg = StrataWSConstants.FILEPATH_NULL_OR_BLANK_MSG;
            logger.error("Error in AttachUtility.uploadActivity() " + errorMsg, new Exception(errorMsg));
            throw new Exception(errorMsg);
        }

        if (!sblConnected) {

            logger.info("Siebel is not connected - Logging in again.");
            if (!login()) {
                logger.error("SiebelUtilty Exception--> ", new Exception("Siebel Login Failure"));
                throw new Exception("Siebel Login Failure");
            }
        }

        SiebelBusObject busObject = null;
        SiebelBusComp attachBusComp = null;

        try {

            if (parentType.equalsIgnoreCase(StrataWSConstants.ACTIVITY_VALUE)) {
                busObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
                attachBusComp = busObject.getBusComp(SiebelConstants.ACTIVITY_ATTACHMENT_BC_NAME);
            } else {
                busObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BO_NAME);
                attachBusComp = busObject.getBusComp(SiebelConstants.SR_ATTACHMENT_BC_NAME);
            }

            if (busObject == null || attachBusComp == null) {
                logger.warn("SiebelUtilty--> Siebel BusComp or BusObject is null");
            }

            logger.info("AttachUtilty --> Attempting to create file record for: {}", srvrFilePath);
            logger.debug("Using attach config: " + attachConfig.toString());

            attachBusComp.newRecord(0);
            /*Set attach buscomp fields*/
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_ACT_ID_FIELD, parentRowId);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILESRC_FIELD,
                    attachConfig.getFileSrcType());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DEFER_FLG_FIELD,
                    attachConfig.getFileDeferFlg());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DOCK_REQ_FLG_FIELD,
                    attachConfig.getFileDockReqFlg());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DOCK_STAT_FLG_FIELD,
                    attachConfig.getFileDockStatFlg());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_AUTO_UPD_FLG_FIELD,
                    attachConfig.getFileAutoUpdFlg());

            /* Not setting the below fields as they are not required. Siebel auto-generates the values. */
            /* String fileBaseName = FilenameUtils.getBaseName(srcFilePath);
            String fileExt = FilenameUtils.getExtension(srcFilePath);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD, fileBaseName);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD, fileExt);
            */

            String createParamsAsArray[] = { srvrFilePath, SiebelConstants.ACT_ATTACH_FILENAME_FIELD,
                    attachConfig.getSblKeepLink() };
            logger.debug("Method Params: {}", ArrayUtils.toString(createParamsAsArray));
            /* CreateFile will return Success or Failure */
            createFileReturns = attachBusComp.invokeMethod(SiebelConstants.CREATE_FILE_METHOD_NAME,
                    createParamsAsArray);
            logger.info("File created message from Siebel: " + createFileReturns);

            writeOrDeleteNewRecord(attachBusComp);

            if (createFileReturns.equalsIgnoreCase(SiebelConstants.CREATE_FILE_SUCCESS_MSG)) {
                createFileReturnStr = attachBusComp
                        .getFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD)
                        + "."
                        + attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD);
            } else {
                createFileReturnStr = "";
            }

        } catch (SiebelException se) {
            if (se.getErrorCode() == SiebelConstants.DUP_FILE_RECORD_ERR_CODE) {
                createFileReturnStr = "Siebel error--> "
                        + StrataWSConstants.SBL_FILE_RECORD_ALREADY_EXISTS_MSG;
            } else {
                createFileReturnStr = "Siebel error--> " + se.getErrorMessage();
            }
            logSiebelException(se);
        } finally {

            releaseBusComp(attachBusComp);
            releaseBusObject(busObject);
            logoff();
        }

        return createFileReturnStr;
    }

    /**
     * Inserts uploaded attachment record under activity
     * 
     * @param activityId
     * @param filePath
     *            path on the siebel file server
     * @param attachConfig
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public String uploadActivityAttachment(String activityId, String filePath, AttachConfig attachConfig)
            throws Exception {

        logger.info("AttachUtilty --> Uploading attachment for activity Id: {}", activityId);

        String createFileReturns = "";
        String createFileReturnStr = "";
        String srvrFilePath = null;

        if (StringUtils.isBlank(activityId)) {
            String errorMsg = StrataWSConstants.ACTIVITY_VALUE + StrataWSConstants.NULL_OR_BLANK_MSG;
            logger.error("Error in AttachUtility.uploadActivity() " + errorMsg, new Exception(errorMsg));
            throw new Exception(errorMsg);
        }

        if (StringUtils.isNotBlank(filePath)) {
            logger.debug("AttachUtilty --> File path param exists: {}", filePath);
            srvrFilePath = filePath;
        } else {
            String errorMsg = StrataWSConstants.FILEPATH_NULL_OR_BLANK_MSG;
            logger.error("Error in AttachUtility.uploadActivity() " + errorMsg, new Exception(errorMsg));
            throw new Exception(errorMsg);
        }

        if (!sblConnected) {

            logger.info("Siebel is not connected - Logging in again.");
            if (!login()) {
                logger.error("SiebelUtilty Exception--> ", new Exception("Siebel Login Failure"));
                throw new Exception("Siebel Login Failure");
            }
        }

        SiebelBusObject busObject = null;
        SiebelBusComp attachBusComp = null;

        try {

            busObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
            attachBusComp = busObject.getBusComp(SiebelConstants.ACTIVITY_ATTACHMENT_BC_NAME);

            if (busObject == null || attachBusComp == null) {
                logger.warn("SiebelUtilty--> Siebel BusComp or BusObject is null");
            }

            logger.info("AttachUtilty --> Attempting to create file record for: {}", srvrFilePath);
            logger.debug("Using attach config: " + attachConfig.toString());

            attachBusComp.newRecord(0);
            /*Set attach buscomp fields*/
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_ACT_ID_FIELD, activityId);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILESRC_FIELD,
                    attachConfig.getFileSrcType());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DEFER_FLG_FIELD,
                    attachConfig.getFileDeferFlg());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DOCK_REQ_FLG_FIELD,
                    attachConfig.getFileDockReqFlg());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DOCK_STAT_FLG_FIELD,
                    attachConfig.getFileDockStatFlg());
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_AUTO_UPD_FLG_FIELD,
                    attachConfig.getFileAutoUpdFlg());

            /* Not setting the below fields as they are not required. Siebel auto-generates the values. */
            /* String fileBaseName = FilenameUtils.getBaseName(srcFilePath);
            String fileExt = FilenameUtils.getExtension(srcFilePath);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD, fileBaseName);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD, fileExt);
            */

            String createParamsAsArray[] = { srvrFilePath, SiebelConstants.ACT_ATTACH_FILENAME_FIELD,
                    attachConfig.getSblKeepLink() };
            logger.debug("Method Params: {}", ArrayUtils.toString(createParamsAsArray));
            /* CreateFile will return Success or Failure */
            createFileReturns = attachBusComp.invokeMethod(SiebelConstants.CREATE_FILE_METHOD_NAME,
                    createParamsAsArray);
            logger.info("File created message from Siebel: " + createFileReturns);

            writeOrDeleteNewRecord(attachBusComp);

            if (createFileReturns.equalsIgnoreCase(SiebelConstants.CREATE_FILE_SUCCESS_MSG)) {
                createFileReturnStr = attachBusComp
                        .getFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD)
                        + "."
                        + attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD);
            } else {
                createFileReturnStr = "";
            }

        } catch (SiebelException se) {
            if (se.getErrorCode() == SiebelConstants.DUP_FILE_RECORD_ERR_CODE) {
                createFileReturnStr = "Siebel error--> "
                        + StrataWSConstants.SBL_FILE_RECORD_ALREADY_EXISTS_MSG;
            } else {
                createFileReturnStr = "Siebel error--> " + se.getErrorMessage();
            }
            logSiebelException(se);
        } finally {

            releaseBusComp(attachBusComp);
            releaseBusObject(busObject);
            logoff();
        }

        return createFileReturnStr;
    }

    /**
     * Returns map of attachment paths for SR or activity based on row Id
     * 
     * @param parentRowId
     * @param attachConfig
     * @return attachMap - (filename, file path)
     * @throws Exception
     * @author Amal Varghese
     */
    public Map<String, String> fetchAttachPaths(String parentRowId, AttachConfig attachConfig)
            throws Exception {

        /*String activityId = null;*/
        if (StringUtils.isBlank(parentRowId)) {
            logger.info("Parent Id param is null - using value from attachConfig");
            parentRowId = attachConfig.getSblParentRowId();

            if (StringUtils.isBlank(parentRowId)) {
                String errorMsg = StrataWSConstants.ACTIVITY_VALUE + " or " + StrataWSConstants.SR_VALUE
                        + StrataWSConstants.NULL_OR_BLANK_MSG;
                logger.error("Error in AttachUtility.insertSiebelAttachRecord() " + errorMsg);
                throw new Exception(errorMsg);
            }
        }

        String parentType = attachConfig.getSblAttachType();

        logger.info("SiebelUtilty --> Fetching attachment paths for {} row Id: {}", parentType,
                parentRowId);

        if (!sblConnected) {

            logger.info("Siebel is not connected - Logging in again.");
            if (!login()) {
                logger.error("SiebelUtilty Exception--> ", new Exception("Siebel Login Failure"));
                throw new Exception("Siebel Login Failure");
            }
        }

        SiebelBusObject busObject = null;
        SiebelBusComp attachBusComp = null;

        Map<String, String> attachMap = new HashMap<String, String>();

        if (!sblConnected) {

            logger.info("Siebel is not connected - Logging in again.");
            if (!login()) {
                logger.error("SiebelUtilty Exception--> ", new Exception("Siebel Login Failure"));
                throw new Exception("Siebel Login Failure");
            }
        }

        if (parentType.equalsIgnoreCase(StrataWSConstants.ACTIVITY_VALUE)) {
            busObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
            attachBusComp = busObject.getBusComp(SiebelConstants.ACTIVITY_ATTACHMENT_BC_NAME);
        } else {
            busObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BO_NAME);
            attachBusComp = busObject.getBusComp(SiebelConstants.SR_ATTACHMENT_BC_NAME);
        }

        if (busObject == null || attachBusComp == null) {
            logger.warn("SiebelUtilty--> Siebel BusComp or BusObject is null");
        }

        try {

            attachBusComp.activateField(SiebelConstants.ACT_ATTACH_FILENAME_FIELD);
            attachBusComp.setViewMode(5);
            attachBusComp.clearToQuery();
            attachBusComp.setSearchSpec(SiebelConstants.ACT_ATTACH_ACT_ID_FIELD, parentRowId);
            attachBusComp.executeQuery(true);

            boolean hasAttach = attachBusComp.firstRecord();

            while (hasAttach) {
                logger.info("Reading attachment info");
                String fileName = attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD);
                String fileExt = attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD);
                String fileSize = attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILESIZE_FIELD);
                logger.debug("Attachment info from Siebel - File Name: {} | Extn: {} | Size: {}",
                        fileName, fileExt, fileSize);

                String fullFileName = fileName + "." + fileExt;

                logger.info("Full File Name: " + fullFileName);

                /* Ignore original message.txt */
                if (StringUtils.equals(fullFileName,
                        StrataWSConstants.ATTACHMENT_ORIGINAL_MESSAGE_FILENAME)) {
                    logger.info("Ignoring file");
                } else {

                    String fieldNameAsArray[] = { SiebelConstants.ACT_ATTACH_FILENAME_FIELD };

                    String siebelFilePath = attachBusComp.invokeMethod("GetFile", fieldNameAsArray);
                    logger.debug("FilePath before getting file path from siebel: " + siebelFilePath);

                    siebelFilePath = getSiebelFilePath(siebelFilePath);

                    attachMap.put(fullFileName, siebelFilePath);
                }

                try {
                    hasAttach = attachBusComp.nextRecord();
                } catch (SiebelException sble) {
                    if (sble.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE)
                        hasAttach = false;
                }
            }
        } catch (SiebelException se) {
            logSiebelException(se);
        } finally {

            releaseBusComp(attachBusComp);
            releaseBusObject(busObject);
            logoff();
        }
        return attachMap;
    }

    /**
     * Inserts uploaded attachment record under activity
     * 
     * @deprecated
     * @param activityId
     * @param filePath
     * @return
     * @throws Exception
     */
    public String uploadActivityAttachment(String activityId, String filePath) throws Exception {

        logger.info("AttachUtilty --> Uploading attachment for activity Id: {}", activityId);

        String createFileReturns = null;
        String srcFilePath = null;

        if (StringUtils.isBlank(activityId)) {
            logger.error("Activity Id is null or blank", new Exception("Activity Id is Null or Blank"));
            throw new Exception("Activity Id is null or blank");
        }

        if (StringUtils.isNotBlank(filePath)) {
            logger.debug("AttachUtilty --> File path param exists: {}", filePath);
            srcFilePath = filePath;
            /*fileToUpload = FilenameUtils.getName(filePath);*/
        } else {
            logger.debug("AttachUtilty --> File path param doesn't exist - Using default");
            srcFilePath = FilenameUtils.getName(attachConfig.getDefaultAttachUplFile());
        }

        if (StringUtils.isBlank(srcFilePath)) {
            logger.error("File Name is null or blank", new Exception("Activity Id is Null or Blank"));
            throw new Exception("File Name is null or blank");
        }

        if (!sblConnected) {

            logger.info("Siebel is not connected - Logging in again.");
            if (!login()) {
                logger.error("SiebelUtilty Exception--> ", new Exception("Siebel Login Failure"));
                throw new Exception("Siebel Login Failure");
            }
        }

        SiebelBusObject busObject = null;
        SiebelBusComp attachBusComp = null;

        try {

            busObject = dataBean.getBusObject(SiebelConstants.ACTIVITY_BO_NAME);
            attachBusComp = busObject.getBusComp(SiebelConstants.ACTIVITY_ATTACHMENT_BC_NAME);

            if (busObject == null || attachBusComp == null) {
                logger.warn("SiebelUtilty--> Siebel BusComp or BusObject is null");
            }

            /*            attachBusComp.activateField(SiebelConstants.ACT_ATTACH_FILENAME_FIELD);
                        attachBusComp.activateField(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD);
                        attachBusComp.setViewMode(5);
                        attachBusComp.clearToQuery();
                        attachBusComp.setSearchSpec(SiebelConstants.ACT_ATTACH_ACT_ID_FIELD, activityId);
                        attachBusComp.executeQuery(true);

                        boolean activityExists = attachBusComp.firstRecord();

                        if (activityExists) {*/

            logger.info("AttachUtilty --> Attempting to create file record for: {}", srcFilePath);

            attachBusComp.newRecord(0);
            /*Set attach buscomp fields*/
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_ACT_ID_FIELD, activityId);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILESRC_FIELD, "FILE");
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DEFER_FLG_FIELD, "R");
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DOCK_REQ_FLG_FIELD, "N");
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_DOCK_STAT_FLG_FIELD, "E");
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILE_AUTO_UPD_FLG_FIELD, "Y");

            /* Not setting the below fields as they are not required. Siebel auto-generates the values. */
            /* String fileBaseName = FilenameUtils.getBaseName(srcFilePath);
            String fileExt = FilenameUtils.getExtension(srcFilePath);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD, fileBaseName);
            attachBusComp.setFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD, fileExt);
            */

            String createParamsAsArray[] = { srcFilePath, SiebelConstants.ACT_ATTACH_FILENAME_FIELD, "N" };
            logger.debug("Method Params: {}", ArrayUtils.toString(createParamsAsArray));
            createFileReturns = attachBusComp.invokeMethod(SiebelConstants.CREATE_FILE_METHOD_NAME,
                    createParamsAsArray);

            writeOrDeleteNewRecord(attachBusComp);

            if (createFileReturns.equalsIgnoreCase("Success")) {
                createFileReturns = attachBusComp
                        .getFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD)
                        + "."
                        + attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD);
            }

        } catch (SiebelException se) {
            logSiebelException(se);
        } finally {

            releaseBusComp(attachBusComp);
            releaseBusObject(busObject);
            logoff();
        }

        return createFileReturns;
    }

    /**
     * Returns map of attachment paths an activity based on activity Id
     * 
     * @deprecated Use fetchAttachPaths() which can be used for both SR and act
     * @param activityId
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public Map<?, ?> fetchActivityAttachPaths(String activityId) throws Exception {

        logger.info("SiebelUtilty --> Fetching attachment paths for activity Id: {}", activityId);

        if (StringUtils.isBlank(activityId)) {
            logger.error("Activity Id is Null or Blank", new Exception("Activity Id is Null or Blank"));
            throw new Exception("Activity Id is Null or Blank");
        }

        SiebelBusObject busObject = null;
        SiebelBusComp attachBusComp = null;

        Map attachMap = new HashMap();

        if (!sblConnected) {

            logger.info("Siebel is not connected - Logging in again.");
            if (!login()) {
                logger.error("SiebelUtilty Exception--> ", new Exception("Siebel Login Failure"));
                throw new Exception("Siebel Login Failure");
            }
        }

        busObject = dataBean.getBusObject(SiebelConstants.INBOUND_BO_NAME);
        attachBusComp = busObject.getBusComp(SiebelConstants.ACTIVITY_ATTACHMENT_BC_NAME);

        if (busObject == null || attachBusComp == null) {
            logger.warn("SiebelUtilty--> Siebel BusComp or BusObject is null");
        }
        try {

            attachBusComp.activateField(SiebelConstants.ACT_ATTACH_FILENAME_FIELD);
            attachBusComp.setViewMode(5);
            attachBusComp.clearToQuery();
            attachBusComp.setSearchSpec(SiebelConstants.ACT_ATTACH_ACT_ID_FIELD, activityId);
            attachBusComp.executeQuery(true);

            boolean hasAttach = attachBusComp.firstRecord();

            while (hasAttach) {
                logger.info("Reading attachment info");
                String fileName = attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILENAME_FIELD);
                String fileExt = attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILEEXTN_FIELD);
                String fileSize = attachBusComp.getFieldValue(SiebelConstants.ACT_ATTACH_FILESIZE_FIELD);
                logger.debug("Attachment info from Siebel - File Name: {} | Extn: {} | Size: {}",
                        fileName, fileExt, fileSize);

                String fullFileName = fileName + "." + fileExt;

                logger.info("Full File Name: " + fullFileName);

                /* Ignore original message.txt */
                if (StringUtils.equals(fullFileName,
                        StrataWSConstants.ATTACHMENT_ORIGINAL_MESSAGE_FILENAME)) {
                    logger.info("Ignoring file");
                } else {

                    String fieldNameAsArray[] = { SiebelConstants.ACT_ATTACH_FILENAME_FIELD };

                    String siebelFilePath = attachBusComp.invokeMethod("GetFile", fieldNameAsArray);
                    logger.debug("FilePath before getting file path from siebel: " + siebelFilePath);

                    siebelFilePath = getSiebelFilePath(siebelFilePath);

                    attachMap.put(fullFileName, siebelFilePath);
                }

                try {
                    hasAttach = attachBusComp.nextRecord();
                } catch (SiebelException sble) {
                    if (sble.getErrorCode() == SiebelConstants.SBL8_EOF_ERR_CODE)
                        hasAttach = false;
                }
            }
        } catch (SiebelException se) {
            logSiebelException(se);
        } finally {

            releaseBusComp(attachBusComp);
            releaseBusObject(busObject);
            logoff();
        }
        return attachMap;

    }

    protected static String getSiebelFilePath(String filePath) {
        String siebelfilePath = filePath;
        int startIndex = filePath.indexOf(",");
        if (startIndex >= 0) {
            siebelfilePath = filePath.substring(startIndex + 1, filePath.length());
        }
        return siebelfilePath;
    }
}
