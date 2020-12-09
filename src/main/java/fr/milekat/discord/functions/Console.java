package fr.milekat.discord.functions;

import fr.milekat.discord.Main;

import java.util.Scanner;

public class Console {
    public Console() {
        try (Scanner scanner = new Scanner(System.in)) {
            while(true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("stop")) {
                    Main.stopSequence();
                } else if (input.equalsIgnoreCase("debug")) {
                    debug();
                } else if (input.equalsIgnoreCase("jedis")) {
                    jedis();
                } else if (input.equalsIgnoreCase("backups")) {
                    backups();
                } else if (input.equalsIgnoreCase("savefile")) {
                    backups();
                } else {
                    Main.log("Commande inconnue");
                    sendHelp();
                }
            }
        }
    }

    /**
     *      Liste des commandes dispo pour la console du bot
     */
    private void sendHelp() {
        Main.log("debug: Active/Désactive le débug.");
        Main.log("backups: Active/Désactive les backups auto.");
        Main.log("savefile <path>: Save un dossier complet en zip envoyé sur Discord.");
        Main.log("stop: Stop le bot !");
    }

    private void debug() {
        Main.debugMode = !Main.debugMode;
        Main.log("Mode débug: " + Main.debugMode + ".");
    }

    private void jedis() {
        Main.jedisDebug = !Main.jedisDebug;
        Main.log("Mode débug jedis: " + Main.jedisDebug + ".");
    }

    private void backups() {
        Main.enableBackups = !Main.enableBackups;
        Main.log("Backups: " + Main.enableBackups + ".");
    }
}
