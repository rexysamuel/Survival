package main.reximux.captcha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import net.md_5.bungee.api.ChatColor;

public class CaptchaGUI implements Listener {

	// Instances of the main class and random class
	JavaPlugin plugin;
	Random r;

	// A store of the verified players. This will contain players who have verified
	// recently
	ArrayList<Player> verified = new ArrayList<Player>();

	// The title of the GUI
	String title = ChatColor.BLUE.toString() + ChatColor.BOLD + "Please select ";

	// The amount of times each user has passed a captcha
	HashMap<Player, Integer> amountPassed = new HashMap<Player, Integer>();

	// The colours to display within the GUI
	ArrayList<Material> colours = new ArrayList<Material>();

	// Instantiate the class
	public CaptchaGUI(JavaPlugin plugin) {
		// Initialise the classes
		this.plugin = plugin;
		r = new Random();
		// If there is no colours in the list, add them
		if (colours.isEmpty()) {
			colours.add(Material.WHITE_STAINED_GLASS_PANE);
			colours.add(Material.ORANGE_STAINED_GLASS_PANE);
			colours.add(Material.MAGENTA_STAINED_GLASS_PANE);
			colours.add(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
			colours.add(Material.YELLOW_STAINED_GLASS_PANE);
			colours.add(Material.LIME_STAINED_GLASS_PANE);
			colours.add(Material.PINK_STAINED_GLASS_PANE);
			colours.add(Material.CYAN_STAINED_GLASS_PANE);
			colours.add(Material.PURPLE_STAINED_GLASS_PANE);
			colours.add(Material.BLUE_STAINED_GLASS_PANE);
			colours.add(Material.BROWN_STAINED_GLASS_PANE);
			colours.add(Material.GREEN_STAINED_GLASS_PANE);
			colours.add(Material.RED_STAINED_GLASS_PANE);
		}
	}

	// Method to verify a player
	public void verifyPlayer(Player pl) {
		// Add one to the amount of times a player has passed the captcha
		amountPassed.put(pl, amountPassed.get(pl) + 1);
		// If the user hasn't passed the right amount of times, present the GUI again
		if (amountPassed.get(pl) < plugin.getConfig().getInt("captcha-times")) {
			send(pl);
		} else {
			// Remove them from the amount of times they have passed so we clear up memory
			amountPassed.remove(pl);
			// Close the GUI
			pl.closeInventory();
			// Send the message to them to say they have passed the captcha
			pl.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("pass-message")));
			// Get the list of verified players from the config.yml
			List<String> s = plugin.getConfig().getStringList("Verified-Players");
			// Add the player to the list
			s.add(pl.getUniqueId().toString());
			// Save the list back to the config.yml
			plugin.getConfig().set("Verified-Players", s);
			// Save the config.yml
			plugin.saveConfig();
			// Add the player to the list of verified users
			verified.add(pl);
			// Alert staff they pass
			alertOp(pl, "", true);
			// Send the join message
			Bukkit.broadcastMessage(Main.format.replaceAll("NAME", pl.getName()));
			// Reset their walk speed
			for (PotionEffect pe : pl.getActivePotionEffects()) {
				pl.removePotionEffect(pe.getType());
			}
			// Remove their god mode
			pl.setInvulnerable(false);
		}
	}

	// Send the inventory to the player
	public void send(Player p) {
		// If the player has not already attempted the captcha, add them to the list
		// with
		// 0 attempts
		if (!amountPassed.containsKey(p)) {
			amountPassed.put(p, 0);
		}
		// Get a random colour for the captcha
		int index = r.nextInt(colours.size());
		Material mat = colours.get(index);
		// Strip the material down to a string (EG: Material.RED_STAINED_GLASS_PANE to
		// "red")
		String name = mat.toString().replaceAll("_STAINED_GLASS_PANE", "").replaceAll("_", " ").toLowerCase();
		// Create the inventory with 4 rows (36 slots) and format the title
		Inventory inv = addBorder(Bukkit.getServer().createInventory(null, 36, title + name));
		// Add random colours to the GUI from the colours list
		for (int item = 0; item <= 13; item++) {
			int toAdd = r.nextInt(colours.size());
			ItemStack actualItem = new ItemStack(colours.get(toAdd));
			ItemMeta im = actualItem.getItemMeta();
			// This is to prevent items stacking (EG two types of the same glass)
			im.setCustomModelData(item);
			im.setDisplayName(formatItemName(actualItem.getType()));
			actualItem.setItemMeta(im);
			inv.addItem(actualItem);
		}
		// Pick a random integer (0-6)
		int slot = r.nextInt(6);
		// The slot relative to the inventory which we will add the new item
		int realSlot;
		if (r.nextBoolean()) {
			// Top row selected
			realSlot = 10 + slot;
		} else {
			// Bottom row selected
			realSlot = 19 + slot;
		}
		// Format the correct item
		ItemStack is = new ItemStack(mat);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(formatItemName(mat));
		is.setItemMeta(im);

		// Put the item in the inventory to ensure it is always solvable
		inv.setItem(realSlot, is);
		// Open the inventory
		p.openInventory(inv);
	}

	// Add a border to the inventory (Around the top row, bottom row and sides)
	private Inventory addBorder(Inventory inv) {
		for (int i = 0; i <= 9; i++) {
			inv.setItem(i, emptyGlass());
		}
		inv.setItem(17, emptyGlass());
		inv.setItem(18, emptyGlass());
		for (int i = 26; i <= 35; i++) {
			inv.setItem(i, emptyGlass());
		}
		return inv;
	}

	// Ensure it runs as LOWEST so that it is not overrided by another plugin so
	// they can not talk
	@EventHandler(priority = EventPriority.LOWEST)
	public void chat(AsyncPlayerChatEvent e) {
		// If the player is not verified either in memory or the config.yml
		if (!playerVerified(e.getPlayer())) {
			if (!verified.contains(e.getPlayer())) {
				// Warn the player
				e.getPlayer().sendMessage(
						ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("can-not-talk")));
				// Cancel the message
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void close(InventoryCloseEvent e) {
		Player p = (Player) e.getPlayer();
		// Add a scheduler so that the GUI does not glitch
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				// If the player is not verified at all
				if (!verified.contains(p)) {
					if (!playerVerified(p)) {
						try {
							// If the dismissed inventory is the captcha inventory, the player
							if (p.getOpenInventory() != null) {
								if (!p.getOpenInventory().getTitle().contains(title)) {
									// Prevent double logging
									send(p);
								}
							}
						} catch (Exception e2) {
						}
					}
				}
			}
		}, 2);
	}

	// Check whether a player is verified or not
	public boolean playerVerified(Player pl) {
		return plugin.getConfig().getStringList("Verified-Players").contains(pl.getUniqueId().toString());
	}

	// Listen for click events
	@EventHandler
	public void click(InventoryClickEvent e) {
		try {
			// Get the item they click
			ItemStack is = e.getCurrentItem();
			// Get their view of the inventory
			InventoryView view = e.getView();
			// Proceed if the inventory title is correct
			if (view.getTitle().contains(title)) {
				// Strip down the title to a material and compare to the clicked item's material
				if (is.getType().equals(
						Material.getMaterial(view.getTitle().replaceAll(title, "").replaceAll(" ", "_").toUpperCase()
								+ "_STAINED_GLASS_PANE"))) {
					// Verify the player as they have clicked the correct item
					verifyPlayer((Player) e.getWhoClicked());
				} else {
					// Kick the player, they have failed the captcha
					Player p = (Player) e.getWhoClicked();
					p.kickPlayer(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("captcha-failed-message").replaceAll("%amount%",
									"" + plugin.getConfig().getInt("Failure-Ban-Times"))));
					// Alert staff
					alertOp(p, "", false);
					// Log that they have failed with a reason
					new FailLogger(plugin, p, "Failed Captcha");
				}
				// Cancel the click so they can't take or drop items
				e.setCancelled(true);
			}
		} catch (Exception e2) { // If they click outside the GUI, supress the error
		}
	}

	// The border glass for the GUI, this has no name
	private ItemStack emptyGlass() {
		ItemStack is = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(" ");
		is.setItemMeta(im);
		return is;
	}

	private void alertOp(Player p, String reason, boolean pass) {
		if (!pass) {
			if (reason != "") {
				for (Player op : Bukkit.getOnlinePlayers()) {
					if (op.isOp() || op.hasPermission("captcha.viewalert"))
						op.sendMessage(ChatColor.RED + p.getName() + " has failed the captcha for " + reason);
				}
			} else {
				for (Player op : Bukkit.getOnlinePlayers()) {
					if (op.isOp() || op.hasPermission("captcha.viewalert"))
						op.sendMessage(ChatColor.RED + p.getName() + " has failed the captcha");
				}
			}
		} else {
			for (Player op : Bukkit.getOnlinePlayers()) {
				if (op.isOp() || op.hasPermission("captcha.viewalert"))
					op.sendMessage(ChatColor.GREEN + p.getName() + " has passed the captcha");
			}
		}
	}

	// Format a material down into a coloured, easily readable string
	private String formatItemName(Material mat) {
		String name = mat.toString().replaceAll("_STAINED_GLASS_PANE", "");
		String formattedName = "";
		for (String s : name.split("_")) {
			formattedName = formattedName + s.substring(0, 1).toUpperCase() + s.substring(1, s.length()).toLowerCase()
					+ " ";
		}
		return applyColour(formattedName, mat);
	}

	// Add a colour to a string based on its material
	private String applyColour(String s, Material m) {
		if (m.equals(Material.WHITE_STAINED_GLASS_PANE)) {
			s = ChatColor.WHITE + s;
		} else if (m.equals(Material.ORANGE_STAINED_GLASS_PANE)) {
			s = ChatColor.GOLD + s;
		} else if (m.equals(Material.MAGENTA_STAINED_GLASS_PANE)) {
			s = ChatColor.DARK_PURPLE + s;
		} else if (m.equals(Material.LIGHT_BLUE_STAINED_GLASS_PANE)) {
			s = ChatColor.AQUA + s;
		} else if (m.equals(Material.YELLOW_STAINED_GLASS_PANE)) {
			s = ChatColor.YELLOW + s;
		} else if (m.equals(Material.LIME_STAINED_GLASS_PANE)) {
			s = ChatColor.GREEN + s;
		} else if (m.equals(Material.PINK_STAINED_GLASS_PANE)) {
			s = ChatColor.LIGHT_PURPLE + s;
		} else if (m.equals(Material.CYAN_STAINED_GLASS_PANE)) {
			s = ChatColor.DARK_AQUA + s;
		} else if (m.equals(Material.PURPLE_STAINED_GLASS_PANE)) {
			s = ChatColor.DARK_PURPLE + s;
		} else if (m.equals(Material.BLUE_STAINED_GLASS_PANE)) {
			s = ChatColor.DARK_BLUE + s;
		} else if (m.equals(Material.BROWN_STAINED_GLASS_PANE)) {
			s = ChatColor.GRAY + s;
		} else if (m.equals(Material.GREEN_STAINED_GLASS_PANE)) {
			s = ChatColor.DARK_GREEN + s;
		} else if (m.equals(Material.RED_STAINED_GLASS_PANE)) {
			s = ChatColor.DARK_RED + s;
		}
		return s;
	}

}
