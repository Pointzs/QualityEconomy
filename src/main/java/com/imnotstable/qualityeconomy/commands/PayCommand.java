package com.imnotstable.qualityeconomy.commands;

import com.imnotstable.qualityeconomy.api.QualityEconomyAPI;
import com.imnotstable.qualityeconomy.configuration.Configuration;
import com.imnotstable.qualityeconomy.configuration.MessageType;
import com.imnotstable.qualityeconomy.configuration.Messages;
import com.imnotstable.qualityeconomy.util.CommandUtils;
import com.imnotstable.qualityeconomy.util.Number;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PayCommand implements Command {
  
  private final CommandTree command = new CommandTree("pay")
    .then(new LiteralArgument("toggle")
      .executesPlayer(this::togglePay))
    .then(new OfflinePlayerArgument("target")
      .replaceSuggestions(CommandUtils.getOnlinePlayerSuggestion())
      .then(new AmountArgument("amount")
        .executesPlayer(this::pay)));
  private boolean isRegistered = false;
  
  public void register() {
    if (isRegistered || !Configuration.isCommandEnabled("pay"))
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
  
  private void togglePay(Player sender, CommandArguments args) {
    boolean toggle = !QualityEconomyAPI.isPayable(sender.getUniqueId());
    QualityEconomyAPI.setPayable(sender.getUniqueId(), toggle);
    if (toggle) {
      Messages.sendParsedMessage(sender, MessageType.PAY_TOGGLE_ON);
    } else {
      Messages.sendParsedMessage(sender, MessageType.PAY_TOGGLE_OFF);
    }
  }
  
  private void pay(Player sender, CommandArguments args) {
    OfflinePlayer target = (OfflinePlayer) args.get("target");
    if (CommandUtils.requirement(QualityEconomyAPI.hasAccount(target.getUniqueId()), MessageType.PLAYER_NOT_FOUND, sender))
      return;
    if (!QualityEconomyAPI.isPayable(target.getUniqueId())) {
      Messages.sendParsedMessage(sender, MessageType.NOT_ACCEPTING_PAYMENTS);
      return;
    }
    double amount = parseAmount((String) args.get("amount"));
    if (CommandUtils.requirement(QualityEconomyAPI.hasBalance(sender.getUniqueId(), amount), MessageType.SELF_NOT_ENOUGH_MONEY, sender))
      return;
    Messages.sendParsedMessage(sender, MessageType.PAY_SEND,
      Number.formatCommas(amount),
      target.getName()
    );
    if (target.isOnline())
      Messages.sendParsedMessage(target.getPlayer(), MessageType.PAY_RECEIVE,
        Number.formatCommas(amount),
        sender.getName());
    QualityEconomyAPI.transferBalance(sender.getUniqueId(), target.getUniqueId(), amount);
  }

  private double parseAmount(String amountString) {
    // Extract the number part from the string
    String numberPart = amountString.substring(0, amountString.length() - 1);
    // Extract the suffix character
    char suffix = amountString.charAt(amountString.length() - 1);

    // Convert the number part to double
    double number = Double.parseDouble(numberPart);

    // Determine the multiplier based on the suffix
    double multiplier;
    switch (suffix) {
        case 'm':
            multiplier = 1_000_000; // million
            break;
        case 'k':
            multiplier = 1_000; // thousand
            break;
        case 'b':
            multiplier = 1_000_000_000; // billion
            break;
        default:
            // If an invalid suffix is provided, treat it as 1 (no suffix)
            multiplier = 1;
            break;
    }

    // Calculate the final amount
    return number * multiplier;
  }
}
