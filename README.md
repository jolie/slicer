# Jolie Slicer

Jolie Slicer is the companion tool to the Sliceable Monolith[^1] development methodology.

The tool (`jolieslicer`) is available on npm:
```bash
npm install -g @jolie/slicer
```
Usage:
```bash
jolieslicer <monolith.ol> -c <slicer.json> [--slice <output_directory> | --simulate <service_name>]
```
## Sliceable Monolith

1. The entire microservices architecture is coded in a single Jolie file (`monolith.ol`):
    1. Services are parameterised by their deployment configuration (`config`)
    2. Input and Output ports are parameterized by their deployment location available under the path `config.ServiceName.locations._`

    As an example:
    ```jolie
    service Gateway( config ) {
      inputPort ip {
        location: config.Gateway.locations._[0]
        ...
      }
      outputPort CommandSide {
        location: config.CommandSide.locations._[0]
        ...
      }
      main { ... }
    }
    ```

2. A configuration file (`slicer.json`) describes the services in the architecture and their ports:
    ```json
    {
      "Gateway": {
        "params": {},
        "ports": [
          8080
        ]
      },
      "CommandSide": {
        "params": {},
        "ports": [
          "internal"
        ]
      },
      ...
    }
    ```
    Services provide their functionality through one or more ports, each declared as either:
    - *Internal* (`"internal"`) and accessible only by services in the same deployment
    - *Exposed* (e.g., `8080`) and accessible by external clients through a TCP socket

3. The Jolie Slicer can now be used to either:
    1. Run the microservices architecture as a single executable by specifying the name of the service that acts as the entry point of the application:
        ```bash
        jolieslicer monolith.ol -c slicer.json --simulate Gateway
        ```
    2. Slice the monolith into separate codebases, one for each service mentioned in the configuration (`slicer.json`):
        ```bash
        jolieslicer monolith.ol -c slicer.json --slice <output_directory>
        ```
        and a reasonable docker-compose configuration for the distributed deployment of the architecture:
        ```bash
        cd <output_directory> && docker compose up
        ```

## Example

An example is provided under [`example/`](example/):
- [`monolith.ol`](example/monolith.ol) implements a Sliceable Monolith
- [`slicer.json`](example/slicer.json) is the Slicer configuration
- [`microservices/`](example/microservices/) contains the result of slicing the monolith

From within the direcotry `example`, one can:
- Run the monolith as a single executable:
    ```bash
    jolieslicer monolith.ol -c slicer.json --simulate Main
    ```
- Run the service `Test` to locally execute an integration test:
    ```bash
    jolieslicer monolith.ol -c slicer.json --simulate Test
    ```
- Slice the monolith into separete codebases:
    ```bash
    jolieslicer monolith.ol -c slicer.json --slice microservices
    ```

[^1]:[Sliceable Monolith: Monolith First, Microservices Later](https://doi.org/10.1109/SCC53864.2021.00050)
