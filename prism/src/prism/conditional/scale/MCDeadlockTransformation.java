package prism.conditional.scale;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.ModelTransformation;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelTransformation;
import prism.StateValues;

//FIXME ALG: add comment
public class MCDeadlockTransformation<M extends ProbModel> implements ModelTransformation<M, M>
{
	private M originalModel;
	private M transformedModel;
	private DeadlockOperator operator;

	/**
	 * [ REFS: <i>deadlockStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	@SuppressWarnings("unchecked")
	public MCDeadlockTransformation(M model, JDDNode deadlockStates, JDDNode statesOfInterest)
			throws PrismException
	{
		originalModel    = model;
		operator         = new DeadlockOperator(model, deadlockStates, statesOfInterest);
		transformedModel = (M) originalModel.getTransformed(operator);
	}

	/**
	 * [ REFS: <i>afterStates</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getDeadlockStates()
	{
		return operator.deadlockStates.copy();
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

	public class DeadlockOperator extends ProbModelTransformation
	{
		private JDDNode deadlockStates;
		private JDDNode statesOfInterest;
	
		/**
		 * [ REFS: <i>deadlockStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
		 */
		public DeadlockOperator(ProbModel model, JDDNode deadlockStates, JDDNode statesOfInterest)
		{
			super(model);
			this.deadlockStates   = deadlockStates.copy();
			this.statesOfInterest = statesOfInterest.copy();
		}
	
		@Override
		public void clear()
		{
			super.clear();
			JDD.Deref(deadlockStates, statesOfInterest);
		}
	
		@Override
		public int getExtraStateVariableCount()
		{
			return 0;
		}
	
		/** Deadlock states. **/
		@Override
		public JDDNode getTransformedTrans() throws PrismException
		{
			JDDNode transIdentity    = JDD.Identity(originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());
			JDDNode transDeadlock    = JDD.And(deadlockStates.copy(), transIdentity);
			JDDNode transNonDeadlock = JDD.Times(originalModel.getTrans().copy(), JDD.Not(deadlockStates.copy()));
	
			return JDD.Apply(JDD.MAX, transDeadlock, transNonDeadlock);
		}
	
		/** Start in states of interest. **/
		@Override
		public JDDNode getTransformedStart() throws PrismException
		{
			return statesOfInterest.copy();
		}

		public JDDVars getExtraRowVars()
		{
			return extraRowVars;
		}
	}
}
