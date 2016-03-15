package explicit.conditional.transformer;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import common.BitSetTools;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.ModelType;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
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
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.MDPDroppedAllChoices;
import explicit.modelviews.MDPRestricted;

// FIXME ALG: add comment
public interface ResetConditionalTransformer<M extends Model> extends ConditionalTransformer<M>
{
	@SuppressWarnings("unchecked")
	@Override
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return canHandleModelType(model)
		       && canHandleObjective((M) model, expression)
		       && canHandleCondition((M) model, expression);
	}

	boolean canHandleModelType(Model model);

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

	boolean canHandleCondition(M model, ExpressionConditional expression)
			throws PrismLangException;

	@Override
	ConditionalReachabilitiyTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException;

	default ModelTransformation<M, ? extends M> transformReset(M model, BitSet reset, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) Reset
		ResetTransformer<M> resetTransformer = getResetTransformer();
		ResetTransformation<M> resetTransformation = resetTransformer.transformModel(model, reset, statesOfInterest);
		// 2) Reachability
		M resetModel = resetTransformation.getTransformedModel();
		BitSet resetStatesOfInterest = resetTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> restrictTransformation = transformRestrict(resetModel, resetStatesOfInterest);
		// 3) Compose Transformations
		return new ModelTransformationNested<>(resetTransformation, restrictTransformation);
	}

	ResetTransformer<M> getResetTransformer();

	ModelTransformation<M, ? extends M> transformRestrict(M model, BitSet states);

	default BitSet checkSatisfiability(M model, BitSet remain, BitSet goal, BitSet statesOfInterest)
			throws UndefinedTransformationException
	{
		BitSet unsatisfiable = computeProb0A(model, remain, goal);
		if (!BitSetTools.areDisjoint(unsatisfiable, statesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		return unsatisfiable;
	}

	default BitSet computeStates(M model, Expression expression)
			throws PrismException
	{
		return getModelChecker().checkExpression(model, expression, null).getBitSet();
	}

	BitSet computeProb0A(M model, BitSet remain, BitSet goal);

	BitSet computeProb0E(M model, BitSet remain, BitSet goal);

	ModelTransformation<M, M> deadlockStates(M model, BitSet states, BitSet statesOfInterest);



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
		public BitSet computeProb0A(explicit.DTMC model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob0(model, remain, goal, pre);
		}

		@Override
		public BitSet computeProb0E(explicit.DTMC model, BitSet remain, BitSet goal)
		{
			return computeProb0A(model, remain, goal);
		}

		@Override
		public ModelTransformation<explicit.DTMC, explicit.DTMC> deadlockStates(explicit.DTMC model, BitSet states, BitSet statesOfInterest)
		{
			IntFunction<Iterator<Entry<Integer, Double>>> deadlock = state -> states.get(state) ? Collections.emptyIterator() : null;
			return new BasicModelTransformation<>(model, new DTMCAlteredDistributions(model, deadlock), statesOfInterest);
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
		public BitSet computeProb0A(explicit.MDP model, BitSet remain, BitSet goal)
		{
			return modelChecker.prob0(model, remain, goal, false, null);
		}

		@Override
		public BitSet computeProb0E(explicit.MDP model, BitSet remain, BitSet goal)
		{
			return modelChecker.prob0(model, remain, goal, true, null);
		}


		@Override
		public ModelTransformation<explicit.MDP, explicit.MDP> deadlockStates(explicit.MDP model, BitSet states, BitSet statesOfInterest)
		{
			return new BasicModelTransformation<>(model, new MDPDroppedAllChoices(model, states), statesOfInterest);
		}
	}
}
