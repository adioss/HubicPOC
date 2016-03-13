package com.adioss.ovh;

import org.junit.Ignore;
import org.junit.Test;

import static com.adioss.ovh.AuthenticationInformation.createAuthenticationInformationWithoutCode;
import static com.adioss.ovh.HubicClient.createHubicClient;

/**
 * Unit test for simple {@link HubicClient}.
 */

public class HubicClientWithoutCodeTest {
    private static final String clientId = "";
    private static final String clientSecret = "";

    @Ignore
    @Test
    public void shouldCreateHubicClientWithoutCode() {
        String redirectUrl = "https://api.hubic.com/sandbox/";
        String scope = "usage.r,account.r,getAllLinks.r,credentials.r,sponsorCode.r,activate.w,sponsored.r,links.drw";
        HubicClient hubicClient = createHubicClient(createAuthenticationInformationWithoutCode(clientId, clientSecret, redirectUrl, scope));
    }
}
