package eu.hiddenite.money.commands;

import eu.hiddenite.money.MoneyPlugin;
import eu.hiddenite.money.helpers.TabCompleteHelper;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;

public class PayCommand extends Command implements TabExecutor {
    private final MoneyPlugin plugin;

    public PayCommand(MoneyPlugin plugin) {
        super("pay");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }
        if (args.length != 2) {
            String error = plugin.getConfig().getString("messages.pay-usage");
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        ProxiedPlayer toPlayer = plugin.getProxy().getPlayer(args[0]);
        if (toPlayer == null) {
            String error = plugin.getConfig().getString("messages.error-player-not-online");
            error = error.replace("{PLAYER}", args[0]);
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        double amount = -1;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ignored) {
        }
        if (amount < 0.01 || amount > 1000000000.0) {
            String error = plugin.getConfig().getString("messages.error-invalid-amount");
            error = error.replace("{AMOUNT}", args[1]);
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        long trueAmount = (long)(amount * 100);

        ProxiedPlayer fromPlayer = (ProxiedPlayer)sender;
        plugin.payMoney(fromPlayer, toPlayer, trueAmount);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabCompleteHelper.matchPlayer(args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("100", "10", "1", "0.01");
        }
        return TabCompleteHelper.empty();
    }
}
