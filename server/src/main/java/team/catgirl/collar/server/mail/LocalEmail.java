package team.catgirl.collar.server.mail;

import com.google.common.io.Files;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.api.profiles.Profile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalEmail extends AbstractEmail {

    private final Logger LOGGER = Logger.getLogger(LocalEmail.class.getName());

    public LocalEmail(AppUrlProvider urlProvider) {
        super(urlProvider);
    }

    @Override
    public void send(Profile profile, String subject, String templateName, Map<String, Object> variables) {
        variables = prepareVariables(profile, variables);
        String path = "target/emails/" + profile.name + "/" + System.currentTimeMillis() + "-" + templateName;
        write(new File(path + ".txt"), renderText(templateName, variables));
        write(new File(path + ".html"), renderHtml(templateName, variables));
    }

    private void write(File file, String value) {
        file.getParentFile().mkdirs();
        try {
            Files.write(value.getBytes(StandardCharsets.UTF_8), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.log(Level.INFO, "Email sent " + file);
    }
}
