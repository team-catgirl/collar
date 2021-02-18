package team.catgirl.collar.api.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A simple text message
 */
public final class TextMessage implements Message {

    public static final int MAX_LENGTH = 2000;

    @JsonProperty("content")
    public final String content;

    public TextMessage(@JsonProperty("content") String content) {
        if (content.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("content exceeds " + MAX_LENGTH + " characters");
        }
        this.content = content;
    }
}
