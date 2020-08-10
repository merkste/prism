package prism.statebased;

import java.util.Objects;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StochModel;
import prism.StochModelChecker;
import prism.statebased.SimplePathEvent.Finally;
import prism.statebased.SimplePathEvent.Globally;
import prism.statebased.SimplePathEvent.Next;
import prism.statebased.SimplePathEvent.Reach;
import prism.statebased.SimplePathEvent.Until;

//FIXME ALG: add comment
public interface MCModelChecker<M extends ProbModel, C extends ProbModelChecker> extends SimplePathEventModelChecker<M, C>
{
	default JDDNode computeProb0(M model, boolean negated, JDDNode remain, JDDNode goal)
	{
		if (negated) {
			return computeProb1(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		JDDNode trans01 = model.getTrans01();
		JDDNode reach   = model.getReach();
		JDDVars rowVars = model.getAllDDRowVars();
		JDDVars colVars = model.getAllDDColVars();
		return PrismMTBDD.Prob0(trans01, reach, rowVars, colVars, remain, goal);
	}

	default JDDNode computeProb1(M model, boolean negated, JDDNode remain, JDDNode goal)
	{
		if (negated) {
			return computeProb0(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		JDDNode prob0 = computeProb0(model, false, remain, goal);
		JDDNode prob1 = computeProb1(model, remain, goal, prob0);
		JDD.Deref(prob0);
		return prob1;
	}

	default JDDNode computeProb1(M model, JDDNode remain, JDDNode goal, JDDNode prob0)
	{
		Objects.requireNonNull(goal);
		Objects.requireNonNull(prob0);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		JDDNode trans01 = model.getTrans01();
		JDDNode reach   = model.getReach();
		JDDVars rowVars = model.getAllDDRowVars();
		JDDVars colVars = model.getAllDDColVars();
		return PrismMTBDD.Prob1(trans01, reach, rowVars, colVars, remain, goal, prob0);
	}

	default JDDNode computeNextProbs(M model, boolean negated, JDDNode goal)
			throws PrismException
	{
		Objects.requireNonNull(goal);
		StateValues probabilities = getModelChecker(model).computeNextProbs(model.getTrans(), goal);
		if (negated) {
			probabilities.subtractFromOne();
		}
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}

	default JDDNode computeUntilProbs(M model, boolean negated, JDDNode remain, JDDNode goal)
			throws PrismException
	{
		// FIXME ALG: consider precomputation
		Objects.requireNonNull(goal);
		if (remain == ALL_STATES) {
			remain = model.getReach();
		}
		StateValues probabilities = getModelChecker(model).checkProbUntil(remain, goal, false);
		if (negated) {
			probabilities.subtractFromOne();
		}
		return probabilities.convertToStateValuesMTBDD().getJDDNode();
	}

	default JDDNode computeProb0(Reach<M> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb0((Finally<M>) reach);
		}
		if (reach instanceof Globally) {
			Finally<M> eventually = ((Globally<M>)reach).asFinally();
			JDDNode result        = computeProb0(eventually);
			eventually.clear();
			return result;
		}
		if (reach instanceof Until) {
			return computeProb0((Until<M>) reach);
		}
		Until<M> until = reach.asUntil();
		JDDNode result = computeProb0(until);
		until.clear();
		return result;
	}

	default JDDNode computeProb0(Finally<M> eventually)
			throws PrismException
	{
		return computeProb0(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	default JDDNode computeProb0(Until<M> until)
			throws PrismException
	{
		return computeProb0(until.getModel(), until.isNegated(),until.getRemain(), until.getGoal());
	}

	default JDDNode computeProb1(Reach<M> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb1((Finally<M>) reach);
		}
		if (reach instanceof Globally) {
			Finally<M> eventually = ((Globally<M>)reach).asFinally();
			JDDNode result                = computeProb1(eventually);
			eventually.clear();
			return result;
		}
		if (reach instanceof Until) {
			return computeProb1((Until<M>) reach);
		}
		Until<M> until = reach.asUntil();
		JDDNode result = computeProb1(until);
		until.clear();
		return result;
	}

	default JDDNode computeProb1(Finally<M> eventually)
			throws PrismException
	{
		return computeProb1(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	default JDDNode computeProb1(Until<M> until)
			throws PrismException
	{
		return computeProb1(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	default JDDNode computeProbs(SimplePathEvent<M> path)
			throws PrismException
	{
		if (path instanceof Finally) {
			return computeProbs((Finally<M>) path);
		}
		if (path instanceof Globally) {
			Finally<M> eventually = ((Globally<M>)path).asFinally();
			JDDNode result        = computeProbs(eventually);
			eventually.clear();
			return result;
		}
		if (path instanceof Next) {
			return computeProbs((Next<M>)path);
		}
		if (path instanceof Until) {
			return computeProbs((Until<M>) path);
		}
		if (path instanceof Reach) {
			Until<M> until = ((Reach<M>)path).asUntil();
			JDDNode result = computeProbs(until);
			until.clear();
			return result;
		}
		throw new PrismException("Unsupported simple path property " + path);
	}

	default JDDNode computeProbs(Finally<M> eventually)
			throws PrismException
	{
		return computeUntilProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	default JDDNode computeProbs(Next<M> next)
			throws PrismException
	{
		return computeNextProbs(next.getModel(), next.isNegated(), next.getGoal());
	}

	default JDDNode computeProbs(Until<M> until)
			throws PrismException
	{
		return computeUntilProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}



	public static class CTMC extends SimplePathEventModelChecker.Basic<StochModel, StochModelChecker> implements MCModelChecker<StochModel, StochModelChecker>
	{
		public CTMC(StochModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static class DTMC extends SimplePathEventModelChecker.Basic<ProbModel, ProbModelChecker> implements MCModelChecker<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}
}
