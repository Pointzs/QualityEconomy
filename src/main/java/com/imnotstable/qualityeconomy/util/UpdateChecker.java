package com.imnotstable.qualityeconomy.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.imnotstable.qualityeconomy.QualityEconomy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker {
  
  public static void load() {
    String latestVersion = getLatestVersion();
    if (latestVersion == null)
      return;
    if (!Version.requiresUpdate(new Version(latestVersion))) {
      new Debug.QualityLogger("QualityEconomy is out of date. Please update it at the link below.", "https://github.com/ImNotStable/QualityEconomy/releases/latest").log();
      Bukkit.getPluginManager().registerEvents(new Listener() {
        @EventHandler
        public void on(PlayerJoinEvent event) {
          Player player = event.getPlayer();
          if (player.isOp() || player.hasPermission("qualityeconomy.admin")) {
            player.sendMessage(Component.text("QualityEconomy is out of date.", NamedTextColor.GRAY));
            player.sendMessage(Component.text().append(
              Component.text("Please update it ", NamedTextColor.GRAY),
              Component.text("here", NamedTextColor.GREEN)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl("https://github.com/ImNotStable/QualityEconomy/releases/latest"))
            ).build());
          }
        }
      }, QualityEconomy.getInstance());
    }
  }
  
  private static String getLatestVersion() {
    String url = "https://api.github.com/repos/ImNotStable/QualityEconomy/releases/latest";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
      JsonObject jsonObject = new Gson().fromJson(reader, JsonObject.class);
      return jsonObject.get("tag_name").getAsString();
    } catch (Exception exception) {
      new Debug.QualityError("Failed to check for update.", exception).log();
    }
    return null;
  }
  
}
