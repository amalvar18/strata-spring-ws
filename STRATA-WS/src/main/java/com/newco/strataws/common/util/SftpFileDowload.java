package com.newco.strataws.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Security;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.sftp.SFTPConfig;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpFileDowload {

    private static final Logger logger = LoggerFactory.getLogger(SftpFileDowload.class);

    public static Boolean downloadFileViaJSchSFTP(String filePath, SFTPConfig sftpConfig)
            throws Exception {

        JSch jsch = new JSch();
        Session jSession = null;
        Channel channel = null;
        ChannelSftp sftpChannel = null;
        File file = null;
        Boolean downFlag = false;

        try {

            logger.info("Downloading file via Jsch SFTP: " + filePath);

            logger.debug("Start Index--> {} | End Index {}", sftpConfig.getSblFtpRemoteDir().length(),
                    filePath.indexOf('_'));

            String sftpUser = sftpConfig.getSblFtpServerUser();
            Integer sftpPort = sftpConfig.getSblFtpPort();
            String sftpServer = sftpConfig.getSblFtpServer();
            String sftpPassword = sftpConfig.getFtpPassword();
            String localDir = sftpConfig.getSftpLocalDir();

            logger.debug("Using SftpConfig--> {}", sftpConfig.toString());

            Security.insertProviderAt(new BouncyCastleProvider(), 1);
            logger.info("Using Username--> {}", sftpUser);

            sftpServer = filePath.substring(sftpConfig.getSblFtpRemoteDir().length(),
                    filePath.indexOf('_'));
            logger.info("Server name: " + sftpServer);

            jSession = jsch.getSession(sftpUser, sftpServer, sftpPort);
            /* logger.debug("Using Password--> {}", sftpPassword); */
            jSession.setConfig("StrictHostKeyChecking", "no");
            jSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            jSession.setPassword(sftpPassword);

            /*java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");*/

            if (StringUtils.isNotBlank(sftpUser) && StringUtils.isNotBlank(sftpPassword)
                    && StringUtils.isNotBlank(sftpServer) && sftpPort > 1) {
                jSession.connect();
                channel = jSession.openChannel("sftp");
                channel.connect();
                sftpChannel = (ChannelSftp) channel;

                String newPath = filePath.replaceAll("\\\\", "/");
                logger.info("The new path is {}", newPath);

                String fileName = newPath.substring(sftpConfig.getSblFtpRemoteDir().length(),
                        newPath.length());
                logger.info("File name is {}", fileName);

                File outputDir = new File(localDir);

                if (!outputDir.exists()) {
                    outputDir.mkdir();
                    logger.info("Output Directory successfully created");
                }

                String outputFilePath = localDir + "/" + fileName;
                logger.info("Writing to path: {}", outputFilePath);

                /* String outputFileName = generateOutputFilePath(sftpConfig.getSftpLocalDir(), filename); */

                file = new File(outputFilePath);

                sftpChannel.get(fileName, new FileOutputStream(file));
                sftpChannel.rm(fileName);
                downFlag = true;
            } else {
                logger.info("FTP Servername Or Username or password value is blank");
            }
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
            exitSftpChannel(sftpChannel);
            disconnectChannel(channel);
            disconnectSession(jSession);
            /*if (sftpChannel != null) {
                sftpChannel.exit();
            }
            if (channel != null) {
                channel.disconnect();
            }
            if (jSession != null && jSession.isConnected()) {
                jSession.disconnect();
            }*/
        }
        return downFlag;
    }

    /**
     * Downloads all attachments and returns them in a zip file. Download is via SFTP using jsch
     * 
     * @param parentNum
     *            SR Number or Activity Id
     * @param filePaths
     * @param sftpConfig
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public static String downloadAsZipViaJSchSFTP(String parentNum, List<String> filePaths,
            SFTPConfig sftpConfig) throws Exception {

        JSch jsch = new JSch();
        Session jSession = null;
        Channel channel = null;
        ChannelSftp sftpChannel = null;
        Boolean downFlag = false;
        String zipFileName = "";

        if (filePaths.isEmpty()) {
            return "";
        }

        try {

            String sftpUser = sftpConfig.getSblFtpServerUser();
            Integer sftpPort = sftpConfig.getSblFtpPort();
            String sftpServer = sftpConfig.getSblFtpServer();
            String sftpPassword = sftpConfig.getFtpPassword();
            String localDir = sftpConfig.getSftpLocalDir();

            logger.debug("Default SftpConfig--> {}", sftpConfig.toString());

            /* Get details of first file to make intial connection. 
             * The file path returned from Siebel is such that the server name is the string immediately 
             * following the directory path and immediately before the underscore character */
            String firstFilePath = filePaths.get(0);
            sftpServer = firstFilePath.substring(sftpConfig.getSblFtpRemoteDir().length(),
                    firstFilePath.indexOf('_'));
            logger.info("Initial Connection - Using Username {} on server {}", sftpUser, sftpServer);

            /* Use bouncycastle as security provider to resolve Deffie-Hellman Key Error -
             * "java.security.InvalidAlgorithmParameterException: Prime size must be multiple of 64..." */
            Security.insertProviderAt(new BouncyCastleProvider(), 1);

            jSession = jsch.getSession(sftpUser, sftpServer, sftpPort);
            /* logger.debug("Using Password--> {}", sftpPassword); */
            /* Disable strict host key checking */
            jSession.setConfig("StrictHostKeyChecking", "no");
            /* Use public key and override Kerberos prompt */
            jSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            jSession.setPassword(sftpPassword);

            if (StringUtils.isNotBlank(sftpUser) && StringUtils.isNotBlank(sftpPassword)
                    && StringUtils.isNotBlank(sftpServer) && sftpPort > 1) {

                jSession.connect();
                channel = jSession.openChannel("sftp");
                channel.connect();
                sftpChannel = (ChannelSftp) channel;

                byte[] buf = new byte[1024];
                zipFileName = localDir + parentNum + ".zip";
                logger.info("Writing to zip file: {}", zipFileName);

                /* Create directory if it doesn't exist */
                File localFileDir = new File(localDir);
                if (!localFileDir.exists()) {
                    localFileDir.mkdirs();
                }

                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));

                for (String filePath : filePaths) {

                    sftpServer = filePath.substring(sftpConfig.getSblFtpRemoteDir().length(),
                            filePath.indexOf('_'));
                    logger.info("Using Username {} on server {}", sftpUser, sftpServer);

                    /* Check if server has changed and restart new Jsch session if server has changed */
                    if (!sftpServer.equalsIgnoreCase(sftpConfig.getSblFtpServer())) {

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
                        jSession = jsch.getSession(sftpUser, sftpServer, sftpPort);

                        /* Disable strict host key checking */
                        jSession.setConfig("StrictHostKeyChecking", "no");
                        /* Use public key and override Kerberos prompt */
                        jSession.setConfig("PreferredAuthentications",
                                "publickey,keyboard-interactive,password");
                        jSession.setPassword(sftpPassword);

                        jSession.connect();
                        channel = jSession.openChannel("sftp");
                        channel.connect();
                        sftpChannel = (ChannelSftp) channel;
                    }

                    sftpConfig.setSblFtpServer(sftpServer);

                    logger.info("Downloading file via Jsch SFTP: " + filePath);
                    /*logger.debug("Start Index--> {} | End Index {}", sftpConfig.getSblFtpRemoteDir()
                            .length(), filePath.indexOf('_'));*/

                    String newPath = filePath.replaceAll("\\\\", "/");
                    logger.info("The new path is {}", newPath);

                    String fileName = newPath.substring(sftpConfig.getSblFtpRemoteDir().length(),
                            newPath.length());
                    logger.info("File name is {}", fileName);

                    InputStream inputStream = sftpChannel.get(fileName);
                    /*FileInputStream fin = new FileInputStream(fileName);*/
                    zos.putNextEntry(new ZipEntry(fileName));
                    int len;

                    /*while ((len = fin.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }*/
                    while ((len = inputStream.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }
                    zos.closeEntry();
                    /* fin.close(); */
                    inputStream.close();
                    sftpChannel.rm(fileName);
                }
                zos.close();
                downFlag = true;

            } else {
                logger.info("FTP Servername Or Username or password value is blank");
            }
        } catch (JSchException jsEx) {
            logger.error(StrataWSConstants.SFTP_REFUSED_CONNECTION_MSG + ": " + jsEx.toString());
            throw new JSchException(jsEx.toString());
        } catch (SftpException sftpEx) {
            logger.error(StrataWSConstants.FILE_DOWNLOAD_FAILED_MSG + ": " + sftpEx.toString());
            throw new SftpException(0, sftpEx.toString());
        } catch (Exception e) {
            logger.error(StrataWSConstants.GENERIC_ERROR_MSG + " in downloadFileViaJSchSFTP: {}", e);
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
            logger.debug("Download Flag value: " + downFlag);
        }
        return (downFlag) ? zipFileName : "";
    }

    /*    public static String downloadAsZipViaJSchSFTP(String activityId, List<String> filePaths,
                SFTPConfig sftpConfig) throws Exception {

            JSch jsch = new JSch();
            Session jSession = null;
            Channel channel = null;
            ChannelSftp sftpChannel = null;
            Boolean downFlag = false;
            String zipFileName = "";

            if (filePaths.isEmpty()) {
                return "";
            }

            try {

                String sftpUser = sftpConfig.getSblFtpServerUser();
                Integer sftpPort = sftpConfig.getSblFtpPort();
                String sftpServer = sftpConfig.getSblFtpServer();
                String sftpPassword = sftpConfig.getFtpPassword();
                String localDir = sftpConfig.getSftpLocalDir();

                logger.debug("Using SftpConfig--> {}", sftpConfig.toString());

                 Use bouncycastle as security provider to resolve Deffie-Hellman Key Error -
                 * "java.security.InvalidAlgorithmParameterException: Prime size must be multiple of 64..." 
                Security.insertProviderAt(new BouncyCastleProvider(), 1);
                logger.info("Using Username--> {}", sftpUser);

                jSession = jsch.getSession(sftpUser, sftpServer, sftpPort);

                 logger.debug("Using Password--> {}", sftpPassword); 
                 Disable strict host key checking 
                jSession.setConfig("StrictHostKeyChecking", "no");
                 Use public key and override Kerberos prompt 
                jSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                jSession.setPassword(sftpPassword);

                if (StringUtils.isNotBlank(sftpUser) && StringUtils.isNotBlank(sftpPassword)
                        && StringUtils.isNotBlank(sftpServer) && sftpPort > 1) {
                    jSession.connect();
                    channel = jSession.openChannel("sftp");
                    channel.connect();
                    sftpChannel = (ChannelSftp) channel;

                    byte[] buf = new byte[1024];
                    zipFileName = localDir + "/" + activityId + ".zip";
                    logger.info("Writing to zip file: {}", zipFileName);
                    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));

                    for (String filePath : filePaths) {

                        logger.info("Downloading file via Jsch SFTP: " + filePath);
                        logger.debug("Start Index--> {} | End Index {}", sftpConfig.getSblFtpRemoteDir()
                                .length(), filePath.indexOf('_'));

                        String newPath = filePath.replaceAll("\\\\", "/");
                        logger.info("The new path is {}", newPath);

                        String fileName = newPath.substring(sftpConfig.getSblFtpRemoteDir().length(),
                                newPath.length());
                        logger.info("File name is {}", fileName);

                        InputStream inputStream = sftpChannel.get(fileName);
                        FileInputStream fin = new FileInputStream(fileName);
                        zos.putNextEntry(new ZipEntry(fileName));
                        int len;

                        while ((len = fin.read(buf)) > 0) {
                            zos.write(buf, 0, len);
                        }
                        while ((len = inputStream.read(buf)) > 0) {
                            zos.write(buf, 0, len);
                        }
                        zos.closeEntry();
                         fin.close(); 
                        inputStream.close();
                        sftpChannel.rm(fileName);
                    }
                    zos.close();
                    downFlag = true;

                } else {
                    logger.info("FTP Servername Or Username or password value is blank");
                }
            } catch (JSchException jsEx) {
                logger.error(StrataWSConstants.SFTP_REFUSED_CONNECTION_MSG + ": " + jsEx.toString());
                throw new JSchException(jsEx.toString());
            } catch (SftpException sftpEx) {
                logger.error(StrataWSConstants.FILE_DOWNLOAD_FAILED_MSG + ": " + sftpEx.toString());
                throw new SftpException(0, sftpEx.toString());
            } catch (Exception e) {
                logger.error(StrataWSConstants.GENERIC_ERROR_MSG + " in downloadFileViaJSchSFTP: {}", e);
                throw new Exception(e);
            } finally {
                if (sftpChannel != null) {
                    sftpChannel.exit();
                }
                if (channel != null) {
                    channel.disconnect();
                }
                if (jSession != null && jSession.isConnected()) {
                    jSession.disconnect();
                }
                logger.debug("Download Flag value: " + downFlag);
            }
            return (downFlag) ? zipFileName : "";
        }*/

    @SuppressWarnings("unused")
    private static String generateOutputFilePath(String sftpDir, String filename) {

        String outputFileName = "";
        String newPath = filename.replaceAll("\\\\", "/");
        logger.info("The new path is {}", newPath);

        outputFileName = sftpDir + "/" + newPath;
        return outputFileName;
    }

    private static void disconnectSession(Session jschSession) {

        if (jschSession != null && jschSession.isConnected()) {
            logger.debug("Disconnecting jsch session");
            jschSession.disconnect();
        }
    }

    private static void disconnectChannel(Channel channel) {

        if (channel != null && channel.isConnected()) {
            logger.debug("Disconnecting channel");
            channel.disconnect();
        }
    }

    private static void exitSftpChannel(ChannelSftp sftpChannel) {

        if (sftpChannel != null && sftpChannel.isConnected()) {
            logger.debug("Exiting SFTP channel");
            sftpChannel.exit();
        }
    }
}
