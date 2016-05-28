package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.Model;
import prism.ModelTransformation;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.conditional.transform.NewMCResetTransformation;
import prism.conditional.transform.NewMDPResetTransformation;

// FIXME ALG: add comment
public interface NewResetConditionalTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	ModelTransformation<M, ? extends M> transformReset(M model, JDDNode unsatisfiedStates, JDDNode resetStates, JDDNode statesOfInterest)
			throws PrismException;



	public static abstract class DTMC extends NewConditionalTransformer.DTMC implements NewResetConditionalTransformer<ProbModel, ProbModelChecker>
	{

		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.DTMC) && (model instanceof explicit.DTMC);
		}

		@Override
		public NewMCResetTransformation transformReset(ProbModel model, JDDNode unsatisfiedStates, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new NewMCResetTransformation(model, JDD.Or(unsatisfiedStates, resetStates), statesOfInterest);
		}
	}



	public static abstract class MDP extends NewConditionalTransformer.MDP implements NewResetConditionalTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.MDP) && (model instanceof explicit.MDP);
		}

		@Override
		public ModelTransformation<NondetModel, ? extends NondetModel> transformReset(NondetModel model, JDDNode unsatisfiedStates, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new NewMDPResetTransformation(model, unsatisfiedStates, resetStates, statesOfInterest);
		}
	}
}
