import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.io.FileWriter;

public class SearchEngine {

	// variables to check if arguments are present in
	// CMD/Terminal argument and their respected paths
	static boolean corpusDirArg = false;
	static String corpusDirPath;

	static boolean stopListArg = false;
	static String stopListPath;

	static boolean invertedIndexArg = false;
	static String invertedIndexPath;

	static boolean queryArg = false;
	static String queryArgPath;

	static boolean resultsArg = false;
	static String resultsArgPath;


	// data structures to keep record of corpus etc
	static Hashtable<String, String> stopWords = new Hashtable<>();
	static Hashtable<String, Hashtable<String, Integer>> invIndex = new Hashtable<>();
	static Hashtable<String, Hashtable<String, ArrayList<Integer>>>invDocIndex = new Hashtable<>();


	// ---- the main ----
	public static void main(String[] args) throws InterruptedException {

		// check it arguments are even numbere
		if(args.length % 2 != 0) {
			System.out.println("# of arguments passed: " +args.length);
			System.out.println("There needs to be an even number of arguments");
			System.exit(1);
		}

		readArgs(args);	// read arguments
		// verifyRequired();	// verify all required files are ready
		compileStopList();	// read stop words
		compileCorpus();	// compile all corpus (inverted Word & Docs)
		// readQueryFile();

		try {
			FileWriter writer = new FileWriter("/Users/rez/Documents/GitHub/CSCI-335-Project/InvertDocumentIndex.txt");
			writer.write(invDocIndex.toString());
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		

	}
	// ---- end of main ----


	// read all html files (corpus)
	static public void compileCorpus() throws InterruptedException {

		if(corpusDirArg) {

			File cDir = new File(corpusDirPath);
			File[] file = cDir.listFiles();	// list of all html files

			for(int i=0; i<file.length; i++) {	// for each file

				if(!file[i].isDirectory()) {	// check if not directory

					try {
						String line;	// to read line by line
						Scanner reader = new Scanner(file[i]);
						int cutOff = 0;	// if not html file then cutoff
						boolean isHTML = false;	// and move to next file
						int index = 0;	// to track index of word in a file

						while(reader.hasNextLine()) {
							line = reader.nextLine();

							if(!isHTML) {	// only read HTML files
								if(line.toLowerCase().indexOf("html") < 12 && 	// verify if <!DOCTYPE html
									line.toLowerCase().indexOf("html") > 0) {									
									isHTML = true;
								}
								else {
									cutOff++;
									if(cutOff == 100 || !reader.hasNextLine()) {
										break;
									}
								}
							}

							else {

								String fileName = file[i].getName();	// current html file name
								String[] stArr = line.split(" ");	// each word in html split in empty space

								for(String word : stArr) {	// for each word in doc
									word.trim();
									index++;	// indexed by each word

									//compiling Inverted Document Index
									if(!word.equals("")) {	// exclude empty strings

										if(!invDocIndex.containsKey(fileName)) {	// if brand new file

											Hashtable<String, ArrayList<Integer>> iDoc = new Hashtable<>();
											ArrayList<Integer> countArr = new ArrayList<>();
											countArr.add(1);	// add total count
											countArr.add(index);	// add index of first occurrence 
											iDoc.put(word, countArr);
											invDocIndex.put(fileName, iDoc);	// add (file, word, count, occurrence index)
										}

										else {	// if file has been added before

											Hashtable<String, ArrayList<Integer>> iDoc = invDocIndex.get(fileName);

											if(!iDoc.containsKey(word)) {	// if brand new word

												ArrayList<Integer> countArr = new ArrayList<Integer>();
												countArr.add(1);
												countArr.add(index);
												iDoc.put(word, countArr);	// add (word, count, occurence index)
											}

											else {	// if both file and word is present

												iDoc.get(word).set(0, iDoc.get(word).get(0)+1);	// total count increment
												iDoc.get(word).add(index);	// add (count , occurence index)
											}
										}
									}

									// compiling Inverted Word Index
									// same as above but only for inverted words (no occurence count)
									if(!word.equals("") && !stopWords.containsKey(word)) {

										if(!invIndex.containsKey(word)) {

											Hashtable<String, Integer> doc = new Hashtable<>();
											doc.put(fileName, 1);
											invIndex.put(word, doc);
										}

										else {

											Hashtable<String, Integer> doc = invIndex.get(word);

											if(!doc.containsKey(fileName)) {

												doc.put(fileName, 1);
											}

											else {

												int count = doc.get(fileName);
												doc.put(fileName, ++count);
											}
										}
									}
								}
							}
						}

						reader.close();
						
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}		
	}


	static public void readQueryFile() {

		if(queryArg && resultsArg) {

			String st="";

			try {

				File queryFile = new File(queryArgPath);
				Scanner reader = new Scanner(queryFile);
				String line;
				int i = 1;

				while(reader.hasNextLine()) {

					line = reader.nextLine();
					String[] query = line.split(" ");

					if(query.length > 2) {
						System.out.println("Two words per line required");
						System.exit(4);
					}

					else {

						if(query[0].equals("Query")) {

							if(invIndex.containsKey(query[1])) {

								Hashtable<String, Integer> result = invIndex.get(query[1]);
								st+=(i+") Query Term: "+query[1]+"\n");
								i++;

								for(String doc : result.keySet()) {
									st+=("\tIn Doc: "+doc+"\t\tCount: "+result.get(doc)+"\n");
								}
							}
						}

						if(query[0].equals("Frequency")) {


							for (String docs : invDocIndex.keySet()) {
								// System.out.println(docs);
								Hashtable<String, ArrayList<Integer>> words = invDocIndex.get(docs);

								if(words.containsKey(query[1])) {
									// System.out.println("found");
									ArrayList<Integer> list = words.get(query[1]);
									st+=(i+") Frequency Term: "+query[1]+"\n");
									i++;

									st+=("\tDoc: "+docs+"\t\tCount: "+list.get(0)+"\t\tOccurrence Index: ");

									for(int x=1; x<list.size(); x++) {

										st+="["+list.get(x)+"], ";
									}
									st+="\n";
								}
							}
						}
					}
				}
				reader.close();

				// System.out.println(st);
				try {

					FileWriter writer = new FileWriter(resultsArgPath);
					writer.write(st);
					writer.close();

				} catch (IOException e) {
					e.printStackTrace();
				}


			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		else {
			System.out.println("Query file not provided");
			System.exit(3);
		}
	}


	// compile the stop words OR just import a ready list
	static public void compileStopList() {
		if(stopListArg) {
			try {
				File stopListFile = new File(stopListPath);
				Scanner reader = new Scanner(stopListFile);

				while(reader.hasNextLine()) {
					String text = reader.nextLine();
					stopWords.put(text, text);
				}

				reader.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}


	// read arguments for files and directories
	static void readArgs(String[] args) {

		boolean failed = false;

		for(int i=0; i<args.length; i=i+2) {
			if(args[i].equals("-CorpusDir")) {
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					corpusDirArg = true;
					corpusDirPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" is not a correct path for " + args[i]);
					failed = true;
				}
			}
			if (args[i].equals("-InvertedIndex")) {
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					invertedIndexArg = true;
					invertedIndexPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" is not a correct path for " + args[i]);
					failed = true;
				}
			}
			if (args[i].equals("-StopList")) {
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					stopListArg = true;
					stopListPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" is not a correct path for " + args[i]);
					failed = true;
				}
			}
			if (args[i].equals("-Queries")) {
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					queryArg = true;
					queryArgPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" is not a correct path for " + args[i]);
					failed = true;
				}
			}
			if (args[i].equals("-Results")) {
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					resultsArg = true;
					resultsArgPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" does not exist for " + args[i]);
					System.out.println("Create the file? (yes/no) ");
					Scanner sc = new Scanner(System.in);
					String create = sc.nextLine();
					if(create.charAt(0) == 'y' || create.charAt(0) == 'Y') {
						try {
							tempFile.createNewFile();
							resultsArg = true;
							resultsArgPath = args[i+1];
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else {
						System.out.println("Argument "+args[i]+" was unsuccesful");
						failed = true;
					}
				}
			}
		}
		if (failed) System.exit(2);
	}


	// verify if all required items are available
	static boolean verifyRequired() {
		if(corpusDirArg == false || invertedIndexArg == false ||
			stopListArg == false || queryArg == false || resultsArg == false) {

			return false;
		}
		else return true;
	}
}