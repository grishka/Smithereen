package smithereen.jtwigext;

import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.value.context.ValueContext;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import smithereen.lang.Lang;

public class NumberSequenceFunction extends SimpleJtwigFunction{
	// https://stackoverflow.com/questions/41614100/how-to-invoke-custom-functions-in-jtwig

	public NumberSequenceFunction(){
	}

	@Override
	public String name(){
		return "sequence";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		functionRequest.minimumNumberOfArguments(2);
		functionRequest.maximumNumberOfArguments(3);
		int start=getInt(functionRequest.get(0));
		int end=getInt(functionRequest.get(1));
		int step;
		if(functionRequest.getNumberOfArguments()==3)
			step=getInt(functionRequest.get(2));
		else
			step=start<end ? 1 : -1;
		Integer[] res=new Integer[Math.abs((start-end)/step)+1];
		int _i=0;
		int i=start;
		while(_i<res.length){
			res[_i]=i;
			i+=step;
			_i++;
		}
		return res;
	}

	private static int getInt(Object o){
		if(o instanceof BigDecimal)
			return ((BigDecimal) o).intValue();
		if(o instanceof Integer || o instanceof Long)
			return (int)o;
		throw new IllegalArgumentException("How many goddamn ways of storing integers in objects are there?");
	}
}
