package smithereen.model.util;

import java.net.URI;
import java.util.List;

import smithereen.model.Group;
import smithereen.model.User;

public record QuickSearchResults(List<User> users, List<Group> groups, List<URI> externalObjects){
}
