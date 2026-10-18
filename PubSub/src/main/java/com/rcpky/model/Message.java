package com.rcpky.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Message {

    private final String id;
    private final String topicName;
    private final String payload;
    private final Map<String, String> metadata;
    private final long timestamp;

    public Message(String topicName, String payload, Map<String, String> metadata) {
        this.id = UUID.randomUUID().toString();
        this.topicName = topicName;
        this.payload = payload;
        this.metadata = Collections.unmodifiableMap(
                metadata != null ? new HashMap<>(metadata) : new HashMap<>());
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String topicName, String payload) {
        this(topicName, payload, null);
    }

    public String getId()          { return id; }
    public String getTopicName()   { return topicName; }
    public String getPayload()     { return payload; }
    public Map<String, String> getMetadata() { return metadata; }
    public long getTimestamp()     { return timestamp; }

    @Override
    public String toString() {
        return "Message{id='" + id.substring(0, 8) + "', payload='" + payload
                + "', metadata=" + metadata + "}";
    }
}
