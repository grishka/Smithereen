package smithereen.activitypub.objects;

import java.util.Set;

public interface ForeignActor{
	Set<String> WEBSITE_FIELD_KEYS=Set.of("website", "web", "web site", "blog", "homepage", "www", "site", "personal page", "personal website", "personal blog");
	Set<String> GENDER_FIELD_KEYS=Set.of(
			"pronoun",
			"pronouns",
			"gender",
			"sex",
			"пол",
			"гендер",
			"местоимения",
			"пол / sex",
			"sex / пол"
	);
	Set<String> GENDER_FIELD_MALE_VALUES=Set.of(
			"he",
			"he/him",
			"male",
			"мужской",
			"он",
			"он/его"
	);
	Set<String> GENDER_FIELD_FEMALE_VALUES=Set.of(
			"she",
			"she/her",
			"female",
			"женский",
			"она",
			"она/ее",
			"она/её"
	);
	Set<String> GENDER_FIELD_OTHER_VALUES=Set.of(
			"they/them",
			"nonbinary",
			"non-binary",
			"it"
	);

	boolean needUpdate();
}
