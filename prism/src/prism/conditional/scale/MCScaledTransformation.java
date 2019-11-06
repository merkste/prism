package prism.conditional.scale;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.ModelTransformation;
import prism.PrismComponent;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelTransformation;
import prism.StateValues;
import prism.StateValuesMTBDD;

//FIXME ALG: add comment
public class MCScaledTransformation<M extends ProbModel> extends PrismComponent implements ModelTransformation<M, M>
{
	private M originalModel;
	private M scaledModel;
	private JDDNode validStates;

	boolean debug = false;

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>originProbs, statesOfInterest</i> ]
	 */
	public MCScaledTransformation(PrismComponent parent, final M originalModel, final JDDNode originProbs, final JDDNode statesOfInterest) throws PrismException
	{
		this(parent, originalModel, originProbs, originProbs.copy(), statesOfInterest);
	}

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>originProbs, targetProbs, statesOfInterest</i> ]
	 */
	@SuppressWarnings("unchecked")
	public MCScaledTransformation(PrismComponent parent, final M originalModel, final JDDNode originProbs, final JDDNode targetProbs, final JDDNode statesOfInterest) throws PrismException
	{
		super(parent);
		this.originalModel = originalModel;

		// originProbsInverted := state s -> 1/originProbs
		JDDNode originProbsInverted = JDD.Apply(JDD.DIVIDE, JDD.Constant(1), originProbs.copy());
		if (debug) {
			StateValuesMTBDD.print(mainLog, originProbs.copy(), originalModel, "originProbs");
			StateValuesMTBDD.print(mainLog, originProbsInverted.copy(), originalModel, "originProbsInverted");
		}

		// support := state s -> 1 if originProbs>0, 0 if originProbs=0
		JDDNode support = JDD.GreaterThan(originProbs, 0.0);

		// targetProbsColumn := state s' -> targetProbs
		JDDNode targetProbsColumn = JDD.PermuteVariables(targetProbs, originalModel.getAllDDRowVars(), originalModel.getAllDDColVars()); 

		// P'(s,v) = P(s,v) * P(v, originProb)
		JDDNode newTransTargetScaled = JDD.Apply(JDD.TIMES, originalModel.getTrans().copy(), targetProbsColumn);

		// P''(s,v) = P'(s,v) * (1 / P(s, originProb))
		JDDNode newTransScaled = JDD.Apply(JDD.TIMES, newTransTargetScaled, originProbsInverted);

		assert exitRatesAreEqual(originalModel.getTrans(), newTransScaled, support, originalModel.getAllDDColVars())
			: "scaling is expected to preserve the exit rate";

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
		scaledModel = (M) originalModel.getTransformed(scalingTransformation);
		validStates = newStart.copy();
		scalingTransformation.clear();
	}

	/**
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public boolean exitRatesAreEqual(JDDNode originalTrans, JDDNode scaledTrans, JDDNode support, JDDVars colVars)
	{
		JDDNode orignalRates = JDD.SumAbstract(JDD.Apply(JDD.TIMES, originalTrans.copy(), support.copy()), colVars);
		JDDNode scaledRates = JDD.SumAbstract(JDD.Apply(JDD.TIMES, scaledTrans.copy(), support.copy()), colVars);
		boolean result = JDD.EqualSupNorm(orignalRates, scaledRates, 1e-6);
		JDD.Deref(orignalRates, scaledRates);
		return result;
	}

	@Override
	public M getOriginalModel()
	{
		return originalModel;
	}

	@Override
	public M getTransformedModel()
	{
		return scaledModel;
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformed) throws PrismException
	{
		if (debug) {
			mainLog.println("svTransformed");
			svTransformed.print(mainLog);
		}

		StateValuesMTBDD sv = svTransformed.convertToStateValuesMTBDD();
		// switch state values to original model (different reachable states)
		StateValuesMTBDD svOriginal = new StateValuesMTBDD(sv.getJDDNode().copy(), originalModel);
		// clear argument StateValues
		sv.clear();
		if (debug) {
			mainLog.println("svOriginal");
			svOriginal.print(mainLog);
		}

		// filter: set all (reachable) states that are not valid to NaN
		svOriginal.filter(validStates, Double.NaN);
		if (debug) {
			StateValuesMTBDD.print(mainLog, validStates, originalModel, "validStates");
			mainLog.println("sv (filtered)");
			svOriginal.print(mainLog);
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
