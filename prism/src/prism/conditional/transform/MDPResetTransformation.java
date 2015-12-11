package prism.conditional.transform;

import common.StopWatch;

import jdd.JDD;
import jdd.JDDNode;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.PrismException;
import prism.PrismLog;

/**
 * A NondetModelTransformation that adds reset-actions to a given set of states.
 *
 * <br>
 * For each resetState an additional tau action is added, that deterministically
 * leads back to the {@code resetState}.
 */
public class MDPResetTransformation extends NondetModelTransformation {
	/** The log */
	private PrismLog log;

	/** States where a reset transition will be added */
	private JDDNode resetStates;
	/** The reset target */
	private JDDNode resetTarget;

	/** The verbosity flag */
	private boolean verbose = false;

	/** Constructor.
	 *
	 * <br> [ STORES: resetStates, resetTarget, derefed on later call to clear() ]
	 * @param model the model to be transformed
	 * @param resetStates for these states, an additional tau action to the resetTarget is added
	 * @param resetTarget the target state in the model for the reset action
	 * @param log PrismLog for status / debug output
	 */
	public MDPResetTransformation(NondetModel model,
	                                      JDDNode resetStates,
	                                      JDDNode resetTarget,
	                                      PrismLog log) throws PrismException
	{
		super(model);
		this.resetStates = resetStates;
		this.resetTarget = resetTarget;

		if (!JDD.isSingleton(resetTarget, originalModel.getAllDDRowVars())) {
			throw new PrismException("Reset target has to be a single state!");
		}

		this.log = log;
	}

	/** Set the verbosity flag */
	public void setVerbose(boolean value) {
		this.verbose = value;
	}

	@Override
	public void clear()
	{
		// call underlying clear
		super.clear();
		// clear stored JDDNodes
		JDD.Deref(resetStates);
		JDD.Deref(resetTarget);
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
		StopWatch sw = new StopWatch(log);

		if (verbose) log.println("Reset transformation:");

		if (verbose) sw.start("normal_to_normal");
		JDDNode normal_to_normal =
			JDD.Times(notTau(),
			          originalModel.getTrans().copy());
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(normal_to_normal));

		if (verbose) sw.start("reset_to_target");
		JDDNode reset_to_target = 
			JDD.Times(resetStates.copy(),
			          tau(),
			          resetTarget());
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(reset_to_target));

		// plug everything transitions together...
		if (verbose) sw.start("normal_to_normal | reset_to_target");
		newTrans = JDD.Apply(JDD.MAX, normal_to_normal, reset_to_target);
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(newTrans));

		return newTrans;
	}

	/** Print minterms of the various DDs that are used (for debugging). */
	public void debugDDs()
	{
		JDD.PrintMinterms(log, tau(), "tau()");
		JDD.PrintMinterms(log, notTau(), "notTau()");
		JDD.PrintMinterms(log, getTransformedStart(), "getTransformedStart()");
	}

	@Override
	public JDDNode getTransformedStart()
	{
		return originalModel.getStart().copy();
	}
}
