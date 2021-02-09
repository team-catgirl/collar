package team.catgirl.collar.server;

import team.catgirl.collar.server.configuration.Configuration;

import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Configuration configuration = args.length > 0 && "environment".equals(args[0]) ? Configuration.fromEnvironment() : Configuration.defaultConfiguration();
        WebServer webServer = new WebServer(configuration);
        webServer.start((services) -> LOGGER.info("Do you want to play a block game game?"));
    }
}
