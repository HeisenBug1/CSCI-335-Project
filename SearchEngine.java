import java.io.*;

public class SearchEngine {

	// ---- the main ----
	public static void main(String[] args) {

		// check it arguments are even numbere
		if(args.length % 2 != 0) {
			System.out.println("# of arguments passed: " +args.length);
			System.out.println("There needs to be an even number of arguments");
			System.exit(1);
		}

		Engine e = new Engine(args);
		// e.start();
	}
	// ---- end of main ----
}