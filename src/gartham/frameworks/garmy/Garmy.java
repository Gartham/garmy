package gartham.frameworks.garmy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.exceptions.RateLimitedException;

public class Garmy {
	private final BlockingQueue<Bot> bots = new LinkedBlockingQueue<>();
	private ScheduledExecutorService ses = Executors.newScheduledThreadPool(25);

	public void setExecutor(ScheduledExecutorService ses) {
		this.ses = ses;
	}

	public void add(Bot bot) {
		bots.add(bot);
	}

	public List<String> add(File tokens) throws FileNotFoundException {
		try (Scanner s = new Scanner(tokens)) {
			List<String> failures = null;
			while (s.hasNextLine()) {
				String line = null;
				try {
					line = s.nextLine();
					add(line);
				} catch (LoginException e) {
					if (failures == null)
						failures = new ArrayList<>();
					failures.add(line);
				}
			}
			return failures;
		}
	}

	public List<String> add(String... tokens) {
		return add(List.of(tokens));
	}

	public List<String> add(Collection<String> tokens) {
		List<String> failures = null;
		for (String s : tokens) {
			try {
				add(s);
			} catch (LoginException e) {
				if (failures == null)
					failures = new ArrayList<>();
				failures.add(s);
			}
		}
		return failures;
	}

	public void add(String token) throws LoginException {
		bots.add(new Bot(token));
	}

	public void simuRun(Action... actions) {
		for (Action a : actions) {
			ses.submit(() -> {
				try {
					Bot bot = bots.take();
					simuRun(bot, a);
				} catch (InterruptedException e3) {
					return;
				}
			});
		}
	}

	private void simuRun(Bot bot, Action a) throws InterruptedException {
		while (true)
			try {
				a.run(bot.getBot());
				bots.add(bot);
				break;
			} catch (RateLimitedException e1) {
				long mil = System.currentTimeMillis();
				Bot b = bots.poll(e1.getRetryAfter(), TimeUnit.MILLISECONDS);
				if (b != null) {
					// Queue bot we just used to go back in queue when it's ready to be reused.
					getThread(() -> {
						try {
							Thread.sleep(e1.getRetryAfter() - System.currentTimeMillis() + mil);
						} catch (InterruptedException e) {
							return;
						}
						bots.add(bot);
						return;
					}).start();
					// Execute the action on the next bot.
					simuRun(b, a);
					break;
				}
				// Continue and retry. Because we waited for the timeout, this thread is free
				// again.
			} catch (Exception e2) {
				bots.add(bot);
				System.err.println("Error on: " + bot.getBot().getSelfUser().getAsTag());
				e2.printStackTrace();
				break;
			}
	}

	public void sequRun(Action... actions) {
		try {
			for (Action a : actions) {
				Bot bot = bots.take();
				sequRun(bot, a);
			}
		} catch (InterruptedException e) {
		}
	}

	private void sequRun(Bot bot, Action a) throws InterruptedException {
		while (true) {
			try {
				a.run(bot.getBot());
				bots.add(bot);
				break;
			} catch (RateLimitedException e) {
				long mil = System.currentTimeMillis();
				Bot b = bots.poll(e.getRetryAfter(), TimeUnit.MILLISECONDS);
				if (b != null) {
					getThread(() -> {
						try {
							Thread.sleep(e.getRetryAfter() - System.currentTimeMillis() + mil);
						} catch (InterruptedException e1) {
							return;
						}
						bots.add(bot);
						return;
					}).start();
					sequRun(b, a);
					break;
				}
			} catch (Exception e) {
				bots.add(bot);
				System.err.println("Error on: " + bot.getBot().getSelfUser().getAsTag());
				e.printStackTrace();
			}
		}
	}

	public Thread getThread(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	}
}
