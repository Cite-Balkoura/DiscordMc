package fr.milekat.discord.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateMilekat {
    private static final Pattern periodPattern = Pattern.compile("([0-9]+)([smhj])");
    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final DateFormat dfsys = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    public static String setDateNow() {
        return df.format(new Date());
    }

    public static String setDatesysNow() {
        return dfsys.format(new Date());
    }

    public static String setDate(Date date) {
        return df.format(date);
    }

    public static Date getDate(String date) throws ParseException {
        return df.parse(date);
    }

    /**
     *      Compare le temps entre 2 dates
     * @param date1 date de départ (Plus petite pour positif)
     * @param date2 date d'arrivée
     * @return HashMap avec D h m s ms
     */
    public static HashMap<String, String> getReamingTime(Date date1, Date date2){
        HashMap<String, String> RtHashMap = new HashMap<String, String>();
        long diff = date1.getTime() - date2.getTime();
        RtHashMap.put("ms","" + diff);
        RtHashMap.put("s","" + diff / 1000 % 60);
        RtHashMap.put("m","" + diff / (60 * 1000) % 60);
        RtHashMap.put("h","" + diff / (60 * 60 * 1000) % 24);
        RtHashMap.put("D","" + diff / (24 * 60 * 60 * 1000));
        return RtHashMap;
    }

    /**
     *      Convertir les 4j5h9m3s en duration
     * @param period String à transformer en duration
     * @return duration
     */
    public static Long parsePeriod(String period){
        if(period == null) return null;
        period = period.toLowerCase(Locale.ENGLISH);
        Matcher matcher = periodPattern.matcher(period);
        Instant instant= Instant.EPOCH;
        while(matcher.find()){
            int num = Integer.parseInt(matcher.group(1));
            String typ = matcher.group(2);
            switch (typ) {
                case "j":
                    instant=instant.plus(Duration.ofDays(num));
                    break;
                case "h":
                    instant=instant.plus(Duration.ofHours(num));
                    break;
                case "m":
                    instant=instant.plus(Duration.ofMinutes(num));
                    break;
                case "s":
                    instant=instant.plus(Duration.ofSeconds(num));
                    break;
            }
        }
        return instant.toEpochMilli();
    }
}