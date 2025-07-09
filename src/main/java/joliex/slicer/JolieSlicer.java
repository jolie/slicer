/*
 * Copyright (C) 2021 Valentino Picotti
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package joliex.slicer;

import jolie.Interpreter;
import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import jolie.lang.CodeCheckException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;


public class JolieSlicer extends JavaService {
    private static final String PROGRAM_CHILD = "program";
    public static final String SERVICES_CHILD = "services";
    public static final String OUTPUT_DIRECTORY_CHILD = "outputDirectory";
    private static final boolean INCLUDE_DOCUMENTATION = false;
 
    @RequestResponse
    public void slice( Value request ) throws FaultException {

        final Path programPath = Path.of( request.getFirstChild( PROGRAM_CHILD ).strValue() );
        final Set<String> services = request.getChildren( SERVICES_CHILD )
                .stream().map(Value::strValue).collect(Collectors.toSet());
        final Path outputDirectory;
        if( request.hasChildren( OUTPUT_DIRECTORY_CHILD ) ) {
            outputDirectory = Path.of(request.getFirstChild( OUTPUT_DIRECTORY_CHILD ).strValue() );
        } else { // Generete the sliced program into a directory with the same name of the program
            String filename = programPath.getFileName().toString();
            int fileExtensionIndex = filename.lastIndexOf( ".ol" );
            filename = filename.substring( 0, fileExtensionIndex );
            outputDirectory = programPath.resolveSibling( filename );
        }
        ArrayList<String> newArgs = new ArrayList<>();
        String[] interpreterArgs = Interpreter.getInstance().optionArgs();
        
        for(int i = 0; i < interpreterArgs.length; i++ ) {
            if( "-p".equals(interpreterArgs[i]) ) {
                newArgs.add(interpreterArgs[i]);
                i++;
                newArgs.add(interpreterArgs[i]);
            }
        }
        newArgs.add( programPath.toString() );

        try( CommandLineParser cmdLnParser =
                     new CommandLineParser( newArgs.toArray(new String[0]), JolieSlicer.class.getClassLoader() ) ) {

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

            final Slicer slicer = Slicer.create( program, outputDirectory, services );
            slicer.generateServiceDirectories();
        } catch ( CommandLineException | InvalidConfigurationFileException | CodeCheckException | IOException e ) {
            throw new FaultException( e.getClass().getSimpleName(), e );
        }
    }
}
