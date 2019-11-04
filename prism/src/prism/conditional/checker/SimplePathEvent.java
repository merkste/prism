package prism.conditional.checker;

import java.util.Objects;

import jdd.Clearable;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.ExpressionTemporal;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;



public abstract class SimplePathEvent<M extends Model> implements Clearable, Cloneable
{
	protected M       model;
	protected boolean negated;



	public SimplePathEvent(M model, boolean negated)
	{
		Objects.requireNonNull(model);
		this.model     = model;
		this.negated   = negated;
	}

	/**
	 * Deref all referenced state sets (JDD nodes). Do not clear the model.
	 */
	@Override
	public abstract void clear();

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

	public abstract SimplePathEvent<M> lift(LTLProduct<M> product);

	/**
	 * Construct a property which state sets are subsets of a model's reachable state space.
	 * 
	 * @param model
	 * @return a property in {@code model}
	 */
	public abstract <OM extends Model> SimplePathEvent<OM> copy(OM model);

	@SuppressWarnings("unchecked")
	@Override
	public SimplePathEvent<M> clone()
	{
		try {
			return (SimplePathEvent<M>) super.clone();
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
		if (!(obj instanceof SimplePathEvent)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		SimplePathEvent other = (SimplePathEvent) obj;
		return model == other.model && negated == other.negated;
	}

	/**
	 * Throw an exception, iff the paths are defined for different models.
	 */
	public void requireSameModel(SimplePathEvent<?> path)
	{
		requireSameModel(path.getModel());
	}

	/**
	 * Throw an exception, iff the receiver's model is defined from the argument.
	 */
	public void requireSameModel(Model model)
	{
		if (this.model != model) {
			throw new IllegalArgumentException("Both models must be the same");
		}
	}

	/**
	 * [ REFS: <i>result</i>, DEREFS: none ]
	 */
	protected JDDNode allStates()
	{
		return model.getReach().copy();
	}

	protected boolean isAllStates(JDDNode states)
	{
		return model.getReach().equals(states);
	}

	/**
	 * [ REFS: result, DEREFS: <i>states</i> ]
	 */
	protected JDDNode restrict(JDDNode states)
	{
		Objects.requireNonNull(states);
		return JDD.And(states, allStates());
	}



	public static class Next<M extends Model> extends SimplePathEvent<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Next;

		protected JDDNode goal;

		/**
		 * [ REFS: none, DEREFS: <i>goal</i> ]
		 */
		public Next(M model, JDDNode goal)
		{
			this(model, false, goal);
		}

		/**
		 * [ REFS: none, DEREFS: <i>goal</i> ]
		 */
		public Next(M model, boolean negated, JDDNode goal)
		{
			super(model, negated);
			this.goal = restrict(goal);
		}

		@Override
		public void clear()
		{
			JDD.Deref(goal);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getGoal()
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
			return new Next<>(product.getProductModel(), negated, goal.copy());
		}

		@Override
		public <OM extends Model> Next<OM> copy(OM model)
		{
			return new Next<>(model, negated, goal.copy());
		}

		@Override
		public Next<M> clone()
		{
			Next<M> clone = (Next<M>) super.clone();
			clone.goal    = goal.copy();
			return clone;
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



	public static abstract class Reach<M extends Model> extends SimplePathEvent<M>
	{
		public Reach(M model, boolean negated)
		{
			super(model, negated);
		}

		public abstract boolean hasToRemain();

		/**
		 * Convert reach property to until property.
		 *
		 * @return a new until instance that is equivalent to the receiver
		 */
		public abstract Until<M> asUntil();

		@Override
		public abstract Reach<M> lift(LTLProduct<M> product);

		@Override
		public abstract <OM extends Model> Reach<OM> copy(OM model);

		@Override
		public Reach<M> clone()
		{
			return (Reach<M>) super.clone();
		};
	}



	public static class Finally<M extends Model> extends Reach<M>
	{
		public static final TemporalOperator OPERATOR = TemporalOperator.Finally;

		protected JDDNode goal;

		/**
		 * [ REFS: none, DEREFS: <i>goal</i> ]
		 */
		public Finally(M model, JDDNode goal)
		{
			this(model, false, goal);
		}

		/**
		 * [ REFS: none, DEREFS: <i>goal</i> ]
		 */
		public Finally(M model, boolean negated, JDDNode goal)
		{
			super(model, negated);
			this.goal = restrict(goal);
		}

		@Override
		public void clear()
		{
			JDD.Deref(goal);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getGoal()
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

		/**
		 * Convert property to finally property.
		 *
		 * @return a new finally instance that is equivalent to the receiver
		 */
		public Finally<M> asFinally()
		{
			return this.clone();
		}

		@Override
		public Until<M> asUntil()
		{
			return new Until<>(model, negated, allStates(), goal.copy());
		}

		@Override
		public Finally<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Finally<>(product.getProductModel(), negated, goal.copy());
		}

		@Override
		public <OM extends Model> Finally<OM> copy(OM model)
		{
			return new Finally<>(model, negated, goal.copy());
		}

		@Override
		public Finally<M> clone()
		{
			Finally<M> clone = (Finally<M>) super.clone();
			clone.goal       = goal.copy();
			return clone;
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

		protected JDDNode remain;

		/**
		 * [ REFS: none, DEREFS: <i>remain</i> ]
		 */
		public Globally(M model, JDDNode remain)
		{
			this(model, false, remain);
		}

		/**
		 * [ REFS: none, DEREFS: <i>remain</i> ]
		 */
		public Globally(M model, boolean negated, JDDNode remain)
		{
			super(model, negated);
			this.remain = restrict(remain);
		}

		@Override
		public void clear()
		{
			JDD.Deref(remain);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getRemain()
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

		/**
		 * Convert property to finally property.
		 *
		 * @return a new finally instance that is equivalent to the receiver
		 */
		public Finally<M> asFinally()
		{
			JDDNode finallyGoal = JDD.And(allStates(), JDD.Not(remain.copy()));
			return new Finally<>(model, ! negated, finallyGoal);
		}

		@Override
		public Until<M> asUntil()
		{
			JDDNode untilGoal = JDD.And(allStates(), JDD.Not(remain.copy()));
			return new Until<>(model, ! negated, allStates(), untilGoal);
		}

		@Override
		public Globally<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Globally<>(product.getProductModel(), negated, remain.copy());
		}

		@Override
		public <OM extends Model> Globally<OM> copy(OM model)
		{
			return new Globally<>(model, negated, remain.copy());
		}

		@Override
		public Globally<M> clone()
		{
			Globally<M> clone = (Globally<M>) super.clone();
			clone.remain      = remain.copy();
			return clone;
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

		protected JDDNode remain;
		protected JDDNode goal;

		/**
		 * [ REFS: none, DEREFS: <i>remain, goal</i> ]
		 */
		public Until(M model, JDDNode remain, JDDNode goal)
		{
			this(model, false, remain, goal);
		}

		/**
		 * [ REFS: none, DEREFS: <i>remain, goal</i> ]
		 */
		public Until(M model, boolean negated, JDDNode remain, JDDNode goal)
		{
			super(model, negated);
			this.remain = restrict(remain);
			this.goal   = restrict(goal);
		}

		@Override
		public void clear()
		{
			JDD.Deref(remain, goal);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getRemain()
		{
			return remain;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getGoal()
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
				return ! goal.equals(JDD.ZERO);
			} else {
				// stay in remain
				return ! isAllStates(remain);
			}
		}

		@Override
		public Until<M> asUntil()
		{
			return this.clone();
		}

		@Override
		public Until<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Until<>(product.getProductModel(), negated, remain.copy(), goal.copy());
		}

		@Override
		public <OM extends Model> Until<OM> copy(OM model)
		{
			return new Until<>(model, negated, remain.copy(), goal.copy());
		}

		@Override
		public Until<M> clone()
		{
			Until<M> clone = (Until<M>) super.clone();
			clone.remain   = remain.copy();
			clone.goal     = goal.copy();
			return clone;
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

		protected JDDNode stop;
		protected JDDNode remain;

		/**
		 * [ REFS: none, DEREFS: <i>stop, remain</i> ]
		 */
		public Release(M model, JDDNode stop, JDDNode remain)
		{
			this(model, false, stop, remain);
		}

		/**
		 * [ REFS: none, DEREFS: <i>stop, remain</i> ]
		 */
		public Release(M model, boolean negated, JDDNode stop, JDDNode remain)
		{
			super(model, negated);
			this.stop   = restrict(stop);
			this.remain = restrict(remain);
		}

		@Override
		public void clear()
		{
			JDD.Deref(stop, remain);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getStop()
		{
			return stop;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getRemain()
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
			Until<M> until = asUntil();
			boolean result = until.hasToRemain();
			until.clear();
			return result;
		}

		@Override
		public Until<M> asUntil()
		{
			// φ R ψ = ¬(¬φ U ¬ψ)
			JDDNode untilRemain = JDD.And(allStates(), JDD.Not(stop.copy()));
			JDDNode untilStop   = JDD.And(allStates(), JDD.Not(remain.copy()));
			return new Until<>(model, ! negated, untilRemain, untilStop);
		}

		@Override
		public Release<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new Release<>(product.getProductModel(), negated, stop.copy(), remain.copy());
		}

		@Override
		public <OM extends Model> Release<OM> copy(OM model)
		{
			return new Release<>(model, negated, stop.copy(), remain.copy());
		}

		@Override
		public Release<M> clone()
		{
			Release<M> clone = (Release<M>) super.clone();
			clone.stop       = stop.copy();
			clone.remain     = remain.copy();
			return clone;
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

		protected JDDNode remain;
		protected JDDNode goal;

		/**
		 * [ REFS: none, DEREFS: <i>remain, goal</i> ]
		 */
		public WeakUntil(M model, JDDNode remain, JDDNode goal)
		{
			this(model, false, remain, goal);
		}

		/**
		 * [ REFS: none, DEREFS: <i>remain, goal</i> ]
		 */
		public WeakUntil(M model, boolean negated, JDDNode remain, JDDNode goal)
		{
			super(model, negated);
			this.remain = restrict(remain);
			this.goal   = restrict(goal);
		}

		@Override
		public void clear()
		{
			JDD.Deref(remain, goal);
		}

		@Override
		public TemporalOperator getOperator()
		{
			return OPERATOR;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getRemain()
		{
			return remain;
		}

		/**
		 * [ REFS: none, DEREFS: none ]
		 */
		public JDDNode getGoal()
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
			Until<M> until = asUntil();
			boolean result = until.hasToRemain();
			until.clear();
			return result;
		}

		@Override
		public Until<M> asUntil()
		{
			// φ W ψ ≡ ¬((φ ∧ ¬ψ) U (¬φ ∧ ¬ψ))
			JDDNode untilRemain = JDD.And(remain.copy(), JDD.Not(goal.copy()));
			JDDNode untilGoal   = JDD.And(allStates(), JDD.Not(remain.copy()));
			untilGoal           = JDD.And(untilGoal, JDD.Not(goal.copy()));
			return new Until<>(model, ! negated, untilRemain, untilGoal);
		}

		@Override
		public WeakUntil<M> lift(LTLProduct<M> product)
		{
			requireSameModel(product.getOriginalModel());
			return new WeakUntil<>(product.getProductModel(), negated, remain.copy(), goal.copy());
		}

		@Override
		public <OM extends Model> WeakUntil<OM> copy(OM model)
		{
			return new WeakUntil<>(model, remain.copy(), goal.copy());
		}

		@Override
		public WeakUntil<M> clone()
		{
			WeakUntil<M> clone = (WeakUntil<M>) super.clone();
			clone.remain       = remain.copy();
			clone.goal         = goal.copy();
			return clone;
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
