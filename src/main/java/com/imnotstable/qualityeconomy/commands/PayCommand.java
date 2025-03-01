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
      .then(new DoubleArgument("amount", Number.getMinimumValue())
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
    double amount = Number.roundObj(args.get("amount"));
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
  
}
