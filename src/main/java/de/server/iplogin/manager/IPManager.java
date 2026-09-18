package de.server.iplogin.manager;

import de.server.iplogin.IPLoginPlugin;
import de.server.iplogin.storage.DatenSpeicher;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class IPManager {

    private final IPLoginPlugin plugin;
    private final DatenSpeicher speicher;
    private final Map<String, List<String>> spielerIps = new HashMap<>();

    public IPManager(IPLoginPlugin plugin, DatenSpeicher speicher) {
        this.plugin = plugin;
        this.speicher = speicher;
        ladeAusConfig();
    }

    private void ladeAusConfig() {
        spielerIps.clear();
        FileConfiguration cfg = speicher.getIpConfig();
        if (cfg.getConfigurationSection("spieler") == null) return;

        for (String uuid : cfg.getConfigurationSection("spieler").getKeys(false)) {
            List<String> ips = cfg.getStringList("spieler." + uuid);
            if (ips != null) spielerIps.put(uuid, new ArrayList<>(ips));
        }
        plugin.getLogger().info("IP-Daten geladen: " + spielerIps.size() + " Spieler.");
    }

    public void speichere() {
        FileConfiguration cfg = speicher.getIpConfig();
        cfg.set("spieler", null);
        for (Map.Entry<String, List<String>> e : spielerIps.entrySet()) {
            cfg.set("spieler." + e.getKey(), e.getValue());
        }
        speicher.speichereIps();
    }

    public boolean istIpRegistriert(UUID uuid, String ip) {
        List<String> ips = spielerIps.get(uuid.toString());
        return ips != null && ips.contains(ip);
    }

    public boolean hatIps(UUID uuid) {
        List<String> ips = spielerIps.get(uuid.toString());
        return ips != null && !ips.isEmpty();
    }

    public List<String> getIps(UUID uuid) {
        return spielerIps.getOrDefault(uuid.toString(), Collections.emptyList());
    }

    public boolean fuegeIpHinzu(UUID uuid, String ip) {
        List<String> ips = spielerIps.computeIfAbsent(uuid.toString(), k -> new ArrayList<>());
        if (ips.contains(ip)) return false;

        int max = plugin.getConfig().getInt("max-ips-pro-spieler", 5);
        if (max > 0 && ips.size() >= max) return false;

        ips.add(ip);
        speichere();
        return true;
    }

    public boolean entferneIp(UUID uuid, String ip) {
        List<String> ips = spielerIps.get(uuid.toString());
        if (ips == null || !ips.remove(ip)) return false;
        speichere();
        return true;
    }

    public void entferneAlleIps(UUID uuid) {
        spielerIps.remove(uuid.toString());
        speichere();
    }

    public void registriereErstenJoin(UUID uuid, String ip) {
        List<String> ips = spielerIps.computeIfAbsent(uuid.toString(), k -> new ArrayList<>());
        if (!ips.contains(ip)) {
            ips.add(ip);
            speichere();
        }
    }

    public void speichereSpielerName(UUID uuid, String name) {
        FileConfiguration cfg = speicher.getSpielerConfig();
        cfg.set("spieler." + uuid + ".name", name);
        cfg.set("spieler." + uuid + ".last-seen", System.currentTimeMillis());
        speicher.speichereSpieler();
    }

    public String getSpielerName(UUID uuid) {
        return speicher.getSpielerConfig().getString("spieler." + uuid + ".name");
    }

    public UUID findeUuidByName(String name) {
        FileConfiguration cfg = speicher.getSpielerConfig();
        if (cfg.getConfigurationSection("spieler") == null) return null;
        for (String key : cfg.getConfigurationSection("spieler").getKeys(false)) {
            String gespeichert = cfg.getString("spieler." + key + ".name");
            if (gespeichert != null && gespeichert.equalsIgnoreCase(name)) {
                return UUID.fromString(key);
            }
        }
        return null;
    }
}
