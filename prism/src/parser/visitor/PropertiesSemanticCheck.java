//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package parser.visitor;

import parser.ast.ConstantList;
import parser.ast.Expression;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuantileProb;
import parser.ast.ExpressionQuantileProbNormalForm;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionTemporal;
import parser.ast.FormulaList;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.TemporalOperatorBound;
import prism.ModelInfo;
import prism.PrismLangException;

/**
 * Perform any required semantic checks on a PropertiesFile (or parts of it).
 * These checks are done *before* any undefined constants have been defined.
 * Optionally pass in parent ModulesFile (or leave null);
 */
public class PropertiesSemanticCheck extends SemanticCheck
{
	private PropertiesFile propertiesFile;
	private ModelInfo modelInfo;
	private ModulesFile modulesFile;
	private String quantileVariableName = null;

	public PropertiesSemanticCheck(PropertiesFile propertiesFile)
	{
		this(propertiesFile, null);
	}

	public PropertiesSemanticCheck(PropertiesFile propertiesFile, ModelInfo modelInfo)
	{
		setPropertiesFile(propertiesFile);
		setModelInfo(modelInfo);
	}

	public void setPropertiesFile(PropertiesFile propertiesFile)
	{
		this.propertiesFile = propertiesFile;
	}

	public void setModelInfo(ModelInfo modelInfo)
	{
		this.modelInfo = modelInfo;
		if (modelInfo instanceof ModulesFile) {
			this.modulesFile = (ModulesFile) modelInfo;
		} else {
			this.modulesFile = null;
		}
	}

	public Object visit(FormulaList e) throws PrismLangException
	{
		// Override - don't need to do any semantic checks on formulas
		// (they will have been expanded in place, where needed)
		// (and we shouldn't check them - e.g. clock vars appearing in errors would show as an error)
		return null;
	}
	
	public void visitPost(LabelList e) throws PrismLangException
	{
		int i, n;
		String s;
		n = e.size();
		for (i = 0; i < n; i++) {
			s = e.getLabelName(i);
			if ("deadlock".equals(s))
				throw new PrismLangException("Cannot define a label called \"deadlock\" - this is a built-in label", e.getLabel(i));
			if ("init".equals(s))
				throw new PrismLangException("Cannot define a label called \"init\" - this is a built-in label", e.getLabel(i));
		}
	}

	public void visitPost(ConstantList e) throws PrismLangException
	{
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getConstant(i) != null && !e.getConstant(i).isConstant()) {
				throw new PrismLangException("Definition of constant \"" + e.getConstantName(i) + "\" is not constant", e.getConstant(i));
			}
		}
	}

	public void visitPost(ExpressionTemporal e) throws PrismLangException
	{
		int op = e.getOperator();
		Expression operand1 = e.getOperand1();
		Expression operand2 = e.getOperand2();
		boolean hasBounds = e.getBounds().countBounds() > 0;

		// Other checks (which parser should never allow to occur anyway)
		if (op == ExpressionTemporal.P_X && (operand1 != null || operand2 == null || hasBounds)) {
			throw new PrismLangException("Cannot attach bounds to " + e.getOperatorSymbol() + " operator", e);
		}
		if (op == ExpressionTemporal.R_C) {
			boolean malformed = false;
			if (operand1 != null || operand2 != null) { malformed = true;}
			if (e.hasBounds()) {
				if (e.getBounds().countBounds() > 1 || !e.getBounds().hasDefaultBound()) { malformed = true; }
				else if (e.getBounds().getDefaultBound().hasLowerBound()) {
					// NB: upper bound is optional (e.g. multi-objective allows R...[C] operator)
					malformed = true;
				}
			}
			if (malformed)
				throw new PrismLangException("Badly formed " + e.getOperatorSymbol() + " operator", e);
		}
		if (op == ExpressionTemporal.R_I) {
			boolean malformed = false;
			if (operand1 != null || operand2 != null) {malformed = true;}
			if (e.hasBounds()) {
				if (e.getBounds().countBounds() > 1 || !e.getBounds().hasDefaultBound()) { malformed = true; }
				else if (e.getBounds().getDefaultBound().hasLowerBound() || !e.getBounds().getDefaultBound().hasUpperBound()) {
					malformed = true;
				}
			}
			if (malformed)
				throw new PrismLangException("Badly formed " + e.getOperatorSymbol() + " operator", e);
		}
		if (op == ExpressionTemporal.R_S && (operand1 != null || operand2 != null || hasBounds)) {
			throw new PrismLangException("Badly formed " + e.getOperatorSymbol() + " operator", e);
		}
	}

	public void visitPost(TemporalOperatorBound e) throws PrismLangException
	{
		// TODO: JK Refactor!
		Expression lBound = e.getLowerBound();
		if (lBound != null && !lBound.isConstant() && quantileVariableName==null) {
			throw new PrismLangException("Lower bound in " + e.toString() + " is not constant", lBound);
		}
		if (lBound != null && !lBound.isConstant()) {
			throw new PrismLangException("Lower bound " + e + " is not constant", lBound);
		}
		Expression uBound = e.getUpperBound();
		if (uBound != null && !uBound.isConstant() && quantileVariableName==null) {
			throw new PrismLangException("Upper bound in " + e.toString() + " is not constant", uBound);
		}
		if (uBound != null && !uBound.isConstant()) {
			throw new PrismLangException("Upper bound " + e + " not constant", uBound);
		}
	}

	public void visitPost(ExpressionProb e) throws PrismLangException
	{
		if (e.getModifier() != null) {
			throw new PrismLangException("Modifier \"" + e.getModifier() + "\" not supported for P operator");
		}
		if (e.getProb() != null && !e.getProb().isConstant()) {
			throw new PrismLangException("P operator probability bound is not constant", e.getProb());
		}
	}

	public void visitPost(ExpressionReward e) throws PrismLangException
	{
		if (e.getModifier() != null) {
			throw new PrismLangException("Modifier \"" + e.getModifier() + "\" not supported for R operator");
		}
		if (e.getRewardStructIndex() != null) {
			if (e.getRewardStructIndex() instanceof Expression) {
				Expression rsi = (Expression) e.getRewardStructIndex();
				if (!(rsi.isConstant())) {
					throw new PrismLangException("R operator reward struct index is not constant", rsi);
				}
			} else if (e.getRewardStructIndex() instanceof String) {
				String s = (String) e.getRewardStructIndex();
				if (modulesFile != null && modulesFile.getRewardStructIndex(s) == -1) {
					throw new PrismLangException("R operator reward struct index \"" + s + "\" does not exist", e);
				}
			}
		}
		if (e.getRewardStructIndexDiv() != null) {
			if (e.getRewardStructIndexDiv() instanceof Expression) {
				Expression rsi = (Expression) e.getRewardStructIndexDiv();
				if (!(rsi.isConstant())) {
					throw new PrismLangException("R operator reward struct index is not constant", rsi);
				}
			} else if (e.getRewardStructIndexDiv() instanceof String) {
				String s = (String) e.getRewardStructIndexDiv();
				if (modulesFile != null && modulesFile.getRewardStructIndex(s) == -1) {
					throw new PrismLangException("R operator reward struct index \"" + s + "\" does not exist", e);
				}
			}
		}
		if (e.getReward() != null && !e.getReward().isConstant()) {
			throw new PrismLangException("R operator reward bound is not constant", e.getReward());
		}
	}

	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		if (e.getModifier() != null) {
			throw new PrismLangException("Modifier \"" + e.getModifier() + "\" not supported for S operator");
		}
		if (e.getProb() != null && !e.getProb().isConstant()) {
			throw new PrismLangException("S operator probability bound is not constant", e.getProb());
		}
	}

	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		String name = e.getName();
		// Allow special cases
		if ("deadlock".equals(name) || "init".equals(name))
			return;
		// Otherwise check if it exists
		if (modelInfo != null && modelInfo.getLabelIndex(name) != -1) { 
			return;
		} else if (propertiesFile != null) {
			LabelList labelList = propertiesFile.getCombinedLabelList();
			if (labelList != null && labelList.getLabelIndex(name) != -1) {
				return;
			}
		}
		throw new PrismLangException("Undeclared label", e);
	}

	public void visitPost(ExpressionFilter e) throws PrismLangException
	{
		// Check filter type is valid
		if (e.getOperatorType() == null) {
			throw new PrismLangException("Unknown filter type \"" + e.getOperatorName() + "\"", e);
		}
	}

	public void visitPost(ExpressionIdent e) throws PrismLangException
	{
		if (quantileVariableName != null && e.getName().equals(quantileVariableName)){
			return;
		} else {
			super.visitPost(e);
		}
	}

	public void visitPre(ExpressionQuantileProbNormalForm e)
	{
		quantileVariableName = e.getQuantileVariable();
	}

	public void visitPost(ExpressionQuantileProbNormalForm e)
	{
		quantileVariableName = null;
	}

	public void visitPre(ExpressionQuantileProb e)
	{
		quantileVariableName = e.getQuantileVariable();
	}

	public void visitPost(ExpressionQuantileProb e)
	{
		quantileVariableName = null;
	}
}
