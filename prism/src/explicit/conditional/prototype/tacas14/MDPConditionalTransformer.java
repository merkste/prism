package explicit.conditional.prototype.tacas14;

import java.util.BitSet;

import common.iterable.IterableBitSet;
import explicit.BasicModelExpressionTransformation;
import explicit.Distribution;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.MinMax;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.conditional.NewConditionalTransformer;
import explicit.conditional.prototype.ConditionalReachabilitiyTransformation;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

@Deprecated
public abstract class MDPConditionalTransformer extends NewConditionalTransformer.Basic<explicit.MDP, MDPModelChecker>
{
	public MDPConditionalTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleModelType(Model model)
	{
		return model instanceof explicit.MDP;
	}

	protected void redirectChoices(final MDPSimple model, final BitSet states, final int target1, final int target2, final double[] probabilities)
	{
		for (Integer state : new IterableBitSet(states)) {
			final double probability = probabilities[state];
			final Distribution distribution = new Distribution();
			if (probability != 0.0) {
				distribution.add(target1, probability);
			}
			if (1.0 - probability != 0.0) {
				distribution.add(target2, 1.0 - probability);
			}
			model.clearState(state);
			model.addChoice(state, distribution);
		}
	}

	protected void addDiracChoice(final MDPSimple transformedModel, final Integer from, final int to, final String action)
	{
		final Distribution distribution = new Distribution();
		distribution.add(to, 1.0);
		if (action == null) {
			transformedModel.addChoice(from, distribution);
		} else {
			transformedModel.addActionLabelledChoice(from, distribution, action);
		}
	}

	@Override
	public boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!(model instanceof explicit.MDP)) {
			return false;
		}
		final explicit.MDP mdp = (explicit.MDP) model;
		return canHandleCondition(mdp, expression) && canHandleObjective(mdp, expression);
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final OpRelOpBound oprel = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	@Override
	public abstract boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException;

	@Override
	public ModelExpressionTransformation<explicit.MDP, explicit.MDP> transform(explicit.MDP model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		ConditionalReachabilitiyTransformation<explicit.MDP,explicit.MDP> transformation = transformReachability(model, expression, statesOfInterest);
		// construct expression Pmax=? [ F "goal" ]
		BitSet goalStates                 = transformation.getGoalStates();
		String goalString                 = transformation.getTransformedModel().addUniqueLabel("goal", goalStates);
		ExpressionTemporal finallyGoal    = Expression.Finally(new ExpressionLabel(goalString));
		ExpressionProb probMaxFinallyGoal = new ExpressionProb(finallyGoal, MinMax.max(), "=", null);
		// wrap in new transformation
		return new BasicModelExpressionTransformation<>(transformation, expression, probMaxFinallyGoal);
	}

	public abstract ConditionalReachabilitiyTransformation<explicit.MDP, explicit.MDP> transformReachability(explicit.MDP model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException;
}