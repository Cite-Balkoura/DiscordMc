package fr.milekat.discord.engines;

import fr.milekat.discord.Main;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimerTask;

public class InscriptionsCount extends TimerTask {
    private final JDA api = Main.getBot();
    private final VoiceChannel channel = api.getVoiceChannelById(761247631950610473L);

    @Override
    public void run() {
        Connection connection = Main.getSqlConnect().getConnection();
        try {
            PreparedStatement q = connection.prepareStatement("SELECT COUNT(`player_id`) as player_count" +
                    " FROM `balkoura_player` WHERE `team_id` <> 1;");
            q.execute();
            q.getResultSet().last();
            if (channel!=null) channel.getManager()
                    .setName(q.getResultSet().getString("player_count") + " participants validés")
                    .queue();
            q.close();
        } catch (SQLException throwables) {
            Main.log("Impossible d'update le channel des inscrits!");
            throwables.printStackTrace();
        }
    }
}
