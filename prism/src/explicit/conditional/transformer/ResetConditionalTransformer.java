package explicit.conditional.transformer;

import java.util.BitSet;

import common.BitSetTools;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.ModelType;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.PredecessorRelation;
import explicit.conditional.transformer.ResetTransformer;
import explicit.conditional.transformer.ResetTransformer.ResetTransformation;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.MDPRestricted;

// FIXME ALG: add comment
public interface ResetConditionalTransformer<M extends Model> extends ConditionalTransformer<M>
{
	@SuppressWarnings("unchecked")
	@Override
	default boolean canHandle(Model model, ExpressionConditional expression) throws PrismLangException
	{
		return canHandleModelType(model)
		       && canHandleObjective((M) model, expression)
		       && canHandleCondition((M) model, expression);
	}

	boolean canHandleModelType(Model model);

	default boolean canHandleObjective(M model, ExpressionConditional expression) throws PrismLangException
	{
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	boolean canHandleCondition(M model, ExpressionConditional expression) throws PrismLangException;

	@Override
	ConditionalReachabilitiyTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException;

	default ModelTransformation<M, ? extends M> transformReset(M model, BitSet resetStates, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) Reset
		ResetTransformer<M> resetTransformer = getResetTransformer();
		ResetTransformation<M> resetTransformation = resetTransformer.transformModel(model, resetStates, statesOfInterest);
		// 2) Reachability
		M resetModel = resetTransformation.getTransformedModel();
		BitSet resetStatesOfInterest = resetTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> restrictTransformation = transformRestrict(resetModel, resetStatesOfInterest);
		// 3) Compose Transformations
		return new ModelTransformationNested<>(resetTransformation, restrictTransformation);
	}

	ResetTransformer<M> getResetTransformer();

	ModelTransformation<M, ? extends M> transformRestrict(M model, BitSet states);

	default void checkSatisfiability(M model, BitSet goalStates, BitSet statesOfInterest) throws UndefinedTransformationException
	{
		BitSet unsatisfiable = computeProb0A(model, goalStates);
		if (!BitSetTools.areDisjoint(unsatisfiable, statesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
	}

	default BitSet computeStates(M model, Expression expression) throws PrismException
	{
		return getModelChecker().checkExpression(model, expression, null).getBitSet();
	}

	BitSet computeProb0A(M model, BitSet goalStates);

	BitSet computeProb0E(M model, BitSet goalStates);



	public static abstract class DTMC extends ConditionalTransformer.Basic<explicit.DTMC, DTMCModelChecker> implements ResetConditionalTransformer<explicit.DTMC>
	{

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.DTMC) && (model instanceof explicit.DTMC);
		}

		@Override
		public ResetTransformer<explicit.DTMC> getResetTransformer()
		{
			return new ResetTransformer.DTMC(modelChecker);
		}

		@Override
		public ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformRestrict(explicit.DTMC model, BitSet states)
		{
			return DTMCRestricted.transform(model, states);
		}

		@Override
		public BitSet computeProb0A(explicit.DTMC model, BitSet goalStates)
		{
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob0(model, null, goalStates, pre);
		}

		@Override
		public BitSet computeProb0E(explicit.DTMC model, BitSet goalStates)
		{
			return computeProb0A(model, goalStates);
		}
	}



	public static abstract class MDP extends ConditionalTransformer.Basic<explicit.MDP, MDPModelChecker> implements ResetConditionalTransformer<explicit.MDP>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.MDP) && (model instanceof explicit.MDP);
		}

		@Override
		public ResetTransformer<explicit.MDP> getResetTransformer()
		{
			return new ResetTransformer.MDP(modelChecker);
		}

		@Override
		public ModelTransformation<explicit.MDP, ? extends explicit.MDP> transformRestrict(explicit.MDP model, BitSet states)
		{
			return MDPRestricted.transform(model, states);
		}


		@Override
		public BitSet computeProb0A(explicit.MDP model, BitSet goalStates)
		{
			return modelChecker.prob0(model, null, goalStates, false, null);
		}

		@Override
		public BitSet computeProb0E(explicit.MDP model, BitSet goalStates)
		{
			return modelChecker.prob0(model, null, goalStates, true, null);
		}
	}
}