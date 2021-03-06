package eu.hiddenite.money;

import com.google.gson.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class DailyBonusManager implements Listener {
    private static class LoginData {
        LoginData(long lastLogin, int currentStreak) {
            this.lastLogin = lastLogin;
            this.currentStreak = currentStreak;
        }

        public long lastLogin;
        public int currentStreak;
    }

    private final MoneyPlugin plugin;

    private static final String LAST_LOGINS_FILE = "last-logins.json";
    private final HashMap<UUID, LoginData> lastLogins = new HashMap<>();

    private Configuration getConfig() { return plugin.getConfig(); }
    private Logger getLogger() { return plugin.getLogger(); }

    public DailyBonusManager(MoneyPlugin plugin) {
        this.plugin = plugin;

        if (!getConfig().getBoolean("daily-bonus.enabled")) {
            return;
        }

        loadLastLogins();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler(priority = 100)
    public void onPlayerPostLogin(PostLoginEvent event) {
        LoginData loginData;
        Calendar now = Calendar.getInstance();
        synchronized (lastLogins) {
            loginData = lastLogins.getOrDefault(event.getPlayer().getUniqueId(), null);
        }

        if (loginData != null) {
            Calendar previous = Calendar.getInstance();
            previous.setTimeInMillis(loginData.lastLogin);

            int streak = loginData.currentStreak;

            boolean isSameDay = false;
            boolean isNextDay = false;

            if (previous.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    previous.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                    previous.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                isSameDay = true;
            }

            previous.add(Calendar.DAY_OF_MONTH, 1);

            if (previous.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    previous.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                    previous.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                isNextDay = true;
            }

            if (!isSameDay) {
                if (isNextDay) {
                    streak += 1;
                } else {
                    streak = 1;
                }

                long amount = getConfig().getLong("daily-bonus.money-per-streak") * streak;
                if (amount > getConfig().getLong("daily-bonus.max-per-day")) {
                    amount = getConfig().getLong("daily-bonus.max-per-day");
                }

                plugin.getEconomy().addMoney(event.getPlayer().getUniqueId(), amount);

                String message = streak == 1 ?
                        getConfig().getString("daily-bonus.messages.no-streak") :
                        getConfig().getString("daily-bonus.messages.streak");

                message = message.replace("{AMOUNT}", plugin.getEconomy().format(amount));
                message = message.replace("{STREAK}", String.valueOf(streak));

                event.getPlayer().sendMessage(TextComponent.fromLegacyText(message));
            }

            loginData = new LoginData(now.getTimeInMillis(), streak);
        } else {
            loginData = new LoginData(now.getTimeInMillis(), 1);
        }

        synchronized (lastLogins) {
            lastLogins.put(event.getPlayer().getUniqueId(), loginData);
            plugin.getProxy().getScheduler().runAsync(plugin, this::saveLastLogins);
        }
    }

    private void loadLastLogins() {
        lastLogins.clear();

        File file = new File(plugin.getDataFolder(), LAST_LOGINS_FILE);
        if (file.exists()) {
            try {
                JsonObject root = new JsonParser().parse(new FileReader(file)).getAsJsonObject();
                JsonObject users = root.getAsJsonObject("users");

                for (Map.Entry<String, JsonElement> user : users.entrySet()) {
                    UUID uuid = UUID.fromString(user.getKey());
                    JsonObject data = user.getValue().getAsJsonObject();

                    long ts = data.get("ts").getAsLong();
                    int streak = data.get("streak").getAsInt();

                    lastLogins.put(uuid, new LoginData(ts, streak));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getLogger().info("Loaded " + LAST_LOGINS_FILE + ": " + lastLogins.size());
    }

    private void saveLastLogins() {
        synchronized (lastLogins) {
            File file = new File(plugin.getDataFolder(), LAST_LOGINS_FILE);

            JsonObject users = new JsonObject();
            for (Map.Entry<UUID, LoginData> user : lastLogins.entrySet()) {
                JsonObject data = new JsonObject();
                data.addProperty("ts", user.getValue().lastLogin);
                data.addProperty("streak", user.getValue().currentStreak);
                users.add(user.getKey().toString(), data);
            }

            JsonObject root = new JsonObject();
            root.add("users", users);

            try (Writer writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().create();
                gson.toJson(root, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        getLogger().info("Saved " + LAST_LOGINS_FILE);
    }
}
