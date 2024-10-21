package com.task05;

import java.util.Map;

public class EventResponse {
    private final String id;
    private final int principalId;
    private final String createdAt;
    private final Map<String, String> body;

    public EventResponse(String id, int principalId, String createdAt, Map<String, String> body) {
        this.id = id;
        this.principalId = principalId;
        this.createdAt = createdAt;
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public int getPrincipalId() {
        return principalId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getBody() {
        return body;
    }
}
