package io.intino.goros.space;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_ALWAYS;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_DAILY;

public class Shifter {
	private static final String MAVEN_URL = "https://repo1.maven.org/maven2/";
	private static final String INTINO_RELEASES = "https://artifactory.intino.io/artifactory/releases";
	private static final String INTINO_SNAPSHOTS = "https://artifactory.intino.io/artifactory/snapshot-libraries";

	public static final String GROUP_ID = "io.intino.goros.modernizing";

	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(file);
		NodeList nodeList = document.getElementsByTagName("platform");
		if (nodeList == null || nodeList.getLength() == 0) throw new Exception("Platform parameter not found");
		Node platform = nodeList.item(0);
		String artifactId = platform.getTextContent().substring(0, platform.getTextContent().indexOf("-"));
		String version = platform.getTextContent().substring(platform.getTextContent().indexOf("-") + 1);
		List<File> libraries = find(GROUP_ID, artifactId, version);
		execute(libraries, file);
	}

	private static void execute(List<File> libraries, File file) throws IOException {
		File logFile = new File(file.getParentFile(), "modernization.log");
		if (logFile.exists()) logFile.delete();
		List<String> commandParameters = new ArrayList<>();
		String javaBin = java.lang.System.getProperty("java.home") + separator + "bin" + separator + "java";
		commandParameters.add(javaBin);
		commandParameters.add("-Dfile.encoding=UTF-8");
		commandParameters.addAll(Arrays.asList("-jar", libraries.get(0).getAbsolutePath()));
		commandParameters.add(file.getAbsolutePath());
		try {
			new ProcessBuilder(commandParameters).redirectErrorStream(true).redirectOutput(logFile).redirectError(logFile).start().waitFor();
		} catch (InterruptedException ignored) {
		}
	}

	private static List<File> find(String groupId, String artifact, String version) throws DependencyResolutionException {
		var resolver = new MavenDependencyResolver(new File(System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository").getAbsolutePath(), artifactories());
		DependencyResult result = resolver.resolve(new DefaultArtifact(groupId, artifact, "jar", version), JavaScopes.COMPILE);
		List<Dependency> dependencies = MavenDependencyResolver.dependenciesFrom(result, false);
		return dependencies.stream().map(d -> d.getArtifact().getFile()).collect(Collectors.toList());
	}

	private static List<RemoteRepository> artifactories() {
		List<RemoteRepository> remotes = new ArrayList<>();
		remotes.add(MavenDependencyResolver.repository(INTINO_SNAPSHOTS, "intino-maven", true, UPDATE_POLICY_ALWAYS));
		remotes.add(MavenDependencyResolver.repository(INTINO_RELEASES, "intino-maven", false, UPDATE_POLICY_DAILY));
		remotes.add(MavenDependencyResolver.repository(MAVEN_URL, "maven-central", false, UPDATE_POLICY_DAILY));
		return remotes;
	}
}
