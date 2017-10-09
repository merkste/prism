package prism.conditional.transform;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.ModelTransformation;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.ProbModel;
import prism.ProbModelTransformation;
import prism.StateValues;



// FIXME ALG: update comment
/**
 * A NondetModelTransformation that adds reset-actions to a given set of states.
 *
 * <br>
 * For each resetState an additional tau action is added, that deterministically
 * leads back to the {@code resetState}.
 */
public class NewMCResetTransformation<M extends ProbModel> implements ModelTransformation<M, M>
{
	public static final String SINGLE_RESET_TARGET = "expected a single state of interest";

	protected M originalModel;
	protected M transformedModel;
	protected MCResetOperator operator;



	/**
	 * [ REFS: <i>deadlockStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	public NewMCResetTransformation(M model, JDDNode resetDet, JDDNode statesOfInterest)
			throws PrismException
	{
		this(model, new MCResetOperator(model, resetDet, statesOfInterest));
	}

	/**
	 * [ REFS: <i>deadlockStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	@SuppressWarnings("unchecked")
	public NewMCResetTransformation(M model, MCResetOperator operator)
			throws PrismException
	{
		this.originalModel    = model;
		this.operator         = operator;
		this.transformedModel = (M) originalModel.getTransformed(operator);
	}

	@Override
	public M getOriginalModel()
	{
		return originalModel;
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

		StateValues svOriginalModel = svTransformedModel.sumOverDDVars(operator.getExtraRowVars(), originalModel);
		svTransformedModel.clear();

		return svOriginalModel;
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return transformedModel.getStart().copy();
	}



	public static class MCResetOperator extends ProbModelTransformation
	{
//		/** The log */
//		private PrismLog log;

		/** States where a reset transition will be added */
		private JDDNode resetDet;
		/** The reset target */
		private JDDNode resetTarget;

//		/** The verbosity flag */
//		private boolean verbose = false;

		/** Constructor.
		 *
		 * <br> [ STORES: resetDet, resetTarget, derefed on later call to clear() ]
		 * @param model the model to be transformed
		 * @param resetDet for these states, an additional tau action to the resetTarget is added
		 * @param resetTarget the target state in the model for the reset action
		 * @param log PrismLog for status / debug output
		 */
//		public MCResetOperator(ProbModel model,
//		                                      JDDNode resetDet,
//		                                      JDDNode resetTarget,
//		                                      PrismLog log) throws PrismException
		public MCResetOperator(ProbModel model,
		                       JDDNode resetDet,
		                       JDDNode resetTarget) throws PrismException
		{
			super(model);
			checkResetTarget(model, resetTarget);
			this.resetDet = resetDet;
			this.resetTarget = resetTarget;

			//			this.log = log;
		}

		public static void checkResetTarget(ProbModel model, JDDNode statesOfInterest) throws PrismException
		{
			if (!JDD.isSingleton(statesOfInterest, model.getAllDDRowVars())) {
//				JDD.PrintMinterms(prism.getLog(), statesOfInterest.copy());
				throw new PrismNotSupportedException(SINGLE_RESET_TARGET);
			}
		}

//		/** Set the verbosity flag */
//		public void setVerbose(boolean value) {
//			this.verbose = value;
//		}

		@Override
		public void clear()
		{
			// call underlying clear
			super.clear();
			// clear stored JDDNodes
			JDD.Deref(resetDet, resetTarget);
		}

		/** Returns a JDDNode for the reset target (column variables)
		 * <br>[ REFS: <i>result</i> ]
		 */
		public JDDNode resetTarget() throws PrismException {
			// switch from row -> col
			JDDNode resetCol =
					JDD.PermuteVariables(resetTarget.copy(),
					                     originalModel.getAllDDRowVars(),
					                     originalModel.getAllDDColVars());
			return resetCol;
		}

		@Override
		public int getExtraStateVariableCount()
		{
			// we need no extra state variables
			return 0;
		}

		@Override
		public JDDNode getTransformedTrans() throws PrismException
		{
			JDDNode newTrans;
//			StopWatch sw = new StopWatch(log);

//			if (verbose) log.println("Reset transformation:");

//			if (verbose) sw.start("normal_to_normal");
			JDDNode normal_to_normal =
				JDD.Times(JDD.Not(resetDet.copy()),
				          originalModel.getTrans().copy());
//			if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(normal_to_normal));

//			if (verbose) sw.start("reset_to_target");
			JDDNode reset_to_target = 
				JDD.Times(resetDet.copy(),
				          resetTarget());
//			if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(reset_to_target));

			// plug everything transitions together...
//			if (verbose) sw.start("normal_to_normal | reset_to_target");
			newTrans = JDD.Apply(JDD.MAX, normal_to_normal, reset_to_target);
//			if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(newTrans));

			return newTrans;
		}

//		/** Print minterms of the various DDs that are used (for debugging). */
//		public void debugDDs()
//		{
//			JDD.PrintMinterms(log, tau(), "tau()");
//			JDD.PrintMinterms(log, notTau(), "notTau()");
//			JDD.PrintMinterms(log, getTransformedStart(), "getTransformedStart()");
//		}

		@Override
		public JDDNode getTransformedStart()
		{
			return originalModel.getStart().copy();
		}

		public JDDVars getExtraRowVars()
		{
			return extraRowVars;
		}
	}
}
