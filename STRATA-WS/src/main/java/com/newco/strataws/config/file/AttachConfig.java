package com.newco.strataws.config.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Configuration for Attachments
 * 
 * @author Amal Varghese
 *
 */
@Component
@PropertySource(value = "classpath:${envTarget:qa}.properties")
public class AttachConfig {

    private String fileName;

    private String fileExt;

    private Integer fileSize;

    /*Whether SR or Activity Attachment*/
    private String sblAttachType;

    /*SR or activity row Id*/
    private String sblParentRowId;

    @Value("${act.file.src.type}")
    private String fileSrcType;

    @Value("${act.file.defer.flg}")
    private String fileDeferFlg;

    @Value("${act.file.dock.req.flg}")
    private String fileDockReqFlg;

    @Value("${act.file.dock.stat.flg}")
    private String fileDockStatFlg;

    @Value("${act.file.auto.upd.flg}")
    private String fileAutoUpdFlg;

    @Value("${act.file.keep.link}")
    private String sblKeepLink;

    @Value("${default.attach.upload.file}")
    private String defaultAttachUplFile;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getSblAttachType() {
        return sblAttachType;
    }

    public void setSblAttachType(String sblAttachType) {
        this.sblAttachType = sblAttachType;
    }

    public String getSblParentRowId() {
        return sblParentRowId;
    }

    public void setSblParentRowId(String sblParentRowId) {
        this.sblParentRowId = sblParentRowId;
    }

    public String getFileSrcType() {
        return fileSrcType;
    }

    public void setFileSrcType(String fileSrcType) {
        this.fileSrcType = fileSrcType;
    }

    public String getFileDeferFlg() {
        return fileDeferFlg;
    }

    public void setFileDeferFlg(String fileDeferFlg) {
        this.fileDeferFlg = fileDeferFlg;
    }

    public String getFileDockReqFlg() {
        return fileDockReqFlg;
    }

    public void setFileDockReqFlg(String fileDockReqFlg) {
        this.fileDockReqFlg = fileDockReqFlg;
    }

    public String getFileDockStatFlg() {
        return fileDockStatFlg;
    }

    public void setFileDockStatFlg(String fileDockStatFlg) {
        this.fileDockStatFlg = fileDockStatFlg;
    }

    public String getFileAutoUpdFlg() {
        return fileAutoUpdFlg;
    }

    public void setFileAutoUpdFlg(String fileAutoUpdFlg) {
        this.fileAutoUpdFlg = fileAutoUpdFlg;
    }

    public String getSblKeepLink() {
        return sblKeepLink;
    }

    public void setSblKeepLink(String sblKeepLink) {
        this.sblKeepLink = sblKeepLink;
    }

    public String getDefaultAttachUplFile() {
        return defaultAttachUplFile;
    }

    public void setDefaultAttachUplFile(String defaultAttachUplFile) {
        this.defaultAttachUplFile = defaultAttachUplFile;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AttachConfig [fileName=").append(fileName).append(", fileExt=").append(fileExt)
                .append(", fileSize=").append(fileSize).append(", sblAttachType=").append(sblAttachType)
                .append(", sblParentRowId=").append(sblParentRowId).append(", fileSrcType=")
                .append(fileSrcType).append(", fileDeferFlg=").append(fileDeferFlg)
                .append(", fileDockReqFlg=").append(fileDockReqFlg).append(", fileDockStatFlg=")
                .append(fileDockStatFlg).append(", fileAutoUpdFlg=").append(fileAutoUpdFlg)
                .append(", sblKeepLink=").append(sblKeepLink).append(", defaultAttachUplFile=")
                .append(defaultAttachUplFile).append("]");
        return builder.toString();
    }
}
