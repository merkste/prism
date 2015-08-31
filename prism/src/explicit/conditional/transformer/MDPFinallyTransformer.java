package explicit.conditional.transformer;

import java.util.BitSet;

import common.BitSetTools;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.ModelTransformation;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.UndefinedTransformationException;
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
	public ConditionalMDPTransformation transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkStatesOfInterest(statesOfInterest);

		// 1) Condition: compute "condition goal states"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		final BitSet conditionGoalStates = modelChecker.checkExpression(model, conditionGoal, null).getBitSet();

		// 2) Objective: extract objective
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();

		return transform(model, objective, conditionGoalStates, statesOfInterest);
	}

	public ConditionalMDPTransformation transform(final MDP model, final BitSet objectiveGoalStates, final Expression condition, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1) Condition: compute "condition goal states"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(condition)).getOperand2();
		final BitSet conditionGoalStates = modelChecker.checkExpression(model, conditionGoal, null).getBitSet();

		return transform(model, objectiveGoalStates, conditionGoalStates, statesOfInterest);
	}

	public ConditionalMDPTransformation transform(final MDP model, final ExpressionProb objective, final BitSet conditionGoalStates,
			final BitSet statesOfInterest) throws PrismException
	{
		// 1) Objective: compute "objective goal states"
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objective.getExpression())).getOperand2();
		final BitSet objectiveGoalStates = modelChecker.checkExpression(model, objectiveGoal, null).getBitSet();

		return transform(model, objectiveGoalStates, conditionGoalStates, statesOfInterest);
	}

	public ConditionalMDPTransformation transform(final MDP model, final BitSet objectiveGoalStates, final BitSet conditionGoalStates,
			final BitSet statesOfInterest) throws PrismException
	{
		// FIXME consider moving checks outwards and inserting an assertion
		checkStatesOfInterest(statesOfInterest);
		//    check whether the condition is satisfiable in the state of interest
		final BitSet noPathToCondition = modelChecker.prob0(model, null, conditionGoalStates, false, null);
		if (!BitSetTools.areDisjoint(noPathToCondition, statesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		// 1) Normal Form Transformation
		final GoalFailStopTransformer normalFormTransformer = new GoalFailStopTransformer(modelChecker);
		final ModelTransformation<MDP, MDP> normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoalStates,
				conditionGoalStates);

		//    compute "bad states" == {s | Pmin=0[<> (Obj or Cond)]}
		// FIXME ALG: prove simplification: bad states == {s | Pmin=0[<> (Cond)]}
		final BitSet badStates = modelChecker.prob0(model, null, conditionGoalStates, true, null);

		//    reset transformation
		final BitSet normalFormStatesOfInterest = normalFormTransformation.mapToTransformedModel(statesOfInterest);
		assert normalFormStatesOfInterest.cardinality() == 1 : "expected one and only one state of interest";
		final int targetState = normalFormStatesOfInterest.nextSetBit(0);
		final int failState = model.getNumStates() + GoalFailStopTransformer.FAIL;
		final FailStateResetTransformer resetTransformer = new FailStateResetTransformer(modelChecker);
		final ModelTransformation<MDP, MDP> resetTransformation = resetTransformer.transformModel(normalFormTransformation.getTransformedModel(),
				badStates, targetState, failState);

		// FIXME ALG: consider restriction to part reachable from states of interest

		// 2) Create Mapping
		// FIXME ALG: consider ModelExpressionTransformationNested
		// FIXME ALG: refactor to remove tedious code duplication
		final Integer[] mapping = new Integer[model.getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			final Integer normalFormState = normalFormTransformation.mapToTransformedModel(state);
			final Integer resetState = resetTransformation.mapToTransformedModel(normalFormState);
			mapping[state] = resetState;
		}

		final int goalState = model.getNumStates() + GoalFailStopTransformer.GOAL;
		final BitSet goalStates = BitSetTools.asBitSet(goalState);

		return new ConditionalMDPTransformation(model, resetTransformation.getTransformedModel(), mapping, goalStates);
	}
}