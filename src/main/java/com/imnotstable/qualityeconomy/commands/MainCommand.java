package com.imnotstable.qualityeconomy.commands;

import com.imnotstable.qualityeconomy.QualityEconomy;
import com.imnotstable.qualityeconomy.configuration.Configuration;
import com.imnotstable.qualityeconomy.configuration.Messages;
import com.imnotstable.qualityeconomy.storage.StorageManager;
import com.imnotstable.qualityeconomy.storage.accounts.Account;
import com.imnotstable.qualityeconomy.storage.accounts.AccountManager;
import com.imnotstable.qualityeconomy.util.Debug;
import com.imnotstable.qualityeconomy.util.Misc;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class MainCommand implements Command {
  
  private final Pattern IMPORT_FILE_PATTERN = Pattern.compile("^QualityEconomy \\d{4}.\\d{2}.\\d{2} \\d{2}-\\d{2}\\.json$");
  private final CommandTree command = new CommandTree("qualityeconomy")
    .withAliases("qe")
    .withPermission("qualityeconomy.admin")
    .then(new LiteralArgument("reload")
      .executes(this::reload)
      .then(new LiteralArgument("messages")
        .executes(this::reloadMessages)))
    .then(new LiteralArgument("database")
      .then(new LiteralArgument("reset")
        .withRequirement(sender -> Debug.DEBUG_MODE)
        .executesConsole(this::resetDatabase))
      .then(new LiteralArgument("import")
        .then(new GreedyStringArgument("importable")
          .replaceSuggestions(ArgumentSuggestions.strings(info -> getImportableFiles()))
          .executes(this::importDatabase)))
      .then(new LiteralArgument("export")
        .executes(this::exportDatabase))
      .then(new LiteralArgument("createFakeEntries")
        .withRequirement(sender -> Debug.DEBUG_MODE)
        .then(new IntegerArgument("entries", 1)
          .executes(this::createFakeEntries)))
      .then(new LiteralArgument("changeAllEntries")
        .withRequirement(sender -> Debug.DEBUG_MODE)
        .executes(this::changeAllEntries)))
    .then(new LiteralArgument("economy")
      .withRequirement(sender -> Configuration.areCustomCurrenciesEnabled())
      .then(new LiteralArgument("createCustomCurrency")
        .then(new StringArgument("name")
          .executes(this::createCustomCurrency)))
      .then(new LiteralArgument("deleteCustomCurrency")
        .then(new StringArgument("name")
          .replaceSuggestions(ArgumentSuggestions.strings(info -> StorageManager.getActiveStorageType().getCurrencies().toArray(new String[0])))
          .executes(this::deleteCustomCurrency)
        )));
  private boolean isRegistered;
  
  public void register() {
    if (isRegistered)
      return;
    command.register();
    isRegistered = true;
  }
  
  public void unregister() {
    if (!isRegistered)
      return;
    CommandAPI.unregister(command.getName(), true);
    isRegistered = false;
  }
  
  private void reload(CommandSender sender, CommandArguments args) {
    Debug.Timer timer = new Debug.Timer("reload()");
    StorageManager.endStorageProcesses();
    Configuration.load();
    Messages.load();
    CommandManager.unregisterCommands();
    StorageManager.initStorageProcesses();
    CommandManager.registerCommands();
    timer.end();
    sender.sendMessage(Component.text("Reloading QualityEconomy...", NamedTextColor.GRAY));
  }
  
  private void reloadMessages(CommandSender sender, CommandArguments args) {
    Messages.load();
    sender.sendMessage(Component.text("Reloading QualityEconomy messages.yml...", NamedTextColor.GRAY));
  }
  
  private void resetDatabase(ConsoleCommandSender sender, CommandArguments args) {
    StorageManager.getActiveStorageType().wipeDatabase();
    sender.sendMessage(Component.text("Resetting database...", NamedTextColor.RED));
  }
  
  private void importDatabase(CommandSender sender, CommandArguments args) {
    String importable = (String) args.get("importable");
    if (Misc.equals(importable, "Essentials"))
      transferPluginData(importable, sender);
    else {
      StorageManager.importDatabase(importable);
      sender.sendMessage(Component.text("Imported Database", NamedTextColor.GREEN));
    }
  }
  
  private void transferPluginData(String plugin, CommandSender sender) {
    new BukkitRunnable() {
      @Override
      public void run() {
        Debug.Timer timer = new Debug.Timer("transferPluginData()");
        Collection<Account> accounts = new ArrayList<>();
        if (plugin.equals("Essentials")) {
          File[] userdata = new File("plugins/Essentials/userdata").listFiles((dir, name) -> Misc.isUUID(name.split("\\.")[0]));
          if (userdata == null || userdata.length == 0) {
            sender.sendMessage(Component.text("Failed to import Database", NamedTextColor.RED));
            return;
          }
          for (File userfile : userdata) {
            YamlConfiguration user = YamlConfiguration.loadConfiguration(userfile);
            UUID uuid = UUID.fromString(userfile.getName().split("\\.")[0]);
            String username = user.getString("last-account-name");
            double balance = Double.parseDouble(user.getString("money"));
            accounts.add(new Account(uuid).setUsername(username).setBalance(balance));
          }
        }
        StorageManager.getActiveStorageType().wipeDatabase();
        StorageManager.getActiveStorageType().createAccounts(accounts);
        AccountManager.setupAccounts();
        sender.sendMessage(Component.text("Imported Database", NamedTextColor.GREEN));
        timer.end();
      }
    }.runTaskAsynchronously(QualityEconomy.getInstance());
  }
  
  private void exportDatabase(CommandSender sender, CommandArguments args) {
    StorageManager.exportDatabase("plugins/QualityEconomy/exports/");
    sender.sendMessage(Component.text("Exporting database", NamedTextColor.GREEN));
  }
  
  private void createFakeEntries(CommandSender sender, CommandArguments args) {
    int entries = (int) args.get("entries");
    AccountManager.createFakeAccounts(entries != 0 ? entries : 10);
  }
  
  private void changeAllEntries(CommandSender sender, CommandArguments args) {
    AccountManager.changeAllAccounts();
  }
  
  private void createCustomCurrency(CommandSender sender, CommandArguments args) {
    String currency = args.get("name").toString().toUpperCase();
    if (StorageManager.getActiveStorageType().getCurrencies().contains(currency)) {
      sender.sendMessage(Component.text("That currency already exists", NamedTextColor.RED));
      return;
    }
    sender.sendMessage(Component.text("Creating custom currency \"" + currency + "\"", NamedTextColor.GRAY));
    StorageManager.getActiveStorageType().addCurrency(currency);
  }
  
  private void deleteCustomCurrency(CommandSender sender, CommandArguments args) {
    String currency = args.get("name").toString().toUpperCase();
    if (!StorageManager.getActiveStorageType().getCurrencies().contains(currency)) {
      sender.sendMessage(Component.text("That currency does not exist", NamedTextColor.RED));
      return;
    }
    sender.sendMessage(Component.text("Deleting custom currency \"" + currency + "\"", NamedTextColor.GRAY));
    StorageManager.getActiveStorageType().removeCurrency(currency);
  }
  
  private String[] getImportableFiles() {
    File dataFolder = QualityEconomy.getInstance().getDataFolder();
    FilenameFilter filter = (dir, name) -> IMPORT_FILE_PATTERN.matcher(name).matches();
    
    File exportsFolder = new File(dataFolder, "exports");
    List<String> exportsFiles = exportsFolder.isDirectory()
      ? Optional.ofNullable(exportsFolder.listFiles(filter))
      .map(Arrays::asList)
      .orElse(Collections.emptyList())
      .stream()
      .map(file -> "exports/" + file.getName())
      .toList()
      : Collections.emptyList();
    
    File backupFolder = new File(dataFolder, "backups");
    List<String> backupFiles = backupFolder.isDirectory()
      ? Optional.ofNullable(backupFolder.listFiles(filter))
      .map(Arrays::asList)
      .orElse(Collections.emptyList())
      .stream()
      .map(file -> "backups/" + file.getName())
      .toList()
      : Collections.emptyList();
    
    List<String> completions = new ArrayList<>(exportsFiles);
    completions.addAll(backupFiles);
    
    if (new File("plugins/Essentials/userdata").isDirectory()) {
      completions.add("Essentials");
    }
    
    return completions.toArray(new String[0]);
  }
  
  
}
