package me.philippheuer.twitch4j.modules;

import de.btobastian.sdcf4j.handler.TwitchHandler;
import me.philippheuer.twitch4j.TwitchClient;
import lombok.Getter;

@Getter
public class Sdcf4 extends IModule {
    private TwitchHandler handler;

    public Sdcf4() {
        super("Simple Discord Command Framework for Java (Twitch4J)", "Damian Staszewski", "0.1-SNAPSHOT");
    }

    @Override
    public void enable(TwitchClient client) {
        this.handler = new TwitchHandler(client);
        this.handler.registerListeners();
    }

    @Override
    public void disable() {
        this.handler.unregisterListeners();
    }
}
