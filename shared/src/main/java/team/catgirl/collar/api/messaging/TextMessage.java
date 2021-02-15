package team.catgirl.collar.api.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A simple text message
 */
public final class TextMessage implements Message {
    @JsonProperty("content")
    public final String content;

    public TextMessage(String content) {
        this.content = content;
    }
}
