package de.btobastian.sdcf4j.handler;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.jcabi.log.Logger;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.Sdcf4jMessage;
import de.btobastian.sdcf4j.TwitchCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.philippheuer.twitch4j.TwitchClient;
import me.philippheuer.twitch4j.events.Event;
import me.philippheuer.twitch4j.events.IListener;
import me.philippheuer.twitch4j.events.event.irc.ChannelMessageEvent;
import me.philippheuer.twitch4j.events.event.irc.PrivateMessageEvent;
import me.philippheuer.twitch4j.message.commands.CommandPermission;
import me.philippheuer.twitch4j.model.Channel;
import me.philippheuer.twitch4j.model.User;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class TwitchHandler extends CommandHandler {

    private final TwitchClient client;

    private final Multimap<User, CommandPermission> permissions = MultimapBuilder.hashKeys().hashSetValues().build();
    private final SortedSet<SimpleCommand> commands = new TreeSet<SimpleCommand>();

    protected String defaultPrefix = "!";

    public TwitchHandler(TwitchClient client) {
        this.client = client;
    }

    public void registerListeners() {
        client.getDispatcher().registerListener((IListener<ChannelMessageEvent>) this::invokeChannelMessage);
        client.getDispatcher().registerListener((IListener<PrivateMessageEvent>) this::invokePrivateMessage);
    }

    public void unregisterListeners() {
        client.getDispatcher().unregisterListener((IListener<ChannelMessageEvent>) this::invokeChannelMessage);
        client.getDispatcher().unregisterListener((IListener<PrivateMessageEvent>) this::invokePrivateMessage);
    }

    @Override
    public void registerCommand(CommandExecutor executor) {
        for (Method method : executor.getClass().getMethods()) {
            if (method.isAnnotationPresent(TwitchCommand.class)) {
                TwitchCommand annotation = method.getAnnotation(TwitchCommand.class);
                if (annotation.aliases().length > 0) {
                    commands.add(new SimpleCommand(annotation, method, executor));
                } else throw new IllegalArgumentException("Aliases array cannot be empty!");
            }
        }
    }

    private void invokeChannelMessage(final ChannelMessageEvent event) {
        if (event.getMessage().startsWith(defaultPrefix)) {
            CommandSplitter splitter = new CommandSplitter(event.getMessage().substring(defaultPrefix.length()));
            commands.stream()
                    .filter(cmd -> Arrays.asList(cmd.getCommandAnnotation().aliases()).contains(splitter.getCommand()))
                    .findAny()
                    .ifPresent(cmd -> {
                        TwitchCommand commandAnnotation = cmd.getCommandAnnotation();
                        if (commandAnnotation.privateMessages()) {
                            return;
                        }
                        if (!commandAnnotation.channelMessages()) {
                            return;
                        }
                        if (!hasPermission(event.getUser(), commandAnnotation.requiredPermissions())) {
                            if (Sdcf4jMessage.MISSING_PERMISSIONS.getMessage() != null) {
                                event.sendMessage(Sdcf4jMessage.MISSING_PERMISSIONS.getMessage());
                            }
                        }

                        final Object[] parameters = getParameters(splitter, cmd, event);
                        if (commandAnnotation.async()) {
                            final SimpleCommand commandFinal = cmd;
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    invokeMethod(commandFinal, event, parameters);
                                }
                            });
                            t.setDaemon(true);
                            t.start();
                        } else {
                            invokeMethod(cmd, event, parameters);
                        }
                    });

        }
    }

    private void invokePrivateMessage(final PrivateMessageEvent event) {
        if (event.getMessage().startsWith(defaultPrefix)) {
            CommandSplitter splitter = new CommandSplitter(event.getMessage().substring(defaultPrefix.length()));
        }
    }

    private <E extends Event> void invokeMethod(SimpleCommand command, E event, Object[] parameters) {
        Method method = command.getMethod();
        Object reply = null;
        try {
            method.setAccessible(true);
            reply = method.invoke(command.getExecutor(), parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.error(this, "Cannot invoke method %s!", method.getName(), ExceptionUtils.getStackTrace(e));
        }
        if (reply != null && reply instanceof String) {
            if (PrivateMessageEvent.class.isAssignableFrom(event.getClass())) {
                ((PrivateMessageEvent) event).sendMessage(String.valueOf(reply));
            } else if (ChannelMessageEvent.class.isAssignableFrom(event.getClass())) {
                ((ChannelMessageEvent) event).sendMessage(String.valueOf(reply));
            }
        }
    }

    private <E extends Event> Object[] getParameters(CommandSplitter splitter, SimpleCommand cmd, E event) {
        List<Class<?>> parameterTypes = Arrays.asList(cmd.getMethod().getParameterTypes());
        final List<Object> parameters = new ArrayList<>(parameterTypes.size());
        int strCounter = 0;
        parameterTypes.forEach(type -> {
            if (type == CommandSplitter.class) {
                parameters.add(splitter);
            } else if (type == String.class) {
                parameters.add(splitter.getCommand());
            } else if (type == String[].class) {
                parameters.add(splitter.getArgs().toArray(new String[splitter.getArgs().size()]));
            } else if (type == ChannelMessageEvent.class && ChannelMessageEvent.class.isAssignableFrom(event.getClass())) {
                parameters.add(event);
            } else if (type == PrivateMessageEvent.class && PrivateMessageEvent.class.isAssignableFrom(event.getClass())) {
                parameters.add(event);
            } else if (type == TwitchClient.class) {
                parameters.add(event.getClient());
            } else if (type == Channel.class && ChannelMessageEvent.class.isAssignableFrom(event.getClass())) {
                parameters.add(((ChannelMessageEvent)event).getChannel());
            } else if (type == User.class) {
                if (PrivateMessageEvent.class.isAssignableFrom(event.getClass()))
                    parameters.add(((PrivateMessageEvent)event).getUser());
                else if (ChannelMessageEvent.class.isAssignableFrom(event.getClass()))
                    parameters.add(((ChannelMessageEvent)event).getUser());
            } else if (type == Object[].class) {
                parameters.add(getObjectsFromString(event.getClient(), splitter.getArgs()));
            } else parameters.add(null);
        });
        return parameters.toArray();
    }

    private Object[] getObjectsFromString(TwitchClient client, List<String> args) {
        List<Object> objects = new ArrayList<>(args.size());
        args.forEach(arg -> objects.add(getObjectFromString(client, arg)));

        return objects.toArray();
    }

    private Object getObjectFromString(TwitchClient client, String arg) {
        if (arg.toLowerCase().matches("^@[0-9a-z][0-9a-z_]{3,}$")) {
            return client.getUserEndpoint().getUserByUserName(arg.substring(1).toLowerCase()).get();
        } else return arg;
    }

    public void unregisterCommand(String command) {
        commands.removeIf(cmd -> Arrays
                .asList(cmd.commandAnnotation.aliases())
                .contains((command.startsWith(defaultPrefix)) ?
                        command.substring(defaultPrefix.length()) :
                        command));
    }

    public void addPermission(User user, CommandPermission permission) {
        permissions.put(user, permission);
    }

    public boolean hasPermission(User user, CommandPermission permission) {
        return permissions.containsEntry(user, permission);
    }

    public void setDefaultPrefix(String defaultPrefix) {
        if (defaultPrefix == null) {
            this.defaultPrefix = "";
        } else {
            this.defaultPrefix = defaultPrefix.replace(" ", "");
        }
    }

    private boolean checkPermission(CommandPermission[] has, CommandPermission[] required) {
        return Arrays.stream(required)
                .filter(req -> Arrays.asList(has).contains(req))
                .collect(Collectors.toList())
                .size() > 0;
    }

    @Getter
    @AllArgsConstructor
    public class SimpleCommand {
        private final TwitchCommand commandAnnotation;
        private final Method method;
        private final CommandExecutor executor;
    }
}
