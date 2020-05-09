package it.polito.tdp.extflightdelays.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import it.polito.tdp.extflightdelays.model.Airline;
import it.polito.tdp.extflightdelays.model.Airport;
import it.polito.tdp.extflightdelays.model.Flight;
import it.polito.tdp.extflightdelays.model.Rotta;

public class ExtFlightDelaysDAO {

	public List<Airline> loadAllAirlines() {
		String sql = "SELECT * from airlines";
		List<Airline> result = new ArrayList<Airline>();

		try {
			Connection conn = ConnectDB.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				result.add(new Airline(rs.getInt("ID"), rs.getString("IATA_CODE"), rs.getString("AIRLINE")));
			}

			conn.close();
			return result;

		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Errore connessione al database");
			throw new RuntimeException("Error Connection Database");
		}
	}

	//invece di farci ritornare la lista di aereoporti, andiamo a manipolare la nostra mappa
	//di aereoporti, in base a quello che trovo e aggiungendo quelli con id che non c'era ancora
	public void loadAllAirports(Map<Integer, Airport> idMap) {
		String sql = "SELECT * FROM airports";

		try {
			Connection conn = ConnectDB.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				
				if(!idMap.containsKey(rs.getInt("ID"))) {
					Airport airport = new Airport(rs.getInt("ID"), rs.getString("IATA_CODE"), rs.getString("AIRPORT"),
						rs.getString("CITY"), rs.getString("STATE"), rs.getString("COUNTRY"), rs.getDouble("LATITUDE"),
						rs.getDouble("LONGITUDE"), rs.getDouble("TIMEZONE_OFFSET"));
					idMap.put(airport.getId(), airport);
				}
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Errore connessione al database");
			throw new RuntimeException("Error Connection Database");
		}
	}

	public List<Flight> loadAllFlights() {
		String sql = "SELECT * FROM flights";
		List<Flight> result = new LinkedList<Flight>();

		try {
			Connection conn = ConnectDB.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Flight flight = new Flight(rs.getInt("ID"), rs.getInt("AIRLINE_ID"), rs.getInt("FLIGHT_NUMBER"),
						rs.getString("TAIL_NUMBER"), rs.getInt("ORIGIN_AIRPORT_ID"),
						rs.getInt("DESTINATION_AIRPORT_ID"),
						rs.getTimestamp("SCHEDULED_DEPARTURE_DATE").toLocalDateTime(), rs.getDouble("DEPARTURE_DELAY"),
						rs.getDouble("ELAPSED_TIME"), rs.getInt("DISTANCE"),
						rs.getTimestamp("ARRIVAL_DATE").toLocalDateTime(), rs.getDouble("ARRIVAL_DELAY"));
				result.add(flight);
			}

			conn.close();
			return result;

		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Errore connessione al database");
			throw new RuntimeException("Error Connection Database");
		}
	}

	//contiamo il numero effettivo distinto di compagnie che operano su un certo aereoporto che passiamo come parametro
	public int getAirlinesNumber(Airport a) {
		String sql = "SELECT COUNT(DISTINCT(AIRLINE_ID)) AS tot " + 
				"FROM flights " + 
				"WHERE (ORIGIN_AIRPORT_ID = ?) || (DESTINATION_AIRPORT_ID = ?)";
		int res = -1;
		try {
			Connection conn = ConnectDB.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			st.setInt(1, a.getId());
			st.setInt(2, a.getId());
			ResultSet rs = st.executeQuery();
			
			if(rs.next()) {
				res = rs.getInt("tot");
			}
			conn.close();
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Errore connessione al database");
			throw new RuntimeException("Error Connection Database");
		}
		
		return res;
	}

	/*
	 	Si fa dare tutte le rotte in cui ho le rotte nelle singole direzioni (che poi dovremo gestire
	 	in quanto non vorremo poi avere le direzioni, ma gli archi sono non orientati) in cui raggruppo
	 	dai voli, per aereoporto di partenza e arrivo e conto quante righe ci sono in questa direzione
	 	specifica perche' il peso degli archi in questo esercizio non e' piu' la distanza media, ma solo
	 	il numero di connessioni che ci sono.
	 	
	 	Dal risultato della query si fa ridare tutte le rotte usando la identity map degli aereoporti
	 */
	public List<Rotta> getRotte(Map<Integer, Airport> idMap) {
		String sql = "SELECT ORIGIN_AIRPORT_ID, DESTINATION_AIRPORT_ID, COUNT(*) as tot " +
					"FROM flights " +
					"GROUP BY ORIGIN_AIRPORT_ID, DESTINATION_AIRPORT_ID";
		List<Rotta> result = new ArrayList<Rotta>();
		try {
			Connection conn = ConnectDB.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();
			
			while(rs.next()) {
				Airport sorgente = idMap.get(rs.getInt("ORIGIN_AIRPORT_ID"));
				Airport destinazione = idMap.get(rs.getInt("DESTINATION_AIRPORT_ID"));
				
				if(sorgente != null && destinazione != null) {
					result.add(new Rotta(sorgente, destinazione, rs.getInt("tot")));
				} else{
					System.out.println("ERRORE IN GET ROTTE");
				}

			}
			conn.close();
		}catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Errore connessione al database");
			throw new RuntimeException("Error Connection Database");
		}
		
		return result;
	}
}
