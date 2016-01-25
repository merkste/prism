package explicit.modelviews.methods;

import common.functions.TripleMapping;
import common.methods.BinaryMethod;
import explicit.NondetModel;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
public class CallNondetModel
{
	private static final NondetModelGetAction GET_ACTION = new NondetModelGetAction();

	public static NondetModelGetAction getAction()
	{
		return GET_ACTION;
	}

	public static final class NondetModelGetAction
			implements TripleMapping<NondetModel, Integer, Integer, Object>,
			BinaryMethod<NondetModel, Integer, Integer, Object>
	{
		@Override
		public Object apply(final NondetModel model, final Integer state, final Integer choice)
		{
			return model.getAction(state, choice);
		}
	}
}