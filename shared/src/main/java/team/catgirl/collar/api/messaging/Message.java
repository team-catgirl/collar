package team.catgirl.collar.api.messaging;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A Message sent via the Messaging API
 * All subtypes must use Jackson annotations for serialization
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public interface Message {}
