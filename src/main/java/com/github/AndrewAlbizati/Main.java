package com.github.AndrewAlbizati;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        String token;

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

            // Load properties
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

        Bot bot = new Bot(token);
        bot.start();
    }
}
