package quantile;

import jdd.JDD;
import jdd.JDDNode;


/**
 * Quantile case: 
 *   Quantile(Un, P<p [ F<=? b])
 *   Quantile(Un, P<p [ a U<=? b])
 *
 */
public class ReachabilityUpperRewardBoundUniversal extends ReachabilityUpperRewardBound {

	public ReachabilityUpperRewardBoundUniversal(QuantileCalculator qc, QuantileCalculatorContext qcc) {
		super(qc, qcc);
	}

	@Override
	public boolean min() {
		// do min
		return true;
	}
	
	public JDDNode getZeroStates(boolean forIterationZero) {
		JDDNode zeroStates = super.getZeroStates(forIterationZero);
		
		if (forIterationZero) {
			// we are universal, the existence of positive
			// reward transitions for some state that is not a goal state
			// => 0
			JDDNode statesWithPosR = qcc.getStatesWithPosRewardTransitions();
			statesWithPosR = JDD.And(statesWithPosR,
			                         qcc.getModel().getReach().copy());
			statesWithPosR = JDD.And(statesWithPosR,
			                         JDD.Not(getOneStates(forIterationZero)));
			
			zeroStates = JDD.Or(zeroStates, statesWithPosR);
		}
		
		return zeroStates;
	}
}
