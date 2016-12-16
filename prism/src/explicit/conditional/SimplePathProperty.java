package explicit.conditional;

import java.util.BitSet;

import common.BitSetTools;
import explicit.Model;
import explicit.conditional.ExpressionInspector;
import parser.ast.Expression;
import parser.ast.ExpressionTemporal;
import prism.PrismLangException;

public abstract class SimplePathProperty implements Cloneable
{
	public static final BitSet ALL_STATES = null;

	protected Model   model;
	protected boolean negated;



	public SimplePathProperty(boolean negated, Model model)
	{
		this.model   = model;
		this.negated = negated;
	}

	public abstract TemporalOperator getOperator();

	@Override
	public SimplePathProperty clone()
	{
		try {
			return (SimplePathProperty) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			throw new RuntimeException("Object#clone is expected to work for Cloneable objects.", e);
		}
	}

	/**
	 * Construct a property which state sets are subsets of a model's reachable state space.
	 * 
	 * @param model
	 * @return a property in {@code model}
	 */
	public SimplePathProperty copy(Model model)
	{
		SimplePathProperty copy = this.clone();
		copy.model              = model;
		return copy;
	}

	public boolean isNegated()
	{
		return negated;
	}

	public static BitSet restrict(BitSet states, Model model)
	{
		if (states == ALL_STATES) {
			return ALL_STATES;
		}
		return states.get(0, model.getNumStates());
	}

//	public static SimplePathProperty fromExpression(Model model, Expression expression) throws PrismException
//	{
//		ExpressionTemporal temporalExpression;
//		try {
//			temporalExpression = Expression.getTemporalOperatorForSimplePathFormula(expression);
//		} catch (PrismLangException e) {
//			throw new IllegalArgumentException(e);
//		}
//		int operator = temporalExpression.getOperator();
//		switch (operator) {
//		case ExpressionTemporal.P_X:
//			return new Next(model, expression);
//		case ExpressionTemporal.P_F:
//		case ExpressionTemporal.P_G:
//			return new Finally(model, expression, true);
//		case ExpressionTemporal.P_U:
//		case ExpressionTemporal.P_R:
//		case ExpressionTemporal.P_W:
//			return new Until(model, expression, true);
//		default:
//			throw new IllegalArgumentException("Unsupported temporal operator: " + operator);
//		}
//	}

	public static void requireUnboundedSimplePathFormula(Expression expression)
	{
		if (! isUnboundedSimplePathFormula(expression)) {
			throw new IllegalArgumentException("Expression is not an unbounded simple path formula: " + expression);
		}
	}


	public static boolean isUnboundedSimplePathFormula(Expression expression)
	{
		try {
			if (! expression.isSimplePathFormula()) {
				return false;
			}
			ExpressionTemporal temporalExpression = Expression.getTemporalOperatorForSimplePathFormula(expression);
			if (temporalExpression.hasBounds()) {
				return false;
			}
		} catch (PrismLangException e) {
			throw new RuntimeException(e);
		}
		return true;
	}



	public static class Next extends SimplePathProperty
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Next;

		protected BitSet goal;

//		public Next(Model model, Expression expression) throws PrismException 
//		{
//			this(model, expression, false);
//		}
//
//		public Next(Model model, Expression expression, boolean convert) throws PrismException 
//		{
//			Expression canonical    = normalizeUnboundedSimpleNextFormula(expression, convert);
//			ExpressionTemporal next = Expression.getTemporalOperatorForSimplePathFormula(canonical);
//
//			StateModelChecker modelChecker = StateModelChecker.createModelChecker(model.getModelType());
//			StateValues goalSV   = modelChecker.checkExpression(model, next.getOperand2(), ALL_STATES);
//			assert (goalSV.getType() instanceof TypeBool);
//			negated = Expression.isNot(canonical);
//			goal    = goalSV.getBitSet();
//		}

		public Next(BitSet goal, Model model)
		{
			this(false, goal, model);
		}

		public Next(boolean negated, BitSet goal, Model model)
		{
			super(negated, model);
			this.goal = restrict(goal, model);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		public BitSet getGoal()
		{
			return goal;
		}

		@Override
		public Next clone()
		{
			return (Next) super.clone();
		}

		@Override
		public Next copy(Model model)
		{
			Next copy = (Next) super.copy(model);
			copy.goal = restrict(goal, model);
			return copy;
		}

		@Override
		public String toString()
		{
			String goalString     = goal == ALL_STATES ? "true" : "goal";
			String temporalString = getOperator() + " " + goalString;
			return negated ? "! " + temporalString : temporalString;
		}

		public static Expression normalizeUnboundedSimpleNextFormula(Expression expression, boolean convert)
		{
			requireUnboundedSimplePathFormula(expression);

			try {
				Expression canonical;
				if (convert) {
					canonical = Expression.convertSimplePathFormulaToCanonicalForm(expression);
				} else {
					canonical = ExpressionInspector.trimUnaryOperations(expression);
				}
				ExpressionTemporal temporal = Expression.getTemporalOperatorForSimplePathFormula(canonical);
				if (temporal.getOperator() == ExpressionTemporal.P_X) {
					return canonical;
				}
			} catch (PrismLangException e) {
				// throw IllegalArgumentException below
			}
			throw new IllegalArgumentException("Expression cannot be converted to next form: " + expression);
		}
	}

	public static abstract class Reach extends SimplePathProperty
	{
		public Reach(boolean negated, Model model) {
			super(negated, model);
		}

		public abstract Until asUntil();
	}

	public static class Finally extends Reach
		{
			public static final TemporalOperator OPERATOR = TemporalOperator.Finally;
	
			protected BitSet goal;
	
	//		public Finally(Model model, Expression expression) throws PrismException
	//		{
	//			this(model, expression, false);
	//		}
	
	//		public Finally(Model model, Expression expression, boolean convert) throws PrismException
	//		{
	//			super(model, normalizeUnboundedSimpleFinallyFormula(expression, convert), true);
	//		}
	
			public Finally(BitSet goal, Model model)
			{
				this(false, goal, model);
			}
	
			public Finally(boolean negated, BitSet goal, Model model)
			{
				super(negated, model);
				this.goal = restrict(goal, model);
			}
	
			@Override
			public TemporalOperator getOperator()
			{
				return OPERATOR;
			}
	
			public BitSet getGoal()
			{
				return goal;
			}
	
			@Override
			public Finally clone()
			{
				return (Finally) super.clone();
			}
	
			@Override
			public Finally copy(Model model)
			{
				Finally copy = (Finally) super.copy(model);
				copy.goal    = restrict(goal, model);
				return copy;
			}
	
			@Override
			public String toString()
			{
				String goalString     = goal   == ALL_STATES ? "true" : "goal";
				String temporalString = getOperator() + " " + goalString;
				return negated ? "! " + temporalString : temporalString;
			}

			@Override
			public Until asUntil()
			{
				return new Until(negated, ALL_STATES, goal, model);
			}
	
			public static Expression normalizeUnboundedSimpleFinallyFormula(Expression expression, boolean convert)
			{
				requireUnboundedSimplePathFormula(expression);
	
				try {
					ExpressionTemporal temporal = Expression.getTemporalOperatorForSimplePathFormula(expression);
					int operator                = temporal.getOperator();
					if (operator == ExpressionTemporal.P_F) {
						return ExpressionInspector.trimUnaryOperations(expression);
					}
					if (convert) {
						// convert G, U, R, W
						Expression canonical = Expression.convertSimplePathFormulaToCanonicalForm(expression);
						temporal             = Expression.getTemporalOperatorForSimplePathFormula(canonical);
						operator             = temporal.getOperator();
						if (operator == ExpressionTemporal.P_U && Expression.isTrue(temporal.getOperand1())) {
							ExpressionTemporal eventually = Expression.Finally(temporal.getOperand2());
							return Expression.isNot(canonical) ? Expression.Not(eventually) : eventually;
						}
					}
				} catch (PrismLangException e) {
					// throw IllegalArgumentException below
				}
				throw new IllegalArgumentException("Expression cannot be converted to finally form: " + expression);
			}
		}

	public static class Globally extends Reach
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Globally;
	
		protected BitSet remain;
	
		public Globally(BitSet remain, Model model)
		{
			this(false, remain, model);
		}
	
		public Globally(boolean negated, BitSet remain, Model model)
		{
			super(negated, model);
			this.remain = restrict(remain, model);
		}
	
		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}
	
		public BitSet getRemain()
		{
			return remain;
		}
	
		@Override
		public Globally clone()
		{
			return (Globally) super.clone();
		}
	
		@Override
		public Globally copy(Model model)
		{
			Globally copy  = (Globally) super.copy(model);
			copy.remain = restrict(remain, model);
			return copy;
		}
	
		@Override
		public String toString()
		{
			String goalString     = remain == ALL_STATES ? "true" : "remain";
			String temporalString = getOperator() + " " + goalString;
			return negated ? "! " + temporalString : temporalString;
		}
	
		@Override
		public Until asUntil()
		{
			BitSet untilGoal = BitSetTools.complement(model.getNumStates(), remain);
			return new Until(! negated, ALL_STATES, untilGoal, model);
		}
	}

	public static class Until extends Reach
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Until;

		protected BitSet remain;
		protected BitSet goal;

//		public Until(Model model, Expression expression) throws PrismException 
//		{
//			this(model, expression, false);
//		}

//		public Until(Model model, Expression expression, boolean convert) throws PrismException 
//		{
//			Expression canonical     = normalizeUnboundedSimpleUntilFormula(expression, convert);
//			ExpressionTemporal until = Expression.getTemporalOperatorForSimplePathFormula(canonical);
//
//			StateModelChecker modelChecker = StateModelChecker.createModelChecker(model.getModelType());
//			StateValues remainSV = modelChecker.checkExpression(model, until.getOperand1(), ALL_STATES);
//			StateValues goalSV   = modelChecker.checkExpression(model, until.getOperand2(), ALL_STATES);
//			assert (remainSV.getType() instanceof TypeBool) && (goalSV.getType() instanceof TypeBool);
//			negated = Expression.isNot(canonical);
//			remain  = remainSV.getBitSet();
//			goal    = goalSV.getBitSet();
//		}

		public Until(BitSet remain, BitSet goal, Model model)
		{
			this(false, remain, goal, model);
		}

		public Until(boolean negated, BitSet remain, BitSet goal, Model model)
		{
			super(negated, model);
			this.remain = restrict(remain, model);
			this.goal   = restrict(goal, model);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		public BitSet getRemain()
		{
			return remain;
		}
		
		public BitSet getGoal()
		{
			return goal;
		}

		@Override
		public Until clone()
		{
			return (Until) super.clone();
		}

		@Override
		public Until copy(Model model)
		{
			Until copy  = (Until) super.copy(model);
			copy.remain = restrict(remain, model);
			copy.goal   = restrict(goal, model);
			return copy;
		}

		@Override
		public String toString()
		{
			String remainString   = remain == ALL_STATES ? "true" : "remain";
			String goalString     = goal   == ALL_STATES ? "true" : "goal";
			String temporalString = remainString + " " + getOperator() + " " + goalString;
			return negated ? "!(" + temporalString + ")" : temporalString;
		}

		@Override
		public Until asUntil()
		{
			return this;
		}

		public static Expression normalizeUnboundedSimpleUntilFormula(Expression expression, boolean convert)
		{
			requireUnboundedSimplePathFormula(expression);
			try {
				Expression canonical;
				if (convert) {
					canonical = Expression.convertSimplePathFormulaToCanonicalForm(expression);
				} else {
					canonical = ExpressionInspector.trimUnaryOperations(expression);
				}
				ExpressionTemporal temporal = Expression.getTemporalOperatorForSimplePathFormula(canonical);
				if (temporal.getOperator() == ExpressionTemporal.P_U) {
					return canonical;
				}
			} catch (PrismLangException e) {
				// throw IllegalArgumentException below
			}
			throw new IllegalArgumentException("Expression cannot be converted to until form: " + expression);
		}
	}



	public static class Release extends Reach
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Release;

		protected BitSet stop;
		protected BitSet remain;

		public Release(BitSet stop, BitSet remain, Model model)
		{
			this(false, stop, remain, model);
		}

		public Release(boolean negated, BitSet stop, BitSet remain, Model model)
		{
			super(negated, model);
			this.stop   = restrict(stop, model);
			this.remain = restrict(remain, model);
		}
		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		public BitSet getStop()
		{
			return stop;
		}

		public BitSet getRemain()
		{
			return remain;
		}

		@Override
		public Release clone()
		{
			return (Release) super.clone();
		}

		@Override
		public Release copy(Model model)
		{
			Release copy = (Release) super.copy(model);
			copy.stop    = restrict(stop, model);
			copy.remain  = restrict(remain, model);
			return copy;
		}

		@Override
		public String toString()
		{
			String stopString     = stop   == ALL_STATES ? "true" : "stop";
			String remainString   = remain == ALL_STATES ? "true" : "remain";
			String temporalString = stopString + " " + getOperator() + " " + remainString;
			return negated ? "!(" + temporalString + ")" : temporalString;
		}

		@Override
		public Until asUntil()
		{
			// φ R ψ = ¬(¬φ U ¬ψ)
			BitSet untilRemain = BitSetTools.complement(model.getNumStates(), stop);
			BitSet untilStop   = BitSetTools.complement(model.getNumStates(), remain);
			return new Until(! negated, untilRemain, untilStop, model);
		}
	}

	public static class WeakUntil extends Reach
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.WeakUntil;

		protected BitSet remain;
		protected BitSet goal;

		public WeakUntil(BitSet remain, BitSet goal, Model model)
		{
			this(false, remain, goal, model);
		}

		public WeakUntil(boolean negated, BitSet remain, BitSet goal, Model model)
		{
			super(negated, model);
			this.remain = restrict(remain, model);
			this.goal   = restrict(goal, model);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		public BitSet getRemain()
		{
			return remain;
		}

		public BitSet getGoal()
		{
			return goal;
		}

		@Override
		public WeakUntil clone()
		{
			return (WeakUntil) super.clone();
		}

		@Override
		public WeakUntil copy(Model model)
		{
			WeakUntil copy = (WeakUntil) super.copy(model);
			copy.remain    = restrict(remain, model);
			copy.goal      = restrict(goal, model);
			return copy;
		}

		@Override
		public String toString()
		{
			String remainString   = remain == ALL_STATES ? "true" : "remain";
			String goalString     = goal   == ALL_STATES ? "true" : "goal";
			String temporalString = remainString + " " + getOperator() + " " + goalString;
			return negated ? "!(" + temporalString + ")" : temporalString;
		}

		@Override
		public Until asUntil()
		{
			// φ W ψ ≡ ¬((φ ∧ ¬ψ) U (¬φ ∧ ¬ψ))
			BitSet untilRemain = BitSetTools.minus(remain, goal);
			BitSet untilGoal   = BitSetTools.complement(model.getNumStates(), remain);
			goal.andNot(goal);
			return new Until(! negated, untilRemain, untilGoal, model);
		}
	}



	public static enum TemporalOperator
	{
		Finally {
			public int getConstant()
			{
				return ExpressionTemporal.P_F;
			}
		},
		Globally {
			public int getConstant()
			{
				return ExpressionTemporal.P_G;
			}
		},
		Next {
			public int getConstant() {
				return ExpressionTemporal.P_X;
			}
		},
		Release {
			public int getConstant() {
				return ExpressionTemporal.P_R;
			}
		},
		Until {
			public int getConstant() {
				return ExpressionTemporal.P_U;
			}
		},
		WeakUntil {
			public int getConstant() {
				return ExpressionTemporal.P_W;
			}
		};

		public abstract int getConstant();

		public String toString()
		{
			return ExpressionTemporal.opSymbols[getConstant()];
		}
	}
}
