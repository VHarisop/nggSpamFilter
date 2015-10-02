CLPATH=".:JInsect.jar:OpenJGraph.jar"

all: Main 


Main: Main.java ConfusionMatrix.java NGramGraphClassifier.java Modeller.java
	javac -cp $(CLPATH) Main.java
