package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.ModelTransformation;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.ReachabilityTransformation;
import explicit.conditional.transformer.mdp.GoalFailStopTransformer.GoalFailStopTransformation;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

public class MDPFinallyTransformer extends MDPConditionalTransformer
{
	public MDPFinallyTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	protected boolean canHandleCondition(final MDP model, final ExpressionConditional expression)
	{
		final Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	protected boolean canHandleObjective(final MDP model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	public ConditionalReachabilitiyTransformation<MDP, MDP> transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: extract objective
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objective.getExpression())).getOperand2();
		final BitSet objectiveGoalStates = modelChecker.checkExpression(model, objectiveGoal, null).getBitSet();

		// 2) Condition: compute "condition goal states"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		final BitSet conditionGoalStates = modelChecker.checkExpression(model, conditionGoal, null).getBitSet();

		// 3) Bad States Transformation
		final BadStatesTransformation transformation = transformBadStates(model, objectiveGoalStates, conditionGoalStates, statesOfInterest);

		// 4) Reset Transformation
		return transformReset(transformation, statesOfInterest);
	}

	protected BadStatesTransformation transformBadStates(final MDP model, final BitSet objectiveGoalStates, final BitSet conditionGoalStates,
			final BitSet statesOfInterest) throws PrismException
	{
		checkSatisfiability(model, conditionGoalStates, statesOfInterest);

		// 1) Normal Form Transformation
		final GoalFailStopTransformer normalFormTransformer = new GoalFailStopTransformer(modelChecker);
		final GoalFailStopTransformation<MDP> normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoalStates, conditionGoalStates);

		// 2) Bad States Transformation
		//    bad states == {s | Pmin=0[<> Condition]}
		final BitSet badStates = modelChecker.prob0(model, null, conditionGoalStates, true, null);
		// reset from fail state as well
		badStates.set(normalFormTransformation.getFailState());

		return new BadStatesTransformation(normalFormTransformation, badStates);
	}

	public static class BadStatesTransformation extends BasicModelTransformation<MDP, MDP> implements ReachabilityTransformation<MDP, MDP>
	{
		private final BitSet badStates;
		private final BitSet goalStates;

		public BadStatesTransformation(final ReachabilityTransformation<MDP, MDP> transformation, final BitSet badStates)
		{
			this(transformation, transformation.getGoalStates(), badStates);
		}

		public BadStatesTransformation(final ModelTransformation<MDP, MDP> transformation, final BitSet goalStates, final BitSet badStates)
		{
			super(transformation);
			this.goalStates = goalStates;
			this.badStates = badStates;
		}

		public BitSet getBadStates()
		{
			return badStates;
		}

		@Override
		public BitSet getGoalStates()
		{
			return goalStates;
		}
	}
}