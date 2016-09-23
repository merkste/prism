package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelTransformation;
import prism.StateValues;
import prism.StateValuesMTBDD;

public class MCScaledTransformation implements ModelTransformation<ProbModel, ProbModel> {
	private ProbModel originalModel;
	private ProbModel scaledModel;
	private JDDNode validStates;
	private Prism prism;

	boolean debug = false;

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>probReachGoal, statesOfInterest</i> ]
	 */
	public MCScaledTransformation(Prism prism, final ProbModel originalModel, final JDDNode probReachGoal, final JDDNode statesOfInterest) throws PrismException
	{
		this.originalModel = originalModel;
		this.prism = prism;

		if (debug) {
			StateValuesMTBDD.print(prism.getLog(), probReachGoal.copy(), originalModel, "probReachGoal");
		}

		// prob1DividedByReachGoal := state s -> 1/probReachGoal
		JDDNode prob1DividedByReachGoal = JDD.Apply(JDD.DIVIDE, JDD.Constant(1), probReachGoal.copy());
		if (debug) {
			StateValuesMTBDD.print(prism.getLog(), prob1DividedByReachGoal.copy(), originalModel, "prob1DividedByReachGoal");
		}

		// reachGoal01 := state s -> 1 if probReachGoal>0, 0 if probReachGoal=0
		JDDNode reachGoal01 = JDD.GreaterThan(probReachGoal.copy(), 0);

		// probReachGoalColumn := state s' -> probReachGoal
		JDDNode probReachGoalColumn = JDD.PermuteVariables(probReachGoal, originalModel.getAllDDRowVars(), originalModel.getAllDDColVars()); 

		// P'(s,v) = P(s,v) * P(v, reachGoal) 
		JDDNode newTransSuccessorScaled = JDD.Apply(JDD.TIMES, originalModel.getTrans().copy(), probReachGoalColumn);

		// P''(s,v) = P'(s,v) * (1 / P(s, reachGoal)) 
		JDDNode newTransScaled = JDD.Apply(JDD.TIMES, newTransSuccessorScaled, prob1DividedByReachGoal);
		
		// P'''(s,v) = 0 for P(s, reachGoal) = 0 and P''(s,v) otherwise
		final JDDNode newTrans = JDD.Apply(JDD.TIMES, newTransScaled, reachGoal01.copy());

		// start'(s) = statesOfInterest(s) && P(s, reachGoal) > 0
		final JDDNode newStart = JDD.And(statesOfInterest, reachGoal01);

		ProbModelTransformation scalingTransformation = 
		new ProbModelTransformation(originalModel)
		{
			@Override
			public void clear()
			{
				super.clear();
				JDD.Deref(newTrans, newStart);
			}

			@Override
			public int getExtraStateVariableCount()
			{
				return 0;
			}

			@Override
			public JDDNode getTransformedTrans()
			{
				return newTrans.copy();
			}

			@Override
			public JDDNode getTransformedStart()
			{
				return newStart.copy();
			}
		};

		// store scale model
		scaledModel = originalModel.getTransformed(scalingTransformation);
		validStates = newStart.copy();
		scalingTransformation.clear();
	}

	@Override
	public ProbModel getOriginalModel() {
		return originalModel;
	}

	@Override
	public ProbModel getTransformedModel() {
		return scaledModel;
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformed) throws PrismException {
		if (debug) {
			prism.getMainLog().println("svTransformed");
			svTransformed.print(prism.getMainLog());
		}

		StateValuesMTBDD sv = svTransformed.convertToStateValuesMTBDD();
		// switch state values to original model (different reachable states)
		StateValuesMTBDD svOriginal = new StateValuesMTBDD(sv.getJDDNode().copy(), originalModel);
		// clear argument StateValues
		sv.clear();
		if (debug) {
			prism.getMainLog().println("svOriginal");
			svOriginal.print(prism.getMainLog());
		}

		// filter: set all (reachable) states that are not valid to NaN
		svOriginal.filter(validStates, Double.NaN);
		if (debug) {
			StateValuesMTBDD.print(prism.getMainLog(), validStates, originalModel, "validStates");
			prism.getMainLog().println("sv (filtered)");
			svOriginal.print(prism.getMainLog());
		}

		return svOriginal;
	}

	@Override
	public void clear() {
		scaledModel.clear();
		JDD.Deref(validStates);
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return validStates.copy();
	}
}
