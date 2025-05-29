package Trade_Player.Listeners;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import Trade_Player.Main_plugin;
import Trade_Player.Managers.TRADE_Manager;
import Trade_Player.Managers.TradeSession;

public class TRADE_Listener implements Listener {
    private final Main_plugin plugin;
    private boolean isClosing = false;

    public TRADE_Listener(Main_plugin plugin) {
        this.plugin = plugin;
    }

@EventHandler(priority = EventPriority.HIGHEST)
public void onInventoryClick(InventoryClickEvent e) {
    if (!e.getView().getTitle().contains("TRADE")) return;

    Player player = (Player) e.getWhoClicked();
    TradeSession session = TRADE_Manager.getTradeSession(player);
    if (session == null) return;

    // Xử lý các loại click đặc biệt
    if (e.getClick().isShiftClick() || e.getClick().isKeyboardClick()) {
        e.setCancelled(true);
        return;
    }

    // Xử lý khi click từ inventory người chơi
    if (e.getClickedInventory() == player.getInventory()) {
        // Chỉ cho phép kéo thả vào slot hợp lệ
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            e.setCancelled(true);
        }
        return;
    }

    // Xử lý click trong GUI trade
    int slot = e.getRawSlot();

    // Xử lý custom slot (nút xác nhận/hủy)
    if (isCustomSlot(slot)) {
        e.setCancelled(true);
        handleCustomSlotClick(player, slot);
        return;
    }

    // Kiểm tra slot hợp lệ
    if (!isAllowedForPlayer(player, slot)) {
        e.setCancelled(true);
        return;
    }

    // Cho phép thao tác với vật phẩm
    switch (e.getAction()) {
        case PLACE_ALL:
        case PLACE_ONE:
        case PLACE_SOME:
        case SWAP_WITH_CURSOR:
            session.addItem(player, slot, e.getCursor());
            break;
        case PICKUP_ALL:
        case PICKUP_HALF:
        case PICKUP_SOME:
        case PICKUP_ONE:
            session.addItem(player, slot, null);
            break;
        default:
            e.setCancelled(true);
    }
}

    private boolean isCustomSlot(int slot) {
        List<Map<?, ?>> customSlots = plugin.getUIConfig().getMapList("custom_slots");
        return customSlots != null && customSlots.stream().anyMatch(s -> (int) s.get("slot") == slot);
    }

    private void handleCustomSlotClick(Player player, int slot) {
        List<Map<?, ?>> customSlots = plugin.getUIConfig().getMapList("custom_slots");
        if (customSlots == null) {
            Bukkit.getLogger().warning("Custom slots config is null!");
            return;
        }
        
        for (Map<?, ?> config : customSlots) {
            if ((int) config.get("slot") == slot) {
                String action = (String) config.get("action");
                TradeSession session = TRADE_Manager.getTradeSession(player);
                
                if (session == null) {
                    player.sendMessage("§cKhông tìm thấy phiên trade!");
                    return;
                }
                
                Bukkit.getLogger().info("Player " + player.getName() + " clicked custom slot: " + slot + " (" + action + ")");
                
                switch (action) {
                    case "accept":
                        session.confirmTrade(player);
                        break;
                    case "deny":
                        session.cancelTrade();
                        break;
                    default:
                        Bukkit.getLogger().warning("Unknown custom slot action: " + action);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (isClosing) return;
        isClosing = true;

        if (!e.getView().getTitle().contains("TRADE")) {
            isClosing = false;
            return;
        }

        Player player = (Player) e.getPlayer();
        TradeSession session = TRADE_Manager.getTradeSession(player);
        if (session != null) {
            Player partner = (session.getPlayer1().equals(player)) ? session.getPlayer2() : session.getPlayer1();

            // Chỉ huỷ phiên nếu CẢ HAI CHƯA XÁC NHẬN
            if (!session.isAccepted1() && !session.isAccepted2()) {
                TRADE_Manager.removeTradeSession(player);
                TRADE_Manager.removeTradeSession(partner);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (partner.isOnline() && partner.getOpenInventory().getTitle().contains("TRADE")) {
                        partner.closeInventory();
                    }
                }, 1L);

                player.sendMessage(plugin.getMessenger().get("TRADE_CANCELLED"));
                partner.sendMessage(plugin.getMessenger().get("TRADE_CANCELLED"));
            }
        }

        isClosing = false;
    }

@EventHandler(priority = EventPriority.HIGHEST)
public void onInventoryDrag(InventoryDragEvent e) {
    if (!e.getView().getTitle().contains("TRADE")) return;

    Player player = (Player) e.getWhoClicked();
    TradeSession session = TRADE_Manager.getTradeSession(player);
    if (session == null) return;

    // Nếu bất kỳ slot kéo thả không hợp lệ -> cancel toàn bộ
    for (int slot : e.getRawSlots()) {
        if (!isAllowedForPlayer(player, slot)) {
            e.setCancelled(true);
            return;
        }
    }
}


// Thêm phương thức kiểm tra slot hợp lệ cho từng người chơi
private boolean isAllowedForPlayer(Player player, int slot) {
    TradeSession session = TRADE_Manager.getTradeSession(player);
    if (session == null) return false;

    // Xác định player1 và player2
    boolean isPlayer1 = player.equals(session.getPlayer1());
    int[] allowedSlots = isPlayer1 ? 
        new int[]{10, 11, 19, 20, 21, 28, 29, 30, 37, 38, 39} : 
        new int[]{15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};

    for (int s : allowedSlots) {
        if (slot == s) return true;
    }
    return false;
}
}