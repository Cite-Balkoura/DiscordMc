package fr.milekat.discord.engines;

import fr.milekat.discord.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimerTask;

public class SQLPingLoad extends TimerTask {
    @Override
    public void run() {
        Connection connection = Main.getSqlConnect().getConnection();
        try {
            PreparedStatement q = connection.prepareStatement("SELECT `player_id` FROM `" + Main.SQLPREFIX +
                    "player` WHERE `player_id` = '1';");
            q.execute();
            q.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
