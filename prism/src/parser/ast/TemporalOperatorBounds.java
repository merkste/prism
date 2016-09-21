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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import parser.visitor.ASTVisitor;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;

/**
 * A conjunction of bounds attached to a temporal operator.
 * <br>
 * If there is only one bound, it can be a DEFAULT_BOUND, where the
 * bound type is context dependent.
 */
public class TemporalOperatorBounds extends ASTElement {
	/** The list (conjunction) of TemporalOperatorBound */
	public List<TemporalOperatorBound> bounds = new ArrayList<TemporalOperatorBound>();

	/** Get the list of bounds */
	public List<TemporalOperatorBound> getBounds()
	{
		return bounds;
	}

	/** Adds the specified bound. Can only be used to add non-default bounds! */
	public void addBound(TemporalOperatorBound bound)
	{
		if (bound.getBoundType() == TemporalOperatorBound.BoundType.DEFAULT_BOUND) {
			throw new IllegalArgumentException("Can not add default bound!");
		}
		bounds.add(bound);
	}

	/**
	 * Sets the default bound as the only bound. It is an error if bound is not a default bound.
	 */
	public void setDefaultBound(TemporalOperatorBound bound) {
		if (bound.getBoundType() != TemporalOperatorBound.BoundType.DEFAULT_BOUND) {
			throw new IllegalArgumentException("Can not set non-default bound as default bound!");
		}

		bounds.clear();
		bounds.add(bound);
	}

	/** Get the number of bounds */
	public int countBounds() {return bounds.size();}

	/** Get the number of bounds of type TIME_BOUND */
	public int countTimeBounds() {return countBoundsOfType(TemporalOperatorBound.BoundType.TIME_BOUND);}

	/** Get the number of bounds of type STEP_BOUND */
	public int countStepBounds() {return countBoundsOfType(TemporalOperatorBound.BoundType.STEP_BOUND);}

	/** Get the number of bounds of type REWARD_BOUND */
	public int countRewardBounds() {return countBoundsOfType(TemporalOperatorBound.BoundType.REWARD_BOUND);}

	/**
	 * Get the number of bounds that resolve to a time bound in a discrete-time model,
	 * i.e., TIME_BOUND, STEP_BOUND and DEFAULT_BOUND
	 */
	public int countTimeBoundsDiscrete() {
		return countBoundsOfType(TemporalOperatorBound.BoundType.TIME_BOUND,
				TemporalOperatorBound.BoundType.STEP_BOUND,
				TemporalOperatorBound.BoundType.DEFAULT_BOUND);
	}

	/**
	 * Get the number of bounds that resolve to a time bound in a continuous-time model,
	 * i.e., TIME_BOUND and DEFAULT_BOUND
	 */
	public int countTimeBoundsContinuous() {
		return countBoundsOfType(TemporalOperatorBound.BoundType.TIME_BOUND,
				TemporalOperatorBound.BoundType.DEFAULT_BOUND);
	}

	/** Is the list of bounds non-empty? */
	public boolean hasBounds() {return !bounds.isEmpty();}

	/** Are there bounds of type TIME_BOUND */
	public boolean hasTimeBounds() {return countTimeBounds() > 0;}

	/** Are there bounds of type STEP_BOUND */
	public boolean hasStepBounds() {return countStepBounds() > 0;}

	/** Are there bounds of type REWARD_BOUND */
	public boolean hasRewardBounds() {return countRewardBounds() > 0;}

	/** Do we have a bound of type DEFAULT_BOUND? */
	public boolean hasDefaultBound() {return countBoundsOfType(TemporalOperatorBound.BoundType.DEFAULT_BOUND) > 0;}

	/**
	 * Returns the default bound, if it exists. Otherwise, {@code null} is returned.
	 */
	public TemporalOperatorBound getDefaultBound()
	{
		return getFirstOfBoundType(TemporalOperatorBound.BoundType.DEFAULT_BOUND);
	}

	/**
	 * Returns the default bound or the single time bound.
	 * Returns {@code null} if there is none,
	 * throws exception if there are multiple time bounds.
	 */
	public TemporalOperatorBound getTimeBoundForContinuousTime() throws PrismLangException
	{
		if (hasDefaultBound()) {return getDefaultBound();}
		if (countTimeBounds() > 1) {
			throw new PrismLangException("Multiple time bounds, not supported!");
		}
		return getFirstOfBoundType(TemporalOperatorBound.BoundType.TIME_BOUND);
	}

	/**
	 * Returns the default bound, the single time bound or the single step bound.
	 * Returns {@code null} if there is none, throws exception if there are multiple bounds.
	 */
	public TemporalOperatorBound getStepBoundForDiscreteTime() throws PrismLangException
	{
		if (hasDefaultBound()) {return getDefaultBound();}
		if (countTimeBounds() + countStepBounds() > 1) {
			throw new PrismLangException("Multiple step/time bounds, not supported!");
		}
		if (hasStepBounds()) {
			return getFirstOfBoundType(TemporalOperatorBound.BoundType.STEP_BOUND);
		}
		return getFirstOfBoundType(TemporalOperatorBound.BoundType.TIME_BOUND);
	}

	/**
	 * Return the step bounds in a discrete time setting,
	 * i.e., all STEP_BOUND, TIME_BOUND and DEFAULT_BOUND.
	 */
	public List<TemporalOperatorBound> getStepBoundsForDiscreteTime() throws PrismLangException
	{
		return getBoundsOfType(TemporalOperatorBound.BoundType.STEP_BOUND,
		                       TemporalOperatorBound.BoundType.TIME_BOUND,
		                       TemporalOperatorBound.BoundType.DEFAULT_BOUND);
	}

	/**
	 * Return the time bounds in a continuous time setting,
	 * i.e., all TIME_BOUND and DEFAULT_BOUND.
	 */
	public List<TemporalOperatorBound> getStepBoundsForContinuousTime() throws PrismLangException
	{
		return getBoundsOfType(TemporalOperatorBound.BoundType.TIME_BOUND,
		                       TemporalOperatorBound.BoundType.DEFAULT_BOUND);
	}

	/**
	 * Returns the standard time/step bound for the given model type,
	 * either from default bound or specific time / step bound.
	 * Throws an exception if there are more than one step/time bounds.
	 * @param modelType the model type
	 */
	public TemporalOperatorBound getStandardBound(ModelType modelType) throws PrismLangException
	{
		if (modelType.continuousTime()) {
			return getTimeBoundForContinuousTime();
		} else {
			return getStepBoundForDiscreteTime();
		}
	}

	/**
	 * Return the reward bounds.
	 */
	public List<TemporalOperatorBound> getRewardBounds() throws PrismLangException
	{
		return getBoundsOfType(TemporalOperatorBound.BoundType.REWARD_BOUND);
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}

	@Override
	public String toString() {
		if (hasDefaultBound()) {
			return getDefaultBound().toString();
		} else {
			// TODO(JK): Change format: String result = "^{";
			String result = "{";
			boolean first=true;
			for (TemporalOperatorBound bound : getBounds()) {
				if (!first) result+=", ";
				first=false;
				result+=bound.toString();
			}
			result+="}";
			return result;
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TemporalOperatorBounds))
			return false;
		TemporalOperatorBounds other = (TemporalOperatorBounds) obj;
		if (bounds == null) {
			if (other.bounds != null)
				return false;
		} else if (!bounds.equals(other.bounds))
			return false;
		return true;
	}

	@Override
	public boolean isMatchingElement(ASTElement other)
	{
		if (!(other instanceof TemporalOperatorBounds))
			return false;

		return true;
	}

	@Override
	public TemporalOperatorBounds deepCopy() {
		TemporalOperatorBounds result = new TemporalOperatorBounds();
		for (TemporalOperatorBound bound : bounds) {
			result.bounds.add(bound.deepCopy());
		}
		return result;
	}

	public static List<List<TemporalOperatorBound>> groupBoundsDiscreteTime(List<TemporalOperatorBound> bounds) throws PrismException {
		return groupBounds(bounds, true);
	}
	
	public static List<List<TemporalOperatorBound>> groupBoundsContinuousTime(List<TemporalOperatorBound> bounds) throws PrismException {
		return groupBounds(bounds, false);
	}
	
	/**
	 * Group a list of TemporalOperatorBound by their reward structure / type.
	 * <br>
	 * Returns a List of Lists, where all TemporalOperatorBound in an inner list
	 * are of the same type (TIME_BOUND, STEP_BOUND, REWARD_BOUND) and for
	 * REWARD_BOUND share the same RewardStructure.
	 *
	 * The RewardStructure in the TemporalOperatorBound has to be already resolved.
	 */
	private static List<List<TemporalOperatorBound>> groupBounds(List<TemporalOperatorBound> bounds, boolean discreteTime) throws PrismException {
		List<TemporalOperatorBound> stepBounds = null;
		List<TemporalOperatorBound> timeBounds = null;
		// map from resolved reward structure index to list of bounds
		Map<Integer, List<TemporalOperatorBound>> rewardBounds = new HashMap<Integer, List<TemporalOperatorBound>>();
		List<List<TemporalOperatorBound>> result = new ArrayList<List<TemporalOperatorBound>>();

		stepBounds = filter(bounds, TemporalOperatorBound.BoundType.STEP_BOUND);
		if (discreteTime) {
			stepBounds.addAll(filter(bounds, TemporalOperatorBound.BoundType.TIME_BOUND));
			stepBounds.addAll(filter(bounds, TemporalOperatorBound.BoundType.DEFAULT_BOUND));
		} else {
			timeBounds = filter(bounds, TemporalOperatorBound.BoundType.TIME_BOUND);
			timeBounds.addAll(filter(bounds, TemporalOperatorBound.BoundType.DEFAULT_BOUND));
		}

		if (stepBounds != null && !stepBounds.isEmpty())
			result.add(stepBounds);

		if (timeBounds != null && !timeBounds.isEmpty())
			result.add(timeBounds);

		for (TemporalOperatorBound bound : filter(bounds, TemporalOperatorBound.BoundType.REWARD_BOUND)) {
			Integer rs = bound.getResolvedRewardStructIndex();
			if (rs == null) {
				throw new PrismException("groupBounds: RewardStruct has not yet been resolved.");
			}

			if (!rewardBounds.containsKey(rs)) {
				rewardBounds.put(rs, new ArrayList<TemporalOperatorBound>());
			}
			rewardBounds.get(rs).add(bound);
		}

		for (Entry<Integer, List<TemporalOperatorBound>> entry : rewardBounds.entrySet()) {
			result.add(entry.getValue());
		}

		return result;
	}

	/** Helper: Count the number of bounds with the specified type */
	private int countBoundsOfType(TemporalOperatorBound.BoundType... boundTypes)
	{
		return getBoundsOfType(boundTypes).size();
	}

	/** Get the first bound of the given type, {@code null} if there is none. */
	public TemporalOperatorBound getFirstOfBoundType(TemporalOperatorBound.BoundType boundType) {
		for (TemporalOperatorBound bound : bounds) {
			if (bound.getBoundType() == boundType) {
				return bound;
			}
		}
		return null;
	}

	/** Get a list of all bounds of the given types */
	public List<TemporalOperatorBound> getBoundsOfType(TemporalOperatorBound.BoundType... types) {
		HashSet<TemporalOperatorBound.BoundType> typeSet = new HashSet<TemporalOperatorBound.BoundType>(Arrays.asList(types));
		ArrayList<TemporalOperatorBound> result = new ArrayList<TemporalOperatorBound>();

		for (TemporalOperatorBound bound : getBounds()) {
			if (typeSet.contains(bound.getBoundType())) {
				result.add(bound);
			}
		}

		return result;
	}
	public static List<TemporalOperatorBound> filter(List<TemporalOperatorBound> bounds, TemporalOperatorBound.BoundType boundType) {
		List<TemporalOperatorBound> result = new ArrayList<TemporalOperatorBound>();

		for (TemporalOperatorBound bound : bounds) {
			if (bound.getBoundType() == boundType) {
				result.add(bound);
			}
		}

		return result;
	}
}
