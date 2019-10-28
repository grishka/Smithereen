package smithereen.jtwigext;

import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.value.context.ValueContext;
import org.jtwig.value.convert.string.StringConverter;

import java.util.Locale;

import smithereen.lang.Lang;

public class LangPluralFunction extends SimpleJtwigFunction{
	// https://stackoverflow.com/questions/41614100/how-to-invoke-custom-functions-in-jtwig

	public LangPluralFunction(){
	}

	@Override
	public String name(){
		return "LP";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		Locale locale=(Locale)functionRequest.getRenderContext().getCurrent(ValueContext.class).resolve("locale");
		String key=(String) functionRequest.get(0);
		int quantity=(int) functionRequest.get(1);
		if(functionRequest.getNumberOfArguments()==1){
			return Lang.get(locale).plural(key, quantity);
		}else{
			String[] args=new String[functionRequest.getNumberOfArguments()-2];
			StringConverter conv=functionRequest.getEnvironment().getValueEnvironment().getStringConverter();
			for(int i=0;i<args.length;i++){
				args[i]=conv.convert(functionRequest.get(i+2));
			}
			return Lang.get(locale).plural(key, quantity, args);
		}
	}
}
