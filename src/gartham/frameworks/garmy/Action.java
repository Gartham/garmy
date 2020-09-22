package gartham.frameworks.garmy;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.RateLimitedException;

public interface Action {
	void run(JDA jda) throws Exception, RateLimitedException;
}
