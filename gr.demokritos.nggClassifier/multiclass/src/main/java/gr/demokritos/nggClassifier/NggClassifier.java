/*
This file is part of nggSpamFilter.

nggSpamFilter is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

nggSpamFilter is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with nggSpamFilter.  If not, see <http://www.gnu.org/licenses/>.

Copyright (C) Vasileios Charisopoulos, 2015

*/

package gr.demokritos.nggClassifier;

import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.documentModel.representations.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.PrintStream;

import gr.demokritos.dataTools.*;

import java.util.Arrays;

/**
 * A simple Java class that performs k-ary classification 
 * tasks using folded (i.e. n-fold validation) data sets.
 * Each dataset should be in its own subfolder.
 * @author VHarisop
 * 
 * Examples:
 * 		String baseDir = args[0];
 * 
 * 		// create new classifier with 10-fold cross validation
 * 		ConfusionMatrix cnf = new NggClassifier(baseDir);
 *
 */
public class NggClassifier {

	private boolean usesValidation = false;
	private String[] classLabels;
	private File[] dataDirs; 		// directories for data
	private DataSplitter[] dtsp;	// data splitters for each category
	private int order; 				// order of folding for validation
	
	// models[i] is the model graph for the i-th category
	private DocumentNGramGraph[] models;
	
	private NGramCachedGraphComparator ngc; 
	
	/**
	 * Creates a classifier using N-gram graphs
	 * given a list of directories containing datasets for 
	 * each category. Every dataset is split into a number of
	 * disjoint test sets (k-folding)
	 * @param dataDirs the list of directories
	 * @param order the order of folding for validation
	 */
	public NggClassifier(File[] dataDirs, int order)
	{
		// save the data directories and the order of folding
		this.dataDirs = dataDirs;
		this.order = order;

		// enable validation-related methods
		usesValidation = true;
		
		// allocate data splitter array
		dtsp = new DataSplitter[dataDirs.length];
		
		// Create a data splitter for each category
		for (int i = 0; i < dataDirs.length; ++i) {
			dtsp[i] = new DataSplitter(order, dataDirs[i].getAbsolutePath());
		}
		
		// initialize a graph comparator
		ngc = new NGramCachedGraphComparator();
	}
	
	/**
	 * Creates a classifier using N-gram graphs given a list of
	 * directories split into subfolders containing train and test sets
	 * for each category
	 * @param baseDir the base directory for all datasets
	 */
	public NggClassifier(String baseDir) {
		File dir = new File(baseDir);
		dataDirs = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		
		initLabels(dir);
		
		// initialize a graph comparator
		ngc = new NGramCachedGraphComparator();
		
		// create models based on training sets
		createModels();
	}
	
	public NggClassifier(String baseDir, int order) {

		this.order = order;

		// enable validation-related methods
		usesValidation = true;

		File dir = new File(baseDir);
		dataDirs = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		

		initLabels(dir);
		
		// allocate data splitter array
		dtsp = new DataSplitter[dataDirs.length];
				
		// Create a data splitter for each category
		for (int i = 0; i < dataDirs.length; ++i) {
			dtsp[i] = new DataSplitter(order, dataDirs[i].getAbsolutePath());
		}
		
		// initialize a graph comparator
		ngc = new NGramCachedGraphComparator();
	}
	
	/**
	 * Initialize the class labels of the dataset
	 * @param dir the root directory of the dataset
	 */
	private void initLabels(File dir) {
		
		classLabels = dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
	}
	
	/**
	 * Simple getter for the classifier's class labels
	 * @return an array of strings containing the class labels
	 */
	public String[] getLabels() {
		return classLabels;
	}


	/**
	 * Compute a set of SVM features 
	 * for both the training and the testing 
	 * sets and output them to files.
	 */
	public void exportSvmFeatures() {

		PrintStream stdout = System.out;
		exportSvmFeatures(true);  	// training set
		exportSvmFeatures(false); 	// test set

		// restore original stdout
		System.setOut(stdout);


	}

	/**
	 * Compute a set of SVM features for a dataset
	 * and print them to stdout
	 * @param type a boolean denoting if we should use the
	 * training set or the test set
	 */
	private void exportSvmFeatures(boolean type) {
	
		/* TODO: make this code more concise */

		String ext = "/Test";
		String name = "svmtest.txt";
		if (type) { 
			ext = "/Train";
			name = "svmtrain.txt"; 
		}		

		// try to redirect output
		try {
			FileOutputStream f = new FileOutputStream(name);
			System.setOut(new PrintStream(f));
		} 
		catch (Exception ex) {
			ex.printStackTrace();
		}


		for (int i = 0; i < dataDirs.length; i++) {
			// get a set of document nggs for the set
			// iterate(i) for each category
			DocumentNGramGraph[] nggs = 
				Modeller.extractGraphs(dataDirs[i].getAbsolutePath() + ext);

			// double[][] valueSims = new double[nggs.length][models.length];
			for (int j = 0; j < nggs.length; j++) {
		
				// output class label first
				System.out.print(i + " ");
				for (int k = 0; k < models.length; k++) {

					// compute a vector of value similarities,
					// one value for each model(k), one vector for
					// each document(j)
					// valueSims[j][k] = computeSimilarity(nggs[j], k);
					double valueSim = computeSimilarity(nggs[j], k);

					// k + 1 because feature indexing starts from 1 in LibSVM
					System.out.printf("%d:%f ", k + 1, valueSim);
				}
				
				System.out.println();
			}
		}
	}
	
	/**
	 * Perform binary classification on a given set of test data
	 * @param n the index of the test set to use
	 * @return the resulting confusion matrix
	 */
	public ConfusionMatrix classify(int n) {
		
		if (!usesValidation) {
			throw new UnsupportedOperationException("No folding order specified");
		}

		createModels(n);
		// return the resulting confusion matrix
		return classify_all_categories(n);
	}
	
	/**
	 * Perform classification using all 
	 * the available splits as test sets
	 * @return an array of confusion matrices
	 */
	public ConfusionMatrix[] classify() {
		
		ConfusionMatrix[] matrices = new ConfusionMatrix[order];
		for (int i = 0; i < order; ++i) {
			matrices[i] = classify(i);
		}
		return matrices;
	}

	/**
	 * Perform n-fold validation by running classification
	 * on the n disjoint test sets resulting from the data split
	 * @return the average accuracy calculated
	 */
	public double cross_validate() {
		
		double accSum = 0;
		for (int i = 0; i < order; ++i) {
			accSum += classify(i).accuracy();
		}
		
		return accSum / order;
	}
	
	
	/** 
	 * Creates the dataset's model graphs.
	 * Also performs maximal common subset removal to come up
	 * with a more distinctive graph for each category
	 */
	public void createModels() {
		models = new DocumentNGramGraph[dataDirs.length];
		String trainPath;
		
		// create all models using data in Train/ dirs
		for (int i = 0; i < models.length; ++i) {
			trainPath = dataDirs[i].getAbsolutePath() + "/Train";
			models[i] = new Modeller(trainPath).getModel();
		}
		
		DocumentNGramGraph maxSub = computeMaxSubset();
		removeNoise(maxSub);
	}
	
	/**
	 * Creates the dataset's model graphs.
	 * Also performs maximal common subset removal to come up
	 * with a more distinctive graph for each category.
	 * @param n the order of data folding to use
	 */
	private void createModels(int n) {
		
		Pair[] clims = new Pair[dataDirs.length];
		models = new DocumentNGramGraph[dataDirs.length];
		
		// get test set limits for all models
		for (int i = 0; i < models.length; ++i) {
			
			clims[i] = dtsp[i].getNthTestIndices(n);
			models[i] = new Modeller(dataDirs[i].getAbsolutePath(), clims[i]).getModel();
		}
		
		// TODO: Replace with code generalized to K categories
		// get the maximal common subset
		DocumentNGramGraph maxSub = computeMaxSubset();
		removeNoise(maxSub);
		
	}
	
	
	/**
	 * Computes the maximal common subset of the model n-gram graphs
	 * @return the maximal common subset of the model graphs
	 */
	private DocumentNGramGraph computeMaxSubset() {
		DocumentNGramGraph maxSubset = models[0];
		
		// approximate the max subset
		for (int i = 1; i < models.length; ++i) {
			maxSubset = maxSubset.intersectGraph(models[i]);
		}
		
		return maxSubset;
	}
	
	/**
	 * Remove maximal common subset from all model graphs, reducing
	 * the noise present in each model to assist in classification
	 * @param maxSub the maximal common subset
	 */
	private void removeNoise(DocumentNGramGraph maxSub) {
		for (int i = 0; i < models.length; ++i) {
			models[i] = models[i].allNotIn(maxSub);
		}
	}
	
	/**
	 * Computes the maximal common subset of 2 n-gram graphs and
	 * removes it from both to reduce noise in classification tasks. 
	 * @param wdg a document n-gram graph
	 * @param otherWdg another document n-gram graph
	 * @return the pair of graphs with the maximal common subset removed
	 */
	public static DocumentNGramGraph[] removeNoise(DocumentNGramGraph wdg,
			 DocumentNGramGraph otherWdg)
	{
		DocumentNGramGraph maxSubset = otherWdg.intersectGraph(wdg);
		return new DocumentNGramGraph[] { wdg.allNotIn(maxSubset), otherWdg.allNotIn(maxSubset) };
	}
	
	/**
	 * Performs classification on all categories and puts the results
	 * in a confusion matrix.
	 * @param n the order of data fold from which to draw the test sets
	 * @return the resulting confusion matrix 
	 */
	private ConfusionMatrix classify_all_categories(int n) {
		int ctgs = models.length;
		
		int[][] ctrows = new int[ctgs][ctgs];
		for (int i = 0; i < ctgs; ++i) {
			ctrows[i] = classify_category(i, n);
		}
		
		return (new ConfusionMatrix(ctgs, ConfusionMatrix.flattenSeqs(ctrows)));
	}
	
	/**
	 * Performs classification on all categories and puts the 
	 * results in a confusion matrix
	 * @return the resulting confusion matrix
	 */
	public ConfusionMatrix classify_all_categories() {
		int ctgs = models.length;
		int [][] ctrows = new int[ctgs][ctgs];
		for (int i = 0; i < ctgs; ++i) {
			ctrows[i] = classify_category(i);
		}
		
		return (new ConfusionMatrix(ctgs, ConfusionMatrix.flattenSeqs(ctrows)));
	}
	
	
	/**
	 * Perform classification on a test set of a given category
	 * using the test data resulting from the data split
	 * @param ctg the category of the test set's data
	 * @param n the index of the fold of the data
	 * @return an array of ints, corresponding to the category's
	 * 		   row in the confusion matrix
	 */
	private int[] classify_category(int ctg, int n) {
		
		// a row of values initialized to 0
		int[] ctRow = new int[models.length];
		
		// get the list of files
		File[] filenameList = dtsp[ctg].getNthTest(n);
		
		for (File s: filenameList) {
			DocumentNGramGraph ngg = new DocumentNGramGraph();
			try {
				// TODO: find full path! (used to be hamDir + "/" + s)
				ngg.loadDataStringFromFile(s.getAbsolutePath());
				
				// update the assigned category's row
				ctRow[classify_candidate(ngg)] += 1;
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return ctRow;
	}
	
	/**
	 * Perform classification on a test set of a given category
	 * using the test data resulting from the data split
	 * @param ctg the category of the test set's data
	 * @return an array of ints, corresponding to the category's
	 * 		   row in the confusion matrix
	 */
	private int[] classify_category(int ctg) {
		
		// a row of values initialized to 0
		int[] ctRow = new int[models.length];
		
		// get the list of files
		File dirPath = new File(dataDirs[ctg].getAbsolutePath() + "/Test");
		File[] filenameList = dirPath.listFiles(new FileFilter() {
			public boolean accept(File path) {
				return path.isFile();
			}
		});
		
		for (File s: filenameList) {
			DocumentNGramGraph ngg = new DocumentNGramGraph();
			try {
				// TODO: find full path! (used to be hamDir + "/" + s)
				ngg.loadDataStringFromFile(s.getAbsolutePath());
				
				// update the assigned category's row
				ctRow[classify_candidate(ngg)] += 1;
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return ctRow;
	}
	
	/**
	 * Decides whether the candidate graph should be 
	 * classified as belonging to a certain category. 
	 * @param cng the candidate graph
	 * @return the index of the category {@code cng} is assigned to
	 */
	private int classify_candidate(DocumentNGramGraph cng)
	{
		double max_similarity = -1; double curSim;
		int winning_category = 0;
		
		// test against every model graph
		for (int i = 0; i < models.length; ++i) {
			curSim = computeSimilarity(cng, i);
			
			// if similarity was larger, update result
			if (curSim > max_similarity) {
				max_similarity = curSim;
				winning_category = i;
			}
		}
		return winning_category;
	}
	
	/**
	 * Computes the similarity between the candidate graph
	 * and a given model graph
	 * @param cng the candidate graph
	 * @param mdl the category of the model graph
	 * @return the value similarity between the graphs
	 */
	private double computeSimilarity(DocumentNGramGraph cng, int mdl) {
		GraphSimilarity gs = ngc.getSimilarityBetween(cng, models[mdl]);
		return gs.ValueSimilarity;
	}
	
	
}
