package explicit.modelviews.methods;

import common.functions.AbstractPairPredicate;
import common.functions.primitive.AbstractPredicateInteger;
import common.functions.primitive.PredicateInteger;
import common.methods.UnaryMethod;
import explicit.Model;

public class CallModel
{
	private static final ModelIsDeadlockState IS_DEADLOCK_STATE = new ModelIsDeadlockState();

	public static ModelIsDeadlockState isDeadlockState()
	{
		return IS_DEADLOCK_STATE;
	}

	public static final class ModelIsDeadlockState extends AbstractPairPredicate<Model, Integer>implements UnaryMethod<Model, Integer, Boolean>
	{
		@Override
		public PredicateInteger on(final Model model)
		{
			return curry(model);
		}

		@Override
		public PredicateInteger curry(final Model model)
		{
			return new AbstractPredicateInteger()
			{
				@Override
				public boolean getBoolean(final int state)
				{
					return model.isDeadlockState(state);
				}
			};
		}

		@Override
		public boolean getBoolean(final Model model, final Integer state)
		{
			return model.isDeadlockState(state);
		}
	};
}