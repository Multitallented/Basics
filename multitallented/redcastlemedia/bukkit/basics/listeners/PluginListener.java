package multitallented.redcastlemedia.bukkit.basics.listeners;

import java.util.HashSet;
import multitallented.plugins.heroscoreboard.HeroScoreboard;
import multitallented.redcastlemedia.bukkit.basics.Basics;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 *
 * @author Multitallented
 */
public class PluginListener implements Listener {
    private final Basics plugin;
    public final HashSet<String> mutedPlayers = new HashSet<>();
    //private final HashMap<String, Long> lastChat = new HashMap<>();
    public PluginListener(Basics plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.cleanUpPlayer(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.initPlayer(event.getPlayer());
    }
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        switch (event.getPlugin().getDescription().getName()) {
            case "Vault":
                RegisteredServiceProvider<Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
                if (permissionProvider != null) {
                    Basics.perms = permissionProvider.getProvider();
                    System.out.println("[Basics] hooked into Vault");
                }
                break;
            case "HeroScoreboard":
                Basics.hsb = (HeroScoreboard) event.getPlugin();
                break;
        }
    }
    
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        switch (event.getPlugin().getDescription().getName()) {
            case "Vault":
                Basics.perms = null;
                break;
            case "HeroScoreboard":
                Basics.hsb = null;
                break;
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onPlayerHeal(EntityRegainHealthEvent event) {
        if (Basics.hsb == null) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player p = (Player) event.getEntity();
        
        if (event.getRegainReason() != RegainReason.SATIATED) {
            return;
        }
        boolean hi = Basics.hsb.isPlayerInCombat(p);
        if (hi) {
            event.setCancelled(true);
        }
    }
    
    /*@EventHandler
    public void onPlayerHunger(FoodLevelChangeEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (player.getFoodLevel() -  event.getFoodLevel() == 1) {
            player.setSaturation(player.getSaturation() + 36);
        }
    }*/
    
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        if (mutedPlayers.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "[Basics] You are muted!");
            return;
        }
        /*if (lastChat.containsKey(event.getPlayer().getName()) &&
            System.currentTimeMillis() < lastChat.get(event.getPlayer().getName()) + 400) {
            event.setCancelled(true);
            event.getPlayer().kickPlayer("spam");
            return;
        }
        lastChat.put(event.getPlayer().getName(), System.currentTimeMillis());*/
    }
    
    /*@EventHandler
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        if (lastChat.containsKey(event.getPlayer().getName()) &&
            System.currentTimeMillis() < lastChat.get(event.getPlayer().getName()) + 400) {
            event.setCancelled(true);
            event.getPlayer().kickPlayer("spam");
            return;
        }
        lastChat.put(event.getPlayer().getName(), System.currentTimeMillis());
    }*/
}
