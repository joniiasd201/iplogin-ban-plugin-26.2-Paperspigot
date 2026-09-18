package de.server.iplogin.manager;

import de.server.iplogin.IPLoginPlugin;
import de.server.iplogin.storage.DatenSpeicher;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BanManager {

    private final IPLoginPlugin plugin;
    private final DatenSpeicher speicher;
    private final Map<String, BanDaten> spielerBans = new HashMap<>();
    private final Map<String, BanDaten> ipBans = new HashMap<>();

    private static final Pattern DAUER_PATTERN =
            Pattern.compile("(?:(\\d+)w)?(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

    public BanManager(IPLoginPlugin plugin, DatenSpeicher speicher) {
        this.plugin = plugin;
        this.speicher = speicher;
        ladeBans();
    }

    private void ladeBans() {
        spielerBans.clear();
        ipBans.clear();
        FileConfiguration cfg = speicher.getBanConfig();

        ConfigurationSection spielerSec = cfg.getConfigurationSection("spieler-bans");
        if (spielerSec != null) {
            for (String uuid : spielerSec.getKeys(false)) {
                ConfigurationSection s = spielerSec.getConfigurationSection(uuid);
                if (s == null) continue;
                BanDaten ban = new BanDaten();
                ban.uuid = uuid;
                ban.spielerName = s.getString("name", "Unbekannt");
                ban.grund = s.getString("grund", "Kein Grund");
                ban.gebanntVon = s.getString("gebannt-von", "Konsole");
                ban.banZeit = s.getLong("ban-zeit");
                ban.ablaufZeit = s.getLong("ablauf-zeit", -1);
                ban.permanent = s.getBoolean("permanent", false);
                ban.ips = s.getStringList("ips");
                spielerBans.put(uuid, ban);
            }
        }

        ConfigurationSection ipSec = cfg.getConfigurationSection("ip-bans");
        if (ipSec != null) {
            for (String ip : ipSec.getKeys(false)) {
                ConfigurationSection s = ipSec.getConfigurationSection(ip);
                if (s == null) continue;
                BanDaten ban = new BanDaten();
                ban.ip = ip;
                ban.spielerName = s.getString("name", "Unbekannt");
                ban.grund = s.getString("grund", "Kein Grund");
                ban.gebanntVon = s.getString("gebannt-von", "Konsole");
                ban.banZeit = s.getLong("ban-zeit");
                ban.ablaufZeit = s.getLong("ablauf-zeit", -1);
                ban.permanent = s.getBoolean("permanent", false);
                ipBans.put(ip, ban);
            }
        }
        plugin.getLogger().info("Bans geladen: " + spielerBans.size() + " Spieler, " + ipBans.size() + " IPs.");
    }

    public void speichereBans() {
        FileConfiguration cfg = speicher.getBanConfig();
        cfg.set("spieler-bans", null);
        cfg.set("ip-bans", null);

        for (Map.Entry<String, BanDaten> e : spielerBans.entrySet()) {
            BanDaten ban = e.getValue();
            String base = "spieler-bans." + e.getKey();
            cfg.set(base + ".name", ban.spielerName);
            cfg.set(base + ".grund", ban.grund);
            cfg.set(base + ".gebannt-von", ban.gebanntVon);
            cfg.set(base + ".ban-zeit", ban.banZeit);
            cfg.set(base + ".ablauf-zeit", ban.ablaufZeit);
            cfg.set(base + ".permanent", ban.permanent);
            cfg.set(base + ".ips", ban.ips);
        }

        for (Map.Entry<String, BanDaten> e : ipBans.entrySet()) {
            BanDaten ban = e.getValue();
            String base = "ip-bans." + e.getKey().replace(".", "_");
            cfg.set(base + ".name", ban.spielerName);
            cfg.set(base + ".grund", ban.grund);
            cfg.set(base + ".gebannt-von", ban.gebanntVon);
            cfg.set(base + ".ban-zeit", ban.banZeit);
            cfg.set(base + ".ablauf-zeit", ban.ablaufZeit);
            cfg.set(base + ".permanent", ban.permanent);
        }
        speicher.speichereBans();
    }

    public BanDaten getSpielerBan(UUID uuid) {
        BanDaten ban = spielerBans.get(uuid.toString());
        if (ban != null && ban.istAbgelaufen()) {
            spielerBans.remove(uuid.toString());
            speichereBans();
            return null;
        }
        return ban;
    }

    public BanDaten getIpBan(String ip) {
        BanDaten ban = ipBans.get(ip);
        if (ban != null && ban.istAbgelaufen()) {
            ipBans.remove(ip);
            speichereBans();
            return null;
        }
        return ban;
    }

    public boolean istSpielerGebannt(UUID uuid) { return getSpielerBan(uuid) != null; }
    public boolean istIpGebannt(String ip) { return getIpBan(ip) != null; }

    public Map<String, BanDaten> getSpielerBans() { return Collections.unmodifiableMap(spielerBans); }
    public Map<String, BanDaten> getIpBans() { return Collections.unmodifiableMap(ipBans); }

    public void banneSpieler(UUID uuid, String name, String ip, long dauerMillis, String grund, String admin) {
        boolean permanent = dauerMillis < 0;
        long jetzt = System.currentTimeMillis();
        long ablauf = permanent ? -1 : jetzt + dauerMillis;

        BanDaten spielerBan = new BanDaten();
        spielerBan.uuid = uuid.toString();
        spielerBan.spielerName = name;
        spielerBan.grund = grund;
        spielerBan.gebanntVon = admin;
        spielerBan.banZeit = jetzt;
        spielerBan.ablaufZeit = ablauf;
        spielerBan.permanent = permanent;
        spielerBan.ips = new ArrayList<>();
        if (ip != null) spielerBan.ips.add(ip);
        spielerBans.put(uuid.toString(), spielerBan);

        speichereBans();
    }

    public void banneIp(String ip, String name, long dauerMillis, String grund, String admin) {
        boolean permanent = dauerMillis < 0;
        long jetzt = System.currentTimeMillis();
        long ablauf = permanent ? -1 : jetzt + dauerMillis;

        BanDaten ban = new BanDaten();
        ban.ip = ip;
        ban.spielerName = name;
        ban.grund = grund;
        ban.gebanntVon = admin;
        ban.banZeit = jetzt;
        ban.ablaufZeit = ablauf;
        ban.permanent = permanent;
        ipBans.put(ip, ban);
        speichereBans();
    }

    public boolean entbanneSpieler(UUID uuid) {
        if (spielerBans.remove(uuid.toString()) != null) {
            speichereBans();
            return true;
        }
        return false;
    }

    public boolean entbanneIp(String ip) {
        if (ipBans.remove(ip) != null) {
            speichereBans();
            return true;
        }
        return false;
    }

    public void entferneAbgelaufeneBans() {
        boolean geaendert = false;
        Iterator<Map.Entry<String, BanDaten>> it = spielerBans.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().istAbgelaufen()) { it.remove(); geaendert = true; }
        }
        it = ipBans.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().istAbgelaufen()) { it.remove(); geaendert = true; }
        }
        if (geaendert) speichereBans();
    }

    public static long parseDauer(String eingabe) {
        if (eingabe.equalsIgnoreCase("permanent") || eingabe.equalsIgnoreCase("perm")) return -1;
        Matcher matcher = DAUER_PATTERN.matcher(eingabe.toLowerCase());
        if (!matcher.matches()) return -2;

        long millis = 0;
        if (matcher.group(1) != null) millis += Long.parseLong(matcher.group(1)) * 7L * 24 * 60 * 60 * 1000;
        if (matcher.group(2) != null) millis += Long.parseLong(matcher.group(2)) * 24L * 60 * 60 * 1000;
        if (matcher.group(3) != null) millis += Long.parseLong(matcher.group(3)) * 60L * 60 * 1000;
        if (matcher.group(4) != null) millis += Long.parseLong(matcher.group(4)) * 60L * 1000;
        if (matcher.group(5) != null) millis += Long.parseLong(matcher.group(5)) * 1000L;

        return millis > 0 ? millis : -2;
    }

    public static String formatiereDauer(long millis, IPLoginPlugin plugin) {
        if (millis < 0) return plugin.getConfig().getString("dauer-permanent", "Permanent");
        long sekunden = millis / 1000;
        long tage = sekunden / 86400;
        long stunden = (sekunden % 86400) / 3600;
        long minuten = (sekunden % 3600) / 60;
        long sek = sekunden % 60;

        StringBuilder sb = new StringBuilder();
        if (tage > 0) sb.append(tage).append("d ");
        if (stunden > 0) sb.append(stunden).append("h ");
        if (minuten > 0) sb.append(minuten).append("m ");
        if (sek > 0 && tage == 0) sb.append(sek).append("s");
        return sb.toString().trim();
    }

    public static String formatiereAblauf(long ablaufZeit, IPLoginPlugin plugin) {
        if (ablaufZeit < 0) return plugin.getConfig().getString("dauer-permanent", "Permanent");
        long verbleibend = ablaufZeit - System.currentTimeMillis();
        if (verbleibend <= 0) return "Abgelaufen";
        return formatiereDauer(verbleibend, plugin);
    }

    public static class BanDaten {
        public String uuid;
        public String ip;
        public String spielerName;
        public String grund;
        public String gebanntVon;
        public long banZeit;
        public long ablaufZeit;
        public boolean permanent;
        public List<String> ips = new ArrayList<>();

        public boolean istAbgelaufen() {
            if (permanent) return false;
            return ablaufZeit > 0 && System.currentTimeMillis() > ablaufZeit;
        }

        public String formatierteDauer(IPLoginPlugin plugin) {
            return formatiereAblauf(ablaufZeit, plugin);
        }
    }
}
