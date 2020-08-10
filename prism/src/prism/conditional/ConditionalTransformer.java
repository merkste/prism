package prism.conditional;

import java.util.Objects;

import explicit.conditional.ExpressionInspector;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionTemporal;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StateValuesMTBDD;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.transformer.LtlProductTransformer;
import prism.statebased.MCModelChecker;
import prism.statebased.MDPModelChecker;
import prism.statebased.SimplePathEvent;
import prism.statebased.SimplePathEvent.Finally;
import prism.statebased.SimplePathEvent.Globally;
import prism.statebased.SimplePathEvent.Next;
import prism.statebased.SimplePathEvent.Release;
import prism.statebased.SimplePathEvent.TemporalOperator;
import prism.statebased.SimplePathEvent.Until;
import prism.statebased.SimplePathEvent.WeakUntil;
import prism.PrismComponent;

//FIXME ALG: add comment
public interface ConditionalTransformer<M extends ProbModel, C extends StateModelChecker>
{
	public static final JDDNode ALL_STATES = null;

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

	boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException;

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

	ModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException;

	PrismLog getLog();

	C getModelChecker();

	C getModelChecker(M model) throws PrismException;

	LtlProductTransformer<M> getLtlTransformer();

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
		JDDNode goal, remain, stop;
		switch (operator) {
		case Next:
			goal = mc.checkExpressionDD(temporal.getOperand2(), allStates(model));
			return new Next<>(model, negated, goal);
		case Finally:
			goal = mc.checkExpressionDD(temporal.getOperand2(), allStates(model));
			return new Finally<>(model, negated, goal);
		case Globally:
			remain = mc.checkExpressionDD(temporal.getOperand2(), allStates(model));
			return new Globally<>(model, negated, remain);
		case Until:
			remain = mc.checkExpressionDD(temporal.getOperand1(), allStates(model));
			goal   = mc.checkExpressionDD(temporal.getOperand2(), allStates(model));
			return new Until<>(model, negated, remain, goal);
		case Release:
			stop   = mc.checkExpressionDD(temporal.getOperand1(), allStates(model));
			remain = mc.checkExpressionDD(temporal.getOperand2(), allStates(model));
			return new Release<>(model, negated, stop, remain);
		case WeakUntil:
			remain = mc.checkExpressionDD(temporal.getOperand1(), allStates(model));
			goal   = mc.checkExpressionDD(temporal.getOperand2(), allStates(model));
			return new WeakUntil<>(model, negated, remain, goal);
		default:
			throw new IllegalArgumentException("unsupported temporal operator arity");
		}
	}

	/**
	 * [ REFS: result, DEREFS: none ]
	 */
	default JDDNode allStates(M model)
	{
		return model.getReach().copy();
	}

	/**
	 * [ REFS: result, DEREFS: none ]
	 */
	default JDDNode noStates()
	{
		return JDD.Constant(0);
	}



	public static abstract class Basic<M extends ProbModel, C extends StateModelChecker> extends PrismComponent implements ConditionalTransformer<M, C>
	{
		protected C modelChecker;
		protected LtlProductTransformer<M> ltlTransformer;

		public Basic(C modelChecker) {
			super(modelChecker);
			Objects.requireNonNull(modelChecker);
			this.modelChecker = modelChecker;
		}

		@Override
		public C getModelChecker()
		{
			return modelChecker;
		}

		@SuppressWarnings("unchecked")
		@Override
		public C getModelChecker(M model)
				throws PrismException
		{
			// Create fresh model checker for model
			return (C) getModelChecker().createModelChecker(model);
		}

		@Override
		public LtlProductTransformer<M> getLtlTransformer()
		{
			if (ltlTransformer == null) {
				ltlTransformer = new LtlProductTransformer<M>(modelChecker);
			}
			return ltlTransformer;
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



	public interface MC<M extends ProbModel, C extends ProbModelChecker> extends ConditionalTransformer<M, C>
	{
		public MCModelChecker<M,C> getMcModelChecker();
	}



	public interface CTMC extends MC<StochModel, StochModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.CTMC) && (model instanceof StochModel);
		}

		@Override
		default MCModelChecker<StochModel, StochModelChecker> getMcModelChecker()
		{
			return new MCModelChecker.CTMC(getModelChecker());
		}
	}



	public interface DTMC extends MC<ProbModel, ProbModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.DTMC) && (model instanceof ProbModel) && !(model instanceof StochModel);
		}

		@Override
		default MCModelChecker<ProbModel, ProbModelChecker> getMcModelChecker()
		{
			return new MCModelChecker.DTMC(getModelChecker());
		}
	}



	public interface MDP extends ConditionalTransformer<NondetModel, NondetModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.MDP) && (model instanceof NondetModel);
		}

		default MDPModelChecker getMDPModelChecker()
		{
			return new MDPModelChecker(getModelChecker());
		}
	}
}
