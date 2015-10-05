
import gr.demokritos.iit.jinsect.documentModel.representations.*;

import java.io.File;
import java.io.FilenameFilter;
// import java.util.Arrays;

import dataset.*;


/**
 * A simple Java class that creates an N-gram graph representation
 * for an email in text form.
 * @author VHarisop
 *
 */
public class Modeller {

	private String[] filenameList;
	private DocumentNGramGraph[] distroGraphs;
	private DocumentNGramGraph modelGraph;

	/**
	 * Creates an instance of Modeller from a given directory
	 * that contains .txt files.
	 * @param dirPath the directory path
	 */
	public Modeller(String dirPath) {	
		File directory = new File(dirPath);
		
		// save the directory .corpus files to a filelist
		filenameList = directory.list(new FilenameFilter() {
			public boolean accept(File dirPath, String fileName) {
				return fileName.endsWith(".txt");
			}
		});
		
		
		initGraphs(dirPath);
		updateGraphs();
	}
	
	/** 
	 * Creates an instance of Modeller from a given directory
	 * that has been split into training and test sets
	 * @param dirPath the directory of the set
	 * @param limit a Pair object showing the indices of the test set
	 */
	public Modeller(String dirPath, Pair limit) {
		
		File directory = new File(dirPath);
		
		filenameList = directory.list(new FilenameFilter() {
			public boolean accept(File dirPath, String fileName) {
				return fileName.endsWith(".txt");
			}
		});
		
		// initialize graphs with the Pair filter
		initGraphs(dirPath, limit);
		updateGraphs();
	}
	
	/**
	 * Initializes a set of N-Gram Graphs for each file in the corpus
	 * @param dirPath the directory of the dataset
	 * @param limit a Pair object with the indices to exclude 
	 */
	private void initGraphs(String dirPath, Pair limit) {
		
		distroGraphs = new DocumentNGramGraph[filenameList.length - limit.range()];
		String filename; int run_index = 0;
		
		System.out.println("Reading graphs...");
		// create an array of N-Gram graphs, one for each corpus
		for (int index = 0; index < filenameList.length; index++) {
			
			try {
				
				// if not in limits, then use for training
				if (!limit.includes(index)) {
					filename = dirPath + "/" + filenameList[index];
				
					// create the distribution graphs for the email body
					distroGraphs[run_index] = new DocumentNGramGraph(); 
					distroGraphs[run_index].loadDataStringFromFile(filename);
					
					// notify for progress
					if ((run_index % 50) == 0) { System.out.print(run_index + "... "); }
					run_index++;
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}			
		}
		System.out.println("Done!");
	}
	
	
	/**
	 * Initializes a set of N-Gram Graphs for each file in the corpus
	 * @param dirPath the directory of the modelling set
	 */
	private void initGraphs(String dirPath) {
		
		distroGraphs = new DocumentNGramGraph[filenameList.length];
			
		String filename;
		System.out.print("Reading graphs...");
		// create an array of N-Gram graphs, one for each corpus
		for (int index = 0; index < filenameList.length; index++) {
			
			try {
				filename = dirPath + "/" + filenameList[index];
				
				// create the distribution graphs for the email body
				distroGraphs[index] = new DocumentNGramGraph(); 
				distroGraphs[index].loadDataStringFromFile(filename);
				
				if (index % 50 == 0) { System.out.print(index + " ..."); }

			} catch (Exception ex) {
				ex.printStackTrace();
			}			
		}
		System.out.println("Done!");
	}
	
	/**
	 * Iterates over all graphs created and merges them into
	 * one "model" graph.
	 */
	private void updateGraphs() {
		// create an initial graph as a starting point
		modelGraph = distroGraphs[0];
		double lr = 0.5;
		
		System.out.println("Updating graphs...");
		// progressively merge all graphs into the model graph
		for (int index = 1; index < distroGraphs.length; index++)
		{
			// update learning parameter to use weight averaging
			lr = 1 - (index / (double)(index + 1));
			
			modelGraph.mergeGraph(distroGraphs[index], lr);
		}
	}
	
	/**
	 * Simple getter for accessing the model graph
	 * @return the model graph
	 */
	public DocumentNGramGraph getModel() {
		return modelGraph;
	}
	
}
