#!/usr/bin/env jolie

/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021 Valentino Picotti
 * Copyright (C) 2021 Marco Peressotti
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
from mustache import Mustache
from runtime import Runtime
from string-utils import StringUtils

from .jolie-slicer import Slicer

// configuration constants (edit with care)
constants {
  APP_NAME = "jolieslicer",
  APP_VERSION = "0.1.6",
  JOLIE_VERSION = "1.13.1",
  DEFAULT_BASE_PORT = 10000,
  DEFAULT_TEMPLATES_DIR = "/templates/",
  DOCKER_TEMPLATE ="Dockerfile.mustache",
  COMPOSE_TEMPLATE ="docker-compose.mustache",
  SERVICE_PARAMFILE = "config.json",
}

// program constants
constants {
  MODE_SLICE = "slice",
  MODE_SIMULATE = "simulate",
  INTERNAL = "internal"
}

service Launcher {
  execution: single

	embed Console as console
	embed File as file
  embed Mustache as mst
	embed Runtime as runtime
	embed StringUtils as str
	embed Slicer as slicer

  define printUsage {
    println@console( fmt@str("{name} {version}

usage: {name} application configuration [options]

description: {name} is a command line tool to support the development of Jolie applications following the the Sliceable Monolith development methodology. This tool allows to run a sliceable monolith application locally (via the --run option) or to slice it into a set of services (with --slice) that can be run in a containerized environment, such as Docker.

arguments:
  application: the Jolie application to run or slice
  configuration: the application configuration file in JSON format

options:
  -s, --slice output_dir  Slice the application and write the output to the specified directory
  --run                   Runs the application locally
  --base-port port        Base port for internal services (default: {defaultBasePort})
  --templates path        Custom templates
  -h, --help              Show this help message"{
      name = APP_NAME,
      version = APP_VERSION
      defaultBasePort = DEFAULT_BASE_PORT,
    } ) )()
  }

  main {
    //-------------------------------------------------------------------
    // parse command line arguments
    scope( usage ) {
      install( Help =>
          printUsage
          halt@runtime( { status = 0 } )( )
      )
      install( default =>
          println@console( usage.( usage.default ) +"\n" )()
          printUsage
          halt@runtime( { status = 1 } )( )
      )
      if (#args == 0 || args[0] == "--help" || args[0] == "-h") {
        throw( Help )
      }
      params.programFile = args[0]
      if ( #args == 1 ) {
        throw(UsageError, "Configuration file not specified" )
      }
      params.configFile = args[1]
      i = 2
      while( i < #args ) {
        if ( args[i] == "--slice") {
          if ( params.mode == MODE_SLICE ) {
            throw(UsageError, "--slice can be specified only once" )
          }
          if ( params.mode == MODE_SIMULATE ) {
            throw(UsageError, "--slice and --run cannot be both specified" )
          }
          i += 1
          if ( #args == i ) {
            throw(UsageError, "Output directory not specified" )
          }
          params.mode = MODE_SLICE
          params.outputDir = args[i]
        } else if ( args[i] == "--run") {
          if ( params.mode == MODE_SIMULATE ) {
            throw(UsageError, "--slice can be specified only once" )
          }
          if ( params.mode == MODE_SLICE ) {
            throw(UsageError, "--slice and --run cannot be both specified" )
          }
          params.mode = MODE_SIMULATE
        } else if ( args[i] == "--base-port") {
          i += 1
          params.basePort = int( args[i] )
          if ( params.basePort < 1024 || params.basePort > 65435 ) {
            throw(UsageError, "Base port must be between 1024 and 65435" )
          }
        } else if ( args[i] == "--templates") {
          i += 1
          params.templatesDir = args[i]
          if ( #args == i ) {
            throw(UsageError, "Template directory not specified" )
          }
        // } else if ( args[i] == "--no-overwrite" ) {
        //   params.no_overwrite = true
        } else if (args[i] == "--help" || args[i] == "-h") {
          throw( help )
        } else {
          throw(UsageError, "Invalid argument: " + args[i] + "." )
        }
        i += 1
      }
      if (!is_defined( params.mode ) ) {
        throw(UsageError, "Missing argument: --slice or --run must be specified." )
      }
      if (!is_defined( params.basePort ) ) {
        params.basePort = DEFAULT_BASE_PORT
      }
      if (!is_defined( params.templatesDir ) ) {
        params.templatesDir = getRealServiceDirectory@file() + DEFAULT_TEMPLATES_DIR
      }
      // ensure templates and output directory ends with file separator
      getFileSeparator@file()( FILE_SEPARATOR )
      if( !endsWith@str( params.templatesDir { suffix -> FILE_SEPARATOR } ) ) {
        params.templatesDir += FILE_SEPARATOR
      }
      if( params.mode == MODE_SLICE && !endsWith@str( params.outputDir { suffix -> FILE_SEPARATOR } ) ) {
        params.outputDir += FILE_SEPARATOR
      }
    }
    // end of arguments parsing
    //-------------------------------------------------------------------
    // parse configuration 
    scope( parse_config ) {
      install( 
        default =>
          println@console( "An error occurred while processing the configuration file: " + params.configFile )()
          println@console( parse_config.( parse_config.default ) )();
          exit
      )
      readFile@file( { 
        filename -> params.configFile, 
        format = "json" 
      } )( configuration )
      foreach( serviceName : configuration ) {
        service -> configuration.( serviceName )
        undef(service.locations)
        service.name = toLowerCase@str( serviceName )
        for( i = 0, i < #service.ports, i++ ) {
          if( params.mode == MODE_SLICE ) {
            if( service.ports[i] == INTERNAL ) {
              service.ports[i] << (params.basePort + i){ internal = true }
            } else {
              service.ports[i].internal = false
            }
            service.locations[i] = "socket://" + service.name + ":" + service.ports[i]
          } else {
            if( service.ports[i] == INTERNAL ) {
              service.locations[i] = "local://" + service.name
              if ( i > 0 ) {
                service.locations[i] += i
              }
            } else {
              service.locations[i] = "socket://" + service.name + ":" + service.ports[i]
            }
          }
        }
      }
    }
    params.configuration -> configuration
    undef(params.configFile)
    // println@console( valueToPrettyString@str( params ) )()
    // end of configuration parsing
    //-------------------------------------------------------------------
    // 
    if(params.mode == MODE_SIMULATE) {
      println@console( "Starting: " + params.programFile )()
      println@console( "Press Ctrl+C to terminate" )()
      println@console( "" )()
      // embed and run each service locally with its slice of configuration
      foreach( serviceName : params.configuration ) {
        configSlice.( serviceName ).locations << params.configuration.( serviceName ).locations
      }
      foreach( serviceName : params.configuration ) {
        // add params for this service
        configSlice.( serviceName ).params -> params.configuration.( serviceName ).params
        loadEmbeddedService@runtime( {
          service = serviceName
          filepath = params.programFile
          params << params.configuration 
        } )()
        // restore configSlice
        undef( configSlice.( serviceName ).params )
      }
      linkIn( Exit )
    } else if (params.mode == MODE_SLICE) {
      // ----------------------------------------------
      // directory structure
      foreach( serviceName : params.configuration ) {
        params.outputDir.( serviceName ) = params.configuration.( serviceName ).name + FILE_SEPARATOR
      }
      // ----------------------------------------------
      // slice jolie codebase
      request.program = params.programFile
      request.outputDirectory << params.outputDir
      i = 0
      foreach( serviceName : params.configuration ) {
        request.services[i++] = serviceName
      }
      slice@slicer( request )()
      // ----------------------------------------------
      // slice configuration file
      foreach( serviceName : params.configuration ) {
        configSlice.( serviceName ).locations << params.configuration.( serviceName ).locations
      }
      foreach( serviceName : params.configuration ) {
        // add params for this service and write the file
        configSlice.( serviceName ).params << params.configuration.( serviceName ).params
        writeFile@file( {
          filename = params.outputDir + params.outputDir.( serviceName ) + SERVICE_PARAMFILE
          content << configSlice
          format = "json"
        } )()
        // restore configSlice
        undef( configSlice.( serviceName ).params )
      }
      undef( configSlice )
      // ----------------------------------------------
      // dockerfile
      readFile@file( { 
        filename = params.templatesDir + DOCKER_TEMPLATE 
      } )( render.template )
      with( render.data ){
        .jolie_version = JOLIE_VERSION
        .config_file = SERVICE_PARAMFILE
      }
      foreach( serviceName : params.configuration ) {
        render.data.service_file = serviceName + ".ol"
        render.data.ports -> params.configuration.( serviceName ).ports
        render@mst( render )( content )
        writeFile@file( {
          filename = params.outputDir + params.outputDir.( serviceName ) + "Dockerfile"
          content -> content
          format = "text"
        } )()
      }
      undef(render)
      undef(content)
      // dockercompose
      readFile@file( { 
        filename = params.templatesDir + COMPOSE_TEMPLATE 
      } )( render.template )
      foreach( serviceName: params.configuration ) {
        service -> params.configuration.( serviceName )
        serviceData.name = service.name
        serviceData.path = params.outputDir.( serviceName )
        for( i = 0, i < #service.ports, i++ ) {
          if( !service.ports[i].internal ) {
            serviceData.ports[#serviceData.ports] << {
              external = service.ports[i]
              internal = service.ports[i]
            }
          }
        }
        serviceData.has_ports = is_defined( serviceData.ports )
        render.data.services[#render.data.services] << serviceData
        undef( serviceData )
      }
      render@mst( render )( content )
      writeFile@file( {
        filename = params.outputDir + "docker-compose.yml"
        content -> content
        format = "text"
      } )()
    }
    //-------------------------------------------------------------------
  }

}