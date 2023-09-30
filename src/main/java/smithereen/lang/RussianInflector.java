package smithereen.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.model.User;

public class RussianInflector extends Inflector{
	@Override
	public boolean isInflectable(String str){
		return str.matches("^[а-яА-ЯёЁ -]+$");
	}

	@Override
	public String inflectNamePart(String namePart, NamePart which, User.Gender gender, Case lCase){
		namePart=namePart.trim();
		if(!namePart.contains(" ") && !namePart.contains("-")){
			switch(which){
				case FIRST:
					return RussianInflectionRulesGenerated.inflectFirstName(namePart, gender, lCase, false);
				case LAST:
					return RussianInflectionRulesGenerated.inflectLastName(namePart, gender, lCase, false);
				case MIDDLE:
					return RussianInflectionRulesGenerated.inflectMiddleName(namePart, gender, lCase, false);
			}
		}
		// Двойные фамилии и вот это всё
		Matcher matcher=Pattern.compile("[а-яА-ЯёЁ]+").matcher(namePart);
		StringBuffer sb=new StringBuffer();
		boolean first=true;
		while(matcher.find()){
			switch(which){
				case FIRST:
					matcher.appendReplacement(sb, RussianInflectionRulesGenerated.inflectFirstName(matcher.group(), gender, lCase, first));
					break;
				case LAST:
					matcher.appendReplacement(sb, RussianInflectionRulesGenerated.inflectLastName(matcher.group(), gender, lCase, first));
					break;
				case MIDDLE:
					matcher.appendReplacement(sb, RussianInflectionRulesGenerated.inflectMiddleName(matcher.group(), gender, lCase, first));
					break;
			}
			first=false;
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	@Override
	public User.Gender detectGender(String namePart, NamePart which){
		switch(which){
			case FIRST:
				return RussianInflectionRulesGenerated.genderForFirstName(namePart);
			case LAST:
				return RussianInflectionRulesGenerated.genderForLastName(namePart);
			case MIDDLE:
				return RussianInflectionRulesGenerated.genderForMiddleName(namePart);
			default:
				throw new IllegalArgumentException();
		}
	}
}
