package fr.milekat.discord.obj;

import java.util.UUID;

public class Profil {
    private final UUID uuid;
    private final String name;
    private int team;
    private String muted;
    private String banned;
    private String reason;
    private long discordid;

    public Profil(UUID uuid, String name, int team, String muted, String banned, String reason, long discordid) {
        this.uuid = uuid;
        this.name = name;
        this.team = team;
        this.muted = muted;
        this.banned = banned;
        this.reason = reason;
        this.discordid = discordid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public String getMuted() {
        return muted;
    }

    public void setMuted(String muted) {
        this.muted = muted;
    }

    public String getBanned() {
        return banned;
    }

    public void setBanned(String banned) {
        this.banned = banned;
    }

    public boolean isMute(){
        return !this.muted.equals("pas mute");
    }

    public boolean isBan(){
        return !this.banned.equals("pas ban");
    }

    public long getDiscordid() {
        return discordid;
    }

    public void setDiscordid(long discordid) {
        this.discordid = discordid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
