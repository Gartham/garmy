package gartham.frameworks.garmy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot {

	private final Map<Class<? extends GenericEvent>, List<? extends GenericEvent>> loggedEvents = new HashMap<>();
	private final Map<Class<? extends GenericEvent>, List<Class<? extends GenericEvent>>> eventTypeHierarchy = new HashMap<>();

	public Map<Class<? extends GenericEvent>, List<? extends GenericEvent>> getLoggedEvents() {
		return loggedEvents;
	}

	public synchronized <T extends GenericEvent> boolean isPresent(Class<T> eventType,
			Function<? super T, Boolean> filterer) {
		if (loggedEvents.containsKey(eventType)) {
			@SuppressWarnings("unchecked")
			List<T> events = (List<T>) loggedEvents.get(eventType);
			for (T ge : events)
				if (filterer.apply(ge))
					return true;
			@SuppressWarnings({ "unchecked", "rawtypes" })
			// Get the subtypes for this type.
			List<Class<? extends T>> subclasses = (List<Class<? extends T>>) (List) eventTypeHierarchy.get(eventType);

			for (Class<? extends T> subtype : subclasses)
				if (isPresent(subtype, filterer))
					return true;
		}
		return false;
	}

	public synchronized <T extends GenericEvent> void await(Class<T> eventType, Function<? super T, Boolean> filterer)
			throws InterruptedException {
		while (!isPresent(eventType, filterer))
			wait();
	}

	private final JDA bot;

	public JDA getBot() {
		return bot;
	}

	@SuppressWarnings("unchecked")
	public Bot(JDA bot) {
		this.bot = bot;
		bot.addEventListener((EventListener) event -> {
			synchronized (this) {
				if (!loggedEvents.containsKey(event.getClass())) {
					// Add to type hierarchy.
					addToTypeHierarchy(event.getClass());
					loggedEvents.put(event.getClass(), new ArrayList<>());
				}
				((List<GenericEvent>) loggedEvents.get(event.getClass())).add(event);
				notifyAll();
			}
		});
	}

	/**
	 * Called when an event type is not already in the type hierarchy, to add the
	 * type to the hierarchy.
	 * 
	 * @param <T>
	 * @param newType
	 * @author Gartham
	 */
	@SuppressWarnings("unchecked")
	private <T extends GenericEvent> void addToTypeHierarchy(Class<T> newType) {
		eventTypeHierarchy.put(newType, new ArrayList<>(3));// Register this type.
//		System.out.println(newType);
		if (newType != Event.class) {
			Class<? super T> superType = newType.getSuperclass();// Finish registration by linking with superclass.
			if (superType != null) {
				if (!eventTypeHierarchy.containsKey(superType))
					addToTypeHierarchy((Class<? extends GenericEvent>) superType);
				eventTypeHierarchy.get(superType).add(newType);
			}
			for (Class<? super T> superInterface : (Class<? super T>[]) newType.getInterfaces())
				if (GenericEvent.class.isAssignableFrom(superInterface)) {
					if (!eventTypeHierarchy.containsKey(superInterface))
						addToTypeHierarchy((Class<? extends GenericEvent>) superInterface);
					eventTypeHierarchy.get(superInterface).add(newType);
				}
		}
	}

	public Bot(String token) throws LoginException {
		this(JDABuilder.create(token, EnumSet.allOf(GatewayIntent.class)).build());
	}

	public Bot(String token, GatewayIntent... intents) throws LoginException {
		this(JDABuilder.create(Arrays.asList(intents)).setToken(token).build());
	}

	public void awaitReady() throws InterruptedException {
		bot.awaitReady();
	}

}
