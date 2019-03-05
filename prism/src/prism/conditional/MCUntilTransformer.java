package prism.conditional;

import java.util.Objects;

import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
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
import prism.conditional.SimplePathProperty.Reach;
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.BasicModelExpressionTransformation;
import prism.conditional.transform.MCDeadlockTransformation;
import prism.conditional.transform.MCPivotTransformation;
import jdd.JDD;
import jdd.JDDNode;

public abstract class MCUntilTransformer<M extends ProbModel, C extends ProbModelChecker> extends ConditionalTransformer.MC<M, C>
{
	public MCUntilTransformer(Prism prism, C modelChecker)
	{
		super(prism, modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final Expression until     = removeNegation(condition);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// Can handle all ExpressionQuant: P, R, S and L
		return true;
	}

	@Override
	public ModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		Expression condition    = expression.getCondition();
		Reach<M> conditionReach = (Reach<M>) computeSimplePathProperty(model, condition);
		Until<M> conditionPath  = conditionReach.asUntil();
		conditionReach.clear();
		ModelTransformation<M, ? extends M> transformation = transformModel(model, conditionPath, statesOfInterest, ! requiresSecondMode(expression));
		return new BasicModelExpressionTransformation<>(transformation, expression, expression.getObjective());
	}

	protected ModelTransformation<M, ? extends M> transformModel(final M model, final Until<M> conditionPath, final JDDNode statesOfInterest,
			final boolean deadlock) throws PrismException
	{
//>>> Debug: print states of interest
//		prism.getLog().println("States of interest:");
//		JDD.PrintMinterms(prism.getLog(), statesOfInterest.copy());
//		new StateValuesMTBDD(statesOfInterest.copy(), model).print(prism.getLog());

		// FIXME ALG: reuse prob0, prob1
		final JDDNode prob0 = computeProb0(conditionPath);
		final JDDNode prob1 = computeProb1(conditionPath);
		final JDDNode probs = computeProbs(conditionPath);

		if (JDD.IsContainedIn(statesOfInterest, prob0)) {
			// FIXME ALG: Deref JDDNodes!
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

//>>> Debug: print probabilities
//		prism.getLog().println("Probs:");
//		JDD.PrintMinterms(prism.getLog(), probs.copy());
//		new StateValuesMTBDD(probs.copy(), model).print(prism.getLog());

		ModelTransformation<M, M> pivotTransformation;
		JDDNode liftedProbs;
		if (deadlock) {
			// FIXME ALG: If we introduce a new label for state formulas of the objective's and condition's path formulas
			//            we can even handle path formulas containing non-propositional state formulas.
			// pivots from remain and goal
			JDDNode pivots = getPivots(model, conditionPath);
			// deadlock in pivots
			pivotTransformation = new MCDeadlockTransformation<>(model, pivots, statesOfInterest);
			JDD.Deref(pivots);
			// lift probs
			liftedProbs = probs.copy();
		} else {
			// pivots from prob1
			JDDNode pivots = prob1;
			// switch mode in pivots
			pivotTransformation = new MCPivotTransformation<>(model, pivots, statesOfInterest, true);
			// lift probs
			liftedProbs = JDD.Apply(JDD.MAX, probs.copy(), ((MCPivotTransformation<M>) pivotTransformation).getAfter());
		}

//>>> Debug: print pivot states
//		prism.getLog().println("Pivot states:");
//		JDD.PrintMinterms(prism.getLog(), prob1.copy());
//		new StateValuesMTBDD(prob1.copy(), model).print(prism.getLog());

// >>> Debug: print lifted probabilities
//		prism.getLog().println("Lifted probs:");
//		JDD.PrintMinterms(prism.getLog(), liftedProbs.copy());
//		new StateValuesMTBDD(liftedProbs.copy(), pivotModel).print(prism.getLog());

		conditionPath.clear();
		JDD.Deref(prob0, prob1, probs);

		M pivotModel                  = pivotTransformation.getTransformedModel();
		JDDNode pivotStatesOfInterest = pivotTransformation.getTransformedStatesOfInterest();
		liftedProbs                   = JDD.Times(liftedProbs, pivotModel.getReach().copy());

		MCScaledTransformation<M> scaledTransformation = new MCScaledTransformation<>(prism, pivotModel, liftedProbs.copy(), pivotStatesOfInterest.copy());

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

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	protected JDDNode getPivots(final M model, final Until<M> until)
	{
		if (! until.isNegated()) {
			// pivots = goal
			return until.getGoal().copy();
		}
		// pivots = ! (remain | goal)
		return JDD.Not(JDD.Or(until.getRemain().copy(), until.getGoal().copy()));
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
		final Expression objective = expression.getObjective().getExpression();
		if (!ExpressionInspector.isUnboundedSimpleUntilFormula(objective)) {
			// can optimize non-negated unbounded simple until objectives only
			return true;
		}
		final ExpressionTemporal objectivePath = (ExpressionTemporal) objective;

		final Expression condition = ExpressionInspector.trimUnaryOperations(expression.getCondition());
		if (!ExpressionInspector.isUnboundedSimpleUntilFormula(condition)) {
			// can optimize non-negated unbounded simple until conditions only
			return true;
		}
		final ExpressionTemporal conditionPath = (ExpressionTemporal) condition;

		Expression objectiveGoal = ExpressionInspector.trimUnaryOperations(objectivePath.getOperand2());
		Objects.requireNonNull(objectiveGoal);
		Expression conditionGoal = ExpressionInspector.trimUnaryOperations(conditionPath.getOperand2());
		Objects.requireNonNull(conditionGoal);

		// FIXME ALG: If we use state sets instead of formulas, we can determine whether
		//            objectiveGoal is a subset of conditionGoal
		try {
			if (!objectiveGoal.syntacticallyEquals(conditionGoal)) {
				// objective and condition goals must be the same
				return true;
			}
		} catch (PrismLangException e) {
			return true;
		}

		if (objectiveGoal.isProposition() && conditionGoal.isProposition()) {
			Expression objectiveRemain = objectivePath.getOperand1();
			if (objectiveRemain == null || objectiveRemain.isProposition()) {
				Expression conditionRemain = conditionPath.getOperand1();
				return conditionRemain == null || conditionRemain.isProposition();
			}
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



	public static class CTMC extends MCUntilTransformer<StochModel, StochModelChecker> implements ConditionalTransformer.CTMC
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}
	}



	public static class DTMC extends MCUntilTransformer<ProbModel, ProbModelChecker> implements ConditionalTransformer.DTMC
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}
	}
}