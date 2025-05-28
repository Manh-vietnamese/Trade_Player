package Trade_Player;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import Trade_Player.Commands.TRADE_Command;
import Trade_Player.Listeners.TRADE_Listener;
import Trade_Player.Messenger.Messager;

public class Main_plugin extends JavaPlugin {
    private Messager messenger;
    private FileConfiguration uiConfig;

    @Override
    public void onEnable() {
        // Tạo thư mục data nếu chưa tồn tại
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Khởi tạo Messenger
        this.messenger = new Messager(this, getDataFolder(), getLogger());

        // Tạo file cấu hình
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("UI_trade.yml", false); // Đảm bảo file được copy

        // Tải UI_trade.yml
        File uiFile = new File(getDataFolder(), "UI_trade.yml");
        if (!uiFile.exists()) {
            saveResource("UI_trade.yml", false);
        }
        uiConfig = YamlConfiguration.loadConfiguration(uiFile);

        // Đăng ký lệnh và listener
        getCommand("trade").setExecutor(new TRADE_Command(this));
        getServer().getPluginManager().registerEvents(new TRADE_Listener(this), this);
    }

    public Messager getMessenger() {return messenger;}
    public FileConfiguration getUIConfig() {return uiConfig;}
    public static Main_plugin getPlugin() {return getPlugin(Main_plugin.class);}
}