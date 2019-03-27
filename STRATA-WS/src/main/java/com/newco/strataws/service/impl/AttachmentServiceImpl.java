package com.newco.strataws.service.impl;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.common.util.SftpFileDowload;
import com.newco.strataws.common.util.SftpFileUpload;
import com.newco.strataws.config.file.AttachConfig;
import com.newco.strataws.config.sftp.SFTPConfig;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.service.AttachmentService;
import com.newco.strataws.siebel.AttachUtility;

@Service("attachmentService")
public class AttachmentServiceImpl implements AttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentServiceImpl.class);

    @Override
    public Map<String, String> fetchActivityAttachmentPaths(String activityId, SiebelConfig sblConfig)
            throws Exception {

        logger.debug(
                "AttachmentServiceImpl--> fetchActivityAttachmentPaths() called with activity Id value: {}",
                activityId);
        AttachUtility attachUtility = new AttachUtility(sblConfig);

        return (Map<String, String>) attachUtility.fetchActivityAttachPaths(activityId);
    }

    @Override
    public Map<String, String> fetchAttachmentPaths(String parentId, SiebelConfig siebelConfig,
            AttachConfig attachConfig) throws Exception {

        logger.debug(
                "AttachmentServiceImpl--> fetchActivityAttachmentPaths() called with {} Id value: {}",
                attachConfig.getSblAttachType(), parentId);
        AttachUtility attachUtility = new AttachUtility(siebelConfig);

        return attachUtility.fetchAttachPaths(parentId, attachConfig);
    }

    @Override
    public String fetchAttachmentsAsZip(String activityId, List<String> fileList,
            SFTPConfig siebelFTPConfig) throws Exception {

        return SftpFileDowload.downloadAsZipViaJSchSFTP(activityId, fileList, siebelFTPConfig);

    }

    @Override
    public String uploadNewAttachment(String parentId, InputStream recInputStream,
            SiebelConfig siebelConfig, SFTPConfig sftpConfig, AttachConfig attachConfig) throws Exception {

        boolean fileUpFlag = false;
        String srvrFilePath = sftpConfig.getSblFtpRemoteDir() + attachConfig.getFileName();
        String createFileReturnStr = "";
        String failureMsg = "";

        if (StringUtils.isBlank(parentId)) {
            String errorMsg = StrataWSConstants.NULL_OR_BLANK_MSG;
            logger.error("Error in AttachmentService : " + errorMsg, new Exception(errorMsg));
            throw new Exception(errorMsg);
        }

        fileUpFlag = SftpFileUpload.uploadFileViaJSchSFTP(recInputStream, sftpConfig, attachConfig);
        if (fileUpFlag) {
            logger.info("Uploaded file to server. Updating Siebel records");
            AttachUtility attachUtility = new AttachUtility(siebelConfig);
            createFileReturnStr = attachUtility.insertSiebelAttachRecord(parentId, srvrFilePath,
                    attachConfig);

            if (StringUtils.isBlank(createFileReturnStr)
                    || StringUtils.startsWithIgnoreCase(createFileReturnStr, "Siebel error")) {
                String errorMsg = "error--> " + StrataWSConstants.SBL_FILE_RECORD_CREATION_FAILED_MSG
                        + " : " + createFileReturnStr;
                logger.error("Error in AttachmentService: " + errorMsg);
                createFileReturnStr = errorMsg;
            } else {
                createFileReturnStr = SiebelConstants.CREATE_FILE_SUCCESS_MSG + " : "
                        + createFileReturnStr;
            }
        } else {
            String errorMsg = "error--> " + StrataWSConstants.FILE_UPLOAD_FAILED_MSG;
            logger.error("Error in AttachmentService: " + errorMsg);
            createFileReturnStr = errorMsg;
        }

        return createFileReturnStr;
    }

    @Override
    public String uploadAttachment(String parentId, InputStream recInputStream,
            SiebelConfig siebelConfig, SFTPConfig sftpConfig, AttachConfig attachConfig) throws Exception {

        boolean fileUpFlag = false;
        String srvrFilePath = sftpConfig.getSblFtpRemoteDir() + attachConfig.getFileName();
        String createFileReturnStr = "";
        String failureMsg = "";

        if (StringUtils.isBlank(parentId)) {
            String errorMsg = StrataWSConstants.PARENT_VALUE + StrataWSConstants.NULL_OR_BLANK_MSG;
            logger.error("Error in AttachmentService : " + errorMsg, new Exception(errorMsg));
            throw new Exception(errorMsg);
        }

        fileUpFlag = SftpFileUpload.uploadFileViaJSchSFTP(recInputStream, sftpConfig, attachConfig);
        if (fileUpFlag) {
            logger.info("Uploaded file to server. Updating Siebel records");
            AttachUtility attachUtility = new AttachUtility(siebelConfig);
            createFileReturnStr = attachUtility.uploadActivityAttachment(parentId, srvrFilePath,
                    attachConfig);

            if (StringUtils.isBlank(createFileReturnStr)
                    || StringUtils.startsWithIgnoreCase(createFileReturnStr, "Siebel error")) {
                String errorMsg = "error--> " + StrataWSConstants.SBL_FILE_RECORD_CREATION_FAILED_MSG
                        + " : " + createFileReturnStr;
                logger.error("Error in AttachmentService: " + errorMsg);
                createFileReturnStr = errorMsg;
            } else {
                createFileReturnStr = SiebelConstants.CREATE_FILE_SUCCESS_MSG + " : "
                        + createFileReturnStr;
            }
        } else {
            String errorMsg = "error--> " + StrataWSConstants.FILE_UPLOAD_FAILED_MSG;
            logger.error("Error in AttachmentService: " + errorMsg);
            createFileReturnStr = errorMsg;
        }

        return createFileReturnStr;
    }

    /* @Override
    public Boolean checkForAttachment(String activityId) {
        // TODO Auto-generated method stub

        return null;
    }*/

}
