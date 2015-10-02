import gr.demokritos.iit.jinsect.documentModel.representations.*;

import java.io.File;
import java.io.FilenameFilter;


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
	 * that contains .corpus files.
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
	 * Initializes a set of N-Gram Graphs for each file in the corpus
	 * @param dirPath the directory of the modelling set
	 */
	private void initGraphs(String dirPath) {
		
		distroGraphs = new DocumentNGramGraph[filenameList.length];
			
		String filename;
		System.out.println("Reading graphs...");
		// create an array of N-Gram graphs, one for each corpus
		for (int index = 0; index < filenameList.length; index++) {
			
			if (index % 50 == 0) { System.out.println(index); }
			try {
				filename = dirPath + "/" + filenameList[index];
				
				// create the distribution graphs for the email body
				distroGraphs[index] = new DocumentNGramGraph(); 
				distroGraphs[index].loadDataStringFromFile(filename);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}			
		}
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
			if (index % 50 == 0) { System.out.println(index); }
			
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
