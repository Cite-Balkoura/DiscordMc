package fr.milekat.discord.event;

import fr.milekat.discord.Main;
import fr.milekat.discord.obj.Profil;
import fr.milekat.discord.obj.Team;
import fr.milekat.discord.utils.MojangNames;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static fr.milekat.discord.Main.*;

public class Inscription extends ListenerAdapter {
    private final JDA api = Main.getBot();
    private final Guild server = api.getGuildById(554074223870738433L);
    private final Role teamrole = api.getRoleById(712716246432350208L);
    private final TextChannel validationchannel = api.getTextChannelById(713533614524203079L);
    private final TextChannel rechercheteam = api.getTextChannelById(712719477414035536L);
    private final long reactmsgid = 713751748732387338L;

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        api.retrieveUserById(event.getUserIdLong()).queue(user -> {
            if (user==null || user.isBot() || server==null) return;
            if (!Main.profilHashMap.containsKey(user.getIdLong())) {
                if (event.getMessageIdLong() == reactmsgid) {
                    if (!Main.regEnCours.contains(user)) {
                        if (event.getReactionEmote().getAsCodepoints().equalsIgnoreCase("U+2705")) {
                            user.openPrivateChannel().queue(dm -> {
                                Main.waitbot.add(user);
                                Main.regEnCours.add(user);
                                Main.registerstep.put(user, 0);
                                log("L'utilisateur: " + user.getAsTag() + " débute l'inscription");
                                dm.sendMessage("Bonjour, merci d'avoir accepté notre règlement.").queue();
                                dm.sendTyping().queue();
                                sleep(2000);
                                dm.sendMessage("Voici le formulaire à suivre pour soumettre ton inscription:").queue();
                                dm.sendTyping().queue();
                                sleep(3000);
                                dm.sendMessage("1) Merci de renseigner ton pseudo minecraft (No Crack) actuel " +
                                        "(S'il change d'ici la cité ce n'est pas un souci):").queue();
                                Main.registerstep.put(user, 1);
                                Main.waitbot.remove(user);
                                debug("En attente du pseudo de " + user.getAsTag());
                            });
                        }
                    } else {
                        user.openPrivateChannel().queue(dm -> dm.sendMessage("Tu es déjà en cours d'incription..").queue());
                    }
                } else if (event.getChannel().getType().equals(ChannelType.PRIVATE)) {
                    PrivateChannel dm = event.getPrivateChannel();
                    Main.waitbot.add(user);
                    debug("Nouvelle émote dm de " + user.getAsTag() + ", émote: " + event.getReactionEmote().getAsCodepoints());
                    if (Main.registerstep.getOrDefault(user,0).equals(2)) {
                        if (event.getReactionEmote().getAsCodepoints().equalsIgnoreCase("U+2705")) {
                            Main.registerstep.put(user, 0);
                            dm.sendTyping().queue();
                            dm.sendMessage("Pseudo validé !").queueAfter(500L, TimeUnit.MILLISECONDS);
                            dm.sendTyping().queue();
                            sleep(1000);
                            dm.sendMessage("Super, l'inscription est déjà presque terminée :smile: !").queue();
                            dm.sendTyping().queue();
                            sleep(3000);
                            dm.sendMessage("2) Pour finir, petite question de départagement (Répondre en 1 seul " +
                                    "message, max 400 chars), quel sont t'es objetifs sur une cité ?").queue();
                            Main.registerstep.put(user, 3);
                            debug(user.getAsTag() + " valide l'UUID: " + reguuid.getOrDefault(user,null));
                        } else {
                            Main.registerstep.put(user, 0);
                            dm.sendTyping().queue();
                            sleep(500);
                            dm.sendMessage("Ouups ! Tu peux ré-essayer.").queue();
                            Main.registerstep.put(user, 1);
                            debug("L'utilisateur " + user.getAsTag() + " ne valide pas l'UUID: " + reguuid.getOrDefault(user,null));
                        }
                    } else if (Main.registerstep.getOrDefault(user,0).equals(4)) {
                        if (event.getReactionEmote().getAsCodepoints().equalsIgnoreCase("U+2705")) {
                            Main.registerstep.put(user, 0);
                            dm.sendTyping().queue();
                            sleep(500);
                            dm.sendMessage("Super ! J'enregistre le tout, " +
                                    "et je l'envois au modérateurs pour validation.").queue();
                            dm.sendTyping().queue();
                            sleep(1000);
                            dm.sendMessage("Si vous n'avez pas de réponse sous " +
                                    "72h c'est que votre demande est refusée.").queue();
                            server.retrieveMember(user).queue(member -> newCandid(member, user));
                            debug("L'utilisateur " + user.getAsTag() + " a validé ses informations, envoi au staff..");
                        } else if (event.getReactionEmote().getAsCodepoints().equalsIgnoreCase("U+274C")) {
                            Main.registerstep.put(user, 0);
                            dm.sendTyping().queue();
                            sleep(500);
                            dm.sendMessage("Ce n'est pas bon ? Alors on reprend.").queue();
                            dm.sendTyping().queue();
                            sleep(2000);
                            dm.sendMessage("1) Merci de renseigner ton pseudo minecraft (No Crack) actuel " +
                                    "(S'il change d'ici la cité ce n'est pas un souci):").queue();
                            Main.registerstep.put(user, 1);
                            debug("L'utilisateur " + user.getAsTag() + "  ne valide pas ses infos, retry.");
                        }
                    }
                    Main.waitbot.remove(user);
                }
            } else if (event.getChannel().getType().equals(ChannelType.PRIVATE) &&
                    Main.profilHashMap.get(event.getUserIdLong()).getTeam()==0) {
                if (event.getReactionEmote().getAsCodepoints().equalsIgnoreCase("U+2705")) {
                    event.getPrivateChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                        if (message.getEmbeds().size() == 0) return;
                        MessageEmbed embed = message.getEmbeds().get(0);
                        if (embed==null || embed.getFooter()==null || embed.getFooter().getText()==null) return;
                        Team team = Main.teams.get(Integer.parseInt(embed.getFooter().getText()));
                        if (!(team.getSize() > 5)) {
                            addPlayerTeam(team,Main.profilHashMap.get(event.getUserIdLong()));
                            event.getPrivateChannel().sendMessage("Félicitations, tu viens de rejoindre l'équipe " +
                                    team.getName() + ".").queue();
                            event.getPrivateChannel().sendMessage("Tu peux maintenant inviter d'autres membres " +
                                    "dans cette équipe.").queue();
                            debug("En attente des équipiers de " + user.getAsTag());
                        } else {
                            event.getPrivateChannel().sendMessage("Désolé, l'équipe est pleine.").queue();
                            debug("L'équipe " + team.getName() + " est pleine!");
                        }
                    });
                }
            } else if (event.getChannel().equals(validationchannel)) {
                event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                    if (message.getEmbeds().size()==0) return;
                    if (!message.getReactions().get(0).hasCount() || message.getReactions().get(0).getCount() < 5) return;
                    MessageEmbed embed = message.getEmbeds().get(0);
                    if (embed==null || embed.getFooter()==null || embed.getFooter().getText()==null) return;
                    if (event.getReactionEmote().getAsCodepoints().equalsIgnoreCase("U+2705")) {
                        server.retrieveMemberById(embed.getFooter().getText()).queue(member -> {
                            Connection connection = Main.getSqlConnect().getConnection();
                            try {
                                UUID uuid = UUID.fromString(Objects.requireNonNull(embed.getFields().get(2).getValue()));
                                PreparedStatement q = connection.prepareStatement("INSERT INTO `" + Main.SQLPREFIX +
                                        "player`(`uuid`, `discord_id`, `name`) VALUES (?,?,?);");
                                q.setString(1, uuid.toString());
                                q.setLong(2, member.getIdLong());
                                q.setString(3, embed.getFields().get(0).getValue());
                                q.execute();
                                q.close();
                                Main.profilHashMap.put(member.getIdLong(), new Profil(uuid,
                                        MojangNames.getName(uuid.toString()),
                                        0,
                                        0,
                                        "pas mute",
                                        "pas ban",
                                        null,
                                        false,
                                        0,
                                        false,
                                        member.getIdLong()));
                                Main.uuidUserHashMap.put(uuid,member.getUser());
                                assert teamrole != null;
                                server.addRoleToMember(member,teamrole).queue();
                                Main.waitbot.add(user);
                                member.getUser().openPrivateChannel().queue(this::msgCreateTeam);
                                Main.waitbot.remove(user);
                                Main.registerstep.remove(member.getUser());
                                Main.reguuid.remove(member.getUser());
                                Main.regmotivations.remove(member.getUser());
                                Main.regEnCours.remove(user);
                                debug("Le staff valide " + user.getAsTag());
                            } catch (SQLException throwables) {
                                debug("Erreur SQL suite à la validation de " + user.getAsTag());
                                throwables.printStackTrace();
                            }
                        });
                    } else {
                        message.clearReactions().queue();
                    }
                });
            }
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return;
        User user = event.getAuthor();
        MessageChannel channel = event.getChannel();
        if (event.getChannel().getType().equals(ChannelType.PRIVATE)) {
            if (Main.waitbot.contains(user)) return;
            if (!Main.profilHashMap.containsKey(user.getIdLong())) {
                if (!Main.registerstep.containsKey(user)) return;
                switch (Main.registerstep.get(user)) {
                    case 1: {
                        if (event.getMessage().getContentRaw().split(" ").length != 1) {
                            channel.sendMessage("Désolé mais je n'ai pas bien compris, " +
                                    "tu peux ré-essayer (Pseudo sans espace).").queue();
                            debug(user.getAsTag() + " pseudo rentré avec espaces ?");
                            return;
                        }
                        String uuid = MojangNames.getUuid(event.getMessage().getContentRaw());
                        if (Main.uuidUserHashMap.containsKey(UUID.fromString(uuid))) {
                            channel.sendMessage("Désolé mais cet utilisateur est déjà enregistré.").queue();
                            channel.sendMessage("Si tu es bien cet utilisateur, contact le staff.").queue();
                            channel.sendMessage("Si non, recommence avec le bon pseudo.").queue();
                            debug(user.getAsTag() + " pseudo " + event.getMessage().getContentRaw() + " déjà reg.");
                            return;
                        }
                        EmbedBuilder skin = new EmbedBuilder();
                        skin.setTitle("Est-ce bien ton skin ?")
                                .setColor(Color.GREEN)
                                .setImage("https://crafatar.com/renders/body/" + uuid +
                                        "?size=512&overlay&default=MHF_Alex");
                        channel.sendMessage(skin.build()).queue(message -> {
                            message.addReaction("✅").queue();
                            message.addReaction("❌").queue();
                        });
                        Main.registerstep.put(user, 2);
                        Main.reguuid.put(user, UUID.fromString(uuid));
                        debug("En attente de la validation du skin de " + user.getAsTag());
                        break;
                    }
                    case 3: {
                        if (event.getMessage().getContentRaw().length() > 400) {
                            channel.sendMessage("Tu m'as l'air un peu trop motivé, essaies de synthétiser !").queue();
                            debug(user.getAsTag() + " msg motivation trop long !");
                            return;
                        }
                        Main.registerstep.put(user, 0);
                        Main.waitbot.add(user);
                        channel.sendTyping().queue();
                        sleep(750);
                        channel.sendMessage("C'est nôté !").queue();
                        channel.sendTyping().queue();
                        sleep(1000);
                        channel.sendMessage("Je récapitule...").queue();
                        EmbedBuilder validation = new EmbedBuilder();
                        validation.setTitle("Tu valides ta fiche ? (Si non, je reprends au début)")
                                .addField("Ton Pseudo Minecraft actuel",
                                        MojangNames.getName(Main.reguuid.get(user).toString()), false)
                                .addField("T'es motivations pour la cité", event.getMessage().getContentRaw(),false)
                                .setColor(Color.GREEN)
                                .setThumbnail("https://crafatar.com/renders/body/" + Main.reguuid.get(user) +
                                        "?size=512&overlay&default=MHF_Alex");
                        channel.sendMessage(validation.build()).queue(message -> {
                            message.addReaction("✅").queue();
                            message.addReaction("❌").queue();
                        });
                        Main.regmotivations.put(user, event.getMessage().getContentRaw());
                        Main.registerstep.put(user, 4);
                        Main.waitbot.remove(user);
                        debug(user.getAsTag() + " attente validation des infos.");
                        break;
                    }
                }
            } else if (Main.profilHashMap.containsKey(user.getIdLong()) && Main.profilHashMap.get(user.getIdLong()).getTeam()==0) {
                // Récupération commande
                String[] message = event.getMessage().getContentRaw().split(" ");
                if (message.length != 3) {
                    event.getChannel().sendMessage("Merci d'utiliser team create <nom de team> pour créer une" +
                            " nouvelle équipe, et la rejoindre.").queue();
                    debug(user.getAsTag() + " commande création team erreur.");
                    return;
                }
                if (!message[0].equalsIgnoreCase("team")) return;
                if (!message[1].equalsIgnoreCase("create")) return;
                Connection connection = Main.getSqlConnect().getConnection();
                try {
                    PreparedStatement q = connection.prepareStatement("INSERT IGNORE INTO `" + Main.SQLPREFIX +
                            "team`(`team_name`) VALUES (?) RETURNING `team_id`;");
                    q.setString(1,message[2]);
                    q.execute();
                    Main.waitbot.add(user);
                    channel.sendTyping().queue();
                    sleep(500);
                    channel.sendMessage("Vérification..").queue();
                    channel.sendTyping().queue();
                    sleep(750);
                    if (q.getResultSet().last()) {
                        Main.profilHashMap.get(user.getIdLong()).setTeam(q.getResultSet().getInt("team_id"));
                        PreparedStatement q2 = connection.prepareStatement("UPDATE `" + Main.SQLPREFIX +
                                "player` SET `team_id` = ? WHERE `uuid` = ?;");
                        q2.setInt(1,q.getResultSet().getInt("team_id"));
                        q2.setString(2,Main.profilHashMap.get(user.getIdLong()).getUuid().toString());
                        q2.execute();
                        q2.close();
                        channel.sendMessage("Félicitation !").queue();
                        channel.sendTyping().queue();
                        sleep(1000);
                        channel.sendMessage("Tu viens de créer l'équipe " + message[2] + ".").queue();
                        sleep(1000);
                        channel.sendMessage("Tu peux maintenant décider de rester solo dans ton équipe pour " +
                                "jouer le classement Solo, soit inviter d'autres membres sans équipe pour jouer le " +
                                "classement Duo/Trio ou Équipe (6 maxi).").queue();
                        channel.sendMessage("Ton grade sera mis à jour d'ici quelques instants.").queue();
                        log(user.getAsTag() + " créé l'équipe" + message[2]);
                    } else {
                        channel.sendMessage("Désolé, cette équipe, n'est pas disponible.").queue();
                        debug(user.getAsTag() + " a tenté de créé l'équipe " + message[2] + " mais elle existe déjà.");
                    }
                    q.close();
                    Main.waitbot.remove(user);
                } catch (SQLException throwables) {
                    Main.log("Erreur lors de l'injection de l'équipe");
                    throwables.printStackTrace();
                }
            }
        } else if (event.getChannel().equals(rechercheteam)) {
            Member member = event.getMember();
            if (member==null) return;
            if (Main.profilHashMap.get(user.getIdLong()).getTeam()==0) return;
            String[] message = event.getMessage().getContentRaw().split(" ");
            if (message.length != 3 || !message[0].equalsIgnoreCase("team") ||
                    !message[1].equalsIgnoreCase("invite")){
                event.getMessage().delete().queue();
                return;
            }
            if (Main.teams.get(Main.profilHashMap.get(user.getIdLong()).getTeam()).getSize()>5) {
                api.openPrivateChannelById(member.getId()).queue(dm->
                        dm.sendMessage("Désolé, ton équipe est pleine tu ne peux plus inviter de membres.").queue());
                event.getMessage().delete().queue();
                return;
            }
            Member target = event.getMessage().getMentionedMembers().get(0);
            if (target!=null && target.getRoles().contains(teamrole)) {
                api.openPrivateChannelById(target.getId()).queue(dm -> {
                    EmbedBuilder invitation = new EmbedBuilder();
                    invitation.setTitle("Invitation à rejoindre une équipe")
                            .addField("Le joueur",member.getAsMention(),true)
                            .addField("T'invite à rejoindre l'équipe",
                                    Main.teams.get(Main.profilHashMap.get(user.getIdLong()).getTeam()).getName(),
                                    true)
                            .addField("ATTENTION !!","Rejoindre une équipe est un acte définitif", false)
                            .setColor(Color.GREEN)
                            .setFooter(String.valueOf(Main.profilHashMap.get(user.getIdLong()).getTeam()))
                            .setThumbnail("https://crafatar.com/renders/body/" +
                                    Main.profilHashMap.get(user.getIdLong()).getUuid()
                                    + "?size=512&overlay&default=MHF_Alex");
                    dm.sendMessage(invitation.build()).queue(accepte -> {
                        accepte.addReaction("✅").queue();
                        accepte.addReaction("❌").queue();
                    });
                });
                debug(user.getAsTag() + " invite le joueur " + target.getUser().getAsTag() + " à rejoindre son équipe.");
            } else {
                api.openPrivateChannelById(member.getId()).queue(dm->
                        dm.sendMessage("Joueur introuvable / ne recherche pas d'équipe.").queue());
                event.getMessage().delete().queue();
                debug(user.getAsTag() + " invite x mais il ne cherche pas d'équipe.");
            }
        }
    }

    private void msgCreateTeam(PrivateChannel dm) {
        dm.sendMessage("Bonjour ! ta candidature a été acceptée.").queue();
        dm.sendTyping().queue();
        sleep(2000);
        dm.sendMessage("Pour terminer ta pré-inscription, il te faut désormais rejoindre une équipe.").queue();
        dm.sendTyping().queue();
        sleep(1000);
        dm.sendMessage("Tu as donc 2 solutions :").queue();
        dm.sendMessage("1) Créer et rejoindre une équipe.").queue();
        dm.sendMessage("2) Rejoindre une équipe suite à une invitation.").queue();
        dm.sendMessage("**ATTENTION** rejoindre une équipe est une action définitive!").queue();
        dm.sendMessage("Avoir une équipe est obligatoire pour participer à la cité.").queue();
        dm.sendMessage("Pour créer une équipe: **team create <nom de team>** (Action définitive).").queue();
        assert rechercheteam != null;
        dm.sendMessage("Si tu préfères rejoindre une équipe, rends toi dans "
                + rechercheteam.getAsMention() + ", tu pourra échanger sur tes ambitions.").queue();
        dm.sendMessage("Sinon tu souhaites inviter des joueurs ayants validés leur " +
                "pré-inscription, mais saches avant tout que tous vos biens seront partagés ! Alors fait " +
                "bien attention aux joueurs à qui tu accordes ta confiance.").queue();
        dm.sendMessage("Pour inviter un joueur: **team invite <@Mention>** dans " +
                rechercheteam.getAsMention() + " (Attention vous partagerez tout !)").queue();
    }

    private void newCandid(Member member, User user) {
        EmbedBuilder validation = new EmbedBuilder();
        validation.setTitle("Nouvelle candidature de")
                .setDescription(member.getAsMention())
                .addField("Pseudo Minecraft actuel",
                        MojangNames.getName(Main.reguuid.get(user).toString()), false)
                .addField("Motivations pour la cité", Main.regmotivations.get(user), false)
                .addField("UUID", Main.reguuid.get(user).toString(), false)
                .setColor(Color.YELLOW)
                .setFooter(user.getId())
                .setThumbnail("https://crafatar.com/renders/body/" + Main.reguuid.get(user) +
                        "?size=512&overlay&default=MHF_Alex");
        sleep(750);
        assert validationchannel != null;
        validationchannel.sendMessage(validation.build()).queue(message -> {
            message.addReaction("✅").queue();
            message.addReaction("❌").queue();
        });
    }

    private void addPlayerTeam(Team team, Profil profil) {
        Connection connection = Main.getSqlConnect().getConnection();
        try {
            PreparedStatement q = connection.prepareStatement("UPDATE `" + Main.SQLPREFIX +
                    "player` SET `team_id` = ? WHERE `uuid` = ?;");
            q.setInt(1,team.getId());
            q.setString(2, profil.getUuid().toString());
            q.execute();
            team.addMembers(profil.getName());
            team.setSize(team.getSize() + 1);
            profil.setTeam(team.getId());
            q.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }
}
