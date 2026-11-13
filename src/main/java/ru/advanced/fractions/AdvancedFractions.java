
package ru.advanced.fractions;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import ru.advanced.fractions.database.DatabaseManager;
import ru.advanced.fractions.manager.FractionManager;
import ru.advanced.fractions.manager.MemberManager;
import ru.advanced.fractions.model.Fraction;
import ru.advanced.fractions.model.FractionMember;
import ru.advanced.fractions.util.ColorUtil;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancedFractions extends JavaPlugin {

    private DatabaseManager database;
    private FractionManager fractions;
    private MemberManager members;

    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        database = new DatabaseManager();
        fractions = new FractionManager(getConfig());
        members = new MemberManager(database);

        database.init(new File(
                getDataFolder(),
                getConfig().getString("settings.database-file", "fractions.db")
        )).thenCompose(v -> members.load())
                .whenComplete((v, error) ->
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (error != null) {
                                getLogger().severe(
                                        "Database initialization failed: " + error.getMessage()
                                );
                                getServer().getPluginManager().disablePlugin(this);
                                return;
                            }

                            registerCommands();
                            getLogger().info("AdvancedFractions enabled.");
                        })
                );
    }

    private void registerCommands() {
        List<String> commands = List.of(
                "fraction", "invite", "faccept", "fdeny",
                "uninvite", "giverank", "fwarn", "unfwarn",
                "f", "r", "fb", "rb", "d", "gov",
                "freload", "setleader"
        );

        for (String name : commands) {
            var command = getCommand(name);

            if (command != null) {
                command.setExecutor(this::execute);
            } else {
                getLogger().warning("Command not found in plugin.yml: " + name);
            }
        }
    }

    private boolean execute(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("freload")) {
            if (!sender.hasPermission("fractions.admin")) {
                return message(sender, "no-permission");
            }

            reloadConfig();
            fractions = new FractionManager(getConfig());

            sender.sendMessage(color("&aКонфигурация перезагружена."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        FractionMember member = members.get(player.getUniqueId());

        switch (name) {

            case "fraction": {
                String available = String.join(
                        ", ",
                        fractions.all().stream()
                                .map(Fraction::getId)
                                .toList()
                );

                player.sendMessage(color("&6Доступные фракции: &f" + available));

                if (member != null) {
                    player.sendMessage(color(
                            "&aВаша фракция: " + member.getFractionId()
                                    + " | ранг " + member.getRank()
                    ));
                }

                return true;
            }

            case "invite": {
                if (args.length < 1 || member == null || member.getRank() < 9) {
                    return message(player, "not-member");
                }

                Player target = Bukkit.getPlayerExact(args[0]);

                if (target == null) {
                    return message(player, "player-not-found");
                }

                if (members.get(target.getUniqueId()) != null) {
                    return message(player, "not-member");
                }

                long expiration = System.currentTimeMillis()
                        + getConfig().getLong("settings.invite-expire-seconds", 60) * 1000L;

                invites.put(
                        target.getUniqueId(),
                        new Invite(
                                player.getUniqueId(),
                                member.getFractionId(),
                                expiration
                        )
                );

                target.sendMessage(color(
                        "&e" + player.getName()
                                + " приглашает вас во фракцию "
                                + member.getFractionId()
                                + ". Используйте /faccept"
                ));

                return true;
            }

            case "faccept": {
                Invite invite = invites.remove(player.getUniqueId());

                if (invite == null || invite.expires() < System.currentTimeMillis()) {
                    return message(player, "invite-expired");
                }

                FractionMember newMember = new FractionMember(
                        player.getUniqueId(),
                        player.getName(),
                        invite.fraction(),
                        1,
                        0,
                        false
                );

                returnAsync(
                        player,
                        members.add(newMember),
                        "&aВы вступили во фракцию " + invite.fraction() + "."
                );

                return true;
            }

            case "fdeny": {
                invites.remove(player.getUniqueId());
                player.sendMessage(color("&eПриглашение отклонено."));
                return true;
            }

            case "uninvite": {
                if (args.length < 1 || member == null || member.getRank() < 9) {
                    return message(player, "not-member");
                }

                OfflinePlayerLookup target = offline(args[0]);

                if (target == null) {
                    return message(player, "player-not-found");
                }

                returnAsync(
                        player,
                        members.remove(target.uuid()),
                        "&aИгрок уволен."
                );

                return true;
            }

            case "giverank": {
                if (args.length < 2 || member == null || member.getRank() < 9) {
                    return message(player, "not-member");
                }

                OfflinePlayerLookup target = offline(args[0]);

                if (target == null) {
                    return message(player, "player-not-found");
                }

                try {
                    int rank = Integer.parseInt(args[1]);

                    if (rank < 1 || rank > 10 || rank >= member.getRank()) {
                        return message(player, "no-permission");
                    }

                    returnAsync(
                            player,
                            members.changeRank(target.uuid(), rank),
                            "&aРанг обновлён."
                    );

                } catch (NumberFormatException exception) {
                    player.sendMessage(color("&cРанг должен быть числом от 1 до 10."));
                }

                return true;
            }

            case "fwarn": {
                if (args.length < 1 || member == null || member.getRank() < 9) {
                    return message(player, "not-member");
                }

                OfflinePlayerLookup target = offline(args[0]);

                if (target == null) {
                    return message(player, "player-not-found");
                }

                members.addWarn(target.uuid()).whenComplete((count, error) ->
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (error != null) {
                                player.sendMessage(color("&cОшибка базы данных."));
                            } else {
                                player.sendMessage(color(
                                        "&aВыговор выдан. Всего: " + count
                                ));
                            }
                        })
                );

                return true;
            }

            case "f":
            case "r":
            case "fb":
            case "rb":
            case "d": {
                if (member == null) {
                    return message(player, "not-member");
                }

                if (args.length == 0) {
                    return true;
                }

                String text = String.join(" ", args);

                Fraction fraction = fractions.get(member.getFractionId());

                if (fraction == null) {
                    return message(player, "not-member");
                }

                for (Player online : Bukkit.getOnlinePlayers()) {
                    FractionMember onlineMember = members.get(online.getUniqueId());

                    if (onlineMember == null) {
                        continue;
                    }

                    boolean canReceive;

                    if (name.equals("d")) {
                        canReceive = fraction.hasDepartmentAccess();
                    } else {
                        canReceive = onlineMember.getFractionId()
                                .equals(member.getFractionId());
                    }

                    if (canReceive) {
                        online.sendMessage(color(
                                "&8[" + member.getFractionId() + "] "
                                        + "&f" + player.getName()
                                        + ": " + text
                        ));
                    }
                }

                return true;
            }

            case "gov": {
                if (member == null || member.getRank() < 9) {
                    return message(player, "no-permission");
                }

                String announcement = String.join(" ", args);

                if (announcement.isBlank()) {
                    return true;
                }

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(color(
                            "&6[ГОСВОЛНА] &f"
                                    + player.getName()
                                    + ": " + announcement
                    ));
                }

                return true;
            }

            case "setleader": {
                if (!player.hasPermission("fractions.admin") || args.length < 2) {
                    return message(player, "no-permission");
                }

                OfflinePlayerLookup target = offline(args[0]);

                if (target == null) {
                    return message(player, "player-not-found");
                }

                returnAsync(
                        player,
                        members.add(new FractionMember(
                                target.uuid(),
                                args[0],
                                args[1],
                                10,
                                0,
                                false
                        )),
                        "&aЛидер назначен."
                );

                return true;
            }
        }

        return true;
    }

    private record Invite(UUID inviter, String fraction, long expires) {}

    private record OfflinePlayerLookup(UUID uuid) {}

    private OfflinePlayerLookup offline(String name) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);

        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            return null;
        }

        return new OfflinePlayerLookup(offlinePlayer.getUniqueId());
    }

    private void returnAsync(
            Player player,
            CompletableFuture<Void> future,
            String success
    ) {
        future.whenComplete((result, error) ->
                Bukkit.getScheduler().runTask(this, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    player.sendMessage(color(
                            error == null
                                    ? success
                                    : "&cОперация не выполнена: ошибка базы данных."
                    ));
                })
        );
    }

    private boolean message(CommandSender sender, String key) {
        sender.sendMessage(color(
                getConfig().getString("messages.prefix", "")
                        + getConfig().getString("messages." + key, "&cОшибка.")
        ));

        return true;
    }

    private String color(String text) {
        return ColorUtil.color(text);
    }

    public FractionManager getFractions() {
        return fractions;
    }

    public MemberManager getMembers() {
        return members;
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }
}
