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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyPlugin extends Plugin {
    private Configuration config;
    private Database database;

    private DecimalFormat formatter;

    public Configuration getConfig() {
        return config;
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

        formatter = (DecimalFormat) NumberFormat.getInstance(Locale.CANADA);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        getProxy().getPluginManager().registerCommand(this, new MoneyCommand(this));
        getProxy().getPluginManager().registerCommand(this, new PayCommand(this));
    }

    @Override
    public void onDisable() {
        database.close();
    }

    public void sendCurrentMoneyMessage(ProxiedPlayer player) {
        long currentMoney = 0;
        try (PreparedStatement ps = database.prepareStatement("SELECT amount FROM currency WHERE player_id = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentMoney = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        String message = getConfig().getString("messages.money");
        String formattedMoney = formatter.format(currentMoney / 100.0);
        message = message.replace("{MONEY}", formattedMoney);
        player.sendMessage(TextComponent.fromLegacyText(message));
    }

    public void payMoney(ProxiedPlayer fromPlayer, ProxiedPlayer toPlayer, long amount) {
        boolean wasUpdated;
        try (PreparedStatement ps = database.prepareStatement("UPDATE currency" +
                " SET amount = amount - ?" +
                " WHERE player_id = ? AND amount >= ?")) {
            ps.setLong(1, amount);
            ps.setString(2, fromPlayer.getUniqueId().toString());
            ps.setLong(3, amount);
            wasUpdated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            getLogger().warning("Could not take $" + amount + " from " + fromPlayer.getName());
            e.printStackTrace();
            return;
        }

        if (!wasUpdated) {
            String error = getConfig().getString("messages.error-not-enough-money");
            fromPlayer.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        try (PreparedStatement ps = database.prepareStatement("INSERT INTO currency" +
                " (player_id, amount)" +
                " VALUES (?, ?)" +
                " ON DUPLICATE KEY UPDATE amount = amount + ?"
        )) {
            ps.setString(1, toPlayer.getUniqueId().toString());
            ps.setLong(2, amount);
            ps.setLong(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            getLogger().warning("Could not add $" + amount + " to " + toPlayer.getName());
            e.printStackTrace();
        }

        getLogger().info(fromPlayer.getName() + " sent $" + amount + " to " + toPlayer.getName());

        String formattedAmount = formatter.format(amount / 100.0);

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
