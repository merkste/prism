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

public class MCScaledTransformation implements ModelTransformation<ProbModel, ProbModel>
{
	private ProbModel originalModel;
	private ProbModel scaledModel;
	private JDDNode validStates;
	private Prism prism;

	boolean debug = false;

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>originProbs, statesOfInterest</i> ]
	 */
	public MCScaledTransformation(Prism prism, final ProbModel originalModel, final JDDNode originProbs, final JDDNode statesOfInterest) throws PrismException
	{
		this(prism, originalModel, originProbs, originProbs.copy(), statesOfInterest);
	}

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>originProbs, targetProbs, statesOfInterest</i> ]
	 */
	public MCScaledTransformation(Prism prism, final ProbModel originalModel, final JDDNode originProbs, final JDDNode targetProbs, final JDDNode statesOfInterest) throws PrismException
	{
		this.originalModel = originalModel;
		this.prism = prism;

		// originProbsInverted := state s -> 1/originProbs
		JDDNode originProbsInverted = JDD.Apply(JDD.DIVIDE, JDD.Constant(1), originProbs.copy());
		if (debug) {
			StateValuesMTBDD.print(prism.getLog(), originProbs.copy(), originalModel, "originProbs");
			StateValuesMTBDD.print(prism.getLog(), originProbsInverted.copy(), originalModel, "originProbsInverted");
		}

		// support := state s -> 1 if originProbs>0, 0 if originProbs=0
		JDDNode support = JDD.GreaterThan(originProbs, 0.0);

		// targetProbsColumn := state s' -> targetProbs
		JDDNode targetProbsColumn = JDD.PermuteVariables(targetProbs, originalModel.getAllDDRowVars(), originalModel.getAllDDColVars()); 

		// P'(s,v) = P(s,v) * P(v, originProb)
		JDDNode newTransTargetScaled = JDD.Apply(JDD.TIMES, originalModel.getTrans().copy(), targetProbsColumn);

		// P''(s,v) = P'(s,v) * (1 / P(s, originProb))
		JDDNode newTransScaled = JDD.Apply(JDD.TIMES, newTransTargetScaled, originProbsInverted);

		// P'''(s,v) = 0 for P(s, originProb) = 0 and P''(s,v) otherwise
		final JDDNode newTrans = JDD.Apply(JDD.TIMES, newTransScaled, support.copy());

		// start'(s) = statesOfInterest(s) && P(s, originProb) > 0
		final JDDNode newStart = JDD.And(statesOfInterest, support);

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
	public ProbModel getOriginalModel()
	{
		return originalModel;
	}

	@Override
	public ProbModel getTransformedModel()
	{
		return scaledModel;
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformed) throws PrismException
	{
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
	public void clear()
	{
		scaledModel.clear();
		JDD.Deref(validStates);
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return validStates.copy();
	}
}
