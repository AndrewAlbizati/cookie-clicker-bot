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
    public static void newGame(SlashCommandInteraction interaction, HashMap<Long, Game> games) {
        if (games.containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You already have a game started.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }
        Game game = new Game(interaction.getUser());
        games.put(interaction.getUser().getId(), game);

        Message message = interaction.getUser().sendMessage(game.toEmbedBuilder(), ActionRow.of(Button.primary("click", "\uD83C\uDF6A"))).join();
        game.setMessage(message);

        interaction.createImmediateResponder()
                .setContent(":thumbsup:")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond().join();
    }

    public static void buy(SlashCommandInteraction interaction, HashMap<Long, Game> games) {
        if (!games.containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You must start a game before buying items.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        Game game = games.get(interaction.getUser().getId());

        String item = interaction.getOptionStringValueByName("ITEM").get();
        int amount = Math.toIntExact(interaction.getOptionLongValueByName("AMOUNT").orElse(1L));

        amount = Math.abs(amount);

        while (game.getCookies() < game.getCost(item, amount)) {
            amount--;
        }

        if (game.buy(item, amount)) {
            interaction.createImmediateResponder()
                    .setContent("Successfully purchased " + amount + " building" + (amount == 1 ? "" : "s"))
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
        } else {
            interaction.createImmediateResponder()
                    .setContent("Couldn't purchase item.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
        }
    }

    public static void help(SlashCommandInteraction interaction, HashMap<Long, Game> games) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Cookie Clicker");
        eb.setColor(new Color(204, 204, 204));
        eb.setDescription("To start a game of Cookie Clicker, type /newgame. A message will be directly sent to you with more instructions on how to play.");

        LinkedHashMap<User, Long> sortedMap = new LinkedHashMap<>();
        for (Long key : games.keySet()) {
            sortedMap.put(games.get(key).getUser(), games.get(key).getCookies());
        }

        // Sort the LinkedHashMap
        List<Map.Entry<User, Long>> entries = new ArrayList<>(sortedMap.entrySet());
        Collections.sort(entries, Comparator.comparingLong(Map.Entry::getValue));


        sortedMap.clear();
        for(Map.Entry<User, Long> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        List<User> reverseOrderedKeys = new ArrayList<>(sortedMap.keySet());
        Collections.reverse(reverseOrderedKeys);

        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (User user : reverseOrderedKeys) {
            builder.append(i + ". " + user.getDiscriminatedName() + " **(" + String.format("%,d", sortedMap.get(user)) + " :cookie:)**");
            if (++i > 10) {
                break;
            }
        }

        eb.addField("Rankings", builder.toString());

        interaction.createImmediateResponder()
                .addEmbed(eb)
                .respond().join();
    }

    public static void resendMessage(SlashCommandInteraction interaction, HashMap<Long, Game> games) {
        if (!games.containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You aren't currently playing Cookie Clicker. Type /newgame to start a game.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }
        Game game = games.get(interaction.getUser().getId());
        Message message = game.getUser().sendMessage(game.toEmbedBuilder(), ActionRow.of(Button.primary("click", "\uD83C\uDF6A"))).join();
        game.getMessage().delete();
        game.setMessage(message);

        interaction.createImmediateResponder()
                .setContent(":thumbsup:")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond().join();

        Bot.saveGames();
    }

    public static void quit(SlashCommandInteraction interaction, HashMap<Long, Game> games) {
        if (!games.containsKey(interaction.getUser().getId())) {
            interaction.createImmediateResponder()
                    .setContent("You aren't currently playing Cookie Clicker. Type /newgame to start a game.")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond().join();
            return;
        }

        games.remove(interaction.getUser().getId());

        interaction.createImmediateResponder()
                .setContent(":thumbsup:")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond().join();

        Bot.saveGames();
    }
}
