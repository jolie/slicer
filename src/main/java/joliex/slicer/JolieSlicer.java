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

import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.Program;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class JolieSlicer extends JavaService {

    private static final boolean INCLUDE_DOCUMENTATION = false;
    private static final String[] EMPTY_INCLUDE_PATHS = new String[0];
    private static final ClassLoader CLASS_LOADER = Main.class.getClassLoader();

    @RequestResponse
    public void slice( Value request ) throws FaultException {

        final Path programPath = Path.of( request.getFirstChild( "program" ).strValue() );
        final Path configPath = Path.of( request.getFirstChild( "config" ).strValue() );
        final Path outputDirectory;
        if( request.hasChildren( "outputDirectory" ) ) {
            outputDirectory = Path.of(request.getFirstChild( "outputDirectory" ).strValue() );
        } else { // Generete the sliced program into a directory with the same name of the program
            String filename = programPath.getFileName().toString();
            int fileExtensionIndex = filename.lastIndexOf( ".ol" );
            filename = filename.substring( 0, fileExtensionIndex );
            outputDirectory = programPath.resolveSibling( filename );
        }

        try ( InputStream stream = Files.newInputStream( programPath ) ) {
            final Path programDirectory = programPath.getParent();

            final Scanner scanner = new Scanner(stream, programDirectory.toUri(), null, INCLUDE_DOCUMENTATION);
            final OLParser olParser = new OLParser(scanner, EMPTY_INCLUDE_PATHS, CLASS_LOADER);
            final Program program = olParser.parse();

            final Slicer slicer = Slicer.create(
                    program,
                    configPath,
                    outputDirectory);

            slicer.generateServiceDirectories();

        } catch ( FileNotFoundException e ) {
            throw new FaultException( "FileNotFound", e );
        } catch ( ParserException | InvalidConfigurationFileException | IOException e ) {
            throw new FaultException( e.getClass().getSimpleName(), e );
        }
    }
}
