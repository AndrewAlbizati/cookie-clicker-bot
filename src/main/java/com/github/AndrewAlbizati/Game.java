package com.github.AndrewAlbizati;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Game {
    private static JSONObject itemData;
    private static final String[] buildings = {"Cursor", "Grandma", "Factory", "Mine", "Shipment", "Alchemy Lab", "Portal", "Time Machine"};

    private final User user;
    private final long startTime;
    private Message message;

    private double cookies;
    private double cookiesPerSecond;

    private int cursors;
    private int grandmas;
    private int factories;
    private int mines;
    private int shipments;
    private int alchemyLabs;
    private int portals;
    private int timeMachines;

    static {
        // Get all information about the store (saved locally in resources)
        try {
            InputStream jsonStream = Game.class.getResourceAsStream("/store.json");

            JSONParser parser = new JSONParser();
            itemData = (JSONObject) parser.parse(new InputStreamReader(jsonStream, "UTF-8"));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public Game(User user) {
        this.user = user;
        startTime = System.currentTimeMillis();
    }

    public Game(JSONObject object, DiscordApi api, long userId, long saveTime) {
        long messageId = (long) object.get("message-id");
        this.user = api.getUserById(userId).join();

        message = user.openPrivateChannel().join().getMessageById(messageId).join();

        startTime = (long) object.get("time-started");
        cookies = (double) object.get("cookies");

        cursors = Math.toIntExact((long)object.get("cursor"));
        grandmas = Math.toIntExact((long)object.get("grandma"));
        factories = Math.toIntExact((long)object.get("factory"));
        mines = Math.toIntExact((long)object.get("mine"));
        shipments = Math.toIntExact((long)object.get("shipment"));
        alchemyLabs = Math.toIntExact((long)object.get("alchemy lab"));
        portals = Math.toIntExact((long)object.get("portal"));
        timeMachines = Math.toIntExact((long)object.get("time machine"));

        updateCPS();
        for (long i = 0; i < (System.currentTimeMillis() / 1000) - (saveTime / 1000); i++) {
            updateCookies();
        }
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public void addCookie() {
        cookies += 1;
    }

    public void updateCookies() {
        cookies += cookiesPerSecond;
    }

    public void updateCPS() {
        double cps = 0.0;

        JSONObject cursor = (JSONObject) itemData.get("cursor");
        cps += (double) cursor.get("cps") * cursors;

        JSONObject grandma = (JSONObject) itemData.get("grandma");
        cps += (double) grandma.get("cps") * grandmas;

        JSONObject factory = (JSONObject) itemData.get("factory");
        cps += (double) factory.get("cps") * factories;

        JSONObject mine = (JSONObject) itemData.get("mine");
        cps += (double) mine.get("cps") * mines;

        JSONObject shipment = (JSONObject) itemData.get("shipment");
        cps += (double) shipment.get("cps") * shipments;

        JSONObject alchemyLab = (JSONObject) itemData.get("alchemy lab");
        cps += (double) alchemyLab.get("cps") * alchemyLabs;

        JSONObject portal = (JSONObject) itemData.get("portal");
        cps += (double) portal.get("cps") * portals;

        JSONObject timeMachine = (JSONObject) itemData.get("time machine");
        cps += (double) timeMachine.get("cps") * timeMachines;

        cookiesPerSecond = cps;
    }

    public int getAmountOwned(String item) {
        return switch (item.toLowerCase()) {
            case "cursor" -> cursors;
            case "grandma" -> grandmas;
            case "factory" -> factories;
            case "mine" -> mines;
            case "shipment" -> shipments;
            case "alchemy lab" -> alchemyLabs;
            case "portal" -> portals;
            case "time machine" -> timeMachines;
            default -> -1;
        };
    }

    public int getCost(String item, int amount) {
        int basePrice = 0;
        int numOwned = 0;
        switch (item.toLowerCase()) {
            case "cursor" -> {
                JSONObject cursor = (JSONObject) itemData.get("cursor");
                basePrice = Integer.parseInt(cursor.get("base-price").toString());
                numOwned = cursors;
            }
            case "grandma" -> {
                JSONObject grandma = (JSONObject) itemData.get("grandma");
                basePrice = Integer.parseInt(grandma.get("base-price").toString());
                numOwned = grandmas;
            }
            case "factory" -> {
                JSONObject factory = (JSONObject) itemData.get("factory");
                basePrice = Integer.parseInt(factory.get("base-price").toString());
                numOwned = factories;
            }
            case "mine" -> {
                JSONObject mine = (JSONObject) itemData.get("mine");
                basePrice = Integer.parseInt(mine.get("base-price").toString());
                numOwned = mines;
            }
            case "shipment" -> {
                JSONObject shipment = (JSONObject) itemData.get("shipment");
                basePrice = Integer.parseInt(shipment.get("base-price").toString());
                numOwned = shipments;
            }
            case "alchemy lab" -> {
                JSONObject alchemyLab = (JSONObject) itemData.get("alchemy lab");
                basePrice = Integer.parseInt(alchemyLab.get("base-price").toString());
                numOwned = alchemyLabs;
            }
            case "portal" -> {
                JSONObject portal = (JSONObject) itemData.get("portal");
                basePrice = Integer.parseInt(portal.get("base-price").toString());
                numOwned = portals;
            }
            case "time machine" -> {
                JSONObject timeMachine = (JSONObject) itemData.get("time machine");
                basePrice = Integer.parseInt(timeMachine.get("base-price").toString());
                numOwned = timeMachines;
            }
        }

        return (int) (Math.ceil(basePrice * Math.pow(1.1, amount + numOwned) / 0.1) - Math.ceil(basePrice * Math.pow(1.1, numOwned) / 0.1));
    }

    public boolean buy(String item, int amount) {
        if (getCookies() < getCost(item, amount)) {
            return false;
        }
        cookies -= getCost(item, amount);
        switch (item.toLowerCase()) {
            case "cursor" -> cursors += amount;
            case "grandma" -> grandmas += amount;
            case "factory" -> factories += amount;
            case "mine" -> mines++;
            case "shipment" -> shipments += amount;
            case "alchemy lab" -> alchemyLabs += amount;
            case "portal" -> portals++;
            case "time machine" -> timeMachines += amount;
        }
        updateCPS();
        return true;
    }

    public long getCookies() {
        return (long) Math.floor(cookies);
    }

    public double getCookiesPerSecond() {
        return cookiesPerSecond;
    }

    public EmbedBuilder toEmbedBuilder() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Cookie Clicker");
        eb.setColor(new Color(204, 204, 204));

        eb.setThumbnail("https://play-lh.googleusercontent.com/OssE3ON9WsLZedOF39UCgtIHcRYfV0OqQS9O78LfmRdxSyKdHX52G2OFa0LkG6D-k9w");

        eb.setDescription("Cookies: **" + String.format("%,d", getCookies()) + "**\n" +
                "Cookies/second: **" + round(cookiesPerSecond, 1) + "**");

        eb.addField("Buying Items", "Type /buy <item name> to buy an item");

        for (String building : buildings) {
            eb.addField(building + " (:cookie: " + String.format("%,d", getCost(building.toLowerCase(), 1)) + ")",
                    "*" + ((JSONObject) itemData.get(building.toLowerCase())).get("description").toString()
                            + " (" + ((JSONObject) itemData.get(building.toLowerCase())).get("cps").toString() + " CPS)" +
                            "*\n**" + getAmountOwned(building) + " owned.**");
        }

        eb.setFooter("Updates every time the cookie is clicked");

        return eb;
    }

    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();

        object.put("message-id", message.getId());
        object.put("time-started", startTime);
        object.put("cookies", round(cookies, 1));
        object.put("cursor", cursors);
        object.put("grandma", grandmas);
        object.put("factory", factories);
        object.put("mine", mines);
        object.put("shipment", shipments);
        object.put("alchemy lab", alchemyLabs);
        object.put("portal", portals);
        object.put("time machine", timeMachines);

        return object;
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public String toString() {
        return "cookies: " + getCookies() + ", " +
                "cps: " + getCookiesPerSecond() + ", " +
                "cursors: " + cursors + ", " +
                "grandmas: " + grandmas + ", " +
                "factories: " + factories + ", " +
                "mines: " + mines + ", " +
                "shipments: " + shipments + ", " +
                "alchemy labs: " + alchemyLabs + ", " +
                "portals: " + portals + ", " +
                "time machines" + timeMachines;
    }
}
