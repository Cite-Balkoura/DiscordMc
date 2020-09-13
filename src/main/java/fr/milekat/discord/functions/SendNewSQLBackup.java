package fr.milekat.discord.functions;

import fr.milekat.discord.Main;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.File;

import static fr.milekat.discord.Main.log;

public class SendNewSQLBackup {
    public SendNewSQLBackup(String filename) {
        MessageChannel backupchannel = Main.getBot().getTextChannelById("715850937515900999");
        if (backupchannel ==null) return;
        log("Nouveau backup reçu:" + filename + ".zip");
        File file = new File("saves/" + filename + ".zip");
        backupchannel.sendFile(file).content("Backup du " + filename).queue();
    }
}
