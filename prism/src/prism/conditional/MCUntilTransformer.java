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
import prism.ModelChecker;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.conditional.transform.MCDeadlockTransformation;
import prism.conditional.transform.MCPivotTransformation;
import jdd.JDD;
import jdd.JDDNode;

public class MCUntilTransformer extends MCConditionalTransformer
{
	public MCUntilTransformer(ProbModelChecker modelChecker, Prism prism) {
		super(modelChecker, prism);
	}

	@Override
	public boolean canHandleCondition(final ProbModel model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final Expression until = removeNegation(condition);
		return ExpressionInspector.isSimpleUntilFormula(until);
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
		return transformModel(model, condition, statesOfInterest, ! requiresSecondMode(expression));
	}

	protected ModelTransformation<ProbModel, ProbModel> transformModel(final ProbModel model, final Expression condition, final JDDNode statesOfInterest,
			final boolean deadlock) throws PrismException
	{
//>>> Debug: print states of interest
//		prism.getLog().println("States of interest:");
//		JDD.PrintMinterms(prism.getLog(), statesOfInterest.copy());
//		new StateValuesMTBDD(statesOfInterest.copy(), model).print(prism.getLog());

		final Expression until = ExpressionInspector.normalizeExpression(condition);
		final JDDNode remain = getRemainStates(model, until);
		final JDDNode goal = getGoalStates(model, until);
		final boolean negated = until instanceof ExpressionUnaryOp;

		final JDDNode prob0 = computeProb0(model, remain, goal);
		final JDDNode prob1 = computeProb1(model, remain, goal, prob0);
		final JDDNode probs = computeProbabilities(model, remain, goal, prob0, prob1, negated);

		if (JDD.IsContainedIn(statesOfInterest, prob0)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

//>>> Debug: print probabilities
//		prism.getLog().println("Probs:");
//		JDD.PrintMinterms(prism.getLog(), probs.copy());
//		new StateValuesMTBDD(probs.copy(), model).print(prism.getLog());

		ModelTransformation<ProbModel, ProbModel> pivotTransformation;
		JDDNode liftedProbs;
		if (deadlock) {
			// pivots from remain and goal;
			JDDNode pivots = getPivots(model, remain, goal, negated);
			// deadlock in pivots
			pivotTransformation = new MCDeadlockTransformation(model, pivots, statesOfInterest);
			JDD.Deref(pivots);
			// lift probs
			liftedProbs = probs.copy();
		} else {
			// pivots from prob0 or prob1;
			JDDNode pivots = negated ? prob0.copy() : prob1.copy();
			// switch mode in pivots
			pivotTransformation = new MCPivotTransformation(model, pivots, statesOfInterest, true);
			JDD.Deref(pivots);
			// lift probs
			liftedProbs = JDD.Apply(JDD.MAX, probs.copy(), ((MCPivotTransformation) pivotTransformation).getAfter());
		}

//>>> Debug: print pivot states
//		prism.getLog().println("Pivot states:");
//		JDD.PrintMinterms(prism.getLog(), prob1.copy());
//		new StateValuesMTBDD(prob1.copy(), model).print(prism.getLog());

// >>> Debug: print lifted probabilities
//		prism.getLog().println("Lifted probs:");
//		JDD.PrintMinterms(prism.getLog(), liftedProbs.copy());
//		new StateValuesMTBDD(liftedProbs.copy(), pivotModel).print(prism.getLog());

		JDD.Deref(remain, goal, prob0, prob1, probs);

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

	protected JDDNode getRemainStates(final ProbModel model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal until = (ExpressionTemporal) removeNegation(expression);
		return checkExpression(model, until.getOperand1());
	}

	protected JDDNode getGoalStates(final ProbModel model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal until = (ExpressionTemporal) removeNegation(expression);
		return checkExpression(model, until.getOperand2());
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

	protected boolean requiresSecondMode(final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.trimUnaryOperations(expression.getCondition());
		if (!ExpressionInspector.isSimpleUntilFormula(condition)) {
			// can optimize unbounded simple until conditions only
			return true;
		}
		final ExpressionTemporal conditionPath = (ExpressionTemporal) condition;

		final Expression objective = expression.getObjective();
		final Expression objectiveSubExpr;
		if (ExpressionInspector.isReachablilityReward(objective)) {
			objectiveSubExpr = ((ExpressionReward) objective).getExpression();
		} else if (objective instanceof ExpressionProb) {
			objectiveSubExpr = ((ExpressionProb) objective).getExpression();
			if (! ExpressionInspector.isSimpleUntilFormula(objectiveSubExpr)) {
				return true;
			}
		} else {
			return true;
		}
		final ExpressionTemporal objectivePath = (ExpressionTemporal) objectiveSubExpr;

		Expression conditionGoal = ExpressionInspector.trimUnaryOperations(conditionPath.getOperand2());
		Expression objectiveGoal = ExpressionInspector.trimUnaryOperations(objectivePath.getOperand2());
		if (conditionGoal != null && objectiveGoal != null) {
			try {
				return !objectiveGoal.syntacticallyEquals(conditionGoal);
			} catch (PrismLangException e) {}
		}
		return true;
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

	private JDDNode computeProbabilities(final ProbModel model, final JDDNode remain, final JDDNode goal, final JDDNode prob0, final JDDNode prob1, final boolean negated) throws PrismException
	{
		ProbModelChecker mc = (ProbModelChecker) modelChecker.createModelChecker(model);
		StateValues probabilities = mc.checkProbUntil(remain, goal, false);
		if (negated) {
			probabilities.subtractFromOne();
		}
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}

//	private JDDNode computeProb0(final ProbModel model, JDDNode remain, JDDNode goal)
//	{
//		JDDNode trans01 = model.getTrans01();
//		JDDNode reach   = model.getReach();
//		JDDVars rowVars = model.getAllDDRowVars();
//		JDDVars colVars = model.getAllDDColVars();
//		return PrismMTBDD.Prob0(trans01, reach, rowVars, colVars, remain, goal);
//	}
//
//	private JDDNode computeProb1(final ProbModel model, JDDNode remain, JDDNode goal, JDDNode prob0)
//	{
//		JDDNode trans01 = model.getTrans01();
//		JDDNode reach   = model.getReach();
//		JDDVars rowVars = model.getAllDDRowVars();
//		JDDVars colVars = model.getAllDDColVars();
//		return PrismMTBDD.Prob1(trans01, reach, rowVars, colVars, remain, goal, prob0);
//	}
}