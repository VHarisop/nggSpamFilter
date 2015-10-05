CLPATH=".:../JInsect.jar:../OpenJGraph.jar:dataset.jar"

all: Main 

Main: Main.java NggClassifier.java Modeller.java DataSplitter.java
	javac -cp $(CLPATH) Main.java

run: Main
	java -cp $(CLPATH) Main $(BASEDIR)

clean: 
	rm *.class 
