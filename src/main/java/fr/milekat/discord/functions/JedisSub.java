package fr.milekat.discord.functions;

import fr.milekat.discord.event.Bot_Chat;
import redis.clients.jedis.JedisPubSub;
import java.sql.SQLException;
import static fr.milekat.discord.Main.log;

public class JedisSub extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equalsIgnoreCase("discord")) {
            String[] msg = message.split("#:#");
            Bot_Chat bot_chat = new Bot_Chat();
            log("SUB:{"+channel+"},MSG:{"+message+"}");
            switch (msg[0].toLowerCase()) {
                case "new_msg":
                {
                    try {
                        bot_chat.newChat(Integer.parseInt(msg[1]));
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    break;
                }
                case "new_mp":
                {
                    try {
                        bot_chat.sendDiscordPrivate(Integer.parseInt(msg[1]));
                    } catch (SQLException e) {
                        log("Erreur dans l'envoi du message: " + e);
                        e.printStackTrace();
                    }
                    break;
                }
                case "join_notif":
                {
                    bot_chat.sendChatDiscord(":point_right: " + msg[1] + " a rejoint la cité :point_right:");
                    break;
                }
                case "quit_notif":
                {
                    bot_chat.sendChatDiscord(":wave: " + msg[1] + " a quitté la cité :wave:");
                    break;
                }
                case "log_sanction":
                {
                    Moderation moderation = new Moderation();
                    if (msg[1].equalsIgnoreCase("repport")) {
                        moderation.report(msg[2],msg[3],msg[4]);
                    } else if (msg[1].equalsIgnoreCase("ban")) {
                        moderation.ban(msg[2],msg[3],msg[4],msg[5]);
                    } else if (msg[1].equalsIgnoreCase("unban")) {
                        moderation.unban(msg[2],msg[3],msg[4]);
                    } else if (msg[1].equalsIgnoreCase("mute")){
                        moderation.mute(msg[2],msg[3],msg[4],msg[5]);
                    } else if (msg[1].equalsIgnoreCase("unmute")){
                        moderation.unmute(msg[2],msg[3],msg[4]);
                    }
                    break;
                }
                case "sqlbackup_done":
                {
                    if (msg.length==2) new SendNewSQLBackup(msg[1]);
                }
            }
        } else {
            log("PUB:{"+message+"}");
        }
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        log("Redis connecté à " + channel);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        log("Redis déconnecté de " + channel);
    }
}