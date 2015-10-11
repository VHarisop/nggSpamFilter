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

package gr.demokritos.dataTools;

/**
 * Represents the confusion matrix for a classification problem 
 * over a number of classes
 * @author VHarisop
 *
 */
public class ConfusionMatrix {
	
	private int numClasses;			// number of class labels
	private int[][] confMatrix;		// confusion matrix
	
	public ConfusionMatrix(int _numClasses) {
		numClasses = _numClasses;
		confMatrix = new int[numClasses][numClasses];
	}
	
	public ConfusionMatrix(int _numClasses, int[] seqs) {
		numClasses = _numClasses;
		confMatrix = new int[numClasses][numClasses];
		setMatrix(seqs);
	}
	
	/**
	 * Prints the confusion matrix in a pretty format
	 */
	public void printMatrix() {
		System.out.println("Confusion Matrix");
		
		for (int i = 0; i < numClasses; ++i) {
			for (int j = 0; j < numClasses; ++j) {
				System.out.print(confMatrix[i][j] + "\t");
			}
			System.out.println();
		}
	}
	
	/**
	 * Sets the confusion matrix based on a given a sequence of integers
	 * @param seq a one-dimensional integer sequence (i.e. a flattened conf. matrix) 
	 */
	public void setMatrix(int[] seq) throws AssertionError {
		assert(seq.length == (numClasses * numClasses));
		int row, column;
		
		for (int i = 0; i < seq.length; ++i) {
			row = i / numClasses;
			column = i % numClasses;
			confMatrix[row][column] = seq[i];
		}
	}

	/**
	 * Calculate the false positive labellings on 
	 * a category, by summing the appropriate column
	 * @param classNum the index of the class
	 * @return the number of false positives
	 */
	private int falsePositives(int classNum) {
		int fp = 0;
		for (int i = 0; i < numClasses; i++) {
			if (i == classNum) 
				continue;
			fp += confMatrix[i][classNum];
		}

		return fp;
	}
	
	/**
	 * Calculate the false negative labellings on 
	 * a category, by summing the appropriate column
	 * @param classNum the index of the class
	 * @return the number of false negatives
	 */
	private int falseNegatives(int classNum) {
		int fn = 0;
		for (int i = 0; i < numClasses; i++) {
			if (i == classNum) 
				continue;
			fn += confMatrix[classNum][i];
		}

		return fn;
	}

	/**
	 * Returns the precision and recall for a given class
	 * @param classNum the index of the class
	 * @return a two-element array containing precision and recall
	 */
	public double[] precisionAndRecall(int classNum) {
		int hits = confMatrix[classNum][classNum];
		int precDenom = 0; int recDenom = 0;
		
		for (int i = 0; i < numClasses; i++) {
			precDenom += confMatrix[i][classNum];
			recDenom += confMatrix[classNum][i];
		}
		
		return new double[] { ((double) hits / precDenom), ((double) hits / recDenom) };
	}

	/**
	 * Calculates the precision for a given class
	 * @param classNum the index of the class
	 * @return the class precision
	 */
	public double precision(int classNum) {
		int hits = confMatrix[classNum][classNum];
		int denom = 0;
		for (int i = 0; i < numClasses; ++i) {
			denom += confMatrix[i][classNum];
		}

		return ((double) hits / denom);
	}

	/**
	 * Calculates the recall for a given class
	 * @param classNum the index of the class
	 * @return the class recall
	 */
	public double recall(int classNum) {
		int hits = confMatrix[classNum][classNum];
		int denom = 0;
		for (int i = 0; i < numClasses; ++i) {
			denom += confMatrix[classNum][i];
		}

		return ((double) hits / denom);
	}

	/**
	 * Returns the F1 score for a given class
	 * @param classNum the index of the class
	 * @return the F1 score
	 */
	public double f1Score(int classNum) {
		double [] stats = precisionAndRecall(classNum);
		return getF1Score(stats[0], stats[1]);
	}
	
	/**
	 * Returns the F1 score given the precision and recall of a class
	 * @param precision the precision value
	 * @param recall the recall value
	 * @return the F1 score
	 */
	public static double getF1Score(double precision, double recall) {
		return 2 * precision * recall / (precision + recall);
	}
	
	/**
	 * Calculates the macro-averaged recall
	 * @return the macro-averaged recall 
	 */
	public double macroAvgRecall() {
		double sum = 0;
		for (int i = 0; i < numClasses; ++i) {
			sum += recall(i);
		}

		return (sum / numClasses);
	}

	/**
	 * Calculates the macro-averaged precision
	 * @return the macro-averaged precision 
	 */
	public double macroAvgPrecision() {
		double sum = 0;
		for (int i = 0; i < numClasses; ++i) {
			sum += precision(i);
		}

		return (sum / numClasses);
	}

	/**
	 * Calculates the macro-averaged F1 score
	 * @return the F1 score
	 */
	public double macroAvgF1Score() {
		double sum = 0;
		for (int i = 0; i < numClasses; ++i) {
			sum += f1Score(i);
		}

		return (sum / numClasses);
	}
	
	/**
	 * Calculates the micro-averaged F1 score
	 * @return the F1 score
	 */
	public double microAvgF1Score() {
		return getF1Score(microAvgPrecision(), microAvgRecall());
	}

	/** 
	 * Computes the micro-average recall score
	 * @return the micro-averaged recall
	 */
	public double microAvgRecall() {
		int sum = 0;
		int hits = 0;
		for (int i = 0; i < numClasses; i++) {
			sum += falseNegatives(i); 
			hits += confMatrix[i][i];
		}

		// return (sum(TP_i) / sum(TP_i + FN_i))
		return (((double) hits) / (sum + hits));
	}

	/** 
	 * Computes the micro-average precision score
	 * @return the micro-averaged precision
	 */
	public double microAvgPrecision() {
		int sum = 0;
		int hits = 0;
		for (int i = 0; i < numClasses; i++) {
			sum += falsePositives(i); 
			hits += confMatrix[i][i];
		}

		// return (sum(TP_i) / sum(TP_i + FP_i))
		return (((double) hits) / (sum + hits));
	}

	/**
	 * Returns the accuracy score of the confusion matrix
	 * @return the total accuracy
	 */
	public double accuracy() {
		int hits = 0; int total = 0;
		
		// calculate hits by traversing the array
		for (int i = 0; i < numClasses; ++i) {
			for (int j = 0; j < numClasses; ++j) {
				total += confMatrix[i][j];
				
				// hits are located on the diagonal
				if (j == i) { hits += confMatrix[i][i]; }
			}
		}
		
		return (double) hits / (total);
	}
	
	/**
	 * Returns the misclassification rate extracted
	 * from the confusion matrix 
	 * @return the misclassification rate
	 */
	public double missRate() {
		return (1 - accuracy());
	}
	
	/**
	 * Concatenates two 1D arrays into one
	 * @param seqA the first array
	 * @param seqB the second array
	 * @return the concatenated array 
	 */
	private static int[] combineSeqs(int[] seqA, int[] seqB) {
		int[] toRet = new int[seqA.length + seqB.length];
		System.arraycopy(seqA, 0, toRet, 0, seqA.length);
		System.arraycopy(seqB, 0, toRet, seqA.length, seqB.length);
		
		return toRet;
	}
	
	/**
	 * Flattens a sequence of int arrays into a one-dimensional array
	 * @param sequences a series of int[] arrays (varargs)
	 * @return the flattened array
	 */
	public static int[] flattenSeqs(int[] ... sequences) {
		int[] toRet = new int[0];
		for (int i = 0; i < sequences.length; i++) {
			toRet = combineSeqs(toRet, sequences[i]);
		}
		
		return toRet;
	}
	
	
}
