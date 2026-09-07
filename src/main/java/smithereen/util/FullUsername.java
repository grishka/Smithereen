package smithereen.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.IDN;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

public class FullUsername{
	@NotNull
	private final String username;

	@NotNull
	private final String domain;

	private FullUsername(@NotNull String username, @NotNull String domain){
		this.username=username;
		this.domain=domain;
	}

	@NotNull
	public String getUsername(){
		return username;
	}

	@NotNull
	public String getDomain(){
		return domain;
	}

	private String render(Function<String, String> domainRenderer){
		String res=username;
		if(!domain.isEmpty()){
			res+="@";
			res+=domainRenderer.apply(domain);
		}
		return res;
	}

	@NotNull
	public String percentEncoded(){
		return render(domain->URLEncoder.encode(UriRenderer.renderDomain(domain), StandardCharsets.UTF_8));
	}

	/**
	 * <p>The representation of the username suitable for displaying in the UI.</p>
	 */
	@NotNull
	public String humanReadable(){
		return render(UriRenderer::renderDomain);
	}

	@Override
	public boolean equals(Object o){
		if(!(o instanceof FullUsername that)) return false;
		return username.equalsIgnoreCase(that.username) && domain.equalsIgnoreCase(that.domain);
	}

	@Override
	public int hashCode(){
		return Objects.hash(username.toLowerCase(), domain.toLowerCase());
	}

	@Override
	public String toString(){
		return render(Function.identity());
	}

	@NotNull
	@Contract("_ -> new")
	public static FullUsername create(@NotNull String fullUsername){
		String realUsername;
		String domain="";
		int atIndex=fullUsername.indexOf('@');
		if(atIndex>=0){
			realUsername=fullUsername.substring(0, atIndex);
			domain=fullUsername.substring(atIndex+1);
		}else{
			realUsername=fullUsername;
		}
		try{
			domain=IDN.toASCII(domain);
		}catch(IllegalArgumentException ignored){
		}
		return new FullUsername(realUsername, domain);
	}
}
