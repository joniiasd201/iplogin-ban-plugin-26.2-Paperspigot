package de.server.iplogin.listener;

import de.server.iplogin.IPLoginPlugin;
import de.server.iplogin.manager.BanManager.BanDaten;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class LoginListener implements Listener {

    private final IPLoginPlugin plugin;

    public LoginListener(IPLoginPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();

        // 1. IP-Ban prüfen
        BanDaten ipBan = plugin.getBanManager().getIpBan(ip);
        if (ipBan != null) {
            String nachricht = plugin.nachricht("msg-ip-gebummt",
                    "grund", ipBan.grund,
                    "dauer", ipBan.formatierteDauer(plugin),
                    "admin", ipBan.gebanntVon);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, nachricht);
            return;
        }

        // 2. Spieler-Ban prüfen
        BanDaten spielerBan = plugin.getBanManager().getSpielerBan(uuid);
        if (spielerBan != null) {
            String nachricht = plugin.nachricht("msg-spieler-gebannt",
                    "grund", spielerBan.grund,
                    "dauer", spielerBan.formatierteDauer(plugin),
                    "admin", spielerBan.gebanntVon);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, nachricht);
            return;
        }

        // 3. IP-Zuordnung prüfen
        boolean hatIps = plugin.getIpManager().hatIps(uuid);

        if (!hatIps) {
            if (plugin.getConfig().getBoolean("automatische-ip-registrierung", true)) {
                plugin.getIpManager().registriereErstenJoin(uuid, ip);
            }
        } else {
            if (!plugin.getIpManager().istIpRegistriert(uuid, ip)) {
                String nachricht = plugin.nachricht("msg-ip-nicht-registriert");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, nachricht);
                return;
            }
        }

        plugin.getIpManager().speichereSpielerName(uuid, name);
    }
}
