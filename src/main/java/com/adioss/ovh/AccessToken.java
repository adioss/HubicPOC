package com.adioss.ovh;

final class AccessToken {
    private static final String TOKEN_TYPE = "Bearer";

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;

    public AccessToken(String accessToken, long expiresIn, String refreshToken) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getBearer() {
        return TOKEN_TYPE + " " + this.getAccessToken();
    }
}
