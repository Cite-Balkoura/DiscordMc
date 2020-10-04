package fr.milekat.discord.functions;

import static fr.milekat.discord.Main.jedisPub;

public class JedisPub {
    /**
     *      Envoi d'un message sur le Redis
     * @param msg message à envoyer
     */
    public static void sendRedis(String msg){
        jedisPub.publish("discord", msg);
    }
}