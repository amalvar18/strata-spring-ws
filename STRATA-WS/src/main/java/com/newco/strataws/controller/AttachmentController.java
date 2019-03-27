package com.newco.strataws.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.common.util.AuthUtils;
import com.newco.strataws.config.file.AttachConfig;
import com.newco.strataws.config.sftp.SFTPConfig;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.exception.NotFoundException;
import com.newco.strataws.model.ServiceRequestHlpr;
import com.newco.strataws.service.ActivityService;
import com.newco.strataws.service.AttachmentService;
import com.newco.strataws.service.SrService;
import com.newco.surya.base.exceptions.AncestorSiebelUtilityException;

@RestController
public class AttachmentController {

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    SrService srService;

    @Autowired
    ActivityService activityService;

    @Autowired
    SiebelConfig siebelConfig;

    @Autowired
    AttachConfig attachConfig;

    @Autowired
    SFTPConfig sftpConfig;

    @Autowired
    List<String> allowedFileTypes;

    @Autowired
    Map<String, String> sblSrvrMap;

    private static final Logger logger = LoggerFactory.getLogger(AttachmentController.class);

    /**
     * Controller method for getting activity attachments. Returns a zip file
     * 
     * @param clientName
     * @param activityId
     * @param sblEnv
     * @param sblLoginId
     * @param sblPasswrd
     * @param sblSftpLogin
     * @param sblSftpPasswrd
     * @return
     * @author Amal Varghese
     */
    /*@GetMapping(value = "/activity/attach/{client}/{id}", produces = "application/zip")*/
    @GetMapping(value = "/activity/attach/{client}/{id}")
    public @ResponseBody ResponseEntity<?> fetchActAttachments(@PathVariable("client") String clientName,
            @PathVariable("id") String activityId, @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestHeader(value = "Siebel-SFTP-Login-Id") String sblSftpLogin,
            @RequestHeader(value = "Siebel-SFTP-Password") String sblSftpPasswrd) {

        logger.debug("In controller SvreqRestController - fetchActAttachments()");
        logger.debug("Client Name: {} & Activity Id: {}", clientName, activityId);

        /*Check if Siebel credentials present in header */
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error(
                    "Error in AttachmentController::fetchActAttachments()--> {}. Will return Unauthorized ",
                    errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        }
        /*Check if Siebel SFTP credentials present in header */
        if (!AuthUtils.verifySftpHeaders(sblSftpLogin, sblSftpPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_FS_CRED_MISSING_MSG;
            logger.error(
                    "Error in AttachmentController::fetchActAttachments()--> {}. Will return Unauthorized ",
                    errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<byte[]> respEntity = null;
        try {
            /*for (Map.Entry<String, String> entry : sblSrvrMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                logger.debug("key: " + key + "| value: " + value);
            }*/

            /*Update siebel config based on request data*/
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            siebelConfig.setSblEnv(sblEnv);
            /* Will try to fetch server URL from sblSrvrMap config*/
            if (StringUtils
                    .isNotBlank(sblSrvrMap.get(sblEnv + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                logger.info("Request header SblEnv value: {} - updated siebel URL from config map to {}",
                        sblEnv, siebelConfig.getSblServerURL());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "URL:", siebelConfig.getSblServerURL()));
            }

            /*Update sftp config based on request*/
            sftpConfig.setSblFtpServerUser(sblSftpLogin);
            sftpConfig.setFtpPassword(sblSftpPasswrd);
            if (StringUtils.isNotBlank(sblSrvrMap.get(sblEnv
                    + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX))) {

                sftpConfig.setSblFtpServer(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX));
                logger.info(
                        "Request header SblEnv value: {} - updated SFTP server from config map to {}",
                        sblEnv, sftpConfig.getSblFtpServer());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "SFTP server:", sftpConfig.getSblFtpServer()));
            }
            logger.debug(sftpConfig.toString());

            if (!activityService.checkActivityValidity(activityId, siebelConfig)) {
                throw new NotFoundException(StrataWSConstants.ACTIVITY_VALUE, true);
            }

            Map<String, String> attachMap = attachmentService.fetchActivityAttachmentPaths(activityId,
                    siebelConfig);
            if (attachMap.isEmpty()) {
                logger.info("No attachments Found for activity {}.", activityId);
                String errMsg = "No attachments Found";
                return new ResponseEntity<>(errMsg, HttpStatus.NO_CONTENT);
            }

            List<String> filelist = new ArrayList<String>(attachMap.values());
            String fileName = activityId + ".zip";
            /*String zipFilePath = SftpFileDowload
                    .downloadAsZipViaJSchSFTP(activityId, filelist, ftpConfig);*/

            String zipFilePath = attachmentService
                    .fetchAttachmentsAsZip(activityId, filelist, sftpConfig);

            if (StringUtils.isNotBlank(zipFilePath)) {
                File result = new File(zipFilePath);
                if (result.exists()) {
                    InputStream in = new FileInputStream(result);
                    byte[] out = IOUtils.toByteArray(in);

                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.add("content-disposition", "attachment; filename=" + fileName);
                    responseHeaders.add("Content-Type", "application/zip");

                    respEntity = new ResponseEntity<byte[]>(out, responseHeaders, HttpStatus.OK);
                    logger.info("Attachments were successfully downloaded from Siebel. Returning zip file");
                    return respEntity;
                } else {
                    logger.error("File {} was not found at the given path.", result.getPath());
                    String errMsg = StrataWSConstants.FILE_NOT_FOUND_MSG;
                    return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
                }
            } else {
                logger.error("Zip file path {} is null or blank.", zipFilePath);
                String errMsg = "Unable to return the attachments";
                return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
            }
        } catch (NotFoundException ne) {
            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            logger.error("NotFoundException in AttachmentController::fetchActAttachments()--> ", ne);
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {
            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in AttachmentController::fetchActAttachments()--> ", ase);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            String errMsg = StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in AttachmentController::fetchActAttachments()--> ", e);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Controller method for getting SR attachments. Returns a zip file
     * 
     * @param clientName
     * @param srNumber
     * @param sblEnv
     * @param sblLoginId
     * @param sblPasswrd
     * @param sblSftpLogin
     * @param sblSftpPasswrd
     * @return
     * @author Amal Varghese
     */
    @GetMapping(value = "/svreq/attach/{client}/{srNum}")
    public @ResponseBody ResponseEntity<?> fetchSrAttachments(@PathVariable("client") String clientName,
            @PathVariable("srNum") String srNumber, @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestHeader(value = "Siebel-SFTP-Login-Id") String sblSftpLogin,
            @RequestHeader(value = "Siebel-SFTP-Password") String sblSftpPasswrd) {

        logger.debug("In controller SvreqRestController - fetchSRAttachments()");
        logger.debug("Client Name: {} & SR#: {}", clientName, srNumber);

        /*Check if Siebel credentials present in header */
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error(
                    "Error in AttachmentController::fetchSrAttachments()--> {}. Will return Unauthorized ",
                    errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        }
        /*Check if Siebel SFTP credentials present in header */
        if (!AuthUtils.verifySftpHeaders(sblSftpLogin, sblSftpPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_FS_CRED_MISSING_MSG;
            logger.error(
                    "Error in AttachmentController::fetchSrAttachments()--> {}. Will return Unauthorized ",
                    errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        }

        /*Verify SR# is not null or blank*/
        if (StringUtils.isBlank(srNumber)) {
            String errMsg = StringUtils.joinWith(" ", "error-->", StrataWSConstants.SR_VALUE,
                    StrataWSConstants.NUM_VALUE, StrataWSConstants.NULL_OR_BLANK_MSG);
            logger.error("Error in AttachmentController::fetchSrAttachments() - will return Bad Request: "
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<byte[]> respEntity = null;
        ServiceRequestHlpr serviceRequestHlpr = null;

        try {

            /*Update siebel config based on request data*/
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            siebelConfig.setSblEnv(sblEnv);
            /* Will try to fetch server URL from sblSrvrMap config*/
            if (StringUtils
                    .isNotBlank(sblSrvrMap.get(sblEnv + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                logger.info("Request header SblEnv value: {} - updated siebel URL from config map to {}",
                        sblEnv, siebelConfig.getSblServerURL());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "URL:", siebelConfig.getSblServerURL()));
            }

            /*Update sftp config based on request*/
            sftpConfig.setSblFtpServerUser(sblSftpLogin);
            sftpConfig.setFtpPassword(sblSftpPasswrd);
            if (StringUtils.isNotBlank(sblSrvrMap.get(sblEnv
                    + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX))) {

                sftpConfig.setSblFtpServer(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX));
                logger.info(
                        "Request header SblEnv value: {} - updated SFTP server from config map to {}",
                        sblEnv, sftpConfig.getSblFtpServer());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "SFTP server:", sftpConfig.getSblFtpServer()));
            }
            logger.debug(sftpConfig.toString());

            attachConfig.setSblAttachType(StrataWSConstants.SR_VALUE);

            serviceRequestHlpr = srService.getSrDetails(srNumber, siebelConfig);
            String srSblRowId = serviceRequestHlpr.getSblRowId();

            if (StringUtils.isNotBlank(srSblRowId)) {
                attachConfig.setSblParentRowId(srSblRowId);
                logger.debug(attachConfig.toString());
            } else {
                String errMsg = "error-->" + StrataWSConstants.SR_VALUE + " "
                        + StrataWSConstants.NOT_FOUND_IN_SBL_MSG;
                logger.error(errMsg + " for SR#: {}", srNumber);
                return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
            }

            Map<String, String> attachMap = attachmentService.fetchAttachmentPaths(srSblRowId,
                    siebelConfig, attachConfig);
            if (attachMap.isEmpty()) {
                logger.info("No attachments Found for SR# {}.", srNumber);
                String errMsg = "No attachments Found";
                return new ResponseEntity<>(errMsg, HttpStatus.NO_CONTENT);
            }

            List<String> filelist = new ArrayList<String>(attachMap.values());
            String fileName = srNumber + ".zip";
            /*String zipFilePath = SftpFileDowload
                    .downloadAsZipViaJSchSFTP(activityId, filelist, ftpConfig);*/

            String zipFilePath = attachmentService.fetchAttachmentsAsZip(srNumber, filelist, sftpConfig);

            if (StringUtils.isNotBlank(zipFilePath)) {
                File result = new File(zipFilePath);
                if (result.exists()) {
                    InputStream in = new FileInputStream(result);
                    byte[] out = IOUtils.toByteArray(in);

                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.add("content-disposition", "attachment; filename=" + fileName);
                    responseHeaders.add("Content-Type", "application/zip");

                    respEntity = new ResponseEntity<byte[]>(out, responseHeaders, HttpStatus.OK);
                    logger.info("Attachments were successfully downloaded from Siebel. Returning zip file");
                    return respEntity;
                } else {
                    logger.error("File {} was not found at the given path.", result.getPath());
                    String errMsg = StrataWSConstants.FILE_NOT_FOUND_MSG;
                    return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
                }
            } else {
                logger.error("Zip file path {} is null or blank.", zipFilePath);
                String errMsg = "Unable to return the attachments";
                return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
            }

        } catch (NotFoundException ne) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            logger.error("NotFoundException in AttachmentController::fetchSrAttachments()--> ", ne);
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in AttachmentController::fetchSrAttachments()--> ", ase);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            String errMsg = StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in AttachmentController::fetchSrAttachments()--> ", e);
            return new ResponseEntity<>(errMsg, HttpStatus.OK);
        }

    }

    /**
     * Controller method for activity attachment upload
     * 
     * @param clientName
     * @param activityId
     * @param file
     * @param sblEnv
     * @param sblLoginId
     * @param sblPasswrd
     * @param sblSftpLogin
     * @param sblSftpPasswrd
     * @return ResponseEntity
     * @author Amal Varghese
     */
    @PostMapping(value = "/activity/attach/{client}/{id}")
    public @ResponseBody ResponseEntity<?> uploadActAttachment(@PathVariable("client") String clientName,
            @PathVariable("id") String activityId, @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestHeader(value = "Siebel-SFTP-Login-Id") String sblSftpLogin,
            @RequestHeader(value = "Siebel-SFTP-Password") String sblSftpPasswrd) {

        /*Check if Siebel credentials present in header */
        if (StringUtils.isBlank(sblLoginId) || StringUtils.isBlank(sblPasswrd)
                || StringUtils.isBlank(sblEnv)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error(errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }
        /*Check if Siebel SFTP credentials present in header */
        if (StringUtils.isBlank(sblSftpLogin) || StringUtils.isBlank(sblSftpPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_FS_CRED_MISSING_MSG;
            logger.error(errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> respEntity = null;
        /*File receivedFile = null;*/
        String responseStr = "";
        InputStream recInputStream = null;

        logger.info(attachConfig.toString());

        if (!file.isEmpty()) {
            try {

                String fileExtn = FilenameUtils.getExtension(file.getOriginalFilename());
                if (!checkValidContentType(allowedFileTypes, fileExtn)) {
                    String errMsg = "error-->" + StrataWSConstants.FILE_EXTN_INVALID_MSG
                            + ". Supported types: " + allowedFileTypes.toString();
                    logger.error("Error in AttachmentController::uploadActAttachment()--> " + errMsg);
                    return new ResponseEntity<>(errMsg, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }

                byte[] recFilebytes = file.getBytes();
                logger.info("File {} received having size {} bytes", file.getOriginalFilename(),
                        file.getSize());
                recInputStream = new ByteArrayInputStream(recFilebytes);

                if (recInputStream.available() <= 0) {
                    String errMsg = "error-->" + StrataWSConstants.FILE_INPUT_FAILED_MSG;
                    logger.error("Error in AttachmentController::uploadActAttachment() - No bytes available for Inputstream to read: "
                            + errMsg);
                    return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                /*Update siebel config based on request data*/
                siebelConfig.setLogin(sblLoginId);
                siebelConfig.setSblPasswrd(sblPasswrd);
                siebelConfig.setSblEnv(sblEnv);
                /* Will try to fetch server URL from sblSrvrMap config*/
                if (StringUtils.isNotBlank(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                    siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                            + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                    logger.info(
                            "Request header SblEnv value: {} - updated siebel URL from config map to {}",
                            sblEnv, siebelConfig.getSblServerURL());
                } else {
                    logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                            "URL:", siebelConfig.getSblServerURL()));
                }

                /*Update Attach config based on request */
                attachConfig.setFileName(file.getOriginalFilename());
                attachConfig.setSblParentRowId(activityId);
                attachConfig.setSblAttachType(StrataWSConstants.ACTIVITY_VALUE);
                attachConfig.setFileExt(file.getContentType());
                attachConfig.setFileSize((int) file.getSize());
                /*logger.debug(attachConfig.toString());*/

                /*Update sftp config based on request*/
                sftpConfig.setSblFtpServerUser(sblSftpLogin);
                sftpConfig.setFtpPassword(sblSftpPasswrd);
                if (StringUtils.isNotBlank(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX))) {

                    sftpConfig.setSblFtpServer(sblSrvrMap.get(sblEnv
                            + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX));
                    logger.info(
                            "Request header SblEnv value: {} - updated SFTP server from config map to {}",
                            sblEnv, sftpConfig.getSblFtpServer());
                } else {
                    logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                            "SFTP server:", sftpConfig.getSblFtpServer()));
                }
                logger.debug(sftpConfig.toString());

                /*file.transferTo(receivedFile);*/

                if (!activityService.checkActivityValidity(activityId, siebelConfig)) {
                    throw new NotFoundException(StrataWSConstants.ACTIVITY_VALUE, true);
                }

                responseStr = attachmentService.uploadAttachment(activityId, recInputStream,
                        siebelConfig, sftpConfig, attachConfig);
                respEntity = new ResponseEntity<>(responseStr, HttpStatus.OK);
                recInputStream.close();
            } catch (AncestorSiebelUtilityException ase) {

                String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                        + ase.getErrorMessage();
                logger.error("SiebelUtilityException in AttachmentController::uploadActAttachment()--> ",
                        ase);
                return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {

                String errMsg = "error--> " + StrataWSConstants.FILE_UPLOAD_FAILED_MSG + " : "
                        + e.getMessage();
                logger.error("Exception in AttachmentController::uploadActAttachment()--> " + e);
                respEntity = new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                IOUtils.closeQuietly(recInputStream);
            }
        } else {
            String errMsg = "error--> " + StrataWSConstants.FILE_EMPTY_MSG;
            logger.error("Error in AttachmentController::uploadActAttachment()--> " + errMsg);
            respEntity = new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }
        return respEntity;
    }

    /**
     * Controller method for SR attachment upload
     * 
     * @param clientName
     * @param srNum
     * @return
     */
    @PostMapping(value = "/svreq/attach/{client}/{srNum}")
    public @ResponseBody ResponseEntity<?> uploadSrAttachment(@PathVariable("client") String clientName,
            @PathVariable("srNum") String srNum, @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestHeader(value = "Siebel-SFTP-Login-Id") String sblSftpLogin,
            @RequestHeader(value = "Siebel-SFTP-Password") String sblSftpPasswrd) {

        /*Check if Siebel credentials present in header */
        if (StringUtils.isBlank(sblLoginId) || StringUtils.isBlank(sblPasswrd)
                || StringUtils.isBlank(sblEnv)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error(errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }
        /*Check if Siebel SFTP credentials present in header */
        if (StringUtils.isBlank(sblSftpLogin) || StringUtils.isBlank(sblSftpPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_FS_CRED_MISSING_MSG;
            logger.error(errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }
        /*Check if SR Number is present in the request*/
        if (StringUtils.isBlank(srNum)) {
            String errMsg = "error-->" + SiebelConstants.SR_NUM_FIELD_NAME + ""
                    + StrataWSConstants.NULL_OR_BLANK_MSG;
            logger.error(errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> respEntity = null;
        /*File receivedFile = null;*/
        String responseStr = "";
        InputStream recInputStream = null;
        ServiceRequestHlpr serviceRequestHlpr = null;

        logger.info(attachConfig.toString());

        if (!file.isEmpty()) {
            try {
                /*Check if file is of a valid type*/
                String fileExtn = FilenameUtils.getExtension(file.getOriginalFilename());
                if (!checkValidContentType(allowedFileTypes, fileExtn)) {
                    String errMsg = "error-->" + StrataWSConstants.FILE_EXTN_INVALID_MSG
                            + ". Supported: " + allowedFileTypes.toString();
                    logger.error("Error in AttachmentController::uploadSrAttachment() - " + errMsg);
                    return new ResponseEntity<>(errMsg, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }
                byte[] recFilebytes = file.getBytes();
                logger.info("File {} of type {} received having size {} bytes",
                        file.getOriginalFilename(), file.getContentType(), file.getSize());
                recInputStream = new ByteArrayInputStream(recFilebytes);

                if (recInputStream.available() <= 0) {
                    String errMsg = "error-->" + StrataWSConstants.FILE_INPUT_FAILED_MSG;
                    logger.error("Error in AttachmentController::uploadSrAttachment() - No bytes available for Inputstream to read: "
                            + errMsg);
                    return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                /*Update siebel config based on request data*/
                /* Will try to fetch server URL from sblSrvrMap config*/
                if (StringUtils.isNotBlank(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                    siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                            + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                    logger.info(
                            "Request header SblEnv value: {} - updated siebel URL from config map to {}",
                            sblEnv, siebelConfig.getSblServerURL());
                } else {
                    logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                            "URL:", siebelConfig.getSblServerURL()));
                }
                siebelConfig.setLogin(sblLoginId);
                siebelConfig.setSblPasswrd(sblPasswrd);
                siebelConfig.setSblEnv(sblEnv);

                /*Update Attach config based on request */
                attachConfig.setFileName(file.getOriginalFilename());
                attachConfig.setSblAttachType(StrataWSConstants.SR_VALUE);
                attachConfig.setFileExt(file.getContentType());
                attachConfig.setFileSize((int) file.getSize());
                /*logger.debug(attachConfig.toString());*/

                /*Update sftp config based on request*/
                if (StringUtils.isNotBlank(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX))) {

                    sftpConfig.setSblFtpServer(sblSrvrMap.get(sblEnv
                            + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX));
                    logger.info(
                            "Request header SblEnv value: {} - updated SFTP server from config map to {}",
                            sblEnv, sftpConfig.getSblFtpServer());
                } else {
                    logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                            "SFTP server:", sftpConfig.getSblFtpServer()));
                }
                /*if (sblSrvrMap.containsKey(sblEnv + "_SBL_SRVR_NAME")) {
                    sftpConfig.setSblFtpServer(sblSrvrMap.get(sblEnv + "_SBL_SRVR_NAME"));
                }*/
                sftpConfig.setSblFtpServerUser(sblSftpLogin);
                sftpConfig.setFtpPassword(sblSftpPasswrd);
                logger.debug(sftpConfig.toString());

                serviceRequestHlpr = srService.getSrDetails(srNum, siebelConfig);
                String srSblRowId = serviceRequestHlpr.getSblRowId();

                if (StringUtils.isNotBlank(srSblRowId)
                        && !serviceRequestHlpr.getSblSrStatus().equalsIgnoreCase(
                                SiebelConstants.SBL_CLOSED_SR_STATUS)) {

                    attachConfig.setSblParentRowId(srSblRowId);
                    logger.debug(attachConfig.toString());
                    responseStr = attachmentService.uploadNewAttachment(srSblRowId, recInputStream,
                            siebelConfig, sftpConfig, attachConfig);
                    respEntity = new ResponseEntity<String>(responseStr, HttpStatus.OK);
                    recInputStream.close();
                } else {
                    String errMsg = "error-->" + StrataWSConstants.SR_VALUE + " "
                            + StrataWSConstants.NOT_FOUND_IN_SBL_MSG + " or "
                            + StrataWSConstants.SR_CLOSED_MSG;
                    logger.error(errMsg + " for SR#: {}", srNum);
                    return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
                }
            } catch (AncestorSiebelUtilityException ase) {

                String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                        + ase.getErrorMessage();
                logger.error("SiebelUtilityException in AttachmentController::uploadSrAttachment()--> ",
                        ase);
                return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {

                String errMsg = "error-->" + StrataWSConstants.FILE_UPLOAD_FAILED_MSG + " : "
                        + e.getMessage();
                logger.error("Exception in AttachmentController::uploadSrAttachment()--> " + e);
                return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                IOUtils.closeQuietly(recInputStream);
            }
        } else {
            String errMsg = "error--> " + StrataWSConstants.FILE_EMPTY_MSG;
            logger.error("Error in AttachmentController::uploadSrAttachment()--> " + errMsg);
            respEntity = new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }

        return respEntity;
    }

    private Boolean checkValidContentType(List<String> allowedFileTypeList, String fileExtn) {
        logger.info("Checking if file is of a valid Type. Allowed File Types: "
                + allowedFileTypes.toString());
        try {
            if (allowedFileTypeList.contains(fileExtn)) {
                logger.info("File is of valid type");
                return true;
            } else {
                logger.info(StrataWSConstants.FILE_EXTN_INVALID_MSG);
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception in checkValidContentType(): " + e);
            return false;
        }
    }
    /* @GetMapping(value = "/attach/{id}", headers = "Accept=application/json")
    @GetMapping(value = "/attach/{client}/{id}")
    public @ResponseBody ResponseEntity fetchAttachments(@PathVariable("client") String clientName,
            @PathVariable("id") String activityId
            ) {

        logger.debug("In controller SvreqRestController - fetchAttachments()");
        logger.debug("Client Name: {} & Activity Id: {}", clientName, activityId);
        ResponseEntity respEntity = null;
        try {
            AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class,
                    SFTPConfig.class);

            SiebelConfig siebelConfig = (SiebelConfig) context.getBean(SiebelConfig.class);
            SFTPConfig ftpConfig = (SFTPConfig) context.getBean(SFTPConfig.class);

             Map<String, String> attachMap = new HashMap<String, String>();
            SiebelUtility siebelUtility = new SiebelUtility(siebelConfig);
            Map<String, String> attachMap = siebelUtility.fetchActivityAttachPaths(activityId);

            Map<String, String> attachMap = attachmentService.fetchAttachmentPaths(activityId,
                    siebelConfig);
            if (attachMap.isEmpty()) {
                logger.info("No attachments Found for activity: {}.", activityId);
                String errorMessage = "No attachments Found";
                return new ResponseEntity<>(errorMessage, HttpStatus.OK);
            }

            List<String> filelist = new ArrayList<String>(attachMap.values());
            String fileName = activityId + ".zip";
            String zipFilePath = SftpFileDowload
                    .downloadAsZipViaJSchSFTP(activityId, filelist, ftpConfig);

            String zipFilePath = attachmentService.fetchAttachmentsAsZip(activityId, filelist, ftpConfig);

            if (StringUtils.isNotBlank(zipFilePath)) {
                File result = new File(zipFilePath);
                if (result.exists()) {
                    InputStream in = new FileInputStream(result);
                    byte[] out = IOUtils.toByteArray(in);

                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.add("content-disposition", "attachment; filename=" + fileName);
                    responseHeaders.add("Content-Type", "application/zip");

                    respEntity = new ResponseEntity<>(out, responseHeaders, HttpStatus.OK);
                } else {
                    logger.error("File {} was not found at the given path.", result.getPath());
                    String errorMessage = "File not found";
                    respEntity = new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
                }
            } else {
                logger.error("Zip file path {} is null or blank.", zipFilePath);
                String errorMessage = "Unable to return the attachments";
                respEntity = new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
            }

        }

        catch (Exception e) {
            String errorMessage = StrataWSConstants.GENERIC_ERROR_MSG + " : " + e;
            return new ResponseEntity<>(errorMessage, HttpStatus.OK);
        }
        return respEntity;

    }*/
}
