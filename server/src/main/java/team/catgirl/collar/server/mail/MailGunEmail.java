package team.catgirl.collar.server.mail;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.api.profiles.Profile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MailGunEmail extends AbstractEmail {

    private static final Logger LOGGER = Logger.getLogger(MailGunEmail.class.getName());

    private final HttpClient http;
    private final String domain;
    private final String apiKey;

    public MailGunEmail(HttpClient http, AppUrlProvider urlProvider, String domain, String apiKey) {
        super(urlProvider);
        this.http = http;
        this.domain = domain;
        this.apiKey = apiKey;
    }

    @Override
    public void send(Profile profile, String subject, String templateName, Map<String, Object> variables) {
        variables = prepareVariables(profile, variables);
        Map<String, Object> formBody = new HashMap<>();
        formBody.put("from", "no-reply@collarmc.com");
        formBody.put("to", profile.email);
        formBody.put("subject", subject);
        formBody.put("html", renderHtml(templateName, variables));
        formBody.put("text", renderText(templateName, variables));

        String auth = BaseEncoding.base64().encode(("api:" + apiKey).getBytes(StandardCharsets.UTF_8));

        try {
            Request request = Request.url("https://api.mailgun.net/v3/" + domain + " /messages")
                    .addHeader("Authorization", "Basic " + auth)
                    .post(formBody);
            http.execute(request, Response.noContent());
            LOGGER.log(Level.INFO, "Sent " + templateName + " email to " + profile.email);
        } catch (HttpException e) {
            LOGGER.log(Level.SEVERE, "Connection issue", e);
        }
    }
}
