package smithereen;

import smithereen.controllers.WallController;

public class ApplicationContext{
	private final WallController wallController;

	public ApplicationContext(){
		wallController=new WallController(this);
	}

	public WallController getWallController(){
		return wallController;
	}
}
