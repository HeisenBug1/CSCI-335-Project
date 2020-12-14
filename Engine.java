import java.util.Scanner;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.lang.ClassNotFoundException;
import java.lang.StringBuilder;
import java.io.*;

public class Engine {

	// variables to check if arguments are present in
	// CMD/Terminal argument and their respected paths
	boolean corpusDirArg = false;
	String corpusDirPath;

	boolean stopListArg = false;
	String stopListPath;

	boolean invertedIndexArg = false;
	String invertedIndexPath;

	boolean queryArg = false;
	String queryArgPath;

	boolean resultsArg = false;
	String resultsArgPath;

	boolean snip = false;
	int snipVal = 0;

	boolean stemmed = false;
	boolean gui = false;

	// data structures to keep record of corpus etc
	Hashtable<String, String> stopWords;

	// to find which documents have the given word
	// 		 Word 			  FileName	 Count
	Hashtable<String, Hashtable<String, Integer>> invWordIndex;	// for words in a file
	Hashtable<String, Hashtable<String, Integer>> invWordIndexP;	// for stemmed words in a file

	// to find any word's count & occurence in a given document (index 0 holds total count and >0 hold occurrences)
	//		FileName 		Word 	   [(0)Count, (1>)Occurrence]
	Hashtable<String, Hashtable<String, ArrayList<Integer>>> invDocIndex;	// for files with indexed words
	Hashtable<String, Hashtable<String, ArrayList<Integer>>> invDocIndexP;	// for files with stemmed indexed words


	public Engine(String[] args) {

		this.readArgs(args);

		String st = "PersistantData/";
		
		File file1 = new File(st+"invWordIndex.obj");
		File file2 = new File(st+"invWordIndexP.obj");
		File file3 = new File(st+"invDocIndex.obj");
		File file4 = new File(st+"invDocIndexP.obj");
		File file5 = new File(st+"stopWords.obj");
		File file6 = new File(st+"corpusDir.obj");

		if(file1.exists() && file2.exists() && file3.exists() && file4.exists() && file5.exists() && file6.exists()) {
			this.corpusDirArg = false;
		}
			
		else {

			this.verifyRequired();

			this.invWordIndex = new Hashtable<>();
			this.invWordIndexP = new Hashtable<>();
			this.invDocIndex = new Hashtable<>();
			this.invDocIndexP = new Hashtable<>();
			this.stopWords = new Hashtable<>();

			this.compileStopList();
			this.compileCorpus();

			try {
				this.backUp();
			}
			catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}	
	}

	// read all html files (corpus)
	private void compileCorpus() {

		if(corpusDirArg) {

			System.out.println("Compiling Corpus");

			File cDir = new File(corpusDirPath);
			File[] file = cDir.listFiles();	// list of all html files

			if(file.length == 0) {
				System.out.println("No HTML files in Corpus Directory");
				System.exit(7);
			}

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
									word = word.toLowerCase();
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
		else {
			System.out.println("Corpus Directory Not Provided");
			System.exit(9);
		}	
	}

	// read query and output in result file
	public String readQueryFile() {

		StringBuilder st = new StringBuilder();

		if(queryArg) {

			try {

				File queryFile = new File(queryArgPath);
				Scanner reader = new Scanner(queryFile);
				String line;
				int i = 1;

				while(reader.hasNextLine()) {

					line = reader.nextLine();
					String[] query = line.split(" ");

					if(query.length != 2) {
						System.out.println("Two words per line required in Query File. Skipping to next line");
						continue;
					}

					else {

						// restore corpus first
						if(!this.corpusDirArg) {
							try {
								this.restore();
								this.corpusDirArg = true;
							}
							catch (InterruptedException ex) {
								ex.printStackTrace();
							}
						}

						String q1 = query[0].toLowerCase();
						String q2 = query[1].toLowerCase();

						if(stemmed) {
							// Stemming for query
							Stemmer stm = new Stemmer();
							stm.add(q2.toCharArray(), q2.length());
							stm.stem();
							q2 = stm.toString();
						}

						// search which docs contains the word
						if(q1.equals("query")) {

							Hashtable<String, Integer> result = null;

							if(stemmed) {
								if(invWordIndexP.containsKey(q2)) {
									result = invWordIndexP.get(q2);
								}
							}
							else {
								if(invWordIndex.containsKey(q2)) {
									result = invWordIndex.get(q2);
								}
							}

							if(result != null) {
								st.append("============================================================\n");
								if(stemmed)
									st.append((i+") Query Term: "+q2+" (Stemmed); Original: "+query[1]+"\n\n"));
								else
									st.append((i+") Query Term: "+q2+"\n\n"));
								i++;

								for(String doc : result.keySet()) {

									String stt = "";

									if(snip) {
										stt = " Snippet: ' ";
										if(stemmed) {
											stt += this.getSnip(doc, invDocIndexP.get(doc).get(q2).get(1));
										}
										else
											stt += this.getSnip(doc, invDocIndex.get(doc).get(q2).get(1));
									}
									
									st.append(("\tIn Doc: "+doc+"\t[Count: "+result.get(doc)+"]"+stt+" '\n\n"));
								}
							}
						}

						// search the frequency of a word in all docs
						if(q1.toLowerCase().equals("frequency")) {

							if(!stopWords.containsKey(q2)) {	// continue search if searched word not a stopWord

								st.append("============================================================\n");
								if(stemmed)
									st.append((i+") Frequency Term: "+q2+" (Stemmed); Original: "+query[1]+"\n\n"));
								else
									st.append((i+") Frequency Term: "+q2+"\n\n"));
								i++;

								Hashtable<String, Hashtable<String, ArrayList<Integer>>> invDoc = null;

								if(stemmed)
									invDoc = invDocIndexP;
								else
									invDoc = invDocIndex;

								for (String docs : invDoc.keySet()) {

									Hashtable<String, ArrayList<Integer>> words = null;

									if(stemmed)
										words = invDocIndexP.get(docs);
									else
										words = invDocIndex.get(docs);

									if(words.containsKey(q2)) {

										ArrayList<Integer> list = words.get(q2);

										st.append(("\tDoc: "+docs+"\t[Count: "+list.get(0)+"]"+"\tOccurrence Index: "));

										for(int x=1; x<list.size(); x++) {

											st.append("["+list.get(x)+"], ");
										}
										st.append("\n\n");
									}
								}
							}
						}
					}
				}

				reader.close();

				if(resultsArg) {
					try {

						FileWriter writer = new FileWriter(resultsArgPath);
						writer.write(st.toString());
						writer.close();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}


			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		else {
			System.out.println("Query file not provided");
			System.exit(3);
		}

		if(gui)
			return st.toString();
		else
			return "Results created in: "+resultsArgPath;
	}


	// compile the stop words OR just import a ready list
	private void compileStopList() {
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
		else
		{
			System.out.println("Stop Word File not Provided");
			System.exit(8);
		}
	}


	// read arguments for files and directories
	private void readArgs(String[] args) {

		boolean failed = false;
		boolean incrementOnce = false;
		int i = 0;

		while(i < args.length) {
			if(args[i].toLowerCase().equals("-corpusdir")) {
				if(i+1 == args.length) {
					System.out.println("No argument provided for: "+args[i]);
					System.exit(2);
				}
				if(args[i+1].charAt(0) == '-') {
					System.out.println("Invalid Argument: "+args[i+1]+" for Flag: "+args[i]);
					failed = true;
				}
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					this.corpusDirArg = true;
					this.corpusDirPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" does not exist for " + args[i]);
					failed = true;
				}
			}
			else if (args[i].toLowerCase().equals("-invertedIndex")) {
				if(i+1 == args.length) {
					System.out.println("No argument provided for: "+args[i]);
					System.exit(2);
				}
				if(args[i+1].charAt(0) == '-') {
					System.out.println("Invalid Argument: "+args[i+1]+" for Flag: "+args[i]);
					failed = true;
				}
				this.invertedIndexArg = true;
				this.invertedIndexPath = args[i+1];
			}
			else if (args[i].toLowerCase().equals("-stoplist")) {
				if(i+1 == args.length) {
					System.out.println("No argument provided for: "+args[i]);
					System.exit(2);
				}
				if(args[i+1].charAt(0) == '-') {
					System.out.println("Invalid Argument: "+args[i+1]+" for Flag: "+args[i]);
					failed = true;
				}
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					this.stopListArg = true;
					this.stopListPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" does not exist for " + args[i]);
					failed = true;
				}
			}
			else if (args[i].toLowerCase().equals("-queries")) {
				if(i+1 == args.length) {
					System.out.println("No argument provided for: "+args[i]);
					System.exit(2);
				}
				if(args[i+1].charAt(0) == '-') {
					System.out.println("Invalid Argument: "+args[i+1]+" for Flag: "+args[i]);
					failed = true;
				}
				File tempFile = new File(args[i+1]);
				if(tempFile.exists()) {
					this.queryArg = true;
					this.queryArgPath = args[i+1];
				}
				else {
					System.out.println(args[i+1] +" does not exist for " + args[i]);
					failed = true;
				}
			}
			else if (args[i].toLowerCase().equals("-results")) {
				this.resultsArg = true;
				if(i+1 == args.length) {
					System.out.println("No argument provided for: "+args[i]);
					System.out.println("Using default: Results.txt");
					this.resultsArgPath = "Results.txt";
				}
				else {
					if(args[i+1].charAt(0) == '-') {
						System.out.println("No argument provided for: "+args[i]);
						System.out.println("Using default: Results.txt");
						this.resultsArgPath = "Results.txt";
						incrementOnce = true;
					}
					else
						this.resultsArgPath = args[i+1];
				}
			}
			else if(args[i].toLowerCase().equals("-snippet")){

				if(i+1 == args.length) {
					System.out.println("No argument provided for: "+args[i]);
					System.exit(2);
				}

				try {
					this.snipVal = Integer.parseInt(args[i+1]);
					if(snipVal < 1) {
						System.out.println("Snippet value needs to be greater than 0");
						System.out.println("Remove -Snippet flag to not use it. Skipping Snippet");
					}
					else {
						this.snip = true;
					}
				} catch (NumberFormatException ex) {
					System.out.println("("+args[i+1]+") Not an integer value. Skipping Snippet");
					this.snip = false;
				}
			}
			else if (args[i].toLowerCase().equals("-stemmed")) {
				this.stemmed = true;
				incrementOnce = true;
			}
			else if(args[i].toLowerCase().equals("-gui")) {
				this.gui = true;
				incrementOnce = true;
			}
			else {
				if(!failed)
					System.out.println("Argument: "+args[i]+" not valid. Skipping");
			}
			if(incrementOnce) 
				i++;
			else
				i = i+2;

			incrementOnce = false;
		}
		if (failed) System.exit(2);
		if(!resultsArg && !gui) {
			System.out.println("No output method provided. (GUI or TextFile)");
			System.out.println("Using TextFile as default to: Results.txt");
			this.resultsArg = true;
			this.resultsArgPath = "Results.txt";
		}
	}


	// verify if all required items are available
	private void verifyRequired() {
		if(corpusDirArg == false || stopListArg == false || queryArg == false) {
			System.out.println("Minumum required arguments not satisfied");
			System.out.println("Needed: Corpus Directory, Stop Word List, Query File");
			System.exit(6);
		}
	}

	// restore member hastables objects using Deserialization
	private void restore() throws InterruptedException{
		try {
			String st = "PersistantData/";

			FileInputStream file1 = new FileInputStream(st+"invWordIndex.obj");
			FileInputStream file2 = new FileInputStream(st+"invWordIndexP.obj");
			FileInputStream file3 = new FileInputStream(st+"invDocIndex.obj");
			FileInputStream file4 = new FileInputStream(st+"invDocIndexP.obj");
			FileInputStream file5 = new FileInputStream(st+"stopWords.obj");

			ObjectInputStream out1 = new ObjectInputStream(file1);
			ObjectInputStream out2 = new ObjectInputStream(file2);
			ObjectInputStream out3 = new ObjectInputStream(file3);
			ObjectInputStream out4 = new ObjectInputStream(file4);
			ObjectInputStream out5 = new ObjectInputStream(file5);
			
			// using threads to make things faster
			Thread t1 = new Thread(){
				public void run(){
					try {
						// System.out.println("Restoring: Inverted Word Index");
						invWordIndex = (Hashtable) out1.readObject();
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t2 = new Thread(){
				public void run(){
					try {
						// System.out.println("Restoring: Stemmed Inverted Word Index");
						invWordIndexP = (Hashtable) out2.readObject();
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t3 = new Thread(){
				public void run(){
					try {
						// System.out.println("Restoring: Inverted Document Index");
						invDocIndex = (Hashtable) out3.readObject();
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t4 = new Thread(){
				public void run(){
					try {
						// System.out.println("Restoring: Stemmed Inverted Document Index");
						invDocIndexP = (Hashtable) out4.readObject();
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t5 = new Thread(){
				public void run(){
					try {
						// System.out.println("Restoring: Stop Words");
						stopWords = (Hashtable) out5.readObject();
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};

			System.out.println("Restoring Corpus");
			t1.start(); t2.start(); t3.start(); t4.start(); t5.start();	// starts all threads at onece

			// restore corpus path while waiting
			FileInputStream cd = new FileInputStream(st+"corpusDir.obj");
			ObjectInputStream cdIn = new ObjectInputStream(cd);
			try {
				corpusDirPath = (String) cdIn.readObject();
			}
			catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}
			cdIn.close(); cd.close();

			t1.join(); t2.join(); t3.join(); t4.join(); t5.join();	// waits HERE for all of them to finish
			System.out.println("Done Restoring");

			out1.close(); out2.close(); out3.close(); out4.close(); out5.close();
			file1.close(); file2.close(); file3.close(); file4.close(); file5.close();

		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	// backing up Hastables using Serialization
	private void backUp() throws InterruptedException {

		try {
			String st = "PersistantData/";
			File dir = new File(st);
			if(!dir.exists())
				dir.mkdirs();

			FileOutputStream file1 = new FileOutputStream(st+"invWordIndex.obj");
			FileOutputStream file2 = new FileOutputStream(st+"invWordIndexP.obj");
			FileOutputStream file3 = new FileOutputStream(st+"invDocIndex.obj");
			FileOutputStream file4 = new FileOutputStream(st+"invDocIndexP.obj");
			FileOutputStream file5 = new FileOutputStream(st+"stopWords.obj");

			ObjectOutputStream out1 = new ObjectOutputStream(file1);
			ObjectOutputStream out2 = new ObjectOutputStream(file2);
			ObjectOutputStream out3 = new ObjectOutputStream(file3);
			ObjectOutputStream out4 = new ObjectOutputStream(file4);
			ObjectOutputStream out5 = new ObjectOutputStream(file5);
			
			Thread t1 = new Thread(){
				public void run(){
					try {
						// System.out.println("Backing Up: invWordIndex");
						out1.writeObject(invWordIndex);
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t2 = new Thread(){
				public void run(){
					try {
						// System.out.println("Backing Up: invWordIndexP");
						out2.writeObject(invWordIndexP);
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t3 = new Thread(){
				public void run(){
					try {
						// System.out.println("Backing Up: invDocIndex");
						out3.writeObject(invDocIndex);
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t4 = new Thread(){
				public void run(){
					try {
						// System.out.println("Backing Up: invDocIndexP");
						out4.writeObject(invDocIndexP);
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t5 = new Thread(){
				public void run(){
					try {
						// System.out.println("Backing Up: stopWords");
						out5.writeObject(stopWords);
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			};

			System.out.println("Backing Up Corpus");
			t1.start(); t2.start(); t3.start(); t4.start(); t5.start();	// starts all threads at once

			// backUp corpus path while waiting
			FileOutputStream cd = new FileOutputStream(st+"corpusDir.obj");
			ObjectOutputStream cdOut = new ObjectOutputStream(cd);
			try {
				cdOut.writeObject(corpusDirPath);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
			cdOut.close(); cd.close();

			// waits HERE for all threads to finish
			t1.join(); t2.join(); t3.join(); t4.join(); t5.join();
			System.out.println("Done Backing Up");

			out1.close(); out2.close(); out3.close(); out4.close(); out5.close();
			file1.close(); file2.close(); file3.close(); file4.close(); file5.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public String getSnip(String fileName, int searchIndex) {

		CircularQueue<String> fifo = new CircularQueue<String>((snipVal*2)+1);	// this is the magic trick

		try {
			File file = new File(corpusDirPath+"/"+fileName);
			Scanner sc = new Scanner(file);
			boolean found = false;
			boolean exit = false;
			int index = 0;
			int snip = snipVal;

			while(sc.hasNextLine()) {
				String[] st = sc.nextLine().split(" ");	// use then same split your using in buinding corpus
				for(int i=0; i<st.length; i++) {
					index++;
					fifo.add(st[i]);
					if(!found && index == searchIndex)
						found = true;
					if(found)
						snip--;
					if(snip == -1)
						exit = true;
					if(exit)
						break;
				}
				if(exit)
					break;
			}
			sc.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		
		return fifo.getString();
	}
}
