import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import dataset.ConfusionMatrix;

public class Main {
	
	public static void main(String[] args) {
		
		/* create a multiclass classifier on base directory 
		 * and perform classification on all categories
		 */
		NggClassifier nggc = new NggClassifier(args[0]);
		ConfusionMatrix cnf = nggc.classify_all_categories();

		/* print the classification results
		 * for every class (precision & recall)
		 */
		String[] labels = nggc.getLabels();
		System.out.println(labels[0] + " " + Arrays.toString(cnf.precisionAndRecall(0)));
		System.out.println(labels[1] + " " + Arrays.toString(cnf.precisionAndRecall(1)));
	}

}
