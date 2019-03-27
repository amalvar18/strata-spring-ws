package com.newco.strataws.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.newco.strataws.common.PropertyConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.siebel.SiebelConfig;

@Configuration
@ComponentScan(basePackages = { "com.newco.strataws" })
@PropertySources({ @PropertySource(value = "classpath:${envTarget:qa}.properties")
/*@PropertySource(value = "classpath:qc.properties")*/})
public class AppConfig {

    @Autowired
    private Environment env;

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /*{
        logger.info("Loading config for environment: {} based on JVM param",
                env.getProperty(PropertyConstants.APP_SERVER_ENV_PROP));
    }*/

    @Bean
    public SiebelConfig siebelConfig() {
        SiebelConfig config = new SiebelConfig();
        logger.info("Loading config for environment: [{}] based on JVM param",
                env.getProperty(PropertyConstants.APP_SERVER_ENV_PROP));
        config.setSblServerURL(env.getProperty(PropertyConstants.SBL_DEFAULT_SERVER_URL_PROP));
        /*config.setLogin(env.getProperty(StrataWSConstants.SBL_LOGIN_PROP));*/

        /*        config.setSealedFile(env.getProperty(PropertyConstants.SEALED_KEYFILE_PROP));
                config.setSecretFile(env.getProperty(PropertyConstants.SECRET_KEYFILE_PROP));
                config.setClientOrg(env.getProperty(PropertyConstants.CLIENT_SBL_ORG_PROP));
                config.setClientAccount(env.getProperty(PropertyConstants.CLIENT_SBL_ACCOUNT_PROP));
                config.setClientServerSite(env.getProperty(PropertyConstants.CLIENT_SERVER_SITE_PROP));*/

        config.setMaxSrShortDescLength(Integer.parseInt(env
                .getProperty(PropertyConstants.SR_SHORT_DESCR_MAX_LENGTH_PROP)));
        config.setMaxSrLongDescLength(Integer.parseInt(env
                .getProperty(PropertyConstants.SR_LONG_DESCR_MAX_LENGTH_PROP)));
        config.setMaxActDescLength(Integer.parseInt(env
                .getProperty(PropertyConstants.ACT_DESCR_MAX_LENGTH_PROP)));
        config.setMaxActCommentLength(Integer.parseInt(env
                .getProperty(PropertyConstants.ACT_COMMENTS_MAX_LENGTH_PROP)));
        /*config.setDefaultAttachUploadFile(env.getProperty(StrataWSConstants.DEFAULT_UPLD_ATTACH_FILE_PROP));*/
        return config;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        /*multipartResolver.setMaxUploadSize(1000000);*/
        multipartResolver.setMaxUploadSize(Integer.parseInt(env
                .getProperty(PropertyConstants.MAX_FILE_UPLOAD_SIZE_PROP)));
        return multipartResolver;
    }

    @Bean
    public List<String> allowedFileTypes() {
        String filetypes = env.getProperty(PropertyConstants.ALLOWED_FILETYPES_LIST_PROP);
        List<String> allowedFileTypesList = Arrays.asList(filetypes.split("\\s*,\\s*"));
        return allowedFileTypesList;
    }

    @Bean
    public Map<String, String> sblSrvrMap() {
        Map<String, String> sblSrvrMap = new HashMap();
        sblSrvrMap.put("MCS2" + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX,
                env.getProperty(PropertyConstants.SBL_MCS2_SERVER_URL_PROP));
        sblSrvrMap.put("MCS2" + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX,
                env.getProperty(PropertyConstants.SBL_MCS2_SERVER_NAME_PROP));
        sblSrvrMap.put("MCS3" + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX,
                env.getProperty(PropertyConstants.SBL_MCS3_SERVER_URL_PROP));
        sblSrvrMap.put("MCS3" + StrataWSConstants.SRVR_MAP_SRVR_NAME_KEY_SUFFIX,
                env.getProperty(PropertyConstants.SBL_MCS3_SERVER_NAME_PROP));
        return sblSrvrMap;
    }

    /*
     * @Bean public SFTPConfig siebelFtpConfig() { SFTPConfig config =
     * new SFTPConfig();
     * 
     * config.setSblFtpServerUser(env.getProperty(StrataWSConstants.
     * SBL_FTP_SERVER_PROP));
     * config.setSblFtpPort(Integer.parseInt(env.getProperty
     * (StrataWSConstants.SBL_FTP_PORT_PROP)));
     * config.setFtpSealedFile(env.getProperty
     * (StrataWSConstants.FTP_SEALED_KEYFILE_PROP));
     * config.setFtpSecretFile(env.getProperty
     * (StrataWSConstants.FTP_SECRET_KEYFILE_PROP));
     * 
     * return config; }
     */
}
