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
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.conditional.transform.MCPivotTransformation;
import jdd.JDD;
import jdd.JDDNode;

public class MCNextTransformer extends MCConditionalTransformer
{
	public MCNextTransformer(ProbModelChecker modelChecker, Prism prism) {
		super(modelChecker, prism);
	}

	@Override
	public boolean canHandle(Model model, ExpressionConditional expression)
	{
		if (!(model instanceof ProbModel) || (model instanceof NondetModel)) {
			return false;
		}
		final ProbModel mc = (ProbModel) model;
		return canHandleCondition(mc, expression) && canHandleObjective(mc, expression);
	}

	public boolean canHandleCondition(final ProbModel model, final ExpressionConditional expression)
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

	public boolean canHandleObjective(final ProbModel model, final ExpressionConditional expression)
	{
		// FIXME ALG: steady state computation
		return !ExpressionInspector.isSteadyStateReward(expression.getObjective());
	}

	@Override
	public ModelTransformation<ProbModel, ProbModel> transform(ProbModel model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();
		return transformModel(model, condition, statesOfInterest);
	}

	protected ModelTransformation<ProbModel, ProbModel> transformModel(final ProbModel model, final Expression condition, final JDDNode statesOfInterest)
			throws PrismException
	{
//>>> Debug: print states of interest
//		prism.getLog().println("States of interest:");
//		JDD.PrintMinterms(prism.getLog(), statesOfInterest.copy());
//		new StateValuesMTBDD(statesOfInterest.copy(), model).print(prism.getLog());

		final Expression next = ExpressionInspector.normalizeExpression(condition);
		final JDDNode goal = getGoalStates(model, next);
		final boolean negated = next instanceof ExpressionUnaryOp;

		final JDDNode probs = computeProbabilities(model, goal, negated);

		final JDDNode support = JDD.Apply(JDD.GREATERTHAN, probs.copy(), JDD.ZERO.copy());
		final boolean satisfiable = JDD.AreIntersecting(support, statesOfInterest);
		JDD.Deref(support);
		if (! satisfiable) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

//>>> Debug: print probabilities
//		prism.getLog().println("Probs:");
//		JDD.PrintMinterms(prism.getLog(), probs.copy());
//		new StateValuesMTBDD(probs.copy(), model).print(prism.getLog());

		// pivots from prob0 or prob1;
		JDDNode pivots = negated ? JDD.Not(goal) : goal;
		// switch mode in pivots
		ModelTransformation<ProbModel, ProbModel> pivotTransformation = new MCPivotTransformation(model, pivots, statesOfInterest, false);

//>>> Debug: print pivot states
//		prism.getLog().println("Pivot states:");
//		JDD.PrintMinterms(prism.getLog(), pivots.copy());
//		new StateValuesMTBDD(pivots.copy(), model).print(prism.getLog());

		JDD.Deref(pivots);
		// lift probs
		JDDNode liftedProbs = JDD.Apply(JDD.MAX, probs.copy(), ((MCPivotTransformation) pivotTransformation).getAfter());
		JDD.Deref(probs);

//>>> Debug: print lifted probabilities
//		prism.getLog().println("Lifted probs:");
//		JDD.PrintMinterms(prism.getLog(), liftedProbs.copy());
//		new StateValuesMTBDD(liftedProbs.copy(), pivotModel).print(prism.getLog());

		ProbModel pivotModel = pivotTransformation.getTransformedModel();
		JDDNode pivotStatesOfInterest = pivotTransformation.getTransformedStatesOfInterest();
		liftedProbs = JDD.Times(liftedProbs, pivotModel.getReach().copy());

		MCScaledTransformation scaledTransformation = new MCScaledTransformation(prism, pivotModel, liftedProbs.copy(), pivotStatesOfInterest.copy());

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

		JDD.Deref(liftedProbs, statesOfInterest, pivotStatesOfInterest);

		return new ModelTransformationNested<>(pivotTransformation, scaledTransformation);
	}

	public JDDNode getPivots(final ProbModel model, final JDDNode remain, final JDDNode goal, final boolean negated)
	{
		if (!negated) {
			// pivots = goal
			return goal.copy();
		}
		// pivots = ! (remain | goal)
		return JDD.Not(JDD.Or(remain.copy(), goal.copy()));
	}

	protected JDDNode getGoalStates(final ProbModel model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal next = getExpressionTemporal(expression);
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

	private JDDNode computeProbabilities(final ProbModel model, final JDDNode goal, final boolean negated) throws PrismException
	{
		ProbModelChecker mc = (ProbModelChecker) modelChecker.createModelChecker(model);
		StateValues probabilities = mc.computeNextProbs(model.getTrans(), goal);
		if (negated) {
			probabilities.subtractFromOne();
		}
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}
}