from string_utils import StringUtils
from console import Console
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
service CommandSide ( config : undefined ){
	execution: concurrent
	inputPort InternalCommands {
		location: config.CommandSide.locations[0]
		protocol: http{
			format = "json"
		}
		interfaces:
			CommandSideInterface,
			ShutDownInterface
	}
	inputPort ExternalCommands {
		location: config.CommandSide.locations[1]
		protocol: http{
			format = "json"
		}
		interfaces: CommandSideInterface
	}
	outputPort EventStore {
		location: config.EventStore.locations[0]
		protocol: http{
			format = "json"
		}
		interfaces: EventStoreInterface
	}
	embed Console as C
	embed StringUtils as S
	init {
		debug = config.CommandSide.params.debug
	}
	main {
		[ createParkingArea( pa )( id ){
			synchronized( dbToken ){
				id = #global.db
				global.db[id].id = id
				global.db[id].info << pa
			}
		} ]{
			if( debug ){
				valueToPrettyString@S( pa )( str )
				println@C( "UPDATED: " + str )(  )
			}
			synchronized( dbToken ){
				event.type = "PA_CREATED"
				event[0] << global.db[id]
			}
			publishEvent@EventStore( event )
		}
		[ updateParkingArea( pa )( r ){
			valueToPrettyString@S( pa )( str )
			println@C( "UPDATED: " + str )(  )
			synchronized( dbToken ){
				global.db[pa.id].info << pa.info
			}
			r = "OK"
		} ]{
			event.type = "PA_UPDATED"
			event << pa
			if( debug ){
				valueToPrettyString@S( pa )( str )
				println@C( "UPDATED: " + str )(  )
			}
			publishEvent@EventStore( event )
		}
		[ deleteParkingArea( id )( r ){
			synchronized( dbToken ){
				undef( global.db[id] )
			}
			r = "OK"
		} ]{
			event << {
				type = "PA_DELETED"
				id = id
			}
			publishEvent@EventStore( event )
		}
		[ shutDown( void ) ]{
			println@C( "Shutting down" )(  )
			exit
		}
	}
}