package fr.milekat.discord.functions;

import fr.milekat.discord.Main;
import fr.milekat.discord.utils.DateMilekat;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

public class Moderation {
    private final JDA api = Main.getBot();
    private final Guild server = api.getGuildById(554074223870738433L);
    private final MessageChannel logchannel = Main.getBot().getTextChannelById(703601685041250325L);
    private final Category bancategory = api.getCategoryById(554104234442883094L);
    private final Role teamrole = api.getRoleById(712716246432350208L);
    private final Role validrole = api.getRoleById(554076272557555773L);
    private final Role banrole = api.getRoleById(554103808725221376L);
    private final Role muterole = api.getRoleById(712715689357475856L);

    /**
     *      Arrivée d'une nouvelle sanction, récupération du membre "Ciblé" et du "Modo" qui a fait la sanction
     */
    public void newSanction(String action, String target, String modo, String duree, String expiration, String raison, String cmd) {
        if (server==null) {
            Main.log("Erreur de récupération du serveur.");
            return;
        }
        if (duree.equalsIgnoreCase("def")) {
            duree = "Définitif";
            expiration = "Jusqu'à décision du staff";
        }
        String finalDuree = duree;
        String finalExpiration = expiration;
        server.retrieveMemberById(target).queue(targetMember -> {
            if (modo.equalsIgnoreCase("console")) {
                doSanction(action, targetMember, server.getSelfMember(), finalDuree, finalExpiration, raison, cmd);
            } else {
                server.retrieveMemberById(modo).queue(modoMember ->
                        doSanction(action, targetMember, modoMember, finalDuree, finalExpiration, raison, cmd));
            }
        });
    }

    /**
     *      Dispatch en fonction du type d'action
     */
    private void doSanction(String action, Member targetMember, Member modoMember, String duree, String expiration, String raison, String cmd) {
        if (action.equalsIgnoreCase("repport")) {
            report(targetMember,modoMember,raison, cmd);
        } else if (action.equalsIgnoreCase("ban")) {
            ban(targetMember,modoMember,duree,expiration,raison, cmd);
        } else if (action.equalsIgnoreCase("unban")) {
            unban(targetMember,modoMember,raison, cmd);
        } else if (action.equalsIgnoreCase("mute")){
            mute(targetMember,modoMember,duree,expiration,raison, cmd);
        } else if (action.equalsIgnoreCase("unmute")){
            unmute(targetMember,modoMember,raison, cmd);
        } else if (action.equalsIgnoreCase("kick")) {
            kick(targetMember,modoMember, raison, cmd);
        }
    }

    /**
     *      Log de report
     */
    private void report(Member target, Member modo, String raison, String cmd) {
        sendLogDiscord("Report", target, modo, null, null, raison);
        logSQLSanction(7,target,modo,raison,cmd);
    }

    /**
     *      Log de ban
     */
    private void ban(Member target, Member modo, String duree, String expiration, String raison, String cmd) {
        if (banrole==null || server==null || bancategory==null || teamrole==null || validrole==null) return;
        TextChannel newBanChannel = null;
        String chname = "ban-" + target.getEffectiveName().toLowerCase().replaceAll(" ","-");
        chname = chname.replaceAll("[^a-zA-Z0-9-]", "");
        for (TextChannel loopchannel : bancategory.getTextChannels()) {
            if (loopchannel.getName().equalsIgnoreCase(chname)) {
                newBanChannel = loopchannel;
                setBanchannel(newBanChannel, target, modo, duree, expiration, raison);
                break;
            }
        }
        if (newBanChannel==null) {
            server.createTextChannel(chname).setParent(bancategory).queue(channel ->
                    setBanchannel(channel, target, modo, duree, expiration, raison));
        }
        sendLogDiscord("Ban",target,modo,duree,expiration,raison);
        if (target.getRoles().contains(teamrole)) server.removeRoleFromMember(target,teamrole).queue();
        if (target.getRoles().contains(validrole)) server.removeRoleFromMember(target,validrole).queue();
        if (duree.equalsIgnoreCase("définitif")) {
            logSQLSanction(4,target,modo,raison,cmd);
        } else logSQLSanction(5,target,modo,raison,cmd);
    }

    /**
     *      Le channel est créé, définition des permissions du salon + send du premier msg
     */
    private void setBanchannel(TextChannel banchannel, Member target, Member modo, String duree, String expiration, String raison) {
        try {
            banchannel.getPermissionOverrides().clear();
            if (bancategory == null) return;
            banchannel.getManager().sync(bancategory).queue();
            banchannel.createPermissionOverride(target).clear().setAllow(Permission.VIEW_CHANNEL).queue();
        } catch (UnsupportedOperationException | IllegalStateException ignored) {}
            banchannel.getManager().setTopic("Canal privé suite au bannissement de " + target.getEffectiveName() +
                    ", par " + modo.getEffectiveName()).queue();
            banchannel.sendMessage("Bonjour " + target.getAsMention() + " ! Tu as été banni(e) par: " + modo.getAsMention() +
                    "." + "Tu peux essayer de t'expliquer avec le staff." + System.lineSeparator() +
                    "Si tu quittes ce discord, tu seras banni(e) automatiquement, à vie. Cette conversation est sauvegardée."
                    + System.lineSeparator() + System.lineSeparator() +
                    "**Raison:** " + raison + System.lineSeparator() +
                    "**Durée:** " + duree + System.lineSeparator() +
                    "**Expire:** " + expiration).queue();
    }

    /**
     *      Log d'unban + remove du salon + restoration des permissions de l'user
     */
    private void unban(Member target, Member modo, String raison, String cmd) {
        if (banrole==null || server==null || teamrole==null || validrole==null || bancategory==null) return;
        String chname = "ban-" + target.getEffectiveName().toLowerCase().replaceAll(" ","-");
        chname = chname.replaceAll("[^a-zA-Z0-9-]", "");
        server.removeRoleFromMember(target, banrole).queue();
        for (TextChannel loopchannel : bancategory.getTextChannels()) {
            if (loopchannel.getName().equalsIgnoreCase(chname)) {
                loopchannel.delete().queue();
            }
        }
        target.getUser().openPrivateChannel().queue(dm -> {
            dm.sendMessage("Votre banissement a pris fin, vous pouvez dès à présent revenir sur la cité.").queue();
            dm.sendMessage("IP du serveur: mc.cite-balkoura.fr").queue();
            sendLogDiscord("UnBan",target,modo,null,null,raison);
            if (Main.profilHashMap.get(target.getIdLong()).getTeam()>0) server.addRoleToMember(target, validrole).queue();
            if (Main.profilHashMap.get(target.getIdLong()).getTeam()==0) server.addRoleToMember(target, teamrole).queue();
        });
        logSQLSanction(6,target,modo,raison,cmd);
    }

    /**
     *      Log de mute
     */
    private void mute(Member target, Member modo, String duree, String expiration, String raison, String cmd) {
        if (muterole == null || server == null) return;
        server.addRoleToMember(target, muterole).queue();
        sendLogDiscord("Mute",target, modo,duree,expiration,raison);
        logSQLSanction(0,target,modo,raison,cmd);
    }

    /**
     *      Log d'unmute
     */
    private void unmute(Member target, Member modo, String raison, String cmd) {
        if (muterole == null || server == null) return;
        server.removeRoleFromMember(target, muterole).queue();
        target.getUser().openPrivateChannel().queue(dm ->
                dm.sendMessage("Votre banissement a pris fin, vous pouvez dès à présent revenir sur la cité.").queue());
        sendLogDiscord("UnMute",target, modo,null,null,raison);
        logSQLSanction(1,target,modo,raison,cmd);
    }

    /**
     *      Log d'un kick
     */
    private void kick(Member target, Member modo, String raison, String cmd) {
        sendLogDiscord("Kick",target, modo,null,null,raison);
        logSQLSanction(3,target,modo,raison,cmd);
    }

    /**
     *      Envoi d'un Log sur le cannal de log
     */
    private void sendLogDiscord(String sanction, Member target, Member modo, String duree, String expiration, String raison) {
        if (logchannel==null) return;
        EmbedBuilder Sanction = new EmbedBuilder();
        Sanction.setTitle("Nouveau log")
                .setColor(Color.RED)
                .addField(":bust_in_silhouette: Joueur sanctionné", target.getAsMention(),true)
                .addField(":police_officer: Modérateur", modo.getAsMention(),true)
                .addField(":hammer_pick: Action", sanction,true)
                .setTimestamp(new Date().toInstant());
        if (duree!=null && expiration!=null) {
            Sanction.addField(":stopwatch: Durée", duree + System.lineSeparator() + expiration, true);
        }
        Sanction.addField(":label: Raison", raison,true);
        logchannel.sendMessage(Sanction.build()).queue();
    }

    /**
     *      Log des sanctions dans le SQL
     */
    private void logSQLSanction(Integer sanction, Member target, Member modo, String raison, String cmd) {
        Connection connection = Main.getSqlConnect().getConnection();
        try {
            PreparedStatement q = connection.prepareStatement("INSERT INTO `balkoura_sanction`" +
                    "(`player_id`, `sanction_date`, `cmd`, `sanction_type`, `send_by`, `sanction_reason`) VALUES " +
                    "(COALESCE((SELECT `player_id` FROM `balkoura_player` WHERE `discord_id` = '" + target.getId() + "'),'??'),?,?,?," +
                    "COALESCE((SELECT `player_id` FROM `balkoura_player` WHERE `discord_id` = '" + modo.getId() + "'),'Console'),?);");
            q.setString(1, DateMilekat.setDateNow());
            q.setString(2, cmd);
            q.setInt(3, sanction);
            q.setString(4, raison);
            q.execute();
            q.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
