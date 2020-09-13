package fr.milekat.discord.engines;

import fr.milekat.discord.Main;
import fr.milekat.discord.obj.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;

public class TeamsEngine extends TimerTask {
    @Override
    public void run() {
        Connection connection = Main.getSqlConnect().getConnection();
        try {
            PreparedStatement q = connection.prepareStatement("SELECT a.*, c.* FROM `" + Main.SQLPREFIX + "team` as a " +
                    "LEFT JOIN (SELECT `team_id`, COUNT(`player_id`) as memcount, GROUP_CONCAT(`name`) as members " +
                    "FROM `" + Main.SQLPREFIX + "player` GROUP BY team_id) AS c ON a.`team_id` = c.`team_id`;");
            q.execute();
            Main.teams.clear();
            while (q.getResultSet().next()) {
                int count = 0;
                ArrayList<String> members = null;
                if (!(q.getResultSet().getString("memcount")==null)) {
                    count = q.getResultSet().getInt("memcount");
                    members = new ArrayList<>(Arrays.asList(q.getResultSet().getString("members").split(",")));
                }
                Team team = new Team(q.getResultSet().getInt("team_id"),
                        q.getResultSet().getString("team_name"),
                        q.getResultSet().getString("team_tag"),
                        q.getResultSet().getInt("money"),
                        members,
                        count);
                Main.teams.put(q.getResultSet().getInt("team_id"),team);
            }
            q.close();
        } catch (SQLException throwables) {
            Main.log("Impossible d'update la liste des teams !");
            throwables.printStackTrace();
        }
    }
}
