package Trade_Player.Managers;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import Trade_Player.Main_plugin;

public class TradeSession {
    private final Main_plugin plugin;
    private final Player player1;
    private final Player player2;
    private boolean accepted1 = false;
    private boolean accepted2 = false;
    private final ItemStack[] items1 = new ItemStack[11]; // 0-10
    private final ItemStack[] items2 = new ItemStack[11]; // 0-10
    public TradeSession(Player p1, Player p2, Main_plugin plugin) {
        this.player1 = p1;
        this.player2 = p2;
        // tham chiếu plugin
        this.plugin = plugin;
        // Tự động mở GUI khi tạo phiên
        openTradeGUI();
    }
    
public void addItem(Player player, int slot, ItemStack item) {
    // Debug log
    Bukkit.getLogger().info("Adding item to slot " + slot + " for " + player.getName() + 
                          ": " + (item != null ? item.getType() + " x" + item.getAmount() : "null"));

    // Ánh xạ slot
    Map<Integer, Integer> slotMapping = player.equals(player1) ?
        Map.of(10,0,11,1,19,2,20,3,21,4,28,5,29,6,30,7,37,8,38,9,39,10) :
        Map.of(15,0,16,1,23,2,24,3,25,4,32,5,33,6,34,7,41,8,42,9,43,10);

    Integer mappedSlot = slotMapping.get(slot);
    if (mappedSlot == null) return;

    // Cập nhật vật phẩm
    ItemStack[] targetItems = player.equals(player1) ? items1 : items2;
    targetItems[mappedSlot] = item != null ? item.clone() : null;

    // Cập nhật GUI
    updateTradeGUI();
}

private void updateTradeGUI() {
    Bukkit.getScheduler().runTask(plugin, () -> {
        Inventory gui = player1.getOpenInventory().getTopInventory();
        
        // Cập nhật vật phẩm cho player1
        Map<Integer, Integer> slotMap1 = Map.of(10,0,11,1,19,2,20,3,21,4,28,5,29,6,30,7,37,8,38,9,39,10);
        slotMap1.forEach((guiSlot, arrSlot) -> {
            gui.setItem(guiSlot, items1[arrSlot] != null ? items1[arrSlot].clone() : null);
        });

        // Cập nhật vật phẩm cho player2
        Map<Integer, Integer> slotMap2 = Map.of(15,0,16,1,23,2,24,3,25,4,32,5,33,6,34,7,41,8,42,9,43,10);
        slotMap2.forEach((guiSlot, arrSlot) -> {
            gui.setItem(guiSlot, items2[arrSlot] != null ? items2[arrSlot].clone() : null);
        });
    });
}

    public void confirmTrade(Player player) {
        if (player.equals(player1)) {
            accepted1 = true;
        } else if (player.equals(player2)) {
            accepted2 = true;
        }

        // Thông báo cho người xác nhận
        player.sendMessage(plugin.getMessenger().get("TRADE_YOU_CONFIRMED"));

        // Thông báo cho đối phương
        Player partner = (player.equals(player1)) ? player2 : player1;
        partner.sendMessage(plugin.getMessenger().get("TRADE_PARTNER_CONFIRMED"));

        if (accepted1 && accepted2) {
            executeTrade();
        }
    }
    
    // Chuyển vật phẩm
    private void executeTrade() {
        // Kiểm tra inventory đủ chỗ
        if (!canFitItems(player1, items2) || !canFitItems(player2, items1)) {
            cancelTrade();
            player1.sendMessage(plugin.getMessenger().get("TRADE_INVENTORY_FULL"));
            player2.sendMessage(plugin.getMessenger().get("TRADE_INVENTORY_FULL"));
            return;
        }

        // Chuyển đồ từ A -> B
        transferItems(player1, player2, items1);
        // Chuyển đồ từ B -> A
        transferItems(player2, player1, items2);

        // Thông báo thành công
        player1.sendMessage(plugin.getMessenger().get("TRADE_SUCCESS"));
        player2.sendMessage(plugin.getMessenger().get("TRADE_SUCCESS"));
        closeTradeGUI();
    }

    private boolean canFitItems(Player player, ItemStack[] items) {
        Inventory inv = player.getInventory();
        for (ItemStack item : items) {
            if (item == null) continue;
            Map<Integer, ItemStack> remaining = inv.addItem(item.clone());
            if (!remaining.isEmpty()) {
                inv.removeItem(item); // Rollback
                return false;
            }
            inv.removeItem(item); // Rollback check
        }
        return true;
    }

    private void transferItems(Player from, Player to, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item == null) continue;
            ItemStack clone = item.clone();
            // Thêm vật phẩm vào inventory của người nhận
            Map<Integer, ItemStack> remaining = to.getInventory().addItem(clone);
            // Nếu không đủ chỗ, thả vật phẩm xuống đất
            remaining.values().forEach(remainingItem -> 
                from.getWorld().dropItem(from.getLocation(), remainingItem)
            );
            // Xóa vật phẩm khỏi inventory của người gửi
            from.getInventory().removeItem(clone); // Sử dụng removeItem thay vì removeItemAnySlot
        }
    }

    // Phương thức mở GUI
    private void openTradeGUI() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory gui = Bukkit.createInventory(null, 54, "TRADE - " + player1.getName() + " | " + player2.getName());

            // Đặt barrier vào ô trống không phải slot allowed/custom
            ItemStack barrier = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = barrier.getItemMeta();
            meta.setDisplayName(" ");
            barrier.setItemMeta(meta);

            for (int i = 0; i < 54; i++) {
                if (!isAllowedSlot(i) && !isCustomSlot(i) && gui.getItem(i) == null) {
                    gui.setItem(i, barrier);
                }
            }

            // ⭐ Thêm đoạn code này để tải custom_slots từ config ⭐
            List<Map<?, ?>> customSlots = plugin.getUIConfig().getMapList("custom_slots");
            if (customSlots == null) return; // Kiểm tra null

            // Đặt vật phẩm custom (nút xác nhận/hủy)
            for (Map<?, ?> slotConfig : customSlots) {
                int slot = (int) slotConfig.get("slot");
                String itemName = (String) slotConfig.get("item");
                String displayName = ChatColor.translateAlternateColorCodes('&', (String) slotConfig.get("name"));
                
                ItemStack item = new ItemStack(Material.getMaterial(itemName));
                ItemMeta itemMeta = item.getItemMeta();
                itemMeta.setDisplayName(displayName);
                item.setItemMeta(itemMeta);
                
                gui.setItem(slot, item);
            }

            // Mở GUI cho cả hai
            player1.openInventory(gui);
            player2.openInventory(gui);
        });
    }
    // Phương thức kiểm tra slot allowed
    private boolean isAllowedSlot(int slot) {
        int[] allowedSlots = {10, 11, 15, 16, 19, 20, 21, 23, 24, 25, 28, 29, 30, 32, 33, 34, 37, 38, 39, 41, 42, 43};
        for (int s : allowedSlots) {
            if (slot == s) return true;
        }
        return false;
    }
    private void closeTradeGUI() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player1.isOnline()) player1.closeInventory();
            if (player2.isOnline()) player2.closeInventory();
        });
    }

    // Phương thức hủy trao đổi
public void cancelTrade() {
    returnItems(player1, items1);
    returnItems(player2, items2);
    
    player1.sendMessage(plugin.getMessenger().get("TRADE_CANCELLED"));
    player2.sendMessage(plugin.getMessenger().get("TRADE_CANCELLED"));
    closeTradeGUI();
}

    private void returnItems(Player owner, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null) owner.getInventory().addItem(item);
        }
    }

    // Getter để truy cập từ bên ngoài (nếu cần)
    public Player getPlayer1() {return player1;}
    public Player getPlayer2() {return player2;}
    public boolean isAccepted1() {return accepted1;}
    public boolean isAccepted2() {return accepted2;}
    public ItemStack[] getItems1() {return items1;}
    public ItemStack[] getItems2() {return items2;}

    private boolean isCustomSlot(int slot) {
    List<Map<?, ?>> customSlots = plugin.getUIConfig().getMapList("custom_slots");
    return customSlots != null && customSlots.stream().anyMatch(s -> (int) s.get("slot") == slot);
}
}