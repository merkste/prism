package explicit.modelviews.methods;

import common.functions.AbstractTripleMapping;
import common.methods.BinaryMethod;
import explicit.NondetModel;

public class CallNondetModel
{
	private static final NondetModelGetAction GET_ACTION = new NondetModelGetAction();

	public static NondetModelGetAction getAction()
	{
		return GET_ACTION;
	}

	public static final class NondetModelGetAction
			extends AbstractTripleMapping<NondetModel, Integer, Integer, Object>
			implements BinaryMethod<NondetModel, Integer, Integer, Object>
	{
		@Override
		public Object get(final NondetModel model, final Integer state, final Integer choice)
		{
			return model.getAction(state, choice);
		}
	}
}