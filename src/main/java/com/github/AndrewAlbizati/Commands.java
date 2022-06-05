package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Commands {
    private final Bot bot;

    public Commands(Bot bot) {
        this.bot = bot;
    }

    /**
     * Responds to the /newgame command by initializing a new game.
     * @param interaction The slash command interaction that was created for a /newgame command being called.
     */
    public void newGame(SlashCommandInteraction interaction) {
        // User is already playing a game
        if (bot.getGames().containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You already have a game started.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        // Create game, add it to games HashMap
        Game game = new Game(interaction.getUser());
        bot.getGames().put(interaction.getUser().getId(), game);

        Message message = interaction.getUser().sendMessage(game.toEmbedBuilder(), ActionRow.of(Button.primary("click", "\uD83C\uDF6A"))).join();
        game.setMessage(message);

        interaction.createImmediateResponder()
                .setContent(":thumbsup:")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond().join();
    }

    /**
     * Responds to the /buy command by buying items in a game.
     * @param interaction The slash command interaction that was created for a /buy command being called.
     */
    public void buy(SlashCommandInteraction interaction) {
        // User isn't playing a game
        if (!bot.getGames().containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You must start a game before buying items.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Game game = bot.getGames().get(interaction.getUser().getId());

        // Get item requested
        String itemRequested = interaction.getOptionStringValueByName("ITEM").orElse("");
        Items item = Items.stringToItem(itemRequested);
        if (item == null) {
            interaction.createImmediateResponder()
                    .setContent("Item not found.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        long amount = Math.abs(interaction.getOptionLongValueByName("AMOUNT").orElse(1L));

        // Decrease amount purchased if it's too large
        while (game.getCookies() < game.getCost(item, amount)) {
            amount--;
        }

        if (amount != 0 && game.buy(item, amount)) {
            // Purchase successful
            interaction.createImmediateResponder()
                    .setContent("Successfully purchased " + amount + " item" + (amount == 1 ? "" : "s"))
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
        } else {
            // Purchase failed
            interaction.createImmediateResponder()
                    .setContent("Couldn't purchase item(s).")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    /**
     * Responds to the /help command by showing information about the bot, including a leaderboard.
     * @param interaction The slash command interaction that was created for a /help command being called.
     */
    public void help(SlashCommandInteraction interaction) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Cookie Clicker");
        eb.setColor(new Color(204, 204, 204));
        eb.setDescription("To start a game of Cookie Clicker, type /newgame. A message will be directly sent to you with more instructions on how to play.");

        // Transfer bot games to a LinkedHashMap
        LinkedHashMap<User, Long> sortedMap = new LinkedHashMap<>();
        for (Long key : bot.getGames().keySet()) {
            sortedMap.put(bot.getGames().get(key).getUser(), bot.getGames().get(key).getCookies());
        }

        // Sort the LinkedHashMap
        // Store sorted items in a list of entries
        List<Map.Entry<User, Long>> entries = new ArrayList<>(sortedMap.entrySet());
        entries.sort(Comparator.comparingLong(Map.Entry::getValue));

        // Add list of entries back into the LinkedHashMap
        sortedMap.clear();
        for(Map.Entry<User, Long> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        // Reverse the order of the LinkedHashMap
        List<User> reverseOrderedKeys = new ArrayList<>(sortedMap.keySet());
        Collections.reverse(reverseOrderedKeys);

        // Create leaderboard
        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (User user : reverseOrderedKeys) {
            builder.append(i); // Place
            builder.append(". ");
            builder.append(user.getDiscriminatedName()); // Username
            builder.append(" **(");
            builder.append(String.format("%,d", sortedMap.get(user))); // Cookies
            builder.append(" :cookie:)**");
            // Stop at the 10th user
            if (++i > 10) {
                break;
            }
        }

        eb.addField("Rankings", builder.toString());

        interaction.createImmediateResponder()
                .addEmbed(eb)
                .respond().join();
    }

    /**
     * Responds to the /resendmessage command by resending the game message in case the user can't find the original.
     * @param interaction The slash command interaction that was created for a /resendmessgae command being called.
     */
    public void resendMessage(SlashCommandInteraction interaction) {
        // User isn't playing a game
        if (!bot.getGames().containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You aren't currently playing Cookie Clicker. Type /newgame to start a game.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Game game = bot.getGames().get(interaction.getUser().getId());

        try {
            // Send a new message, delete the old message
            Message message = game.getUser().sendMessage(game.toEmbedBuilder(), ActionRow.of(Button.primary("click", "\uD83C\uDF6A"))).join();
            game.getMessage().delete();
            game.setMessage(message);

            interaction.createImmediateResponder()
                    .setContent(":thumbsup:")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
        } catch (Exception e) {
            // Message failed to be replaced
            e.printStackTrace();
            interaction.createImmediateResponder()
                    .setContent(":thumbsdown: (" + e.getMessage() + ")")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
        }

        bot.saveGames();
    }

    /**
     * Responds to the /quit command by erasing the game that the user is playing from the saves.json file.
     * @param interaction The slash command interaction that was created for a /quit command being called.
     */
    public void quit(SlashCommandInteraction interaction) {
        // User isn't playing a game
        if (!bot.getGames().containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You aren't currently playing Cookie Clicker. Type /newgame to start a game.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        bot.getGames().remove(interaction.getUser().getId());

        interaction.createImmediateResponder()
                .setContent(":thumbsup:")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond().join();

        bot.saveGames();
    }
}
