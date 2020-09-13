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
                } else if (input.equalsIgnoreCase("backups")) {
                    backups();
                } else {
                    Main.log("Commande inconnue");
                }
            }
        }
    }

    private void debug() {
        Main.debugMode = !Main.debugMode;
        Main.log("Mode débug: " + Main.debugMode + ".");
    }

    private void backups() {
        Main.enableBackups = !Main.enableBackups;
        Main.log("Backups: " + Main.enableBackups + ".");
    }
}
