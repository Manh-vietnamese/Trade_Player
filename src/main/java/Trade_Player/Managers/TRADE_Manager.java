package Trade_Player.Managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import Trade_Player.Main_plugin;

import java.util.HashMap;
import java.util.Map;

public class TRADE_Manager {
    private static final Map<Player, TradeSession> activeTrades = new HashMap<>();
    private static final Map<Player, Player> pendingRequests = new HashMap<>();
    private static final Map<Player, Long> requestTimestamps = new HashMap<>();

    // ========== Quản lý phiên trao đổi ==========
    public static void createTrade(Player sender, Player target) {
        // Kiểm tra người chơi đã có phiên trao đổi chưa
        if (activeTrades.containsKey(sender) || activeTrades.containsKey(target)) {
            sender.sendMessage("§cBạn đang trong phiên trao đổi khác!");
            return;
        }

        if (!sender.isOnline() || !target.isOnline() || activeTrades.containsKey(sender)) return;

        Main_plugin plugin = Main_plugin.getPlugin();
        TradeSession session = new TradeSession(sender, target, plugin);
        activeTrades.put(sender, session);
        activeTrades.put(target, session);
    }

    public static TradeSession getTradeSession(Player player) {return activeTrades.get(player);}
    
public static void removeTradeSession(Player player) {
    TradeSession session = activeTrades.get(player);
    if (session != null) {
        activeTrades.remove(session.getPlayer1());
        activeTrades.remove(session.getPlayer2());
    }
}

    // ==================== Logic Yêu Cầu Trao Đổi ====================
    public static void sendTradeRequest(Player sender, Player target) {
        pendingRequests.put(target, sender);
        requestTimestamps.put(target, System.currentTimeMillis());
    }

    public static boolean hasPendingRequest(Player target) {return pendingRequests.containsKey(target);}

    public static boolean isRequestExpired(Player target) {
        long timeout = Bukkit.getPluginManager().getPlugin("Trade_Player").getConfig().getInt("trade_request_timeout") * 1000L;
        return (System.currentTimeMillis() - requestTimestamps.get(target)) > timeout;
    }

    public static void acceptTradeRequest(Player target) {
        Player sender = pendingRequests.get(target);
        pendingRequests.remove(target);
        requestTimestamps.remove(target);

        // Tạo phiên trao đổi và mở GUI
        Main_plugin plugin = Main_plugin.getPlugin(Main_plugin.class);
        TradeSession session = new TradeSession(sender, target, plugin);
        activeTrades.put(sender, session);
        activeTrades.put(target, session);
    }

    public static void denyTradeRequest(Player target) {
        pendingRequests.remove(target);
        requestTimestamps.remove(target);
    }

    public static void removePendingRequest(Player target) {
        pendingRequests.remove(target);
        requestTimestamps.remove(target);
    }

    public static Player getPendingRequestSender(Player target) {
        return pendingRequests.get(target);
    }
}