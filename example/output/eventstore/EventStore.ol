from string_utils import StringUtils
from console import Console
type PAID: long
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
service EventStore ( config : undefined ){
	execution: concurrent
	outputPort Subscriber {
		protocol: http{
			format = "json"
		}
		interfaces: NotificationInterface
	}
	inputPort IP {
		location: config.EventStore.locations[0]
		protocol: http{
			format = "json"
		}
		interfaces:
			EventStoreInterface,
			ShutDownInterface
	}
	embed Console as C
	embed StringUtils as S
	init {
		debug = config.EventStore.params.debug
	}
	main {
		[ subscribe( subscriber )( response ){
			if( debug ){
				valueToPrettyString@S( subscriber )( str )
				println@C( "Subscription: " + str )(  )
			}
			for( topic[0] in subscriber.topics ){
				loc = subscriber.location
				thisTopic->global.topics.( topic )
				synchronized( subscriberLocation ){
					thisTopic.subscribers.( loc ) = loc
				}
			}
			if( debug ){
				valueToPrettyString@S( global.topics )( str )
				println@C( "State of topics variable: " )(  )
				println@C( str )(  )
			}
			response = "OK"
		} ]{
			nullProcess
		}
		[ unsubscribe( subscriber )( response ){
			for( topic[0] in subscriber.topics ){
				loc = subscriber.location
				undef( global.topics.( topic ).subscribers.( loc ) )
			}
			response = "OK"
		} ]{
			nullProcess
		}
		[ publishEvent( event ) ]{
			if( debug ){
				valueToPrettyString@S( event )( str )
				println@C( "Received event " + str )(  )
			}
			eventsArray->global.topics.( event.type ).events
			synchronized( dbEvents ){
				pushBack[0]->eventsArray[#eventsArray]
				pushBack << event
			}
			if( debug ){
				valueToPrettyString@S( eventsArray )( str )
				println@C( "Events Array: " + str )(  )
			}
			subscribersMap->global.topics.( event.type ).subscribers
			foreach( subscriber : subscribersMap ){
				synchronized( subscriberLocation ){
					Subscriber.location = subscriber
					notify@Subscriber( event )
				}
			}
		}
		[ shutDown( void ) ]{
			println@C( "Shutting down" )(  )
			exit
		}
	}
}