package prism.conditional;

import java.util.Objects;

import explicit.conditional.ExpressionInspector;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionTemporal;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.checker.SimplePathEvent;
import prism.conditional.checker.SimplePathEvent.Finally;
import prism.conditional.checker.SimplePathEvent.Globally;
import prism.conditional.checker.SimplePathEvent.Next;
import prism.conditional.checker.SimplePathEvent.Reach;
import prism.conditional.checker.SimplePathEvent.Release;
import prism.conditional.checker.SimplePathEvent.TemporalOperator;
import prism.conditional.checker.SimplePathEvent.Until;
import prism.conditional.checker.SimplePathEvent.WeakUntil;
import prism.conditional.transformer.LtlProductTransformer;
import prism.PrismComponent;

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
		protected Prism prism;
		protected C modelChecker;
		protected LtlProductTransformer<M> ltlTransformer;

		public Basic(Prism prism, C modelChecker) {
			super(modelChecker);
			Objects.requireNonNull(prism);
			Objects.requireNonNull(modelChecker);
			this.prism        = prism;
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



	public static abstract class MC<M extends ProbModel, C extends ProbModelChecker> extends Basic<M, C>
	{
		public MC(Prism prism, C modelChecker)
		{
			super(prism, modelChecker);
		}

		public JDDNode computeProb0(M model, boolean negated, JDDNode remain, JDDNode goal)
		{
			if (negated) {
				return computeProb1(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			JDDNode trans01 = model.getTrans01();
			JDDNode reach   = model.getReach();
			JDDVars rowVars = model.getAllDDRowVars();
			JDDVars colVars = model.getAllDDColVars();
			return PrismMTBDD.Prob0(trans01, reach, rowVars, colVars, remain, goal);
		}

		public JDDNode computeProb1(M model, boolean negated, JDDNode remain, JDDNode goal)
		{
			if (negated) {
				return computeProb0(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			JDDNode prob0 = computeProb0(model, false, remain, goal);
			JDDNode prob1 = computeProb1(model, remain, goal, prob0);
			JDD.Deref(prob0);
			return prob1;
		}

		public JDDNode computeProb1(M model, JDDNode remain, JDDNode goal, JDDNode prob0)
		{
			Objects.requireNonNull(goal);
			Objects.requireNonNull(prob0);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			JDDNode trans01 = model.getTrans01();
			JDDNode reach   = model.getReach();
			JDDVars rowVars = model.getAllDDRowVars();
			JDDVars colVars = model.getAllDDColVars();
			return PrismMTBDD.Prob1(trans01, reach, rowVars, colVars, remain, goal, prob0);
		}

		public JDDNode computeNextProbs(M model, boolean negated, JDDNode goal)
				throws PrismException
		{
			Objects.requireNonNull(goal);
			StateValues probabilities = getModelChecker(model).computeNextProbs(model.getTrans(), goal);
			if (negated) {
				probabilities.subtractFromOne();
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

		public JDDNode computeUntilProbs(M model, boolean negated, JDDNode remain, JDDNode goal)
				throws PrismException
		{
			// FIXME ALG: consider precomputation
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			StateValues probabilities = getModelChecker(model).checkProbUntil(remain, goal, false);
			if (negated) {
				probabilities.subtractFromOne();
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

		public JDDNode computeProb0(Reach<M> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeProb0((Finally<M>) reach);
			}
			if (reach instanceof Globally) {
				Finally<M> eventually = ((Globally<M>)reach).asFinally();
				JDDNode result        = computeProb0(eventually);
				eventually.clear();
				return result;
			}
			if (reach instanceof Until) {
				return computeProb0((Until<M>) reach);
			}
			Until<M> until = reach.asUntil();
			JDDNode result = computeProb0(until);
			until.clear();
			return result;
		}

		public JDDNode computeProb0(Finally<M> eventually)
				throws PrismException
		{
			return computeProb0(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeProb0(Until<M> until)
				throws PrismException
		{
			return computeProb0(until.getModel(), until.isNegated(),until.getRemain(), until.getGoal());
		}

		public JDDNode computeProb1(Reach<M> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeProb1((Finally<M>) reach);
			}
			if (reach instanceof Globally) {
				Finally<M> eventually = ((Globally<M>)reach).asFinally();
				JDDNode result                = computeProb1(eventually);
				eventually.clear();
				return result;
			}
			if (reach instanceof Until) {
				return computeProb1((Until<M>) reach);
			}
			Until<M> until = reach.asUntil();
			JDDNode result = computeProb1(until);
			until.clear();
			return result;
		}

		public JDDNode computeProb1(Finally<M> eventually)
				throws PrismException
		{
			return computeProb1(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeProb1(Until<M> until)
				throws PrismException
		{
			return computeProb1(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public JDDNode computeProbs(SimplePathEvent<M> path)
				throws PrismException
		{
			if (path instanceof Finally) {
				return computeProbs((Finally<M>) path);
			}
			if (path instanceof Globally) {
				Finally<M> eventually = ((Globally<M>)path).asFinally();
				JDDNode result        = computeProbs(eventually);
				eventually.clear();
				return result;
			}
			if (path instanceof Next) {
				return computeProbs((Next<M>)path);
			}
			if (path instanceof Until) {
				return computeProbs((Until<M>) path);
			}
			if (path instanceof Reach) {
				Until<M> until = ((Reach<M>)path).asUntil();
				JDDNode result = computeProbs(until);
				until.clear();
				return result;
			}
			throw new PrismException("Unsupported simple path property " + path);
		}

		public JDDNode computeProbs(Finally<M> eventually)
				throws PrismException
		{
			return computeUntilProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeProbs(Next<M> next)
				throws PrismException
		{
			return computeNextProbs(next.getModel(), next.isNegated(), next.getGoal());
		}

		public JDDNode computeProbs(Until<M> until)
				throws PrismException
		{
			return computeUntilProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}
	}



	public interface CTMC extends ConditionalTransformer<StochModel, StochModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.CTMC) && (model instanceof StochModel);
		}
	}



	public interface DTMC extends ConditionalTransformer<ProbModel, ProbModelChecker>
	{
		@Override
		default boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.DTMC) && (model instanceof ProbModel) && !(model instanceof StochModel);
		}
	}



	public static abstract class MDP extends Basic<NondetModel, NondetModelChecker>
	{
		public MDP(Prism prism, NondetModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.MDP) && (model instanceof NondetModel);
		}

		public JDDNode computeProb0A(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
		{
			if (negated) {
				return computeProb1A(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();
			return PrismMTBDD.Prob0A(trans01, reach, rowVars, colVars, nondetVars, remain, goal);
		}

		public JDDNode computeProb0E(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
		{
			if (negated) {
				return computeProb1E(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDNode nondetMask = model.getNondetMask();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();
			return PrismMTBDD.Prob0E(trans01, reach, nondetMask, rowVars, colVars, nondetVars, remain, goal);
		}

		public JDDNode computeProb1A(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
		{
			if (negated) {
				return computeProb0A(model, false, remain, goal);
			}
			JDDNode prob0E = computeProb0E(model, false, remain, goal);
			JDDNode prob1A = computeProb1A(model, remain, goal, prob0E);
			JDD.Deref(prob0E);
			return prob1A;
		}

		public JDDNode computeProb1A(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0E)
		{
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDNode nondetMask = model.getNondetMask();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();
			return PrismMTBDD.Prob1A(trans01, reach, nondetMask, rowVars, colVars, nondetVars, prob0E, goal);
		}

		public JDDNode computeProb1E(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
		{
			if (negated) {
				return computeProb0E(model, false, remain, goal);
			}
			JDDNode prob0A = computeProb0A(model, false, remain, goal);
			JDDNode prob1E = computeProb1E(model, remain, goal, prob0A);
			JDD.Deref(prob0A);
			return prob1E;
		}

		public JDDNode computeProb1E(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0A)
		{
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();
			return PrismMTBDD.Prob1E(trans01, reach, rowVars, colVars, nondetVars, remain, goal, prob0A);
		}

		public JDDNode computeNextMaxProbs(NondetModel model, boolean negated, JDDNode goal)
				throws PrismException
		{
			if (negated) {
				// Pmax(¬φ) = 1 - Pmin(φ);
				JDDNode probabilities = computeNextMinProbs(model, false, goal);
				return subtractFromOne(model, probabilities);
			}
			Objects.requireNonNull(goal);
			StateValues probabilities = getModelChecker(model).computeNextProbs(model.getTrans(), goal, false);
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

		public JDDNode computeNextMinProbs(NondetModel model, boolean negated, JDDNode goal)
				throws PrismException
		{
			if (negated) {
				// Pmin(¬φ) = 1 - Pmax(φ);
				JDDNode probabilities = computeNextMaxProbs(model, false, goal);
				return subtractFromOne(model, probabilities);
			}
			Objects.requireNonNull(goal);
			StateValues probabilities = getModelChecker(model).computeNextProbs(model.getTrans(), goal, true);
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

		public JDDNode computeUntilMaxProbs(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
				throws PrismException
		{
			if (negated) {
				// Pmax(¬φ) = 1 - Pmin(φ);
				JDDNode probabilities = computeUntilMinProbs(model, false, remain, goal);
				return subtractFromOne(model, probabilities);
			}
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			StateValues probabilities = getModelChecker(model).checkProbUntil(remain, goal, false, false);
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

		public JDDNode computeUntilMinProbs(NondetModel model, boolean negated, JDDNode remain, JDDNode goal)
				throws PrismException
		{
			if (negated) {
				// Pmin(¬φ) = 1 - Pmax(φ);
				JDDNode probabilities = computeUntilMaxProbs(model, false, remain, goal);
				return subtractFromOne(model, probabilities);
			}
			Objects.requireNonNull(goal);
			if (remain == ALL_STATES) {
				remain = model.getReach();
			}
			StateValues probabilities = getModelChecker(model).checkProbUntil(remain, goal, false, true);
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

		public JDDNode computeProb0E(Reach<NondetModel> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeProb0E((Finally<NondetModel>) reach);
			}
			if (reach instanceof Globally) {
				Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
				JDDNode result                  = computeProb0E(eventually);
				eventually.clear();
				return result;
			}
			if (reach instanceof Until) {
				return computeProb0E((Until<NondetModel>) reach);
			}

			Until<NondetModel> until = reach.asUntil();
			JDDNode result           = computeProb0E(until);
			until.clear();
			return result;
		}

		public JDDNode computeProb0E(Finally<NondetModel> eventually)
				throws PrismException
		{
			return computeProb0E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeProb0E(Until<NondetModel> until)
				throws PrismException
		{
			return computeProb0E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public JDDNode computeProb0A(Reach<NondetModel> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeProb0A((Finally<NondetModel>) reach);
			}
			if (reach instanceof Globally) {
				Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
				JDDNode result                  = computeProb0A(eventually);
				eventually.clear();
				return result;
			}
			if (reach instanceof Until) {
				return computeProb0A((Until<NondetModel>) reach);
			}
			Until<NondetModel> until = reach.asUntil();
			JDDNode result           = computeProb0A(until);
			until.clear();
			return result;
		}

		public JDDNode computeProb0A(Finally<NondetModel> eventually)
				throws PrismException
		{
			return computeProb0A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeProb0A(Until<NondetModel> until)
				throws PrismException
		{
			return computeProb0A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public JDDNode computeProb1E(Reach<NondetModel> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeProb1E((Finally<NondetModel>) reach);
			}
			if (reach instanceof Globally) {
				Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
				JDDNode result                  = computeProb1E(eventually);
				eventually.clear();
				return result;
			}
			if (reach instanceof Until) {
				return computeProb1E((Until<NondetModel>) reach);
			}
			Until<NondetModel> until = reach.asUntil();
			JDDNode result           = computeProb1E(until);
			until.clear();
			return result;
		}

		public JDDNode computeProb1E(Finally<NondetModel> eventually)
				throws PrismException
		{
			return computeProb1E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeProb1E(Until<NondetModel> until)
				throws PrismException
		{
			return computeProb1E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public JDDNode computeProb1A(Reach<NondetModel> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeProb1A((Finally<NondetModel>) reach);
			}
			if (reach instanceof Globally) {
				Finally<NondetModel> eventually = ((Globally<NondetModel>)reach).asFinally();
				JDDNode result                  = computeProb1A(eventually);
				eventually.clear();
				return result;
			}
			if (reach instanceof Until) {
				return computeProb1A((Until<NondetModel>) reach);
			}
			Until<NondetModel> until = reach.asUntil();
			JDDNode result           = computeProb1A(until);
			until.clear();
			return result;
		}

		public JDDNode computeProb1A(Finally<NondetModel> eventually)
				throws PrismException
		{
			return computeProb1A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeProb1A(Until<NondetModel> until)
				throws PrismException
		{
			return computeProb1A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public JDDNode computeMaxProbs(SimplePathEvent<NondetModel> path)
				throws PrismException
		{
			if (path instanceof Finally) {
				return computeMaxProbs((Finally<NondetModel>) path);
			}
			if (path instanceof Globally) {
				Finally<NondetModel> eventually = ((Globally<NondetModel>)path).asFinally();
				JDDNode result                  = computeMaxProbs(eventually);
				eventually.clear();
				return result;
			}
			if (path instanceof Next) {
				return computeMaxProbs((Next<NondetModel>) path);
			}
			if (path instanceof Until) {
				return computeMaxProbs((Until<NondetModel>) path);
			}
			if (path instanceof Reach) {
				Until<NondetModel> until = ((Reach<NondetModel>)path).asUntil();
				JDDNode result           = computeMaxProbs(until);
				until.clear();
				return result;
			}
			throw new PrismException("Unsupported simple path property " + path);
		}

		public JDDNode computeMaxProbs(Finally<NondetModel> eventually)
				throws PrismException
		{
			return computeUntilMaxProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeMaxProbs(Next<NondetModel> next)
				throws PrismException
		{
			return computeNextMaxProbs(next.getModel(), next.isNegated(), next.getGoal());
		}

		public JDDNode computeMaxProbs(Until<NondetModel> until)
				throws PrismException
		{
			return computeUntilMaxProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public JDDNode computeMinProbs(SimplePathEvent<NondetModel> path)
				throws PrismException
		{
			if (path instanceof Finally) {
				return computeMinProbs((Finally<NondetModel>) path);
			}
			if (path instanceof Globally) {
				Finally<NondetModel> eventually = ((Globally<NondetModel>)path).asFinally();
				JDDNode result                  = computeMinProbs(eventually);
				eventually.clear();
				return result;
			}
			if (path instanceof Next) {
				return computeMinProbs((Next<NondetModel>) path);
			}
			if (path instanceof Until) {
				return computeMinProbs((Until<NondetModel>) path);
			}
			if (path instanceof Reach) {
				Until<NondetModel> until = ((Reach<NondetModel>)path).asUntil();
				JDDNode result           = computeMinProbs(until);
				until.clear();
				return result;
			}
			throw new PrismException("Unsupported simple path property " + path);		}

		public JDDNode computeMinProbs(Finally<NondetModel> eventually)
				throws PrismException
		{
			return computeUntilMinProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public JDDNode computeMinProbs(Next<NondetModel> next)
				throws PrismException
		{
			return computeNextMinProbs(next.getModel(), next.isNegated(), next.getGoal());
		}

		public JDDNode computeMinProbs(Until<NondetModel> until)
				throws PrismException
		{
			return computeUntilMinProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}
	}
}
