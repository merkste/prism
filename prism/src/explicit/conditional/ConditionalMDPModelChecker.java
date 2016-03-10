package explicit.conditional;

import java.util.BitSet;
import java.util.SortedSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import parser.type.TypeDouble;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;
import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSparse;
import explicit.MinMax;
import explicit.ModelCheckerResult;
import explicit.ModelTransformation;
import explicit.StateValues;
import explicit.conditional.transformer.ConditionalTransformer;
import explicit.conditional.transformer.FinallyFinallyTransformer;
import explicit.conditional.transformer.LtlConditionTransformer;
import explicit.conditional.transformer.LtlLtlTransformer;
import explicit.conditional.transformer.LtlObjectiveTransformer;
import explicit.conditional.transformer.MdpTransformerType;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;
import explicit.modelviews.ModelView;

//FIXME ALG: add comment
public class ConditionalMDPModelChecker extends ConditionalModelChecker<MDP>
{
	protected MDPModelChecker modelChecker;

	public ConditionalMDPModelChecker(MDPModelChecker mc)
	{
		super(mc);
		this.modelChecker = mc;
	}

	@Override
	public StateValues checkExpression(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		assert expression.getObjective() instanceof ExpressionProb;

		final ExpressionProb objective = (ExpressionProb) expression.getObjective();

		OpRelOpBound oprel = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return checkExpressionMin(model, expression, statesOfInterest);
		}
		StateValues result = checkExpressionMax(model, expression, statesOfInterest);

		if (objective.getProb() != null) {
			// compare against bound
			final double bound = objective.getProb().evaluateDouble(modelChecker.getConstantValues());
			if (bound < 0 || bound > 1) {
				throw new PrismException("Invalid probability bound " + bound + " in P operator");
			}
			final BitSet bits = result.getBitSetFromInterval(objective.getRelOp(), bound);
			result.clear();
			result = StateValues.createFromBitSet(bits, model);
		}
		return result;
	}

	private StateValues checkExpressionMax(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		final ConditionalTransformer<MDP> transformer = selectModelTransformer(model, expression);
		StateValues result;
		try {
			final ConditionalReachabilitiyTransformation<MDP, MDP> transformation = transformModel(transformer, model, expression, statesOfInterest);
			final StateValues resultTransformedModel = checkExpressionTransformedModel(transformation, statesOfInterest);
			result = transformation.projectToOriginalModel(resultTransformedModel);
		} catch (UndefinedTransformationException e) {
			result = new StateValues(TypeDouble.getInstance(), Double.NaN, model);
		}
		return result;
	}

	private StateValues checkExpressionMin(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismLangException, PrismException
	{
		// P>x [prop] <=>	Pmin=?[prop]	> x
		//					1-Pmax=?[!prop]	> x
		//					Pmax=?[!prop]	< 1-x
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();

		OpRelOpBound oprel = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMin() : "Pmin expected: " + expression;

		final Expression inverseExpression;

		if (oprel.isNumeric()) {
			final ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), MinMax.max(), RelOp.COMPUTE_VALUES.toString(),
					null);
			inverseExpression = Expression.Minus(Expression.Literal(1), new ExpressionConditional(inverseObjective, expression.getCondition()));
		} else {
			final RelOp inverseRelop = oprel.getRelOp().negate(true); // negate but keep strictness

			final Expression inverseProb = Expression.Minus(Expression.Literal(1), objective.getProb());
			final ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), inverseRelop.toString(), inverseProb);
			inverseExpression = new ExpressionConditional(inverseObjective, expression.getCondition());
		}
		// Debug output
		mainLog.println(inverseExpression);
		return modelChecker.checkExpression(model, inverseExpression, statesOfInterest);
	}

	public ConditionalReachabilitiyTransformation<MDP, MDP> transformModel(final ConditionalTransformer<MDP> transformer, final MDP model, final ExpressionConditional expression,
			final BitSet statesOfInterest) throws PrismException
	{
		Class<?> type = transformer.getClass();
		type = type.getEnclosingClass() == null ? type : type.getEnclosingClass();
		mainLog.println("\nTransforming model (using " + type.getSimpleName() + ") for " + expression);
		long overallTime = System.currentTimeMillis();
		ConditionalReachabilitiyTransformation<MDP, MDP> transformation = (ConditionalReachabilitiyTransformation<MDP, MDP>) transformer.transform(model, expression, statesOfInterest);
		long transformationTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nTime for model transformation: " + transformationTime / 1000.0 + " seconds.");

		if (isVirtualModel(transformation.getTransformedModel())) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_VIRTUAL_MODELS)) {
				mainLog.println("Using virtual model");
			} else {
				// FIXME ALG: consider restriction to reachable state space
				mainLog.println("Converting virtual model to " + MDPSparse.class.getSimpleName());
				long buildTime = System.currentTimeMillis();
				final MDP transformedModel = new MDPSparse(transformation.getTransformedModel());
				buildTime = System.currentTimeMillis() - buildTime;
				final ModelTransformation<MDP, MDP> simpleTransformation = new BasicModelTransformation<>(transformation.getOriginalModel(),
						transformedModel, transformation.getMapping());
				transformation = new ConditionalReachabilitiyTransformation<>(simpleTransformation, transformation.getGoalStates());
				mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
			}
		}

		overallTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nOverall time for model transformation: " + overallTime / 1000.0 + " seconds.");
		mainLog.print("Transformed model has ");
		mainLog.println(transformation.getTransformedModel().infoString());
		return transformation;
	}

	public boolean isVirtualModel(final MDP model)
	{
		return (model instanceof ModelView) && ((ModelView) model).isVirtual();
	}

	public StateValues checkExpressionTransformedModel(final ConditionalReachabilitiyTransformation<MDP, MDP> transformation, final BitSet statesOfInterest) throws PrismException
	{
		final MDP transformedModel = transformation.getTransformedModel();
		final BitSet goalStates = transformation.getGoalStates();

		mainLog.println("\nChecking property in transformed model ...");
		long timer = System.currentTimeMillis();
		final ModelCheckerResult result = modelChecker.computeReachProbs(transformedModel, goalStates, false);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return StateValues.createFromDoubleArray(result.soln, transformedModel);
	}

	public ConditionalTransformer<MDP> selectModelTransformer(final MDP model, final ExpressionConditional expression) throws PrismException
	{
		final String specification = settings.getString(PrismSettings.CONDITIONAL_MDP);
		final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);

		ConditionalTransformer<MDP> transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_LEGACY_TRANSFORMATIONS)) {
			for (MdpTransformerType type : types) {
				switch (type) {
				case FinallyFinally:
					transformer = new explicit.conditional.transformer.legacy.MDPFinallyTransformer(modelChecker);
					break;
				case FinallyLtl:
					transformer = new explicit.conditional.transformer.legacy.MDPLTLConditionTransformer(modelChecker);
					break;
				case LtlLtl:
					transformer = new explicit.conditional.transformer.legacy.MDPLTLTransformer(modelChecker);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
				;
			}
		} else {
			for (MdpTransformerType type : types) {
				switch (type) {
				case FinallyFinally:
					transformer = new FinallyFinallyTransformer.MDP(modelChecker);
					break;
				case LtlFinally:
					transformer = new LtlObjectiveTransformer.MDP(modelChecker);
					break;
				case FinallyLtl:
					transformer = new LtlConditionTransformer.MDP(modelChecker);
					break;
				case LtlLtl:
					transformer = new LtlLtlTransformer.MDP(modelChecker);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
			}
		}

		throw new PrismException("Cannot model check " + expression);
	}
}