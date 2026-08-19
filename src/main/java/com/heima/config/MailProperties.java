package com.heima.config;

import cn.hutool.extra.mail.MailAccount;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ruankao.mail")
public class MailProperties {

    private String host = "smtp.163.com";
    private Integer port = 465;
    private Boolean auth = true;
    private String user = "";
    private String pass = "";
    private String from = "";
    private String fromName = "软考通";
    private Boolean sslEnable = true;

    public MailAccount toAccount() {
        MailAccount account = new MailAccount();
        account.setHost(host);
        account.setPort(port);
        account.setAuth(Boolean.TRUE.equals(auth));
        account.setUser(user);
        account.setPass(pass);
        account.setFrom(formatFrom());
        account.setSslEnable(Boolean.TRUE.equals(sslEnable));
        return account;
    }

    private String formatFrom() {
        if (fromName != null && !fromName.isBlank() && from != null && !from.isBlank()) {
            return fromName + "<" + from + ">";
        }
        return from;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getAuth() {
        return auth;
    }

    public void setAuth(Boolean auth) {
        this.auth = auth;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public Boolean getSslEnable() {
        return sslEnable;
    }

    public void setSslEnable(Boolean sslEnable) {
        this.sslEnable = sslEnable;
    }
}
