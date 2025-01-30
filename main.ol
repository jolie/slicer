#!/usr/bin/env jolie

/*
 * Copyright (C) 2024 Valentino Picotti
 * Copyright (C) 2024 Fabrizio Montesi <famontesi@gmail.com>
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

from console import Console
from file import File
from json-utils import JsonUtils
from runtime import Runtime
from string-utils import StringUtils


from .configurator import Configurator
from .jolie-slicer import Slicer

constants {
  DOCKERCOMPOSE_FILENAME = "docker-compose.yml",
  SERVICECONFIG_FILENAME = "config.json",
  DOCKERFILE_FILENAME = "Dockerfile",
  SLICER_USAGE = " <program_file> -c <config_file> [--slice <output_directory> | --simulate <service_name>]"
}

service Main( params: undefined ) {
  embed Configurator as cfg
	embed Console as console
  embed File as file
  embed JsonUtils as json
  embed Runtime as runtime
	embed Slicer as slicer
	embed StringUtils as str

	inputPort input {
		location: "local"
		RequestResponse: run
	}

  outputPort launcher {
    location: params.launcher.location
    OneWay: done( undefined )
  }

	define printUsage {
		if( !is_defined( __tool ) || !is_string( __tool ) ) {
      __tool = "jolieslicer"
		}
		println@console( "Usage: " + __tool + SLICER_USAGE )()
	}

	main {
    args -> params.args
    scope( usage ) {
      install( default =>
            println@console( usage.( usage.default ) )();
            __tool = launcherRequest;
            printUsage
            done@launcher()
          )
      i = 0
      while( i < #args ) {
        if( args[i] == "--config" || args[i] == "-c" ) {
          slicerConfigFile = args[++i]
        } else if ( args[i] == "--output" || args[i] == "-o" ) {
          outputDir = args[++i]
        } else if ( args[i] == "--slice" ) {
          outputDir = args[++i]
        } else if ( args[i] == "--simulate" ) {
          simulate = args[++i] 
        } else {
          startsWith@str( args[i] { prefix = "-" } )( isAnOption )
          if( isAnOption ) {
            throw( UnrecognizedOption, "Unrecognized option " + args[i] )
          } else if( is_defined( program ) ) {
              throw( ProgramAlreadySpecified,
                     "Program file already specified (" +
                      program + "): found " +
                      args[i] )
          } else {
            program = args[i]
          }
        }
        ++i
      }
      if( !is_defined(slicerConfigFile) || !is_defined(program) ) {
        throw( MissingArgument, "An argument is missing" )
      } else if( !is_defined(simulate) && !is_defined(outputDir) ) {
        throw( MissingArgument, "An argument is missing" )
      }

      runSimulator = is_defined(simulate)
      readFile@file( { filename -> slicerConfigFile, format = "json" } )( slicerConfig )
      produceDeploymentConfig@cfg( { slicerConfig -> slicerConfig, simulate = runSimulator } )
                                 ( deployment )
      if( runSimulator ) {
        println@console( "---- RUNNING SERVICE " + request.simulate + " ----" )()
        loadEmbeddedService@runtime( {
            service -> simulate
            filepath -> program
            params -> deployment } )()
        linkIn( Exit )
      } else if( is_defined( outputDir ) ) {
        // Here we receive java exceptions from the slicer, so we print the stack trace instead
        // install( default =>
        //   		println@console( usage.( usage.default ).stackTrace )()
        // )

        // Produce sliced monolith
        request.program -> program
        request.outputDirectory -> outputDir
        foreach( service: slicerConfig ){
          request.services[#request.services] << service
        }
        slice@slicer( request )()

        // Generate docker-compose
        getFileSeparator@file()( FILE_SEPARATOR )
        endsWith@str( outputDir { suffix -> FILE_SEPARATOR } )( hasSeparator )
        if( !hasSeparator ) {
          outputDir += FILE_SEPARATOR
        }
        produceComposefile@cfg( { slicerConfig -> slicerConfig } )( composefile )
        writeFile@file( {
            filename = outputDir + DOCKERCOMPOSE_FILENAME
            content -> composefile } )()

        // Generate service config.json
        with( serviceConfig ){
          getJsonString@json( deployment )( .content )
          // Make the file a tiny bit more human-readble
          replaceAll@str( .content { regex = "\\\\" replacement = "" } )( .content )
          replaceAll@str( .content {
              regex = "([{},\\[\\]])([^,])"
              replacement = "$1\n$2" } )( .content )
        }
        // Generate Dockerfile
        produceDockerfiles@cfg( { slicerConfig -> slicerConfig } )( dockerfiles )

        // Write config and dockerfiles
        foreach( service: slicerConfig ) {
          toLowerCase@str( service )( serviceDir )
          serviceDir = outputDir + serviceDir + FILE_SEPARATOR
          writeFile@file( {
              filename = serviceDir + DOCKERFILE_FILENAME
              content -> dockerfiles.(service) } )()
          serviceConfig.filename = serviceDir + SERVICECONFIG_FILENAME
          writeFile@file( serviceConfig )()
        }
        done@launcher()
      }
    }
	}
}
