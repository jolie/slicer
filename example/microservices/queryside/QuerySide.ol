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
interface NotificationInterface {
	oneWay:
		notify( DomainEvent )
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
service QuerySide ( config : undefined ){
	execution: concurrent
	inputPort InputQuery {
		location: config.QuerySide.locations._[0]
		protocol: http{
			format = "json"
		}
		interfaces:
			QuerySideInterface,
			NotificationInterface,
			ShutDownInterface
	}
	outputPort EventStore {
		location: config.EventStore.locations._[0]
		protocol: http{
			format = "json"
		}
		interfaces: EventStoreInterface
	}
	embed Console as C
	embed StringUtils as S
	embed Time as T
	init {
		global.debug = config.QuerySide.params.debug
		subscriber.location = config.QuerySide.locations._[0]
		pushBackTopic[0]->subscriber.topics[#subscriber.topics]
		pushBackTopic = "PA_CREATED"
		pushBackTopic = "PA_UPDATED"
		pushBackTopic = "PA_DELETED"
		if( global.debug ){
			valueToPrettyString@S( subscriber )( str )
			println@C( str )(  )
		}
		sleep@T( 1000 )(  )
		subscribe@EventStore( subscriber )( res )
		println@C( "Queryside subscription: " + res )(  )
	}
	main {
		[ getParkingArea( id )( response ){
			synchronized( dbToken ){
				println@C( "Get " + id )(  )
				if( is_defined( global.db[id] ) ){
					response[0] << global.db[id]
				} else {
					response = "NOT FOUND"
				}
			}
		} ]{
			nullProcess
		}
		[ getParkingAreas( location )( response ){
			synchronized( dbToken ){
				dbsize = #global.db
				pushBack[0]->response.list[#response.list]
				if( dbsize > 0 ){
					if( dbsize <= 3 ){
						for( pa[0] in global.db ){
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
		} ]{
			nullProcess
		}
		[ notify( event ) ]{
			if( global.debug ){
				valueToPrettyString@S( event )( str )
				println@C( "Notified of: " + str )(  )
			}
			type->event.type
			if( type == "PA_CREATED" || type == "PA_UPDATED" ){
				synchronized( dbToken ){
					global.db[event.id].id = event.id
					global.db[event.id].info << event.info
				}
			} else if( type == "PA_DELETED" ){
				synchronized( dbToken ){
					undef( global.db[event.id] )
				}
			}
		}
		[ shutDown( void ) ]{
			println@C( "Shutting down" )(  )
			exit
		}
	}
}