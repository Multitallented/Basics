package multitallented.redcastlemedia.bukkit.basics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import multitallented.plugins.heroscoreboard.HeroScoreboard;
import multitallented.redcastlemedia.bukkit.basics.listeners.PluginListener;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Multitallented
 */
public class Basics extends JavaPlugin {
    public static Permission perms;
    public static HeroScoreboard hsb;
    private HashMap<Player, Player> invites = new HashMap<>();
    private HashMap<Player, Player> destinations = new HashMap<>();
    private HashMap<Player, HashSet<Player>> ignored = new HashMap<>();
    private HashMap<String, Long> homeCooldowns = new HashMap<String, Long>();
    private HashMap<String, Location> homeLocation = new HashMap<String, Location>();
    private HashMap<String, String> lastSender = new HashMap<String, String>();
    private PluginListener listener;
    
    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null) {
                Basics.perms = permissionProvider.getProvider();
                System.out.println("[Basics] hooked into Vault");
            }
        }
        listener = new PluginListener(this);
        if (Bukkit.getPluginManager().isPluginEnabled("HeroeScoreboard")) {
            Basics.hsb = (HeroScoreboard) Bukkit.getPluginManager().getPlugin("HeroeScoreboard");
        }
        System.out.println("[Basics] enabled!");
    }
    
    @Override
    public void onDisable() {
        System.out.println("[Basics] disabled!");
    }
    
    public void cleanUpPlayer(Player player) {
        homeCooldowns.remove(player.getName());
        homeLocation.remove(player.getName());
        lastSender.remove(player.getName());
        invites.remove(player);
        destinations.remove(player);
        ignored.remove(player);
    }
    
    public void initPlayer(Player player) {
        homeCooldowns.put(player.getName(), System.currentTimeMillis() + 300000);
    }
    
    @Override
    public boolean onCommand(CommandSender cs, Command command, String label, String[] args) {
        Logger log = Logger.getLogger("Minecraft");
        if (Basics.perms == null) {
            log.warning("[Basics] command attempted while null permissions");
            return true;
        }
        if (label.equals("gm")) {
            if (!(cs instanceof Player)) {
                cs.sendMessage("[Basics] only players can use that command");
                return true;
            }
            Player p = (Player) cs;
            if (!Basics.perms.has(p, "basics.gm")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            
            if (p.getGameMode() == GameMode.SURVIVAL) {
                p.setGameMode(GameMode.CREATIVE);
            } else {
                p.setGameMode(GameMode.SURVIVAL);
            }
            return true;
        } else if (label.equals("spawn")) {
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "[Basics] Only players can use this command");
                return true;
            }
            final Player pl = (Player) cs;
            
            pl.sendMessage(ChatColor.GRAY + "[Basics] You will be teleported in 5s");
            
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

                @Override
                public void run() {
                    if (!pl.isOnline()) {
                        return;
                    }
                    if (pl.isDead()) {
                        pl.sendMessage(ChatColor.RED + "[Basics] Teleport failed. You died");
                    }
                    if (Basics.hsb != null && Basics.hsb.isPlayerInCombat(pl)) {
                        pl.sendMessage(ChatColor.RED + "[Basics] Teleport failed. You are in combat");
                    } else {
                        pl.teleport(Bukkit.getWorld("flat").getSpawnLocation());
                        pl.sendMessage(ChatColor.GREEN + "[Basics] You have been teleported");
                    }
                }
                
            }, 100L);
            
            return true;
        } else if (label.equals("sudo") && args.length > 1) {
            if (!Basics.perms.has(cs, "basics.sudo")) {
                cs.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            Player pl = Bukkit.getPlayer(args[0]);
            if (pl == null) {
                cs.sendMessage(ChatColor.RED + "Unable to find " + args[0]);
                return true;
            }
            String newCommand = "";
            for (String s : args) {
                if (s.equals(args[0])) {
                    continue;
                } else if (s.equals(args[1])) {
                    newCommand += s;
                } else {
                    newCommand += " " + s;
                }
            }
            pl.performCommand(newCommand);
            return true;
        } else if (label.equals("r")) {
            if (!(cs instanceof Player)) {
                return true;
            }
            Player player = (Player) cs;
            if (!lastSender.containsKey(player.getName())) {
                player.sendMessage(ChatColor.RED + "[Basics] You have no previous private messages to reply to.");
                return true;
            }
            String message = "";
            for (String s : args) {
                message += " " + s;
            }
            player.performCommand("msg " + lastSender.get(player.getName()) + message);
            return true;
        } else if (label.equals("mute") && args.length > 0) {
            Player pl = Bukkit.getPlayer(args[0]);
            if (!Basics.perms.has(cs, "basics.mute")) {
                cs.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            if (pl == null) {
                cs.sendMessage(ChatColor.RED + "Unable to find " + args[0]);
                return true;
            }
            if (listener.mutedPlayers.contains(pl.getName())) {
                listener.mutedPlayers.remove(pl.getName());
                cs.sendMessage(ChatColor.GREEN + "[Basics] Unmuted " + pl.getName());
                pl.sendMessage(ChatColor.GREEN + "[Basics] You have been unmuted");
            } else {
                listener.mutedPlayers.add(pl.getName());
                cs.sendMessage(ChatColor.GREEN + "[Basics] Muted " + pl.getName());
                pl.sendMessage(ChatColor.RED + "[Basics] You have been muted");
            }
            return true;
        } else if (label.equals("home")) {
            
            if (!(cs instanceof Player)) {
                return true;
            }
            final Player player = (Player) cs;
            
            if (homeCooldowns.containsKey(player.getName()) && homeCooldowns.get(player.getName()) > System.currentTimeMillis()) {
                
                long gracePeriod = Math.abs(System.currentTimeMillis() - homeCooldowns.get(player.getName()));
                
                long minutes = (gracePeriod / (1000 * 60)) % 60;
                long seconds = (gracePeriod / 1000) % 60;
                player.sendMessage(ChatColor.RED + "[Basics] Home cooldown: " + ChatColor.RED + minutes + "m " + seconds + "s");
                return true;
            }
            final Location loc;
            
            if (!homeLocation.containsKey(player.getName())) {
                File playerFile = new File(getDataFolder(), player.getName());
                if (!playerFile.exists()) {
                    try {
                        playerFile.createNewFile();
                    } catch (IOException ex) {
                        player.sendMessage(ChatColor.RED + "[Basics] unable to find home location");
                        return true;
                    }
                }
                FileConfiguration config = new YamlConfiguration();
                try {
                    config.load(playerFile);
                    String[] locationString = config.getString("home").split(",");
                    loc = new Location(Bukkit.getWorld(locationString[0]), Double.parseDouble(locationString[1]), 
                               Double.parseDouble(locationString[2]),
                               Double.parseDouble(locationString[3]));
                    homeLocation.put(player.getName(), loc);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "[Basics] unable to find home location");
                    return true;
                }
            } else {
                loc = homeLocation.get(player.getName());
            }
            if (loc == null) {
                player.sendMessage(ChatColor.RED + "[Basics] unable to find home location");
                return true;
            }
            
            homeCooldowns.put(player.getName(), System.currentTimeMillis() + 300000);
            player.sendMessage(ChatColor.GREEN + "[Basics] You will be teleported home in 5s");
            
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (player.isDead()) {
                        player.sendMessage(ChatColor.RED + "[Basics] Teleport failed. You died");
                        return;
                    }
                    if (Basics.hsb != null && Basics.hsb.isPlayerInCombat(player)) {
                        try {
                            player.sendMessage(ChatColor.RED + "[Basics] Teleport failed. You are in combat");
                        } catch (Exception e) {
                        }
                        return;
                    }
                    player.teleport(loc);
                    player.sendMessage(ChatColor.GREEN + "[Basics] You have been teleported home.");
                }
                
            }, 100L);
            return true;
        } else if (label.equals("sethome")) {
            if (!(cs instanceof Player)) {
                return true;
            }
            Player player = (Player) cs;
            
            Location loc = player.getLocation();
            File playerFile = new File(getDataFolder(), player.getName());
            if (!playerFile.exists()) {
                try {
                    playerFile.createNewFile();
                } catch (IOException ex) {
                    player.sendMessage(ChatColor.RED + "[Basics] unable to find home location");
                    return true;
                }
            }
            FileConfiguration config = new YamlConfiguration();
            try {
                config.load(playerFile);
                config.set("home", loc.getWorld().getName() + "," +
                        loc.getX() + "," + loc.getY() + "," + loc.getZ());
                config.save(playerFile);
                homeLocation.put(player.getName(), loc);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "[Basics] unable to find home location");
                return true;
            }
            
            player.sendMessage(ChatColor.GREEN + "[Basics] Home location set");
            
            return true;
        } else if (label.equals("tp") && args.length > 0) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage("[Basics] only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.tp")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            Player pl = Bukkit.getPlayer(args[0]);
            if (pl == null) {
                String[] locationParts = args[0].split(",");
                if (locationParts.length != 3 && locationParts.length != 4) {
                    p.sendMessage(ChatColor.RED + "Unable to find " + args[0]);
                    return true;
                }
                Location location;
                try {
                    if (locationParts.length == 4) {
                        location = new Location(Bukkit.getWorld(locationParts[0]),
                                Double.parseDouble(locationParts[1]),
                                Double.parseDouble(locationParts[2]),
                                Double.parseDouble(locationParts[3]));
                    } else {
                        location = new Location(p.getWorld(),
                                Double.parseDouble(locationParts[0]),
                                Double.parseDouble(locationParts[1]),
                                Double.parseDouble(locationParts[2]));
                    }
                    p.teleport(location);
                } catch (Exception e) {
                    p.sendMessage(ChatColor.RED + "Invalid Location");
                    return true;
                }
                return true;
            }
            p.teleport(pl);
            return true;
        } else if (label.equals("tphere") && args.length > 0) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage("[Basics] only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.tphere")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            Player pl = Bukkit.getPlayer(args[0]);
            if (pl == null) {
                p.sendMessage(ChatColor.RED + "Unable to find " + args[0]);
                return true;
            }
            pl.teleport(p);
            return true;
        } else if (label.equals("help")) {
            //cs.sendMessage("[1]To find a fight, /hmm queue");
            cs.sendMessage("[1]Protect your home with /hs create shelter");
            cs.sendMessage("[2]To join a town, be patient and ask in chat.");
            cs.sendMessage("[3]Also try /spawn");
            return true;
        } else if (label.equals("rules")) {
            cs.sendMessage("[1]No hacking, modding, or exploiting glitches");
            cs.sendMessage("[2]Dont break blocks that you down own in your HS super-region");
            cs.sendMessage("[3]No repeated swearing or caps or spam");
            cs.sendMessage("[4]Dont kill someone repeatedly, destroy their bed or leave");
            cs.sendMessage("[5]Dont ask staff for favors or bans");
            cs.sendMessage("[6]Dont advertise other servers here");
            cs.sendMessage("[7]No lava/water/gravel/sand/etc dumping into protected areas");
            return true;
        } else if (label.equals("slay")) {
            if (args.length == 0) {
                cs.sendMessage(ChatColor.RED + "Please specify a player to slay");
                return true;
            }
            if (!Basics.perms.has(cs, "basics.slay")) {
                cs.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            Player pl = Bukkit.getPlayer(args[0]);
            if (pl == null) {
                cs.sendMessage(ChatColor.RED + "Unable to find " + args[0]);
                return true;
            }
            pl.damage(2000);
            cs.sendMessage(ChatColor.GRAY + "[Basics] " + pl.getDisplayName() + " has been slain");
            return true;
        } else if (label.equals("suicide")) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage("[Basics] only players can use that command");
                return true;
            }
            p.damage(2000);
            return true;
        } else if (label.equals("weather")) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage("[Basics] only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.weather")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("storm")) {
                p.getWorld().setStorm(true);
                p.sendMessage(ChatColor.GRAY + "[Basics] Weather set to storm");
            } else {
                p.getWorld().setStorm(false);
                p.sendMessage(ChatColor.GRAY + "[Basics] Weather set to sunny");
            }
            return true;
        } else if (label.equals("ci")) {
            if (!Basics.perms.has(cs, "basics.ci")) {
                cs.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            if (args.length > 0) {
                Player p = Bukkit.getPlayer(args[0]);
                if (p == null) {
                    return true;
                }
                p.getInventory().clear();
                cs.sendMessage(ChatColor.GRAY + "[Basics] " + p.getDisplayName() + "'s inventory has been cleared");
                p.sendMessage(ChatColor.GRAY + "[Basics] Your inventory has been cleared");
            } else {
                try {
                    Player p = (Player) cs;
                    p.getInventory().clear();
                    p.sendMessage(ChatColor.GRAY + "[Basics] Your inventory has been cleared");
                } catch (Exception e) {
                    cs.sendMessage(ChatColor.GRAY + "[Basics] Only players can use /ci");
                }
            }
            return true;
        } else if (label.equals("invsee")) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage("[Basics] only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.invsee")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            if (args.length > 0) {
                Player pl = Bukkit.getPlayer(args[0]);
                if (pl == null) {
                    return true;
                }
                p.openInventory(pl.getInventory());
            } else {
                p.closeInventory();
            }
            return true;
        } else if (label.equals("seen") && args.length > 0) {
            try {
                Player p = Bukkit.getPlayer(args[0]);
                long lastPlayed = (System.currentTimeMillis() - p.getLastPlayed()) / 3600000;
                long hours = lastPlayed % 24;
                long days = lastPlayed / 24;
                cs.sendMessage(p.getName() + " was last online " + days + "d " + hours + "h ago");
            } catch (Exception e) {
                try {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
                    long lastPlayed = (System.currentTimeMillis() - op.getLastPlayed()) / 3600000;
                    long hours = lastPlayed % 24;
                    long days = lastPlayed / 24;
                    cs.sendMessage(op.getName() + " was last online " + days + "d " + hours + "h ago");
                } catch (Exception ex) {
                    cs.sendMessage("Can't find user " + args[0]);
                }
            }
            return true;
        } else if (label.equals("broadcast") && args.length > 0) {
            if (!Basics.perms.has(cs, "basics.broadcast")) {
                cs.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            String message = "";
            for (String s : args) {
                message += s + " ";
            }
            Bukkit.broadcastMessage(ChatColor.GREEN + "[Broadcast] " + ChatColor.RED + message);
            return true;
        } else if (label.equals("give") && args.length > 2) {
            if (!Basics.perms.has(cs, "basics.give")) {
                cs.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            try {
                Player p = Bukkit.getPlayer(args[0]);
                String matName = args[1];
                int damageValue = 0;
                if (args[1].split(":").length > 1) {
                    matName = args[1].split(":")[0];
                    cs.sendMessage(matName);
                    damageValue = Integer.parseInt(args[1].split(":")[1]);
                    Material mat = Material.getMaterial(matName);
                    if (mat == null) {
                        cs.sendMessage(ChatColor.RED + "[Basics] Unknown item " + args[1]);
                        return true;
                    }
                    p.getInventory().addItem(new ItemStack(mat,Integer.parseInt(args[2]), (short) damageValue));
                    p.updateInventory();
                    cs.sendMessage(ChatColor.GRAY + "Gave " + p.getDisplayName() + " " + args[2] + " " + mat.name());
                } else {
                    Material mat = Material.getMaterial(matName);
                    if (mat == null) {
                        cs.sendMessage(ChatColor.RED + "[Basics] Unknown item " + args[1]);
                        return true;
                    }
                    p.getInventory().addItem(new ItemStack(mat,Integer.parseInt(args[2])));
                    p.updateInventory();
                    cs.sendMessage(ChatColor.GRAY + "Gave " + p.getDisplayName() + " " + args[2] + " " + mat.name());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (label.equals("i") && args.length > 1) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage("[Basics] Only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.give")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            try {
                String matName = args[0];
                if (args[0].split(":").length > 1) {
                    int damageValue;
                    matName = args[0].split(":")[0];
                    int matID = 0;
                    try {
                        matID = Integer.parseInt(matName);
                    } catch (Exception e) {
                        
                    }
                    matName = matName.toUpperCase();
                    damageValue = Integer.parseInt(args[0].split(":")[1]);
                    Material mat = Material.getMaterial(matName);
                    if (matID < 1) {
                        cs.sendMessage(ChatColor.RED + "[Basics] Unknown item " + args[0]);
                        return true;
                    } else {
                        mat = Material.getMaterial(matID);
                    }
                    if (mat == null) {
                        cs.sendMessage(ChatColor.RED + "[Basics] Unknown item " + args[0]);
                        return true;
                    }
                    p.getInventory().addItem(new ItemStack(mat,Integer.parseInt(args[1]), (short) damageValue));
                    p.updateInventory();
                    p.sendMessage(ChatColor.GRAY + "Giving you " + args[1] + " " + mat.name());
                } else {
                    matName = matName.replace(" ", "_").toUpperCase();
                    Material mat = Material.getMaterial(matName);
                    if (mat == null) {
                        cs.sendMessage(ChatColor.RED + "[Basics] Unknown item " + args[0]);
                        return true;
                    }
                    p.getInventory().addItem(new ItemStack(mat,Integer.parseInt(args[1])));
                    p.updateInventory();
                    p.sendMessage(ChatColor.GRAY + "Giving you " + args[1] + " " + mat.name());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if ((label.equals("msg") || label.equals("tell") || label.equals("m") || label.equals("t")) && args.length > 1) {
            Player p = Bukkit.getPlayer(args[0]);
            if (p == null) {
                cs.sendMessage(ChatColor.RED + "[Basics] Could not find player " + args[0]);
                return true;
            }
            Player player = null;
            try {
                player = (Player) cs;
                if (ignored.get(p).contains(player)) {
                    player.sendMessage(ChatColor.RED + "[Basics] " + p.getDisplayName() + " is ignoring you");
                    return true;
                }
            } catch (Exception e) {
                
            }
            String message = "";
            for (int i = 1; i<args.length; i++) {
                message += " " + args[i];
            }
            p.sendMessage(ChatColor.GRAY + "[From:" + ChatColor.GREEN + cs.getName() + ChatColor.GRAY + "]" + message);
            cs.sendMessage(ChatColor.GRAY + "[To:" + ChatColor.GREEN + p.getDisplayName() + ChatColor.GRAY + "]" + message);
            lastSender.put(p.getName(), cs.getName());
            lastSender.put(cs.getName(), p.getName());
            return true;
        } else if (label.equals("ignore") && args.length > 0) {
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                cs.sendMessage(ChatColor.RED + "[Basics] Could not find player " + args[0]);
                return true;
            }
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage(ChatColor.RED + "[Basics] Only players can use that command");
                return true;
            }
            if (!ignored.containsKey(p)) {
                HashSet<Player> temp = new HashSet<>();
                temp.add(player);
                ignored.put(p, temp);
                cs.sendMessage(ChatColor.GOLD + "[Basics] messages from " + player.getDisplayName() + " are now ignored");
            } else {
                if (ignored.get(p).contains(player)) {
                    ignored.get(p).remove(player);
                    cs.sendMessage(ChatColor.GOLD + "[Basics] messages from " + player.getDisplayName() + " are now not ignored");
                } else {
                    ignored.get(p).add(player);
                    cs.sendMessage(ChatColor.GOLD + "[Basics] messages from " + player.getDisplayName() + " are now ignored");
                }
            }
            return true;
        } else if (label.equals("tpa") && args.length > 0) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage(ChatColor.RED + "[Basics] Only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.tpa")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                p.sendMessage(ChatColor.RED + "[Basics] No player online by the name of " + args[0]);
                return true;
            }
            //Check ignore
            if (ignored.containsKey(player) && ignored.get(player).contains(p)) {
                p.sendMessage(ChatColor.RED + "[Basics] " + player.getName() + " is ignoring you.");
                return true;
            }
            
            p.sendMessage(ChatColor.GRAY + "[Basics] Waiting for " + player.getName() + "'s permission...");
            player.sendMessage(ChatColor.GREEN + "[Basics] " + p.getDisplayName() + " wants to teleport to you. /tpaccept");
            invites.put(player, p);
            destinations.put(p, player);
            return true;
        } else if (label.equals("tpahere") && args.length > 0) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage(ChatColor.RED + "[Basics] Only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.tpa")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                p.sendMessage(ChatColor.RED + "[Basics] No player online by the name of " + args[0]);
                return true;
            }
            if (ignored.containsKey(player) && ignored.get(player).contains(p)) {
                p.sendMessage(ChatColor.RED + "[Basics] " + player.getName() + " is ignoring you.");
                return true;
            }
            
            p.sendMessage(ChatColor.GRAY + "[Basics] Waiting for " + player.getName() + "'s permission...");
            player.sendMessage(ChatColor.GREEN + "[Basics] " + p.getDisplayName() + " wants you to teleport to them. /tpaccept");
            invites.put(player, p);
            destinations.put(player, p);
            return true;
        } else if (label.equals("tpaccept")) {
            Player p = null;
            try {
                p = (Player) cs;
            } catch (Exception e) {
                cs.sendMessage(ChatColor.RED + "[Basics] Only players can use that command");
                return true;
            }
            if (!Basics.perms.has(p, "basics.tpa")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use that command");
                return true;
            }
            if (!invites.containsKey(p)) {
                p.sendMessage(ChatColor.RED + "[Basics] You dont have any pending invites");
                return true;
            }
            if (destinations.containsKey(p)) {
                p.sendMessage(ChatColor.GREEN + "[Basics] You will be teleported in 5s");
                invites.get(p).sendMessage(ChatColor.GREEN + "[Basics] " + p.getDisplayName() + " accepted your tp request");
            } else {
                invites.get(p).sendMessage(ChatColor.GREEN + "[Basics] You will be teleported in 5s");
                p.sendMessage(ChatColor.GREEN + "[Basics] " + invites.get(p).getDisplayName() + " accepted your tp request");
            }
            final Player p1 = p;
            final Player player = invites.get(p);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

                @Override
                public void run() {
                    if (!p1.isOnline() || !player.isOnline()) {
                        return;
                    }
                    if (p1.isDead() || player.isDead()) {
                        p1.sendMessage(ChatColor.RED + "[Basics] Teleport failed. Someone died");
                        player.sendMessage(ChatColor.RED + "[Basics] Teleport failed. Someone died");
                    }
                    if (Basics.hsb != null && (Basics.hsb.isPlayerInCombat(p1) ||
                            Basics.hsb.isPlayerInCombat(player))) {
                        try {
                            p1.sendMessage(ChatColor.RED + "[Basics] Teleport failed. Someone is in combat");
                            player.sendMessage(ChatColor.RED + "[Basics] Teleport failed. Someone is in combat");
                        } catch (Exception e) {
                            
                        }
                    } else {
                        if (destinations.containsKey(p1)) {
                            p1.teleport(player);
                            destinations.remove(p1);
                            invites.remove(p1);
                        } else if (destinations.containsKey(player)) {
                            player.teleport(p1);
                            destinations.remove(player);
                            invites.remove(p1);
                        }
                    }
                }
                
            }, 100L);
            return true;
        
        } else {
            cs.sendMessage(ChatColor.RED + "[Basics] Unknown command " + label);
            return true;
        }
    }
}
