/*
 * Copyright (C) 2024 Valentino Picotti
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

type SimulatorRequest {
  deployment: undefined
  program: string
  simulate: string
}

interface SimulatorIface {
  RequestResponse:
    run( SimulatorRequest )( undefined )
}

service Simulator {
	embed Console as console
  embed File as file
  embed JsonUtils as json
  embed Runtime as runtime
	embed StringUtils as str

  inputPort in {
    location: "local"
    interfaces: SimulatorIface
  }

  main {
    run( request )( response ){
      println@console( "---- RUNNING SERVICE " + request.simulate + " ----" )()
      loadEmbeddedService@runtime( {
          service -> request.simulate
          filepath -> request.program
          params -> request.deployment } )()
      linkIn( Exit )
    }
  }
}