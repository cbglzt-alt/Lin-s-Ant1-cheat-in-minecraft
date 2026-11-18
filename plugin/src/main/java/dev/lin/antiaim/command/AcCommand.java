package dev.lin.antiaim.command;

import dev.lin.antiaim.data.PlayerData;
import dev.lin.antiaim.data.PlayerDataService;
import dev.lin.antiaim.punish.PunishmentService;
import dev.lin.antiaim.record.CaptureService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AcCommand implements CommandExecutor, TabCompleter {
    private static final List<String> LABELS = List.of(
            "A1-legit-melee",
            "B1-ka-always-default",
            "B2-ka-onhit",
            "B3-ka-none",
            "C1-ka-max3",
            "C2-ka-only-on-look"
    );

    private final CaptureService captures;
    private final PlayerDataService players;
    private final PunishmentService punishment;

    public AcCommand(CaptureService captures, PlayerDataService players, PunishmentService punishment) {
        this.captures = captures;
        this.players = players;
        this.punishment = punishment;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("linantiaim.admin")) {
            player.sendMessage("No permission.");
            return true;
        }
        if (args.length < 1) {
            usage(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("vl")) {
            showVl(player, player.getUniqueId());
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) {
            showVl(player, player.getUniqueId());
            player.sendMessage(captures.statusLine(player));
            return true;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            PlayerData data = players.get(player);
            punishment.reset(data);
            player.sendMessage("VL reset.");
            return true;
        }
        if (!args[0].equalsIgnoreCase("record")) {
            usage(player);
            return true;
        }
        if (args.length < 2) {
            usage(player);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "start" -> {
                    if (args.length < 3) {
                        player.sendMessage("Need label. Example: /ac record start B1-ka-always-default");
                        return true;
                    }
                    String file = captures.start(player, args[2]);
                    player.sendMessage("Recording → plugins/LinAntiAim/records/" + file);
                }
                case "stop" -> {
                    String file = captures.stop(player, "command");
                    if (file == null) {
                        player.sendMessage("Not recording.");
                    } else {
                        player.sendMessage("Stopped → " + file);
                    }
                }
                case "status" -> player.sendMessage(captures.statusLine(player));
                default -> player.sendMessage("Unknown: start|stop|status");
            }
        } catch (Exception e) {
            player.sendMessage("Error: " + e.getMessage());
        }
        return true;
    }

    private void showVl(Player viewer, java.util.UUID target) {
        PlayerData data = players.get(target);
        if (data == null) {
            viewer.sendMessage("VL: no data.");
            return;
        }
        viewer.sendMessage("VL total=" + data.totalVl()
                + " (cancel@" + punishment.cancelAtVl() + ", kick@" + punishment.kickAtVl() + ")"
                + " snap=" + data.aimSnapVl()
                + " align=" + data.alignVl()
                + " aura=" + data.auraVl()
                + (data.damageCancelled(System.currentTimeMillis()) ? " [damage-cancel ACTIVE]" : ""));
    }

    private static void usage(CommandSender s) {
        s.sendMessage("Usage: /ac record <start|stop|status> [label] | /ac vl | /ac reset");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("record", "vl", "status", "reset"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("record")) {
            return filter(List.of("start", "stop", "status"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("record") && args[1].equalsIgnoreCase("start")) {
            return filter(LABELS, args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
