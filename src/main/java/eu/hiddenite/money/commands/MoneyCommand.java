package eu.hiddenite.money.commands;

import eu.hiddenite.money.MoneyPlugin;
import eu.hiddenite.money.helpers.TabCompleteHelper;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class MoneyCommand extends Command implements TabExecutor {
    private final MoneyPlugin plugin;

    public MoneyCommand(MoneyPlugin plugin) {
        super("money");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }
        plugin.sendCurrentMoneyMessage((ProxiedPlayer)sender);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return TabCompleteHelper.empty();
    }
}
