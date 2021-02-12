package team.catgirl.collar.server.mail;

import team.catgirl.collar.server.services.profiles.Profile;

import java.util.Map;

public interface Email {
    void send(Profile profile, String subject, String templateName, Map<String, Object> variables);
}
