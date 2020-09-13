package fr.milekat.discord.functions;

import static fr.milekat.discord.Main.jedisPub;

public class JedisPub {
    /**
     *      Envoi d'un message sur le Redis
     * @param msg message à envoyer
     */
    public static void sendRedis(String msg){/*
        Jedis jedis = new Jedis("149.91.80.146", 6379);
        jedis.auth("aucyLUYyXkD67XPNFEdjpXfhBgqHvWLs9vn4vudytUSGPKZsvt");*/
        jedisPub.publish("discord", msg);
        /*jedis.quit();*/
    }
}