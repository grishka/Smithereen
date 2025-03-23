package smithereen.model.fasp.responses;

import java.net.URI;
import java.util.List;

import smithereen.util.validation.AllFieldsAreRequired;
import smithereen.util.validation.NonEmpty;
import smithereen.util.validation.RequiredField;

public record FASPProviderInfoResponse(@RequiredField String name, List<PrivacyPolicyLink> privacyPolicy, @RequiredField @NonEmpty List<VersionedCapability> capabilities, String signInUrl, String contactEmail, String fediverseAccount){
	public record PrivacyPolicyLink(String language, URI url){}
	@AllFieldsAreRequired
	public record VersionedCapability(String id, String version){}
}
