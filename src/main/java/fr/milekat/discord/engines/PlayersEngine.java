package fr.milekat.discord.engines;

import fr.milekat.discord.Main;
import fr.milekat.discord.obj.Profil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;

public class PlayersEngine extends TimerTask {
    private final JDA api = Main.getBot();
    private final Guild server = api.getGuildById(554074223870738433L);
    private final Role teamrole = api.getRoleById(712716246432350208L);
    private final Role validrole = api.getRoleById(554076272557555773L);
    private final Role banrole = api.getRoleById(554103808725221376L);
    private final Role muterole = api.getRoleById(712715689357475856L);

    @Override
    public void run() {
        Connection connection = Main.getSqlConnect().getConnection();
        try {
            PreparedStatement q = connection.prepareStatement("SELECT `uuid`, `name`, `team_id`, " +
                    "`chat_mode`, `muted`, `banned`, `reason`, `modson`, `player_pts_event`, `maintenance`, " +
                    "`discord_id` FROM `" + Main.SQLPREFIX + "player` WHERE `name` != 'Annonce';");
            q.execute();
            Main.profilHashMap.clear();
            Main.uuidUserHashMap.clear();
            while (q.getResultSet().next()){
                 UUID uuid = UUID.fromString(q.getResultSet().getString("uuid"));
                 long discordid = q.getResultSet().getLong("discord_id");
                 Profil profil = new Profil(uuid,
                                q.getResultSet().getString("name"),
                                q.getResultSet().getInt("team_id"),
                                q.getResultSet().getInt("chat_mode"),
                                q.getResultSet().getString("muted"),
                                q.getResultSet().getString("banned"),
                                q.getResultSet().getString("reason"),
                                q.getResultSet().getBoolean("modson"),
                                q.getResultSet().getInt("player_pts_event"),
                                q.getResultSet().getBoolean("maintenance"),
                                discordid);
                Main.profilHashMap.put(discordid, profil);
                assert server != null;
                assert muterole != null;
                assert banrole != null;
                assert teamrole != null;
                assert validrole != null;
                server.retrieveMemberById(q.getResultSet().getLong("discord_id")).queue(member -> {
                    try {
                        List<Role> roles = member.getRoles();
                        Main.uuidUserHashMap.put(uuid, member.getUser());
                        // Check Mute
                        if (profil.isMute()) {
                            if (!roles.contains(muterole)) {
                                server.addRoleToMember(member, muterole).queue();
                            }
                        } else {
                            if (roles.contains(muterole)) {
                                server.removeRoleFromMember(member, muterole).queue();
                            }
                        }
                        // Check Ban
                        if (profil.isBan()) {
                            if (!roles.contains(banrole)) server.addRoleToMember(member, banrole).queue();
                            if (roles.contains(teamrole)) server.removeRoleFromMember(member, teamrole).queue();
                            if (roles.contains(validrole)) server.removeRoleFromMember(member, validrole).queue();
                        } else {
                            if (roles.contains(banrole)) server.removeRoleFromMember(member, banrole).queue();
                            if (profil.getTeam() > 0 && !roles.contains(validrole))
                                server.addRoleToMember(member, validrole).queue();
                            if (profil.getTeam() > 0 && roles.contains(teamrole))
                                server.removeRoleFromMember(member, teamrole).queue();
                            if (profil.getTeam() == 0 && !roles.contains(teamrole))
                                server.addRoleToMember(member, teamrole).queue();
                            if (profil.getTeam() == 0 && roles.contains(validrole))
                                server.removeRoleFromMember(member, validrole).queue();
                        }
                        if (!member.getEffectiveName().equalsIgnoreCase(profil.getName())) {
                            member.modifyNickname(profil.getName()).queue();
                        }
                    } catch (HierarchyException ignore) {}
                }, ignore -> {/* Si le member est introuvable ou autre on ignore ! */});
            }
            q.close();
        } catch (SQLException throwables) {
            Main.log("Impossible d'update la liste des joueurs !");
            throwables.printStackTrace();
        }
    }
}
