package paulevs.status;

import net.risingworld.api.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
	private final Map<String, ConfigEntry<?>> entries = new HashMap<>();
	private final Map<String, String> preEntries = new HashMap<>();
	private final List<String> order = new ArrayList<>();
	private final File file;
	
	public Config(String name, Plugin plugin) {
		file = new File(plugin.getPath() + "/" + name + ".conf");
		if (file.exists()) load();
	}
	
	public void save() {
		if (!file.exists()) writeFile();
		else if (entries.size() != preEntries.size()) writeFile();
	}
	
	public void addEntry(String name, float value, String... comments) {
		String stored = preEntries.get(name);
		if (stored != null) value = Float.parseFloat(stored);
		entries.put(name, new ConfigEntry<>(name, value, List.of(comments)));
		order.add(name);
	}
	
	public void addEntry(String name, int value, String... comments) {
		String stored = preEntries.get(name);
		if (stored != null) value = Integer.parseInt(stored);
		entries.put(name, new ConfigEntry<>(name, value, List.of(comments)));
		order.add(name);
	}
	
	public void addEntry(String name, boolean value, String... comments) {
		String stored = preEntries.get(name);
		if (stored != null) value = Boolean.parseBoolean(stored);
		entries.put(name, new ConfigEntry<>(name, value, List.of(comments)));
		order.add(name);
	}
	
	public void addEntry(String name, String value, String... comments) {
		String stored = preEntries.get(name);
		if (stored != null) value = stored;
		entries.put(name, new ConfigEntry<>(name, value, List.of(comments)));
		order.add(name);
	}
	
	public boolean getBool(String name) {
		return (Boolean) entries.get(name).value;
	}
	
	public float getFloat(String name) {
		return (Float) entries.get(name).value;
	}
	
	public int getInt(String name) {
		return (Integer) entries.get(name).value;
	}
	
	public String getString(String name) {
		return (String) entries.get(name).value;
	}
	
	private void writeFile() {
		int max = entries.size() - 1;
		try {
			FileWriter writer = new FileWriter(file);
			for (int i = 0; i < order.size(); i++) {
				entries.get(order.get(i)).append(writer);
				if (i < max) writer.append('\n');
			}
			writer.flush();
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void load() {
		List<String> lines = null;
		try {
			lines = Files.readAllLines(file.toPath());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (lines == null) return;
		lines.stream().filter(line -> line.length() > 2 && line.charAt(0) != '#').forEach(line -> {
			String[] parts = line.split("=");
			String name = parts[0].trim();
			String value = parts[1].trim();
			preEntries.put(name, value);
		});
	}
	
	private static class ConfigEntry <T> {
		final List<String> comments = new ArrayList<>();
		final String name;
		final T value;
		
		public ConfigEntry(String name, T value, List<String> comments) {
			this.comments.addAll(comments);
			this.value = value;
			this.name = name;
		}
		
		void append(FileWriter writer) throws IOException {
			for (String comment : comments) {
				writer.append("# ");
				writer.append(comment);
				writer.append('\n');
			}
			writer.append(name);
			writer.append(" = ");
			writer.append(String.valueOf(value));
			writer.append('\n');
		}
	}
}
