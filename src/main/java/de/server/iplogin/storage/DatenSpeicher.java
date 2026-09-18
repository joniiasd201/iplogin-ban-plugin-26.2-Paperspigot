package de.server.iplogin.storage;

import de.server.iplogin.IPLoginPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class DatenSpeicher {

    private final IPLoginPlugin plugin;
    private final File spielerDatei;
    private final File banDatei;
    private final File ipDatei;

    private FileConfiguration spielerConfig;
    private FileConfiguration banConfig;
    private FileConfiguration ipConfig;

    public DatenSpeicher(IPLoginPlugin plugin) {
        this.plugin = plugin;
        this.spielerDatei = new File(plugin.getDataFolder(), "players.yml");
        this.banDatei = new File(plugin.getDataFolder(), "bans.yml");
        this.ipDatei = new File(plugin.getDataFolder(), "ips.yml");
    }

    public void ladeAlle() {
        spielerConfig = ladeDatei(spielerDatei);
        banConfig = ladeDatei(banDatei);
        ipConfig = ladeDatei(ipDatei);
    }

    private FileConfiguration ladeDatei(File datei) {
        if (!datei.exists()) {
            try {
                datei.getParentFile().mkdirs();
                datei.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte Datei nicht erstellen: " + datei.getName());
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(datei);
    }

    public void speichereAlle() {
        speichere(spielerConfig, spielerDatei);
        speichere(banConfig, banDatei);
        speichere(ipConfig, ipDatei);
    }

    private void speichere(FileConfiguration config, File datei) {
        try {
            config.save(datei);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte Datei nicht speichern: " + datei.getName());
            e.printStackTrace();
        }
    }

    public void speichereSpieler() { speichere(spielerConfig, spielerDatei); }
    public void speichereBans() { speichere(banConfig, banDatei); }
    public void speichereIps() { speichere(ipConfig, ipDatei); }

    public FileConfiguration getSpielerConfig() { return spielerConfig; }
    public FileConfiguration getBanConfig() { return banConfig; }
    public FileConfiguration getIpConfig() { return ipConfig; }
}
