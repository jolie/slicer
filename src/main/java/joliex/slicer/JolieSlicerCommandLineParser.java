package joliex.slicer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;

public class JolieSlicerCommandLineParser extends CommandLineParser {

	private final JolieSlicerArgumentHandler argHandler;

	private JolieSlicerCommandLineParser( String[] args, ClassLoader parentClassLoader,
		JolieSlicerArgumentHandler argHandler ) throws CommandLineException, IOException {
		super( args, parentClassLoader, argHandler );
		this.argHandler = argHandler;
		// if( argHandler.configFile == null ) {
		// throw new CommandLineException("Configuration file not specified.");
		// }

		if( argHandler.outputDirectory == null ) {
			File programFilePath = getInterpreterConfiguration().programFilepath();
			String fileName = programFilePath.getName();
			fileName = fileName.substring( 0, fileName.lastIndexOf( ".ol" ) );
			argHandler.outputDirectory = programFilePath.toPath().resolveSibling( fileName );
			System.out.println( "Generating output files at: " + argHandler.outputDirectory );
		}

		if( argHandler.configFile == null ) {
			throw new CommandLineException( "Missing configuration file (--config config_file)" );
		}
	}

	public static JolieSlicerCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException {
		return new JolieSlicerCommandLineParser( args, parentClassLoader, new JolieSlicerArgumentHandler() );
	}

	private static class JolieSlicerArgumentHandler implements CommandLineParser.ArgumentHandler {
		private String serviceName = null;
		public Path outputDirectory = null;
		public Path configFile = null;

		@Override
		public int onUnrecognizedArgument( List< String > argumentsList, int index ) throws CommandLineException {
			// TODO: service is already an option. Delete the if
			if( "--service".equals( argumentsList.get( index ) ) ) {
				index++;
				serviceName = argumentsList.get( index );
			} else if( "--output".equals( argumentsList.get( index ) ) || "-o".equals( argumentsList.get( index ) ) ) {
				index++;
				outputDirectory = Paths.get( argumentsList.get( index ) );
			} else if( "--config".equals( argumentsList.get( index ) ) ) {
				index++;
				configFile = Paths.get( argumentsList.get( index ) );
			}
			return index;
		}
	}

	@Override
	protected String getHelpString() {
		return new StringBuilder().append( "Usage: jolieslicer --service name_of_service program_file\n\n" ).toString();
	}

	public String getServiceName() {
		return argHandler.serviceName;
	}

	public Path getOutputDirectory() {
		return argHandler.outputDirectory;
	}

	public Path getConfigFile() {
		return argHandler.configFile;
	}

}
