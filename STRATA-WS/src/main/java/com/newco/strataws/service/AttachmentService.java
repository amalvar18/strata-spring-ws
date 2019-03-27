package com.newco.strataws.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.newco.strataws.config.file.AttachConfig;
import com.newco.strataws.config.sftp.SFTPConfig;
import com.newco.strataws.config.siebel.SiebelConfig;

public interface AttachmentService {

    /**
     * Gets the activity attachment paths
     * 
     * @param activityId
     * @param siebelConfig
     * @return
     * @throws Exception
     */
    public Map<String, String> fetchActivityAttachmentPaths(String activityId, SiebelConfig siebelConfig)
            throws Exception;

    /**
     * Gets the attachment paths
     * 
     * @param parentId
     *            activity or SR Id
     * @param siebelConfig
     * @param attachConfig
     * @return
     * @throws Exception
     */
    public Map<String, String> fetchAttachmentPaths(String parentId, SiebelConfig siebelConfig,
            AttachConfig attachConfig) throws Exception;

    /**
     * Check if activity has attachments
     * 
     * @param activityId
     * @return
     */
    /* public Boolean checkForAttachment(String activityId);*/

    public String fetchAttachmentsAsZip(String activityId, List<String> fileList,
            SFTPConfig siebelFTPConfig) throws Exception;

    /**
     * Uploads a file attachment into Siebel
     * 
     * @deprecated
     * @param parentId
     * @param recInputStream
     * @param siebelConfig
     * @param sftpConfig
     * @param attachConfig
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public String uploadAttachment(String parentId, InputStream recInputStream,
            SiebelConfig siebelConfig, SFTPConfig sftpConfig, AttachConfig attachConfig) throws Exception;

    /**
     * Uploads a file attachment into Siebel
     * 
     * @param parentId
     * @param recInputStream
     * @param siebelConfig
     * @param sftpConfig
     * @param attachConfig
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public String uploadNewAttachment(String parentId, InputStream recInputStream,
            SiebelConfig siebelConfig, SFTPConfig sftpConfig, AttachConfig attachConfig) throws Exception;
}
