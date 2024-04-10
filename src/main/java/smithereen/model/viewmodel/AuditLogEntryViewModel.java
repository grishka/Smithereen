package smithereen.model.viewmodel;

import smithereen.model.AuditLogEntry;

public record AuditLogEntryViewModel(AuditLogEntry entry, String mainTextHtml, String extraTextHtml){
}
