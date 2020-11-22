Note: All Flags are case insensitive, but any argument following a flag is case sensitive
	(except for -Stemmed, which does not need a following argument)
	example: [-stemmed] or [-CorpusDir CoRpUsDiReCtOrY] or [-corpusdir corpusDIRECTORY]

Note: If a file or directory mentioned as argument is in same directory as the java files, then just directly name the file/directory. Else if the file/directory is in a different location then you will need to provide the exact full path to the file/directory.



First Run: (Minimum Required Arguments - In any order)

	-CorpusDir corpus -StopList stopwords.txt -Queries QueryFile.txt -Results Results.txt

	Where: "corpus" is the name of the html directory



Every Run After First: (will restore objects backed up previously, unless deleted)