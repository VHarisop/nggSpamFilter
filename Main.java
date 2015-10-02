public class Main {

	public static void main(String[] args) {
		
		// create an NGG classifier 
		NGramGraphClassifier clf = new NGramGraphClassifier(args[0]);
		
		// perform classification
		clf.classify(args[1]);
		
	}
}
