package parser;

import java.util.IdentityHashMap;

import parser.ast.Expression;

public class EvaluateContextStateCached extends EvaluateContextState
{
	protected IdentityHashMap<Expression, Object> cache = new IdentityHashMap<>();

	public EvaluateContextStateCached(State state)
	{
		super(state);
	}

	public EvaluateContextStateCached(Values constantValues, State state)
	{
		super(constantValues, state);
	}

	public Object fetchResult(Expression expression)
	{
		return cache.get(expression);
	}

	public Object storeResult(Expression expression, Object result)
	{
		cache.put(expression, result);
		return result;
	}	
}
