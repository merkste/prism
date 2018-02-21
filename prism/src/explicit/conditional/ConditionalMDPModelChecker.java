package explicit.conditional;

import java.util.BitSet;
import java.util.SortedSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuant;
import parser.ast.RelOp;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.MDPSparse;
import explicit.MinMax;
import explicit.ModelExpressionTransformation;
import explicit.StateValues;
import explicit.conditional.NewFinallyUntilTransformer;
import explicit.conditional.prototype.virtual.FinallyLtlTransformer;
import explicit.conditional.prototype.virtual.FinallyUntilTransformer;
import explicit.conditional.prototype.virtual.LtlLtlTransformer;
import explicit.conditional.prototype.virtual.LtlUntilTransformer;
import explicit.conditional.transformer.MdpTransformerType;
import explicit.conditional.transformer.UndefinedTransformationException;
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
		ExpressionQuant<?> objective = expression.getObjective();
		if (! (objective instanceof ExpressionProb)) {
			throw new PrismNotSupportedException("Can only model check conditional probabilities in MDPs, not " + expression.getClass().getSimpleName());
		}

		OpRelOpBound oprel = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return checkExpressionMin(model, expression, statesOfInterest);
		}
		return checkExpressionMax(model, expression, statesOfInterest);
	}

	private StateValues checkExpressionMax(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMax() : "Pmax expected: " + expression;

		NewConditionalTransformer<MDP, MDPModelChecker> transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			throw new PrismNotSupportedException("Cannot model check " + expression);
		}

		StateValues result;
		try {
			ModelExpressionTransformation<MDP, ? extends MDP> transformation = transformModel(transformer, model, expression, statesOfInterest);
			StateValues resultTransformedModel = checkExpressionTransformedModel(transformation, statesOfInterest);
			result = transformation.projectToOriginalModel(resultTransformedModel);
		} catch (UndefinedTransformationException e) {
			mainLog.println("\nTransformation failed: " + e.getMessage());
			result = createUndefinedStateValues(model, expression);
		}

		return result;
	}

	private StateValues checkExpressionMin(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismLangException, PrismException
	{
		// P>x [prop] <=> Pmin=?[prop]    > x
		//                1-Pmax=?[!prop] > x
		//                Pmax=?[!prop]   < 1-x
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMin() : "Pmin expected: " + expression;

		final Expression inverseExpression;
		if (oprel.isNumeric()) {
			ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), MinMax.max(), RelOp.COMPUTE_VALUES.toString(), null);
			inverseExpression               = Expression.Minus(Expression.Literal(1), new ExpressionConditional(inverseObjective, expression.getCondition()));
		} else {
			RelOp inverseRelop              = oprel.getRelOp().negate(true); // negate but keep strictness
			Expression inverseProb          = Expression.Minus(Expression.Literal(1), objective.getProb());
			ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), MinMax.max(), inverseRelop.toString(), inverseProb);
			inverseExpression = new ExpressionConditional(inverseObjective, expression.getCondition());
		}
		// Debug output
		getLog().println("Transform Pmin conditional expression to "+inverseExpression);
		return modelChecker.checkExpression(model, inverseExpression, statesOfInterest);
	}

	public ModelExpressionTransformation<MDP, ? extends MDP> transformModel(final NewConditionalTransformer<MDP, MDPModelChecker> transformer, final MDP model, final ExpressionConditional expression,
			final BitSet statesOfInterest) throws PrismException
	{
		mainLog.println("\nTransforming model (using " + transformer.getName() + ") for " + expression);
		long overallTime = System.currentTimeMillis();
		ModelExpressionTransformation<MDP, ? extends MDP> transformation = transformer.transform(model, expression, statesOfInterest);
		long transformationTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nTime for model transformation: " + transformationTime / 1000.0 + " seconds.");

		MDP transformedModel = transformation.getTransformedModel();
		if (isVirtualModel(transformedModel)  || transformedModel instanceof MDPSimple) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_VIRTUAL_MODELS)) {
				mainLog.println("Using simple/virtual model");
			} else {
				mainLog.println("\nConverting simple/virtual model to " + MDPSparse.class.getSimpleName());
				long buildTime                   = System.currentTimeMillis();
				MDPSparse transformedModelSparse = new MDPSparse(transformedModel);
				buildTime = System.currentTimeMillis() - buildTime;
				mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
				// build in transformation
				BasicModelTransformation<MDP, ? extends MDP> sparseTransformation = new BasicModelTransformation<>(transformation.getTransformedModel(), transformedModelSparse);
				sparseTransformation = sparseTransformation.compose(transformation);
				// attach transformed expression
				Expression originalExpression    = transformation.getOriginalExpression();
				Expression transformedExpression = transformation.getTransformedExpression();
				transformation                   = new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
			}
		}
		overallTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nOverall time for model transformation: " + overallTime / 1000.0 + " seconds.");
		mainLog.print("Transformed model has ");
		mainLog.println(transformedModel.infoString());
		return transformation;
	}

	public boolean isVirtualModel(final MDP model)
	{
		return (model instanceof ModelView) && ((ModelView) model).isVirtual();
	}

	public StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<MDP, ? extends MDP> transformation, final BitSet statesOfInterest) throws PrismException
	{
		MDP transformedModel               = transformation.getTransformedModel();
		Expression transformedExpression   = transformation.getTransformedExpression();
		BitSet transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();

		mainLog.println("\nChecking transformed property in transformed model: " + transformedExpression);
		long timer     = System.currentTimeMillis();
		StateValues sv = modelChecker.checkExpression(transformedModel, transformedExpression, transformedStatesOfInterest);
		timer          = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return sv;
	}

	public NewConditionalTransformer<MDP, MDPModelChecker> selectModelTransformer(final MDP model, final ExpressionConditional expression) throws PrismException
	{
		final String specification                = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_RESET);
		final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);

		NewConditionalTransformer<MDP, MDPModelChecker> transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
			for (MdpTransformerType type : types) {
				switch (type) {
				case FinallyFinally:
					transformer = new explicit.conditional.prototype.tacas14.MDPFinallyTransformer(modelChecker);
					break;
				case FinallyLtl:
					transformer = new explicit.conditional.prototype.tacas14.MDPLTLConditionTransformer(modelChecker);
					break;
				case LtlLtl:
					transformer = new explicit.conditional.prototype.tacas14.MDPLTLTransformer(modelChecker);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
			}
		} else if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
			for (MdpTransformerType type : types) {
				switch (type) {
				case FinallyFinally:
					transformer = new FinallyUntilTransformer.MDP(modelChecker);
					break;
				case LtlFinally:
					transformer = new LtlUntilTransformer.MDP(modelChecker);
					break;
				case FinallyLtl:
					transformer = new FinallyLtlTransformer.MDP(modelChecker);
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
		} else {
			for (MdpTransformerType type : types) {
				switch (type) {
				case FinallyFinally:
					transformer = new NewFinallyUntilTransformer.MDP(modelChecker);
					break;
				case LtlFinally:
					transformer = new NewLtlUntilTransformer.MDP(modelChecker);
					break;
				case FinallyLtl:
					transformer = new NewFinallyLtlTransformer.MDP(modelChecker);
					break;
				case LtlLtl:
					transformer = new NewLtlLtlTransformer.MDP(modelChecker);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
			}
		}
		return null;
	}
}