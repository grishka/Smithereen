package smithereen.jtwigext;

import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.value.context.ValueContext;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import smithereen.lang.Lang;

public class LangDateFunction extends SimpleJtwigFunction{
	// https://stackoverflow.com/questions/41614100/how-to-invoke-custom-functions-in-jtwig

	public LangDateFunction(){
	}

	@Override
	public String name(){
		return "LD";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		Locale locale=(Locale)functionRequest.getRenderContext().getCurrent(ValueContext.class).resolve("locale");
		TimeZone tz=(TimeZone) functionRequest.getRenderContext().getCurrent(ValueContext.class).resolve("timeZone");
		Object arg=functionRequest.get(0);
		if(arg instanceof Date)
			return Lang.get(locale).formatDate((Date) functionRequest.get(0), tz);
		return "????";
	}
}
