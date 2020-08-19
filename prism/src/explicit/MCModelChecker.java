package explicit;

import java.util.BitSet;

import parser.ast.ExpressionConditional;
import prism.PrismException;

public interface MCModelChecker<M extends DTMC>
{
	DTMCModelChecker createDTMCModelChecker() throws PrismException;

	BitSet prob0(M model, BitSet remain, BitSet target, PredecessorRelation pre) throws PrismException;

	BitSet prob1(M model, BitSet remain, BitSet target, PredecessorRelation pre) throws PrismException;

	// FIXME ALG: Make generic in model type?
	StateValues checkExpressionConditional(Model model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException;

	// FIXME ALG: Make generic in model type?
	ModelCheckerResult computeSteadyStateProbsForBSCC(DTMC dtmc, BitSet states, double result[]) throws PrismException;
}
