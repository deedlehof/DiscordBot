package main.java;

import jdk.nashorn.internal.scripts.JD;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    private Properties properties;

    public Main(){
        properties = getProperties("config.properties");

        System.out.println("Logging bot in...");
        JDA bot = createClient(properties.getProperty("token"));
        bot.addEventListener(new MessageHandler(properties));
        bot.addEventListener(new MusicPlayer(properties));

    }

    private static JDA createClient(String token){
        JDA jda = null;
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(token)
                    .buildBlocking();
            jda.setAutoReconnect(true);
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (RateLimitedException e) {
            e.printStackTrace();
        }
        return jda;
    }

    private Properties getProperties(String filename){
        Properties prop = new Properties();
        InputStream input;
        try{

            input = new FileInputStream(filename);

            prop.load(input);

            input.close();

        } catch (IOException e){
            e.printStackTrace();
        }

        return prop;
    }

    public static void main(String args[]){
        Main test = new Main();
    }
}
