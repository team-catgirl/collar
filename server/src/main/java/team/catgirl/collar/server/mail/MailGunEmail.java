package team.catgirl.collar.server.mail;

import com.commit451.mailgun.Contact;
import com.commit451.mailgun.Mailgun;
import com.commit451.mailgun.SendMessageRequest;
import org.jetbrains.annotations.NotNull;
import spark.ModelAndView;
import team.catgirl.collar.server.http.HandlebarsTemplateEngine;
import team.catgirl.collar.server.services.profiles.Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailGunEmail extends AbstractEmail {
    private final Mailgun mailgun;

    public MailGunEmail(Mailgun mailgun) {
        this.mailgun = mailgun;
    }

    @Override
    public void send(Profile profile, String subject, String templateName, Map<String, Object> variables) {
        variables = prepareVariables(profile, variables);
        SendMessageRequest message = new SendMessageRequest.Builder(new Contact("noreply@collarmc.com", "Collar"))
                .to(List.of(new Contact(profile.email, profile.name)))
                .subject(subject)
                .text(renderText(templateName, variables))
                .html(renderHtml(templateName, variables))
                .build();
        mailgun.sendMessage(message);
    }
}
