package com.newco.strataws.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.common.util.AuthUtils;
import com.newco.strataws.common.util.SftpFileDowload;
import com.newco.strataws.common.util.SftpFileUpload;
import com.newco.strataws.config.AppConfig;
import com.newco.strataws.config.file.AttachConfig;
import com.newco.strataws.config.sftp.SFTPConfig;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.service.AttachmentService;
import com.newco.strataws.service.SrService;
import com.newco.strataws.siebel.AttachUtility;

/*@Controller
@RequestMapping("/siebel")*/
public class GenericTestController {

    @Autowired
    private TestSiebelUtility createSR;

    @Autowired
    SrService srService;

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    SiebelConfig siebelConfig;

    @Autowired
    SFTPConfig ftpConfig;

    @Autowired
    AttachConfig attachConfig;

    private static final Logger logger = LoggerFactory.getLogger(GenericTestController.class);

    @RequestMapping("/testSR")
    public String siebelLogin() {

        System.out.println("In siebellogin() of controller");
        /* TestSiebelUtility createSR = new TestSiebelUtility(); */
        try {
            createSR.createSR();
        } catch (Exception e) {

            e.printStackTrace();
        }

        return "/index";
    }

    @RequestMapping(value = "/testAttachZip/{id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity testAttachZip(@PathVariable String id) {

        ResponseEntity respEntity = null;
        try {
            AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class,
                    SFTPConfig.class);

            SiebelConfig siebelConfig = (SiebelConfig) context.getBean(SiebelConfig.class);
            SFTPConfig ftpConfig = (SFTPConfig) context.getBean(SFTPConfig.class);

            Map<String, String> attachMap = new HashMap<String, String>();
            AttachUtility attachUtility = new AttachUtility(siebelConfig);
            attachMap = (Map<String, String>) attachUtility.fetchActivityAttachPaths(id);
            if (attachMap.isEmpty()) {
                logger.info("No attachments Found for activity: {}.", id);
                String errorMessage = "No attachments Found";
                return new ResponseEntity<>(errorMessage, HttpStatus.OK);
            }

            List<String> filelist = new ArrayList<String>(attachMap.values());
            String fileName = id + ".zip";
            String zipFilePath = SftpFileDowload.downloadAsZipViaJSchSFTP(id, filelist, ftpConfig);

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
                    String errorMessage = "File not found";
                    respEntity = new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
                }
            } else {
                String errorMessage = "Unable to return the attachments";
                respEntity = new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
            }

        }

        catch (Exception e) {
            String errorMessage = StrataWSConstants.GENERIC_ERROR_MSG + " : " + e;
            return new ResponseEntity<>(errorMessage, HttpStatus.OK);
        }
        return respEntity;
    }

    @RequestMapping(value = "/testAttach/{id}", method = RequestMethod.GET)
    public @ResponseBody Map testAttach(@PathVariable String id) {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class,
                SFTPConfig.class);

        Map<String, String> attachMap = new HashMap<String, String>();

        try {
            AttachUtility attachUtility = new AttachUtility(siebelConfig);

            attachMap = (Map<String, String>) attachUtility.fetchActivityAttachPaths(id);

            for (Map.Entry<String, String> entry : attachMap.entrySet()) {
                logger.debug(entry.getKey() + " : " + entry.getValue());

                String fileRemotePath = entry.getValue();

                Boolean fileDownloadFlag = false;

                fileDownloadFlag = SftpFileDowload.downloadFileViaJSchSFTP(fileRemotePath, ftpConfig);

                if (fileDownloadFlag) {
                    logger.info("Received file " + entry.getKey());
                } else {
                    logger.info("Failed to download file " + entry.getKey());
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        } finally {

            context.destroy();
            /*Thread.currentThread().stop();*/

        }
        return attachMap;
    }

    @RequestMapping(value = "/testUpload/{id}", method = RequestMethod.POST)
    public @ResponseBody String testAttachUpload(@PathVariable String id) {

        /* AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class,
                 SFTPConfig.class);*/

        String responseString = "";
        Boolean fileUpFlag = false;

        try {

            logger.debug("In Controller - testAttachUpload()");

            String fileToUpload = "";

            fileToUpload = attachConfig.getDefaultAttachUplFile();

            String srvrFilePath = ftpConfig.getSblFtpRemoteDir() + FilenameUtils.getName(fileToUpload);

            fileUpFlag = SftpFileUpload.uploadTestFileViaJSchSFTP(fileToUpload,
                    ftpConfig.getSblFtpServer(), ftpConfig);
            if (fileUpFlag) {
                logger.info("Uploaded file to server. Updating Siebel records");
                siebelConfig.setLogin(StrataWSConstants.DEFAULT_BAC_SBL_LOGIN);
                siebelConfig.setSblPasswrd(AuthUtils.decryptPassword(siebelConfig.getSecretFile(),
                        siebelConfig.getSealedFile()));
                AttachUtility attachUtility = new AttachUtility(siebelConfig);
                responseString = attachUtility.uploadActivityAttachment(id, srvrFilePath);
                fileUpFlag = true;
            }

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        } finally {
            if (fileUpFlag) {
                logger.info("Uploaded file " + responseString);
                responseString = "File Upload Success | Name :" + responseString;
            } else {
                logger.info("Failed to upload file " + responseString);
            }
            /*context.close();*/
            /*Thread.currentThread().stop();*/
        }
        return responseString;
    }

    /*@RequestMapping(value = "/testAttach/{id}", method = RequestMethod.GET)
    public @ResponseBody Map testAttach(@PathVariable String id) {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class,
                SFTPConfig.class);

         SiebelConfig siebelConfig = (SiebelConfig) context.getBean(SiebelConfig.class);

         SFTPConfig ftpConfig = (SFTPConfig) context.getBean(SFTPConfig.class);

        Map<String, String> attachMap = new HashMap<String, String>();

         SessionFactory<LsEntry> sessionFactory = context.getBean(CachingSessionFactory.class); 

         DefaultSftpSessionFactory defSessionFactory = (DefaultSftpSessionFactory)
         * context.getBean("sftpSessionFactory", CachingSessionFactory.class); 

         QueueChannel localFileChannel = context.getBean(QueueChannel.class); MessageHandler handler =
         * (MessageHandler) context.getBean("resultFileHandler"); 

         SourcePollingChannelAdapter adapter = context.getBean(SourcePollingChannelAdapter.class); 

        try {
            SiebelUtility siebelUtility = new SiebelUtility(siebelConfig);

            attachMap = siebelUtility.fetchActivityAttachPaths(id);

             PollableChannel localPFileChannel = context.getBean("fromSftpChannel", PollableChannel.class); 

             Message<?> received; 

            ftpConfig.setSftpLocalDir(ftpConfig.getSftpLocalDir().concat(id));

              for (Map.Entry<String, String> entry : attachMap.entrySet()) {
                  logger.debug(entry.getKey() + " : " + entry.getValue());

                   String fileRemotePath = entry.getValue(); 

                  Boolean fileDownloadFlag = false;

                 

                   fileDownloadFlag = SftpFileDowload.downloadFileViaJSchSFTP(fileRemotePath, ftpConfig); 

                 

                   String sblFtpRemoteDir = context.getBean(SFTPConfig.class) .getSblFtpRemoteDir().toLowerCase();
                   * ftpConfig.setSblFtpRemoteDir(sblFtpRemoteDir); logger.debug("Remote Directory: " + sblFtpRemoteDir);
                   * int sblFtpRemoteDirLen = sblFtpRemoteDir.length(); String serverName =
                   * fileRemotePath.substring(sblFtpRemoteDir.length(), fileRemotePath.indexOf('_'));
                   * logger.info("Server: {} ", serverName); Message<?> received = localFileChannel.receive(10000);
                   * handler.handleMessage(received); 

                   if (fileDownloadFlag) { logger.info("Received file " + entry.getKey()); } else {
                   * logger.info("Failed to download file " + entry.getKey()); } 
              }

            List<String> filelist = new ArrayList<String>(attachMap.values());
            String str = SftpFileDowload.downloadAsZipViaJSchSFTP(id, filelist, ftpConfig);

             return attachMap; 
        } catch (Exception e) {

            e.printStackTrace();

            return null;
        } finally {
             localFileChannel.clear(); 
            context.destroy();
            Thread.currentThread().stop();

        }
        return attachMap;
    }*/
}
