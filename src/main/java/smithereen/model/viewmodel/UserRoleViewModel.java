package smithereen.model.viewmodel;

import smithereen.model.admin.UserRole;

public record UserRoleViewModel(UserRole role, int numUsers, boolean canEdit){
}
