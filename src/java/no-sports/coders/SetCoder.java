package no_sports.coders;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import mikera.vectorz.AVector;
import mikera.vectorz.BitVector;
import nuroko.coders.AbstractCoder;
import nuroko.core.NurokoException;

public class SetCoder<T> extends AbstractCoder<Collection<T>> {
	final int length;
	final Object[] choiceArray;
	final HashMap<T,Integer> choiceMap=new HashMap<T,Integer>();

	public SetCoder(Collection<T> choices) {
		length=choices.size();
		choiceArray=new Object[length];
		int i=0;
		for (T t: choices) {
			choiceArray[i]=t;
			choiceMap.put(t, i);
			i++;
		}
	}
	
	public SetCoder(T... values) {
		this(Arrays.asList(values));
	}

	@Override 
	public int codeLength() {
		return length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<T> decode(AVector v, int offset) {
		int choice=0;
    HashSet<T> results = new HashSet<T>();
		for (int i=0; i<length; i++) {
			double d=v.get(offset+i);
			if (d>0.0) {
        results.add((T)choiceArray[i]);
			}
		}
		return results;
	}

	@Override
	public void encode(Collection<T> objects, AVector dest, int offset) {
    // TODO: don't modify output in event of exception below
    dest.fillRange(offset, length, 0.0);
    for (T object : objects) {
      Integer i=choiceMap.get(object);
      if (i != null)
        dest.set(offset+i,1.0);
    }
	}

	public List<T> getChoiceList() {
		return (List<T>) Arrays.asList(choiceArray);
	}
	
	@Override
	public AVector createOutputVector() {
		return BitVector.createLength(codeLength());
	}
}
