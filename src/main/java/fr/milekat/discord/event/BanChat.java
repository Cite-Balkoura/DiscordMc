package fr.milekat.discord.event;

import fr.milekat.discord.Main;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class BanChat extends ListenerAdapter {
    private final JDA api = Main.getBot();
    private final Category bancategorie = api.getCategoryById(554104234442883094L);
    private final TextChannel logbanchannel = api.getTextChannelById(756683301212913699L);

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getCategory()==null || !event.getMessage().getCategory().equals(bancategorie)) return;
        if (logbanchannel == null || event.getTextChannel().equals(logbanchannel)) return;
        logbanchannel.sendMessage(event.getTextChannel().getName() + System.lineSeparator() +
                event.getAuthor().getAsMention() + " **»** " + event.getMessage().getContentRaw()).queue();
    }
}
