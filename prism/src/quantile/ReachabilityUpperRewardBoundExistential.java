package quantile;

/**
 * Quantile case: 
 *   Quantile(Ex, P<p [ F<=? b])
 *   Quantile(Ex, P<p [ a U<=? b])
 *
 */
public class ReachabilityUpperRewardBoundExistential extends ReachabilityUpperRewardBound {

	public ReachabilityUpperRewardBoundExistential(QuantileCalculator qc, QuantileCalculatorContext qcc)
	{
		super(qc, qcc);
	}

	@Override
	public boolean min() {
		// do maximum
		return false;
	}
}
