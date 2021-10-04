# Jolie Slicer
## About the slicer
We propose Sliceable Monolith, a new methodology for developing microservice architectures and perform their integration testing by leveraging most of the simplicity of a monolith: a single codebase and a local execution environment that simulates distribution. Then, a tool compiles a codebase for each microservice and a cloud deployment configuration. The key enabler of our approach is the technology-agnostic service definition language offered by Jolie.

## Download and setup the slicer
The following steps require Jolie and Java 11 to be installed, there will be provided a step-by-step guide, in the end, describing how to set the Jolie development version up. 

1) Clone Jolie Slicer GitHub repository at: https://github.com/jolie/slicer
2) Change directory to the slicer, and download maven dependencies using the command “mvn install”
3) Create the following symlinks to use the slicer in any location:
```
sudo ln -s /path/to/launcher.ol /usr/local/bin/slicer
sudo ln -s /path/to/slicer/dist /path/to/slicer/lib
```
4) Might get errors because of access permissions, be sure to change them for "launcher.ol"
```
chmod +x launcher.ol
```
5) Success! The slicer should now be callable in any location on your system. Try calling "slicer" and it should print the usage information. 

## How to use the slicer
1) Requirements before slicing

The slicer requires a Jolie file ("monolith.ol") containing all services and interfaces, along with a config.json file. The config file defines what services the user wants to extract from the monolith, which in the example beneath are Foo and Bar.
```
{
    "Foo": {
        "location" : "local://T"
    },
    "Bar": {
        "location" : "local://CS"
    }
}
```
2) Running the slicer

When the slicer has been set up, a monolith has been developed, and a config file has been created the slicer is ready to be used. Inside the folder with the monolith and config file, the user can use the following command to use the slicer:
```
slicer --config config.json monolith.ol
```

## Setting up jolie development version
1) Clone jolie GitHub repository at: https://github.com/jolie/jolie
2) Change directory to jolie/ and download maven dependencies using the command “mvn install”
3) Download dev-setup for jolie by running the command: “./scripts/dev-setup.sh $YOUR_PATH”, where $YOUR_PATH e.g. could be /usr/local/bin
4) Add "JOLIE_HOME=”/$YOUR_PATH/jolie-dist" to .bashrc
5) Log in and out, should now be able to use "jolie --version" to see the current version
