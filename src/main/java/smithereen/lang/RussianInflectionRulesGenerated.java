package smithereen.lang;

import smithereen.model.User;

// Auto-generated, don't edit
// Сгенерировано из https://github.com/petrovich/petrovich-rules, не редактируйте
// Скрипт: /codegen/gen_inflection_rules.php
class RussianInflectionRulesGenerated{

	public static String inflectLastName(String input, User.Gender gender, Inflector.Case _case, boolean firstWord){
		if(_case==Inflector.Case.NOMINATIVE) return input;
		String inputLower=input.toLowerCase();
		if(firstWord && (inputLower.equals("бонч") || inputLower.equals("абдул") || inputLower.equals("белиц") || inputLower.equals("гасан") || inputLower.equals("дюссар") || inputLower.equals("дюмон") || inputLower.equals("книппер") || inputLower.equals("корвин") || inputLower.equals("ван") || inputLower.equals("шолом") || inputLower.equals("тер") || inputLower.equals("призван") || inputLower.equals("мелик") || inputLower.equals("вар") || inputLower.equals("фон"))){
			return input;
		}
		if(inputLower.equals("дюма") || inputLower.equals("тома") || inputLower.equals("дега") || inputLower.equals("люка") || inputLower.equals("ферма") || inputLower.equals("гамарра") || inputLower.equals("петипа") || inputLower.equals("шандра") || inputLower.equals("скаля") || inputLower.equals("каруана")){
			return input;
		}
		if(inputLower.equals("гусь") || inputLower.equals("ремень") || inputLower.equals("камень") || inputLower.equals("онук") || inputLower.equals("богода") || inputLower.equals("нечипас") || inputLower.equals("долгопалец") || inputLower.equals("маненок") || inputLower.equals("рева") || inputLower.equals("кива") || inputLower.equals("щёлок")){
			return input;
		}
		if(gender==User.Gender.MALE && (inputLower.equals("вий") || inputLower.equals("сой") || inputLower.equals("цой") || inputLower.equals("хой"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.equals("грин") || inputLower.equals("дарвин") || inputLower.equals("регин") || inputLower.equals("цин"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ом";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("б") || inputLower.endsWith("в") || inputLower.endsWith("г") || inputLower.endsWith("д") || inputLower.endsWith("ж") || inputLower.endsWith("з") || inputLower.endsWith("й") || inputLower.endsWith("к") || inputLower.endsWith("л") || inputLower.endsWith("м") || inputLower.endsWith("н") || inputLower.endsWith("п") || inputLower.endsWith("р") || inputLower.endsWith("с") || inputLower.endsWith("т") || inputLower.endsWith("ф") || inputLower.endsWith("х") || inputLower.endsWith("ц") || inputLower.endsWith("ч") || inputLower.endsWith("ш") || inputLower.endsWith("щ") || inputLower.endsWith("ъ") || inputLower.endsWith("ь"))){
			return input;
		}
		if(inputLower.endsWith("орота")){
			return input;
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("ска") || inputLower.endsWith("цка"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, PREPOSITIONAL, INSTRUMENTAL, DATIVE -> "ой";
				case ACCUSATIVE -> "ую";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("чая"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, PREPOSITIONAL, INSTRUMENTAL, DATIVE -> "ей";
				case ACCUSATIVE -> "ую";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("чий"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "его";
				case DATIVE -> "ему";
				case INSTRUMENTAL -> "им";
				case PREPOSITIONAL -> "ем";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("цкая") || inputLower.endsWith("ская") || inputLower.endsWith("ная") || inputLower.endsWith("ая"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, PREPOSITIONAL, INSTRUMENTAL, DATIVE -> "ой";
				case ACCUSATIVE -> "ую";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("яя"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, PREPOSITIONAL, INSTRUMENTAL, DATIVE -> "ей";
				case ACCUSATIVE -> "юю";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("иной") || inputLower.endsWith("уй"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("ца")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "ы";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("рих"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ом";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("ия")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, PREPOSITIONAL, DATIVE -> "и";
				case ACCUSATIVE -> "ю";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("иа") || inputLower.endsWith("аа") || inputLower.endsWith("оа") || inputLower.endsWith("уа") || inputLower.endsWith("ыа") || inputLower.endsWith("еа") || inputLower.endsWith("юа") || inputLower.endsWith("эа")){
			return input;
		}
		if(inputLower.endsWith("о") || inputLower.endsWith("е") || inputLower.endsWith("э") || inputLower.endsWith("и") || inputLower.endsWith("ы") || inputLower.endsWith("у") || inputLower.endsWith("ю")){
			return input;
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("их") || inputLower.endsWith("ых"))){
			return input;
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("ова") || inputLower.endsWith("ева") || inputLower.endsWith("на") || inputLower.endsWith("ёва"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, PREPOSITIONAL, INSTRUMENTAL, DATIVE -> "ой";
				case ACCUSATIVE -> "у";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("га") || inputLower.endsWith("ка") || inputLower.endsWith("ха") || inputLower.endsWith("ча") || inputLower.endsWith("ща") || inputLower.endsWith("жа") || inputLower.endsWith("ша")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ой";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("а")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "ы";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ой";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ь"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("я")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "ю";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("обей"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ья";
				case DATIVE -> "ью";
				case INSTRUMENTAL -> "ьем";
				case PREPOSITIONAL -> "ье";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ей"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ян") || inputLower.endsWith("ан") || inputLower.endsWith("йн"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ом";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ынец"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ца";
				case DATIVE -> "цу";
				case INSTRUMENTAL -> "цом";
				case PREPOSITIONAL -> "це";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("нец") || inputLower.endsWith("мец") || inputLower.endsWith("робец"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ца";
				case DATIVE -> "цу";
				case INSTRUMENTAL -> "цем";
				case PREPOSITIONAL -> "це";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ай"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("гой") || inputLower.endsWith("кой"))){
			return switch(_case){
				case GENITIVE, ACCUSATIVE -> input.substring(0, input.length()-1)+"го";
				case DATIVE -> input.substring(0, input.length()-1)+"му";
				case INSTRUMENTAL -> input.substring(0, input.length()-2)+"им";
				case PREPOSITIONAL -> input.substring(0, input.length()-1)+"м";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ой"))){
			return switch(_case){
				case GENITIVE, ACCUSATIVE -> input.substring(0, input.length()-1)+"го";
				case DATIVE -> input.substring(0, input.length()-1)+"му";
				case INSTRUMENTAL -> input.substring(0, input.length()-2)+"ым";
				case PREPOSITIONAL -> input.substring(0, input.length()-1)+"м";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ах") || inputLower.endsWith("ав") || inputLower.endsWith("ив") || inputLower.endsWith("шток"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ом";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ший") || inputLower.endsWith("щий") || inputLower.endsWith("жий") || inputLower.endsWith("ний"))){
			return switch(_case){
				case GENITIVE, ACCUSATIVE -> input.substring(0, input.length()-2)+"его";
				case DATIVE -> input.substring(0, input.length()-2)+"ему";
				case INSTRUMENTAL -> input.substring(0, input.length()-1)+"м";
				case PREPOSITIONAL -> input.substring(0, input.length()-2)+"ем";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ый") || inputLower.endsWith("кий") || inputLower.endsWith("хий"))){
			return switch(_case){
				case GENITIVE, ACCUSATIVE -> input.substring(0, input.length()-2)+"ого";
				case DATIVE -> input.substring(0, input.length()-2)+"ому";
				case INSTRUMENTAL -> input.substring(0, input.length()-1)+"м";
				case PREPOSITIONAL -> input.substring(0, input.length()-2)+"ом";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ий"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "и";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ок"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ка";
				case DATIVE -> "ку";
				case INSTRUMENTAL -> "ком";
				case PREPOSITIONAL -> "ке";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("обец") || inputLower.endsWith("швец") || inputLower.endsWith("ьвец"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("аец") || inputLower.endsWith("иец") || inputLower.endsWith("еец"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "йца";
				case DATIVE -> "йцу";
				case INSTRUMENTAL -> "йцем";
				case PREPOSITIONAL -> "йце";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("опец"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ца";
				case DATIVE -> "цу";
				case INSTRUMENTAL -> "цем";
				case PREPOSITIONAL -> "це";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("вец") || inputLower.endsWith("сец") || inputLower.endsWith("убец") || inputLower.endsWith("ырец"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ца";
				case DATIVE -> "цу";
				case INSTRUMENTAL -> "цом";
				case PREPOSITIONAL -> "це";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ц") || inputLower.endsWith("ч") || inputLower.endsWith("ш") || inputLower.endsWith("щ"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ен") || inputLower.endsWith("нн") || inputLower.endsWith("он") || inputLower.endsWith("ун") || inputLower.endsWith("б") || inputLower.endsWith("г") || inputLower.endsWith("д") || inputLower.endsWith("ж") || inputLower.endsWith("з") || inputLower.endsWith("к") || inputLower.endsWith("л") || inputLower.endsWith("м") || inputLower.endsWith("п") || inputLower.endsWith("р") || inputLower.endsWith("с") || inputLower.endsWith("т") || inputLower.endsWith("ф") || inputLower.endsWith("х"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ом";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("в") || inputLower.endsWith("н"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ым";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		return input;
	}

	public static String inflectFirstName(String input, User.Gender gender, Inflector.Case _case, boolean firstWord){
		if(_case==Inflector.Case.NOMINATIVE) return input;
		String inputLower=input.toLowerCase();
		if(gender==User.Gender.MALE && (inputLower.equals("лев"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ьва";
				case DATIVE -> "ьву";
				case INSTRUMENTAL -> "ьвом";
				case PREPOSITIONAL -> "ьве";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.equals("пётр"))){
			return input.substring(0, input.length()-3)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "етра";
				case DATIVE -> "етру";
				case INSTRUMENTAL -> "етром";
				case PREPOSITIONAL -> "етре";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.equals("павел"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ла";
				case DATIVE -> "лу";
				case INSTRUMENTAL -> "лом";
				case PREPOSITIONAL -> "ле";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.equals("яша"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.equals("илья"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "ю";
				case INSTRUMENTAL -> "ёй";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.equals("шота"))){
			return input;
		}
		if(gender==User.Gender.FEMALE && (inputLower.equals("агидель") || inputLower.equals("жизель") || inputLower.equals("нинель") || inputLower.equals("рашель") || inputLower.equals("рахиль"))){
			return switch(_case){
				case GENITIVE, PREPOSITIONAL, DATIVE -> input.substring(0, input.length()-1)+"и";
				case ACCUSATIVE -> input;
				case INSTRUMENTAL -> input+"ю";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("е") || inputLower.endsWith("ё") || inputLower.endsWith("и") || inputLower.endsWith("о") || inputLower.endsWith("у") || inputLower.endsWith("ы") || inputLower.endsWith("э") || inputLower.endsWith("ю")){
			return input;
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("уа") || inputLower.endsWith("иа"))){
			return input;
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("б") || inputLower.endsWith("в") || inputLower.endsWith("г") || inputLower.endsWith("д") || inputLower.endsWith("ж") || inputLower.endsWith("з") || inputLower.endsWith("й") || inputLower.endsWith("к") || inputLower.endsWith("л") || inputLower.endsWith("м") || inputLower.endsWith("н") || inputLower.endsWith("п") || inputLower.endsWith("р") || inputLower.endsWith("с") || inputLower.endsWith("т") || inputLower.endsWith("ф") || inputLower.endsWith("х") || inputLower.endsWith("ц") || inputLower.endsWith("ч") || inputLower.endsWith("ш") || inputLower.endsWith("щ") || inputLower.endsWith("ъ") || inputLower.endsWith("иа") || inputLower.endsWith("ль"))){
			return input;
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("ь"))){
			return switch(_case){
				case GENITIVE, PREPOSITIONAL, DATIVE -> input.substring(0, input.length()-1)+"и";
				case ACCUSATIVE -> input;
				case INSTRUMENTAL -> input+"ю";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ь"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("га") || inputLower.endsWith("ка") || inputLower.endsWith("ха") || inputLower.endsWith("ча") || inputLower.endsWith("ща") || inputLower.endsWith("жа")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ой";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("ша"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ша") || inputLower.endsWith("ча") || inputLower.endsWith("жа"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("а")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "ы";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ой";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("ка") || inputLower.endsWith("га") || inputLower.endsWith("ха"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ой";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("ца"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "ы";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("а"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "ы";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ой";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("ия"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, PREPOSITIONAL, DATIVE -> "и";
				case ACCUSATIVE -> "ю";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("я")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "и";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "ю";
				case INSTRUMENTAL -> "ей";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ий"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "и";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ей") || inputLower.endsWith("й"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "я";
				case DATIVE -> "ю";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("бек"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ка";
				case DATIVE -> "ку";
				case INSTRUMENTAL -> "ком";
				case PREPOSITIONAL -> "ке";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ек") || inputLower.endsWith("ёк"))){
			return input.substring(0, input.length()-2)+switch(_case){
				case GENITIVE, ACCUSATIVE -> "ька";
				case DATIVE -> "ьку";
				case INSTRUMENTAL -> "ьком";
				case PREPOSITIONAL -> "ьке";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ш") || inputLower.endsWith("ж"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("б") || inputLower.endsWith("в") || inputLower.endsWith("г") || inputLower.endsWith("д") || inputLower.endsWith("ж") || inputLower.endsWith("з") || inputLower.endsWith("к") || inputLower.endsWith("л") || inputLower.endsWith("м") || inputLower.endsWith("н") || inputLower.endsWith("п") || inputLower.endsWith("р") || inputLower.endsWith("с") || inputLower.endsWith("т") || inputLower.endsWith("ф") || inputLower.endsWith("х") || inputLower.endsWith("ц") || inputLower.endsWith("ч"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ом";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(inputLower.endsWith("ния") || inputLower.endsWith("рия") || inputLower.endsWith("вия")){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE, DATIVE -> "и";
				case ACCUSATIVE -> "ю";
				case INSTRUMENTAL, PREPOSITIONAL -> "ем";
				default -> throw new IllegalArgumentException();
			};
		}
		return input;
	}

	public static String inflectMiddleName(String input, User.Gender gender, Inflector.Case _case, boolean firstWord){
		if(_case==Inflector.Case.NOMINATIVE) return input;
		String inputLower=input.toLowerCase();
		if(firstWord && (inputLower.equals("борух"))){
			return input;
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("мич") || inputLower.endsWith("ьич") || inputLower.endsWith("кич"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ом";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.MALE && (inputLower.endsWith("ич"))){
			return input+switch(_case){
				case GENITIVE, ACCUSATIVE -> "а";
				case DATIVE -> "у";
				case INSTRUMENTAL -> "ем";
				case PREPOSITIONAL -> "е";
				default -> throw new IllegalArgumentException();
			};
		}
		if(gender==User.Gender.FEMALE && (inputLower.endsWith("на"))){
			return input.substring(0, input.length()-1)+switch(_case){
				case GENITIVE -> "ы";
				case DATIVE, PREPOSITIONAL -> "е";
				case ACCUSATIVE -> "у";
				case INSTRUMENTAL -> "ой";
				default -> throw new IllegalArgumentException();
			};
		}
		return input;
	}

	public static User.Gender genderForLastName(String input){
		input=input.toLowerCase();
		if(input.equals("бова") || input.equals("регин") || input.equals("дарвин") || input.equals("пэйлин") || input.equals("грин") || input.equals("цин") || input.equals("шенгелая"))
			return User.Gender.UNKNOWN;

		if(input.endsWith("ова") || input.endsWith("ая") || input.endsWith("ына") || input.endsWith("ина") || input.endsWith("ева") || input.endsWith("ска") || input.endsWith("ёва"))
			return User.Gender.FEMALE;

		if(input.endsWith("кий") || input.endsWith("ов") || input.endsWith("ын") || input.endsWith("ев") || input.endsWith("ин") || input.endsWith("ёв") || input.endsWith("хий") || input.endsWith("ний") || input.endsWith("ый") || input.endsWith("ой"))
			return User.Gender.MALE;

		return User.Gender.UNKNOWN;
	}

	public static User.Gender genderForFirstName(String input){
		input=input.toLowerCase();
		if(input.equals("сева") || input.equals("иона") || input.equals("муса") || input.equals("саша") || input.equals("алвард") || input.equals("валери") || input.equals("кири") || input.equals("анри") || input.equals("ким") || input.equals("райхон") || input.equals("закия") || input.equals("захария") || input.equals("женя") || input.equals("карен"))
			return User.Gender.UNKNOWN;

		if(input.equals("абиба") || input.equals("савва") || input.equals("лёва") || input.equals("вова") || input.equals("ага") || input.equals("ахмедага") || input.equals("алиага") || input.equals("амирага") || input.equals("агга") || input.equals("серега") || input.equals("фейга") || input.equals("гога") || input.equals("алиада") || input.equals("муктада") || input.equals("абида") || input.equals("алда") || input.equals("маджуда") || input.equals("нурлыхуда") || input.equals("гиа") || input.equals("элиа") || input.equals("гарсиа") || input.equals("вавила") || input.equals("гавриила") || input.equals("генка") || input.equals("лука") || input.equals("дима") || input.equals("зосима") || input.equals("тима") || input.equals("фима") || input.equals("фома") || input.equals("кузьма") || input.equals("жора") || input.equals("миша") || input.equals("ермила") || input.equals("данила") || input.equals("гаврила") || input.equals("абдалла") || input.equals("аталла") || input.equals("абдилла") || input.equals("атилла") || input.equals("кайролла") || input.equals("абулла") || input.equals("абула") || input.equals("свитлана") || input.equals("бена") || input.equals("гена") || input.equals("агелина") || input.equals("джанна") || input.equals("кришна") || input.equals("степа") || input.equals("дра") || input.equals("назера") || input.equals("валера") || input.equals("эстера") || input.equals("двойра") || input.equals("калистра") || input.equals("заратустра") || input.equals("юра") || input.equals("иса") || input.equals("аиса") || input.equals("халиса") || input.equals("холиса") || input.equals("валенса") || input.equals("мусса") || input.equals("ата") || input.equals("паата") || input.equals("алета") || input.equals("никита") || input.equals("мота") || input.equals("шота") || input.equals("фаста") || input.equals("коста") || input.equals("маритта") || input.equals("малюта") || input.equals("васюта") || input.equals("вафа") || input.equals("мустафа") || input.equals("ганифа") || input.equals("лев") || input.equals("яков") || input.equals("шелли") || input.equals("константин") || input.equals("марсель") || input.equals("рамиль") || input.equals("эмиль") || input.equals("бактыгуль") || input.equals("даниэль") || input.equals("игорь") || input.equals("рауль") || input.equals("поль") || input.equals("анхель") || input.equals("михель") || input.equals("мигель") || input.equals("микель") || input.equals("микаиль") || input.equals("микаель") || input.equals("михаэль") || input.equals("самаэль") || input.equals("лазарь") || input.equals("алесь") || input.equals("олесь") || input.equals("шамиль") || input.equals("рафаэль") || input.equals("джамаль") || input.equals("арминэ") || input.equals("изя") || input.equals("кузя") || input.equals("гия") || input.equals("мазия") || input.equals("кирикия") || input.equals("ркия") || input.equals("еркия") || input.equals("эркия") || input.equals("гулия") || input.equals("аксания") || input.equals("закария") || input.equals("зекерия") || input.equals("гарсия") || input.equals("шендля") || input.equals("филя") || input.equals("вилля") || input.equals("толя") || input.equals("ваня") || input.equals("саня") || input.equals("загиря") || input.equals("боря") || input.equals("цайся") || input.equals("вася") || input.equals("ося") || input.equals("петя") || input.equals("витя") || input.equals("митя") || input.equals("костя") || input.equals("алья") || input.equals("илья") || input.equals("ларья") || input.equals("артём"))
			return User.Gender.MALE;

		if(input.equals("судаба") || input.equals("сураба") || input.equals("любава") || input.equals("джанлука") || input.equals("варвара") || input.equals("наташа") || input.equals("зайнаб") || input.equals("любов") || input.equals("сольвейг") || input.equals("шакед") || input.equals("аннаид") || input.equals("ингрид") || input.equals("синди") || input.equals("аллаберди") || input.equals("сандали") || input.equals("лали") || input.equals("натали") || input.equals("гулькай") || input.equals("алтынай") || input.equals("гюнай") || input.equals("гюльчитай") || input.equals("нурангиз") || input.equals("лиз") || input.equals("элиз") || input.equals("ботагоз") || input.equals("юлдуз") || input.equals("диляфруз") || input.equals("габи") || input.equals("сажи") || input.equals("фанни") || input.equals("мери") || input.equals("элдари") || input.equals("эльдари") || input.equals("хилари") || input.equals("хиллари") || input.equals("аннемари") || input.equals("розмари") || input.equals("товсари") || input.equals("ансари") || input.equals("одри") || input.equals("тери") || input.equals("ири") || input.equals("катри") || input.equals("мэри") || input.equals("сатаней") || input.equals("ефтений") || input.equals("верунчик") || input.equals("гюзел") || input.equals("этел") || input.equals("рэйчел") || input.equals("джил") || input.equals("мерил") || input.equals("нинелл") || input.equals("бурул") || input.equals("ахлам") || input.equals("майрам") || input.equals("махаррам") || input.equals("мириам") || input.equals("дилярам") || input.equals("асем") || input.equals("мерьем") || input.equals("мирьем") || input.equals("эркаим") || input.equals("гулаим") || input.equals("айгерим") || input.equals("марьям") || input.equals("мирьям") || input.equals("эван") || input.equals("гульжиган") || input.equals("айдан") || input.equals("айжан") || input.equals("вивиан") || input.equals("гульжиан") || input.equals("лилиан") || input.equals("мариан") || input.equals("саиман") || input.equals("джоан") || input.equals("чулпан") || input.equals("лоран") || input.equals("моран") || input.equals("джохан") || input.equals("гульшан") || input.equals("аделин") || input.equals("жаклин") || input.equals("карин") || input.equals("каролин") || input.equals("каталин") || input.equals("катрин") || input.equals("керстин") || input.equals("кэтрин") || input.equals("мэрилин") || input.equals("рузалин") || input.equals("хелин") || input.equals("цеткин") || input.equals("ширин") || input.equals("элисон") || input.equals("дурсун") || input.equals("кристин") || input.equals("гульжиян") || input.equals("марьян") || input.equals("ренато") || input.equals("зейнеп") || input.equals("санабар") || input.equals("дильбар") || input.equals("гулизар") || input.equals("гульзар") || input.equals("пилар") || input.equals("дагмар") || input.equals("элинар") || input.equals("нилуфар") || input.equals("анхар") || input.equals("гаухар") || input.equals("естер") || input.equals("эстер") || input.equals("дженнифер") || input.equals("линор") || input.equals("элинор") || input.equals("элеонор") || input.equals("айнур") || input.equals("гульнур") || input.equals("шамсинур") || input.equals("элнур") || input.equals("ильсияр") || input.equals("нигяр") || input.equals("сигитас") || input.equals("агнес") || input.equals("анес") || input.equals("долорес") || input.equals("инес") || input.equals("анаис") || input.equals("таис") || input.equals("эллис") || input.equals("элис") || input.equals("кларис") || input.equals("амнерис") || input.equals("айрис") || input.equals("дорис") || input.equals("беатрис") || input.equals("грейс") || input.equals("грэйс") || input.equals("ботагос") || input.equals("маргос") || input.equals("джулианс") || input.equals("арус") || input.equals("диляфрус") || input.equals("саодат") || input.equals("зулхижат") || input.equals("хамат") || input.equals("патимат") || input.equals("хатимат") || input.equals("альжанат") || input.equals("маймунат") || input.equals("гульшат") || input.equals("биргит") || input.equals("рут") || input.equals("иргаш") || input.equals("айнаш") || input.equals("агнеш") || input.equals("зауреш") || input.equals("тэрбиш") || input.equals("ануш") || input.equals("азгануш") || input.equals("гаруш") || input.equals("николь") || input.equals("адась") || input.equals("любовь") || input.equals("руфь") || input.equals("ассоль") || input.equals("юдифь") || input.equals("гретель") || input.equals("греттель") || input.equals("адель") || input.equals("жизель") || input.equals("гузель") || input.equals("нинель") || input.equals("этель") || input.equals("асель") || input.equals("агарь") || input.equals("рахиль") || input.equals("фамарь") || input.equals("иаиль") || input.equals("есфирь") || input.equals("астинь") || input.equals("рапунцель") || input.equals("афиля") || input.equals("тафиля") || input.equals("фаня") || input.equals("аня"))
			return User.Gender.FEMALE;

		if(input.endsWith("улла"))
			return User.Gender.UNKNOWN;

		if(input.endsWith("аба") || input.endsWith("б") || input.endsWith("ав") || input.endsWith("ев") || input.endsWith("ов") || input.endsWith("г") || input.endsWith("д") || input.endsWith("ж") || input.endsWith("з") || input.endsWith("би") || input.endsWith("ди") || input.endsWith("жи") || input.endsWith("али") || input.endsWith("ри") || input.endsWith("ай") || input.endsWith("ей") || input.endsWith("ий") || input.endsWith("ой") || input.endsWith("ый") || input.endsWith("к") || input.endsWith("л") || input.endsWith("ам") || input.endsWith("ем") || input.endsWith("им") || input.endsWith("ом") || input.endsWith("ум") || input.endsWith("ым") || input.endsWith("ям") || input.endsWith("ан") || input.endsWith("ен") || input.endsWith("ин") || input.endsWith("сейн") || input.endsWith("он") || input.endsWith("ун") || input.endsWith("ян") || input.endsWith("ио") || input.endsWith("ло") || input.endsWith("ро") || input.endsWith("то") || input.endsWith("шо") || input.endsWith("п") || input.endsWith("ар") || input.endsWith("др") || input.endsWith("ер") || input.endsWith("ир") || input.endsWith("ор") || input.endsWith("тр") || input.endsWith("ур") || input.endsWith("ыр") || input.endsWith("яр") || input.endsWith("ас") || input.endsWith("ес") || input.endsWith("ис") || input.endsWith("йс") || input.endsWith("кс") || input.endsWith("мс") || input.endsWith("ос") || input.endsWith("нс") || input.endsWith("рс") || input.endsWith("ус") || input.endsWith("юс") || input.endsWith("яс") || input.endsWith("ат") || input.endsWith("мет") || input.endsWith("кт") || input.endsWith("нт") || input.endsWith("рт") || input.endsWith("ст") || input.endsWith("ут") || input.endsWith("ф") || input.endsWith("х") || input.endsWith("ш") || input.endsWith("ы") || input.endsWith("сь") || input.endsWith("емеля") || input.endsWith("коля"))
			return User.Gender.MALE;

		if(input.endsWith("иба") || input.endsWith("люба") || input.endsWith("лава") || input.endsWith("ева") || input.endsWith("га") || input.endsWith("да") || input.endsWith("еа") || input.endsWith("иза") || input.endsWith("иа") || input.endsWith("ика") || input.endsWith("нка") || input.endsWith("ска") || input.endsWith("ела") || input.endsWith("ила") || input.endsWith("лла") || input.endsWith("эла") || input.endsWith("има") || input.endsWith("на") || input.endsWith("ра") || input.endsWith("са") || input.endsWith("та") || input.endsWith("фа") || input.endsWith("елли") || input.endsWith("еса") || input.endsWith("сса") || input.endsWith("гуль") || input.endsWith("нуэль") || input.endsWith("гюль") || input.endsWith("нэ") || input.endsWith("ая") || input.endsWith("ея") || input.endsWith("ия") || input.endsWith("йя") || input.endsWith("ля") || input.endsWith("мя") || input.endsWith("оя") || input.endsWith("ря") || input.endsWith("ся") || input.endsWith("вья") || input.endsWith("лья") || input.endsWith("мья") || input.endsWith("нья") || input.endsWith("рья") || input.endsWith("сья") || input.endsWith("тья") || input.endsWith("фья") || input.endsWith("зя"))
			return User.Gender.FEMALE;

		return User.Gender.UNKNOWN;
	}

	public static User.Gender genderForMiddleName(String input){
		input=input.toLowerCase();
		if(input.endsWith("на") || input.endsWith("кызы") || input.endsWith("гызы"))
			return User.Gender.FEMALE;

		if(input.endsWith("ич") || input.endsWith("оглы") || input.endsWith("улы") || input.endsWith("уулу"))
			return User.Gender.MALE;

		return User.Gender.UNKNOWN;
	}

}