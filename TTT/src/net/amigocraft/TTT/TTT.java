package net.amigocraft.TTT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.amigocraft.TTT.AutoUpdate;
import net.amigocraft.TTT.Metrics;
import net.amigocraft.TTT.localization.Localization;
import static net.amigocraft.TTT.TTTPlayer.*;
import net.amigocraft.TTT.utils.NumUtils;
import net.amigocraft.TTT.utils.WorldUtils;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TTT extends JavaPlugin implements Listener {

	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static Logger log = Logger.getLogger("Minecraft");
	public static TTT plugin = new TTT();
	public static Localization local = new Localization();
	public static String lang;

	public HashMap<String, Integer> time = new HashMap<String, Integer>();
	public HashMap<String, Integer> tasks = new HashMap<String, Integer>();
	public HashMap<String, Integer> gameTime = new HashMap<String, Integer>();
	public List<Body> bodies = new ArrayList<Body>();
	public List<Body> foundBodies = new ArrayList<Body>();
	public List<String> discreet = new ArrayList<String>();

	public int tries = 0;

	@Override
	public void onEnable(){
		// check if server is offline
		if (!getServer().getOnlineMode()){
			if (!getServer().getIp().equals("127.0.0.1") && !getServer().getIp().equals("localhost")){
				log.info("[TTT] This plugin does not support offline servers! Disabling...");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
			else
				log.info("[TTT] Server is probably using BungeeCord. Allowing plugin to load...");
		}

		// register events and the plugin variable
		getServer().getPluginManager().registerEvents(this, this);
		TTT.plugin = this;

		// check if config should be overwritten
		saveDefaultConfig();
		if (!getConfig().getString("config-version").equals(this.getDescription().getVersion())){
			File config = new File(this.getDataFolder(), "config.yml");
			config.delete();
		}

		// create the default config
		saveDefaultConfig();

		TTT.lang = getConfig().getString("localization");

		// autoupdate
		if (getConfig().getBoolean("enable-auto-update")){
			try {new AutoUpdate(this);}
			catch (Exception e){e.printStackTrace();}
		}

		// submit metrics
		if (getConfig().getBoolean("enable-metrics")){
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
			}
			catch (IOException e) {log.warning("[TTT] " + local.getMessage("metrics-fail"));}
		}

		File invDir = new File(this.getDataFolder() + File.separator + "inventories");
		invDir.mkdir();

		log.info(this + " " + local.getMessage("enabled"));
	}

	@Override
	public void onDisable(){
		log.info(this + " " + local.getMessage("disabled"));
	}

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if (commandLabel.equalsIgnoreCase("ttt")){
			if (args.length > 0){
				if (args[0].equalsIgnoreCase("import")){
					if (sender.hasPermission("ttt.import")){
						if (args.length > 1){
							File folder = new File(args[1]);
							if (folder.exists()){
								if (!args[1].substring(0, 3).equalsIgnoreCase("TTT_")){
									if (WorldUtils.isWorld(folder)){
										File newFolder = new File("TTT_" + args[1]);
										if (!newFolder.exists()){
											try {
												File sessionLock = new File(folder + File.separator + "session.lock");
												File uidDat = new File(folder + File.separator + "uid.dat");
												sessionLock.delete();
												uidDat.delete();
												FileUtils.copyDirectory(folder, newFolder);
												sender.sendMessage(ChatColor.GREEN + "[TTT] " + local.getMessage("import-success"));
											}
											catch (IOException e){
												sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("folder-error"));
												e.printStackTrace();
											}
										}
										else
											sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("already-imported"));
									}
									else
										sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("cannot-load-world"));
								}
								else
									sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("start-error"));
							}
							else
								sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("folder-not-found"));
						}
						else {
							sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("invalid-args-1"));
							sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("usage-import"));
						}						
					}
					else
						sender.sendMessage(ChatColor.RED + local.getMessage("no-permission-import"));
				}
				else if (args[0].equalsIgnoreCase("join")){
					if (sender instanceof Player){
						if (sender.hasPermission("ttt.join")){
							if (args.length > 1){
								if (gameTime.get(args[1]) == null){
									File folder = new File(args[1]);
									File tttFolder = new File("TTT_" + args[1]);
									if (folder.exists() && tttFolder.exists()){
										boolean loaded = false;
										for (World w : Bukkit.getServer().getWorlds()){
											if(w.getName().equals("TTT_" + args[1])){
												loaded = true;
												break;
											}
										}
										final String worldName = args[1];
										if (!loaded){
											getServer().createWorld(new WorldCreator("TTT_" + worldName));
										}
										((Player)sender).teleport(getServer().getWorld("TTT_" + worldName).getSpawnLocation());
										new TTTPlayer(((Player)sender).getName(), worldName);
										File invF = new File(getDataFolder() + File.separator + "inventories" + File.separator + sender.getName() + ".inv");
										Inventory inv = ((Player)sender).getInventory();
										PlayerInventory pInv = (PlayerInventory)inv;
										try {
											if (!invF.exists())
												invF.createNewFile();
											YamlConfiguration invY = new YamlConfiguration();
											invY.load(invF);
											for (int i = 0; i < inv.getContents().length; i++)
												invY.set(Integer.toString(i), inv.getContents()[i]);
											if (pInv.getHelmet() != null)
												invY.set("h", pInv.getHelmet());
											if (pInv.getChestplate() != null)
												invY.set("c", pInv.getChestplate());
											if (pInv.getLeggings() != null)
												invY.set("l", pInv.getLeggings());
											if (pInv.getBoots() != null)
												invY.set("b", pInv.getBoots());
											invY.save(invF);
										}
										catch (Exception ex){
											ex.printStackTrace();
											sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("inv-save-error"));
										}
										inv.clear();
										pInv.setArmorContents(new ItemStack[]{null, null, null, null});
										sender.sendMessage(ChatColor.GREEN + local.getMessage("success-join") + " " + worldName);
										List<String> testers = new ArrayList<String>();
										testers.add("ZerosAce00000");
										testers.add("momhipie");
										testers.add("xJHA929x");
										testers.add("jmm1999");
										testers.add("jon674");
										testers.add("HardcoreBukkit");
										testers.add("shiny3");
										testers.add("jpf6368");
										String addition = "";
										if (sender.getName().equals("AngryNerd1"))
											addition = ", " + ChatColor.DARK_RED + local.getMessage("creator") + ", " + ChatColor.DARK_PURPLE;
										else if (testers.contains(sender.getName())){
											addition = ", " + ChatColor.DARK_RED + local.getMessage("tester") + ", " + ChatColor.DARK_PURPLE;
										}
										Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[TTT] " + sender.getName() + addition + " " + local.getMessage("joined-map") + " \"" + worldName + "\"");
										int ingamePlayers = 0;
										for (TTTPlayer p : players)
											if (p.getGame().equals(worldName))
												ingamePlayers += 1;
										if (ingamePlayers >= getConfig().getInt("minimum-players") && !time.containsKey(worldName)){
											for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers())
												p.sendMessage(ChatColor.DARK_PURPLE + local.getMessage("round-starting"));
											time.put(worldName, getConfig().getInt("setup-time"));
											tasks.put(worldName, setupTimer(worldName));
										}
										else {
											((Player)sender).sendMessage(ChatColor.DARK_PURPLE + local.getMessage("waiting"));
										}
									}
									else
										sender.sendMessage(ChatColor.RED + local.getMessage("map-invalid"));
									folder = null;
									tttFolder = null;
								}
								else
									sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("in-progress"));
							}
							else {
								sender.sendMessage(ChatColor.RED + local.getMessage("invalid-args-1"));
								sender.sendMessage(ChatColor.RED + local.getMessage("usage-join"));
							}
						}
						else
							sender.sendMessage(ChatColor.RED + local.getMessage("no-permission-join"));
					}
					else
						sender.sendMessage(ChatColor.RED + local.getMessage("must-be-ingame"));
				}
				else if (args[0].equalsIgnoreCase("quit")){
					if (sender instanceof Player){
						if (sender.hasPermission("ttt.quit")){
							if (isPlayer(sender.getName())){
								WorldUtils.teleportPlayer((Player)sender);
								TTTPlayer tPlayer = getTTTPlayer(sender.getName());
								getTTTPlayer(sender.getName()).destroy();
								if (getServer().getWorld("TTT_" + tPlayer.getGame()) != null)
									for (Player pl : getServer().getWorld("TTT_" + tPlayer.getGame()).getPlayers())
										pl.sendMessage(ChatColor.DARK_PURPLE + "[TTT] " + ((Player)sender).getName() + local.getMessage("left-game").replace("%", tPlayer.getGame()));
								Player p = (Player)sender;
								p.getInventory().clear();
								File invF = new File(getDataFolder() + File.separator + "inventories" + File.separator + p.getName() + ".inv");
								if (invF.exists()){
									try {
										YamlConfiguration invY = new YamlConfiguration();
										invY.load(invF);
										ItemStack[] invI = new ItemStack[p.getInventory().getSize()];
										for (String k : invY.getKeys(false)){
											if (NumUtils.isInt(k))
												invI[Integer.parseInt(k)] = invY.getItemStack(k);
											else if (k.equalsIgnoreCase("h"))
												p.getInventory().setHelmet(invY.getItemStack(k));
											else if (k.equalsIgnoreCase("c"))
												p.getInventory().setChestplate(invY.getItemStack(k));
											else if (k.equalsIgnoreCase("l"))
												p.getInventory().setLeggings(invY.getItemStack(k));
											else if (k.equalsIgnoreCase("b"))
												p.getInventory().setBoots(invY.getItemStack(k));
										}
										p.getInventory().setContents(invI);
										p.updateInventory();
										invF.delete();
									}
									catch (Exception ex){
										ex.printStackTrace();
										p.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("inv-load-error"));
									}
								}
							}
							else
								sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("not-in-game"));
						}
						else
							sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("no-permission-quit"));
					}
					else
						sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("must-be-ingame"));
				}
				else if (args[0].equalsIgnoreCase("setspawn")){
					if (sender.hasPermission("ttt.setspawn")){
						if (sender instanceof Player){
							try {
								File spawnFile = new File(getDataFolder() + File.separator + "spawn.yml");
								if (!spawnFile.exists()){
									log.info("No spawn.yml found, creating...");
									spawnFile.createNewFile();
								}
								YamlConfiguration spawnYaml = new YamlConfiguration();
								spawnYaml.load(spawnFile);
								spawnYaml.set("world", ((Player)sender).getLocation().getWorld().getName());
								spawnYaml.set("x", ((Player)sender).getLocation().getX());
								spawnYaml.set("y", ((Player)sender).getLocation().getY());
								spawnYaml.set("z", ((Player)sender).getLocation().getZ());
								spawnYaml.set("pitch", ((Player)sender).getLocation().getPitch());
								spawnYaml.set("yaw", ((Player)sender).getLocation().getYaw());
								spawnYaml.save(spawnFile);
							}
							catch (Exception ex){
								ex.printStackTrace();
							}
						}
						else
							sender.sendMessage(ChatColor.RED + local.getMessage("must-be-ingame"));
					}
					else
						sender.sendMessage(ChatColor.RED + local.getMessage("no-permission"));

				}
				else {
					sender.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("invalid-args-2"));
					sender.sendMessage(ChatColor.RED + local.getMessage("usage-1"));
				}
			}
			else {
				sender.sendMessage(ChatColor.RED + local.getMessage("invalid-args-1"));
				sender.sendMessage(ChatColor.RED + local.getMessage("usage-1"));
			}
			return true;
		}
		return false;
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e){
		if (e.getMessage().startsWith("kit")){
			if (isPlayer(e.getPlayer().getName())){
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("no-kits"));
			}
		}
		else if (e.getMessage().startsWith("msg") || e.getMessage().startsWith("tell") || e.getMessage().startsWith("r") || e.getMessage().startsWith("msg") || e.getMessage().startsWith("me")){
			String p = e.getPlayer().getName();
			if (isPlayer(p)){
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("no-pm"));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e){
		if (e.getEntityType() == EntityType.PLAYER){
			Player p = (Player)e.getEntity();
			int armor = 0;
			if (e.getCause() == DamageCause.ENTITY_ATTACK ||
					e.getCause() == DamageCause.PROJECTILE ||
					e.getCause() == DamageCause.FIRE ||
					e.getCause() == DamageCause.FIRE_TICK ||
					e.getCause() == DamageCause.BLOCK_EXPLOSION || 
					e.getCause() == DamageCause.CONTACT ||
					e.getCause() == DamageCause.LAVA ||
					e.getCause() == DamageCause.ENTITY_EXPLOSION){
				HashMap<Material, Integer> protection = new HashMap<Material, Integer>();
				protection.put(Material.LEATHER_HELMET, 1);
				protection.put(Material.LEATHER_CHESTPLATE, 3);
				protection.put(Material.LEATHER_LEGGINGS, 2);
				protection.put(Material.LEATHER_BOOTS, 1);
				protection.put(Material.IRON_HELMET, 2);
				protection.put(Material.IRON_CHESTPLATE, 5);
				protection.put(Material.IRON_LEGGINGS, 3);
				protection.put(Material.IRON_BOOTS, 1);
				protection.put(Material.CHAINMAIL_HELMET, 2);
				protection.put(Material.CHAINMAIL_CHESTPLATE, 5);
				protection.put(Material.CHAINMAIL_LEGGINGS, 3);
				protection.put(Material.CHAINMAIL_BOOTS, 1);
				protection.put(Material.GOLD_HELMET, 2);
				protection.put(Material.GOLD_CHESTPLATE, 6);
				protection.put(Material.GOLD_LEGGINGS, 5);
				protection.put(Material.GOLD_BOOTS, 2);
				protection.put(Material.DIAMOND_HELMET, 3);
				protection.put(Material.DIAMOND_CHESTPLATE, 8);
				protection.put(Material.DIAMOND_LEGGINGS, 6);
				protection.put(Material.DIAMOND_BOOTS, 3);
				if (p.getInventory().getArmorContents()[0] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[0].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[0].getType());
				if (p.getInventory().getArmorContents()[1] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[1].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[1].getType());
				if (p.getInventory().getArmorContents()[2] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[2].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[2].getType());
				if (p.getInventory().getArmorContents()[3] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[3].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[3].getType());
			}
			if (e.getDamage() - ((armor * .04) * e.getDamage()) >= ((Player)e.getEntity()).getHealth()){
				if (isPlayer(p.getName())){
					if (getTTTPlayer(p.getName()).getRole() != null){
						e.setCancelled(true);
						p.setHealth(20);
						p.sendMessage(ChatColor.DARK_PURPLE + local.getMessage("dead"));
						getTTTPlayer(p.getName()).setDead(true);
						Block block = p.getLocation().getBlock();
						block.setType(Material.CHEST);
						Chest chest = (Chest)block.getState();
						// player identifier
						ItemStack id = new ItemStack(Material.PAPER, 1);
						ItemMeta idMeta = id.getItemMeta();
						idMeta.setDisplayName(local.getMessage("id"));
						List<String> idLore = new ArrayList<String>();
						idLore.add(local.getMessage("body-of"));
						idLore.add(((Player)e.getEntity()).getName());
						idMeta.setLore(idLore);
						id.setItemMeta(idMeta);
						// role identifier
						ItemStack ti = new ItemStack(Material.WOOL, 1);
						ItemMeta tiMeta = ti.getItemMeta();
						if (getTTTPlayer(p.getName()).getRole() == Role.INNOCENT){
							ti.setDurability((short)5);
							tiMeta.setDisplayName("�2" + local.getMessage("innocent"));
							List<String> tiLore = new ArrayList<String>();
							tiLore.add(local.getMessage("innocent-id"));
							tiMeta.setLore(tiLore);
						}
						else if (getTTTPlayer(p.getName()).getRole() == Role.TRAITOR){
							ti.setDurability((short)14);
							tiMeta.setDisplayName("�4" + local.getMessage("traitor"));
							List<String> lore = new ArrayList<String>();
							lore.add(local.getMessage("traitor-id"));
							tiMeta.setLore(lore);
						}
						else if (getTTTPlayer(p.getName()).getRole() == Role.DETECTIVE){
							ti.setDurability((short)11);
							tiMeta.setDisplayName("�1" + local.getMessage("detective"));
							List<String> lore = new ArrayList<String>();
							lore.add(local.getMessage("detective-id"));
							tiMeta.setLore(lore);
						}
						ti.setItemMeta(tiMeta);
						chest.getInventory().addItem(new ItemStack[]{id, ti});
						bodies.add(new Body(p.getName(), getTTTPlayer(p.getName()).getRole(), FixedLocation.getFixedLocation(block), System.currentTimeMillis()));
					}
					else
						p.setHealth(20);
				}
			}
			if (getTTTPlayer(p.getName()).isDead()){
				e.setCancelled(true);
			}
			if (e instanceof EntityDamageByEntityEvent){
				EntityDamageByEntityEvent ed = (EntityDamageByEntityEvent)e;
				if (ed.getDamager().getType() == EntityType.PLAYER){
					if (((Player)ed.getDamager()).getItemInHand() != null)
						if (((Player)ed.getDamager()).getItemInHand().getItemMeta() != null)
							if (((Player)ed.getDamager()).getItemInHand().getItemMeta().getDisplayName() != null)
								if (((Player)ed.getDamager()).getItemInHand().getItemMeta().getDisplayName().equals("�5" + local.getMessage("crowbar")))
									e.setDamage(getConfig().getInt("crowbar-damage"));
					if (getTTTPlayer(((Player)ed.getDamager()).getName()).isDead()){
						e.setCancelled(true);
					}

					if (isPlayer(((Player)ed.getDamager()).getName())){
						if (gameTime.get(((Player)ed.getDamager()).getWorld().getName()) == null)
							e.setCancelled(true);
					}
				}
			}
		}
	}

	public int setupTimer(final String worldName){
		return getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run(){
				// verify that all players are still online
				List<TTTPlayer> offlinePlayers = new ArrayList<TTTPlayer>();
				for (TTTPlayer tp : players){
					if (tp.getGame().equals(worldName)){
						Player p = getServer().getPlayer(tp.getName());
						if (p != null){
							if (!getServer().getWorld("TTT_" + worldName).getPlayers().contains(p)){
								offlinePlayers.add(tp);
								Bukkit.broadcastMessage("[TTT]" + tp.getName() + " " + local.getMessage("left-map") + " \"" + worldName + "\"");
							}
						}
					}
				}
				for (TTTPlayer p : offlinePlayers){
					p.destroy();
				}
				int currentTime = time.get(worldName);
				int playerCount = 0; 
				for (TTTPlayer tp : players){
					if (tp.getGame().equals(worldName))
						playerCount += 1;
				}
				if (playerCount >= getConfig().getInt("minimum-players")){
					if((currentTime % 10) == 0 && currentTime > 0){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + local.getMessage("begin") + " " + currentTime + " " + local.getMessage("seconds") + "!");
						}
					}
					else if (currentTime > 0 && currentTime < 10){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + local.getMessage("begin") + " " + currentTime + " " + local.getMessage("seconds") + "!");
						}
					}
					else if (currentTime <= 0){
						int players = getServer().getWorld("TTT_" + worldName).getPlayers().size();
						int traitorNum = 0;
						int limit = (int)(players * getConfig().getDouble("traitor-ratio"));
						if (limit == 0)
							limit = 1;
						List<String> innocents = new ArrayList<String>();
						List<String> traitors = new ArrayList<String>();
						List<String> detectives = new ArrayList<String>();
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							innocents.add(p.getName());
							p.sendMessage(ChatColor.DARK_PURPLE + local.getMessage("begun"));
						}
						while (traitorNum < limit){
							Random randomGenerator = new Random();
							int index = randomGenerator.nextInt(players);
							String traitor = innocents.get(index);
							if (innocents.contains(traitor)){
								innocents.remove(traitor);
								traitors.add(traitor);
								traitorNum += 1;
							}
						}
						int dLimit = (int)(players * getConfig().getDouble("detective-ratio"));
						if (players >= getConfig().getInt("minimum-players-for-detective") && dLimit == 0)
							dLimit += 1;
						int detectiveNum = 0;
						while (detectiveNum < dLimit){
							Random randomGenerator = new Random();
							int index = randomGenerator.nextInt(innocents.size());
							String detective = innocents.get(index);
							innocents.remove(detective);
							detectives.add(detective);
							detectiveNum += 1;
						}
						ItemStack crowbar = new ItemStack(Material.IRON_SWORD, 1);
						ItemMeta cbMeta = crowbar.getItemMeta();
						cbMeta.setDisplayName("�5" + local.getMessage("crowbar"));
						crowbar.setItemMeta(cbMeta);
						ItemStack gun = new ItemStack(Material.ANVIL, 1);
						ItemMeta gunMeta = crowbar.getItemMeta();
						gunMeta.setDisplayName("�5" + local.getMessage("gun"));
						gun.setItemMeta(gunMeta);
						ItemStack ammo = new ItemStack(Material.ARROW, 28);
						ItemStack dnaScanner = new ItemStack(Material.COMPASS, 1);
						ItemMeta dnaMeta = dnaScanner.getItemMeta();
						dnaMeta.setDisplayName("�1" + local.getMessage("dna-scanner"));
						dnaScanner.setItemMeta(dnaMeta);
						for (TTTPlayer tp : TTTPlayer.players){
							Player pl = getServer().getPlayer(tp.getName());
							if (innocents.contains(tp.getName())){
								tp.setRole(Role.INNOCENT);
								pl.sendMessage(ChatColor.DARK_GREEN + local.getMessage("you-are-innocent"));
								pl.getInventory().addItem(new ItemStack[]{crowbar, gun, ammo});
							}
							else if (traitors.contains(tp.getName())){
								tp.setRole(Role.TRAITOR);
								pl.sendMessage(ChatColor.DARK_RED + local.getMessage("you-are-traitor"));
								if (traitors.size() > 1){
									pl.sendMessage(ChatColor.DARK_RED + local.getMessage("allies"));
									for (String t : traitors)
										pl.sendMessage("- " + t);
								}
								else
									pl.sendMessage(ChatColor.DARK_RED + local.getMessage("alone"));
								pl.getInventory().addItem(new ItemStack[]{crowbar, gun, ammo});
							}
							else if (detectives.contains(tp.getName())){
								tp.setRole(Role.DETECTIVE);
								pl.sendMessage(ChatColor.BLUE + local.getMessage("you-are-detective"));
								pl.getInventory().addItem(new ItemStack[]{crowbar, gun, ammo, dnaScanner});
							}
							pl.setHealth(20);
							pl.setFoodLevel(20);
						}
						time.remove(worldName);
						gameTime.put(worldName, getConfig().getInt("time-limit"));
						Bukkit.getScheduler().cancelTask(tasks.get(worldName));
						tasks.remove(worldName);
						gameTimer(worldName);
					}
					if (currentTime > 0)
						time.put(worldName, currentTime - 1);
				}
				else {
					time.remove(worldName);
					Bukkit.getScheduler().cancelTask(tasks.get(worldName));
					for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
						p.sendMessage(ChatColor.DARK_PURPLE + local.getMessage("waiting"));
					}
				}
			}
		}, 0L, 20L);
	}

	public void gameTimer(final String worldName){
		tasks.put(worldName, getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				// verify that all players are still online
				List<TTTPlayer> offlinePlayers = new ArrayList<TTTPlayer>();
				for (TTTPlayer tp : players){
					if (tp.getGame().equals(worldName)){
						Player p = getServer().getPlayer(tp.getName());
						if (p != null){
							if (!getServer().getWorld("TTT_" + worldName).getPlayers().contains(p)){
								Bukkit.broadcastMessage("[TTT]" + tp.getName() + " " + local.getMessage("left-map") + " \"" + worldName + "\"");
								offlinePlayers.add(tp);
							}
						}
					}
				}
				for (TTTPlayer tp : offlinePlayers){
					tp.destroy();
				}

				// set compass targets
				for (TTTPlayer p : players){
					if (p.getKiller() != null){
						Player tracker = getServer().getPlayer(p.getName());
						Player killer = getServer().getPlayer(p.getKiller());
						if (tracker != null || killer != null)
							if (!offlinePlayers.contains(tracker) && !offlinePlayers.contains(killer))
								tracker.setCompassTarget(killer.getLocation());
					}
				}

				// check if game is over
				boolean iLeft = false;
				boolean tLeft = false;
				for (TTTPlayer tp : players){
					if (tp.getGame().equals(worldName)){
						if (tp.getRole() == Role.INNOCENT){
							iLeft = true;
						}
						if (tp.getRole() == Role.TRAITOR){
							tLeft = true;
						}
					}
				}
				if (!(tLeft && iLeft)){
					List<Body> removeBodies = new ArrayList<Body>();
					List<Body> removeFoundBodies = new ArrayList<Body>(); 
					for (Body b : bodies){
						if (getTTTPlayer(b.getName()).isDead()){
							if (getTTTPlayer(b.getName()).getGame().equals(worldName)){
								removeBodies.add(b);
								if (foundBodies.contains(b))
									removeFoundBodies.add(b);
							}
						}
					}

					for (Body b : removeBodies)
						bodies.remove(b);

					for (Body b : removeFoundBodies)
						foundBodies.remove(b);

					removeBodies.clear();
					removeFoundBodies.clear();

					if (!tLeft)
						Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "[TTT] " + local.getMessage("innocent-win").replace("%", "\"" + worldName + "\"") + "!");
					if (!iLeft)
						Bukkit.broadcastMessage(ChatColor.DARK_RED + "[TTT] " + local.getMessage("traitor-win").replace("%", "\"" + worldName + "\"") + "!");
					for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
						if (isPlayer(p.getName())){
							TTTPlayer tp = getTTTPlayer(p.getName());
							if (tp != null){
								if (tp.isDead()){
									p.setAllowFlight(false);
									for (Player pl : getServer().getOnlinePlayers()){
										pl.showPlayer(p);
									}
								}
								tp.destroy();
								p.getInventory().clear();
								File invF = new File(getDataFolder() + File.separator + "inventories" + File.separator + p.getName() + ".inv");
								if (invF.exists()){
									try {
										YamlConfiguration invY = new YamlConfiguration();
										invY.load(invF);
										ItemStack[] invI = new ItemStack[p.getInventory().getSize()];
										for (String k : invY.getKeys(false)){
											invI[Integer.parseInt(k)] = invY.getItemStack(k);
										}
										p.getInventory().setContents(invI);
										p.updateInventory();
										invF.delete();
									}
									catch (Exception ex){
										ex.printStackTrace();
										p.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("inv-load-error"));
									}
								}
							}
						}
						WorldUtils.teleportPlayer(p);
					}
					gameTime.remove(worldName);
					getServer().getScheduler().cancelTask(tasks.get(worldName));
					tasks.remove(tasks.get(worldName));
					getServer().unloadWorld("TTT_" + worldName, false);
					rollbackWorld(worldName);
				}
				else {
					int newTime = gameTime.get(worldName) - 1;
					gameTime.remove(worldName);
					gameTime.put(worldName, newTime);
					if (newTime % 60 == 0 && newTime >= 60){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(newTime / 60) + " " + local.getMessage("minutes") + " " + local.getMessage("left"));
						}
					}
					else if (newTime % 10 == 0 && newTime > 10 && newTime < 60){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(newTime) + " " + local.getMessage("seconds") + " " + local.getMessage("left"));
						}
					}
					else if (newTime < 10 && newTime > 0){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(newTime) + " " + local.getMessage("seconds") + " " + local.getMessage("left"));
						}
					}
					else if (newTime <= 0){
						List<Body> removeBodies = new ArrayList<Body>();
						List<Body> removeFoundBodies = new ArrayList<Body>(); 
						for (Body b : bodies){
							if (getTTTPlayer(b.getName()).isDead()){
								if (getTTTPlayer(b.getName()).getGame().equals(worldName)){
									removeBodies.add(b);
									if (foundBodies.contains(b))
										removeFoundBodies.add(b);
								}
							}
						}

						for (Body b : removeBodies)
							bodies.remove(b);

						for (Body b : removeFoundBodies)
							foundBodies.remove(b);

						removeBodies.clear();
						removeFoundBodies.clear();

						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_GREEN + "[TTT] " + local.getMessage("innocent-win").replace("%", "\"" + worldName + "\"") + "!");
							if (getTTTPlayer(p.getName()).isDead()){
								p.setAllowFlight(false);
								for (Player pl : getServer().getOnlinePlayers()){
									pl.showPlayer(p);
								}
							}
							getTTTPlayer(p.getName()).destroy();
							p.getInventory().clear();
							File invF = new File(getDataFolder() + File.separator + "inventories" + File.separator + p.getName() + ".inv");
							if (invF.exists()){
								try {
									YamlConfiguration invY = new YamlConfiguration();
									invY.load(invF);
									ItemStack[] invI = new ItemStack[p.getInventory().getSize()];
									for (String k : invY.getKeys(false)){
										if (NumUtils.isInt(k))
											invI[Integer.parseInt(k)] = invY.getItemStack(k);
									}
									p.getInventory().setContents(invI);
									if (invY.getItemStack("h") != null)
										p.getInventory().setHelmet(invY.getItemStack("h"));
									if (invY.getItemStack("c") != null)
										p.getInventory().setChestplate(invY.getItemStack("c"));
									if (invY.getItemStack("l") != null)
										p.getInventory().setLeggings(invY.getItemStack("l"));
									if (invY.getItemStack("b") != null)
										p.getInventory().setBoots(invY.getItemStack("b"));
									p.updateInventory();
									invF.delete();
								}
								catch (Exception ex){
									ex.printStackTrace();
									p.sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("inv-load-fail"));
								}
							}
							gameTime.remove(worldName);
							WorldUtils.teleportPlayer(p);
						}
						Bukkit.getScheduler().cancelTask(tasks.get(worldName));
						getServer().unloadWorld("TTT_" + worldName, false);
						rollbackWorld(worldName);
					}
				}
				// hide dead players
				for (TTTPlayer p : players){
					if (p.isDead()){
						if (getServer().getPlayer(p.getName()) != null){
							if (getServer().getWorld("TTT_" + worldName).getPlayers().contains(getServer().getPlayer(p.getName()))){
								getServer().getPlayer(p.getName()).setAllowFlight(true);
								for (TTTPlayer other : players){
									if (other.getGame().equals(worldName))
										getServer().getPlayer(other.getName()).hidePlayer(getServer().getPlayer(p.getName()));
								}
							}
						}
					}
				}
			}
		}, 0L, 20L));
	}

	public void rollbackWorld(final String worldName){
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			public void run(){
				File folder = new File(worldName);
				if (folder.exists()){
					if (WorldUtils.isWorld(folder)){
						File newFolder = new File("TTT_" + worldName);
						try {
							FileUtils.copyDirectory(folder, newFolder);
							log.info("[TTT] " + local.getMessage("rollback") + " \"" + worldName + "\"!");
						}
						catch (IOException ex){
							log.info("[TTT] " + local.getMessage("folder-error") + " " + worldName);
							ex.printStackTrace();
						}
					}
					else
						log.info("[TTT] " + local.getMessage("cannot-load-world"));
				}
			}
		}, 100L);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent e){
		for (HumanEntity he : e.getViewers()){
			Player p = (Player)he;
			if (isPlayer(p.getName())){
				if (getTTTPlayer(p.getName()).isDead()){
					e.setCancelled(true);
				}
				else if (e.getInventory().getType() == InventoryType.CHEST){
					Block block = ((Chest)e.getInventory().getHolder()).getBlock();
					for (Body b : bodies){
						if (b.getLocation().equals(FixedLocation.getFixedLocation(block))){
							e.setCancelled(true);
							break;
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler (priority = EventPriority.HIGH)
	public void onPlayerChat(AsyncPlayerChatEvent e){
		for (Player p : getServer().getOnlinePlayers()){
			// check if sender is in TTT game
			if (getTTTPlayer(e.getPlayer().getName()) != null){
				if (!p.getWorld().getName().equals(e.getPlayer().getWorld().getName()))
					e.getRecipients().remove(p);
			}

			// check if sender is dead
			else if (getTTTPlayer(p.getName()).isDead()){
				if (getTTTPlayer(p.getName()).isDead()){
					if (!p.getWorld().getName().equals("TTT_" + getTTTPlayer(p.getName()).getGame()))
						e.getRecipients().remove(p);
				}
				else
					e.getRecipients().remove(p);
			}
		}

		if (getTTTPlayer(e.getPlayer().getName()) != null){
			TTTPlayer tPlayer = getTTTPlayer(e.getPlayer().getName());
			if (tPlayer.getRole() != null){
				if (tPlayer.getRole() == Role.DETECTIVE){
					final Player player = e.getPlayer();
					e.getPlayer().setDisplayName(ChatColor.BLUE + "[Detective] " + e.getPlayer().getDisplayName());
					getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable(){
						public void run(){
							String name = player.getDisplayName();
							name = name.replace(ChatColor.BLUE + "[Detective] ", "");
							player.setDisplayName(name);
						}
					}, 1L);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent e){
		TTTPlayer tPlayer = getTTTPlayer(e.getPlayer().getName());
		if (tPlayer != null){
			if (!tPlayer.isDead()){
				if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
					if (e.getClickedBlock().getType() == Material.CHEST){
						int index = -1;
						for (int i = 0; i < bodies.size(); i++){
							if (bodies.get(i).getLocation().equals(FixedLocation.getFixedLocation(e.getClickedBlock()))){
								index = i;
								break;
							}
						}
						if (index != -1){
							boolean found = false;
							for (Body b : foundBodies){
								if (b.getLocation().equals(FixedLocation.getFixedLocation(e.getClickedBlock()))){
									found = true;
									break;
								}
							}
							if (!found){
								for (Player p : e.getPlayer().getWorld().getPlayers()){
									if (bodies.get(index).getRole() == Role.INNOCENT)
										p.sendMessage(ChatColor.DARK_GREEN + e.getPlayer().getName() + " " + local.getMessage("found-body").replace("%", bodies.get(index).getName())  + ". " + local.getMessage("was-innocent"));
									else if (bodies.get(index).getRole() == Role.TRAITOR)
										p.sendMessage(ChatColor.DARK_RED + e.getPlayer().getName() + " " + local.getMessage("found-body").replace("%", bodies.get(index).getName())  + ". " + local.getMessage("was-traitor"));
									else if (bodies.get(index).getRole() == Role.DETECTIVE)
										p.sendMessage(ChatColor.DARK_BLUE + e.getPlayer().getName() + " " + local.getMessage("found-body").replace("%", bodies.get(index).getName())  + ". " + local.getMessage("was-detective"));
								}
								foundBodies.add(bodies.get(index));
							}
							if (tPlayer.getRole() == Role.DETECTIVE){
								if (e.getPlayer().getItemInHand() != null){
									if (e.getPlayer().getItemInHand().getType() == Material.COMPASS){
										if (e.getPlayer().getItemInHand().getItemMeta() != null){
											if (e.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null){
												if (e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals("�1" + local.getMessage("dna-scanner"))){
													e.setCancelled(true);
													Player killer = getServer().getPlayer(getTTTPlayer(bodies.get(index).getName()).getKiller());
													if (killer != null){
														if (isPlayer(killer.getName())){
															tPlayer.setTracking(killer.getName());
															e.getPlayer().sendMessage(ChatColor.BLUE + local.getMessage("collected-dna").replace("%", bodies.get(index).getName()));
														}
														else
															e.getPlayer().sendMessage(ChatColor.BLUE + local.getMessage("killer-left"));
													}
													else
														e.getPlayer().sendMessage(ChatColor.BLUE + local.getMessage("killer-left"));
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR){
					if (e.getPlayer().getItemInHand() != null){
						if (e.getPlayer().getItemInHand().getItemMeta() != null){
							if (e.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null){
								if (e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals("�5" + local.getMessage("gun"))){
									if ((getConfig().getBoolean("guns-outside-arenas"))){
										if (tPlayer.isDead()){
											e.setCancelled(true);
											if (e.getPlayer().getInventory().contains(Material.ARROW) || !getConfig().getBoolean("require-ammo-for-guns")){
												if (getConfig().getBoolean("require-ammo-for-guns")){
													removeArrow(e.getPlayer().getInventory());
													e.getPlayer().updateInventory();
												}
												e.getPlayer().launchProjectile(Arrow.class);
											}
											else
												e.getPlayer().sendMessage(ChatColor.RED + local.getMessage("need-ammo"));
										}
									}
								}
							}
						}
					}
				}
				else {
					e.setCancelled(true);
					if (tPlayer.isDead()){
						if (e.getClickedBlock() != null){
							for (Body b : bodies){
								if (b.getLocation().equals(FixedLocation.getFixedLocation(e.getClickedBlock()))){
									if (e.getClickedBlock().getType() == Material.CHEST){
										Inventory chestinv = ((Chest)e.getClickedBlock().getState()).getInventory();
										Inventory inv = getServer().createInventory(null, chestinv.getSize());
										inv.setContents(chestinv.getContents());
										e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + local.getMessage("discreet"));
										discreet.add(e.getPlayer().getName());
										e.getPlayer().openInventory(inv);
									}
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e){
		if (isPlayer(e.getPlayer().getName()))
			e.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e){
		if (isPlayer(e.getPlayer().getName()))
			e.setCancelled(true);
	}

	public void removeArrow(Inventory inv){
		for (int i = 0; i < inv.getContents().length; i++){
			ItemStack is = inv.getItem(i);
			if (is != null){
				if (is.getType() == Material.ARROW){
					if (is.getAmount() == 1)
						inv.setItem(i, null);
					else if (is.getAmount() > 1)
						is.setAmount(is.getAmount() - 1);
					break;
				}
			}
		}
	}

	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent e){
		if (getTTTPlayer(e.getPlayer().getName()) != null)
			if (getTTTPlayer(e.getPlayer().getName()).isDead())
				e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e){
		if (isPlayer(e.getPlayer().getName())){
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + "[TTT] " + local.getMessage("no-drop"));
		}
	}

	@EventHandler
	public void onFoodDeplete(FoodLevelChangeEvent e){
		if (e.getEntity().getType() == EntityType.PLAYER){
			Player p = (Player)e.getEntity();
			if (isPlayer(p.getName()))
				e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e){
		if (discreet.contains(e.getPlayer().getName()))
			discreet.remove(e.getPlayer().getName());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e){
		String p = e.getPlayer().getName();
		if (getTTTPlayer(p).getRole() != null){
			String worldName = "";
			if (isPlayer(p)){
				worldName = getTTTPlayer(p).getGame();
				destroy(p);
			}
			for (Player pl : getServer().getWorld("TTT_" + worldName).getPlayers())
				pl.sendMessage(ChatColor.DARK_PURPLE + "[TTT] " + p + local.getMessage("left-game").replace("%", worldName));
			for (Player pl : getServer().getWorld("TTT_" + worldName).getPlayers())
				pl.sendMessage(ChatColor.DARK_PURPLE + "[TTT] " + p + local.getMessage("left-game").replace("%", worldName));
		}
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent e){
		String p = e.getPlayer().getName();
		if (getTTTPlayer(p) != null){
			if (e.getFrom().getWorld().getName().replace("TTT_", "") != getTTTPlayer(p).getGame()){
				for (Player pl : getServer().getWorld("TTT_" + getTTTPlayer(p).getGame()).getPlayers())
					pl.sendMessage(ChatColor.DARK_PURPLE + "[TTT] " + p + local.getMessage("left-game").replace("%", getTTTPlayer(p).getGame()));
				destroy(p);
			}
		}
	}

	public void onHealthRegenerate(EntityRegainHealthEvent e){
		if (e.getEntity() instanceof Player){
			Player p = (Player)e.getEntity();
			if (isPlayer(p.getName())){
				if (gameTime.get(getTTTPlayer(p.getName())) != null)
					e.setCancelled(true);
			}
		}
	}
}
