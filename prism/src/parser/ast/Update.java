//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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
import java.util.BitSet;

import parser.*;
import parser.type.*;
import parser.visitor.*;
import prism.PrismLangException;

/**
 * Class to store a single update, i.e. a mapping from variables to expressions. 
 * e.g. (s'=1)&amp;(x'=x+1)
 */
public class Update extends ASTElement
{
	// Lists of variable/expression pairs (and types)
	private ArrayList<String> vars;
	private ArrayList<Expression> exprs;
	private ArrayList<Type> types;
	// We also store an ExpressionIdent to match each variable.
	// This is to just to provide positional info.
	private ArrayList<ExpressionIdent> varIdents;
	// The indices of each variable in the model to which it belongs
	private ArrayList<Integer> indices;
	// Parent Updates object
	private Updates parent;

	/**
	 * Create an empty update.
	 */
	public Update()
	{
		vars = new ArrayList<String>();
		exprs = new ArrayList<Expression>();
		types = new ArrayList<Type>();
		varIdents = new ArrayList<ExpressionIdent>();
		indices = new ArrayList<Integer>();
	}

	// Set methods

	public void addElement(ExpressionIdent v, Expression e)
	{
		vars.add(v.getName());
		exprs.add(e);
		types.add(null); // Type currently unknown
		varIdents.add(v);
		indices.add(-1); // Index currently unknown
	}

	public void setVar(int i, ExpressionIdent v)
	{
		vars.set(i, v.getName());
		varIdents.set(i, v);
	}

	public void setExpression(int i, Expression e)
	{
		exprs.set(i, e);
	}

	public void setType(int i, Type t)
	{
		types.set(i, t);
	}

	public void setVarIndex(int i, int index)
	{
		indices.set(i, index);
	}

	public void setParent(Updates u)
	{
		parent = u;
	}

	// Get methods

	public int getNumElements()
	{
		return vars.size();
	}

	public String getVar(int i)
	{
		return vars.get(i);
	}

	public Expression getExpression(int i)
	{
		return exprs.get(i);
	}

	public Type getType(int i)
	{
		return types.get(i);
	}

	public ExpressionIdent getVarIdent(int i)
	{
		return varIdents.get(i);
	}

	public int getVarIndex(int i)
	{
		return indices.get(i);
	}

	public Updates getParent()
	{
		return parent;
	}

	/**
	 * Execute this update, based on variable values specified as a Values object,
	 * returning the result as a new Values object copied from the existing one.
	 * Values of any constants should also be provided.
	 * @param constantValues Values for constants
	 * @param oldValues Variable values in current state
	 */
	public Values update(Values constantValues, Values oldValues) throws PrismLangException
	{
		int i, n;
		Values res;
		res = new Values(oldValues);
		n = exprs.size();
		for (i = 0; i < n; i++) {
			Object newValue = getExpression(i).evaluate(constantValues, oldValues);
			if (getVarIndex(i) == -2) {
				res.setViewValue(getVar(i), newValue);
			} else {
				res.setValue(getVar(i), newValue);
			}
		}
		return res;
	}

	/**
	 * Execute this update, based on variable values specified as a Values object,
	 * applying changes in variables to a second Values object. 
	 * Values of any constants should also be provided.
	 * @param constantValues Values for constants
	 * @param oldValues Variable values in current state
	 * @param newValues Values object to apply changes to
	 */
	public void update(Values constantValues, Values oldValues, Values newValues) throws PrismLangException
	{
		int i, n;
		n = exprs.size();
		for (i = 0; i < n; i++) {
			Object newValue = getExpression(i).evaluate(constantValues, oldValues);
			if (getVarIndex(i) == -2) {
				newValues.setViewValue(getVar(i), newValue);
			} else {
				newValues.setValue(getVar(i), newValue);
			}
		}
	}

	/**
	 * Execute this update, based on variable values specified as a State object,
	 * returning the result as a new State object copied from the existing one.
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 */
	public State update(State oldState) throws PrismLangException
	{
		int i, n;
		State res;
		res = new State(oldState);
		n = exprs.size();
		for (i = 0; i < n; i++) {
			Object newValue = getExpression(i).evaluate(oldState);
			if (getVarIndex(i) == -2) {
				res.setViewValue(getVar(i), newValue);
			} else {
				res.setValue(getVarIndex(i), newValue);
			}
		}
		return res;
	}

	/**
	 * Execute this update, based on variable values specified as a State object.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. oldState and newState should be equal when passed in.) 
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 */
	public void update(State oldState, State newState) throws PrismLangException
	{
		int i, n;
		n = exprs.size();
		for (i = 0; i < n; i++) {
			Object newValue = getExpression(i).evaluate(oldState);
			if (getVarIndex(i) == -2) {
				newState.setViewValue(getVar(i), newValue);
			} else {
				newState.setValue(getVarIndex(i), newValue);
			}
		}
	}

	/**
	 * Execute this update, based on variable values specified as a State object.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. oldState and newState should be equal when passed in.) 
	 * Both State objects represent only a subset of the total set of variables,
	 * with this subset being defined by the mapping varMap.
	 * Only variables in this subset are updated.
	 * But if doing so requires old values for variables outside the subset, this will cause an exception. 
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 * @param varMap A mapping from indices (over all variables) to the subset (-1 if not in subset). 
	 */
	public void updatePartially(State oldState, State newState, int[] varMap) throws PrismLangException
	{
		int i, j, n;
		n = exprs.size();
		for (i = 0; i < n; i++) {
			if (getVarIndex(i) == -2)
				throw new PrismLangException("Partial updates are currently not supported for views.");
			j = varMap[getVarIndex(i)];
			if (j != -1) {
				newState.setValue(j, getExpression(i).evaluate(new EvaluateContextSubstate(oldState, varMap)));
			}
		}
	}

	/**
	 * Check whether this update (from a particular state) would cause any errors, mainly variable overflows.
	 * Variable ranges are specified in the passed in VarList.
	 * Throws an exception if such an error occurs.
	 */
	public State checkUpdate(State oldState, VarList varList) throws PrismLangException
	{
		int i, n, valNew;
		State res;
		res = new State(oldState);
		n = exprs.size();
		for (i = 0; i < n; i++) {
			valNew = varList.encodeToInt(i, getExpression(i).evaluate(oldState));
			if (valNew < varList.getLow(i) || valNew > varList.getHigh(i))
				throw new PrismLangException("Value of variable " + getVar(i) + " overflows", getExpression(i));
		}
		return res;
	}

	// Methods required for ASTElement:

	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	/**
	 * Convert to string.
	 */
	public String toString()
	{
		int i, n;
		String s = "";

		n = exprs.size();
		// normal case
		if (n > 0) {
			for (i = 0; i < n - 1; i++) {
				s = s + "(" + vars.get(i) + "'=" + exprs.get(i) + ") & ";
			}
			s = s + "(" + vars.get(n - 1) + "'=" + exprs.get(n - 1) + ")";
		}
		// special (empty) case
		else {
			s = "true";
		}

		return s;
	}

	@Override
	public Update deepCopy(DeepCopy copier) throws PrismLangException
	{
		copier.copyAll(exprs);
		copier.copyAll(varIdents);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Update clone()
	{
		Update clone = (Update) super.clone();

		clone.indices   = (ArrayList<Integer>) indices.clone();
		clone.vars      = (ArrayList<String>) vars.clone();
		clone.exprs     = (ArrayList<Expression>) exprs.clone();
		clone.types     = (ArrayList<Type>) types.clone();
		clone.varIdents = (ArrayList<ExpressionIdent>) varIdents.clone();

		return clone;
	}

	@Override
	public int shallowHashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((indices == null) ? 0 : indices.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		result = prime * result + ((vars == null) ? 0 : vars.hashCode());
		return result;
	}
	
	/**
	 * @return The set of variables written by this update.
	 */
	public BitSet getWrittenVariables() {
		final BitSet variables = new BitSet();
		for (int variable = getNumElements() - 1; variable >= 0; variable--) {
			variables.set(getVarIndex(variable));
		}
		return variables;
	}


	/**
	 * @param index of a variable in the model
	 * @return the variable's ExpressionIdent or null if this update does not write the variable
	 */
	public ExpressionIdent getVarIdentFromIndex(final int index)
	{
		int variable = getLocalVarIndex(index);

		return variable < 0 ? null : getVarIdent(variable);
	}

	/**
	 * @param index of a variable in the model
	 * @return local index of the variable of -1 if this update does not write the variable
	 */
	public int getLocalVarIndex(final int index) {
		for (int variable = indices.size() - 1; variable >= 0; variable--) {
			if (getVarIndex(variable) == index) {
				return variable;
			}
		}
		return -1;
	}

	public Update split(final int index) {
		int variable = getLocalVarIndex(index);

		if (variable < 0) {
			return null;
		}
		final Update update = new Update();
		update.setPosition(this);
		// copy assignment to new update
		update.addElement((ExpressionIdent) getVarIdent(variable).deepCopy(), getExpression(variable).deepCopy());
		update.setType(0, getType(variable));
		update.setVarIndex(0, getVarIndex(variable));
		update.setParent(parent);
		// remove assignment from this update
		vars.remove(variable);
		exprs.remove(variable);
		types.remove(variable);
		varIdents.remove(variable);
		indices.remove(variable);
		return update;
	}

	public Update cummulateUpdatesForVariable(final Update other, final int index) throws PrismLangException
	{
		assert this.getNumElements() == 1 : "one and only one variable update expected";
		assert other.getNumElements() == 1 : "one and only one variable update expected";
		assert this.getVarIndex(0) == index : "update expected to write variable #" + index;
		assert other.getVarIndex(0) == index : "update expected to write variable #" + index;

		ExpressionBinaryOp expression;
		ExpressionBinaryOp otherExpression;
		if ((exprs.get(0) instanceof ExpressionBinaryOp) && (other.exprs.get(0) instanceof ExpressionBinaryOp)) {
			expression = (ExpressionBinaryOp) exprs.get(0);
			otherExpression = (ExpressionBinaryOp) other.exprs.get(0);
		} else {
			throw new PrismLangException("updates do not use binary operators");
		}

		final Integer[] supported = new Integer[] {ExpressionBinaryOp.PLUS, ExpressionBinaryOp.MINUS, ExpressionBinaryOp.TIMES};
		int operator = expression.getOperator();
		if (! Arrays.asList(supported).contains(operator)) {
			throw new PrismLangException("unsupported operator " + expression.getOperatorSymbol());
		}
		if (operator == ExpressionBinaryOp.MINUS) {
			operator = ExpressionBinaryOp.PLUS;
			expression = convertMinusToPlus(expression, index);
		}
		if (otherExpression.getOperator() == ExpressionBinaryOp.MINUS) {
			otherExpression = convertMinusToPlus(otherExpression, index);
		}
		if (operator != otherExpression.getOperator()) {
			throw new PrismLangException("incompatible top level opertors " + expression.getOperatorSymbol() + " and " + otherExpression.getOperatorSymbol());
		}

		final Tuple<ExpressionVar, Expression> thisSplit = splitExpression(expression, index);
		final Tuple<ExpressionVar, Expression> otherSplit = splitExpression(otherExpression, index);

		final ExpressionBinaryOp joined = new ExpressionBinaryOp(operator, thisSplit.first, new ExpressionBinaryOp(operator, thisSplit.second, otherSplit.second));
		final Update update = (Update) this.deepCopy();
		update.exprs.set(0, joined);
		update.setParent(parent);
		return update;
	}

	private ExpressionBinaryOp convertMinusToPlus(ExpressionBinaryOp expression, final int index) throws PrismLangException
	{
		if(!isVariable(expression.getOperand1(), index)) {
			throw new PrismLangException("variable has to be the minuend", expression);
		}
		return (ExpressionBinaryOp) Expression.Plus(expression.getOperand1(), new ExpressionUnaryOp(ExpressionUnaryOp.MINUS, expression.getOperand2()));
	}

	private Tuple<ExpressionVar, Expression> splitExpression(final ExpressionBinaryOp expression, final int index) throws PrismLangException
	{
		assert index >= 0 : "variable index has to be be positive";

		final Tuple<ExpressionVar, Expression> result;

		if (isVariable(expression.getOperand1(), index)) {
			result = new Tuple<ExpressionVar, Expression>((ExpressionVar) expression.getOperand1(), expression.getOperand2());
		} else if (isVariable(expression.getOperand2(), index)) {
			result = new Tuple<ExpressionVar, Expression>((ExpressionVar) expression.getOperand2(), expression.getOperand1());
		} else {
			throw new PrismLangException("variable #" + index + " does not occur as top level operand", expression);
		}
		SearchVariable search = new SearchVariable(index);
		result.second.accept(search);
		if (search.isSuccessful()) {
			throw new PrismLangException("both operands depend on variable #" + index, expression);
		}
		return result;
	}

	private boolean isVariable(final Expression expression, final int index) {
		assert index >= 0 : "variable index has to be be positive";

		if (!(expression instanceof ExpressionVar)) {
			return false;
		}
		return ((ExpressionVar) expression).getIndex() == index;
	}


	public class SearchVariable extends ASTTraverse
	{
		final private int variable;
		private boolean successful = false;

		public SearchVariable(final int index) {
			this.variable = index;
		}

		public boolean isSuccessful()
		{
			return successful;
		}

		public void visitPost(final ExpressionVar e) throws PrismLangException
		{
			int index = e.getIndex();
			if (index < 0) {
				throw new PrismLangException("Index of variable not yet set.", e);
			}
			successful = successful || (index == variable);
		}
	}

	private class Tuple<S, T>
	{
		public final S first;
		public final T second;

		public Tuple(S first, T second)
		{
			this.first = first;
			this.second = second;
		}
	}
}

//------------------------------------------------------------------------------
