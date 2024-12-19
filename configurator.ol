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
from mustache import Mustache
from string-utils import StringUtils

type DeploymentConfigRequest {
  slicerConfig: undefined
  simulate: bool
}

type DockerfileRequest {
  slicerConfig: undefined
}

type ComposefileRequest {
  slicerConfig: undefined
}

interface ConfiguratorIface {
  RequestResponse:
    produceDeploymentConfig( DeploymentConfigRequest )( undefined ),
    produceDockerfiles( DockerfileRequest )( undefined ),
    produceComposefile( ComposefileRequest )( string )
}

constants {
  LOCAL = "local://",
  SOCKET = "socket://",
  INTERNAL = "internal",
  BASE_PORT = 10000,
  TEMPLATES_DIR = "/templates/",
  DOCKER_TEMPLATE ="Dockerfile.mustache",
  COMPOSE_TEMPLATE ="docker-compose.mustache",
  JOLIE_EXTENSION = ".ol",
  CONFIG_FILENAME = "config.json"
}

service Configurator {
  execution: sequential

  embed Console as console
  embed File as file
  embed Mustache as mst
  embed StringUtils as str

  inputPort in {
    location: "local"
    interfaces: ConfiguratorIface
  }

  main {
    [ produceDeploymentConfig( request )( response ){
      // readFile@file( { filename = request.config  format = "json" } )( config )
      // produce deployment config from slicer.json
      config -> request.slicerConfig
      deployment << config
      deployment.simulator = request.simulate
      foreach( service : config ) {
        undef( deployment.( service ).ports )
        toLowerCase@str( service )( host )
        i = 0
        for( port in config.(service).ports ) {
          serviceLocation -> deployment.( service ).locations._[i]
          isLocal = request.simulate && port == INTERNAL
          serviceLocation = if ( isLocal ) LOCAL else SOCKET
          serviceLocation += host
          if( !isLocal ) {
            serviceLocation += ":"
          }
          if( request.simulate && is_int( port ) ) {
            serviceLocation += port
          } else {
            serviceLocation += BASE_PORT + i
          }
          ++i
        }
      }
      response -> deployment
    } ]
    [ produceDockerfiles( request )( response ){
      config -> request.slicerConfig
      // Which one to use??
      getRealServiceDirectory@file()( rdir )
      // getServiceDirectory@file()( dir )
      // getServiceParentPath@file()( pdir ) 
      readFile@file( { filename = rdir + TEMPLATES_DIR + DOCKER_TEMPLATE } )( render.template )
      with( render.data ){
        .jolie_version = "1.12.1"
        .config_file = CONFIG_FILENAME
      }

      foreach( service: config ) {
        render.data.service_file = service + JOLIE_EXTENSION
        for( i = 0, i < #config.( service ).ports, i++ ) {
          render.data.ports[#render.data.ports] = BASE_PORT + i++
        }
        render@mst( render )( response.( service ) )
      }
    } ]
    [ produceComposefile( request )( response ){
      config -> request.slicerConfig
      // Which one to use??
      getRealServiceDirectory@file()( rdir )
      // getServiceDirectory@file()( dir )
      // getServiceParentPath@file()( pdir ) 
      readFile@file( { filename = rdir + TEMPLATES_DIR + COMPOSE_TEMPLATE } )( render.template )
      serviceIdx = 0
      foreach( service: config ) {
        serviceData -> render.data.services[serviceIdx]
        serviceData.name = toLowerCase@str( service )
        for( portIdx = 0, portIdx < #config.( service ).ports, portIdx++ ) {
          port -> config.( service ).ports[portIdx]
          if( is_int( port ) ){
            serviceData.ports[#serviceData.ports] << {
                exposed -> port
                internal = BASE_PORT + portIdx }
          }
        }
        serviceData.has_ports = is_defined( serviceData.ports )
        serviceIdx += 1
      }
      render@mst( render )( response )
    } ]
  }
}