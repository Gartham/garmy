package gartham.frameworks.garmy;

import java.util.Arrays;
import java.util.EnumSet;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot {
	private final JDA bot;

	public Bot(JDA bot) {
		this.bot = bot;
	}

	public Bot(String token) throws LoginException {
		bot = JDABuilder.create(token, EnumSet.allOf(GatewayIntent.class)).build();
	}

	public Bot(String token, GatewayIntent... intents) throws LoginException {
		bot = JDABuilder.create(Arrays.asList(intents)).setToken(token).build();
	}

}
