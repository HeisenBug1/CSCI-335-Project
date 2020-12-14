import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.util.Scanner;

public class SearchEngine {

	// ---- the main ----
	public static void main(String[] args) {

		Engine e = new Engine(args);
		String st = e.readQueryFile();

		Scanner sc = new Scanner(st);

		if(!sc.nextLine().contains("Results created in:")) {
			JFrame frame = new JFrame("Search Engine");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JTextArea textArea = new JTextArea(st);
			textArea.setSize(1200,600);
			textArea.setLineWrap(true);
			textArea.setEditable(false);
			JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

			frame.add(scrollPane);
			frame.pack();
			frame.setVisible(true);
		}

		sc.close();
	}
	// ---- end of main ----
}