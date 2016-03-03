package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.MappingIterator;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceType;
import acceptance.AcceptanceStreett.StreettPair;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.PrismException;
import prism.PrismLangException;
import explicit.LTLModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.MDPFinallyTransformer.BadStatesTransformation;

public class MDPLTLTransformer extends MDPConditionalTransformer
{
	private static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};

	private final LTLProductTransformer<MDP> ltlTransformer;
	private LTLModelChecker ltlModelChecker;

	public MDPLTLTransformer(final MDPModelChecker modelChecker) throws PrismException
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
		return ltlTransformer.canHandle(model, objective.getExpression());
	}

	@Override
	public ConditionalReachabilitiyTransformation<MDP, MDP> transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		final Expression objective = ((ExpressionProb) expression.getObjective()).getExpression();
		final LabeledDA objectiveDA = ltlTransformer.constructDA(model, objective, ACCEPTANCE_TYPES);

		// 2) Condition: build omega automaton
		final Expression condition = expression.getCondition();
		final LabeledDA conditionDA = ltlTransformer.constructDA(model, condition, ACCEPTANCE_TYPES);

		// 3) Bad States Transformation
		final BadStatesTransformation transformation = transform(model, objectiveDA, conditionDA, statesOfInterest);

		// 4) Reset Transformation
		return transformReset(transformation, statesOfInterest);
	}

	protected BadStatesTransformation transform(final MDP model, final LabeledDA objectiveDA, final LabeledDA conditionDA, final BitSet statesOfInterest)
			throws PrismException, UndefinedTransformationException
	{
		final BadStatesTransformation badStatesTransformation;
		final AcceptanceType objectiveAcceptanceType = objectiveDA.getAutomaton().getAcceptance().getType();
		final AcceptanceType conditionAcceptanceType = conditionDA.getAutomaton().getAcceptance().getType();
		final ModelTransformation<MDP, MDP> product;
		if (objectiveAcceptanceType == AcceptanceType.REACH) {
			// 1) LTL Product Transformation for Objective
			final LTLProduct<MDP> objectiveProduct = ltlTransformer.constructProduct(model, objectiveDA, statesOfInterest);
			product = objectiveProduct;
			final MDP objectiveModel = objectiveProduct.getTransformedModel();
			final BitSet objectiveGoalStates = ltlTransformer.getGoalStates(objectiveProduct);
			final BitSet transformedStatesOfInterest = objectiveProduct.getTransformedStatesOfInterest();

			// 2) Bad States Transformation
			final MDPLTLConditionTransformer ltlConditionTransformer = new MDPLTLConditionTransformer(modelChecker);
			badStatesTransformation = ltlConditionTransformer.transformBadStates(objectiveModel, objectiveGoalStates, conditionDA.liftToProduct(objectiveProduct), transformedStatesOfInterest);
		} else if (conditionAcceptanceType  == AcceptanceType.REACH) {
			// 1) LTL Product Transformation for Condition
			final LTLProduct<MDP> conditionProduct = ltlTransformer.constructProduct(model, conditionDA, statesOfInterest);
			product = conditionProduct;
			final MDP conditionModel = conditionProduct.getTransformedModel();
			final BitSet conditionGoalStates = ltlTransformer.getGoalStates(conditionProduct);
			final BitSet transformedStatesOfInterest = conditionProduct.getTransformedStatesOfInterest();

			// 2) Bad States Transformation
			final MDPLTLObjectiveTransformer ltlObjectiveTransformer = new MDPLTLObjectiveTransformer(modelChecker);
			badStatesTransformation = ltlObjectiveTransformer.transformBadStates(conditionModel, objectiveDA.liftToProduct(conditionProduct), conditionGoalStates, transformedStatesOfInterest);
		} else {
			checkAcceptanceType(objectiveAcceptanceType);
			checkAcceptanceType(conditionAcceptanceType);

			// 1) LTL Product Transformation for Condition
			final LTLProduct<MDP> conditionProduct = ltlTransformer.constructProduct(model, conditionDA, statesOfInterest);
			product = conditionProduct;
			final MDP conditionModel = conditionProduct.getProductModel();
			final BitSet conditionGoalStates = ltlTransformer.getGoalStates(conditionProduct);
			final BitSet conditionStatesOfInterest = conditionProduct.getTransformedStatesOfInterest();

			checkSatisfiability(conditionModel, conditionGoalStates, conditionStatesOfInterest);

			// 2) LTL Product Transformation for Objective
			final LTLProduct<MDP> objectiveAndConditionProduct = ltlTransformer.constructProduct(conditionModel, objectiveDA.liftToProduct(conditionProduct), conditionStatesOfInterest);
			final MDP objectiveAndConditionModel = objectiveAndConditionProduct.getProductModel();

			// 3) Lift Condition Acceptance
			final AcceptanceStreett conditionAcceptanceLifted = new AcceptanceStreett();
			for (StreettPair streettPair : (AcceptanceStreett) conditionProduct.getAcceptance()) {
				BitSet R = objectiveAndConditionProduct.liftFromModel(streettPair.getR());
				BitSet G = objectiveAndConditionProduct.liftFromModel(streettPair.getG());
				conditionAcceptanceLifted.add(new StreettPair(R, G));
			}

			// 4) Conjunction of Objective and Condition Acceptance
			final AcceptanceStreett objectiveAndConditionAcceptance = new AcceptanceStreett();
			objectiveAndConditionAcceptance.addAll((AcceptanceStreett) objectiveAndConditionProduct.getAcceptance());
			objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);

			// 5) Objective & Condition Goal States
			final BitSet objectiveAndConditionGoalStates = ltlModelChecker.findAcceptingECStates(objectiveAndConditionModel, objectiveAndConditionAcceptance);

			// 5) BadStates Transformation
			//    bad states == {s | Pmin=0[<> Condition]}
			final BitSet badStates = ltlModelChecker.findAcceptingECStates(objectiveAndConditionModel, conditionAcceptanceLifted.complementToRabin());
			// reduce number of transitions, i.e.
			// - reset only from r-states of streett acceptance
			// - do not reset from goal states
			final BitSet rStates = BitSetTools.union(new MappingIterator.From<>(conditionAcceptanceLifted, StreettPair::getR));
			badStates.and(rStates);
			badStates.andNot(objectiveAndConditionGoalStates);

			badStatesTransformation = new BadStatesTransformation(objectiveAndConditionProduct, objectiveAndConditionGoalStates, badStates);
		}

		// 3) Compose Transformations
		final ModelTransformationNested<MDP, MDP, MDP> transformation = new ModelTransformationNested<>(product, badStatesTransformation);
		return new BadStatesTransformation(transformation, badStatesTransformation.getGoalStates(), badStatesTransformation.getBadStates());
	}

	protected void checkAcceptanceType(final AcceptanceType objectiveAcceptanceType) throws PrismException
	{
		if (objectiveAcceptanceType != AcceptanceType.STREETT) {
			throw new PrismException("unsupported acceptance type: " + objectiveAcceptanceType);
		}
	}
}