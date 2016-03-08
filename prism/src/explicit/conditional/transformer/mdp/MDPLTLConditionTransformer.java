package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.MappingIterator;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceStreett.StreettPair;
import acceptance.AcceptanceType;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;
import explicit.LTLModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.ModelTransformationNested;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.GoalFailTransformer;
import explicit.conditional.transformer.GoalFailTransformer.GoalFailTransformation;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.mdp.MDPFinallyTransformer.BadStatesTransformation;

public class MDPLTLConditionTransformer extends MDPConditionalTransformer
{
	private static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};

	private final LTLProductTransformer<MDP> ltlTransformer;
	private LTLModelChecker ltlModelChecker;

	public MDPLTLConditionTransformer(final MDPModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<>(modelChecker);
		ltlModelChecker = new LTLModelChecker(this);
	}

	@Override
	protected boolean canHandleCondition(final MDP model, final ExpressionConditional expression) throws PrismLangException
	{
		return ltlTransformer.canHandle(model, expression.getCondition());
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

		// 1) Objective: compute "objective goal states"
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objective.getExpression())).getOperand2();
		final BitSet objectiveGoalStates = modelChecker.checkExpression(model, objectiveGoal, null).getBitSet();

		// 2) Condition: build omega automaton
		final Expression condition = expression.getCondition();
		final LabeledDA conditionDA = ltlTransformer.constructDA(model, condition, ACCEPTANCE_TYPES);

		// 3) Bad States Transformation
		final BadStatesTransformation transformation = transformBadStates(model, objectiveGoalStates, conditionDA, statesOfInterest);

		// 4) Reset Transformation
		return transformReset(transformation, statesOfInterest);
	}

	protected BadStatesTransformation transformBadStates(final MDP model, final BitSet objectiveGoalStates, final LabeledDA conditionDA,
			final BitSet statesOfInterest) throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		final LTLProduct<MDP> product = ltlTransformer.constructProduct(model, conditionDA, statesOfInterest);
		final BitSet conditionGoalStates = ltlTransformer.getGoalStates(product);
		final MDP conditionModel = product.getProductModel();
		final BitSet objectiveGoalStatesLifted = product.liftFromModel(objectiveGoalStates);

		// 2) Bad States Transformation
		final BadStatesTransformation badStatesTransformation;
		switch (product.getAcceptance().getType()) {
		case REACH:
			final BitSet transformedStatesOfInterest = product.getTransformedStatesOfInterest();
			final MDPFinallyTransformer finallyTransformer = new MDPFinallyTransformer(modelChecker);
			badStatesTransformation = finallyTransformer.transformBadStates(conditionModel, objectiveGoalStatesLifted, conditionGoalStates, transformedStatesOfInterest);
			break;
		case STREETT:
			checkSatisfiability(model, conditionGoalStates, statesOfInterest);
			badStatesTransformation = transformBadStates(product, objectiveGoalStatesLifted, conditionGoalStates);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}

		final ModelTransformationNested<MDP, MDP, MDP> transformation = new ModelTransformationNested<>(product, badStatesTransformation);
		return new BadStatesTransformation(transformation, badStatesTransformation.getGoalStates(), badStatesTransformation.getBadStates());
	}

	protected BadStatesTransformation transformBadStates(final LTLProduct<MDP> product, final BitSet objectiveGoalStates, final BitSet conditionGoalStates) throws PrismException
	{
		final MDP model = product.getTransformedModel();

		// 1) Normal Form Transformation
		final GoalFailTransformer.MDP normalFormTransformer = new GoalFailTransformer.MDP(modelChecker);
		final GoalFailTransformation<MDP> normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoalStates, conditionGoalStates);

		// 2) Bad States Transformation
		//    bad states == {s | Pmin=0[<> Condition]}
		final AcceptanceStreett conditionAcceptance = (AcceptanceStreett) product.getAcceptance();
		final BitSet badStates = ltlModelChecker.findAcceptingECStates(model, conditionAcceptance.complementToRabin());
		// reduce number of transitions, i.e.
		// - reset only from r-states of streett acceptance
		// - do not reset from goal states
		final BitSet rStates = BitSetTools.union(new MappingIterator.From<>(conditionAcceptance, StreettPair::getR));
		badStates.and(rStates);
		badStates.andNot(objectiveGoalStates);
		// reset from fail state as well
		badStates.set(normalFormTransformation.getFailState());

		return new BadStatesTransformation(normalFormTransformation, badStates);
	}
}