//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package parser.ast;

import parser.type.TypeInt;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * A bound on a temporal operator.
 * <br>
 * There are three types of bounds that can be stored:
 * <ul>
 * <li>TIME_BOUND, e.g., time <= 4 goal</li>
 * <li>STEP_BOUND, e.g., steps <= 4 goal</li>
 * <li>REWARD_BOUND, e.g., reward{"rew"} <= 4 goal</li>
 * </ul>
 * A DEFAULT_BOUND is a bound that does not explicitly specify the
 * type of bound. The type for this has to be derived from the context.
 * <br>
 * For continous-time models, time and step bounds differ. For
 * discrete-time models, they are the same.
 */
public class TemporalOperatorBound extends ASTElement {
	/** The type of the bound */
	public enum BoundType {
		/**
		 * A default bound, i.e., specified without explicitly marking
		 * the type. The type has to be deduced from the context.
		 */
		DEFAULT_BOUND,
		/**
		 * A bound on the time.
		 */
		TIME_BOUND,
		/**
		 * A bound on the number of steps.
		 */
		STEP_BOUND,
		/**
		 * A bound on the accumulated reward.
		 */
		REWARD_BOUND
	};

	/** The expression for the lower bound, {@code null} for "none", i.e., zero */
	protected Expression lBound = null;
	/** The expression for the upper bound, {@code null} for "none", i.e., infinity */
	protected Expression uBound = null;

	/** Strictness of the lower bound comparator: true: &gt;, false: &gt;= */
	protected boolean lBoundStrict = false;
	/** Strictness of the upper bound comparator: true: &lt;, false: &lt;= */
	protected boolean uBoundStrict = false;

	/** Display as =T rather than [T,T] ? */
	protected boolean equals = false;

	/** For reward bounds the reward structure index (String or Expression) */
	protected Object rewardStructureIndex = null;

	/** (Optional) The resolved reward structure (0-based index, as in ModelInfo) */
	protected Integer resolvedRewardStructIndex = null;

	/** The bound type */
	protected BoundType boundType = BoundType.DEFAULT_BOUND;

	/** Is this a default bound? */
	public boolean isDefaultBound() {return boundType == BoundType.DEFAULT_BOUND;}
	/** Is this a time bound? */
	public boolean isTimeBound() {return boundType == BoundType.TIME_BOUND;}
	/** Is this a step bound? */
	public boolean isStepBound() {return boundType == BoundType.STEP_BOUND;}
	/** Is this a reward bound? */
	public boolean isRewardBound() {return boundType == BoundType.REWARD_BOUND;}

	/** Returns true if the bounds are integer valued. Presumes that the types are annotated. */
	public boolean isIntegerValued() {
		if (hasUpperBound() && !(getUpperBound().getType() instanceof TypeInt)) {
			return false;
		}
		if (hasLowerBound() && !(getLowerBound().getType() instanceof TypeInt)) {
			return false;
		}
		return true;
	}

	/** Do we have a lower bound? */
	public boolean hasLowerBound() {
		return lBound != null;
	}

	/** Do we have an upper bound? */
	public boolean hasUpperBound() {
		return uBound != null;
	}

	/** Get the lower bound expression (may be {@code null}) */
	public Expression getLowerBound()
	{
		return lBound;
	}

	/** Is the lower bound strict, i.e., &gt; instead of &gt;= ? */
	public boolean lowerBoundIsStrict()
	{
		return lBoundStrict;
	}

	/** Get the upper bound expression (may be {@code null}) */
	public Expression getUpperBound()
	{
		return uBound;
	}

	/** Is the upper bound strict, i.e., &lt; instead of &lt;= ? */
	public boolean upperBoundIsStrict()
	{
		return uBoundStrict;
	}

	/**
	 * Returns true if this TemporalOperatorBound has the same domain
	 * as {@other}, in the discrete time setting (step = time bound).
	 * <br>
	 * Requires that the reward structure index (for reward bounds)
	 * has already been resolved, i.e., that {@code setRewardStruct()}
	 * has been called.
	 */
	public boolean hasSameDomainDiscreteTime(TemporalOperatorBound other) {
		if (boundType == BoundType.REWARD_BOUND) {
			if (getResolvedRewardStructIndex() == null || other.getResolvedRewardStructIndex() == null) {
				throw new IllegalStateException("Invalid operation: TemporalOperatorBound.hasSameDomainDiscreteTime() requires that reward structures have already been resolved");
			}
			return other.getBoundType() == boundType && getResolvedRewardStructIndex() == other.getResolvedRewardStructIndex();
		} else {
			// STEP = TIME = DEFAULT
			return other.getBoundType() == BoundType.DEFAULT_BOUND ||
			       other.getBoundType() == BoundType.STEP_BOUND ||
			       other.getBoundType() == BoundType.TIME_BOUND;
		}
	}

	/**
	 * Returns true if this TemporalOperatorBound has the same domain
	 * as {@other}, in the continuous time setting (step != time bound).
 	 * <br>
	 * Requires that the reward structure index (for reward bounds)
	 * has already been resolved, i.e., that {@code setRewardStruct()}
	 * has been called.
	 */
	public boolean hasSameDomainContinuousTime(TemporalOperatorBound other) {
		if (boundType == BoundType.REWARD_BOUND) {
			if (getResolvedRewardStructIndex() == null || other.getResolvedRewardStructIndex() == null) {
				throw new IllegalStateException("Invalid operation: TemporalOperatorBound.hasSameDomainContinuousTime() requires that reward structures have already been resolved");
			}
			return other.getBoundType() == boundType && getResolvedRewardStructIndex() == other.getResolvedRewardStructIndex();
		} else if (boundType == BoundType.TIME_BOUND) {
			return other.getBoundType() == BoundType.TIME_BOUND ||
			       other.getBoundType() == BoundType.DEFAULT_BOUND;
		} else {
			return other.getBoundType() ==  BoundType.STEP_BOUND;
		}
	}

	/**
	 * Returns true if lower/upper bound are equal and should be displayed as =T
	 */
	public boolean getEquals()
	{
		return equals;
	}

	/**
	 * Set lower time bound to be of form >= e
	 * ({@code null} denotes no lower bound, i.e. zero)
	 */
	public void setLowerBound(Expression e)
	{
		setLowerBound(e, false);
	}

	/**
	 * Set lower time bound to be of form >= e or > e
	 * ({@code null} denotes no lower bound, i.e. zero)
	 */
	public void setLowerBound(Expression e, boolean strict)
	{
		lBound = e;
		lBoundStrict = strict;
	}

	/**
	 * Set upper time bound to be of form <= e
	 * ({@code null} denotes no upper bound, i.e. infinity)
	 */
	public void setUpperBound(Expression e)
	{
		setUpperBound(e, false);
	}

	/**
	 * Set upper time bound to be of form <= e or < e
	 * ({@code null} denotes no upper bound, i.e. infinity)
	 */
	public void setUpperBound(Expression e, boolean strict)
	{
		uBound = e;
		uBoundStrict = strict;
	}

	/**
	 * Set both lower/upper time bound to e, i.e. "=e".
	 */
	public void setEqualBounds(Expression e)
	{
		lBound = e;
		lBoundStrict = false;
		uBound = e;
		uBoundStrict = false;
		equals = true;
	}

	/** Get the type of this bound */
	public BoundType getBoundType() {
		return boundType;
	}

	/** Set the type of this bound */
	public void setBoundType(BoundType boundType) {
		this.boundType = boundType;
	}

	/** Set the reward structure index (String or Expression) */
	public void setRewardStructureIndex(Object index) {
		rewardStructureIndex = index;
	}

	/** Get the reward structure index (String or Expression) */
	public Object getRewardStructureIndex() {
		return rewardStructureIndex;
	}

	/** Set the resolved RewardStruct */
	public void setResolvedRewardStructIndex(Integer r) {
		resolvedRewardStructIndex = r;
	}

	/** Get the resolved RewardStruct (if available, otherwise {@code null} */
	public Integer getResolvedRewardStructIndex() {
		return resolvedRewardStructIndex;
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}

	@Override
	public boolean isMatchingElement(ASTElement other)
	{
		if (!(other instanceof TemporalOperatorBound))
			return false;

		TemporalOperatorBound otherBound = (TemporalOperatorBound) other;

		if (boundType != otherBound.boundType)
			return false;

		if (lBound!=null && otherBound.lBound!=null && lBoundStrict != otherBound.lBoundStrict)
			return false;
		
		if (uBound!=null && otherBound.uBound!=null && uBoundStrict != otherBound.uBoundStrict)
			return false;

		if ( (rewardStructureIndex!=null) != (otherBound.rewardStructureIndex!=null) )
			return false;

		if (rewardStructureIndex != null && !rewardStructureIndex.equals(otherBound.rewardStructureIndex))
			return false;

		return true;
	}

	@Override
	public String toString() {
		String result ="";
		switch (boundType) {
		case DEFAULT_BOUND:
			result="";
			break;
		case REWARD_BOUND:
			result="reward";
			if (rewardStructureIndex!=null) {
				result+="{";
				if (rewardStructureIndex instanceof String){
					result+="\"" + rewardStructureIndex+ "\"";
				} else {
					result+=rewardStructureIndex;
				}
				result+="}";
			}
			break;
		case STEP_BOUND:
			result="steps";
			break;
		case TIME_BOUND:
			result="time";
			break;
		}

		if (!hasLowerBound()) {
			if (hasUpperBound())
				result += "<" + (upperBoundIsStrict() ? "" : "=") + getUpperBound();
			else
				return "<empty-bound>";
		} else {
			if (!hasUpperBound())
				result += ">" + (lowerBoundIsStrict() ? "" : "=") + getLowerBound();
			else {
				if (getEquals())
					result += "=" + getLowerBound();
				else
					result += "[" + getLowerBound() + "," + getUpperBound() + "]";
			}
		}

		return result;
	}

	@Override
	public TemporalOperatorBound deepCopy() {
		TemporalOperatorBound result = new TemporalOperatorBound();

		if (lBound != null) {
			result.lBound = lBound.deepCopy();
		}
		if (uBound != null) {
			result.uBound = uBound.deepCopy();
		}
		result.lBoundStrict = lBoundStrict;
		result.uBoundStrict = uBoundStrict;
		result.equals = equals;
		result.boundType = boundType;
		result.rewardStructureIndex = rewardStructureIndex;

		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((boundType == null) ? 0 : boundType.hashCode());
		result = prime * result + (equals ? 1231 : 1237);
		result = prime * result + ((lBound == null) ? 0 : lBound.hashCode());
		result = prime * result + (lBoundStrict ? 1231 : 1237);
		result = prime * result + ((rewardStructureIndex == null) ? 0 : rewardStructureIndex.hashCode());
		result = prime * result + ((uBound == null) ? 0 : uBound.hashCode());
		result = prime * result + (uBoundStrict ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TemporalOperatorBound))
			return false;
		TemporalOperatorBound other = (TemporalOperatorBound) obj;
		if (boundType != other.boundType)
			return false;
		if (equals != other.equals)
			return false;
		if (lBound == null) {
			if (other.lBound != null)
				return false;
		} else if (!lBound.equals(other.lBound))
			return false;
		if (lBoundStrict != other.lBoundStrict)
			return false;
		if (rewardStructureIndex == null) {
			if (other.rewardStructureIndex != null)
				return false;
		} else if (!rewardStructureIndex.equals(other.rewardStructureIndex))
			return false;
		if (uBound == null) {
			if (other.uBound != null)
				return false;
		} else if (!uBound.equals(other.uBound))
			return false;
		if (uBoundStrict != other.uBoundStrict)
			return false;
		return true;
	}
}
