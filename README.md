# CS980-DataScience

This repository contains code  for assignments in the CS980 Data Science class. 

## Installation

Clone the repository into some directory in your system.

Open a terminal, and move into the directory.

You should find a pom.xml file. Type the following:
```
mvn package
```

## Execution

Once the above command is executed, you should find a new directory called target. Move into the target directory.
```
cd target
```

### Building the Lucene Index

The first step is to create the Lucene Index. Run the following command where the first argument is the paragraph corpus 
file and the second argument is the output directory into which the index will be created.
```
java -jar Builder-jar-with-dependencies.jar <paragraphCBOR> <LuceneIndex>
```

### Querying the Lucene Index

The second step is to query the created Index. Run the following command where the first argument is the outline file and the
second argument is the location of the Lucene Index.
```
java -jar Searcher-jar-with-dependencies.jar <OutlinesCBOR> <LuceneIndex>
```
