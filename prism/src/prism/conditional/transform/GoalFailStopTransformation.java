package prism.conditional.transform;

import java.util.function.Function;

import common.StopWatch;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.PrismException;
import prism.PrismLog;
import prism.ProbModel;
import prism.ProbModelTransformation;
import prism.StateValues;

public class GoalFailStopTransformation<M extends ProbModel> implements ModelTransformation<M, M>
{
	public static final boolean ROW    = true;
	public static final boolean COLUMN = false;

	protected ModelTransformation<M, M> transformation;
	protected String goalLabel;
	protected String failLabel;
	protected String stopLabel;
	protected String badLabel;

	/**
	 * [ REFS: <i>...</i>, DEREFS: <i>...</i> ]
	 */
	public GoalFailStopTransformation(ModelTransformation<M, M> transformation, String goalLabel, String failLabel, String stopLabel, String badLabel) throws PrismException
	{
		this.transformation = transformation;
		this.goalLabel      = goalLabel;
		this.failLabel      = failLabel;
		this.stopLabel      = stopLabel;
		this.badLabel       = badLabel;
	}

	/**
	 * [ REFS: <i>...</i>, DEREFS: <i>...</i> ]
	 */
	public GoalFailStopTransformation(M model, GoalFailStopOperator<M> operator, JDDNode badStates) throws PrismException
	{
		this.transformation = operator.apply(model);
		M transformedModel = getTransformedModel();

		// store trap states under a unique label
		goalLabel = transformedModel.addUniqueLabelDD("goal", JDD.And(operator.goal(ROW), transformedModel.getReach().copy()));
		failLabel = transformedModel.addUniqueLabelDD("fail", JDD.And(operator.fail(ROW), transformedModel.getReach().copy()));
		stopLabel = transformedModel.addUniqueLabelDD("stop", JDD.And(operator.stop(ROW), transformedModel.getReach().copy()));

		// FIXME ALG: Exclude non-trap states and normal-form states from bad states
		JDDNode nonTrapStates    = JDD.And(operator.notrap(ROW), transformedModel.getReach().copy());
		JDDNode normalFormStates = JDD.And(JDD.Or(operator.getConditionNormalStates(), operator.getObjectiveNormalStates()), transformedModel.getReach().copy());
		badStates                = JDD.And(badStates, nonTrapStates);
		badStates                = JDD.And(badStates, normalFormStates);
		badLabel                 = transformedModel.addUniqueLabelDD("bad", badStates);
		////>>> Debug: print badStates
		//getLog().println("badStates:");
		//new StateValuesMTBDD(badStates.copy(), transformedModel()).print(getLog());
	}

	public GoalFailStopTransformation<M> compose(ModelTransformation<M,M> transformation) throws PrismException
	{
		ModelTransformationNested<M,M,M> nested = new ModelTransformationNested<>(transformation, this);
		return new GoalFailStopTransformation<>(nested, goalLabel, failLabel, stopLabel, badLabel);
	}

	@Override
	public M getOriginalModel()
	{
		return transformation.getOriginalModel();
	}

	@Override
	public M getTransformedModel()
	{
		return transformation.getTransformedModel();
	}

	@Override
	public void clear()
	{
		transformation.clear();
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
	{
		return transformation.projectToOriginalModel(svTransformedModel);
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return transformation.getTransformedStatesOfInterest();
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

	public String getBadLabel()
	{
		return badLabel;
	}

//	public JDDNode getConditonUnsatisfiedStates()
//	{
//		return operator.getConditionUnsatisfied();
//	}
//
//	public JDDNode getNormalFormStates()
//	{
//		return JDD.And(JDD.Or(operator.getConditionNormalStates(), operator.getObjectiveNormalStates()), transformedModel.getReach().copy());
//	}
//
//	public JDDNode getNonTrapStates()
//	{
//		return JDD.And(operator.notrap(ROW), transformedModel.getReach().copy().copy());
//	}
//
//	public JDDNode getTrapStates()
//	{
//		return JDD.And(operator.trap(ROW), transformedModel.getReach().copy().copy());
//	}



	public static interface GoalFailStopOperator<M extends ProbModel>
	{
		default ModelTransformation<M, M> apply(M model) throws PrismException
		{
			GoalFailStopOperator<M> operator = this;
			M transformedModel = this.transform(model);

			return new ModelTransformation<M, M>()
			{
				@Override
				public M getOriginalModel()
				{
					return model;
				}

				@Override
				public M getTransformedModel()
				{
					return transformedModel;
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

					StateValues svOriginalModel = svTransformedModel.sumOverDDVars(operator.getExtraRowVars(), model);
					svTransformedModel.clear();

					return svOriginalModel;
				}

				@Override
				public JDDNode getTransformedStatesOfInterest()
				{
					return transformedModel.getStart().copy();
				}
			};
		}

		M transform(M model) throws PrismException;

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

		default JDDNode notrap(boolean row)
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
			JDDNode result = JDD.Not(notrap(row));

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
			JDDNode result = JDD.And((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
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
			log.println(" normal_to_normal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_normal));
			//			if (debug) {
			//				JDD.PrintMinterms(log, originalModel.getTrans().copy(), "trans");
			//				JDD.PrintMinterms(log, normal_to_normal.copy(), "normal_to_normal");
			//			}

			JDDNode objective_to_goal = watch.run(this::transformObjectiveToGoal);
			log.println(" objective_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(objective_to_goal));
			//			if (debug)
			//				JDD.PrintMinterms(log, objective_to_goal.copy(), "objective_to_goal");

			JDDNode objective_to_fail = watch.run(this::transformObjectiveToFail);
			log.println(" objective_to_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(objective_to_fail));
			//			if (debug)
			//				JDD.PrintMinterms(log, objective_to_fail.copy(), "objective_to_fail");

			JDDNode condition_to_goal = watch.run(this::transformConditionToGoal);
			log.println(" condition_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(condition_to_goal));
			//			if (debug)
			//				JDD.PrintMinterms(log, condition_to_goal.copy(), "condition_to_goal");

			JDDNode condition_to_stop = watch.run(this::transformConditionToStop);
			log.println(" condition_to_stop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(condition_to_stop));
			//			if (debug)
			//				JDD.PrintMinterms(log, condition_to_stop.copy(), "condition_to_stop");

			JDDNode unsatisfied_to_fail = watch.run(this::transformUnsatisfiedToFail);
			log.println(" unsatisfied_to_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(unsatisfied_to_fail));
			//			if (debug)
			//				JDD.PrintMinterms(log, unsatisfied_self_loop, "unsatisfied_self_loop");

			JDDNode goal_self_loop = watch.run(this::transformGoalSelfLoop);
			log.println(" goal_self_loop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(goal_self_loop));
			//			if (debug)
			//				JDD.PrintMinterms(log, goal_self_loop.copy(), "goal_self_loop");

			JDDNode fail_self_loop = watch.run(this::transformFailSelfLoop);
			log.println(" fail_self_loop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(fail_self_loop));
			//			if (debug)
			//				JDD.PrintMinterms(log, fail_self_loop.copy(), "fail_self_loop");

			JDDNode stop_self_loop = watch.run(this::transformStopSelfLoop);
			log.println(" stop_self_loop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(stop_self_loop));
			//			if (debug)
			//				JDD.PrintMinterms(log, stop_self_loop, "stop_self_loop");

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
			log.println("  |= stop_self_loop" + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, unsatisfied_to_fail);
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
			return JDD.Times(notrap(ROW), JDD.Not(getObjectiveNormalStates()), JDD.Not(getConditionNormalStates()), JDD.Not(getConditionUnsatisfied()),
					notTau(), notrap(COLUMN), getOriginalModel().getTrans().copy());
		}

		default JDDNode transformObjectiveToGoal()
		{
			return JDD.Times(notrap(ROW), getObjectiveNormalStates(), tau(), goal(COLUMN), getConditionNormalProbs());
		}

		default JDDNode transformObjectiveToFail()
		{
			JDDNode oneMinusConditionNormalProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getConditionNormalProbs());
			return JDD.Times(notrap(ROW), getObjectiveNormalStates(), tau(), fail(COLUMN), oneMinusConditionNormalProbs);
		}

		default JDDNode transformConditionToGoal()
		{
			return JDD.Times(notrap(ROW), getConditionNormalStates(), tau(), goal(COLUMN), getObjectiveNormalProbs());
		}

		default JDDNode transformConditionToStop()
		{
			JDDNode oneMinusObjectiveNormalProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getObjectiveNormalProbs());
			return JDD.Times(notrap(ROW), getConditionNormalStates(), tau(), stop(COLUMN), oneMinusObjectiveNormalProbs);
		}

		default JDDNode transformGoalSelfLoop()
		{
			return JDD.Times(goal(ROW), tau(), goal(COLUMN));
		}

		default JDDNode transformFailSelfLoop()
		{
			return JDD.Times(fail(ROW), tau(), fail(COLUMN));
		}

		default JDDNode transformStopSelfLoop()
		{
			return JDD.Times(stop(ROW), tau(), stop(COLUMN));
		}

		default JDDNode transformUnsatisfiedToFail()
		{
			return JDD.Times(notrap(ROW), getConditionUnsatisfied(), JDD.Not(getObjectiveNormalStates()), // do not deadlock normal-form states
					tau(), fail(COLUMN));
		}

		default JDDNode getTransformedStart() throws PrismException
		{
			// FIXME ALG: use states of interest as start function
			JDDNode start = JDD.And(notrap(ROW), getOriginalModel().getStart().copy());
			//			if (debug)
			//				JDD.PrintMinterms(log, start.copy(), "start");
			return start;
			//			return getOriginalModel().getReach().copy();
		}



		public static class MDP extends NondetModelTransformation implements GoalFailStopOperator<NondetModel>
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
			public MDP(NondetModel model, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs, JDDNode conditionNormalStates,
					JDDNode conditionNormalProbs, JDDNode conditionUnsatisfied, PrismLog log) throws PrismException
			{
				super(model);
				this.log = log;

				assert (!JDD.AreIntersecting(objectiveNormalStates, conditionNormalStates));

				this.objectiveNormalStates = objectiveNormalStates;
				this.objectiveNormalProbs  = objectiveNormalProbs;
				this.conditionNormalStates = conditionNormalStates;
				this.conditionNormalProbs  = conditionNormalProbs;
				this.conditionUnsatisfied  = conditionUnsatisfied;
			}

			@Override
			public NondetModel transform(NondetModel model) throws PrismException
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
				return JDD.And(notrap(ROW), conditionUnsatisfied.copy());
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



		public static class DTMC extends ProbModelTransformation implements GoalFailStopOperator<ProbModel>
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
			public DTMC(ProbModel model, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs, JDDNode conditionNormalStates,
					JDDNode conditionNormalProbs, JDDNode conditionUnsatisfied, PrismLog log) throws PrismException
			{
				super(model);
				this.log = log;

				assert (!JDD.AreIntersecting(objectiveNormalStates, conditionNormalStates));

				this.objectiveNormalStates = objectiveNormalStates;
				this.objectiveNormalProbs  = objectiveNormalProbs;
				this.conditionNormalStates = conditionNormalStates;
				this.conditionNormalProbs  = conditionNormalProbs;
				this.conditionUnsatisfied  = conditionUnsatisfied;
			}

			@Override
			public ProbModel transform(ProbModel model) throws PrismException
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
				return JDD.And(notrap(ROW), conditionUnsatisfied.copy());
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
}
