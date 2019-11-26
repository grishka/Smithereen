package smithereen.jsonld;

import org.json.JSONObject;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import smithereen.Utils;

public class URDNA2015{

	public static List<RDFTriple> normalize(List<RDFTriple> input){
		return new NormalizationState().normalize(input);
	}

	public static String canonicalize(JSONObject json, URI baseURI){
		List<RDFTriple> norm=normalize(JLDDocument.toRDF(json, baseURI));
		ArrayList<String> lines=new ArrayList<>(norm.size());
		for(RDFTriple t:norm)
			lines.add(t.toString());
		Collections.sort(lines);
		StringBuilder sb=new StringBuilder();
		for(String line:lines){
			sb.append(line);
			sb.append('\n');
		}
		return sb.toString();
	}

	private static class NormalizationState{
		public HashMap<String, ArrayList<RDFTriple>> blankNodeToQuadsMap=new HashMap<>();
		public HashMap<String, ArrayList<String>> hashToBlankNodesMap=new HashMap<>();
		public BlankNodeIssuer canonicalIssuer=new BlankNodeIssuer("_:c14n");

		private MessageDigest sha256;

		{
			try{
				sha256=MessageDigest.getInstance("SHA-256");
			}catch(NoSuchAlgorithmException ignore){}
		}

		public List<RDFTriple> normalize(List<RDFTriple> input){
			for(RDFTriple quad:input){
				if(quad.object instanceof String){
					String bnode=(String)quad.object;
					if(!blankNodeToQuadsMap.containsKey(bnode))
						blankNodeToQuadsMap.put(bnode, new ArrayList<>());
					blankNodeToQuadsMap.get(bnode).add(quad);
				}
				if(quad.subject instanceof String){
					String bnode=(String)quad.subject;
					if(!blankNodeToQuadsMap.containsKey(bnode))
						blankNodeToQuadsMap.put(bnode, new ArrayList<>());
					blankNodeToQuadsMap.get(bnode).add(quad);
				}
				if(quad.graphName instanceof String){
					String bnode=(String)quad.graphName;
					if(!blankNodeToQuadsMap.containsKey(bnode))
						blankNodeToQuadsMap.put(bnode, new ArrayList<>());
					blankNodeToQuadsMap.get(bnode).add(quad);
				}
			}

			ArrayList<String> nonNormalizedIdentifiers=new ArrayList<>(blankNodeToQuadsMap.keySet());
			boolean simple=true;
			while(simple){
				simple=false;
				hashToBlankNodesMap.clear();
				for(String identifier:nonNormalizedIdentifiers){
					String hash=hashFirstDegreeQuads(identifier);
					if(!hashToBlankNodesMap.containsKey(hash))
						hashToBlankNodesMap.put(hash, new ArrayList<>());
					hashToBlankNodesMap.get(hash).add(identifier);
				}
				ArrayList<String> hashes=new ArrayList<>(hashToBlankNodesMap.keySet());
				Collections.sort(hashes);
				for(String hash:hashes){
					ArrayList<String> idList=hashToBlankNodesMap.get(hash);
					if(idList.size()>1)
						continue;
					canonicalIssuer.issue(idList.get(0));
					nonNormalizedIdentifiers.remove(idList.get(0));
					hashToBlankNodesMap.remove(hash);
					simple=true;
				}
			}
			ArrayList<String> hashes=new ArrayList<>(hashToBlankNodesMap.keySet());
			Collections.sort(hashes);
			for(String hash:hashes){
				ArrayList<String> idList=hashToBlankNodesMap.get(hash);
				ArrayList<HashNResult> hashPathList=new ArrayList<>();
				for(String id:idList){
					if(canonicalIssuer.issuedIdentifiersMap.containsKey(id))
						continue;
					BlankNodeIssuer tempIssuer=new BlankNodeIssuer("_:b");
					tempIssuer.issue(id);
					hashPathList.add(hashNDegreeQuads(id, tempIssuer));
				}
				hashPathList.sort(new Comparator<HashNResult>(){
					@Override
					public int compare(HashNResult o1, HashNResult o2){
						return o1.hash.compareTo(o2.hash);
					}
				});
				for(HashNResult result:hashPathList){
					for(String existingID:result.issuer.issuedIdentifiersList){
						canonicalIssuer.issue(existingID);
					}
				}
			}

			ArrayList<RDFTriple> normalized=new ArrayList<>();
			for(RDFTriple quad:input){
				RDFTriple quadCopy=new RDFTriple(quad);
				if(quadCopy.subject instanceof String)
					quadCopy.subject=canonicalIssuer.issue((String)quadCopy.subject);
				if(quadCopy.object instanceof String)
					quadCopy.object=canonicalIssuer.issue((String)quadCopy.object);
				if(quadCopy.graphName instanceof String)
					quadCopy.graphName=canonicalIssuer.issue((String)quadCopy.graphName);
				normalized.add(quadCopy);
			}

			return normalized;
		}

		private String hashFirstDegreeQuads(String refBlankNodeID){
			ArrayList<String> nquads=new ArrayList<>();
			ArrayList<RDFTriple> quads=blankNodeToQuadsMap.get(refBlankNodeID);
			for(RDFTriple quad:quads){
				RDFTriple _quad=quad;
				if(quad.object instanceof String){
					String bnode=(String)quad.object;
					_quad=new RDFTriple(quad);
					if(bnode.equals(refBlankNodeID))
						_quad.object="_:a";
					else
						_quad.object="_:z";
				}
				if(quad.subject instanceof String){
					String bnode=(String)quad.subject;
					if(_quad==quad)
						_quad=new RDFTriple(quad);
					if(bnode.equals(refBlankNodeID))
						_quad.subject="_:a";
					else
						_quad.subject="_:z";
				}
				if(quad.graphName instanceof String){
					String bnode=(String)quad.graphName;
					if(_quad==quad)
						_quad=new RDFTriple(quad);
					if(bnode.equals(refBlankNodeID))
						_quad.graphName="_:a";
					else
						_quad.graphName="_:z";
				}
				String serialized=_quad.toString();
				nquads.add(serialized);
			}
			Collections.sort(nquads);
			return hash(String.join("\n", nquads)+'\n');
		}

		private <T> List<List<T>> listPermutations(List<T> list) {
			if (list.size() == 0) {
				List<List<T>> result = new ArrayList<>();
				result.add(new ArrayList<T>());
				return result;
			}
			List<List<T>> returnMe = new ArrayList<>();
			T firstElement = list.remove(0);

			List<List<T>> recursiveReturn = listPermutations(list);
			for (List<T> li : recursiveReturn) {
				for (int index = 0; index <= li.size(); index++) {
					List<T> temp = new ArrayList<>(li);
					temp.add(index, firstElement);
					returnMe.add(temp);
				}

			}
			return returnMe;
		}

		private HashNResult hashNDegreeQuads(String identifier, BlankNodeIssuer issuer){
			HashMap<String, ArrayList<String>> hashToRelatedBlankNodesMap=new HashMap<>();
			ArrayList<RDFTriple> quads=blankNodeToQuadsMap.get(identifier);
			for(RDFTriple quad:quads){
				if(quad.subject instanceof String){
					String bnode=(String)quad.subject;
					if(!bnode.equals(identifier)){
						String hash=hashRelatedBlankNode(bnode, quad, issuer, "s");
						if(!hashToRelatedBlankNodesMap.containsKey(hash))
							hashToRelatedBlankNodesMap.put(hash, new ArrayList<>());
						hashToRelatedBlankNodesMap.get(hash).add(bnode);
					}
				}
				if(quad.object instanceof String){
					String bnode=(String)quad.object;
					if(!bnode.equals(identifier)){
						String hash=hashRelatedBlankNode(bnode, quad, issuer, "o");
						if(!hashToRelatedBlankNodesMap.containsKey(hash))
							hashToRelatedBlankNodesMap.put(hash, new ArrayList<>());
						hashToRelatedBlankNodesMap.get(hash).add(bnode);
					}
				}
				if(quad.graphName instanceof String){
					String bnode=(String)quad.graphName;
					if(!bnode.equals(identifier)){
						String hash=hashRelatedBlankNode(bnode, quad, issuer, "g");
						if(!hashToRelatedBlankNodesMap.containsKey(hash))
							hashToRelatedBlankNodesMap.put(hash, new ArrayList<>());
						hashToRelatedBlankNodesMap.get(hash).add(bnode);
					}
				}
			}
			StringBuilder dataToHash=new StringBuilder();
			ArrayList<String> relatedHashes=new ArrayList<>(hashToRelatedBlankNodesMap.keySet());
			Collections.sort(relatedHashes);
			for(String relatedHash:relatedHashes){
				ArrayList<String> blankNodeList=hashToRelatedBlankNodesMap.get(relatedHash);
				dataToHash.append(relatedHash);
				List<List<String>> permutations=listPermutations(blankNodeList);
				BlankNodeIssuer chosenIssuer=null;
				String chosenPath="";

				permutationLoop:
				for(List<String> permutation:permutations){
					BlankNodeIssuer issuerCopy=new BlankNodeIssuer(issuer);
					StringBuilder path=new StringBuilder();
					ArrayList<String> recursionList=new ArrayList<>();
					for(String related:permutation){
						if(canonicalIssuer.issuedIdentifiersMap.containsKey(related)){
							path.append(canonicalIssuer.issuedIdentifiersMap.get(related));
						}else{
							if(!issuerCopy.issuedIdentifiersMap.containsKey(related))
								recursionList.add(related);
							path.append(issuerCopy.issue(related));
						}
						if(!chosenPath.isEmpty() && path.length()>=chosenPath.length() && path.toString().compareTo(chosenPath)>0)
							continue permutationLoop;
					}
					for(String related:recursionList){
						HashNResult result=hashNDegreeQuads(related, issuerCopy);
						path.append(issuerCopy.issue(related));
						path.append('<');
						path.append(result.hash);
						path.append('>');
						issuerCopy=result.issuer;
						if(!chosenPath.isEmpty() && path.length()>=chosenPath.length() && path.toString().compareTo(chosenPath)>0)
							continue permutationLoop;
					}
					if(chosenPath.isEmpty() || path.toString().compareTo(chosenPath)<0){
						chosenPath=path.toString();
						chosenIssuer=issuerCopy;
					}
				}
				dataToHash.append(chosenPath);
				issuer=chosenIssuer;
			}
			HashNResult result=new HashNResult();
			result.hash=hash(dataToHash.toString());
			result.issuer=issuer;
			return result;
		}

		private String hashRelatedBlankNode(String related, RDFTriple quad, BlankNodeIssuer issuer, String position){
			String identifier;
			if(canonicalIssuer.issuedIdentifiersMap.containsKey(related))
				identifier=canonicalIssuer.issuedIdentifiersMap.get(related);
			else if(issuer.issuedIdentifiersMap.containsKey(related))
				identifier=issuer.issuedIdentifiersMap.get(related);
			else
				identifier=hashFirstDegreeQuads(related);
			StringBuilder input=new StringBuilder(position);
			if(!"g".equals(position)){
				input.append('<');
				input.append(quad.predicate);
				input.append('>');
			}
			input.append(identifier);
			return hash(input.toString());
		}

		private String hash(String src){
			return Utils.byteArrayToHexString(sha256.digest(src.getBytes(StandardCharsets.UTF_8)));
		}

		private static class HashNResult{
			public String hash;
			public BlankNodeIssuer issuer;
		}
	}

	private static class BlankNodeIssuer{
		public String identifierPrefix;
		public int identifierCounter=0;
		public HashMap<String, String> issuedIdentifiersMap=new HashMap<>();
		public ArrayList<String> issuedIdentifiersList=new ArrayList<>();

		public BlankNodeIssuer(String identifierPrefix){
			this.identifierPrefix=identifierPrefix;
		}

		public BlankNodeIssuer(BlankNodeIssuer other){
			identifierPrefix=other.identifierPrefix;
			identifierCounter=other.identifierCounter;
			issuedIdentifiersMap.putAll(other.issuedIdentifiersMap);
			issuedIdentifiersList.addAll(other.issuedIdentifiersList);
		}

		public String issue(String existingID){
			if(issuedIdentifiersMap.containsKey(existingID))
				return issuedIdentifiersMap.get(existingID);
			String issuedID=identifierPrefix+identifierCounter;
			issuedIdentifiersList.add(existingID);
			issuedIdentifiersMap.put(existingID, issuedID);
			identifierCounter++;
			return issuedID;
		}
	}
}
