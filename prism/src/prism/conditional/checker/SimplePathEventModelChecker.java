package prism.conditional.checker;

import jdd.JDDNode;
import prism.Model;
import prism.PrismComponent;
import prism.PrismException;
import prism.ProbModel;
import prism.StateModelChecker;
import prism.StateValuesMTBDD;

public interface SimplePathEventModelChecker<M extends ProbModel, C extends StateModelChecker>
{
	public static final JDDNode ALL_STATES = null;

	public C getModelChecker();

	@SuppressWarnings("unchecked")
	default C getModelChecker(M model) throws PrismException
	{
		// Create fresh model checker for model
		return (C) getModelChecker().createModelChecker(model);
	}



	public static abstract class Basic<M extends ProbModel, C extends StateModelChecker> extends PrismComponent implements SimplePathEventModelChecker<M, C>
	{
		protected final C modelChecker;

		public Basic(C modelChecker)
		{
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		@Override
		public C getModelChecker()
		{
			return modelChecker;
		}

		/**
		 * Subtract probabilities from one in-place.
		 * 
		 * [ REFS: result, DEREFS: <i>probabilities</i> ]
		 * 
		 * @param probabilities
		 * @return JDDNode holding the result
		 */
		public static <M extends Model> JDDNode subtractFromOne(M model, JDDNode probabilities)
		{
			StateValuesMTBDD sv = new StateValuesMTBDD(probabilities, model);
			sv.subtractFromOne();
			return sv.getJDDNode();
		}
	}
}
