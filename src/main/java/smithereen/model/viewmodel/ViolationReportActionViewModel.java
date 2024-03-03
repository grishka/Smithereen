package smithereen.model.viewmodel;

import smithereen.model.ViolationReportAction;

public record ViolationReportActionViewModel(ViolationReportAction action, String mainTextHtml, String extraTextHtml){
}
