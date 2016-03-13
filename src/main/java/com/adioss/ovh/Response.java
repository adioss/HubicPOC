package com.adioss.ovh;

import java.util.List;
import java.util.Map;

public class Response {
    private final int code;
    private final Map<String, List<String>> headers;
    private final Object content;

    public Response(int code, Map<String, List<String>> headers, Object content) {
        this.code = code;
        this.headers = headers;
        this.content = content;
    }

    public int getCode() {
        return code;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Object getContent() {
        return content;
    }
}
