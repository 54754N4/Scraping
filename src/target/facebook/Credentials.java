package target.facebook;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.io.Files;

public final class Credentials {
	public final transient String username, password;
	
	public Credentials(Path path) throws IOException {
		List<String> lines = Files.readLines(path.toFile(), StandardCharsets.UTF_8);
		username = lines.get(0);
		password = lines.get(1);
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
	public File serializeTo(String filepath) throws IOException {
		File file = Paths.get(filepath).toFile();
		Files.asCharSink(file, StandardCharsets.UTF_8)
			.write(String.format("%s%n%s%n", username, password));
		return file;
	}
	
	@Override
	public String toString() {
		return String.format("U:%s%nP:%s%n", username, password);
	}
	
	public static Credentials fromFile(Path filepath) throws IOException {
		return new Credentials(filepath);
	}
}