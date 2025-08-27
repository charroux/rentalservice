# rentalservice

## PUT using curl

```
curl --header "Content-Type: application/json" \
--request PUT \
--data '{"begin":"1/9/2025","end":"2/9/2025"}' \
"http://localhost:8080/cars/AA11BB?rent=true"
```

## Cloud natuve rule

Never log in the console.
Use a logger instead : https://github.com/charroux/rentalservice/blob/main/src/main/java/com/example/rent/RentWebService.java

## Java Persistance API

The Java class to be persisted:https://github.com/charroux/rentalservice/blob/main/src/main/java/com/example/rent/data/Car.java

The interface to access programmaticaly the database: https://github.com/charroux/rentalservice/blob/main/src/main/java/com/example/rent/data/CarRepository.java

Database initial values inside the main: https://github.com/charroux/rentalservice/blob/main/src/main/java/com/example/rent/RentApplication.java

### The Database 

#### Libraries 
Spring library
```
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```
The datavase:
```
implementation 'com.h2database:h2'
```

See: https://github.com/charroux/rentalservice/blob/main/build.gradle

### Web console activation
```
spring.h2.console.enabled=true
```
See: https://github.com/charroux/rentalservice/blob/main/src/main/resources/application.properties

### Start the project and get an accesss to the console

http://localhost:8080/h2-console

Retrieve the connection's info in the logs.
Example : H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:414949e9-2995-4cb8-a50b-0010fa244021'
Use this value as the JDBC URL

