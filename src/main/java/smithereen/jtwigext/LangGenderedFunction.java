package smithereen.jtwigext;

import org.jtwig.escape.EscapeEngine;
import org.jtwig.escape.NoneEscapeEngine;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.render.context.RenderContextHolder;
import org.jtwig.value.context.ValueContext;
import org.jtwig.value.convert.string.StringConverter;

import java.util.Locale;

import smithereen.data.User;
import smithereen.lang.Lang;

public class LangGenderedFunction extends SimpleJtwigFunction{
	// https://stackoverflow.com/questions/41614100/how-to-invoke-custom-functions-in-jtwig

	public LangGenderedFunction(){
	}

	@Override
	public String name(){
		return "LG";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		functionRequest.minimumNumberOfArguments(2);
		Locale locale=(Locale)functionRequest.getRenderContext().getCurrent(ValueContext.class).resolve("locale");
		String key=(String) functionRequest.get(0);
		User.Gender gender=(User.Gender) functionRequest.get(1);
		if(gender==null)
			gender=User.Gender.UNKNOWN;
		if(functionRequest.getNumberOfArguments()==2){
			RenderContextHolder.get().set(EscapeEngine.class, NoneEscapeEngine.instance());
			return Lang.get(locale).gendered(key, gender);
		}else{
			String[] args=new String[functionRequest.getNumberOfArguments()-2];
			StringConverter conv=functionRequest.getEnvironment().getValueEnvironment().getStringConverter();
			EscapeEngine escapeEngine=RenderContextHolder.get().getCurrent(EscapeEngine.class);
			for(int i=0;i<args.length;i++){
				args[i]=escapeEngine.escape(conv.convert(functionRequest.get(i+2)));
			}
			RenderContextHolder.get().set(EscapeEngine.class, NoneEscapeEngine.instance());
			return Lang.get(locale).gendered(key, gender, args);
		}
	}
}
