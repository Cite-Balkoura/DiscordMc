package fr.milekat.discord.engines;

import fr.milekat.discord.Main;
import fr.milekat.discord.functions.JedisPub;
import fr.milekat.discord.utils.DateMilekat;

import java.util.TimerTask;

public class Backup extends TimerTask {
    @Override
    public void run() {
        if (Main.enableBackups) {
            JedisPub.sendRedis("sqlbackup_create#:#" + DateMilekat.setDatesysNow());
        } else {
            Main.log("Attention, backups SQL désactivés, backup skip.");
        }
    }
}
