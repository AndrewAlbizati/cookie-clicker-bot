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

    private final User user;
    private final long startTime;
    private Message message;

    private double cookies;
    private double cookiesPerSecond;

    private long cursors;
    private long grandmas;
    private long factories;
    private long mines;
    private long shipments;
    private long alchemyLabs;
    private long portals;
    private long timeMachines;

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

        cursors = (long) object.get("cursor");
        grandmas = (long) object.get("grandma");
        factories = (long) object.get("factory");
        mines = (long) object.get("mine");
        shipments = (long) object.get("shipment");
        alchemyLabs = (long) object.get("alchemy lab");
        portals = (long) object.get("portal");
        timeMachines = (long) object.get("time machine");

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

    public long getAmountOwned(Items item) {
        return switch (item) {
            case CURSOR -> cursors;
            case GRANDMA -> grandmas;
            case FACTORY -> factories;
            case MINE -> mines;
            case SHIPMENT -> shipments;
            case ALCHEMY_LAB -> alchemyLabs;
            case PORTAL -> portals;
            case TIME_MACHINE -> timeMachines;
        };
    }

    public long getCost(Items item, long amount) {
        long numOwned = getAmountOwned(item);

        long basePrice = (long) ((JSONObject) itemData.get(item.toString().toLowerCase())).get("base-price");

        return (long) (Math.ceil(basePrice * Math.pow(1.1, amount + numOwned) / 0.1) - Math.ceil(basePrice * Math.pow(1.1, numOwned) / 0.1));
    }

    public boolean buy(Items item, long amount) {
        long cost = getCost(item, amount);
        if (getCookies() < cost) {
            return false;
        }

        cookies -= cost;
        switch (item) {
            case CURSOR -> cursors += amount;
            case GRANDMA -> grandmas += amount;
            case FACTORY -> factories += amount;
            case MINE -> mines += amount;
            case SHIPMENT -> shipments += amount;
            case ALCHEMY_LAB -> alchemyLabs += amount;
            case PORTAL -> portals += amount;
            case TIME_MACHINE -> timeMachines += amount;
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
                "Cookies/second: **" + String.format("%,.1f", round(cookiesPerSecond, 1)) + "**");

        eb.addField("Buying Items", "Type /buy <item name> to buy an item");

        for (Items item : Items.values()) {
            eb.addField(item + " (:cookie: " + String.format("%,d", getCost(item, 1)) + ")",
                    "*" + ((JSONObject) itemData.get(item.toString().toLowerCase())).get("description").toString()
                            + " (" + ((JSONObject) itemData.get(item.toString().toLowerCase())).get("cps").toString() + " CPS)" +
                            "*\n**" + getAmountOwned(item) + " owned.**");
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
