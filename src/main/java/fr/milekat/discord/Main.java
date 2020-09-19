package fr.milekat.discord;

import fr.milekat.discord.engines.Backup;
import fr.milekat.discord.engines.PlayersEngine;
import fr.milekat.discord.engines.SQLPingLoad;
import fr.milekat.discord.engines.TeamsEngine;
import fr.milekat.discord.event.BanChat;
import fr.milekat.discord.event.Bot_Chat;
import fr.milekat.discord.event.Inscription;
import fr.milekat.discord.functions.Console;
import fr.milekat.discord.functions.JedisSub;
import fr.milekat.discord.obj.Profil;
import fr.milekat.discord.obj.Team;
import fr.milekat.discord.utils.DateMilekat;
import fr.milekat.discord.utils.MariaManage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.UUID;

public class Main {
    private static MariaManage sql;
    private static JDA api;

    public static HashMap<Long, Profil> profilHashMap = new HashMap<>();
    public static HashMap<UUID, User> uuidUserHashMap = new HashMap<>();
    public static HashMap<Integer, Team> teams = new HashMap<>();
    public static String SQLPREFIX = "balkoura_";
    /* Pré-inscription */
    public static ArrayList<User> regEnCours = new ArrayList<>();
    public static HashMap<User, Integer> registerstep = new HashMap<>();
    public static HashMap<User, UUID> reguuid = new HashMap<>();
    public static HashMap<User, String> regmotivations = new HashMap<>();
    //  Empêche la lecture des msg de l'utilisateur (le temps que le bot parle)
    public static ArrayList<User> waitbot = new ArrayList<>();

    public static boolean debugMode;
    public static boolean enableBackups;

    public static ArrayList<String> commandes = new ArrayList<>();

    public static Jedis jedisPub;
    private static Jedis jedisSub;
    private static JedisSub subscriber;

    public static void main(String[] args) throws Exception {
        debugMode = true;
        log("Débugs activés '/debug' pour désactiver");
        enableBackups = true;
        log("Backups activés '/backup' pour désactiver");
        commandes.add("team");
        commandes.add("msg");
        // Discord
        api = JDABuilder.createDefault("NDg3MjcxODM0ODMyNzMyMTYx.XpcgLg.SmzvIo3KZhK4xCZhawYX8HPVr3o").build().awaitReady();
        api.getPresence().setPresence(OnlineStatus.ONLINE,Activity.watching("web.cite-balkoura.fr"));
        // SQL
        sql = new MariaManage("jdbc:mysql://",
                "149.91.80.146",
                "minecraft",
                "minecraft",
                "aucyLUYyXkD67XPNFEdjpXfhBgqHvWLs9vn4vudytUSGPKZsvt");
        sql.connection();
        // Jedis
        jedisPub = new Jedis("149.91.80.146",6379,0);
        jedisPub.auth("aucyLUYyXkD67XPNFEdjpXfhBgqHvWLs9vn4vudytUSGPKZsvt");
        jedisSub = new Jedis("149.91.80.146",6379,0);
        jedisSub.auth("aucyLUYyXkD67XPNFEdjpXfhBgqHvWLs9vn4vudytUSGPKZsvt");
        subscriber = new JedisSub();
        new Thread("Redis-Discord-Sub") {
            @Override
            public void run() {
                try {
                    jedisSub.subscribe(subscriber,"backup", "discord", "cite", "event", "survie", "bungee");
                } catch (Exception e) {
                    log("Subscribing failed." + e);
                    e.printStackTrace();
                }
            }
        }.start();
        // Routines
        new Thread("SQL-Things") {
            @Override
            public void run() {
                Timer sqlLoader = new Timer();
                sqlLoader.schedule(new SQLPingLoad(),0,600000);
            }
        }.start();
        new Thread("Players-Engine") {
            @Override
            public void run() {
                Timer teamsEngine = new Timer();
                teamsEngine.schedule(new TeamsEngine(),0,60000);
                Timer playerEngine = new Timer();
                playerEngine.schedule(new PlayersEngine(),0,60000);
            }
        }.start();
        new Thread("Backup-Engine") {
            @Override
            public void run() {
                Timer backuptimer = new Timer();
                backuptimer.schedule(new Backup(),300000,7200000);
            }
        }.start();
        // Event
        api.addEventListener(new Bot_Chat());
        api.addEventListener(new Inscription());
        api.addEventListener(new BanChat());
        // Chargement de la console
        new Thread("Console") {
            @Override
            public void run() {
                new Console();
            }
        }.start();
        // Log
        log("Load du bot terminé.");
        for (Profil profil : Main.profilHashMap.values()) {
            Main.log(profil.getDiscordid() + "");
        }
    }

    public static MariaManage getSqlConnect(){
        return sql;
    }

    public static JDA getBot(){
        return api;
    }

    public static void log(String log){
        System.out.println("[" + DateMilekat.setDateNow() + "] " + log);
    }

    public static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Main.log("Erreur de delay:" + delay);
            e.printStackTrace();
        }
    }

    public static void debug(String msg) {
        if (debugMode) log("[DEBUG] " + msg);
    }

    public static void stopSequence() {
        log("Déconnexion du bot...");
        api.getPresence().setStatus(OnlineStatus.OFFLINE);
        log("Good bye!");
        System.exit(0);
    }
}