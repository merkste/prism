package explicit.conditional;

import java.util.BitSet;

import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.ProbModelChecker;
import explicit.StateModelChecker;
import explicit.conditional.checker.MCModelChecker;
import explicit.conditional.checker.MDPModelChecker;
import explicit.conditional.checker.SimplePathEvent;
import explicit.conditional.checker.SimplePathEvent.Finally;
import explicit.conditional.checker.SimplePathEvent.Globally;
import explicit.conditional.checker.SimplePathEvent.Next;
import explicit.conditional.checker.SimplePathEvent.Release;
import explicit.conditional.checker.SimplePathEvent.TemporalOperator;
import explicit.conditional.checker.SimplePathEvent.Until;
import explicit.conditional.checker.SimplePathEvent.WeakUntil;
import explicit.conditional.transformer.LtlProductTransformer;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismSettings;
import prism.PrismComponent;

public interface ConditionalTransformer<M extends Model, C extends StateModelChecker>
{
	public static final BitSet ALL_STATES = null;
	public static final BitSet NO_STATES  = new BitSet(0);

	default String getName()
	{
		Class<?> type = this.getClass();
		type = type.getEnclosingClass() == null ? type : type.getEnclosingClass();
		return type.getSimpleName();
	}

	/**
	 * Test whether the transformer can handle a model and a conditional expression.
	 * 
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return canHandleModelType(model)
		       && canHandleObjective(model, expression)
		       && canHandleCondition(model, expression);
	}

	boolean canHandleModelType(Model model);

	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		// can handle probabilities only
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	boolean canHandleCondition(Model model,ExpressionConditional expression)
			throws PrismLangException;

	/**
	 * Throw an exception, iff the transformer cannot handle the model and expression.
	 */
	default void checkCanHandle(Model model, ExpressionConditional expression) throws PrismException
	{
		if (! canHandle(model, expression)) {
			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
		}
	}

	ModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException;

	PrismLog getLog();

	C getModelChecker();

	C getModelChecker(M model) throws PrismException;

	LtlProductTransformer<M> getLtlTransformer();

	PrismSettings getSettings();

	default SimplePathEvent<M> computeSimplePathProperty(M model, Expression expression)
			throws PrismException
	{
		Expression trimmed = ExpressionInspector.trimUnaryOperations(expression);

		if (!trimmed.isSimplePathFormula() || Expression.containsTemporalTimeBounds(trimmed) || Expression.containsTemporalRewardBounds(trimmed))
		{
			throw new IllegalArgumentException("expected unbounded simple path formula");
		}

		boolean negated = Expression.isNot(trimmed);
		ExpressionTemporal temporal;
		if (negated) {
			temporal = (ExpressionTemporal) ExpressionInspector.removeNegation(trimmed);
		} else {
			temporal = (ExpressionTemporal) trimmed;
		}

		C mc                      = getModelChecker(model);
		TemporalOperator operator = TemporalOperator.fromConstant(temporal.getOperator());
		BitSet goal, remain, stop;
		switch (operator) {
		case Next:
			goal = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Next<>(model, negated, goal);
		case Finally:
			goal = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Finally<>(model, negated, goal);
		case Globally:
			remain = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Globally<>(model, negated, remain);
		case Until:
			remain = mc.checkExpression(model, temporal.getOperand1(), ALL_STATES).getBitSet();
			goal   = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Until<>(model, negated, remain, goal);
		case Release:
			stop   = mc.checkExpression(model, temporal.getOperand1(), ALL_STATES).getBitSet();
			remain = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Release<>(model, negated, stop, remain);
		case WeakUntil:
			remain = mc.checkExpression(model, temporal.getOperand1(), ALL_STATES).getBitSet();
			goal   = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new WeakUntil<>(model, negated, remain, goal);
		default:
			throw new IllegalArgumentException("unsupported temporal operator arity");
		}
	}



	public static abstract class Basic<M extends Model, C extends ProbModelChecker> extends PrismComponent implements ConditionalTransformer<M, C>
	{
		protected C modelChecker;
		protected LtlProductTransformer<M> ltlTransformer;

		public Basic(C modelChecker)
		{
			super(modelChecker);
			this.modelChecker  = modelChecker;
		}

		@Override
		public C getModelChecker()
		{
			return modelChecker;
		}

		@Override
		public C getModelChecker(M model)
				throws PrismException
		{
			// Create fresh model checker for model
			@SuppressWarnings("unchecked")
			C mc = (C) C.createModelChecker(model.getModelType(), this);
			mc.inheritSettings(getModelChecker());
			return mc;
		}

		@Override
		public LtlProductTransformer<M> getLtlTransformer()
		{
			if (ltlTransformer == null) {
				ltlTransformer = new LtlProductTransformer<M>(modelChecker);
			}
			return ltlTransformer;
		}

		@Override
		public PrismSettings getSettings()
		{
			return settings;
		}
	}



	public interface MC<M extends explicit.DTMC, C extends ProbModelChecker> extends ConditionalTransformer<M,C>
	{
		MCModelChecker<M,C> getMcModelChecker();
	}



	public interface CTMC extends MC<explicit.CTMC, CTMCModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return model instanceof explicit.CTMC;
		}

		@Override
		default MCModelChecker<explicit.CTMC, CTMCModelChecker> getMcModelChecker()
		{
			return new MCModelChecker.CTMC(getModelChecker());
		}
	}



	public interface DTMC extends MC<explicit.DTMC, DTMCModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return model instanceof explicit.DTMC;
		}

		@Override
		default MCModelChecker<explicit.DTMC, DTMCModelChecker> getMcModelChecker()
		{
			return new MCModelChecker.DTMC(getModelChecker());
		}
	}



	public interface MDP extends ConditionalTransformer<explicit.MDP,explicit.MDPModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return model instanceof explicit.MDP;
		}

		default MDPModelChecker getMDPModelChecker()
		{
			return new MDPModelChecker(getModelChecker());
		}
	}
}
