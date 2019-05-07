package explicit;

import java.util.BitSet;

import parser.ast.ExpressionConditional;
import prism.PrismException;

public interface MCModelChecker
{
	DTMCModelChecker createDTMCModelChecker() throws PrismException;

	StateValues checkExpressionConditional(Model model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException;

	ModelCheckerResult computeSteadyStateProbsForBSCC(DTMC dtmc, BitSet states, double result[]) throws PrismException;
}
