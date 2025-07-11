from runtime import Runtime
from math import Math
from string_utils import StringUtils
from console import Console
from time import Time

type PAID: long

type ParkingArea: void {
    id: PAID
    info: ParkingAreaInformation
}

type ParkingAreaInformation: void {
    name: string
    availability*: TimePeriod
    chargingSpeed: ChargingSpeed
}

type ChargingSpeed: string( enum( ["FAST", "SLOW"] ) )

type TimePeriod: void {
    start: int( ranges( [0, 23] ) )
    end: int( ranges( [1,24] ) )
}

type PACreatedEvent: void {
    type: string( enum( ["PA_CREATED"] ) )
    id: PAID
    info: ParkingAreaInformation
}

type PAUpdatedEvent: void {
    type: string( enum( ["PA_UPDATED"] ) )
    id: PAID
    info: ParkingAreaInformation
}

type PADeletedEvent: void {
    type: string( enum( ["PA_DELETED"] ) )
    id: PAID
}

type DomainEvent: PACreatedEvent | PAUpdatedEvent | PADeletedEvent

interface CommandSideInterface {
    RequestResponse:
        createParkingArea( ParkingAreaInformation )( PAID ),
        updateParkingArea( ParkingArea )( string ),
        deleteParkingArea( PAID )( string )
}

interface ShutDownInterface {
    OneWay:
        shutDown( void )
}


service CommandSide( config : undefined ) {
    execution: concurrent

    inputPort InternalCommands {
        location: config.CommandSide.locations[0]
        protocol: http { format = "json" } 
        interfaces:
            CommandSideInterface,
            ShutDownInterface
    }

    inputPort ExternalCommands {
        location: config.CommandSide.locations[1]
        protocol: http { format = "json" } 
        interfaces:
            CommandSideInterface
    }

    outputPort EventStore {
        location: config.EventStore.locations[0]
        protocol: http { format = "json" } 
        interfaces: EventStoreInterface
    }

    // embed EventStore in EventStore
    embed Console as console
    embed StringUtils as stringUtils

    init { debug = config.CommandSide.params.debug }

    main {
        [ createParkingArea( pa )( id )
          {
              synchronized( dbToken ) {
                  id = #global.db
                  with ( global.db[ id ] ) {
                      .id = id;
                      .info << pa
                  }
              }
          }
        ] {
              if( debug ){
                  valueToPrettyString@stringUtils( pa )( str )
                  println@console( "UPDATED: " + str )()
              }
              synchronized( dbToken ) {
                  event.type = "PA_CREATED"
                  event << global.db[id]
              }
              publishEvent@EventStore( event )
          }
        [ updateParkingArea( pa )( r ){
              valueToPrettyString@stringUtils( pa )( str )
              println@console( "UPDATED: " + str )()
              synchronized( dbToken ) {
                  with( global.db[pa.id] ) {
                      .info << pa.info
                  }
              }
              r = "OK"
          }
        ] {
              event.type = "PA_UPDATED";
              event << pa
              if( debug ){
                  valueToPrettyString@stringUtils( pa )( str )
                  println@console( "UPDATED: " + str )()
              }
              publishEvent@EventStore( event )
        }
        [ deleteParkingArea( id )( r ){
              synchronized ( dbToken ) {
                  undef( global.db[id] )
              }
              r = "OK"
          }
        ] {
              event << {
                  .type = "PA_DELETED";
                  .id = id
              }
              publishEvent@EventStore( event )
        }
        [ shutDown( void ) ]{
              println@console( "Shutting down" )()
              exit
          }
    }
}

type GetParkingAreaResponse: ParkingArea | string( enum( ["NOT FOUND"] ) )
type GetParkingAreasResponse: void {
    list*: ParkingAreaInformation
}

// A location is just an index into the vector global.db
type Location: int

interface QuerySideInterface {
    RequestResponse:
        getParkingArea( PAID )( GetParkingAreaResponse ),
        getParkingAreas( Location )( GetParkingAreasResponse  ) 
}

interface NotificationInterface {
    OneWay:
       notify( DomainEvent )
}

service QuerySide( config: undefined ) {
    execution: concurrent

    inputPort InputQuery {
        location: config.QuerySide.locations[0]
        protocol: http { format = "json" } 
        interfaces:
            QuerySideInterface,
            NotificationInterface,
            ShutDownInterface
    }

    // inputPort EventStoreNotifications {
    //     location: QUERYSIDE
    //     protocol: http
    //     interfaces: NotificationInterface
    // }

    outputPort EventStore {
        location: config.EventStore.locations[0]
        protocol: http { format = "json" } 
        interfaces: EventStoreInterface
    }

    // embed Runtime as Runtime
    // embed EventStore in EventStore
    embed Console as console
    embed StringUtils as stringUtils
    embed Time as time
    embed Runtime as runtime

    init {
        debug = config.QuerySide.params.debug
        subscriber.location = config.QuerySide.locations[0]
        subscriber.topics[0] = "PA_CREATED"
        subscriber.topics[1] = "PA_UPDATED"
        subscriber.topics[2] = "PA_DELETED"

        sleep@time( 2000 )()

        if (debug ) {
            println@console( "[DEBUG QuerySide] state:" + dumpState@runtime() )()
        }

        subscribe@EventStore( subscriber )( res )

        println@console( "Queryside subscription: " + res )()
    }

    main {
        [ getParkingArea( id )( response ) {
            synchronized( dbToken ) {
                println@console( "Get " + id )()
                if ( is_defined( global.db[id] ) ) {
                    response << global.db[id]
                } else {
                    response = "NOT FOUND"
                }
            }
        } ] { nullProcess }
        [ getParkingAreas( location )( response ) {
            synchronized( dbToken ) {
                dbsize = #global.db
                pushBack -> response.list[#response.list]
                if ( dbsize > 0 ) {
                    if ( dbsize <= 3 ) {
                        for( pa in global.db ) {
                            pushback << pa.info
                        }
                    } else {
                        i0 = location++ % dbsize
                        i1 = location++ % dbsize
                        i2 = location % dbsize
                        pushBack << global.db[i0].info
                        pushBack << global.db[i1].info
                        pushBack << global.db[i2].info
                    }
                }
            }
        } ] { nullProcess }
        [ notify( event ) ] {
            if( debug ) {
                valueToPrettyString@stringUtils( event )( str )
                println@console( "Notified of: " + str)()
            }
            type -> event.type
            if ( type == "PA_CREATED" || type == "PA_UPDATED" ) {
                synchronized( dbToken ) {
                    with( global.db[event.id] ) {
                        .id = event.id;
                        .info << event.info
                    }
                }
            } else if( type == "PA_DELETED" ) {
                synchronized( dbToken ) {
                    undef( global.db[event.id] )
                }
            }
        }
        [ shutDown( void ) ]{
              println@console( "Shutting down" )()
              exit
          }
    }
}

type Topic: string

type Subscriber: void {
    topics[1,*]: Topic
    location: string
}

type SubscriptionResponse: string 

interface EventStoreInterface {
    RequestResponse:
        subscribe( Subscriber )( SubscriptionResponse ),
        unsubscribe( Subscriber )( string )
    OneWay:
        publishEvent( DomainEvent )
}

service EventStore( config: undefined ) {
    execution: concurrent

    outputPort Subscriber {
        protocol: http { format = "json" } 
        interfaces: NotificationInterface
    }

    inputPort IP {
        location: config.EventStore.locations[0]
        protocol: http { format = "json" } 
        interfaces:
            EventStoreInterface, ShutDownInterface
    }

    embed Console as console
    embed StringUtils as stringUtils
    embed Runtime as runtime

    init { 
        debug = config.EventStore.params.debug 
        if( debug ) {
            println@console( "[DEBUG EventStore] state:" + dumpState@runtime() )()
        }
    }

    main {
        [ subscribe( subscriber )( response ) {
            if( debug ) {
                valueToPrettyString@stringUtils( subscriber )( str )
                println@console( "Subscription: " + str )()
            }
            for( topic in subscriber.topics ) {
                loc = subscriber.location
                thisTopic -> global.topics.( topic )
                synchronized( subscriberLocation ) {
                    thisTopic.subscribers.( loc ) = loc
                }
                /* for ( ev in thisTopic.events ) {
                    synchronized( subscriberLocation ) {
                        Subscriber.location = subscriber.location
                        notify@Subscriber( ev )
                    }
                } */
            }
            if( debug ) {
                valueToPrettyString@stringUtils( global.topics )( str )
                println@console( "State of topics variable: " )()
                println@console( str )()
            }
            response = "OK"
        } ] { nullProcess }
        [ unsubscribe( subscriber )( response ) {
            for( topic in subscriber.topics ) {
                loc = subscriber.location
                undef( global.topics.( topic ).subscribers.( loc ) )
            }
            response = "OK"
        } ] { nullProcess }
        [ publishEvent( event ) ] {
            if( debug ) {
                valueToPrettyString@stringUtils( event )( str )
                println@console( "Received event " + str )()
            }
            eventsArray -> global.topics.( event.type ).events
            synchronized( dbEvents ) {
                pushBack -> eventsArray[#eventsArray]
                pushBack << event
            }
            if( debug ) {
                valueToPrettyString@stringUtils( eventsArray )( str )
                println@console( "Events Array: " + str )()
            }
            subscribersMap -> global.topics.( event.type ).subscribers
            foreach( subscriber : subscribersMap ) {
                synchronized( subscriberLocation ) {
                    Subscriber.location = subscriber
                    notify@Subscriber( event )
                }
            }
        }
        [ shutDown( void ) ]{
              println@console( "Shutting down" )()
              exit
        }
    }
}

service Test( config: undefined ) {
    execution: single
    outputPort EventStore {
        location: config.EventStore.locations[0]
        protocol: http { format = "json" } 
        interfaces:
            EventStoreInterface,
            ShutDownInterface
    }

    outputPort CommandSide {
        location: config.CommandSide.locations[0]
        protocol: http { format = "json" } 
        interfaces:
            CommandSideInterface,
            ShutDownInterface
    }

    outputPort QuerySide {
        location: config.QuerySide.locations[0]
        protocol: http { format = "json" }
        interfaces:
            QuerySideInterface,
            ShutDownInterface
    }

    embed Time as time
    embed Console as console
    embed StringUtils as stringUtils
    embed Runtime as runtime

    inputPort ip {
        location: config.Test.locations[0]
        protocol: http { format = "json" }
        interfaces:
            NotificationInterface
    }

    init {
        // defaults
        params << {
            delay = 1000
            debug = false
        }
        // overwrite with config params
        params << config.Test.params
        global.params -> params
    }

    main {
        sleep@time( params.delay )()
        subscription << {
            location = config.Test.locations[0]
            topics[0] = "PA_CREATED"
            topics[1] = "PA_DELETED"
        }
        if( params.debug ){
            println@console( "Subscribing as:" )()
            valueToPrettyString@stringUtils( subscription )( dbg )
            println@console( dbg )()
        }
        subscribe@EventStore( subscription )( res )
        parkingArea << {
            name = "PA_123"
            chargingSpeed = "FAST"
            availability[0] << { start = 8 end = 13 }
        }
        if( params.debug ){
            println@console( "Creating Parking Area:" )()
            valueToPrettyString@stringUtils( parkingArea )( dbg )
            println@console( dbg )()
        }
        createParkingArea@CommandSide( parkingArea )( paid )
        notify( event )
        if( params.debug ){
            println@console( "Received Event:" )()
            valueToPrettyString@stringUtils( event )( dbg )
            println@console( dbg )()
        }
        if( event.type != "PA_CREATED" || event.id != paid )
            throw( AssertionFailed )

        if( params.debug ){
            println@console( "Deleting Parking Area with ID: " + paid )()
        }
        deleteParkingArea@CommandSide( paid )()
        notify( event )
        if( params.debug ){
            println@console( "Received Event:" )()
            valueToPrettyString@stringUtils( event )( dbg )
            println@console( dbg )()
        }
        if( event.type != "PA_DELETED" || event.id != paid )
            throw( AssertionFailed )

        unsubscribe@EventStore( subscription )( res )
        println@console( "Test passed." )()
    }
}