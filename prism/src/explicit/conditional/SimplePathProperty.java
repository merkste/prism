package explicit.conditional;

import java.util.BitSet;
import java.util.Objects;

import common.BitSetTools;
import explicit.LTLModelChecker.LTLProduct;
import explicit.Model;
import parser.ast.ExpressionTemporal;



public abstract class SimplePathProperty<M extends Model> implements Cloneable
{
	protected M       model;
	protected int     numStates;
	protected boolean negated;



	public SimplePathProperty(M model, boolean negated)
	{
		Objects.requireNonNull(model);
		this.model     = model;
		this.numStates = model.getNumStates();
		this.negated   = negated;
	}

	public abstract TemporalOperator getOperator();

	public M getModel()
	{
		return model;
	}

	public boolean isNegated()
	{
		return negated;
	}

	public abstract boolean isCoSafe();

	public abstract SimplePathProperty<M> lift(LTLProduct<M> product);

	/**
	 * Construct a property which state sets are subsets of a model's reachable state space.
	 * 
	 * @param model
	 * @return a property in {@code model}
	 */
	public abstract <OM extends Model> SimplePathProperty<OM> copy(OM model);

	@SuppressWarnings("unchecked")
	@Override
	public SimplePathProperty<M> clone()
	{
		try {
			return (SimplePathProperty<M>) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			throw new RuntimeException("Object#clone is expected to work for Cloneable objects.", e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		result = prime * result + (negated ? 1231 : 1237);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SimplePathProperty)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		SimplePathProperty other = (SimplePathProperty) obj;
		return model == other.model && negated == other.negated;
	}

	protected BitSet allStates()
	{
		BitSet states = new BitSet(numStates);
		states.set(0, numStates);
		return states;
	}

	protected boolean isAllStates(BitSet states)
	{
		return states.nextClearBit(0) == numStates;
	}

	/**
	 * Throw an exception, iff the paths are defined for different models.
	 */
	protected void requireSameModel(SimplePathProperty<?> path)
	{
		requireSameModel(path.getModel());
	}

	/**
	 * Throw an exception, iff the receiver's model is defined from the argument.
	 */
	protected void requireSameModel(Model model)
	{
		if (this.model != model) {
			throw new IllegalArgumentException("Both models must be the same");
		}
	}

	protected BitSet restrict(BitSet states)
	{
		Objects.requireNonNull(states);
		// states highest index is included in model's state space
		if (states.length() <= numStates) {
			return states;
		}
		// restrict states to model's state space
		return states.get(0, numStates);
	}



	public static class Next<M extends Model> extends SimplePathProperty<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Next;

		protected BitSet goal;

		public Next(M model, BitSet goal)
		{
			this(model, false, goal);
		}

		public Next(M model, boolean negated, BitSet goal)
		{
			super(model, negated);
			this.goal = restrict(goal);
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
		public boolean isCoSafe()
		{
			return true;
		}

		@Override
		public Next<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Next<>(product.getProductModel(), negated, product.liftFromModel(goal));
		}

		@Override
		public <OM extends Model> Next<OM> copy(OM model)
		{
			return new Next<>(model, negated, goal);
		}

		@Override
		public Next<M> clone()
		{
			return (Next<M>) super.clone();
		}

		@Override
		public String toString()
		{
			String goalString     = isAllStates(goal)   ? "true" : "goal";
			String temporalString = getOperator() + " " + goalString;
			return negated ? "! " + temporalString : temporalString;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((goal == null) ? 0 : goal.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof Next)) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			Next other = (Next) obj;
			return goal.equals(other.goal);
		}
	}



	public static abstract class Reach<M extends Model> extends SimplePathProperty<M>
	{
		public Reach(M model, boolean negated) {
			super(model, negated);
		}

		public abstract boolean hasToRemain();

		public abstract Until<M> asUntil();

		@Override
		public abstract Reach<M> lift(LTLProduct<M> product);
	}



	public static class Finally<M extends Model> extends Reach<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Finally;

		protected BitSet goal;

		public Finally(M model, BitSet goal)
		{
			this(model, false, goal);
		}

		public Finally(M model, boolean negated, BitSet goal)
		{
			super(model, negated);
			this.goal = restrict(goal);
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
		public boolean isCoSafe()
		{
			return ! negated;
		}

		@Override
		public boolean hasToRemain()
		{
			return negated;
		}

		public Finally<M> asFinally()
		{
			return this;
		}

		@Override
		public Until<M> asUntil()
		{
			return new Until<>(model, negated, allStates(), goal);
		}

		@Override
		public Finally<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Finally<>(product.getProductModel(), negated, product.liftFromModel(goal));
		}

		@Override
		public <OM extends Model> Finally<OM> copy(OM model)
		{
			return new Finally<>(model, negated, goal);
		}

		@Override
		public Finally<M> clone()
		{
			return (Finally<M>) super.clone();
		}

		@Override
		public String toString()
		{
			String goalString     = isAllStates(goal)   ? "true" : "goal";
			String temporalString = getOperator() + " " + goalString;
			return negated ? "! " + temporalString : temporalString;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((goal == null) ? 0 : goal.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof Finally)) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			Finally other = (Finally) obj;
			return goal.equals(other.goal);
		}
	}



	public static class Globally<M extends Model> extends Reach<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Globally;

		protected BitSet remain;

		public Globally(M model, BitSet remain)
		{
			this(model, false, remain);
		}

		public Globally(M model, boolean negated, BitSet remain)
		{
			super(model, negated);
			this.remain = restrict(remain);
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
		public boolean isCoSafe()
		{
			return negated;
		}

		@Override
		public boolean hasToRemain()
		{
			return ! negated;
		}

		public Finally<M> asFinally()
		{
			BitSet finallyGoal = BitSetTools.complement(numStates, remain);
			return new Finally<>(model, ! negated, finallyGoal);
		}

		@Override
		public Until<M> asUntil()
		{
			BitSet untilGoal = BitSetTools.complement(numStates, remain);
			return new Until<>(model, ! negated, allStates(), untilGoal);
		}

		@Override
		public Globally<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Globally<>(product.getProductModel(), negated, product.liftFromModel(remain));
		}

		@Override
		public <OM extends Model> Globally<OM> copy(OM model)
		{
			return new Globally<>(model, negated, remain);
		}

		@Override
		public Globally<M> clone()
		{
			return (Globally<M>) super.clone();
		}

		@Override
		public String toString()
		{
			String remainString   = isAllStates(remain) ? "true" : "remain";
			String temporalString = getOperator() + " " + remainString;
			return negated ? "! " + temporalString : temporalString;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((remain == null) ? 0 : remain.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof Globally)) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			Globally other = (Globally) obj;
			return remain.equals(other.remain);
		}
	}



	public static class Until<M extends Model> extends Reach<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Until;

		protected BitSet remain;
		protected BitSet goal;

		public Until(M model, BitSet remain, BitSet goal)
		{
			this(model, false, remain, goal);
		}

		public Until(M model, boolean negated, BitSet remain, BitSet goal)
		{
			super(model, negated);
			this.remain = restrict(remain);
			this.goal   = restrict(goal);
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
		public boolean isCoSafe()
		{
			return ! negated;
		}

		@Override
		public boolean hasToRemain()
		{
			if (negated) {
				// stay in S\goal
				return ! goal.isEmpty();
			} else {
				// stay in remain
				return ! isAllStates(remain);
			}
		}

		@Override
		public Until<M> asUntil()
		{
			return this;
		}

		@Override
		public Until<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Until<>(product.getProductModel(), negated, product.liftFromModel(remain), product.liftFromModel(goal));
		}

		@Override
		public <OM extends Model> Until<OM> copy(OM model)
		{
			return new Until<>(model, negated, remain, goal);
		}

		@Override
		public Until<M> clone()
		{
			return (Until<M>) super.clone();
		}

		@Override
		public String toString()
		{
			String remainString   = isAllStates(remain) ? "true" : "remain";
			String goalString     = isAllStates(goal)   ? "true" : "goal";
			String temporalString = remainString + " " + getOperator() + " " + goalString;
			return negated ? "!(" + temporalString + ")" : temporalString;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((goal == null) ? 0 : goal.hashCode());
			result = prime * result + ((remain == null) ? 0 : remain.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof Until)) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			Until other = (Until) obj;
			return remain.equals(other.remain) && goal.equals(other.goal);
		}
	}



	public static class Release<M extends Model> extends Reach<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Release;

		protected BitSet stop;
		protected BitSet remain;

		public Release(M model, BitSet stop, BitSet remain)
		{
			this(model, false, stop, remain);
		}

		public Release(M model, boolean negated, BitSet stop, BitSet remain)
		{
			super(model, negated);
			this.stop   = restrict(stop);
			this.remain = restrict(remain);
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
		public boolean isCoSafe()
		{
			return negated;
		}

		@Override
		public boolean hasToRemain()
		{
			return asUntil().hasToRemain();
		}

		@Override
		public Until<M> asUntil()
		{
			// φ R ψ = ¬(¬φ U ¬ψ)
			BitSet untilRemain = BitSetTools.complement(numStates, stop);
			BitSet untilStop   = BitSetTools.complement(numStates, remain);
			return new Until<>(model, ! negated, untilRemain, untilStop);
		}

		@Override
		public Release<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Release<>(product.getProductModel(), negated, product.liftFromModel(stop), product.liftFromModel(remain));
		}

		@Override
		public <OM extends Model> Release<OM> copy(OM model)
		{
			return new Release<>(model, negated, stop, remain);
		}

		@Override
		public Release<M> clone()
		{
			return (Release<M>) super.clone();
		}

		@Override
		public String toString()
		{
			String stopString     = isAllStates(stop)   ? "true" : "stop";
			String remainString   = isAllStates(remain) ? "true" : "remain";
			String temporalString = stopString + " " + getOperator() + " " + remainString;
			return negated ? "!(" + temporalString + ")" : temporalString;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((remain == null) ? 0 : remain.hashCode());
			result = prime * result + ((stop == null) ? 0 : stop.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof Release)) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			Release other = (Release) obj;
			return stop.equals(other.stop) && remain.equals(other.remain);
		}
	}



	public static class WeakUntil<M extends Model> extends Reach<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.WeakUntil;

		protected BitSet remain;
		protected BitSet goal;

		public WeakUntil(M model, BitSet remain, BitSet goal)
		{
			this(model, false, remain, goal);
		}

		public WeakUntil(M model, boolean negated, BitSet remain, BitSet goal)
		{
			super(model, negated);
			this.remain = restrict(remain);
			this.goal   = restrict(goal);
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
		public boolean isCoSafe()
		{
			return negated;
		}

		@Override
		public boolean hasToRemain()
		{
			return asUntil().hasToRemain();
		}

		@Override
		public Until<M> asUntil()
		{
			// φ W ψ ≡ ¬((φ ∧ ¬ψ) U (¬φ ∧ ¬ψ))
			BitSet untilRemain = BitSetTools.minus(remain, goal);
			BitSet untilGoal   = BitSetTools.complement(numStates, remain);
			untilGoal.andNot(goal);
			return new Until<>(model, ! negated, untilRemain, untilGoal);
		}

		@Override
		public WeakUntil<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new WeakUntil<>(product.getProductModel(), negated, product.liftFromModel(remain), product.liftFromModel(goal));
		}

		@Override
		public <OM extends Model> WeakUntil<OM> copy(OM model)
		{
			return new WeakUntil<>(model, remain, goal);
		}

		@Override
		public WeakUntil<M> clone()
		{
			return (WeakUntil<M>) super.clone();
		}

		@Override
		public String toString()
		{
			String remainString   = isAllStates(remain) ? "true" : "remain";
			String goalString     = isAllStates(goal)   ? "true" : "goal";
			String temporalString = remainString + " " + getOperator() + " " + goalString;
			return negated ? "!(" + temporalString + ")" : temporalString;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((goal == null) ? 0 : goal.hashCode());
			result = prime * result + ((remain == null) ? 0 : remain.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof WeakUntil)) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			WeakUntil other = (WeakUntil) obj;
			return remain.equals(other.remain) && goal.equals(other.goal);
		}
	}



	public static enum TemporalOperator
	{
		Finally(ExpressionTemporal.P_F, 1),
		Globally(ExpressionTemporal.P_G, 1),
		Next(ExpressionTemporal.P_X, 1),
		Release(ExpressionTemporal.P_R, 2),
		Until(ExpressionTemporal.P_U, 2),
		WeakUntil(ExpressionTemporal.P_W, 2);

		protected int constant;
		protected int arity;

		private TemporalOperator(int constant, int arity)
		{
			this.constant = constant;
			this.arity    = arity;
		}

		public int getConstant()
		{
			return constant;
		}

		public int getArity() {
			return arity;
		}

		public String toString()
		{
			return ExpressionTemporal.opSymbols[getConstant()];
		}

		public static TemporalOperator fromConstant(int c)
		{
			for (TemporalOperator op : values()) {
				if (op.getConstant() == c) {
					return op;
				}
			}
			throw new IllegalArgumentException("unknown temporal opertar constant: " + c);
		}
	}
}
