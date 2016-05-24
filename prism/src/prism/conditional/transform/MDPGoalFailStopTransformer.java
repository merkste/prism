package prism.conditional.transform;

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
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.conditional.NewConditionalTransformer;



public class MDPGoalFailStopTransformer extends NewConditionalTransformer.MDP
{
	public static final boolean ROW    = true;
	public static final boolean COLUMN = false;



	public MDPGoalFailStopTransformer(NondetModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleCondition(NondetModel model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	public boolean canHandleObjective(NondetModel model, ExpressionConditional expression)
			throws PrismLangException
	{
		// can handle probabilites only
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
	public MDPGoalFailStopTransformation transform(NondetModel model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) Objective: extract objective
		ExpressionProb objectiveExpr = (ExpressionProb) expression.getObjective();
		Expression objectiveGoalExpr = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objectiveExpr.getExpression())).getOperand2();
		JDDNode objectiveGoal        = computeStates(model, objectiveGoalExpr);

		// 2) Condition: compute "condition remain and goal states"
		Expression conditionExpr       = ExpressionInspector.normalizeExpression(expression.getCondition());
		ExpressionTemporal untilExpr   = (ExpressionTemporal)ExpressionInspector.removeNegation(conditionExpr);
		Expression conditionRemainExpr = untilExpr.getOperand1();
		JDDNode conditionRemain        = computeStates(model, conditionRemainExpr);
		Expression conditionGoalExpr   = untilExpr.getOperand2();
		JDDNode conditionGoal          = computeStates(model, conditionGoalExpr);
		boolean conditionNegated       = conditionExpr instanceof ExpressionUnaryOp;

		// compute objective normal states and enlarge set by prob1a
		JDDNode objectiveNormalStates = computeProb1A(model, null, objectiveGoal);
		// compute Pmax(Objective)
		// FIXME ALG: reuse precomputation?
		JDDNode objectiveProbs        = computeUntilMaxProbs(model, null, objectiveGoal, false);
		JDD.Deref(objectiveGoal);

		//>>> Debug: print probabilities
//		getLog().println("objectiveNormalStates:");
//		JDD.PrintMinterms(getLog(), objectiveNormalStates.copy());
//		new StateValuesMTBDD(objectiveNormalStates.copy(), model).print(getLog());

		
		
		// compute condition normal states and enlarge set by prob1a
		JDDNode conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
		JDDNode conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
		JDDNode conditionNormalStates = computeProb1A(model, conditionWeakRemain, conditionWeakGoal);
		conditionNormalStates         = JDD.And(conditionNormalStates, JDD.Not(objectiveNormalStates.copy()));
		// compute Pmax(Condition)
		JDDNode conditionProbs        = computeUntilMaxProbs(model, conditionRemain, conditionGoal, conditionNegated);

		//>>> Debug: print probabilities
//		getLog().println("conditionNormalStates:");
//		JDD.PrintMinterms(getLog(), conditionNormalStates.copy());
//		new StateValuesMTBDD(conditionNormalStates.copy(), model).print(getLog());

		
		
		
		// check satisfiability
		// FIXME ALG: consider whether this is actually an error here
		JDDNode conditionUnsatisfied  = conditionNegated ? computeProb1A(model, conditionRemain, conditionGoal) : computeProb0A(model, conditionRemain, conditionGoal);
		if (JDD.IsContainedIn(statesOfInterest, conditionUnsatisfied)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
//		JDD.Deref(conditionRemain, conditionGoal, statesOfInterest);
		JDD.Deref(conditionRemain, conditionGoal, conditionUnsatisfied, statesOfInterest);

		// transform model and expression
		MDPGoalFailStopOperator operator = new MDPGoalFailStopOperator(model, objectiveNormalStates, objectiveProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied, getLog());
		return new MDPGoalFailStopTransformation(model, expression, operator);
}

	public JDDNode getWeakGoalStates(NondetModel model, JDDNode remain, JDDNode goal, boolean negated)
	{
		if (negated) {
			// terminal = ! (remain | goal)
			return JDD.Not(JDD.Or(remain.copy(), goal.copy()));
		}
		return goal;
	}

	public JDDNode getWeakRemainStates(NondetModel model, JDDNode remain, JDDNode goal, boolean negated)
	{
		if (negated) {
			// remain = ! goal
			return JDD.Not(goal.copy());
		}
		return remain;
	}




	public class MDPGoalFailStopTransformation implements ModelExpressionTransformation<NondetModel, NondetModel>
	{
		protected MDPGoalFailStopOperator operator;
	
		protected NondetModel originalModel;
		protected NondetModel transformedModel;
		protected String goalLabel;
		protected String failLabel;
		protected String stopLabel;
		protected ExpressionConditional originalExpression;
		protected ExpressionConditional transformedExpression;
	
	
	
		/**
		 * [ REFS: <i>...</i>, DEREFS: <i>...</i> ]
		 */
		public MDPGoalFailStopTransformation(NondetModel model, ExpressionConditional expression, MDPGoalFailStopOperator operator)
				throws PrismException
		{
			this.originalModel = model;
			this.originalExpression    = expression;
			this.operator = operator;
			this.transformedModel = originalModel.getTransformed(operator);

			// store trap states under a unique label
			goalLabel = transformedModel.addUniqueLabelDD("goal", operator.goal(ROW));
			failLabel = transformedModel.addUniqueLabelDD("fail", operator.fail(ROW));
			stopLabel = transformedModel.addUniqueLabelDD("stop", operator.stop(ROW));

			// transform expression
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			ExpressionProb transformedObjective     = new ExpressionProb(Expression.Finally(new ExpressionLabel(goalLabel)), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
			ExpressionTemporal transformedCondition = Expression.Finally(Expression.Or(new ExpressionLabel(goalLabel), new ExpressionLabel(stopLabel)));
			transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);
		}

		@Override
		public NondetModel getOriginalModel()
		{
			return originalModel;
		}

		@Override
		public NondetModel getTransformedModel()
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
			return operator.conditionUnsatisfied(ROW);
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



	public static class MDPGoalFailStopOperator extends NondetModelTransformation
		{
			protected PrismLog log;
	
			protected JDDNode objectiveNormalStates;
			protected JDDNode objectiveProbs;
			protected JDDNode conditionNormalStates;
			protected JDDNode conditionProbs;
			protected JDDNode conditionUnsatisfied;
	
	
	
			/**
			 * [ REFS: <i>none</i>, DEREFS: (on clear) <i>objectiveNormalStates, objectiveProbs, conditionNormalStates, and conditionProbs</i> ]
			 */
			public MDPGoalFailStopOperator(NondetModel model,
			                               JDDNode objectiveNormalStates,
			                               JDDNode objectiveProbs,
			                               JDDNode conditionNormalStates,
			                               JDDNode conditionProbs,
			                               JDDNode conditionUnsatisfied,
			                               PrismLog log
			                              ) throws PrismException
			{
				super(model);
				this.log = log;
	
				assert (! JDD.AreIntersecting(objectiveNormalStates, conditionNormalStates));
	
				this.objectiveNormalStates = objectiveNormalStates;
				this.objectiveProbs        = objectiveProbs;
				this.conditionNormalStates = conditionNormalStates;
				this.conditionProbs        = conditionProbs;
				this.conditionUnsatisfied  = conditionUnsatisfied;
			}
	
			@Override
			public void clear()
			{
				// call underlying clear
				super.clear();
				// clear stored JDDNodes
				// FIXME ALG: check deref!!!
				JDD.Deref(objectiveNormalStates);
				JDD.Deref(objectiveProbs);
				JDD.Deref(conditionNormalStates);
				JDD.Deref(conditionProbs);
//				JDD.Deref(conditionUnsatisfied);
			}
	
			public JDDNode conditionUnsatisfied(boolean row)
			{
				JDDNode unsatisfiedRow = JDD.And(normal(ROW), conditionUnsatisfied.copy());
				if (row) {
					return unsatisfiedRow;
				}
				return JDD.PermuteVariables(unsatisfiedRow, originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());
			}
	
			public JDDNode normal(boolean row)
			{
				// !extra(0) & !extra(1)
				return JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
				               JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));
			}
	
			public JDDNode trap(boolean row)
			{
				// !normal & !originalVar(0) & !originalVar(1) & ....
				JDDNode result =  JDD.Not(normal(row));
	
				JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
				for (int i = 0; i < vars.getNumVars(); i++) {
					result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
				}
				return result;
			}
	
			public JDDNode goal(boolean row) {
				// extra(0) & !extra(1) & !originalVar(0) & !originalVar(1) & ....
				JDDNode result = JDD.And(        (row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
				                         JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));
	
				return JDD.And(result, trap(row));
	//			JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
	//			for (int i = 0; i < vars.getNumVars(); i++) {
	//				result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
	//			}
	//			return result;
			}
	
			public JDDNode fail(boolean row) {
				// !extra(0) & extra(1) & !originalVar(0) & !originalVar(1) & ....
				JDDNode result = JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
				                                 (row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy());
	
				return JDD.And(result, trap(row));
	//			JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
	//			for (int i = 0; i < vars.getNumVars(); i++) {
	//				result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
	//			}
	//			return result;
			}
	
			public JDDNode stop(boolean row) {
				// extra(0) & extra(1) & !originalVar(0) & !originalVar(1) & ....
				JDDNode result = JDD.And((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
				                         (row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy());
	
				return JDD.And(result, trap(row));
	//			JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
	//			for (int i = 0; i < vars.getNumVars(); i++) {
	//				result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
	//			}
	//			return result;
			}
	
			public JDDNode tau() {
				JDDNode result = extraActionVars.getVar(0).copy();
				for (int i = 0; i < originalModel.getAllDDNondetVars().getNumVars(); i++) {
					result = JDD.And(result, JDD.Not(originalModel.getAllDDNondetVars().getVar(i).copy()));
				}
				return result;
			}
	
			public JDDNode notTau() {
				return JDD.Not(extraActionVars.getVar(0).copy());
			}
	
			@Override
			public int getExtraStateVariableCount()
			{
				// we need 2 extra state variables:
				// 00 = normal
				// 01 = goal
				// 10 = fail
				// 11 = stop
				return 2;
			}
	
			@Override
			public int getExtraActionVariableCount()
			{
				return 1;
			}
	
			@Override
			public JDDNode getTransformedTrans() throws PrismException
			{
				JDDNode newTrans;
				long time;
	
				log.println("Goal/fail/stop/reset transformation:");
	
	//			if (debug)
	//				originalModel.printTransInfo(log, true);
	
				time = System.currentTimeMillis();
				JDDNode normal_to_normal =
					JDD.Times(normal(ROW),
					          notTau(),
	//				          JDD.And(JDD.Not(objectiveNormalStates.copy()),
	//				                  JDD.Not(conditionNormalStates.copy()),
	//				                  JDD.Not(unsatisfied(ROW))),
					          JDD.Not(objectiveNormalStates.copy()),
					          JDD.Not(conditionNormalStates.copy()),
					          JDD.Not(conditionUnsatisfied(ROW)),
					          normal(COLUMN),
					          originalModel.getTrans().copy());
				time = System.currentTimeMillis() - time;
				log.println(" normal_to_normal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(normal_to_normal));
	//			if (debug) {
	//				JDD.PrintMinterms(log, originalModel.getTrans().copy(), "trans");
	//				JDD.PrintMinterms(log, normal_to_normal.copy(), "normal_to_normal");
	//			}
	
				time = System.currentTimeMillis();
				JDDNode objective_to_goal =
					JDD.Times(normal(ROW),
					          tau(),
					          objectiveNormalStates.copy(),
					          goal(COLUMN),
					          conditionProbs.copy());
				time = System.currentTimeMillis() - time;
				log.println(" objective_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(objective_to_goal));
	//			if (debug)
	//				JDD.PrintMinterms(log, objective_to_goal.copy(), "objective_to_goal");
	
				time = System.currentTimeMillis();
				JDDNode oneMinusConditionProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), conditionProbs.copy());
				JDDNode objective_to_fail =
					JDD.Times(normal(ROW),
					          tau(),
					          objectiveNormalStates.copy(),
					          fail(COLUMN),
					          oneMinusConditionProbs);
				time = System.currentTimeMillis() - time;
				log.println(" objective_to_fail: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(objective_to_fail));
	//			if (debug)
	//				JDD.PrintMinterms(log, objective_to_fail.copy(), "objective_to_fail");
	
				time = System.currentTimeMillis();
				JDDNode condition_to_goal =
					JDD.Times(normal(ROW),
					          tau(),
					          conditionNormalStates.copy(),
					          goal(COLUMN),
					          objectiveProbs.copy());
				time = System.currentTimeMillis() - time;
				log.println(" condition_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(condition_to_goal));
	//			if (debug)
	//				JDD.PrintMinterms(log, condition_to_goal.copy(), "condition_to_goal");
	
				time = System.currentTimeMillis();
				JDDNode oneMinusObjectiveProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), objectiveProbs.copy());
				JDDNode condition_to_stop =
					JDD.Times(normal(ROW),
					          tau(),
					          conditionNormalStates.copy(),
					          stop(COLUMN),
					          oneMinusObjectiveProbs);
				time = System.currentTimeMillis() - time;
				log.println(" condition_to_stop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(condition_to_stop));
	//			if (debug)
	//				JDD.PrintMinterms(log, condition_to_stop.copy(), "condition_to_stop");
	
				time = System.currentTimeMillis();
				JDDNode goal_self_loop =
					JDD.Times(goal(ROW),
					          tau(),
					          goal(COLUMN));
				time = System.currentTimeMillis() - time;
				log.println(" goal_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(goal_self_loop));
	//			if (debug)
	//				JDD.PrintMinterms(log, goal_self_loop.copy(), "goal_self_loop");
	
				time = System.currentTimeMillis();
				JDDNode fail_self_loop =
					JDD.Times(fail(ROW),
					          tau(),
					          fail(COLUMN));
				time = System.currentTimeMillis() - time;
				log.println(" fail_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(fail_self_loop));
	//			if (debug)
	//				JDD.PrintMinterms(log, fail_self_loop.copy(), "fail_self_loop");
	
				time = System.currentTimeMillis();
				JDDNode stop_self_loop =
					JDD.Times(stop(ROW),
					          tau(),
					          stop(COLUMN));
				time = System.currentTimeMillis() - time;
				log.println(" stop_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(stop_self_loop));
	//			if (debug)
	//				JDD.PrintMinterms(log, stop_self_loop, "stop_self_loop");
	
				time = System.currentTimeMillis();
				JDDNode unsatisfied_self_loop =
					JDD.Times(conditionUnsatisfied(ROW),
					          tau(),
					          JDD.Not(objectiveNormalStates.copy()), // do not deadlock normal-form states
					          normal(COLUMN),
					          JDD.Identity(originalModel.getAllDDRowVars(), originalModel.getAllDDColVars()));
				time = System.currentTimeMillis() - time;
				log.println(" stop_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(stop_self_loop));
	//			if (debug)
	//				JDD.PrintMinterms(log, unsatisfied_self_loop, "unsatisfied_self_loop");
	
				// plug new transitions together...
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, goal_self_loop, fail_self_loop);
				time = System.currentTimeMillis() - time;
				log.println("\n goal_self_loop\n  |= fail_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, stop_self_loop);
				time = System.currentTimeMillis() - time;
				log.println("  |= stop_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, unsatisfied_self_loop);
				time = System.currentTimeMillis() - time;
				log.println("  |= unsatisfied_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_goal);
				time = System.currentTimeMillis() - time;
				log.println("  |= objective_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_fail);
				time = System.currentTimeMillis() - time;
				log.println("  |= objective_to_fail: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_goal);
				time = System.currentTimeMillis() - time;
				log.println("  |= condition_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_stop);
				time = System.currentTimeMillis() - time;
				log.println("  |= condition_to_stop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_normal);
				time = System.currentTimeMillis() - time;
				log.println("  |= normal_to_normal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));
	
	//			if (debug)
	//			JDD.PrintMinterms(log, newTrans.copy(), "newTrans");
	
				return newTrans;
			}
	
			@Override
			public JDDNode getTransformedStart() throws PrismException
			{
				// FIXME ALG: use states of interest as start function
				JDDNode start = JDD.And(normal(ROW), originalModel.getStart().copy());
	//			if (debug)
	//				JDD.PrintMinterms(log, start.copy(), "start");
				return start;
			}
	
			public JDDVars getExtraRowVars()
			{
				return extraRowVars;
			}
		}
}