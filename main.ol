#!/usr/bin/env jolie

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

from string-utils import StringUtils
from console import Console
from runtime import Runtime
from file import File

from jolie-slicer import Slicer

service Launcher {
	embed Runtime as runtime
	embed File as file
	embed StringUtils as stringUtils
	embed Console as console
	embed Slicer as slicer

	define usage {
		println@console( "Usage: slicer <program_file> -c <config_file> -o <output_directory>" )()
	}
	
	main {
		scope( usage ) {
			install( default => usage )
			i = 0
			while( i < #args ) {
				if( args[i] == "--config" || args[i] == "-c" ) {
					request.config = args[++i]
				} else if ( args[i] == "--output" || args[i] == "-o" ) {
					request.outputDirectory = args[++i]
				} else {
					startsWith@stringUtils( args[i] { prefix = "-" } )( isAnOption )
					if( isAnOption ) {
						throw( UnrecognizedOption, "Unrecognized option " + args[i] )
					} else {
						request.program = args[i]
					}
				}
				++i
			}
			if( !is_defined(request.config) && !is_defined(request.program) ) {
				throw( MissingArgument, "An argument is missing" )
			}
		}

		getRealServiceDirectory@file()( home )
		getFileSeparator@file()( fs )
		println@console( home )()



			// loadEmbeddedService@runtime( {
			  //     filepath = home + sep + "jolie-slicer.ol"
		//     // type = "Java"
			  //     service = "Slicer"
			  //     // params -> config
			// } )()
		// slice@Slicer( request )()
	}
}