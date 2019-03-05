package prism.conditional.reset;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.ModelTransformation;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.StateValues;



//FIXME ALG: update comment
/**
 * A NondetModelTransformation that adds reset-actions to a given set of states.
 *
 * <br>
 * For each resetState an additional tau action is added, that deterministically
 * leads back to the {@code resetState}.
 */
public class MDPResetTransformation implements ModelTransformation<NondetModel, NondetModel>
{
	public static final boolean ROW    = true;
	public static final boolean COLUMN = false;
	public static final String SINGLE_RESET_TARGET = "expected a single state of interest";

	protected NondetModel originalModel;
	protected NondetModel transformedModel;
	protected MDPResetOperator operator;



	/**
	 * [ REFS: <i>deadlockStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	public MDPResetTransformation(NondetModel model, JDDNode resetNonDet, JDDNode statesOfInterest)
			throws PrismException
	{
		this(model, new MDPResetOperator(model, resetNonDet, statesOfInterest));
	}

	/**
	 * [ REFS: <i>deadlockStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	public MDPResetTransformation(NondetModel model, JDDNode resetDet, JDDNode resetNonDet, JDDNode statesOfInterest)
			throws PrismException
	{
		this(model, new MDPResetOperator(model, resetDet, resetNonDet, statesOfInterest));
	}

	/**
	 * [ REFS: <i>deadlockStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	public MDPResetTransformation(NondetModel model, MDPResetOperator operator)
			throws PrismException
	{
		this.originalModel    = model;
		this.operator         = operator;
		this.transformedModel = originalModel.getTransformed(operator);
	}

	@Override
	public NondetModel getOriginalModel()
	{
		return originalModel;
	}

	@Override
	public NondetModel getTransformedModel()
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



	public static class MDPResetOperator extends NondetModelTransformation
	{
//		/** The log */
//		private PrismLog log;

		/** States where a reset transition replaces all choices */
		private JDDNode resetDet;
		/** States where a reset transition will be added */
		private JDDNode resetNonDet;
		/** The reset target */
		private JDDNode resetTarget;

//		/** The verbosity flag */
//		private boolean verbose = false;

		/** Constructor.
		 *
		 * <br> [ STORES: resetNonDet, resetTarget, derefed on later call to clear() ]
		 * @param model the model to be transformed
		 * @param resetNonDet for these states, an additional tau action to the resetTarget is added
		 * @param resetTarget the target state in the model for the reset action
		 * @param log PrismLog for status / debug output
		 */
//		public MDPResetOperator(NondetModel model,
//		                                      JDDNode resetNonDet,
//		                                      JDDNode resetTarget,
//		                                      PrismLog log) throws PrismException
		public MDPResetOperator(NondetModel model,
		                        JDDNode resetNonDet,
		                        JDDNode resetTarget) throws PrismException
		{
			this(model, JDD.Constant(0), resetNonDet, resetTarget);
		}

		/** Constructor.
		 *
		 * <br> [ STORES: resetNonDet, resetTarget, derefed on later call to clear() ]
		 * @param model the model to be transformed
		 * @param resetNonDet for these states, an additional tau action to the resetTarget is added
		 * @param resetTarget the target state in the model for the reset action
		 * @param log PrismLog for status / debug output
		 */
//		public MDPResetOperator(NondetModel model,
//		                                      JDDNode resetDet,
//		                                      JDDNode resetNonDet,
//		                                      JDDNode resetTarget,
//		                                      PrismLog log) throws PrismException
		public MDPResetOperator(NondetModel model,
		                        JDDNode resetDet,
		                        JDDNode resetNonDet,
		                        JDDNode resetTarget) throws PrismException
		{
			super(model);
			checkResetTarget(model, resetTarget);
			this.resetDet    = resetDet;
			this.resetNonDet = resetNonDet;
			this.resetTarget = resetTarget;
//			this.log = log;
		}

		public static void checkResetTarget(NondetModel model, JDDNode statesOfInterest) throws PrismException
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
			JDD.Deref(resetDet, resetNonDet, resetTarget);
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

		/** Returns the tau action.
		 * <br>[ REFS: <i>result</i> ]
		 */
		public JDDNode tau() {
			// extraActionVar(0) & !originalActionVar(0) & !originalActionVar(1) & ...
			JDDNode result = extraActionVars.getVar(0).copy();
			for (int i = 0; i < originalModel.getAllDDNondetVars().getNumVars(); i++) {
				result = JDD.And(result, JDD.Not(originalModel.getAllDDNondetVars().getVar(i).copy()));
			}
			return result;
		}

		/** Returns mask for a non-tau action.
		 * An action A in the original model becomes action And(A, nonTau()) in the
		 * transformed model.
		 * <br>[ REFS: <i>result</i> ]
		 */
		public JDDNode notTau() {
			return JDD.Not(extraActionVars.getVar(0).copy());
		}

		@Override
		public int getExtraStateVariableCount()
		{
			// we need no extra state variables
			return 0;
		}

		@Override
		public int getExtraActionVariableCount()
		{
			// we need 1 extra action variable for the tau action
			return 1;
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
				          notTau(),
				          originalModel.getTrans().copy());
//			if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(normal_to_normal));

//			if (verbose) sw.start("reset_to_target");
			JDDNode reset_to_target = 
				JDD.Times(JDD.Or(resetDet.copy(), resetNonDet.copy()),
				          tau(),
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
