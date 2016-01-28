package parser.visitor;

import java.util.Iterator;

import parser.ast.*;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismLangException;
/**
 * Find all ExpressionBoundVariables with the given name and replace with the given value (or delete completely)
 */
public class BoundVariableReplacer extends ASTTraverseModify{
	public static final int DELETE = -1;
	
	private boolean delete = false;
	private ExpressionLiteral replacementValue;
	private String name;
	
	public BoundVariableReplacer(String name, int replacementValue){
		this.name = name;
		this.replacementValue = new ExpressionLiteral(TypeInt.getInstance(), replacementValue);
	}
	public BoundVariableReplacer(String name, double replacementValue){
		this.name = name;
		this.replacementValue = new ExpressionLiteral(TypeDouble.getInstance(), replacementValue);
	}
	public BoundVariableReplacer(String name){
		this.name = name;
		delete = true;
	}
	
	@Override
	public Object visit(ExpressionTemporal e) throws PrismLangException {
		if (!delete) return super.visit(e);
		
		TemporalOperatorBounds bounds = e.getBounds();
		Iterator<TemporalOperatorBound> it = bounds.getBounds().iterator();
		while (it.hasNext()) {
			TemporalOperatorBound cur = it.next();
			
			boolean removeLower = false;
			boolean removeUpper = false;
			
			if (cur.hasLowerBound()) {
				Expression lb = cur.getLowerBound();
				if (lb instanceof ExpressionBoundVariable &&
				    name.equals(((ExpressionBoundVariable)lb).getName())) {
					removeLower = true;
				}
			}
			
			if (cur.hasUpperBound()) {
				Expression ub = cur.getUpperBound();
				if (ub instanceof ExpressionBoundVariable &&
				    name.equals(((ExpressionBoundVariable)ub).getName())) {
					removeUpper = true;
				}
			}

			boolean removeCompletely =
				(removeUpper && removeLower) ||
				(removeUpper && !cur.hasLowerBound()) ||
				(removeLower && !cur.hasUpperBound());
			
			if (removeCompletely)
				it.remove();
			else if (removeLower)
				cur.setLowerBound(null);
			else if (removeUpper)
				cur.setUpperBound(null);
		}
		
		return e;
	}
	
	public Object visit(ExpressionBoundVariable e){
		if (e.getName().equals(name)) {
			if (delete)  // is handled above
				return e;

			replacementValue.setPosition(e);
			return replacementValue;
		}
		//in any other case do nothing 
		return e;
	}
}