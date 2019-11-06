package prism.conditional.scale;

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
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.ConditionalTransformer;
import jdd.JDD;
import jdd.JDDNode;

//FIXME ALG: add comment
public interface MCNextTransformer<M extends ProbModel, C extends ProbModelChecker> extends ScaleTransformer<M, C>
{
	@Override
	default boolean canHandleCondition(final Model model, final ExpressionConditional expression)
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
		return true;
	}

	@Override
	default boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// Can handle all ExpressionQuant: P, R, S and L
		return true;
	}

	default ModelTransformation<M, ? extends M> transformModel(final M model, final ExpressionConditional expression, final JDDNode statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();

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
			// FIXME ALG: Deref JDDNodes!
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

		MCScaledTransformation<M> scaledTransformation = new MCScaledTransformation<>(getModelChecker(), pivotModel, liftedOriginProbs.copy(), liftedTargetProbs.copy(), pivotStatesOfInterest.copy());

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
	default JDDNode getPivotStates(final ProbModel model, final JDDNode goal, final boolean negated)
	{
		if (! negated) {
			return goal.copy();
		}
		return JDD.Not(goal.copy());
	}

	default JDDNode getGoalStates(final M model, final Expression expression) throws PrismException
	{
		ExpressionTemporal next = getExpressionTemporal(expression);
		return checkExpression(model, next.getOperand2());
	}

	default ExpressionTemporal getExpressionTemporal(final Expression expression) throws PrismLangException
	{
		if (Expression.isNot(expression)) {
			return getExpressionTemporal(((ExpressionUnaryOp) expression).getOperand());
		}
		if (expression instanceof ExpressionTemporal) {
			return (ExpressionTemporal) expression;
		}
		throw new PrismLangException("expected (negated) temporal formula but found", expression);
	}

	default Expression removeNegation(final Expression expression)
	{
		if (expression instanceof ExpressionUnaryOp) {
			// assume negated formula
			return removeNegation(((ExpressionUnaryOp) expression).getOperand());
		}
		// assume non-negated formula
		return expression;
	}

	default Expression getConditionGoal(final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ((ExpressionTemporal) removeNegation(condition)).getOperand2();
	}

	default Expression getObjectiveGoal(final ExpressionConditional expression)
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

	default JDDNode checkExpression(final M model, final Expression expression) throws PrismException
	{
		final JDDNode statesOfInterest = model.getReach().copy();
		return checkExpression(model, expression, statesOfInterest);
	}

	default JDDNode checkExpression(final M model, final Expression expression, final JDDNode statesOfInterest) throws PrismException
	{
		Objects.requireNonNull(statesOfInterest);
		C mc = getModelChecker(model);
		StateValues stateValues = mc.checkExpression(expression, statesOfInterest);
		return stateValues.convertToStateValuesMTBDD().getJDDNode();
	}

	default JDDNode computeProbabilities(final M model, final JDDNode goal, final boolean negated) throws PrismException
	{
		C mc = getModelChecker(model);
		StateValues probabilities = mc.computeNextProbs(model.getTrans(), goal);
		if (negated) {
			probabilities.subtractFromOne();
		}
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}



	public static class CTMC extends ConditionalTransformer.Basic<StochModel, StochModelChecker> implements MCNextTransformer<StochModel, StochModelChecker>, ScaleTransformer.CTMC
	{
		public CTMC(StochModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static class DTMC extends ConditionalTransformer.Basic<ProbModel, ProbModelChecker> implements MCNextTransformer<ProbModel, ProbModelChecker>, ScaleTransformer.DTMC
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}
}
