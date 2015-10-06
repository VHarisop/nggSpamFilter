import java.lang.Math;

public final class Validation {

	private double[] missRates;
	private int ndata;

	/**
	 * Creates a validation object from 
	 * an array of miss rates 
	 * @param missRates an array of misclass. rates
	 */
	public Validation(double[] missRates) {
		this.missRates = missRates;
		ndata = missRates.length;
	}

	/** 
	 * Computes the t value for the
	 * paired-t student sample test
	 */
	public double tVal() {
		double mAvg = 0;
		double mDiffs = 0;

		for (double mr: missRates) { mAvg += mr; }
		mAvg /= ndata;

		// compute the denominator for the student test
		for (double mr: missRates) {
			mDiffs += (mr - mAvg) * (mr - mAvg);
		}
		mDiffs = Math.sqrt(mDiffs / (ndata - 1));

		return (Math.sqrt(ndata) * mAvg / mDiffs);
	}


}
