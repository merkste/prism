package prism.conditional.prototype;

import java.io.File;
import java.io.FileNotFoundException;

import mtbdd.PrismMTBDD;
import common.StopWatch;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.LTLModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.NondetModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.conditional.NewConditionalTransformer;
import prism.conditional.transform.MDPResetTransformation;
import acceptance.AcceptanceRabinDD;
import acceptance.AcceptanceStreettDD;
import acceptance.AcceptanceType;

@Deprecated
public class MDPLTLTransformer extends NewConditionalTransformer.MDP
{
	boolean debug = false;
	boolean useNormalFormTransformation = false;

	public MDPLTLTransformer(NondetModelChecker modelChecker) {
		super(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return getLtlTransformer().canHandle(model, objective.getExpression());
	}

	@Override
	public ModelExpressionTransformation<NondetModel, NondetModel> transform(
			final NondetModel model,
			final ExpressionConditional expression,
			final JDDNode statesOfInterest)
			throws PrismException {

		if (!JDD.isSingleton(statesOfInterest, model.getAllDDRowVars())) {
			JDD.PrintMinterms(getLog(), statesOfInterest.copy());
			throw new PrismNotSupportedException("conditional MDP transformations only supported for a single state");
		}
		StopWatch stopwatch = new StopWatch(getLog());
		final LTLModelChecker ltlModelChecker = new LTLModelChecker(this);

		// 1. Condition LTL product
		final Expression condition = expression.getCondition();

		stopwatch.start("model x DSA(condition) product");
		final LTLProduct<NondetModel> conditionProduct =
			ltlModelChecker.constructProductMDP(modelChecker,
			                                    model,
			                                    condition,
			                                    statesOfInterest.copy(),
			                                    AcceptanceType.STREETT);
		final NondetModel conditionModel = conditionProduct.getProductModel();
		stopwatch.stop();

		// compute conditionGoalStates = { s : Pmax_s( acc(condition) )=1 }
		final AcceptanceStreettDD conditionAcceptance = (AcceptanceStreettDD) conditionProduct.getAcceptance();
		getLog().println("Compute Pmax=1[ condition ]...");
		stopwatch.start("Pmax=1[ condition ]");
		final JDDNode conditionGoalStates =
			ltlModelChecker.findAcceptingECStates(conditionAcceptance,
			                                      conditionProduct.getProductModel(), null, null, false);
		stopwatch.stop();

		// check whether the condition is satisfiable in the state of interest
		getLog().println("Check whether condition is satisfiable...");
		stopwatch.start("condition satisfiability check");
		JDDNode prob0AForCondition =
			PrismMTBDD.Prob0A(conditionModel.getTrans01(),
		                      conditionModel.getReach(),
		                      conditionModel.getAllDDRowVars(),
		                      conditionModel.getAllDDColVars(),
		                      conditionModel.getAllDDNondetVars(),
		                      conditionModel.getReach(),
		                      conditionGoalStates);
		StateValuesMTBDD sv = new StateValuesMTBDD(prob0AForCondition, conditionModel);
		StateValues svOriginal = conditionProduct.projectToOriginalModel(sv);
		stopwatch.stop();
		if (svOriginal.maxOverBDD(statesOfInterest) == 1.0) {
			// clean-up
			svOriginal.clear();
			JDD.Deref(statesOfInterest, conditionGoalStates);
			conditionProduct.clear();

			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		svOriginal.clear();

		// 2. Objective LTL product
		final ExpressionProb objectiveProb = (ExpressionProb)expression.getObjective();
		final Expression objective = objectiveProb.getExpression();
		stopwatch.start("model x DSA(condition) x DSA(objective) product");
		final LTLProduct<NondetModel> objectiveAndConditionProduct =
			ltlModelChecker.constructProductMDP(modelChecker,
			                                    conditionModel,
			                                    objective,
			                                    conditionModel.getStart().copy(),
			                                    AcceptanceType.STREETT);
		stopwatch.stop();

		// phi & psi Street acceptance
		final AcceptanceStreettDD objectiveAcceptance =
			(AcceptanceStreettDD)objectiveAndConditionProduct.getAcceptance();
		final AcceptanceStreettDD objectiveAndConditionAcceptance =
			objectiveAcceptance.and(conditionAcceptance);

		// compute "objective and condition goal states"
		final NondetModel objectiveAndConditionModel = objectiveAndConditionProduct.getProductModel();
		getLog().println("Compute Pmax=1[ objective & condition ])");
		stopwatch.start("Pmax=1[ objective & condition ]");
		final JDDNode objectiveAndConditionGoalStates =
			ltlModelChecker.findAcceptingECStates(objectiveAndConditionAcceptance,
			                                      objectiveAndConditionModel,
			                                      null,
			                                      null,
			                                      false);
		stopwatch.stop();
		getLog().println("Pmax=1[ objective & condition ] satisfied for "+JDD.GetNumMintermsString(objectiveAndConditionGoalStates, objectiveAndConditionModel.getNumDDRowVars())+" states.\n");		
		objectiveAndConditionAcceptance.clear();

		// compute "bad states", states where the scheduler has a strategy
		// to ensure that the condition is never satisfied
		final AcceptanceRabinDD complementConditionAcceptance = conditionAcceptance.complement();
		getLog().println("Compute Pmax=1[ !condition ]");
		stopwatch.start("Pmax=1[ !condition ]");
		// TODO(JK): Can be computed in conditionModel instead...
		JDDNode badEcStates = ltlModelChecker.findAcceptingECStates(complementConditionAcceptance, objectiveAndConditionModel, null, null, false);
		stopwatch.stop();
		getLog().println("Pmax=1[ !condition ] satisfied for "+JDD.GetNumMintermsString(badEcStates, objectiveAndConditionModel.getNumDDRowVars())+" states.\n");
		complementConditionAcceptance.clear();

		// restrict badEcStates to those that are not also objectiveAndConditionGoalStates
		badEcStates = JDD.And(badEcStates, JDD.Not(objectiveAndConditionGoalStates.copy()));

		// restrict bad states to those that appear in some R of a street pair of the condition
//		JDDNode r_states = JDD.Constant(0);
//		for (StreettPairDD streettPair : conditionAcceptance) {
//			r_states = JDD.Or(r_states, streettPair.getR());
//		}
//		if (debug) {
//			StateValuesMTBDD.print(prism.getLog(), r_states.copy(), objectiveAndConditionModel, "r_states");
//			StateValuesMTBDD.print(prism.getLog(), badEcStates.copy(), objectiveAndConditionModel, "badEcStates");
//		}
//		final JDDNode badStates = JDD.And(badEcStates, r_states);
		JDDNode badStates = badEcStates;
		getLog().println("Reset states (Pmax=1[!condition] and in R for some pair) = "+JDD.GetNumMintermsString(badStates, objectiveAndConditionModel.getNumDDRowVars())+" states.");		
		if (debug)
			StateValuesMTBDD.print(getLog(), badStates.copy(), objectiveAndConditionModel, "badStates");

		final JDDVars extraRowVars = new JDDVars();
		extraRowVars.mergeVarsFrom(objectiveAndConditionProduct.getAutomatonRowVars());
		extraRowVars.mergeVarsFrom(conditionProduct.getAutomatonRowVars());

		NondetModelTransformation transform = null;
		final NondetModel transformedModel;
		String goalLabel = null;
		if (!useNormalFormTransformation) {
			// use the reset transformation
			MDPResetTransformation mr_transform =
				new MDPResetTransformation(objectiveAndConditionModel,
				                                       badStates.copy(),
				                                       objectiveAndConditionModel.getStart().copy(),
				                                       getLog()
				                                      );

			getLog().println("\nTransforming using reset transformation...");
			stopwatch.start("reset transformation");
			transformedModel = objectiveAndConditionModel.getTransformed(mr_transform);
			stopwatch.stop();
			// store goal state under a unique label
			goalLabel = transformedModel.addUniqueLabelDD("goal", objectiveAndConditionGoalStates.copy());

			if (debug) mr_transform.debugDDs();

			transform = mr_transform;
		} else {
			// Normal Form Transformation
			// P′(s,goal) = Prmax_M,s(ψ)
			// P′(s,fail) = 1−Prmax_M,s(ψ)

			getLog().println("Compute Pmax[ condition ]");
			stopwatch.start("Pmax[ condition ]");
			// TODO(JK): Could be done in conditionModel and lifted...
			NondetModelChecker mcProduct = getModelChecker(objectiveAndConditionModel);
			StateValuesMTBDD conditionMaxResult =
					mcProduct.checkProbUntil(objectiveAndConditionModel.getReach(),  // true ...
					                         conditionGoalStates,   // ... Until target
					                         false,    // quantitative
					                         false     // max
							).convertToStateValuesMTBDD();
			// we are only interested in the objectiveAndConditionGoalStates
			conditionMaxResult.filter(objectiveAndConditionGoalStates);
			// TODO(JK): these should all be 1.0 anyways...
			final JDDNode conditionMaxProbs = conditionMaxResult.getJDDNode().copy();
			conditionMaxResult.clear();
			stopwatch.stop();

			MDPGoalFailResetTransformation mgfr_transform =
				new MDPGoalFailResetTransformation(objectiveAndConditionModel,
			                                       objectiveAndConditionGoalStates.copy(),
			                                       conditionMaxProbs.copy(),
			                                       badStates.copy(),
			                                       objectiveAndConditionModel.getStart().copy(),
			                                       getLog()
			                                      );

			JDD.Deref(conditionMaxProbs);
			getLog().println("\nTransforming using goal/fail/reset transformation...");
			stopwatch.start("goal/fail/reset transformation");
			transformedModel = objectiveAndConditionModel.getTransformed(mgfr_transform);
			stopwatch.stop();
			// store goal state under a unique label
			final JDDNode goalStates = mgfr_transform.goal(true);
			goalLabel = transformedModel.addUniqueLabelDD("goal", goalStates);

			if (debug) mgfr_transform.debugDDs();

			extraRowVars.mergeVarsFrom(mgfr_transform.getExtraStateVars());
			transform = mgfr_transform;
		}
		final JDDNode transformedStatesOfInterest = transform.getTransformedStart();

		final ExpressionProb transformedExpression = (ExpressionProb)objectiveProb.deepCopy();
		// replace expression
		transformedExpression.setExpression(new ExpressionTemporal(ExpressionTemporal.P_F, null, new ExpressionLabel(goalLabel)));
		transformedExpression.typeCheck();

		if (debug) {
			try {
				objectiveAndConditionModel.exportToFile(Prism.EXPORT_DOT_STATES, true, new File("product.dot"));
			} catch (FileNotFoundException e) {}

			try {
				transformedModel.exportToFile(Prism.EXPORT_DOT_STATES, true, new File("transformed.dot"));
			} catch (FileNotFoundException e) {}
		}

		// cleanup
		transform.clear();
		conditionProduct.clear();
		objectiveAndConditionProduct.clear();
		JDD.Deref(objectiveAndConditionGoalStates, badStates, conditionGoalStates, statesOfInterest);

		return new ModelExpressionTransformation<NondetModel, NondetModel>() {
			// references (and clears) the following variables:
			// - transformedModel (the transformed model)
			// - extraRowVars     (the automata row vars)
			// - mask             (the transformed states of interest)
			//
			// references
			//  - debug
			//  - model (the original model)
			//  - transformedExpression

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
				JDD.Deref(transformedStatesOfInterest);
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
					StateValuesMTBDD.print(getLog(), transformedStatesOfInterest.copy(), getTransformedModel(), "mask");
				}
				sv.filter(transformedStatesOfInterest);

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
				return transformedStatesOfInterest.copy();
			}
		};
	}
}
