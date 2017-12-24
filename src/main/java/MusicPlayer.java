package main.java;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MusicPlayer extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private final String PREFIX;

    private final String commandChannelName;

    public MusicPlayer(Properties properties) {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        PREFIX = properties.getProperty("command-prefix");
        commandChannelName = properties.getProperty("command-channel");

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;
        if(event.getTextChannel().getName() != commandChannelName) return;

        String[] command = event.getMessage().getContentDisplay().split(" ", 2);
        Guild guild = event.getGuild();

        TextChannel channel = event.getTextChannel();
        Member member = event.getMember();

        if (command.length >= 1 && command[0].startsWith(PREFIX)) {
            command[0] = command[0].replaceFirst(PREFIX, "").toLowerCase();

            switch (command[0]){
                case "join":
                    join(channel, member, guild);
                    break;
                case "leave":
                    leave(channel, member, guild);
                    break;
                case "play":
                case "queue":
                    loadAndPlay(member, channel, command[1]);
                    break;
                case "unpause":
                    pause(channel, false);
                    break;
                case "pause":
                    if (getGuildAudioPlayer(guild).player.isPaused())
                        pause(channel, false);
                    else
                        pause(channel, true);
                    break;
                case "skip":
                    skipTrack(channel);
                    break;
                case "help":
                    help(channel);
                    break;
            }
        }

        super.onMessageReceived(event);
    }

    private void help(TextChannel channel){
        channel.sendMessage("COMMANDS:\n" +
                "!join\n" +
                "!leave\n" +
                "!play url\n" +
                "!pause\n" +
                "!skip\n"
        ).queue();

    }

    private void join(TextChannel channel, Member member, Guild guild){
        AudioManager audioManager = guild.getAudioManager();
        VoiceChannel voiceChannel = member.getVoiceState().getChannel();

        if(audioManager.isConnected()) {
            if(voiceChannel.getId() == audioManager.getConnectedChannel().getId()) return;
        }

        if(voiceChannel == null){
            channel.sendMessage("You aren't in a voice channel!").queue();
        } else {
            channel.sendMessage("Connected to **" + voiceChannel.getName() + "**.").queue();
            audioManager.openAudioConnection(voiceChannel);
        }
    }

    private void leave(TextChannel channel, Member member, Guild guild){
        VoiceChannel voiceChannel = member.getVoiceState().getChannel();
        AudioManager audioManager = guild.getAudioManager();

        if(voiceChannel == null){
            channel.sendMessage("You aren't in a voice channel!").queue();
        } else if(audioManager.isConnected()){
            channel.sendMessage("Goodbye!").queue();
            audioManager.closeAudioConnection();
        } else {
            channel.sendMessage("I'm not connected to **" + voiceChannel.getName() + "**").queue();
        }
    }


    private void loadAndPlay(final Member member, final TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(member, channel, channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(member, channel, channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Member member, TextChannel channel, Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        join(channel, member, guild);

        musicManager.scheduler.queue(track);
    }

    private void pause(TextChannel channel, boolean pause) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.player.setPaused(pause);

        if(pause)
            channel.sendMessage("Paused track.").queue();
        else
            channel.sendMessage("Resumed track.").queue();
    }


    private void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to next track.").queue();
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }
}