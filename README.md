# Jolie Slicer

Jolie Slicer is a command line tool to support the development of Jolie applications following the the [Sliceable Monolith](https://doi.org/10.1109/SCC53864.2021.00050) development methodology. 
This tool allows to run a sliceable monolith application locally in a single process (via the --run option) or to slice it into a set of services (with --slice) that can be deployed and run in a containerized environment, such as Docker.

## Installing

The tool is available as a Docker image on [github](http://ghcr.io/jolie/slicer):
```bash
docker pull ghcr.io/jolie/slicer:latest
```
and as a package on [npm](https://www.npmjs.com/package/@jolie/slicer):
```bash
npm install -g @jolie/slicer
```

Usage:
```bash
jolieslicer application.ol configuration.json [options]
```
## Sliceable Monolith

1. The entire microservices architecture is coded in a single Jolie file (`application.ol`):
    1. Services are parameterised by their deployment configuration (`configuration`)
    2. Input and Output ports are parameterized by their deployment location available under the path `configuration.ServiceName.locations`

    As an example:
    ```jolie
    service Gateway( configuration ) {
      inputPort ip {
        location: configuration.Gateway.locations[0]
        ...
      }
      outputPort CommandSide {
        location: configuration.CommandSide.locations[0]
        ...
      }
      main { ... }
    }
    ```
2. A configuration file (`configuration.json`) describes the services in the architecture, their ports, and initialisation parameters:
    ```json
    {
      "Gateway": {
        "ports": [
          8080
        ],
        "params": { ... }
      },
      "CommandSide": {
        "ports": [
          "internal"
        ],
        "params": { ... }
      },
      ...
    }
    ```
    Services provide their functionality through one or more ports, each declared as either:
    - *Internal* (`"internal"`) and accessible only by services in the same deployment;
    - *External* (e.g., `8080`) and accessible by external clients through a TCP socket.
    
    Initialisation parameters for each service can be provided under `"params"`.
3. The Jolie Slicer can now be used to either:
    1. Run the microservices architecture as a single executable:
        ```bash
        jolieslicer application.ol configuration.json --run
        ```
    2. Slice the monolith into separate codebases and 
        ```bash
        jolieslicer application.ol configuration.json--slice output_directory
        ```

See [`example/`](example/) for an example application and ["Sliceable Monolith: Monolith First, Microservices Later" (Picotti et al. 2021)](https://doi.org/10.1109/SCC53864.2021.00050) for details on the Sliceable Monolith methodology see .