package de.server.iplogin.command;

import de.server.iplogin.IPLoginPlugin;
import de.server.iplogin.manager.BanManager;
import de.server.iplogin.manager.BanManager.BanDaten;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class BanCommand implements CommandExecutor, TabCompleter {

    private final IPLoginPlugin plugin;

    public BanCommand(IPLoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        switch (cmd) {
            case "ban":
            case "tempban":
                return handleBan(sender, args, false);
            case "ipban":
                return handleBan(sender, args, true);
            case "unban":
                return handleUnban(sender, args);
            case "banip":
                return handleBanIp(sender, args);
            case "unbanip":
            case "ipunban":
                return handleUnbanIp(sender, args);
            case "baninfo":
                return handleBanInfo(sender, args);
            case "banlist":
                return handleBanList(sender);
            default:
                return false;
        }
    }

    private boolean handleBan(CommandSender sender, String[] args, boolean ipBan) {
        if (!sender.hasPermission(ipBan ? "iplogin.ipban" : "iplogin.ban")) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-berechtigung"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/" + (ipBan ? "ipban" : "ban") + " <spieler> <dauer> <grund>");
            return true;
        }

        String name = args[0];
        String dauerStr = args[1];
        String grund = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        long dauer = BanManager.parseDauer(dauerStr);
        if (dauer == -2) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ungueltige-dauer"));
            return true;
        }

        UUID uuid = findeUuid(name);
        if (uuid == null) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-nicht-gefunden"));
            return true;
        }

        String ip = null;
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) ip = online.getAddress().getAddress().getHostAddress();

        String adminName = sender.getName();
        plugin.getBanManager().banneSpieler(uuid, name, ip, dauer, grund, adminName);

        if (online != null) {
            String kickMsg = plugin.nachricht("msg-spieler-gebannt",
                    "grund", grund,
                    "dauer", BanManager.formatiereDauer(dauer, plugin),
                    "admin", adminName);
            online.kickPlayer(kickMsg);
        }

        if (plugin.getConfig().getBoolean("broadcast-bei-ban", true)) {
            String broadcast = plugin.nachricht("msg-spieler-gebannt-broadcast",
                    "spieler", name, "admin", adminName, "grund", grund,
                    "dauer", BanManager.formatiereDauer(dauer, plugin));
            Bukkit.broadcastMessage(plugin.prefix() + broadcast);
        } else {
            sender.sendMessage(plugin.prefix() + "§a" + name + " wurde gebannt.");
        }
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("iplogin.unban")) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-berechtigung"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/unban <spieler>");
            return true;
        }

        UUID uuid = findeUuid(args[0]);
        if (uuid == null) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-nicht-gefunden"));
            return true;
        }

        if (plugin.getBanManager().entbanneSpieler(uuid)) {
            if (plugin.getConfig().getBoolean("broadcast-bei-unban", true)) {
                String broadcast = plugin.nachricht("msg-spieler-entbannt-broadcast", "spieler", args[0], "admin", sender.getName());
                Bukkit.broadcastMessage(plugin.prefix() + broadcast);
            } else {
                sender.sendMessage(plugin.prefix() + "§a" + args[0] + " wurde entbannt.");
            }
        } else {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ban-nicht-gefunden"));
        }
        return true;
    }

    private boolean handleBanIp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("iplogin.ipban")) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-berechtigung"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/banip <ip> <dauer> <grund>");
            return true;
        }

        String ip = args[0];
        String dauerStr = args[1];
        String grund = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        long dauer = BanManager.parseDauer(dauerStr);
        if (dauer == -2) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ungueltige-dauer"));
            return true;
        }

        plugin.getBanManager().banneIp(ip, "Unbekannt", dauer, grund, sender.getName());
        sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ip-gebannt",
                "ip", ip, "grund", grund, "dauer", BanManager.formatiereDauer(dauer, plugin)));
        return true;
    }

    private boolean handleUnbanIp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("iplogin.ipban")) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-berechtigung"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/unbanip <ip>");
            return true;
        }

        if (plugin.getBanManager().entbanneIp(args[0])) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ip-entbannt", "ip", args[0]));
        } else {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ban-nicht-gefunden"));
        }
        return true;
    }

    private boolean handleBanInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("iplogin.baninfo")) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-berechtigung"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/baninfo <spieler>");
            return true;
        }

        UUID uuid = findeUuid(args[0]);
        if (uuid == null) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-nicht-gefunden"));
            return true;
        }

        BanDaten ban = plugin.getBanManager().getSpielerBan(uuid);
        if (ban == null) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ban-nicht-gefunden"));
            return true;
        }

        sender.sendMessage("§8§m------------------------");
        sender.sendMessage("§cBan-Info: §f" + ban.spielerName);
        sender.sendMessage("§7UUID: §f" + ban.uuid);
        sender.sendMessage("§7Grund: §f" + ban.grund);
        sender.sendMessage("§7Gebannt von: §f" + ban.gebanntVon);
        sender.sendMessage("§7Gebannt am: §f" + new java.util.Date(ban.banZeit));
        sender.sendMessage("§7Dauer: §f" + ban.formatierteDauer(plugin));
        if (!ban.ips.isEmpty()) sender.sendMessage("§7IPs: §f" + String.join(", ", ban.ips));
        sender.sendMessage("§8§m------------------------");
        return true;
    }

    private boolean handleBanList(CommandSender sender) {
        if (!sender.hasPermission("iplogin.banlist")) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-berechtigung"));
            return true;
        }

        plugin.getBanManager().entferneAbgelaufeneBans();
        Map<String, BanDaten> bans = plugin.getBanManager().getSpielerBans();

        if (bans.isEmpty()) {
            sender.sendMessage(plugin.prefix() + "§7Keine aktiven Bans.");
            return true;
        }

        sender.sendMessage("§8§m-------- §cAktive Bans §8§m--------");
        for (BanDaten ban : bans.values()) {
            sender.sendMessage("§c" + ban.spielerName + " §7| §f" + ban.grund + " §7| §f" + ban.formatierteDauer(plugin));
        }
        sender.sendMessage("§8§m------------------------------");
        return true;
    }

    private UUID findeUuid(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online.getUniqueId();
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(name)) return op.getUniqueId();
        }
        return plugin.getIpManager().findeUuidByName(name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmd = command.getName().toLowerCase();

        if (args.length == 1) {
            if (cmd.equals("unban") || cmd.equals("baninfo")) {
                for (BanDaten ban : plugin.getBanManager().getSpielerBans().values()) {
                    if (ban.spielerName.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(ban.spielerName);
                }
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && (cmd.equals("ban") || cmd.equals("tempban") || cmd.equals("ipban"))) {
            for (String d : new String[]{"30m", "1h", "2h", "1d", "2d", "7d", "1d12h", "permanent"}) {
                if (d.startsWith(args[1].toLowerCase())) completions.add(d);
            }
        } else if (args.length == 1 && (cmd.equals("unbanip") || cmd.equals("ipunban"))) {
            for (String ip : plugin.getBanManager().getIpBans().keySet()) {
                if (ip.startsWith(args[0])) completions.add(ip);
            }
        }
        return completions;
    }
}
