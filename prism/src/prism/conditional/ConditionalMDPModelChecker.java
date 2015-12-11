package prism.conditional;

import explicit.MinMax;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.ModelChecker;
import prism.ModelExpressionTransformation;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.StateValues;
import prism.StateValuesMTBDD;

public class ConditionalMDPModelChecker extends ConditionalModelChecker<NondetModel> {
	
	protected NondetModelChecker mc;

	public ConditionalMDPModelChecker(NondetModelChecker mc, Prism prism) {
		super(prism);
		this.mc = mc;
	}
	
	public StateValues checkExpression(final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException {
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(mc.getConstantValues());
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return checkExpressionMin(model, expression, statesOfInterest);
		}
		StateValues result = checkExpressionMax(model, expression, statesOfInterest);

		return result;
	}

	public StateValues checkExpressionMax(final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException {
		double n = JDD.GetNumMinterms(statesOfInterest, model.getNumDDRowVars());
		if (n != 1) {
			throw new PrismException("Currently, only a single state of interest is supported for MDP conditionals, got "+n);
		}

		try {
			final ConditionalTransformer<NondetModelChecker, NondetModel> transformer = selectModelTransformer(model, expression);
			final ModelExpressionTransformation<NondetModel, NondetModel> transformation = transformModel(transformer, model, expression, statesOfInterest);
			final StateValues resultTransformed = checkExpressionTransformedModel(transformation);

			final StateValues resultOriginal = transformation.projectToOriginalModel(resultTransformed);
			transformation.clear();

			return resultOriginal;
		} catch (UndefinedTransformationException e) {
			// the condition is unsatisfiable for the state of interest
			ExpressionProb expr = (ExpressionProb) expression.getObjective();
			if (expr.getProb() != null) {
				// P with bound -> false
				return new StateValuesMTBDD(JDD.Constant(0.0), model);	
			} else {
				// =? -> not a number
				return new StateValuesMTBDD(JDD.Constant(Double.NaN), model);
			}
		}
	}

	private StateValues checkExpressionMin(final NondetModel model, final ExpressionConditional expression, final JDDNode statesOfInterest) throws PrismLangException, PrismException {
		// P>x [prop] <=>	Pmin=?[prop]	> x
		//					1-Pmax=?[!prop]	> x
		//					Pmax=?[!prop]	< 1-x
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(mc.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMin(): "Pmin expected: " + expression;
		final Expression inverseExpression;
		if (oprel.isNumeric()) {
			final ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), MinMax.max(), RelOp.COMPUTE_VALUES.toString(), null);
			inverseExpression = Expression.Minus(Expression.Literal(1), new ExpressionConditional(inverseObjective, expression.getCondition()));
		} else {
			final RelOp inverseRelop = oprel.getRelOp().negate(true);  // negate but keep strictness
			final Expression inverseProb = Expression.Minus(Expression.Literal(1), objective.getProb());
			final ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), inverseRelop.toString(), inverseProb);
			inverseExpression = new ExpressionConditional(inverseObjective, expression.getCondition());
		}
		// Debug output
		prism.getLog().println("Transform Pmin conditional expression to "+inverseExpression);
		return mc.checkExpression(inverseExpression, statesOfInterest);
	}

	
	private ModelExpressionTransformation<NondetModel,NondetModel> transformModel(final ConditionalTransformer<NondetModelChecker, NondetModel> transformer, final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException {
		// Debug output
		// ModelPrinter.exportToDotFile(model, "../conditional/conditional_mc_original.dot", target);
		prism.getLog().println("\nTransforming model (using " + transformer.getClass().getSimpleName() + ") for " + expression);
		long timer = System.currentTimeMillis();
		final ModelExpressionTransformation<NondetModel, NondetModel> transformation = 
				(ModelExpressionTransformation<NondetModel, NondetModel>) transformer.transform(model, expression, statesOfInterest);
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("Time for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().println("Transformed model information:");
		transformation.getTransformedModel().printTransInfo(prism.getLog(), false);
		// Debug output
		// ModelPrinter.exportToDotFile(this, "../conditional/conditional_mc_transformed.dot", untilTransforamtion.mapStates(target));
		return transformation;
	}

	private ConditionalTransformer<NondetModelChecker, NondetModel> selectModelTransformer(final ProbModel model, final ExpressionConditional expression) throws PrismException {
		boolean forceLtlCondition = prism.getSettings().getBoolean(PrismSettings.PRISM_ALL_PATHFORMULAS_VIA_LTL);

		if (!forceLtlCondition && 
			new MDPFinallyTransformer(mc, prism).canHandle(model, expression)) {
			return new MDPFinallyTransformer(mc, prism);
		}

		if(new MDPLTLTransformer(mc, prism).canHandle(model, expression)) {
			return new MDPLTLTransformer(mc, prism);
		}

		throw new PrismException("Cannot model check " + expression);
	}

	private StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<NondetModel, NondetModel> transformation) throws PrismException {
		final NondetModel transformedModel = transformation.getTransformedModel();
		final Expression transformedExpression = transformation.getTransformedExpression();

		prism.getLog().println("\nChecking reachability ("+transformedExpression+") in transformed model ...");
		long timer = System.currentTimeMillis();

		ModelChecker mcTransformed = mc.createModelChecker(transformedModel);
		
		final StateValues resultProduct = mcTransformed.checkExpression(transformedExpression, transformation.getTransformedStatesOfInterest());
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("Time for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return resultProduct;
	}
}
