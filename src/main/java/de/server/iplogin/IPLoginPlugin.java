package de.server.iplogin;

import de.server.iplogin.command.BanCommand;
import de.server.iplogin.command.IPLoginCommand;
import de.server.iplogin.listener.LoginListener;
import de.server.iplogin.manager.BanManager;
import de.server.iplogin.manager.IPManager;
import de.server.iplogin.storage.DatenSpeicher;
import org.bukkit.plugin.java.JavaPlugin;

public class IPLoginPlugin extends JavaPlugin {

    private static IPLoginPlugin instance;
    private DatenSpeicher datenSpeicher;
    private IPManager ipManager;
    private BanManager banManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        this.datenSpeicher = new DatenSpeicher(this);
        this.datenSpeicher.ladeAlle();

        this.ipManager = new IPManager(this, datenSpeicher);
        this.banManager = new BanManager(this, datenSpeicher);

        getServer().getPluginManager().registerEvents(new LoginListener(this), this);

        getCommand("iplogin").setExecutor(new IPLoginCommand(this));
        getCommand("iplogin").setTabCompleter(new IPLoginCommand(this));

        BanCommand banCommand = new BanCommand(this);
        getCommand("ban").setExecutor(banCommand);
        getCommand("unban").setExecutor(banCommand);
        getCommand("banip").setExecutor(banCommand);
        getCommand("unbanip").setExecutor(banCommand);
        getCommand("baninfo").setExecutor(banCommand);
        getCommand("banlist").setExecutor(banCommand);
        getCommand("tempban").setExecutor(banCommand);
        getCommand("ipban").setExecutor(banCommand);
        getCommand("ipunban").setExecutor(banCommand);

        getLogger().info("IPLoginBan wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (datenSpeicher != null) {
            datenSpeicher.speichereAlle();
        }
        getLogger().info("IPLoginBan wurde deaktiviert.");
    }

    public static IPLoginPlugin getInstance() { return instance; }
    public DatenSpeicher getDatenSpeicher() { return datenSpeicher; }
    public IPManager getIpManager() { return ipManager; }
    public BanManager getBanManager() { return banManager; }

    public String nachricht(String pfad, String... ersetzungen) {
        String msg = getConfig().getString(pfad, "&cNachricht nicht gefunden: " + pfad);
        for (int i = 0; i < ersetzungen.length - 1; i += 2) {
            msg = msg.replace("{" + ersetzungen[i] + "}", ersetzungen[i + 1]);
        }
        return msg.replace("&", "§");
    }

    public String prefix() {
        return getConfig().getString("prefix", "&8[&cIPLogin&8] &7").replace("&", "§");
    }
}
