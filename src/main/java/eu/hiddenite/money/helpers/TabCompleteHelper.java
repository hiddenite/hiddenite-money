package eu.hiddenite.money.helpers;

import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashSet;
import java.util.Set;

public class TabCompleteHelper {
    public static Set<String> matchPlayer(String argument) {
        Set<String> matches = new HashSet<>();
        String search = argument.toUpperCase();
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getName().toUpperCase().startsWith(search)) {
                matches.add(player.getName());
            }
        }
        return matches;
    }

    public static Set<String> empty() {
        return ImmutableSet.of();
    }
}
