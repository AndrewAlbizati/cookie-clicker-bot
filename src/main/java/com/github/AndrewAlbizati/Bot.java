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
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final HashMap<Long, Game> games = new HashMap<>();
    private static DiscordApi api;

    public static void main(String[] args) {
        String token = "";

        // Get token from config.properties
        try {
            // Create config.properties if absent
            File f = new File("config.properties");
            if (f.createNewFile()) {
                System.out.println(f.getName() + " created.");
                FileWriter fw = new FileWriter("config.properties");
                fw.write("token=");
                fw.close();
            }

            Properties prop = new Properties();
            FileInputStream ip = new FileInputStream("config.properties");
            prop.load(ip);
            ip.close();

            // Get the bot token
            token = prop.getProperty("token");

            if (token == null || token.length() == 0) {
                throw new NullPointerException("Please add the bot's token to config.properties");
            }

            // Create saves.json
            File saves = new File("saves.json");
            if (saves.createNewFile()) {
                FileWriter writer = new FileWriter("saves.json");
                writer.write("{}"); // Empty JSON object
                writer.close();
                System.out.println(saves.getName() + " has been created");
            }

            // Stop program if an error is raised (bot token not found)
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return;
        }

        // Create the bot
        api = new DiscordApiBuilder().setToken(token).login().join();

        // Let the user know the bot is working correctly
        System.out.println("Logged in as " + api.getYourself().getDiscriminatedName());


        // Set bot status
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity(ActivityType.PLAYING, "Type /newgame to start a game");

        // Create slash commands (may take a few mins to update on Discord)
        SlashCommand.with("newgame", "Starts a game of Cookie Clicker").createGlobal(api).join();

        SlashCommand.with("buy", "Buy an item in your game",
                Arrays.asList(
                        SlashCommandOption.create(SlashCommandOptionType.STRING, "ITEM", "Building to be purchased", true),
                        SlashCommandOption.create(SlashCommandOptionType.LONG, "AMOUNT", "Amount of buildings to be purchased", false)
                )).createGlobal(api).join();

        SlashCommand.with("help", "Information about Cookie Clicker and server leaderboard").createGlobal(api).join();
        SlashCommand.with("resendmessage", "Resends the game message").createGlobal(api).join();
        SlashCommand.with("quit", "Quits the current game").createGlobal(api).join();

        api.addSlashCommandCreateListener(event -> {
            switch (event.getSlashCommandInteraction().getCommandName().toLowerCase()) {
                case "newgame" -> Commands.newGame(event.getSlashCommandInteraction(), games);
                case "buy" -> Commands.buy(event.getSlashCommandInteraction(), games);
                case "help" -> Commands.help(event.getSlashCommandInteraction(), games);
                case "resendmessage" -> Commands.resendMessage(event.getSlashCommandInteraction(), games);
                case "quit" -> Commands.quit(event.getSlashCommandInteraction(), games);
            }
        });

        api.addMessageComponentCreateListener(event -> {
            MessageComponentInteraction interaction = event.getMessageComponentInteraction();
            if (!interaction.getCustomId().equals("click")) {
                return;
            }

            long userId = interaction.getUser().getId();
            if (!games.containsKey(userId)) {
                return;
            }

            games.get(userId).addCookie();
            interaction.acknowledge();
        });

        loadGames();

        scheduler.scheduleAtFixedRate(() -> {
            if ((System.currentTimeMillis() / 1000) % 300 == 0) {
                saveGames();
            }

            for (Game game : games.values()) {
                try {
                    game.updateCookies();
                    if ((System.currentTimeMillis() / 1000) % 10 == 0) {
                        game.getMessage().edit(game.toEmbedBuilder());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void saveGames() {
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

    private static void loadGames() {
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
