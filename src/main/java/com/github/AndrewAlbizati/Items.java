package com.github.AndrewAlbizati;

public enum Items {
    CURSOR, GRANDMA, FACTORY, MINE, SHIPMENT, ALCHEMY_LAB, PORTAL, TIME_MACHINE;

    public String toString() {
        return capitalize(this.name().replaceAll("_", " "));
    }

    private static String capitalize(String s) {
        StringBuilder sb = new StringBuilder();
        for (String s1 : s.split(" ")) {
            sb.append(s1.substring(0, 1).toUpperCase() + s1.substring(1).toLowerCase());
            sb.append(" ");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static Items stringToItem(String item) {
        return switch (item.toLowerCase()) {
            case "cursor" -> CURSOR;
            case "grandma" -> GRANDMA;
            case "factory" -> FACTORY;
            case "mine" -> MINE;
            case "shipment" -> SHIPMENT;
            case "alchemy lab" -> ALCHEMY_LAB;
            case "portal" -> PORTAL;
            case "time machine" -> TIME_MACHINE;
            default -> null;
        };
    }
}
