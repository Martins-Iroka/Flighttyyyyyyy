package com.aliumujib.flightyy.data.remote.impl

import com.aliumujib.flightyy.data.cache.airlines.AirlineCache
import com.aliumujib.flightyy.data.cache.airports.AirportCache
import com.aliumujib.flightyy.data.contracts.remote.ISchedulesRemote
import com.aliumujib.flightyy.data.model.schedule.ArrivalDepartureEntity
import com.aliumujib.flightyy.data.model.schedule.FlightEntity
import com.aliumujib.flightyy.data.model.schedule.ScheduleEntity
import com.aliumujib.flightyy.data.remote.api.retrofit.ApiService
import com.aliumujib.flightyy.data.remote.models.Schedule
import io.reactivex.Observable
import org.joda.time.format.DateTimeFormat
import java.text.SimpleDateFormat
import javax.inject.Inject

class ScheduleRemote @Inject constructor(
    var apiService: ApiService,
    var airportCache: AirportCache,
    var airlineCache: AirlineCache
) : ISchedulesRemote {

    override fun getSchedules(
        origin: String,
        destination: String,
        fromDateTime: String
    ): Observable<List<ScheduleEntity>> {
        return apiService.getCharacters(origin, destination, fromDateTime).map {
            it.ScheduleResource.Schedule
        }.map {
            createScheduleEntities(it)
        }
    }

    private fun createScheduleEntities(list: List<Schedule>): List<ScheduleEntity> {
        val scheduleList = mutableListOf<ScheduleEntity>()
        val dateTimeFormatter = SimpleDateFormat("YYYY-MM-DD'T'HH:MM")

        list.forEach {
            val flights = mutableListOf<FlightEntity>()
            it.Flight.forEach { flight ->
                val arrival = ArrivalDepartureEntity(
                    airportCache.get(flight.Arrival.AirportCode),
                    dateTimeFormatter.parse(flight.Arrival.ScheduledTimeLocal.DateTime)
                )
                val departure = ArrivalDepartureEntity(
                    airportCache.get(flight.Departure.AirportCode),
                    dateTimeFormatter.parse(flight.Departure.ScheduledTimeLocal.DateTime)
                )
                val airline = airlineCache.get(flight.MarketingCarrier.AirlineID)

                val flightEntity = FlightEntity(
                    arrival,
                    departure,
                    airline,
                    flight.MarketingCarrier.FlightNumber,
                    flight.Details.Stops.StopQuantity
                )

                flights.add(flightEntity)
            }

            val dtf = DateTimeFormat.forPattern("mm'H'ss'M'")
            val scheduleEntity = ScheduleEntity(flights, 0L)
            scheduleList.add(scheduleEntity)
        }

        return scheduleList
    }

}