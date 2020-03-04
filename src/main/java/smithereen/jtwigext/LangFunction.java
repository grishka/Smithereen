package smithereen.jtwigext;

import org.jtwig.escape.EscapeEngine;
import org.jtwig.escape.NoneEscapeEngine;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.render.context.RenderContext;
import org.jtwig.render.context.RenderContextHolder;
import org.jtwig.value.context.ValueContext;
import org.jtwig.value.convert.string.StringConverter;

import java.util.Locale;

import smithereen.lang.Lang;

public class LangFunction extends SimpleJtwigFunction{
	// https://stackoverflow.com/questions/41614100/how-to-invoke-custom-functions-in-jtwig

	public LangFunction(){
	}

	@Override
	public String name(){
		return "L";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		Locale locale=(Locale)functionRequest.getRenderContext().getCurrent(ValueContext.class).resolve("locale");
		String key=(String) functionRequest.get(0);
		if(functionRequest.getNumberOfArguments()==1){
			RenderContextHolder.get().set(EscapeEngine.class, NoneEscapeEngine.instance());
			return Lang.get(locale).get(key);
		}else{
			String[] args=new String[functionRequest.getNumberOfArguments()-1];
			StringConverter conv=functionRequest.getEnvironment().getValueEnvironment().getStringConverter();
			EscapeEngine escapeEngine=RenderContextHolder.get().getCurrent(EscapeEngine.class);
			for(int i=0;i<args.length;i++){
				args[i]=escapeEngine.escape(conv.convert(functionRequest.get(i+1)));
			}
			RenderContextHolder.get().set(EscapeEngine.class, NoneEscapeEngine.instance());
			return Lang.get(locale).get(key, args);
		}
	}
}
