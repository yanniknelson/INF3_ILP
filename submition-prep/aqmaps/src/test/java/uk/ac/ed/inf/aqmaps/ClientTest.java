package uk.ac.ed.inf.aqmaps;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ClientTest {
	
	@Mock
	HttpClient mockHttpClient;
	
	@Mock
	HttpResponse mockNoFlyResponse;
	
	@Captor ArgumentCaptor<HttpRequest> URLcaptor;

	@Test
	void testGetNoFly() throws IOException, InterruptedException, NoSuchFieldException, SecurityException {
		
		Integer port = 80;
//		String day = "01";
//		String month = "01";
//		String year = "2020";
		
		String ExpectedRequest = "http://localhost:" + Integer.toString(port) + "/buildings/no-fly-zones.geojson GET";
		
		Mockito.when(mockNoFlyResponse.body()).thenReturn(Files.readString(Path.of("src/test/resources/buildings/no-fly-zones.geojson")));
		Mockito.when(mockHttpClient.send(URLcaptor.capture(),Mockito.any())).thenReturn(mockNoFlyResponse);
		
		ClientWrapper testClient = new Client(port);
		FieldSetter.setField(testClient, testClient.getClass().getDeclaredField("client"), mockHttpClient);
		ArrayList<ArrayList<Location>> nofly = testClient.getNoFly();
		assertEquals(nofly.size(), 4);
		assertEquals(nofly.get(1).size(), 5);
		assertEquals(nofly.get(2).size(), 5);
		assertEquals(nofly.get(3).size(), 11);
		assertEquals(URLcaptor.getValue().toString(), ExpectedRequest);
	}

	@Test
	void testGetDestinations() throws IOException, InterruptedException, NoSuchFieldException, SecurityException {
		Integer port = 80;
		String day = "01";
		String month = "01";
		String year = "2020";
		
//		Mockito.when(mockNoFlyResponse.body()).thenReturn(Files.readString(Path.of("src/test/resources/words")));
		Mockito.when(mockHttpClient.send(Mockito.any(),Mockito.any())).thenAnswer(new Answer<HttpResponse<String>>() {
			@Override
			public HttpResponse<String> answer(InvocationOnMock invocation) throws Throwable {
				var response = Mockito.mock(HttpResponse.class);
				var invargs = invocation.getArguments();
				var request = (HttpRequest)invargs[0];
				var parts = String.join("/", Arrays.copyOfRange(request.toString().split(" ")[0].split("/"), 3, request.toString().split(" ")[0].split("/").length));
				String body = Files.readString(Path.of("src/test/resources/" + parts));
				Mockito.when(response.body()).thenReturn(body);
				return response;
			}
		});
		
		ClientWrapper testClient = new Client(port);
		FieldSetter.setField(testClient, testClient.getClass().getDeclaredField("client"), mockHttpClient);
		ArrayList<Sensor> t = testClient.getDestinations(day, month, year);
		assertEquals(t.size(), 33);
	}

	@Test
	void testLocationFromWords() throws IOException, InterruptedException, NoSuchFieldException, SecurityException {
		Integer port = 80;
		
		Mockito.when(mockHttpClient.send(Mockito.any(),Mockito.any())).thenAnswer(new Answer<HttpResponse<String>>() {
			@Override
			public HttpResponse<String> answer(InvocationOnMock invocation) throws Throwable {
				var response = Mockito.mock(HttpResponse.class);
				var invargs = invocation.getArguments();
				var request = (HttpRequest)invargs[0];
				var parts = String.join("/", Arrays.copyOfRange(request.toString().split(" ")[0].split("/"), 3, request.toString().split(" ")[0].split("/").length));
				System.out.println(parts);
				String body = Files.readString(Path.of("src/test/resources/" + parts));
				Mockito.when(response.body()).thenReturn(body);
				return response;
			}
		});
		
		ClientWrapper testClient = new Client(port);
		FieldSetter.setField(testClient, testClient.getClass().getDeclaredField("client"), mockHttpClient);
		Location t = testClient.LocationFromWords("along.spill.limp");
		assertEquals(t.longitude(), -3.191161);
		assertEquals(t.latitude(), 55.942742);
	}

}
