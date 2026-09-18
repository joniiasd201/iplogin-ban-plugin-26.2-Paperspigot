package de.server.iplogin.command;

import de.server.iplogin.IPLoginPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IPLoginCommand implements CommandExecutor, TabCompleter {

    private final IPLoginPlugin plugin;

    public IPLoginCommand(IPLoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iplogin.admin")) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-berechtigung"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/iplogin <spieler> §7| §fadd §7| §fremove §7| §fclear §7| §freload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getDatenSpeicher().ladeAlle();
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-reload-erfolgreich"));
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/iplogin add <spieler> <ip>");
                return true;
            }
            UUID uuid = findeUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-nicht-gefunden"));
                return true;
            }
            String ip = args[2];
            if (!istGueltigeIp(ip)) {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ungueltige-ip"));
                return true;
            }
            if (plugin.getIpManager().fuegeIpHinzu(uuid, ip)) {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ip-hinzugefuegt", "ip", ip, "spieler", args[1]));
            } else {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ip-bereits-vorhanden"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/iplogin remove <spieler> <ip>");
                return true;
            }
            UUID uuid = findeUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-nicht-gefunden"));
                return true;
            }
            if (plugin.getIpManager().entferneIp(uuid, args[2])) {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ip-entfernt", "ip", args[2], "spieler", args[1]));
            } else {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-ip-nicht-vorhanden"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (args.length < 2) {
                sender.sendMessage(plugin.prefix() + "§7Verwendung: §f/iplogin clear <spieler>");
                return true;
            }
            UUID uuid = findeUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-nicht-gefunden"));
                return true;
            }
            plugin.getIpManager().entferneAlleIps(uuid);
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-alle-ips-entfernt", "spieler", args[1]));
            return true;
        }

        UUID uuid = findeUuid(args[0]);
        if (uuid == null) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-nicht-gefunden"));
            return true;
        }

        List<String> ips = plugin.getIpManager().getIps(uuid);
        sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-spieler-ip-liste", "spieler", args[0]));
        if (ips.isEmpty()) {
            sender.sendMessage(plugin.prefix() + plugin.nachricht("msg-keine-ips"));
        } else {
            for (String ip : ips) {
                sender.sendMessage("§7 - §f" + ip);
            }
        }
        return true;
    }

    private UUID findeUuid(String name) {
        if (Bukkit.getPlayer(name) != null) return Bukkit.getPlayer(name).getUniqueId();
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(name)) return op.getUniqueId();
        }
        return plugin.getIpManager().findeUuidByName(name);
    }

    private boolean istGueltigeIp(String ip) {
        String regex = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        return ip.matches(regex);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("iplogin.admin")) return completions;

        if (args.length == 1) {
            for (String sub : new String[]{"add", "remove", "clear", "reload"}) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && op.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(op.getName());
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("clear"))) {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && op.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(op.getName());
                }
            }
        }
        return completions;
    }
}
