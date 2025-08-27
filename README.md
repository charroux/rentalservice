# rentalservice

## PUT using curl

```
curl --header "Content-Type: application/json" \
--request PUT \
--data '{"begin":"1/9/2025","end":"2/9/2025"}' \
"http://localhost:8080/cars/AA11BB?rent=true"
```