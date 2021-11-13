package main.reximux.captcha;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FailLogger {

	public FailLogger(JavaPlugin plugin, Player p, String reason) {
		// Only log if the logger is enabled
		if (plugin.getConfig().getBoolean("Use-Logging")) {
			// Format the text
			String text = reason + " - " + p.getUniqueId().toString() + " - " + getTime();
			// Get their previous failures
			List<String> failures = plugin.getConfig().getStringList("Failed-Captcha-Attempts." + p.getName());
			// Only ban players if the module is enabled
			if (plugin.getConfig().getBoolean("Ban-after-too-many-tries")) {
				if (failures.size() > 0) {
					// Manage the commands ran to ban people if their failure count is too high
					if ((failures.size() % plugin.getConfig().getInt("Failure-Ban-Times")) == 0) {
						// Ban the user, not just kick them
						plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
								plugin.getConfig().getString("Ban-Command").replaceAll("%player%", p.getName()));
						// Add the ban to the log
						text = text + " - Ban issued (exceeded maximum failure count)";
					}
				}
			}
			// Add the new failure to the previous failures
			failures.add(text);
			// Save the captcha failure
			plugin.getConfig().set("Failed-Captcha-Attempts." + p.getName(), failures);
			// Save the config.yml
			plugin.saveConfig();
		}
	}

	// Get the time as a formatted date
	private String getTime() {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		return ("[" + format.format(new Date()) + "]");
	}

	// Get the amount of times a player has failed the captcha
	public static int getFailures(JavaPlugin plugin, Player p) {
		// The placeholder
		int times = 0;
		// Iterate through all the times they have failed and add to times every pass
		for (String s : plugin.getConfig().getStringList("Failed-Captcha-Attempts." + p.getName())) {
			if (s.contains(p.getUniqueId().toString())) {
				times++;
			}
		}
		return times;
	}

}
