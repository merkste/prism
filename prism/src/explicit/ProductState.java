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


package explicit;

/**
 * A product state, i.e., a pair of two indizes for
 * referencing the states of the left and right part of the product.
 * <br>
 * Objects are {@code Comparable}, i.e., can be used in TreeSets etc.
 */
public class ProductState implements Comparable<ProductState>
{
	/** The first state */
	private Integer state_1;
	/** The second state */
	private Integer state_2;

	/** Constructor */
	public ProductState(Integer state_1, Integer state_2)
	{
		this.state_1 = state_1;
		this.state_2 = state_2;
	}

	/** Get the first state */
	public Integer getFirstState()
	{
		return state_1;
	}

	/** Get the second state */
	public Integer getSecondState()
	{
		return state_2;
	}

	@Override
	public String toString()
	{
		return "[" + state_1 + "," + state_2 + "]";
	}

	@Override
	public int compareTo(ProductState other)
	{
		int rv = state_1.compareTo(state_2);
		if (rv == 0) {
			return state_2.compareTo(other.state_2);
		} else {
			return rv;
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((state_1 == null) ? 0 : state_1.hashCode());
		result = prime * result + ((state_2 == null) ? 0 : state_2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProductState other = (ProductState) obj;
		return state_1.equals(other.state_1) &&
				state_2.equals(other.state_2);
	}
}
