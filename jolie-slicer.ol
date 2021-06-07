
from types.JavaException import JavaExceptionType, WeakJavaExceptionType
from types.IOException import IOExceptionType
from file import FileNotFoundType

type SliceRequest: void {
    program: string
    config: string
    outputDirectory? : string
}

interface SlicerInterface {
    RequestResponse:
        slice( SliceRequest )( void ) throws
            FileNotFound( FileNotFoundType )
            IOException( IOExceptionType )
            ParserException( JavaExceptionType )
            InvalidConfigurationFileException( JavaExceptionType )
}


service Slicer {
    inputPort ip {
        location: "local"
        interfaces: SlicerInterface
    }
    foreign java {
        class: "joliex.slicer.JolieSlicer"
    }
}