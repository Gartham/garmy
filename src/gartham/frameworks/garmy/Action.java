package gartham.frameworks.garmy;

import net.dv8tion.jda.api.exceptions.RateLimitedException;

public interface Action {
	void run(Bot jda) throws Exception, RateLimitedException;
}
