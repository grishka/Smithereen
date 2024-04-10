package smithereen.model.viewmodel;

import smithereen.model.UserRole;

public record UserRoleViewModel(UserRole role, int numUsers, boolean canEdit){
}
