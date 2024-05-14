package it.corso.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import it.corso.dto.UserLoginRequestDto;
import it.corso.dto.UserLoginResponseDto;
import it.corso.dto.UserRegistrationDto;
import it.corso.model.User;
import it.corso.service.UserService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Path("/user")
public class UserController {

	@Autowired
	private UserService userService;
	
	
	
	@POST
	@Path("/registration")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(@Valid @RequestBody UserRegistrationDto userRegistrationDto){
		try {
			if(!Pattern.matches("(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}", userRegistrationDto.getPassword())){
				return Response.status(Response.Status.BAD_REQUEST).build();
				
			}
			
			if(userService.existsUser(userRegistrationDto.getEmail())){
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
			
			userService.registerUser(userRegistrationDto);
			return Response.status(Response.Status.OK).build();
			
		} catch (Exception e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
	}
	
	
	
	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response loginUtente(@RequestBody UserLoginRequestDto userLoginRequestDto){
		
		try {
			if(userService.loginUser(userLoginRequestDto)){
				return Response.ok(issueToken(userLoginRequestDto.getEmail())).build();
			}
			
			return Response.status(Response.Status.BAD_REQUEST).build();
			
		} catch (Exception e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
	}
	
	
	
	
	
	
	private UserLoginResponseDto issueToken(String email) {
		// Definiamo una chiave segreta (array di byte) necessario poi per la crittografia HMAC
		byte[] secretKey = "supersecretkeytestfinale123456789101112131415161718192021222324252627282930".getBytes();
		// Creiamo una chiave per l'algoritmo HMAC
		Key key = Keys.hmacShaKeyFor(secretKey);
		// Otteniamo le informazioni dell'utente dal servizio
		User userInfo = userService.getUserByEmail(email);
		// Creiamo un insieme di informazioni da includere nel token (claims) [ coppia <"nome", valore> ]
		Map<String, Object> map = new HashMap<>();
		map.put("name", userInfo.getName());
		map.put("lastname", userInfo.getLastname());
		map.put("email", email);
		map.put("city",  userInfo.getCity());
		
		List<String> weatherForecast = new ArrayList<>();
		userInfo.getWeatherForecast().forEach(weather -> weatherForecast.add(weather.getName()));
		map.put("weatherForecast", weatherForecast);
		
		// Definiamo la data di creazione e il tempo di vita del token
		Instant now = Instant.now();
		Date creationDate = Date.from(now); 					//data creazione
		Date end = Date.from(now.plus(60, ChronoUnit.MINUTES)); //TTL di 60min
		
		// Creiamo il token JWT firmato con la chiave segreta
		String jwtToken = Jwts.builder()
		    .setClaims(map)                     // Impostiamo le informazioni aggiuntive (claims) nel token
		    .setIssuer("http://localhost:8080") // Indichiamo chi ha emesso il token
		    .setIssuedAt(creationDate)          // Impostiamo la data di creazione del token
		    .setExpiration(end)                 // Impostiamo la scadenza del token
		    .signWith(key)                      // Firmiamo il token con la chiave segreta
		    .compact();                         // Compattiamo il token in una stringa

		// Creiamo un oggetto UtenteLoginResponseDto per contenere il token e altre informazioni
		UserLoginResponseDto token = new UserLoginResponseDto();
		token.setToken(jwtToken);                   // Impostiamo il token JWT
		token.setTokenCreationTime(creationDate);    // Impostiamo il timestamp di creazione del token
		token.setTtl(end);                          // Impostiamo il tempo di vita del token

		// Restituiamo il token JWT e altre informazioni associate
		return token;
	}


}
