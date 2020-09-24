package fr.milekat.discord.event;

import fr.milekat.discord.Main;
import fr.milekat.discord.functions.JedisPub;
import fr.milekat.discord.utils.ChatColor;
import fr.milekat.discord.utils.DateMilekat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static fr.milekat.discord.Main.*;

public class BotChat extends ListenerAdapter {
    private final JDA api = Main.getBot();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return;
        if (!Main.profilHashMap.containsKey(event.getAuthor().getIdLong())) return;
        if (event.getChannel().equals(chatchannel)){
            if (!event.getMessage().getAuthor().isBot()){
                event.getMessage().delete().queue();
                UUID uuid = Main.profilHashMap.get(event.getAuthor().getIdLong()).getUuid();
                if (Main.profilHashMap.get(event.getAuthor().getIdLong()).isMute()) {
                    event.getAuthor().openPrivateChannel().complete().sendMessage(
                            "Impossible d'envoyer un message, vous êtes mute.").queue();
                    return;
                }
                // Injection SQL
                int id = insertSQLNewChat(uuid, event.getMessage().getContentRaw());
                // Envoi serveur
                try {
                    newChat(id);
                } catch (SQLException e) {
                    Main.log("Impossible d'envoyer le message sur Discord.");
                    e.printStackTrace();
                }
                // Envoi REDIS
                JedisPub.sendRedis("new_msg#:#" + id);
            }
        } if (event.getChannel().getType().equals(ChannelType.PRIVATE)) {
            // Récupération commande
            String[] message = event.getMessage().getContentRaw().split(" ");
            if (message[0].equalsIgnoreCase("msg") && message.length < 3) {
                event.getChannel().sendMessage("Merci d'utiliser **msg <joueur> <message>** pour envoyer un msg privé.")
                        .queue();
                return;
            } else if (!commandes.contains(message[0])) {
                event.getChannel().sendMessage("Commande inconnue").queue();
                sendHelp(event.getChannel());
                return;
            }
            if (Main.profilHashMap.get(event.getAuthor().getIdLong()).isMute()) {
                event.getAuthor().openPrivateChannel().complete().sendMessage(
                        "Impossible d'envoyer un message, vous êtes mute.").queue();
                debug(event.getAuthor().getAsTag() + " tentative de MP alors que mute.");
                return;
            }
            Connection sql = Main.getSqlConnect().getConnection();
            debug("Nouveau mp de " + event.getAuthor().getAsTag());
            try {
                PreparedStatement q = sql.prepareStatement("SELECT `uuid` FROM `" + Main.SQLPREFIX +
                        "player` WHERE SOUNDEX(`name`) = SOUNDEX('" + message[1] + "')");
                q.execute();
                if (!q.getResultSet().last()) {
                    event.getChannel().sendMessage("Le joueur est introuvable.").queue();
                    q.close();
                    debug("Destinataire introuvable.");
                    return;
                }
                q.getResultSet().last();
                StringBuilder sb = new StringBuilder();
                for (String loop : message) {
                    if (!loop.equals(message[0]) && !loop.equals(message[1])) {
                        sb.append(loop);
                        sb.append(" ");
                    }
                }
                UUID uuid = Main.profilHashMap.getOrDefault(event.getAuthor().getIdLong(),null).getUuid();
                if (uuid==null) {
                    log("Erreur MP, le profil n'existe pas (UUID NULL).");
                    return;
                }
                // Injection SQL
                int id = insertSQLNewPrivate(uuid, UUID.fromString(q.getResultSet().getString("uuid")),
                        sb.toString());
                q.close();
                // Envoi serveur
                try {
                    sendDiscordPrivate(id);
                } catch (SQLException e) {
                    Main.log("Impossible d'envoyer le message-2 sur Discord.");
                    e.printStackTrace();
                }
                // Envoi REDIS
                JedisPub.sendRedis("new_mp#:#" + id);
            } catch (SQLException e) {
                Main.log("Impossible d'envoyer le message-3 sur Discord.");
                e.printStackTrace();
            }
        }
    }

    /**
     *      Insert du nvx msg dans la bdd
     * @param uuid joueur
     * @param msg message
     * @return id du message
     */
    private int insertSQLNewChat(UUID uuid, String msg){
        Connection connection = Main.getSqlConnect().getConnection();
        int id = 0;
        try {
            PreparedStatement q = connection.prepareStatement("INSERT INTO `" + Main.SQLPREFIX + "chat`" +
                    "(`player_id`, `msg`, `date_msg`, `msg_type`) VALUES ((SELECT player_id FROM `" + Main.SQLPREFIX +
                    "player` WHERE `uuid` = '"+ uuid +"'), ? ,'"+ DateMilekat.setDateNow()+"', '3') RETURNING msg_id;");
            q.setString(1,msg);
            q.execute();
            q.getResultSet().next();
            id = q.getResultSet().getInt(1);
            q.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Main.log("Erreur lors de l'injection d'un message dans le SQL.");
        }
        return id;
    }

    /**
     *      Envoi d'un message sur le chanel "554088761584123905"
     * @param id id à envoyer
     */
    public void newChat(int id) throws SQLException {
        PreparedStatement q = getChatId(id);
        if (q == null) {
            Main.log("Erreur SQL ?");
            return;
        }
        q.getResultSet().last();
        int type = q.getResultSet().getInt("msg_type");
        if (type==1 || type==3) {
            sendChatDiscord("**" + q.getResultSet().getString("prefix") +
                    q.getResultSet().getString("name") + " »** " +
                    ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',q.getResultSet().getString("msg"))));
        } else if (type==4) {
            sendChatDiscord("**" + q.getResultSet().getString("prefix") +
                    q.getResultSet().getString("name") + " »** " +
                    ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                            q.getResultSet().getString("msg").replaceAll("%nl%", System.lineSeparator()))));

        }
        q.close();
    }

    /**
     *      Injection SQL d'un msg privé envoyé depuis discord
     * @param uuid du sender
     * @param dest_uuid du dest
     * @param msg message envoyé
     * @return id du message
     */
    private int insertSQLNewPrivate(UUID uuid,UUID dest_uuid , String msg){
        Connection sql = Main.getSqlConnect().getConnection();
        int id = 0;
        try {
            PreparedStatement q = sql.prepareStatement(
                    "INSERT INTO `" + Main.SQLPREFIX + "chat`(`player_id`, `msg`, `date_msg`, `msg_type`, `dest_id`) VALUES " +
                            "((SELECT player_id FROM `" + Main.SQLPREFIX + "player` WHERE `uuid` = '"+ uuid +"'), ? , ? , '2', " +
                            "(SELECT player_id FROM `" + Main.SQLPREFIX + "player` WHERE `uuid` = '"+ dest_uuid +"')) " +
                            "RETURNING msg_id;");
            q.setString(1,msg);
            q.setString(2,DateMilekat.setDateNow());
            q.execute();
            q.getResultSet().next();
            id = q.getResultSet().getInt(1);
            q.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Main.log("Erreur lors de l'injection d'un message dans le SQL.");
        }
        return id;
    }

    /**
     *      Envoi du msg sur le canal Mc
     * @param msg à envoyer
     */
    public void sendChatDiscord(String msg){
        assert chatchannel != null;
        chatchannel.sendMessage(msg).queue();
        Main.log(msg);
    }

    /**
     *      Envoie d'un mp
     * @param id du message à envoyer
     */
    public void sendDiscordPrivate(int id) throws SQLException {
        PreparedStatement q = getChatId(id);
        if (q == null) {
            Main.log("Erreur SQL ?");
            return;
        }
        q.getResultSet().last();
        if (q.getResultSet().getString("discord_id")!=null){
            privateFormat(q.getResultSet().getString("discord_id"),
                    q.getResultSet().getString("dest_name"),
                    ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',q.getResultSet().getString("msg"))),
                    true);
        }
        if (q.getResultSet().getString("dest_discord_id")!=null){
            privateFormat(q.getResultSet().getString("dest_discord_id"),
                    q.getResultSet().getString("name"),
                    ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',q.getResultSet().getString("msg"))),
                    false);
        }
    }

    /**
     *      Formatage msg privé + envoi à l'user (id)
     * @param id user à envoyé
     * @param sender qui a envoyé le MP
     * @param msg le message
     */
    private void privateFormat(String id,String sender, String msg, boolean isSender) {
        if (isSender) {
            msg = "[Moi > "+ sender + "] " + msg;
        } else {
            msg = "[" + sender + " > Moi] " +  msg;
        }
        api.openPrivateChannelById(id).complete().sendMessage(msg).queue();
    }

    /**
     *      Récupération d'un message avec son ID
     * @param id du message à récupérer
     * @return requête SQL
     */
    private PreparedStatement getChatId(int id){
        Connection connection = Main.getSqlConnect().getConnection();
        String query = "SELECT chat.msg_id as msg_id, chat.msg as msg, chat.msg_type as msg_type, chat.date_msg as date_msg, " +
                //-- Sender + dest info
                "sender.name, sender.uuid, sender.discord_id, dest.name as dest_name, dest.uuid as dest_uuid, " +
                "dest.discord_id as dest_discord_id, COALESCE(CONCAT(senderLG.grank, ' '), '') as prefix " +
                "FROM " + Main.SQLPREFIX + "chat chat " +
                "LEFT JOIN " + Main.SQLPREFIX + "player sender ON chat.player_id = sender.player_id " +
                "LEFT JOIN " + Main.SQLPREFIX + "player dest ON chat.dest_id = dest.player_id " +
                "LEFT JOIN luckperms_players senderLP ON sender.uuid = senderLP.uuid " +
                "LEFT JOIN luckperms_groups senderLG ON senderLP.primary_group = senderLG.name " +
                "WHERE chat.msg_id = ? ORDER BY chat.msg_id ASC;";
        try {
            PreparedStatement q = connection.prepareStatement(query);
            q.setInt(1, id);
            q.execute();
            return q;
        } catch (SQLException e) {
            Main.log("Erreur lors de la récupération du message : " + id + ".");
            e.printStackTrace();
        }
        return null;
    }

    private void sendHelp(MessageChannel channel) {
        TextChannel rechercheteam = api.getTextChannelById(712719477414035536L);
        channel.sendMessage("Pour envoyer un MP sur minecraft **msg <joueur> <message>**.").queue();
        channel.sendMessage("Pour créer une équipe: **team create <nom de team>** (Action définitive).").queue();
        assert rechercheteam != null;
        channel.sendMessage("Pour inviter un joueur: **team invite <@Mention>** dans " +
                rechercheteam.getAsMention() + " (Attention vous partagerez tout !)").queue();    }
}