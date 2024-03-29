package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final HashMap<Long, Game> games = new HashMap<>();
    private final String token;
    private DiscordApi api;

    public Bot(String token) {
        this.token = token;
    }

    public HashMap<Long, Game> getGames() {
        return games;
    }

    /**
     * Starts the bot, loads games, adds commands, and initializes event listeners.
     */
    public void start() {
        // Create the bot
        api = new DiscordApiBuilder().setToken(token).login().join();

        // Let the user know the bot is working correctly
        System.out.println("Logged in as " + api.getYourself().getDiscriminatedName());

        // Set bot status
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity(ActivityType.PLAYING, "Type /newgame to start a game");

        loadGames();
        addCommands();
        addListeners();

        // Save all active games every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            for (Game game : games.values()) {
                try {
                    game.updateCookies();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            saveGames();
        }, 1, 5, TimeUnit.MINUTES);
    }

    /**
     * Adds all necessary event listeners for the bot to function.
     * This includes slash command create and message component create.
     */
    private void addListeners() {
        Commands commands = new Commands(this);

        // Listen to slash commands
        api.addSlashCommandCreateListener(event -> {
            switch (event.getSlashCommandInteraction().getCommandName().toLowerCase()) {
                case "newgame" -> commands.newGame(event.getSlashCommandInteraction());
                case "buy" -> commands.buy(event.getSlashCommandInteraction());
                case "help" -> commands.help(event.getSlashCommandInteraction());
                case "resendmessage" -> commands.resendMessage(event.getSlashCommandInteraction());
                case "quit" -> commands.quit(event.getSlashCommandInteraction());
            }
        });

        // Listen to button presses
        api.addMessageComponentCreateListener(event -> {
            MessageComponentInteraction interaction = event.getMessageComponentInteraction();
            if (!interaction.getCustomId().equals("click")) {
                return;
            }

            long userId = interaction.getUser().getId();
            if (!games.containsKey(userId)) {
                return;
            }
            Game game = games.get(userId);

            game.updateCookies();
            game.addCookie();

            interaction.acknowledge();
            game.getMessage().edit(game.toEmbedBuilder());
        });
    }

    /**
     * Adds all commands to allow the bot to run.
     * This includes /newgame, /buy, /help, /resendmessage, and /quit.
     */
    private void addCommands() {
        // Create slash commands (may take a few minutes to update on Discord)
        SlashCommand.with("newgame", "Starts a game of Cookie Clicker").createGlobal(api).join();

        SlashCommand.with("buy", "Buy an item in your game",
                Arrays.asList(
                        SlashCommandOption.create(SlashCommandOptionType.STRING, "ITEM", "Building to be purchased", true),
                        SlashCommandOption.create(SlashCommandOptionType.LONG, "AMOUNT", "Amount of buildings to be purchased", false)
                )).createGlobal(api).join();

        SlashCommand.with("help", "Information about Cookie Clicker and bot leaderboard").createGlobal(api).join();
        SlashCommand.with("resendmessage", "Resends the game message").createGlobal(api).join();
        SlashCommand.with("quit", "Quits the current game").createGlobal(api).join();
    }

    /**
     * Saves all active games to the saves.json file.
     */
    public void saveGames() {
        try {
            JSONObject saves = new JSONObject();
            JSONObject gamesObj = new JSONObject();
            for (long id : games.keySet()) {
                gamesObj.put(String.valueOf(id), games.get(id).toJSONObject());
            }

            saves.put("games", gamesObj);
            saves.put("last-saved", System.currentTimeMillis());

            FileWriter writer = new FileWriter("saves.json");
            writer.write(saves.toJSONString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads all games from the saves.json file.
     */
    private void loadGames() {
        try {
            String fileName = "saves.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject saves = (JSONObject) parser.parse(reader);
            reader.close();

            if (!saves.containsKey("last-saved")) {
                return;
            }

            long lastSaved = (long) saves.get("last-saved");

            JSONObject gamesObj = (JSONObject) saves.get("games");
            for (Object obj : gamesObj.keySet()) {
                String key = (String) obj;
                JSONObject game = (JSONObject) gamesObj.get(key);
                games.put(Long.parseLong(key), new Game(game, api, Long.parseLong(key), lastSaved));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
