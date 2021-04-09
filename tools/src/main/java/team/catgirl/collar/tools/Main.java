package team.catgirl.collar.tools;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import team.catgirl.collar.client.utils.Http;
import team.catgirl.collar.utils.Utils;


import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class Main {

    public static void main(String[] args) {
        System.out.println("Collar Administration Tool (CAT). Type `help` for commands.");
        AdminTool adminTool = new AdminTool(Http.collar());
        CommandDispatcher<AdminTool> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("use")
                .then(argument("config", string())
                .executes(ctx -> {
                    String config = ctx.getArgument("config", String.class);
                    return adminTool.currentConfig(config);
                })));

        dispatcher.register(literal("use")
                .executes(ctx -> {
                    String name = adminTool.currentConfig();
                    if (name == null) {
                        System.out.println("no config in use");
                    } else {
                        System.out.println("using config " + name);
                    }
                    return 1;
                }));

        dispatcher.register(literal("reset")
                .then(literal("password")
                .then(argument("email", string())
                .executes(ctx -> {
                    String email = ctx.getArgument("email", String.class);
                    adminTool.resetPassword(email);
                    return 1;
                }))));

        dispatcher.register(literal("reset")
                .then(literal("identity")
                .then(argument("email", string())
                        .executes(ctx -> {
                            String email = ctx.getArgument("email", String.class);
                            adminTool.resetIdentity(email);
                            return 1;
                }))));

        dispatcher.register(literal("config")
            .then(literal("list").executes(ctx -> {
                Set<String> configNames = adminTool.listConfig();
                if (configNames.isEmpty()) {
                    System.err.println("there are no configurations set");
                } else {
                    configNames.forEach(System.out::println);
                }
                return 1;
            })));

        dispatcher.register(literal("config")
                .then(literal("remove")
                .then(argument("name", string()).executes(ctx -> {
                    String name = ctx.getArgument("name", String.class);
                    adminTool.removeConfig(name);
                    return 1;
                }))));

        dispatcher.register(literal("config")
            .then(literal("add")
            .then(argument("name", string())
            .then(argument("apiUrl", string())
            .then(argument("email", string())
            .then(argument("password", string())
                    .executes(ctx -> {
                        String name = ctx.getArgument("name", String.class);
                        String apiUrl = ctx.getArgument("apiUrl", String.class);
                        String email = ctx.getArgument("email", String.class);
                        String password = ctx.getArgument("password", String.class);
                        return adminTool.addConfig(name, apiUrl, email, password);
        })))))));

        dispatcher.register(literal("exit")
            .executes(ctx -> 0));

        dispatcher.register(literal("help")
            .executes(ctx -> {
                System.out.println(getUsage(dispatcher, adminTool));
                return 1;
        }));

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            System.out.println();
            System.out.print("> ");
            String line = scanner.nextLine().strip();
            int result;
            try {
                result = dispatcher.execute(line, adminTool);
            } catch (Throwable e) {
                System.err.println(e.getMessage());
                System.err.println(getUsage(dispatcher, adminTool));
                result = -1;
                if (!(e instanceof CommandSyntaxException)) {
                    e.printStackTrace();
                }
            }
            if (result == 0) {
                break;
            }
        }
    }

    private static String getUsage(CommandDispatcher<AdminTool> dispatcher, AdminTool source) {
        StringBuilder builder = new StringBuilder();
        builder.append("usages:");
        for (String s : dispatcher.getAllUsage(dispatcher.getRoot(), source, true)) {
            builder.append("\n").append(" ").append(s);
        }
        return builder.toString();
    }

    private static LiteralArgumentBuilder<AdminTool> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<AdminTool, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
