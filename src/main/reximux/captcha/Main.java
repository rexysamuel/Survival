package main.reximux.captcha;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Main extends JavaPlugin implements Listener {

	public static String format;

	// An instance of the CaptchaGUI class
	CaptchaGUI gui;

	public void onEnable() {
		getLogger().info("Thanks for using captcha!");
		saveDefaultConfig();
		// Initialise the CaptchaGUI
		gui = new CaptchaGUI(this);
		// Register the join event, chat event, inventory close event, etc...
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(gui, this);

	}

	@EventHandler
	public void leave(PlayerQuitEvent e) {
		if (!playerVerified(e.getPlayer())) {
			if (!gui.verified.contains(e.getPlayer())) {
				e.setQuitMessage("");
			}
		}
	}

	@EventHandler
	public void join(PlayerJoinEvent e) {
		// If the player is verified, ignore them
		if (!playerVerified(e.getPlayer())) {
			format = e.getJoinMessage().replace(e.getPlayer().getName(), "NAME");
			e.setJoinMessage("");
			// Wait "wait-time" ticks before sending the GUI so they do not instantly close
			// it
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					// The player is not verified, alert console to make logs easier and send the
					// captcha to the player
					Player p = e.getPlayer();
					getLogger().info(p.getName() + " has not verified their captcha code before. Sending now.");
					gui.send(p);
					p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 10000, 255, true, false, false));
					p.setInvulnerable(true);
				}
			}, getConfig().getInt("wait-time"));
		}
	}

	// Check if the player is verified or not
	public boolean playerVerified(Player pl) {
		return getConfig().getStringList("Verified-Players").contains(pl.getUniqueId().toString());
	}
}
