import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import dataset.ConfusionMatrix;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		/* File directory = new File(args[0]);
		File[] dirList = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		
		System.out.println(Arrays.toString(dirList)); */
		
		NggClassifier nggc = new NggClassifier(args[0]);
		ConfusionMatrix cnf = nggc.classify_all_categories();
		String[] labels = nggc.getLabels();
		System.out.println(labels[0] + " " + Arrays.toString(cnf.precisionAndRecall(0)));
		System.out.println(labels[1] + " " + Arrays.toString(cnf.precisionAndRecall(1)));
	}

}
