package eu.hiddenite.money;

import eu.hiddenite.money.commands.MoneyCommand;
import eu.hiddenite.money.commands.PayCommand;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MoneyPlugin extends Plugin {
    private Configuration config;
    private Database database;
    private Economy economy;

    public Configuration getConfig() {
        return config;
    }

    public Economy getEconomy() {
        return economy;
    }

    @Override
    public void onEnable() {
        if (!loadConfiguration()) {
            return;
        }

        database = new Database(config, getLogger());
        if (!database.open()) {
            getLogger().warning("Could not connect to the database. Plugin disabled.");
            return;
        }

        economy = new Economy(database, getLogger());

        getProxy().getPluginManager().registerCommand(this, new MoneyCommand(this));
        getProxy().getPluginManager().registerCommand(this, new PayCommand(this));

        new DailyBonusManager(this);
    }

    @Override
    public void onDisable() {
        database.close();
    }

    public void sendCurrentMoneyMessage(ProxiedPlayer player) {
        long currentMoney = economy.getMoney(player.getUniqueId());

        String message = getConfig().getString("messages.money");
        message = message.replace("{MONEY}", economy.format(currentMoney));
        player.sendMessage(TextComponent.fromLegacyText(message));
    }

    public void payMoney(ProxiedPlayer fromPlayer, ProxiedPlayer toPlayer, long amount) {
        Economy.ResultType result = economy.removeMoney(fromPlayer.getUniqueId(), amount);
        if (result == Economy.ResultType.NOT_ENOUGH_MONEY) {
            String error = getConfig().getString("messages.error-not-enough-money");
            fromPlayer.sendMessage(TextComponent.fromLegacyText(error));
        }
        if (result != Economy.ResultType.SUCCESS) {
            return;
        }

        result = economy.addMoney(toPlayer.getUniqueId(), amount);
        if (result != Economy.ResultType.SUCCESS) {
            return;
        }

        getLogger().info(fromPlayer.getName() + " sent $" + amount + " to " + toPlayer.getName());

        String formattedAmount = economy.format(amount);

        String message = getConfig().getString("messages.pay-sent-to");
        message = message.replace("{AMOUNT}", formattedAmount);
        message = message.replace("{PLAYER}", toPlayer.getName());
        fromPlayer.sendMessage(TextComponent.fromLegacyText(message));

        message = getConfig().getString("messages.pay-received-from");
        message = message.replace("{AMOUNT}", formattedAmount);
        message = message.replace("{PLAYER}", fromPlayer.getName());
        toPlayer.sendMessage(TextComponent.fromLegacyText(message));
    }

    private boolean loadConfiguration() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                getLogger().warning("Could not create the configuration folder.");
                return false;
            }
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            getLogger().warning("No configuration file found, creating a default one.");

            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            config = ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
