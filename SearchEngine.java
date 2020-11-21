import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.io.FileWriter;

public class SearchEngine {

	// ---- the main ----
	public static void main(String[] args) throws InterruptedException {

		// check it arguments are even numbere
		if(args.length % 2 != 0) {
			System.out.println("# of arguments passed: " +args.length);
			System.out.println("There needs to be an even number of arguments");
			System.exit(1);
		}

		readArgs(args);	// read arguments
		// if(verifyRequired()){	// verify all required files are ready
		// 	compileStopList();	// read stop words
		// 	compileCorpus();	// compile all corpus (inverted Word & Docs)
		// 	readQueryFile();	// reads query file and output result
		// }
		// else {
		// 	System.out.println("All required arguments not present");
		// 	System.exit(5);
		// }

		compileStopList();
		compileCorpus();
		readQueryFile();
	
		// Uncomment bottom two TRY/CATCH blocks to create inverted index files
		// try {
		// 	FileWriter writer = new FileWriter("InvertDocumentIndex.txt");
		// 	writer.write(invDocIndex.toString());
		// 	writer.close();
		// }catch (IOException e) {
		// 	e.printStackTrace();
		// }
		
		// try {
		// 	FileWriter writer = new FileWriter("InvertWordIndexP.txt");
		// 	writer.write(invDocIndexP.get("Alan_Turing").get("Turing").toString());
		// 	writer.close();
		// }catch (IOException e) {
		// 	e.printStackTrace();
		// }
	}
	// ---- end of main ----

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

	// 				 Word 			  FileName	 Count
	static Hashtable<String, Hashtable<String, Integer>> invWordIndex = new Hashtable<>();	// for words in a file
	static Hashtable<String, Hashtable<String, Integer>> invWordIndexP = new Hashtable<>();// for stemmed words in a file

	//				FileName 		Word 	   [(0)Count, (1>)Occurrence]
	static Hashtable<String, Hashtable<String, ArrayList<Integer>>> invDocIndex = new Hashtable<>();	// for files with indexed words
	static Hashtable<String, Hashtable<String, ArrayList<Integer>>> invDocIndexP = new Hashtable<>(); // for files with stemmed indexed words

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
									break;
								}
								else {	// read first 100 lines to check if its html doc
									cutOff++;
									if(cutOff == 100) {
										break;
									}
								}
							}
						}

						reader.close();

						if(isHTML) {

							reader = new Scanner(file[i]);

							// read again from the beginning to accurately count index
							while(reader.hasNextLine()) {
								line = reader.nextLine();

								String fileName = file[i].getName();	// current html file name
								String[] stArr = line.split(" ");	// each word in html split in empty space

								for(String word : stArr) {	// for each word in doc
									word.trim();
									index++;	// indexed by each word

									if(word.length() < 30) {	// ignore too long word, possibly useless html syntax

										if(!word.equals("")) {	// exclude empty strings

											// Stemming done here
											Stemmer st = new Stemmer();
											st.add(word.toCharArray(), word.length());
											st.stem();
											String wordP = st.toString();

											// compiling Inverted Document Index here
											if(!invDocIndex.containsKey(fileName)) {	// if brand new file/doc

												// Without Stemming
												Hashtable<String, ArrayList<Integer>> iDoc = new Hashtable<>();
												ArrayList<Integer> countArr = new ArrayList<>();
												countArr.add(1);	// index[0] contains total occurrence count
												countArr.add(index);	// index[1] & onwards contains the index of occurrences
												iDoc.put(word, countArr);
												invDocIndex.put(fileName, iDoc);	// store (file, word, count, occurrence-index) basically
																					// like [file [word [count, occurrences]]]
											}

											else {	// if file/doc has been added before, then just update

												Hashtable<String, ArrayList<Integer>> iDoc = invDocIndex.get(fileName);

												// if brand new word
												if(!iDoc.containsKey(word)) {

													// No Stemming
													ArrayList<Integer> countArr = new ArrayList<Integer>();
													countArr.add(1);
													countArr.add(index);
													iDoc.put(word, countArr);	// store (word, count, occurence-index)
												}

												else {	// if both file and word is present, then JUST UPDATE

													// No Stemming
													iDoc.get(word).set(0, iDoc.get(word).get(0)+1);	// update total count increment
													iDoc.get(word).add(index);	// update (count , occurence-index (for new word))
												}
											}


											// compiling STEMMED Inverted Document Index here (Similar to above line 141)
											if(!invDocIndexP.containsKey(fileName)) {

												// With Stemming (Similar to above)
												Hashtable<String, ArrayList<Integer>> iDocP = new Hashtable<>();
												ArrayList<Integer> countArrP = new ArrayList<>();
												countArrP.add(1);
												countArrP.add(index);
												iDocP.put(wordP, countArrP);
												invDocIndexP.put(fileName, iDocP);
											}

											else {	// if file/doc has been added before, then just update

												Hashtable<String, ArrayList<Integer>> iDocP = invDocIndexP.get(fileName);

												// if brand new Stemmed word
												if(!iDocP.containsKey(wordP)) {
													ArrayList<Integer> countArrP = new ArrayList<>();
													countArrP.add(1);
													countArrP.add(index);
													iDocP.put(wordP, countArrP);
												}

												else {	// if both file and word is present, then JUST UPDATE (Stemmed)

													// WIth Stemming (Similar to above)
													iDocP.get(wordP).set(0, iDocP.get(wordP).get(0)+1);
													iDocP.get(wordP).add(index);
												}

											}	// ---- Compiling Inverted Documnets done ---- //


											// compiling Inverted Word Index here
											// same as above but only for inverted words (no occurence count)
											if(!stopWords.containsKey(word)) {	// if not a stopWord

												// if brand new word
												if(!invWordIndex.containsKey(word)) {

													Hashtable<String, Integer> doc = new Hashtable<>();
													doc.put(fileName, 1);	// add which file contains word
													invWordIndex.put(word, doc);	// store word & the file it exists in
												}

												else {	// if word exists already

													Hashtable<String, Integer> doc = invWordIndex.get(word);

													// if word exists but not the file/doc
													if(!doc.containsKey(fileName))
														doc.put(fileName, 1);													

													else {	// if both word and file/doc exists

														int count = doc.get(fileName);
														doc.put(fileName, ++count);	// increment count
													}
												}

												// if brand new Stemmed word (Similar to above)
												if(!invWordIndexP.containsKey(wordP)) {

													Hashtable<String, Integer> docP = new Hashtable<>();
													docP.put(fileName, 1);	// add which file contains stemmed word
													invWordIndexP.put(wordP, docP);	// store stemmed word & the file it exists in
												}

												else {	// if stemmed word exists

													Hashtable<String, Integer> docP = invWordIndexP.get(wordP);

													// if stemmed word exists but not file/doc
													if(!docP.containsKey(fileName))
														docP.put(fileName, 1);

													else {	// if both stemmed word & file/doc exists

														int countP = docP.get(fileName);
														docP.put(fileName, ++countP);	// increment count
													}
												}
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

	// read query and output in result file
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

					if(query.length != 2) {
						System.out.println("Two words per line required");
						System.exit(4);
					}

					else {

						// search which docs contains the word
						if(query[0].toLowerCase().equals("query")) {

							if(invWordIndexP.containsKey(query[1])) {

								Hashtable<String, Integer> result = invWordIndexP.get(query[1]);
								st+=(i+") Query Term: "+query[1]+"\n");
								i++;

								for(String doc : result.keySet()) {
									st+=("\tIn Doc: "+doc+"\t\tCount: "+result.get(doc)+"\n");
								}
							}

							else {
								System.out.println("Word: "+query[1]+" not in any document");
								System.exit(6);
							}
						}

						// search the frequency of a word in all docs
						if(query[0].toLowerCase().equals("frequency")) {

							if(!stopWords.containsKey(query[1])) {	// continue search if searched word not a stopWord

								for (String docs : invDocIndexP.keySet()) {
									// System.out.println(docs);
									Hashtable<String, ArrayList<Integer>> words = invDocIndexP.get(docs);

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
				}

				reader.close();

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
