package explicit.conditional.transformer;

import java.util.function.IntPredicate;

import explicit.Model;
import explicit.modelviews.DTMCAdditionalStates;
import explicit.modelviews.MDPAdditionalStates;
import prism.PrismComponent;

public interface ResetStateTransformer<M extends Model> extends ResetTransformer<M>
{
	public static final String REDIRECT = "redirect";



	@Override
	default ResetStateTransformation<M> transformModel(M model, IntPredicate states, int target)
	{
		int resetState = model.getNumStates();
		M redirectModel = addResetTransitions(addResetState(model), states, REDIRECT, resetState);
		M resetModel = addResetTransitions(redirectModel, s -> s == resetState, RESET, target);

		return new ResetStateTransformation<>(model, resetModel, resetState, REDIRECT, target, RESET);
	}

	public abstract M addResetState(M model);



	public class DTMC extends ResetTransformer.DTMC implements ResetStateTransformer<explicit.DTMC>
	{
		public DTMC(final PrismComponent parent)
		{
			super(parent);
		}

		@Override
		public DTMCAdditionalStates addResetState(explicit.DTMC model)
		{
			return new DTMCAdditionalStates(model, 1);
		}
	}



	public class MDP extends ResetTransformer.MDP implements ResetStateTransformer<explicit.MDP>
	{
		public MDP(final PrismComponent parent)
		{
			super(parent);
		}

		@Override
		public MDPAdditionalStates addResetState(explicit.MDP model)
		{
			return new MDPAdditionalStates(model, 1);
		}
	}



	public static class ResetStateTransformation<M extends Model> extends ResetTransformation<M>
	{
		private final int resetState;
		private final Object redirectAction;

		public ResetStateTransformation(M originalModel, M transformedModel, int resetState, Object redirectAction, int targetState, Object resetAction)
		{
			super(originalModel, transformedModel, targetState, resetAction);
			this.resetState = resetState;
			this.redirectAction = redirectAction;
		}

		public ResetStateTransformation(ResetStateTransformation<? extends M> transformation)
		{
			super(transformation);
			this.resetState = transformation.resetState;
			this.redirectAction = transformation.redirectAction;
		}

		public int getResetState()
		{
			return resetState;
		}

		public Object getRedirectAction()
		{
			return redirectAction;
		}
	}
}