package fr.milekat.discord;

import fr.milekat.discord.engines.*;
import fr.milekat.discord.event.BanChat;
import fr.milekat.discord.event.BotChat;
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
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import redis.clients.jedis.Jedis;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class Main {
    private static MariaManage sql;
    private static JDA api;
    private static JSONObject jsonO;

    /* Core */
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

    public static TextChannel chatchannel;

    public static boolean debugMode;
    public static boolean jedisDebug;
    public static boolean enableBackups;

    public static ArrayList<String> commandes = new ArrayList<>();

    public static Jedis jedisPub;
    private static Jedis jedisSub;
    private static JedisSub subscriber;

    public static void main(String[] args) throws Exception {
        debugMode = true;
        log("Débugs activés '/debug' pour désactiver");
        enableBackups = true;
        log("Backups activés '/backups' pour désactiver");
        commandes.add("team");
        commandes.add("msg");
        // JSON
        JSONParser jsonP = new JSONParser();
        jsonO = (JSONObject) jsonP.parse(new FileReader("config.json"));
        // Discord
        api = JDABuilder.createDefault((String) getConfig().get("bot_token")).build().awaitReady();
        api.getPresence().setPresence(OnlineStatus.ONLINE,Activity.watching("web.cite-balkoura.fr"));
        Main.chatchannel = api.getTextChannelById(554088761584123905L);
        if (new Date().getTime() >
                new GregorianCalendar(2020, Calendar.DECEMBER,19,14,0).getTimeInMillis()) {
            Main.chatchannel = api.getTextChannelById(754764155508228277L);
        }
        // SQL
        JSONObject sqlconfig = (JSONObject) getConfig().get("SQL");
        sql = new MariaManage("jdbc:mysql://",
                (String) sqlconfig.get("host"),
                (String) sqlconfig.get("db"),
                (String) sqlconfig.get("user"),
                (String) sqlconfig.get("mdp"));
        sql.connection();
        // Jedis
        JSONObject redisconfig = (JSONObject) getConfig().get("redis");
        jedisPub = new Jedis((String) redisconfig.get("host"),6379,0);
        jedisPub.auth((String) redisconfig.get("auth"));
        jedisSub = new Jedis((String) redisconfig.get("host"),6379,0);
        jedisSub.auth((String) redisconfig.get("auth"));
        subscriber = new JedisSub();
        new Thread("Redis-Discord-Sub") {
            @Override
            public void run() {
                try {
                    if (jedisDebug) log("Load Jedis channels");
                    jedisSub.subscribe(subscriber, getJedisChannels());
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
        new Thread("InscriptionsCount-Engine") {
            @Override
            public void run() {
                Timer inscriptionsCount = new Timer();
                inscriptionsCount.schedule(new InscriptionsCount(),0,60000);
            }
        }.start();
        // Event
        api.addEventListener(new BotChat());
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

    public static JSONObject getConfig(){
        return jsonO;
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

    private static String[] getJedisChannels() {
        try {
            Connection connection = sql.getConnection();
            PreparedStatement q = connection.prepareStatement("SELECT * FROM `balkoura_redis_channels`");
            q.execute();
            ArrayList<String> jedisChannels = new ArrayList<>();
            while (q.getResultSet().next()) jedisChannels.add(q.getResultSet().getString("channel"));
            jedisChannels.add("backup");
            return jedisChannels.toArray(new String[0]);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }
}