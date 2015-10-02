# nggSpamFilter
A simple spam filter using N-gram graphs and the JInsect toolkit.

## Dependencies
The JINSECT toolkit is a Java-based toolkit and library that supports 
and demonstrates the use of n-gram graphs within Natural Language Processing 
applications, ranging from summarization and summary evaluation to text 
classiﬁcation and indexing.

nggSpamFilter relies heavily on JInsect.
You can download a copy from [here](http://sourceforge.net/projects/jinsect/)

## Compilation
Extract the .jar files from the `JInsect` package into the same directory as the
source files. Then, do

```
javac -cp '.:JInsect.jar:OpenJGraph.jar' Main.java
```

or use the provided Makefile (simply run `make`).


## Usage 
nggSpamFilter accepts two parameters: 

1. a directory containing the training set,
consisting of spam and ham (non-spam) files in .txt format (`trainDirectory`)
2. a directory containing the test set (`testDirectory`)

Both directories must contain the files in their respective 
subfolders `/Spam` and `/Ham` in order for the program to work properly.

Once you have populated the directories as required, simply run 

``` 
java -cp '.:JInsect.jar:OpenJGraph.jar' Main trainDirectory testDirectory
```


