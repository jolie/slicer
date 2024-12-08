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

from .jolie-slicer import Slicer

service Main {
	embed Slicer as slicer
	embed Console as console
	embed StringUtils as stringUtils

	inputPort input {
		location: "local"
		RequestResponse: run
	}

	define printUsage {
		if( is_defined( __tool ) && is_string( __tool ) ) {
			println@console( "Usage: " + __tool + " <program_file> -c <config_file> -o <output_directory>" )()
		} else {
			println@console( "Usage: slicer <program_file> -c <config_file> -o <output_directory>" )()
		}
	}

	main {
		run( launcherRequest )() {
			scope( usage ) {
				install( default =>
							println@console( usage.( usage.default ) )();
							__tool = launcherRequest;
							printUsage
						)
				i = 0
				while( i < #launcherRequest.args ) {
					if( launcherRequest.args[i] == "--config" || launcherRequest.args[i] == "-c" ) {
						request.config = launcherRequest.args[++i]
					} else if ( launcherRequest.args[i] == "--output" || launcherRequest.args[i] == "-o" ) {
						request.outputDirectory = launcherRequest.args[++i]
					} else {
						startsWith@stringUtils( launcherRequest.args[i] { prefix = "-" } )( isAnOption )
						if( isAnOption ) {
							throw( UnrecognizedOption, "Unrecognized option " + launcherRequest.args[i] )
						} else if( is_defined( request.program ) ) {
						    throw( ProgramAlreadySpecified,
						           "Program file already specified (" +
						            request.program + "): found " +
						            launcherRequest.args[i] )
						} else {
							request.program = launcherRequest.args[i]
						}
					}
					++i
				}
				if( !is_defined(request.config) || !is_defined(request.program) ) {
					throw( MissingArgument, "An argument is missing" )
				}
				// Here we receive java exceptions from the slicer, so we print the stack trace instead
				install( default =>
							println@console( usage.( usage.default ).stackTrace )()
				)
				slice@slicer( request )(  )
			}
		}
	}
}
