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

import gr.demokritos.iit.jinsect.documentModel.representations.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

import dataset.*;

/**
 * A simple Java class that creates an N-gram graph representation
 * for an email in text form.
 * @author VHarisop
 *
 */
public class Modeller {

	private File[] fileList;
	private DocumentNGramGraph[] distroGraphs;
	private DocumentNGramGraph modelGraph;
	
	private static boolean useThreads = true;
	private int numThreads = 4;

	/** 
	 * static setter for enabling/disabling threading
	 */
	public static void setThreading(boolean setting) {
		useThreads = setting;
	}

	/**
	 * Creates an instance of Modeller from a given directory
	 * that contains .txt files.
	 * @param dirPath the directory path
	 */
	public Modeller(String dirPath) {	
		File directory = new File(dirPath);
		
		// save the directory .corpus files to a filelist
		fileList = directory.listFiles(new FileFilter() {
			public boolean accept(File path) {
				return path.isFile();
			}
		});
		
		if (useThreads) {
			initGraphsThreaded(dirPath);
			updateGraphsThreaded();
		}
		else {
			initGraphs(dirPath);
			updateGraphs(); 
		}
	}
	
	/** 
	 * Creates an instance of Modeller from a given directory
	 * that has been split into training and test sets
	 * @param dirPath the directory of the set
	 * @param limit a Pair object showing the indices of the test set
	 */
	public Modeller(String dirPath, Pair limit) {
		
		File directory = new File(dirPath);
		
		fileList = directory.listFiles(new FileFilter() {
			public boolean accept(File path) {
				return path.isFile();
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
		
		distroGraphs = new DocumentNGramGraph[fileList.length - limit.range()];
		String filename; int run_index = 0;
		
		System.out.println("Reading graphs...");
		// create an array of N-Gram graphs, one for each corpus
		for (int index = 0; index < fileList.length; index++) {
			
			try {
				
				// if not in limits, then use for training
				if (!limit.includes(index)) {
					filename = fileList[index].getAbsolutePath();
				
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
		
		distroGraphs = new DocumentNGramGraph[fileList.length];
			
		String filename;
		System.out.print("Reading graphs...");
		// create an array of N-Gram graphs, one for each corpus
		for (int index = 0; index < fileList.length; index++) {
			
			try {
				filename = fileList[index].getAbsolutePath();
				
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

	private void initGraphsThreaded(String dirPath) {
		distroGraphs = new DocumentNGramGraph[fileList.length];

		final int sz = (distroGraphs.length / numThreads) + 1;
		final String dirp = dirPath;
		Thread[] tids = new Thread[8];

		for (int i = 0; i < numThreads; ++i) {
			tids[i] = new Thread("Thread-" + i) {
				public void run() {
					int tid = (int) Thread.currentThread().getId() % numThreads;
					int startInd = tid * sz;
					int endInd = (tid + 1) * sz;
					// get max(myend, nggs.length)
					endInd = (endInd > distroGraphs.length) ? distroGraphs.length : endInd; 
					
					for (int j = startInd; j < endInd; j++) {
						try {   
							String filename = fileList[j].getAbsolutePath();
							distroGraphs[j] = new DocumentNGramGraph();
							distroGraphs[j].loadDataStringFromFile(filename);
						} catch (Exception ex) { ex.printStackTrace(); }
					}

					System.out.printf("Thread %d finished: %d-%d\n", tid, startInd, endInd);

				}
			};
			tids[i].start();
		}

		// wait for all threads to finish
		for (int i = 0; i < numThreads; ++i) {
			try {
				tids[i].join();
			}
			catch (InterruptedException ex) { ex.printStackTrace(); }
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
			// update learning parameter to use weight averaging
			lr = 1 - (index / (double)(index + 1));
			
			modelGraph.mergeGraph(distroGraphs[index], lr);
		}
	}

	private void updateGraphsThreaded() {
		
		final int sz = (distroGraphs.length / numThreads) + 1;
		final DocumentNGramGraph[] mdls = new DocumentNGramGraph[numThreads];

		Thread[] tids = new Thread[numThreads];

		for (int i = 0; i < numThreads; ++i) {
			tids[i] = new Thread("Thread-" + i) {
				public void run() {
					int tid = (int) Thread.currentThread().getId() % numThreads;
					int startInd = tid * sz;
					int endInd = (tid + 1) * sz;
					// get max(myend, nggs.length)
					endInd = (endInd > distroGraphs.length) ? distroGraphs.length : endInd; 
					
					double lr = 0.5;
					mdls[tid] = distroGraphs[startInd];

					// merge intermediate model graphs
					for (int j = startInd + 1; j < endInd; j++) {
						mdls[tid].mergeGraph(distroGraphs[j], lr);
						lr = 1 - (j / (double)(j + 1));
					}

					System.out.printf("Thread %d finished merging: %d-%d\n", tid, startInd, endInd);
				}
			};
			tids[i].start();
		}

		// wait for all threads to finish
		for (int i = 0; i < numThreads; ++i) {
			try {
				tids[i].join();
			}
			catch (InterruptedException ex) { ex.printStackTrace(); }
		}

		// merge all intermediate models as well 
		modelGraph = mdls[0]; 
		double lr = 0.5;
		for (int i = 1; i < numThreads; ++i) {
			modelGraph.mergeGraph(mdls[i], lr);
			lr = 1 - (i / (double)(i + 1));
		}
	}
	
	/**
	 * Simple getter for accessing the model graph
	 * @return the model graph
	 */
	public DocumentNGramGraph getModel() {
		return modelGraph;
	}


	/**
	 * Extract a set of DocumentNGramGraphs for a 
	 * given directory of documents 
	 * @param baseDir the root of the document directory
	 * @return an array of nggs, one for each document
	 */
	public static DocumentNGramGraph[] extractGraphs(String baseDir) {

		// extract all filenames into a list
		File[] fileList = (new File(baseDir)).listFiles(new FileFilter() {
			public boolean accept(File path) {
				return path.isFile();
			}
		});

		// allocate an array of nggs
		DocumentNGramGraph[] nggs = new DocumentNGramGraph[fileList.length];
		for (int i = 0; i < fileList.length; i++) {
			nggs[i] = new DocumentNGramGraph();

			try {
				nggs[i].loadDataStringFromFile(fileList[i].getAbsolutePath());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} 

		return nggs;
	}

	/**
	 * Simple getter for accessing the full array of 
	 * document graphs
	 * @return the array of graphs for all documents
	 */
	public DocumentNGramGraph[] getGraphs() {
		return distroGraphs;
	}
	
}
