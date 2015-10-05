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

import dataset.Pair;

/**
 * A simple Java class that splits a dataset into 
 * training and test subsets and performs n-fold cross validation.
 * @author VHarisop
 */
public class DataSplitter {

	private File[] fileList;
	private int n; 
	private int chunkSize;

	private Pair[] limits;

	/**
	 * Create a DataSplitter object for n-fold cross 
	 * validation given a directory containing a
	 * dataset and the order of folding
	 * @param order the order (n) of folding
	 * @param directory the directory containing the dataset
	 */
	public DataSplitter(int order, String directory) throws IllegalArgumentException {
		n = order; 
		
		// populate the filelist
		File dir = new File(directory);
		fileList = dir.listFiles(new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".txt");
			}
		});

		if (n >= fileList.length) {
			throw new IllegalArgumentException("N is larger than #data");
		}

		// split the dataset into training and test sets
		splitDataset();
	}

	/**
	 * Populates a list of Pair(x, y) denoting the starting/ending indices 
	 * of the n folds of the dataset
	 */
	private void splitDataset() {
		// get the length of the data to split into n chunks
		int dataLen = fileList.length;
		
		// set the size of each chunk
		chunkSize = dataLen / n; 

		limits = new Pair[n];
		for (int i = 0; i < n; i++) {
			// get limits of testing chunks
			// calculate ending index
			int endIndex = 
				((i + 1) * chunkSize) < dataLen ? ((i+1) * chunkSize) : dataLen;

			limits[i] = new Pair(i * chunkSize, endIndex);
		}
	}
	
	/**
	 * Returns a list of strings containing the files used for
	 * the test set of the Nth fold 
	 * @param n the number of the fold requested
	 * @return a list of filenames belonging to the test set
	 */
	public File[] getNthTest(int n) {
		File[] testSet = new File[chunkSize];
		int offset = limits[n].x;
		for (int i = 0; i < chunkSize; ++i) {
			testSet[i] = fileList[i + offset];
		}
		return testSet;
	}
	
	public File[] getFilelist() {
		return fileList;
	}
	
	/**
	 * Return the indices of the Nth test set, to 
	 * be used in file filtering and cross validation
	 * @param n the number of the requested set
	 * @return a Pair object containing start/end indices
	 */
	public Pair getNthTestIndices(int n) {
		return limits[n];
	}
	
	/**
	 * Print the start and end indices for 
	 * the N folds of the dataset. 
	 * Used for debugging purposes
	 */
	public void debugPrint() {
		for (Pair i: limits) {
			System.out.println(i);
		}
	}

}
