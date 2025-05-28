package Trade_Player.Commands;

import Trade_Player.Main_plugin;
import Trade_Player.Managers.TRADE_Manager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class TRADE_Command implements CommandExecutor {
    private final Main_plugin plugin;

    public TRADE_Command(Main_plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Xử lý lệnh reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("trade.reload")) {
                sender.sendMessage(plugin.getMessenger().get("TRADE_NO_PERMISSION"));
                return true;
            }
            plugin.reloadConfig();
            plugin.getMessenger().reload();
            sender.sendMessage(plugin.getMessenger().get("TRADE_RELOAD_SUCCESS"));
            return true;
        }

        // Xử lý lệnh trade thông thường
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessenger().get("TRADE_PLAYER_ONLY"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(plugin.getMessenger().get("TRADE_USAGE"));
            return true;
        }

        // Xử lý sub-commands
        switch (args[0].toLowerCase()) {
            case "accept":
                handleAccept(player); // Chỉ truyền player
                break;
            case "deny":
                handleDeny(player);
                break;
            default:
                handleTradeRequest(player, args[0]); // Truyền player và targetName
                break;
        }
        return true;
    }

    // Xử lý gửi yêu cầu trao đổi
    private void handleTradeRequest(Player sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(plugin.getMessenger().get("TRADE_PLAYER_OFFLINE"));
            return;
        }

        // Kiểm tra khoảng cách
        int maxDistance = plugin.getConfig().getInt("trade_max_distance", 10);
        if (maxDistance != -1 &&
            sender.getLocation().distance(target.getLocation()) > maxDistance) {
            sender.sendMessage(plugin.getMessenger().get("TRADE_TOO_FAR"));
            return;
        }

        // Gửi yêu cầu trao đổi
        TRADE_Manager.sendTradeRequest(sender, target);
        sender.sendMessage(plugin.getMessenger().get("TRADE_REQUEST_SENT"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", sender.getName());
        target.sendMessage(plugin.getMessenger().get("TRADE_REQUEST_RECEIVED", placeholders));
    }
    
    // Xử lý chấp nhận yêu cầu
private void handleAccept(Player player) {
    if (!TRADE_Manager.hasPendingRequest(player)) {
        player.sendMessage(plugin.getMessenger().get("TRADE_NO_PENDING_REQUEST"));
        return;
    }

    Player sender = TRADE_Manager.getPendingRequestSender(player);
    if (sender == null || !sender.isOnline()) {
        player.sendMessage(plugin.getMessenger().get("TRADE_REQUEST_EXPIRED"));
        return;
    }

    // Kiểm tra khoảng cách
    int maxDistance = plugin.getConfig().getInt("trade_max_distance", 10);
    if (player.getLocation().distance(sender.getLocation()) > maxDistance) {
        player.sendMessage(plugin.getMessenger().get("TRADE_TOO_FAR"));
        return;
    }

    TRADE_Manager.acceptTradeRequest(player);
}

    // Xử lý từ chối yêu cầu
    private void handleDeny(Player player) {
        if (!TRADE_Manager.hasPendingRequest(player)) {
            player.sendMessage(plugin.getMessenger().get("TRADE_NO_PENDING_REQUEST"));
            return;
        }

        TRADE_Manager.denyTradeRequest(player);
        player.sendMessage(plugin.getMessenger().get("TRADE_DENIED"));
    }
}