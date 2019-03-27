package com.newco.strataws.config.sftp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:${envTarget:qa}.properties")
/*@PropertySource(value = "classpath:qc.properties")*/
public class SFTPConfig {

    @Value("${siebel.ftp.server}")
    private String sblFtpServer;

    /*    @Value("${siebel.ftp.server.user}")*/
    private String sblFtpServerUser;

    @Value("${siebel.ftp.port:22}")
    private Integer sblFtpPort;

    @Value("${siebel.ftp.secretfilename}")
    private String ftpSecretFile;

    @Value("${siebel.ftp.sealedfilename}")
    private String ftpSealedFile;

    private String ftpPassword;

    @Value("${siebel.ftp.remote.dir}")
    private String sblFtpRemoteDir;

    @Value("${siebel.ftp.local.dir:${java.io.tmpdir}/localDownload}")
    private String sftpLocalDir;

    @Value("${sftp.remote.directory.download.filter:*.*}")
    private String sftpRemoteDirectoryDownloadFilter;

    /*private static final Logger logger = LoggerFactory.getLogger(SFTPConfig.class);*/

    public String getSblFtpServer() {
        return sblFtpServer;
    }

    public void setSblFtpServer(String sblFtpServer) {
        this.sblFtpServer = sblFtpServer;
    }

    public String getSblFtpServerUser() {
        return sblFtpServerUser;
    }

    public void setSblFtpServerUser(String sblFtpServerUser) {
        this.sblFtpServerUser = sblFtpServerUser;
    }

    public Integer getSblFtpPort() {
        return sblFtpPort;
    }

    public void setSblFtpPort(Integer sblFtpPort) {
        this.sblFtpPort = sblFtpPort;
    }

    public String getFtpSecretFile() {
        return ftpSecretFile;
    }

    public void setFtpSecretFile(String ftpSecretFile) {
        this.ftpSecretFile = ftpSecretFile;
    }

    public String getFtpSealedFile() {
        return ftpSealedFile;
    }

    public void setFtpSealedFile(String ftpSealedFile) {
        this.ftpSealedFile = ftpSealedFile;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public String getSblFtpRemoteDir() {
        return sblFtpRemoteDir;
    }

    public void setSblFtpRemoteDir(String sblFtpRemoteDir) {
        this.sblFtpRemoteDir = sblFtpRemoteDir;
    }

    public String getSftpLocalDir() {
        return sftpLocalDir;
    }

    public void setSftpLocalDir(String sftpLocalDir) {
        this.sftpLocalDir = sftpLocalDir;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SFTPConfig [sblFtpServer=").append(sblFtpServer).append(", sblFtpServerUser=")
                .append(sblFtpServerUser).append(", sblFtpPort=").append(sblFtpPort)
                .append(", ftpSecretFile=").append(ftpSecretFile).append(", ftpSealedFile=")
                .append(ftpSealedFile).append(", sblFtpRemoteDir=").append(sblFtpRemoteDir)
                .append(", sftpLocalDir=").append(sftpLocalDir)
                .append(", sftpRemoteDirectoryDownloadFilter=").append(sftpRemoteDirectoryDownloadFilter)
                .append("]");
        return builder.toString();
    }

    /*    @Bean
    public QueueChannel fromSftpChannel() {
        return new QueueChannel();
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata defaultPoller() {
        PollerMetadata pollerMetadata = new PollerMetadata();
        pollerMetadata.setTrigger(new PeriodicTrigger(600000));
        return pollerMetadata;
    }
    */
    /* @Bean
     public SessionFactory<LsEntry> sftpSessionFactory() throws Exception {
         DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
         factory.setHost(sblFtpServer);
         factory.setPort(sblFtpPort);
         factory.setUser(sblFtpServerUser);
         setFtpPassword(SiebelUtility.decryptPassword(ftpSecretFile, ftpSealedFile));
         factory.setPassword(ftpPassword);
         factory.setAllowUnknownKeys(true);
         logger.debug("Using SftpConfig: {}", this.toString());
         return new CachingSessionFactory<LsEntry>(factory);
     }*/

    /*
        @Bean
        public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() throws Exception {
            SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
            fileSynchronizer.setDeleteRemoteFiles(false);
            fileSynchronizer.setRemoteDirectory(sblFtpRemoteDir);
            fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter("*.doc"));

            return fileSynchronizer;
        }

        @Bean
         @InboundChannelAdapter(channel = "sftpChannel", poller =
         * @Poller(fixedDelay = "5000")) 
        @InboundChannelAdapter(channel = "fromSftpChannel", autoStartup = "true", poller = @Poller(fixedDelay = "5000"))
        public MessageSource<File> sftpMessageSource() throws Exception {
            SftpInboundFileSynchronizingMessageSource source = new SftpInboundFileSynchronizingMessageSource(
                    sftpInboundFileSynchronizer());
            source.setLocalDirectory(new File(sftpLocalDir));
            source.setAutoCreateLocalDirectory(true);
            source.setLocalFilter(new AcceptOnceFileListFilter<File>());
            return source;
        }

        @Bean
        @ServiceActivator(inputChannel = "fromSftpChannel")
        @Autowired
        public MessageHandler resultFileHandler() {
            return new MessageHandler() {
                @Override
                public void handleMessage(Message<?> message) throws MessagingException {
                     logger.error(
                     * "Error in resultfilehandler() while returning MessageHandler: {}"
                     * , message.getPayload()); 
                    File f = (File) message.getPayload();
                    logger.info("File Name {} downloaded", f.getName());
                }
            };
        }*/
}
