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

The Java class to be persisted:
