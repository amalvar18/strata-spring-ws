package com.newco.strataws.config.siebel;

import org.springframework.stereotype.Component;

@Component
public class SiebelConfig {

    /*
     * @Autowired private Environment env;
     */

    private String sblEnv;

    private String sblServerURL;

    private String login;

    private String sealedFile;

    private String secretFile;

    private String sblPasswrd;

    private String clientOrg;

    private String clientAccount;

    private String clientServerSite;

    private Integer maxSrShortDescLength;

    private Integer maxSrLongDescLength;

    private Integer maxActDescLength;

    private Integer maxActCommentLength;

    /*
     * public SiebelConfig() {
     * setSblServerURL(env.getProperty("sbl.mcs2.server.url")); }
     */

    public String getSblEnv() {
        return sblEnv;
    }

    public void setSblEnv(String sblEnv) {
        this.sblEnv = sblEnv;
    }

    public String getSblServerURL() {
        return sblServerURL;
    }

    public void setSblServerURL(String sblServerURL) {
        this.sblServerURL = sblServerURL;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSealedFile() {
        return sealedFile;
    }

    public void setSealedFile(String sealedFile) {
        this.sealedFile = sealedFile;
    }

    public String getSecretFile() {
        return secretFile;
    }

    public void setSecretFile(String secretFile) {
        this.secretFile = secretFile;
    }

    public String getClientOrg() {
        return clientOrg;
    }

    public void setClientOrg(String clientOrg) {
        this.clientOrg = clientOrg;
    }

    public String getClientAccount() {
        return clientAccount;
    }

    public void setClientAccount(String clientAccount) {
        this.clientAccount = clientAccount;
    }

    public String getClientServerSite() {
        return clientServerSite;
    }

    public void setClientServerSite(String clientServerSite) {
        this.clientServerSite = clientServerSite;
    }

    public String getSblPasswrd() {
        return sblPasswrd;
    }

    public void setSblPasswrd(String sblPasswrd) {
        this.sblPasswrd = sblPasswrd;
    }

    public Integer getMaxSrShortDescLength() {
        return maxSrShortDescLength;
    }

    public void setMaxSrShortDescLength(Integer maxSrShortDescLength) {
        this.maxSrShortDescLength = maxSrShortDescLength;
    }

    public Integer getMaxSrLongDescLength() {
        return maxSrLongDescLength;
    }

    public void setMaxSrLongDescLength(Integer maxSrLongDescLength) {
        this.maxSrLongDescLength = maxSrLongDescLength;
    }

    public Integer getMaxActDescLength() {
        return maxActDescLength;
    }

    public void setMaxActDescLength(Integer maxActDescLength) {
        this.maxActDescLength = maxActDescLength;
    }

    public Integer getMaxActCommentLength() {
        return maxActCommentLength;
    }

    public void setMaxActCommentLength(Integer maxActCommentLength) {
        this.maxActCommentLength = maxActCommentLength;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SiebelConfig [sblEnv=").append(sblEnv).append(", sblServerURL=")
                .append(sblServerURL).append(", login=").append(login).append(", sealedFile=")
                .append(sealedFile).append(", secretFile=").append(secretFile).append(", sblPasswrd=")
                .append(sblPasswrd.replaceAll(".", "*")).append(", clientOrg=").append(clientOrg)
                .append(", clientAccount=").append(clientAccount).append(", clientServerSite=")
                .append(clientServerSite).append(", maxSrShortDescLength=").append(maxSrShortDescLength)
                .append(", maxSrLongDescLength=").append(maxSrLongDescLength)
                .append(", maxActDescLength=").append(maxActDescLength).append(", maxActCommentLength=")
                .append(maxActCommentLength).append("]");
        return builder.toString();
    }

}
