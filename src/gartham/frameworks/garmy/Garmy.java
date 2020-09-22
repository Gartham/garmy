package gartham.frameworks.garmy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import javax.security.auth.login.LoginException;

public class Garmy {
	private final List<Bot> bots = new ArrayList<>();

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
}
