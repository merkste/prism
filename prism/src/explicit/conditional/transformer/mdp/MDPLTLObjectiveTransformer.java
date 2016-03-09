package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import acceptance.AcceptanceType;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.ModelTransformationNested;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.GoalStopTransformer;
import explicit.conditional.transformer.GoalStopTransformer.GoalStopTransformation;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.mdp.MDPFinallyTransformer.BadStatesTransformation;

// FIXME ALG: prove correctness of transformation
public class MDPLTLObjectiveTransformer extends MDPConditionalTransformer
{
	private static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};

	private final LTLProductTransformer<MDP> ltlTransformer;

	public MDPLTLObjectiveTransformer(final MDPModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<>(modelChecker);
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
		return ltlTransformer.canHandle(model, objective.getExpression());
	}

	@Override
	public ConditionalReachabilitiyTransformation<MDP, MDP> transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		final Expression objective = ((ExpressionProb) expression.getObjective()).getExpression();
		final LabeledDA objectiveDA = ltlTransformer.constructDA(model, objective, ACCEPTANCE_TYPES);

		// 2) Condition: compute "condition goal states"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		final BitSet conditionGoalStates = modelChecker.checkExpression(model, conditionGoal, null).getBitSet();

		// 3) Bad States Transformation
		final BadStatesTransformation transformation = transformBadStates(model, objectiveDA, conditionGoalStates, statesOfInterest);

		// 4) Reset Transformation
		return transformReset(transformation, statesOfInterest);
	}

	protected BadStatesTransformation transformBadStates(final MDP model, final LabeledDA objectiveDA, final BitSet conditionGoalStates,
			final BitSet statesOfInterest) throws PrismException
	{
		checkSatisfiability(model, conditionGoalStates, statesOfInterest);

		// 1) LTL Product Transformation for Objective
		final LTLProduct<MDP> product = ltlTransformer.constructProduct(model, objectiveDA, statesOfInterest);
		final BitSet objectiveGoalStates = ltlTransformer.findAcceptingStates(product);
		final MDP objectiveModel = product.getProductModel();
		final BitSet conditionGoalStatesLifted = product.liftFromModel(conditionGoalStates);

		// 2) Bad States Transformation
		final BadStatesTransformation badStatesTransformation;
		switch (product.getAcceptance().getType()) {
		case REACH:
			final BitSet transformedStatesOfInterest = product.getTransformedStatesOfInterest();
			final MDPFinallyTransformer finallyTransformer = new MDPFinallyTransformer(modelChecker);
			badStatesTransformation = finallyTransformer.transformBadStates(objectiveModel, objectiveGoalStates, conditionGoalStatesLifted, transformedStatesOfInterest);
			break;
		case STREETT:
			badStatesTransformation = transformBadStates(objectiveModel, objectiveGoalStates, conditionGoalStatesLifted);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}

		// 3) Compose Transformations
		final ModelTransformationNested<MDP, MDP, MDP> transformation = new ModelTransformationNested<>(product, badStatesTransformation);
		return new BadStatesTransformation(transformation, badStatesTransformation.getGoalStates(), badStatesTransformation.getBadStates());
	}

	protected BadStatesTransformation transformBadStates(final MDP model, final BitSet objectiveGoalStates, final BitSet conditionGoalStates) throws PrismException
	{
		// 1) Normal Form Transformation
		final GoalStopTransformer.MDP normalFormTransformer = new GoalStopTransformer.MDP(modelChecker);
		final GoalStopTransformation<MDP> normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoalStates, conditionGoalStates);

		// 2) Bad States Transformation
		//    bad states == {s | Pmin=0[<> Condition]}
		final BitSet badStates = modelChecker.prob0(model, null, conditionGoalStates, true, null);

		return new BadStatesTransformation(normalFormTransformation, badStates);
	}
}