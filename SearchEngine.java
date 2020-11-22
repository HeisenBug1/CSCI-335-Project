import java.io.*;

public class SearchEngine {

	// ---- the main ----
	public static void main(String[] args) {

		Engine e = new Engine(args);
		e.readQueryFile();
	}
	// ---- end of main ----
}