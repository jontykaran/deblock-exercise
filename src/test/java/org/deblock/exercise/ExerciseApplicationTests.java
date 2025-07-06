package org.deblock.exercise;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"crazyair.api.url=http://localhost:8001/flights",
		"toughjet.api.url=http://localhost:8002/flights"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExerciseApplicationTests {

	private WireMockServer crazyAirMockServer = new WireMockServer(8001);
	private WireMockServer toughJetMockServer = new WireMockServer(8002);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeAll
	void startWireMockServers() {
		crazyAirMockServer.start();
		toughJetMockServer.start();
	}

	@AfterAll
	void stopWireMockServers() {
		crazyAirMockServer.stop();
		toughJetMockServer.stop();
	}

	@BeforeEach
	void resetWireMocks() {
		crazyAirMockServer.resetAll();
		toughJetMockServer.resetAll();
	}

	@Test
	void shouldReturnMergedFlightResultsFromAllSuppliers() throws Exception {
		// given
		LocalDate departureDate = LocalDate.of(2025, 7, 20);
		LocalDate returnDate = LocalDate.of(2025, 7, 25);
		SearchRequest request = new SearchRequest("LHR", "AMS", departureDate, returnDate, 1);

		// stub CrazyAir API
		crazyAirMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.withQueryParam("origin", equalTo("LHR"))
				.withQueryParam("destination", equalTo("AMS"))
				.withQueryParam("departureDate", equalTo("2025-07-20"))
				.withQueryParam("returnDate", equalTo("2025-07-25"))
				.withQueryParam("passengerCount", equalTo("1"))
				.willReturn(okJson("""
				[
				  {
					"airline": "CrazyAir",
					"price": 123.45,
					"cabinclass": "E",
					"departureAirportCode": "LHR",
					"destinationAirportCode": "AMS",
					"departureDate": "2025-07-20T10:00:00",
					"arrivalDate": "2025-07-20T12:00:00"
				  }
				]
			""")));

		// stub ToughJet API
		toughJetMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.withQueryParam("from", equalTo("LHR"))
				.withQueryParam("to", equalTo("AMS"))
				.withQueryParam("outboundDate", equalTo("2025-07-20"))
				.withQueryParam("inboundDate", equalTo("2025-07-25"))
				.withQueryParam("numberOfAdults", equalTo("1"))
				.willReturn(okJson("""
				[
				  {
					"carrier": "ToughJetAir1",
					"basePrice": 100.0,
					"tax": 10.0,
					"discount": 10.0,
					"departureAirportName": "LHR",
					"arrivalAirportName": "AMS",
					"outboundDateTime": "2025-07-20T10:00:00",
					"inboundDateTime": "2025-07-25T20:00:00"
				  },
				  {
					"carrier": "ToughJetAir2",
					"basePrice": 320.0,
					"tax": 45.0,
					"discount": 20.0,
					"departureAirportName": "LHR",
					"arrivalAirportName": "AMS",
					"outboundDateTime": "2025-07-20T10:00:00",
					"inboundDateTime": "2025-07-25T20:00:00"
				  }
				]
			""")));

		// when requested
		String responseJson = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/flights")
						.param("origin", request.origin())
						.param("destination", request.destination())
						.param("departureDate", request.departureDate().toString())
						.param("returnDate", request.returnDate().toString())
						.param("numberOfPassengers", String.valueOf(request.numberOfPassengers()))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		// then assert
		List<FlightResponse> actualFlights = objectMapper.readValue(responseJson, new TypeReference<>() {});
		List<FlightResponse> expectedFlights = List.of(
				new FlightResponse("ToughJetAir1", "ToughJet",  99.0, "LHR", "AMS", LocalDateTime.of(2025, 7, 20, 10, 0), LocalDateTime.of(2025, 7, 25, 20, 0)),
				new FlightResponse("CrazyAir", "CrazyAir", 123.45, "LHR", "AMS", LocalDateTime.of(2025, 7, 20, 10, 0), LocalDateTime.of(2025, 7, 20, 12, 0)),
				new FlightResponse("ToughJetAir2", "ToughJet", 292.0, "LHR", "AMS",  LocalDateTime.of(2025, 7, 20, 10, 0), LocalDateTime.of(2025, 7, 25, 20, 0))
		);
		assertEquals(expectedFlights, actualFlights);
	}

	@Test
	void shouldReturnEmptyListWhenNoFlightsFromSuppliers() throws Exception {
		SearchRequest request = new SearchRequest("LHR", "AMS", LocalDate.of(2025, 7, 20), LocalDate.of(2025, 7, 25), 1);

		crazyAirMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.willReturn(okJson("[]")));

		toughJetMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.willReturn(okJson("[]")));

		String responseJson = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/flights")
						.param("origin", request.origin())
						.param("destination", request.destination())
						.param("departureDate", request.departureDate().toString())
						.param("returnDate", request.returnDate().toString())
						.param("numberOfPassengers", String.valueOf(request.numberOfPassengers()))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		List<FlightResponse> actual = objectMapper.readValue(responseJson, new TypeReference<>() {});
		assertEquals(List.of(), actual);
	}

	@Test
	void shouldReturnFlightsWhenOnlyCrazyAirResponds() throws Exception {
		SearchRequest request = new SearchRequest("LHR", "AMS", LocalDate.of(2025, 7, 20), LocalDate.of(2025, 7, 25), 1);

		crazyAirMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.willReturn(okJson("""
                        [
                          {
                            "airline": "CrazyAir",
                            "price": 123.45,
                            "cabinclass": "E",
                            "departureAirportCode": "LHR",
                            "destinationAirportCode": "AMS",
                            "departureDate": "2025-07-20T10:00:00",
                            "arrivalDate": "2025-07-20T12:00:00"
                          }
                        ]
                    """)));

		toughJetMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.willReturn(okJson("[]")));

		String responseJson = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/flights")
						.param("origin", request.origin())
						.param("destination", request.destination())
						.param("departureDate", request.departureDate().toString())
						.param("returnDate", request.returnDate().toString())
						.param("numberOfPassengers", String.valueOf(request.numberOfPassengers()))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		List<FlightResponse> actual = objectMapper.readValue(responseJson, new TypeReference<>() {});
		List<FlightResponse> expected = List.of(
				new FlightResponse("CrazyAir", "CrazyAir", 123.45, "LHR", "AMS",
						LocalDateTime.of(2025, 7, 20, 10, 0),
						LocalDateTime.of(2025, 7, 20, 12, 0))
		);

		assertEquals(expected, actual);
	}

	@Test
	void shouldReturnExceptionWhenBothSupplierFails() throws Exception {
		SearchRequest request = new SearchRequest("LHR", "AMS", LocalDate.of(2025, 7, 20), LocalDate.of(2025, 7, 25), 1);

		crazyAirMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.willReturn(serverError()));

		toughJetMockServer.stubFor(get(urlPathEqualTo("/flights"))
				.willReturn(serverError()));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/flights")
						.param("origin", request.origin())
						.param("destination", request.destination())
						.param("departureDate", request.departureDate().toString())
						.param("returnDate", request.returnDate().toString())
						.param("numberOfPassengers", String.valueOf(request.numberOfPassengers()))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is5xxServerError());
	}

	@Test
	void shouldReturnExceptionWhenInvalidRequestInputIsSent() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/flights")
						.param("origin", "LHR")
						.param("destination", "AMS")
						.param("departureDate", "2025-07-05")
						.param("returnDate", "2025-07-08")
						.param("numberOfPassengers", "5") // too many passangers
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenParamsMissing() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/flights")
						.param("origin", "LHR")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}
}
