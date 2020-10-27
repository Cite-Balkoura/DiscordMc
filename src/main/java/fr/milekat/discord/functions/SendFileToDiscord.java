package fr.milekat.discord.functions;

import fr.milekat.discord.Main;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.File;

import static fr.milekat.discord.Main.log;

public class SendFileToDiscord {
    public static void sendNewSQLBackup(String filename) {
        log("Nouveau backup reçu:" + filename + ".zip");
        MessageChannel backupchannel = Main.getBot().getTextChannelById(715850937515900999L);
        if (backupchannel == null) {
            Main.log("Erreur envoi fichier, channel inconnu");
            return;
        }
        backupchannel.sendFile(new File("saves/" + filename + ".zip")).content("Backup du " + filename).queue();
    }

    public static void sendDiscordFile(File file, long channelid) {
        if (file==null) {
            Main.log("Erreur envoi fichier, fichier null");
            return;
        }
        MessageChannel backupchannel = Main.getBot().getTextChannelById(channelid);
        if (backupchannel == null) {
            Main.log("Erreur envoi fichier, channel inconnu");
            return;
        }
        backupchannel.sendFile(file).content("Backup du " + file.getName()).queue(ignore -> file.delete());
    }
}
