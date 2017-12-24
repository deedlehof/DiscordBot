package main.java;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Properties;

public class MessageHandler extends ListenerAdapter {

    public static final int RELAXED = 0;
    public static final int NORMAL = 1;
    public static final int STRICT = 2;

    private final String PREFIX;

    private String[] bannedWords;

    public MessageHandler(Properties properties){
        //bannedWords = new String[]{"test"};
        //get the banned words from the properties file (separated by ',')
        bannedWords = properties.getProperty("banned").split(",");
        PREFIX = properties.getProperty("command-prefix");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //Event specific information
        User author = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.

        if(author.isBot()) return;

        if(checkForBanned(message, bannedWords, STRICT)){
            System.out.println(author.getName() + " : " + message.getContentDisplay());
            event.getMessage().delete().complete(); //maybe change complete to something else
        }
        super.onMessageReceived(event);
    }

    private static boolean checkForBanned(Message message, String[] banned, int level){
        String msg = message.toString().toLowerCase();
        if(level == RELAXED) { //if the word is typed exactly the way it is in properties
            for (String word : banned) {
                if (relaxedMsgContainsWord(msg, word)){
                    return true;
                }
            }
        } else if(level == NORMAL) { //adds checking for extra letters in the word
            for (String word : banned) {
                if (strictMsgContainsWord(msg, word)) {
                    return true;
                }
            }
        } else { //STRICT: checks if the word is anywhere in the message separated by spaces or special characters
            msg = removeWhitespace(msg);
            msg = removeSpecialChars(msg);

            for (String word : banned) {
                if (strictMsgContainsWord(msg, word)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean relaxedMsgContainsWord(String msg, String key){
        String[] words = msg.split(" ");
        for(String word: words){
            if(word.equals(key)) return true;
        }

        return false;
    }

    private static boolean strictMsgContainsWord(String msg, String key){
        char[] keyChars = key.toCharArray();
        char[] msgChars = msg.toCharArray();

        boolean isChecking = false;
        int keyPos = 0;

        for(int c = 0; c < msgChars.length; c++){
            //starts checking process if the start of the word is found
            if (!isChecking && msgChars[c] == keyChars[0]) isChecking = true;
                //if we have started checking and the current letter isn't the same as the previous
            else if(isChecking && keyChars[keyPos] != msgChars[c]){
                //check if we've reached the end of the key
                if(keyPos < keyChars.length-1) {
                    keyPos++;
                } else { //reached end. we have a match
                    return true;
                }

                //check if the next letter is not equal to our key position and
                //that we don't have a repeat in our key sequence. Ex. street
                if(keyChars[keyPos] != msgChars[c] && keyChars[keyPos] != keyChars[keyPos-1]){
                    keyPos = 0;
                    isChecking = false;
                }
            }
        }

        //edge case for the word being at the very end of a statement
        if(isChecking && msgChars[msgChars.length-1] == keyChars[keyChars.length-1])
            return true;


        return false;
    }

    private static String removeWhitespace(String input){
        String output = "";
        char[] inputChars = input.toCharArray();

        for(int i = 0; i < input.length(); i++){
            if(inputChars[i] != ' ')
                output += inputChars[i];
        }

        return output;
    }

    private static String removeSpecialChars(String input){
        return input.replaceAll("[^a-zA-Z]", "");
    }
}
