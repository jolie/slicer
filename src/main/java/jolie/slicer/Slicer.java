
package jolie.slicer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import jolie.lang.parse.ast.EmbedServiceNode;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ServiceNode;

/**
 * Slicer
 */
public class Slicer {
	final Program program;
	final Path configPath;
	final Path outputDirectory;
	final JSONObject config;
	final DependenciesResolver dependenciesResolver;
	Map< String, Program > slices = null;

	static final String DOCKERFILE_FILENAME = "Dockerfile";
	static final String DOCKERCOMPOSE_FILENAME = "docker-compose.yml";

	private Slicer( Program p, Path configPath, Path outputDirectory )
		throws FileNotFoundException, InvalidConfigurationFileException {
		this.program = p;
		this.configPath = configPath;
		this.outputDirectory = outputDirectory;
		Object o = JSONValue.parse( new FileReader( configPath.toFile() ) );
		if( !(o instanceof JSONObject) ) {
			String msg = "Top level definition must be a json object";
			throw new InvalidConfigurationFileException( msg );
		}
		this.config = (JSONObject) o;
		p.children()
			.stream()
			.filter( ServiceNode.class::isInstance )
			.map( ServiceNode.class::cast )
			.forEach( Slicer::removeAutogeneratedInputPorts );
		this.dependenciesResolver = new DependenciesResolver( p );
	}

	public static Slicer create( Program p, Path configPath, Path outputDirectory )
		throws FileNotFoundException, InvalidConfigurationFileException {
		Slicer slicer = new Slicer( p, configPath, outputDirectory );
		slicer.sliceProgram();
		return slicer;
	}

	public void validateConfigurationFile( Map< String, ServiceNode > declaredServices )
		throws InvalidConfigurationFileException {
		final StringBuilder msg = new StringBuilder();
		final Path programPath = Paths.get( program.context().sourceName() ).getFileName();

		final Set< String > undeclaredServices = new HashSet<>( config.keySet() );
		undeclaredServices.removeAll( declaredServices.entrySet() );
		if( !undeclaredServices.isEmpty() ) {
			for( String service : undeclaredServices ) {
				msg.append( "Service " )
					.append( service )
					.append( " in " )
					.append( configPath.getFileName() )
					.append( " is not declared in program " )
					.append( programPath )
					.append( System.lineSeparator() );
			}
		}

		ArrayList< String > servicesWithoutParameter = declaredServices.entrySet().stream()
			.filter( e -> config.containsKey( e.getKey() ) && e.getValue().parameterConfiguration().isPresent() )
			.map( e -> e.getValue().name() )
			.collect( Collectors.toCollection( ArrayList::new ) );
		if( !servicesWithoutParameter.isEmpty() ) {

			for( String service : servicesWithoutParameter ) {
				msg.append( "Service " + service + " in " + programPath ).append( " does not declare a parameter" )
					.append( System.lineSeparator() );
			}
		}
		if( !msg.toString().isEmpty() ) {
			throw new InvalidConfigurationFileException( msg.toString() );
		}
	}

	private void sliceProgram() {
		/* Slices only the services mentioned in the config */
		slices = new HashMap<>();
		program.children()
			.stream()
			.filter( ServiceNode.class::isInstance )
			.map( ServiceNode.class::cast )
			// Slice only services that are present in the configuration
			.filter( s -> config.containsKey( s.name() ) )
			.forEach( s -> {
				// Sort dependencies by their line to preserve the ordering given by the programmer
				List< OLSyntaxNode > newProgram =
					dependenciesResolver.getServiceDependencies( s )
						.stream()
						.sorted( Comparator.comparing( dep -> dep.context().line() ) )
						.collect( Collectors.toList() );
				newProgram.add( s );
				slices.put( s.name(), new Program( program.context(), newProgram ) );
			} );
	}

	private static void removeAutogeneratedInputPorts( ServiceNode service ) {
		ArrayList< OLSyntaxNode > toBeRemoved = service.program()
			.children()
			.stream()
			.filter( EmbedServiceNode.class::isInstance )
			.map( EmbedServiceNode.class::cast )
			.filter( EmbedServiceNode::isNewPort )
			.map( EmbedServiceNode::bindingPort )
			.collect( ArrayList::new, ArrayList::add, ArrayList::addAll );
		service.program().children().removeAll( toBeRemoved );
	}

	public void generateServiceDirectories()
		throws IOException {
		Files.createDirectories( outputDirectory );
		for( Map.Entry< String, Program > service : slices.entrySet() ) {
			JoliePrettyPrinter pp = new JoliePrettyPrinter();
			// Create Service Directory
			Path serviceDir = outputDirectory.resolve( service.getKey().toLowerCase() );
			Files.createDirectories( serviceDir );
			// Copy configuration file
			Path newConfigPath = serviceDir.resolve( configPath.getFileName() );
			Files.copy( configPath, newConfigPath, StandardCopyOption.REPLACE_EXISTING );
			// Output Jolie
			Path jolieFilePath = serviceDir.resolve( service.getKey() + ".ol" );
			try( OutputStream os =
				Files.newOutputStream( jolieFilePath, CREATE, TRUNCATE_EXISTING, WRITE ) ) {
				pp.visit( service.getValue() );
				os.write( pp.toString().getBytes() );
			}
			// Output Dockerfile
			try( OutputStream os =
				Files.newOutputStream( serviceDir.resolve( DOCKERFILE_FILENAME ),
					CREATE, TRUNCATE_EXISTING, WRITE ) ) {
				String dfString = String.format(
					"FROM jolielang/jolie%n"
						+ "COPY %1$s .%n"
						+ "COPY %2$s .%n"
						+ "CMD [\"jolie\", \"--params\", \"%2$s\", \"%1$s\"]",
					jolieFilePath.getFileName(),
					configPath.getFileName() );
				os.write( dfString.getBytes() );
			}
		}
		// Output docker-compose
		try( Formatter fmt =
			new Formatter( outputDirectory.resolve( DOCKERCOMPOSE_FILENAME ).toFile() ) ) {
			createDockerCompose( fmt );
		}
	}

	private void createDockerCompose( Formatter fmt ) {
		String padding = "";
		fmt.format( "version: \"3.9\"%n" )
			.format( "services:%n" );
		for( Map.Entry< String, Program > service : slices.entrySet() ) {
			fmt.format( "%2s%s:%n", padding, service.getKey().toLowerCase() )
				.format( "%4s", padding )
				.format( "build: ./%s%n", service.getKey().toLowerCase() );
		}
	}

	public Map< String, Program > getSlices() {
		return slices;
	}
}
