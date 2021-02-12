package team.catgirl.collar.server.mail;

import com.google.common.io.Files;
import team.catgirl.collar.server.services.profiles.Profile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LocalEmail extends AbstractEmail {
    @Override
    public void send(Profile profile, String subject, String templateName, Map<String, Object> variables) {
        String path = "target/emails/" + profile.name + "/" + System.currentTimeMillis() + "-" + templateName;
        write(new File(path + ".txt"), renderText(templateName, variables));
        write(new File(path + ".html"), renderHtml(templateName, variables));
    }

    private void write(File file, String value) {
        try {
            Files.write(value.getBytes(StandardCharsets.UTF_8), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
