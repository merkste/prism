package prism.conditional.prototype;

import java.io.File;
import java.io.FileNotFoundException;

import mtbdd.PrismMTBDD;
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
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.NondetModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.conditional.NewConditionalTransformer;

@Deprecated
public class MDPFinallyTransformer extends NewConditionalTransformer.MDP
{
	private boolean debug = false;
	
	public MDPFinallyTransformer(NondetModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		final Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	public ModelExpressionTransformation<NondetModel, NondetModel> transform(
			final NondetModel model,
			final ExpressionConditional expression,
			final JDDNode statesOfInterest)
			throws PrismException
	{

		// Create fresh model checker for model
		NondetModelChecker mc = (NondetModelChecker) modelChecker.createModelChecker(model);

		long time;
		
		// compute E aka "objective goalState"
		final ExpressionProb objectiveProb = (ExpressionProb) expression.getObjective();
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objectiveProb.getExpression())).getOperand2();
		getLog().println("Compute F (target for objective)");
		time = System.currentTimeMillis();
		final JDDNode objectiveGoalStates = mc.checkExpressionDD(objectiveGoal, JDD.Constant(1));
		time = System.currentTimeMillis() - time;
		getLog().println("Time for computing F: " + time / 1000.0 + " seconds.");
		if (debug)
			JDD.PrintMinterms(getLog(), objectiveGoalStates.copy(), "objectiveGoalStates");
		
		// compute C aka "condition goalState"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		time = System.currentTimeMillis();
		getLog().println("Compute G (target for condition)");
		final JDDNode conditionGoalStates = mc.checkExpressionDD(conditionGoal, JDD.Constant(1));
		time = System.currentTimeMillis() - time;
		getLog().println("Time for computing G: " + time / 1000.0 + " seconds.");
		if (debug)
			JDD.PrintMinterms(getLog(), conditionGoalStates.copy(), "conditionGoalStates");
		
		// compute bad states == {s | Pmin=0[<> Condition]}
		JDDNode targetStates = conditionGoalStates.copy();
		JDDNode prob0E = PrismMTBDD.Prob0E(model.getTrans01(),
		                                            model.getReach(),
		                                            model.getNondetMask(),
		                                            model.getAllDDRowVars(),
		                                            model.getAllDDColVars(),
		                                            model.getAllDDNondetVars(),
		                                            model.getReach(),
		                                            targetStates);
		JDD.Deref(targetStates);
		final JDDNode badStates = JDD.And(prob0E, JDD.Not(objectiveGoalStates.copy()));

		// compute Pmax(<>E | C)
		time = System.currentTimeMillis();
		getLog().println("Compute Pmax(<> F)");
		StateValuesMTBDD objectiveMaxResult = mc.checkProbUntil(model.getReach(),
		                                                        objectiveGoalStates,
		                                                        false, false).convertToStateValuesMTBDD();
		time = System.currentTimeMillis() - time;
		getLog().println("Time for computing Pmax(<> F): " + time / 1000.0 + " seconds.");
		if (debug) {
			getLog().println("objectiveMaxResult");
			objectiveMaxResult.print(getLog());
		}
		final JDDNode objectiveMaxProbs = objectiveMaxResult.getJDDNode().copy();
		objectiveMaxResult.clear();

		// compute Pmax(<>C | E)
		JDDNode prob0A = PrismMTBDD.Prob0A(model.getTrans01(), model.getReach(), model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), model.getReach(), conditionGoalStates);
		if (JDD.IsContainedIn(statesOfInterest, prob0A)) {
			JDD.Deref(prob0A);
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		JDD.Deref(prob0A);
		time = System.currentTimeMillis();
		getLog().println("Compute Pmax(<> G)");
		StateValuesMTBDD conditionMaxResult = mc.checkProbUntil(model.getReach(),
		                                                        conditionGoalStates,
		                                                        false, false).convertToStateValuesMTBDD();
		time = System.currentTimeMillis() - time;
		getLog().println("Time for computing Pmax(<> G): " + time / 1000.0 + " seconds.");
		if (debug) {
			getLog().println("conditionMaxResult");
			conditionMaxResult.print(getLog());
		}

		final JDDNode conditionMaxProbs = conditionMaxResult.getJDDNode().copy();
		conditionMaxResult.clear();

		time = System.currentTimeMillis();
		getLog().println("Compute G' = G \\ F:");
		final JDDNode conditionGoalStatesClean = JDD.And(conditionGoalStates.copy(), JDD.Not(objectiveGoalStates.copy()));
		time = System.currentTimeMillis() - time;
		getLog().println("Time for G': " + time / 1000.0 + " seconds.");
		if (debug)
			JDD.PrintMinterms(getLog(), conditionGoalStatesClean.copy(), "conditionGoalStatesClean");

		class GoalFailTransformation extends NondetModelTransformation {
			public GoalFailTransformation(NondetModel model)
			{
				super(model);
			}

			public JDDNode normal(boolean row) {
				return JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
				               JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));
			}

			public JDDNode fail(boolean row) {
				JDDNode result = JDD.And((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
				                         JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));

				JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
				for (int i = 0; i < vars.getNumVars(); i++) {
					result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
				}
				return result;
			}

			public JDDNode stop(boolean row) {
				JDDNode result = JDD.And((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
				                         (row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy());

				JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
				for (int i = 0; i < vars.getNumVars(); i++) {
					result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
				}
				return result;
			}
			
			public JDDNode goal(boolean row) {
				JDDNode result = JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
				                         (row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy());

				JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
				for (int i = 0; i < vars.getNumVars(); i++) {
					result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
				}
				return result;
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

				getLog().println("Goal/fail transformation:");

				if (debug)
					originalModel.printTransInfo(getLog(), true);

				time = System.currentTimeMillis();
				JDDNode normal_to_normal =
					JDD.Times(normal(true),
					          notTau(),
					          JDD.And(JDD.Not(conditionGoalStates.copy()),
					                  JDD.Not(objectiveGoalStates.copy())),
					          normal(false),
					          originalModel.getTrans().copy());
				time = System.currentTimeMillis() - time;
				getLog().println(" normal_to_normal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(normal_to_normal));
				if (debug) {
					JDD.PrintMinterms(getLog(), originalModel.getTrans().copy(), "trans");
					JDD.PrintMinterms(getLog(), normal_to_normal.copy(), "normal_to_normal");
				}

				time = System.currentTimeMillis();
				JDDNode objective_to_goal =
					JDD.Times(normal(true),
					          tau(),
					          objectiveGoalStates.copy(),
					          goal(false),
					          conditionMaxProbs.copy());
				time = System.currentTimeMillis() - time;
				getLog().println(" objective_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(objective_to_goal));
				if (debug)
					JDD.PrintMinterms(getLog(), objective_to_goal.copy(), "objective_to_goal");

				time = System.currentTimeMillis();
				JDDNode oneMinusConditionMaxProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), conditionMaxProbs.copy());
				JDDNode objective_to_fail =
					JDD.Times(normal(true),
					          tau(),
					          objectiveGoalStates.copy(),
					          fail(false),
					          oneMinusConditionMaxProbs);
				time = System.currentTimeMillis() - time;
				getLog().println(" objective_to_fail: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(objective_to_fail));
				if (debug)
					JDD.PrintMinterms(getLog(), objective_to_fail.copy(), "objective_to_fail");

				time = System.currentTimeMillis();
				JDDNode condition_to_goal =
					JDD.Times(normal(true),
					          tau(),
					          conditionGoalStatesClean.copy(),
					          goal(false),
					          objectiveMaxProbs.copy());
				time = System.currentTimeMillis() - time;
				getLog().println(" condition_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(condition_to_goal));
				if (debug)
					JDD.PrintMinterms(getLog(), condition_to_goal.copy(), "condition_to_goal");

				time = System.currentTimeMillis();
				JDDNode oneMinusObjectiveMaxProbs = JDD.Apply(JDD.MINUS, JDD.Constant(1), objectiveMaxProbs.copy());
				JDDNode condition_to_stop =
					JDD.Times(normal(true),
					          tau(),
					          conditionGoalStatesClean.copy(),
					          stop(false),
					          oneMinusObjectiveMaxProbs);
				time = System.currentTimeMillis() - time;
				getLog().println(" condition_to_stop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(condition_to_stop));
				if (debug)
					JDD.PrintMinterms(getLog(), condition_to_stop.copy(), "condition_to_stop");

				time = System.currentTimeMillis();
				JDDNode goal_self_loop =
					JDD.Times(goal(true),
					          tau(),
					          goal(false));
				time = System.currentTimeMillis() - time;
				getLog().println(" goal_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(goal_self_loop));
				if (debug)
					JDD.PrintMinterms(getLog(), goal_self_loop.copy(), "goal_self_loop");

				time = System.currentTimeMillis();
				JDDNode startCol =
						JDD.PermuteVariables(statesOfInterest.copy(),
						                     originalModel.getAllDDRowVars(),
						                     originalModel.getAllDDColVars());

				time = System.currentTimeMillis();
				JDDNode bad_reset =
					JDD.Times(badStates.copy(),
					          normal(true),
					          tau(),
					          startCol.copy(),
					          normal(false));
				time = System.currentTimeMillis() - time;
				getLog().println(" bad_reset: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(bad_reset));
				if (debug) {
					JDD.PrintMinterms(getLog(), bad_reset.copy(), "bad_reset");
				}

				time = System.currentTimeMillis();
				JDDNode fail_reset =
					JDD.Times(fail(true),
					          tau(),
					          startCol,
					          normal(false));
				time = System.currentTimeMillis() - time;
				getLog().println(" fail_reset: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(fail_reset));
				if (debug)
					JDD.PrintMinterms(getLog(), fail_reset.copy(), "fail_reset");

				time = System.currentTimeMillis();
				JDDNode stop_self_loop =
					JDD.Times(stop(true),
					          tau(),
					          stop(false));
				time = System.currentTimeMillis() - time;
				getLog().println(" stop_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(stop_self_loop));
				if (debug)
					JDD.PrintMinterms(getLog(), stop_self_loop, "stop_self_loop");

				
				// plug new transitions together...
				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, goal_self_loop, fail_reset);
				time = System.currentTimeMillis() - time;
				getLog().println("\n goal_self_loop\n  |= fail_reset: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, stop_self_loop);
				time = System.currentTimeMillis() - time;
				getLog().println("  |= stop_self_loop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_goal);
				time = System.currentTimeMillis() - time;
				getLog().println("  |= objective_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, objective_to_fail);
				time = System.currentTimeMillis() - time;
				getLog().println("  |= objective_to_fail: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_goal);
				time = System.currentTimeMillis() - time;
				getLog().println("  |= condition_to_goal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, condition_to_stop);
				time = System.currentTimeMillis() - time;
				getLog().println("  |= condition_to_stop: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, bad_reset);
				time = System.currentTimeMillis() - time;
				getLog().println("  |= bad_to_fail: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				time = System.currentTimeMillis();
				newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_normal);
				time = System.currentTimeMillis() - time;
				getLog().println("  |= normal_to_normal: "+ time / 1000.0 +" seconds, MTBDD nodes = "+JDD.GetNumNodes(newTrans));

				if (debug) JDD.PrintMinterms(getLog(), newTrans.copy(), "newTrans");

				return newTrans;
			}

			@Override
			public JDDNode getTransformedStart() throws PrismException
			{
				JDDNode newStart = JDD.And(normal(true), statesOfInterest.copy());
				if (debug)
					JDD.PrintMinterms(getLog(), newStart.copy(), "newStart");
				return newStart;
			}

			public JDDVars getExtraStateVars()
			{
				return extraRowVars.copy();
			}
		};

		
		GoalFailTransformation transform = new GoalFailTransformation(model);

		getLog().println("\nTransforming using goal/fail transformation...");
		time = System.currentTimeMillis();
		final NondetModel transformedModel = model.getTransformed(transform);
		getLog().println("Time for goal/fail transformation: " + time / 1000.0 + " seconds.\n");
		final JDDNode goalStates = transform.goal(true);
		// store goal state under a unique label
		final String goalLabel = transformedModel.addUniqueLabelDD("goal", goalStates);

		final JDDNode mask = transform.getTransformedStart();
		final JDDVars extraRowVars = transform.getExtraStateVars();

		final ExpressionProb expressionProb = (ExpressionProb)expression.getObjective();
		// TODO: is this correct?
		final ExpressionProb transformedExpression = (ExpressionProb) expressionProb.deepCopy();
		transformedExpression.setExpression(new ExpressionTemporal(ExpressionTemporal.P_F, null, new ExpressionLabel(goalLabel)));
		transformedExpression.typeCheck();

		if (debug) {
			try {
				transformedModel.exportToFile(Prism.EXPORT_DOT, true, new File("t.dot"));
			} catch (FileNotFoundException e) {}
		}

		// cleanup

		transform.clear();
		JDD.Deref(badStates, conditionMaxProbs, objectiveMaxProbs, objectiveGoalStates,
		          conditionGoalStates, conditionGoalStatesClean, statesOfInterest);
		
		return new ModelExpressionTransformation<NondetModel, NondetModel>() {

			@Override
			public NondetModel getOriginalModel()
			{
				return model;
			}

			@Override
			public NondetModel getTransformedModel()
			{
				return transformedModel;
			}

			@Override
			public void clear()
			{
				JDD.Deref(mask);
				extraRowVars.derefAll();
				transformedModel.clear();
			}

			@Override
			public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
			{
				StateValuesMTBDD sv = svTransformedModel.convertToStateValuesMTBDD();
				if (debug) {
					getLog().println("sv:");
					sv.print(getLog());
					StateValuesMTBDD.print(getLog(), mask.copy(), getTransformedModel(), "mask");
				}
				sv.filter(mask);

				StateValues result = sv.sumOverDDVars(extraRowVars, getOriginalModel());
				if (debug) {
					getLog().println("result:");
					result.print(getLog());
				}
				sv.clear();

				return result;
			}

			@Override
			public Expression getTransformedExpression()
			{
				return transformedExpression;
			}

			@Override
			public Expression getOriginalExpression()
			{
				return expression;
			}

			@Override
			public JDDNode getTransformedStatesOfInterest()
			{
				return mask.copy();
			}
		};
	}
}
