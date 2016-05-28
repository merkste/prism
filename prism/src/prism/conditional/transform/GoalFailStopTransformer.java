package prism.conditional.transform;

import java.util.function.Function;

import common.StopWatch;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.ModelExpressionTransformation;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.NondetModelTransformation;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.ProbModelTransformation;
import prism.StateModelChecker;
import prism.StateValues;
import prism.conditional.NewConditionalTransformer;
import prism.conditional.SimplePathProperty.Finally;
import prism.conditional.SimplePathProperty.Until;



public interface GoalFailStopTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	public static final boolean ROW    = true;
	public static final boolean COLUMN = false;



	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		// can handle probabilities only
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return false;
		}
		// can handle simple finally formulae only
		Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	default GoalFailStopTransformation<M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) Objective: extract objective
		Expression objectiveExpr = ((ExpressionProb) expression.getObjective()).getExpression();
		Finally objectivePath    = new Finally(objectiveExpr, getModelChecker(), true);

		// 2) Condition: compute "condition remain and goal states"
		Until conditionPath      = new Until(expression.getCondition(), getModelChecker(), true);

		// check satisfiability
		// FIXME ALG: consider whether this is actually an error here
		// FIXME ALG: release resources
		JDDNode conditionUnsatisfied      = checkSatisfiability(model, conditionPath, statesOfInterest);
		JDD.Deref(statesOfInterest);
		JDDNode conditionMaybeUnsatisfied = computeMaybeUnsatified(model, conditionPath);

		// compute normal-form states and probabilities for objective
		// FIXME ALG: reuse precomputation?
		JDDNode objectiveNormalStates = computeNormalFormStates(model, objectivePath);
		JDDNode objectiveNormalProbs  = computeNormalFormProbs(model, objectivePath);
		objectivePath.clear();

		// compute normal-form states and probabilities for objective
		// FIXME ALG: reuse precomputation?
		JDDNode conditionNormalStates = computeNormalFormStates(model, conditionPath);
////>>> Debug: print conditionNormalStates
//getLog().println("conditionNormalStates:");
//JDD.PrintMinterms(getLog(), conditionNormalStates.copy());
//new StateValuesMTBDD(conditionNormalStates.copy(), model).print(getLog());
		conditionNormalStates         = JDD.And(conditionNormalStates, JDD.Not(objectiveNormalStates.copy()));
		JDDNode conditionProbs        = computeNormalFormProbs(model, conditionPath);
		conditionPath.clear();

////>>> Debug: print conditionUnsatisfied
//getLog().println("conditionUnsatisfied:");
//JDD.PrintMinterms(getLog(), conditionUnsatisfied.copy());
//new StateValuesMTBDD(conditionUnsatisfied.copy(), model).print(getLog());
////>>> Debug: print objectiveNormalStates
//getLog().println("objectiveNormalStates:");
//JDD.PrintMinterms(getLog(), objectiveNormalStates.copy());
//new StateValuesMTBDD(objectiveNormalStates.copy(), model).print(getLog());

		// transform model and expression
		GoalFailStopOperator<M> operator = configureOperator(model, conditionUnsatisfied, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs);
		return new GoalFailStopTransformation<>(model, expression, operator, conditionMaybeUnsatisfied);
	}

	default JDDNode checkSatisfiability(M model, Until condition, JDDNode statesOfInterest)
			throws UndefinedTransformationException
	{
		JDDNode conditionUnsatisfied  = computeUnsatified(model, condition);
		if (JDD.IsContainedIn(statesOfInterest, conditionUnsatisfied)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		return conditionUnsatisfied;
	}

	JDDNode computeUnsatified(M model, Until until);

	JDDNode computeMaybeUnsatified(M model, Until until);

	JDDNode computeNormalFormStates(M model, Until until);

	JDDNode computeNormalFormProbs(M model, Until until) throws PrismException;

	GoalFailStopOperator<M> configureOperator(M model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
			JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException;



	public class DTMC extends NewConditionalTransformer.DTMC implements GoalFailStopTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeUnsatified(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0(model, until.getRemain(), until.getGoal());
			}
		}

		@Override
		public JDDNode computeMaybeUnsatified(ProbModel model, Until until)
		{
			return computeUnsatified(model, until);
		}

		@Override
		public JDDNode computeNormalFormStates(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1(model, until.getRemain(), until.getGoal());
			}
// FIXME ALG: fishy: should be all states with Pmin=1 (Condition)
//			JDDNode conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
//			JDDNode conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
//			return computeProb1(model, conditionWeakRemain, conditionWeakGoal);
		}

		@Override
		public JDDNode computeNormalFormProbs(ProbModel model, Until until) throws PrismException
		{
			return computeUntilProbs(model, until);
		}

		@Override
		public GoalFailStopOperator<ProbModel> configureOperator(ProbModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
				JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new MCGoalFailStopOperator(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied, getLog());
		}
	}



	public class MDP extends NewConditionalTransformer.MDP implements GoalFailStopTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeUnsatified(NondetModel model, Until until)
		{
			return until.isNegated() ? computeProb1A(model, until.getRemain(), until.getGoal()) : computeProb0A(model, until.getRemain(), until.getGoal());
		}

		@Override
		public JDDNode computeMaybeUnsatified(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1E(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0E(model, until.getRemain(), until.getGoal());
			}
		}

		@Override
		public JDDNode computeNormalFormStates(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			}
// FIXME ALG: fishy: should be all states with Pmin=1 (Condition)
//			JDDNode conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
//			JDDNode conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
//			return computeProb1A(model, conditionWeakRemain, conditionWeakGoal);
		}

		@Override
		public JDDNode computeNormalFormProbs(NondetModel model, Until until) throws PrismException
		{
			return computeUntilMaxProbs(model, until);
		}

		public GoalFailStopOperator<NondetModel> configureOperator(NondetModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
				JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new MDPGoalFailStopOperator(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied, getLog());
		}
	}



	public class GoalFailStopTransformation<M extends ProbModel> implements ModelExpressionTransformation<M, M>
	{
		protected GoalFailStopOperator<M> operator;

		protected M originalModel;
		protected M transformedModel;
		protected String goalLabel;
		protected String failLabel;
		protected String stopLabel;
		protected JDDNode conditionMaybeUnsatisfied;
		protected ExpressionConditional originalExpression;
		protected ExpressionConditional transformedExpression;



		/**
		 * [ REFS: <i>...</i>, DEREFS: <i>...</i> ]
		 */
		public GoalFailStopTransformation(M model, ExpressionConditional expression, GoalFailStopOperator<M> operator, JDDNode conditionMaybeUnsatisfied)
				throws PrismException
		{
			this.originalModel      = model;
			this.originalExpression = expression;
			this.operator           = operator;
			this.transformedModel   = operator.apply(originalModel);
			this.conditionMaybeUnsatisfied = conditionMaybeUnsatisfied;

			// store trap states under a unique label
			goalLabel = transformedModel.addUniqueLabelDD("goal", operator.goal(ROW));
			failLabel = transformedModel.addUniqueLabelDD("fail", operator.fail(ROW));
			stopLabel = transformedModel.addUniqueLabelDD("stop", operator.stop(ROW));

			Expression conditionExpr       = ExpressionInspector.normalizeExpression(expression.getCondition());
 			boolean conditionNegated       = conditionExpr instanceof ExpressionUnaryOp;

			// transform expression
			ExpressionProb objective            = (ExpressionProb) expression.getObjective();
			ExpressionProb transformedObjective = new ExpressionProb(Expression.Finally(new ExpressionLabel(goalLabel)), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
			Expression transformedCondition     = Expression.Finally(Expression.Parenth(Expression.Or(new ExpressionLabel(goalLabel), new ExpressionLabel(stopLabel))));
			if (conditionNegated) {
				// FIXME ALG: add (! F b) to transformed condition iff condition == !(a U b)
				// ¬(a U b) == (¬b U ¬(a|b)) | G(¬b)
				ExpressionTemporal untilExpr = (ExpressionTemporal) ExpressionInspector.removeNegation(conditionExpr);
				Expression conditionGoalExpr = untilExpr.getOperand2();
				transformedCondition         = Expression.Or(transformedCondition, Expression.Globally(Expression.Parenth(ExpressionInspector.trimUnaryOperations(Expression.Not(conditionGoalExpr)))));
			}
			transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);
		}

		@Override
		public M getOriginalModel()
		{
			return originalModel;
		}

		@Override
		public M getTransformedModel()
		{
			return transformedModel;
		}

		@Override
		public ExpressionConditional getOriginalExpression()
		{
			return originalExpression;
		}

		@Override
		public ExpressionConditional getTransformedExpression()
		{
			return transformedExpression;
		}

		@Override
		public void clear()
		{
			operator.clear();
			transformedModel.clear();
		}

		@Override
		public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
		{
			JDDNode transformedStatesOfInterest = getTransformedStatesOfInterest();
			svTransformedModel.filter(transformedStatesOfInterest);
			JDD.Deref(transformedStatesOfInterest);

			StateValues svOriginalModel = svTransformedModel.sumOverDDVars(operator.getExtraRowVars(), originalModel);
			svTransformedModel.clear();

			return svOriginalModel;
		}

		@Override
		public JDDNode getTransformedStatesOfInterest()
		{
			return transformedModel.getStart().copy();
		}

		public JDDNode getConditonUnsatisfiedStates()
		{
			return operator.getConditionUnsatisfied();
		}

		public JDDNode getConditonMaybeUnsatisfiedStates()
		{
			return JDD.And(getNormalStates(), conditionMaybeUnsatisfied.copy());
		}

		public String getGoalLabel()
		{
			return goalLabel;
		}

		public String getFailLabel()
		{
			return failLabel;
		}

		public String getStopLabel()
		{
			return stopLabel;
		}

		public JDDNode getNormalStates()
		{
			return operator.normal(ROW);
		}

		public JDDNode getTrapStates()
		{
			return operator.trap(ROW);
		}
	}



	public interface GoalFailStopOperator<M extends ProbModel>
	{
		M apply(M model) throws PrismException;

		void clear();

		PrismLog getLog();

		M getOriginalModel();

		JDDNode getObjectiveNormalStates();

		JDDNode getObjectiveNormalProbs();

		JDDNode getConditionNormalStates();

		JDDNode getConditionNormalProbs();

		JDDNode getConditionUnsatisfied();

		JDDVars getExtraRowVars();

		JDDVars getExtraColVars();

		JDDNode tau();

		JDDNode notTau();

		default int getExtraStateVariableCount()
		{
			// we need 2 extra state variables:
			// 00 = normal
			// 01 = goal
			// 10 = fail
			// 11 = stop
			return 2;
		}

		default JDDNode normal(boolean row)
		{
			JDDVars extraRowVars = getExtraRowVars();
			JDDVars extraColVars = getExtraColVars();
			// !extra(0) & !extra(1)
			return JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
			               JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));
		}

		default JDDNode trap(boolean row)
		{
			// !normal & !originalVar(0) & !originalVar(1) & ....
			JDDNode result =  JDD.Not(normal(row));

			M originalModel = getOriginalModel();
			JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
			for (int i = 0; i < vars.getNumVars(); i++) {
				result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
			}
			return result;
		}

		default JDDNode goal(boolean row)
		{
			JDDVars extraRowVars = getExtraRowVars();
			JDDVars extraColVars = getExtraColVars();
			// extra(0) & !extra(1) & !originalVar(0) & !originalVar(1) & ....
			JDDNode result = JDD.And(        (row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
			                         JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));

			return JDD.And(result, trap(row));
		}

		default JDDNode fail(boolean row)
		{
			JDDVars extraRowVars = getExtraRowVars();
			JDDVars extraColVars = getExtraColVars();
			// !extra(0) & extra(1) & !originalVar(0) & !originalVar(1) & ....
			JDDNode result = JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
			                                 (row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy());

			return JDD.And(result, trap(row));
		}

		default JDDNode stop(boolean row)
		{
			JDDVars extraRowVars = getExtraRowVars();
			JDDVars extraColVars = getExtraColVars();
			// extra(0) & extra(1) & !originalVar(0) & !originalVar(1) & ....
			JDDNode result = JDD.And((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
			                         (row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy());

			return JDD.And(result, trap(row));
		}

		default JDDNode getTransformedTrans() throws PrismException
		{
			PrismLog log = getLog();
			StopWatch watch = new StopWatch(log);
			Function<JDDNode, String> printNumNodes = (node) -> "MTBDD nodes = " + JDD.GetNumNodes(node);

			log.println("Goal/fail/stop/reset transformation:");

//			if (debug)
//				originalModel.printTransInfo(log, true);

			JDDNode normal_to_normal = watch.run(this::transformNormalToNormal);
			log.println(" normal_to_normal: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_normal));
//			if (debug) {
//				JDD.PrintMinterms(log, originalModel.getTrans().copy(), "trans");
//				JDD.PrintMinterms(log, normal_to_normal.copy(), "normal_to_normal");
//			}

			JDDNode objective_to_goal = watch.run(this::transformObjectiveToGoal);
			log.println(" objective_to_goal: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(objective_to_goal));
//			if (debug)
//				JDD.PrintMinterms(log, objective_to_goal.copy(), "objective_to_goal");

			JDDNode objective_to_fail = watch.run(this::transformObjectiveToFail);
			log.println(" objective_to_fail: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(objective_to_fail));
//			if (debug)
//				JDD.PrintMinterms(log, objective_to_fail.copy(), "objective_to_fail");

			JDDNode condition_to_goal = watch.run(this::transformConditionToGoal);
			log.println(" condition_to_goal: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(condition_to_goal));
//			if (debug)
//				JDD.PrintMinterms(log, condition_to_goal.copy(), "condition_to_goal");

			JDDNode condition_to_stop = watch.run(this::transformConditionToStop);
			log.println(" condition_to_stop: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(condition_to_stop));
//			if (debug)
//				JDD.PrintMinterms(log, condition_to_stop.copy(), "condition_to_stop");

			JDDNode goal_self_loop = watch.run(this::transformGoalSelfLoop);
			log.println(" goal_self_loop: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(goal_self_loop));
//			if (debug)
//				JDD.PrintMinterms(log, goal_self_loop.copy(), "goal_self_loop");

			JDDNode fail_self_loop = watch.run(this::transformFailSelfLoop);
			log.println(" fail_self_loop: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(fail_self_loop));
//			if (debug)
//				JDD.PrintMinterms(log, fail_self_loop.copy(), "fail_self_loop");

			JDDNode stop_self_loop = watch.run(this::transformStopSelfLoop);
			log.println(" stop_self_loop: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(stop_self_loop));
//			if (debug)
//				JDD.PrintMinterms(log, stop_self_loop, "stop_self_loop");

			JDDNode unsatisfied_self_loops = watch.run(this::transformUnsatisfiedSelfLoops);
			log.println(" unsatisfied_self_loops: "+ watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(unsatisfied_self_loops));
//			if (debug)
//				JDD.PrintMinterms(log, unsatisfied_self_loop, "unsatisfied_self_loop");

			// plug new transitions together...
			JDDNode newTrans;

			log.println();

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, goal_self_loop, fail_self_loop);
			watch.stop();
			log.println(" goal_self_loop\n  |= fail_self_loop" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, stop_self_loop);
			watch.stop();
			log.println("  |= stop_self_loop" +  watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, unsatisfied_self_loops);
			watch.stop();
			log.println("  |= unsatisfied_self_loops" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_goal);
			watch.stop();
			log.println("  |= objective_to_goal" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_fail);
			watch.stop();
			log.println("  |= objective_to_fail" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_goal);
			watch.stop();
			log.println("  |= condition_to_goal" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_stop);
			watch.stop();
			log.println("  |= condition_to_stop" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_normal);
			watch.stop();
			log.println("  |= normal_to_normal" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

//			if (debug)
//			JDD.PrintMinterms(log, newTrans.copy(), "newTrans");

			return newTrans;
		}

		default JDDNode transformNormalToNormal()
		{
			return JDD.Times(normal(ROW),
			                 JDD.Not(getObjectiveNormalStates()),
			                 JDD.Not(getConditionNormalStates()),
			                 JDD.Not(getConditionUnsatisfied()),
			                 notTau(),
			                 normal(COLUMN),
			                 getOriginalModel().getTrans().copy());
		}

		default JDDNode transformObjectiveToGoal()
		{
			return JDD.Times(normal(ROW),
			                 getObjectiveNormalStates(),
			                 tau(),
			                 goal(COLUMN),
			                 getConditionNormalProbs());
		}

		default JDDNode transformObjectiveToFail()
		{
			JDDNode oneMinusConditionNormalProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getConditionNormalProbs());
			return JDD.Times(normal(ROW),
			                 getObjectiveNormalStates(),
			                 tau(),
			                 fail(COLUMN),
			                 oneMinusConditionNormalProbs);
		}

		default JDDNode transformConditionToGoal()
		{
			return JDD.Times(normal(ROW),
			                 getConditionNormalStates(),
			                 tau(),
			                 goal(COLUMN),
			                 getObjectiveNormalProbs());
		}

		default JDDNode transformConditionToStop()
		{
			JDDNode oneMinusObjectiveNormalProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getObjectiveNormalProbs());
			return JDD.Times(normal(ROW),
			                 getConditionNormalStates(),
			                 tau(),
			                 stop(COLUMN),
			                 oneMinusObjectiveNormalProbs);
		}

		default JDDNode transformGoalSelfLoop()
		{
			return JDD.Times(goal(ROW),
			                 tau(),
			                 goal(COLUMN));
		}

		default JDDNode transformFailSelfLoop()
		{
			return JDD.Times(fail(ROW),
			                 tau(),
			                 fail(COLUMN));
		}

		default JDDNode transformStopSelfLoop()
		{
			return JDD.Times(stop(ROW),
			                 tau(),
			                 stop(COLUMN));
		}

		default JDDNode transformUnsatisfiedSelfLoops()
		{
			return JDD.Times(getConditionUnsatisfied(),
			                 JDD.Not(getObjectiveNormalStates()), // do not deadlock normal-form states
			                 tau(),
			                 normal(COLUMN),
			                 JDD.Identity(getOriginalModel().getAllDDRowVars(), getOriginalModel().getAllDDColVars()));
		}

		default JDDNode getTransformedStart() throws PrismException
		{
			// FIXME ALG: use states of interest as start function
			JDDNode start = JDD.And(normal(ROW), getOriginalModel().getStart().copy());
//			if (debug)
//				JDD.PrintMinterms(log, start.copy(), "start");
			return start;
//			return getOriginalModel().getReach().copy();
		}
	}



	public static class MDPGoalFailStopOperator extends NondetModelTransformation implements GoalFailStopOperator<NondetModel>
	{
		protected PrismLog log;

		protected JDDNode objectiveNormalStates;
		protected JDDNode objectiveNormalProbs;
		protected JDDNode conditionNormalStates;
		protected JDDNode conditionNormalProbs;
		protected JDDNode conditionUnsatisfied;



		/**
		 * [ REFS: <i>none</i>, DEREFS: (on clear) <i>objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionNormalProbs, and conditionUnsatisfied</i> ]
		 */
		public MDPGoalFailStopOperator(NondetModel model,
		                               JDDNode objectiveNormalStates,
		                               JDDNode objectiveNormalProbs,
		                               JDDNode conditionNormalStates,
		                               JDDNode conditionNormalProbs,
		                               JDDNode conditionUnsatisfied,
		                               PrismLog log
		                              ) throws PrismException
		{
			super(model);
			this.log = log;

			assert (! JDD.AreIntersecting(objectiveNormalStates, conditionNormalStates));

			this.objectiveNormalStates = objectiveNormalStates;
			this.objectiveNormalProbs  = objectiveNormalProbs;
			this.conditionNormalStates = conditionNormalStates;
			this.conditionNormalProbs  = conditionNormalProbs;
			this.conditionUnsatisfied  = conditionUnsatisfied;
		}

		@Override
		public NondetModel apply(NondetModel model) throws PrismException
		{
			return model.getTransformed(this);
		}

		@Override
		public PrismLog getLog()
		{
			return log;
		}

		@Override
		public NondetModel getOriginalModel()
		{
			return originalModel;
		}

		@Override
		public JDDNode getObjectiveNormalStates()
		{
			return objectiveNormalStates.copy();
		}

		@Override
		public JDDNode getObjectiveNormalProbs()
		{
			return objectiveNormalProbs.copy();
		}

		@Override
		public JDDNode getConditionNormalStates()
		{
			return conditionNormalStates.copy();
		}

		@Override
		public JDDNode getConditionNormalProbs()
		{
			return conditionNormalProbs.copy();
		}

		@Override
		public JDDNode getConditionUnsatisfied()
		{
			return JDD.And(normal(ROW), conditionUnsatisfied.copy());
		}

		@Override
		public JDDVars getExtraRowVars()
		{
			return extraRowVars;
		}

		@Override
		public JDDVars getExtraColVars()
		{
			return extraColVars;
		}

		@Override
		public void clear()
		{
			super.clear();
			// FIXME ALG: check deref!!!
			JDD.Deref(objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionNormalProbs, conditionUnsatisfied);
		}

		@Override
		public int getExtraStateVariableCount()
		{
			return GoalFailStopOperator.super.getExtraStateVariableCount();
		}

		@Override
		public JDDNode getTransformedTrans() throws PrismException
		{
			return GoalFailStopOperator.super.getTransformedTrans();
		}

		@Override
		public JDDNode getTransformedStart() throws PrismException
		{
			return GoalFailStopOperator.super.getTransformedStart();
		}

		@Override
		public int getExtraActionVariableCount()
		{
			return 1;
		}

		@Override
		public JDDNode tau()
		{
			JDDNode result = extraActionVars.getVar(0).copy();
			for (int i = 0; i < originalModel.getAllDDNondetVars().getNumVars(); i++) {
				result = JDD.And(result, JDD.Not(originalModel.getAllDDNondetVars().getVar(i).copy()));
			}
			return result;
		}

		@Override
		public JDDNode notTau()
		{
			return JDD.Not(extraActionVars.getVar(0).copy());
		}
	}



	public static class MCGoalFailStopOperator extends ProbModelTransformation implements GoalFailStopOperator<ProbModel>
	{
		protected PrismLog log;

		protected JDDNode objectiveNormalStates;
		protected JDDNode objectiveNormalProbs;
		protected JDDNode conditionNormalStates;
		protected JDDNode conditionNormalProbs;
		protected JDDNode conditionUnsatisfied;



		/**
		 * [ REFS: <i>none</i>, DEREFS: (on clear) <i>objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, and conditionNormalProbs</i> ]
		 */
		public MCGoalFailStopOperator(ProbModel model,
		                               JDDNode objectiveNormalStates,
		                               JDDNode objectiveNormalProbs,
		                               JDDNode conditionNormalStates,
		                               JDDNode conditionNormalProbs,
		                               JDDNode conditionUnsatisfied,
		                               PrismLog log
		                              ) throws PrismException
		{
			super(model);
			this.log = log;

			assert (! JDD.AreIntersecting(objectiveNormalStates, conditionNormalStates));

			this.objectiveNormalStates = objectiveNormalStates;
			this.objectiveNormalProbs  = objectiveNormalProbs;
			this.conditionNormalStates = conditionNormalStates;
			this.conditionNormalProbs  = conditionNormalProbs;
			this.conditionUnsatisfied  = conditionUnsatisfied;
		}

		@Override
		public ProbModel apply(ProbModel model) throws PrismException
		{
			return model.getTransformed(this);
		}

		@Override
		public PrismLog getLog()
		{
			return log;
		}

		@Override
		public ProbModel getOriginalModel()
		{
			return originalModel;
		}

		@Override
		public JDDNode getObjectiveNormalStates()
		{
			return objectiveNormalStates.copy();
		}

		@Override
		public JDDNode getObjectiveNormalProbs()
		{
			return objectiveNormalProbs.copy();
		}

		@Override
		public JDDNode getConditionNormalStates()
		{
			return conditionNormalStates.copy();
		}

		@Override
		public JDDNode getConditionNormalProbs()
		{
			return conditionNormalProbs.copy();
		}

		@Override
		public JDDNode getConditionUnsatisfied()
		{
			return JDD.And(normal(ROW), conditionUnsatisfied.copy());
		}

		@Override
		public JDDVars getExtraRowVars()
		{
			return extraRowVars;
		}

		@Override
		public JDDVars getExtraColVars()
		{
			return extraColVars;
		}

		@Override
		public void clear()
		{
			super.clear();
			// FIXME ALG: check deref!!!
			JDD.Deref(objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionNormalProbs, conditionUnsatisfied);
		}

		@Override
		public int getExtraStateVariableCount()
		{
			return GoalFailStopOperator.super.getExtraStateVariableCount();
		}

		@Override
		public JDDNode getTransformedTrans() throws PrismException
		{
			return GoalFailStopOperator.super.getTransformedTrans();
		}

		@Override
		public JDDNode getTransformedStart() throws PrismException
		{
			return GoalFailStopOperator.super.getTransformedStart();
		}

		@Override
		public JDDNode tau()
		{
			return JDD.Constant(1);
		}

		@Override
		public JDDNode notTau()
		{
			return JDD.Constant(1);
		}
	}
}