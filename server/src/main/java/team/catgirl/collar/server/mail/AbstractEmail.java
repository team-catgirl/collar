package team.catgirl.collar.server.mail;

import spark.ModelAndView;
import team.catgirl.collar.server.http.HandlebarsTemplateEngine;
import team.catgirl.collar.server.services.profiles.Profile;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractEmail implements Email {
    private HandlebarsTemplateEngine handlebars = new HandlebarsTemplateEngine("/emails");

    protected Map<String, Object> prepareVariables(Profile profile, Map<String, Object> variables) {
        variables = new HashMap<>(variables);
        variables.put("name", profile.name);
        variables.put("email", profile.email);
        return variables;
    }

    protected String renderHtml(String templateName, Map<String, Object> variables) {
        return renderTemplate(templateName + "-html", variables);
    }

    protected String renderText(String templateName, Map<String, Object> variables) {
        return renderTemplate(templateName + "-txt", variables);
    }

    private String renderTemplate(String templateName, Map<String, Object> variables) {
        return handlebars.render(new ModelAndView(variables, templateName));
    }
}
