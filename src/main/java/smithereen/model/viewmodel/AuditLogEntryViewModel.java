package smithereen.model.viewmodel;

import smithereen.model.admin.AuditLogEntry;

public record AuditLogEntryViewModel(AuditLogEntry entry, String mainTextHtml, String extraTextHtml){
}
