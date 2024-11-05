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
	OneWay:
		shutDown( void )
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
service EventStore ( config : undefined ){
	execution: concurrent
	outputPort Subscriber {
		protocol: http{
			format = "json"
		}
		interfaces: NotificationInterface
	}
	inputPort IP {
		location: config.EventStore.location
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
		global.debug = config.EventStore.debug
	}
	main {
		[ subscribe( subscriber )( response ){
			if( global.debug ){
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
			if( global.debug ){
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
		} ]{
			nullProcess
		}
		[ publishEvent( event ) ]{
			if( global.debug ){
				valueToPrettyString@S( event )( str )
				println@C( "Received event " + str )(  )
			}
			eventsArray->global.topics.( event.type ).events
			synchronized( dbEvents ){
				pushBack[0]->eventsArray[#eventsArray]
				pushBack << event
			}
			if( global.debug ){
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