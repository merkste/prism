package parser.visitor;

import java.util.List;

import parser.ast.Expression;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionTemporal;
import parser.ast.TemporalOperatorBound;
import prism.PrismLangException;


public class ReplaceBound extends ASTTraverseModify {
	private TemporalOperatorBound rewardBound;
	private String replacementLabel;
	private boolean success = false;
	
	public ReplaceBound(TemporalOperatorBound rewardBound, String replacementLabel)
	{
		this.rewardBound = rewardBound;
		this.replacementLabel = replacementLabel;
	}
	
	public boolean wasSuccessfull() {return success;}

	public void visitPost(ExpressionTemporal expr) throws PrismLangException
	{
		if (!expr.hasBounds()) return;
		List<TemporalOperatorBound> bounds = expr.getBounds().getBounds();
		
		ExpressionLabel label = new ExpressionLabel(replacementLabel);
		
		for (int i=0; i< bounds.size(); i++) { 
			if (bounds.get(i) == rewardBound) {
				bounds.remove(i);
				
				success = true;
				
				switch (expr.getOperator()) {
				case ExpressionTemporal.P_F:
				case ExpressionTemporal.P_G:
				case ExpressionTemporal.P_U:
					expr.setOperand2(Expression.Parenth(ExpressionTemporal.And(expr.getOperand2(), label)));
					break;
				case ExpressionTemporal.P_R:
					expr.setOperand1(Expression.Parenth(ExpressionTemporal.And(expr.getOperand1(), label)));
				case ExpressionTemporal.P_W:
					// TODO ??
					throw new PrismLangException("Weak-Until can not be annotated with a reward bound.");
				case ExpressionTemporal.P_X:
					throw new PrismLangException("Nextstep can not be annotated with a reward bound.");
				}
			}
		}
	}
}
