package smithereen.model.board;

public enum BoardTopicsSortOrder{
	CREATED_ASC,
	CREATED_DESC,
	UPDATED_ASC,
	UPDATED_DESC;

	public BoardTopicsSortOrder getReverse(){
		return switch(this){
			case CREATED_ASC -> CREATED_DESC;
			case CREATED_DESC -> CREATED_ASC;
			case UPDATED_ASC -> UPDATED_DESC;
			case UPDATED_DESC -> UPDATED_ASC;
		};
	}
}
