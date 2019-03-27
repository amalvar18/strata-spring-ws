package com.newco.strataws.exception;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Not Found")
public class NotFoundException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String WHITESPACE = " ";
    private static final String prefixMsg = "Could not find the";

    String itemId;

    String itemType;

    String customMessage;

    Boolean siebelItemFlg = false;

    public NotFoundException(String itemType) {
        this.itemType = itemType;
        customMessage = StringUtils.joinWith(WHITESPACE, prefixMsg, itemType);
    }

    public NotFoundException(String itemType, Boolean siebelItemFlg) {
        this.itemType = itemType;
        this.siebelItemFlg = siebelItemFlg;
        customMessage = String.join(WHITESPACE, prefixMsg, itemType, siebelItemFlg ? "in Siebel" : "");
        /*customMessage = StringUtils.joinWith(WHITESPACE, prefixMsg, itemType, siebelItemFlg ? "in Siebel"
                : "");*/
    }

    public NotFoundException(String itemType, Boolean siebelItemFlg, String customMessage) {
        this.itemType = itemType;
        this.siebelItemFlg = siebelItemFlg;
        this.customMessage = customMessage;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public Boolean getSiebelItemFlg() {
        return siebelItemFlg;
    }

    public void setSiebelItemFlg(Boolean siebelItemFlg) {
        this.siebelItemFlg = siebelItemFlg;
    }

}
