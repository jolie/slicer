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
	requestResponse:
		updateParkingArea( ParkingArea )( string ),
		deleteParkingArea( PAID )( string ),
		createParkingArea( ParkingAreaInformation )( PAID )
}
interface ShutDownInterface {
	oneWay:
		shutDown( void )
}
type GetParkingAreaResponse: ParkingArea | string
type GetParkingAreasResponse: void {
	list*: ParkingAreaInformation
}
type Location: int
interface QuerySideInterface {
	requestResponse:
		getParkingArea( PAID )( GetParkingAreaResponse ),
		getParkingAreas( Location )( GetParkingAreasResponse )
}
type Topic: string
type Subscriber: void {
	topics[1,*]: Topic
	location: string
}
type SubscriptionResponse: string
interface EventStoreInterface {
	oneWay:
		publishEvent( DomainEvent )
	requestResponse:
		subscribe( Subscriber )( SubscriptionResponse ),
		unsubscribe( Subscriber )( string )
}
service Main ( config : undefined ){
	outputPort EventStore {
		location: config.EventStore.locations[0]
		protocol: http{
			format = "json"
		}
		interfaces:
			EventStoreInterface,
			ShutDownInterface
	}
	outputPort CommandSide {
		location: config.CommandSide.locations[0]
		protocol: http{
			format = "json"
		}
		interfaces:
			CommandSideInterface,
			ShutDownInterface
	}
	outputPort QuerySide {
		location: config.QuerySide.locations[0]
		protocol: http{
			format = "json"
		}
		interfaces:
			QuerySideInterface,
			ShutDownInterface
	}
	inputPort ip {
		location: config.Main.locations[0]
		protocol: http{
			format = "json"
		}
		aggregates:
			QuerySide,
			CommandSide
	}
	embed Time as time
	embed Console as console
	embed StringUtils as su
	embed Runtime as runtime
	define printLoading {
		for( i = 0, i < 3, i++ ){
			sleep@time( 200 )(  )
			print@console( ". " )(  )
		}
		sleep@time( 200 )(  )
		println@console( "done!" )(  )
	}
	init {
		debug = config.Main.params.debug
		if( is_defined( config.simulator ) && config.simulator ){
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
			dependencies[2] << {
				service = "QuerySide"
				filepath = "monolith.ol"
				params -> config
			}
			println@console( "---- EMBEDDING DEPENDENCIES ----" )(  )
			for( service[0] in dependencies ){
				print@console( "Embedding " + service.service + ": " )(  )
				loadEmbeddedService@runtime( service )(  )
				printLoading
			}
		}
	}
	main {
		linkIn( Exit )
	}
}