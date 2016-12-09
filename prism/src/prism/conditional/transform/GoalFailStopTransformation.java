package prism.conditional.transform;

import java.util.Objects;
import java.util.function.Function;

import common.StopWatch;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.Model;
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

		JDDNode nonTrapStates    = operator.notrap(ROW);
		JDDNode normalFormStates = JDD.Or(operator.getGoalStopStates(), operator.getGoalFailStates());
		badStates                = JDD.And(badStates, transformedModel.getReach().copy());
		badStates                = JDD.And(badStates, nonTrapStates);
		badStates                = JDD.And(badStates, JDD.Not(normalFormStates));
		badLabel                 = transformedModel.addUniqueLabelDD("bad", badStates);
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



	public static class ProbabilisticRedistribution
	{
		protected JDDNode states;
		protected JDDNode probabilities; 

		public ProbabilisticRedistribution()
		{
			this(JDD.Constant(0), JDD.Constant(0));
		}

		public ProbabilisticRedistribution(JDDNode states, JDDNode probabilities)
		{
			Objects.requireNonNull(states);
			Objects.requireNonNull(probabilities);
			this.states        = states;
			this.probabilities = probabilities;
		}

		public JDDNode getStates()
		{
			return states.copy();
		}

		public JDDNode getProbabilities()
		{
			return probabilities.copy();
		}

		public ProbabilisticRedistribution swap(Model model)
		{
			// inverse probabilities to swap target states
			probabilities = JDD.Apply(JDD.MINUS, model.getReach().copy(), probabilities);
			return this;
		}

		public void clear()
		{
			JDD.Deref(states, probabilities);
			states = probabilities = null;
		}
	}


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

		JDDNode getGoalFailStates();

		JDDNode getGoalFailProbs();

		JDDNode getGoalStopStates();

		JDDNode getGoalStopProbs();

		JDDNode getStopFailStates();

		JDDNode getStopFailProbs();

		JDDNode getInstantGoalStates();

		JDDNode getInstantFailStates();

		JDDNode getStatesOfInterest();

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
			log.println(" normal_to_normal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_normal));
			//			if (debug) {
			//				JDD.PrintMinterms(log, originalModel.getTrans().copy(), "trans");
			//				JDD.PrintMinterms(log, normal_to_normal.copy(), "normal_to_normal");
			//			}

			JDDNode normal_to_goal_fail= watch.run(this::transformToGoalFail);
			log.println(" normal_to_goal_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_goal_fail));
			//			if (debug)
			//				JDD.PrintMinterms(log, normal_to_goal_fail.copy(), "normal_to_goal_fail");

			JDDNode normal_to_goal_stop = watch.run(this::transformNormalToGoalStop);
			log.println(" normal_to_goal_stop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_goal_stop));
			//			if (debug)
			//				JDD.PrintMinterms(log, normal_to_goal_stop.copy(), "normal_to_goal_stop");

			JDDNode normal_to_stop_fail = watch.run(this::transformNormalToStopFail);
			log.println(" normal_to_stop_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_stop_fail));
			//			if (debug)
			//				JDD.PrintMinterms(log, normal_to_stop_fail.copy(), "normal_to_stop_fail");

//			JDDNode objective_to_goal = watch.run(this::transformObjectiveToGoal);
//			log.println(" objective_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(objective_to_goal));
//			//			if (debug)
//			//				JDD.PrintMinterms(log, objective_to_goal.copy(), "objective_to_goal");
//
//			JDDNode objective_to_fail = watch.run(this::transformObjectiveToFail);
//			log.println(" objective_to_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(objective_to_fail));
//			//			if (debug)
//			//				JDD.PrintMinterms(log, objective_to_fail.copy(), "objective_to_fail");
//
//			JDDNode condition_to_goal = watch.run(this::transformConditionToGoal);
//			log.println(" condition_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(condition_to_goal));
//			//			if (debug)
//			//				JDD.PrintMinterms(log, condition_to_goal.copy(), "condition_to_goal");
//
//			JDDNode condition_to_stop = watch.run(this::transformConditionToStop);
//			log.println(" condition_to_stop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(condition_to_stop));
//			//			if (debug)
//			//				JDD.PrintMinterms(log, condition_to_stop.copy(), "condition_to_stop");

			JDDNode normal_to_goal = watch.run(this::transformNormalToGoal);
			log.println(" normal_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_goal));
			//			if (debug)
			//				JDD.PrintMinterms(log, normal_to_goal, "normal_to_goal");

			JDDNode normal_to_fail = watch.run(this::transformNormalToFail);
			log.println(" normal_to_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(normal_to_fail));
			//			if (debug)
			//				JDD.PrintMinterms(log, normal_to_fail, "normal_to_fail");

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
			log.println(" goal_self_loop\n  |= fail_self_loop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, stop_self_loop);
			watch.stop();
			log.println("  |= stop_self_loop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_goal_fail);
			watch.stop();
			log.println("  |= normal_to_goal_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_goal_stop);
			watch.stop();
			log.println("  |= normal_to_goal_stop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_stop_fail);
			watch.stop();
			log.println("  |= normal_to_stop_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

//			watch.start();
//			newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_goal);
//			watch.stop();
//			log.println("  |= objective_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));
//
//			watch.start();
//			newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_fail);
//			watch.stop();
//			log.println("  |= objective_to_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));
//
//			watch.start();
//			newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_goal);
//			watch.stop();
//			log.println("  |= condition_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));
//
//			watch.start();
//			newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_stop);
//			watch.stop();
//			log.println("  |= condition_to_stop: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_goal);
			watch.stop();
			log.println("  |= normal_to_goal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_fail);
			watch.stop();
			log.println("  |= normal_to_fail: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

			watch.start();
			newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_normal);
			watch.stop();
			log.println("  |= normal_to_normal: " + watch.elapsedSeconds() + " seconds, " + printNumNodes.apply(newTrans));

//			if (debug)
//			JDD.PrintMinterms(log, newTrans.copy(), "newTrans");

			return newTrans;
		}

		default JDDNode transformNormalToNormal()
		{
			return JDD.Times(notrap(ROW),
			                 JDD.Not(getGoalFailStates()),
			                 JDD.Not(getGoalStopStates()),
			                 JDD.Not(getStopFailStates()),
			                 JDD.Not(getInstantGoalStates()),
			                 JDD.Not(getInstantFailStates()),
			                 notTau(),
			                 notrap(COLUMN),
			                 getOriginalModel().getTrans().copy());
		}

		default JDDNode transformToGoalFail()
		{
			JDDNode normalToGoal = JDD.Times(notrap(ROW),
			                                 getGoalFailStates(),
			                                 tau(),
			                                 goal(COLUMN),
			                                 getGoalFailProbs());

			JDDNode oneMinusGoalFailProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getGoalFailProbs());
			JDDNode normalToFail = JDD.Times(notrap(ROW),
			                                 getGoalFailStates(),
			                                 tau(),
			                                 fail(COLUMN),
			                                 oneMinusGoalFailProbs);

			return JDD.Apply(JDD.MAX, normalToFail, normalToGoal);
		}

//		default JDDNode transformObjectiveToGoal()
//		{
//			return JDD.Times(notrap(ROW),
//			                 getGoalFailStates(),
//			                 tau(),
//			                 goal(COLUMN),
//			                 getGoalFailProbs());
//		}
//
//		default JDDNode transformObjectiveToFail()
//		{
//			JDDNode oneMinusGoalFailProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getGoalFailProbs());
//			return JDD.Times(notrap(ROW),
//			                 getGoalFailStates(),
//			                 tau(),
//			                 fail(COLUMN),
//			                 oneMinusGoalFailProbs);
//		}

		default JDDNode transformNormalToGoalStop()
		{
			JDDNode normalToGoal = JDD.Times(notrap(ROW),
			                                 getGoalStopStates(),
			                                 tau(),
			                                 goal(COLUMN),
			                                 getGoalStopProbs());

			JDDNode oneMinusGoalStopProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getGoalStopProbs());
			JDDNode normalToStop = JDD.Times(notrap(ROW),
			                                 getGoalStopStates(),
			                                 tau(),
			                                 stop(COLUMN),
			                                 oneMinusGoalStopProbs);

			return JDD.Apply(JDD.MAX, normalToGoal, normalToStop);
		}

//		default JDDNode transformConditionToGoal()
//		{
//			return JDD.Times(notrap(ROW),
//			                 getGoalStopStates(),
//			                 tau(),
//			                 goal(COLUMN),
//			                 getGoalStopProbsProbs());
//		}
//
//		default JDDNode transformConditionToStop()
//		{
//			JDDNode oneMinusGoalStopProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getGoalStopProbsProbs());
//			return JDD.Times(notrap(ROW),
//			                 getGoalStopStates(),
//			                 tau(),
//			                 stop(COLUMN),
//			                 oneMinusGoalStopProbs);
//		}

		default JDDNode transformNormalToStopFail()
		{
			JDDNode normalToStop = JDD.Times(notrap(ROW),
			                                 getStopFailStates(),
			                                 tau(),
			                                 stop(COLUMN),
			                                 getStopFailProbs());

			JDDNode oneMinusStopFailProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), getStopFailProbs());
			JDDNode normalToFail = JDD.Times(notrap(ROW),
			                                 getStopFailStates(),
			                                 tau(),
			                                 fail(COLUMN),
			                                 oneMinusStopFailProbs);

			return JDD.Apply(JDD.MAX, normalToStop, normalToFail);
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

		default JDDNode transformNormalToGoal()
		{
			return JDD.Times(notrap(ROW),
			                 getInstantGoalStates(),
			                 JDD.Not(getGoalFailStates()), // do not infer with probabilistic states
			                 JDD.Not(getGoalStopStates()), // do not infer with probabilistic states
			                 tau(),
			                 goal(COLUMN));
		}

		default JDDNode transformNormalToFail()
		{
			return JDD.Times(notrap(ROW),
			                 getInstantFailStates(),
			                 JDD.Not(getGoalFailStates()), // do not infer with probabilistic states
			                 tau(),
			                 fail(COLUMN));
		}

		default JDDNode getTransformedStart() throws PrismException
		{
			return JDD.And(notrap(ROW), getStatesOfInterest());
//			return JDD.And(notrap(ROW),
//			               getOriginalModel().getStart().copy());
		}



		public static class DTMC extends ProbModelTransformation implements GoalFailStopOperator<ProbModel>
		{
			protected PrismLog log;

			private ProbabilisticRedistribution goalFail;
			private ProbabilisticRedistribution goalStop;
			private ProbabilisticRedistribution stopFail;
			protected JDDNode instantGoalStates;
			protected JDDNode instantFailStates;
			protected JDDNode statesOfInterest;



			/**
			 * [ REFS: <i>none</i>, DEREFS: (on clear) <i>goalFailStates, goalFailProbs, goalStopStates, goalStopProbs, instantFailStates, and statesOfInterest</i> ]
			 */
			public DTMC(ProbModel model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest, PrismLog log)
					throws PrismException
			{
				this(model, goalFail, goalStop, new ProbabilisticRedistribution(), instantGoalStates, instantFailStates, statesOfInterest, log);
			}

			/**
			 * [ REFS: <i>none</i>, DEREFS: (on clear) <i>goalFailStates, goalFailProbs, goalStopStates, goalStopProbs, instantFailStates, and statesOfInterest</i> ]
			 */
			public DTMC(ProbModel model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest, PrismLog log)
					throws PrismException
			{
				super(model);
				this.log = log;

				// FIXME ALG: ensure that the state set do not overlap 
//				assert (!JDD.AreIntersecting(goalFailStates, goalStopStates));
//				assert (!JDD.AreIntersecting(goalFailStates, instantFailStates));

				this.goalFail          = goalFail;
				this.goalStop          = goalStop;
				this.stopFail          = stopFail;
				this.instantGoalStates = instantGoalStates;
				this.instantFailStates = instantFailStates;
				this.statesOfInterest  = statesOfInterest;
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
			public JDDNode getGoalFailStates()
			{
				return goalFail.getStates();
			}

			@Override
			public JDDNode getGoalFailProbs()
			{
				return goalFail.getProbabilities();
			}

			@Override
			public JDDNode getGoalStopStates()
			{
				return goalStop.getStates();
			}

			@Override
			public JDDNode getGoalStopProbs()
			{
				return goalStop.getProbabilities();
			}

			@Override
			public JDDNode getStopFailStates()
			{
				return stopFail.getStates();
			}

			@Override
			public JDDNode getStopFailProbs()
			{
				return stopFail.getProbabilities();
			}

			@Override
			public JDDNode getInstantGoalStates()
			{
				return instantGoalStates.copy();
			}

			@Override
			public JDDNode getInstantFailStates()
			{
				return instantFailStates.copy();
			}

			@Override
			public JDDNode getStatesOfInterest()
			{
				return statesOfInterest.copy();
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
				goalFail.clear();
				goalStop.clear();
				stopFail.clear();
				JDD.Deref(instantGoalStates, instantFailStates, statesOfInterest);
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



		public static class MDP extends NondetModelTransformation implements GoalFailStopOperator<NondetModel>
		{
			protected PrismLog log;

			private ProbabilisticRedistribution goalFail;
			private ProbabilisticRedistribution goalStop;
			private ProbabilisticRedistribution stopFail;
			protected JDDNode instantGoalStates;
			protected JDDNode instantFailStates;
			protected JDDNode statesOfInterest;



			/**
			 * [ REFS: <i>none</i>, DEREFS: (on clear) <i>goalFailStates, goalFailProbs, goalStopStates, goalStopProbs, instantFailStates, and statesOfInterest</i> ]
			 */
			public MDP(NondetModel model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest, PrismLog log)
					throws PrismException
			{
				this(model, goalFail, goalStop, new ProbabilisticRedistribution(), instantGoalStates, instantFailStates, statesOfInterest, log);
			}

			/**
			 * [ REFS: <i>none</i>, DEREFS: (on clear) <i>goalFailStates, goalFailProbs, goalStopStates, goalStopProbs, instantFailStates, and statesOfInterest</i> ]
			 */
			public MDP(NondetModel model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest, PrismLog log)
					throws PrismException
			{
				super(model);
				this.log = log;

				// FIXME ALG: ensure that the state set do not overlap 
//				assert (!JDD.AreIntersecting(goalFailStates, goalStopStates));
//				assert (!JDD.AreIntersecting(goalFailStates, instantFailStates));

				this.goalFail          = goalFail;
				this.goalStop          = goalStop;
				this.stopFail          = stopFail;
				this.instantGoalStates = instantGoalStates;
				this.instantFailStates = instantFailStates;
				this.statesOfInterest  = statesOfInterest;
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
			public JDDNode getGoalFailStates()
			{
				return goalFail.getStates();
			}

			@Override
			public JDDNode getGoalFailProbs()
			{
				return goalFail.getProbabilities();
			}

			@Override
			public JDDNode getGoalStopStates()
			{
				return goalStop.getStates();
			}

			@Override
			public JDDNode getGoalStopProbs()
			{
				return goalStop.getProbabilities();
			}

			@Override
			public JDDNode getStopFailStates()
			{
				return stopFail.getStates();
			}

			@Override
			public JDDNode getStopFailProbs()
			{
				return stopFail.getProbabilities();
			}

			@Override
			public JDDNode getInstantGoalStates()
			{
				return instantGoalStates.copy();
			}

			@Override
			public JDDNode getInstantFailStates()
			{
				return instantFailStates.copy();
			}

			@Override
			public JDDNode getStatesOfInterest()
			{
				return statesOfInterest.copy();
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
				goalFail.clear();
				goalStop.clear();
				stopFail.clear();
				JDD.Deref(instantGoalStates, instantFailStates, statesOfInterest);
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
	}
}
