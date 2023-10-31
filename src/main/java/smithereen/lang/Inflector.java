package smithereen.lang;

import org.jetbrains.annotations.Nullable;

import smithereen.model.User;

public abstract class Inflector{
	public abstract boolean isInflectable(String str);
	public abstract String inflectNamePart(String namePart, NamePart which, User.Gender gender, Case lCase);
	public abstract User.Gender detectGender(String namePart, NamePart which);

	public User.Gender detectGender(@Nullable String first, @Nullable String last, @Nullable String middle){
		if(first!=null){
			User.Gender gender=detectGender(first, NamePart.FIRST);
			if(gender!=User.Gender.UNKNOWN)
				return gender;
		}
		if(last!=null){
			User.Gender gender=detectGender(last, NamePart.LAST);
			if(gender!=User.Gender.UNKNOWN)
				return gender;
		}
		if(middle!=null){
			User.Gender gender=detectGender(middle, NamePart.MIDDLE);
			if(gender!=User.Gender.UNKNOWN)
				return gender;
		}
		return User.Gender.UNKNOWN;
	}

	public enum NamePart{
		FIRST,
		LAST,
		MIDDLE
	}

	public enum Case{
		NOMINATIVE,
		GENITIVE,
		DATIVE,
		ACCUSATIVE,
		INSTRUMENTAL,
		PREPOSITIONAL;
	}
}
