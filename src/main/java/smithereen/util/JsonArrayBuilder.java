package smithereen.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class JsonArrayBuilder{
	public static final Collector<Object, JsonArray, JsonArray> COLLECTOR=new Collector<>(){
		@Override
		public Supplier<JsonArray> supplier(){
			return JsonArray::new;
		}

		@Override
		public BiConsumer<JsonArray, Object> accumulator(){
			return (arr, o)->{
				if(o instanceof JsonElement el)
					arr.add(el);
				else if(o instanceof String s)
					arr.add(s);
				else if(o instanceof Number n)
					arr.add(n);
				else if(o instanceof Boolean b)
					arr.add(b);
				else
					throw new UnsupportedOperationException("object has unsupported type: "+o.getClass());
			};
		}

		@Override
		public BinaryOperator<JsonArray> combiner(){
			return (a1, a2)->{
				JsonArray res=new JsonArray();
				res.addAll(a1);
				res.addAll(a2);
				return res;
			};
		}

		@Override
		public Function<JsonArray, JsonArray> finisher(){
			return Function.identity();
		}

		@Override
		public Set<Characteristics> characteristics(){
			return Set.of(Characteristics.IDENTITY_FINISH);
		}
	};

	private final JsonArray arr=new JsonArray();

	public JsonArrayBuilder add(JsonElement el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(String el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(Number el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(boolean el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(JsonObjectBuilder el){
		arr.add(el.build());
		return this;
	}

	public JsonArrayBuilder add(JsonArrayBuilder el){
		arr.add(el.build());
		return this;
	}

	public JsonArray build(){
		return arr;
	}

	public int size(){
		return arr.size();
	}

	public boolean isEmpty(){
		return arr.isEmpty();
	}

	public static JsonArray fromCollection(Collection<?> list){
		if(list==null)
			return null;
		JsonArray arr=new JsonArray();
		for(Object o:list){
			switch(o){
				case null -> arr.add((JsonElement) null);
				case JsonElement el -> arr.add(el);
				case String s -> arr.add(s);
				case Number n -> arr.add(n);
				case Boolean b -> arr.add(b);
				default -> throw new UnsupportedOperationException("object has unsupported type: "+o.getClass());
			}
		}
		return arr;
	}
}
