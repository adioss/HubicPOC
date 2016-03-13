package com.adioss.ovh;

import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class HttpAPI {
    private URL url;
    private Map<String, String> headers;
    private Map<String, String> urlParameters;
    private Map<String, String> postData;
    private Output output;
    private Path outputPath;


    public HttpAPI query(String url) {
        try {
            this.url = new URL(url);
            this.headers = new HashMap<>();
            this.urlParameters = new HashMap<>();
            this.postData = new HashMap<>();
            this.output = Output.PLAIN;
            this.outputPath = null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return this;
    }

    public HttpAPI header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }


    public HttpAPI urlParameter(String key, String value) {
        this.urlParameters.put(key, value);
        return this;
    }


    public HttpAPI postData(String key, String value) {
        this.postData.put(key, value);
        return this;
    }

    public HttpAPI plain() {
        this.output = Output.PLAIN;
        return this;
    }

    public HttpAPI binary(Path target) {
        outputPath = target;
        this.output = Output.BINARY;
        return this;
    }

    public HttpAPI json() {
        this.output = Output.JSON;
        return this;
    }

    public HttpAPI jsonArray() {
        this.output = Output.JSON_ARRAY;
        return this;
    }


    // HTTP GET request
    public Response get() {
        return basicHttpQuery("GET");
    }

    // HTTP HEAD request
    public Response head() {
        return basicHttpQuery("HEAD");
    }

    // HTTP DELETE request
    public Response delete() {
        return basicHttpQuery("DELETE");
    }

    // HTTP POST request
    public Response post() {
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) this.url.openConnection();
            httpsURLConnection.setRequestMethod("POST");
            appendUrlParameter(httpsURLConnection);
            appendHeaders(httpsURLConnection);
            byte[] postData = buildPostParameters();
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.getOutputStream().write(postData);
            return new Response(httpsURLConnection.getResponseCode(), httpsURLConnection.getHeaderFields(), getResponseContent(httpsURLConnection));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // HTTP PUT request
    public Response put() {
        return put(null);
    }

    public Response put(Path sourcePath) {
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) this.url.openConnection();
            httpsURLConnection.setRequestMethod("PUT");
            appendUrlParameter(httpsURLConnection);
            appendHeaders(httpsURLConnection);
            httpsURLConnection.setDoOutput(true);
            if (sourcePath != null) {
                try (BufferedOutputStream outputStream = new BufferedOutputStream(httpsURLConnection.getOutputStream());
                     BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourcePath.toFile()))) {
                    int i;
                    while ((i = inputStream.read()) >= 0) {
                        outputStream.write(i);
                    }
                }
            } else {
                httpsURLConnection.setFixedLengthStreamingMode(0);
            }

            return new Response(httpsURLConnection.getResponseCode(), httpsURLConnection.getHeaderFields(), getResponseContent(httpsURLConnection));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Response basicHttpQuery(String verb) {
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) this.url.openConnection();
            httpsURLConnection.setRequestMethod(verb);
            appendUrlParameter(httpsURLConnection);
            appendHeaders(httpsURLConnection);
            return new Response(httpsURLConnection.getResponseCode(), httpsURLConnection.getHeaderFields(), getResponseContent(httpsURLConnection));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object getResponseContent(HttpsURLConnection httpsURLConnection) throws IOException {
        if (this.output == Output.BINARY) {
            try (InputStream inputStream = httpsURLConnection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
                int bytesRead = -1;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                return null;
            }
            return outputPath;
        } else {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    response.append(inputLine);
                }
                bufferedReader.close();
                return convertResult(response);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private void appendHeaders(HttpsURLConnection httpsURLConnection) {
        for (String key : this.headers.keySet()) {
            httpsURLConnection.setRequestProperty(key, this.headers.get(key));
        }
    }

    private void appendUrlParameter(HttpsURLConnection httpsURLConnection) {
        for (String key : this.urlParameters.keySet()) {
            httpsURLConnection.setRequestProperty(key, this.urlParameters.get(key));
        }
    }

    private byte[] buildPostParameters() throws UnsupportedEncodingException {
        StringBuilder postParameters = new StringBuilder();
        for (String key : this.postData.keySet()) {
            if (postParameters.length() != 0) {
                postParameters.append("&");
            }

            postParameters.append(key).append("=").append(URLEncoder.encode(String.valueOf(this.postData.get(key)), "UTF-8"));
        }
        return postParameters.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Object convertResult(StringBuilder response) {
        switch (this.output) {
            case JSON: {
                JsonParser parser = new JsonParser();
                return parser.parse(response.toString());
            }
            case JSON_ARRAY: {
                JsonParser parser = new JsonParser();
                return parser.parse(response.toString()).getAsJsonArray();
            }
            default: {
                return response.toString();
            }

        }
    }

    private enum Output {
        PLAIN, JSON, JSON_ARRAY, BINARY
    }
}
