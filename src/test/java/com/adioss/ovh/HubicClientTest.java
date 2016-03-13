package com.adioss.ovh;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.adioss.ovh.AuthenticationInformation.createAuthenticationInformationWithCode;
import static com.adioss.ovh.HubicClient.createHubicClient;
import static org.junit.Assert.*;

/**
 * Unit test for simple {@link HubicClient}.
 */

public class HubicClientTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private static Path testFilePath = Paths.get(HubicClientTest.class.getResource("test.txt").getPath());

    // see https://api.hubic.com/sandbox to get some
    private static final String clientId = "";
    private static final String clientSecret = "";
    private static final String code = "";
    private static HubicClient hubicClient;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        hubicClient = createHubicClient(createAuthenticationInformationWithCode(clientId, clientSecret, code));
    }


    @Test
    public void shouldRefreshToken() {
        Object o = hubicClient.refreshToken();
        assertNotNull(o);
    }


    @Test
    public void shouldSetMetadata() {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Container-Meta-Author", "MarkTwain");
        headers.put("X-Container-Meta-Web-Directory-Type", "text/directory");
        headers.put("X-Container-Meta-Century", "Nineteenth");
        // When
        Response result = hubicClient.setMetadata("/Documents/zap.sh", headers);
        // Then
        assertNotNull(result);
        assertEquals(202, result.getCode());
    }

    @Test
    public void shouldListDirectory() {
        // Given
        String path = "Documents";
        // When
        Response result = hubicClient.listDirectory(path);
        // Then
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getContent().toString().length() > 10);
    }

    @Test
    public void shouldCreateDirectory() {
        // Given
        String path = "/Documents/test" + UUID.randomUUID().toString();
        // When
        Response result = hubicClient.createDirectory(path);
        // Then
        assertNotNull(result);
        assertNotNull(hubicClient.getInfo(path));
        assertEquals(201, result.getCode());
    }

    @Test
    public void shouldGetInfo() {
        // Given
        String path = "/Documents";
        // When
        Response result = hubicClient.getInfo(path);
        // Then
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue("0".equals(result.getHeaders().get("Content-Length").get(0)));
    }

    @Test
    public void shouldUploadElement() {
        // Given
        String targetPath = "/Documents/test" + UUID.randomUUID().toString() + ".txt";
        // When
        Response result = hubicClient.upload(testFilePath, targetPath);
        // Then
        assertNotNull(result);
        assertEquals(201, result.getCode());
    }

    @Test
    public void shouldDownloadElement() {
        // Given
        String fileName = "test" + UUID.randomUUID().toString() + ".txt";
        String source = "/Documents/" + fileName;
        hubicClient.upload(testFilePath, source);
        Path target = Paths.get(testFolder.getRoot().getPath(), fileName);
        // When
        Response result = hubicClient.download(source, target);
        // Then
        assertNotNull(result);
        assertEquals(200, result.getCode());
        testFolder.delete();
    }


    @Test
    public void shouldDeleteElement() {
        // Given
        String targetPath = "/Documents/test" + UUID.randomUUID().toString() + ".txt";
        hubicClient.upload(testFilePath, targetPath);
        // When
        Response result = hubicClient.delete(targetPath);
        // Then
        assertNotNull(result);
        assertEquals(204, result.getCode());
    }

    @Test
    public void shouldCopyElement() {
        // Given
        String fileName = "test" + UUID.randomUUID().toString() + ".txt";
        String sourcePath = "/Documents/" + fileName;
        String targetPath = "/Documents/titi/" + fileName;
        Response upload = hubicClient.upload(testFilePath, sourcePath);
        // When
        Response result = hubicClient.copy("default" + sourcePath, targetPath);
        // Then
        assertNotNull(result);
        assertEquals(201, result.getCode());
    }
}
