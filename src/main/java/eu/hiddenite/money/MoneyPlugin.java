package eu.hiddenite.money;

import eu.hiddenite.money.commands.MoneyCommand;
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

        getProxy().getPluginManager().registerCommand(this, new MoneyCommand(this));
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

        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        String message = getConfig().getString("messages.money");
        String formattedMoney = formatter.format(currentMoney / 100.0);
        message = message.replace("{MONEY}", formattedMoney);
        player.sendMessage(TextComponent.fromLegacyText(message));
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
