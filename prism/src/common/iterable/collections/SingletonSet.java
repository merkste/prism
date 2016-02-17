package common.iterable.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import common.iterable.EmptyIterator;
import common.iterable.SingletonIterator;

/**
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public class SingletonSet<T> implements Set<T>
{
	protected T element;

	public SingletonSet()
	{
		this(null);
	}

	public SingletonSet(final T item)
	{
		element = item;
	}

	@Override
	public Iterator<T> iterator()
	{
		if (isEmpty()){
			return EmptyIterator.Of();
		}
		return new SingletonIterator.Of<T>(element)
		{
			@Override
			public void remove()
			{
				SingletonSet.this.element = null;
			}
		};
	}

	@Override
	public int size()
	{
		return isEmpty() ? 0 : 1;
	}

	@Override
	public boolean isEmpty()
	{
		return (element == null);
	}

	@Override
	public boolean contains(final Object o)
	{
		return !isEmpty() && element.equals(o);
	}

	@Override
	public Object[] toArray()
	{
		return isEmpty() ? new Object[] {} : new Object[] { element };
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> S[] toArray(final S[] a)
	{
		final S[] result;
		final int size = size();
		if (a.length >= size){
			result = a;
		} else {
			result = (S[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size());
		}

		final Iterator<T> it = iterator();
		for (int index = 0; index < size; index++){
			result[index] = (S) (it.hasNext() ? it.next() : null);
		}
		return result;
	}

	@Override
	public boolean add(final T item)
	{
		if (isEmpty()){
			element = item;
			return true;
		}
		if (! element.equals(item)){
			throw new IllegalArgumentException("A SingletonSet can hold only one item.");
		}
		return false;
	}

	@Override
	public boolean remove(final Object o)
	{
		if (isEmpty()){
			return false;
		}
		if (element.equals(o)){
			clear();
			return true;
		}
		return false;
	}

	private boolean containsOnlyOneUniqueElement(final Collection<?> c)
	{
		Object obj = null;
		for (Object each : c){
			if (obj == null){
				obj = each;
			} else {
				if (!obj.equals(each)){
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean containsAll(final Collection<?> c)
	{
		switch (c.size()){
		case 0:
			return true;
		case 1:
			return c.contains(element);
		default:
			return containsOnlyOneUniqueElement(c) && c.contains(element);
		}
	}

	@Override
	public boolean addAll(final Collection<? extends T> c)
	{
		if (!isEmpty()){
			if (containsAll(c)){
				return false;
			}
			throw new IllegalArgumentException("A SingletonSet can hold only one item.");
		}
		switch (c.size()){
		case 0:
			return false;
		case 1:
			for (T elem : c){
				return add(elem);
			}
		default:
			if (containsOnlyOneUniqueElement(c)){
				for (T elem : c){
					return add(elem);
				}
			}
			throw new IllegalArgumentException("A SingletonSet can hold only one item.");
		}
	}

	@Override
	public boolean removeAll(final Collection<?> c)
	{
		if (isEmpty()){
			return false;
		}
		if (c.contains(element)){
			clear();
			return true;
		}
		return false;
	}

	@Override
	public boolean retainAll(final Collection<?> c)
	{
		if (isEmpty() || c.contains(element)){
			return false;
		}
		clear();
		return true;
	}

	@Override
	public void clear()
	{
		element = null;
	}

	public T getElement()
	{
		return element;
	}

	@Override
	public String toString()
	{
		if (isEmpty()){
			return "{}";
		}
		return "{" + element.toString() + "}";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (! (obj instanceof SingletonSet)){
			return false;
		}
		final SingletonSet<?> other = (SingletonSet<?>) obj;
		if (isEmpty()){
			return other.isEmpty();
		}
		if (other.isEmpty()){
			return false;
		}
		return element.equals(other.element);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((element == null) ? 0 : element.hashCode());
		return result;
	}
}