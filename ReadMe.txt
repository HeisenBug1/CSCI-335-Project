Note: All Flags are case insensitive, but any argument following a flag is case sensitive
	(except for -Stemmed, which does not need a following argument)
	example: [-stemmed] or [-CorpusDir CoRpUsDiReCtOrY] or [-corpusdir corpusDIRECTORY]

Note: If a file or directory mentioned as argument is in same directory as the java files, then just directly name the file/directory. Else if the file/directory is in a different location then you will need to provide the exact full path to the file/directory.




To Compile:-	javac SearchEngine.java

To Run:-		java SearchEngine -Flag1 arg1 -flag2 -FLAG3 arg3 ... ... (In any order)





First Run: (Minimum Arguments Required for First Run - In any order)

	-CorpusDir corpus -StopList stopwords.txt -Queries QueryFile.txt -Results Results.txt

	Note: "corpus" here, is the name of the directory holding all HTML files
	Note: First Run will create Serialized backup of hastables



Every Run After First: (will restore objects backed up previously, unless deleted)
	
	-Queries QueryFile.txt -Results Results.txt (REQUIRED - Any Order)
	-Stemmed -Snippet <Int>		(Optional - Any Order)

	Note: You can keep any optional flags ON or OFF
	Note: Corpus directory & StopList not required after first run



Query File: Queries can be either "Query <term>" or "Frequency <term>" (case insensitive)

	Example: Query sleep
	Example: Frequency Sleep

	Note: Query file can have multiple lines of queries
	Note: Both "Query" and "Frequency" can display stemmed queries
	Note: Only "Query" can show snippets (If snippet flag is turned on with a valid value. If snippet value is not valid, it will ignore snippet flag)