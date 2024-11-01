from runtime import Runtime
from math import Math
from string_utils import StringUtils
from console import Console
from time import Time

constants {
   COMMANDSIDE = "socket://localhost:10000",
   QUERYSIDE = "socket://localhost:10001",
   EVENTSTORE = "socket://localhost:10002",
   TESTER = "socket://localhost:10003"
}


type PAID : long

type ParkingArea : void {
    .id : PAID
    .info : ParkingAreaInformation
}

type ParkingAreaInformation : void {
    .name : string
    .availability * : TimePeriod
    .chargingSpeed : ChargingSpeed
}

type ChargingSpeed : string( enum( ["FAST", "SLOW"] ) )

type TimePeriod : void {
    .start : int( ranges( [0, 23] ) )
    .end : int( ranges( [1,24] ) )
}

type PACreatedEvent : void {
    .type : string( enum( ["PA_CREATED"] ) )
    .id : PAID
    .info : ParkingAreaInformation
}

type PAUpdatedEvent : void {
    .type : string( enum( ["PA_UPDATED"] ) )
    .id : PAID
    .info : ParkingAreaInformation
}

type PADeletedEvent : void {
    .type : string( enum( ["PA_DELETED"] ) )
    .id : PAID
}

type DomainEvent : PACreatedEvent | PAUpdatedEvent | PADeletedEvent

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

    inputPort InputCommands {
        location: config.CommandSide.location
        protocol: http { format = "json" } 
        interfaces:
            CommandSideInterface,
            ShutDownInterface
    }

    outputPort EventStore {
        location: config.EventStore.location
        protocol: http { format = "json" } 
        interfaces: EventStoreInterface
    }

    // embed EventStore in EventStore
    embed Console as C
    embed StringUtils as S

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
              valueToPrettyString@S( pa )( str )
              println@C( "UPDATED: " + str )()
              synchronized( dbToken ) {
                  event.type = "PA_CREATED"
                  event << global.db[id]
              }
              publishEvent@EventStore( event )
          }
        [ updateParkingArea( pa )( r ){
              valueToPrettyString@S( pa )( str )
              println@C( "UPDATED: " + str )()
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
              valueToPrettyString@S( pa )( str )
              println@C( "UPDATED: " + str )()
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
              println@C( "Shutting down" )()
              exit
          }
    }
}

type GetParkingAreaResponse : ParkingArea | string( enum( ["NOT FOUND"] ) )
type GetParkingAreasResponse : void {
    .list* : ParkingAreaInformation
}

/* A location is just an index into the vector global.db
*/
type Location : int

interface QuerySideInterface {
    RequestResponse:
        getParkingArea( PAID )( GetParkingAreaResponse ),
        getParkingAreas( Location )( GetParkingAreasResponse  ) 
}

interface NotificationInterface {
    OneWay:
       notify( DomainEvent )
}

service QuerySide( config : undefined ) {
    execution: concurrent

    inputPort InputQuery {
        location: config.QuerySide.location
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
        location: config.EventStore.location
        protocol: http { format = "json" } 
        interfaces: EventStoreInterface
    }

    embed Runtime as Runtime
    // embed EventStore in EventStore
    embed Console as C
    embed StringUtils as S

    init {
        // getLocalLocation@Runtime()( subscriber.location )
        subscriber.location = config.QuerySide.location
        pushBackTopic -> subscriber.topics[#subscriber.topics]
        pushBackTopic = "PA_CREATED"
        pushBackTopic = "PA_UPDATED"
        pushBackTopic = "PA_DELETED"

        valueToPrettyString@S( subscriber )( str )
        println@C( str )()
        
        subscribe@EventStore( subscriber )( res )

        println@C( "Queryside subscription: " + res )()
    }

    main {
        [ getParkingArea( id )( response ) {
            synchronized( dbToken ) {
                println@C( "Get " + id )()
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
            valueToPrettyString@S( event )( str )
            println@C( "Notified of: " + str)()
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
              println@C( "Shutting down" )()
              exit
          }
    }
}

type Topic : string

type Subscriber : void {
    .topics [1,*] : Topic
    .location : string
}

type SubscriptionResponse : string 

interface EventStoreInterface {
    RequestResponse:
        subscribe( Subscriber )( SubscriptionResponse ),
        unsubscribe( Subscriber )( string )
    OneWay:
        publishEvent( DomainEvent )
}

service EventStore( config : undefined ) {
    execution: concurrent

    outputPort Subscriber {
        protocol: http { format = "json" } 
        interfaces: NotificationInterface
    }

    inputPort IP {
        location: config.EventStore.location
        protocol: http { format = "json" } 
        interfaces:
            EventStoreInterface,
            ShutDownInterface
    }

    embed Console as C
    embed StringUtils as S

    main {
        [ subscribe( subscriber )( response ) {
            valueToPrettyString@S( subscriber )( str )
            println@C( "Subscription: " + str )()
            for( topic in subscriber.topics ) {
                loc = subscriber.location
                thisTopic -> global.topics.( topic )
                thisTopic.subscribers.( loc ) = loc
                for ( ev in thisTopic.events ) {
                    synchronized( subscriberLocation ) {
                        Subscriber.location = subscriber.location
                        notify@Subscriber( ev )
                    }
                }
            }
            valueToPrettyString@S( global.topics )( str )
            println@C( "State of topics variable: " )()
            println@C( str )()
            response = "OK"
        } ] { nullProcess }
        [ unsubscribe( subscriber )( response ) {
            for( topic in subscriber.topics ) {
                loc = subscriber.location
                undef( global.topics.( topic ).subscribers.( loc ) )
            }
        } ] { nullProcess }
        [ publishEvent( event ) ] {
            valueToPrettyString@S( event )( str )
            println@C( "Received event " + str )()
            eventsArray -> global.topics.( event.type ).events
            synchronized( dbEvents ) {
                pushBack -> eventsArray[#eventsArray]
                pushBack << event
            }
            valueToPrettyString@S( eventsArray )( str )
            println@C( "Events Array: " + str )()
            subscribersMap -> global.topics.( event.type ).subscribers
            foreach( subscriber : subscribersMap ) {
                synchronized( subscriberLocation ) {
                    Subscriber.location = subscriber
                    notify@Subscriber( event )
                }
            }
        }
        [ shutDown( void ) ]{
              println@C( "Shutting down" )()
              exit
          }
    }
}

service Test( config: undefined ) {

    outputPort EventStore {
        location: config.EventStore.location
        protocol: http { format = "json" } 
        interfaces:
            EventStoreInterface,
            ShutDownInterface
    }

    outputPort CommandSide {
        location: config.CommandSide.location
        protocol: http { format = "json" } 
        interfaces:
            CommandSideInterface,
            ShutDownInterface
    }

    outputPort QuerySide {
        location: config.QuerySide.location
        protocol: http { format = "json" } 
        interfaces:
            QuerySideInterface,
            ShutDownInterface
    }

    embed Time as time
    embed EventStore(config) in EventStore
    embed CommandSide(config) in CommandSide
    embed QuerySide(config) in QuerySide

    inputPort ip {
        location: config.Test.location
        protocol: http { format = "json" }
        interfaces:
            NotificationInterface
    }

    main {
        sleep@time( 1000 )()
        subscribe@EventStore( {
            location = config.Test.location
            topics[0] = "PA_CREATED"
            topics[1] = "PA_DELETED"
        } )( res )
        parkingArea << {
            name = "PA_123"
            chargingSpeed = "FAST"
            availability[0] << { start = 8 end = 13 }
        }
        createParkingArea@CommandSide( parkingArea )( paid )
        notify( event )
        if( event.type != "PA_CREATED" || event.id != paid )
            throw( AssertionFailed )
        deleteParkingArea@CommandSide( paid )()
        notify( event )
        if( event.type != "PA_DELETED" || event.id != paid )
            throw( AssertionFailed )
    }
}