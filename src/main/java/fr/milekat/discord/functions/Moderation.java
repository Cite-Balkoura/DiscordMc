package fr.milekat.discord.functions;

import fr.milekat.discord.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Date;
import java.util.List;

public class Moderation {
    private final JDA api = Main.getBot();
    private final Guild server = api.getGuildById(554074223870738433L);
    private final MessageChannel logchannel = Main.getBot().getTextChannelById(703601685041250325L);
    private final Category bancategory = api.getCategoryById(554104234442883094L);
    private final Role teamrole = api.getRoleById(712716246432350208L);
    private final Role validrole = api.getRoleById(554076272557555773L);
    private final Role banrole = api.getRoleById(554103808725221376L);
    private final Role muterole = api.getRoleById(712715689357475856L);

    /**
     *      Log de report
     */
    public void report(String target, String sender, String raison) {
        assert server != null;
        server.retrieveMemberById(target).queue(cible -> sendLogDiscord("Report", cible, sender, null, raison));
    }

    /**
     *      Log de ban
     */
    public void ban(String target, String modo, String duree, String raison) {
        assert server != null;
        server.retrieveMemberById(target).queue(cible -> {
            if (!modo.equalsIgnoreCase("console")) {
                server.retrieveMemberById(modo).queue(member ->
                        setBanrole(cible, member.getEffectiveName(), modo, duree, raison));
            } else {
                setBanrole(cible, server.getMember(api.getSelfUser()).getEffectiveName(), modo, duree, raison);
            }

        });
    }

    private void setBanrole(Member cible, String mods, String modo, String duree, String raison) {
        String chname = "ban-" + cible.getEffectiveName().toLowerCase().replaceAll(" ","-");
        chname = chname.replaceAll("[^a-zA-Z0-9-]", "");
        assert banrole != null;
        assert server != null;
        server.addRoleToMember(cible, banrole).queue();
        TextChannel banchannel = null;
        assert bancategory != null;
        for (TextChannel channel : bancategory.getTextChannels()) {
            if (channel.getName().equalsIgnoreCase(chname)) {
                banchannel = channel;
                setBanchannel(banchannel, cible, mods, duree, raison);
                break;
            }
        }
        if (banchannel==null) {
            server.createTextChannel(chname).setParent(bancategory)
                    .queue(channel -> setBanchannel(channel, cible, mods, duree, raison));
        }
        sendLogDiscord("Ban",cible,modo,duree,raison);
        List<Role> roles = cible.getRoles();
        if (roles.contains(teamrole)) {
            assert teamrole != null;
            server.removeRoleFromMember(cible,teamrole).queue();
        }
        if (roles.contains(validrole)) {
            assert validrole != null;
            server.removeRoleFromMember(cible,validrole).queue();
        }
    }

    private void setBanchannel(TextChannel banchannel, Member cible, String mods, String duree, String raison) {
        try {
            banchannel.getPermissionOverrides().clear();
        } catch (UnsupportedOperationException ignored) {}
        assert bancategory != null;
        banchannel.getManager().sync(bancategory).queue();
        banchannel.getManager().setTopic("Canal privé suite au bannissement de " + cible.getEffectiveName() + ", par " + mods).queue();
        banchannel.createPermissionOverride(cible).clear().setAllow(Permission.VIEW_CHANNEL).queue();
        banchannel.sendMessage("Bonjour " + cible.getAsMention() + " ! Tu es là pour la raison suivante: " + raison + "."
                + System.lineSeparator() + "Tu peux essayer de t'expliquer avec le staff, la sanction prendra fin le: `" + duree + "`.")
                .queue();
    }

    /**
     *      Log d'unban
     */
    public void unban(String target, String modo, String raison) {
        assert server != null;
        server.retrieveMemberById(target).queue(cible -> {
            String chname = "ban-" + cible.getEffectiveName().toLowerCase().replaceAll(" ","-");
            chname = chname.replaceAll("[^a-zA-Z0-9-]", "");
            assert banrole != null;
            server.removeRoleFromMember(cible, banrole).queue();
            for (TextChannel channel : bancategory.getTextChannels()) {
                if (channel.getName().equalsIgnoreCase(chname)) {
                    channel.delete().queue();
                    break;
                }
            }
            cible.getUser().openPrivateChannel().queue(dm -> {
                dm.sendMessage("Votre banissement a pris fin, vous pouvez dès à présent revenir sur la cité.").queue();
                dm.sendMessage("IP du serveur: mc.cite-balkoura.fr").queue();
                sendLogDiscord("UnBan",cible,modo,null,raison);
                if (Main.profilHashMap.get(cible.getIdLong()).getTeam()>0) {
                    assert validrole != null;
                    server.addRoleToMember(cible, validrole).queue();
                }
                if (Main.profilHashMap.get(cible.getIdLong()).getTeam()==0) {
                    assert teamrole != null;
                    server.addRoleToMember(cible, teamrole).queue();
                }
            });
        });
    }

    /**
     *      Log de mute
     */
    public void mute(String target, String modo, String duree, String raison) {
        assert server != null;
        server.retrieveMemberById(target).queue(cible -> {
            assert muterole != null;
            server.addRoleToMember(cible, muterole).queue();
            sendLogDiscord("Mute",cible, modo,duree,raison);
        });
    }

    /**
     *      Log d'unmute
     */
    public void unmute(String target, String modo, String raison) {
        assert server != null;
        server.retrieveMemberById(target).queue(cible -> {
            assert muterole != null;
            server.removeRoleFromMember(cible, muterole).queue();
            cible.getUser().openPrivateChannel().queue(dm -> {
                dm.sendMessage("Votre banissement a pris fin, vous pouvez dès à présent revenir sur la cité.");
                sendLogDiscord("UnMute",cible, modo,null,raison);
            });
        });
    }

    /**
     *      Log d'un kick
     */
    public void kick(String target, String modo, String raison) {
        assert server != null;
        server.retrieveMemberById(target).queue(cible -> {
            sendLogDiscord("Kick",cible, modo,null,raison);
        });
    }

    /**
     *      Envoi d'un Log sur le cannal de log
     */
    private void sendLogDiscord(String sanction, Member cible, String mods, String duree, String raison){
        if (!mods.equalsIgnoreCase("console")) {
            assert server != null;
            server.retrieveMemberById(mods).queue(member ->
                    createLog(sanction, cible, member.getAsMention(), duree, raison));
        } else {
            createLog(sanction, cible, api.getSelfUser().getAsMention(), duree, raison);
        }
    }

    private void createLog(String sanction, Member cible, String modo, String duree, String raison) {
        EmbedBuilder Sanction = new EmbedBuilder();
        Sanction.setTitle("Nouveau log")
                .setColor(Color.RED)
                .addField(":bust_in_silhouette: Joueur sanctionné", cible.getAsMention(),true)
                .addField(":police_officer: Modérateur", modo,true)
                .addField(":hammer_pick: Action", sanction,true)
                .setTimestamp(new Date().toInstant());
        if (duree!=null) {
            Sanction.addField(":stopwatch: Jusqu'au", duree, true);
        }
        Sanction.addField(":label: Raison", raison,true);
        logchannel.sendMessage(Sanction.build()).queue();
    }
}
