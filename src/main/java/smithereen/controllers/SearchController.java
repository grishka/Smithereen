package smithereen.controllers;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.SearchResult;
import smithereen.model.User;
import smithereen.model.util.QuickSearchResults;
import smithereen.storage.GroupStorage;
import smithereen.storage.SearchStorage;
import smithereen.storage.UserStorage;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SearchController{
	private final ApplicationContext context;

	public SearchController(ApplicationContext context){
		this.context=context;
	}

	public QuickSearchResults quickSearch(String query, User self){
		try{
			if(StringUtils.isEmpty(query) || query.length()<2)
				return new QuickSearchResults(List.of(), List.of(), List.of());

			List<User> users=List.of();
			List<Group> groups=List.of();
			List<URI> externalObjects=List.of();
			if(isURL(query)){
				if(!query.startsWith("http:") && !query.startsWith("https:"))
					query="https://"+query;
				query=normalizeURLDomain(query);
				URI uri=URI.create(query);
				try{
					ActivityPubObject obj=context.getObjectLinkResolver().resolve(uri, ActivityPubObject.class, false, false, false);
					if(obj instanceof User u){
						users=List.of(u);
					}else if(obj instanceof Group g){
						groups=List.of(g);
					}else{
						externalObjects=List.of(uri);
					}
				}catch(ObjectNotFoundException x){
					if(!Config.isLocal(uri)){
						try{
							Actor actor=context.getObjectLinkResolver().resolve(uri, Actor.class, false, false, false);
							if(actor instanceof User u){
								users=List.of(u);
							}else if(actor instanceof Group g){
								groups=List.of(g);
							}else{
								throw new AssertionError();
							}
						}catch(ObjectNotFoundException|IllegalStateException xx){
							externalObjects=List.of(uri);
						}
					}
				}
			}else if(isUsernameAndDomain(query)){
				Matcher matcher=TextProcessor.USERNAME_DOMAIN_PATTERN.matcher(query);
				matcher.find();
				String username=matcher.group(1);
				String domain=matcher.group(2);
				String full=username;
				if(domain!=null)
					full+='@'+domain;
				User user=UserStorage.getByUsername(full);
				SearchResult sr;
				if(user!=null){
					users=List.of(user);
				}else{
					Group group=GroupStorage.getByUsername(full);
					if(group!=null){
						groups=List.of(group);
					}else{
						externalObjects=List.of(URI.create(full));
					}
				}
			}else{
				List<SearchResult> results=SearchStorage.search(query, self.id, 10);
				users=new ArrayList<>();
				groups=new ArrayList<>();
				for(SearchResult result:results){
					switch(result.type){
						case USER -> users.add(result.user);
						case GROUP -> groups.add(result.group);
					}
				}
			}
			return new QuickSearchResults(users, groups, externalObjects);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> searchUsers(String query, User self, int count){
		try{
			return UserStorage.getByIdAsList(SearchStorage.searchUsers(query, self.id, count));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> searchFriends(String query, User self, int offset, int count){
		try{
			PaginatedList<Integer> ids=SearchStorage.searchFriends(query, self.id, offset, count);
			return new PaginatedList<>(ids, UserStorage.getByIdAsList(ids.list));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Group> searchGroups(User self, String query, boolean events, User target, int offset, int count){
		try{
			PaginatedList<Integer> ids=SearchStorage.searchGroups(query, events, target.id, offset, count, self!=null && self.id==target.id);
			return new PaginatedList<>(ids, GroupStorage.getByIdAsList(ids.list));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
