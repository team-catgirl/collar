package team.catgirl.collar.server.http;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.jetty.io.RuntimeIOException;
import spark.ModelAndView;
import spark.TemplateEngine;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HandlebarsTemplateEngine extends TemplateEngine {

    private final Handlebars handlebars;

    /**
     * Constructs a handlebars template engine
     *
     * @param resourceRoot the resource root
     */
    public HandlebarsTemplateEngine(String resourceRoot, boolean cacheTemplates) {
        TemplateLoader templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix(resourceRoot);
        templateLoader.setSuffix(".mustache");

        Handlebars handlebars = new Handlebars(templateLoader);
        if (cacheTemplates) {
            Cache<TemplateSource, Template> cache = CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(1000).build();
            this.handlebars = handlebars.with(new GuavaTemplateCache(cache));
        } else {
            this.handlebars = handlebars;
        }
    }

    @Override
    public String render(ModelAndView modelAndView) {
        String viewName = modelAndView.getViewName();
        try {
            Template template = handlebars.compile(viewName);
            return template.apply(modelAndView.getModel());
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}
