from runtime import Runtime
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
type ChargingSpeed: string
type TimePeriod: void {
	start: int
	end: int
}
type PACreatedEvent: void {
	type: string
	id: PAID
	info: ParkingAreaInformation
}
type PAUpdatedEvent: void {
	type: string
	id: PAID
	info: ParkingAreaInformation
}
type PADeletedEvent: void {
	type: string
	id: PAID
}
type DomainEvent: PACreatedEvent | PAUpdatedEvent | PADeletedEvent
interface CommandSideInterface {
	RequestResponse:
		updateParkingArea( ParkingArea )( string ),
		deleteParkingArea( PAID )( string ),
		createParkingArea( ParkingAreaInformation )( PAID )
}
interface ShutDownInterface {
	OneWay:
		shutDown( void )
}
type GetParkingAreaResponse: ParkingArea | string
type GetParkingAreasResponse: void {
	list*: ParkingAreaInformation
}
type Location: int
interface QuerySideInterface {
	RequestResponse:
		getParkingArea( PAID )( GetParkingAreaResponse ),
		getParkingAreas( Location )( GetParkingAreasResponse )
}
interface NotificationInterface {
	OneWay:
		notify( DomainEvent )
}
type Topic: string
type Subscriber: void {
	topics[1,*]: Topic
	location: string
}
type SubscriptionResponse: string
interface EventStoreInterface {
	OneWay:
		publishEvent( DomainEvent )
	RequestResponse:
		subscribe( Subscriber )( SubscriptionResponse ),
		unsubscribe( Subscriber )( string )
}
service Test ( config : undefined ){
	outputPort EventStore {
		location: config.EventStore.location
		protocol: http{
			format = "json"
		}
		interfaces:
			EventStoreInterface,
			ShutDownInterface
	}
	outputPort CommandSide {
		location: config.CommandSide.location
		protocol: http{
			format = "json"
		}
		interfaces:
			CommandSideInterface,
			ShutDownInterface
	}
	outputPort QuerySide {
		location: config.QuerySide.location
		protocol: http{
			format = "json"
		}
		interfaces:
			QuerySideInterface,
			ShutDownInterface
	}
	embed Time as time
	embed Console as console
	embed StringUtils as su
	embed Runtime as runtime
	inputPort ip {
		location: config.Test.location
		protocol: http{
			format = "json"
		}
		interfaces: NotificationInterface
	}
	define printLoading {
		for( i = 0, i < 3, i++ ){
			sleep@time( 200 )(  )
			print@console( ". " )(  )
		}
		sleep@time( 200 )(  )
		println@console( "done!" )(  )
	}
	init {
		global.debug = config.Test.debug
		if( !is_defined( config.Test.docker ) || !config.Test.docker ){
			dependencies[0] << {
				service = "EventStore"
				filepath = "monolith.ol"
				params -> config
			}
			dependencies[1] << {
				service = "CommandSide"
				filepath = "monolith.ol"
				params -> config
			}
			println@console( "---- EMBEDDING DEPENDENCIES ----" )(  )
			for( service[0] in dependencies ){
				print@console( "Embedding " + service.service + ": " )(  )
				loadEmbeddedService@runtime( service )(  )
				printLoading
			}
		} else {
      replaceAll@su( config.Test.location { regex = "localhost" replacement = "test" } )( config.Test.location )
    }
	}
	main {
		sleep@time( 1000 )(  )
		subscription << {
			location = config.Test.location
			topics[0] = "PA_CREATED"
			topics[1] = "PA_DELETED"
		}
		if( global.debug ){
			println@console( "Subscribing as:" )(  )
			valueToPrettyString@su( subscription )( dbg )
			println@console( dbg )(  )
		}
		subscribe@EventStore( subscription )( res )
		parkingArea << {
			name = "PA_123"
			chargingSpeed = "FAST"
			availability[0] << {
				start = 8
				end = 13
			}
		}
		if( global.debug ){
			println@console( "Creating Parking Area:" )(  )
			valueToPrettyString@su( parkingArea )( dbg )
			println@console( dbg )(  )
		}
		createParkingArea@CommandSide( parkingArea )( paid )
		notify( event )
		if( global.debug ){
			println@console( "Received Event:" )(  )
			valueToPrettyString@su( event )( dbg )
			println@console( dbg )(  )
		}
		if( event.type != "PA_CREATED" || event.id != paid ){
			throw( AssertionFailed )
		}
		if( global.debug ){
			println@console( "Deleting Parking Area with ID: " + paid )(  )
		}
		deleteParkingArea@CommandSide( paid )(  )
		notify( event )
		if( global.debug ){
			println@console( "Received Event:" )(  )
			valueToPrettyString@su( event )( dbg )
			println@console( dbg )(  )
		}
		if( event.type != "PA_DELETED" || event.id != paid ){
			throw( AssertionFailed )
		}
		println@console( "Test passed." )(  )
	}
}