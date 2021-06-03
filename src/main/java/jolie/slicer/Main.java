/***************************************************************************
 *   Copyright (C) 2020 by Valentino Picotti                               *
 *   Copyright (C) 2020 by Fabrizio Montesi                                *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.slicer;

import java.io.IOException;
import java.util.Map;

import jolie.Interpreter;
import jolie.JolieURLStreamHandlerFactory;
import jolie.cli.CommandLineException;
import jolie.lang.CodeCheckingException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.util.ParsingUtils;

/**
 *
 * @author Valentino
 */
public class Main {

	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}

	private static final boolean INCLUDE_DOCUMENTATION = false;

	public static void main( String[] args ) {
		System.out.println( "Hello world!" );

		// TODO: Extend CommandLineParser with a new option to slice a service
		// - Understand if its okey to extend the command line parser or what
		// - There are a lot of things that the constructor of CLP does that maybe
		// we are not inrested in
		try( JolieSlicerCommandLineParser cmdLnParser =
			JolieSlicerCommandLineParser.create( args, Main.class.getClassLoader() ) ) {

			Interpreter.Configuration intConf = cmdLnParser.getInterpreterConfiguration();

			SemanticVerifier.Configuration semVerConfig =
				new SemanticVerifier.Configuration( intConf.executionTarget() );
			semVerConfig.setCheckForMain( false );

			Program program = ParsingUtils.parseProgram(
				intConf.inputStream(),
				intConf.programFilepath().toURI(),
				intConf.charset(),
				intConf.includePaths(),
				intConf.packagePaths(),
				intConf.jolieClassLoader(),
				intConf.constants(),
				semVerConfig,
				INCLUDE_DOCUMENTATION );

			Slicer slicer = Slicer.create(
				program,
				cmdLnParser.getConfigFile(),
				cmdLnParser.getOutputDirectory() );

			Map< String, Program > slices = slicer.getSlices();

			// Debug output
			slices.forEach( ( key, value ) -> {
				System.out.println( "Service " + key + ":" );
				JoliePrettyPrinter prettyService = new JoliePrettyPrinter();
				prettyService.visit( value );
				System.out.println( prettyService.toString() );
			} );

			slicer.generateServiceDirectories();

		} catch( CommandLineException | InvalidConfigurationFileException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException | ParserException | CodeCheckingException | ModuleException e ) {
			e.printStackTrace();
		}
	}
}
