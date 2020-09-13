package fr.milekat.discord.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static fr.milekat.discord.Main.log;

public class MariaManage {
    private Connection connection;
    private String driver,url,host,database,user,pass;

    public MariaManage(String url, String host, String database, String user, String pass){
        //this.driver = "com.mysql.jdbc.Driver";
        this.url = url;
        this.host = host;
        this.database = database;
        this.user = user;
        this.pass = pass;
    }

    public void connection(){
        try {
            //Class.forName(this.driver);
            connection = DriverManager.getConnection(url + host + "/" + database + "?autoReconnect=true&useUnicode=true" +
                    "&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", user, pass);
            log("SQL connecté !");
        } catch (SQLException e) {
            e.printStackTrace();
            log("Erreur SQL.");
        }
    }

    public void disconnect(){
        try {
            connection.close();
            log("SQL déconnecté !");
        } catch (SQLException e) {
            e.printStackTrace();
            log("Erreur SQL.");
        }
    }

    public boolean isConnected(){
        return connection != null;
    }

    public Connection getConnection(){
        isConnected();
        return connection;
    }
}