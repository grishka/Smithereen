package smithereen;

import java.io.Console;
import java.io.IOError;
import java.sql.SQLException;

import smithereen.model.Account;
import smithereen.model.User;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;

public class CLI{

	private static final char ESC='\u001b';
	private static final String E_RESET=ESC+"[0m";
	private static final String E_FG_RED=ESC+"[31m";
	private static final String E_FG_GREEN=ESC+"[32m";
	private static final String E_FG_YELLOW=ESC+"[33m";
	private static final String E_FG_BLUE=ESC+"[34m";
	private static final String E_FG_MAGENTA=ESC+"[35m";
	private static final String E_FG_CYAN=ESC+"[36m";
	private static final String E_FG_WHITE=ESC+"[37m";
	private static final String E_BOLD=ESC+"[1m";
	private static final String E_SILENT_INPUT=ESC+"[8m";

	public static void initializeAdmin(){
		try{
			Console c=System.console();
			if(c==null)
				throw new IllegalStateException("No console");

			int count=UserStorage.getLocalUserCount();
			if(count>0){
				System.err.println("There already are local users. This command is only intended to create the first user after the server is installed and the database is empty.");
				return;
			}

			System.out.println(E_FG_GREEN+E_BOLD+"Welcome to Smithereen!"+E_RESET+E_BOLD+" You will now answer a few questions to create the first user on your server."+E_RESET+"\n" +
					"This user will have administrative privileges.\n");

			String username, email, password;

			while(true){
				System.out.print("Username: ");
				username=c.readLine();
				if(!Utils.isValidUsername(username)){
					System.out.println("This username isn't valid. Usernames can only contain letters, numbers, . and _, and must be at least 2 characters long. Please try again.");
					continue;
				}
				if(Utils.isReservedUsername(username)){
					System.out.println("This username is reserved by the system. You can't use it. Please try again.");
					continue;
				}
				break;
			}
			while(true){
				System.out.print("E-mail: ");
				email=c.readLine();
				if(!Utils.isValidEmail(email)){
					System.out.println("This e-mail isn't valid. Please try again.");
					continue;
				}
				break;
			}
			while(true){
				System.out.print("Password: ");
				password=new String(c.readPassword());
				if(password.length()<4){
					System.out.println("This password is too short. Passwords must be at least 4 characters long. Please try again.");
					continue;
				}
				break;
			}
			while(true){
				System.out.print("Password again: ");
				String password2=new String(c.readPassword());
				if(!password.equals(password2)){
					System.out.println("Passwords don't match.");
					continue;
				}
				break;
			}
			SessionStorage.registerNewAccount(username, password, email, username, "", User.Gender.UNKNOWN);
			Account acc=SessionStorage.getAccountForUsernameAndPassword(username, password);
			UserStorage.setAccountAccessLevel(acc.id, Account.AccessLevel.ADMIN);
			System.out.println(E_BOLD+"You're all set! Now, make sure your web server is properly configured, then navigate to this server in your web browser and log into your account."+E_RESET);
		}catch(SQLException|IOError x){
			x.printStackTrace();
		}
	}
}
