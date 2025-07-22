package smithereen.model.viewmodel;

import smithereen.model.admin.ViolationReportAction;

public record ViolationReportActionViewModel(ViolationReportAction action, String mainTextHtml, String extraTextHtml){
}
