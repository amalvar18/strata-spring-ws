package com.newco.strataws.common.util;

import java.io.File;
import java.io.InputStream;
import java.security.Security;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.file.AttachConfig;
import com.newco.strataws.config.sftp.SFTPConfig;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * SFTP file upload operations
 * 
 * @author Amal Varghese
 *
 */
public class SftpFileUpload {

    private static final Logger logger = LoggerFactory.getLogger(SftpFileUpload.class);

    /**
     * Uploads a File received. File needs to be provided as an InputStream
     * 
     * @param recInputStream
     * @param sftpConfig
     * @param attachConfig
     * @return true if file upload is successful, false otherwise
     * @throws Exception
     * @author Amal Varghese
     */
    public static Boolean uploadFileViaJSchSFTP(InputStream recInputStream, SFTPConfig sftpConfig,
            AttachConfig attachConfig) throws Exception {

        JSch jsch = new JSch();
        Session jSession = null;
        Channel channel = null;
        ChannelSftp sftpChannel = null;

        Boolean upFlag = false;

        String sftpUser = sftpConfig.getSblFtpServerUser();
        Integer sftpPort = sftpConfig.getSblFtpPort();
        String sftpServer = sftpConfig.getSblFtpServer();
        String sftpPassword = sftpConfig.getFtpPassword();
        /*String remoteDir = sftpConfig.getSblFtpRemoteDir();*/

        logger.info("Server name: " + sftpServer);

        logger.debug("Using SftpConfig--> {}", sftpConfig.toString());

        if (StringUtils.isNotBlank(sftpUser) && StringUtils.isNotBlank(sftpPassword)
                && StringUtils.isNotBlank(sftpServer) && sftpPort > 1) {

            try {
                /* Use bouncycastle as security provider to resolve Deffie-Hellman Key Error -
                 * "java.security.InvalidAlgorithmParameterException: Prime size must be multiple of 64..." */
                Security.insertProviderAt(new BouncyCastleProvider(), 1);

                jSession = jsch.getSession(sftpUser, sftpServer, sftpPort);
                jSession.setConfig("StrictHostKeyChecking", "no");
                jSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                jSession.setPassword(sftpPassword);

                jSession.connect();
                channel = jSession.openChannel("sftp");
                channel.connect();
                sftpChannel = (ChannelSftp) channel;

                String fileName = attachConfig.getFileName();
                logger.info("File name bring uploaded: {}", fileName);

                if (recInputStream == null) {
                    logger.debug("InputStream is null");
                }

                logger.debug("SFTP Channel local dir: {}", sftpChannel.lpwd());
                logger.debug("SFTP Channel remote dir: {}", sftpChannel.pwd());

                sftpChannel.put(recInputStream, fileName);
                upFlag = true;
            } catch (JSchException jsEx) {
                logger.error("Jsch Error: " + jsEx.toString());
                throw new JSchException(jsEx.toString());
            } catch (SftpException sftpEx) {
                logger.error("SFTP Error: " + sftpEx.toString());
                throw new SftpException(0, sftpEx.toString());
            } catch (Exception e) {
                logger.error("Exception occurred in downloadFileViaJSchSFTP: {}", e);
                throw new Exception(e);
            } finally {
                if (sftpChannel != null && sftpChannel.isConnected()) {
                    logger.debug("Exiting SFTP channel");
                    sftpChannel.exit();
                }
                if (channel != null && channel.isConnected()) {
                    logger.debug("Disconnecting channel");
                    channel.disconnect();
                }
                if (jSession != null && jSession.isConnected()) {
                    logger.debug("Disconnecting jsch session");
                    jSession.disconnect();
                }
            }
        } else {
            logger.info("FTP Server name Or Username or password value is blank");
            throw new Exception(StrataWSConstants.GENERIC_ERROR_MSG
                    + " FTP Server name Or Username or password value is blank.");
        }
        return upFlag;
    }

    public static Boolean uploadTestFileViaJSchSFTP(String localFilePath, String sftpServer,
            SFTPConfig sftpConfig) throws Exception {

        JSch jsch = new JSch();
        Session jSession = null;
        Channel channel = null;
        ChannelSftp sftpChannel = null;
        File file = null;
        Boolean upFlag = false;

        String sftpUser = sftpConfig.getSblFtpServerUser();
        Integer sftpPort = sftpConfig.getSblFtpPort();
        String sealedFileName = sftpConfig.getFtpSealedFile();
        String secretFileName = sftpConfig.getFtpSecretFile();
        /*String sftpServer = sftpConfig.getSblFtpServer();*/
        sftpConfig.setFtpPassword(AuthUtils.decryptPassword(secretFileName, sealedFileName));
        String sftpPassword = sftpConfig.getFtpPassword();
        @SuppressWarnings("unused")
        String localDir = sftpConfig.getSftpLocalDir();
        String remoteDir = sftpConfig.getSblFtpRemoteDir();
        /*String defAttachUplFile = sftpConfig.getDefaultAttachUploadFile();*/

        /*To Remove*/
        if (StringUtils.isBlank(localFilePath)) {
            logger.info("File path is null - Using default upload file");
            /*localFilePath = localDir + defAttachUplFile;*/
        }

        logger.info("Server name: " + sftpServer);

        logger.debug("Using SftpConfig--> {}", sftpConfig.toString());

        if (StringUtils.isNotBlank(sftpUser) && StringUtils.isNotBlank(sftpPassword)
                && StringUtils.isNotBlank(sftpServer) && sftpPort > 1) {

            try {
                /* Use bouncycastle as security provider to resolve Deffie-Hellman Key Error -
                 * "java.security.InvalidAlgorithmParameterException: Prime size must be multiple of 64..." */
                Security.insertProviderAt(new BouncyCastleProvider(), 1);

                jSession = jsch.getSession(sftpUser, sftpServer, sftpPort);
                jSession.setConfig("StrictHostKeyChecking", "no");
                jSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                jSession.setPassword(sftpPassword);

                jSession.connect();
                channel = jSession.openChannel("sftp");
                channel.connect();
                sftpChannel = (ChannelSftp) channel;

                /*logger.debug("Start Index--> {} | End Index {}",
                        sftpConfig.getSblFtpRemoteDir().length(), localFilePath.indexOf('_'));*/

                logger.info("Local file path: " + localFilePath);
                String fileName = FilenameUtils.getName(localFilePath);
                logger.info("File name bring uploaded: {}", fileName);

                /*String newPath = remoteDir + fileName;*/
                logger.info("Remote dir on server from prop file : {}", remoteDir);
                String newPath = remoteDir.replaceAll(StrataWSConstants.UNIX_PATH_PREFIX,
                        StrataWSConstants.WINDOWS_PATH_PREFIX);
                logger.info("The new path on ftp server: {}", newPath);

                /*String fileName = newPath.substring(sftpConfig.getSblFtpRemoteDir().length(),
                        newPath.length());*/
                file = new File(localFilePath);
                if (file.exists()) {
                    logger.debug("File found on disk");
                }

                logger.debug("Channel local dir: {}", sftpChannel.lpwd());
                sftpChannel.lcd(FilenameUtils.getFullPath(localFilePath));

                /*logger.debug("Channel remote dir: {}", sftpChannel.pwd());
                sftpChannel.cd(newPath);
                logger.debug("Channel remote dir after update: {}", sftpChannel.pwd());*/
                /*sftpChannel.cd(remoteDir);*/

                logger.debug("Final Channel local dir: {}", sftpChannel.lpwd());
                logger.debug("Final Channel remote dir: {}", sftpChannel.pwd());
                sftpChannel.put(localFilePath, fileName);
                /*sftpChannel.rm(localFilePath);*/
                upFlag = true;
            } catch (JSchException jsEx) {
                logger.error("Jsch Error: " + jsEx.toString());
                throw new JSchException(jsEx.toString());
            } catch (SftpException sftpEx) {
                logger.error("SFTP Error: " + sftpEx.toString());
                throw new SftpException(0, sftpEx.toString());
            } catch (Exception e) {
                logger.error("Exception occurred in downloadFileViaJSchSFTP: {}", e);
                throw new Exception(e);
            } finally {
                if (sftpChannel != null && sftpChannel.isConnected()) {
                    logger.debug("Exiting SFTP channel");
                    sftpChannel.exit();
                }
                if (channel != null && channel.isConnected()) {
                    logger.debug("Disconnecting channel");
                    channel.disconnect();
                }
                if (jSession != null && jSession.isConnected()) {
                    logger.debug("Disconnecting jsch session");
                    jSession.disconnect();
                }
            }
        } else {
            logger.info("FTP Server name Or Username or password value is blank");
            throw new Exception(StrataWSConstants.GENERIC_ERROR_MSG
                    + " FTP Server name Or Username or password value is blank.");
        }
        return upFlag;
    }
}
