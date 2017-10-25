package explicit.conditional.prototype.virtual;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import common.BitSetTools;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.MinMax;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.PredecessorRelation;
import explicit.ProbModelChecker;
import explicit.conditional.NewConditionalTransformer;
import explicit.conditional.prototype.ConditionalReachabilitiyTransformation;
import explicit.conditional.transformer.ResetTransformer;
import explicit.conditional.transformer.ResetTransformer.ResetTransformation;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.MDPDroppedAllChoices;
import explicit.modelviews.MDPRestricted;

@Deprecated
public interface ResetConditionalTransformer<M extends Model, MC extends ProbModelChecker> extends NewConditionalTransformer<M, MC>
{
	@Override
	default ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		ConditionalReachabilitiyTransformation<M,M> transformation = transformReachability(model, expression, statesOfInterest);

		// Construct expression F "goal"
		BitSet goalStates              = transformation.getGoalStates();
		String goalString              = transformation.getTransformedModel().addUniqueLabel("goal", goalStates);
		ExpressionTemporal finallyGoal = Expression.Finally(new ExpressionLabel(goalString));
		// Inherit P operator and bounds
		ExpressionProb objective       = (ExpressionProb) expression.getObjective();
		ExpressionProb probFinallyGoal = new ExpressionProb(finallyGoal, MinMax.max(), objective.getRelOp().toString(), objective.getBound());

		// wrap in new transformation
		return new BasicModelExpressionTransformation<>(transformation, expression, probFinallyGoal);
	}

	ConditionalReachabilitiyTransformation<M, M> transformReachability(M model, ExpressionConditional expression, BitSet statesOfInterest)
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

	default BitSet checkSatisfiability(M model, BitSet remain, BitSet goal, boolean negated, BitSet statesOfInterest)
			throws PrismException
	{
		BitSet unsatisfiable = computeUnsatisfiable(model, remain, goal, negated);
		if (!BitSetTools.areDisjoint(unsatisfiable, statesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		return unsatisfiable;
	}

	default BitSet computeUnsatisfiable(M model, BitSet remain, BitSet goal, boolean negated)
			throws PrismException
	{
		return negated ? computeProb1A(model, remain, goal) : computeProb0A(model, remain, goal);
	}

	default BitSet computeStates(M model, Expression expression)
			throws PrismException
	{
		return getModelChecker(model).checkExpression(model, expression, null).getBitSet();
	}

	BitSet computeProb0A(M model, BitSet remain, BitSet goal) throws PrismException;

	BitSet computeProb0E(M model, BitSet remain, BitSet goal) throws PrismException;

	BitSet computeProb1A(M model, BitSet remain, BitSet goal) throws PrismException;

	BitSet computeProb1E(M model, BitSet remain, BitSet goal) throws PrismException;

	ModelTransformation<M, M> deadlockStates(M model, BitSet states, BitSet statesOfInterest);



	@Deprecated
	public static abstract class DTMC extends NewConditionalTransformer.Basic<explicit.DTMC, DTMCModelChecker> implements ResetConditionalTransformer<explicit.DTMC, DTMCModelChecker>, NewConditionalTransformer.DTMC
	{

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
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
				throws PrismException
		{
			DTMCModelChecker     mc = getModelChecker(model);
			PredecessorRelation pre = model.getPredecessorRelation(mc, true);
			return mc.prob0(model, remain, goal, pre);
		}

		@Override
		public BitSet computeProb0E(explicit.DTMC model, BitSet remain, BitSet goal)
				throws PrismException
		{
			return computeProb0A(model, remain, goal);
		}

		@Override
		public BitSet computeProb1A(explicit.DTMC model, BitSet remain, BitSet goal)
				throws PrismException
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob1(model, remain, goal, pre);
		}

		@Override
		public BitSet computeProb1E(explicit.DTMC model, BitSet remain, BitSet goal)
				throws PrismException
		{
			return computeProb1A(model, remain, goal);
		}

		@Override
		public ModelTransformation<explicit.DTMC, explicit.DTMC> deadlockStates(explicit.DTMC model, BitSet states, BitSet statesOfInterest)
		{
			IntFunction<Iterator<Entry<Integer, Double>>> deadlock = state -> states.get(state) ? Collections.emptyIterator() : null;
			return new BasicModelTransformation<>(model, new DTMCAlteredDistributions(model, deadlock), statesOfInterest);
		}
	}



	@Deprecated
	public static abstract class MDP extends NewConditionalTransformer.Basic<explicit.MDP, MDPModelChecker> implements ResetConditionalTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return model instanceof explicit.MDP;
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
				throws PrismException
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob0(model, remain, goal, false, null, pre);
		}

		@Override
		public BitSet computeProb0E(explicit.MDP model, BitSet remain, BitSet goal)
				throws PrismException
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob0(model, remain, goal, true, null, pre);
		}

		@Override
		public BitSet computeProb1A(explicit.MDP model, BitSet remain, BitSet goal)
				throws PrismException
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob1(model, remain, goal, true, null, pre);
		}

		@Override
		public BitSet computeProb1E(explicit.MDP model, BitSet remain, BitSet goal)
				throws PrismException
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob1(model, remain, goal, false, null, pre);
		}

		@Override
		public ModelTransformation<explicit.MDP, explicit.MDP> deadlockStates(explicit.MDP model, BitSet states, BitSet statesOfInterest)
		{
			return new BasicModelTransformation<>(model, new MDPDroppedAllChoices(model, states), statesOfInterest);
		}
	}
}
