package com.adioss.ovh;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.adioss.ovh.AuthenticationInformation.createAuthenticationInformationWithCode;

public class HubicClient {
    private static final Logger LOG = LoggerFactory.getLogger(HubicClient.class);
    private static final String URL_OAUTH_AUTH = "https://api.hubic.com/oauth/auth/";
    private static final String URL_OAUTH_TOKEN = "https://api.hubic.com/oauth/token/";
    private static final String URL_CREDENTIALS = "https://api.hubic.com/1.0/account/credentials";

    private final HttpAPI httpAPI;
    private final AuthenticationInformation authenticationInformation;
    private String token;
    private String endpoint;
    private AccessToken accessToken;

    public static HubicClient createHubicClient(AuthenticationInformation authenticationInformation) {
        String code = authenticationInformation.getCode();
        if (code == null || code.isEmpty()) {
            LOG.info("1. Go to: " + createAuthorizeUrl(authenticationInformation));
            LOG.info("2. Check authorization and login");
            LOG.info("3. Copy the authorization code.");
            try {
                code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
            } catch (IOException e) {
                exitWithError("Error on init: " + e.getMessage());
            }
        }
        return new HubicClient(createAuthenticationInformationWithCode(authenticationInformation.getClientId(), authenticationInformation.getClientSecret(), code));
    }


    /**
     * curl -H "Authorization: AUTH_TOKEN" -d "refresh_token=REFRESH_TOKEN&grant_type=refresh_token" https://api.hubic.com/oauth/token/ -i -X POST
     */
    public long refreshToken() {
        byte[] base64 = (authenticationInformation.getClientId() + ":" + authenticationInformation.getClientSecret()).getBytes();
        String authorization = "Basic " + new String(Base64.getEncoder().encode(base64));
        Response post = httpAPI.query(URL_OAUTH_TOKEN).json()
                .header("Authorization", authorization)
                .postData("refresh_token", accessToken.getRefreshToken())
                .postData("grant_type", "refresh_token")
                .json()
                .post();
        if (post != null && post.getCode() == 200 && post.getContent() != null) {
            JsonObject postContent = (JsonObject) post.getContent();
            accessToken = new AccessToken(postContent.get("access_token").getAsString(),
                    new Date().getTime() + postContent.get("expires_in").getAsInt(),
                    accessToken.getRefreshToken());
            return accessToken.getExpiresIn();
        }
        return -1;
    }


    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" ENDPOINT_URL/default/Documents -i -X HEAD
     *
     * @param path of the element
     * @return a {@link Response}
     */
    public Response getInfo(String path) {
        return httpAPI.query(endpoint + "/default" + path + "?format=json").json().header("X-Auth-Token", token).head();
    }


    public Response setMetadata(String path, Map<String, String> headers) {
        HttpAPI query = httpAPI.query(endpoint + "/default" + path);
        headers.put("X-Auth-Token", token);
        for (String headerName : headers.keySet()) {
            query.header(headerName, headers.get(headerName));
        }
        return query.post();
    }

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" ENDPOINT_URL/default?format=json -i -X GET
     *
     * @param path of the directory
     * @return a {@link Response}
     */
    public Response listDirectory(String path) {
        return httpAPI.query(endpoint + "/default?path=" + path + "&format=json").jsonArray().header("X-Auth-Token", token).get();
    }

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" -H "Content-Length: 0" -H "Content-Type: application/directory" ENDPOINT_URL/default{path} -i -X PUT
     *
     * @param path of the new directory
     * @return a {@link Response}
     */
    public Response createDirectory(String path) {
        return httpAPI.query(endpoint + "/default" + path)
                .header("X-Auth-Token", token)
                .header("Content-Length", "0")
                .header("Content-Type", "application/directory")
                .put();
    }

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" -T "/home/adio/Bureau/README.md" ENDPOINT_URL/default/titi/README.md -i -X PUT
     *
     * @param source     {@link Path} of the source file to upload
     * @param targetPath target path
     * @return a {@link Response}
     */
    public Response upload(Path source, String targetPath) {
        return httpAPI.query(endpoint + "/default" + targetPath)
                .header("X-Auth-Token", token)
                .put(source);
    }

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" ENDPOINT_URL/default/titi/README.md -i -X DELETE
     *
     * @param path of the element to delete
     * @return a {@link Response}
     */
    public Response delete(String path) {
        return httpAPI.query(endpoint + "/default" + path).header("X-Auth-Token", token).delete();
    }

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" ENDPOINT_URL/default/Documents/zap.sh -i -X GET -o zap.sh
     *
     * @param source of the element to delete
     * @param target output path
     * @return a {@link Response}
     */
    public Response download(String source, Path target) {
        return httpAPI.query(endpoint + "/default" + source).binary(target).header("X-Auth-Token", token).get();
    }

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" -H "Content-Length: 0" -H "X-Copy-From: default/Documents/zap.sh" ENDPOINT_URL/default/Documents/titi/zap.sh -i -X PUT
     *
     * @param sourcePath of the element to copy
     * @param targetPath of the copied element
     * @return a {@link Response}
     */
    public Response copy(String sourcePath, String targetPath) {
        return httpAPI.query(endpoint + "/default" + targetPath)
                .header("X-Auth-Token", token)
                .header("X-Copy-From", sourcePath)
                .put();
    }


    private HubicClient(AuthenticationInformation authenticationInformation) {
        this.authenticationInformation = authenticationInformation;
        httpAPI = new HttpAPI();
        try {
            retrieveAccessToken();
            retrieveAuthorization();
        } catch (Exception e) {
            exitWithError("Error on init: " + e.getMessage());
        }
    }

    private void retrieveAccessToken() throws Exception {
        byte[] base64 = (authenticationInformation.getClientId() + ":" + authenticationInformation.getClientSecret()).getBytes();
        String authorization = "Basic " + new String(Base64.getEncoder().encode(base64));
        Response post = httpAPI.query(URL_OAUTH_TOKEN)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", authorization)
                .postData("code", authenticationInformation.getCode())
                .postData("redirect_uri", "https://api.hubic.com/sandbox/")
                .postData("grant_type", "authorization_code")
                .json()
                .post();
        if (post != null && post.getCode() == 200) {
            JsonObject postContent = (JsonObject) post.getContent();
            accessToken = new AccessToken(postContent.get("access_token").getAsString(),
                    new Date().getTime() + postContent.get("expires_in").getAsInt(),
                    postContent.get("refresh_token").getAsString());
        } else {
            throw new RuntimeException("Impossible to retrieve accessToken");
        }

    }

    private void retrieveAuthorization() {
        Response authorization = httpAPI.query(URL_CREDENTIALS).json().header("Authorization", accessToken.getBearer()).get();
        if (authorization != null) {
            JsonObject result = (JsonObject) authorization.getContent();
            token = result.get("token").getAsString();
            endpoint = result.get("endpoint").getAsString();
            if (LOG.isDebugEnabled()) {
                LOG.debug("token:" + token + " and endpoint:" + endpoint);
            }
        } else {
            throw new RuntimeException("Impossible to retrieve token and/or endpoint");
        }
    }

    private static void exitWithError(String message) {
        LOG.error(message);
        System.exit(1);
    }

    private static String createAuthorizeUrl(AuthenticationInformation authenticationInformation) {
        return URL_OAUTH_AUTH + "?" +
                "client_id=" + authenticationInformation.getClientId() +
                "&redirect_uri=" + authenticationInformation.getRedirectUrl() +
                "&scope=" + authenticationInformation.getScope() +
                "&response_type=code" +
                "&state=RandomString_" + UUID.randomUUID();
    }
}
