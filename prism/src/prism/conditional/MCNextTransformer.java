package prism.conditional;

import java.util.Objects;

import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.Model;
import prism.ModelChecker;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.transform.BasicModelExpressionTransformation;
import prism.conditional.transform.MCPivotTransformation;
import jdd.JDD;
import jdd.JDDNode;

public abstract class MCNextTransformer<M extends ProbModel, C extends ProbModelChecker> extends NewConditionalTransformer.MC<M, C>
{
	public MCNextTransformer(Prism prism, C modelChecker)
	{
		super(prism, modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		try {
			if (!(condition instanceof ExpressionTemporal && condition.isSimplePathFormula())) {
				// can handle simple path conditions only
				return false;
			}
		} catch (PrismLangException e) {
			// condition cannot be checked whether it is a simple formula
			return false;
		}
		final ExpressionTemporal temporal = (ExpressionTemporal) condition;
		if (!(temporal.getOperator() == ExpressionTemporal.P_X)) {
			// can handle next conditions only
			return false;
		}
		if (temporal.hasBounds()) {
			// can handle unbounded conditions only
			return false;
		}
		if (ExpressionInspector.isSteadyStateReward(expression.getObjective())) {
			// cannot handle steady state computation yet
			return false;
		}
		return true;
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// FIXME ALG: steady state computation
		return !ExpressionInspector.isSteadyStateReward(expression.getObjective());
	}

	@Override
	public ModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();
		ModelTransformation<M, ? extends M> transformation = transformModel(model, condition, statesOfInterest);
		return new BasicModelExpressionTransformation<>(transformation, expression, expression.getObjective());
	}

	protected ModelTransformation<M, ? extends M> transformModel(final M model, final Expression condition, final JDDNode statesOfInterest)
			throws PrismException
	{
//>>> Debug: print states of interest
//		prism.getLog().println("States of interest:");
//		JDD.PrintMinterms(prism.getLog(), statesOfInterest.copy());
//		new StateValuesMTBDD(statesOfInterest.copy(), model).print(prism.getLog());

		final Expression next = ExpressionInspector.normalizeExpression(condition);
		final JDDNode goal    = getGoalStates(model, next);
		final boolean negated = next instanceof ExpressionUnaryOp;

		final JDDNode originProbs = computeProbabilities(model, goal, negated);
		final JDDNode targetProbs = goal.copy();

		final JDDNode support     = JDD.Apply(JDD.GREATERTHAN, originProbs.copy(), JDD.ZERO.copy());
		final boolean satisfiable = JDD.AreIntersecting(support, statesOfInterest);
		JDD.Deref(support);
		if (! satisfiable) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

//>>> Debug: print probabilities
//		prism.getLog().println("Origin probs:");
//		JDD.PrintMinterms(prism.getLog(), originProbs.copy());
//		new StateValuesMTBDD(originProbs.copy(), model).print(prism.getLog());
//		prism.getLog().println("Target probs:");
//		JDD.PrintMinterms(prism.getLog(), targetProbs.copy());
//		new StateValuesMTBDD(targetProbs.copy(), model).print(prism.getLog());

		JDDNode pivots = getPivotStates(model, goal, negated);
		JDD.Deref(goal);
		// switch mode in pivots
		ModelTransformation<M, M> pivotTransformation = new MCPivotTransformation<>(model, pivots, statesOfInterest, false);

//>>> Debug: print pivot states
//		prism.getLog().println("Pivot states:");
//		JDD.PrintMinterms(prism.getLog(), pivots.copy());
//		new StateValuesMTBDD(pivots.copy(), model).print(prism.getLog());

		JDD.Deref(pivots);
		// lift probs
		JDDNode liftedOriginProbs = JDD.Apply(JDD.MAX, originProbs.copy(), ((MCPivotTransformation<M>) pivotTransformation).getAfter());
		JDDNode liftedTargetProbs = JDD.Apply(JDD.MAX, targetProbs.copy(), ((MCPivotTransformation<M>) pivotTransformation).getAfter());
		JDD.Deref(originProbs, targetProbs);

//>>> Debug: print lifted probabilities
//		prism.getLog().println("Lifted origin probs:");
//		JDD.PrintMinterms(prism.getLog(), liftedOriginProbs.copy());
//		new StateValuesMTBDD(liftedOriginProbs.copy(), pivotModel).print(prism.getLog());
//		prism.getLog().println("Lifted target probs:");
//		JDD.PrintMinterms(prism.getLog(), liftedTargetProbs.copy());
//		new StateValuesMTBDD(liftedTargetProbs.copy(), pivotModel).print(prism.getLog());

		M pivotModel = pivotTransformation.getTransformedModel();
		JDDNode pivotStatesOfInterest = pivotTransformation.getTransformedStatesOfInterest();
		liftedOriginProbs = JDD.Times(liftedOriginProbs, pivotModel.getReach().copy());

		MCScaledTransformation<M> scaledTransformation = new MCScaledTransformation<>(prism, pivotModel, liftedOriginProbs.copy(), liftedTargetProbs.copy(), pivotStatesOfInterest.copy());

//>>> Debug: print transformed states of interest
//		prism.getLog().println("Transformed states of interest:");
//		JDD.PrintMinterms(prism.getLog(), scaledTransformation.getTransformedStatesOfInterest());
//		new StateValuesMTBDD(scaledTransformation.getTransformedStatesOfInterest().copy(), pivotModel).print(prism.getLog());

//>>> Debug: print export models as dot
//		try {
//			model.exportToFile(Prism.EXPORT_DOT, true, new File("model.dot"));
//			pivotTransformation.getTransformedModel().exportToFile(Prism.EXPORT_DOT, true, new File("pivotModel.dot"));
//			scaledTransformation.getTransformedModel().exportToFile(Prism.EXPORT_DOT, true, new File("scaledModel.dot"));
//			prism.getLog().println("Debug: export successful");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		JDD.Deref(liftedOriginProbs, liftedTargetProbs, statesOfInterest, pivotStatesOfInterest);

		return new ModelTransformationNested<>(pivotTransformation, scaledTransformation);
	}

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getPivotStates(final ProbModel model, final JDDNode goal, final boolean negated)
	{
		if (! negated) {
			return goal.copy();
		}
		return JDD.Not(goal.copy());
	}

	protected JDDNode getGoalStates(final ProbModel model, final Expression expression) throws PrismException
	{
		ExpressionTemporal next = getExpressionTemporal(expression);
		return checkExpression(model, next.getOperand2());
	}

	protected ExpressionTemporal getExpressionTemporal(final Expression expression) throws PrismLangException
	{
		if (Expression.isNot(expression)) {
			return getExpressionTemporal(((ExpressionUnaryOp) expression).getOperand());
		}
		if (expression instanceof ExpressionTemporal) {
			return (ExpressionTemporal) expression;
		}
		throw new PrismLangException("expected (negated) temporal formula but found", expression);
	}

	protected Expression removeNegation(final Expression expression)
	{
		if (expression instanceof ExpressionUnaryOp) {
			// assume negated formula
			return removeNegation(((ExpressionUnaryOp) expression).getOperand());
		}
		// assume non-negated formula
		return expression;
	}

	protected Expression getConditionGoal(final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ((ExpressionTemporal) removeNegation(condition)).getOperand2();
	}

	protected Expression getObjectiveGoal(final ExpressionConditional expression)
	{
		final Expression objective = expression.getObjective();
		Expression objectiveExpression = null;
		;
		if (ExpressionInspector.isReachablilityReward(objective)) {
			objectiveExpression = ((ExpressionReward) objective).getExpression();
		} else if (objective instanceof ExpressionProb) {
			objectiveExpression = ((ExpressionProb) objective).getExpression();
		}
		final Expression nonNegatedObjective = removeNegation(objectiveExpression);
		if (nonNegatedObjective instanceof ExpressionTemporal) {
			return ((ExpressionTemporal) removeNegation(objectiveExpression)).getOperand2();
		}
		// no goal expression
		return null;
	}

	protected JDDNode checkExpression(final ProbModel model, final Expression expression) throws PrismException
	{
		final JDDNode statesOfInterest = model.getReach().copy();
		return checkExpression(model, expression, statesOfInterest);
	}

	protected JDDNode checkExpression(final ProbModel model, final Expression expression, final JDDNode statesOfInterest) throws PrismException
	{
		Objects.requireNonNull(statesOfInterest);
		final ModelChecker mc = modelChecker.createModelChecker(model);
		final StateValues stateValues = mc.checkExpression(expression, statesOfInterest);
		return stateValues.convertToStateValuesMTBDD().getJDDNode();
	}

	protected JDDNode computeProbabilities(final ProbModel model, final JDDNode goal, final boolean negated) throws PrismException
	{
		ProbModelChecker mc = (ProbModelChecker) modelChecker.createModelChecker(model);
		StateValues probabilities = mc.computeNextProbs(model.getTrans(), goal);
		if (negated) {
			probabilities.subtractFromOne();
		}
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}



	public static class CTMC extends MCNextTransformer<StochModel, StochModelChecker> implements NewConditionalTransformer.CTMC
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}
	}



	public static class DTMC extends MCNextTransformer<ProbModel, ProbModelChecker> implements NewConditionalTransformer.DTMC
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}
	}
}