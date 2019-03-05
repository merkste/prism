package prism.conditional.scale;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.ModelTransformation;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelTransformation;
import prism.StateValues;

public class MCPivotTransformation<M extends ProbModel> implements ModelTransformation<M, M>
{
	/** Flag for "produce BDD for the row variables" */
	private final static boolean ROW = true;
	/** Flag for "produce BDD for the column variables" */
	private final static boolean COL = false;

	private M originalModel;
	private M transformedModel;
	private UnionOperator operator;

	/**
	 * [ REFS: <i>pivotStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	public MCPivotTransformation(M model, JDDNode pivotStates, JDDNode statesOfInterest)
			throws PrismException
	{
		this(model, pivotStates, statesOfInterest, false);
	}
	/**
	 * [ REFS: <i>pivotStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
	 */
	@SuppressWarnings("unchecked")
	public MCPivotTransformation(M model, JDDNode pivotStates, JDDNode statesOfInterest, boolean startInPivotStates)
			throws PrismException
	{
		originalModel    = model;
		operator         = new UnionOperator(model, pivotStates, statesOfInterest);
		transformedModel = (M) originalModel.getTransformed(operator);
	}

	/**
	 * [ REFS: <i>pivotStates</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getPivotStates()
	{
		return operator.pivotStates.copy();
	}

	/**
	 * [ REFS: <i>beforeStates</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getBefore()
	{
		return operator.before(ROW);
	}

	/**
	 * [ REFS: <i>afterStates</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getAfter()
	{
		return operator.after(ROW);
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

	public class UnionOperator extends ProbModelTransformation
	{
		private JDDNode pivotStates;
		private JDDNode statesOfInterest;
		private boolean startInPivotStates;

		/**
		 * [ REFS: <i>pivotStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
		 */
		public UnionOperator(ProbModel model, JDDNode pivotStates, JDDNode statesOfInterest)
		{
			this(model, pivotStates, statesOfInterest, false);
		}
		
		/**
		 * [ REFS: <i>pivotStates, statesOfInterest</i>, DEREFS: <i>none</i> ]
		 */
		public UnionOperator(ProbModel model, JDDNode pivotStates, JDDNode statesOfInterest, boolean startInPivotStates)
		{
			super(model);
			this.pivotStates = pivotStates.copy();
			this.statesOfInterest = statesOfInterest.copy();
			this.startInPivotStates = startInPivotStates;
		}

		@Override
		public void clear()
		{
			super.clear();
			JDD.Deref(pivotStates, statesOfInterest);
		}

		@Override
		public int getExtraStateVariableCount()
		{
			return 1;
		}

		/** Switch from before to after once pivots are reached. **/
		@Override
		public JDDNode getTransformedTrans() throws PrismException
		{
			JDDNode pivotCols = JDD.PermuteVariables(pivotStates.copy(), originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());

			JDDNode transBefore = JDD.Times(before(ROW), before(COL), JDD.Not(pivotCols.copy()));
			JDDNode transPivot  = JDD.Times(before(ROW), after(COL), pivotCols.copy());
			JDDNode transAfter  = JDD.Times(after(ROW), after(COL));
			JDDNode transUnion  = JDD.Or(JDD.Or(transBefore, transPivot), transAfter);

			JDD.Deref(pivotCols);
			return JDD.Times(originalModel.getTrans().copy(), transUnion);
		}

		/**
		 * Depending on the flag {@code startInPivots},<br/>
		 * either start in (before and not pivots) + (after and pivots)<br/>
		 * or start in (before) only.
		 **/
		@Override
		public JDDNode getTransformedStart() throws PrismException
		{
			if (startInPivotStates) {
				JDDNode beforeAndNotPivots = JDD.And(before(ROW), JDD.Not(pivotStates.copy()));
				JDDNode afterAndPivots     = JDD.And(after(ROW), pivotStates.copy());
				return JDD.And(statesOfInterest.copy(), JDD.Or(beforeAndNotPivots, afterAndPivots));
			}
			return JDD.And(statesOfInterest.copy(), before(ROW));
		}

		public JDDNode before(boolean row)
		{
			return JDD.Not(after(row));
		}

		public JDDNode after(boolean row)
		{
			return row ? extraRowVars.getVar(0).copy() : extraColVars.getVar(0).copy();
		}

		public JDDVars getExtraRowVars()
		{
			return extraRowVars;
		}
	}
}