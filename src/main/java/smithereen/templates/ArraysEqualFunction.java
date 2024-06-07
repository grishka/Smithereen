package smithereen.templates;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class ArraysEqualFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Object a=args.get("a"), b=args.get("b");
		if(a instanceof int[] ia && b instanceof int[] ib){
			return Arrays.equals(ia, ib);
		}else if(a instanceof long[] la && b instanceof long[] lb){
			return Arrays.equals(la, lb);
		}else if(a instanceof byte[] ba && b instanceof byte[] bb){
			return Arrays.equals(ba, bb);
		}else if(a instanceof Object[] oa && b instanceof Object[] ob){
			return Arrays.equals(oa, ob);
		}
		return false;
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("a", "b");
	}
}
