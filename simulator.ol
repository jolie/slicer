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
from file import File
from console import Console
from runtime import Runtime
from json-utils import JsonUtils

interface SimulatorIface {
  RequestResponse:
    run( undefined )( undefined )
}

service Simulator {
	embed Console as console
	embed StringUtils as str
  embed JsonUtils as json
  embed File as file
  embed Runtime as runtime

  inputPort in {
    location: "local"
    interfaces: SimulatorIface
  }

  // outputPort self {
  // }

  main {
    run( request )( response ){
      println@console( "Simulator" )()
      readFile@file( { filename = request.config  format = "json" } )( slicerConfig )
      // produce deployment.json from slicer.json
      foreach( service : slicerConfig ) {
        i = 0
        for( location in slicerConfig.(service).locations ) {
          match@str( location { regex = "([^:]+)(?>:(\\d{1,5}\\Z))?" } )( match )
          if( match.group[2] != "" ) { // port declaration
            deployment.( service ).locations[i] = "socket://localhost:" + match.group[2]
          } else {
            deployment.( service ).locations[i] = "local://" + match.group[1]
          }
          ++i
        }
      }
      getJsonString@json( deployment )( json )
      replaceAll@str( json { regex = "\\\\" replacement = "" } )( json )
      replaceAll@str( json { regex = "([{},\\[\\]])([^,])" replacement = "$1\n$2" } )( json )
      writeFile@file( { filename = "deployment.json" content -> json } )()
    }
  }
}