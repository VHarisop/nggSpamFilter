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

import java.io.File;
import java.io.FilenameFilter;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;

import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import gr.demokritos.iit.jinsect.documentModel.representations.*;

/**
 * A simple Java class that encapsulates binary (spam vs. ham) 
 * classification tasks using fixed training and test sets
 * @author VHarisop
 *
 */
public class NGramGraphClassifier {

	private String spamDir, hamDir;
	private String spamTestDir, hamTestDir;
	
	private String[] filenameList;
	
	// models[0] is the spam model, models[1] is the ham model
	private DocumentNGramGraph[] models;
	
	private NGramCachedGraphComparator ngc; 
	
	/**
	 * Creates a spam classifier using N-gram graphs
	 * given a training directory
	 * @param _trainDir the directory of the training set
	 */
	public NGramGraphClassifier(String _trainDir)
	{
		spamDir = _trainDir + "/Spam"; hamDir = _trainDir + "/Ham";
		createModels();
	}
	
	/**
	 * Perform binary classification on a given set of test data
	 * @param _testDir the directory of the test set
	 */
	public void classify(String _testDir) {
		spamTestDir = _testDir + "/Spam"; hamTestDir = _testDir + "/Ham";
		
		int[] spamNums = classify_spam(spamTestDir);
		int[] hamNums = classify_ham(hamTestDir);
		
		int [] confData = ConfusionMatrix.flattenSeqs(spamNums, hamNums);
		ConfusionMatrix cnf = new ConfusionMatrix(2, confData);
		
		double [] spamStats = cnf.precisionAndRecall(0);
		double [] hamStats = cnf.precisionAndRecall(1);
		
		// print statistics
		System.out.println("Spam Precision: " + spamStats[0]);
		System.out.println("Spam recall: " + spamStats[1]);
		System.out.println("Spam F1 score: " + cnf.f1Score(0));
		
		System.out.println("Ham Precision: " + hamStats[0]);
		System.out.println("Ham Recall: " + hamStats[1]);
		System.out.println("Ham F1 score: " + cnf.f1Score(1));
	}
	
	/**
	 * Creates the two model graphs.
	 * Also performs maximal common subset removal to come up
	 * with a more distinctive graph for each category.
	 */
	private void createModels() {
		
		INSECTFileDB<DocumentNGramGraph> db = new INSECTFileDB<DocumentNGramGraph>();
		
		if (db.existsObject("spam", "grph") && db.existsObject("ham", "grph")) {
			
			// load objects from file if they exist 
			DocumentNGramGraph spamModel = db.loadObject("spam", "grph");
			DocumentNGramGraph hamModel = db.loadObject("ham", "grph");
			
			models = new DocumentNGramGraph[] { spamModel, hamModel };
		}
		
		else { // models have to be built from the beginning 
			// create the 2 corresponding model graphs
			DocumentNGramGraph spamModel = new Modeller(spamDir).getModel();
			DocumentNGramGraph hamModel = new Modeller(hamDir).getModel();
						
			// create the models, remove noise 
			models = NGramGraphClassifier.removeNoise(spamModel, hamModel);
			
			// save the models for future use
			db.saveObject(models[0], "spam", "grph");
			db.saveObject(models[1], "ham", "grph");
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
	 * Performs binary classification (SPAM vs. HAM) on a set of ham messages
	 * using a pair of n-gram graphs as model for spam and ham messages respectively
	 * @param dirPath the directory of the ham test-set
	 * @return an array of integers denoting false positives and true negatives respectively
	 */
	private int[] classify_ham(String dirPath)
	{
		int hams = 0;
		
		File directory = new File(dirPath);
		ngc = new NGramCachedGraphComparator();
		
		DocumentNGramGraph spamModel = models[0];
		DocumentNGramGraph hamModel = models[1];
		
		// save all .txt files
		filenameList = directory.list(new FilenameFilter() {
			public boolean accept(File dirPath, String fileName) {
				return fileName.endsWith(".txt");
			}
		}); 
		
		
		for (int index = 0; index < filenameList.length; ++index)
		{
			
			DocumentNGramGraph ngg = new DocumentNGramGraph();
			try {
				ngg.loadDataStringFromFile(dirPath + "/" + filenameList[index]);
				if (classify(ngg, hamModel, spamModel)) {
					hams += 1;
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
			
		}
		// ret[0]: false positives
		// ret[1]: true negatives
		return (new int[] {filenameList.length - hams, hams});
	}
	
	/**
	 * Decides whether the candidate graph should be 
	 * classified as belonging to a certain category. 
	 * @param cng the candidate graph
	 * @param wdg the model graph of the first category
	 * @param otherWdg the model graph of the second category
	 * @return True if classified as part of the first category, else False
	 */
	private boolean classify(DocumentNGramGraph cng,
							DocumentNGramGraph wdg, 
						    DocumentNGramGraph otherWdg)
	{
		return (computeSimilarity(cng, wdg) > computeSimilarity(cng, otherWdg));
						    
	}
	
	/**
	 * Computes the similarity between the candidate graph
	 * and a given model graph
	 * @param cng the candidate graph
	 * @param wdg the model graph
	 * @return the value similarity between the graphs
	 */
	private double computeSimilarity(DocumentNGramGraph cng, 
											DocumentNGramGraph wdg) {
		GraphSimilarity gs = ngc.getSimilarityBetween(cng, wdg);
		return gs.ValueSimilarity;
	}
	
	
	/**
	 * Performs binary classification (SPAM vs. HAM) on a set of spam messages
	 * using a pair of n-gram graphs as model for spam and ham messages respectively
	 * @param dirPath the directory of the spam test-set
	 * @return an array of ints denoting true positives and false negatives respectively
	 */
	private int[] classify_spam(String dirPath)						  
	{
		int spams = 0;
		
		File directory = new File(dirPath);
		ngc = new NGramCachedGraphComparator();
		
		DocumentNGramGraph spamModel = models[0];
		DocumentNGramGraph hamModel = models[1];
		
		// save the directory .txt files to a filelist
		filenameList = directory.list(new FilenameFilter() {
			public boolean accept(File dirPath, String fileName) {
				return fileName.endsWith(".txt");
			}
		});
	
		for (int index = 0; index < filenameList.length; index++) {
			
			DocumentNGramGraph ngg = new DocumentNGramGraph();
			try {
				ngg.loadDataStringFromFile(dirPath + "/" + filenameList[index]);
				if (classify(ngg, spamModel, hamModel)) {
					spams += 1;
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		// ret[0]: true positives
		// ret[1]: false negatives
		return (new int[] {spams, filenameList.length - spams});
	}
	
}
