package prism.conditional.checker;

import java.util.Objects;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.StateValues;
import prism.conditional.checker.SimplePathEvent.Finally;
import prism.conditional.checker.SimplePathEvent.Globally;
import prism.conditional.checker.SimplePathEvent.Next;
import prism.conditional.checker.SimplePathEvent.Reach;
import prism.conditional.checker.SimplePathEvent.Until;

public class MDPModelChecker extends SimplePathEventModelChecker.Basic<NondetModel, NondetModelChecker>
{
	public MDPModelChecker(NondetModelChecker modelChecker)
	{
		super(modelChecker);
	}

	public JDDNode computeProb0A(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
	{
		if (negated) {
			return computeProb1A(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		JDDNode trans01    = model.getTrans01();
		JDDNode reach      = model.getReach();
		JDDVars rowVars    = model.getAllDDRowVars();
		JDDVars colVars    = model.getAllDDColVars();
		JDDVars nondetVars = model.getAllDDNondetVars();
		return PrismMTBDD.Prob0A(trans01, reach, rowVars, colVars, nondetVars, remain, goal);
	}

	public JDDNode computeProb0E(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
	{
		if (negated) {
			return computeProb1E(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		JDDNode trans01    = model.getTrans01();
		JDDNode reach      = model.getReach();
		JDDNode nondetMask = model.getNondetMask();
		JDDVars rowVars    = model.getAllDDRowVars();
		JDDVars colVars    = model.getAllDDColVars();
		JDDVars nondetVars = model.getAllDDNondetVars();
		return PrismMTBDD.Prob0E(trans01, reach, nondetMask, rowVars, colVars, nondetVars, remain, goal);
	}

	public JDDNode computeProb1A(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
	{
		if (negated) {
			return computeProb0A(model, false, remain, goal);
		}
		JDDNode prob0E = computeProb0E(model, false, remain, goal);
		JDDNode prob1A = computeProb1A(model, remain, goal, prob0E);
		JDD.Deref(prob0E);
		return prob1A;
	}

	public JDDNode computeProb1A(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0E)
	{
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		JDDNode trans01    = model.getTrans01();
		JDDNode reach      = model.getReach();
		JDDNode nondetMask = model.getNondetMask();
		JDDVars rowVars    = model.getAllDDRowVars();
		JDDVars colVars    = model.getAllDDColVars();
		JDDVars nondetVars = model.getAllDDNondetVars();
		return PrismMTBDD.Prob1A(trans01, reach, nondetMask, rowVars, colVars, nondetVars, prob0E, goal);
	}

	public JDDNode computeProb1E(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
	{
		if (negated) {
			return computeProb0E(model, false, remain, goal);
		}
		JDDNode prob0A = computeProb0A(model, false, remain, goal);
		JDDNode prob1E = computeProb1E(model, remain, goal, prob0A);
		JDD.Deref(prob0A);
		return prob1E;
	}

	public JDDNode computeProb1E(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0A)
	{
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		JDDNode trans01    = model.getTrans01();
		JDDNode reach      = model.getReach();
		JDDVars rowVars    = model.getAllDDRowVars();
		JDDVars colVars    = model.getAllDDColVars();
		JDDVars nondetVars = model.getAllDDNondetVars();
		return PrismMTBDD.Prob1E(trans01, reach, rowVars, colVars, nondetVars, remain, goal, prob0A);
	}

	public JDDNode computeNextMaxProbs(NondetModel model, boolean negated, JDDNode goal)
			throws PrismException
	{
		if (negated) {
			// Pmax(¬φ) = 1 - Pmin(φ);
			JDDNode probabilities = computeNextMinProbs(model, false, goal);
			return subtractFromOne(model, probabilities);
		}
		Objects.requireNonNull(goal);
		StateValues probabilities = getModelChecker(model).computeNextProbs(model.getTrans(), goal, false);
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}

	public JDDNode computeNextMinProbs(NondetModel model, boolean negated, JDDNode goal)
			throws PrismException
	{
		if (negated) {
			// Pmin(¬φ) = 1 - Pmax(φ);
			JDDNode probabilities = computeNextMaxProbs(model, false, goal);
			return subtractFromOne(model, probabilities);
		}
		Objects.requireNonNull(goal);
		StateValues probabilities = getModelChecker(model).computeNextProbs(model.getTrans(), goal, true);
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}

	public JDDNode computeUntilMaxProbs(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
			throws PrismException
	{
		if (negated) {
			// Pmax(¬φ) = 1 - Pmin(φ);
			JDDNode probabilities = computeUntilMinProbs(model, false, remain, goal);
			return subtractFromOne(model, probabilities);
		}
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		StateValues probabilities = getModelChecker(model).checkProbUntil(remain, goal, false, false);
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}

	public JDDNode computeUntilMinProbs(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
			throws PrismException
	{
		if (negated) {
			// Pmin(¬φ) = 1 - Pmax(φ);
			JDDNode probabilities = computeUntilMaxProbs(model, false, remain, goal);
			return subtractFromOne(model, probabilities);
		}
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		StateValues probabilities = getModelChecker(model).checkProbUntil(remain, goal, false, true);
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}

	public JDDNode computeProb0E(Reach<NondetModel> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb0E((Finally<NondetModel>) reach);
		}
		if (reach instanceof Globally) {
			Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
			JDDNode result                  = computeProb0E(eventually);
			eventually.clear();
			return result;
		}
		if (reach instanceof Until) {
			return computeProb0E((Until<NondetModel>) reach);
		}

		Until<NondetModel> until = reach.asUntil();
		JDDNode result           = computeProb0E(until);
		until.clear();
		return result;
	}

	public JDDNode computeProb0E(Finally<NondetModel> eventually)
			throws PrismException
	{
		return computeProb0E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public JDDNode computeProb0E(Until<NondetModel> until)
			throws PrismException
	{
		return computeProb0E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public JDDNode computeProb0A(Reach<NondetModel> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb0A((Finally<NondetModel>) reach);
		}
		if (reach instanceof Globally) {
			Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
			JDDNode result                  = computeProb0A(eventually);
			eventually.clear();
			return result;
		}
		if (reach instanceof Until) {
			return computeProb0A((Until<NondetModel>) reach);
		}
		Until<NondetModel> until = reach.asUntil();
		JDDNode result           = computeProb0A(until);
		until.clear();
		return result;
	}

	public JDDNode computeProb0A(Finally<NondetModel> eventually)
			throws PrismException
	{
		return computeProb0A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public JDDNode computeProb0A(Until<NondetModel> until)
			throws PrismException
	{
		return computeProb0A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public JDDNode computeProb1E(Reach<NondetModel> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb1E((Finally<NondetModel>) reach);
		}
		if (reach instanceof Globally) {
			Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
			JDDNode result                  = computeProb1E(eventually);
			eventually.clear();
			return result;
		}
		if (reach instanceof Until) {
			return computeProb1E((Until<NondetModel>) reach);
		}
		Until<NondetModel> until = reach.asUntil();
		JDDNode result           = computeProb1E(until);
		until.clear();
		return result;
	}

	public JDDNode computeProb1E(Finally<NondetModel> eventually)
			throws PrismException
	{
		return computeProb1E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public JDDNode computeProb1E(Until<NondetModel> until)
			throws PrismException
	{
		return computeProb1E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public JDDNode computeProb1A(Reach<NondetModel> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb1A((Finally<NondetModel>) reach);
		}
		if (reach instanceof Globally) {
			Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
			JDDNode result                  = computeProb1A(eventually);
			eventually.clear();
			return result;
		}
		if (reach instanceof Until) {
			return computeProb1A((Until<NondetModel>) reach);
		}
		Until<NondetModel> until = reach.asUntil();
		JDDNode result           = computeProb1A(until);
		until.clear();
		return result;
	}

	public JDDNode computeProb1A(Finally<NondetModel> eventually)
			throws PrismException
	{
		return computeProb1A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public JDDNode computeProb1A(Until<NondetModel> until)
			throws PrismException
	{
		return computeProb1A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public JDDNode computeMaxProbs(SimplePathEvent<NondetModel> path)
			throws PrismException
	{
		if (path instanceof Finally) {
			return computeMaxProbs((Finally<NondetModel>) path);
		}
		if (path instanceof Globally) {
			Finally<NondetModel> eventually = ((Globally<NondetModel>)path).asFinally();
			JDDNode result                  = computeMaxProbs(eventually);
			eventually.clear();
			return result;
		}
		if (path instanceof Next) {
			return computeMaxProbs((Next<NondetModel>) path);
		}
		if (path instanceof Until) {
			return computeMaxProbs((Until<NondetModel>) path);
		}
		if (path instanceof Reach) {
			Until<NondetModel> until = ((Reach<NondetModel>)path).asUntil();
			JDDNode result           = computeMaxProbs(until);
			until.clear();
			return result;
		}
		throw new PrismException("Unsupported simple path property " + path);
	}

	public JDDNode computeMaxProbs(Finally<NondetModel> eventually)
			throws PrismException
	{
		return computeUntilMaxProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public JDDNode computeMaxProbs(Next<NondetModel> next)
			throws PrismException
	{
		return computeNextMaxProbs(next.getModel(), next.isNegated(), next.getGoal());
	}

	public JDDNode computeMaxProbs(Until<NondetModel> until)
			throws PrismException
	{
		return computeUntilMaxProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public JDDNode computeMinProbs(SimplePathEvent<NondetModel> path)
			throws PrismException
	{
		if (path instanceof Finally) {
			return computeMinProbs((Finally<NondetModel>) path);
		}
		if (path instanceof Globally) {
			Finally<NondetModel> eventually = ((Globally<NondetModel>)path).asFinally();
			JDDNode result                  = computeMinProbs(eventually);
			eventually.clear();
			return result;
		}
		if (path instanceof Next) {
			return computeMinProbs((Next<NondetModel>) path);
		}
		if (path instanceof Until) {
			return computeMinProbs((Until<NondetModel>) path);
		}
		if (path instanceof Reach) {
			Until<NondetModel> until = ((Reach<NondetModel>)path).asUntil();
			JDDNode result           = computeMinProbs(until);
			until.clear();
			return result;
		}
		throw new PrismException("Unsupported simple path property " + path);
	}

	public JDDNode computeMinProbs(Finally<NondetModel> eventually)
			throws PrismException
	{
		return computeUntilMinProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public JDDNode computeMinProbs(Next<NondetModel> next)
			throws PrismException
	{
		return computeNextMinProbs(next.getModel(), next.isNegated(), next.getGoal());
	}

	public JDDNode computeMinProbs(Until<NondetModel> until)
			throws PrismException
	{
		return computeUntilMinProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}
}
