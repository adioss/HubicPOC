package com.adioss.ovh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuthenticationInformation {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationInformation.class);
    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private String scope;
    private String code;

    public static AuthenticationInformation createAuthenticationInformationWithCode(String clientId, String clientSecret, String code) {
        return new AuthenticationInformation(clientId, clientSecret, null, null, code);
    }

    public static AuthenticationInformation createAuthenticationInformationWithoutCode(String clientId, String clientSecret, String redirectUrl, String scope) {
        return new AuthenticationInformation(clientId, clientSecret, encode(redirectUrl), scope, null);
    }

    private static String encode(String redirectUrl) {
        try {
            return URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            LOG.error("Impossible to encode redirect URL.");
            System.exit(1);
        }
        return null;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getScope() {
        return scope;
    }

    public String getCode() {
        return code;
    }

    private AuthenticationInformation(String clientId, String clientSecret, String redirectUrl, String scope, String code) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
        this.scope = scope;
        this.code = code;
    }
}
