package parser.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import parser.ast.ASTElement;
import prism.PrismLangException;

/**
 * Traverse the AST and store the encountered ASTElements in a list.
 */

public class ASTToList extends ASTTraverse {
	
	/** A pair (parentIndex, element). The parent index is -1 for the root element. */
	public class ElementWithParent {
		private int parentIndex;
		private ASTElement element;
		
		ElementWithParent(int parentIndex, ASTElement element)
		{
			this.parentIndex = parentIndex;
			this.element = element;
		}
		
		public int getParentIndex()
		{
			return parentIndex;
		}

		public ASTElement getElement()
		{
			return element;
		}

	}
	
	private List<ElementWithParent> result = new ArrayList<ElementWithParent>();
	private Stack<Integer> parents = new Stack<Integer>();

	@Override
	public void defaultVisitPre(ASTElement e)
	{
		int parentIndex = parents.isEmpty() ? -1 : 0;
		result.add(new ElementWithParent(parentIndex, e));

		int index = result.size() - 1;
		parents.push(index);
	}

	@Override
	public void defaultVisitPost(ASTElement e)
	{
		parents.pop();
	}

	/**
	 * Traverses the syntax tree and stores the ASTElements in a list of ElementWithParent.
	 * @param root the root of the syntax tree
	 * @return the list, or {@code null} if there was an error
	 */
	public static List<ElementWithParent> toList(ASTElement root) {
		ASTToList visitor = new ASTToList();
		try {
			root.accept(visitor);
		} catch (PrismLangException e) {
			return null;
		}
		return visitor.result;
	}
}
